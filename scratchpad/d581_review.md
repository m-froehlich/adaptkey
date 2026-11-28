# D-581 (Markup Artefact Sweep DE+EN) — Review-Liste, Stand 2026-11-28

Freigabe-Status: **ANGEWENDET (2026-11-28, Freigabe im Chip: gesamt + Option A).** Vermessung:
`scratchpad/d581_survey.py` (Report: `scratchpad/d581_survey_report.txt`), Abhängigkeits-Probe:
`scratchpad/d581_probe2.py`. Gate-Baseline vor der Runde: DE `--capitalises-nouns` PASS
(189.781 Rows), EN `--lang en` PASS (88.745 Rows, gebündelt unter `app/src/main/assets/en/`).

**Nachträge beim Anwenden (dokumentiert):** (1) EN bigram tatsächlich **161** Rows entfernt
(Statt 333 — die Schätzung enthielt Zeilen an behaltenen Realwörtern wie times/trade). (2) Die
Gate-Denylist fing bei ihrem ersten EN-Lauf `apos` 2 (NOUN,OTHER, Lemma `apo`) — das `&apos;`-
Fragment, 24. Survey-Treffer, in der Freigabeliste übersehen; als `d581_apply2.py` nachgezogen
(D-570-apply2-Präzedenz). `apo` 5 (griechische Transliteration, kein Markup) bleibt bewusst.
Endstand: **DE 189.430 dict / 79.393 bigram · EN 88.732 dict / 79.839 bigram.**

## A. DE — `dictionaries/de/dict.tsv` (189.781 Rows)

### A1. Entfernen: Markup-/Entity-Klasse (13 Rows)
| Surface | Freq | POS | Befund |
|---|---|---|---|
| nbsp | 134.624 | OTHER | `&nbsp;`-Artefakt, Rang 21 |
| thinsp | 179 | OTHER | Entity-Artefakt |
| lt | 175 | OTHER | `<`-Entity-Fragment |
| reg | 131 | OTHER | `®`-Fragment |
| amp | 65 | NOUN,OTHER | `&`-Fragment (kein deutsches Wort) |
| para | 60 | OTHER | `¶`-Fragment (Survey-Neufund, Plan nannte die Klasse) |
| copy | 35 | NOUN,OTHER | `©`-Fragment |
| dagger | 18 | OTHER | `†`-Fragment |
| deg | 14 | NOUN,OTHER | `°`-Fragment |
| Trade | 197 | NOUN | `™`-Artefakt |
| Bull | 157 | NOUN | `•`-Artefakt |
| Bulls | 11 | NOUN, Lemma `Bull` | Plural-Artefakt — **mit entfernt**, sonst orphaned Lemma (D-577-Gate) |
| Divide | 19 | NOUN,OTHER | `÷`-Artefakt |

**Bewusst behalten** (echte Wörter, case-insensitive Kollision mit Entity-Namen): `Euro` 4.695,
`Cent` 438, `Times` 673, `Yen` 157 — Währungsnamen; `Pound` 65 (englisch — Leakage, aber nicht
Markup-Klasse: **out of scope dieser Runde**, Vermerk für eine spätere Leakage-Runde).

### A2. Entfernen: nicht-tippbare Zeilen (333 Rows)
Alle Zeilen mit Leerzeichen/Klammer in der Oberfläche (`des Nord` 916, `des Ost` 782,
`(des) Französisch` 463, … `der Venus` 208 …) — POS-Mix: 333× NOUN. Keine Compound-
Restaurierung (Plan-Vorgabe M1: „prüfen, nicht restaurieren“ — Compound-Kandidaten wie
`Nordwest` existieren bereits als eigene Zeilen).

### A3. Archaik-Rest (4 Rows): Entfernen statt Blacklist-Mechanik
`aßt` 1 (VERB, Lemma essen), `buk` 1 (backen), `wob` 1 (weben), `gor` 1 (gären).
Plan sagt „blacklisten (D-206-Politik)“ — der D-206-Blacklist-Mechanismus ist aber durch das
**D-473-Followup (2026-09-17) superseded**: „blacklisting was the wrong mechanism … should not
exist in the dictionary at all any more“. Umsetzung daher: **aus dict.tsv entfernen + in die
Gate-Denylist aufnehmen** (die moderne „Blacklist“ = Re-Intro scheitert am Gate).

