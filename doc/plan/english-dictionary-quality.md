# AdaptKey — Plan: Englisch-Wörterbuch-Qualität (Pruning, Kontraktionen, Veto-Sweep, EN-Politik)

**Status: IN ARBEIT — Nutzer-Go 2026-11-21 ("Ziehe ihn komplett durch, committe nach jeder Phase").
M1 (D-567), M2 (D-568), M3 (D-569) und M11 (D-570) sind umgesetzt und gecommitted; die übrigen
Runden folgen in der Vorgeschlagenen Reihenfolge (§5).**
Identifier-Zeiger: die ursprüngliche Reservierung D-559..D-566 ist hinfällig — D-559..D-566 gehören
inzwischen zum Plan `feature-tips.md` (Themenliste `doc/books/AdaptKey-Topics.md`). Dieser Plan läuft
auf **D-567 ff.**: M1=D-567 (erledigt), M2=D-568, M3=D-569, M11=D-570, M4=D-571, M6=D-572, M5=D-573,
M7=D-574, M8/M9=D-575. Versionen ab 1.3.29/629 (erste Code-Runde ist M6), History ab §567.

Origin: Nutzer-Anfrage (2026-11-21) nach dem D-558-Gerätetest. Die D-558-Verhaltensziele grünen
(„mit" vorne für „mut", „dur" bleibt „dur"), aber die stillen Korrekturen „mut"→„mit" und
„dur"→„dir" feuern nicht — Ursache: das D-106-Fremdsprachen-Veto blockt beide Tokens, weil das
**englische** Bündel-Wörterbuch die Rausch-Artefakte `mut` (Frequenz 4) und `Dur` (Frequenz 11)
kennt. Der Nutzer: das englische Wörterbuch soll „mal genauer unter die Lupe genommen" werden,
Rauschwörter so weit wie möglich entfernen, den Blick „über die Rauschwörter hinaus" weiten —
die EN-Qualität ist essentiell für alle Nutzer des Pakets (Autokorrektur, Chips) **und** für alle
anderen lateinischen Sprachen (D-106-Veto + A-03-Klassifikator-Routing). Dieser Plan bündelt die
Ist-Analyse, die Maßnahmen und die offenen Details; der ursprüngliche Korrekturvorschlag
(EN-Artefakte + DE-Dur-Frequenz) ist darin Maßnahme M1.

---

## 0. Kurzfassung

- **M1 (sofort, eigene Datenrunde D-559):** EN-Artefakte `mut`/`Dur` entfernen; DE-`Dur` 501→100
  (Pack 53). Bringt „mut"→„mit" (1304×, Confidence 1,0) und „dur"→„dir" (127×, 0,78 ≥ 0,75) bei
  MEDIUM zum stillen Feuern. Kein Code, keine Version-Bump-Pflicht (Datenrunde, D-545-Präzedenz).
- **M2–M3 (Kern-Pruning):** zwei chirurgische Sweep-Klassen mit Review-Liste vor dem Anwenden —
  Lemma-Flexions-Artefakte von Funktionswörtern („ands", Frequenz 292.861, Rang 5!) und die
  DE↔EN-Überschneidungs-Opfer (381 deutsche Tokens mit de-Frequenz ≥ 300, deren stiller Autokorrektur-Pfad
  durch winzige EN-Einträge zu Totalsperren wird: „eine", „ist", „sich", „nach", „durch", „dir", …).
- **M4 (Kontraktionen):** das EN-Wörterbuch enthält **null** Apostroph-Einträge — keine einzige
  englische Kontraktion („don't", „it's", „can't" …). D-544 macht den Apostroph zum Wortzeichen,
  das Wörterbuch liefert dazu nichts. Top-Kontraktionen ergänzen, Frequenzen als feste,
  dokumentierte Rangliste (entschieden).
- **M5 (Confusables-TODO):** die im `EnglishRules`-KDoc registrierte offene Frequenzkorrektur der
  **alle** ~1.384 QWERTY-Konfusable-Paare (`fir`/`for`, `otter`/`other`, `nave`/`have`) —
  Listen-getrieben, D-330-Präzedenz (`dein`/`sein`) (entschieden: alle Paare).
- **M6 (EN-Pinning-Stoppwörter):** `EnglishRules` hat keine `pinnedContinuationStopwords` — nach
  „the/of/and …" pinnen Wiktionary-Korpus-Nomen die Leiste (das deutsche D-503-Problem, ungelöst
  für Englisch). Liste nach `GermanRules`-Vorbild.
