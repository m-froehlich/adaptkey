# Merges the real Hungarian-Wikipedia-dump frequency counts (wiki_dump_freq.tsv, from extract_wiki_dump.py)
# with POS tags from the English Wiktionary's own coverage of Hungarian (wiktionary_allpos.tsv, from
# extract_wiktionary.py - no native edition exists for this language, confirmed directly) into the initial
# dict.tsv - one shared source feeding both this step and the word-family completion that runs after it.
# Directly modelled on dictionaries/pl/merge_dict.py.

from pathlib import Path

BASE = Path(__file__).parent
FREQ_IN = BASE / "wiki_dump_freq.tsv"
ALLPOS_IN = BASE / "wiktionary_allpos.tsv"
EN_DICT_IN = BASE.parent.parent / "app" / "src" / "main" / "assets" / "en" / "dict.tsv"
OUT = BASE / "dict.tsv"
NOISE_REVIEW_OUT = BASE / "noise_review_candidates.tsv"

POS_MAP = {
    "noun": "NOUN",
    "verb": "VERB",
    "adj": "ADJECTIVE",
    "prep": "PREPOSITION",
    "postp": "PREPOSITION",
    "prep_phrase": "PREPOSITION",
    "name": "PROPER_NOUN",
}

EN_COMMON_MIN_FREQ = 100
TARGET_TOP = 1_000_000
UNRECOGNISED_MIN_COUNT = 20

ENUM_ORDER = ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"]


def load_kaikki():
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
    if "PROPER_NOUN" in cats and len(cats) > 1:
        cats.discard("PROPER_NOUN")
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
