# Extract every D-number topic with its best available title from History/Progress/Spec (v2).
# Output: scratchpad/topics_extract.tsv (num, source, title)
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOC = ROOT / "doc/books"

hist = (DOC / "AdaptKey-History.md").read_text(encoding="utf-8")
prog = (DOC / "AdaptKey-Progress.md").read_text(encoding="utf-8")
spec = (DOC / "AdaptKey-Spec.md").read_text(encoding="utf-8")

titles: dict[int, tuple[int, str]] = {}

CLUSTER = r"D-(\d{1,3})(?:(?:\.\.|,|/|;|\s|-|–|follow-up|followup|'s)\s*(?:D-)?(\d{1,3}))*"


def nums_in(cluster: str) -> list[int]:
    nums = [int(x) for x in re.findall(r"D-(\d{1,3})", cluster)]
    # expand D-a..D-b ranges
    out = []
    for m in re.finditer(r"D-(\d{1,3})\.\.D-(\d{1,3})", cluster):
        a, b = int(m.group(1)), int(m.group(2))
        if 0 < b - a <= 40:
            out.extend(range(a, b + 1))
    return sorted(set(nums) | set(out))


def put(num: int, prio: int, title: str):
    title = re.sub(r"\s+", " ", title).strip(" .;:")
    if len(title) < 4:
        return
    if num not in titles or titles[num][0] <= prio:
        titles[num] = (prio, title)


def put_cluster(cluster: str, prio: int, title: str):
    for n in nums_in(cluster):
        put(n, prio, title)


# A) History early design definitions:  ### D-01 - Title
for m in re.finditer(r"(?m)^### (D-\d{1,3}(?:[^:\n]{0,60}?))*?\s*[-:]\s*(.+)$", hist):
    pass  # handled by the simpler pattern below
for m in re.finditer(r"(?m)^### D-(\d{1,3})\s*[-:]\s*(.+)$", hist):
    put(int(m.group(1)), 10, m.group(2))

# B) History round headings. Shapes: "## §N (vX): D-473 - t" (backfill), "## §N - D-502 (part 1): t",
#    "## §N - D-555: t (vX -> vY, versionCode a -> b)" (native). Title = heading text with version
#    tags trimmed, spread over the leading D-cluster.
def clean_heading(h: str) -> str:
    h = re.sub(r"\s+", " ", h).strip()
    h = re.sub(r"^\((?:rides )?v[^)]*\):\s*", "", h)  # leading backfill version tag
    h = re.sub(r"\s*\((?:rides )?v[^)]*\)\s*$", "", h)  # trailing version info (simple parens)
    h = re.sub(r"\s*\((?:v[\d.]+|no version bump|no code change)[^)]*\)\s*$", "", h)
    return h.strip(" .")

for m in re.finditer(r"(?m)^## §\d+ (?:- )?([^\n]+)$", hist):
    h = clean_heading(m.group(1)).lstrip("- ").strip()
    if re.match(r"^D-\d{1,3}", h):
        lead = h.split(":")[0]
        if len(lead) <= 60:
            put_cluster(lead, 20, h)
        else:
            put_cluster(h[:30], 20, h)

# C) Spec numbered headings with D-references
for m in re.finditer(r"(?m)^## (\d+)\. (.+)$", spec):
    title = m.group(2)
    ids = re.findall(r"D-\d{1,3}", title)
    if ids:
        put_cluster(" ".join(ids), 25, title)

# D) Bold inline refs that carry a name: **D-522 (the caret slot)**, **D-509..D-514 - the Alt-Belegungs-Reform**
for m in re.finditer(r"\*\*(D-\d{1,3}(?:\.\.D-\d{1,3})?(?:[^*\n]{0,120}))\*\*", hist + "\n" + prog):
    inner = m.group(1)
    if re.match(r"D-\d{1,3}(\.\.D-\d{1,3})?(\s|:|-|–|\()", inner):
        put_cluster(inner, 30, inner)

# E) Progress topic stubs: - **D-558 - IMPLEMENTED ... / - **D-509..D-514 - the Alt-Belegungs-Reform - ...
for m in re.finditer(r"(?m)^- \*\*(D-\d{1,3}(?:\.\.D-\d{1,3})?(?:\s*(?:,|/|\.\.|follow-up|followup|'s)\s*D?\d{0,3})*)\s*[-–]?\s*(\(?[A-ZÄÖÜ][^*]{15,})", prog):
    put_cluster(m.group(1), 40, m.group(2))

all_refs = set()
for m in re.finditer(r"D-(\d{1,3})(?![\d])", hist + "\n" + prog + "\n" + spec):
    all_refs.add(int(m.group(1)))

# context snippets for numbers without any title
def context_snippet(num: int) -> str:
    pat = re.compile(rf"D-{num}(?!\d)")
    for text in (prog, hist, spec):
        for m in pat.finditer(text):
            line_start = text.rfind("\n", 0, m.start()) + 1
            line_end = text.find("\n", m.end())
            line = text[line_start:line_end if line_end != -1 else len(text)]
            line = line.strip()
            if line.startswith("#") or line.startswith("|"):
                continue
            # take a window around the mention
            pos = m.start() - line_start
            lo = max(0, pos - 40)
            hi = min(len(line), pos + 130)
            snip = line[lo:hi]
            if lo > 0:
                snip = "…" + snip.lstrip()
            return snip
    return ""

rows = []
for num in sorted(set(titles) | all_refs):
    if num in titles:
        rows.append((num, "DEF", titles[num][1]))
    else:
        rows.append((num, "CTX", context_snippet(num)))

out = ["num\tsource\ttitle"]
for num, src, title in rows:
    out.append(f"{num}\t{src}\t{title}")
Path(ROOT / "scratchpad/topics_extract.tsv").write_text("\n".join(out), encoding="utf-8", newline="\n")

defs = [n for n, s, _ in rows if s == "DEF"]
ctx = [n for n, s, _ in rows if s == "CTX"]
print(f"total numbers: {len(rows)}; with titles: {len(defs)}; context-only: {len(ctx)}")
print("context-only:", ctx)
