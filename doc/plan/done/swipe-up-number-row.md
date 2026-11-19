# AdaptKey — Plan: Zahlenreihe per Swipe-Up gemeinsam mit der Extra-Row einblenden

**Status: CLOSED (2026-11-19).** Implemented in §562 (v1.3.24/versionCode 624, 2026-11-19) - code,
Robolectric tests, spec addenda (§14/G-03/L-06/C-09-Tabelle/§43) and the Progress-Eintrag D-554 done,
ein Version-Bump für die ganze Runde. Build + alle Unit-Tests grün (1.970, 0 Failures, inkl. der neuen
`NumberRowRevealRoboTest`). Die vier Kern-Fragen waren am selben Tag mit den Empfehlungen entschieden
(§7.1–§7.4); die Nebenpunkte ebenso (§7.5 v1 ohne Animation, §7.6 `c09_summary` in values/ + values-de).
Noch nicht gerätebestätigt - die Checkliste steht in §562.

Origin: Nutzer-Anfrage (2026-11-19) — wer die Zahlenreihe ausblendet (C-09), hat auf der
Buchstabenseite gar keine Ziffern mehr. Ziffern als Alt-Belegung auf der obersten Buchstabenreihe
(wie andere Tastaturen es machen) kamen als Alternative nicht in Frage: sie würde diese Reihe
überladen (dazu mehr in §2). Die Idee stattdessen: wenn (und nur wenn) C-09 aus ist, blendet der
bekannte §48-Swipe-Up nicht nur die Extra-Row, sondern **zugleich die Zahlenreihe** ein; erst der
Swipe-Down räumt beides wieder weg.

---

## 0. Kurzfassung des Vorschlags

- **Kein Settings-Flip, sondern ein transitorischer Overlay-Zustand**: die swipe-geöffnete
  Zahlenreihe ist keine geänderte C-09-Einstellung, sondern ein flüchtiger Zusatz-Flag im
  Tastatur-View („revealed“). Effektiv gilt auf der Buchstabenseite:
  `Ziffernreihe sichtbar = C-09-Einstellung || revealed`. Nicht persistiert, nicht im Backup.
- **Öffnen nur durch den §48-Gesture-Swipe** (`OPEN_SETTINGS_ROW` aus `KeyGesture.resolve()`):
  der Swipe-Up macht exakt was er heute tut (Extra-Row öffnen) und setzt zusätzlich den
  Reveal-Flag — wenn C-09 aus ist und der Swipe von der Buchstabenseite startet
  (Entscheidung §7.2). Die *programmatischen* Row-Öffnungen (Credential-Feld D-142,
  URL-Feld D-185, Emoji-Panel D-507) öffnen **nur** die Extra-Row, nie die Zahlenreihe.
- **Schließen immer gemeinsam**: jeder bestehende Close-Pfad der Extra-Row (Swipe-Down von
  Tastenfeld/Suggestion-Bar/Row selbst, Frischfeld-Reset `closeImmediately()` in
  `onStartInputView`) räumt auch den Reveal weg. Invariante: **revealed ⇒ Extra-Row offen** —
  sonst droht der in §4.3 beschriebene De-Sync, bei dem ein Swipe-Down die Tastatur wegräumt,
  während die Zahlenreihe noch steht.
- **Keine Änderung an** `KeyGesture`, `PanelNavigation`, Settings-Schema oder Backup-Codec.
  Der kombinierte Key (Swipe-Up → Zahlenebene, L-03/D-531) bleibt unberührt.

## 1. Ausgangslage (Ist-Stand im Code)

- **C-09** (`SettingsStore.KEY_NUMBER_ROW` = `c09_number_row`, Default an, `AdaptSettings.showNumberRow`)
  steuert `AdaptKeyboardView.showNumberRow` (`AdaptKeyService.applySettings()`); der Setter ruft
  `rebuildRows()`, und `rowsFor()` prepends die Ziffernreihe nur auf `InputSurface.LETTERS`
  (alle sechs Layouts teilen den Builder, L-06). Die Symbolseiten (SymbolLayout) haben ihre
  Ziffernreihe **immer**, unabhängig von C-09.
- **§48 Extra-Row** (`ExtraRowView`): Swipe-Up irgendwo auf dem Tastenfeld öffnet (außer vom
  kombinierten Key, der seinen eigenen L-03-Swipe behält); Swipe-Down schließt — von drei Quellen
  (Tastenfeld, Suggestion-Bar, Row selbst), alles läuft in `AdaptKeyService.dismissKeyboardOrCloseExtraRow()`:
  Row zuerst, erst wenn sie zu ist (und `g03_swipe_down_dismiss` an) die Tastatur. Beim Feldwechsel
  räumt `onStartInputView` die Row per `closeImmediately()` weg — **das ist der einzige
  „Auto-Hide“-Mechanismus**, ein Timer-basiertes Auto-Hide existiert nicht (D-187 hat das
  Schließen bei Button-Taps bewusst wieder abgeschafft).
