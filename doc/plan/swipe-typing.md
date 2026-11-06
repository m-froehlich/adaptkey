# AdaptKey — Plan: Swipe/Glide-Typing (G-09, Entscheidungen ab D-516)

**Status: IN UMSETZUNG (2026-11-06). R1 (D-516, Erkennungskern, §540) und R2 (D-517, komplette
Erstfreigabe v1.2.106, §541) erledigt — R2 noch nicht geräte-bestätigt.** Ausstehend: R3 (D-518,
begrenzte Geräte-/Community-Kalibrierungsrunde mit Abschlusskriterium). Alle acht offenen Punkte sind
entschieden (Nutzer-Calls, §8). Diese Datei ist in sich geschlossen:
`.kilo/command/start-prompt.md` + dieses Dokument reichen einer frischen Session zur Umsetzung.
Origin: Community-Anfrage (2026-11-05) — Glide-Typing fehle als Kern-Feature; Nutzer-Calls vorab:
opt-in (Schalter, default **aus**), bei aktivem Glide entfällt der Page-Wechsel per Swipe (D-19-Konflikt),
der Swipe-Weg wird sichtbar mitgezeichnet, Erstfreigabe „sofort gut, keine halbe Sache" (§8.8).

---

## 0. Antwort auf die Vorab-Fragen (Kurzfassung)

- **Kein Kernel, keine Neuralnetze, keine großen Wort-Statistik-Tabellen.** Die übliche Berechnungsweise
  (Literaturstand seit ~2010, z. B. SHARK2; AOSP LatinIME macht es konzeptionell genauso) ist: den
  Wisch-Weg gleichmäßig abtasten, für jedes Kandidatenwort den *idealen* Weg durch die Tasten-Mittelpunkte
  berechnen, beide Formen alignieren (Dynamic Programming / vereinfachtes DTW) und die geometrische Kosten-
  mit der vorhandenen Wortfrequenz kombinieren. Pro Wort fallen nur `Länge × Tastenzentren` an —
  on the fly, nichts wird pro Wort gespeichert.
- **Die teuren Zutaten hat AdaptKey bereits**: Wörterbücher mit Frequenzen (bundled + gelernt, pro Sprache),
  Prefix-Index (`unigramsByPrefix`), Umlaut-Folding (`Umlaut`/`DataDiacriticFolding` — der Wisch über `a`
  matched `ä`), persönliche Bigramm-/Trigramm-Kontexte für das Re-Ranking (D-365/D-429-Reskalierung
  existiert), gerenderte Tasten-Geometrie (`AdaptKeyboardView`/`Key`-Bounds). Neu bauen wir nur:
  Weg-Abtastung, Geometrie-Kosten, Kandidaten-Shortlist, Zeichnung des Wegs, Gesture-Klassifikation,
  Settings-Schalter.
- **Budget: nicht 17 Wochenbudgets.** Realistisch **4–7 Sessions gesamt**; fertiges Feature (inkl.
  begrenzter Kalibrierrunde) nach **3–5**. Aufschlüsselung in §7. Die unvorhersehbare Größe ist — wie bei
  jedem Gesture-Feature dieses Projekts (D-508, D-380, D-401) — die Schwellwert-/Gefühlskalibrierung am
  Gerät, nicht der Algorithmus.

---

## 1. Motivation

Community-Meldung: Wisch-Typing (Wort in einem durchgehenden Strich über die Buchstaben ziehen, Gboard-Style)
sei ein Kern-Feature moderner Tastaturen und fehle AdaptKey. Der Nutzer selbst nutzt es nicht — Qualitäts-
und Gefühlskalibrierung läuft deshalb bewusst über Unit-Tests mit synthetischen Traces (§6) und
Geräte-/Community-Feedback-Runden, nicht über die eigenen Daumen.

## 2. Verified current state (Symbole statt Zeilennummern — vor dem Anfassen re-lokalisieren)

- **Touch-Auflösung**: `AdaptKeyboardView.onTouchEvent` (AdaptKeyboardView.kt) verzweigt zuerst auf
  `cursorControlActive` (G-08), sonst Tap-/Long-Press-/Swipe-Auflösung über `movedBeyondSlop`,
  `movedOutsideKey`, `swipeDirectionIfFarEnough`, `resolveSwipe`. T-01: `ACTION_DOWN` ist der kanonische
  Tap-Punkt; Bewegung bis `ACTION_UP` beeinflusst die Key-Auflösung nicht.
