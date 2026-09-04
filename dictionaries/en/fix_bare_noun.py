# One-off script: closes the bare-NOUN capitalisation-safety gap found while building French's/Spanish's
# own Wortfamilien completion (see the Language Contribution Guide's own step 4, "mandatory bare-noun
# safety check"). English does not capitalise common nouns, but D-422's own merge_wiktionary.py had no
# safety net against a word's own genuine-but-rare/archaic Wiktionary noun sense (e.g. "and"/"or" as
# logic-gate nouns) forcing CapitalisationEngine's rule 3 every time an ordinary function word was typed.
# Mechanical, unconditional fix: any row whose tag set is exactly {NOUN} gets OTHER added. Never touches a
# row that also carries PROPER_NOUN (isProper correctly forces capitalisation regardless of language) or
# any other tag combination. Column order/count and row order are otherwise preserved exactly.

import sys

IN_PATH = sys.argv[1]
OUT_PATH = sys.argv[2]

fixed = 0
total = 0
with open(IN_PATH, encoding="utf-8") as f, open(OUT_PATH, "w", encoding="utf-8", newline="\n") as out:
    for line in f:
        line = line.rstrip("\n")
        if not line:
            out.write("\n")
            continue
        total += 1
        parts = line.split("\t")
        if len(parts) >= 3 and parts[2] == "NOUN":
            parts[2] = "NOUN,OTHER"
            fixed += 1
        out.write("\t".join(parts) + "\n")

print(f"{fixed} of {total} rows fixed (bare NOUN -> NOUN,OTHER) -> {OUT_PATH}")
