# -*- coding: utf-8 -*-
# Einmal-Skript (nicht Teil der Laufzeit-Pipeline): liest den kaikki.org/wiktextract-JSONL-Dump der
# SPANISCHEN Wiktionary-Ausgabe (es.wiktionary.org selbst dokumentiert Spanisch, NICHT die englische
# Wiktionary-Ausgabe ueber Spanisch - siehe die Language Contribution Guide's eigenen Abschnitt 3:
# kaikki.org/dictionary/downloads/es/es-extract.jsonl.gz, 99,3MB komprimiert, NICHT
# kaikki.org/dictionary/Spanish/kaikki.org-dictionary-Spanish.jsonl.gz, nur 91MB - hier liegen die beiden
# Groessen zufaellig nah beieinander, das macht die falsche Quelle aber nicht weniger falsch). Direkt an
# dictionaries/fr/extract_wiktionary.py angelehnt - siehe dessen eigenen Kommentar fuer die volle
# Begruendung der beiden strukturellen Unterschiede zu Deutsch/Griechisch (form-of sitzt in `senses[]`,
# nicht im obersten `tags`-Feld; Qualifier wie archaic/obsolete sitzen direkt in `senses[].tags`).
#
# Echt gemessen fuer diese Sprache: 647.802 rohe "verb"-Eintraege, aber nur rund 19.000 davon echte Lemmata
# mit eigener Konjugationstabelle (der Rest sind einzelne konjugierte Formen als eigene Wiktionary-Seiten,
# z.B. "hablo" als eigener Eintrag mit `senses[0].form_of = [{"word": "hablar"}]` statt nur als forms[]-
# Element von "hablar" selbst) - der form_of-Filter ist hier nicht optional, sondern unverzichtbar.
#
# Quelle: https://kaikki.org/dictionary/downloads/es/es-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-INHALTE CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

VALID_FORM_RE = re.compile(r"^[a-záéíóúüñ]+(-[a-záéíóúüñ]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}


def split_alternate_forms(form):
    for sep in ("/", "\\", ","):
        if sep in form:
            return [part.strip() for part in form.split(sep) if part.strip()]
    return [form]


# D-444-followup: defensive, ported from dictionaries/fr/extract_wiktionary.py's own real bug fix - French's
# native edition writes some verb-conjugation slots as "il/elle/on mange" (pronoun alternatives glued
# together, sharing one trailing verb form), which a naive "/"-split turns into bogus bare-pronoun "forms"
# (see that file's own module comment for the full story: a verb ratio of ~163x traced back to "il"'s own
# huge corpus frequency getting attached to thousands of unrelated verbs). Spanish's own extract was checked
# directly and does not exhibit this shape (every generated wiktionary_verbs.tsv row was already a genuine
# single word, zero remaining spaces or slashes) - applied here anyway, defensively, so both language
# scripts share one robust extraction rule rather than relying on this edition happening not to need it.
MULTIWORD_TAGS = {"multiword-construction"}


def last_token(form):
    parts = form.split()
    return parts[-1] if parts else form


def usable_forms(entry, word):
    result = {}
    for f in entry.get("forms", []):
        raw_form = f.get("form", "")
        if not raw_form or raw_form in ("-", "—"):
            continue
        tags = set(f.get("tags", []))
        if not tags or "table-tags" in tags or "inflection-template" in tags:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        stripped = last_token(raw_form.strip().lower())
        for form in split_alternate_forms(stripped):
            form = form.strip()
            if form == word or not VALID_FORM_RE.match(form):
                continue
            result[form] = True
    return list(result.keys())


def is_form_of_entry(entry):
    return any(s.get("form_of") for s in entry.get("senses", []))


def entry_has_modern_sense(entry):
    senses = entry.get("senses", [])
    if not senses:
        return True
    for sense in senses:
        if not (set(sense.get("tags", [])) & EXCLUDE_QUALIFIERS):
            return True
    return False


def main(in_path, out_dir):
    nouns = {}
    verbs = {}
    adjectives = {}
    prepositions = set()
    proper_nouns = set()
    # D-444: the FULL set of pos values kaikki documents for a word (every pos type, not only the four this
    # script builds forms for) - the proper-noun collision check needs this broader signal, not just
    # "does this word also appear as a noun/verb/adjective in this same run": a closed-class reading
    # (pronoun, adverb, conjunction, determiner, interjection - none of which PartOfSpeech has a category
    # for) is just as real a collision as a noun/verb one. Real, found example: Spanish "les" (a pronoun,
    # "to them") also has a genuine but rare Wiktionary "name" (surname) entry - without this broader check
    # it would have been wrongly force-capitalised as PROPER_NOUN every time, the exact class of bug this
    # round's own bare-noun safety check exists to prevent, just for PROPER_NOUN instead of NOUN.
    all_pos = {}
    total = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 1000000 == 0:
                print(
                    f"  ... {total} Zeilen, nouns={len(nouns)} verbs={len(verbs)} "
                    f"adjectives={len(adjectives)} preps={len(prepositions)} names={len(proper_nouns)}",
                    file=sys.stderr,
                )
            line = line.strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue
            if entry.get("lang_code") != "es":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word or not VALID_FORM_RE.match(word.lower()):
                continue
            word = word.lower()
            if not entry_has_modern_sense(entry):
                continue

            pos = entry.get("pos")
            all_pos.setdefault(word, set()).add(pos)

            if pos not in ("noun", "verb", "adj", "prep", "prep_phrase", "name"):
                continue
            if pos in ("prep", "prep_phrase"):
                prepositions.add(word)
                continue
            if pos == "name":
                proper_nouns.add(word)
                continue

            forms = usable_forms(entry, word)
            target = {"noun": nouns, "verb": verbs, "adj": adjectives}[pos]
            if word not in target:
                target[word] = forms

    with open(f"{out_dir}/wiktionary_allpos.tsv", "wb") as out:
        out.write("word\tpos_values\n".encode("utf-8"))
        for word in sorted(all_pos):
            out.write(f"{word}\t{','.join(sorted(all_pos[word]))}\n".encode("utf-8"))

    def write_forms(path, mapping):
        with open(path, "wb") as out:
            out.write("word\tform\n".encode("utf-8"))
            for word, forms in sorted(mapping.items()):
                if not forms:
                    out.write(f"{word}\t\n".encode("utf-8"))
                    continue
                for form in forms:
                    out.write(f"{word}\t{form}\n".encode("utf-8"))

    write_forms(f"{out_dir}/wiktionary_nouns.tsv", nouns)
    write_forms(f"{out_dir}/wiktionary_verbs.tsv", verbs)
    write_forms(f"{out_dir}/wiktionary_adjectives.tsv", adjectives)

    with open(f"{out_dir}/wiktionary_prepositions.tsv", "wb") as out:
        out.write("word\n".encode("utf-8"))
        for word in sorted(prepositions):
            out.write(f"{word}\n".encode("utf-8"))

    with open(f"{out_dir}/wiktionary_propernouns.tsv", "wb") as out:
        out.write("word\n".encode("utf-8"))
        for word in sorted(proper_nouns):
            out.write(f"{word}\n".encode("utf-8"))

    print(
        f"Fertig: {total} Zeilen gelesen. nouns={len(nouns)} verbs={len(verbs)} "
        f"adjectives={len(adjectives)} prepositions={len(prepositions)} propernouns={len(proper_nouns)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "es-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
