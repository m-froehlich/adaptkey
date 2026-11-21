# Renumber feature-tips.md: the stale D-546..D-553 placeholders collide with numbers the books
# have since assigned (D-546..548 jetzt stack, D-554..558). Shift +13 -> D-559..D-566.
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
p = ROOT / "doc/plan/feature-tips.md"
t = p.read_text(encoding="utf-8")
counts = {}
for old, new in [(546, 559), (547, 560), (548, 561), (549, 562), (550, 563), (551, 564), (552, 565), (553, 566)]:
    pat = re.compile(r"D-" + str(old) + r"(?![\d])")
    t, n = pat.subn("D-" + str(new), t)
    counts[old] = n

old_note = (
    "höchste vergebene Decision-Nummer im Quelltext ist **D-545**; dieser Plan belegt\n"
    "  D-546 ff."
)
new_note = (
    "höchste vergebene Decision-Nummer im Quelltext ist **D-558**; dieser Plan belegt\n"
    "  D-559 ff. (Urenummerierung 2026-11-21: die ursprünglichen Platzhalter D-546–D-553 kollidierten\n"
    "  mit den inzwischen vergebenen Nummern D-546–D-548 (jetzt-Stack) und D-554–D-558; die verbindliche\n"
    "  Nummernliste ist doc/books/AdaptKey-Topics.md)"
)
if old_note in t:
    t = t.replace(old_note, new_note)
    print("numbering note replaced")
else:
    print("NOTE: old numbering note NOT found verbatim - inspect manually")

p.write_text(t, encoding="utf-8", newline="\n")
print("replacements:", counts)
left = re.findall(r"D-5(?:4[6-9]|5[0-3])(?!\d)", t)
print("stale 546-553 refs left:", left)
print("new refs present:", sorted(set(re.findall(r"D-5(?:5[6-9]|6[0-6])(?!\d)", t))))
