# Extracts real word-frequency and bigram counts for Portuguese from the COMPLETE Portuguese Wikipedia XML
# dump (ptwiki-latest-pages-articles.xml.bz2, the single combined dump - not a capped first split) - per
# explicit user instruction that a page-capped run (the French/Spanish precedent, D-441/D-443) is not
# acceptable for this round; the whole dump must be processed. Directly modelled on
# dictionaries/fr/extract_wiki_dump.py/dictionaries/es/extract_wiki_dump.py (see their own comments for the
# three real pitfalls this script also guards against: an iterparse memory leak fixed via root.clear() after
# every page, a too-slow per-character template stripper replaced with the regex-driven iterative version,
# and python -u for a backgrounded run so a fully-buffered redirected stdout never reads as stuck) - with two
# real additions needed at full-dump scale (millions of pages, not tens of thousands), both genuinely new
# compared to the capped fr/es reference:
#  1. Multiprocessing (this machine has 6 physical / 12 logical cores) - the per-page wikitext-cleaning and
#     tokenising step is real CPU work (regex-heavy), and running it single-threaded would make a full-dump
#     run take many hours longer than necessary. The main process streams pages via iterparse (I/O-bound,
#     stays single-threaded - iterparse's own state cannot be shared across processes) and hands each page's
#     raw text to a worker pool via multiprocessing.Pool.imap; only the final per-page (word list, bigram
#     list) result is merged into the accumulating Counters in the main process.
#  2. Periodic hapax pruning - an uncapped run's word/bigram Counters would otherwise grow without bound
#     (Wikipedia at full scale has millions of distinct tokens/pairs, the overwhelming majority appearing
#     exactly once). Every checkpoint, any entry with count == 1 is dropped once the Counter has grown past a
#     size threshold - a standard streaming-frequency technique, and safe for this project's own purposes: a
#     word/bigram that is genuinely common reappears many times throughout the corpus and keeps accumulating
#     regardless of an early hapax instance being pruned away; UNRECOGNISED_MIN_COUNT (merge_dict.py, >=20)
#     already discards anything this rare anyway.
# Streams the bz2-compressed XML directly rather than decompressing to disk first.

import bz2
import multiprocessing as mp
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DUMP = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/e10e4699-5c3d-4af2-afff-911642f35e8c"
    "/scratchpad/ptwiki/ptwiki-full.xml.bz2"
)
BASE = Path(__file__).parent
FREQ_OUT = BASE / "wiki_dump_freq.tsv"
BIGRAM_OUT = BASE / "wiki_dump_bigram.tsv"

# Portuguese alphabet plus its real diacritic variants (tilde nasalisation ã/õ, acute á/é/í/ó/ú, grave à,
# circumflex â/ê/ô, cedilla ç).
PORTUGUESE_LETTERS = set("abcdefghijklmnopqrstuvwxyzãõáéíóúàâêôç")
TOKEN_RE = re.compile(r"[a-zãõáéíóúàâêôç]+(?:-[a-zãõáéíóúàâêôç]+)*|['’]", re.IGNORECASE)
APOSTROPHES = ("'", "\u2019")
SINGLE_CHAR_OK = ("a", "e", "o", "é", "à")

MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"


TEMPLATE_INNERMOST_RE = re.compile(r"\{\{[^{}]*\}\}")
TABLE_INNERMOST_RE = re.compile(r"\{\|[^{}]*?\|\}", re.DOTALL)


def strip_templates_and_tables(text, max_iterations=50):
    """Removes every {{template}}/{|table|} span, including nested ones (MediaWiki markup) - repeatedly
    removes whatever currently has no further braces inside (the innermost span), converging to the fully
    stripped text in one pass per nesting level. Regex-driven (C-speed re.sub), not a manual per-character
    scan - see dictionaries/fr/extract_wiki_dump.py's own comment for why that approach was confirmed too
    slow at real dump scale."""
    for _ in range(max_iterations):
        new_text = TEMPLATE_INNERMOST_RE.sub(" ", text)
        new_text = TABLE_INNERMOST_RE.sub(" ", new_text)
        if new_text == text:
            break
        text = new_text
    return text


