# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Lithuanian (`kaikki.org/dictionary/Lithuanian/kaikki.org-dictionary-
# Lithuanian.jsonl.gz`, 8.7MB) - Lithuanian has NO native Wiktionary edition on kaikki.org (confirmed
# directly).
#
# D-450 REAL FINDING, the same pattern already found and solved for the shared Serbo-Croatian source (see
# dictionaries/sh/extract_wiktionary.py's own module docstring): this source's own inflection-table forms are
# written in traditional PITCH-ACCENT dictionary notation (e.g. "nãmas"/"namai̇̃" for "namas"/"house", using
# combining/precomposed stress-accent marks) - not the plain orthography anyone actually types. These accent
# marks fall outside the ordinary Lithuanian Latin alphabet, so `VALID_FORM_RE` would silently (and safely)
# reject them with no special-casing - but that would lose every inflected form this source documents.
# Confirmed directly that each such form instead carries a `links` field whose second element is the real,
# plain-orthography spelling (e.g. `[["nãmas", "namas#Lithuanian"]]`) - `usable_forms()` below prefers this
# recovered spelling over the raw pitch-accented one, the identical fix already applied for Serbo-Croatian.
#
# D-450 SECOND FINDING, also shared with the Serbo-Croatian source: this source's own `"error-unrecognized-
# form"` tag turns up on entries that ARE real, valid words (confirmed against "dirbti"/"to work": almost its
# entire personal-conjugation table is tagged this way, purely because wiktextract's own parser could not
# recognise this source's own template - `links` still resolves them correctly). Deliberately NOT excluded
# here, unlike most other languages this project has built.
#
# Quelle: https://kaikki.org/dictionary/Lithuanian/kaikki.org-dictionary-Lithuanian.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

LITHUANIAN_LETTERS = set("abcdefghijklmnopqrstuvwxyząčęėįšųūž")
VALID_FORM_RE = re.compile(r"^[a-ząčęėįšųūž]+(-[a-ząčęėįšųūž]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

# D-450 REAL FINDING: "error-unrecognized-form" is deliberately OMITTED here, unlike most other languages -
# confirmed directly against a real verb ("dirbti"/"to work") that its own conjugation table tags almost
# every real, valid personal-conjugation form this way, purely because wiktextract's own parser could not
# recognise this source's own template (the `links` field still resolves them correctly) - the same
# source-specific exception already found for the shared Serbo-Croatian source.
EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unknown-tag", "class"}
MULTIWORD_TAGS = {"multiword-construction"}


def resolve_form_string(f):
    """Prefers the real, plain-orthography spelling from `links` over the raw, possibly pitch-accented
    `form` field - same recovery already applied for the shared Serbo-Croatian source."""
    links = f.get("links")
    if links and len(links[0]) > 1 and links[0][1]:
        target = links[0][1].split("#")[0]
        if target:
            return target
    return f.get("form", "")


def usable_forms(entry, word):
    result = {}
    for f in entry.get("forms", []):
        tags = set(f.get("tags", []))
        if tags & EXCLUDE_FORM_TAGS:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        raw_form = resolve_form_string(f)
        if not raw_form or raw_form in ("-", "—"):
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
            if entry.get("lang_code") != "lt":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "lt-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
