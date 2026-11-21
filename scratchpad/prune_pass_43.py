# 43rd pruning pass: move Progress rounds §495-§519 into History.md (verbatim, token-verified).
# Also repairs the cp1252-encoded §566 section (newest, added 2026-11-21) to UTF-8.
# Run: python scratchpad/prune_pass_43.py            (dry run)
#      python scratchpad/prune_pass_43.py --apply    (write files)
import re, sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROG = ROOT / "doc/books/AdaptKey-Progress.md"
HIST = ROOT / "doc/books/AdaptKey-History.md"
APPLY = "--apply" in sys.argv
SEP = "\n\n\n\n"  # section separator used by the surrounding backfilled sections (§480-§520)

prog_text = PROG.read_text(encoding="utf-8")
prog_lines = prog_text.split("\n")
hist_bytes = HIST.read_bytes()

# ---- locate round bullet starts in Progress ----
bullet_re = re.compile(r"^- \*\*§(\d+)")
starts = []
for i, ln in enumerate(prog_lines):
    m = bullet_re.match(ln)
    if m:
        starts.append((i, int(m.group(1))))

older_heading_idx = next(i for i, ln in enumerate(prog_lines) if ln.startswith("## Older Rounds"))
starts = [(i, n) for i, n in starts if i < older_heading_idx]
print("round bullets found:", [n for _, n in starts])
assert sorted(set(n for _, n in starts)) == list(range(495, 520))
assert len(starts) == 26, f"expected 26 bullets (§513 twice), got {len(starts)}"
assert older_heading_idx > starts[-1][0]

# ---- locate insertion point in History (byte level) ----
sec_re = re.compile(rb"(?m)^## (?:\xc2\xa7|\xa7)(\d+)")
secs = []
for m in sec_re.finditer(hist_bytes):
    secs.append((m.start(), int(m.group(1))))
nums = [n for _, n in secs]
violations = [(nums[k - 1], nums[k]) for k in range(1, len(nums)) if nums[k] < nums[k - 1]]
print("ordering violations (known historical quirks):", violations)
assert 494 in nums and 520 in nums
assert not (set(range(495, 520)) & set(nums)), "§495-§519 already exist in History?!"
offset_520 = next(off for off, n in secs if n == 520)
offset_566 = next(off for off, n in secs if n == 566)
print(f"History: inserting before byte {offset_520} (§520); §566 region starts at {offset_566}")

# ---- token check helpers ----
def tokens(text: str) -> Counter:
    text = text.replace("**", " ")
    out = []
    for t in text.split():
        t = t.strip(".,;:!?()[]{}\"'`#—–")
        if not t or t == "-" or re.fullmatch(r"#+", t):
            continue
        if re.fullmatch(r"§\d+", t):
            continue
        if re.fullmatch(r"v\d+(\.\d+)+", t):  # version tags are set aside (both sides)
            continue
        out.append(t)
    return Counter(out)

def split_title_body(block_lines):
    """Return (version_tag_with_colon, title, body_source_lines). Merges wrapped titles."""
    first = block_lines[0]
    m = re.match(r"^- \*\*§\d+ (\([^)]*\):) (.*)$", first)
    assert m, f"unexpected first line: {first[:120]}"
    ver = m.group(1).strip()
    rest = m.group(2)
    close = rest.find("**")
    assert close != -1
    title_parts = [rest[:close]]
    head_txt = rest[:close].rstrip()
    body_start_idx = 1
    if rest[close + 2:].strip() == "" and len(block_lines) > 1 and head_txt and head_txt[-1] not in ".!?":
        nxt = block_lines[1].strip()
        if nxt.startswith("**"):
            cont = nxt[2:]
            c2 = cont.find("**")
            assert c2 != -1, f"continuation lacks closing **: {nxt[:120]}"
            title_parts.append(cont[:c2])
            # body continues on the continuation line after the closing **, then the remaining lines
            body_first = cont[c2 + 2:]
            body_lines = ([body_first] if body_first.strip() else []) + block_lines[2:]
            return ver, " ".join(p.strip() for p in title_parts), body_lines
    body_first = rest[close + 2:]
    body_lines = ([body_first] if body_first.strip() else []) + block_lines[1:]
    return ver, " ".join(p.strip() for p in title_parts), body_lines

def transform(n: int, start: int, end: int):
    raw_lines = prog_lines[start:end]
    while raw_lines and raw_lines[-1].strip() == "":
        raw_lines.pop()
    ver, title, body_lines = split_title_body(raw_lines)
    # body style of the neighbouring backfilled sections: every source line is its own paragraph
    paras = [ln.strip() for ln in body_lines if ln.strip()]
    heading = f"## §{n} {ver} {title}".replace("  ", " ")
    heading = re.sub(r"\): ", "): ", heading)  # no-op safety
    built = heading + SEP + "\n\n".join(paras)
    raw = "\n".join(raw_lines)
    a, b = tokens(raw), tokens(built)
    delta = (a - b) + (b - a)
    if delta:
        print(f"§{n} DELTA ({sum(delta.values())}):")
        for t, c in list(delta.items())[:12]:
            print(f"   {c}x {t!r}")
        sys.exit(f"token check failed for §{n}")
    print(f"§{n}: token check OK | heading: {heading[:100]}")
    return built