- **M7 (EN-Großschreibungs-Politik):** 4.566 noun-only-Einträge („American", „China" …) sind
  §6-Regel-3-Kandidaten mit echt falschen Ergebnissen („china vase" → „China vase"). Entschieden:
  **(a) Re-Tagging nach D-441** mit kleiner expliziter Ausnahmeliste.
- **M8/M9 (Pipeline + Regression):** die Once-Skripte (`extract_wiktionary.py`,
  `merge_wiktionary.py`, `fix_bare_noun.py`) zu einem reproduzierbaren Build für
  `app/src/main/assets/en/` festigen (Version-Stempel, Verify-Schritt, `build_zip.py`-Präzedenz);
  EN-Regressionstest über das reale Bündel-Diktat (mut→mit, dur→dir, Top-25-Sanität ohne „ands",
  Kontraktionen, Veto-Opfer-Liste leer) + `quality_gate.py`-Erweiterung für EN.
- Später/optional (M10): D-106-Veto mit Frequenz-Schwelle als Code-Alternative (bewusst zurückgestellt —
  Daten-Pruning bevorzugt); EN-Morphologie-Irregularien; EN-Bigram-Quelle dokumentieren.
- **M11 (neu, 2026-11-21): EN-Namen- und Proper-Noun-Sweep nach dem deutschen Präzedenz** (§1.5,
  §4 Frage 7): Personennamen **vollständig** raus (entschieden, inkl. Spitzen), Mehrwort-Splitter
  und Mojibake-Zeilen raus, Behalten-Klassen (Monate, Länder, Städte, Demonyme) bestätigen,
  Berufswort-Zwillinge (smith, miller, …) als lowercase-Einträge ergänzen.

**Entscheide des Nutzers zu §4 (2026-11-21):** (1) Junk-Schwanz **behalten**. (2) M3-Schwellen wie
empfohlen (EN-Frequenz ≤ 20, Homograph-Whitelist). (3) Kontraktions-Frequenzen als feste,
dokumentierte Rangliste. (4) M7-Politik **(a) Re-Tagging** nach D-441. (5) **M1 sofort einzeln**.
(6) M5 **alle** ~1.384 Paare.

Aufwand: **M1 eine Session; M2/M3/M11 zwei bis drei Sessions (Pruning-Kern); M4, M5, M6, M7 je
eine; M8/M9 begleitend** (§5).

---

## 1. Ist-Situation (vermessen 2026-11-21, `app/src/main/assets/en/dict.tsv`)

### 1.1 Volumen und Frequenzverteilung

- **116.248 EN-Einträge** (DE: 189.781). Frequenz-Buckets: ≤1: 661 · 2–5: 23.474 · 6–20: 46.709 ·
  21–100: 27.392 · 101–1000: 14.978 · >1000: 3.034. D. h. **~20,8 % des Wörterbuchs (24.135
  Einträge) hat Frequenz ≤ 5**; 8.165 Einträge ≤ 3.
- Charakter-Sanität ist gut: 0 Einträge mit Ziffern, 2 mit Bindestrich, 0 Case-Kollisionen
  (gleiches Lowercase, verschiedene Schreibungen), 2.100 Nicht-ASCII (`café` & Co., legitim),
  3.775 All-Caps-Akronyme (US/II/BC/DNA …, legitim für EN).

### 1.2 Artefakt-Klassen (fundstellen)

1. **Funktionswort-Flexions-Artefakte (schwerstgewichtet):** `ands` steht mit **Frequenz 292.861 auf
   Rang 5** — vor `to`, `is`, `was` — als Plural-Form von `and` (718.841) mit Lemma-Link. Entstanden
   im Morphologie-Schritt von `dictionaries/en/merge_wiktionary.py` (Plural-Ergänzung mit
   Median-Verhältnis-Frequenz, §322-Muster), angewandt auf Wiktionary-Substantiv-Zeilen, die es
   eigentlich gar nicht geben darf. Gleiche Klasse verdächtig: `ins` (119, Lemma `in`).
