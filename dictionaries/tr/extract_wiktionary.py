# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# NATIVE Turkish Wiktionary edition (Vikisözlük itself documents Turkish, NOT the English Wiktionary's own
# coverage of Turkish: kaikki.org/dictionary/downloads/tr/tr-extract.jsonl.gz, 43.3MB compressed, NOT
# kaikki.org/dictionary/Turkish/kaikki.org-dictionary-Turkish.jsonl.gz, 34.1MB) and extracts, for nouns/
# verbs/adjectives, every attested, grammatically-tagged inflection form, plus the closed class of
# prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/pl/extract_wiktionary.py (generic word\tform output).
#
# Verified directly against real entries before writing this (per the Guide's own D-447 hardening): Turkish's
# own native edition documents noun/adjective inflection extremely richly - 55,687 of 91,839 real noun
# lemmas have real forms (the full 6-case system: nominative/genitive/dative/accusative/locative/ablative,
# singular+plural+possessive-person paradigms); 11,009 of 12,081 adjective lemmas (comparative/superlative
# plus the same case system where the adjective is used predicatively). Verbs: only 6,621 of 77,658 raw
# "verb" entries are senses[].form_of references (8.5%, the same "most lemmas carry their own full forms[]
# table directly" shape Polish's own D-448 round found, not the individually-paged-form pattern French/
# Spanish/Italian/Dutch/Portuguese showed).
#
# D-449 REAL MULTI-WORD FORM CHECK (per the Guide's own mandatory pre-use verification - never assume a
# helper built for one language's own shape is safe to reuse unmodified): Turkish's own multi-word forms are
# of a different, simpler shape from both Dutch's (D-447) and Polish's (D-448) - confirmed by direct
# inspection, not assumed:
#  1. Comparative/superlative is formed ANALYTICALLY with a separate preceding word - "daha X" (more X) /
#     "en X" (most X) - never a single inflected word at all. There is no single "real" word hiding inside
#     these to recover; the whole two-word phrase is correctly out of scope.
#  2. A number of Wiktionary's own Turkish headwords are themselves genuine multi-word open compounds (e.g.
#     "birleşik söz" - "compound word" - or "mayıs böceği" - "cockchafer") - their own forms[] naturally
#     inherit that same multi-word shape (suffixes attach to the LAST word only, e.g. "birleşik sözü"). These
#     entries are already excluded at the entry level (the `word` field itself fails `VALID_FORM_RE`, which
#     requires letters/hyphens only, no spaces) before `usable_forms()` is ever reached for them.
# Net effect: unlike Polish, there is no known case here where splitting first would recover something
# genuinely useful - forms are rejected outright whenever they contain whitespace, the same order Dutch's own
# fix uses, verified independently for Turkish rather than assumed to transfer from either precedent.
#
# D-449 REAL, TURKISH-SPECIFIC CASING FIX (found before it could silently corrupt data): see
# dictionaries/tr/extract_wiki_dump.py's own module docstring for the full account - ordinary Python
# `str.lower()` gets the ASCII "I"/dotted "İ" pair wrong for Turkish (`"IŞIK".lower()` wrongly produces
# `"işik"` instead of the real word `"ışık"`), a silent, still-plausible-looking corruption rather than a
# loud error. The same `turkish_lower()` helper is used here for every place raw JSON text is normalised to
# lower case (`word`, each form's own text).
#
# Quelle: https://kaikki.org/dictionary/downloads/tr/tr-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

TURKISH_LOWER_MAP = str.maketrans({"İ": "i", "I": "ı"})


def turkish_lower(s: str) -> str:
    return s.translate(TURKISH_LOWER_MAP).lower()


TURKISH_LETTERS = set("abcçdefgğhıijklmnoöprsştuüvyz")
VALID_FORM_RE = re.compile(r"^[a-zçğıiöşü]+(-[a-zçğıiöşü]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical"}
MULTIWORD_TAGS = {"multiword-construction"}


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself -
    any form containing whitespace is rejected outright (see module docstring point 1/2 for why no
    last_token()-style recovery is attempted for Turkish, unlike Polish)."""
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
        stripped = turkish_lower(raw_form.strip())
        if any(ch.isspace() for ch in stripped):
            continue
        # D-449: a bare "daha"/"en" (the analytic comparative/superlative marker word, ordinarily written
        # attached to the adjective as "daha X"/"en X" - see module docstring point 1) turns up alone,
        # without its adjective, in a handful of entries (confirmed: 2 of 21,994 comparative/superlative
        # forms in the whole file, "obez" being the one affected word found) - almost certainly a source
        # annotation inconsistency, not a genuine one-word form. Excluded the same way Polish's own "nie"
        # placeholder quirk (D-448) was: a small, confirmed-real exception, not a guess.
        if stripped in ("daha", "en") and tags & {"comparative", "superlative"}:
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
            if entry.get("lang_code") != "tr":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word:
                continue
            word = turkish_lower(word.strip())
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "tr-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
