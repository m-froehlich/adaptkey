# D-581 apply2 (D-570-apply2 precedent): the gate's D-581 base denylist caught 'apos' 2
# (NOUN,OTHER, lemma 'apo') - the &apos; entity fragment, the 24th survey hit, overlooked in the
# approved review list's EN removal set. Same true-markup class, amended and applied here.
# 'apo' 5 (Greek transliteration, not markup) deliberately stays.
import io

PATH = "app/src/main/assets/en/dict.tsv"

with io.open(PATH, encoding="utf-8", newline="") as f:
    lines = f.read().split("\n")
trailing = lines[-1] == ""
if trailing:
    lines = lines[:-1]

gone = [ln for ln in lines if ln.split("\t")[0] == "apos"]
assert len(gone) == 1, f"expected exactly 1 apos row, found {gone}"
out = [ln for ln in lines if ln.split("\t")[0] != "apos"]

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write("\n".join(out) + ("\n" if trailing else ""))
print(f"EN dict: {len(lines)} -> {len(out)} rows (removed {gone})")
