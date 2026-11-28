# AdaptKey — Plan: Wörterbuch-Qualität Next Level (Deutsch als Blaupause, Englisch mit Sonderrolle)

**Status: OFFEN — Ist-Analyse abgeschlossen (vermessen 2026-11-26), Maßnahmen M1–M12 ausgearbeitet.
Alle vier Entscheidungen getroffen (2026-11-26, je der Empfehlung, siehe §4): Veto-Schwelle bauen
(M7), Verb-Tail Purge + Lemma-Erbe (M9+M10), Floors als Standard / Blend nur Doku (M5), Fragmente
gesenkt behalten (M1). Umsetzung in Runden (§5); jede Runde hat ihren eigenen ruhenden
Session-Chip (Seitenleisten-Gruppe „Wörterbuch-Runden") und startet erst auf ausdrücklichen
Start-Befehl in ihrem Chip. **M1 (D-581) erledigt 2026-11-28 (§581): DE-Pack 55 gepusht,
EN-Reseed 4, Gate mit Typability + Denylist; Abweichung von der M1-Datenliste: EN `amp`
bleibt (echte Verb-Familie amp/amping/amped, §4-Philosophie, Freigabe im Chip).**
**M2 (D-582) erledigt 2026-11-28 (§582): Golden-Set-Harness steht (203 kuratierte Fälle,
`GermanChatEvaluationTest` über die drei Geräte-Pfade), Baseline dokumentiert — die Kernzahl:
stille Chat-Typo-Reparatur Recall 44 % (die dakne-Klasse unter der MEDIUM-Bar); Vorher/Nachher
ist ab jetzt für jede Runde Pflicht.** Nächste Runde: M3 (D-583, DE-Messaging-Register +
Apostroph-Kontraktionen, Pack 56 — die erste Runde, deren sichtbarer Gewinn in diesen Zahlen
steigt).**

Origin: Nutzer-Anfrage (2026-11-26). Die Wörterbuch-Qualität soll „auf ein neues Level“: bessere
Chips, besser funktionierende Auto-Korrektur, echter messbarer und sichtbarer Mehrwert. Deutsch ist
die Blaupause; Englisch hat eine Sonderrolle, weil es **immer mitkonsultiert** wird (D-106-Veto,
A-03-Klassifikator-Fallback) und seine Qualität deshalb in *allen* installierten Sprachen wirkt.
Auslöser ist das Feedback eines Testnutzers, dem die Chips „sehr minderwertig“ erscheinen — er steht
allerdings noch auf **1.3.18**: Ihm fehlen damit elf App-Versionen an Chip-/Korrekturarbeit
(D-544 bis D-572, v1.3.19–v1.3.29) **und** die deutschen Pack-Runden 53/54 (D-567/D-577/D-580).
Sein Eindruck misst also teilweise einen Stand, den das Repo längst verlassen hat — er ist aber
zugleich der beste Beleg dafür, dass Daten- und Code-Runden nur so gut sind wie ihr *Ankommen*
beim Bestandsgerät (M8).

Dieser Plan bündelt die Ist-Analyse (beide Wörterbücher neu vermessen, Runtime-Pipeline ausgewertet)
und leitet daraus zwölf Maßnahmen ab. Er steht bewusst neben dem abgeschlossenen
`english-dictionary-quality.md` (D-567–D-580): dessen Muster (Review-Listen, Gate-Denylist,
Regressionstest über die realen Daten, Device-Test-Protokoll) wird hier verallgemeinert und auf
Deutsch übertragen.

Identifier: D-581 ff. (M1=D-581, M2=D-582, M3=D-583, M4=D-584, M5=D-585, M6=D-586, M7=D-587,
M8=D-588, M9=D-589, M10=D-590, M11=D-591, M12=D-592). History ab §581, Versionen ab 1.3.30/630
für Code-Runden; Datenrunden ohne Bump (D-545-Präzedenz), Pack-Runden mit version.txt + zip +
`LanguagePackCatalog`-Bump (D-497-Konvention) und Push an das gehostete Archiv (D-580-Lektion).

---

## 0. Kurzfassung

- **M1 (D-581, sofort):** Markup-Artefakt-Sweep über DE **und** EN — `nbsp` steht im deutschen
  Wörterbuch auf **Rang 21** (134.624), im englischen auf **Rang 75** (23.034); dazu Entity-Fragmente
  (`jpg` Rang 115, `amp`/`lt`/`reg`/`GT`/`Lt` …) und **333 Zeilen mit eingebetteten Leerzeichen/Klammern**
  („des Nord“ 916, „(des) Französisch“ 463). Nicht tippbar, polluieren Präfix-Chips, Bucket-Scans und
  Bigramm-Kontexte. Gate-Denylist für beide Sprachen verankern.
- **M2 (D-582, Basis für alles Weitere):** Golden-Set-Harness — ein deutscher Chat-Satzbestand
  (~200–300 Fälle) als JVM-Evaluationstest nach dem `FusionEvaluationTest`-Methyl: chip@1-Trefferquote,
  stille Korrektur Precision/Recall. Erst **messbar machen, dann verbessern**; jede folgende Runde
  liefert Vorher/Nachher-Zahlen für die History. Tuning der „not yet device-tuned“-Konstanten
  (`FUZZY_COST_DECAY`, `PREFIX_LENGTH_DECAY`, Trigram-Backoff) wird damit erst möglich.
- **M3 (D-583, größter sichtbarer Hebel):** DE-Messaging-Register + Apostroph-Kontraktionen.
  Der Kernbefund der Analyse: das Wörterbuch misst Umgangssprache mit enzyklopädischer Waage —
  `jedoch` Rang 74 (27.743) vs. `ich` Rang 352 (5.153), `danke` Frequenz **49**, `bitte` nur als
  „Bitte“ (472), `tschüss`/`okay`/`haha`/`super`/`kriegen`/`nix` **fehlen ganz**; **null**
  Apostroph-Zeilen („geht’s“, „gibt’s“ …) — D-544 läuft wie in EN vor D-571 ins Leere. Kuratierter
  Floors-Katalog (~300–600 Wörter, dokumentierte Rangliste nach D-571-Muster).
- **M4 (D-584):** Bigramm-Revision: 1.199 verwaiste Rows, englische Leakage-Paare in der
  **deutschen** Tabelle („bar→from“ 816, „university→of“ 591), `nbsp`-Kontexte; Chat-Kontexte
  taub (`ich`→ 39 Continuations, `danke`→ **0**, `hab`→ 1). EN-Bigramme (80.000 Rows) seit je
  unrefreshet. Verfahren + Rebuild dokumentieren.
- **M5 (D-585):** Register-Kalibrierung als **Verfahren** für alle Sprachen: reproduzierbare
  Mischung aus Enzyklopädie- und Umgangsregister — entschieden (2026-11-26, §4): **kuratierte
  Floors als Standard**, der Korpus-Blend (OpenSubtitles/`wordfreq`, Build-Zeit-only) wird nur
  als dokumentierte Option im Guide geführt; Guide-Sektion, Formel, Review-Liste.
  Heilt auch die `NOUN_REFERENCE_FREQUENCY`-Sättigung (D-227) an der Wurzel —
  der D-545-Handbump („jetzt“) wird zum Regelfall.
- **M6 (D-586):** DE-Konfusables-Audit nach dem EN-D-573-Präzedenz: QWERTZ-Scan, seltene Seite
  heben, Junk-Paare entfernen, Gate-Invariante. Ziel wie EN: **0 stille Risikopaare** außerhalb der
  kuratierten Blacklist.
- **M7 (D-587, entschieden):** Veto-Restklasse: 34 whitelistete, **legitime** seltene EN-Wörter
  (`gab`, `ort`, `mutter`, `praxis`, `spiel`, `grad` …) blockieren als `knownInOtherLanguage`
  deutsche stille Korrekturpfade auf genau diesen Oberflächen. Pruning ist erschöpft (D-569);
  verbleibt nur der code-seitige Frequenz-Schwellen-Veto (Entwurf M10 des alten Plans) — entschieden
  (2026-11-26, §4): Schwelle bauen.
- **M8 (D-588, begleitend ab Runde 1):** Deployment-Pins + Sichtbarkeit: Tests pinnen
  Catalog-Version == `version.txt` (D-579-Lektion) und Reseed-Disziplin für EN-Assets (D-578);
  Änderungsprotokoll pro Runde; Interplay mit `feature-tips.md`; Gerätebestätigungen der
  EN-Datenrunden (D-568–D-576 formal offen) ins Test-Protokoll aufnehmen.
- **M9 (D-589, entschieden: Purge + Lemma-Erbe):** Verb-Schwanz (D-462-Rest): **alle** 5.591 Zeilen mit Frequenz ≤ 1
  sind gemintete Verbformen (`abenteuere` …) — Purge plus M10-Lemma-Erbe (D-590).
- **M10 (D-590, optional, nach M9):** Lemma-Familien-Ranking: chip-only Frequenz-Erbe über die
  bestehende Lemma-Spalte (88.082 Links, 0 verwaist) — `kämen` (52) erbt aus `kommen`, ohne dass
  Daten geminted werden (die `ands`-Lektion von D-568 verbietet Build-Zeit-Minting). Auto-Korrektur
  unverändert, daher regressionsarm.
- **M11 (D-591):** EN-Umgangssprache-Runde: `gonna` 52, `wanna` 30, `ok` 59, `okay` 23, `btw`/`idk`/
  `fyi`/`y’all` fehlen — kleine Floors-Runde + Gate-Erweiterung, fährt mit dem nächsten
  Assets-Bump (Reseed 3→4).
- **M12 (D-592, zuletzt):** Blaupausen-Rollout: Gate-Defaults, Register-Floors und Konfusables-Audit
  als wiederverwendbare Schritte für die ~30 bestehenden Packs; Native-Speaker-Review-Umbrella
  (Progress, Zeile ~1576) bleibt davon unberührt offen.

Aufwand: M1 eine Session, M2 ein bis zwei, M3/M4 je ein bis zwei (Datenrunden mit Review-Listen),
M5 eine (Dokumentation + optional Korpus-Anbindung), M6 eine, M7 eine kleine Code-Runde,
M8 begleitend klein, M9/M10 je eine (entschieden: Purge + Erbe), M11 eine kleine, M12 offen (Paketzug).

---

## 1. Ist-Situation (vermessen 2026-11-26)

### 1.1 Deployment-Stand (warum der Testnutzer „minderwertige“ Chips sieht)

- App: aktuell 1.3.29/629 (D-572). Der Tester steht auf 1.3.18/618 — ihm fehlen v1.3.19–v1.3.29:
  D-544 (Apostroph = Wortzeichen), D-545–D-548 („jetzt“-Chip-Stack, Provider-Proximity-Fix),
  D-557 (A-03 leert die Leiste nicht mehr), D-558 (Konkurrenz-Kleinbuchstaben-Gate), D-572
  (EN-Pinning-Stoppwörter) — und, weil EN gebündelt fährt: die kompletten EN-Datenrunden
  D-567–D-576 plus D-578-Reseed.
- Deutsch: Pack 53/54 (D-567 `Dur`-Frequenz, D-577 Orphan-Lemmas) erreichen ihn erst per
  Pack-Import (D-580: das gehostete Archiv war selbst noch auf 52 — Push-Disziplin ist Teil von M8).

### 1.2 DE-Volumen und -Struktur (`dictionaries/de/dict.tsv`)

- **189.781 Zeilen.** Frequenz-Buckets: ≤ 1: 5.591 (2,9 %) · 2–5: 17.939 (9,5 %) · 6–20: 89.032
  (46,9 %) · 21–100: 54.517 (28,7 %) · 101–1000: 19.988 (10,5 %) · > 1000: 2.714 (1,4 %).
- POS: NOUN 110.356 · ADJECTIVE 30.899 · OTHER 23.074 · VERB 13.960 · NOUN,OTHER 4.732 ·
  NOUN,VERB 691 · Rest kleiner. Lemma-Links 88.082 (46,4 %), **0 verwaist** (D-577).
- Charakter-Sanität sauber: 0 Case-Kollisionen, 0 Bindestrich-Zeilen, **0 Apostroph-Zeilen**,
  9 Zeilen mit Ziffern (`km²`-Klasse), 2.247 ALL-CAPS.
- Top-30 ist bis auf **Rang 21 `nbsp` (134.624)** reine Funktionswort-Front — der Artefakt ist
  Frequenz-Rang-21 und damit der höchstfrequente Nicht-Funktionswort-Eintrag des Wörterbuchs.

### 1.3 Befund A — Register-Skew (der Kernbefund für Chip-Qualität)

Die Frequenzen messen Wikipedia-Register; Chat-/Nachrichten-Register ist die eigentliche Domäne
einer Tastatur. Messwerte (de):

| Wort | Frequenz | Rang | Einordnung |
|---|---|---|---|
| jedoch | 27.743 | 74 | enzyklopädisch, 5× über „ich“ |
| jetzt | 15.000 | 112 | D-545-Handbump — der Blaupausen-Fall |
| ich | 5.153 | 352 | Chat-Top-10-Wort |
| mal | 4.147 | 488 | Chat-Partikel |
| du | 2.226 | 1.043 | Chat-Top-15-Wort |
| bitte (nur „Bitte“) | 472 | 5.847 | Chat-Top-30-Wort |
| wirklich | 646 | 4.312 | Alltag |
| halt (Partikel) | 192 | 13.135 | Alltag |
| **danke** | **49** | **39.989** | Chat-Top-30-Wort |
| **gestern** | **49** | 40.057 | Alltag („heute“ dagegen Rang 89) |
| hallo | 165 | — | Gruß |
| grüße („Grüße“) | 18 | — | Grußformel-Schluss |
| tschüss, okay, haha, super, toll, cool, nix, kriegen, moin, ciao, servus, hey, deins/meins | **fehlen** | — | Chat-Kernbestand |

Konsequenz in der Runtime: Chip-Rang und `CorrectionConfidence` sind log-Frequenz-getrieben
(`forUnknownToken`, `NOUN_REFERENCE_FREQUENCY` 2000/D-227) — Alltagswörter verlieren gegen
enzyklopädische Nachbarn, stille Korrekturen auf Alltagsziele (`dakne`→`danke`) bleiben unter der
MEDIUM-Bar. Genau das fühlt sich als „minderwertige Chips“ an. EN zeigt dasselbe Muster schwächer:
`gonna` 52 (Rang 23.719), `wanna` 30, `ok` 59, `okay` 23; `btw`/`idk`/`fyi`/`y’all` fehlen.

### 1.4 Befund B — Markup-/Artefakt-Reste (DE **und** EN)

- DE: `nbsp` 134.624 (Rang 21), `jpg` 14.825 (Rang 115), `lt` 175, `Trade` 197, `Bull` 157,
  `reg` 131, `amp` 65, `copy` 35, `deg` 14; Domain-/TLD-Fragmente `de` 13.799.
- EN: `nbsp` **23.034 (Rang 75 — Top-100!)**, `GT` 265, `Lt` 186, `ndash` 341, `mdash` 180,
  `amp` 142. Die D-575-Denylist kannte diese Klasse nicht; sie ist in beiden Sprachen lebendig.
- Schaden: Chips für 3+-Zeichen-Präfixe (`nbs…`), Slot-Besetzung in den
  Korrektur-Bucket-Scans (`CANDIDATE_LIMIT` 2000, freq-DESC), Bigramm-Kontexte (`nbsp→km²` 1.323),
  und alle korpus-abgeleiteten Kalibrierungen (`LearnedBigramBoost` wurde z. B. gegen die
  bigram.tsv kalibriert, `nbsp`-Zeilen eingeschlossen).

### 1.5 Befund C — nicht-tippbare Zeilen (DE)

- **333 Zeilen mit eingebetteten Leerzeichen/Klammern:** `des Nord` 916, `des Ost` 782,
  `(des) Französisch` 463, `(des) Russisch` 332, `der Venus` 208, `des Khmer` 203 … — Genitiv-
  Splitter aus dem Tokenisieren. Keine davon ist jemals tippbar (Leerzeichen finalisiert das Token);
  sie belegen Präfix-Buckets (`des…`!) und Korrektur-Scans.
- 221 lowercase NOUN-only-Zeilen, darunter echte Casing-Verletzungen (`text` 5.076 als
  NOUN-only-Kleinform) neben dem Müll der Klasse oben.
- Archaik-Rest (D-206-Politik: Blacklist, nicht Purge): `aßt` 1 (VERB, Lemma `essen`),
  `buk`/`wob`/`gor` je 1.

### 1.6 Befund D — Apostroph-Lücke Deutsch

**0** Apostroph-Zeilen in DE — exakt der Zustand, den EN vor D-571 hatte: D-544 (Apostroph ist
Wortzeichen, ab 1.3.19) läuft ins Leere, „geht’s“/„gibt’s“/„ist’s“/„war’s“/„hat’s“/„kommt’s“ sind
nicht bekannt. Erwartung aus §575 (EN-M4-Korrektur): `gehts`→`geht’s` wird wie `dont`→`don’t`
**Chip-first** sein (INDEL-Kosten halten die stille Bar hoch) — der Wert liegt im Chip, nicht in
der stillen Korrektur, und ist als solcher zu pinnen.

### 1.7 Befund E — Bigramme (`dictionaries/de/bigram.tsv`, `assets/en/bigram.tsv`)

- DE: 79.937 Rows, 10.121 verschiedene Prev-Wörter; Top-200-Unigramme sind 198× abgedeckt — die
  Basis ist gesund **für enzyklopädische Kontexte**. Chat-Kontexte sind taub:
  Continuations von `ich` **39**, `jetzt` **9**, `mal` **32**, `hab` **1**, `danke` **0**
  (Gegenprobe: `der` 5.109, `die` 4.448). Next-Word-Chips nach „Ich “ funktionieren für einen
  Neunutzer damit fast nur aus dem persönlichen Lernen — der Register-Skew von §1.3 wirkt hier
  verdoppelt.
- **1.199 verwaiste Rows** (Wort nicht im dict.tsv): Mischklasse — enzyklopädische Mehrwort-Splitter
  (`max→planck` 285, `der→udssr` 295, `saudi→arabien` 297) und **englische Leakage-Paare in der
  deutschen Tabelle** (`bar→from` 816, `university→of` 591, `academy→of` 269, `museum→of` 261,
  `institute→of` 220). 13/9 Rows mit nicht-alpha-Schlüsseln (`km²`-Klasse).
- EN: 80.000 Rows, seit dem ersten Build **nie refreshet**, Quelle derselbe Wikipedia-Dump
  (`english-dictionary-quality.md` M10-Rest „EN-Bigram-Auffrischung“).

### 1.8 Befund F — Veto-Restklasse (EN-Sonderrolle, konkret)

Der D-106-Veto-Zensus (EN-Frequenz ≤ 20, DE-Frequenz ≥ 300) liegt nach D-569/D-576 bei **34
whitelisteten Oberflächen** — Sweep-Spitze: `gab` (en 7/de 12.521), `ort` (4/4.151), `mutter`
(6/3.226), `sten` (10/2.108), `praxis` (15/1.917), `gar` (6/1.842), `spiel` (11/1.766), `grad`
(15/1.435), `bares`/`barer` (4 und 13 gegen je 1.250), `casanova`, `bip`. Es sind **legitime, aber seltene
englische Wörter** — Daten-Pruning kann sie nicht entfernen, ohne EN-Tippen zu beschädigen, und
Frequenz-Anhebung hilft nicht, weil der Veto nur `knownInOtherLanguage` prüft, ohne Frequenz.
Solange das so ist, bleibt die stille Korrektur deutscher Typos, die genau auf diese 34
Oberflächen landen, tot. Der im alten Plan zurückgestellte Code-Entwurf („M10: Veto mit
Frequenz-Schwelle, z. B. ≥ 50 in der Fremdsprache“) ist die Lösung — entschieden
(2026-11-26, §4): Schwelle bauen.

### 1.9 Befund G — Verb-Schwanz (D-462-Rest)

Alle 5.591 Zeilen mit Frequenz ≤ 1 sind VERB-Zeilen — der konservative D-462-Import (§483) hat
nie attestierte Formen mit Floor 1 geminted (`abenteuere`, `abenteuerst`, …). Die B1/B2-Analyse
liegt in Progress (D-462); die Entscheidung ist gefallen (2026-11-26, §4): Purge plus
M10-Lemma-Erbe. Die Runde gehört in diesen Plan, weil der Schwanz Bucket-Kapazität und Junk-Chips
am langen Ende kostet.

### 1.10 Befund H — Verfahrens-/Code-Befunde

- **Ranking-Konstanten ohne Device-Tuning:** `FUZZY_COST_DECAY` 0.01 (KDoc: „considered starting
  point, not yet device-tuned“), `PREFIX_LENGTH_DECAY` 0.3, Trigram-Backoff 0.4, Wide-Fuzzy-Budget.
  Ohne Golden-Set (M2) ist jedes Tuning Blindflug.
- **Lemma-Spalte ungenutzt:** gelesen wird sie nur in `sameWordFamily()` (D-404-Familien-Veto).
  Kein Familien-Frequenz-Erbe im Ranking — `kämen` (52) verliert gegen enzyklopädische Nachbarn,
  obwohl `kommen` riesig ist.
- **POS fließt nicht in den Ranking-Score** (nur Kapitalisierung, Confidence-Noun-Flag, Split-Gate).
- **DE-Konfusables nie systematisch auditiert:** EN hat D-573 (780 Anhebungen, 134 Entfernungen,
  0 stille Paare); DE hat nur die kuratierte Blacklist plus Einzelfälle (D-330/D-480/D-558).
- **Gate-Lücken:** `quality_gate.py` kennt Artefakt-Denylist und Kontraktions-Floors nur für
  `--lang en`; Deutsch hat keine Denylist (`nbsp` hätte sie gefangen), keine Typability-Prüfung
  (Leerzeichen/Klammern), keine lowercase-NOUN-Prüfung für Kapitalisierungssprachen, keine
  Bigramm-Hygiene.
- **Deployment unverankert:** D-578 (Reseed vergessen) und D-579 (Catalog 52 vs. Pack 53/54)
  sind je als Lektion dokumentiert, aber kein Test pinnt die Invarianten.
- EN-Colloquial/Register und EN-Bigramm-Refresh sind benannte Reste des alten Plans (M10 dort).

---

## 2. Ziele

1. **Top-100 beider Wörterbücher artefaktfrei** und im Gate verankert: `nbsp`-Klasse, Entity-
   Fragmente und nicht-tippbare Zeilen = 0 in DE und EN; Re-Introduktion scheitert am Gate.
2. **Chat-Register messbar vertreten:** kuratierter Messaging-Basiswortschatz mit dokumentierten
   Floors; im Golden-Set steigt die chip@1-Trefferquote für Gruß-/Alltags-Szenen deutlich über die
   M2-Baseline (Zahl je Runde in der History).
3. **Next-Word lebt auch ohne persönliche Historie:** Chat-Funktionswort-Kontexte (`ich`, `du`,
   `hab`, `jetzt`, `mal`) haben jeweils ≥ 50 saubere Continuations; verwaiste und fremdsprachige
   Bigramm-Rows = 0, im Gate verankert.
4. **Auto-Korrektur auf EN-Niveau:** DE-Konfusables-Audit abgeschlossen — 0 stille Risikopaare
   außerhalb der kuratierten Blacklist (D-573-Präzedenz); Veto-Restklasse (§1.8) gelöst per
   Frequenz-Schwelle (entschieden 2026-11-26) und mit Kalibrierfällen regressionsgepinnt.
5. **Apostroph-DE produktiv:** Top-Kontraktionen („geht’s“-Klasse) vorhanden, `gehts`→`geht’s`
   als Chip gepinnt (chip-first-Erwartung aus §575), Satzanfangs-/§6-Wechselwirkung getestet.
6. **Messbarkeit als dauerhafter Zustand:** Golden-Set-Harness + Vorher/Nachher je Runde; die drei
   „not yet device-tuned“-Konstanten sind kalibriert und dokumentiert.
7. **Ankommen garantiert:** Catalog-/Reseed-Invarianten als Tests; jede Datenrunde ist am
   Bestandsgerät nachweisbar (Import-Check im Protokoll, D-580-Muster).

Nicht-Ziele: Neuaufbau des DE-Korpus von Null; Bundled-Trigramme (S-07 bleibt personal-only);
Tier-3-Mini-LLM; die anderen Sprachpakete außer dem M12-Rollout-Rahmen.

---

## 3. Maßnahmen

### M1 — Markup-Artefakt- und Typability-Sweep (DE+EN) — D-581, sofort

- **DE:** `nbsp`, `lt`, `amp`, `reg`, `copy`, `deg`, `Trade`/`Bull`-Klasse entfernen; die
  333 Leerzeichen-/Klammer-Zeilen entfernen (Review-Liste vor dem Anwenden; einzelne wie
  `der Venus` als Compound-Kandidaten prüfen, nicht restaurieren); lowercase-NOUN-Hygiene
  (`text`→`Text`-Klasse, klein halten); Archaik-Rest `aßt`/`buk`/`wob`/`gor` blacklisten
  (D-206-Politik, nicht purgen).
- **EN:** `nbsp`, `GT`, `Lt`, `ndash`, `mdash`, `amp` entfernen — Erweiterung der D-575-Denylist.
- **Review-Klasse (entschieden 2026-11-26, §4):** tippbare Token-Fragmente `jpg` (Rang 115),
  `www`, `http`, `de` werden **gesenkt behalten** (Frequenz ≤ ~200, OTHER-Tag — Nutzer tippen
  „jpg“ in Chats real), Denylist nur für die echte Markup-Klasse.
- **Gate:** `quality_gate.py` bekommt (a) die Denylist-Klasse sprachneutral als Basis-Check,
  (b) die Typability-Prüfung (kein Leerzeichen/`(``)` in der Oberfläche; Apostroph erlaubt),
  (c) für Kapitalisierungssprachen (`--capitalises-nouns`) die lowercase-NOUN-only-Prüfung.
  DE- und EN-Gate laufen danach grün; DE-Runde fährt als Pack 55 (version.txt + zip + Catalog + Push).

### M2 — Golden-Set-Harness (Messbarkeit zuerst) — D-582

- `GermanChatEvaluationTest` nach dem `FusionEvaluationTest`-Muster (reale `dict.tsv`+`bigram.tsv`
  über `DictionaryAssetParser` in den In-Memory-Store; `assumeTrue` wenn der Baum fehlt).
- Bestand: ~200–300 kuratierte Fälle aus echten Chat-Situationen — Gruß-/Abschlussformeln,
  Alltags-Partikel-Szenen, Typos mit erwarteter stiller Korrektur (`dakne`→`danke`-Klasse),
  Typos mit bewusst **erwartetem** Nicht-Eingriff (`knownInOtherLanguage`, `dur`-Klasse),
  Umlaut-/ß-Fälle, Compound-Prefix-Fälle, Kontraktions-Fälle („gehts“), Next-Word-Kontexte
  („Ich “, „hab “, „jetzt “). Fälle als kuratierte TSV im Test-Baum, jede mit Erwartung
  (Chip-Slot, still, kein Eingriff) und Kategorie.
- Metriken: chip@1/chip@3-Trefferquote, stille-Korrektur Precision/Recall je Kategorie;
  **Baseline-Messung auf dem Ist-Stand dokumentieren** (Zahlen in die History-Section der
  ersten Runde), danach Vorher/Nachher je Maßnahme.
- Dasselbe Harness erweitert das Tuning: `FUZZY_COST_DECAY`, `PREFIX_LENGTH_DECAY`,
  Trigram-Backoff und Wide-Fuzzy-Budget werden über den Fallbestand kalibriert (Ziel: die drei
  KDoc-„not yet device-tuned“-Marker in `DictionarySuggestionProvider.kt` werden zu dokumentierten,
  getesteten Werten); Device-Gegenprobe pro Tuning-Runde (§575-Muster).

### M3 — DE-Messaging-Register + Apostroph-Kontraktionen — D-583

- **Kuratierter Floors-Katalog** (~300–600 Einträge, dokumentierte Rangliste nach dem
  D-571-Muster — für Chat-Vokabular gibt es kein korpus-äquivalentes Formen-Paar-Muster):
  Gruß/Abschluss (`danke`, `bitte`, `hallo`, `tschüss`, `grüße`, `moin`, `ciao`, `servus`, `hey`,
  `hi`, `dankeschön`, `gerne`-Anhebung), Chat-Partikel/Alltag (`mal`, `halt`, `eben`, `wohl`,
  `irgendwie`, `eigentlich`, `wirklich`, `vielleicht`, `gestern`, `gleich`, `moment`),
  umgangssprachliche Formen (`kriegen`/`kriegst`/`kriegte`, `nix`, `deins`/`meins`, `haha`/
  `hahaha`/`hehe`, `okay`, `super`, `toll`, `cool`), Pronomen-Anhebung (`ich`, `du`, `wir`-Klasse
  an Chat-Realität koppeln). Lehnwörter bewusst erlaubt (`sorry`, `nice`, `ok` — die
  D-569-Whitelist-Philosophie für die Rückrichtung); Review-Liste vor dem Anwenden.
- **Apostroph-Kontraktionen:** `geht's`, `gibt's`, `ist's`, `war's`, `hat's`, `kommt's`,
  `bleibt's`, `mach's`, `gib's`, `sieh's` … als feste, dokumentierte Rangliste (Frequenz-Floors
  ≥ 1.000-Äquivalent), inkl. §6-Satzanfangs-Fall und D-196-Chip-Case-Test im Golden-Set.
- Oberflächen-Politik: ein Eintrag je Wort (wkey-Fold + D-196-Case-Match genügt);
  `Danke`-Satzanfang über §6-Auto-Caps, nicht über zweite Oberfläche.
- DE-Runde als Pack 56; Golden-Set-vorher/nachher mitliefern (M3 ist die erste Runde, an der
  sich der sichtbare Mehrwert zahlenmäßig zeigt).

### M4 — Bigramm-Revision (DE-Rebuild-Hygiene, EN-Refresh) — D-584

- **DE-Hygiene:** 1.199 verwaiste Rows aufteilen — Mehrwort-Splitter (`planck`, `udssr`,
  `arabien`) prüfen und die **Wörter** ins dict.tsv aufnehmen oder die Row entfernen;
  englische Leakage-Paare (`bar→from`, `university→of`, …) entfernen; `nbsp`-Kontexte fallen
  mit M1 mit; nicht-alpha-Schlüssel (`km²`-Klasse) in dict.tsv-Anhang oder entfernen.
- **Chat-Kontext-Abdeckung:** Anrede-/Gruß-/Alltags-Paare ergänzen (`wie geht's`, `hallo du`,
  `gute nacht`, `alles klar`, `bis bald`/`bis später`, `liebe grüße`, `viele grüße`,
  `macht sinn`, `ich habe`, `ich bin`, `hab dich` …) — als feste dokumentierte Ergänzungsliste
  mit Floors, keine Korpus-Erfindung.
- **Verfahren:** Bigramm-Rebuild mit Satzgrenzen-Wächter und Hygiene-Gate (Verwaiste = 0,
  Nicht-alpha = 0, fremdsprachige-Leakage-Prüfung gegen die eigenen Kernvokabulare) im
  `Language-Contribution-Guide` dokumentieren; EN-Bigramm-Refresh (M10-Rest des alten Plans)
  nach demselben Verfahren, Ziel-Aufwand klein halten (Refresh statt Neuaufbau).
- DE-Runde als Pack 57 (oder mit M3 kombiniert, wenn die Review-Listen gleichzeitig fertig sind).

### M5 — Register-Kalibrierung als Verfahren (alle Sprachen) — D-585

- M3 heilt Deutsch mit kuratierten Floors; M5 macht daraus ein **reproduzierbares Verfahren**:
  dokumentierte Mischformel aus Enzyklopädie- und Umgangsregister. Entschieden (2026-11-26, §4):
  **kuratierte Floors pro Sprache als Standard** (bewährt, billig, transparent); der Build-Zeit-Blend
  mit einem informalen Korpus (z. B. OpenSubtitles-Frequenzlisten oder `wordfreq`-Daten, Lizenz und
  Attribution in `CREDITS.md`, App bleibt offline) wird nur als **dokumentierte Option** geführt —
  keine Korpus-Anbindung in dieser Planrunde.
- Formel, Schwellen und Review-Listen als neue Sektion im `Language-Contribution-Guide`;
  de ist Referenz-Beispiel. Synergie: die `NOUN_REFERENCE_FREQUENCY`-Sättigung (D-227) trifft
  nach der Kalibrierung tatsächlich seltene Nomen statt enzyklopädisch überhöhter Alltagswörter;
  der D-545-Handbump („jetzt“) wird zum Regelfall mit Verfahren.

### M6 — DE-Konfusables-Audit (D-573-Präzedenz) — D-586

- `confusables_scan.py`-Methode auf dem QWERTZ-Grid gegen das aktuelle DE-Wörterbuch; seltene
  Seite anheben (`ceil(safe/75)`-Muster), Junk-Paare entfernen, strittige Fälle in die
  Review-Liste; Ergebnis in die Gate-Invariante (DE: 0 stille Paare außerhalb der kuratierten
  Blacklist `GermanRules.bundledConfusablesBlacklist()`).
- Kalibrierfälle (`aks`/`als`, `konnen`/`kannen`, `dir`/`dur`-Klasse) in den Golden-Set aufnehmen.

### M7 — Veto-Restklasse: Frequenz-Schwelle (entschieden: bauen) — D-587

- Die 34 whitelisteten legitimen EN-Wörter (§1.8) blockieren deutsche stille Korrekturpfade auf
  ihren Oberflächen. Pruning ist erschöpft; Datenheilung ist falsch (legitime EN-Lesarten).
  Übrig bleibt nur der Code-Weg: `knownInOtherLanguage` wird frequenzskaliert (Entwurf M10 des
  alten Plans: Veto erst ab Fremdfrequenz ≥ Schwellwert, Kalibrierung über `CorrectionConfidence`-
  Konstanten-Muster; Verdrahtung `AdaptKeyService.kt` ~Zeile 5617/8910).
- Gegenrisiko benennen und mit Kalibrierfällen pinnen: ein seltenes englisches Wort, das 1-Edit
  von einem deutschen Wort entfernt ist, verliert seinen Schutz (`sue`/`die`-Klassik, D-164).
  Entschieden (2026-11-26, §4): Schwelle bauen. Code-Runde mit Bump; der Schwellwert wird über
  die M2-Golden-Set-Gegenfälle kalibriert, bevor er festgezogen wird.

### M8 — Deployment-Pins + Sichtbarkeit — D-588, begleitend ab Runde 1

- **Test-Pins:** (a) `LanguagePackCatalog`-Version == `version.txt` je gepinnter Sprache
  (D-579-Regression); (b) `BUNDLED_DICTIONARY_VERSION` == `dictionaries/en/version.txt`
  (D-578-Regression — jede EN-Asset-Änderung erzwingt beide Dateien im selben Commit);
  (c) EN-Gate-Artefakt-Erweiterung aus M1 übernimmt die Denylist-Pflege.
- **Protokoll:** je Datenrunde der Nachweis „erreicht Bestandsgerät“ (Pack-Import am Gerät,
  D-580-Muster); die formal noch nicht gerätebestätigten EN-Runden (D-568–D-576) in den
  nächsten Gerätetest mit aufnehmen.
- **Sichtbarkeit:** je Runde eine changelog-fähige Zeile; Interplay mit `feature-tips.md`
  (die Auto-Korrektur-Tipps machen stille Verbesserungen für den Nutzer sichtbar); dem
  1.3.18-Tester das Update + Pack-54-Import als erste Maßnahme zurichten — sein Feedback nach
  dem Update ist die echte Baseline des „minderwertige Chips“-Berichts.

### M9 — Verb-Schwanz: Purge + Lemma-Erbe (entschieden) — D-589

- 5.591 freq-1-Verbzeilen (§1.9). Entschieden (2026-11-26, §4): **(c) Purge + Ersatz durch das
  M10-Runtime-Erbe** — die nie attestierten Formen fliegen (B1/B2-Analyse aus D-462 als
  Grundlage für die Review-Liste), echte seltene Formen werden über D-590 bedient. Die Runde
  ist eine Datenrunde (Purge) mit direkt anschließender M10-Code-Runde.

### M10 — Lemma-Familien-Ranking (optional, nach M9) — D-590

- Chip-only Frequenz-Erbe über die bestehende Lemma-Spalte: für Kandidaten mit niedriger
  Eigenfrequenz und gültigem Lemma-Link fließt ein gedämpfter Anteil der Lemma-Frequenz
  in den Ranking-Score (`max(own, β·F(lemma))`, β device-tuniert über M2). Kein Build-Zeit-Minting
  (die D-568-`ands`-Lektion), keine Änderung an `CorrectionConfidence` — stille Korrektur bleibt
  own-frequenz-basiert, deshalb regressionsarm. Guardrails: kein Erbe in PROPER_NOUN-Familien,
  Cap gegen Top-Frequenz-Dominanz. M10 ist der Ausgleich für die in M9 gepurgen seltenen, aber
  echten Flexionsformen (`kämen`, `gegangen`-Klasse) — der Nutzer hat Purge + Erbe gemeinsam
  entschieden (2026-11-26, §4).

### M11 — EN-Umgangssprache-Runde — D-591

- Kleine Floors-Runde: `gonna`, `wanna`, `ok`, `okay`, `really`-Klasse anheben; `btw`, `idk`,
  `fyi`, `thx`, `pls`, `y'all` (Kontraktion nach D-571-Ranglisten-Muster) ergänzen; Review-Liste;
  EN-Gate um Colloquial-Floors erweitern. Fährt mit dem nächsten Code-Bump als Reseed 3→4
  (D-578-Muster, vom M8-Pin erzwungen).

### M12 — Blaupausen-Rollout auf die Sprachpakete — D-592

- Nachdem DE stabil ist: Gate-Defaults (Typability, Denylist, Verwaiste-Bigramme) laufen für alle
  Packs; Register-Floors und Konfusables-Audit als Guide-Schritte je Sprache; Priorität nach
  Nutzung (de, en fertig; dann el, es, fr …). Der Native-Speaker-Review-Umbrella (Progress)
  bleibt davon unberührt und offen.

---

## 4. Entscheidungen (alle getroffen am 2026-11-26, je der Empfehlung)

1. **Veto-Frequenzschwelle (M7):** ~~ja/nein?~~ — **entschieden: bauen.** `knownInOtherLanguage`
   wird frequenzskaliert (Schwellwert ~50, kalibriert über die `CorrectionConfidence`-Konstanten und
   die D-164-Gegenfälle als Regression im M2-Golden-Set).
2. **Verb-Schwanz (M9):** ~~Purge / Behalten mit Floor / Purge + M10-Erbe?~~ — **entschieden:
   (c) Purge + M10-Erbe.** Die D-568-Minting-Lektion bleibt respektiert (kein Build-Zeit-Minting),
   die echten Formen werden trotzdem bedient.
3. **Register-Verfahren (M5):** ~~Floors oder Korpus-Blend?~~ — **entschieden: Floors als Standard,
   Blend nur Doku.** Kuratierte Floors je Sprache; der Blend wird als Verfahrensoption im Guide
   dokumentiert und erst angeschaltet, wenn ein Pack die Floors-Manpower nicht hergibt.
4. **Tippbare Fragment-Klasse (M1):** ~~behalten oder entfernen?~~ — **entschieden: gesenkt
   behalten.** `jpg`/`www`/`http`/`de` bleiben (Frequenz ≤ ~200, OTHER-Tag), Denylist nur für die
   echte Markup-Klasse.

## 5. Vorgeschlagene Reihenfolge

1. **M1** (D-581) sofort — Quick Win, Gate-Verankerung, DE-Pack 55. — **ERLEDIGT 2026-11-28 (§581):
   DE 189.430/79.393, EN 88.732/79.839, Pack 55 gepusht, Reseed 4, `amp`-Abweichung dokumentiert.**
2. **M2** (D-582) direkt danach — Baseline auf dem bereinigten Stand, dann gilt Vorher/Nachher
   für alle weiteren Runden.
3. **M3** (D-583) — der sichtbare Kern (Register + Kontraktionen), Pack 56.
4. **M4** (D-584) — Bigramm-Revision, Pack 57; EN-Refresh parallel klein.
5. **M5** (D-585) — Verfahrensdokumentation (Blend nur als dokumentierte Option, §4-3 entschieden).
6. **M6** (D-586) — DE-Konfusables-Audit.
7. **M7** (D-587) — entschieden (Schwelle bauen), kleine Code-Runde mit Bump.
8. **M8** (D-588) begleitend ab Runde 1; der 1.3.18-Tester bekommt als allererstes das
   Update + Pack-Import und testet danach erneut.
9. **M9/M10** (D-589/D-590) — entschieden (Purge + Erbe), Datenrunde + direkt anschließende
   Code-Runde.
10. **M11** (D-591) — kleine EN-Runde auf dem Wege.
11. **M12** (D-592) — Rollout, wenn die Blaupause steht.

Jede Runde: eigene D-Nummer, eigener Commit, History/Progress je Runde (ab §581), Review-Liste
vor jedem Datensweep, Golden-Set-Zahlen je Runde (ab M2). Code-Runden bumpen (ab 1.3.30/630),
DE-Datenrunden fahren als Pack 55/56/57 mit Catalog+Push, EN-Asset-Runden erzwingen Reseed-Bump
(M8-Pin).

**Chips-Betrieb:** jede Runde läuft in ihrer eigenen, ruhenden Session (Seitenleisten-Gruppe
„Wörterbuch-Runden“). Ein Chip führt seine Runde nur auf ausdrücklichen Start-Befehl im Chip
aus; davor hält er sich an den Bereitschafts-Protokolltext aus seiner Ersteinrichtung. Parallel-
Runden am gleichen Wörterbuch sind verboten (Review-Listen und Pack-Versionen kollidieren) —
eine Runde nach der anderen, in der Reihenfolge oben.

---

## 6. Referenzen (name-only / projektwurzel-relativ)

- `english-dictionary-quality.md` — der abgeschlossene EN-Zyklus, dessen Muster (Review-Listen,
  Gate-Denylist, reale Daten-Regressionstests, Device-Protokoll) dieser Plan verallgemeinert.
- `feature-tips.md` — Sichtbarkeitskanal für still wirkende Verbesserungen (M8).
- `doc/books/AdaptKey-Progress.md` — Open-TODOs: D-462-Rest (Verb-Tail), Homograph-Sweep
  (D-368-Nachsatz), Native-Review-Umbrella, EN-Gerätebestätigungs-Stubs.
- `doc/recipes/AdaptKey-Language-Contribution-Guide.md` — Pack-/Pipeline-Prozess; M5/M4/M12
  ergänzen ihn (Register-Kalibrierung, Bigramm-Verfahren, Rollout).
- `dictionaries/quality_gate.py`, `dictionaries/confusables_scan.py`, `dictionaries/lemma_check.py`
  — die Gate-/Scan-Werkzeuge, die M1/M4/M6 erweitern.
- `dictionaries/de/` (Pipeline: `extract_wiktionary_nouns.py`/`extract_wiktionary_verbs.py`/
  `extract_wiktionary_adjektive.py`, `konjugation.py`,
  `build_zip.py`), `dictionaries/en/build_en_dict.py` (Verify-Muster für M11).
- `app/src/main/kotlin/de/froehlichmedia/adaptkey/dictionary/DictionarySuggestionProvider.kt`
  (Ranking, nicht-getunte Konstanten — M2), `CorrectionConfidence.kt` (Frequenz-Sättigung — M5/M7),
  `AdaptKeyService.kt` ~Zeile 5617/8910 (D-106-Veto — M7), `SqliteDictionaryStore.kt` (Lemma-/POS-
  Konsum — M10), `LanguagePackCatalog.kt` + `DictionaryLoader.kt` (Deployment-Pins — M8).
- Tests: `FusionEvaluationTest.kt` (Harness-Muster für M2), `EnglishDictionaryQualityEvaluationTest.kt`
  (Daten-Pin-Muster für M1/M6/M11), `JetztRankingEvaluationTest.kt` (Einzelfall-Muster).
