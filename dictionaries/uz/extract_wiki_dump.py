# Extracts real word-frequency and bigram counts for Uzbek from the COMPLETE Uzbek Wikipedia XML dump
# (uzwiki-latest-pages-articles.xml.bz2, the single combined dump - per the same "full dump only" rule
# established for pt/it/nl/pl/tr/sr/ru/uk/az), verified directly before download: 309,775,381 bytes (~310MB,
# a small dump, similar scale to Azerbaijani's own).
#
# D-450-followup REAL FINDING, checked directly before writing this rather than assumed from the task
# description alone: Uzbek's modifier-letter apostrophe is not one character in practice but FIVE, used
# inconsistently throughout real Wikipedia text - counted directly in a 50MB sample of this very dump:
# U+02BB MODIFIER LETTER TURNED COMMA (231,133 - the correct Unicode character for "oʻ"/"gʻ"), plain ASCII
# U+0027 APOSTROPHE (211,260 - the single most common in practice, presumably keyboard convenience), U+02BC
# MODIFIER LETTER APOSTROPHE (30,841), U+2018 LEFT SINGLE QUOTATION MARK (5,653), U+2019 RIGHT SINGLE
# QUOTATION MARK (3,199). Two GENUINELY DIFFERENT real orthographic uses are tangled together here, confirmed
# by direct inspection of real Wiktionary headwords: U+02BB/its variants mark the "oʻ"/"gʻ" letter pair
# (og'ir/ogʻir/og‘ir - all the same real word, "heavy"), while U+02BC/its variants mark an UNRELATED glottal
# stop in Arabic/Persian loanwords after other consonants (sanʼat/san'at - "art", taʼminot/ta'minot -
# "supply") - visually easy to confuse, but not interchangeable in real orthography. Without normalising
# this, the same real word would fragment across up to five different spellings, each with its own tiny
# frequency count, silently wrecking this pack's own frequency ranking. `normalize_apostrophes()` below
# resolves every one of the five variant characters contextually: immediately after "o"/"g" (case-
# insensitive) it becomes the canonical U+02BB; anywhere else it becomes U+02BC (the glottal-stop marker) -
# a deliberate, documented judgement call (not every stray quotation mark in running prose is actually one of
# these two orthographic phenomena, but real word-internal apostrophe usage is common enough in this corpus,
# confirmed above, that this is the right default; residual quotation-mark contamination is diluted across a
# huge corpus and filtered by the existing frequency-threshold/noise-review step like any other extraction
# noise).
#
# Uzbek has no dotted/dotless-I-style casing quirk (unlike Turkish/Azerbaijani) - its Latin alphabet has only
# plain "i", no separate dotless letter - so plain `str.lower()` is safe here, checked directly.

import bz2
import multiprocessing as mp
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DUMP = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/7f0b100a-aed9-46ab-a0d8-5f2834369153"
    "/scratchpad/uzwiki/uzwiki-full.xml.bz2"
)
BASE = Path(__file__).parent
FREQ_OUT = BASE / "wiki_dump_freq.tsv"
BIGRAM_OUT = BASE / "wiki_dump_bigram.tsv"

APOSTROPHE_VARIANTS_RE = re.compile("[\u0027\u2018\u2019\u02bb\u02bc]")


def normalize_apostrophes(text: str) -> str:
    def repl(m: re.Match) -> str:
        idx = m.start()
        prev = text[idx - 1] if idx > 0 else ""
        return "\u02bb" if prev.lower() in ("o", "g") else "\u02bc"

    return APOSTROPHE_VARIANTS_RE.sub(repl, text)


UZBEK_LETTERS = set("abcdefghijklmnopqrstuvwxyz\u02bb\u02bc")
TOKEN_RE = re.compile(r"[a-z\u02bb\u02bc]+(?:-[a-z\u02bb\u02bc]+)*")
SINGLE_CHAR_OK = ("a", "u", "o")

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
FILE_IMAGE_RE = re.compile(r"\[\[(?:File|Rasm|Tasvir):[^\]]*\]\]", re.IGNORECASE | re.DOTALL)


def clean_wikitext(text: str) -> str:
    text = COMMENT_RE.sub(" ", text)
    text = FILE_IMAGE_RE.sub(" ", text)
    text = strip_templates_and_tables(text)
    text = REF_RE.sub(" ", text)
    text = HTML_TAG_RE.sub(" ", text)
    text = WIKILINK_PIPE_RE.sub(r"\1", text)
    text = EXTLINK_RE.sub(r"\1", text)
    # D-450-followup: BOLD_ITALIC_RE ('{2,5}) must run BEFORE apostrophe normalisation, not after - it
    # targets MediaWiki's own '' / ''' emphasis markup (runs of 2-5 plain ASCII apostrophes), which would
    # otherwise be corrupted into bogus ʻ/ʼ sequences by normalize_apostrophes() below.
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
    cleaned = normalize_apostrophes(cleaned)
    words = []
    bigrams = []
    prev = None
    for tok in TOKEN_RE.findall(cleaned.lower()):
        if any(ch not in UZBEK_LETTERS and ch != "-" for ch in tok):
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

    CHECKPOINT_EVERY = 10000
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
