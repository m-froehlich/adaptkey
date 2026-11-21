# -*- coding: utf-8 -*-
# D-567 (M1, english-dictionary-quality plan): the immediate veto-blocker fix.
#
# EN: remove the two noise artefacts `mut` (freq 4) and `Dur` (freq 11) from the bundled
#     app/src/main/assets/en/dict.tsv - they are the D-106 foreign-language veto's blockers for the
#     silent corrections "mut"->"mit" and "dur"->"dir" in German texts (knownInOtherLanguage ->
#     suppressAutocorrect, AdaptKeyService).
# DE: recalibrate the real German noun `Dur` 501 -> 100 (below the 300 trustworthy bar, pack
#     convention per D-504's frequency-floor reasoning) so the rule-3 capital form no longer beats
#     the adjacent-key reading "dir" 12713; de pack version 52 -> 53.
#
# Every edit is assert-guarded: the target line must exist exactly once, the neighbouring invariants
# (mit/dir unaffected) must hold afterwards. No re-sorting; byte count and line count changes are
# printed for the commit message.

import io
import sys

EN = "app/src/main/assets/en/dict.tsv"
DE = "dictionaries/de/dict.tsv"
VERSION = "dictionaries/de/version.txt"


def load(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        text = f.read()
    return text


def save(path, text):
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def remove_exact(text, line, path):
    needle = line + "\n"
    count = text.count(needle)
    assert count == 1, f"{path}: expected exactly 1 occurrence of {line!r}, found {count}"
    return text.replace(needle, "", 1)


def main():
    # --- EN: drop the two veto blockers -------------------------------------
    en = load(EN)
    before_lines = en.count("\n")
    en = remove_exact(en, "mut\t4\tNOUN,OTHER", EN)
    en = remove_exact(en, "Dur\t11\tPROPER_NOUN", EN)
    after_lines = en.count("\n")
    assert after_lines == before_lines - 2, (before_lines, after_lines)
    assert "\nmut\t" not in en and "\nDur\t" not in en, "mut/Dur still present after removal"
    # The intended correction targets must be untouched.
    assert "\nmit\t233577\t" not in en  # EN has no lowercase mit row (only the MIT acronym)
    save(EN, en)
    print(f"EN: {before_lines} -> {after_lines} lines (removed mut 4, Dur 11)")

    # --- DE: Dur 501 -> 100 --------------------------------------------------
    de = load(DE)
    old_line = "Dur\t501\tNOUN"
    new_line = "Dur\t100\tNOUN"
    count = de.count(old_line + "\n")
    assert count == 1, f"expected exactly 1 {old_line!r}, found {count}"
    de = de.replace(old_line + "\n", new_line + "\n", 1)
    assert de.count(new_line + "\n") == 1
    # Calibration neighbours untouched.
    assert "dir\t12713\t" in de and "mit\t233577\t" in de
    save(DE, de)
    print("DE: Dur 501 -> 100")

    # --- de pack version 52 -> 53 -------------------------------------------
    v = load(VERSION).strip().splitlines()
    assert v[0] == "52", f"expected version 52, found {v[0]!r}"
    # Keep the second line (declared language code) exactly as it is.
    rest = load(VERSION)[len("52"):]
    save(VERSION, "53" + rest)
    print("DE pack version: 52 -> 53")


if __name__ == "__main__":
    main()
    sys.exit(0)
