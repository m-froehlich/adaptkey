# D-581 test-failure analysis: replicate FusionEvaluationTest's `common` window (first 300
# letter-only rows) and the AGGRESSIVE fusion candidate rule (l + connector + r known), to name
# the pairs the sweep newly exposed. Read-only.
import io

CONNECTORS = "yxcvbnm"

with io.open("dictionaries/de/dict.tsv", encoding="utf-8") as f:
    rows = [ln.rstrip("\n").split("\t") for ln in f if ln.strip()]
words = [(r[0], int(r[1])) for r in rows]


def letter_only(w):
    return len(w) >= 2 and w.isalpha()


def window(with_nbsp):
    seq = [(w, fq) for w, fq in words if with_nbsp or w != "nbsp"]
    return [w.lower() for w, _ in [x for x in seq if letter_only(x[0])][:300]]


old = window(True)
new = window(False)
known = {}
for w, fq in words:
    known.setdefault(w.lower(), fq)

newcomers = [w for w in new if w not in old]
print("window newcomers:", newcomers)

fus = []
for i, l in enumerate(new):
    for r in new:
        for c in CONNECTORS:
            fused = l + c + r
            fq = known.get(fused)
            if fq is not None:
                fus.append((l, r, c, fused, fq))
print(f"fusable random-common pairs (any connector): {len(fus)}")
for l, r, c, fused, fq in fus:
    mark = " <-- involves newcomer" if l in newcomers or r in newcomers else ""
    print(f"   {l} + {c} + {r} = {fused} (freq {fq}){mark}")
