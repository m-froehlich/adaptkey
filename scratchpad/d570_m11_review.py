# -*- coding: utf-8 -*-
# D-570 (M11, english-dictionary-quality plan) REVIEW-LIST GENERATOR - does not modify anything.
#
# Classifies every PROPER_NOUN-only entry (tags exactly {PROPER_NOUN}) into the plan's classes:
#   KEEP-months      - the 12 months (legitimately capitalised, plan class 2)
#   KEEP-acronym     - all-caps surfaces (II, BC, SSL, HDMI, ... - plan class 2; EN corpus-acronyms
#                      are a distinct legitimate surface form)
#   KEEP-demonym     - curated demonyms/language/nationality/religious adjectives (plan class 2)
#   KEEP-geography   - curated countries/regions/major cities (plan class 2)
#   REMOVE-person    - curated person first names / surnames (plan class 1, decided: fully out,
#                      including the tops John 10,247 / Smith 2,068)
#   REMOVE-splitter  - multi-word proper-name splitter fragments (Los, Angeles, San, Francisco,
#                      York - plan §1.5 artefact subclass (a))
#   REVIEW-tail      - everything else by frequency band; the plan's §1.5 random sample says the
#                      freq <= 200 band is near-fully Wikipedia biographies/places, so the review
#                      list shows band <= 200 candidates and band > 200 stragglers separately.

import io
import re

EN = "app/src/main/assets/en/dict.tsv"
OUT = "scratchpad/d570_m11_review.tsv"

MONTHS = {
    "January", "February", "March", "April", "May", "June", "July", "August",
    "September", "October", "November", "December",
}

DEMONYMS = {
    "American", "British", "English", "French", "German", "Greek", "Roman", "Soviet", "Jewish",
    "Latin", "Chinese", "Spanish", "Italian", "Dutch", "Portuguese", "Brazilian", "Mexican",
    "Canadian", "Australian", "Indian", "Japanese", "Russian", "Polish", "Turkish", "Swedish",
    "Norwegian", "Danish", "Finnish", "Icelandic", "Irish", "Scottish", "Welsh", "European",
    "African", "Asian", "American", "Arab", "Arabic", "Islamic", "Muslim", "Christian", "Catholic",
    "Protestant", "Anglican", "Orthodox", "Buddhist", "Hindu", "Sikh", "Celtic", "Nordic", "Slavic",
    "Germanic", "Norman", "Saxon", "Viking", "Aztec", "Maya", "Inca", "Aboriginal", "Indigenous",
    "Elizabethan", "Victorian", "Edwardian", "Georgian", "Renaissance", "Gothic", "Baroque",
    "Byzantine", "Ottoman", "Persian", "Armenian", "Georgian2",  # placeholder removed below
    "Korean", "Vietnamese", "Thai", "Indonesian", "Malaysian", "Filipino", "Pakistani",
    "Bangladeshi", "Sri", "Israeli", "Palestinian", "Egyptian", "Moroccan", "Algerian",
    "Tunisian", "Libyan", "Sudanese", "Ethiopian", "Kenyan", "Nigerian", "Ghanaian", "South",
    "Zimbabwean", "Zambian", "Ugandan", "Tanzanian", "Somali", "Cuban", "Jamaican", "Haitian",
    "Dominican", "Puerto", "Rican", "Chilean", "Argentine", "Argentinian", "Colombian",
    "Peruvian", "Venezuelan", "Bolivian", "Ecuadorian", "Paraguayan", "Uruguayan", "Bolivian",
    "Albanian", "Croatian", "Serbian", "Bosnian", "Slovenian", "Slovak", "Czech", "Hungarian",
    "Bulgarian", "Romanian", "Moldovan", "Ukrainian", "Belarusian", "Lithuanian", "Latvian",
    "Estonian", "Kazakh", "Mongolian", "Nepalese", "Nepali", "Burmese", "Cambodian", "Laotian",
    "Slovene", "Macedonian", "Montenegrin", "Kosovar", "Cypriot", "Maltese", "Monacan",
    "Liechtenstein", "Luxembourg", "Belgian", "Swiss", "Austrian", "Azerbaijani", "GeorgianS",
}
DEMONYMS.discard("Georgian2")
DEMONYMS.discard("Liechtenstein")
DEMONYMS.discard("Luxembourg")   # country/city -> geography
DEMONYMS.add("Luxembourger")
DEMONYMS.add("Georgian")