- **Programmatische Row-Öffnungen** neben dem Gesture-Swipe: Credential-Felder (D-142, inkl.
  Button-Flash), URL-Felder (D-185), und `setSurface(EMOJI)` (D-507 — die Row muss offen bleiben,
  damit D-472s „Zurück zur Tastatur“-Button erreichbar bleibt).
- **Ziffern-Einstieg heute bei C-09 aus**: kombiniertes `!?#` → Seite 2 → `?123` → Zahlenebene
  (D-531), oder Swipe-Up am kombinierten Key direkt auf die Zahlenebene (L-03), oder Seite 2s
  garantierte Ziffernreihe (§15). Alles Mehr-Schritt-Wege; für einzelne Ziffern im Fließtext teuer.

## 2. Bewertung der Idee

**Die Idee trägt.** Gründe:

1. **Sie löst das reale Problem ohne neue Fläche**: C-09-aus-Nutzer bekommen Ziffern mit **einem
   Tap** zurück (Reihe steht beim Swipe-Up schon da), statt Überwege über `?123`-Umwege — und
   zwar genau dann, wenn sie sie explizit anfordern (aktiver Swipe), nie unaufgefordert.
2. **Sie überlädt nichts, was schon belegt ist**: Anders als die Alt-Belegung auf der obersten
   Buchstabenreihe (Alternativ-Entwurf, bewusst verworfen) verschiebt sie nichts an der
   Reform-Anatomie: Die D-511-Dual-Corner-Belegung der Buchstaben, die L-05-Hints und die
   reformierten Popup-Reihenfolgen bleiben unangetastet. Alt-Ziffern wären außerdem zwei
   Interaktionen (Long-Press + Slide) statt einem Tap und müssten sich mit den bestehenden
   Corner-Belegungen der obersten Reihe eine Fläche teilen — genau das „Überladen“.
3. **Der Swipe-Up hat noch Kapazität**: Er ist heute exakt eine Funktion (Extra-Row). Ihn zur
   Paar-Aktion zu erweitern, ändert seine Hauptbedeutung nicht — die Extra-Row kommt ja weiterhin.
4. **Gute Nachbarschaft zum L-03-Swipe**: Der kombinierte Key bleibt der Weg in die volle
   Zahlenebene (D-531 für echten Zahl-Input); der neue Reveal ist der Weg für *einzelne Ziffern
   im Buchstaben-Kontext*. Zwei klar getrennte Affordanzen, kein Konflikt.

**Nicht als Alternative gewählt** (bewertet, verworfen):

- *C-09 temporär umschalten und beim Swipe-Down zurückschalten*: persistenter State als
  Seiteneffekt eines Gestures — der nächste Feldstart würde die (zu-)geschaltete Einstellung
  tragen, Settings-Screen und tatsächlicher Zustand können auseinanderlaufen. Schlechter Vertrag.
- *Swipe-Up öffnet bei C-09 aus NUR die Zahlenreihe (statt der Extra-Row)*: klaut dem Gesture
  seine Hauptfunktion; die Extra-Row müsste einen zweiten Öffnungsweg bekommen. Nein.
- *Zahlenreihe automatisch einblenden, wenn ein Feld ziffernlastig aussieht*: Das gibt es schon
  in seiner richtigen Form — Zahlen-/Telefon-/Datums-Felder öffnen die Zahlenebene von selbst
  (D-531). Ein weicherer Auto-Reveal auf der Buchstabenseite wäre unbeabsichtigtes Umkippen der
  Layout-Geometrie beim Tippen; nicht gewollt.

## 3. Entwurf

### 3.1 State

- Neu in `AdaptKeyboardView`: `var numberRowRevealed: Boolean = false` (transitorisch, Default aus,
  Setter ruft `rebuildRows()` wie sein Geschwister `showNumberRow`).
- Intern wird überall dort, wo heute `showNumberRow` fließt (LETTERS-Zweig von `rowsFor()` und
  die beiden `RowGeometry.hasRowAbove/Below`-Aufrufe des D-397-Kaps), mit dem **effektiven Flag**
  `showNumberRow || numberRowRevealed` gearbeitet. Folge: Der Reveal verhält sich in jeder
  Hinsicht exakt wie „C-09 an“ — Zeilengeometrie, D-55-Abstände, Touch-Zonen-Kaps, T-03-Training
  auf Ziffern-Taps (die Key-Geometrie ist identisch zu der, die C-09-an-Nutzer ohnehin haben;
  es entsteht **keine neue Geometrie-Klasse**).
