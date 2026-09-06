# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Azerbaijani (kaikki.org/dictionary/Azerbaijani/kaikki.org-dictionary-
# Azerbaijani.jsonl.gz, 15,724,873 bytes) - Azerbaijani has NO native Wiktionary edition on kaikki.org
# (kaikki.org/dictionary/downloads/az/az-extract.jsonl.gz 404s, confirmed directly before falling back here)
# - so per the Guide's own mandatory disclosure rule, this pack's POS/Wortfamilien source is genuinely
# thinner than a native-sourced one and should be expected to need more follow-up curation later. Directly
# modelled on dictionaries/ru/extract_wiktionary.py's own shape (gzip JSONL, generic word\tform output).
#
# D-450-followup REAL FINDING, verified directly against a real sample ("ev"/"house") before writing this:
# unlike Russian's/Ukrainian's own sources, this fallback carries NO combining stress marks at all - plain
# Latin text throughout - so no stress-stripping step is needed here. It DOES carry the same "class"/
# "table-tags"/"inflection-template" template-diagnostic noise rows every other kaikki-sourced language's
# own script already excludes, plus a genuinely rich possessive-suffix noun paradigm (nominative/accusative/
# dative/locative/ablative/genitive x singular/plural x first/second/third-person possessive - the same
# scale of richness Turkish's own native edition showed, despite this being the "wrong"/thinner English-
# coverage file by size).
#
# D-450-followup REAL CASING FIX, applied proactively - Azerbaijani shares Turkish's own dotted/dotless İ/I
# Unicode SpecialCasing rule (see dictionaries/az/extract_wiki_dump.py's own module docstring for the same
# fix applied there); `azerbaijani_lower()` here is the identical mapping, applied to `word` and to every
# form's own text.
#
# Quelle: https://kaikki.org/dictionary/Azerbaijani/kaikki.org-dictionary-Azerbaijani.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

AZERBAIJANI_LOWER_MAP = str.maketrans({"İ": "i", "I": "ı"})


def azerbaijani_lower(s: str) -> str:
    return s.translate(AZERBAIJANI_LOWER_MAP).lower()


AZERBAIJANI_LETTERS = set("abcçdeəfgğhxıijkqlmnoöprsştuüvyz")
VALID_FORM_RE = re.compile(r"^[abcçdeəfgğhxıijkqlmnoöprsştuüvyz]+(-[abcçdeəfgğhxıijkqlmnoöprsştuüvyz]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "class"}
MULTIWORD_TAGS = {"multiword-construction"}


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself -
    any form containing whitespace is rejected outright (same policy as every other language this project
    has built - a genuine analytic/periphrastic construction is out of scope by construction)."""
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
        stripped = azerbaijani_lower(raw_form.strip())
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
            if total % 200000 == 0:
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
            if entry.get("lang_code") != "az":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word:
                continue
            word = azerbaijani_lower(word.strip())
            if not VALID_FORM_RE.match(word):
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "az-english-coverage.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
