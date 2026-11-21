# Ground the still-unknown D-numbers: print the first definition-style context for each.
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
hist = (ROOT / "doc/books/AdaptKey-History.md").read_text(encoding="utf-8")
prog = (ROOT / "doc/books/AdaptKey-Progress.md").read_text(encoding="utf-8")
spec = (ROOT / "doc/books/AdaptKey-Spec.md").read_text(encoding="utf-8")

targets = [74, 92, 116, 118, 122, 139, 144, 148, 152, 153, 159, 166, 178, 181, 184, 190, 194,
           201, 203, 217, 218, 220, 225, 228, 229, 230, 233, 238, 246, 260, 267, 268, 279, 281,
           285, 296, 299, 300, 307, 308, 311, 315, 317, 320, 323, 339, 363, 372, 375, 381, 383,
           390, 393, 395, 399, 452, 456, 459, 460, 463, 469, 470, 471]

def find(n):
    m = re.search(r"(?m)^### D-%d\b[ :.-]*(.{0,140})" % n, hist)
    if m:
        return "H/###: " + m.group(1)
    m = re.search(r"\*\*D-%d\b[ :.-]*([^\n*]{0,140})" % n, hist)
    if m:
        return "H/**: " + m.group(1)
    m = re.search(r"\*\*D-%d\b[ :.-]*([^\n*]{0,140})" % n, prog)
    if m:
        return "P/**: " + m.group(1)
    m = re.search(r"D-%d\b[: -]+([A-ZÄÖÜ][^\n]{0,130})" % n, hist)
    if m:
        return "H/: " + m.group(1)
    m = re.search(r"D-%d\b[: -]+([A-ZÄÖÜ][^\n]{0,130})" % n, prog)
    if m:
        return "P/: " + m.group(1)
    m = re.search(r"D-%d\b[: -]+([A-ZÄÖÜ][^\n]{0,130})" % n, spec)
    if m:
        return "S/: " + m.group(1)
    return None

for n in targets:
    f = find(n)
    print(f"{n}|{f if f else 'NOTHING'}")
