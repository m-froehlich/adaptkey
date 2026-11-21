# -*- coding: utf-8 -*-
# D-568 (M2, english-dictionary-quality plan) REVIEW-LIST GENERATOR - does not modify anything.
#
# Finds the English dictionary's function-word flexion artefacts, the class behind "ands"
# (frequency 292,861, rank 5 - ahead of "to", "is", "was"): merge_wiktionary.py's plural/
# inflection-completion step ran over Wiktionary noun rows that should not exist, minting
# mechanical flexions of a closed class that has no flexions at all.
#
# Candidate rules (deliberately broad - the review list decides, not the script):
#   R1: the row's lemma (col 4) is itself a closed-class function word, OR
#   R2: the row's word is a function word + a mechanical inflection suffix (s/es/ed/d/ing/'s)
#       and carries no lemma link of its own.
# The function words themselves are NEVER candidates (they are the dictionary's backbone).

import io

EN = "app/src/main/assets/en/dict.tsv"

# Closed class only: articles, conjunctions, prepositions, pronouns, auxiliaries/copula,
# negation. Deliberately NOT content-homographs' own inflections ("downs" the hill noun,
# "runs", ...) - those are reviewed, not auto-removed.
FUNCTION_WORDS = {
    # articles
    "a", "an", "the",
    # coordinating / subordinating conjunctions
    "and", "or", "but", "nor", "so", "yet", "if", "because", "while", "than", "whether",
    # prepositions (single-word, closed set)
    "of", "in", "to", "for", "with", "on", "at", "by", "from", "as", "about", "into",
    "over", "after", "under", "between", "through", "during", "before", "against",
    "without", "within", "along", "across", "behind", "beyond", "off", "up", "down",
    "out", "near", "past", "since", "until", "till", "upon", "among", "around", "toward",
    "towards", "onto", "per", "via", "beneath", "beside", "besides", "despite", "except",
    "inside", "outside", "throughout", "underneath", "amid", "aboard", "atop", "lest",
    # pronouns / determiners
    "i", "me", "my", "mine", "myself", "you", "your", "yours", "yourself", "he", "him",
    "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "we", "us",
    "our", "ours", "ourselves", "they", "them", "their", "theirs", "themselves", "who",
    "whom", "whose", "which", "what", "this", "that", "these", "those",
    # auxiliaries / copula
    "be", "am", "is", "are", "was", "were", "been", "being", "have", "has", "had",
    "having", "do", "does", "did", "doing", "will", "would", "shall", "should", "can",
    "could", "may", "might", "must", "ought",
    # negation
    "not",
}

SUFFIXES = ("s", "es", "ed", "d", "ing", "'s")


def main():
    rows = []
    with io.open(EN, "r", encoding="utf-8", newline="") as f:
        for idx, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            word = parts[0]
            freq = int(parts[1])
            pos = parts[2] if len(parts) > 2 else ""
            lemma = parts[3] if len(parts) > 3 else ""
            rows.append((idx, word, freq, pos, lemma))

    fw_lower = {w.lower() for w in FUNCTION_WORDS}
    by_lower = {}
    for idx, word, freq, pos, lemma in rows:
        by_lower.setdefault(word.lower(), []).append((idx, word, freq, pos, lemma))

    candidates = []
    for idx, word, freq, pos, lemma in rows:
        low = word.lower()
        if low in fw_lower:
            continue  # the function word itself: backbone, never a candidate
        hit = None
        if lemma.lower() in fw_lower:
            hit = f"R1 lemma={lemma}"
        else:
            for suf in SUFFIXES:
                if low.endswith(suf) and len(low) - len(suf) >= 2:
                    stem = low[: -len(suf)]
                    if stem in fw_lower:
                        hit = f"R2 shape={stem}+{suf!r}"
                        break
        if hit:
            # Anything else in the dictionary linking TO this row would be orphaned by removal.
            dependents = [r for r in rows if len(r[4]) > 3 or (len(r) > 4)]
            dependents = [
                r for r in rows
                if len(r) > 4 and r[4] != "" and r[4].lower() == low
            ]
            dep = ", ".join(f"{d[1]}({d[2]})" for d in dependents[:3])
            candidates.append((idx, word, freq, pos, lemma, hit, dep))

    print(f"{len(candidates)} candidate rows (review list, nothing modified):\n")
    print("line#\tword\tfreq\trank-in-dict\tpos\tlemma\trule\tdependents")
    for idx, word, freq, pos, lemma, hit, dep in candidates:
        rank = 1 + sum(1 for r in rows if r[2] > freq)
        print(f"{idx}\t{word}\t{freq}\t{rank}\t{pos}\t{lemma}\t{hit}\t{dep or '-'}")


if __name__ == "__main__":
    main()
