# D-581 follow-up probe (read-only): (1) DE lowercase NOUN-only rows excluding the non-typable
# 333, (2) EN keep-class listing, (3) lemma-dependents of every planned removal on both sides -
# D-577's orphaned-lemma gate would fail a removal that other rows point at as their lemma.
import io

DE = "dictionaries/de/dict.tsv"
EN = "app/src/main/assets/en/dict.tsv"


def load(path):
    rows = []
    with io.open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if line:
                p = line.split("\t")
                rows.append((p[0], int(p[1]), p[2] if len(p) > 2 else "", p[3] if len(p) > 3 else ""))
    return rows


de = load(DE)
en = load(EN)

nontypable = {w for w, f_, p_, l_ in de if " " in w or "(" in w or ")" in w}
print("== DE lowercase NOUN-only EXCLUDING non-typable ==")
for w, f_, p_, l_ in sorted((r for r in de if r[2] == "NOUN" and r[0] and r[0][0].islower() and r[0] not in nontypable),
                            key=lambda r: -r[1]):
    print(f"   {w!r}\t{f_}\t{p_}\t{l_}")

print("== EN keep-class candidates ==")
for w, f_, p_, l_ in sorted((r for r in en if r[0].lower() in {"jpg", "www", "http", "https", "de"}), key=lambda r: r[0]):
    print(f"   {w!r}\t{f_}\t{p_}\t{l_}")

DE_REMOVE = ["nbsp", "lt", "amp", "reg", "copy", "deg", "para", "thinsp", "Trade", "Bull",
             "Divide", "dagger", "aßt", "buk", "wob", "gor"]
EN_REMOVE = ["nbsp", "GT", "Lt", "ndash", "mdash", "thinsp", "Reg", "deg", "middot", "amp"]

for name, rows, targets in (("DE", de, DE_REMOVE), ("EN", en, EN_REMOVE)):
    print(f"== {name} lemma-dependents of planned removals ==")
    tset = set(targets)
    tlower = {t.lower() for t in tset}
    for w, f_, p_, l_ in rows:
        if l_ and (l_ in tset or l_.lower() in tlower):
            print(f"   row {w!r}\t{f_}\t{p_}\t lemma={l_!r} -> would orphan")
    print("   (end)")
