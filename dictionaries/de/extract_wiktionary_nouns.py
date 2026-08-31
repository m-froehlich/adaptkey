# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# deutschen Wiktionary und extrahiert fuer jedes deutsche Substantiv Genus, Genitiv Singular und
# Nominativ Plural (soweit in den Wiktionary-Flexionstabellen vorhanden) in eine kompakte TSV-Datei.
# Ersetzt fuer die abgedeckten Woerter die regelbasierte Generierung (genus.py/deklination.py/plural.py)
# durch echte belegte Formen - Nutzerentscheidung 2026-08-31.
#
# Quelle: https://kaikki.org/dictionary/downloads/de/de-extract.jsonl.gz (wiktextract-Tool: MIT-lizenziert;
# zugrundeliegende Wiktionary-INHALTE: CC BY-SA 4.0 / GFDL, wie auch dict.tsv/bigram.tsv dieses Projekts
# aus einem Wikipedia-Dump stammen - gleiche Lizenzfamilie, Attribution/Share-Alike bei Weitergabe noetig).

import gzip
import json
import sys

GENUS_TAGS = {"masculine": "m", "feminine": "f", "neuter": "n"}


def extract_genus(entry):
    # Das Genus steht beim tatsaechlichen Untersuchen der Rohdaten auf der OBERSTEN Ebene des Eintrags im
    # "tags"-Feld (z.B. entry["tags"] == ["neuter"]), NICHT in den einzelnen "senses" oder in
    # "head_templates" (letzteres existiert bei dieser Sprachausgabe gar nicht) - urspruengliche Annahme
    # war falsch, korrigiert nach Inspektion eines echten Eintrags ("Haus").
    found = set()
    for tag in entry.get("tags", []):
        if tag in GENUS_TAGS:
            found.add(GENUS_TAGS[tag])
    if len(found) == 1:
        return next(iter(found))
    return None  # keine oder widerspruechliche Genus-Angabe - lieber auslassen als raten.


def extract_forms(entry):
    genitiv = None
    plural = None
    for f in entry.get("forms", []):
        tags = set(f.get("tags", []))
        form = f.get("form", "")
        if not form or form in ("-", "—"):
            continue
        if "genitive" in tags and "plural" not in tags and genitiv is None:
            genitiv = form
        if "plural" in tags and "genitive" not in tags and "dative" not in tags and \
           "accusative" not in tags and plural is None:
            plural = form
    return genitiv, plural


def main(in_path, out_path):
    seen = {}
    total = 0
    kept = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 500000 == 0:
                print(f"  ... {total} Zeilen verarbeitet, {kept} Substantive erfasst", file=sys.stderr)
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "de" and entry.get("lang") != "German":
                continue
            if entry.get("pos") != "noun":
                continue
            # "form-of"-Eintraege sind bereits deklinierte Formen, die selbst wieder als "word" auftauchen
            # (z.B. "Haus" kommt sowohl als eigenstaendiges Lemma UND als Eintrag fuer eine Dativ-Variante
            # vor) - diese haben kein/kaum Genus/Formen und wuerden bei "erster Treffer gewinnt" faelschlich
            # das eigentliche Lemma blockieren, falls sie zuerst in der Datei erscheinen.
            if "form-of" in entry.get("tags", []):
                continue
            word = entry.get("word", "")
            if not word or not word[0].isupper():
                continue
            genus = extract_genus(entry)
            genitiv, plural = extract_forms(entry)
            if genus is None and genitiv is None and plural is None:
                continue
            # Bei mehreren Eintraegen fuer dasselbe Wort (Homographe/mehrere Bedeutungen): ersten
            # vollstaendigen Treffer behalten, nicht ueberschreiben - Konflikte werden nicht aufgeloest,
            # sondern das erste brauchbare Ergebnis gewinnt (konservativ).
            if word not in seen:
                seen[word] = (genus or "", genitiv or "", plural or "")
                kept += 1

    with open(out_path, "wb") as out:
        header = "wort\tgenus\tgenitiv_singular\tplural\n"
        out.write(header.encode("utf-8"))
        for word, (genus, genitiv, plural) in sorted(seen.items()):
            line = f"{word}\t{genus}\t{genitiv}\t{plural}\n"
            out.write(line.encode("utf-8"))

    print(f"Fertig: {total} Zeilen gelesen, {kept} Substantive extrahiert -> {out_path}", file=sys.stderr)


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "de-extract.jsonl.gz"
    out_path = sys.argv[2] if len(sys.argv) > 2 else "wiktionary_nomen.tsv"
    main(in_path, out_path)
