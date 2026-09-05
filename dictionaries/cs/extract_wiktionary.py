# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Czech Wiktionary edition (Wikislovník itself documents Czech, NOT the English Wiktionary's own
# coverage of Czech: kaikki.org/dictionary/downloads/cs/cs-extract.jsonl.gz, 38.4MB compressed, correctly
# bigger than the wrong kaikki.org/dictionary/Czech/kaikki.org-dictionary-Czech.jsonl.gz, 19.7MB) and
# extracts, for nouns/verbs/adjectives, every attested, grammatically-tagged inflection form, plus the closed
# class of prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/pl/extract_wiktionary.py/dictionaries/tr/extract_wiktionary.py.
#
# D-450 REAL FINDING, per direct inspection before trusting the data: this native edition's own proper-noun
# coverage is unusually thin - only 7 raw "name" entries in the WHOLE file (confirmed directly, not a bug -
# Czech Wikislovník evidently does not catalogue proper nouns the way the English Wiktionary's own broader
# coverage does for the fallback-sourced languages this project has built). Noun/verb/adjective coverage
# itself is properly tagged in the ordinary shape (no positional/label-interleaved oddity the way Slovak's
# own fallback source turned out to have).
#
# D-450 MULTI-WORD FORM CHECK: checked directly against a real verb's own conjugation table ("dělat" - "to
# do", 28 forms) - no space-containing forms found at all; Czech's own periphrastic past tense ("dělal jsem")
# is apparently not documented as a single multi-word forms[] entry in this edition, so the standard
# whitespace-rejection rule (kept here regardless, as a universal safety net every language in this project
# carries) has nothing to catch for Czech specifically.
#
# Quelle: https://kaikki.org/dictionary/downloads/cs/cs-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

CZECH_LETTERS = set("abcdefghijklmnopqrstuvwxyzáčďéěíňóřšťúůýž")
VALID_FORM_RE = re.compile(r"^[a-záčďéěíňóřšťúůýž]+(-[a-záčďéěíňóřšťúůýž]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unrecognized-form", "error-unknown-tag", "class"}
MULTIWORD_TAGS = {"multiword-construction"}


def usable_forms(entry, word):
    result = {}
    for f in entry.get("forms", []):
        raw_form = f.get("form", "")
        if not raw_form or raw_form in ("-", "—"):
            continue
        tags = set(f.get("tags", []))
        if not tags or tags & EXCLUDE_FORM_TAGS:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        stripped = raw_form.strip().lower()
        if any(ch.isspace() for ch in stripped):
            continue
        if stripped == word or not VALID_FORM_RE.match(stripped):
            continue
        result[stripped] = True
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
    all_pos = {}
    total = 0
    with gzip.open(in_path, "rt", encoding="utf-8") as f:
        for line in f:
            total += 1
            if total % 1000000 == 0:
                print(
                    f"  ... {total} lines, nouns={len(nouns)} verbs={len(verbs)} "
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
            if entry.get("lang_code") != "cs":
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
        f"Done: {total} lines read. nouns={len(nouns)} verbs={len(verbs)} "
        f"adjectives={len(adjectives)} prepositions={len(prepositions)} propernouns={len(proper_nouns)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    in_path = sys.argv[1] if len(sys.argv) > 1 else "cs-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
