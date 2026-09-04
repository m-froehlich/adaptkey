# Extracts real word-frequency and bigram counts for Spanish from an actual Spanish Wikipedia XML dump
# part (eswiki-latest-pages-articles1.xml-p1p159400.bz2, the official first split, ~159,400 pages) -
# directly modelled on dictionaries/fr/extract_wiki_dump.py (D-441-followup, AdaptKey-History.md §415),
# with that round's own two real bugs (an iterparse memory leak, a too-slow per-character template
# stripper) and one observation artifact (stdout buffering under redirection) already fixed from the
# start rather than rediscovered - see that file's own comments for the original failure shapes.

import bz2
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DUMP = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/e35c1f5f-9365-4bf5-974b-7201ef965ed1"
    "/scratchpad/eswiki/eswiki-p1.xml.bz2"
)
BASE = Path(__file__).parent
FREQ_OUT = BASE / "wiki_dump_freq.tsv"
BIGRAM_OUT = BASE / "wiki_dump_bigram.tsv"

SPANISH_LETTERS = set("abcdefghijklmnopqrstuvwxyzáéíóúüñ")
TOKEN_RE = re.compile(r"[a-záéíóúüñ]+(?:-[a-záéíóúüñ]+)*|['’]", re.IGNORECASE)
APOSTROPHES = ("'", "\u2019")

MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"


TEMPLATE_INNERMOST_RE = re.compile(r"\{\{[^{}]*\}\}")
TABLE_INNERMOST_RE = re.compile(r"\{\|[^{}]*?\|\}", re.DOTALL)


def strip_templates_and_tables(text, max_iterations=50):
    """Removes every {{template}}/{|table|} span, including nested ones (MediaWiki markup) - repeatedly
    removes whatever currently has no further braces inside (the innermost span), converging to the fully
    stripped text in one pass per nesting level. Regex-driven (C-speed re.sub, bounded-repetition patterns
    with no backtracking risk) rather than a manual per-character Python scan - dictionaries/fr's own first
    attempt at this used such a scan and it proved too slow at real dump scale (a single 20,000-page
    diagnostic slice did not even finish in 180s), so this round starts directly with the regex approach."""
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
FILE_IMAGE_RE = re.compile(r"\[\[(?:File|Archivo|Imagen|Image):[^\]]*\]\]", re.IGNORECASE | re.DOTALL)


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
            # iterparse keeps every finished element as a child of the streamed root - elem.clear() alone
            # only empties the page element's own text/children, it stays parented under root until root
            # is cleared too, which is what actually frees the memory. dictionaries/fr's own first version
            # of this script called only elem.clear() and exceeded 2GB RSS before the first 20,000-page
            # checkpoint (AdaptKey-History.md §415) - this round calls root.clear() from the very start.
            root.clear()


def main():
    word_counts = Counter()
    bigram_counts = Counter()

    # This machine had ~6.3GB free RAM measured live just before this run (Get-CimInstance
    # Win32_OperatingSystem), more headroom than the French round's own ~4.5GB - but deliberately not
    # scaled up proportionally-to-the-edge: French's fixed extractor peaked at ~3.3GB resident for 80,000
    # pages (136.8M tokens), confirming sublinear (Heaps' law) growth with real margin to spare at that
    # cap. A modest, conservative increase over that already-proven-safe value, well short of both a
    # naive linear scale-up and this dump part's own full 159,400 pages, checkpointing every 5,000 pages
    # so a safety stop never loses already-accumulated real corpus data.
    PAGE_CAP = 100000
    CHECKPOINT_EVERY = 5000
    MIN_BIGRAM = 3

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
    for raw_text in iter_page_texts(DUMP):
        pages += 1
        cleaned = clean_wikitext(raw_text)
        prev = None
        for raw_tok in TOKEN_RE.findall(cleaned.lower()):
            if raw_tok in APOSTROPHES:
                prev = None
                continue
            tok = raw_tok
            if any(ch not in SPANISH_LETTERS and ch != "-" for ch in tok):
                prev = None
                continue
            if len(tok) == 1 and tok not in ("a", "e", "o", "u", "y"):
                prev = None
                continue
            total_tokens += 1
            word_counts[tok] += 1
            if prev is not None:
                bigram_counts[(prev, tok)] += 1
            prev = tok
        if pages % CHECKPOINT_EVERY == 0:
            kept_n = write_outputs()
            print(
                f"{pages} pages, {total_tokens} tokens, {len(word_counts)} distinct words, "
                f"{kept_n} bigram rows saved so far",
                flush=True,
            )
        if pages >= PAGE_CAP:
            print(f"Reached PAGE_CAP={PAGE_CAP}, stopping here by design (not the full dump part).")
            break

    kept_n = write_outputs()
    print(f"Done: {pages} pages, {total_tokens} tokens, {len(word_counts)} distinct words, {kept_n} bigram rows (>= {MIN_BIGRAM})")


if __name__ == "__main__":
    main()
