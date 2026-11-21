# -*- coding: utf-8 -*-
# D-573 (M5) ANALYSIS: bucket the QWERTY confusable pairs of the current en/dict.tsv by risk tier
# and emit the must-heal set (score >= AGGRESSIVE's own auto bar 0.70 - i.e. pairs some level can
# silently apply today) with each rare side classified for the review decision:
#   raise  - genuine English word: raise its frequency to safe/75 (ratio <= 75 -> score <= 0.695,
#            below every level's auto-apply threshold incl. AGGRESSIVE 0.70; bump capped at ~2.1%
#            of the safe side's corpus presence so the word stays rare - no ranking distortion)
#   remove - corpus junk / fragment / mojibake (raising junk would put it in the top ranks)
# The chip-offer-only tier (< 0.70) is documented as the accepted §457 residual - an ignorable
# chip per S-01's own philosophy - unless its rare side is junk being removed anyway.
import io
import math
import sys
from collections import defaultdict

sys.path.insert(0, "dictionaries")
from confusables_scan import ROW_LAYOUTS, build_adjacency, ratio_factor, load_dict

EN = "app/src/main/assets/en/dict.tsv"
AGGRESSIVE_AUTO = 0.70

# Reviewed junk (corpus/tokeniser fragments and mojibake with no English reading) whose pair side
# is therefore REMOVED, not raised - decided on the emitted review list below.
JUNK = set()


def scan_pairs(freq):
    adjacency = build_adjacency(ROW_LAYOUTS["qwerty"])
    words_by_len = defaultdict(list)
    for w in freq:
        words_by_len[len(w)].append(w)
    findings = []
    seen = set()
    for length, words in words_by_len.items():
        word_set = set(words)
        for w in words:
            for i, ch in enumerate(w):
                for repl in adjacency.get(ch, ()):
                    cand = w[:i] + repl + w[i + 1:]
                    if cand == w or cand not in word_set:
                        continue
                    key = tuple(sorted((w, cand)))
                    if key in seen:
                        continue
                    seen.add(key)
                    f_w, f_c = freq[w], freq[cand]
                    s1 = ratio_factor(f_c / max(f_w, 1))
                    s2 = ratio_factor(f_w / max(f_c, 1))
                    score = max(s1, s2)
                    if score >= AGGRESSIVE_AUTO:
                        risky, safe = (w, cand) if s1 >= s2 else (cand, w)
                        findings.append((score, risky, freq[risky], safe, freq[safe]))
    findings.sort(key=lambda x: -x[0])
    return findings


def main():
    freq = load_dict(EN)
    findings = scan_pairs(freq)
    print(f"{len(findings)} pairs at/above AGGRESSIVE auto ({AGGRESSIVE_AUTO})")
    with io.open("scratchpad/d573_m5_auto_pairs.tsv", "w", encoding="utf-8", newline="\n") as out:
        out.write("score\trisky\triskyFreq\tsafe\tsafeFreq\traiseFreq(safe/75)\n")
        for score, risky, rf, safe, sf in findings:
            out.write(f"{score:.3f}\t{risky}\t{rf}\t{safe}\t{sf}\t{max(rf, -(-sf // 75))}\n")
    words = sorted({f[1] for f in findings})
    with io.open("scratchpad/d573_m5_rare_words.txt", "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(words) + "\n")
    print(f"{len(words)} distinct rare sides written -> scratchpad/d573_m5_rare_words.txt")
    tiers = defaultdict(int)
    for score, *_ in findings:
        tiers["0.70-0.75(aggr only)" if score < 0.75 else ">=0.75(medium+)"] += 1
    for k, v in sorted(tiers.items()):
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
