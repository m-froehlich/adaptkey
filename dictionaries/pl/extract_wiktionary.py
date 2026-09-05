# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Polish Wiktionary edition (Wikisłownik itself documents Polish, NOT the English Wiktionary's own
# coverage of Polish: kaikki.org/dictionary/downloads/pl/pl-extract.jsonl.gz, 130.7MB compressed, NOT
# kaikki.org/dictionary/Polish/kaikki.org-dictionary-Polish.jsonl.gz, 65.1MB) and extracts, for nouns/verbs/
# adjectives, every attested, grammatically-tagged inflection form, plus the closed class of prepositions
# and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/it/extract_wiktionary.py/dictionaries/nl/extract_wiktionary.py (generic word\tform output).
#
# Verified directly against real entries before writing this (per the Guide's own D-447 hardening - never
# assume a helper built for one language's own multi-word-form shape is safe to reuse unmodified): Polish's
# own native edition documents noun/adjective/verb inflection EXTREMELY richly - 50,380 of 67,607 real noun
# lemmas have real forms (full 7-case declension: nominative/genitive/dative/accusative/instrumental/
# locative/vocative, singular+plural); 19,060 of 22,126 adjective lemmas (the same 7 cases x gender
# (masculine/feminine/neuter/nonvirile) x number, plus comparative/superlative). Verbs show a GENUINELY
# DIFFERENT shape from every other language processed so far: only 801 of 12,402 raw "verb" entries are
# senses[].form_of references (6.5%, vs. 70-95% for French/Spanish/Italian/Dutch/Portuguese) - most Polish
# verb lemmas carry their own complete forms[] table directly, not spread across individually-paged entries.
#
# D-448 REAL MULTI-WORD FORM CHECK (per the Guide's own mandatory pre-use verification): Polish DOES have
# multi-word forms, but of a genuinely different, benign shape from Dutch's own periphrastic/separable-verb
# problem (D-447) - two kinds, both confirmed by direct inspection, neither poses the same risk:
#  1. A slash-separated alternative where one side is a real word and the other a bare grammatical-note
#     shorthand, e.g. "jestem / -(e)m" (the enclitic contraction note for "być"'s own present tense) - here
#     the REAL word ("jestem") is genuinely useful and worth keeping, while the shorthand half ("-(e)m") is
#     not a word at all and is already rejected by VALID_FORM_RE (starts with "-", contains parentheses).
#     Splitting on "/" FIRST (before any whitespace check) recovers "jestem" correctly.
#  2. Genuine two-word reflexive constructions (Polish "się" is always its own separate word, never fused,
#     e.g. "mieć się"/"mam się") - these are correctly out of scope (not a single dictionary word), and
#     rejected by checking each split piece for internal whitespace AFTER splitting on "/", not before -
#     splitting first is what lets case 1 above still recover "jestem" while case 2 still gets rejected
#     (neither "mieć" nor "się" alone would be produced by splitting "mieć się" on "/", since it contains no
#     "/" - the whole two-word string reaches the whitespace check unchanged and is correctly dropped there).
# This is the OPPOSITE order from Dutch's own fix (which checks whitespace on the whole raw string BEFORE
# ever attempting to split) - confirming the Guide's own new warning that this decision must be verified
# per-language, not copied from whichever language was built most recently.
#
# Quelle: https://kaikki.org/dictionary/downloads/pl/pl-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

POLISH_LETTERS = set("abcdefghijklmnopqrstuvwxyząćęłńóśźż")
VALID_FORM_RE = re.compile(r"^[a-ząćęłńóśźż]+(-[a-ząćęłńóśźż]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    # Polish-specific additions, seen in this edition's own real tag vocabulary (verified directly, not
    # guessed): stylistically-marked senses that should not drive an ordinary, neutral-register suggestion.
    "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical"}
MULTIWORD_TAGS = {"multiword-construction"}


def split_alternate_forms(form):
    for sep in ("/", "\\", ","):
        if sep in form:
            return [part.strip() for part in form.split(sep) if part.strip()]
    return [form]


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself.
    Splits on "/" FIRST, then rejects any resulting piece that still contains whitespace (see module
    docstring point 1/2 above for why this order - not the reverse - is the correct one for Polish)."""
    result = {}
    for f in entry.get("forms", []):
        raw_form = f.get("form", "")
        # D-448: "nie" ("no"/"not") turns up as a literal placeholder value in a handful of real
        # conjugation-table cells (e.g. "tyć"'s own passive participle slot), the same "this form does not
        # exist" role "-"/"—" already play elsewhere - confirmed directly against the raw entry, not
        # guessed, before excluding it (9 rows total, found via a real-data spot-check, not by assumption).
        if not raw_form or raw_form.strip().lower() in ("-", "—", "nie"):
            continue
        tags = set(f.get("tags", []))
        if not tags or tags & EXCLUDE_FORM_TAGS:
            continue
        if tags & EXCLUDE_QUALIFIERS or tags & MULTIWORD_TAGS:
            continue
        for form in split_alternate_forms(raw_form.strip().lower()):
            form = form.strip()
            if any(ch.isspace() for ch in form):
                continue
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
            if entry.get("lang_code") != "pl":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "pl-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