- Auf `InputSurface.SYMBOLS` ist der Flag ein No-op (SymbolLayout ignoriert C-09 sowieso).

### 3.2 Öffnen (nur der Gesture-Swipe)

In `AdaptKeyService.handleSwipe()`, Zweig `GestureAction.OPEN_SETTINGS_ROW`:

```
openExtraRow()
if (!settings.showNumberRow && surface == InputSurface.LETTERS) {
    keyboardView?.numberRowRevealed = true
}
```

(`openExtraRow()` selbst bleibt unangetastet — gerade weil D-507s Emoji-Pfad und die
Feld-Auto-Öffnungen dieselbe Funktion nutzen und **keinen** Reveal auslösen dürfen.)

### 3.3 Schließen (alle Pfade gemeinsam)

- `dismissKeyboardOrCloseExtraRow()` → `closeExtraRow()`: vor/nach dem Row-Close auch
  `keyboardView?.numberRowRevealed = false`. (Ein Swipe-Down räumt also **beides in einem
  Zug** ab; der zweite Swipe dismissiert die Tastatur wie gehabt — G-03-Kette unverändert.
  Mit `g03_swipe_down_dismiss` aus schließt der Swipe-Down weiterhin nur die Paar-Öffnung,
  nie die Tastatur.)
- `onStartInputView()` → `extraRow?.closeImmediately()`: direkt daneben auch den Reveal
  zurücksetzen (Frischfeld-Reset, gleiches Verhalten wie die Extra-Row heute).
- Damit gilt die Invariante **revealed ⇒ Extra-Row offen** an jedem Punkt der Lifecycle.

### 3.4 Verhaltens-Matrix (C-09 aus vorausgesetzt)

| Situation | Extra-Row | Zahlenreihe |
|---|---|---|
| Swipe-Up auf Buchstabenseite | öffnet | öffnet |
| Swipe-Down (1. Swipe) | schließt | schließt |
| Swipe-Down (2. Swipe, `g03` an) | — | — (Tastatur weg) |
| Feldwechsel / Frischfeld | schließt (heute) | schließt (§7.1) |
| Credential-/URL-Feld-Fokus (Auto-Open) | öffnet | **bleibt zu** |
| Emoji-Panel (D-507 Auto-Open) | öffnet/bleibt | §7.4 |
| Button-Taps in der Row (D-187) | bleibt | bleibt |
| Swipe-Up am kombinierten Key (L-03) | bleibt zu | Zahlenebene statt Reveal (heute) |
| Horizontale D-19-Seiten-Wechsel | bleibt, wie heute | auf Symbolseiten egal; Rückweg auf LETTERS zeigt Reihe wieder, solange nichts schloss |

## 4. Konflikt-/Interaktions-Analyse (inkl. der gestellten Auto-Hide-Frage)

### 4.1 Auto-Hide bei Feldwechsel — ja, es gibt eine Interaktion, sie ist behandelbar

Der einzige echte Auto-Hide-Mechanismus der Extra-Row ist der **Frischfeld-Reset**
(`closeImmediately()` in `onStartInputView`) — kein Timer. Weil der Reveal die Row-Lifecycle
teilt, erbt er dieses Verhalten: nach einem Feldwechsel ist beides zu, der Nutzer swiped erneut.
Das ist der Konsistenz-Preis der strengen Kopplung; die Alternative (Reveal überlebt Feldwechsel)
bricht die Invariante und erzeugt den De-Sync aus §4.3 — ich empfehle die strenge Kopplung
(§7.1 zur Entscheidung).

### 4.2 Programmatische Auto-Opens

Credential (D-142), URL (D-185) und Emoji (D-507) öffnen die Row ohne Nutzer-Gesture. Diese
Öffnungen lösen **keinen** Reveal aus (der Reveal liegt im Gesture-Zweig von `handleSwipe`,
nicht in `openExtraRow()`). Ein Passwort-Feld bekommt also die Row mit 🔑-Flash, aber die
Tastaturhöhe springt nicht zusätzlich um eine Zeile. Asymmetrisch (öffnen: nur der Swipe;
schließen: alle Pfade), aber genau das ist die gewollte Semantik: was der Nutzer aktiv
aufgeschlagen hat, wird auch wieder aktiv abgeräumt — was die App automatisch aufschlägt,
räumt sie automatisch wieder ab.

