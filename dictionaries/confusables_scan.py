# Generic keyboard-adjacency confusables scanner (Language Contribution Guide step 7 / D-304/
# D-330-followup's own method) - for a given dict.tsv and a given KeyboardProximity row layout, finds
# every pair of real dictionary words that are a single keyboard-adjacent-key substitution apart and
# scores each with the same CorrectionConfidence.forKnownWordOverride formula the app itself uses
# (ratioFactor = ln(candidateFreq/typedFreq)/ln(500), clamped [0,1]) - a score >= 0.75 (MEDIUM's own
# autoApplyThreshold, AutocorrectAggressiveness.kt) is a genuine silent-autocorrect risk, matching
# exactly the bar German's own dein/sein D-330-followup round used.

import math
import sys
from collections import defaultdict
from pathlib import Path

REQUIRED_OVERRIDE_RATIO = 500.0
MEDIUM_AUTO_APPLY = 0.75
CHIP_OFFER = 0.30

ROW_LAYOUTS = {
    "qwertz": ["1234567890", "qwertzuiop", "asdfghjkl", "yxcvbnm"],
    "qwerty": ["1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"],
    "azerty": ["1234567890", "azertyuiop", "qsdfghjklm", "wxcvbn"],
}


def build_adjacency(rows):
    positions = {}
    for r, keys in enumerate(rows):
        for c, ch in enumerate(keys):
            positions[ch] = (r, c)
    adjacency = defaultdict(set)
    for ch, (r, c) in positions.items():
        for other, (r2, c2) in positions.items():
            if other != ch and abs(r - r2) <= 1 and abs(c - c2) <= 1:
                adjacency[ch].add(other)
    return adjacency


def ratio_factor(ratio):
    if ratio <= 1.0:
        return 0.0
    return min(1.0, math.log(ratio) / math.log(REQUIRED_OVERRIDE_RATIO))


def load_dict(path):
    freq = {}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        word = parts[0]
        try:
            f = int(parts[1])
        except ValueError:
            continue
        freq[word] = f
    return freq


def scan(dict_path, layout_name, min_freq=1):
    adjacency = build_adjacency(ROW_LAYOUTS[layout_name])
    freq = load_dict(dict_path)
    words_by_len = defaultdict(list)
    for w in freq:
        if freq[w] >= min_freq:
            words_by_len[len(w)].append(w)

    findings = []
    seen_pairs = set()
    for length, words in words_by_len.items():
        word_set = set(words)
        for w in words:
            for i, ch in enumerate(w):
                for repl in adjacency.get(ch, ()):
                    candidate = w[:i] + repl + w[i + 1:]
                    if candidate == w or candidate not in word_set:
                        continue
                    pair_key = tuple(sorted((w, candidate)))
                    if pair_key in seen_pairs:
                        continue
                    seen_pairs.add(pair_key)
                    f_w, f_c = freq[w], freq[candidate]
                    # score both directions (typing w risking auto-correct to candidate, and vice versa)
                    score_w_to_c = ratio_factor(f_c / max(f_w, 1))
                    score_c_to_w = ratio_factor(f_w / max(f_c, 1))
                    if score_w_to_c >= CHIP_OFFER or score_c_to_w >= CHIP_OFFER:
                        if score_w_to_c >= score_c_to_w:
                            risky, safe, score = w, candidate, score_w_to_c
                        else:
                            risky, safe, score = candidate, w, score_c_to_w
                        findings.append((score, risky, freq[risky], safe, freq[safe]))
    findings.sort(key=lambda x: -x[0])
    return findings


if __name__ == "__main__":
    dict_path = sys.argv[1]
    layout_name = sys.argv[2]
    min_freq = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    results = scan(dict_path, layout_name, min_freq)
    print(f"{len(results)} candidate pairs at/above chip-offer threshold ({CHIP_OFFER}):")
    for score, risky, risky_f, safe, safe_f in results[:200]:
        tier = "AUTO-APPLY RISK" if score >= MEDIUM_AUTO_APPLY else "chip-offer only"
        print(f"{score:.3f}\t{tier}\t{risky}({risky_f}) -> {safe}({safe_f})")
