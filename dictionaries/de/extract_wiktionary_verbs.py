# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# deutschen Wiktionary und extrahiert fuer jedes deutsche Verb Praesens (6 Formen), Praeteritum
# (6 Formen), Partizip II und Imperativ (Singular+Plural) - soweit in den Wiktionary-Flexionstabellen
# vorhanden - in eine kompakte TSV-Datei. Analog zu extract_wiktionary_nouns.py, gleiche Quelle/Lizenz.
#
# Die Flexionstabelle liefert zwei Arten von Eintraegen im "forms"-Array:
# 1) einfache Einwort-Formen ohne eingebettetes Pronomen (z.B. "gehst", tags=["present"],
#    pronouns=["du"]) - nur fuer einen Teil der Personen vorhanden.
# 2) vollstaendige "Pronomen + Form"-Saetze mit granularen Tags (z.B. "du gingst", tags=["second-person",
#    "singular","past","active","indicative"]) - deckt alle 6 Personen ab, Pronomen muss abgetrennt werden.
# Nur "indicative"+"active" (keine Konjunktiv-/Passiv-Formen) werden verwendet - passt zum bereits
# festgelegten Formen-Katalog des Projekts (nur Praesens/Praeteritum/Partizip II/Imperativ).

import gzip
import json
import sys

PERSON_NUMBER_TO_SLOT = {
    ("first-person", "singular"): 0,
    ("second-person", "singular"): 1,
    ("third-person", "singular"): 2,
    ("first-person", "plural"): 3,
    ("second-person", "plural"): 4,
    ("third-person", "plural"): 5,
}

PRONOUNS = {"ich", "du", "er/sie/es", "wir", "ihr", "sie"}


def strip_pronoun(form):
    parts = form.split(" ", 1)
    if len(parts) == 2 and parts[0].lower() in PRONOUNS:
        return parts[1]
    return form


def extract_verb_forms(entry):
    praesens = [None] * 6
    praeteritum = [None] * 6
    partizip2 = None
    imperativ_sg = None
    imperativ_pl = None

    for f in entry.get("forms", []):
        tags = set(f.get("tags", []))
        form = f.get("form", "")
        if not form:
            continue

        if partizip2 is None and "participle-2" in tags and "perfect" in tags:
            partizip2 = form

        if "imperative" in tags:
            if "singular" in tags and imperativ_sg is None:
                imperativ_sg = form.rstrip("!")
            elif "plural" in tags and imperativ_pl is None:
                imperativ_pl = form.rstrip("!")

        if "indicative" in tags and "active" in tags:
            person = next((p for p in ("first-person", "second-person", "third-person") if p in tags), None)
            number = "singular" if "singular" in tags else ("plural" if "plural" in tags else None)
            if person and number:
                slot = PERSON_NUMBER_TO_SLOT[(person, number)]
                stripped = strip_pronoun(form)
                if "present" in tags and praesens[slot] is None:
                    praesens[slot] = stripped
                elif "past" in tags and praeteritum[slot] is None:
                    praeteritum[slot] = stripped

    return praesens, praeteritum, partizip2, imperativ_sg, imperativ_pl


def main(in_path, out_path):
    seen = {}
    total = 0
    kept = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 500000 == 0:
                print(f"  ... {total} Zeilen verarbeitet, {kept} Verben erfasst", file=sys.stderr)
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "de" and entry.get("lang") != "German":
                continue
            if entry.get("pos") != "verb":
                continue
            if "form-of" in entry.get("tags", []):
                continue
            word = entry.get("word", "")
            if not word or not word[0].islower():
                continue
            praesens, praeteritum, partizip2, imp_sg, imp_pl = extract_verb_forms(entry)
            if not any(praesens) and not any(praeteritum) and not partizip2:
                continue
            if word not in seen:
                seen[word] = (praesens, praeteritum, partizip2 or "", imp_sg or "", imp_pl or "")
                kept += 1

    with open(out_path, "wb") as out:
        header = "infinitiv\tpraes1\tpraes2\tpraes3\tpraes4\tpraes5\tpraes6\t" \
                 "praet1\tpraet2\tpraet3\tpraet4\tpraet5\tpraet6\tpartizip2\timp_sg\timp_pl\n"
        out.write(header.encode("utf-8"))
        for word, (praesens, praeteritum, partizip2, imp_sg, imp_pl) in sorted(seen.items()):
            cols = [word] + [p or "" for p in praesens] + [p or "" for p in praeteritum] + \
                   [partizip2, imp_sg, imp_pl]
            out.write(("\t".join(cols) + "\n").encode("utf-8"))

    print(f"Fertig: {total} Zeilen gelesen, {kept} Verben extrahiert -> {out_path}", file=sys.stderr)


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "de-extract.jsonl.gz"
    out_path = sys.argv[2] if len(sys.argv) > 2 else "wiktionary_verben.tsv"
    main(in_path, out_path)
