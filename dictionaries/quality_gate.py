# Shared quality-gate check (Language Contribution Guide §8): verifies a finished dict.tsv against the
# four checks every language pack in this project confirms before publishing - 0 duplicates (case-
# insensitive), 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows. Usage:
#   python ../quality_gate.py dict.tsv

import sys
from collections import Counter


def main(path):
    rows = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            word = parts[0]
            freq = int(parts[1])
            pos = set(parts[2].split(",")) if len(parts) > 2 and parts[2] else set()
            lemma = parts[3] if len(parts) > 3 else ""
            rows.append((word, freq, pos, lemma))

    lower_counts = Counter(w.lower() for w, _, _, _ in rows)
    dupes = {w: c for w, c in lower_counts.items() if c > 1}

    non_positive = [r for r in rows if r[1] <= 0]

    words_lower = {w.lower() for w, _, _, _ in rows}
    orphaned_lemma = [r for r in rows if r[3] and r[3].lower() not in words_lower]

    bare_noun = [r for r in rows if r[2] == {"NOUN"}]

    print(f"Total rows: {len(rows)}")
    print(f"Case-insensitive duplicates (must be 0): {len(dupes)}")
    if dupes:
        print("  sample:", list(dupes.items())[:10])
    print(f"Non-positive frequencies (must be 0): {len(non_positive)}")
    if non_positive:
        print("  sample:", non_positive[:10])
    print(f"Orphaned lemma links (must be 0): {len(orphaned_lemma)}")
    if orphaned_lemma:
        print("  sample:", orphaned_lemma[:10])
    print(f"Bare-NOUN rows (must be 0): {len(bare_noun)}")
    if bare_noun:
        print("  sample:", bare_noun[:10])

    ok = not dupes and not non_positive and not orphaned_lemma and not bare_noun
    print("QUALITY GATE:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
