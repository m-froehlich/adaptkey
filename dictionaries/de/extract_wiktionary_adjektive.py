# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# deutschen Wiktionary und extrahiert fuer jedes deutsche Adjektiv die komplette, dort belegte
# Flexionstabelle (Grad x Fall x Numerus x Deklinationstyp x Genus) in eine Langform-TSV-Datei. Analog zu
# extract_wiktionary_nouns.py/extract_wiktionary_verbs.py, gleiche Quelle/Lizenz - siehe dort fuer die
# vollstaendige Quellenangabe.
#
# Anders als bei Nomen/Verben liefert Wiktionary hier, wo eine Flexionstabelle ueberhaupt vorhanden ist
# ("source" enthaelt "Flexion:<lemma>"), bereits JEDE einzelne deklinierte Form direkt - kein eigenes
# Regel-Modul noetig, um sie zu erzeugen (siehe AdaptKey-Plan-Adjektive.md). Praedikative Mehrwort-Formen
# ("er ist schoen") werden verworfen - keine Woerterbuch-Token, sondern Phrasen; erkennbar am Leerzeichen
# im Formwert. Adjektive OHNE volle Flexionstabelle, aber mit den drei blossen Grad-Staemmen (Positiv/
# Komparativ/Superlativ als je ein Wort), werden separat in wiktionary_adjektiv_stammformen.tsv notiert -
# Grundlage fuer den Regel-Fallback (adjektiv_deklination.py) bei Woertern ohne volle Tabelle.

import gzip
import json
import sys

GRAD_TAGS = {"positive", "comparative", "superlative"}
FALL_TAGS = {"nominative", "genitive", "dative", "accusative"}
DEKL_TAGS = {"strong", "weak", "mixed"}
GENUS_TAGS = {"masculine": "m", "feminine": "f", "neuter": "n"}


def grad_of(tags):
    return next((t for t in GRAD_TAGS if t in tags), None)


def fall_of(tags):
    return next((t for t in FALL_TAGS if t in tags), None)


def dekl_of(tags):
    return next((t for t in DEKL_TAGS if t in tags), None)


def genus_of(tags):
    for tag, code in GENUS_TAGS.items():
        if tag in tags:
            return code
    return ""


def numerus_of(tags):
    if "plural" in tags:
        return "pl"
    if "singular" in tags:
        return "sg"
    return None


def extract_full_flexion_rows(lemma, entry):
    """Jede einzelne deklinierte Form aus einer vorhandenen Flexion-Tabelle als eigene Zeile
    (grad, fall, numerus, deklinationstyp, genus, form) - dedupliziert (dieselbe Kombination kann in den
    Rohdaten mehrfach mit unterschiedlichem "article"-Wert auftauchen, der hier nicht gebraucht wird, siehe
    Plan-Dokument)."""
    rows = set()
    for f in entry.get("forms", []):
        form = f.get("form", "")
        if not form or " " in form:
            continue
        tags = set(f.get("tags", []))
        if f.get("source", "") != f"Flexion:{lemma}":
            continue
        grad = grad_of(tags)
        fall = fall_of(tags)
        dekl = dekl_of(tags)
        numerus = numerus_of(tags)
        if not (grad and fall and dekl and numerus):
            continue
        genus = genus_of(tags) if numerus == "sg" else ""
        rows.add((grad, fall, numerus, dekl, genus, form))
    return rows


def extract_bare_stems(entry):
    """Die drei blossen Grad-Staemme (Positiv/Komparativ/Superlativ, je ein Wort ohne Leerzeichen -
    "am schoensten" wird hier bewusst verworfen, die deklinierte Form "schoensten" liegt bereits ueber
    extract_full_flexion_rows vor, wo eine Tabelle existiert; fehlt die Tabelle, ist der blosse Stamm
    ohnehin nur ein Fallback-Ausgangspunkt fuer die Regel-Generierung, kein fertiges Wort)."""
    stems = {}
    for f in entry.get("forms", []):
        form = f.get("form", "")
        if not form or " " in form:
            continue
        tags = set(f.get("tags", []))
        if len(tags) != 1:
            continue
        grad = grad_of(tags)
        if grad and grad not in stems:
            stems[grad] = form
    return stems


def main(in_path, out_path_full, out_path_stems):
    full_rows = {}
    stem_rows = {}
    total = 0
    kept = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 500000 == 0:
                print(f"  ... {total} Zeilen verarbeitet, {kept} Adjektive erfasst", file=sys.stderr)
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "de" and entry.get("lang") != "German":
                continue
            if entry.get("pos") != "adj":
                continue
            if "form-of" in entry.get("tags", []):
                continue
            word = entry.get("word", "")
            if not word or not word[0].islower():
                continue

            full = extract_full_flexion_rows(word, entry)
            if full:
                if word not in full_rows:
                    full_rows[word] = full
                    kept += 1
                continue

            stems = extract_bare_stems(entry)
            if stems and word not in stem_rows and word not in full_rows:
                stem_rows[word] = stems
                kept += 1

    with open(out_path_full, "wb") as out:
        header = "lemma\tgrad\tfall\tnumerus\tdeklinationstyp\tgenus\tform\n"
        out.write(header.encode("utf-8"))
        for word in sorted(full_rows):
            for grad, fall, numerus, dekl, genus, form in sorted(full_rows[word]):
                line = f"{word}\t{grad}\t{fall}\t{numerus}\t{dekl}\t{genus}\t{form}\n"
                out.write(line.encode("utf-8"))

    with open(out_path_stems, "wb") as out:
        header = "lemma\tpositiv\tkomparativ\tsuperlativ\n"
        out.write(header.encode("utf-8"))
        for word in sorted(stem_rows):
            s = stem_rows[word]
            line = f"{word}\t{s.get('positive', '')}\t{s.get('comparative', '')}\t{s.get('superlative', '')}\n"
            out.write(line.encode("utf-8"))

    print(
        f"Fertig: {total} Zeilen gelesen, {len(full_rows)} Adjektive mit voller Flexionstabelle, "
        f"{len(stem_rows)} weitere mit blossen Grad-Staemmen -> {out_path_full} / {out_path_stems}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "de-extract.jsonl.gz"
    out_path_full = sys.argv[2] if len(sys.argv) > 2 else "wiktionary_adjektive.tsv"
    out_path_stems = sys.argv[3] if len(sys.argv) > 3 else "wiktionary_adjektiv_stammformen.tsv"
    main(in_path, out_path_full, out_path_stems)
