# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-513 catalog pass: bumps the touched packs' LanguagePackCatalog versions by one and appends the
# round's comment line directly above each `version = N` (the established trailing-comment shape).
# Run from the repo root. Idempotence guard: skips a language whose version already carries the round.

from pathlib import Path
import sys

sys.stdout.reconfigure(encoding="utf-8")

PATH = Path("app/src/main/kotlin/de/froehlichmedia/adaptkey/dictionary/LanguagePackCatalog.kt")

TOUCHED = {
    "GERMAN": (49, 50), "BOSNIAN": (3, 4), "CZECH": (3, 4), "DANISH": (3, 4), "SPANISH": (3, 4),
    "ESTONIAN": (3, 4), "FINNISH": (3, 4), "FRENCH": (4, 5), "CROATIAN": (3, 4), "HUNGARIAN": (3, 4),
    "INDONESIAN": (2, 3), "ITALIAN": (3, 4), "LITHUANIAN": (3, 4), "LATVIAN": (2, 3), "MALAY": (2, 3),
    "NORWEGIAN": (3, 4), "DUTCH": (2, 3), "POLISH": (3, 4), "PORTUGUESE": (4, 5), "ROMANIAN": (3, 4),
    "SLOVAK": (3, 4), "SWEDISH": (3, 4), "SWAHILI": (2, 3), "TAGALOG": (2, 3), "TURKISH": (2, 3),
    "UZBEK": (2, 3),
}

COMMENT = ("// D-513 (Alt-Belegungs-Reform section 7.5): hints.tsv rewritten as a union-merge delta - the "
           "stale German-derived symbols (b=* c=÷ d=° l=~/ñ m=- n=+ v=/, fr w=è) dropped, every language's "
           "own letters kept verbatim. version.txt {old} -> {new}, pack rebuilt and verified by reading "
           "hints.tsv/version.txt back out of the archive, LanguagePackCatalog version {old} -> {new}.")

lines = PATH.read_text(encoding="utf-8").splitlines()
lang = None
out = []
bumped = []
for line in lines:
    m = None
    import re
    mm = re.search(r"Language\.([A-Z_]+),", line)
    if mm:
        lang = mm.group(1)
    vm = re.search(r"^(\s*)version = (\d+)\s*$", line)
    if vm and lang in TOUCHED:
        old, new = TOUCHED[lang]
        current = int(vm.group(2))
        if current == old:
            out.append(vm.group(1) + COMMENT.format(old=old, new=new))
            out.append(vm.group(1) + f"version = {new}")
            bumped.append(lang)
            lang = None
            continue
        elif current == new and any(f"Language.{lang}," in l for l in lines[:len(out)]):
            bumped.append(f"{lang} (already {new})")
            lang = None
    out.append(line)

assert len(bumped) == 26, f"expected 26 bumps, got {len(bumped)}: {bumped}"
PATH.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")
print("bumped:", bumped)
