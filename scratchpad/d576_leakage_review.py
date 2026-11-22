# -*- coding: utf-8 -*-
# D-576 (named-residuals follow-up) REVIEW-LIST GENERATOR - does not modify anything.
#
# The above-threshold German leakage M3 named as its accepted residual (History §569): EN entries
# whose lowercase is a de/dict.tsv word with de freq >= 300, but whose EN frequency sits ABOVE the
# decided <= 20 band (und 217, Das 151, Dar 127, der 778, ...). This generator widens the EN band
# to <= 2000 for the review - enough to catch the whole German-leakage class, small enough that the
# hits are individually reviewable.

import io

EN = "app/src/main/assets/en/dict.tsv"
DE = "dictionaries/de/dict.tsv"
OUT = "scratchpad/d576_leakage_review.tsv"
EN_MAX = 2000
DE_MIN = 300


def load(path):
    rows = {}
    with io.open(path, encoding="utf-8", newline="") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) >= 2:
                try:
                    rows[parts[0]] = (int(parts[1]), parts[2] if len(parts) > 2 else "",
                                      parts[3] if len(parts) > 3 else "")
                except ValueError:
                    pass
    return rows


def main():
    en = load(EN)
    de = {w.lower(): v[0] for w, v in load(DE).items()}

    hits = []
    for word, (freq, pos, lemma) in en.items():
        if freq > EN_MAX:
            continue
        d = de.get(word.lower())
        if d is not None and d >= DE_MIN:
            hits.append((word, freq, d, pos, lemma))

    hits.sort(key=lambda h: -h[2])
    with io.open(OUT, "w", encoding="utf-8", newline="\n") as out:
        out.write("word\tENfreq\tdeFreq\tENpos\tENlemma\n")
        for word, freq, d, pos, lemma in hits:
            out.write(f"{word}\t{freq}\t{d}\t{pos}\t{lemma}\n")
    print(f"{len(hits)} leakage rows (EN freq <= {EN_MAX}, de freq >= {DE_MIN}) -> {OUT}")


if __name__ == "__main__":
    main()
