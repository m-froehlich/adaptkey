
## §530 - D-506: "packs are a base" notice on the language-packs screen, and the cleanup method in the contribution guide (v1.2.92)

User request: users choosing a non-curated language pack must understand that missing words, unwanted
corrections and similar glitches usually come from the uncurated pack data, not from the app - true
today for every language except German, bundled English explicitly included - with a call to improve
one's own pack when able. And: the D-504 cleanup method should live in the contribution guide, since
its per-language steps (noun-lemma shield, homograph-checked strays, hand-reviewed band) are exactly
native-speaker work.

Implementation is deliberately tiny: one localized notice TextView under `d280_intro` in
`activity_language_packs.xml` (string `d506_pack_base_notice` - static text naming German as the only
curated pack; if a second language ever becomes curated, the string changes with it, no catalog
plumbing for a one-bit fact), present in all 32 string files (base English + 31 locales, AI drafts
per the D-478 precedent). The guide gained "Dictionary cleanup for pack maintainers (D-504)" in §3:
the two systematic corpus defects, the three per-language inputs, the report-first workflow and the
rules in brief, pointing at `scratchpad/d504-crosslang-report.txt` for the measured per-language
scale. No Kotlin code, no new tests beyond the existing locale-consistency enforcement. Rides
versionCode 588 / 1.2.92 together with D-504 (§529).