- **D-495**: `gesture/TouchSequenceTracker` — Wisch = eine durchgehende Ein-Zeiger-Bewegung; landet ein
  zweiter Finger, stirbt die Sequenz (kann weder Tap noch Swipe noch Popup werden). Gilt für Glide
  unverändert weiter.
- **Bestehende Feld-Gestures**: G-01 Sprachwechsel (nur Space-Taste der Buchstaben-Seite, D-502-Chevrons),
  D-19 horizontaler Feld-Swipe für den Seitenwechsel (Schalter `d19_surface_swipe`, default an;
  URL/Email-Space fällt durch), G-02 Wortlöschen (Backspace), G-03 Runterwischen zum Schließen
  (Schalter `g03_swipe_down_dismiss`), G-08 Cursor-Control (Space-Langdruck, `CursorControlGesture`).
  D-508: alle Feld-Swipe-Distanzen skalieren über `d508_field_swipe_scale`, Space-Swipe über
  `d508_space_swipe_scale`.
- **D-380 Smear-Schutz**: ein Langdruck, dessen Finger die Taste verlässt, wird nur abgebrochen, wenn der
  Weg bereits eine echte Swipe-Distanz clearingt; sonst bleibt es ein gehaltener Key mit Popup. Für Glide
  relevant (§4.3).
- **Dictionary/Suggestion**: `DictionaryStore.unigramsByPrefix(prefix, limit)` (bundled + gelernt,
  casing-kanonisch), `DictionarySuggestionProvider` (Such-/Ranking-Maschine, Hintergrund-Executor mit
  kooperativem `isCancelled`-Polling — genau das Muster, das Glide-Updates braucht), `Umlaut`/
  `DataDiacriticFolding` ( Folding pro aktiver Sprache), `LearnedBigramBoost`/`LearnedNgram`
  (persönlicher Kontext), `KeyboardProximity`/`KeyboardProximityRegistry` (näherungsbezogene Kosten pro
  Taste für die Tap-Korrektur — wiederverwendbar als Radius-Modell).
- **Commit-Pfade**: Suggestion-Chip-Tap (`onSuggestionClicked`, resolved casing gem. D-440) und ordinary
  typing; `CapitalisationEngine.capitalise()` leitet die Großschreibung am Satz-/Wortanfang ab; n-gram-
  Lernen hängt am ordinary commit (D-327/D-438).
- **Settings**: `SettingsStore` (Schlüsselkonvention `d<N>_<name>`, z. B. `d19_surface_swipe`,
  `g03_swipe_down_dismiss`), `SettingsMapper`, Backup-Schlüsselliste, Gesten-Kategorie (dort leben
  D-19/G-03), Strings in 32 Locales (`UiLocaleConsistencyTest` erzwingt; Übersetzungs-Skript nach
  D-515-Präzedenz in `scratchpad/`).
- **Zeichnung**: `AdaptKeyboardView` ist vollständig self-drawn — eine Ink-Schicht für den Weg ist ein
  normaler `onDraw`-Zusatz, kein neuer View nötig.
- **History**: Glide/Wisch-Typing wurde noch nie diskutiert (Suche nach glide/swipe-typ: 0 relevante
  Treffer) — echtes neues Feature, keine alte Weiche stellt sich wieder auf. Nächste freie Nummern:
  **D-516** (History), **G-09** (Spec §4 Gestures).

## 3. Benutzer- und Verhaltens-Spec (G-09)

- **Opt-in**: ein Schalter in der Gesten-Kategorie (`d516_glide_typing`, default **aus**; genauer
  Schlüsselname beim Implementieren fixieren). Aus = AdaptKey verhält sich byte-identisch zum heutigen
  Stand; die Klassifikation ist vollständig umgangen (kein Overhead, kein Risiko für Bestandstypen).
- **Wo Glide funktioniert**: nur die Buchstaben-Seite, nur mit einem Finger, nur als Strich, der auf einer
  Buchstaben-Taste **beginnt**. Symbol-/Rechner-/URL-/Email-Flächen, Emoji-Suche (D-317 hat ohnehin keine
  Gestures) und Passwort-/Credential-Felder (D-150-Kontext) führen Glide nie aus.
- **Was ein Strich wird**: Taps bleiben Taps (T-01 unverändert, schnellere/unsichere Fälle siehe §4.1).
  Ein Strich gilt als Glide, sobald er die Klassifikations-Schwellen (§4.1) clearingt — danach ist er
  Glide bis zum Release, unabhängig davon, was er unterwegs kreuzt.
