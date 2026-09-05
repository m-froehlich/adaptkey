# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Finnish (`kaikki.org/dictionary/Finnish/kaikki.org-dictionary-
# Finnish.jsonl.gz`, 268.7MB - by far the largest of any fallback-source language checked so far) - Finnish
# has NO native Wiktionary edition on kaikki.org (confirmed directly). **Source quality note, per explicit
# user instruction**: even though this particular fallback source is unusually large and rich (see below),
# expect it to still need more follow-up curation later than a native-sourced pack, per the Guide's own
# standing rule for every fallback-source language.
#
# Verified directly against real entries before writing this: Finnish's own English-Wiktionary coverage is
# extraordinarily rich - 125,244 of 125,814 real noun lemmas have real forms (near-total coverage: the full
# Finnish case system - nominative/genitive/partitive/inessive/elative/illative/adessive/ablative/allative/
# essive/translative/abessive/comitative/instructive x singular/plural, PLUS possessive suffixes for all
# three persons x both numbers); 16,808 of 17,143 adjective lemmas the same. Verbs show the individually-
# paged-conjugated-form shape (28,954 of 41,969 raw "verb" entries, 69%, are `senses[].form_of` references).
#
# D-450 MULTI-WORD FORM CHECK: a real, genuine noise pattern was found and confirmed by direct inspection -
# a literal English-language metadata note, `"no gradation"` (meaning "Finnish consonant gradation does not
# apply to this word" - a genuine grammatical fact, but expressed as English prose, not a Finnish word form
# at all), tagged `["class"]`. This and every other space-containing value are rejected outright by the same
# whitespace check every Scandinavian language built this round already uses - no `last_token()` recovery
# attempted, and no special-casing needed for this specific noise pattern since the whitespace check already
# catches it for free.
#
# D-450 PARADIGM-SIZE CAP, per explicit user decision: an unfiltered first pass generated 13.46 MILLION
# noun/adjective family forms (~158 real forms per lemma - case x number x six possessive-suffix
# combinations, confirmed directly against the real raw JSON for "talo"/"house": 28 real case+number forms
# plus ~130 further real possessive-suffix forms like "taloni"/"talollani"), a dict.tsv 20-40x bigger than
# any other language pack in this project (570MB raw vs. 5-32MB). Presented to the user as a genuine
# structural fork - the user chose to cap generation to the core case x number paradigm only, dropping the
# possessive-suffix forms. Possessive-suffix forms are unambiguously identified by the `"possessive"`/
# `"singular-possessive"`/`"plural-possessive"` tags (confirmed these never appear on ordinary verb personal-
# conjugation forms, which use bare `"first-person"`/`"second-person"`/`"third-person"` tags with no
# `"possessive"` tag alongside them - so this exclusion cannot accidentally strip real verb conjugation data,
# only the genuinely-possessive-suffixed nominal/participial forms).
#
# Quelle: https://kaikki.org/dictionary/Finnish/kaikki.org-dictionary-Finnish.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

FINNISH_LETTERS = set("abcdefghijklmnopqrstuvwxyzåäö")
VALID_FORM_RE = re.compile(r"^[a-zåäö]+(-[a-zåäö]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

EXCLUDE_FORM_TAGS = {
    "table-tags", "inflection-template", "canonical", "error-unrecognized-form", "error-unknown-tag", "class",
    "possessive", "singular-possessive", "plural-possessive",
}
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
            if entry.get("lang_code") != "fi":
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

            if pos not in ("noun", "verb", "adj", "prep", "prep_phrase", "postp", "name"):
                continue
            if pos in ("prep", "prep_phrase", "postp"):
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "fi-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
