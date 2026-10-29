# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# Builds language-packs/adaptkey-lang-de.zip (same shape as dictionaries/el/build_zip.py).
# Run from anywhere; verify afterwards by unzipping and byte-comparing dict.tsv.

import zipfile
from pathlib import Path

BASE = Path(__file__).parent
OUT = BASE.parent.parent / "language-packs" / "adaptkey-lang-de.zip"

FILES = ["dict.tsv", "bigram.tsv", "hints.tsv", "version.txt"]

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in FILES:
        z.write(BASE / name, arcname=name)

print(f"Wrote {OUT}")
with zipfile.ZipFile(OUT) as z:
    print(z.namelist())
