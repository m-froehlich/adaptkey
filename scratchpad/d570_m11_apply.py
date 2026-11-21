# -*- coding: utf-8 -*-
# D-570 (M11) APPLY: the EN proper-name sweep.
#
# Same classification as scratchpad/d570_m11_review.py (29,180 PROPER_NOUN-only rows), plus the
# per-word review of the >200 band (658 rows reviewed one by one; additions in EXTRA_KEEP below) and
# the keep-list fragment cleanup (Sri/Rican/South/Aviv/Ababa/Cape/Town/Hong/Kong/Kuala/Lumpur/Tel/La/
# Rio/Janeiro/Ivory/Sierra/Guinea/Baja/Yankee/Dixie are removed from the keep lists - fragments or
# common-noun collisions whose capital row would force-cap wrongly).
#
# Removal (plan class 1, decided "fully out, including the tops"):
#   - person names: the curated lists (John 10,247, Smith 2,068, ... 372 matched) AND by default
#     every >200 row not explicitly kept AND the whole <=200 biography/place tail (25,966; the
#     plan's §1.5 random sample verified the band is near-fully Wikipedia biographies/places)
#   - multi-word proper-name splitters: Los, Angeles, San, Francisco, York (plan §1.5 artefact (a))
#   - mojibake rows everywhere (they never match a keep class; >200 mojibake persons fall to the
#     default removal)
#   - capital-only rows of words with a genuine lowercase English reading but no lowercase row
#     (United/Gulf/Bureau/Testament/Marvel/Trinity/Enigma/Commons/Isle/Isles/Milky/...): the
#     force-cap they produce ("we are united" -> "United", "a gulf" -> "Gulf") is exactly the
#     §6 rule-4 wrongness this plan's M7 names for NOUN-only rows - and "United" at 18,662 is the
#     largest such row in the dictionary.
# Keep (plan class 2): months, all-caps acronyms, curated demonyms/language/nationality/religious
#   adjectives (incl. Christianity/Islam/Judaism/Hinduism - always capital in English), curated
#   countries/regions/major cities, and the reviewed >200 band keeps (US states, rivers, mythology -
#   always capital in English usage, universities, brands/tech terms, landmarks, the city-name
#   collisions Madison/Charlotte/Victoria/Austin/Jackson/Hamilton whose capitalisation is correct in
#   BOTH readings, Ada (the language), Senate/Commonwealth (US/UK style capitalises), Quran/Talmud).
#
# Class 3 (the Mueller/Schuster lesson): occupation twins get lowercase rows - the capital surname
# rows fly and the trade word enters with the surname's own corpus frequency (same surface family):
#   smith 2068, cook 880, miller 650, cooper 618, wright 576, baker 487, potter 346, butler 291,
#   mason 246, chandler 186, carpenter 167, shepherd 158, porter 131, weaver 116, fletcher 113,
#   tanner 44, warden 42.  (hunter 578 / brewer 76 / tailor 55 already exist lowercase.)
#
# Guards: every removed line asserted present exactly once; no survivor lemma-links into the removal
# set; quality gate re-run; row order otherwise untouched (new rows appended, §322 convention).

import io
import re

EN = "app/src/main/assets/en/dict.tsv"

