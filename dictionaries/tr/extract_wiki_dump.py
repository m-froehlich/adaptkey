# Extracts real word-frequency and bigram counts for Turkish from the COMPLETE Turkish Wikipedia XML dump
# (trwiki-latest-pages-articles.xml.bz2, the single combined dump - not a capped first split, per the same
# "full dump only" rule established for pt/it/nl/pl). Directly modelled on
# dictionaries/pl/extract_wiki_dump.py (see that file's own module docstring for the two real additions
# needed at full-dump scale: multiprocessing across this machine's 6 physical/12 logical cores for the
# CPU-heavy per-page cleaning/tokenising step, and periodic hapax pruning to keep the accumulating Counters
# bounded over millions of pages).
#
# D-449 REAL, TURKISH-SPECIFIC FIX (found before it could silently corrupt data, not after): ordinary
# Python `str.lower()` gets Turkish wrong for exactly one letter pair - Unicode's default casefolding maps
# ASCII "I" (U+0049) -> "i" (dotted, U+0069), but Turkish orthography requires "I" -> "ı" (DOTLESS, U+0131)
# and "İ" (DOTTED CAPITAL, U+0130) -> "i" (not the 2-character "i" + combining-dot-above Unicode default
# casefolding actually produces for U+0130). Confirmed the real, silent-corruption risk directly: Python's
# plain `"IŞIK".lower()` produces `"işik"` (wrong - the real word is "ışık", a Turkish word for "light") -
# a *different, still-plausible-looking* Turkish word, not an error, so this would corrupt frequency data
# silently rather than fail loudly. `turkish_lower()` below fixes both directions before deferring to the
# ordinary case-insensitive Unicode rules for every other letter (ç/ğ/ö/ş/ü have no such quirk).

import bz2
import multiprocessing as mp
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DUMP = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/e10e4699-5c3d-4af2-afff-911642f35e8c"
    "/scratchpad/trwiki/trwiki-full.xml.bz2"
)
BASE = Path(__file__).parent
FREQ_OUT = BASE / "wiki_dump_freq.tsv"
BIGRAM_OUT = BASE / "wiki_dump_bigram.tsv"

TURKISH_LOWER_MAP = str.maketrans({"İ": "i", "I": "ı"})


def turkish_lower(s: str) -> str:
    return s.translate(TURKISH_LOWER_MAP).lower()


# The real, closed 29-letter Turkish alphabet - deliberately excludes q/w/x (not native Turkish letters;
# their presence in a token usually signals a foreign loanword/proper noun, exactly the kind of contamination
# this project's other language extractors already filter out structurally).
TURKISH_LETTERS = set("abcçdefgğhıijklmnoöprsştuüvyz")
TOKEN_RE = re.compile(r"[a-zçğıiöşü]+(?:-[a-zçğıiöşü]+)*|['’]", re.IGNORECASE)
APOSTROPHES = ("'", "\u2019")
SINGLE_CHAR_OK = ("o",)

MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"


TEMPLATE_INNERMOST_RE = re.compile(r"\{\{[^{}]*\}\}")
TABLE_INNERMOST_RE = re.compile(r"\{\|[^{}]*?\|\}", re.DOTALL)


def strip_templates_and_tables(text, max_iterations=50):
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
FILE_IMAGE_RE = re.compile(r"\[\[(?:File|Dosya):[^\]]*\]\]", re.IGNORECASE | re.DOTALL)


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
            root.clear()


def process_page(raw_text):
    cleaned = clean_wikitext(raw_text)
    words = []
    bigrams = []
    prev = None
    for raw_tok in TOKEN_RE.findall(turkish_lower(cleaned)):
        if raw_tok in APOSTROPHES:
            prev = None
            continue
        tok = raw_tok
        if any(ch not in TURKISH_LETTERS and ch != "-" for ch in tok):
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
