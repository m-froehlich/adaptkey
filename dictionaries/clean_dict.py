# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-504: mechanical dictionary cleanup - the data half of the A-05 auto-split tuning round.
#
# Rules, in order. Each is deliberately conservative: anything a mechanical rule cannot decide is
# DEFERRED to a review list (written to --report with a rule tag), never guessed.
#
#   R3 named   explicitly named rows (e.g. Meißen, user decision) are dropped regardless of frequency.
#   R1 retag   a PROPER_NOUN tag on a row whose canonical spelling starts lowercase is a Wikipedia
#              sentence-case extraction artefact (a proper noun is capitalised by definition). Rows
#              with other tags keep them minus PROPER_NOUN; rows tagged ONLY PROPER_NOUN are pure
#              artefacts and are dropped outright.
#   R2 proper  rows still tagged PROPER_NOUN with frequency below --floor are the "uncommon proper
#              noun" population. Three outcomes:
#              - real-common-noun shield (the word is a lemma in the language's own curated Wiktionary
#                noun list, --nomen, or decomposes into 2+ dictionary words of length >= 3, e.g.
#                Marktplatz = Markt + Platz): a common noun mis-tagged by the corpus (German surnames
#                ARE common nouns - Fuchs, Reiter, ...) - retagged to its non-PROPER_NOUN tags (NOUN
#                added if nothing noun-ish remains), never deleted;
#              - not shielded and frequency >= --hard-floor (default 300): DEFERRED to review
#                (R2-review) - this band is hand-reviewed rather than trusted to the machine;
#              - not shielded and frequency < --hard-floor: dropped (R2-drop) - but ONLY when a
#                --nomen shield file was available for the language; without a ground-truth noun list
#                the contamination rate is unmeasurable, so without one NOTHING is dropped and every
#                row lands in review instead. That is the safe default for languages that have not
#                yet built their own curated noun extract.
#   R4 junk    lowercase rows of length <= 3 with no vowel at all are fragments or abbreviations.
#              Fragment test: the row is a prefix or a suffix of at least one longer dictionary word
#              ("sch" -> "schoen"; "bzw"/"km" -> neither) - fragments are dropped (the exact class
#              that carries a split half), abbreviations/units without that evidence are DEFERRED
#              (R4-review): they are legitimately typed (bzw, km, vgl, ggf) and stay.
#   R5 stray   a small curated set of foreign function words with NO plausible reading in any bundled
#              language (each homograph-checked by hand: German readings like As/or/is/up/by exclude
#              a row from this set) - dropped. The set is GERMAN-curated and therefore only applied
#              with --strays; for any other language the same idea needs that language's own
#              homograph-checked list first (e.g. "me" is a real Italian pronoun, "la" a real French
#              article - a blanket run would delete real words).
#
# Report mode (default) prints per-rule counts and samples and writes every decision to --report;
# --apply rewrites dict.tsv in place (UTF-8, LF, row order and column layout preserved). Nothing else
# is touched; every change is in git history.

import argparse
import sys
from pathlib import Path

VOWELS = set(
    "aeiouäöüàáâèéêìíîòóôùúû"
    # Cyrillic (ru/uk/sr/...): without these every Cyrillic letter counts as "vowel-less" and the
    # fragment rule would fire on whole words.
    "аеёиоуыэюяєії̀"
    # Greek (el)
    "αεηιουωάέήίόύώ"
)

# Zero-doubt foreign strays only. Deliberately ABSENT: as/or/is/up/by/be (German or borrowed
# nominal readings), are (Flächenmaß "are"), do/go/no/la/de (solfège, game, TLD, French), and every
# unit/abbreviation (km, cm, bzw, ...) - those live in the R4-review list for a human pass.
FOREIGN_STRAYS = {
    "of", "and", "for", "to", "at", "it", "he", "if", "me", "my", "we", "but", "not", "the",
    "has", "had", "can", "did", "from", "have", "this", "that", "with",
}

# Short vowel-less rows with real typing value (units, abbreviations): protected from R4 before the
# fragment test runs. The fragment evidence (prefix of a longer word) misfires on these via unrelated
# matches ("mm" via "mms", "ggf" via whatever long token happens to start with it).
KEEP_SHORT_ABBREVIATIONS = {
    "mm", "cm", "km", "kg", "mg", "ml", "hl", "nm", "pm", "hr", "lb", "vs", "bf", "th", "dt",
    "ff", "ggf", "bzw", "vgl", "ca", "usw", "evtl",
}

