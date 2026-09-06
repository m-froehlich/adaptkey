# -*- coding: utf-8 -*-
# One-off script (not part of the runtime pipeline): reads kaikki.org/wiktextract's JSONL dump of the
# ENGLISH Wiktionary's own coverage of Uzbek (kaikki.org/dictionary/Uzbek/kaikki.org-dictionary-Uzbek.jsonl.gz,
# 1,860,421 bytes) - Uzbek has NO native Wiktionary edition on kaikki.org (kaikki.org/dictionary/downloads/uz/
# uz-extract.jsonl.gz 404s, confirmed directly before falling back here) - so per the Guide's own mandatory
# disclosure rule, this pack's POS/Wortfamilien source is genuinely thinner than a native-sourced one, and is
# in fact the THINNEST source of any language pack this project has built (well under Ukrainian's own 27.9MB
# fallback, itself already flagged as thin). Directly modelled on dictionaries/az/extract_wiktionary.py.
#
# D-450-followup REAL FINDING: this source uses the SAME modifier-letter-apostrophe normalisation problem
# `extract_wiki_dump.py`'s own module docstring documents in full for the raw Wikipedia corpus (five distinct
# Unicode characters used interchangeably for "oʻ"/"gʻ" vs. the unrelated loanword glottal stop) -
# `normalize_apostrophes()` here is that same function, applied to `word` and to every form's own text.
#
# D-450-followup REAL FINDING, a genuine positive one for once: unlike Turkish's/Azerbaijani's own rounds
# (both confirmed postpositional with essentially no usable closed-class tag), this source DOES carry a
# small, clean, dedicated `postp` (postposition) tag distinct from `prep`/`prep_phrase` - confirmed directly:
# 15 words tagged `postp` in the whole file, none of them showing the kind of competing-sense ambiguity that
# ruled out a Turkish exception list. Mapped to `PREPOSITION` in `merge_dict.py`'s own `POS_MAP` alongside
# `prep`/`prep_phrase` - the app's own `PartOfSpeech` enum has no separate "postposition" concept, and
# functionally a closed-class adposition serves the same suggestion/autocorrect role regardless of which side
# of the noun it sits on.
#
# D-450-followup REAL FINDING: a real sample ("uy"/"house") showed several declension-table rows tagged
# `error-unrecognized-form` (e.g. "uydek"/"uylardek", "like a house"/"like houses") that are genuine, valid
# Uzbek words, not parser noise - the same genuinely-different-meaning-per-source situation
# `dictionaries/sh/extract_wiktionary.py`'s own module docstring already documents for a different source.
# `EXCLUDE_FORM_TAGS` below deliberately OMITS `"error-unrecognized-form"` for this reason, unlike Russian's/
# Ukrainian's/Azerbaijani's own scripts.
#
# No combining stress marks (plain Latin text, confirmed directly) and no dotted/dotless-I casing quirk
# (Uzbek's Latin alphabet has only plain "i", unlike Turkish/Azerbaijani) - plain `str.lower()` is safe here
# once apostrophes are normalised first.
#
# Quelle: https://kaikki.org/dictionary/Uzbek/kaikki.org-dictionary-Uzbek.jsonl.gz
# Lizenz: wiktextract-Tool MIT-lizenziert; zugrundeliegende Wiktionary-Inhalte CC BY-SA 4.0 / GFDL - gleiche
# Lizenzfamilie, aus der auch dict.tsv/bigram.tsv dieses Projekts bereits stammen.

import gzip
import json
import re
import sys

APOSTROPHE_VARIANTS_RE = re.compile("['‘’ʻʼ]")


def normalize_apostrophes(text: str) -> str:
    def repl(m: re.Match) -> str:
        idx = m.start()
        prev = text[idx - 1] if idx > 0 else ""
        return "ʻ" if prev.lower() in ("o", "g") else "ʼ"

    return APOSTROPHE_VARIANTS_RE.sub(repl, text)


UZBEK_LETTERS = set("abcdefghijklmnopqrstuvwxyzʻʼ")
VALID_FORM_RE = re.compile(r"^[a-zʻʼ]+(-[a-zʻʼ]+)*$")

EXCLUDE_QUALIFIERS = {
    "obsolete", "archaic", "dialectal", "nonstandard", "humorous", "childish",
    "informal", "colloquial", "rare", "dated", "proscribed", "alternative", "slang",
}

# D-450-followup: "error-unrecognized-form" deliberately OMITTED here - see module docstring.
EXCLUDE_FORM_TAGS = {"table-tags", "inflection-template", "canonical", "class"}
MULTIWORD_TAGS = {"multiword-construction"}


def usable_forms(entry, word):
    """Every distinct, grammatically-tagged single-word form of `entry` that differs from `word` itself -
    apostrophes normalised first, then any form containing whitespace rejected outright, no last_token()
    recovery attempted (same policy as every other language this project has built)."""
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
        stripped = normalize_apostrophes(raw_form.strip()).lower()
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
            if total % 100000 == 0:
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
            if entry.get("lang_code") != "uz":
                continue
            if is_form_of_entry(entry):
                continue
            word = entry.get("word", "")
            if not word:
                continue
            word = normalize_apostrophes(word.strip()).lower()
            if not VALID_FORM_RE.match(word):
                continue
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
    in_path = sys.argv[1] if len(sys.argv) > 1 else "uz-english-coverage.jsonl.gz"
    out_dir = sys.argv[2] if len(sys.argv) > 2 else "."
    main(in_path, out_dir)
