# -*- coding: utf-8 -*-
# D-577 (named-residuals follow-up) APPLY: the German side's orphaned lemma links.
#
# History §567 named this residual: the de quality gate reports ~479 orphaned lemma links - the
# D-504 purge removed uncommon proper-noun base forms (Jehova, Shakespeare, Osteuropa, ... below
# the frequency floor) but left their genitive/inflected rows' lemma column pointing at the now-
# gone targets. The lemma column has exactly one reader (the family resolution, one hop:
# entry.lemma ?: entry.word), so a dead link is dead weight - the family veto it exists for can
# never fire across a missing target.
#
# Minimal fix: CLEAR the dead lemma values, keep the rows (Jehovas/Mohammeds/Shakespeares are real
# German genitive forms; removing them would be a new purge decision, not this residual's scope).
# After the cleanup: dictionaries/de/lemma_check.py PASS, quality gate PASS, de pack version
# 53 -> 54, zip rebuilt byte-verified (the caller).

import io

DE = "dictionaries/de/dict.tsv"


def main():
    with io.open(DE, "r", encoding="utf-8", newline="") as f:
        lines = f.read().splitlines(True)

    words = {l.split("\t", 1)[0].lower() for l in lines}
    cleared = 0
    out = []
    for line in lines:
        parts = line.rstrip("\n").split("\t")
        if len(parts) > 3 and parts[3] and parts[3].lower() not in words:
            parts[3] = ""
            cleared += 1
            out.append("\t".join(parts) + "\n")
        else:
            out.append(line)

    with io.open(DE, "w", encoding="utf-8", newline="") as f:
        f.writelines(out)
    print(f"cleared {cleared} orphaned lemma links; {len(lines)} rows kept")


if __name__ == "__main__":
    main()
