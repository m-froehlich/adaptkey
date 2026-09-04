import zipfile
from pathlib import Path

BASE = Path(__file__).parent
OUT = BASE.parent.parent / "language-packs" / "adaptkey-lang-fr.zip"

FILES = ["dict.tsv", "bigram.tsv", "hints.tsv", "abbreviations.tsv", "diacritics.tsv", "version.txt"]

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in FILES:
        z.write(BASE / name, arcname=name)

print(f"Wrote {OUT}")
with zipfile.ZipFile(OUT) as z:
    print(z.namelist())