# §513: two bullets merged into one section (file order is descending, so take file-order neighbours)
idx513 = [k for k, (ii, nn) in enumerate(starts) if nn == 513]
assert len(idx513) == 2 and idx513[1] == idx513[0] + 1
end_b = starts[idx513[1] + 1][0]  # start line of the next bullet after the second §513 (that is §512)
raw_both = "\n".join(prog_lines[starts[idx513[0]][0]:end_b])
lines_a = prog_lines[starts[idx513[0]][0]:starts[idx513[1]][0]]
lines_b = prog_lines[starts[idx513[1]][0]:end_b]
while lines_a and lines_a[-1].strip() == "":
    lines_a.pop()
while lines_b and lines_b[-1].strip() == "":
    lines_b.pop()
ver_a, title_a, body_a = split_title_body(lines_a)
ver_b, title_b, body_b = split_title_body(lines_b)
assert ver_a == ver_b == "(v1.2.73):"
paras_a = [ln.strip() for ln in body_a if ln.strip()]
paras_b = [ln.strip() for ln in body_b if ln.strip()]
merged = (f"## §513 {ver_a} {title_a}" + SEP + "\n\n".join(paras_a) + SEP
          + title_b + "\n\n" + "\n\n".join(paras_b))
a, b = tokens(raw_both), tokens(merged)
delta = (a - b) + (b - a)
if delta:
    print(f"§513 DELTA ({sum(delta.values())}):")
    for t, c in list(delta.items())[:12]:
        print(f"   {c}x {t!r}")
    sys.exit("token check failed for §513")
print(f"§513: merged D-494+D-491, token check OK | heading: {title_a[:80]}")

new_sections = {}
new_sections[513] = merged
for n in range(495, 520):
    if n == 513:
        continue
    i = next(ii for ii, nn in starts if nn == n)
    e = next((starts[k + 1][0] if k + 1 < len(starts) else older_heading_idx)
             for k, (ii, nn) in enumerate(starts) if nn == n)
    new_sections[n] = transform(n, i, e)

# ---- §566 cp1252 -> UTF-8 repair ----
region = hist_bytes[offset_566:]
try:
    region.decode("utf-8")
    print("§566 region already UTF-8; no repair needed")
    repaired_region = region
except UnicodeDecodeError:
    repaired_region = region.decode("cp1252").encode("utf-8")
    print(f"§566 region: cp1252 -> UTF-8 repaired ({len(region)} -> {len(repaired_region)} bytes)")

if APPLY:
    head = hist_bytes[:offset_520]
    # trim the separator's trailing newlines; we re-add our own
    head = head.rstrip(b"\n")
    tail = hist_bytes[offset_520:]
    sep_b = SEP.encode("utf-8")
    insert = sep_b + sep_b.join(new_sections[n].encode("utf-8") for n in range(495, 520)) + sep_b
    new_hist = head + insert + tail
    # swap in the repaired §566 region
    cut = new_hist.find(region) if APPLY and repaired_region is region else None
    if repaired_region is not region:
        idx = new_hist.rindex(region)
        new_hist = new_hist[:idx] + repaired_region + new_hist[idx + len(region):]
    HIST.write_bytes(new_hist)
    # whole file must now decode as UTF-8
    HIST.read_text(encoding="utf-8")
    print("History written and re-verified as UTF-8")

    head_p = prog_lines[:starts[0][0]]
    while head_p and head_p[-1].strip() == "":
        head_p.pop()
    rest_p = prog_lines[older_heading_idx:]
    assert rest_p[0].startswith("## Older Rounds (§1-§494")
    new_heading = "## Older Rounds (§1-§519, v0.7.6 through v1.2.78) - Pruned From This File"
    note = (
        "D-497-followup (§519): forty-third pruning pass - §495-§519 removed (twenty-five rounds, the whole "
        "remaining working set), cutoff moved from §495 to §520. Backfilled into History.md first, verbatim, "
        "reformatted from this file's own bullet shape into History.md's heading+paragraph style, verified by "
        "comparing each round's whitespace-token multiset (formatting markers and the §number/version tags "
        "aside) between its old Progress.md block and its new History.md section - all twenty-five came back "
        "identical, so nothing was summarised or dropped; §513's two Progress bullets (D-494 and D-491) were "
        "merged into History's single §513 section and verified as one block. This pass settles the remaining "
        "§495-§519 half of §520's noted History-gap debt (the §480-§494 half was settled by the forty-second "
        "pass) and it empties this file's round working set by design: since §520 every round is recorded "
        "directly in History.md and this file carries only the topic ledger, so the ~25-round working set has "
        "no further entries to prune - the rule is honoured by the file staying round-free from here on. "
        "History's newest section (§566) had been appended cp1252-encoded; its encoding was repaired to the "
        "file's UTF-8 standard in the same pass."
    )
    new_prog = "\n".join(head_p + ["", new_heading, "", note, ""] + rest_p[1:])
    PROG.write_text(new_prog, encoding="utf-8", newline="\n")
    print("Progress written: rounds removed, Older Rounds heading + 43rd-pass note updated")
else:
    print("dry run - rerun with --apply to write")
