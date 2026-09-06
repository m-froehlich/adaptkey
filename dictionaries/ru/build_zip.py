import zipfile
from pathlib import Path

BASE = Path(__file__).parent
OUT = BASE.parent.parent / "language-packs" / "adaptkey-lang-ru.zip"

# D-450-followup: no hints.tsv/diacritics.tsv - Russian Cyrillic's own letters are standalone code points,
# not diacritic composites of a plainer Latin base letter, so there is nothing for either mechanism to do
# (same reasoning as Serbian/Greek, dictionaries/sr/build_zip.py).
FILES = ["dict.tsv", "bigram.tsv", "abbreviations.tsv", "version.txt"]

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in FILES:
        z.write(BASE / name, arcname=name)

print(f"Wrote {OUT}")
with zipfile.ZipFile(OUT) as z:
    print(z.namelist())
