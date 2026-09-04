# Merges the real Spanish-Wikipedia-dump frequency counts (wiki_dump_freq.tsv, from
# extract_wiki_dump.py) with real POS tags from kaikki.org's NATIVE Spanish Wiktionary extract
# (wiktionary_allpos.tsv, from extract_wiktionary.py - the same native-edition extraction step 4's own
# Wortfamilien completion also reads, see merge_wiktionary.py) into the initial dict.tsv - directly
# modelled on dictionaries/fr/merge_dict.py. D-444: previously read a dedicated kaikki_pos.tsv from a
# since-deleted extract_kaikki.py, which used the smaller, wrong (English-Wiktionary-coverage-of-Spanish)
# source - see AdaptKey-History.md's own D-444 entry for the full story of why that was wrong and how it
# was found.

from pathlib import Path

BASE = Path(__file__).parent
FREQ_IN = BASE / "wiki_dump_freq.tsv"
ALLPOS_IN = BASE / "wiktionary_allpos.tsv"
EN_DICT_IN = BASE.parent.parent / "app" / "src" / "main" / "assets" / "en" / "dict.tsv"
OUT = BASE / "dict.tsv"
NOISE_REVIEW_OUT = BASE / "noise_review_candidates.tsv"

# kaikki/wiktextract's own raw "pos" values -> this project's PartOfSpeech (NOUN/VERB/ADJECTIVE/
# PREPOSITION/PROPER_NOUN/OTHER only) - identical mapping to the deleted extract_kaikki.py's own POS_MAP.
POS_MAP = {
    "noun": "NOUN",
    "verb": "VERB",
    "adj": "ADJECTIVE",
    "prep": "PREPOSITION",
    "prep_phrase": "PREPOSITION",
    "name": "PROPER_NOUN",
}

# Step-2-style batched noise removal (Language Contribution Guide §8): a word not recognised by kaikki
# at all is real noise disproportionately often when it is *also* a common English word - Spanish
# Wikipedia legitimately quotes English titles/technical terms/proper nouns verbatim, and this project's
# own bundled en/dict.tsv is a ready-made, already-reviewed reference for "common English word" (>=100
# is comfortably above one-off/rare-tail English entries). Same threshold/reasoning as the French round.
EN_COMMON_MIN_FREQ = 100

# Rank-1 raw dump count is rescaled to ~1,000,000, matching German's own dict.tsv max order of
# magnitude (1,004,234) - see the Language Contribution Guide's step 1 "frequency-scale calibration".
TARGET_TOP = 1_000_000

# A word not found in kaikki (no real Spanish Wiktionary entry under this exact string) is still kept,
# tagged OTHER, once its own real corpus count clears this floor - low enough to keep genuine words
# Wiktionary just doesn't happen to have a page for, high enough that one-off tokeniser/markup noise
# (typically count 1-a handful in a 100k-article sample) mostly does not.
UNRECOGNISED_MIN_COUNT = 20

ENUM_ORDER = ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"]


def load_kaikki():
    """word -> mapped PartOfSpeech tag set, from wiktionary_allpos.tsv's raw per-word pos vocabulary
    (extract_wiktionary.py) - a raw pos value with no PartOfSpeech mapping (pron/adv/conj/det/intj/num/...)
    is simply dropped, matching this project's own closed-class-folds-to-OTHER convention."""
    pos_map = {}
    for line in ALLPOS_IN.read_text(encoding="utf-8").splitlines()[1:]:
        parts = line.split("\t")
        if len(parts) != 2 or not parts[1]:
            continue
        mapped = {POS_MAP[p] for p in parts[1].split(",") if p in POS_MAP}
        if mapped:
            pos_map[parts[0]] = mapped
    return pos_map


def load_common_english_words():
    words = set()
    for line in EN_DICT_IN.read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        try:
            freq = int(parts[1])
        except ValueError:
            continue
        if freq >= EN_COMMON_MIN_FREQ:
            words.add(parts[0].lower())
    return words


def resolve_tags(kaikki_cats: set) -> str:
    cats = set(kaikki_cats)
    # PROPER_NOUN forces unconditional capitalisation ahead of every other §6 rule
    # (CapitalisationEngine.capitalise: isProper is checked before isPureNoun) - only safe to keep when
    # kaikki never saw this exact string used any other way (a real sol/paz-shaped collision with an
    # ordinary common word must not force-capitalise the far more frequent common reading).
    if "PROPER_NOUN" in cats and len(cats) > 1:
        cats.discard("PROPER_NOUN")
    # A bare NOUN would auto-capitalise in Spanish too (§6 rule 3 is not language-gated - see spec §6's
    # D-441 addendum) - pair with OTHER to keep the real NOUN signal for A-05 while landing on rule 5's
    # "ambiguous, no automatic correction" outcome instead, exactly like every other non-noun-only word.
    if cats == {"NOUN"}:
        cats.add("OTHER")
    if not cats:
        return "OTHER"
    ordered = [t for t in ENUM_ORDER if t in cats]
    return ",".join(ordered)


def main():
    kaikki = load_kaikki()
    common_english = load_common_english_words()

    freq_lines = FREQ_IN.read_text(encoding="utf-8").splitlines()
    top_count = None
    entries = []
    unrecognised_kept = 0
    unrecognised_dropped = 0
    english_contamination_removed = 0
    noise_candidates = []

    for line in freq_lines:
        parts = line.split("\t")
        if len(parts) != 2:
            continue
        word, count_s = parts
        count = int(count_s)
        if top_count is None:
            top_count = count
        cats = kaikki.get(word)
        if cats is None:
            if word in common_english:
                english_contamination_removed += 1
                continue
            if count >= UNRECOGNISED_MIN_COUNT:
                unrecognised_kept += 1
                if count < UNRECOGNISED_MIN_COUNT * 3:
                    # a lower-confidence band worth a real (non-mechanical) noise sample later
                    noise_candidates.append((word, count))
                tags = "OTHER"
            else:
                unrecognised_dropped += 1
                continue
        else:
            tags = resolve_tags(cats)
        scaled = max(1, round(count * TARGET_TOP / top_count))
        entries.append((word, scaled, tags))

    entries.sort(key=lambda e: -e[1])

    with OUT.open("w", encoding="utf-8", newline="\n") as f:
        for word, freq, tags in entries:
            f.write(f"{word}\t{freq}\t{tags}\n")

    with NOISE_REVIEW_OUT.open("w", encoding="utf-8", newline="\n") as f:
        for word, count in sorted(noise_candidates, key=lambda x: -x[1])[:2000]:
            f.write(f"{word}\t{count}\n")

    print(f"Wrote {len(entries)} entries to {OUT}")
    print(f"Unrecognised-by-kaikki but kept (>= {UNRECOGNISED_MIN_COUNT}): {unrecognised_kept}")
    print(f"Unrecognised-by-kaikki and dropped (< {UNRECOGNISED_MIN_COUNT}): {unrecognised_dropped}")
    print(f"Common-English-word contamination removed (any count, not in kaikki): {english_contamination_removed}")
    print(f"Noise-review candidate sample written: {min(2000, len(noise_candidates))} rows to {NOISE_REVIEW_OUT}")

    from collections import Counter
    tag_counts = Counter(t for _, _, t in entries)
    print("Tag distribution:", dict(tag_counts.most_common(20)))


if __name__ == "__main__":
    main()