NOUNISH = {"NOUN", "PROPER_NOUN"}


def parse_row(line: str):
    parts = line.rstrip("\n").split("\t")
    if len(parts) < 2:
        return None
    word = parts[0]
    tags = parts[2] if len(parts) > 2 else ""
    return word, parts[1], tags, parts


def tag_set(tags: str):
    return [t for t in tags.split(",") if t]


def compound_parts(word: str, vocabulary: set) -> list:
    """Split a capitalised word into 2+ dictionary words (each length >= 3), or None."""
    lower = word.lower()
    n = len(lower)
    dp = [None] * (n + 1)
    dp[0] = []
    for i in range(3, n + 1):
        for j in range(0, i - 2):
            if dp[j] is not None and lower[j:i] in vocabulary:
                candidate = dp[j] + [lower[j:i]]
                if dp[i] is None or len(candidate) > len(dp[i]):
                    dp[i] = candidate
    return dp[n] if dp[n] and len(dp[n]) >= 2 else None


def main():
    ap = argparse.ArgumentParser(description="D-504 mechanical dictionary cleanup")
    ap.add_argument("dict", help="path to dict.tsv")
    ap.add_argument("--floor", type=int, default=500, help="PROPER_NOUN frequency floor (default 500)")
    ap.add_argument("--hard-floor", type=int, default=300,
                    help="below this a non-shielded PROPER_NOUN row is dropped autonomously; between "
                         "hard-floor and floor it is deferred to review (default 300)")
    ap.add_argument("--drop", action="append", default=[], help="explicit row to drop (repeatable)")
    ap.add_argument("--retag", action="append", default=[],
                    help="explicit row to keep but strip PROPER_NOUN from (repeatable) - the output "
                         "of the hand review over the R2-review band")
    ap.add_argument("--nomen", default=None,
                    help="path to the language's curated Wiktionary noun-lemma list (header row "
                         "skipped); the real-common-noun shield for R2. Without it, R2 never drops "
                         "- everything unshielded goes to review.")
    ap.add_argument("--strays", action="store_true",
                    help="apply the (German-curated) R5 stray-word set; without this flag R5 never "
                         "fires - other languages need their own homograph-checked list first")
    ap.add_argument("--apply", action="store_true", help="rewrite dict.tsv (default: report only)")
    ap.add_argument("--report", default=None, help="write the full decision log here")
    args = ap.parse_args()

    src = Path(args.dict)
    raw = src.read_text(encoding="utf-8", newline="")
    lines = raw.split("\n")

    rows = []
    for line in lines:
        row = parse_row(line)
        rows.append(row)  # None for structural lines (trailing "")

    vocabulary = {r[0].lower() for r in rows if r and len(r[0]) >= 4 and r[0].isalpha()}
    # All 1-3 character prefixes of the longer words: an O(1) fragment test instead of scanning
    # every long word per candidate (the naive any(startswith) scan cost ~a minute per language).
    prefixes = set()
    for w in vocabulary:
        for i in range(1, 3):
            prefixes.add(w[:i])
    prefixes.update(w[:3] for w in vocabulary)

    named = {w.casefold() for w in args.drop}
    force_retag = {w.casefold() for w in args.retag}

    nomen = set()
    if args.nomen:
        for i, l in enumerate(Path(args.nomen).read_text(encoding="utf-8").splitlines()):
            if i == 0 or not l:
                continue
            nomen.add(l.split("\t")[0].lower())
        print(f"nomen shield: {len(nomen)} lemmas from {args.nomen}")
    else:
        print("nomen shield: NONE - R2 drops nothing, everything unshielded goes to review")
    can_autodrop = bool(nomen)
    kept = []
    changes = []
    counts = {"r1_retag": 0, "r1_dropped": 0, "r2_drop": 0, "r2_retag": 0, "r2_review": 0,
              "r3_named": 0, "r4_drop": 0, "r4_review": 0, "r5_drop": 0}

    for line, row in zip(lines, rows):
        if row is None:
            kept.append(line)
            continue
        word, freq_s, tags, parts = row
        try:
            freq = int(freq_s)
        except ValueError:
            kept.append(line)
            continue
        tags_t = tag_set(tags)

        if word.casefold() in named:
            counts["r3_named"] += 1
            changes.append(f"R3-drop\t{word}\t{freq_s}\t{tags}")
            continue

        if word.casefold() in force_retag:
            new_tags = [t for t in tags_t if t != "PROPER_NOUN"]
            if not any(t in NOUNISH for t in new_tags):
                new_tags = ["NOUN"] + new_tags
            new_tags_s = ",".join(new_tags)
            counts["r2_retag"] += 1
            changes.append(f"R2-retag\t{word}\t{freq_s}\t{tags} -> {new_tags_s} (hand review)")
            kept.append("\t".join(parts[:2] + [new_tags_s] + parts[3:]))
            continue

        if "PROPER_NOUN" in tags_t and word[:1].islower():
            if set(tags_t) == {"PROPER_NOUN"}:
                counts["r1_dropped"] += 1
                changes.append(f"R1-drop\t{word}\t{freq_s}\t{tags}")
                continue
            new_tags = ",".join(t for t in tags_t if t != "PROPER_NOUN")
            counts["r1_retag"] += 1
            changes.append(f"R1-retag\t{word}\t{freq_s}\t{tags} -> {new_tags}")
            kept.append("\t".join(parts[:2] + [new_tags] + parts[3:]))
            continue

        if "PROPER_NOUN" in tags_t and freq < args.floor:
            shield = compound_parts(word, vocabulary)
            if shield is None and word.lower() in nomen:
                shield = ["<wiktionary-nomen>"]
            if shield:
                new_tags = [t for t in tags_t if t != "PROPER_NOUN"]
                if not any(t in NOUNISH for t in new_tags):
                    new_tags = ["NOUN"] + new_tags
                new_tags_s = ",".join(new_tags)
                counts["r2_retag"] += 1
                changes.append(f"R2-retag\t{word}\t{freq_s}\t{tags} -> {new_tags_s} ({'+'.join(shield)})")
                kept.append("\t".join(parts[:2] + [new_tags_s] + parts[3:]))
            elif freq < args.hard_floor and can_autodrop:
                counts["r2_drop"] += 1
                changes.append(f"R2-drop\t{word}\t{freq_s}\t{tags}")
            else:
                counts["r2_review"] += 1
                changes.append(f"R2-review\t{word}\t{freq_s}\t{tags}")
                kept.append(line)
            continue

        if len(word) <= 3 and word.islower() and word.isalpha() and not (set(word) & VOWELS):
            if word in KEEP_SHORT_ABBREVIATIONS:
                kept.append(line)
                continue
            lower = word.lower()
            # Prefix evidence only: a left split half is a prefix of the word it was cut from, so a
            # fragment that productively begins longer words ("sch" -> "schoen") is the dangerous
            # class. Suffix matches spurious too easily ("mm" via "dumm", "ff" via "Zoff") and would
            # delete typed abbreviations/units - those go to review instead.
            is_fragment = lower in prefixes
            if is_fragment:
                counts["r4_drop"] += 1
                changes.append(f"R4-drop\t{word}\t{freq_s}\t{tags}")
            else:
                counts["r4_review"] += 1
                changes.append(f"R4-review\t{word}\t{freq_s}\t{tags}")
                kept.append(line)
            continue

        if args.strays and word in FOREIGN_STRAYS:
            counts["r5_drop"] += 1
            changes.append(f"R5-drop\t{word}\t{freq_s}\t{tags}")
            continue

        kept.append(line)

    total = sum(counts.values())
    print(f"{src}: {total} decisions {counts}")
    shown = 0
    for c in changes:
        if c.startswith(("R1-", "R2-retag", "R2-review", "R3-", "R4-", "R5-")) and shown < 60:
            print("  " + c)
            shown += 1
    r2d = [c for c in changes if c.startswith("R2-drop")]
    if r2d:
        print(f"  R2-drop sample ({min(30, len(r2d))} of {len(r2d)}):")
        for c in r2d[:30]:
            print("  " + c)

    if args.report:
        Path(args.report).write_text("\n".join(changes) + "\n", encoding="utf-8", newline="\n")
        print(f"decision log: {args.report} ({len(changes)} rows)")

    if args.apply:
        src.write_text("\n".join(kept), encoding="utf-8", newline="\n")
        remaining = sum(1 for l in kept if l)
        print(f"APPLIED: {src} now {remaining} rows (was {sum(1 for l in lines if l)})")
    else:
        print("report only - rerun with --apply to write")


if __name__ == "__main__":
    sys.exit(main())