REF_RE = re.compile(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", re.DOTALL | re.IGNORECASE)
COMMENT_RE = re.compile(r"<!--.*?-->", re.DOTALL)
HTML_TAG_RE = re.compile(r"<[^>]+>")
WIKILINK_PIPE_RE = re.compile(r"\[\[(?:[^\[\]|]*\|)?([^\[\]|]*)\]\]")
EXTLINK_RE = re.compile(r"\[https?://[^\s\]]+\s*([^\]]*)\]")
BOLD_ITALIC_RE = re.compile(r"'{2,5}")
HEADER_RE = re.compile(r"^=+\s*(.*?)\s*=+$", re.MULTILINE)
FILE_IMAGE_RE = re.compile(r"\[\[(?:File|Ficheiro|Imagem):[^\]]*\]\]", re.IGNORECASE | re.DOTALL)


def clean_wikitext(text: str) -> str:
    text = COMMENT_RE.sub(" ", text)
    text = FILE_IMAGE_RE.sub(" ", text)
    text = strip_templates_and_tables(text)
    text = REF_RE.sub(" ", text)
    text = HTML_TAG_RE.sub(" ", text)
    text = WIKILINK_PIPE_RE.sub(r"\1", text)
    text = EXTLINK_RE.sub(r"\1", text)
    text = BOLD_ITALIC_RE.sub("", text)
    text = HEADER_RE.sub(r"\1", text)
    text = text.replace("|", " ")
    return text


def iter_page_texts(dump_path):
    with bz2.BZ2File(dump_path, "rb") as f:
        context = iter(ET.iterparse(f, events=("start", "end")))
        _, root = next(context)
        for event, elem in context:
            if event != "end" or elem.tag != MW_NS + "page":
                continue
            ns_elem = elem.find(MW_NS + "ns")
            redirect_elem = elem.find(MW_NS + "redirect")
            if ns_elem is not None and ns_elem.text == "0" and redirect_elem is None:
                rev = elem.find(MW_NS + "revision")
                if rev is not None:
                    text_elem = rev.find(MW_NS + "text")
                    if text_elem is not None and text_elem.text:
                        yield text_elem.text
            # Clearing the root after every page (not merely the page element itself) is what actually frees
            # the memory - see dictionaries/fr/extract_wiki_dump.py's own comment for the confirmed real leak
            # (>2GB RSS at just 20,000 pages) this guards against.
            root.clear()


def process_page(raw_text):
    """Runs in a worker process (see module docstring point 1): cleans and tokenises one page's raw
    wikitext, returning its own local word/bigram lists - the main process merges these into the
    accumulating Counters, keeping the CPU-heavy regex work spread across cores."""
    cleaned = clean_wikitext(raw_text)
    words = []
    bigrams = []
    prev = None
    for raw_tok in TOKEN_RE.findall(cleaned.lower()):
        if raw_tok in APOSTROPHES:
            prev = None
            continue
        tok = raw_tok
        if any(ch not in PORTUGUESE_LETTERS and ch != "-" for ch in tok):
            prev = None
            continue
        if len(tok) == 1 and tok not in SINGLE_CHAR_OK:
            prev = None
            continue
        words.append(tok)
        if prev is not None:
            bigrams.append((prev, tok))
        prev = tok
    return words, bigrams


def main():
    word_counts = Counter()
    bigram_counts = Counter()

    CHECKPOINT_EVERY = 20000
    MIN_BIGRAM = 3
    PRUNE_TRIGGER_WORDS = 4_000_000
    PRUNE_TRIGGER_BIGRAMS = 8_000_000
    NUM_WORKERS = 5

    def prune_hapax():
        nonlocal word_counts, bigram_counts
        if len(word_counts) > PRUNE_TRIGGER_WORDS:
            before = len(word_counts)
            word_counts = Counter({w: c for w, c in word_counts.items() if c > 1})
            print(f"  pruned word_counts hapax: {before} -> {len(word_counts)}")
        if len(bigram_counts) > PRUNE_TRIGGER_BIGRAMS:
            before = len(bigram_counts)
            bigram_counts = Counter({k: c for k, c in bigram_counts.items() if c > 1})
            print(f"  pruned bigram_counts hapax: {before} -> {len(bigram_counts)}")

    def write_outputs():
        with FREQ_OUT.open("w", encoding="utf-8", newline="\n") as out_f:
            for word, count in word_counts.most_common():
                out_f.write(f"{word}\t{count}\n")
        kept = [(a, b, c) for (a, b), c in bigram_counts.items() if c >= MIN_BIGRAM]
        kept.sort(key=lambda row: -row[2])
        with BIGRAM_OUT.open("w", encoding="utf-8", newline="\n") as out_f:
            for a, b, c in kept:
                out_f.write(f"{a}\t{b}\t{c}\n")
        return len(kept)

    pages = 0
    total_tokens = 0
    with mp.Pool(processes=NUM_WORKERS) as pool:
        for words, bigrams in pool.imap(process_page, iter_page_texts(DUMP), chunksize=100):
            pages += 1
            word_counts.update(words)
            bigram_counts.update(bigrams)
            total_tokens += len(words)
            if pages % CHECKPOINT_EVERY == 0:
                prune_hapax()
                kept_n = write_outputs()
                print(
                    f"{pages} pages, {total_tokens} tokens, {len(word_counts)} distinct words, "
                    f"{kept_n} bigram rows saved so far"
                )

    kept_n = write_outputs()
    print(f"Done: {pages} pages, {total_tokens} tokens, {len(word_counts)} distinct words, {kept_n} bigram rows (>= {MIN_BIGRAM})")


if __name__ == "__main__":
    main()
