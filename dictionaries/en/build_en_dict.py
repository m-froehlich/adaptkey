# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-575 (M8, english-dictionary-quality plan): the reproducible English dictionary build/verify
# tool. English is a bundled asset (app/src/main/assets/en/, no language-pack zip, no versioned
# download - updates ride the app version), so this tool's job is the build_zip.py-Präzedenz's
# verify half: it re-runs every invariant the pipeline rounds established against the SHIPPED
# asset, prints the asset's sha256 as the byte-stable record for release notes, and validates the
# source-side version stamp (dictionaries/en/version.txt, the pack two-line convention: version
# number + declared language).
#
# CANONICAL PIPELINE (each stage's script is committed; a future data round appends its stage):
#   1. extract  dictionaries/en/extract_wiktionary.py    (manual: the 22.9GB kaikki.org
#              raw-wiktextract-data.jsonl.gz; writes wiktionary_{nouns,verbs,adjectives,
#              prepositions}.tsv - CC BY-SA 4.0 / GFDL, see the script's own header)
#   2. merge    dictionaries/en/merge_wiktionary.py      (tags + Median-ratio inflection mint over
#              existing dict.tsv lemmas, the §322 convention)
#   3. safety   dictionaries/en/fix_bare_noun.py         (bare NOUN -> NOUN,OTHER, D-441)
#   4. D-568    scratchpad/d568_m2_review.py + d568_m2_apply.py      (function-word flexion sweep)
#   5. D-569    scratchpad/d569_m3_review.py + d569_m3_apply.py      (DE<->EN veto-victim sweep)
#   6. D-570    scratchpad/d570_m11_review.py + d570_m11_apply.py + d570_m11_apply2.py (name sweep)
#   7. D-571    scratchpad/d571_m4_apply.py              (contractions, fixed documented ranks)
#   8. D-573    scratchpad/d573_m5_analyze.py + d573_m5_apply.py     (confusables heal)
#   9. D-574    scratchpad/d574_m7_review.py + d574_m7_apply.py      (noun-only re-tagging)
#  10. verify   THIS SCRIPT (gates + invariants + sha256 record)
#
# The pre-D-422 base dictionary no longer exists, so stages 2-9 are not re-runnable from scratch
# against the original sources; they ARE deterministic on their inputs (each script asserts its
# edits), which is what "reproducible" means here: re-running the chain on the same inputs
# reproduces the same bytes, and this tool verifies the result. A future round adds its stage to
# the chain and re-runs this verify.
#
# Usage: python dictionaries/en/build_en_dict.py

import hashlib
import io
import math
import sys
from collections import defaultdict
from pathlib import Path

BASE = Path(__file__).parent
ASSET = BASE.parent.parent / "app" / "src" / "main" / "assets" / "en" / "dict.tsv"
VERSION = BASE / "version.txt"
GATE = BASE.parent / "quality_gate.py"
CONFUSABLES = BASE.parent / "confusables_scan.py"

AGGRESSIVE_AUTO = 0.70
EN_BLACKLIST = {"ae", "ij", "iy", "iz", "ne"}

CORE_CONTRACTIONS = {
    "don't", "it's", "I'm", "didn't", "can't", "that's", "isn't", "won't", "I'll", "let's",
    "doesn't",
}


def sha256(path):
    h = hashlib.sha256()
    with io.open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_rows(path):
    rows = []
    with io.open(path, encoding="utf-8", newline="") as f:
        for line in f:
            line = line.rstrip("\n")
            if line:
                parts = line.split("\t")
                rows.append((parts[0], int(parts[1]),
                             parts[2] if len(parts) > 2 else "",
                             parts[3] if len(parts) > 3 else ""))
    return rows


def check_version():
    ok = True
    if not VERSION.exists():
        print("FAIL: dictionaries/en/version.txt missing (source-side stamp, pack two-line "
              "convention: version + language)")
        return False
    lines = VERSION.read_text(encoding="utf-8").splitlines()
    if not lines or not lines[0].strip().isdigit():
        print(f"FAIL: version.txt first line is not a number: {lines[:1]}")
        ok = False
    if len(lines) < 2 or lines[1].strip() != "en":
        print(f"FAIL: version.txt does not declare 'en': {lines}")
        ok = False
    if ok:
        print(f"OK: version stamp {lines[0].strip()} / {lines[1].strip()}")
    return ok