2. **Deutsche Leakage-Tokens im EN-Wörterbuch:** `die` 1.179, `der` 778, `und` 217, `Das` 151,
   `Dar` 127, `Ich` 42, `ist` 16 … — keine echten englischen Wörter (Ausnahme `die`/`das`-Art
   Homographen sind separat zu prüfen; `die` als englisches Verb ist real und hat 11.775 — die
   1.179-Zeile ist die Duplikat-/Leakage-Zeile derselben Oberfläche? Hier entscheidet die
   Review-Liste, siehe M3).
3. **Null Kontraktionen:** kein einziger Apostroph-Eintrag im Wörterbuch. Für Englisch fehlen damit
   die frequentesten Wortformen neben den Top-Function-Words (`don't`, `it's`, `can't`, `I'm`,
   `isn't`, …). D-544 (Apostroph ist Wortzeichen) läuft damit ins Leere.
4. **Junk-Schwanz mit echter Morphologie-Vererbung:** freq ≤ 3: 8.165 Einträge, Sample: `tibials`,
   `Uranians`, `Cubanos`, `polytetrafluoroethylenes`, `compartmentalizations` — seltene, aber
   teils reale Plurals/Verbformen generiert aus jedem Wiktionary-Headword ohne Frequenz-Boden.
5. **4.566 noun-only-Einträge** (Regel-3-Kandidaten, nicht PROPER_NOUN): `American`, `English`,
   `European`, `John`, `German`, `Roman`, `York`, `China`, `Latin`, `Spanish` — Demonyme (im
   Englischen immer groß) gemischt mit echten Proper-Nouns und Polysemen ohne lowercase-Zwilling
   (0 Case-Kollisionen!): getipptes „china" (Porzellan) force-capitalisiert zu „China".
6. **D-503-Pinning ohne EN-Stoppwörter:** `EnglishRules` erbt die leere Liste — nach `the`/`of`/
   `and` pinnen Korpus-Nomen die Leiste (das exakte Problem, für das `GermanRules` seine Liste hat).
7. **Offenes Confusables-TODO:** der QWERTY-Scan fand ~1.384 Tastatur-Nachbar-Paare über der
   Chip-Schwelle; laut `EnglishRules`-KDoc ist die richtige Heilung Frequenzkorrektur (D-330-Präzedenz),
   bewusst nicht umgesetzt und als offenes TODO in `doc/books/AdaptKey-Progress.md` registriert.

### 1.3 Der Schaden, quantifiziert

- **D-106-Veto-Opfer: 381 deutsche Tokens** mit de-Frequenz ≥ 300, die im EN-Wörterbuch mit
  Frequenz ≤ 20 existieren — für alle gilt: stiller Autokorrektur-Pfad in deutschen Texten
  **komplett tot** (`knownInOtherLanguage` → `suppressAutocorrect`, `AdaptKeyService.kt` Zeile
  5617, vor jeder Korrekturensuche). Spitze der Liste: `eine` (171.332/12), `ist` (169.180/16),
  `sich` (159.213/10), `nach` (113.028/14), `bei` (98.600/17), `durch` (94.303/6), `nicht`
  (88.147/16), `sie`, `zum`, `wie`, `aber`, **`dir` (12.713/14)**, `Stadt`, `kann`, `sein`,
  `Zeit`, `drei`, `Ende`, `ohne`, `dazu` … Der Nutzerfall „mut" (EN-Frequenz 4) und „dur"
  (EN-Frequenz 14 — EN-`Dur` ist case-insensitiv die gleiche Sperre) sind genau diese Klasse.
- **EN-Nutzererlebnis direkt:** Rang-5-Artefakt `ands` in jeder Vorschlagsleiste; fehlende
  Kontraktionen in jedem zweiten Satz; Pinning-Flut nach jedem Artikel.
