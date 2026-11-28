# D-581 (Markup Artefact Sweep DE+EN) apply script - runs the APPROVED review list
# (scratchpad/d581_review.md, approved 2026-11-28: Option A = amp/amping/amped stay).
# Everything is computed in memory and asserted BEFORE any file is written; a failed
# assertion aborts with no write (the D-568/D-569 round precedent). Row order is preserved.
import io
import sys

DE_DICT = "dictionaries/de/dict.tsv"
DE_BIGRAM = "dictionaries/de/bigram.tsv"
EN_DICT = "app/src/main/assets/en/dict.tsv"
EN_BIGRAM = "app/src/main/assets/en/bigram.tsv"

DE_REMOVE = {
    "nbsp", "thinsp", "lt", "reg", "amp", "para", "copy", "dagger", "deg",
    "Trade", "Bull", "Bulls", "Divide",          # A1 markup/entity class (incl. Bulls: lemma link)
    "aßt", "buk", "wob", "gor",                  # A3 archaic rest (D-473-followup: removal)
    "til",                                       # A4 EN leakage, lowercase NOUN
}
DE_RENAME = {"text": "Text", "teleskopmasten": "Teleskopmasten"}     # A5 casing hygiene
DE_KEEP = {"jpg": ("200", None), "de": ("200", None),                # A6 freq <= ~200, OTHER
           "HTTP": (None, "http"), "HTTPS": (None, "https")}         # HTTP/HTTPS: surface lower + OTHER
DE_EXPECT_REMOVE = 351  # 13 entity + 333 non-typable + 4 archaic + 1 til

EN_REMOVE = {"nbsp", "nbsps", "GT", "Lt", "ndash", "mdash", "thinsp", "Reg", "deg", "degs",
             "middot", "middots"}                        # C1 (+C2: amp family approved to STAY)
EN_EXPECT_REMOVE = 12


def nontypable(word):
    return " " in word or "(" in word or ")" in word or "\u00a0" in word


def load(path):
    with io.open(path, encoding="utf-8", newline="") as f:
        lines = f.read().split("\n")
    trailing = lines[-1] == "" if lines else False
    if trailing:
        lines = lines[:-1]
    return lines, trailing


def save(path, lines, trailing):
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write("\n".join(lines) + ("\n" if trailing else ""))


def apply_dict(path, remove, renames, keep, expect_remove, label):
    lines, trailing = load(path)
    removed, renamed, kept = [], [], []
    out = []
    for line in lines:
        parts = line.split("\t")
        word = parts[0]
        if word in remove or nontypable(word):
            removed.append(line)
            continue
        if word in renames:
            parts[0] = renames[word]
            renamed.append((word, parts[0]))
            out.append("\t".join(parts))
            continue
        if word in keep:
            new_freq, new_word = keep[word]
            if new_freq is not None:
                parts[1] = new_freq
            if new_word is not None:
                parts[0] = new_word
                parts[2] = "OTHER"
            kept.append((word, parts[0], parts[1], parts[2]))
            out.append("\t".join(parts))
            continue
        out.append(line)
    assert len(removed) == expect_remove, (
        f"{label}: expected {expect_remove} removals, computed {len(removed)}")
    assert len(renamed) == len(renames), (
        f"{label}: expected {len(renames)} renames, computed {len(renamed)}: {renamed}")
    assert len(kept) == len(keep), (
        f"{label}: expected {len(keep)} keep-class edits, computed {len(kept)}: {kept}")
    print(f"{label}: {len(lines)} -> {len(out)} rows (removed {len(removed)}, "
          f"renamed {renamed}, keep-class {kept})")
    return out, trailing, [r.split("\t")[0] for r in removed]


def apply_bigram(path, removed_words, label):
    lines, trailing = load(path)
    gone = [ln for ln in lines if ln and (ln.split("\t")[0] in removed_words
                                          or ln.split("\t")[1] in removed_words)]
    out = [ln for ln in lines if not (ln and (ln.split("\t")[0] in removed_words
                                              or ln.split("\t")[1] in removed_words))]
    print(f"{label}: {len(lines)} -> {len(out)} rows (removed {len(gone)})")
    return out, trailing, gone


# ---- DE ----
de_out, de_trail, de_removed = apply_dict(DE_DICT, DE_REMOVE, DE_RENAME, DE_KEEP,
                                          DE_EXPECT_REMOVE, "DE dict")
de_bg_out, de_bg_trail, de_bg_gone = apply_bigram(DE_BIGRAM, set(de_removed), "DE bigram")

# ---- EN ----
en_out, en_trail, en_removed = apply_dict(EN_DICT, EN_REMOVE, {}, {}, EN_EXPECT_REMOVE, "EN dict")
en_bg_out, en_bg_trail, en_bg_gone = apply_bigram(EN_BIGRAM, set(en_removed), "EN bigram")

# ---- writes (only reached when every assertion above held) ----
save(DE_DICT, de_out, de_trail)
save(DE_BIGRAM, de_bg_out, de_bg_trail)
save(EN_DICT, en_out, en_trail)
save(EN_BIGRAM, en_bg_out, en_bg_trail)
print(f"WRITTEN. bigram removals: de={len(de_bg_gone)} en={len(en_bg_gone)}")
