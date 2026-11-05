# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Froehlich Media
#
# D-513 verification: byte-exact content check of the rewritten hints deltas (run from repo root).

from pathlib import Path
import sys

sys.stdout.reconfigure(encoding="utf-8")

EXPECTED = {
    "de": "a=ä;d=$;e=€;f=ƒ;h=#;n=ñ;o=ö;p=π;q=@;s=ß;u=ü;x=×",
    "fr": "a=à,â;c=ç;e=é,è,ê,ë;f=ƒ;g=œ;h=#;i=î,ï;j=ï;k=ë;l=«;o=ô;p=π;q=@;r=»;s=€;t=ê;u=ù,û;x=×;y=â;z=û",
    "pl": 'a=ą;c=ć;e=ę;f=ƒ;g=„;h=#;i=…;j=+;k=—;l=ł;n=ń;o=ó;p=π;q=@;r=";s=ś;t=zł;u=&;w=§;x=×;y=•;z=ż,ź',
    "tr": "a=&;c=ç;e=…;f=ƒ;g=ğ;h=#;i=ı;j=—;k=%;l=«;o=ö;p=π;q=@;r=»;s=ş;t=₺;u=ü;w=§;x=×;y=•;z=±",
    "da": 'a=å,æ;c=§;e=…;f=ƒ;g=„;h=#;i=—;j=&;k=%;o=ø;p=π;q=@;r=";s=±;t=kr;u=•;w=‰;x=×;y=¤;z=¶',
    "cs": 'a=á;c=č;d=ď;e=é,ě;f=ƒ;g=„;h=";i=í;j=&;k=%;n=ň;o=ó;p=π;q=@;r=ř;s=š;t=ť;u=ú,ů;w=‰;x=×;y=ý;z=ž',
    "lv": 'a=ā;c=č;e=ē;f=„;g=ģ;h=";i=ī;j=&;k=ķ;l=ļ;n=ņ;o=•;p=π;q=@;r=»;s=š;t=€;u=ū;w=‰;x=×;y=¤;z=ž',
    "uz": "o=oʻ;g=gʻ;f=ƒ;h=#;p=π;q=@;x=×",
    "sk": 'a=á,ä;c=č;d=ď;e=é;f=ƒ;g=„;h=";i=í;j=&;k=%;l=ľ,ĺ;n=ň;o=ó,ô;p=π;q=@;r=ŕ;s=š;t=ť;u=ú;w=‰;x=×;y=ý;z=ž',
    "nl": 'a=†;c=§;e=ë;f=ƒ;g=„;h=#;i=ï;j=…;k=&;l=%;o=ö;p=π;q=@;r=";s=€;t=—;u=ü;w=~;x=×;y=•;z=±',
}

ok = True
for code, want in EXPECTED.items():
    got = (Path("dictionaries") / code / "hints.tsv").read_text(encoding="utf-8").strip()
    if got == want:
        print(f"{code}: OK")
    else:
        ok = False
        print(f"{code}: MISMATCH")
        print(f"  want: {want}")
        print(f"  got : {got}")

en = Path("app/src/main/assets/en/hints.tsv").read_text(encoding="utf-8").strip()
if en == "a=;o=;u=;s=":
    print("en: OK")
else:
    ok = False
    print(f"en: MISMATCH: {en}")

print("ALL OK" if ok else "FAILURES PRESENT")
