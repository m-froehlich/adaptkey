# D-581 (Markup Artefact Sweep DE+EN) survey - READ-ONLY, writes no dictionary files.
# Collects every candidate row for the M1 review list: markup/entity artefacts, non-typable
# rows (whitespace/parens), lowercase NOUN-only rows (DE), the keep-lowered class
# (jpg/www/http/de), the archaic rest (ass/buk/wob/gor), and the bigram-side impact.
import io
import re
from collections import Counter

DE_DICT = "dictionaries/de/dict.tsv"
DE_BIGRAM = "dictionaries/de/bigram.tsv"
EN_DICT = "app/src/main/assets/en/dict.tsv"
EN_BIGRAM = "app/src/main/assets/en/bigram.tsv"

# Canonical HTML-entity/markup name candidates (matched case-insensitively against surfaces;
# per-row curation stays case-sensitive because e.g. EN "bull" is a real word, EN "Bull" is not).
ENTITY_NAMES = {
    "nbsp", "amp", "lt", "gt", "quot", "apos", "copy", "reg", "trade", "deg",
    "hellip", "mdash", "ndash", "ldquo", "rdquo", "lsquo", "rsquo", "sbquo",
    "bdquo", "laquo", "raquo", "times", "divide", "middot", "bull", "dagger",
    "sect", "para", "euro", "szlig", "auml", "ouml", "uuml", "aszlig",
    "thinsp", "ensp", "emsp", "zwnj", "zwj", "shy", "sup2", "sup3",
    "frac12", "frac14", "frac34", "nbsp", "cent", "pound", "yen",
}
KEEP_CLASS = {"jpg", "www", "http", "https", "de"}   # D-581 §4 decision: keep, lower to <=200, OTHER
ARCHAIC = {"aßt", "buk", "wob", "gor"}               # D-206 rest, D-473-followup policy: remove + denylist


def load(path):
    rows = []
    with io.open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t")
            word = parts[0]
            freq = int(parts[1])
            pos = parts[2] if len(parts) > 2 else ""
            lemma = parts[3] if len(parts) > 3 else ""
            rows.append((word, freq, pos, lemma))
    return rows


def survey(name, path, capitalises=False):
    rows = load(path)
    print(f"=== {name}: {len(rows)} rows ===")
    lower_index = {}
    for w, f_, p_, l_ in rows:
        lower_index.setdefault(w.lower(), []).append((w, f_, p_, l_))

    entity_hits = []
    for w, freq, pos, lemma in rows:
        if w.lower() in ENTITY_NAMES:
            entity_hits.append((w, freq, pos, lemma))
    entity_hits.sort(key=lambda r: -r[1])
    print(f"-- entity-name candidates (case-insensitive match): {len(entity_hits)}")
    for w, freq, pos, lemma in entity_hits:
        print(f"   {w!r}\t{freq}\t{pos}\t{lemma}")

    nontypable = [(w, f_, p_, l_) for w, f_, p_, l_ in rows
                  if " " in w or "(" in w or ")" in w or "\u00a0" in w]
    nontypable.sort(key=lambda r: -r[1])
    print(f"-- non-typable rows (space/paren/nbsp in surface): {len(nontypable)}")
    for w, freq, pos, lemma in nontypable[:40]:
        print(f"   {w!r}\t{freq}\t{pos}\t{lemma}")
    if len(nontypable) > 40:
        pos_counter = Counter(p_ for _, _, p_, _ in nontypable)
        print(f"   ... ({len(nontypable) - 40} more; POS mix: {dict(pos_counter)})")

    lower_noun = [(w, f_, p_, l_) for w, f_, p_, l_ in rows
                  if p_ == "NOUN" and w and w[0].islower()]
    lower_noun.sort(key=lambda r: -r[1])
    print(f"-- lowercase NOUN-only rows: {len(lower_noun)}")
    for w, freq, pos, lemma in lower_noun[:25]:
        twins = [t for t in lower_index.get(w.lower(), []) if t[0] != w]
        print(f"   {w!r}\t{freq}\t{pos}\t{lemma}\t twins={twins}")
    if len(lower_noun) > 25:
        print(f"   ... ({len(lower_noun) - 25} more)")

    keep = [(w, f_, p_, l_) for w, f_, p_, l_ in rows if w.lower() in KEEP_CLASS]
    print(f"-- keep-lowered class current state (target: freq<=200, OTHER):")
    for w, freq, pos, lemma in sorted(keep, key=lambda r: r[0]):
        print(f"   {w!r}\t{freq}\t{pos}\t{lemma}")

    arch = [(w, f_, p_, l_) for w, f_, p_, l_ in rows if w in ARCHAIC]
    print(f"-- archaic rest rows: {arch}")

    overlap = set()
    for w, f_, p_, l_ in entity_hits:
        overlap.add(w)
    overlap |= {w for w, _, _, _ in nontypable} | ARCHAIC
    return rows, overlap


def bigram_impact(name, path, removed):
    if not removed:
        return
    total = 0
    hits = 0
    samples = []
    nonalpha = 0
    with io.open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line:
                continue
            total += 1
            parts = line.split("\t")
            prev, word = parts[0], parts[1]
            if prev in removed or word in removed:
                hits += 1
                if len(samples) < 12:
                    samples.append(line)
            if re.search(r"[^\w]", prev) or re.search(r"[^\w]", word):
                nonalpha += 1
    print(f"-- {name}: {total} rows, {hits} rows touch a removed surface, {nonalpha} non-alpha-key rows")
    for s in samples:
        print(f"   {s}")


de_rows, de_removed = survey("DE " + DE_DICT, DE_DICT, capitalises=True)
en_rows, en_removed = survey("EN " + EN_DICT, EN_DICT)

print()
bigram_impact("DE bigram", DE_BIGRAM, de_removed)
bigram_impact("EN bigram", EN_BIGRAM, en_removed)

# hints.tsv artefact check
for h in ("dictionaries/de/hints.tsv", "app/src/main/assets/en/hints.tsv"):
    with io.open(h, encoding="utf-8") as f:
        content = f.read()
    found = [e for e in ENTITY_NAMES | KEEP_CLASS if re.search(r"(^|[=;])" + re.escape(e) + r"([=;]|$)", content)]
    print(f"-- hints {h}: artefact surfaces present: {found}")
