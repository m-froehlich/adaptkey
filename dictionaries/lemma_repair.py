# D-468: generalises D-467's German lemma-column repair (§482) to every other pack.
# Fixes the same two structural defect classes lemma_check.py flags as hard failures:
# cycles (A is an inflection of B and B of A) and chains (the lemma itself has a
# lemma) - self-links fall out of the same machinery as a degenerate one-member cycle.
#
# Chains are always safe to flatten onto their deepest non-cyclic root - no data is
# invented, this is exactly what D-412/§320/§321 already require, and matters because
# AdaptKeyService resolves a word family with exactly ONE hop (entry.lemma ?: entry.word).
#
# Cycle direction is decided the same way D-467 decided all 27 German pairs by hand:
# check the language's own retained Wiktionary extracts (wiktionary_verb*/noun*/
# adjekt*.tsv - whichever exist next to dict.tsv) for which cycle member is attested as
# a paradigm headword (column 0) with the other member(s) among its own listed forms
# (the remaining columns). Two tiers, weakest last:
#   tier 1 (strong): exactly one member X has every OTHER cycle member attested as one
#                     of X's own forms.
#   tier 2 (weak, German's actual method): exactly one member is a paradigm headword at
#                     all (regardless of whether the others are specifically its forms).
# Undecidable cycles get EVERY link among their members dropped - fails open to
# pre-D-404-Tier-2 behaviour, asserts nothing. A chain leading into a cycle is never
# treated specially: once the cycle's own edges are resolved (redirected to a decided
# base, or removed), the chain is just flattened through whatever remains, same as any
# other chain.
#
# Usage: python lemma_repair.py <dict.tsv> [--apply]
# Without --apply: dry run, prints counts and a few samples, does not touch the file.
# With --apply: rewrites dict.tsv in place. word/freq/pos columns are byte-preserved;
# only the lemma column (4th, tab-separated) is added/changed/removed per row.

import sys
import os
import glob
from collections import defaultdict


