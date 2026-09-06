# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Ukrainian (kaikki.org/dictionary/Ukrainian/kaikki.org-dictionary-
# Ukrainian.jsonl.gz, 27,908,333 bytes) - Ukrainian has NO native Wiktionary edition on kaikki.org
# (kaikki.org/dictionary/downloads/uk/uk-extract.jsonl.gz 404s, confirmed directly against rawdata.html
# before falling back here) - so per the Language Contribution Guide's own mandatory disclosure rule, this
# pack's POS/Wortfamilien source is genuinely thinner than a native-sourced one and should be expected to
# need more follow-up curation later than Russian's own native-sourced round did. Directly modelled on
# dictionaries/ru/extract_wiktionary.py (generic word\tform output, gzip JSONL input, same stress-mark fix).
#
# D-450-followup REAL FINDING, verified directly against a real sample ("будинок"/"house") before writing
# this: this source carries the exact same combining-stress-mark shape Russian's own native edition does
# (e.g. "буди́нок" = б-у-д-и-U+0301-н-о-к) - `strip_stress()` below is Russian's own already-fixed version
# (strip only U+0301/U+0300 directly, no NFD normalisation - NFD would corrupt й/ё-equivalent letters the
# same way it corrupted Russian's before that bug was found and fixed, see that module's own docstring for
# the full account), applied proactively here rather than repeating the same mistake from scratch.
#
# Also carries "class"/"table-tags"/"inflection-template" template-diagnostic noise rows (e.g. "no-table-
# tags", "uk-ndecl", "accent-a") and "romanization" Latin-transliteration rows (e.g. "budýnok") - the former
# excluded via `EXCLUDE_FORM_TAGS` (added "class" to the set Turkish's/Russian's own scripts already used),
# the latter naturally rejected by `VALID_FORM_RE` (Latin letters never match the Cyrillic-only pattern), no
# separate tag-based exclusion needed for it.
#
# Ukrainian's apostrophe (U+0027 or U+2019) is a genuine orthographic letter-boundary marker inside real
# words (e.g. "п'ять"/"five", "об'єднання"/"unification" - written after a labial consonant before
# я/ю/є/ї), not a punctuation elision the way it is in some other languages - `VALID_FORM_RE` allows it
# inside a run of letters accordingly, mirroring how `-` is already allowed for compounds.
#
# Ukrainian has no dotted/dotless-I-style casing quirk (unlike Turkish) - plain `str.lower()` is safe here,
# same verified conclusion as Russian's own module.
#
# Quelle: https://kaikki.org/dictionary/Ukrainian/kaikki.org-dictionary-Ukrainian.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

UKRAINIAN_LETTERS = set("абвгґдежзиіїйклмнопрстуфхцчшщьюя")
VALID_FORM_RE = re.compile(r"^[абвгґдежзиіїйклмнопрстуфхцчшщьюя]+([-'’][абвгґдежзиіїйклмнопрстуфхцчшщьюя]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "class", "romanization"}
MULTIWORD_TAGS = {"multiword-construction"}

STRESS_MARKS = ("́", "̀")


def strip_stress(s: str) -> str:
    for mark in STRESS_MARKS:
        s = s.replace(mark, "")
    return s


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself -
    stress marks stripped first, then any form containing whitespace rejected outright, no last_token()
    recovery attempted (same policy as Russian's own module)."""
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
        stripped = strip_stress(raw_form.strip()).lower()
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
            if entry.get("lang_code") != "uk":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word:
                continue
            word = strip_stress(word.strip()).lower()
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "uk-english-coverage.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
