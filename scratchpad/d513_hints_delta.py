# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-513 data pass: rewrites every touched pack's hints.tsv as a UNION-MERGE delta (dropping exactly the
# stale German-derived entries that now conflict with the reformed base or would resurrect freed symbols),
# rewrites the German reference pack to the new base set, writes the bundled English asset as its removal
# delta (a=;o=;u=;s= - the section-2.10 umlaut policy), and bumps each touched pack's version.txt by one.
# Run from the repo root with the real Python: "/c/Program Files/Python314/python scratchpad/d513_hints_delta.py"

from pathlib import Path
import sys

sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).parent.parent

# Entries whose value equals the STALE pre-reform base symbol are dropped per key: m/v/b/c were freed or
# moved (D-512), d's default moved degree->$, n's plus moved to the digit row (base = universal tilde-n),
# the tilde is now 1-left (Nordic l=~), Spanish's l=ñ oddity dies (section 5), and the French w=è scatter
# hint retires (section 2.8). Anything else - every language's own letter or symbol - is kept verbatim.
STALE = {
    "b": ["*"],
    "c": ["÷"],
    "d": ["°"],
    "l": ["~", "ñ"],
    "m": ["-"],
    "n": ["+"],
    "v": ["/"],
    "w": ["è"],
}

TOUCHED = ["bs", "cs", "da", "es", "et", "fi", "fr", "hr", "hu", "id", "it",
           "lt", "lv", "ms", "nb", "nl", "pl", "pt", "ro", "sk", "sv", "sw", "tl", "tr", "uz"]

# de is the curated reference pack: rewritten to exactly the new compiled-in base map.
DE_NEW = "a=ä;d=$;e=€;f=ƒ;h=#;n=ñ;o=ö;p=π;q=@;s=ß;u=ü;x=×"
# en is the bundled removal delta: the reform's base stands except the four German umlaut letters
# (section 2.10 - English keeps them out entirely); s's star left corner is code-side and survives.
EN_NEW = "a=;o=;u=;s="


def rewrite_delta(code: str) -> None:
    path = ROOT / "dictionaries" / code / "hints.tsv"
    text = path.read_text(encoding="utf-8").strip()
    kept, dropped = [], []
    for entry in text.split(";"):
        if not entry:
            continue
        key, _, value = entry.partition("=")
        (dropped if key in STALE and value in STALE[key] else kept).append(entry)
    if not dropped:
        print(f"{code}: already processed (no stale entries left), skipping rewrite")
        return
    assert kept, f"{code}: every entry dropped - would leave no delta"
    path.write_text(";".join(kept) + "\n", encoding="utf-8", newline="\n")
    print(f"{code}: dropped {dropped}")
    bump_version(code)


def bump_version(code: str) -> None:
    path = ROOT / "dictionaries" / code / "version.txt"
    lines = path.read_text(encoding="utf-8").splitlines()
    version = int(lines[0].strip())
    lines[0] = str(version + 1)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print(f"{code}: version.txt {version} -> {version + 1}")


def write_exact(code: str, content: str, pack: bool) -> None:
    if pack:
        path = ROOT / "dictionaries" / code / "hints.tsv"
    else:
        path = ROOT / "app" / "src" / "main" / "assets" / code / "hints.tsv"
    path.write_text(content + "\n", encoding="utf-8", newline="\n")
    print(f"{code}: hints rewritten -> {content}")
    if pack:
        bump_version(code)


for code in TOUCHED:
    rewrite_delta(code)

write_exact("de", DE_NEW, pack=True)
write_exact("en", EN_NEW, pack=False)
print("done")
