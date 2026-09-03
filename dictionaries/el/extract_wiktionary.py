# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# griechischen Wiktionary-Ausgabe und extrahiert fuer Substantive, Verben und Adjektive JEDE belegte,
# grammatisch getaggte Flexionsform (Kasus x Numerus fuer Substantive; Genus x Kasus x Numerus fuer
# Adjektive, dazu Komparativ/Superlativ; Person x Numerus x Zeit/Modus/Genus verbi fuer Verben), sowie die
# geschlossene Klasse der Praepositionen - alles in einem Durchlauf.
#
# ANDERS als bei Deutsch/Englisch: keine feste Anzahl benannter Formen pro Wortart (kein "s_form"/"ing_form"
# etc.) - Griechisch hat schlicht zu viele Kombinationen (Substantive bis zu 8, Adjektive bis zu 24+, Verben
# potenziell 30+ je nach Konjugationstabelle), um sie einzeln von Hand zu benennen. Stattdessen generisch:
# jede eigenstaendige, grammatisch getaggte Form wird als eigene Zeile ausgegeben (word\tform, mehrere Zeilen
# pro Lemma) - merge_wiktionary.py gruppiert sie beim Zusammenfuehren wieder pro Lemma. Periphrastische
# Formen (z.B. Futur "θα γράφω", Konjunktiv "να γράφω" - beides mehrwortige Konstruktionen, keine echten
# Woerterbuch-Woerter) werden automatisch ausgeschlossen, da sie ein Leerzeichen enthalten (derselbe Filter,
# der auch bei Englisch/Deutsch mehrwortige Formen ausschliesst) - kein griechischspezifischer Sonderfall
# noetig.
#
# Quelle: https://kaikki.org/dictionary/downloads/el/el-extract.jsonl.gz (wie beim Deutschen ein eigener,
# kleiner, auf Griechisch als Zielsprache vorgefilterter Extract - anders als beim Englischen, wo die Quelle
# selbst schon Englisch ist und kein kleinerer Extract existiert). Filterung auf lang_code=="el".
#
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-INHALTE CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.
#
# Tag-Vokabular VERIFIZIERT gegen echte Eintraege (inspect_sample.py/check_more.py), nicht geraten: Substantive
# und Adjektive verwenden das erwartete standardisierte wiktextract-Vokabular (nominative/genitive/accusative/
# vocative, singular/plural, masculine/feminine/neuter, comparative/superlative) - identisch zum bei Deutsch/
# Englisch bereits gesehenen Vokabular, da es vom wiktextract-Tool selbst ediitionsuebergreifend standardisiert
# wird. "inflection-template"-getaggte Eintraege (ein Meta-Verweis auf die verwendete Flexionstabellen-Vorlage,
# kein echtes Wort, z.B. "el-κλίση-'λύση'") werden ausgeschlossen.

import gzip
import json
import re
import sys

# Siehe dictionaries/en/extract_wiktionary.py's eigener Kommentar - dasselbe, editionsuebergreifend
# standardisierte wiktextract-Vokabular, hier identisch wiederverwendet.
EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative",
}


# D-426 (bugfix, same day as D-424): a real bug found via post-ship spot-check, not caught before the first
# run - the generic "take every grammatically-tagged form" design (necessary given Greek's own morphological
# complexity, see the module docstring) turned out too permissive for a handful of real Wiktionary data
# shapes that are not themselves standalone words: a bare declension-table ENDING documented with a leading
# hyphen ("-ῶνος" - "the genitive plural of this class ends in -ῶνος", not itself a word), a footnote/
# reference-number artifact glued onto a form ("απεδέχθη3ο"), a cross-referenced Latin-script synonym or
# transliteration mistaken for a Greek form ("Urticaria", "Korinthios"), and two real alternate spellings
# joined by a "/" or "\" separator into one unparsed string ("άρκεσε/ήρκεσε" - the augmented and
# unaugmented aorist, both genuinely valid, just never split apart). A single hard requirement now closes
# every one of these at once, applied AFTER splitting on "/"/"\" first (so a genuine two-forms-in-one case
# recovers both real forms instead of losing both): every accepted form must consist purely of Greek-script
# characters (main Greek + Greek Extended Unicode blocks) - nothing else a real Modern Greek word form would
# ever need, so this single check is stricter than (and subsumes) checking for spaces/apostrophes/digits/
# punctuation/Latin letters individually.
GREEK_ONLY_PATTERN = re.compile(r"^[Ͱ-Ͽἀ-῿]+$")


