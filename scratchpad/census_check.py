import io
from collections import Counter

def load(path):
    rows = {}
    with io.open(path, encoding="utf-8", newline="") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 2:
                try:
                    rows[parts[0]] = int(parts[1])
                except ValueError:
                    pass
    return rows

en = load("app/src/main/assets/en/dict.tsv")
de = {w.lower(): f for w, f in load("dictionaries/de/dict.tsv").items()}

victims = {w for w, f in en.items() if f <= 20 and de.get(w.lower(), 0) >= 300}
WHITELIST = {
    "gab", "sag", "Spiel", "mutter", "rad", "bier", "krone", "Zar", "Alb", "ORT", "Hanse",
    "Koran", "Casanova", "Praxis", "Grad", "Grads", "Ersatz", "Lieder", "Sten",
    "bares", "barer", "golds", "autos", "hamburgers", "Frankfurters", "Henrys",
    "MFs", "EPOs", "GG", "JH", "SEP", "KAP", "BIP", "HSV", "Apr", "Wm",
    "Zug", "Saarland", "Saale", "Neckar", "Lippe", "Minden", "Fulda", "Duisburg",
    "Koblenz", "Bielefeld", "Schwerin", "Siegen", "Erlangen", "Schweinfurt", "Konstanz",
    "Altenburg", "Tempelhof", "Neustadt", "Taunus",
    "orf", "Gar",
}
print("victims:", len(victims), "whitelist:", len(WHITELIST))
print("actual-only:", sorted(victims - WHITELIST))
print("whitelist-only:", sorted(WHITELIST - victims))
