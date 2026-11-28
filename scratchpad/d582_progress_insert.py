# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding="utf-8")

p = "doc/books/AdaptKey-Progress.md"
text = open(p, encoding="utf-8", newline="").read()

anchor = ("581_fusion_probe2.py; 0 accepted before the sweep, so no level-safety regression). "
          "build_en_dict BUILD VERIFY PASS (sha256 341a53f0...); full suite 2,015 green.\n")
assert text.count(anchor) == 1, "anchor not unique"

entry = (
    "\n- **D-582 [Golden-Set Harness] - IMPLEMENTED (§582, test-only round, no data change, no pack, "
    "no bump): the German chat golden set lands - 203 curated cases over the real DE "
    "dict+bigram+EN-bundled stores, the three device paths measured (chips via the deferred "
    "suggestionsFor pass, silent correction as the AdaptKeyService commit sequence verbatim: "
    "diacriticRestoration -> D-106 veto -> autocorrectFor at DEFAULT/MEDIUM, next word via "
    "nextWordSuggestions).** The dictionary-quality-next-level plan's M2. Baseline (Vorher for every "
    "round after this): chips greeting @3=86% / particle @3=93% / compound @12=100% / chip_pin 100%; "
    "silent chat-typo recall 0.44 precision 0.89 (the dakne-class quantified: low-frequency chat "
    "targets lose under MEDIUM's 0.75 bar), typo_common 0.70/0.78, umlaut 10/10, silent_pinned 2/2, "
    "veto_en + safe_known clean; nextword_chat @12=39% vs common controls 100% (dead contexts "
    "measured per case: danke -> empty list, gute -> \"Beziehungen und\"); gap_lexicon 0/7 and "
    "contraction 0/6 (M3's Vorher). Findings: wär (24) silently -> war (81,209) at folded cost 0 - "
    "recorded as a real false positive of the current confidence path, candidate material for M5/M7; "
    "nich->noch, eurch->durch, mehrr->mehre stay in the stock as measured FPs. Floors committed as "
    "rot-guards (greeting @3>=75%, particle @3>=80%, compound @12>=85%, chip_pin @3>=90%, silent "
    "recall>=0.30/precision>=0.60, silent_pinned>=0.99, umlaut>=0.85/0.95, nextword_common @3>=70%, "
    "nextword_chat @12>=25%, no_silent exact-zero); gap categories print-only and asserted stocked. "
    "Ranking constants untouched (the harness is their calibration instrument for a later tuning "
    "round with device Gegenprobe). Gates PASS both cuts; full suite 2,020 green (2,015+5).\n"
)

text = text.replace(anchor, anchor + entry)
open(p, "w", encoding="utf-8", newline="").write(text)
print("inserted OK")
