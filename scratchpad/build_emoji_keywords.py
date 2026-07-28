#!/usr/bin/env python3
"""Throwaway builder for app/src/main/assets/emoji_keywords.tsv (D-316).

Extracts the CLDR search-keyword annotations for each emoji already bundled in emoji_dataset.tsv, for
German and English, from the raw CLDR annotation XML files fetched into scratchpad/cldr/{de,en}.xml
(see CREDITS.md for licence/attribution). Both the "tts" (short name) and the full pipe-separated
keyword-list annotation are used, so a colloquial synonym (e.g. English "poop" for the pile-of-poo
emoji, which CLDR only lists in the keyword annotation, not the short name) is searchable too. One
output line per emoji:

    <emoji>\t<term1>|<term2>|...

Terms from both languages are merged into one deduplicated, lowercased list (search does not
distinguish which language a term came from). Emoji with no CLDR entry in either language are reported
on stderr and simply omitted from the output.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATASET_PATH = ROOT / "app/src/main/assets/emoji_dataset.tsv"
CLDR_DE_PATH = ROOT / "scratchpad/cldr/de.xml"
CLDR_EN_PATH = ROOT / "scratchpad/cldr/en.xml"
OUTPUT_PATH = ROOT / "app/src/main/assets/emoji_keywords.tsv"

VARIATION_SELECTOR_16 = "️"

ANNOTATION_RE = re.compile(
    r'<annotation cp="([^"]+)"(?:\s+type="tts")?>([^<]+)</annotation>'
)


def strip_vs16(emoji: str) -> str:
    return emoji.replace(VARIATION_SELECTOR_16, "")


def load_terms_map(path: Path) -> dict[str, list[str]]:
    """Maps each emoji to every term CLDR offers for it - the tts short name plus every synonym
    from the full keyword-list annotation, in file order."""
    text = path.read_text(encoding="utf-8")
    terms: dict[str, list[str]] = {}
    for match in ANNOTATION_RE.finditer(text):
        cp, value = strip_vs16(match.group(1)), match.group(2)
        bucket = terms.setdefault(cp, [])
        for term in value.split("|"):
            term = term.strip().lower()
            if term and term not in bucket:
                bucket.append(term)
    return terms


def load_target_emoji() -> list[str]:
    seen: dict[str, None] = {}
    for line in DATASET_PATH.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or "\t" not in line:
            continue
        _, emoji = line.split("\t", 1)
        seen.setdefault(emoji, None)
    return list(seen.keys())


def main() -> None:
    de_terms = load_terms_map(CLDR_DE_PATH)
    en_terms = load_terms_map(CLDR_EN_PATH)
    targets = load_target_emoji()

    lines = []
    missing = []
    for emoji in targets:
        key = strip_vs16(emoji)
        terms: list[str] = []
        for term in de_terms.get(key, []) + en_terms.get(key, []):
            if term not in terms:
                terms.append(term)
        if not terms:
            missing.append(emoji)
            continue
        lines.append(f"{emoji}\t{'|'.join(terms)}")

    OUTPUT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {len(lines)} entries to {OUTPUT_PATH}", file=sys.stderr)
    if missing:
        print(f"No CLDR tts entry for {len(missing)} emoji: {' '.join(missing)}", file=sys.stderr)


if __name__ == "__main__":
    main()