COUNTRIES_REGIONS = {
    # countries and geographic regions (canonical English names)
    "Germany", "France", "England", "Scotland", "Wales", "Ireland", "Britain", "Europe", "Asia",
    "Africa", "America", "Australia", "Austria", "Belgium", "Netherlands", "Holland", "Spain",
    "Italy", "Portugal", "Switzerland", "Poland", "Czechoslovakia", "Yugoslavia", "Prussia",
    "Bavaria", "Saxony", "Swabia", "Franconia", "Hesse", "Westphalia", "Thuringia", "Silesia",
    "Pomerania", "Bohemia", "Moravia", "Galicia", "Russia", "Siberia", "Ukraine", "Belarus",
    "Lithuania", "Latvia", "Estonia", "Finland", "Sweden", "Norway", "Denmark", "Iceland",
    "Greenland", "Canada", "Mexico", "Brazil", "Argentina", "Chile", "Peru", "Colombia",
    "Venezuela", "Bolivia", "Ecuador", "Uruguay", "Paraguay", "Cuba", "Jamaica", "Haiti",
    "China", "Japan", "India", "Pakistan", "Bangladesh", "Korea", "Mongolia", "Tibet", "Himalaya",
    "Vietnam", "Thailand", "Cambodia", "Indonesia", "Malaysia", "Philippines", "Turkey", "Anatolia",
    "Armenia", "Georgia", "Azerbaijan", "Kazakhstan", "Iran", "Persia", "Iraq", "Syria", "Lebanon",
    "Israel", "Palestine", "Jordan", "Saudi", "Yemen", "Oman", "Kuwait", "Qatar", "Egypt", "Nubia",
    "Libya", "Tunisia", "Algeria", "Morocco", "Sahara", "Sudan", "Ethiopia", "Eritrea", "Somalia",
    "Kenya", "Tanzania", "Uganda", "Rwanda", "Burundi", "Congo", "Gabon", "Cameroon", "Nigeria",
    "Ghana", "Senegal", "Mali", "Niger", "Chad", "Zambia", "Zimbabwe", "Botswana", "Namibia",
    "Angola", "Mozambique", "Madagascar", "Mauritania", "Benin", "Togo", "Ivory", "Guinea",
    "Liberia", "Sierra", "Antarctica", "Arctic", "Baltic", "Adriatic", "Iberia", "Balkans",
    "Scandinavia", "Benelux", "Riviera", "Tuscany", "Lombardy", "Sicily", "Sardinia", "Corsica",
    "Crete", "Cyprus", "Malta", "Iceland2",  # placeholder removed below
    "Caribbean", "Amazon", "Andes", "Patagonia", "Yucatan", "Baja", "Appalachia", "Midwest",
    "Dixie", "Yankee", "New", "York",  # placeholders removed below - see SPLITTERS
    "Hampshire", "Jersey", "England", "Wales", "Cornwall", "Kent", "Sussex", "Essex", "Yorkshire",
    "Lancashire", "Cumbria", "Devon", "Dorset", "Somerset", "Norfolk", "Suffolk", "Wessex",
    "Mercia", "Northumbria", "Albion", "Caledonia", "Hibernia", "Britannia", "Gaul", "Iberian",
    "Bavarian", "Saxonian", "Hessian", "Swabian", "Thuringian", "Lorraine", "Brittany", "Normandy",
    "Burgundy", "Provence", "Alsace", "Picardy", "Champagne", "Silesian", "Pomeranian", "Bohemian",
    "Moravian", "Serbia", "Croatia", "Slovenia", "Slovakia", "Bulgaria", "Romania", "Moldova",
    "Albania", "Macedonia", "Bosnia", "Herzegovina", "Montenegro", "Kosovo", "Slovak2",
}
for p in ("Iceland2", "New", "York", "Slovak2"):
    COUNTRIES_REGIONS.discard(p)

