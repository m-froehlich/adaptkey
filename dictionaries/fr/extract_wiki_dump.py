# Extracts real word-frequency and bigram counts for French from an actual French Wikipedia XML dump
# part (frwiki-latest-pages-articles1.xml-p1p306134.bz2, ~306,000 articles, the first official dump
# split) - replaces the earlier small live-API random-article sample with the real corpus source the
# Language Contribution Guide's own step 0 calls for. No such extractor previously existed in this repo
# (a documented gap - see the guide's step 0 and AdaptKey-Progress.md's D-280 note); this one is scoped
# to French only. Streams the bz2-compressed XML directly rather than decompressing to disk first (the
# dump is hundreds of MB compressed, several GB as plain XML).

import bz2
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DUMP = Path(
    "C:/Users/MFROEH~1/AppData/Local/Temp/claude/D--workspace-ai/cd41590c-250a-4e4a-9ae8-b577b565ce36"
    "/scratchpad/frwiki/frwiki-p1.xml.bz2"
)
BASE = Path(__file__).parent
FREQ_OUT = BASE / "wiki_dump_freq.tsv"
BIGRAM_OUT = BASE / "wiki_dump_bigram.tsv"

FRENCH_LETTERS = set("abcdefghijklmnopqrstuvwxyzàâäéèêëîïôöùûüÿçœæ")
TOKEN_RE = re.compile(r"[a-zàâäéèêëîïôöùûüÿçœæ]+(?:-[a-zàâäéèêëîïôöùûüÿçœæ]+)*|['’]", re.IGNORECASE)
APOSTROPHES = ("'", "\u2019")

MW_NS = "{http://www.mediawiki.org/xml/export-0.11/}"


TEMPLATE_INNERMOST_RE = re.compile(r"\{\{[^{}]*\}\}")
TABLE_INNERMOST_RE = re.compile(r"\{\|[^{}]*?\|\}", re.DOTALL)


def strip_templates_and_tables(text, max_iterations=50):
    """Removes every {{template}}/{|table|} span, including nested ones (MediaWiki markup) - repeatedly
    removes whatever currently has no further braces inside (the innermost span), converging to the fully
    stripped text in one pass per nesting level. Regex-driven (C-speed re.sub, bounded-repetition patterns
    with no backtracking risk) rather than a manual per-character Python scan, which is what the first,
    much slower version of this script used - confirmed too slow at real dump scale (a single 20,000-page
    diagnostic slice did not even finish in 180s) before being replaced with this approach."""
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
FILE_IMAGE_RE = re.compile(r"\[\[(?:File|Fichier|Image):[^\]]*\]\]", re.IGNORECASE | re.DOTALL)


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
            # iterparse keeps every finished element as a child of the streamed root - without this,
            # memory grows for the whole file's duration (confirmed the hard way: the first run of this
            # script exceeded 2GB RSS before ever reaching the first 20,000-page progress checkpoint).
            # Clearing the root after every page (not merely the page element itself) drops that
            # reference, which is what actually frees the memory - `elem.clear()` alone only empties the
            # page element's own text/children, it stays parented under root until root is cleared too.
            root.clear()


def main():
    word_counts = Counter()
    bigram_counts = Counter()

    # This machine has 16GB RAM with only ~4.5GB free once the rest of this session's own work is
    # accounted for - the first, uncapped attempt reached ~2.1GB resident at just 20,000 of the part's
    # 306,134 pages, real growth but not sublinear enough yet to safely extrapolate to the full part.
    # Capped well short of the full file, and checkpointed to disk periodically (not only at the very
    # end), so a memory-safety stop never loses already-accumulated real corpus data.
    PAGE_CAP = 80000
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
        if pages % CHECKPOINT_EVERY == 0:
            kept_n = write_outputs()
            print(
                f"{pages} pages, {total_tokens} tokens, {len(word_counts)} distinct words, "
                f"{kept_n} bigram rows saved so far"
            )
        if pages >= PAGE_CAP:
            print(f"Reached PAGE_CAP={PAGE_CAP}, stopping here by design (not the full dump part).")
            break

    kept_n = write_outputs()
    print(f"Done: {pages} pages, {total_tokens} tokens, {len(word_counts)} distinct words, {kept_n} bigram rows (>= {MIN_BIGRAM})")


if __name__ == "__main__":
    main()
