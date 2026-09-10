# Shared lemma-column check (D-412), the companion to quality_gate.py. Usage:
#   python ../lemma_check.py dict.tsv [--suspects]
#
# quality_gate.py already verifies that no lemma link is orphaned (points at a word that does not exist).
# This script checks the things that are wrong even when every target exists. Added after D-467 found two
# genuinely wrong German links (waren -> Ware, verlassen -> Verlass) and a survey across all 32 shipped
# dictionaries found the same class everywhere, at far higher rates in the packs whose links came
# mechanically from kaikki.org rather than through German's own individually-reviewed §320/§321/§322 passes.
#
# Why it matters: A-01's known-word ratio override is vetoed outright when the typed word and the candidate
# resolve to the same family (D-404 Tier 2). AdaptKeyService resolves a family with exactly ONE hop -
# `(entry.lemma ?: entry.word)` - so it never walks a chain and a cycle cannot hang it. But a wrong or
# indirect link still makes two words look unrelated (or related) when they are not, and the veto then
# fires, or fails to fire, against a correction the user actually wanted.
#
# STRUCTURAL defects (decidable without speaking the language - these are always wrong):
#   self-link  a word that is its own base form
#   chain      the lemma itself has a lemma; §320/§321 require the true deepest root, not an intermediate
#              hop, precisely because the one-hop resolver above would otherwise give two members of the
#              same real family two different family keys
#   cycle      A is an inflection of B and B is an inflection of A; cannot both hold
#
# SUSPECTS (--suspects; a screening signal, never a verdict): an inflected form that is orders of magnitude
# more frequent than its own base form is usually a mis-link. It is NOT proof - a suppletive paradigm
# legitimately looks exactly like this ("is" <- "be", "είναι" <- "είμαι"), so every hit needs someone who
# reads the language.

import sys
from collections import Counter

SUSPECT_FACTOR = 50
SUSPECT_MIN_FREQ = 500


def load(path):
    freq = {}
    lemma = {}
    display = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 3:
                continue
            key = parts[0].lower()
            freq[key] = int(parts[1])
            display[key] = parts[0]
            if len(parts) > 3 and parts[3]:
                lemma[key] = parts[3].lower()
    return freq, lemma, display


def resolve(word, lemma):
    """Walks to the deepest root. Returns (root, hops, cyclic)."""
    seen = {word}
    current = word
    hops = 0
    while current in lemma:
        nxt = lemma[current]
        if nxt in seen:
            return current, hops, True
        seen.add(nxt)
        current = nxt
        hops += 1
    return current, hops, False


def main(path, show_suspects=False):
    freq, lemma, display = load(path)

    self_links = sorted(w for w, l in lemma.items() if w == l)
    cyclic = []
    chained = []
    for word in lemma:
        _, hops, is_cycle = resolve(word, lemma)
        if is_cycle:
            cyclic.append(word)
        elif hops > 1:
            chained.append(word)
    cyclic.sort()
    chained.sort()

    print(f"Total rows: {len(freq)}")
    print(f"Lemma links: {len(lemma)}")
    print(f"Self-links (must be 0): {len(self_links)}")
    if self_links:
        print("  sample:", [f"{display[w]}" for w in self_links[:10]])
    print(f"Cycles (must be 0): {len(cyclic)}")
    if cyclic:
        print("  sample:", [f"{display[w]}->{lemma[w]}" for w in cyclic[:10]])
    print(f"Chains, lemma of a lemma (must be 0): {len(chained)}")
    if chained:
        sample = []
        for w in chained[:10]:
            mid = lemma[w]
            sample.append(f"{display[w]}->{mid}->{lemma[mid]}")
        print("  sample:", sample)

    if show_suspects:
        suspects = []
        for word, base in lemma.items():
            base_freq = freq.get(base)
            if base_freq and freq[word] >= SUSPECT_MIN_FREQ and freq[word] >= SUSPECT_FACTOR * base_freq:
                suspects.append((freq[word], display[word], base, base_freq))
        suspects.sort(reverse=True)
        print(f"\nSuspect links (screening only, needs a speaker of the language): {len(suspects)}")
        print(f"  criterion: form frequency >= {SUSPECT_FACTOR}x its own base, and >= {SUSPECT_MIN_FREQ}")
        for form_freq, word, base, base_freq in suspects[:40]:
            print(f"    {word} ({form_freq}) <- {base} ({base_freq})")
        if len(suspects) > 40:
            print(f"    ... and {len(suspects) - 40} more")

    ok = not self_links and not cyclic and not chained
    print("\nLEMMA CHECK:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = {a for a in sys.argv[1:] if a.startswith("--")}
    unknown = flags - {"--suspects"}
    if len(args) != 1 or unknown:
        print("usage: python lemma_check.py dict.tsv [--suspects]")
        if unknown:
            print("unknown option(s):", ", ".join(sorted(unknown)))
        sys.exit(2)
    sys.exit(main(args[0], "--suspects" in flags))
