# Build doc/books/AdaptKey-Topics.md: the D-number <-> Kurzbezeichner registry.
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scratchpad"))

from labels_a import LABELS as A
from labels_b import LABELS as B
from labels_c import LABELS as C, FOLLOWUPS
from labels_fix import FIXES

rows = (ROOT / "scratchpad/topics_extract.tsv").read_text(encoding="utf-8").split("\n")[1:]
tsv = {}
for line in rows:
    if not line.strip():
        continue
    num, src, title = line.split("\t", 2)
    tsv[int(num)] = title

labels = {}
labels.update(A)
labels.update(B)
labels.update(C)
labels.update(FIXES)

leftover = [n for n, lab in labels.items() if lab.startswith("TODO-")]
if leftover:
    sys.exit(f"unresolved labels: {leftover}")

missing = [n for n in tsv if n not in labels]
if missing:
    sys.exit(f"numbers without a label: {missing}")

EXTRA_TITLES = {
    559: "Trigger definition - what counts as auto-correction",
    560: "Generic FeatureTips mechanism + catalog",
    561: "FeatureTipStore persistence",
    562: "Bar integration - injection into showSuggestions()",
    563: "Tip lifetime (auto-hide after two commits)",
    564: "Chip design (violet, sparkle prefix)",
    565: "Reset button on the feature-overview page",
    566: "Tip strings (32 locales)",
}

def clean_title(t: str) -> str:
    t = t.strip()
    t = re.sub(r"^- \*\*", "", t)
    t = re.sub(r"^\*\*", "", t)
    t = re.sub(r"^\u2026\s*", "", t)
    t = re.sub(r"^D-\d{1,3}(-followup)?\b[ :.-]*", "", t)
    t = re.sub(r"^(?:[A-ZÄÖÜ](?:[a-zäöüß]|-)+)\s+device-confirmed[^\w]*", "", t)
    t = t.replace("**", "")
    t = re.sub(r"\s+", " ", t).strip(" -;:")
    if len(t) > 118:
        cut = t[:118].rsplit(" ", 1)[0]
        t = cut + " …"
    return t

# follow-up rows grouped under their base number
fu_by_base = {}
for key, val in FOLLOWUPS.items():
    label = val if isinstance(val, str) else val[0]
    m = re.search(r"D-(\d{1,3})", key)
    fu_by_base.setdefault(int(m.group(1)), []).append((key, label))

out = []
out.append("# AdaptKey — Themenliste: D-Nummern und Kurzbezeichner")
out.append("")
out.append("Decodertabelle für die Themen-Nummern, die History, Progress und Spec verwenden. Jedes Thema")
out.append("trägt hier einen kurzen deutschen **Kurzbezeichner**; in Chat und Dokumenten wird ein Thema deshalb")
out.append("immer als `D-xxx „Kurzbezeichner“` genannt, nie als bloße Nummer. Die dritte Spalte zitiert den")
out.append("Originaltitel aus den Büchern (gekürzt) — sie führt per Strg+F zur vollständigen Stelle.")
out.append("")
out.append("Konventionen:")
out.append("")
out.append("- Jede **neue** D-Nummer bekommt sofort hier eine Zeile mit Kurzbezeichner (gleicher Commit wie die")
out.append("  Docs-Runde). Die nächstfreie Nummer steht am Dateiende.")
out.append("- Zeilen mit ↳ sind benannte Nachfolger-Themen (`D-xxx-followup` und Freunde), die in den Büchern")
out.append("  einen Eigennamen tragen.")
out.append("- D-559–D-566 gehören zum Plan `feature-tips.md`; seine alten Platzhalter D-546–D-553 kollidierten")
out.append("  mit den inzwischen vergebenen jetzt-Runden (D-546–D-548) und D-554–D-558 und wurden am")
out.append("  2026-11-21 auf D-559 ff. umnummeriert.")
out.append("- D-290, D-291-Lücke: Nummern, die in keinem Buch auftauchen, sind hier absichtlich nicht aufgeführt.")
out.append("")

nums = sorted(set(tsv) | set(EXTRA_TITLES))
BLOCK = 60
for start in range(1, max(nums) + 1, BLOCK):
    end = start + BLOCK - 1
    block_nums = [n for n in nums if start <= n <= end]
    if not block_nums:
        continue
    out.append(f"## D-{start:03d}–D-{end:03d}")
    out.append("")
    out.append("| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |")
    out.append("|-----|----------------|--------------------------------|")
    for n in block_nums:
        label = labels.get(n)
        title = tsv.get(n) or EXTRA_TITLES.get(n, "")
        src = "Plan `feature-tips.md`" if n in EXTRA_TITLES and n not in tsv else ""
        cells = f"| D-{n:03d} | {label} | {clean_title(title)} |"
        if src:
            cells += f" ← {src} |"
        out.append(cells)
        for key, flabel in fu_by_base.get(n, []):
            out.append(f"| ↳ {key} | {flabel} | folgt der Basis-Nummer |")
    out.append("")

out.append("---")
out.append("")
out.append(f"**Nächste freie Nummer: D-{max(nums) + 1}.** (Höchste vergebene: D-{max(nums)}.)")
out.append("")

path = ROOT / "doc/books/AdaptKey-Topics.md"
path.write_text("\n".join(out), encoding="utf-8", newline="\n")
print(f"registry written: {path} ({len(nums)} numbers, {sum(len(v) for v in fu_by_base.values())} follow-ups)")
print(f"next free: D-{max(nums) + 1}")