MONTHS = {
    "January", "February", "March", "April", "May", "June", "July", "August",
    "September", "October", "November", "December",
}
DEMONYMS = {
    "American", "British", "English", "French", "German", "Greek", "Roman", "Soviet", "Jewish",
    "Latin", "Chinese", "Spanish", "Italian", "Dutch", "Portuguese", "Brazilian", "Mexican",
    "Canadian", "Australian", "Indian", "Japanese", "Russian", "Polish", "Turkish", "Swedish",
    "Norwegian", "Danish", "Finnish", "Icelandic", "Irish", "Scottish", "Welsh", "European",
    "African", "Asian", "Arab", "Arabic", "Islamic", "Muslim", "Christian", "Catholic",
    "Protestant", "Anglican", "Orthodox", "Buddhist", "Hindu", "Sikh", "Celtic", "Nordic", "Slavic",
    "Germanic", "Norman", "Saxon", "Viking", "Aztec", "Maya", "Inca", "Aboriginal", "Indigenous",
    "Elizabethan", "Victorian", "Edwardian", "Georgian", "Renaissance", "Gothic", "Baroque",
    "Byzantine", "Ottoman", "Persian", "Armenian", "Korean", "Vietnamese", "Thai", "Indonesian",
    "Malaysian", "Filipino", "Pakistani", "Bangladeshi", "Israeli", "Palestinian", "Egyptian",
    "Moroccan", "Algerian", "Tunisian", "Libyan", "Sudanese", "Ethiopian", "Kenyan", "Nigerian",
    "Ghanaian", "Zimbabwean", "Zambian", "Ugandan", "Tanzanian", "Somali", "Cuban", "Jamaican",
    "Haitian", "Dominican", "Chilean", "Argentine", "Argentinian", "Colombian", "Peruvian",
    "Venezuelan", "Bolivian", "Ecuadorian", "Paraguayan", "Uruguayan", "Albanian", "Croatian",
    "Serbian", "Bosnian", "Slovenian", "Slovak", "Czech", "Hungarian", "Bulgarian", "Romanian",
    "Moldovan", "Ukrainian", "Belarusian", "Lithuanian", "Latvian", "Estonian", "Kazakh",
    "Mongolian", "Nepalese", "Nepali", "Burmese", "Cambodian", "Laotian", "Slovene", "Macedonian",
    "Montenegrin", "Cypriot", "Maltese", "Belgian", "Swiss", "Austrian", "Azerbaijani",
    "Luxembourger",
}
COUNTRIES_REGIONS = {
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
    "Angola", "Mozambique", "Madagascar", "Mauritania", "Benin", "Togo", "Liberia", "Antarctica",
    "Arctic", "Baltic", "Adriatic", "Iberia", "Balkans", "Scandinavia", "Benelux", "Riviera",
    "Tuscany", "Lombardy", "Sicily", "Sardinia", "Corsica", "Crete", "Cyprus", "Malta",
    "Caribbean", "Amazon", "Andes", "Patagonia", "Yucatan", "Appalachia", "Midwest", "Dixie",
    "Hampshire", "Jersey", "Cornwall", "Kent", "Sussex", "Essex", "Yorkshire", "Lancashire",
    "Cumbria", "Devon", "Dorset", "Somerset", "Norfolk", "Suffolk", "Wessex", "Mercia",
    "Northumbria", "Albion", "Caledonia", "Hibernia", "Britannia", "Gaul", "Lorraine", "Brittany",
    "Normandy", "Burgundy", "Provence", "Alsace", "Picardy", "Champagne", "Serbia", "Croatia",
    "Slovenia", "Slovakia", "Bulgaria", "Romania", "Moldova", "Albania", "Macedonia", "Bosnia",
    "Herzegovina", "Montenegro", "Kosovo",
}
CITIES = {
    "London", "Paris", "Berlin", "Vienna", "Rome", "Madrid", "Lisbon", "Dublin", "Edinburgh",
    "Glasgow", "Cardiff", "Amsterdam", "Brussels", "Copenhagen", "Stockholm", "Oslo", "Helsinki",
    "Reykjavik", "Warsaw", "Prague", "Budapest", "Athens", "Moscow", "Kyiv", "Kiev", "Minsk",
    "Riga", "Vilnius", "Tallinn", "Istanbul", "Ankara", "Jerusalem", "Cairo", "Alexandria",
    "Casablanca", "Algiers", "Tunis", "Tripoli", "Lagos", "Accra", "Nairobi", "Johannesburg",
    "Durban", "Delhi", "Bombay", "Mumbai", "Calcutta", "Kolkata", "Madras", "Bangalore", "Karachi",
    "Lahore", "Dhaka", "Colombo", "Beijing", "Peking", "Shanghai", "Canton", "Macau", "Tokyo",
    "Yokohama", "Osaka", "Kyoto", "Nagoya", "Sapporo", "Seoul", "Pusan", "Hanoi", "Saigon",
    "Bangkok", "Manila", "Jakarta", "Singapore", "Sydney", "Melbourne", "Brisbane", "Perth",
    "Adelaide", "Auckland", "Wellington", "Chicago", "Boston", "Philadelphia", "Houston", "Dallas",
    "Phoenix", "Denver", "Seattle", "Portland", "Miami", "Atlanta", "Detroit", "Cleveland",
    "Pittsburgh", "Baltimore", "Vancouver", "Montreal", "Toronto", "Ottawa", "Quebec", "Havana",
    "Kingston", "Santiago", "Lima", "Quito", "Montevideo", "Asuncion", "Bogota", "Caracas",
    "Brasilia", "Salvador", "Bristol", "Barcelona",
}
SPLITTERS = {"Los", "Angeles", "San", "Francisco", "York", "New"}

