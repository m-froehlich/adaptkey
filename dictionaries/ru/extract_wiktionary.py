# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the NATIVE
# Russian Wiktionary edition (ru.wiktionary.org itself, kaikki.org/dictionary/downloads/ru/ru-extract.jsonl.gz,
# 290,531,383 bytes compressed - directly verified NOT the smaller kaikki.org/dictionary/Russian/kaikki.org-
# dictionary-Russian.jsonl.gz, 88,879,732 bytes, the wrong English-Wiktionary-coverage file for this purpose)
# and extracts, for nouns/verbs/adjectives, every attested, grammatically-tagged inflection form, plus the
# closed class of prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/tr/extract_wiktionary.py (generic word\tform output, gzip JSONL input).
#
# D-450-followup REAL FINDING, found by direct inspection of real entries before writing this (per the Guide's
# own mandatory pre-use verification): this source's own inflection-table forms carry a combining stress mark
# on the stressed vowel - primary stress U+0301 COMBINING ACUTE ACCENT (e.g. "дома́" = д-о-м-а + U+0301) and,
# less often, secondary stress in compounds via U+0300 COMBINING GRAVE ACCENT - NOT part of the real spelling
# anyone actually types.
#
# D-450-followup REAL BUG, found via the mandatory calibration-ratio sanity check (an adjective ratio came
# back 21x, a clear outlier next to the noun/verb ratios' own ~0.4x/~0.97x) - NOT dismissed as "probably fine",
# the raw matched pairs were pulled directly, per the Guide's own instruction: the ORIGINAL version of this
# function assumed (wrongly, and never actually verified) that no plain Russian letter decomposes under
# Unicode NFD on its own, so it NFD-normalised the whole string and dropped every category-Mn character. That
# assumption was false - checked directly with Python's own `unicodedata.normalize`: й (U+0439) DOES have a
# canonical NFD decomposition to a base "и" (U+0438) + COMBINING BREVE (U+0306, also category Mn), and ё
# (U+0451) decomposes to "е" (U+0435) + COMBINING DIAERESIS (U+0308, also Mn). The old code therefore silently
# corrupted every ORDINARY occurrence of й/ё into и/е respectively - e.g. the extremely common adjective
# "новый" ("new") was silently turned into the wrong word "новыи", "другой" ("other") into "другои",
# "последний" ("last") into "последнии" - all still valid-LOOKING Russian words (so `VALID_FORM_RE` never
# caught it), which is exactly why this needed the ratio-sanity check to surface at all, not a parsing error.
# Fixed by NOT normalising at all - the source text is already NFC (й/ё are already single codepoints, the
# stress mark is already a separately-appended combining character, never merged into the base letter), so
# `strip_stress()` now only removes the two specific stress-mark codepoints (U+0301/U+0300) directly, leaving
# every real letter - й/ё included - completely untouched. Re-verified against the same real "дом"/"красивый"
# samples this module's own comments were originally checked against, plus the "новый"/"другой"/"последний"
# family the bug corrupted, before trusting this fix.
#
# D-450-followup REAL MULTI-WORD FORM CHECK (per the Guide's own mandatory pre-use verification - never assume
# a helper built for one language's own shape is safe to reuse unmodified): checked a real verb ("делать") and
# a real adjective ("красивый") directly. Russian's imperfective future is periphrastic (auxiliary "быть" +
# infinitive) and this source documents it as a single literal placeholder row - "бу́ду/бу́дешь… де́лать", tagged
# just "future" - not a real single form at all (slashes, an ellipsis, and two other words baked into the
# string). Forms containing whitespace are rejected outright, the same "no last_token() recovery, treat as out
# of scope" choice Turkish's/Dutch's own fixes made - nothing real is lost, since the periphrastic future's own
# single-word building blocks ("быть"'s own conjugation, the infinitive itself) are already separate dict.tsv
# entries. The adjective sample showed no comparable issue - every comparative/short-form/case form was a
# single real word once the stress mark was stripped.
#
# Russian has no dotted/dotless-I-style casing quirk (unlike Turkish, dictionaries/tr/extract_wiki_dump.py's
# own module docstring) - plain `str.lower()` is safe here, checked directly (Russian's casing is the
# ordinary one-to-one Cyrillic upper/lower mapping Python's default already gets right).
#
# Quelle: https://kaikki.org/dictionary/downloads/ru/ru-extract.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

RUSSIAN_LETTERS = set("абвгдежзийклмнопрстуфхцчшщъыьэюяё")
VALID_FORM_RE = re.compile(r"^[абвгдежзийклмнопрстуфхцчшщъыьэюяё]+(-[абвгдежзийклмнопрстуфхцчшщъыьэюяё]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unrecognized-form"}
MULTIWORD_TAGS = {"multiword-construction"}


STRESS_MARKS = ("́", "̀")


def strip_stress(s: str) -> str:
    for mark in STRESS_MARKS:
        s = s.replace(mark, "")
    return s


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself -
    stress marks stripped first, then any form containing whitespace (a periphrastic-tense placeholder row -
    see module docstring) is rejected outright, no last_token()-style recovery attempted."""
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
            if entry.get("lang_code") != "ru":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "ru-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
