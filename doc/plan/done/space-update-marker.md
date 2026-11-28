# AdaptKey — Plan: Space-Update-Marker (Amber-Markierung bei Sprachpaket-Update)

**Status: CLOSED (2026-11-28, device-confirmed) — die drei Kern-Calls (Farbe = Amber-Vollfläche,
Long-Press = Override immer, Bestätigung = kurzer Grün-Blitz) sind am selben Tag umgesetzt
(v1.3.30/630, D-593..D-596) und vom Nutzer am Gerät gegen ein echtes, pending Paket-Update bestätigt
(„optimal funktioniert"): der komplette Zyklus — Amber-Marker auf der Leertaste, Long-Press mit
Highlight in die Paket-Seite, Installation, Grün-Blitz, Normalzustand, D-401 zurück.**

Origin: Nutzer-Anfrage (2026-11-28) — es hängt viel an einem aktuellen Sprachpaket (Korrektur-
qualität, Register, Gate-Fixes; D-579/D-580 sind das frische Beispiel, dass ein versäumter
Pack-Stand bis zu stillem Funktionsausfall führt). Der „Update verfügbar"-Hinweis lebt heute aber
nur in der Sprachpaket-Seite und wird von dem gesehen, wer zufällig dort vorbeischaut. Die
Leertaste soll den Zustand **am Ort des Tippens** dauerhaft sichtbar machen und den direkten
Handlungsweg anbieten — bewusst mit Druck (der Cursor-Long-Press kommt erst nach der Installation
zurück), aber ohne Alarm-Charakter.

---

## 0. Kurzfassung

- **Signal (D-593)**: das exakte `buildRow()`-Prädikat aus `LanguagePacksActivity` wird für die
  **aktive** Sprache gespiegelt: Sprache installiert (English als gebündelte Ausnahme, §5.1) +
  Catalog-Entry streng neuer als installierte Version + **nicht** durch `suppressedCatalogVersion`
  unterdrückt (D-334). Kein neues Update-Protokoll, keine Netzlogik, kein zweites Warheits-Oracle.
- **Darstellung (D-594)**: Amber-Vollfläche auf dem Space-Key **nur auf der Buchstaben-Ebene**
  (D-097-Konsistenz; auch nicht in URL-/E-Mail-Modus). Beschriftung zweizeilig: Endonym oben,
  das eigene, kurze `d594_space_update_label` („Update verfügbar", je Sprache die etablierte
  Wortwahl aus dem jeweiligen D-280-Hinweis) kleiner darunter, Block insgesamt vertikal auf der
  Taste zentriert.
- **Long-Press (D-595)**: im Update-Zustand navigiert der Space-Long-Press direkt zur
  `LanguagePacksActivity` (mit `EXTRA_HIGHLIGHT_LANGUAGE_CODE` der aktiven Sprache). Die
  Cursor-Control-Geste (D-401) pausiert bis zur Installation — **auch wenn** `cursorControlEnabled`
  an ist. Nach der Installation wirkt wieder der normale D-401-Modus.
- **Grün-Blitz (D-596)**: nach erfolgreicher Installation einmalig kurzer grüner Flächen-Blitz der
  Leertaste beim nächsten Keyboard-Auftritt. Grün bekommt so seine richtige Rolle („erledigt"),
  statt als Dauerzustand „offene Handlung" zu behaupten.

Aufwand: **realistisch 2–3 Sessions** (§6).

---

## 1. Motivation

Sprachpaket-Updates wirken still und ihr Nutzen ist nicht selbsterklärend — verpasste Stände
hinterlassen aber echte Fehlfunktionen (der D-579/D-580-Fall: Catalog 52 statt 54, „dur→dir" feuerte
erst nach Pack-Import). Die Paket-Seite zeigt den Zustand korrekt, aber nur für den, der sie öffnet.
Ein dauerhafter, nicht-blockierender Hinweis genau dort, wo ohnehin getippt wird, schließt diese
Lücke; die gestohlene Cursor-Geste ist der gewollte Hebel, der „später erledigen" unattraktiv macht.

Farb-Diskussion (abgeschlossen): Grün liest sich als „fertig/ok" — die falsche Botschaft für eine
offene Handlung; Rot liest sich als Fehler und verunsichert; Blau ist belegt (App-Akzent und
`language_pack_suggested_background` für „Sprachpaket vorgeschlagen"). Amber/Orange besetzt exakt
die Lücke „da ist etwas für dich zu tun, aber nichts ist kaputt" und ist die etablierte Badge-Sprache
für Updates.

## 2. Verified current state (Symbole statt Zeilennummern — vor dem Anfassen re-lokalisieren)

- **Space-Label**: `AdaptKeyboardView.spaceLabel` (D-003), gesetzt vom Service in
  `updateSpaceLabel()` — genau dort wird auch `languageSwipeAvailable` gepusht; natürlicher Hook
  für das neue Signal, weil alle bestehenden Refresh-Punkte (Sprachwechsel, erste Frame,
  `onStartInput`-Nahe) die Methode bereits rufen.
- **Label-Auflösung**: `labelFor(key)` zeigt das Endonym nur auf `InputSurface.LETTERS` ohne
  `urlMode`/`emailMode`, sonst `SPACE_GLYPH` („␣").
- **Rendering**: `drawKeys()` zeichnet jeden Label/Hint **einzeilig** via `drawText`; Sprachwechsel
  cross-faded über `beginLanguageChangeFade()`/`drawFadingSpaceLabel()` (D-130). Zwei weitere
  Space-Overlays existieren: der `pendingSpaceIndicator`-Punkt (oben Mitte) und die
  `drawSpaceSwipeHints()`-Chevrons.
- **Long-Press**: `scheduleLongPress()` armiert Space nur als
  `isCursorControlCandidate = key.code == SPACE && cursorControlEnabled` (`cursorControlEnabled`
  Default **aus**, gepusht via `applySettings()`); bei Auslösung Haptic + `armCursorControl()`
  (D-401, `CursorControlGesture.kt`); Bewegung jenseits des Slop bricht die Armierung ab. Der
  generische `OnLongPressListener` behandelt Space **nie**.
- **Signal-Präzedenz**: `LanguagePacksActivity.buildRow()` — `installed &&
  InstalledLanguagesStore.installedVersion(...) < entry.version &&
  InstalledLanguagesStore.suppressedCatalogVersion(...) < entry.version`; der Hinweis-String der
  Paket-Seite ist `d280_status_update_available` (32 Sprachen). Der Marker nutzt sein eigenes, kurzes
  `d594_space_update_label` (Umfahrungs-Entscheidung der Umsetzung, 2026-11-28: der Paket-Seiten-String
  trägt den „Installiert - "-Vorspann und einen Punkt mit - genau beides soll auf der Taste nicht
  stehen; je Sprache wird die dort bereits etablierte Wortwahl übernommen, keine neuen Freiform-
  Übersetzungen). D-334 schreibt die
  Unterdrückung genau dann, wenn ein Import-Versuch das gehostete Archiv als stale/älter entlarvt
  hat.
- **Navigation**: `AdaptKeyService.launchFromKeyboard(activity) { extras }` (expliziter Intent +
  `FLAG_ACTIVITY_NEW_TASK`); Onboarding-Nachfolge nutzt sie bereits mit
  `EXTRA_HIGHLIGHT_LANGUAGE_CODE` → Highlight + Scroll-To in der Paket-Seite.
- **Farben**: plain Resources mit Tag/Nacht-Qualifier (`res/values/colors.xml`,
  `res/values-night/colors.xml`), Keyboard-Paints via `ContextCompat.getColor`; Press-Flash
  existiert (`flash()`, 16 ms) — der Grün-Blitz braucht einen eigenen, längeren Pfad nach dem
  gleichen Muster (Runnable + Paint + Invalidate).

## 3. Die Entscheidungen

### 3.1 Farbe: Amber-Vollfläche (D-594)

- Neue Tokens in beiden Paletten: `space_update_background` (Amber, Licht ~`#FFB300`; Nacht
  gleiche Familie, ggf. eine Stufe tiefer gegen Glare — finale Werte am Gerät gegen `#FFFFFF`-
  bzw. `#3C4043`-Umgebung prüfen) und `space_update_text` (dunkel, Amber-tragbar — **weiß auf
  Amber hat schlechten Kontrast**, der Label-Text also nicht von `key_text` erben).
- Vollfläche statt Akzent, weil der Zustand selten ist und selten sein soll; ein dezenter Rand
  wäre am Tag wie in der Nacht zu leicht zu übersehen.

### 3.2 Signal: exaktes D-334-Prädikat, nicht „größer" (D-593)

- Der Marker benutzt dieselbe Wahrheit wie die Paket-Seite — inklusive Unterdrückung. Sonst würde
  die Taste nach einem Stale-Archiv-Versuch dauerhaft etwas behaupten, das es nicht zu installieren
  gibt (genau der Fall, für den D-334 gebaut wurde).
- Als kleine **pure, JVM-testbare** Funktion auslagen (Entry-Suche, installed-Check, Versions- und
  Suppressions-Vergleich), damit die Tests §5 nicht durch Android-Context behindert werden.

### 3.3 Long-Press-Override immer (D-595)

- Nutzer-Call: der Override gilt **immer** im Update-Zustand, nicht nur bei ausgeschaltetem
  Cursor-Modus. Akzeptierter Trade-off: wer D-401 nutzt und das Update aufschiebt, verliert die
  Geste für die Verzögerungszeit — das ist der gewollte Druck.
- Bewegungs-Abbruchlogik bleibt unverändert (bewusstes Halten ist weiterhin nötig; kein
  versehentliches Navigieren beim normalen Tippen).
- Haptic vor der Navigation wie beim Cursor-Arm (Taktgefühl: die Auslösung soll sich wie eine
  bewusste Aktion anfühlen).

### 3.4 Zweizeilig, zentriert, nur Letters-Surface (D-594)

- Endonym-Baseline anheben, das kurze `d594_space_update_label` in kleinerer Schrift darunter, Block
  um die Key-Mitte zentriert; Maße als Bruchteile der Key-Höhe, nicht absolute dp. (Umsetzungs-Note:
  statt `d280_status_update_available` ein eigener String — der Paket-Seiten-Hinweis trägt den
  „Installiert - "-Vorspann und einen Punkt mit, beides gehört nicht auf die Taste; die Wortwahl je
  Sprache wird aus dem eigenen D-280-Hinweis übernommen.)
- Die zweizeilige Block-Mathe als pure Funktion auslagen (Key-Höhe → Baselines/Sizes), testbar
  ohne Canvas.
- In URL-/E-Mail-Modus und auf Symbol-Seiten kein Marker (dort zeigt die Taste auch kein
  Sprachlabel — D-097).
- D-130-Fade: der Marker bleibt statisch stehen, der Fade betrifft nur den Endonym-Swap.

### 3.5 Grün-Blitz nach Installation (D-596)

- `LanguagePacksActivity` setzt im `INSTALL`-Zweig ein One-Shot-Flag (Prefs); der Service konsumiert
  es beim nächsten `onWindowShown` **nur**, wenn die Buchstaben-Ebene mit sichtbarem Space angezeigt
  wird, flasht die Taste grün (~600 ms, eigener Runnable nach dem `flash()`-Muster) und löscht das
  Flag. Überlebt Prozess-Tod, feuert aber genau einmal.
- Danach ist der Marker weg — das Prädikat wird ohnehin frisch gerechnet.

## 4. Randfälle

- Aktive Sprache ohne Catalog-Entry (Community-Import, nicht gehostet) → kein Marker; das ist
  korrekt, es gibt nichts Erkennbares.
- Aktive Sprache **English**: gebündelt, taucht in `InstalledLanguagesStore.load()` nie auf.
  Verhalten exakt wie `buildRow()` spiegeln (dort nachlesen, wie der installed-Check English
  behandelt) — nicht eigenmächtig anders entscheiden.
- Marker gilt nur für die **aktive** Sprache; Updates anderer installierter Sprachen bleiben
  allein auf der Paket-Seite.
- `cursorControlEnabled` aus: der Space-Long-Press tat bisher nichts — der Override ist dann eine
  reine Neuheit, kein Verlust.
- Punkt (`pendingSpaceIndicator`) und Chevrons auf Amber-Fläche: Kontrast am Gerät prüfen, ggf.
  Ausweichfarbe nur im Marker-Zustand.
- Schmale Key-Höhe (Landscape): Sekundärzeile nur zeichnen, wenn der Bruchteil-Platz reicht — sonst
  nur Fläche + Endonym (nie abgeschnittene Zeile).
- Flash-Flag: kosmetisch, One-Shot, wird sofort konsumiert; bewusst **nicht** in den Backup-Export
  (Präzedenz: D-304/D-389 — ein neues Gerät soll keine toten Zustände nachfeuern).

## 5. Umsetzungsskizze

1. **Farben**: `space_update_background`, `space_update_text`, `space_installed_flash` in
   `res/values/colors.xml` + `res/values-night/colors.xml`; Paints in `AdaptKeyboardView` nach dem
   vorhandenen Auflösungsblock.
2. **View-Zeichnung**: Property `spaceUpdateAvailable: Boolean`; Space-Zweig in `drawKeys()`:
   bei Buchstaben-Ebene && Property → Amber-Fläche + zweizeiliger Block (pure Layout-Funktion);
   Fade-Pfad (D-130) behandeln; Punkt/Chevrons-Kontrast prüfen.
3. **View-Long-Press**: eigener Candidate-Zweig `isUpdateNavigateCandidate` statt
   Cursor-Control-Kandidatur, wenn Property aktiv; bei Auslösung Haptic +
   `onCursorControlListener`-artiger neuer Callback `onSpaceUpdateNavigate()`.
4. **Service**: pure Prädikat-Funktion (§3.2) in `updateSpaceLabel()` aufrufen und als Property
   pushen (gleicher Kanal wie `languageSwipeAvailable`); zusätzlich sicherstellen, dass sie nach
   `onWindowShown` frisch gerechnet wird. Navigation:
   `launchFromKeyboard(LanguagePacksActivity::class.java) { putExtra(EXTRA_HIGHLIGHT_LANGUAGE_CODE, activeLanguage.code) }`.
5. **Grün-Blitz**: Flag-Set im `INSTALL`-Zweig der Paket-Seite, Konsum + Flash + Flag-Reset im
   Service bei `onWindowShown`; danach verschwindet der Marker über das ohnehin frisch gerechnete
   Prädikat von selbst.

## 6. Tests

- **JVM**: Prädikat-Funktion (Entry fehlt / gleich / neuer / älter / suppressed / English-Sonderfall
  gemäß buildRow-Verhalten); Layout-Funktion (Baselines/Sizes aus Key-Höhe, genügend-Platz-Fall).
- **Am Gerät**: Tag/Nacht-Kontrast der Amber-Fläche und des dunklen Labels; D-130-Fade mit
  sichtbarem Marker; Punkt+Chevrons auf Amber; D-401 pausiert und kehrt nach Installation zurück
  (Setting an **und** aus); Navigation landet mit Highlight auf der richtigen Zeile; Grün-Blitz
  genau einmal, auch nach Prozess-Tod zwischen Install und nächstem Keyboard-Auftritt; Marker
  verschwindet nach der Installation; D-334-Unterdrückung blendet den Marker aus.

## 7. Aufwand

**Realistisch 2–3 Sessions**: (1) Farben + View-Zeichnung inkl. Layout-Funktion, (2) Service-
Verkabelung + Long-Press-Override + Navigation, (3) Grün-Blitz + Randfälle + Tests.
