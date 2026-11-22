# -*- coding: utf-8 -*-
# Round-split helper: strip/restore the D-577 docs blocks so D-576 and D-577 commit separately.

import io
import sys

HIST = "doc/books/AdaptKey-History.md"
PROG = "doc/books/AdaptKey-Progress.md"
TOP = "doc/books/AdaptKey-Topics.md"

HIST_577 = (
    "\n## §577 - D-577: the DE gate passes clean for the first time since D-504 - all 479 "
    "orphaned lemma links cleared, pack 53 -> 54 (data-only, de pack bump)\n"
)
HIST_577_TAIL = (
    "the follow-up build; the de pack rides its own hosted update path at version 54).\n"
)
PROG_577 = (
    "- **D-577 [DE: verwaiste Lemma-Links] - IMPLEMENTED (§577, data-only, de pack 53 -> 54): "
    "all 479 orphaned lemma links cleared (dead targets from the D-504 purge; the family veto can "
    "never fire across a missing target) - rows kept, links emptied. The DE quality gate passes "
    "CLEAN for the first time since D-504; lemma_check PASS; zip rebuilt byte-verified. Full "
    "suite green.\n"
)
TOP_577 = (
    "| D-577 | DE: verwaiste Lemma-Links | all 479 orphaned lemma links cleared, DE gate passes "
    "clean, pack 53 -> 54 | ← Nachfolge Plan `english-dictionary-quality.md` |\n"
)


def strip():
    h = io.open(HIST, encoding="utf-8", newline="").read()
    start = h.index(HIST_577)
    end = h.index(HIST_577_TAIL) + len(HIST_577_TAIL)
    io.open(HIST, "w", encoding="utf-8", newline="").write(h[:start] + h[end:])
    p = io.open(PROG, encoding="utf-8", newline="").read()
    assert PROG_577 in p
    io.open(PROG, "w", encoding="utf-8", newline="").write(p.replace(PROG_577, "", 1))
    t = io.open(TOP, encoding="utf-8", newline="").read()
    assert TOP_577 in t
    t = t.replace(TOP_577, "", 1)
    t = t.replace("**Nächste freie Nummer: D-578.** (Höchste vergebene: D-577.)",
                  "**Nächste freie Nummer: D-577.** (Höchste vergebene: D-576.)")
    io.open(TOP, "w", encoding="utf-8", newline="").write(t)
    print("stripped §577 from docs")


def restore():
    h = io.open(HIST, encoding="utf-8", newline="").read()
    assert HIST_577 not in h
    assert h.endswith("version 54).\n") is False  # §577 content stripped
    if not h.endswith("\n"):
        h += "\n"
    io.open(HIST, "w", encoding="utf-8", newline="").write(
        h + HIST_577 + "\n" + HIST_577_TAIL + "\n")
    p = io.open(PROG, encoding="utf-8", newline="").read()
    anchor = "the bird reading; typed Turkey preserved by rule 1). Gate PASS, BUILD VERIFY PASS, suite green.\n"
    assert anchor in p
    io.open(PROG, "w", encoding="utf-8", newline="").write(
        p.replace(anchor, anchor + PROG_577, 1))
    t = io.open(TOP, encoding="utf-8", newline="").read()
    anchor_t = ("| D-576 | EN-Reste: Leakage + Turkey | above-threshold German leakage swept "
                "(108 rows), Turkey re-tagged for the bird reading; Tower/Golden/Bay worry "
                "disproven | ← Nachfolge Plan `english-dictionary-quality.md` |\n")
    assert anchor_t in t
    t = t.replace(anchor_t, anchor_t + TOP_577, 1)
    t = t.replace("**Nächste freie Nummer: D-577.** (Höchste vergebene: D-576.)",
                  "**Nächste freie Nummer: D-578.** (Höchste vergebene: D-577.)")
    io.open(TOP, "w", encoding="utf-8", newline="").write(t)
    print("restored §577 in docs")


if __name__ == "__main__":
    {"strip": strip, "restore": restore}[sys.argv[1]]()