### A4. Entfernen: `til` (26, NOUN)
Kein deutsches Wort (EN-Leakage), Kleinschreib-NOUN-Klasse. Entfernen statt `Til`-Rename
(`Til` wäre Eigenname, kein Common Noun).

### A5. Casing-Renames (lowercase bare-NOUN-Hygiene, 2 Rows)
| alt | neu | Freq | Grund |
|---|---|---|---|
| `text` | `Text` | 5.076 | Casing-Verletzung; kein `Text`-Twin vorhanden (geprüft), keine Lemma-Abhängigen |
| `teleskopmasten` | `Teleskopmasten` | 25 | dito |

Getipptes „text“ bleibt bedient (Case-Fold-Lookup, D-196); §6-Auto-Caps führt jetzt korrekt
zur großen Form.

### A6. Keep-lowered-Klasse (§4-Entscheidung: „gesenkt behalten“, Freq ≤ ~200, OTHER)
| Surface | jetzt | wird |
|---|---|---|
| jpg | 14.825, OTHER | Freq 200 |
| de | 13.799, OTHER | Freq 200 |
| www | 153, OTHER | bleibt |
| HTTP | 140, NOUN | Surface `http`, Freq 140, Tag OTHER |
| HTTPS | 43, NOUN | Surface `https`, Freq 43, Tag OTHER |

**DE-Bilanz:** 189.781 → **189.430 Rows** (−351: 13 Entity + 333 nicht-tippbar + 4 Archaik +
1 `til`; +2 Renames; 4 Keep-Klassen-Edits).

## B. DE — `dictionaries/de/bigram.tsv` (79.937 Rows)
Rows entfernen, deren prev **oder** word eine entfernte Oberfläche (A1–A4 inkl. der 333
Mehrwort-Splitter) referenziert — gemessen: **544 Rows** (exakte Zahl fixiert das Apply-Skript).
`de`/`jpg`/`http`-Kontexte bleiben (die Wörter bleiben im dict). Keine nicht-alpha-Schlüssel
gefunden (0).

## C. EN — `app/src/main/assets/en/dict.tsv` (88.745 Rows, gebündelt)

### C1. Entfernen: Markup-Klasse (12 Rows)
`nbsp` 23.034 · `nbsps` 9.384 (Plural-Artefakt, Lemma `nbsp` — **mit entfernt**, sonst
orphaned) · `ndash` 341 · `GT` 265 · `Lt` 186 · `mdash` 180 · `thinsp` 61 · `Reg` 36 ·
`deg` 17 · `degs` 7 (Lemma `deg`) · `middot` 5 · `middots` 2 (Lemma `middot`).

### C2. ⚠ Freigabe-Frage: `amp` (Plan-Abweichung)
Der Plan nennt `amp` (142) in der EN-Entfernungsliste. Die Daten zeigen aber eine **echte
Wiktionary-Verb-Familie**: `amp` 142 NOUN,VERB, dazu `amping` 69 und `amped` 69 (VERB,
Lemma `amp`) — „to amp (up)“ ist reales Englisch. §4-Philosophie („Denylist nur für die echte
Markup-Klasse“) spricht für Behalten.
- **Option A (Empfehlung):** `amp`/`amping`/`amped` **behalten** — kein Entfernen, kein
  Gate-Eintrag.
- **Option B (Plan-Buchstabe):** alle drei entfernen (else orphaned Lemmas).

### C3. Bewusst behalten (echte englische Wörter, case-insensitive Entity-Kollisionen)
`times` 8.718 · `trade` 5.788 · `copy` 907 · `euro` 685 · `cent` 678 · `divide` 513 ·
`bull` 442 · `pound` 311 · `sect` 215 · `dagger` 176 · `para` 101 · `shy` 87 · `yen` 66.