- **Andere lateinische Sprachen:** jede installierte G-01-Sprache erbt dieselbe Veto-Mechanik
  gegen das EN-Wörterbuch (`providers.any { language != activeLang && isKnownWord(token) }`) —
  EN-Rauschen blockiert Autokorrektur in *aller* installierten Sprachen, nicht nur Deutsch.
  Außerdem ist Englisch das A-03-Klassifikator-Fallback (D-557-Kontext): EN-Profiler-Störung
  zahlt überall.

### 1.4 Pipeline

- Quellen: `dictionaries/en/wiktionary_{nouns,verbs,adjectives,prepositions}.tsv` + Einmal-Skripte
  `extract_wiktionary.py` / `merge_wiktionary.py` (Tagging + Flexions-Vervollständigung mit
  Median-Verhältnis-Frequenzen, §322-Muster) / `fix_bare_noun.py` (hat 36.569 Einträge zu
  `NOUN,OTHER` geformt; 4.566 noun-only blieben übrig).
- Auslieferung: `app/src/main/assets/en/{dict,bigram,hints}.tsv` direkt in der APK (Englisch ist
  kein Sprachpaket — kein version.txt, kein Pack-Update-Pfad; Updates fahren mit der App-Version).
  `hints.tsv` ist leer (a=;o=;u=;s= — für EN korrekt), `bigram.tsv` 80.000 Zeilen mit gesunder
  Spitze (`of the` 251.538 …), Lemma-Links: 43.658 Einträge (37,5 %).
- Abstand zur deutschen Pipeline: DE hat ~15 Skripte (Konjugation, Deklination, Stammformen,
  Lemma-Repair, Quality-Gate, Clean/Confusables-Scans, Pack-Build); EN hat 3 Einmal-Skripte und
  keinen reproduzierbaren Build mit Verify-Schritt.

### 1.5 Namen-Analyse (nachgereicht 2026-11-21, Nutzer-Hinweis: deutscher Präzedenz „Müller/Schuster")

- **34.148 Einträge mit PROPER_NOUN-Tag, davon 29.392 PROPER_NOUN-only.** Klassensplit des Bestands
  (Top nach Frequenz + Zufallsprobe): Länder/Regionen/Städte (Germany 7.001, England, France,
  India, China, America, Africa, London 6.011, York 6.204, Texas, Kansas, Athens), **Monate (alle
  12: March 7.227, January 7.043, October …)**, Demonyme/Sprachen (American 15.027 — als NOUN-only
  unter den 4.566, British 12.534, English 11.176, French, German, Greek, Roman, Soviet, Jewish,
  Latin, Chinese, Spanish), **Personen-Vornamen** (John 10.247, Christian 5.705, William 4.516,
  James 4.242, Charles 4.121, David, George, Henry, Paul, Robert, Thomas, Alexander, Richard,
  Peter, Joseph, Michael, Edward, Jean, Mary, Elizabeth …; 167 Treffer allein gegen eine kleine
  Standard-Vornamenliste), **Nachnamen** (Smith 2.068, Martin, Jones, Lee, Johnson, Jackson,
  Lewis, Shah, Adams, Davis, Williams, Wilson, Carter, Russell, Friedrich(!), Campbell, Taylor,
  Miller 650 …; 83 Listen-Treffer), Abkürzungen (II 7.939, BC, SSL, HDMI, ICJ, HDI …).
- **Der Müller/Schuster-Schutz existiert im EN-Wörterbuch null:** **0** PROPER_NOUN-only-Einträge
  haben eine Gattungslesart am selben Lowercase-Key — die Berufswort-Zwillinge (`smith`, `miller`,
  `baker`, `carpenter`) fehlen durchweg. Die deutschen Purge-Fälle haben hier kein englisches
  Fundament.
- **Zufallsprobe aus PROPER_NOUN-only (Frequenz ≤ 200) ist nahezu vollständig
  Wikipedia-Biographien/-Orte:** Sherborne, Foner, Poliakoff, Tarchaneiotes, Nolte, Segre,
  Katherine, Bertha, Bucer, Farid, Copsey, Wenzel, Logan, Lula, Peletier, Pericles, Gratian,
  Bourne, Rehoboth, Umpqua … Der Löwenanteil der 29.392 ist damit genau die Klasse, die im
  Deutschen damals geflogen ist.
