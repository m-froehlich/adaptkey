import zipfile
from pathlib import Path

BASE = Path(__file__).parent
OUT = BASE.parent.parent / "language-packs" / "adaptkey-lang-az.zip"

# D-450-followup: unlike the Cyrillic packs, Azerbaijani IS Latin script and DOES ship hints.tsv/diacritics.tsv
# - see this pack's own catalog entry comment for why both are still meaningful even though every special
# letter already has its own dedicated primary key on AzerbaijaniLayout.
FILES = ["dict.tsv", "bigram.tsv", "abbreviations.tsv", "hints.tsv", "diacritics.tsv", "version.txt"]

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in FILES:
        z.write(BASE / name, arcname=name)

print(f"Wrote {OUT}")
with zipfile.ZipFile(OUT) as z:
    print(z.namelist())