- **Live-Verhalten**: während des Strichs zeigt die Vorschlagsleiste den aktuellen Top-Kandidaten
  (gepinnt, vor allen gewöhnlichen Chips — S-01/S-06-„alternate content"-Präzedenz) **und** das Feld zeigt
  den Kandidaten als Composing-Vorschau (ENTSCHEIDEN §8.2: ja), gedrosselt (~30–50 ms), über den ordinary
  Composing-Rewrite-Pfad. Der abgezogene Weg wird als verblassende Linie mitgezeichnet (Nutzer-Call:
  sichtbar; ENTSCHEIDEN §8.6: ohne eigenen Schalter) — dünne Linie, Farbe aus dem Hint/Akzent-Repertoire,
  Verblasen ~0,6–0,8 s (das übliche Verhalten: Gboard lässt den Trail ebenfalls zügig verblassen).
- **Release**: der Top-Kandidat wird committet — über den Chip-Commit-Pfad (resolved casing,
  `capitalise()` leitet Satz-/Wortanfang ab), kein Autocorrect/A-01-Eingriff (das Wort wurde explizit
  gewählt), n-gram-Lernen läuft über den ordinary commit mit (D-327/D-438). Alternativen stehen in der
  Leiste zum Nachwählen. **Unbekanntes Wort (ENTSCHEIDEN §8.3, wie andere Tastaturen/Gboard):** die vom
  Strich erkannte Buchstabenfolge wird als Wort eingesetzt — sie verhält sich wie ein evtl. falsch
  geschriebenes Wort, wird aber NICHT automatisch ins persönliche Wörterbuch aufgenommen; kein Toast,
  keine Unterdrückung. **Auto-Space (ENTSCHEIDEN §8.4, wie andere Tastaturen/Gboard, default an):**
  committet ein Glide-Wort und der nächste Glide beginnt, wird automatisch ein Leerzeichen eingefügt —
  lazy beim Start des nächsten Strichs (nicht schon beim Release des ersten, damit der Caret danach noch
  frei beweglich bleibt und der Kandidat korrigiert werden kann), nur zwischen zwei Glide-Strichen;
  kein eigener Schalter in der Erstversion (Gboard hat ebenfalls keinen separaten).
- **T-03 unberührt**: Glide-Striche sind keine Taps — sie gehen nie als `(x, y, key)`-Trainingdaten ein
  (analog T-05-Flag). Lernen läuft ausschließlich über den Commit (n-gram/Frequenz wie gehabt).
- **Mehrsprachigkeit**: funktioniert strukturell automatisch pro aktiver Sprache — Tastenzentren kommen
  aus dem gerenderten Layout (QWERTZ/QWERTY/AZERTY/Griechisch/Kyrillisch …), Kandidaten aus dem aktiven
  Wörterbuch samt Folding. Fokus der ersten Qualitätsrunden: Deutsch/Englisch; weitere Sprachen reiten
  strukturell mit, werden nicht separat getestet.

## 4. Technik

### 4.1 Gesture-Klassifikation und Konflikte (der eigentliche Fleiß-Teil)

Neu: ein kleiner, reiner Logik-Kontroller (`glide/GlideGestureController` + `GlideTrace`) — der View
füttert ihn mit Move-Punkten, er meldet Zustand (`IDLE/ACTIVE/REJECTED`) und Kandidaten; der View
behält alle UI-/Commit-Seiten. Klassifikation pro Strich:

1. **Arming**: `ACTION_DOWN` auf einer Buchstaben-Taste der Buchstaben-Seite (kein Space, kein
   Modifier, kein Symbol-Layout), Glide-Schalter an, Feld erlaubt Glide (§3).
2. **Claim**: sobald der Strich **mindestens zwei** verschiedene Buchstaben-Tasten in ausreichender
   Nähe passiert hat UND der Weg die Tastenbreite clearly übersteigt UND die Dauer über einer
   Mindestzeit liegt (Trennung vom schnellsten legitimen Tap-Sweep; Konstanten hardcoded, in R3
   am Gerät kalibriert — bewusst NICHT an `d508_field_swipe_scale` gekoppelt, das eine andere
   Frage kalibriert). Vor dem Claim ist der Strich ein normaler Tap-Kandidat — Bestandsverhalten
   für kurze Striche bleibt intakt.
3. **Nach dem Claim** (bis Release): kein Tap, kein Popup, kein Page-Swipe, kein Dismiss aus diesem
   Strich. Ein zweiter Finger killt den Strich (D-495, unverändert).

Konflikt-Matrix (alle Fälle im Unit-Test pinen):

| Konflikt | Regel |
|---|---|
| **D-19 Page-Swipe** (Nutzer-Call: „beißt sich") | Auf der Buchstaben-Seite wird der horizontale Feld-Swipe **vollständig geschluckt**, sobald Glide aktiv ist: ein Strich, der auf einer Buchstaben-Taste beginnt, kann Glide werden, nie Page-Swipe. `?123`/`ABC` bleibt der Seitenwechsel (D-19-Textfall „nur die ?123-Taste schaltet"). Symbol-Seiten/URL/Email behalten D-19 unverändert (dort gibt es kein Glide). Der D-19-Schalter bleibt wirksam und sichtbar; sein Summary bekommt einen Hinweis-String „Bei aktivem Gesteeneingabe/Glide-Typing schaltet auf der Buchstaben-Seite nur die ?123-Taste" (ENTSCHEIDEN §8.5: Summary-Hinweis, kein Ausgrauen). |
| **G-03 Dismiss** | Bleibt an, bekommt ein **Dominanz-Gate**: Dismiss nur, wenn der vertikale Anteil den horizontalen klar dominiert UND der Strich vor dem Abtauchen nie ≥2 Buchstabentasten geclaimt hat (d. h. ein echter Abwärts-Zopfer von der Tastatur-Mitte, kein Glide, der unten ausläuft). Gerätetest-Punkt in R3. |
| **G-01 Sprachwechsel** | Unberührt — beginnt per Definition auf der Space-Taste; Glide beginnt nie dort. Ein Glide, der mitten durchs Space-Queret, triggert nichts (Origin zählt, §4.1.1). |
| **G-02 Wortlöschen (Backspace)** | Unberührt — Origin Backspace. |
| **G-08 Cursor-Control** | Unberührt — Space-Langdruck-Arm; Glide-Origin ist nie Space. |
| **L-05/L-06 Long-Press-Popups** | Nach Claim unmöglich (kein Popup aus einem Glide-Strich). Vor dem Claim: der D-508-Slop-Abbruch bricht den Langdruck-Arm bei bewegtem Finger ab. **D-380-Spannung**: der Smear-Schutz (langsame Schmierung bleibt Holding mit Popup) gilt bei Glide an nur innerhalb der Taste; eine Schmierung, die die Taste verlässt, bricht ab (der Glide-User hat gegen Popup-beim-Schmieren optiert). Glide aus → D-380 exakt wie heute. (ENTSCHEIDEN §8.7: so bestätigt.) |
| **T-02/T-03 Retro-Korrektur/Lernen** | Glide-Striche erzeugen keine Tap-Punkte: kein `(x,y,key)`, kein Training, keine Raw-Coordinate-Interaktion. |
| **S-03 Stabilisierung** | Der gepinnte Glide-Kandidat steht außerhalb der ordinary Stabilisierung (gleiche Ausnahme-Familie wie S-06/S-08: display-time, maximaler Score/Pinning). |

### 4.2 Erkennungskern (kein Kernel — die Rechnung)

Reine Kotlin-Klasse(n) im neuen Paket `glide/`, keine Android-Imports (Hausstil: testbar ohne Robolectric):

1. **Resampling**: Move-Punkte zu konstantem Bogenmaß umproben (~6–10 px Abstand) → geschwindigkeits-
   invariant; zu kurze/arme Striche werden vor der Scoring gar nicht zugelassen (§4.1.2).
2. **Idealer Weg pro Kandidat**: Polyline durch die Tastenzentren der (gefalteten!) Buchstabenfolge,
   auf dasselbe Bogenmaß resampled; `ä→a` etc. über das aktive Folding (Doppelbuchstaben → doppelter
   Wegpunkt, kein Trick nötig).
3. **Kosten**: vereinfachtes DTW/Alignment zwischen beobachtetem und idealem Weg (mittlere
   Punktdistanz + geringe Strafe für Längen-Missverhältnis) − Prior aus der Wortfrequenz
   (log-skaliert, gleiche Familie wie D-411/D-365) + Bonus aus dem persönlichen Bigramm/Trigramm-
   Kontext des vorherigen Worts (existierende Reskalierung nutzen).
4. **Shortlist statt Vollscan**: pro Update (Release: ein Mal; Live: gedrosselt) werden nur Kandidaten
   bewertet, die eine billige Vorfilterung überstehen (erste+letzte Taste in Endpunktnähe, Länge ±,
   ggf. Richtungs-Histogramm-Grobvergleich). Zielgröße: wenige Hundert Alignments à ≤ ~100 Punkte —
   trivial auf moderner Hardware; Live-Updates laufen im Hintergrund-Executor mit dem bestehenden
   kooperativen `isCancelled`-Muster (supersedierte Berechnung wird weggeworfen).
5. **Folding**: Geometrie und Kandidaten rechnen auf gefalteten Formen; Anzeige/Commit in der kanonischen
   Schreibweise des Dictionary-Eintrags (`unigramsByPrefix` liefert die casing-kanonische Form bereits).
   Der „Umlauts Are Ordinary Characters"-Grundsatz bleibt gewahrt: Wisch über `s` matched `ß`/`ss`-Konvention
   über das bestehende Folding, nie als Fuzzy-Abstufung.
6. **Kein neuer Datensatz**: nichts wird gebundelt, gecacht oder pro Wort vorgerechnet; die einzige
   Neu-Berechnung pro Kandidat ist die Ideallinie (billig). Optional später (explizit NICHT Teil dieses
   Plans): das Tier-3-LLM als Re-Ranker.

### 4.3 Commit- und Preview-Integration

- Release → Chip-Commit-Pfad (`onSuggestionClicked`-Familie): resolved casing (D-440-Regeln:
  `composing.isNotEmpty()` entscheidet Re-Derivation vs. kanonische Form), `capitalise()` leitet
  Satz-/Wortanfang ab, n-gram-Kontext lernt mit. Kein Autocorrect-Pfad, kein S-06-Verbatim-Chip
  (das Wort wurde explizit gewählt), kein A-05-Split.
- Live-Preview (ENTSCHEIDEN §8.2: ja): `setComposingText` des aktuellen Top-Kandidaten, gedrosselt, über
  den ordinary Composing-Rewrite — dieselbe Mechanik wie Buchstaben-Typing; §1-Guardrails (composingAnchor,
  kein Echo-Vertrauen, Ground-Truth-Check, D-492 additiv) werden dadurch nicht berührt, weil Glide
  keinen neuen Commit-/Tear-down-Pfad einführt. Vor R1 einen Blick in die vier Invarianten-Notes; jede
  Abweichung stoppt die Umsetzung (Guardrail-Regel).
- Auto-Space (ENTSCHEIDEN §8.4: ja, Gboard-Verhalten): beim Start eines Glide-Strichs prüft der Controller,
  ob das direkt vor der Caret stehende Wort vom unmittelbar vorherigen Glide-Strich committet wurde (ein
  One-Shot-Flag am Controller, nach dem nächsten beliebigen anderen Input verworfen) — dann wird zuerst
  ein Leerzeichen committed, danach beginnt der neue Strich. Nur Glide→Glide; nach einem Glide-Wort
  getippte Buchstaben bekommen kein automatisches Leerzeichen.
- Caret mitten im Wort: Glide verhält sich wie ordinary typing an der Caret-Position (der bestehende
  Reclaim/D-62-Mechanismus behandelt den Suffix) — Gerätetest-Punkt, kein Sonderbau.

### 4.4 Weg-Zeichnung (Nutzer-Anforderung)

- `AdaptKeyboardView.onDraw`: eine `Path`/Punkteliste des aktiven Strichs + Rest-Fenster nach Release;
  Strichstärke ~2–3 dp gerundet, Farbe aus dem vorhandenen Hint/Akzent-Farbsatz (Dark-Theme erbt es),
  Alpha fällt segmentweise mit dem Alter (~0,6–0,8 s), danach Pfad weg.
- Redraw: `invalidate()` pro Move (custom View, übliche Praxis); feiner: Region-Invalidierung —
  Mikro-Optimierung, nur falls R3-Rückmeldung Ruckeln meldet.

## 5. Settings/Strings/Backup

- Ein SwitchPreference in der Gesten-Kategorie. **Benennung (ENTSCHEIDEN §8.1):** der etablierte
  Fachbegriff ist englisch **„Glide typing"** (Gboards offizieller Einstellungsname; Vorläufer Google
  Keyboard: „Gesture typing", SwiftKey: „Flow") — der deutsche Produktbegriff bei Gboard DE/AOSP ist
  „Gesteeneingabe". DE-Switch-Titel daher: **„Gesteeneingabe (Glide-Typing)"**, Feature-/Spec-/Code-Name
  durchgängig „Glide-Typing" (Schlüssel `d516_glide_typing`); „Wisch-Eingabe" wird nicht verwendet.
  Verifikationspunkt für die String-Runde: deutsche Gboard-Bezeichnung am Gerät gegenprüfen
  (Websearch stand beim Planen nicht zur Verfügung; „Gesteeneingabe" ist Wissenstand, nicht live geprüft).
  Summary beschreibt Opt-in-Charakter + D-19-Einschränkung.
- Schlüssel `d516_glide_typing` (Konvention wie `d19_surface_swipe`), in die Backup-Schlüsselliste.
- Neue Strings (Schalter + D-19-Summary-Erweiterung; kein Fehler-Toast — ENTSCHEIDEN §8.3, unbekanntes
  Wort wird einfach als Wort eingesetzt) in EN + alle 31 Locales via `scratchpad/`-Skript nach
  D-515-Präzedenz; `UiLocaleConsistencyTest` grün.
- FeatureCatalog/FeatureOverview (D-89-Nachfolger) bekommt den neuen Eintrag.

## 6. Tests

- **Erkenner (Kern-Qualität, ohne eigene Benutzung erreichbar)**: synthetische Traces — laufe den idealen
  Weg eines Worts mit kontrolliertem Rauschen/Versatz ab, Erkenner muss das Wort (bzw. seine Faltungs-
  klasse) auf Platz 1 liefern; adversariale Paare (identischer Ideallinienschnitt, z. B. Kurzwörter auf
  einer Linie, `der`/`derw`-Präfixfamilien) werden nur über Frequenz/Kontext korrekt getrennt — genau
  diese Tests fixieren das Frequenz-Gewicht. Deterministisch (seeded), kein Android.
- **Klassifikation**: Tap bleibt Tap (auch schneller Doppel-Tap, D-495-Überlappung), Page-Swipe-Fall
  bei Glide aus vs. an, Dismiss-Dominanz-Gate, Popup-Unterdrückung, zweiter Finger killt, Glide auf
  Symbol-/URL-/Passwort-Feldern nie.
- **Commit**: casing am Satz-/Wortanfang, n-gram-Lernen nach Glide-Commit, kein Autocorrect-Eingriff,
  S-03-Fremd-Pinning, unbekanntes Wort: erkannte Buchstabenfolge wird als Wort eingesetzt (§8.3),
  Auto-Space nur Glide→Glide (lazy beim nächsten Strich-Start, Flag verworfen nach fremdem Input),
  kein Auto-Space nach Tipp-Buchstaben.
- **Settings/Backup/Locales**: Schlüssel im Backup, 32 Locales konsistent, FeatureCatalog-Eintrag.
- Build/Tests per Haus-Kommando (`:app:assembleRelease :app:testDebugUnitTest`, JBR-21-JAVA_HOME).

## 7. Runden & Budget (Versionierung nach rules/version-bump.md)

Nutzer-Qualitätsbar (§8.8): „sofort gut, nicht high sophisticated, irgendwann fertig, keine halbe Sache
zu Anfang" — daraus folgt: die erste sichtbare Version ist das **vollständige** Feature in einem Wurf
(R2), und die Feedback-Runde ist begrenzt, nicht offen endend. Der Erkenner bleibt bewusst einfach-gut
(DTW + Frequenz + Kontext, hardcoded Konstanten) statt mit High-End-Optimierungen vorab überinstrumentiert.

| Runde | Inhalt | Bump |
|---|---|---|
| **R1 (D-516)** | ~~Erkennungskern `glide/` + synthetische-Trace-Tests. Kein UI-Anschluss, für den Nutzer unsichtbar.~~ **ERLEDIGT 2026-11-06 (History §540).** | keiner (unsichtbarer Zwischenschritt) |
| **R2 (D-517)** | ~~**Komplette Erstfreigabe in einem Wurf:** Schalter (32 Locales) + Klassifikation/Konflikt-Matrix + Commit-Pfad (inkl. unbekanntes Wort, Auto-Space) + Live-Composing-Preview + gepinnter Kandidat in der Leiste + Weg-Zeichnung.~~ **ERLEDIGT 2026-11-06 (§541, v1.2.106) — noch nicht geräte-bestätigt.** | bump (1.2.106/602) |
| **R3 (D-518)** | **Begrenzte** Geräte-/Community-Feedback-Runde: Schwellwerte, G-03-Dominanz-Gate, D-380-Gefühl, deutsche Bezeichnung am Gerät gegenprüfen, evtl. Region-Invalidierung. **Abschlusskriterium:** Geräteliste (§6-Klassifikations- + Commit-Fälle am Gerät) grün + Nutzer-Go → Feature fertig; weitere Wünsche laufen danach als normale Post-Release-Community-Runden, nicht als offener Plan. | bump nur falls Code-Änderungen anfallen |

**Budget-Ehrlichkeit**: R1 1–2 Sessions, R2 2–3, R3 1–2 (Kalibrierung ist die historisch nachlaufende
Größe — siehe D-508/D-380/D-401-Historie). Gesamt **4–7 Sessions**; fertiges Feature nach **3–5**.
17 Wochenbudgets sind es mit Sicherheit nicht; das Feature ist mittelgroß (Vergleichsrahmen: D-401
Cursor-Control oder eine Alt-Reform-Phase pro Runde).

## 8. Entschiedene Entscheidungen (Nutzer-Calls, 2026-11-05 — alle offen Gewesenen geschlossen)

1. **Benennung**: etablierter Fachbegriff ist englisch „Glide typing" (Gboard) → Feature heißt
   durchgängig **Glide-Typing** (Spec G-09, Code, Schlüssel `d516_glide_typing`); deutscher
   Produktbegriff (Gboard DE/AOSP): „Gesteeneingabe" → DE-Switch-Titel „Gesteeneingabe (Glide-Typing)".
   „Wisch-Eingabe" entfällt. Verifikation am Gerät in R3 (Websearch stand nicht zur Verfügung).
2. **Live-Preview im Feld**: **ja** (Composing-Preview + gepinnter Leisten-Kandidat, §4.3).
3. **Unbekanntes Wort**: **wie andere Tastaturen (Gboard)** — die erkannte Buchstabenfolge wird als Wort
   eingesetzt (verhält sich wie ein evtl. falsch geschriebenes Wort, keine Auto-Diktierbuch-Aufnahme,
   kein Toast; Alternativen in der Leiste).
4. **Auto-Space**: **wie andere Tastaturen (Gboard, default an)** — automatisch ein Leerzeichen, wenn
   nach einem Glide-Wort der nächste Glide beginnt; lazy beim Start des nächsten Strichs, nur
   Glide→Glide, kein eigener Schalter in der Erstversion (Gboard hat ebenfalls keinen).
5. **D-19-Schalter-Darstellung**: **Summary-Hinweis** (kein Ausgrauen, kein Umkopieren des Flags —
   D-301-Lektion).
6. **Trail-Stil**: **hardcoded, kein eigener Schalter** — dünne Linie, Verblasen ~0,6–0,8 s, wie andere
   Tastaturen (Gboard lässt den Trail ebenfalls zügig verblassen).
7. **D-380 bei Glide an**: **bestätigt** — Schmierschutz gilt nur innerhalb der Taste; Glide aus →
   exakt heutiges Verhalten.
8. **Qualitätsanspruch**: „sofort gut, nicht high sophisticated, irgendwann fertig, keine halbe Sache
   zu Anfang" → R2 liefert das **vollständige** Feature (Schalter + Klassifikation + Commit + Preview +
   Trail), einfach-guter Erkenner statt Überinstrumentierung, begrenzte R3 mit Abschlusskriterium (§7).

## 9. Nicht-Ziele (dieser Plan)

- Kein Tier-3-/LLM-Re-Ranking, kein Trainieren eines eigenen Modells, keine neuen gebandelten Daten.
- Kein Glide auf Symbol-/Rechner-/URL-/Email-Flächen, keine Emoji-Suche-Integration, keine Passwort-Felder.
- Keine eigenen neuen Statistik-Tabellen, kein Word-Embedding, kein Sprachmodell.
- Kein Umbau an T-01/T-02/T-03 oder den §1-Guardrails; Bestandsverhalten mit Schalter=aus bleibt
  byte-identisch.
- Breiten-Gerätetest aller 31 Sprachpacks (Deutsch/Englisch führend; Rest strukturell, nicht separat
  verifiziert).