def check_gate():
    import subprocess
    result = subprocess.run(
        [sys.executable, str(GATE), str(ASSET), "--lang", "en"],
        capture_output=True, text=True,
    )
    tail = (result.stdout or "").strip().splitlines()[-1:]
    print(f"quality_gate --lang en: exit={result.returncode} {tail}")
    return result.returncode == 0


WHITELIST = BASE / "veto_victims_whitelist.txt"
DE_DICT = BASE.parent / "de" / "dict.tsv"


def check_veto_victims(rows):
    """D-569/D-573: the D-106 veto's victim census - EN entries with freq <= 20 whose lowercase is
    a de/dict.tsv word with de freq >= 300 - must stay inside the reviewed whitelist file (the
    single source of truth, also consumed by EnglishDictionaryQualityEvaluationTest)."""
    if not WHITELIST.exists() or not DE_DICT.exists():
        print(f"SKIP: veto-victim census ({WHITELIST.name}/{DE_DICT.name} not found)")
        return True
    whitelist = {w.strip() for w in WHITELIST.read_text(encoding="utf-8").splitlines() if w.strip()}
    de = {w.lower(): f for w, f, _, _ in load_rows(DE_DICT)}
    victims = {w for w, f, _, _ in rows if f <= 20 and de.get(w.lower(), 0) >= 300}
    new = victims - whitelist
    if new:
        print(f"FAIL: {len(new)} new EN Rauschen victims (not on the reviewed whitelist): "
              f"{sorted(new)[:10]}")
        return False
    print(f"OK: veto-victim census {len(victims)}/{len(whitelist)} whitelist words "
          f"(no new Rauschen)")
    return True


def check_confusables():
    sys.path.insert(0, str(BASE.parent))
    from confusables_scan import ROW_LAYOUTS, build_adjacency, ratio_factor

    freq = {}
    for word, f, _, _ in load_rows(ASSET):
        freq[word] = f
    adjacency = build_adjacency(ROW_LAYOUTS["qwerty"])
    words_by_len = defaultdict(list)
    for w in freq:
        words_by_len[len(w)].append(w)
    seen = set()
    bad = []
    for length, words in words_by_len.items():
        word_set = set(words)
        for w in words:
            for i, ch in enumerate(w):
                for repl in adjacency.get(ch, ()):
                    cand = w[:i] + repl + w[i + 1:]
                    if cand == w or cand not in word_set:
                        continue
                    key = tuple(sorted((w, cand)))
                    if key in seen:
                        continue
                    seen.add(key)
                    score = max(ratio_factor(freq[cand] / max(freq[w], 1)),
                                ratio_factor(freq[w] / max(freq[cand], 1)))
                    if score >= AGGRESSIVE_AUTO and w not in EN_BLACKLIST \
                            and cand not in EN_BLACKLIST:
                        bad.append((round(score, 3), w, cand))
    if bad:
        print(f"FAIL: {len(bad)} confusable pairs at/above the AGGRESSIVE auto bar outside the "
              f"blacklist: {bad[:5]}")
        return False
    print(f"OK: 0 confusable pairs at/above AGGRESSIVE auto ({AGGRESSIVE_AUTO}) outside the "
          f"blacklist {sorted(EN_BLACKLIST)}")
    return True


def check_contraction_coverage(rows):
    words = {w.lower(): f for w, f, _, _ in rows}
    missing = [c for c in CORE_CONTRACTIONS if c.lower() not in words]
    if missing:
        print(f"FAIL: contraction coverage hole (D-571): {missing}")
        return False
    print(f"OK: contraction coverage ({len(CORE_CONTRACTIONS)} core forms present)")
    return True


def main():
    ok = True
    ok &= check_version()
    ok &= check_gate()
    ok &= check_confusables()
    rows = load_rows(ASSET)
    ok &= check_contraction_coverage(rows)
    ok &= check_veto_victims(rows)
    digest = sha256(ASSET)
    print(f"en/dict.tsv sha256: {digest}")
    print(f"rows: {len(rows)}")
    print("BUILD VERIFY:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