- **Zwei neue Artefakt-Unterklassen:** (a) **Mehrwort-Eigennamen-Splitter** — `Los` (1.954) und
  `Angeles` (1.643) stehen einzeln („Los Angeles" wurde tokenisiert zersägt; gleiches Muster
  droht bei `San`/`Francisco`, `New`/`York` — York 6.204); (b) **Mojibake-Zeilen** — encoding-
  beschädigte Nicht-ASCII-Einträge (`Germn` für Germán, `gir` für Ægir; von den 2.100
  Nicht-ASCII-Einträgen insgesamt ein Teil).
- Diese Sweep-Klasse ist Autokorrektur-neutral (Namen waren nie Veto-Opfer), betrifft aber
  direkt Chips, §6-Regel-3-Zwangskapitalisierung und den A-03-Klassifikator-Input; Umfang
  Richtung mehrere Zehntausend Zeilen, exakte Zahl entsteht über die Review-Liste (M11).

---

## 2. Ziele

1. **Kein deutscher (oder G-01-)Frequent-Vokabel-Autokorrektur-Pfad mehr durch EN-Rauschen blockiert** —
   die Veto-Opfer-Liste der Kur greift auf 0.
2. **EN-Chips und EN-Autokorrektur auf nativem Niveau:** Top-25 ohne Artefakte, Kontraktionen
   vorhanden, Pinning nach Artikeln ruhig, Confusables-Offenes-TODO abgearbeitet.
3. **Englische Großschreibung korrekt:** kein „china"→„China", Demonyme weiterhin immer groß —
   mit einer entschiedenen, dokumentierten Politik (§4 Frage 4).
4. **Reproduzierbarer EN-Build** mit Verify-Schritt und Version-Stempel, dokumentiert im
   `doc/recipes/AdaptKey-Language-Contribution-Guide.md`.
5. **Regressionssicherheit:** EN-Regressionstest über das reale Bündel-Diktat, Quality-Gate für EN.

Nicht-Ziele: Umstellung des D-106-Vetos auf Frequenz-Schwellen (M10, zurückgestellt — Pruning
bevorzugt, weil es das Rechtlose an der Wurzel entfernt); Neuaufbau des EN-Korpus von Null.

---

## 3. Maßnahmen

### M1 — Sofort-Paket (D-559, Datenrunde, keine Code-Änderung)
- EN `mut` (4) und `Dur` (11) aus `app/src/main/assets/en/dict.tsv` entfernen.
- DE `Dur` 501 → 100 in `dictionaries/de/dict.tsv`; `dictionaries/de/version.txt` 52 → 53;
  `build_zip.py` → neues `language-packs/adaptkey-lang-de.zip`.
- Wirkung bei MEDIUM: „mut"→„mit" 1304× → 1,0 ✓ still; „dur"→„dir" 127× → 0,78 ✓ still;
  D-558-Gate bleibt scharf (0,78 ≥ Chip-Schwelle 0,30 → keine erzwungene Großschreibung);
  Kalibrier-Fälle (Ohren-Floor 70×, `komplezz`, `ddr`) unberührt.
- Prüfungen: Quality-Gate-Skripte von DE auf den DE-Schnitt laufen lassen; EN-Schnitt gegen die
  EN-Confusables-Liste (beide Einträge sind dort nicht verankert — verifizieren); volle Suite.

### M2 — Funktionswort-Flexions-Artefakte (EN)
- Sweep: alle Einträge, deren Lemma ein englisches Funktionswort ist (`and`, `the`, `of`, `in`,
  `to`, `a`, `that`, `is`, `was`, `for`, `with`, …) oder die als Flexionsform eines solchen
  erkennbar sind (`ands`, `ins`), entfernen — Review-Liste vor dem Anwenden (erwartbar: eine
  Handvoll bis wenige Dutzend, aber mit maximaler Wirkung durch die Top-Ränge).
- Danach Top-25-Sanität im Regressionstest pinnen (M9).