EXTRA_KEEP = {
    # US states / regions / provinces
    "California", "Texas", "Colorado", "Louisiana", "Florida", "Carolina", "Indiana", "Missouri",
    "Arizona", "Arkansas", "Oregon", "Maryland", "Wisconsin", "Minnesota", "Nevada", "Oklahoma",
    "Maine", "Massachusetts", "Pennsylvania", "Connecticut", "Mississippi", "Idaho", "Montana",
    "Nebraska", "Iowa", "Utah", "Wyoming", "Vermont", "Kansas", "Virginia", "Georgia",
    "Washington", "Ontario", "Punjab", "Karnataka", "Queensland",
    # countries / territories / islands
    "Afghanistan", "Hungary", "Guatemala", "Honduras", "Taiwan", "Nepal", "Belize", "Nicaragua",
    "Fiji", "Kyrgyzstan", "Djibouti", "Andorra", "Kiribati", "Lesotho", "Brunei", "Liechtenstein",
    "Luxembourg", "Borneo", "Bali", "Bermuda", "Guam", "Cayman", "Falkland", "Americas", "Indies",
    "Zealand", "Columbia", "Arabia",
    # historical regions / rivers / seas
    "Andalusia", "Catalonia", "Castile", "Aragon", "Flanders", "Baden", "Weimar", "Galilee",
    "Antarctic", "Balkan", "Caucasus", "Nile", "Rhine", "Danube", "Thames", "Euphrates", "Ganges",
    "Indus",
    # ancient/historical places
    "Constantinople", "Antioch", "Ephesus", "Delphi", "Babylon", "Assyria", "Mesopotamia",
    "Carthage",
    # cities
    "Kabul", "Kandahar", "Herat", "Beirut", "Damascus", "Baghdad", "Gaza", "Ithaca", "Richmond",
    "Lyon", "Bonn", "Leipzig", "Frankfurt", "Cologne", "Munich", "Nuremberg", "Hanover", "Aachen",
    "Belfast", "Naples", "Venice", "Milan", "Barcelona", "Manchester", "Birmingham", "Ballarat",
    "Dayton", "Erie", "Milwaukee", "Indianapolis", "Honolulu", "Aarhus", "Basel", "Seville",
    "Cannes", "Baku",
    # universities / institutions / landmarks
    "Harvard", "Yale", "Princeton", "Stanford", "Caltech", "Dartmouth", "Broadway", "Louvre",
    "Vatican", "Versailles", "Westminster", "Canterbury", "Greenwich", "Eiffel", "Gettysburg",
    "Auschwitz",
    # brands / tech terms
    "Disney", "Lego", "Intel", "Linux", "Debian", "Unicode", "Fortran", "Lisp", "Ada", "Hz",
    # style-capitalised institutions / books
    "Senate", "Commonwealth", "Quran", "Talmud",
    # religions / religious adjectives (always capital in English)
    "Christianity", "Islam", "Judaism", "Hinduism", "Mormon", "Vedic", "Yahweh",
    # languages / ethnographic / historical adjectives / geologic periods
    "Esperanto", "Sanskrit", "Hindi", "Gaelic", "Semitic", "Turkic", "Norse", "Frankish",
    "Cornish", "Flemish", "Coptic", "Kurdish", "Hellenistic", "Napoleonic", "Euclidean",
    "Jurassic", "Cretaceous", "Pleistocene",
    # mythology / planets (always capital in English usage)
    "Zeus", "Hera", "Artemis", "Leto", "Ares", "Heracles", "Demeter", "Hades", "Circe", "Enki",
    "Loki", "Odin", "Freyja", "Cerberus", "Gilgamesh", "Lilith", "Ajax", "Achilles", "Hector",
    "Jupiter", "Saturn", "Europa",
    # city-name collisions: capitalisation is correct in BOTH readings (city and person name)
    "Madison", "Charlotte", "Victoria", "Austin", "Jackson", "Hamilton", "Berkeley", "Concord",
}

# Force-cap-only rows whose genuine English reading is lowercase (no lowercase row exists): the
# capital row would force-cap wrongly ("we are united" -> "United", "a gulf" -> "Gulf").
REMOVE_FORCECAP_ONLY = {
    "United", "Gulf", "Bureau", "Testament", "Marvel", "Trinity", "Enigma", "Commons", "Isle",
    "Isles", "Milky", "Jr", "Sr", "Inc", "Ltd", "Bros", "Avenue", "Prix", "Deco", "Greco",
    "Austro", "Cola", "Sox", "Reds", "Cowboys", "Braves", "Juventus", "Serie", "Airways",
    "Billboard", "Warner", "Commodore", "Amiga", "Compaq", "Abu", "Din", "Abd", "Qaeda", "Hamas",
    "Hezbollah", "Irgun", "Klan", "Gestapo", "Luftwaffe", "Bundestag", "Dalai", "Lama", "Pasha",
    "Han", "Qing", "Edo", "Fujiwara", "Guanyin", "Ananda", "Luthor", "Poirot", "Galactus",
    "Gotham", "Dracula", "Historia", "Liga", "Filmworks", "Centauri", "Petersburg", "Gini",
    "Rica", "Costa", "Faso", "Burkina", "Sri", "Aviv", "Ababa", "Moines", "Rouge", "Santo",
    "Arbor", "DeMille", "Munch", "Ea",
}

