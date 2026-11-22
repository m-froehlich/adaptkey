# Shared quality-gate check (Language Contribution Guide §8): verifies a finished dict.tsv against the
# four checks every language pack in this project confirms before publishing - 0 duplicates (case-
# insensitive), 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows. Usage:
#   python ../quality_gate.py dict.tsv [--capitalises-nouns] [--lang en]
#
# D-464: the bare-NOUN check is language-dependent and must be switched off for a language that
# capitalises its common nouns. It encodes D-441's convention for languages that do NOT: there, a common
# noun is tagged NOUN,OTHER so CapitalisationEngine's own isNounOnly falls through to §6 rule 5 instead of
# force-capitalising it, and a bare NOUN would be a real tagging bug. For German the exact opposite holds -
# a bare NOUN is precisely what drives its automatic capitalisation - so running the gate unflagged against
# dictionaries/de/dict.tsv reported FAIL with 108,779 "violations", every one of them correct. Pass
# --capitalises-nouns for German (and any future language sharing that orthography, e.g. Luxembourgish);
# leave it off for every other language, where the check is genuinely right.
#
# D-575 (--lang en, the english-dictionary-quality plan's M9 gate extension): for English the gate adds
# two checks on top of the base four (mutually exclusive with --capitalises-nouns, which English never
# takes):
#   - KNOWN-ARTEFACT DENYLIST: the headline rows of every English data round (D-567's veto blockers
#     mut/Dur/muts, D-568's function-word flexion artefacts ands/withs/thats/..., D-569's German
#     leakage eine/ist/dir/..., D-570's force-cap-only class United/Gulf/Bureau/... and its person
#     tops John/York/Los/Angeles) must stay gone - re-introducing any of them fails the gate.
#   - APOSTROPHE COVERAGE: the core contraction forms (D-571's fixed documented rank list, floor
#     frequencies >= 1000) must be present - D-544 made the apostrophe a word character and the
#     dictionary has to feed it.

import sys
from collections import Counter

# D-575: known-artefact denylist (the headline removals of D-567/D-568/D-569/D-570).
KNOWN_ARTEFACTS = {
    # D-567 veto blockers
    "mut", "Dur", "muts",
    # D-568 function-word flexion artefacts
    "ands", "withs", "thats", "weres", "thises", "atting", "atted", "theying", "theyed",
    "buts", "theys", "afters", "whos", "betweens", "whiles", "whiling", "whiled", "woulds",
    "abouts", "abouting", "abouted", "unders", "hims", "thems", "befores", "coulds", "shes",
    "whats", "havings", "offing", "offed", "musts", "shoulds", "despites", "acrosses", "yous",
    "ables", "abling", "abled", "upping", "upped", "mights", "beyonds", "oughts", "bes",
    "besses", "thees", "theed", "ins", "Nots", "alongs", "Hading", "vias",
    # D-569 German leakage headlines
    "Eine", "ist", "Sich", "nach", "bei", "durch", "nicht", "dir", "Stadt", "sein", "Zeit",
    "Ende", "Dort", "wie", "aber", "alle",
    # D-570 force-cap-only class and person tops
    "United", "Gulf", "Bureau", "Testament", "Marvel", "Trinity", "Enigma", "Commons", "Isle",
    "Isles", "Milky", "John", "York", "Los", "Angeles", "Jr", "Sr", "Inc", "Ltd", "Bros",
}

# D-575: core contraction coverage floor (D-571's documented rank list, headline forms).
CONTRACTION_FLOOR = 1000
CORE_CONTRACTIONS = {
    "don't", "it's", "I'm", "didn't", "can't", "that's", "isn't", "won't", "I'll", "let's",
    "doesn't",
}


def main(path, capitalises_nouns=False, lang_en=False):
    rows = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            word = parts[0]
            freq = int(parts[1])
            pos = set(parts[2].split(",")) if len(parts) > 2 and parts[2] else set()
            lemma = parts[3] if len(parts) > 3 else ""
            rows.append((word, freq, pos, lemma))

    lower_counts = Counter(w.lower() for w, _, _, _ in rows)
    dupes = {w: c for w, c in lower_counts.items() if c > 1}

    non_positive = [r for r in rows if r[1] <= 0]

    words_lower = {w.lower() for w, _, _, _ in rows}
    orphaned_lemma = [r for r in rows if r[3] and r[3].lower() not in words_lower]

    bare_noun = [] if capitalises_nouns else [r for r in rows if r[2] == {"NOUN"}]

    artefacts = [r for r in rows if lang_en and r[0] in KNOWN_ARTEFACTS]

    contraction_holes = []
    if lang_en:
        by_lower = {w.lower(): f for w, f, _, _ in rows}
        contraction_holes = [
            c for c in CORE_CONTRACTIONS
            if by_lower.get(c.lower(), 0) < CONTRACTION_FLOOR
        ]

    print(f"Total rows: {len(rows)}")
    print(f"Case-insensitive duplicates (must be 0): {len(dupes)}")
    if dupes:
        print("  sample:", list(dupes.items())[:10])
    print(f"Non-positive frequencies (must be 0): {len(non_positive)}")
    if non_positive:
        print("  sample:", non_positive[:10])
    print(f"Orphaned lemma links (must be 0): {len(orphaned_lemma)}")
    if orphaned_lemma:
        print("  sample:", orphaned_lemma[:10])
    if capitalises_nouns:
        print("Bare-NOUN rows: check skipped (--capitalises-nouns)")
    else:
        print(f"Bare-NOUN rows (must be 0): {len(bare_noun)}")
        if bare_noun:
            print("  sample:", bare_noun[:10])
    if lang_en:
        print(f"Known-artefact rows (must be 0, D-575 denylist): {len(artefacts)}")
        if artefacts:
            print("  sample:", artefacts[:10])
        print(f"Contraction coverage holes (<{CONTRACTION_FLOOR}, D-575): "
              f"{len(contraction_holes)}")
        if contraction_holes:
            print("  sample:", contraction_holes[:10])

    ok = (not dupes and not non_positive and not orphaned_lemma and not bare_noun
          and not artefacts and not contraction_holes)
    print("QUALITY GATE:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    argv = list(sys.argv[1:])
    flags = {a for a in argv if a.startswith("--")}
    if "--lang" in flags:
        i = argv.index("--lang")
        if i + 1 >= len(argv):
            print("--lang requires a value (en)")
            sys.exit(2)
        argv.pop(i)  # the flag; the value is tolerated and currently only "en" exists
        lang_value = argv.pop(i) if i < len(argv) and not argv[i].startswith("--") else ""
        if lang_value.lower() not in ("", "en"):
            print(f"unsupported --lang value: {lang_value}")
            sys.exit(2)
    args = [a for a in argv if not a.startswith("--")]
    unknown = flags - {"--capitalises-nouns", "--lang"}
    if len(args) != 1 or unknown or ("--lang" in flags and "--capitalises-nouns" in flags):
        print("usage: python quality_gate.py dict.tsv [--capitalises-nouns] [--lang en]")
        if unknown:
            print("unknown option(s):", ", ".join(sorted(unknown)))
        sys.exit(2)
    lang_en = "--lang" in flags
    sys.exit(main(args[0], "--capitalises-nouns" in flags, lang_en))
