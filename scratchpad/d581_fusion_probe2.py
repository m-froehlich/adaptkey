# D-581: exact replication of FusionEvaluationTest's AGGRESSIVE-acceptance over common x common,
# under the NEW dict/bigram and under the OLD frequencies (de 13799, jpg 14825, HTTP 140 NOUN),
# to name the newly-accepted pairs and pin the cause. Read-only.
import io

CONNECTORS = "yxcvbnm"

with io.open("dictionaries/de/dict.tsv", encoding="utf-8") as f:
    rows = [ln.rstrip("\n").split("\t") for ln in f if ln.strip()]

new_freq = {}
for r in rows:
    new_freq.setdefault(r[0].lower(), int(r[1]))
OLD_OVERRIDE = {"de": 13799, "jpg": 14825}
old_freq = {w: OLD_OVERRIDE.get(w, fq) for w, fq in new_freq.items()}

with io.open("dictionaries/de/bigram.tsv", encoding="utf-8") as f:
    attested = set()
    for ln in f:
        p = ln.rstrip("\n").split("\t")
        attested.add((p[0].lower(), p[1].lower()))

common = []
for r in rows:
    w = r[0]
    if len(w) >= 2 and w.isalpha():
        common.append(w.lower())
        if len(common) == 300:
            break
print("window head:", common[:8])
print("'de' in window:", "de" in common, "| 'jpg' in window:", "jpg" in common)


def accepted(freqs):
    out = []
    for l in common:
        for r in common:
            for c in CONNECTORS:
                fq = freqs.get(l + c + r)
                if fq is None or fq < 10:
                    continue
                rarer = min(freqs.get(l, 0), freqs.get(r, 0))
                if rarer <= 0 or fq < rarer:
                    continue
                if (l, r) in attested:
                    continue
                out.append((l, r, c, l + c + r, fq, rarer))
    return out


new_acc = accepted(new_freq)
old_acc = accepted(old_freq)
print(f"AGGRESSIVE-accepted, NEW freqs: {len(new_acc)}")
for x in new_acc:
    print("   ", x)
print(f"AGGRESSIVE-accepted, OLD freqs: {len(old_acc)}")
for x in old_acc:
    print("   ", x)
print("newly accepted:", [x for x in new_acc if x not in old_acc])
