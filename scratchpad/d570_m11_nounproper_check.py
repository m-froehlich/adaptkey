# -*- coding: utf-8 -*-
# D-570 review helper: the NOUN,PROPER_NOUN rows that match the curated person-name lists, by
# frequency - the class the plan's M11 names for the top persons ("John 10,247", §4-7 "fully out,
# including the tops") but where the Wiktionary merge also minted marginal generic readings.
import io

EN = "app/src/main/assets/en/dict.tsv"

FIRST = {
    "John", "David", "George", "Henry", "Thomas", "Louis", "Alexander", "Richard", "Joseph",
    "Edward", "Peter", "James", "Robert", "Michael", "William", "Charles", "Paul", "Mary",
    "Elizabeth", "Margaret", "Christian", "Victor", "Karl", "Otto", "Wilhelm", "Heinrich",
    "August", "Hermann", "Bernhard", "Theodor", "Gustav", "Ferdinand", "Maximilian", "Konrad",
    "Matthias", "Simon", "Leo", "Clemens", "Gregor", "Franz", "Ludwig", "Johann", "Klaus",
    "Wolfgang", "Ernst", "Bruno", "Kurt", "Hans", "Ernest", "Albert", "Alfred", "Eugene",
    "Herman", "Leonard", "Vincent", "Lawrence", "Douglas", "Patrick", "Francis", "Benjamin",
    "Samuel", "Gregory", "Raymond", "Jack", "Dennis", "Gerald", "Harold", "Arthur", "Jesse",
    "Ralph", "Philip", "Frederick", "Constantine", "Julian", "Justinian", "Edmund", "Hugh",
    "Oliver", "Manuel", "Cameron", "Burke", "Warren", "Neil", "Xavier", "Maurice", "Ivan",
    "Vladimir", "Mikhail", "Carlos", "Fernando", "Miguel", "Johannes", "Geoffrey", "Antony",
    "Horace", "Ambrose", "Cyril", "Ezra", "Benedict", "Clement", "Winston", "Leon", "Marc",
    "Cecil", "Lucius", "Alexios", "Lucy", "Esther", "Anne", "Jean", "Denis", "Ian", "Nick",
    "Ron", "Jeff", "Dave", "Chris", "Ken", "Matt", "Jon", "Alex", "Jim", "Joe", "Kim", "Dick",
    "Lois", "Douglass", "Giovanni", "Marcus", "Afonso", "Gaius", "Octavian", "Leopold",
    "Ashoka", "Aurangzeb", "Isaiah", "Jerome", "Gilbert", "Isaac", "Abraham", "Moses", "Judah",
    "Saul", "Petrus", "Christoph", "Wanda", "Vadim", "Gediminas",
}
SURN = {
    "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore",
    "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia",
    "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen",
    "Young", "Hernandez", "King", "Wright", "Lopez", "Hill", "Scott", "Green", "Adams", "Baker",
    "Nelson", "Carter", "Mitchell", "Perez", "Roberts", "Turner", "Phillips", "Campbell",
    "Parker", "Evans", "Edwards", "Collins", "Stewart", "Sanchez", "Morris", "Rogers", "Reed",
    "Cook", "Morgan", "Bell", "Murphy", "Bailey", "Rivera", "Cooper", "Richardson", "Cox",
    "Howard", "Ward", "Torres", "Peterson", "Gray", "Ramirez", "Watson", "Brooks", "Kelly",
    "Sanders", "Price", "Bennett", "Wood", "Barnes", "Ross", "Henderson", "Coleman", "Jenkins",
    "Perry", "Powell", "Long", "Patterson", "Hughes", "Flores", "Washington", "Butler", "Simmons",
    "Foster", "Gonzales", "Bryant", "Russell", "Griffin", "Diaz", "Hayes", "Myers", "Ford",
    "Hamilton", "Graham", "Sullivan", "Wallace", "Woods", "Cole", "West", "Jordan", "Owens",
    "Reynolds", "Fisher", "Ellis", "Gibson", "Murray", "Marshall", "Mcdonald", "Ruiz", "Dunn",
    "Shah", "Friedrich", "Schmidt", "Weber", "Wagner", "Becker", "Hoffmann", "Schulz", "Koch",
    "Bauer", "Richter", "Klein", "Wolf", "Neumann", "Braun", "Kruger", "Lange", "Werner",
    "Krause", "Lehmann", "Herrmann", "Konig", "Walter", "Huber", "Kaiser", "Fuchs", "Peters",
    "Lang", "Scholz", "Jung", "Hahn", "Vogel", "Keller", "Gunter", "Frank", "Berger", "Winkler",
    "Roth", "Beck", "Lorenz", "Baumann", "Franke", "Albrecht", "Ebert", "Simon", "Kraus",
    "Steiner", "Moser", "Mayer", "Reuter", "Bucher", "Sutter", "Ziegler", "Kern", "Wimmer",
    "Brunner", "Bachmann", "Brandt", "Gerber", "Reinhart", "Freud", "Breuer", "Buber", "Copsey",
    "Poliakoff", "Segre", "Nolte", "Foner", "Tarchaneiotes", "Peletier", "Bourne", "Hobsbawm",
    "Gellner", "Habermas", "Adorno", "Benjamin", "Bloch", "Marcuse", "Fromm", "Reich", "Erikson",
    "Eriksson", "Heidegger", "Husserl", "Sartre", "Camus", "Beauvoir", "Foucault", "Derrida",
    "Lacan", "Strauss", "Kennedy", "Roosevelt", "Reagan", "Nixon", "Clinton", "Obama", "Churchill",
    "Stalin", "Lenin", "Trotsky", "Kissinger", "McCarthy", "Monroe", "Sinatra", "Lennon",
    "Presley", "Clapton", "Hendrix", "Cobain", "Zappa", "Crosby", "Costello", "Cagney", "Lugosi",
    "Cleese", "Chapman", "Gardner", "Lister", "Kafka", "Spinoza", "Hegel", "Marx", "Engels",
    "Hume", "Locke", "Bacon", "Descartes", "Pascal", "Cauchy", "Fourier", "Laplace", "Bernoulli",
    "Lorentz", "Riemann", "Banach", "Hausdorff", "Galois", "Peirce", "Bohr", "Fermi", "Maxwell",
    "Newton", "Turing", "Shannon", "Beethoven", "Mozart", "Bach", "Haydn", "Chopin", "Verdi",
    "Rossini", "Pasteur", "Darwin", "Aristotle", "Plato", "Heraclitus", "Herodotus", "Tacitus",
    "Pliny", "Cicero", "Ptolemy", "Archimedes", "Euclid", "Gauss", "Hilbert", "Euler",
    "Schopenhauer", "Nietzsche", "Tolstoy", "Dostoevsky", "Solzhenitsyn", "Orwell", "Dickens",
    "Chaucer", "Milton", "Lovecraft", "Homer", "Shaw", "Austen", "Kurosawa", "Maradona",
    "Hitchcock", "Hendrix", "Chaplin", "Oswald", "Rousseau", "Crowley", "Carnegie", "DeMille",
    "Polk", "Johnston", "Macdonald", "Montgomery", "Johnston", "Drake", "Burke", "Goldwater",
    "Coolidge", "Quayle", "Hawke", "Whitlam", "Diefenbaker", "Adenauer", "Khrushchev", "Gaddafi",
    "Rumsfeld", "Kissinger", "Asimov", "Tolkien", "Kirby", "Alcott", "Josephus", "Borges",
    "Irving", "Melville", "Whitman", "Thoreau", "Emerson", "Hawthorne", "Vonnegut", "Kerouac",
    "Burroughs", "Landis", "Kubrick", "Coppola", "Scorsese", "Hawks", "Hewitt", "Botham",
    "Kasparov", "Popper", "Hayek", "Friedman", "Boas", "Boas", "Cantel", "Cantor", "Lynch",
    "Dewey", "Carson", "Reeves", "Kemp", "Harvey", "Holt", "Kane", "Hart", "Clarke", "Duncan",
    "Craig", "Spencer", "Parsons", "Hopper", "Greenberg", "Goldman", "Barnard", "Davidson",
    "Curtis", "Berry", "Walsh", "Huxley", "Barks", "Capp", "Landis", "Dahmer", "Ribbentrop",
    "Speer", "Goebbels", "Himmler", "Disraeli", "Massoud", "Abbas", "Amin", "Madero", "Castro",
    "Gaddafi", "Deng", "Din", "Khan", "Singh", "Rahman", "Ahmad", "Ahmed", "Mohammad", "Kasparov",
    "Herg", "Mussolini", "Hitler", "Rommel", "Goebbels", "Wallenstein", "Casanova", "Shah",
}

persons = FIRST | SURN
rows = [l.rstrip("\n").split("\t") for l in io.open(EN, encoding="utf-8")]
cand = [(int(r[1]), r[0]) for r in rows if len(r) > 2 and r[2] == "NOUN,PROPER_NOUN" and r[0] in persons]
cand.sort(reverse=True)
print(len(cand), "NOUN,PROPER_NOUN person-list rows; top 80:")
print("\n".join(f"{f} {w}" for f, w in cand[:80]))
