# -*- coding: utf-8 -*-
# D-571 (M4, english-dictionary-quality plan) APPLY: add the top English contractions.
#
# The English dictionary contained ZERO apostrophe entries (verified immediately before this run:
# 0 rows matching "'") - D-544 made the apostrophe an ordinary word character, and the dictionary
# had nothing to offer it. Contractions are among the most frequent typed word forms in English
# (don't ranks ~top-40 of everyday text), but the Wikipedia corpus tokenisation split them away
# from their parts, so they never entered the dictionary at all.
#
# Frequencies are a FIXED, DOCUMENTED RANK LIST (the plan's decided §4-3 answer: there is no
# corpus-equivalent form-pair pattern for contractions to calibrate against). The scale is
# calibrated to this dictionary's own frequency range so the entries compete where their real
# typed-English rank puts them: don't between "not" (61,552) and "have" (60,958), it's beside
# "their" (55,092), I'm beside "its" (54,582), ... down to ma'am at rare-but-real. The order is the
# standard typed-English contraction ordering (COCA/Google-Books style), not a corpus measurement
# of this dictionary.
#
# Tags: OTHER throughout (contractions are function-ish; no noun reading anywhere). Rows are
# APPENDED - the dictionary stays frequency-sorted by the §322/Wortfamilien convention, new rows
# never re-sort existing ones. Guard: each row asserted absent (case-insensitively) before insert.

import io

EN = "app/src/main/assets/en/dict.tsv"

CONTRACTIONS = [
    ("don't", 62000),
    ("it's", 55000),
    ("I'm", 46000),
    ("didn't", 38000),
    ("can't", 36000),
    ("that's", 33000),
    ("isn't", 29000),
    ("won't", 27000),
    ("I'll", 26000),
    ("let's", 21000),
    ("doesn't", 20000),
    ("couldn't", 17000),
    ("wasn't", 16500),
    ("wouldn't", 16000),
    ("I'd", 13500),
    ("shouldn't", 12000),
    ("they're", 11500),
    ("we're", 10500),
    ("you're", 10000),
    ("I've", 9500),
    ("he's", 9000),
    ("she's", 7500),
    ("we'll", 7000),
    ("what's", 6800),
    ("aren't", 6000),
    ("who's", 5200),
    ("haven't", 5200),
    ("there's", 5000),
    ("hasn't", 4500),
    ("here's", 4200),
    ("you'll", 4000),
    ("you've", 3600),
    ("we've", 3400),
    ("they'll", 3300),
    ("they've", 3200),
    ("she'll", 3000),
    ("he'll", 2900),
    ("it'll", 2800),
    ("hadn't", 2600),
    ("o'clock", 2200),
    ("ain't", 1800),
    ("mustn't", 1200),
    ("ma'am", 1000),
]


def main():
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        text = f.read()

    existing = {l.split("\t", 1)[0].lower() for l in text.splitlines()}
    for word, _ in CONTRACTIONS:
        assert word.lower() not in existing, f"{word!r} already present"

    with io.open(EN, "a", encoding="utf-8", newline="") as f:
        for word, freq in CONTRACTIONS:
            f.write(f"{word}\t{freq}\tOTHER\n")

    print(f"appended {len(CONTRACTIONS)} contraction rows "
          f"(top: don't 62000, floor: ma'am 1000), tags OTHER")


if __name__ == "__main__":
    main()
