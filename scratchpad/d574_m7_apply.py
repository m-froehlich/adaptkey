# -*- coding: utf-8 -*-
# D-574 (M7) APPLY: re-tag the noun-only force-cap class per the decided policy (a) - Re-Tagging
# nach D-441 with a small explicit exception list.
#
# Every NOUN,PROPER_NOUN row NOT on the curated always-capital list (scratchpad/d574_m7_review.py;
# 214 rows: demonyms/language/nationality/religious adjectives, their plurals, holy books, Nazi)
# is re-tagged NOUN,PROPER_NOUN -> NOUN,OTHER:
#   - CapitalisationEngine's isNounOnly falls through to rule 5 ("ambiguous, no automatic
#     correction") instead of force-capitalising: typed "china vase" stays "china vase", typed
#     "ottoman" (the furniture) stays, typed "sir" stays - while typed "China"/"Sir"/"Ottoman"
#     are preserved by rule 1 (explicit input wins).
#   - The NOUN signal for A-05's split-safety gate survives (contains(NOUN) still true), exactly
#     D-441's convention.
#   - The exception rows keep their tags untouched - they already force-cap via rule 4, which is
#     the desired behaviour for them ("american south" -> "American").
# Scope discipline per the plan: ONLY the audited noun-only class is touched, no global re-marking.
# Named residuals: French/Greek carry NOUN,VERB,ADJECTIVE,PROPER_NOUN (not noun-only - rule 5
# already, S-11 dual chips serve the capital) and the PROPER_NOUN-only polysemes (turkey the bird,
# hamlet, general, ...) are a future content round, not this one.

import io

EN = "app/src/main/assets/en/dict.tsv"
REVIEW = "scratchpad/d574_m7_review.tsv"

OLD = "NOUN,PROPER_NOUN"
NEW = "NOUN,OTHER"


def main():
    retag = {}
    with io.open(REVIEW, "r", encoding="utf-8", newline="") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 4 and parts[3] == "RETAG-NOUN,OTHER":
                retag[parts[1]] = parts[0]

    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    by_word = {}
    for i, line in enumerate(lines):
        by_word[line.split("\t", 1)[0]] = i

    missing = set(retag) - set(by_word)
    assert not missing, f"not found: {sorted(missing)[:10]}"

    changed = 0
    for word, expected_idx in retag.items():
        i = by_word[word]
        assert i == int(expected_idx) - 1, f"line drift for {word}"
        assert OLD in lines[i], f"unexpected tags for {word}: {lines[i]}"
        lines[i] = lines[i].replace(OLD, NEW, 1)
        changed += 1

    with io.open(EN, "w", encoding="utf-8", newline="") as f:
        f.writelines(lines)
    print(f"re-tagged {changed} rows NOUN,PROPER_NOUN -> NOUN,OTHER; exceptions untouched")


if __name__ == "__main__":
    main()
