# -*- coding: utf-8 -*-
# D-569 (M3, english-dictionary-quality plan) REVIEW-LIST GENERATOR - does not modify anything.
#
# The D-106 foreign-language veto's victim census: English dictionary entries with EN frequency <= 20
# whose lowercased surface is a REAL, FREQUENT German word (de frequency >= 300). Every one of them
# kills the silent autocorrect path for that token in German texts (knownInOtherLanguage ->
# suppressAutocorrect, AdaptKeyService) - "eine", "ist", "sich", "nach", "dir" never fire because the
# English corpus happens to carry a handful of stray German words. Per the plan's decided thresholds
# (§4 question 2): EN freq <= 20, de freq >= 300, plus a homograph whitelist reviewed word by word -
# genuine English words (e.g. "die" at 11,775 is excluded by the frequency filter already) stay.

import io

EN = "app/src/main/assets/en/dict.tsv"
DE = "dictionaries/de/dict.tsv"
CANDIDATES_OUT = "scratchpad/d569_m3_candidates.txt"
EN_MAX = 20
DE_MIN = 300


def load(path):
    rows = []
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            rows.append((parts[0], int(parts[1]), parts[2] if len(parts) > 2 else "",
                         parts[3] if len(parts) > 3 else ""))
    return rows


def main():
    en = load(EN)
    de = {w.lower(): f for w, f, _, _ in load(DE)}

    victims = []
    for word, freq, pos, lemma in en:
        if freq > EN_MAX:
            continue
        de_freq = de.get(word.lower())
        if de_freq is not None and de_freq >= DE_MIN:
            victims.append((word, freq, de_freq, pos, lemma))

    victims.sort(key=lambda v: -v[2])
    with io.open(CANDIDATES_OUT, "w", encoding="utf-8", newline="\n") as out:
        out.write(f"{len(victims)} veto victims (EN freq <= {EN_MAX}, de freq >= {DE_MIN}):\n")
        out.write("word\tENfreq\tdeFreq\tENpos\tENlemma\n")
        for word, freq, de_freq, pos, lemma in victims:
            out.write(f"{word}\t{freq}\t{de_freq}\t{pos}\t{lemma}\n")
    print(f"{len(victims)} victims written -> {CANDIDATES_OUT}")


if __name__ == "__main__":
    main()
