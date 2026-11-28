# D-582 survey: dictionary facts needed to curate the golden set (read-only probe).
# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding="utf-8")

words = {}
pos_of = {}
with open("dictionaries/de/dict.tsv", encoding="utf-8") as f:
    for line in f:
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 2:
            continue
        w = parts[0]
        try:
            freq = int(parts[1])
        except ValueError:
            continue
        k = w.lower()
        if k not in words or freq > words[k][0]:
            words[k] = (freq, w)
        pos_of.setdefault(k, set()).update((parts[2] if len(parts) > 2 else "").split(","))

def probe(label, keys):
    print(f"--- {label}")
    for k in keys:
        hit = words.get(k.lower())
        print(f"  {k!r}: {hit}")

probe("greeting/closing", ["danke", "dankeschön", "bitte", "hallo", "moin", "hey", "super",
                           "toll", "cool", "gerne", "grüße", "tschüss", "ciao", "okay", "ok",
                           "schönen", "gute", "nacht", "morgen", "bald", "später", "bis",
                           "wiedersehen", "dank", "gruß", "liebe", "viele", "alles", "klar",
                           "macht", "sinn", "stimmt", "genau", "genial", "krass"])
probe("particles/everyday", ["jetzt", "mal", "halt", "wirklich", "vielleicht", "eigentlich",
                             "irgendwie", "gestern", "gleich", "moment", "heute", "nix",
                             "kriegen", "grad", "eh", "wohl", "eben", "doch", "denn", "ruhig"])
probe("common", ["nicht", "kein", "keine", "mehr", "schon", "noch", "auch", "sehr", "gut",
                 "besser", "wissen", "denke", "glaube", "sage", "sagt", "kommen", "gehen",
                 "machen", "hab", "habe", "bin", "bist", "geht", "gibt", "und", "ich", "du",
                 "wir", "das", "die", "der", "ist", "war", "haben", "sein", "dir", "dur",
                 "mir", "mich", "dich", "uns", "euch"])
probe("umlaut/sz targets", ["können", "grün", "groß", "straße", "münchen", "über", "ändern",
                            "für", "früh", "schön", "müde", "hälfte", "wünsche", "wünschen",
                            "fühle", "gesagt", "wär", "wäre"])
probe("compounds", ["bahnhof", "geburtstag", "wochenende", "einkaufszentrum", "arbeitszeit",
                    "schlafzimmer", "fahrrad", "frühstück", "schlüsselbund", "feierabend",
                    "kaffee", "milch", "supermarkt", "hausaufgaben", "mitfahrgelegenheit"])

# bigram continuation counts for planned next-word contexts
big = {}
with open("dictionaries/de/bigram.tsv", encoding="utf-8") as f:
    for line in f:
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 3:
            continue
        try:
            c = int(parts[2])
        except ValueError:
            continue
        prev = parts[0].lower()
        big.setdefault(prev, []).append((c, parts[1]))

print("--- continuations (top 8) for planned contexts")
for prev in ["ich", "du", "wir", "hab", "habe", "bin", "jetzt", "mal", "danke", "bitte",
             "bis", "gute", "liebe", "viele", "alles", "wie", "ganz", "schön", "das",
             "ich habe", "das war", "ich bin", "wir haben"]:
    if " " in prev:
        continue
    rows = sorted(big.get(prev, []), reverse=True)[:8]
    total = sum(c for c, _ in big.get(prev, []))
    print(f"  {prev!r}: rows={len(big.get(prev, []))} total={total} top={[w for _, w in rows]}")