### C4. Keep-lowered-Klasse (wie DE)
| Surface | jetzt | wird |
|---|---|---|
| jpg | 8.428, NOUN,OTHER | Freq 200, Tag OTHER |
| de | 10.314, NOUN,VERB,PREPOSITION | Freq 200; **Tags bleiben** (echte Multi-POS-Entry, nur Frequenz gesenkt — Abweichung vom OTHER-Tag-Buchstaben, dokumentiert) |
| HTTP | 230, PROPER_NOUN | Freq 200, Tag OTHER |
| HTTPS | 63, PROPER_NOUN | Tag OTHER (Freq bleibt 63) |
| www | 33, OTHER | bleibt |

**EN-Bilanz:** 88.745 → **88.733 Rows** (Option A) bzw. 88.730 (Option B).

## D. EN — `app/src/main/assets/en/bigram.tsv` (80.000 Rows)
Rows mit entfernten Oberflächen auf einer Seite: **333 Rows** entfernen (Option A) /
360 (Option B). Nach measurements, exakt im Apply-Skript.

## E. `dictionaries/quality_gate.py` — Gate-Ausbau (M1-Gate (a)–(c))
1. **Immer aktiv (sprachneutraler Basis-Check):**
   - **Typability:** Oberfläche darf kein Leerzeichen, `(`, `)` oder U+00A0 enthalten
     (Apostroph ausdrücklich erlaubt) — fängt die 333-Klasse dauerhaft.
   - **Neutral-Markup-Denylist** (case-exakt, nie echte Wörter in de/en):
     `nbsp, nbsps, NBSP, thinsp, ensp, emsp, zwnj, zwj, ndash, mdash, hellip, quot, apos,
     laquo, raquo, ldquo, rdquo, lsquo, rsquo, sbquo, bdquo, middot, middots, deg, degs,
     GT, Lt, Trade, Divide`.
2. **Unter `--capitalises-nouns`** (Kapitalisierungssprachen = heute DE, D-464-Präzedenz für
   flag-gekoppelte Checks): (a) **lowercase bare-NOUN-Check** (must be 0 — fängt
   `text`-Klasse), (b) **DE-spezifische Artefakt-Denylist**: `lt, amp, reg, copy, para,
   dagger` (kollidieren als echte EN-Wörter — deshalb nicht neutral) plus die Archaik-Denylist
   `aßt, buk, wob, gor`.
3. Bestehende `--lang en`-Denylist (D-575) unverändert; D-575-Kommentar ergänzt.

Mandated invocations danach grün: `python dictionaries/quality_gate.py dictionaries/de/dict.tsv
--capitalises-nouns` und `… app/src/main/assets/en/dict.tsv --lang en`.

## F. Deployment (Pack-Pflichten §5)
- **DE = Pack 55:** `dictionaries/de/version.txt` 54→55 · `build_zip.py` →
  `language-packs/adaptkey-lang-de.zip` · `LanguagePackCatalog`-Entry GERMAN version → 55 ·
  **Commit + Push an origin/main** (D-580-Lektion: das gehostete Archiv IST die raw-URL auf
  main — ohne Push erreicht die Runde kein Bestandsgerät).
- **EN gebündelt:** `BUNDLED_DICTIONARY_VERSION` 3→4 in `DictionaryLoader.kt`
  (M8-Pin/D-578-Muster — installierte Geräte resseeden beim nächsten App-Start).
  `dictionaries/en/version.txt` bleibt 1 (EN-Präzedenz D-576/D-578: Quell-Stempel, kein Bump).
- `build_en_dict.py` (Verify inkl. Gates + sha256) muss grün laufen.
- JVM-Tests: `EnglishDictionaryQualityEvaluationTest` (Daten-Pins) + Kern-Tests; Pins prüfen.

## G. Dokumentation
History-Section §581 (append-only), Progress-Eintrag, Plan-Statuszeile (M1 erledigt),
Commit(s): Planungs-Nachtrag (Plan + Themenlisten-Zeilen D-581–D-592, uncommitted seit
2026-11-26) als eigener Docs-Commit, danach der D-581-Runden-Commit; ein Push für beide.
Nicht angerührt: `doc/plan/feature-tips.md` + die vier Ist-Analyse-Probes (fremde/ältere
Artefakte, bleiben untracked).
