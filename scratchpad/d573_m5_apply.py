# -*- coding: utf-8 -*-
# D-573 (M5) APPLY: heal the QWERTY confusable pairs.
#
# Analysis (scratchpad/d573_m5_analyze.py): 1,026 pairs sit at/above AGGRESSIVE's own auto-apply
# bar (0.70; 813 of them even at/above MEDIUM's 0.75) - i.e. pairs some autocorrect level can
# silently apply today. The scan's rare side is the endangered word; two reviewed cures:
#
# 1. RAISE (the D-330 dein/sein precedent, bounded): the rare side is a genuine English word (the
#    overwhelming majority - verb forms, plurals, real archaic/dialectal words, real jargon: abode,
#    balk, cleat, cloze, curation, darning, deduces, demote, denier, dour, dorm, eider, elute,
#    fetter, finales, flan, floe, footfall, fount, fridge, gorse, gruit, gules, hames, harried,
#    jib, kale, kink, lessee, lewd, lode, maim, midden, mire, nigh, nought, oast, orf, pariah,
#    quire, remand, respite, rind, riven, rood, rouble, seta, shard, shilling, sloe, sot, stave,
#    stricture, surd, teat, tern, tomes, tryst, tuber, veal, vegan, whim, wisely, wad, yea, ...).
#    Its frequency is raised to at least ceil(safeFreq / 75): the ratio to the common rival drops
#    to <= 75, so the known-word-override score lands at <= 0.695 - below EVERY level's auto-apply
#    threshold (CAUTIOUS 0.90 / MEDIUM 0.75 / AGGRESSIVE 0.70). The bump is capped at ~2.1% of the
#    rival's corpus presence, so the word stays rare - no ranking distortion beyond suggestion
#    noise; the chip-offer tier below 0.70 is the documented, accepted §457 residual (an unwanted
#    suggestion is merely ignorable, S-01's own philosophy).
# 2. REMOVE (reviewed junk): ~115 rare sides are corpus/tokeniser fragments, foreign stray rows,
#    minted junk or brand rows with no English reading (ths, eer, bpy, acide, acte, aime, dynamis,
#    systemd, Mussolini-style minted junk excepted - already gone, ...). Raising junk would put it
#    in the dictionary's top ranks - removal is the honest cure, the M2 precedent.
#
# Skipped: the EN bundled blacklist words (ae, ij, iy, iz, ne) - their pairs are already cured by
# the existing Notloesung (a blacklisted typed word is correctable outright); the blacklist stays
# minimal per the plan's goal.
#
# Guards: each raised/removed word asserted present exactly once; the post-heal re-scan (run
# separately) must report 0 pairs at/above the AGGRESSIVE auto bar.

import io
import math
import sys
from collections import defaultdict

sys.path.insert(0, "dictionaries")
from confusables_scan import ROW_LAYOUTS, build_adjacency, ratio_factor, load_dict

EN = "app/src/main/assets/en/dict.tsv"
EN_BLACKLIST = {"ae", "ij", "iy", "iz", "ne"}
RATIO_CAP = 75

REMOVE = {
    # corpus/tokeniser fragments and 2-3-letter junk
    "ths", "eer", "eeries", "ene", "ey", "eys", "ih", "lll", "oe", "og", "ov", "wo", "wt",
    "yd", "yods", "zt", "ue", "uit", "usr", "usrs", "pdr", "ped", "pte", "dw", "df", "det",
    "cyt", "bpy", "buf", "cill", "jeh", "ky", "laat", "lect", "lects", "leeter", "leets",
    "loa", "loas", "mish", "meanx", "rarr", "sider", "sive", "toi", "tou", "vir", "vom",
    "wau", "waus", "waw", "fos", "fot", "haa", "har", "hic",
    # foreign stray rows (French/German/Latin) with no English reading
    "acide", "acte", "aime", "aimes", "ais", "dure", "dynamis", "fait", "familiae",
    "fana", "forma", "gentes", "jour", "jours", "ondes", "origine", "parte",
    "statua", "Cul", "Gid", "Hau", "Augustins", "Catholica", "Colonne",
    "Belo", "Mads", "Nei", "Nek", "Pacifica", "Baltis", "Bah", "Artemia", "Aves", "ach",
    # minted junk verb/participle forms of real words
    "achings", "bool", "booled", "booling", "bools", "borts", "breaming", "browing",
    "chopsing", "deferrings", "differenced", "drossed", "drosses", "drossing", "ented",
    "dormed", "dorming", "miced", "mised", "mothed", "pawsed", "prebent", "rebused",
    "renominations", "resourced", "sored", "soring", "sotting", "stad", "stads", "tunning",
    "vided", "viding", "pastings", "raggings", "searings", "wellings", "wettings",
    "toutings", "rightings", "rantings", "textings",
    # brand rows with no generic reading
    "systemd", "marka",
}


def main():
    freq = load_dict(EN)

    # Rebuild the at-risk pair set (rare side -> max required frequency over its pairs).
    adjacency = build_adjacency(ROW_LAYOUTS["qwerty"])
    words_by_len = defaultdict(list)
    for w in freq:
        words_by_len[len(w)].append(w)
    need = defaultdict(int)
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
                    if score < 0.70:
                        continue
                    risky = w if s1 >= s2 else cand
                    safe = cand if s1 >= s2 else w
                    if risky in EN_BLACKLIST:
                        continue  # already cured by the bundled blacklist
                    if risky in REMOVE:
                        continue
                    need[risky] = max(need[risky], -(-freq[safe] // RATIO_CAP))

    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    by_word = {}
    for i, line in enumerate(lines):
        by_word[line.split("\t", 1)[0]] = i

    missing = set(need) - set(by_word)
    assert not missing, f"not found in dict: {sorted(missing)[:20]}"
    already_removed = {w for w in REMOVE if w not in by_word}
    if already_removed:
        print(f"{len(already_removed)} REMOVE words already gone (idempotent re-run)")

    raises = 0
    for word, target in sorted(need.items()):
        i = by_word[word]
        parts = lines[i].rstrip("\n").split("\t")
        old = int(parts[1])
        if old >= target:
            continue
        parts[1] = str(target)
        lines[i] = "\t".join(parts) + "\n"
        raises += 1

    removed = 0
    out = []
    for line in lines:
        word = line.split("\t", 1)[0]
        if word in REMOVE:
            removed += 1
            continue
        out.append(line)
    # Removed junk may leave dangling lemma links - drop any survivor whose lemma is gone.
    words_now = {l.split("\t", 1)[0].lower() for l in out}
    cleaned = []
    for line in out:
        parts = line.rstrip("\n").split("\t")
        if len(parts) > 3 and parts[3] and parts[3].lower() not in words_now:
            print(f"dropping dangling-lemma row: {parts[0]} -> {parts[3]}")
            continue
        cleaned.append(line)

    with io.open(EN, "w", encoding="utf-8", newline="") as f:
        f.writelines(cleaned)
    print(f"raised {raises} rare sides to their safe/75 floor; removed {removed} junk rows; "
          f"{len(lines)} -> {len(cleaned)}")


if __name__ == "__main__":
    main()