def split_alternate_forms(form):
    for sep in ("/", "\\"):
        if sep in form:
            return [part.strip() for part in form.split(sep) if part.strip()]
    return [form]


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged form of `entry` that differs from `word` itself - see module
    docstring for why this is generic rather than per-slot like the German/English scripts."""
    result = {}
    for f in entry.get("forms", []):
        raw_form = f.get("form", "")
        if not raw_form or raw_form in ("-", "—"):
            continue
        tags = set(f.get("tags", []))
        if not tags or "inflection-template" in tags:
            continue
        if tags & EXCLUDE_QUALIFIERS:
            continue
        for form in split_alternate_forms(raw_form):
            if form == word or not GREEK_ONLY_PATTERN.match(form):
                continue
            result[form] = True
    return list(result.keys())


def entry_has_modern_sense(entry):
    # Identisch zu dictionaries/en/extract_wiktionary.py's eigener Funktion - siehe deren Kommentar fuer
    # die Begruendung (ein ganzer Eintrag kann selbst archaisch/veraltet sein, ohne dass das ueber die
    # forms[]-Tags sichtbar wird).
    senses = entry.get("senses", [])
    if not senses:
        return True
    for sense in senses:
        glosses = sense.get("raw_glosses") or sense.get("glosses") or []
        for gloss in glosses:
            g = gloss.strip()
            if g.startswith("("):
                closing = g.find(")")
                if closing > 0:
                    qualifier_text = g[1:closing].lower()
                    if any(q in qualifier_text for q in EXCLUDE_QUALIFIERS):
                        continue
            return True
    return False


def main(in_path, out_dir):
    nouns = {}
    verbs = {}
    adjectives = {}
    prepositions = set()
    total = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 1000000 == 0:
                print(
                    f"  ... {total} Zeilen, nouns={len(nouns)} verbs={len(verbs)} "
                    f"adjectives={len(adjectives)} preps={len(prepositions)}",
                    file=sys.stderr,
                )
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "el":
                continue
            pos = entry.get("pos")
            if pos not in ("noun", "verb", "adj", "prep"):
                continue
            if "form-of" in entry.get("tags", []):
                continue
            word = entry.get("word", "")
            if not word:
                continue
            if not entry_has_modern_sense(entry):
                continue

            if pos == "prep":
                prepositions.add(word)
                continue
            forms = usable_forms(entry, word)
            if not forms:
                continue
            target = {"noun": nouns, "verb": verbs, "adj": adjectives}[pos]
            if word not in target:
                target[word] = forms

    def write_forms(path, mapping):
        with open(path, "wb") as out:
            out.write("word\tform\n".encode("utf-8"))
            for word, forms in sorted(mapping.items()):
                for form in forms:
                    out.write(f"{word}\t{form}\n".encode("utf-8"))

    write_forms(f"{out_dir}/wiktionary_nouns.tsv", nouns)
    write_forms(f"{out_dir}/wiktionary_verbs.tsv", verbs)
    write_forms(f"{out_dir}/wiktionary_adjectives.tsv", adjectives)

    with open(f"{out_dir}/wiktionary_prepositions.tsv", "wb") as out:
        out.write("word\n".encode("utf-8"))
        for word in sorted(prepositions):
            out.write(f"{word}\n".encode("utf-8"))

    print(
        f"Fertig: {total} Zeilen gelesen. nouns={len(nouns)} verbs={len(verbs)} "
        f"adjectives={len(adjectives)} prepositions={len(prepositions)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "el-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
