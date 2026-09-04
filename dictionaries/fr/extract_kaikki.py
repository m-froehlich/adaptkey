# Extracts a word -> real-POS map for French from kaikki.org's machine-readable Wiktionary extract
# (wiktextract, MIT-licensed tool / CC BY-SA-licensed content - same licence family already accepted
# for German's/Greek's own dict.tsv, see AdaptKey-History.md §322/§424). Unlike German's own from-
# scratch conjugation/declension generators, French Wiktionary already lists most inflected forms as
# their own individually-tagged entries (a conjugated verb form is its own "pos": "verb" row, not
# merely implied by a lemma's conjugation table), so this script's job is a straight extraction/merge,
# not a generation pipeline.

import gzip
import json
from collections import defaultdict
from pathlib import Path

BASE = Path(__file__).parent
KAIKKI = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/cd41590c-250a-4e4a-9ae8-b577b565ce36"
    "/scratchpad/kaikki/fr_wiktionary.jsonl.gz"
)
OUT = BASE / "kaikki_pos.tsv"

# kaikki/en-Wiktionary "pos" values -> this project's PartOfSpeech (NOUN/VERB/ADJECTIVE/PREPOSITION/
# PROPER_NOUN/OTHER only) - everything not explicitly a noun/verb/adjective/preposition/proper name
# maps to OTHER, matching the closed-class convention German's own dict.tsv already uses.
POS_MAP = {
    "noun": "NOUN",
    "verb": "VERB",
    "adj": "ADJECTIVE",
    "prep": "PREPOSITION",
    "prep_phrase": "PREPOSITION",
    "name": "PROPER_NOUN",
}


def main():
    # word.lower() -> set of mapped POS categories seen across every kaikki entry for that string
    by_word = defaultdict(set)
    n = 0
    with gzip.open(KAIKKI, "rt", encoding="utf-8") as f:
        for line in f:
            obj = json.loads(line)
            if obj.get("lang_code") != "fr":
                continue
            word = obj.get("word", "")
            if not word:
                continue
            mapped = POS_MAP.get(obj.get("pos", ""), "OTHER")
            by_word[word.lower()].add(mapped)
            n += 1

    with OUT.open("w", encoding="utf-8", newline="\n") as out:
        for word in sorted(by_word):
            cats = by_word[word]
            out.write(f"{word}\t{','.join(sorted(cats))}\n")

    print(f"Processed {n} kaikki entries, {len(by_word)} distinct French word strings -> {OUT}")


if __name__ == "__main__":
    main()