OCCUPATION_TWINS = {
    # word -> the capital surname row whose corpus frequency the lowercase twin inherits. Only
    # rows that force-cap are here: PROPER_NOUN-only ones (Smith/Miller/Cooper/Wright/Baker/
    # Fletcher - already inside the sweep's removal set) and NOUN,PROPER_NOUN ones (Potter/Mason/
    # Chandler - removed explicitly as case replacements). The OTHER-tagged trade rows (Cook,
    # Butler, Carpenter, Shepherd, Porter, Weaver, Tanner, Warden) force-cap nothing and stay.
    "smith": "Smith", "miller": "Miller", "cooper": "Cooper", "wright": "Wright",
    "baker": "Baker", "potter": "Potter", "mason": "Mason", "chandler": "Chandler",
    "fletcher": "Fletcher",
}

ACRONYM_RE = re.compile(r"^[A-Z0-9]+$")


def main():
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    keep_words = MONTHS | DEMONYMS | COUNTRIES_REGIONS | CITIES
    freq_of = {}
    for line in lines:
        parts = line.rstrip("\n").split("\t")
        freq_of[parts[0]] = int(parts[1])

    remove = set()
    kept = 0
    removed_tail_le = 0
    removed_tail_gt = 0
    for line in lines:
        parts = line.rstrip("\n").split("\t")
        word = parts[0]
        if len(parts) < 3 or parts[2] != "PROPER_NOUN":
            continue
        freq = freq_of[word]
        if word in MONTHS:
            kept += 1
            continue
        if ACRONYM_RE.match(word):
            kept += 1
            continue
        if word in SPLITTERS or word in REMOVE_FORCECAP_ONLY:
            remove.add(word)
            continue
        if word in DEMONYMS or word in COUNTRIES_REGIONS or word in CITIES:
            kept += 1
            continue
        if word in EXTRA_KEEP:
            kept += 1
            continue
        remove.add(word)
        if freq <= 200:
            removed_tail_le += 1
        else:
            removed_tail_gt += 1

    # Occupation twins: the NOUN,PROPER_NOUN capitals (Potter/Mason/Chandler) join the removal as
    # explicit case replacements (the PROPER_NOUN-only ones are already inside the set) - added
    # BEFORE the dangling check so real trade-word dependents can be retargeted first.
    for cap in ("Potter", "Mason", "Chandler"):
        remove.add(cap)

    # Real trade-word plurals lemma-linking into the removal set ("Masons", "Chandlers"): convert
    # to their lowercase surfaces and retarget the lemma to the incoming lowercase twin, instead of
    # losing a genuine English word. Any OTHER dependent is removed transitively below.
    retarget = {}
    out_lines = []
    for line in lines:
        parts = line.rstrip("\n").split("\t")
        word = parts[0]
        if word in ("Masons", "Chandlers") and len(parts) > 3 and parts[3] in ("Mason", "Chandler"):
            low = word.lower()
            parts[0] = low
            parts[3] = low[:-1]
            retarget[word] = low
            out_lines.append("\t".join(parts) + "\n")
        else:
            out_lines.append(line)
    lines = out_lines
    if retarget:
        print(f"retargeted trade plurals: {retarget}")

    # No survivor may lemma-link into the removal set (transitive, printed for the record).
    while True:
        dangling = []
        for line in lines:
            parts = line.rstrip("\n").split("\t")
            word = parts[0]
            if word in remove:
                continue
            if len(parts) > 3 and parts[3] and parts[3] in remove:
                dangling.append((word, parts[3]))
        if not dangling:
            break
        for word, lemma in dangling:
            print(f"dependent removed (lemma-links to removed {lemma!r}): {word}")
            remove.add(word)

    # Occupation twins: capture the capital rows' frequencies (they are inside the removal set),
    # then emit the lowercase rows appended at the end.
    new_rows = []
    for low, cap in OCCUPATION_TWINS.items():
        assert cap in remove, f"expected {cap!r} in removal set"
        freq = freq_of[cap]
        new_rows.append(f"{low}\t{freq}\tNOUN,OTHER\n")
    existing_lower = {
        l.split("\t", 1)[0].lower()
        for l in lines
        if l.split("\t", 1)[0] not in remove
    }
    new_rows = [r for r in new_rows if r.split("\t", 1)[0] not in existing_lower]

    out = []
    for line in lines:
        word = line.split("\t", 1)[0]
        if word not in remove:
            out.append(line)
    out.extend(new_rows)

    with io.open(EN, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)

    print(f"removed {len(remove)} rows (tail<=200: {removed_tail_le}, tail>200: {removed_tail_gt}, "
          f"rest: {len(remove) - removed_tail_le - removed_tail_gt}); kept {kept} PROPER_NOUN-only; "
          f"{len(lines)} -> {len(out)} rows; +{len(new_rows)} occupation twins")


if __name__ == "__main__":
    main()
