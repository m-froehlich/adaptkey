# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Swedish (`kaikki.org/dictionary/Swedish/kaikki.org-dictionary-
# Swedish.jsonl.gz`, 32.3MB) - Swedish has NO native Wiktionary edition on kaikki.org (confirmed directly
# against `kaikki.org/dictionary/rawdata.html`'s own full list before starting this round), so this is the
# correct, only available source per the Language Contribution Guide's own step 3, not a shortcut. **Source
# quality note, per explicit user instruction**: this pack's data is thinner than a native-sourced language's
# own - expect it to need more follow-up curation later (noise cleanup, missing POS coverage) than German/
# English/Greek/French/Spanish/Portuguese/Italian/Dutch/Polish/Turkish/Czech/Indonesian/Malay ever needed for
# this specific reason.
#
# Extracts, for nouns/verbs/adjectives, every attested, grammatically-tagged inflection form, plus the
# closed class of prepositions and the set of proper nouns - all in one pass. Directly modelled on
# dictionaries/pl/extract_wiktionary.py (generic word\tform output).
#
# Verified directly against real entries before writing this (per the Guide's own D-447/D-448/D-449
# hardening - never assume a helper built for one language's own shape is safe to reuse unmodified): even
# from this thinner English-Wiktionary-coverage source, Swedish noun/adjective inflection is still
# reasonably documented - 28,679 of 39,055 real noun lemmas have real forms (definite/indefinite x
# singular/plural x nominative/genitive); 8,012 of 8,743 adjective lemmas (definite/indefinite x singular/
# plural x gender x comparative/superlative). Verbs show the individually-paged-conjugated-form shape most
# native editions built so far do NOT show (40,696 of 47,757 raw "verb" entries, 85%, are `senses[].form_of`
# references - much closer to French's/Spanish's/Italian's/Dutch's/Portuguese's own shape than Polish's/
# Turkish's).
#
# D-450 MULTI-WORD FORM CHECK: Swedish forms comparative/superlative analytically, marker word FIRST -
# `"mer X"`/`"mest X"` ("more X"/"most X", e.g. `"mer antonym"`/`"mest antonym"`) - the same marker-first
# shape Turkish's own `"daha X"`/`"en X"` has (D-449), not French's/Polish's own marker-LAST shapes. There is
# no single real word to recover from these two-word phrases either way, so - like Dutch/Turkish - any raw
# form containing whitespace is rejected outright, no `last_token()` recovery attempted.
#
# A new, generally-useful exclusion added this round: `"error-unrecognized-form"` - wiktextract's own
# diagnostic tag meaning the source template produced something the tool could not fully parse. Seen
# attached to otherwise-plausible-looking forms in this data (e.g. bare `"antonym"` tagged `["error-
# unrecognized-form", "indefinite", "positive"]`) - excluded out of caution, the same "don't trust a form
# already flagged as suspect by the extraction tool itself" reasoning `"table-tags"`/`"inflection-template"`
# already covered.
#
# Quelle: https://kaikki.org/dictionary/Swedish/kaikki.org-dictionary-Swedish.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

SWEDISH_LETTERS = set("abcdefghijklmnopqrstuvwxyzåäö")
VALID_FORM_RE = re.compile(r"^[a-zåäö]+(-[a-zåäö]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
    "uncommon", "poetic", "ironic", "literary", "depreciative", "elevatedly",
}

EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "error-unrecognized-form"}
MULTIWORD_TAGS = {"multiword-construction"}


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself -
    any form containing whitespace is rejected outright (see module docstring for why no last_token()-style
    recovery is attempted for Swedish, matching Dutch's/Turkish's own fix rather than Polish's/French's)."""
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
            if entry.get("lang_code") != "sv":
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "sv-extract.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
