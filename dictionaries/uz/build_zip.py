import zipfile
from pathlib import Path

BASE = Path(__file__).parent
OUT = BASE.parent.parent / "language-packs" / "adaptkey-lang-uz.zip"

# D-450-followup: no diacritics.tsv - DataDiacriticFolding's own parser requires single-character variants
# (DiacriticTable.parse filters out anything else), and Uzbek's own "oʻ"/"gʻ" is a two-character digraph
# (base letter + U+02BB modifier), not a single precomposed accented letter - see this pack's own catalog
# entry comment for the full architectural finding. hints.tsv still ships (handles the long-press TYPING
# need correctly, verified against AlternativeScript.extendsWord directly) - only the typed-without-diacritic
# autocorrect-RECOVERY side is the real, documented gap.
FILES = ["dict.tsv", "bigram.tsv", "abbreviations.tsv", "hints.tsv", "version.txt"]

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for name in FILES:
        z.write(BASE / name, arcname=name)

print(f"Wrote {OUT}")
with zipfile.ZipFile(OUT) as z:
    print(z.namelist())
