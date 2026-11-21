# -*- coding: utf-8 -*-
# D-568 (M2) APPLY: remove the reviewed function-word flexion artefacts from en/dict.tsv.
#
# Every removal below is a row that exists only as a mechanically minted flexion of a closed-class
# function word (merge_wiktionary.py's completion step over Wiktionary headwords that should never
# have carried noun/verb paradigms: "and" -> "ands" 292,861/rank 6, "with" -> "withs" 71,518/rank 28,
# "that" -> "thats" 74,937/rank 27, "at" -> "atting"/"atted" 46,763/rank 39, "this" -> "thises"
# 31,280/rank 61, "were" -> "weres", "but" -> "buts", "they" -> "theys"/"theying"/"theyed",
# "who" -> "whos", "between" -> "betweens", "while" -> "whiles/whiling/whiled", "will/would" ->
# "woulds", "about" -> "abouts/abouting/abouted", "under" -> "unders", "him" -> "hims",
# "them" -> "thems", "before" -> "befores", "can" -> "coulds", "she" -> "shes", "what" -> "whats",
# "have" -> "havings", "off" -> "offing"/"offed", "must" -> "musts", "shall" -> "shoulds",
# "despite" -> "despites", "across" -> "acrosses", "you" -> "yous", "able-of-can" -> "ables/abling/
# abled", "up" -> "upping"/"upped", "may" -> "mights", "beyond" -> "beyonds", "ought" -> "oughts",
# "be" -> "bes"/"besses", "thee" -> "thees"/"theed", "in" -> "ins" (the plan's named second example,
# also a top German contraction whose veto stays dead while this row exists), "not" -> "Nots",
# "along" -> "alongs", "had" -> "Hading").
#
# Deliberately KEPT (the review list's keep half - real words my shape rules only over-matched, or
# genuine secondary readings of function words): bees, beings, ones, ups, outs, outsides, insides,
# pasts, haves, doings, cans, overs, able/abler/ablest (core adjective; only its minted VERB-of-can
# forms fly), near/nears/nears-family, outed/outing/outings, downed, willed/willing/Wills,
# till/tilled/tilling/tills, mining/mines/mined/minings (lemma "mine" the noun), med, weed, wed,
# shed, heed, hiss, herd, bed, toes/toed, theses, amides, ores, byes, ass, except-family,
# theme-family, paste-family, cane-family, every acronym/proper noun the shape rule touched
# (Ford/AMD/DOS/Andes/Hades/Ares/Ames/Mays/Wes/Peres/Nord/Ord/Tod/Downs/Downing/Canes/SOS/TOS/USS/
# USD/DoD/IND/ISD/ATS/IFS/ANS/HES/BYD/SOD/AMS/ISS/Ors/Hass/Ising/Hering/Mes/SOEs), ites, heres, ates.
#
# Guard: each line must occur exactly once; nothing in the dictionary may lemma-link to a removed
# word (checked against the final file); row order otherwise untouched.

import io

EN = "app/src/main/assets/en/dict.tsv"

REMOVE = {
    "ands", "withs", "thats", "weres", "thises", "atting", "atted", "theying", "theyed",
    "buts", "theys", "afters", "whos", "betweens", "whiles", "whiling", "whiled", "woulds",
    "abouts", "abouting", "abouted", "unders", "hims", "thems", "befores", "coulds", "shes",
    "whats", "havings", "offing", "offed", "musts", "shoulds", "despites", "acrosses", "yous",
    "ables", "abling", "abled", "upping", "upped", "mights", "beyonds", "oughts", "bes",
    "besses", "thees", "theed", "ins", "Nots", "alongs", "Hading", "vias",
}


def main():
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    hits = {}
    for i, line in enumerate(lines):
        word = line.split("\t", 1)[0]
        if word in REMOVE:
            hits.setdefault(word, []).append(i)

    missing = REMOVE - set(hits)
    dupes = {w: len(n) for w, n in hits.items() if len(n) > 1}
    assert not missing, f"not found: {sorted(missing)}"
    assert not dupes, f"duplicated: {dupes}"

    # No surviving row may lemma-link into the removal set.
    for line in lines:
        word = line.split("\t", 1)[0]
        if word in REMOVE:
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) > 3 and parts[3] in REMOVE:
            raise AssertionError(f"{word} lemma-links to removed {parts[3]}")

    kept = [l for l in lines if l.split("\t", 1)[0] not in REMOVE]
    with io.open(EN, "w", encoding="utf-8", newline="") as f:
        f.writelines(kept)
    print(f"removed {len(lines) - len(kept)} rows; {len(kept)} remain")


if __name__ == "__main__":
    main()