CITIES = {
    "London", "Paris", "Berlin", "Vienna", "Rome", "Madrid", "Lisbon", "Dublin", "Edinburgh",
    "Glasgow", "Cardiff", "Amsterdam", "Brussels", "Copenhagen", "Stockholm", "Oslo", "Helsinki",
    "Reykjavik", "Warsaw", "Prague", "Budapest", "Vienna", "Athens", "Rome", "Moscow", "Kyiv",
    "Kiev", "Minsk", "Riga", "Vilnius", "Tallinn", "Istanbul", "Ankara", "Jerusalem", "Tel",
    "Aviv", "Cairo", "Alexandria", "Casablanca", "Algiers", "Tunis", "Tripoli", "Lagos", "Accra",
    "Nairobi", "Addis", "Ababa", "Johannesburg", "Cape", "Town", "Durban", "Delhi", "Bombay",
    "Mumbai", "Calcutta", "Kolkata", "Madras", "Bangalore", "Karachi", "Lahore", "Dhaka", "Colombo",
    "Beijing", "Peking", "Shanghai", "Canton", "Hong", "Kong", "Macau", "Tokyo", "Yokohama",
    "Osaka", "Kyoto", "Nagoya", "Sapporo", "Seoul", "Pusan", "Hanoi", "Saigon", "Bangkok",
    "Manila", "Jakarta", "Singapore", "Kuala", "Lumpur", "Sydney", "Melbourne", "Brisbane",
    "Perth", "Adelaide", "Auckland", "Wellington", "Chicago", "Boston", "Philadelphia", "Houston",
    "Dallas", "Phoenix", "Denver", "Seattle", "Portland", "Miami", "Atlanta", "Detroit",
    "Cleveland", "Pittsburgh", "Baltimore", "Vancouver", "Montreal", "Toronto", "Ottawa", "Quebec",
    "Havana", "Kingston", "Port", "Spain", "Santiago", "Lima", "Quito", "La", "Paz", "Montevideo",
    "Asuncion", "Bogota", "Caracas", "Brasilia", "Rio", "Janeiro", "Salvador", "Rome2", "San2",
}
for p in ("Rome2", "San2", "Port", "Spain"):
    # "Port"/"Spain" only as the composite "Port Spain"-style entries; drop loose fragments
    CITIES.discard("Port")
    CITIES.discard("Spain")

# Keep-lists consolidated
KEEP = MONTHS | DEMONYMS | COUNTRIES_REGIONS | CITIES

