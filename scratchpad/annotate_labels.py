# Insert the Kurzbezeichner into every Progress D-stub head and Spec numbered D-heading.
# Progress:  - **D-558 - IMPLEMENTED ...   ->  - **D-558 [Konkurrenz-Kleinbuchstaben-Gate] - IMPLEMENTED ...
# Spec:      the LAST parenthetical (D-...) of a numbered heading names the owning topic; label appended at line end.
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "scratchpad"))
from labels_a import LABELS as A
from labels_b import LABELS as B
from labels_c import LABELS as C, FOLLOWUPS
from labels_fix import FIXES

labels = {}
labels.update(A)
labels.update(B)
labels.update(C)
labels.update(FIXES)

APPLY = "--apply" in sys.argv

GROUPS = {
    (5, 6): "Tastenton + Haptik",
    (402, 306, 345, 330, 367): "Wörterbuch-Cleanup-Paket",
    (509, 510, 511, 512, 513, 514): "Alt-Belegungs-Reform",
    (535, 536): "Multi-Touch-Trennung + Glide-Commit",
    (531, 532, 533, 534): "Seitennavigation + Glide-Runde",
    (519, 520, 521, 522, 523, 524, 525, 526, 527): "Layout-Runden (Tilde/Währung/Und/Caret)",
    (532, 533, 534): "Glide-Runde (Icon/Lift/Logging)",
    (353, 354, 371, 407): "Autokorrektur-Vertrauen",
    (344, 386, 413): "Download-Ordner-Steuerung",
    (483, 484, 485): "Einstellungs-Bedienbarkeit",
    (280, 281): "Spracharchitektur",
}

# heads whose label should differ from the first number's own label
HEAD_LABELS = {
    "D-19/G-03": "Gesten-Schalter (externer PR #11)",
    "D-516/G-09": "Glide-Typing",
}

HEAD = re.compile(
    r"D-\d{1,3}(?:-followup| follow-up|'s)?"
    r"(?:\.\.D-\d{1,3})?"
    r"(?:(?:/|,\s*|\s+)(?:D-\d{1,3}(?:-followup)?|G-\d{1,3}))*"
)

def stub_head(ln: str):
    m = re.match(r"^- \*\*(" + HEAD.pattern + r")", ln)
    if not m:
        return None, []
    head = m.group(1)
    nums = []
    for r in re.finditer(r"D-(\d{1,3})\.\.D-(\d{1,3})", head):
        nums.extend(range(int(r.group(1)), int(r.group(2)) + 1))
    nums.extend(int(x) for x in re.findall(r"(?<!\.)D-(\d{1,3})(?![\d.])", head))
    seen, out = set(), []
    for n in nums:
        if n not in seen:
            seen.add(n)
            out.append(n)
    return head, out

def group_label(nums: list[int], head: str | None = None) -> str:
    if head and head in HEAD_LABELS:
        return HEAD_LABELS[head]
    t = tuple(nums)
    if t in GROUPS:
        return GROUPS[t]
    if len(nums) == 1:
        return labels.get(nums[0], "")
    return labels.get(nums[0], "")

prog_path = ROOT / "doc/books/AdaptKey-Progress.md"
prog = prog_path.read_text(encoding="utf-8")
out_lines = []
changed = 0
for ln in prog.split("\n"):
    if ln.startswith("- **D-"):
        head, nums = stub_head(ln)
        if head and nums:
            pos = len("- **") + len(head)
            rest = ln[pos:]
            if not rest.startswith(" ["):
                label = group_label(nums, head)
                if label:
                    out_lines.append(ln[:pos] + " [" + label + "]" + rest)
                    changed += 1
                    continue
    out_lines.append(ln)
prog_new = "\n".join(out_lines)

spec_path = ROOT / "doc/books/AdaptKey-Spec.md"
spec = spec_path.read_text(encoding="utf-8")
spec_changed = 0
def spec_head(m: re.Match) -> str:
    global spec_changed
    title = m.group(2)
    owners = re.findall(r"\(([^()]*D-\d{1,3}[^()]*)\)", title)  # parenthesised D-clusters
    if not owners:
        return m.group(0)
    owner = owners[-1]  # the last one names the owning topic
    raw = owner.replace(" ", "")
    label = None
    if "-followup" in raw:
        label = FOLLOWUPS.get(raw) or FOLLOWUPS.get(raw.replace("-followup", ""))
    if label is None:
        nums = sorted(set(int(x) for x in re.findall(r"\d{1,3}", owner)))
        label = group_label(nums)
    if isinstance(label, tuple):
        label = label[0]
    if not label:
        return m.group(0)
    spec_changed += 1
    return f"{m.group(1)} {title} [{label}]"

spec_new = re.sub(r"(?m)^(## \d+\.) ([^\n]+)$", spec_head, spec)

if APPLY:
    prog_path.write_text(prog_new, encoding="utf-8", newline="\n")
    spec_path.write_text(spec_new, encoding="utf-8", newline="\n")
    print(f"APPLIED: {changed} Progress stubs labelled, {spec_changed} Spec headings labelled")
else:
    print(f"dry run: {changed} Progress stubs would be labelled, {spec_changed} Spec headings")
    for ln in prog_new.split("\n"):
        if ln.startswith("- **D-509") or ln.startswith("- **D-519") or ln.startswith("- **D-19/") or ln.startswith("- **D-516") or ln.startswith("- **D-402"):
            print("  P:", ln[:120])
    for ln in spec_new.split("\n"):
        if re.match(r"## (3[5-9]|40)\.", ln):
            print("  S:", ln[:150])
