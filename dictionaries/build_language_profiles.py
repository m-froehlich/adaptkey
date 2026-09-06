# Builds A-03 character n-gram profiles (bi+trigrams, top 200, rank order) for a language directory's
# dict.tsv, in the exact format app/src/main/assets/language_profiles.tsv already uses ("<code>\t<ngram>",
# one line per ranked n-gram, most frequent first). The original builder (a throwaway scratchpad script that
# worked from a UDHR sentence corpus) is no longer in this repo; this one instead works from a language's own
# already-extracted dict.tsv word-frequency table, which by this project's D-450 round is a far bigger real
# corpus (a whole Wikipedia dump's worth of word frequencies) than the original small UDHR source ever was -
# treating each dictionary row as `freq` independent occurrences of that single word (wrapped in its own
# leading/trailing space, exactly as language.CharNgrams.normalize() would produce for an isolated word)
# reconstructs the same n-gram frequency distribution real running text would produce for every n-gram that
# does not straddle two different words - the dominant majority of any top-200 profile, since those are
# overwhelmingly a language's own most common short words and affixes.
#
# The normalization MUST stay byte-for-byte equivalent to language.CharNgrams.normalize()/rankedProfile():
# lowercase, every non-letter becomes a single collapsed space, trim, wrap with one leading/trailing space;
# ties in the top-200 cut broken by the n-gram ascending. See that class's own KDoc for the full contract.
#
# Usage: python build_language_profiles.py <lang-code> [<lang-code> ...]
#   Reads   <lang-code>/dict.tsv (relative to this script's own directory)
#   Writes  <lang-code>.profile.tsv (same directory) - append these into app/src/main/assets/language_profiles.tsv
#           by hand once reviewed, never overwriting the languages already present there.

import sys
import os
from collections import Counter

NGRAM_SIZES = (2, 3)
TOP_N = 200


def normalize(word):
    out = []
    prev_space = False
    for ch in word.lower():
        if ch.isalpha():
            out.append(ch)
            prev_space = False
        elif not prev_space:
            out.append(' ')
            prev_space = True
    trimmed = ''.join(out).strip()
    return f" {trimmed} " if trimmed else " "


def ngrams_of(text, n):
    return [text[i:i + n] for i in range(len(text) - n + 1)]


def build_profile(dict_path):
    counts = Counter()
    with open(dict_path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            word = parts[0]
            freq = int(parts[1])
            norm = normalize(word)
            for n in NGRAM_SIZES:
                for gram in ngrams_of(norm, n):
                    counts[gram] += freq
    ranked = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))
    return [gram for gram, _ in ranked[:TOP_N]]


def main(codes):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    for code in codes:
        dict_path = os.path.join(script_dir, code, "dict.tsv")
        if not os.path.isfile(dict_path):
            print(f"SKIP {code}: no dict.tsv at {dict_path}")
            continue
        profile = build_profile(dict_path)
        out_path = os.path.join(script_dir, f"{code}.profile.tsv")
        with open(out_path, "w", encoding="utf-8", newline="\n") as out:
            for gram in profile:
                out.write(f"{code}\t{gram}\n")
        print(f"{code}: {len(profile)} n-grams -> {out_path}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: python build_language_profiles.py <lang-code> [<lang-code> ...]")
        sys.exit(1)
    main(sys.argv[1:])