# Curated person-name lists (plan class 1: remove fully, tops included). Split into first names and
# surnames; a name matching either list is a REMOVE-person unless it collides with a KEEP class.
FIRST_NAMES = {
    "John", "James", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas",
    "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven", "Paul",
    "Andrew", "Joshua", "Kenneth", "Kevin", "Brian", "George", "Edward", "Ronald", "Timothy",
    "Jason", "Jeffrey", "Ryan", "Jacob", "Gary", "Nicholas", "Eric", "Jonathan", "Stephen", "Larry",
    "Justin", "Scott", "Brandon", "Benjamin", "Samuel", "Gregory", "Alexander", "Frank", "Raymond",
    "Patrick", "Jack", "Dennis", "Jerry", "Tyler", "Aaron", "Jose", "Adam", "Nathan", "Henry",
    "Douglas", "Zachary", "Peter", "Kyle", "Ethan", "Walter", "Noah", "Jeremy", "Christian",
    "Keith", "Roger", "Terry", "Austin", "Sean", "Gerald", "Carl", "Harold", "Dylan", "Arthur",
    "Lawrence", "Jordan", "Jesse", "Bryan", "Billy", "Bruce", "Gabriel", "Joe", "Logan", "Alan",
    "Juan", "Albert", "Willie", "Elijah", "Wayne", "Randy", "Vincent", "Mason", "Roy", "Ralph",
    "Bobby", "Russell", "Bradley", "Philip", "Eugene", "Mary", "Patricia", "Jennifer", "Linda",
    "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen", "Lisa", "Nancy", "Betty",
    "Margaret", "Sandra", "Ashley", "Kimberly", "Emily", "Donna", "Michelle", "Carol", "Amanda",
    "Dorothy", "Melissa", "Deborah", "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia",
    "Kathleen", "Amy", "Angela", "Shirley", "Anna", "Brenda", "Pamela", "Emma", "Nicole", "Helen",
    "Samantha", "Katherine", "Christine", "Debra", "Rachel", "Carolyn", "Janet", "Catherine",
    "Maria", "Heather", "Diane", "Ruth", "Julie", "Olivia", "Joyce", "Virginia", "Victoria",
    "Kelly", "Lauren", "Christina", "Joan", "Evelyn", "Judith", "Megan", "Andrea", "Cheryl",
    "Hannah", "Jacqueline", "Martha", "Gloria", "Teresa", "Ann", "Sara", "Madison", "Frances",
    "Kathryn", "Janice", "Jean", "Abigail", "Alice", "Julia", "Judy", "Sophia", "Grace", "Denise",
    "Amber", "Doris", "Marilyn", "Danielle", "Beverly", "Isabella", "Theresa", "Diana", "Natalie",
    "Brittany", "Charlotte", "Marie", "Kayla", "Alexis", "Lori", "Sophie", "Anna", "Clara",
    "Friedrich", "Wenzel", "Bucer", "Bertha", "Katherine", "Farid", "Pericles", "Gratian",
    "Katherine", "Wilhelm", "Heinrich", "Karl", "Otto", "Hans", "Wolfgang", "Ludwig", "Johann",
    "Franz", "Klaus", "Helmut", "Gerhard", "Uwe", "Rolf", "Jürgen", "Angela", "Ursula", "Monika",
    "Petra", "Claudia", "Sabine", "Ingrid", "Gisela", "Helga", "Christa", "Renate", "Anneliese",
    "Freya", "Astrid", "Ingeborg", "Adolf", "Rudolf", "Hermann", "Friedrich", "Bernhard",
    "Theodor", "Gustav", "Ferdinand", "Maximilian", "Konrad", "Lorenz", "Matthias", "Simon",
    "Timothy", "Titus", "Petrus", "Silvester", "Bonifatius", "Franziskus", "Ignatius", "Augustin",
    "Gregor", "Clemens", "Leo", "Pius", "Urban", "Colette", "Margaret", "Agnes", "Cecilia",
    "Lucia", "Ursula", "Apollonia", "Dorothea", "Klara", "Elisabeth", " Hedwig",
    "Pierre", "Louis", "Antoine", "Henri", "Etienne", "Philippe", "Jacques", "Michel", "Alain",
    "Patrick", "Nicolas", "Olivier", "Laurent", "Catherine", "Isabelle", "Nathalie", "Sylvie",
    "Céline", "Françoise", "Monique", "Yvonne", "Simone", "Renée", "Paulette", "Andrée",
    "Pietro", "Giuseppe", "Antonio", "Francesco", "Angelo", "Vittorio", "Rosa", "Lucia2",
}
for p in ("Lucia2", " Hedwig", "Céline", "Françoise", "Renée", "Paulette", "Andrée"):
    FIRST_NAMES.discard(p)
FIRST_NAMES.add("Hedwig")