### 4.3 Warum die Invariante „revealed ⇒ Row offen“ zwingend ist

Ohne sie wäre „Row zu + Reveal an“ denkbar (z. B. wenn künftige Pfade die Row schließen, aber
den Reveal vergessen). Dann greift bei `dismissKeyboardOrCloseExtraRow()` der
`extraRow?.isOpen != true`-Zweig: ein Swipe-Down dismissiert **die Tastatur**, während die
Zahlenreihe noch steht — ein Zustand, den kein Nutzer-Modell erklärt. Die Implementierung
setzt den Reveal-Reset deshalb an denselben Stellen wie den Row-Close, sodass die Invariante
strukturiert (nicht durch Disziplin) hält.

### 4.4 Weitere geprüfte Punkte — jeweils ohne Handlungsbedarf

- **G-02/G-01/G-03-Swipes von Ziffern-Tasten**: Ziffern-Keys sind von G-01 (Space) und G-02
  (Backspace) ohnehin nicht betroffen; vertikale Gestures von einer Ziffern-Taste funktionieren
  mit Reveal genauso wie von jeder anderen Taste (DOWN → Paar-Close bzw. Dismiss, UP → öffnet
  nach, falls nicht schon offen).
- **Glide (D-516/G-09)**: Glide-Striche starten und werten nur Buchstaben-Tasten
  (`isLetterStroke`), Ziffern sind explizit ausgeschlossen; ein Strich, der nach oben in den
  Reveal-Bereich läuft, verhält sich wie ein Strich über das heutige Reihen-Ende hinaus
  (C-09-an-Nutzer haben exakt diese Geometrie heute schon). Kein neues Konfliktfeld.
- **D-86/D-58-Slide**: Der Reveal ist kein Seitenwechsel und bekommt keinen Slide; die
  Höhenänderung (eine Zeile, Fenster wächst nach oben) ist dieselbe Transitionsklasse, die
  Nutzer heute beim C-09-Umschalten und beim LETTERS↔SYMBOLS-Wechsel (unterschiedliche
  Zeilenzahlen) ohnehin sehen. Optional-Polish: Einblende-Animation (§7.5).
- **D-397/T-03-Kaps und RowGeometry**: fließen über den effektiven Flag mit (§3.1) — genau die
  Behandlung, die der Spec (AdaptKey-Spec.md, §43-Abschnitt zur persistenten Zahlenreihe)
  für C-09-an festlegt; nichts Neues zu tun, solange alle Call-Sites den effektiven Flag
  übernehmen.
- **Kalibrierungs-Screen**: eigener View-Overlay-Instanz mit erzwungenem
  `showNumberRow = true` (CalibrationActivity), keine Row, kein Reveal — unberührt.
- **Sprachwechsel (G-01) bei offenem Reveal**: `rebuildRows()` über Layoutwechsel hinweg,
  Reveal bleibt — die neue Sprache zeigt ihre Reihe ebenfalls, bis abgeräumt wird.
  Konsistent mit dem D-187-Grundsatz (nichts schließt von allein außer den definierten Pfaden).
- **Backup/Restore**: nichts zu tun — der Reveal ist transitorischer Zustand und taucht weder im
  `BackupJsonCodec` noch in `AdaptSettings`/`SettingsStore` auf.
- **KeyGesture/PanelNavigation**: bleiben unverändert; die Kopplung lebt komplett im
  Service-Zweig `OPEN_SETTINGS_ROW` + Close-Pfaden. Die pure Funktion behält ihren
  state-freien Vertrag (dort wird bewusst keine Row-/Reveal-Logik aufgenommen).

## 5. Umsetzungsskizze

1. `AdaptKeyboardView`: `numberRowRevealed`-Property (+ KDoc nach Hausstil: Herkunft §48-Swipe,
   Invariante revealed ⇒ Row offen, Transitorik), effektiver Flag in `rowsFor()`-LETTERS-Zweig
   und in den beiden `RowGeometry`-Call-Sites (`AdaptKeyboardView.kt:3153`, `:3172`).
2. `AdaptKeyService`:
   - `handleSwipe()`-Zweig `OPEN_SETTINGS_ROW`: Reveal setzen (Bedingung §3.2).
   - `closeExtraRow()` und der `closeImmediately()`-Pfad in `onStartInputView()`: Reveal
     zurücksetzen.
3. Keine Änderungen: `KeyGesture`, `PanelNavigation`, `ExtraRowView`, Settings-UI-Schema,
   Backup-Codec, Layouts (alle sechs profitieren unverändert über den gemeinsamen Builder).