### M3 — DE↔EN-Überschneidungs-Sweep (die 381+ Veto-Opfer kur)
- Generierung: EN-Einträge mit **EN-Frequenz ≤ 20**, deren Lowercase in
  `dictionaries/de/dict.tsv` mit de-Frequenz ≥ 300 liegt → Review-Liste (`scratchpad/`-Zwischenartefakt,
  de-Wort, de-Freq, EN-Freq, Beispielzeile).
- Enfernen nur nach Whitelist-Gegenprüfung „ist das wirklich ein englisches Wort?“ — Homographen
  mit hoher eigener EN-Frequenz (`die` 11.775) bleiben; strittige Fälle in die Review-Liste.
- Erfolgskennzahl: Veto-Opfer-Zählung (dasselbe Skript) → 0.

### M4 — Kontraktionen
- Top-EN-Kontraktionen als Einträge ergänzen (don't, it's, I'm, can't, isn't, wasn't, didn't,
  won't, couldn't, shouldn't, wouldn't, that's, what's, let's, he's, she's, they're, we're,
  you're, I've, I'll, we'll, doesn't, ain't, … — Liste im Review).
- Frequenzen als **feste, dokumentierte Rangliste** (entschieden; für Kontraktionen gibt es kein
  korpus-äquivalentes Formen-Paar-Muster).
- End-to-End-Prüfung des D-544-Pfads: getipptes „dont" → Korrektur „don't" (Cost-2, Confidence
  über MEDIUM-Auto-Bar durch Top-Frequenz), Chip, Großschreibung am Satzanfang, Lernen.

### M5 — Confusables-Frequenzkorrekturen (offenes TODO abarbeiten)
- Die ~1.384 Paare aus dem bestehenden QWERTY-Scan (`dictionaries/confusables_scan.py`-Methode auf
  dem QWERTY-Grid, siehe `EnglishRules`-KDoc) durchgehen; die seltene Seite gezielt anheben, wo das
  Risiko real ist (D-330 `dein`/`sein`-Präzedenz), Listen-getrieben mit Review.
- Ziel: die Blacklist-Notlösung (`ij`, `iz`, `iy`, `ae`, `ne`) bleibt minimal, der Rest wird durch
  echte Frequenzverhältnisse geheilt.

### M6 — EN-Pinning-Stoppwörter
- `EnglishRules.pinnedContinuationStopwords()` nach `GermanRules`-Vorbild: englische Artikel,
  Präpositionen, Konjunktionen, Pronomen (`the, of, and, in, to, a, that, is, was, for, with, on,
  at, by, from, as, it, or, an, be, this, his, not, but, they, we, you, he, she, its, their, …`).
- Kontrast-Test im EN-Regressionstest: nach `the` keine Wiktionary-Nomen-Pins mehr, große
  Korpus-Kontinuationen bleiben informativ (D-503-Originalbegründung).

