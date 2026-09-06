# Extracts real word-frequency and bigram counts for Azerbaijani from the COMPLETE Azerbaijani Wikipedia XML
# dump (azwiki-latest-pages-articles.xml.bz2, the single combined dump - per the same "full dump only" rule
# established for pt/it/nl/pl/tr/sr/ru/uk), verified directly before download: 323,312,325 bytes (~323MB,
# a small dump, similar scale to Bosnian/Croatian's own).
#
# D-450-followup REAL CASING FIX, applied proactively (not found the hard way) - Azerbaijani shares Turkish's
# own dotted/dotless İ/I Unicode SpecialCasing rule (confirmed: both "tr" and "az" locales are listed
# together in Unicode's own SpecialCasing.txt): ordinary Python `str.lower()` maps ASCII "I" to dotted "i",
# but Azerbaijani (like Turkish) requires "I" -> dotless "ı". `azerbaijani_lower()` below is Turkish's own
# `turkish_lower()` (dictionaries/tr/extract_wiki_dump.py) applied unchanged, since the mapping itself really
# is identical - verified directly against Unicode's own SpecialCasing.txt rather than assumed from the
# language family alone.

import bz2
import multiprocessing as mp
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DUMP = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/7f0b100a-aed9-46ab-a0d8-5f2834369153"
    "/scratchpad/azwiki/azwiki-full.xml.bz2"
)
BASE = Path(__file__).parent
FREQ_OUT = BASE / "wiki_dump_freq.tsv"
BIGRAM_OUT = BASE / "wiki_dump_bigram.tsv"

AZERBAIJANI_LOWER_MAP = str.maketrans({"İ": "i", "I": "ı"})


def azerbaijani_lower(s: str) -> str:
    return s.translate(AZERBAIJANI_LOWER_MAP).lower()


AZERBAIJANI_LETTERS = set("abcçdeəfgğhxıijkqlmnoöprsştuüvyz")
TOKEN_RE = re.compile(r"[abcçdeəfgğhxıijkqlmnoöprsştuüvyz]+(?:-[abcçdeəfgğhxıijkqlmnoöprsştuüvyz]+)*", re.UNICODE)
SINGLE_CHAR_OK = ("o", "e", "ə", "u")

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
FILE_IMAGE_RE = re.compile(r"\[\[(?:File|Şəkil|Fayl):[^\]]*\]\]", re.IGNORECASE | re.DOTALL)


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
    for tok in TOKEN_RE.findall(azerbaijani_lower(cleaned)):
        if any(ch not in AZERBAIJANI_LETTERS and ch != "-" for ch in tok):
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
