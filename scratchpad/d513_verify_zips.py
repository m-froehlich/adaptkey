# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-513 zip verification: every rebuilt archive's hints.tsv/version.txt must byte-match the repo files,
# and the archive member set must be exactly the pack dir's own pack files (dict/bigram/hints/version
# plus the optional per-language extras the dir actually carries).

import zipfile, sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")

CODES = "bs cs da de es et fi fr hr hu id it lt lv ms nb nl pl pt ro sk sv sw tl tr uz".split()
OPTIONAL = ["abbreviations.tsv", "diacritics.tsv"]

bad = []
for code in CODES:
    d = Path("dictionaries") / code
    zpath = Path("language-packs") / f"adaptkey-lang-{code}.zip"
    expected_members = ["dict.tsv", "bigram.tsv", "hints.tsv", "version.txt"] + [o for o in OPTIONAL if (d / o).exists()]
    with zipfile.ZipFile(zpath) as z:
        names = set(z.namelist())
        if names != set(expected_members):
            bad.append((code, "members", z.namelist()))
            continue
        hints = z.read("hints.tsv")
        repo_hints = (d / "hints.tsv").read_bytes()
        ver = z.read("version.txt").decode("utf-8").strip().splitlines()[0]
        repo_ver = (d / "version.txt").read_text(encoding="utf-8").splitlines()[0].strip()
        if hints != repo_hints or ver != repo_ver:
            bad.append((code, "content", ver, repo_ver))

print("zip verification:", "ALL OK (26 archives: member sets + hints/version byte-match repo)" if not bad else bad)
