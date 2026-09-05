# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Swahili (`kaikki.org/dictionary/Swahili/kaikki.org-dictionary-
# Swahili.jsonl.gz`, 7.0MB) - Swahili has NO native Wiktionary edition on kaikki.org (confirmed directly).
#
# D-450 checks done before writing this, not assumed: (1) standard fully-tagged forms[] shape, no positional/
# untagged oddity (Slovak) and no pitch-accent notation (Serbo-Croatian/Lithuanian). (2) Swahili's own real
# Bantu noun-class system is documented richly and correctly (e.g. "tao" -> "matao", tagged `["class-vi",
# "plural"]` - the ma- class plural prefix, a real grammatical fact, not noise). (3) verb entries carry their
# own canonical form with a leading bound-root hyphen (e.g. "-soma", tagged `["canonical"]`) - already
# excluded by the existing `EXCLUDE_FORM_TAGS`, and `entry.get("word")` itself is always the clean,
# hyphen-free form ("soma"), so no special-casing was needed. (4) a genuine English-prose noise pattern was
# found and confirmed: some past-tense forms are documented as literal instructions rather than real words
# (e.g. "positive subject concord + -lisoma") - caught for free by the standard whitespace-rejection rule
# every language in this project already uses, no special-casing needed. (5) both `"prep"` and `"prep_phrase"`
# tags exist and are used (39 + 17 entries) - a normal prepositional-language shape.
#
# Quelle: https://kaikki.org/dictionary/Swahili/kaikki.org-dictionary-Swahili.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

SWAHILI_LETTERS = set("abcdefghijklmnopqrstuvwxyz")
VALID_FORM_RE = re.compile(r"^[a-z]+(-[a-z]+)*$")

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
            if entry.get("lang_code") != "sw":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "sw-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