def load_dict(path):
    rows = []
    freq, lemma, display = {}, {}, {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            raw = line.rstrip("\n")
            parts = raw.split("\t")
            rows.append(parts)
            if len(parts) < 3:
                continue
            key = parts[0].lower()
            freq[key] = int(parts[1])
            display[key] = parts[0]
            if len(parts) > 3 and parts[3]:
                lemma[key] = parts[3].lower()
    return rows, freq, lemma, display


def load_paradigms(lang_dir):
    """base_lower -> set(form_lower), plus the set of all attested base/headwords.

    Column 0 of any wiktionary_{verb,noun,adjekt/adjective}*.tsv is always the
    headword/base; every other non-empty cell in that row is one of its inflected
    forms. True for the plain word/form long format most packs use, for English's
    wider word/s_form/ing_form/... tables, and for German's own wide
    infinitiv/praes1/.../partizip2 table - one loader covers all of them.
    """
    forms = defaultdict(set)
    headword = set()
    patterns = [
        "wiktionary_verb*.tsv", "wiktionary_verben.tsv",
        "wiktionary_noun*.tsv", "wiktionary_nomen.tsv",
        "wiktionary_adjekt*.tsv", "wiktionary_adjective*.tsv",
    ]
    seen_files = set()
    for pat in patterns:
        for path in glob.glob(os.path.join(lang_dir, pat)):
            if path in seen_files:
                continue
            seen_files.add(path)
            with open(path, encoding="utf-8") as f:
                f.readline()  # header
                for line in f:
                    cells = line.rstrip("\n").split("\t")
                    if not cells or not cells[0].strip():
                        continue
                    base = cells[0].strip().lower()
                    headword.add(base)
                    for cell in cells[1:]:
                        cell = cell.strip()
                        if cell:
                            forms[base].add(cell.lower())
    return forms, headword


def find_cycles(lemma):
    """Functional graph (each word -> at most one lemma). Returns list of cycles,
    each a list of words in cyclic order (length 1 = self-link)."""
    color = {}  # 0/absent = unvisited, 1 = in current walk, 2 = done
    cycles = []
    for start in lemma:
        if color.get(start) == 2:
            continue
        path = []
        node = start
        while True:
            c = color.get(node, 0)
            if c == 2:
                for p in path:
                    color[p] = 2
                break
            if c == 1:
                idx = path.index(node)
                cycles.append(path[idx:])
                for p in path:
                    color[p] = 2
                break
            color[node] = 1
            path.append(node)
            if node not in lemma:
                for p in path:
                    color[p] = 2
                break
            node = lemma[node]
    return cycles


def decide_cycle_base(cycle, forms, headword):
    """Returns (base_or_None, tier_used: 'strong'/'weak'/'dropped')."""
    if len(cycle) == 1:
        return cycle[0], "strong"  # a self-link: itself is trivially its own "base" -> removed

    strong = [c for c in cycle if all(other in forms.get(c, ()) for other in cycle if other != c)]
    if len(strong) == 1:
        return strong[0], "strong"

    weak = [c for c in cycle if c in headword]
    if len(weak) == 1:
        return weak[0], "weak"

    return None, "dropped"


def repair(dict_path):
    lang_dir = os.path.dirname(os.path.abspath(dict_path))
    rows, freq, lemma, display = load_dict(dict_path)
    forms, headword = load_paradigms(lang_dir)

    cycles = find_cycles(lemma)
    stats = {"strong": 0, "weak": 0, "dropped": 0}
    dropped_samples = []
    decided_samples = []

    new_lemma = dict(lemma)  # working copy; cycle edges get redirected/removed here
    for cycle in cycles:
        base, tier = decide_cycle_base(cycle, forms, headword)
        stats[tier] += 1
        if base is None:
            for w in cycle:
                del new_lemma[w]
            dropped_samples.append([display[w] for w in cycle])
        else:
            for w in cycle:
                if w == base:
                    del new_lemma[w]
                else:
                    new_lemma[w] = base
            if len(cycle) > 1:
                decided_samples.append((tier, [display[w] for w in cycle], display[base]))

    # new_lemma is now acyclic (cycle edges resolved above) - flatten every remaining
    # chain onto its deepest root by plain pointer-chasing with memoisation.
    root_cache = {}

    def deepest_root(word):
        if word not in new_lemma:
            return word
        if word in root_cache:
            return root_cache[word]
        seen = [word]
        current = new_lemma[word]
        while current in new_lemma:
            seen.append(current)
            current = new_lemma[current]
        for w in seen:
            root_cache[w] = current
        return current

    final_lemma = {}
    for w in new_lemma:
        r = deepest_root(w)
        if r != w:
            final_lemma[w] = r

    changed = sum(1 for w in set(lemma) | set(final_lemma) if lemma.get(w) != final_lemma.get(w))

    return rows, display, lemma, final_lemma, {
        "cycles_found": len(cycles),
        "resolved_strong": stats["strong"],
        "resolved_weak": stats["weak"],
        "dropped": stats["dropped"],
        "rows_changed": changed,
    }, dropped_samples[:10], decided_samples[:10]


def write_dict(path, rows, display, final_lemma):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        for parts in rows:
            if len(parts) < 3:
                f.write("\t".join(parts) + "\n")
                continue
            key = parts[0].lower()
            target = final_lemma.get(key)
            base3 = parts[:3]
            if target is not None:
                f.write("\t".join(base3 + [display[target]]) + "\n")
            else:
                f.write("\t".join(base3) + "\n")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    apply = "--apply" in sys.argv[1:]
    if len(args) != 1:
        print("usage: python lemma_repair.py dict.tsv [--apply]")
        return 2

    dict_path = args[0]
    rows, display, lemma, final_lemma, stats, dropped_samples, decided_samples = repair(dict_path)

    print(f"{dict_path}: cycles found {stats['cycles_found']} "
          f"(strong {stats['resolved_strong']}, weak {stats['resolved_weak']}, "
          f"dropped {stats['dropped']}); rows with a lemma-column change: {stats['rows_changed']}")
    if decided_samples:
        print("  decided cycle samples (tier, members, chosen base):")
        for tier, members, base in decided_samples:
            print(f"    [{tier}] {members} -> {base}")
    if dropped_samples:
        print("  dropped (undecidable) cycle samples:")
        for members in dropped_samples:
            print(f"    {members}")

    if apply:
        write_dict(dict_path, rows, display, final_lemma)
        print(f"  APPLIED: {dict_path} rewritten.")
    else:
        print("  dry run only, nothing written (pass --apply to write).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
