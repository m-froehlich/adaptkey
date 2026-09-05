# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Malay Wiktionary edition (`kaikki.org/dictionary/downloads/ms/ms-extract.jsonl.gz`, 5.83MB
# compressed, correctly bigger than the wrong English-Wiktionary-coverage file's 4.11MB) and extracts, for
# nouns/verbs/adjectives, every attested, grammatically-tagged inflection form, plus the closed class of
# prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/pl/extract_wiktionary.py.
#
# D-450 REAL FINDINGS, confirmed by direct inspection before writing this: (1) Malay's own dictionary
# entries carry an alternative JAWI-script spelling (the historical Arabic-based script) tagged `["Jawi"]`
# (e.g. "ابد" for "abad") - excluded here, both because Malay's own real, everyday script is Rumi (Latin) and
# because Jawi's own Arabic characters fall outside `VALID_FORM_RE` regardless. (2) Malay's own plural is
# formed by REDUPLICATION, not a suffix or case ending - confirmed directly (e.g. "bank" -> "bank-bank",
# "abalone" -> "abalone-abalone", tagged `["plural"]`) - the standard `(-[...]+)*` hyphen-joining pattern
# already used by every other language's own `VALID_FORM_RE` in this project correctly accepts this shape
# with no special-casing needed. (3) Malay is a genuinely isolating/agglutinative-by-affixation language with
# no case declension and no simple verb conjugation - confirmed directly that verb entries carry essentially
# no real forms[] data beyond the Jawi spelling (Malay's own real verb morphology - meN-/di-/ber- prefixes
# etc. - is documented as SEPARATE dictionary lemmas, not as inflected forms of a base verb) - so Wortfamilien
# completion for Malay verbs is honestly expected to be minimal, a real source/language characteristic, not a
# bug. (4) no "prep" tag exists anywhere in this data at all (confirmed directly) - Malay's own prepositions
# are apparently not documented under this closed-class tag in this edition; `dict.tsv` will honestly have 0
# `PREPOSITION` rows for this reason, the same honest "0 rows, structurally not a bug" pattern Turkish's own
# D-449 entry already established for a different underlying reason.
#
# Quelle: https://kaikki.org/dictionary/downloads/ms/ms-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

MALAY_LETTERS = set("abcdefghijklmnopqrstuvwxyz")
VALID_FORM_RE = re.compile(r"^[a-z]+(-[a-z]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

# "Jawi" excluded (Arabic script, see module docstring) - "Rumi" (the Latin-script tag) is NOT excluded,
# it marks the same real, everyday spelling this project's own dict.tsv already uses.
EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unrecognized-form", "error-unknown-tag", "class", "Jawi"}
MULTIWORD_TAGS = {"multiword-construction"}

# D-450: this shared kaikki source convention (also confirmed for Indonesian, dictionaries/id/
# extract_wiktionary.py's own module docstring - a real ~800x calibration bug found there) documents a
# reduplicated-plural/intensive word as its OWN SEPARATE dictionary entry, whose own forms[] then lists the
# bare singular/root as if it were a "form of" the reduplicated entry - backwards from the base-lemma-first
# convention. Applied here proactively rather than waiting for a real calibration symptom to force it, since
# the same underlying source convention is confirmed shared between Malay and Indonesian.
SELF_REDUPLICATION_RE = re.compile(r"^(.+)-\1$")


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
            if entry.get("lang_code") != "ms":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word or not VALID_FORM_RE.match(word.lower()):
                continue
            word = word.lower()
            if SELF_REDUPLICATION_RE.match(word):
                continue
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "ms-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