### M7 — EN-Großschreibungs-Politik (Regel 3 & Demonyme)
- Bestandsaufnahme der 4.566 noun-only-Einträge in Klassen: Demonyme/Adjektive (`American`,
  `German`, `Spanish`), Polyseme ohne lowercase-Zwilling (`China`/„china Porzellan"),
  Abkürzungs-Zwitter (`II`, `BC`) — Personennamen sind bis dahin über M11 aus dem Bestand.
- Entschiedene Politik: **(a) Re-Tagging nach D-441** mit kleiner expliziter Ausnahmeliste
  (Kalibrierbeispiele: „china vase" bleibt klein; „american south" → „American"). (b) ein
  EN-eigener Kapitalisierungs-Hook ist nur für den Fall reserviert, dass (a) in den
  Kalibrierbeispielen scheitert.
- Umfang bewusst klein halten: die Politik gilt nur für die auditierte Liste, keine globale
  Ummarkierung.

### M11 — EN-Namen- und Proper-Noun-Sweep (deutscher Präzedenz „Müller/Schuster", §1.5)

- **Klasse 1 — entfernen (entschieden, §4 Frage 7: vollständig, wie im Deutschen):**
  Personen-Vornamen und -Nachnamen ohne Gattungslesart (der Bestand hat
  **null** Berufswort-Zwillinge, siehe §1.5) — vom Spitzen-Namen `John` (10.247) bis zum
  Biografie-Langtail der Zufallsprobe; plus Mehrwort-Eigennamen-Splitter (`Los`, `Angeles`, und
  Review für `York`/`San`/`Francisco`-Muster) plus Mojibake-Zeilen (`Germn`, `gir`).
- **Klasse 2 — Behalten-Klassen bestätigen:** Monate (alle 12), Länder/Regionen/Städte
  (Germany, England, London …), Demonyme/Sprachen (American, British, English …), Akronyme
  (II, BC, SSL …). Diese sind im Englischen legitim groß und nutzenbringend (Autocap,
  D-106-Veto-neutral).
- **Klasse 3 — Berufswort-Zwillinge ergänzen statt Namen:** für die realen englischen
  Gattungswörter unter den Nachnamen (`smith`, `miller`, `baker`, `carpenter`, `tailor`/`taylor`
  klären, `mason`, `porter`, `hunter`, `cook`, `butler`, `warden` …) werden lowercase-Einträge
  aufgebaut, wo sie fehlen — die Kapitalform fliegt, das Gattungswort kommt herein (exakt die
  deutsche Müller/Schuster-Lektion).
- **Review-Liste vor dem Anwenden** (scratchpad-Zwischenartefakt), Kategorie pro Zeile;
  Tagging der Behalten-Klasse prüfen (Monate/Länder als PROPER_NOUN — ok; Demonyme sind teils
  NOUN-only, das ist M7s (a)-Re-Tagging-Thema).
- Interaktion mit M7: der Sweep verkleinert die Regel-3-Kandidatenmenge auf die legitimen
  immer-groß-Klassen; das „china"-Polysem-Problem bleibt M7-thema.

### M8 — Pipeline-Festigung
- Aus den Once-Skripten einen reproduzierbaren EN-Build machen: Quellen →
  `app/src/main/assets/en/` mit Version-Stempel (neue Datei `dictionaries/en/version.txt`) und
  Verify-Schritt (Byte-Vergleich, `build_zip.py`-Präzedenz), dokumentiert im
  `doc/recipes/AdaptKey-Language-Contribution-Guide.md`.
- EN-Bigram-Quelle dokumentieren (woher stammen die 80.000 Zeilen, wie aktualisierbar).

### M9 — EN-Regressionstest + Quality-Gate
- JVM-Test über das reale `assets/en/dict.tsv` (FusionEvaluationTest-Muster): Top-25 ohne
  `ands`; `mut`→`mit` und `dur`→`dir` feuern (M1-Nachweis, sobald EN-DICT die Kur durch hat —
  vor M1 negativ pinnen, um die Veto-Opfer zu dokumentieren); Kontraktionen korrigieren;
  Veto-Opfer-Liste (M3) = 0.
- `dictionaries/quality_gate.py` um EN-Checks erweitern (Frequenz-Böden, Funktionswort-Flexionen,
  deutsche Leakage-Zeilen, Apostroph-Coverage-Spitze).

### M10 — Später/optional (bewusst zurückgestellt)
- D-106-Veto mit Frequenz-Schwelle als Code-Alternative (`knownInOtherLanguage` verlangt z. B.
  Frequenz ≥ 50 in der Fremdsprache) — Pruning (M2/M3) macht das überflüssiger; nur falls neue
  Rausch-Fälle zeigen, dass Pruning allein nicht skaliert.
- EN-Morphologie-Irregularien prüfen (`went`, `children`, `better`, `worse` — Coverage und Tags).
- EN-Bigram-Auffrischung aus der selben Korpus-Pipeline wie DE.

---

## 4. Offene Fragen

**Entschieden (2026-11-21):** (1) Junk-Schwanz behalten. (2) M3-Schwellen wie empfohlen
(EN-Frequenz ≤ 20, Homograph-Whitelist gegen hohe DE-Frequenz). (3) Kontraktions-Frequenzen als
feste, dokumentierte Rangliste. (4) M7-Politik (a) Re-Tagging nach D-441 mit kleiner expliziter
Ausnahmeliste. (5) M1 sofort einzeln. (6) M5 alle ~1.384 Paare. (7) EN-Personennamen vollständig
entfernen wie im deutschen Präzedenz (inkl. Spitzen), Berufswort-Zwillinge ergänzen.

1. ~~Junk-Schwanz-Politik~~ — entschieden: behalten.
2. ~~M3-Schwellen~~ — entschieden wie empfohlen.
3. ~~M4-Frequenzquelle~~ — entschieden: feste Rangliste.
4. ~~M7-Politik~~ — entschieden: (a) Re-Tagging.
5. ~~Ship-Pfad~~ — entschieden: M1 sofort einzeln.
6. ~~M5-Umfang~~ — entschieden: alle Paare.
7. ~~NEU (M11): EN-Personennamen — vollständig raus oder Langtail-Schwelle?~~ — entschieden
   (2026-11-21): **wie im Deutschen vollständig raus**, inkl. Spitzen (`John` 10.247, `Smith`
   2.068) — das Autocap „john"→„John" ist der einzige Gegenwert und hält den Preis
   (Zehntausende Biografie-Zeilen in Chips, Lexikon, Klassifikator-Input) nicht wert; der
   deutsche Präzedenz hat sich als „bedeutende Bereinigung" bewährt. Klassen 2 (Monate/Länder/
   Demonyme behalten) und 3 (Berufswort-Zwillinge `smith`/`miller`/… ergänzen) wie unter M11
   beschrieben.

---

## 5. Vorgeschlagene Reihenfolge

1. **M1** als D-559 (Sofort-Paket, Gerätetest mit 1.3.29 oder dem Folge-Build).
2. **M2 + M3 + M11** als eine Pruning-Runde mit Review-Listen (Veto-Zähler → 0; Namen-Sweep,
   Splitter, Mojibake) — der Qualitätskern.
3. **M4** (Kontraktionen) als eigene Runde — eigener End-to-End-Pfad (D-544).
4. **M6** (Pinning-Stoppwörter) als kleine Code-Runde.
5. **M5** (Confusables-TODO) als Listen-Runde.
6. **M7** (Großschreibungs-Politik, Re-Tagging) — nach M11, auf dem geschrumpften Bestand.
7. **M8 + M9** begleitend: Pipeline-Festigung und Regressionstest mit jeder Änderung mitziehen,
   Quality-Gate-Ausbau als Abschluss der großen Runde.

Jede Runde: eigene D-Nummer, eigener Commit, Bump nur bei Code-Änderungen (Datenrunden ohne,
D-545-Präzedenz), History/Progress je Runde. Identifier-Zuordnung (korrigiert 2026-11-21 gegen die
Themenliste — die ursprüngliche Reservierung D-559..D-566 kollidierte mit Plan `feature-tips.md`):
**M1=D-567, M2=D-568, M3=D-569, M11=D-570, M4=D-571, M6=D-572, M5=D-573, M7=D-574, M8/M9=D-575**;
Versionen ab 1.3.29/629 (nur M6 ist Code), History ab §567 — wie in `e314b90e` (feature-tips)
bzw. `1dcdde94` beschrieben.

---

## 6. Referenzen (name-only / projektwurzel-relativ)

- `AdaptKey-Progress.md` — offenes EN-Confusables-TODO (M5), D-558-Stand, Identifier-Zeiger.
- `AdaptKey-History.md` §322 (§322-Kalibrierungsmuster), §565/§566 (D-557/D-558).
- `AdaptKey-Spec.md` §6 (D-441-Tagging-Politik), D-106-Abschnitt (Veto), S-07/D-503 (Pinning).
- `app/src/main/kotlin/de/froehlichmedia/adaptkey/language/EnglishRules.kt` (M5-/M6-Hebel),
  `GermanRules.kt` (Stoppwort-Vorbild), `AdaptKeyService.kt` Zeile 5617/8910 (Veto-Verdrahtung).
- `dictionaries/en/` (Quellen + Skripte), `dictionaries/de/` (Vorbild-Pipeline, `build_zip.py`),
  `dictionaries/quality_gate.py`, `dictionaries/confusables_scan.py`.
- `doc/recipes/AdaptKey-Language-Contribution-Guide.md` (§8 — per-language Tagging-Entscheidung),
  `doc/recipes/fdroid-release.md` (Ship-Pfad).