SURNAMES = {
    "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore",
    "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia",
    "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen",
    "Young", "Hernandez", "King", "Wright", "Lopez", "Hill", "Green", "Adams", "Baker", "Nelson",
    "Carter", "Mitchell", "Perez", "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans",
    "Edwards", "Collins", "Stewart", "Sanchez", "Morris", "Rogers", "Reed", "Cook", "Morgan",
    "Bell", "Murphy", "Bailey", "Rivera", "Cooper", "Richardson", "Cox", "Howard", "Ward",
    "Torres", "Peterson", "Gray", "Ramirez", "Watson", "Brooks", "Kelly", "Sanders", "Price",
    "Bennett", "Wood", "Barnes", "Ross", "Henderson", "Coleman", "Jenkins", "Perry", "Powell",
    "Long", "Patterson", "Hughes", "Flores", "Washington", "Butler", "Simmons", "Foster",
    "Gonzales", "Bryant", "Russell", "Griffin", "Diaz", "Hayes", "Myers", "Ford", "Hamilton",
    "Graham", "Sullivan", "Wallace", "Woods", "Cole", "West", "Jordan", "Owens", "Reynolds",
    "Fisher", "Ellis", "Gibson", "Murray", "Marshall", "Mcdonald", "Ruiz", "Dunn", "Shah",
    "Friedrich", "Schmidt", "Weber", "Wagner", "Becker", "Hoffmann", "Schulz", "Koch", "Bauer",
    "Richter", "Klein", "Wolf", "Schröder", "Neumann", "Braun", "Krüger", "Hofmann", "Lange",
    "Werner", "Krause", "Lehmann", "Köhler", "Herrmann", "König", "Walter", "Huber", "Kaiser",
    "Fuchs", "Peters", "Lang", "Scholz", "Weiß", "Jung", "Hahn", "Vogel", "Friedrich", "Keller",
    "Günther", "Frank", "Berger", "Winkler", "Roth", "Beck", "Lorenz", "Baumann", "Franke",
    "Albrecht", "Ebert", "Simon", "Kraus", "Brauer", "Lehner", "Egger", "Hofer", "Steiner",
    "Moser", "Mayer", "Huber", "Wagner", "Reuter", "Bucher", "Sutter", "Ziegler", "Kern",
    "Wimmer", "Brunner", "Schwarzer", "Bachmann", "Brandt", "Gerber", "Enderle", "Reinhart",
    "Freud", "Breuer", "Buber", "Scholem", "Sholem", "Copsey", "Poliakoff", "Segre", "Nolte",
    "Foner", "Tarchaneiotes", "Peletier", "Bourne", "Hobsbawm", "Gellner", "Habermas",
    "Adorno", "Benjamin", "Bloch", "Marcuse", "Fromm", "Reich", "Erikson", "Eriksson",
    "Heidegger", "Husserl", "Sartre", "Camus", "Beauvoir", "Foucault", "Derrida", "Lacan",
    "Levi", "Strauss", "Levi", "Freud", "Jung2",
}
for p in ("Jung2", "Levi"):
    SURNAMES.discard(p)
SURNAMES.add("Strauss")

# Multi-word proper-name splitter fragments (plan §1.5 artefact subclass (a)).
SPLITTERS = {"Los", "Angeles", "San", "Francisco", "York", "New"}

ACRONYM_RE = re.compile(r"^[A-Z0-9]+$")


def main():
    rows = []
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        for idx, line in enumerate(f, 1):
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 3 and parts[2] == "PROPER_NOUN":
                rows.append((idx, parts[0], int(parts[1])))

    print(f"PROPER_NOUN-only entries: {len(rows)}")

    counts = {}
    classes = []
    for idx, word, freq in rows:
        if word in MONTHS:
            cls = "KEEP-months"
        elif ACRONYM_RE.match(word):
            cls = "KEEP-acronym"
        elif word in SPLITTERS:
            cls = "REMOVE-splitter"
        elif word in DEMONYMS:
            cls = "KEEP-demonym"
        elif word in COUNTRIES_REGIONS or word in CITIES:
            cls = "KEEP-geography"
        elif word in FIRST_NAMES or word in SURNAMES:
            cls = "REMOVE-person"
        elif freq <= 200:
            cls = "REVIEW-tail<=200"
        else:
            cls = "REVIEW-tail>200"
        classes.append((idx, word, freq, cls))
        counts[cls] = counts.get(cls, 0) + 1

    for k in sorted(counts):
        print(f"  {k}: {counts[k]}")

    with io.open(OUT, "w", encoding="utf-8", newline="\n") as out:
        out.write("line#\tword\tfreq\tclass\n")
        for idx, word, freq, cls in sorted(classes, key=lambda c: (c[3], -c[2])):
            out.write(f"{idx}\t{word}\t{freq}\t{cls}\n")
    print(f"written -> {OUT}")


if __name__ == "__main__":
    main()
