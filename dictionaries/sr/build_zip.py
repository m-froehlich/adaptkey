import zipfile
from pathlib import Path

BASE = Path(__file__).parent
OUT = BASE.parent.parent / "language-packs" / "adaptkey-lang-sr.zip"

# D-450-followup: no hints.tsv/diacritics.tsv - Serbian Cyrillic's own letters are standalone code points,
# not diacritic composites of a plainer Latin base letter, so there is nothing for either mechanism to do
# (see SerbianLayout.kt's own KDoc); Greek, the only other non-Latin-script pack, omits both for the same
# reason (dictionaries/el/build_zip.py).
FILES = ["dict.tsv", "bigram.tsv", "abbreviations.tsv", "version.txt"]

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in FILES:
        z.write(BASE / name, arcname=name)

print(f"Wrote {OUT}")
with zipfile.ZipFile(OUT) as z:
    print(z.namelist())