4. Strings (optional, siehe §7.6): `c09_summary` um den Hinweis erweitern („bei ausgeschalteter
   Reihe holt ein Swipe-Up die Ziffern vorübergehend zurück“); Übersetzungs-Fußabdruck beachten
   (c09_summary existiert in allen Sprachdateien).
5. Tests:
   - **Robolectric** (`AdaptKeyboardView`-Instanz): Reveal an → LETTERS-Zeilen enthalten die
     Ziffernreihe und `measuredHeight` wächst um eine Zeile; Reveal aus → Zustand wie
     C-09-aus; SYMBOLS unverändert; Zusammenspiel mit `showNumberRow=true` (Reveal redundant).
   - Wenn die Kopplungslogik (Wann-Reveal/Wann-Close) mehr als triviale Verzweigung bekommt:
     in ein kleines pure-Objekt ziehen (Hausmuster `PanelNavigation`/`KeyGesture`) und JVM-testen.
     Aktuell ist sie schlank genug, um im Service zu bleiben; kein Test-Only-Zerlegen um jeden Preis.
   - KeyGestureTest bleibt unverändert grün (keine Contract-Änderung).

## 6. Doku-Folgen

- `doc/books/AdaptKey-Spec.md`: §14 (Extra Row) um die Paar-Aktion ergänzen; L-06/C-09-Absatz
  („C-09 governs only the main letters page …“) um den Reveal als zweite, transitorische Quelle
  der Reihe erweitern; G-03 (der Row-first-Swipe-Down) und die C-09-Zeile der Settings-Tabelle;
  §43 (D-397-Kaps): der Reveal zählt als „shown“. (Die §-Nummern im Code — „§48“ für die
  Extra-Row — sind History-Einträge, nicht Spec-Sektionen; die Spec-Sektion ist §14.)
- `doc/books/AdaptKey-History.md`: Runden-Eintrag §562 (nächste freie §-Nummer) mit Version-Bump
  v1.3.23 → v1.3.24 / versionCode 623 → 624 (ein Bump pro Runde, Hauskonvention).
- `doc/books/AdaptKey-Progress.md`: Entscheid-Eintrag D-554 (nächste freie Nummer, vor Umsetzung
  geprüft) mit Origin und den §7-Entscheidungen.
- Verweise auf diesen Plan nur über den Dateinamen, nie über den Ordner (Konvention
  `doc/plan/README.md`).

## 7. Entscheidungen und offene Nebenpunkte

Entschieden am 2026-11-19 (Nutzer-Entscheidung, je die Empfehlung):

1. **Lebensdauer über Feldwechsel: streng gekoppelt.** Der Frischfeld-Reset schließt Row und
   Reveal gemeinsam, wie er die Extra-Row heute schon schließt. Die Invariante
   „revealed ⇒ Row offen“ hält strukturell (§4.3); der wörtliche „erst bei Swipe Down“ gilt
   innerhalb eines Felds, nicht über Feldwechsel hinweg. Die dreistufige Swipe-Down-Kette
   (Reveal → Row → Dismiss) wird bewusst **nicht** gebaut.
2. **Reveal nur vom Haupt-Surface.** Der Reveal greift nur, wenn der Swipe-Up auf
   `InputSurface.LETTERS` startet (§3.2). Von einer Symbolseite öffnet der Swipe-Up nur die
   Extra-Row; der Rückweg auf LETTERS zeigt die Reihe erst beim nächsten Swipe-Up.
3. **Swipe-Up bei schon offener Row: Reveal trotzdem.** „Swipe-Up = beides an“ bleibt die eine
   Regel, auch wenn die Row programmatisch geöffnet war (z. B. URL-Feld-Auto-Open) — der Swipe
   selbst ist der öffnende Akt.
4. **Emoji-Rundtrip: Reveal bleibt bestehen.** Nach Emoji-Panel und Rückkehr zur LETTERS-Seite
   steht die Reihe wieder da (konsequent D-187: nichts schließt von allein außer den
   definierten Pfaden). Kein Miträumen beim Emoji-Eintritt.

Offen (Nebenpunkte, nicht umsetzungsblockierend):

5. **Animation**: Beim Umsetzen auf der Empfehlung geblieben — v1 ohne eigene Animation (instant
   rebuild, Transitionsklasse wie das heutige C-09-Umschalten); Einblende-Polish (Reihenhöhe
   animieren nach D-132-Muster) bleibt möglicher Nachtrag.
6. **Strings**: Beim Umsetzen so entschieden wie empfohlen — `c09_summary` erweitert, aber nur in
   `values/` + `values-de/`; die anderen Locales fahren mit der nächsten Übersetzungsrunde mit.
