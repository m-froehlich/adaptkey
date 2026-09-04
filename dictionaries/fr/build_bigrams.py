# Builds bigram.tsv for the French language pack from real French Wikipedia article text (the same
# source German/Greek's own bundled dictionaries were built from, per the Language Contribution
# Guide's step 0) - fetched live via the public MediaWiki API (random-article batches, plaintext
# extracts), tokenised, and counted. No script for this step existed in the repo before this round
# (a known, documented gap - see the guide's step 0); this one is scoped narrowly to French.

import json
import re
import time
import urllib.request
from collections import Counter
from pathlib import Path

BASE = Path(__file__).parent
CORPUS_CACHE = BASE / "wiki_corpus_raw.txt"
OUT = BASE / "bigram.tsv"

API = "https://fr.wikipedia.org/w/api.php"
BATCHES = 700
PER_BATCH = 20
DELAY_SECONDS = 1.2

FRENCH_LETTERS = set("abcdefghijklmnopqrstuvwxyzàâäéèêëîïôöùûüÿçœæ")
TOKEN_RE = re.compile(r"[a-zàâäéèêëîïôöùûüÿçœæ]+(?:-[a-zàâäéèêëîïôöùûüÿçœæ]+)*|['’]", re.IGNORECASE)


def fetch_batch(offset_seed):
    url = (
        f"{API}?action=query&format=json&generator=random&grnnamespace=0&grnlimit={PER_BATCH}"
        f"&prop=extracts&explaintext=1&exsectionformat=plain"
    )
    req = urllib.request.Request(url, headers={"User-Agent": "AdaptKey-dictionary-builder/1.0"})
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    pages = data.get("query", {}).get("pages", {})
    return [p.get("extract", "") for p in pages.values()]


def main():
    if CORPUS_CACHE.exists():
        text = CORPUS_CACHE.read_text(encoding="utf-8")
        print(f"Using cached corpus ({len(text)} chars)")
    else:
        chunks = []
        consecutive_failures = 0
        for i in range(BATCHES):
            try:
                extracts = fetch_batch(i)
                chunks.extend(extracts)
                consecutive_failures = 0
                time.sleep(DELAY_SECONDS)
            except Exception as e:
                consecutive_failures += 1
                backoff = min(30, 2 * consecutive_failures)
                print(f"batch {i} failed: {e} - backing off {backoff}s")
                time.sleep(backoff)
            if i % 10 == 0:
                print(f"batch {i}/{BATCHES}, chunks so far: {len(chunks)}, chars so far: {sum(len(c) for c in chunks)}")
                # periodic save so a later interruption doesn't lose progress
                CORPUS_CACHE.write_text("\n".join(chunks), encoding="utf-8")
        text = "\n".join(chunks)
        CORPUS_CACHE.write_text(text, encoding="utf-8")
        print(f"Fetched corpus: {len(text)} chars from {len(chunks)} articles")

    word_counts = Counter()
    bigram_counts = Counter()
    prev = None
    total_tokens = 0
    for raw_tok in TOKEN_RE.findall(text.lower()):
        if raw_tok in ("'", "’"):
            # elision marker - breaks the bigram chain (matches dict.tsv's own no-apostrophe-tokens
            # convention), rather than fabricating a false adjacency across it.
            prev = None
            continue
        tok = raw_tok
        if any(ch not in FRENCH_LETTERS and ch != "-" for ch in tok):
            prev = None
            continue
        if len(tok) == 1 and tok not in ("a", "à", "y", "ô"):
            prev = None
            continue
        total_tokens += 1
        word_counts[tok] += 1
        if prev is not None:
            bigram_counts[(prev, tok)] += 1
        prev = tok

    print(f"Total tokens: {total_tokens}, distinct words: {len(word_counts)}, distinct bigrams: {len(bigram_counts)}")

    # Keep bigrams with a real, non-trivial count only - a count-1 bigram is corpus noise more often
    # than a genuine, reusable next-word pairing at this corpus size.
    MIN_COUNT = 3
    kept = [(a, b, c) for (a, b), c in bigram_counts.items() if c >= MIN_COUNT]
    kept.sort(key=lambda row: -row[2])

    with OUT.open("w", encoding="utf-8", newline="\n") as f:
        for a, b, c in kept:
            f.write(f"{a}\t{b}\t{c}\n")

    print(f"Wrote {len(kept)} bigram rows (>= {MIN_COUNT} occurrences) to {OUT}")


if __name__ == "__main__":
    main()
