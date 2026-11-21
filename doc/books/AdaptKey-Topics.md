# AdaptKey — Themenliste: D-Nummern und Kurzbezeichner

Decodertabelle für die Themen-Nummern, die History, Progress und Spec verwenden. Jedes Thema
trägt hier einen kurzen deutschen **Kurzbezeichner**; in Chat und Dokumenten wird ein Thema deshalb
immer als `D-xxx (Kurzbezeichner)` genannt, nie als bloße Nummer. Die dritte Spalte zitiert den
Originaltitel aus den Büchern (gekürzt) — sie führt per Strg+F zur vollständigen Stelle.

Konventionen:

- **Fehlentscheidung 2026-11-21, bewusst so belassen:** Die Erstbelegung der Kurzbezeichner geschah auf
  Deutsch, obwohl die Bücher durchgängig Englisch sind — das war falsch. Aus Token-Budget-Gründen bleiben
  die bestehenden deutschen Labels zunächst stehen; **neue Kurzbezeichner kommen ab sofort auf Englisch
  hinzu** (wie die übrigen Bücher), und in Chat/Zitaten kann die deutsche Übersetzung mitgeliefert werden.
  Eine spätere Durchziehung aller Labels auf Englisch ist dem Nutzer bekannt und geplant, wenn wieder
  Token-Reserve da ist. Diese Zeile ist der Verweis für diese Fehlentscheidung.
- Jede **neue** D-Nummer bekommt sofort hier eine Zeile mit Kurzbezeichner (gleicher Commit wie die
  Docs-Runde). Die nächstfreie Nummer steht am Dateiende.
- Zeilen mit ↳ sind benannte Nachfolger-Themen (`D-xxx-followup` und Freunde), die in den Büchern
  einen Eigennamen tragen.
- D-559–D-566 gehören zum Plan `feature-tips.md`; seine alten Platzhalter D-546–D-553 kollidierten
  mit den inzwischen vergebenen jetzt-Runden (D-546–D-548) und D-554–D-558 und wurden am
  2026-11-21 auf D-559 ff. umnummeriert.
- Nummern, die in keinem Buch auftauchen (Lücken wie D-290), sind hier absichtlich nicht aufgeführt.

## D-001–D-060

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-001 | Langdruck-Popup mit Alternativen | multi-alternative long-press popup |
| D-002 | Punkt-Taste: Satzzeichenliste | Full-Stop Long-Press Punctuation List |
| D-003 | Leertaste zeigt die Sprache | space bar shows the language |
| D-004 | Tasten-Press-Feedback (Aufblitzen) | space bar (and every special key) flashes on press |
| D-005 | Tastenton (optional) | optional key sound + haptics (default off) |
| D-006 | Haptik-Feedback (optional) | Optional Haptic Feedback *(configurable, default off)* |
| D-007 | Beschleunigendes Backspace-Halten | accelerating backspace-on-hold |
| D-008 | Backspace-Shift-Rücksetzung | Backspace Shift-Restore Refinement (extends the G-05 addendum) |
| D-009 | Roh-Tipp-Aufzeichnung (Diagnose) | raw-tap recording (opt-in diagnostic) |
| D-010 | Backspace am Eintragsanfang | backspace at start of entry |
| D-011 | Frühere + Fuzzy-Vorschläge | earlier + fuzzy suggestions |
| D-012 | Fuzzy-Umlaut-Korrekturen | Fuzzy, Umlaut-Aware Correction Candidates |
| D-013 | Nutzer-Worttraining | user word training |
| D-014 | In-Tastatur-Langdruck-Feedback | in-keyboard long-press feedback |
| D-015 | Feststell-Taste (Caps Lock) | Caps Lock |
| D-016 | Musterbasierte Tastenvergrößerung | pattern-driven key enlargement |
| D-017 | Onboarding-USP-Text | onboarding USP text |
| D-018 | Emoji-Panel-Schalter | emoji panel toggle |
| D-019 | Seitenwechsel-Wisch | G-03 gesture toggles (external PR #11) - RESOLVED (§520, v1.2.79/v1.2.80, adopted 2026-10-26) |
| ↳ D-19/G-03 | Gesten-Schalter (externer PR #11) | folgt der Basis-Nummer |
| D-020 | Größere Gesten-Schwellen | Larger Gesture Thresholds |
| D-021 | Tasten-Zellenabstand | key cell padding |
| D-022 | Komma-Popup: Satzzeichen-Satz |  |
| D-023 | Vertikales Popup | vertical popup |
| D-024 | Touch-Modell-Visualisierung | touch-model visualisation |
| D-025 | Textfarben-Bug | text colour / D-26 mid-word bug |
| D-026 | Wortmitten-Bearbeitungs-Bug | text colour / D-26 mid-word bug |
| D-027 | Leertasten-Oberkante (Bug) | space-bar top edge (bug) |
| D-028 | Nachbar-+Distanz-2-Korrektur | proximity + distance-2 correction |
| D-029 | Satzzeichen frisst Vorschlags-Leerzeichen | punctuation eats accepted-suggestion space |
| D-030 | Einfrieren nach Backspace-Halten | (critical bug) keyboard freeze after held backspace |
| D-031 | Backspace-Halt-Tempo | backspace-hold speed |
| D-032 | Langdruck-Verzögerung + Einstellung | long-press delay + setting |
| D-033 | Popup-Grundtaste unten ausgerichtet | popup primary bottom-aligned |
| D-034 | Vibrationsdauer | vibration |
| D-035 | Wisch-Schwellenwerte | swipe thresholds |
| D-036 | Paste-Chip (direktes Einfügen) | direct paste |
| D-037 | Zaghafteres Wortlernen | (dictionary, `learnWord`) |
| D-038 | Korrekturqualität (Erstbuchstabe/Umlaut/Kosten) | Correction Quality: First-Char, Umlaut-Initial, Cost-Ranked |
| D-039 | Rohkoordinaten-Buchstabenkorrektur | Raw-Coordinate Per-Character Correction |
| D-040 | Ziffer im Wort behalten | digit-in-word |
| D-041 | Ziffer-Korrektur als Nachbar | Digits Are Ordinary Neighbour Characters in Correction |
| D-042 | Zahlenreihen-Trefferzone zu hoch | Number-Row Hit Zone Too Tall *(bug)* |
| D-043 | Nächste-Wort-Vorhersage | Next-Word Prediction in the Suggestion Bar |
| D-044 | Horizontale Alt-Popups, zentriert | Alt-Key Popups: Horizontal, Centred, Finger-Below Selection |
| D-045 | Auto-Großschreibung nach Interpunktions-Löschung | Restore Auto-Capitalisation After Deleting a Punctuation at Line Start *(bug)* |
| D-046 | Wischlänge erneut erhöht | Swipe Length Still Too Short |
| D-047 | Combined-Taste ohne Emoji-Glyph | Combined Key Must Not Show the Emoji Glyph When the Panel Is Off *(bug)* |
| D-048 | Umlautwörter vor Worttrennung | Umlaut Words Are First-Class; a Fold Match Beats a Split |
| D-049 | Roh-Tipp-Aufzeichnung als Onboarding-USP | Raw Touch-Recording as a Headline Onboarding USP |
| D-050 | Vorschlagsleiste dauerhaft sichtbar | Suggestion Bar Stays Permanently Visible |
| D-051 | Punkt-Langdruck-Reihenfolge | Period Long-Press Order Is `! . ?` |
| D-052 | Zahlenreihe wieder vollwertig | The Number Row Is a Full-Height Key Row Again |
| D-053 | Zahlenreihe mit Langdruck | The Number Row Long-Presses Like Every Other Key |
| D-054 | Ein-Alternativ-Popups zur Mitte | Single-Alternative Popups Are Nudged Toward the Keyboard Centre |
| D-055 | Reihenabstands-Einstellungen | Two New Spacing Settings: Below the Number Row and Above the Space Row |
| D-056 | Backspace-Halt: Einzelzeichen schneller | Backspace Hold: Single-Character Phase Slightly Faster |
| D-057 | Seitenwechsel-Breite neu | Page-Swipe Width Retune (Space Bar Gets More) |
| D-058 | Seitenwechsel-Animation | Page-Change Animation |
| D-059 | Optionale ?123-Taste | Optional `?123` Key (Default On) |
| D-060 | Schnell-Einfügen ohne Wirkung | Quick Paste Does Nothing (Bug) |

## D-061–D-120

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-061 | Enter in Adressleisten | Enter Does Not Submit in Browser Address Bars / Search Fields (Bug) |
| D-062 | Live-Vorschläge in der Wortmitte | Mid-Word Editing Produces Live Suggestions |
| D-063 | Umlaut-Vorschlag vor Korrektur | Missing Umlauts / ß: Suggest-Up-Front, Auto-Correct When Unambiguous |
| D-064 | Chip-Wegzieh-Geste (Papierkorb) | Suggestion-Bar Word Cannot Be Dragged to Trash (Bug) |
| D-065 | konnen-zu-kannen-Korrektur | `konnen` Auto-Corrects to `kannen` (Bug, HIGH) |
| D-066 | Tasten-Vibration wirkungslos | Key Vibration Does Not Fire At All (Bug) |
| D-067 | Zu eifrige Worttrennung | Word Splitting Fires Too Eagerly; Must Require High Confidence (Bug) |
| D-068 | Kalibrierung: Tippstil befragen | Rethink Calibration: Ask the Typing Pattern Directly, Refine Only Slowly Over Time (Big Idea, Deferred) |
| D-069 | Worttrennung gewinnt durch Fundreihenfolge | Word Split Wins Merely By Being Found First, Not By Being the Best Match (Bug) |
| D-070 | Tastenklang Schreibmaschine | Key-Press Sound Should Read as a Typewriter, Not a Digital Beep |
| D-071 | Muster-Seeding: Offset-Richtung | Pattern Seeding Must Also Predefine the Systematic Offset *Direction*, Not Just the Spread |
| D-072 | Zahlenreihe im Kalibrierungs-Seeding | The Number Row Is Missing From Calibration Seeding (Bug) |
| D-073 | Tippstile nach Verbreitung sortieren | Reorder the Typing Patterns by Real-World Prevalence; Skip Also Applies a Sensible Default |
| D-074 | Veraltetes Muster angezeigt (Bug) | Resolved (Unconfirmed Cause) |
| D-075 | Vibration-Verfolgung (D-66-Folge) | Key-Press Vibration Still Does Not Fire (Bug, D-66 Follow-Up) |
| D-076 | Seitenanimation rückwärts/Resize | Page-Slide Animation Runs Backwards and Resizes Mid-Slide (Bug) |
| D-077 | Empfohlen-Label entfernt | Remove the "(Recommended)" Label From the Two-Thumbs Button |
| D-078 | Doppelte Tippstil-Zeile entfernt | Dead Duplicate "Typing Pattern" Settings Row (Cleanup, Found During D-74 Follow-Up Investigation) |
| D-079 | Tippstil-Umbenennung | Rename "Typing Pattern" to "Typing Style" |
| D-080 | Zonen-Intro unterm Kamera-Loch | "Show Touch Zones" Intro Text Sits Under the Front-Camera Cutout |
| D-081 | Onboarding-Panel: Kamera-Loch | The Onboarding Panel Has the Same Cutout Gap |
| D-082 | Kein Zonen-Übersprechen (bestätigt) | Confirmed: No More Bleed |
| D-083 | Tastenklang zu laut/verzögert | Key-Click Sound: Too Loud and Slightly Delayed |
| D-084 | Wortmitte ohne Vorschläge | Mid-Word Editing (D-62) Produces No Suggestions At All (Bug) |
| D-085 | Tastenklang halbiert | Key-Click Sound: Halved Again |
| D-086 | Seitenanimation: Resize-Ordnung | Page-Slide: Resize Before Growing, After Shrinking; Bottom-Aligned Rows |
| D-087 | Wortmitte-Korrektur inaktiv ( reopened) | Root-Cause Fixes: Mid-Word Correction |
| D-088 | Feedback bei angenommener Korrektur | Refined: Sound Sharpened/Louder, Flash Replaced with a Flying Word, Settings Rename |
| D-089 | Feature-Übersichtskatalog | the in-app feature-overview catalog (`FeatureCatalog`, D-89, Settings → Info & Privacy) refreshed for the first time … |
| ↳ D-89-followup | Feature-Katalog-Auffrischung | folgt der Basis-Nummer |
| D-090 | Alt-Hinweise P/B/V (π × ÷) | Implemented: New Alt-Key Hints P → π, B → ×, V → ÷ (v0.7.44) |
| D-091 | Wischrichtung umgekehrt | Implemented: Reversed the D-19 Swipe Direction (v0.7.45) |
| D-092 | Symbolseiten-Redesign (Rechner) | addendum, raised by the user while resolving the space-key question |
| D-093 | Symbolseiten ohne 1/2+ABC-Knopf | Symbol Pages Drop `1/2` and `ABC` When the Combined `?123` Key Is Disabled *(implemented, v0.7.42)* |
| D-094 | Slide-Richtung nach D-91 | Implemented: Fixed the Slide-Animation Direction After D-91 (v0.7.52) |
| D-095 | Einstellungs-Kurztaste (geparkt) | Idea, Parked: a Settings-Shortcut Key Somewhere on the Keyboard |
| D-096 | v/b/x/c-Alt-Hinweise neu | Implemented: Reorganised the v / b / x / c Alt-Hints (v0.7.48) |
| D-097 | Symbol-Leertaste ohne Sprachlabel | Implemented: Symbol Pages' Space Key No Longer Shows the Language Label (v0.7.49) |
| D-098 | Währungs-Popup + Ecken-Hinweis | Implemented: Currency Popup Order + "More Alternatives" Corner Indicator (v0.7.50) |
| D-099 | Griechisch-Popup auf π | Implemented: Greek-Letter Popup on the π Key (v0.7.48) |
| D-100 | ABC/= -Tausch | Also: Swapped `ABC` and `=` |
| D-101 | Klammer-Tasten getrennt | Corrected: `(` and `)` Stay Two Separate Keys |
| D-102 | Seite-2-Alt-Belegungen korrigiert | Corrected/Extended: Bracket, Currency and Underscore Alts on Page 2 |
| D-103 | Trig-Taste (Rechner) | / D-104: Trig-Function and Angle-Unit Keys on the Calculator Page |
| D-104 | deg/rad-Umschalter | / D-104: Trig-Function and Angle-Unit Keys on the Calculator Page |
| D-105 | Hochzahlen auf Zifferntasten | Every Main-Page Digit Key Gets a Superscript Second Alternative |
| D-106 | Spracharchitektur (installierbare Sprachen) | Stages 1+2 Implemented: English as an Explicit Language, Cross-Dictionary Loanword Protection |
| D-107 | Erkennungs-Highlight-Semantik (geprüft) | Closed: S-05 Highlight Semantics Confirmed as Settled, No Reversal |
| D-108 | Langdruck verträgt Smear | Long-Press Popup Now Tolerates a Small In-Key Smear |
| D-109 | Lernen mit Grenzen (T-03) | T-03 Learning Now Has Bounds |
| D-110 | eBay-Feld: keine Auto-Großschreibung | , D-141, D-129 Confirmed Working on Device (No Code Change) |
| D-111 | stelle/Stelle-Prüfung | Not a Preview Bug: "stelle"/"Stelle" Is D-115's Exact Mechanism, Confirmed |
| D-112 | Kontextabhängige Korrektur-Großschreibung | a correction whose own case must follow the sentence/field context ("Fur" at a sentence start) now |
| D-113 | Ratio-Override nur Cost-1-Tippos | §44's Ratio-Override Restricted to Genuine Cost-1 Typos |
| D-114 | Keine stillen Low-Confidence-Korrekturen | A Low-Confidence Autocorrect Candidate Is Never Silently Applied + the Missing Word Added |
| D-115 | stelle/Stelle: Ranking vs. Datenlücke | Two Different Root Causes, Not One: a Ranking Bug vs. a True Data Gap |
| D-116 | Komposita: Erstteil + Rest (Vorschlag) | CONFIRMED WORKING on device |
| D-117 | Mehrfach-Typo-Vorschlag über Budget | A Multi-Typo Word Beyond D-28's Budget Still Surfaces as a Suggestion |
| D-118 | Durchgestrichen-Korrektur (Fall) | struck outright |
| D-119 | Leerzeichen in der Wortmitte | SPACE Mid-Word Now Inserts a Literal Space Instead of Finalising the Whole Token |
| D-120 | Interpunktion an der Caret-Position | Mid-Word Punctuation Now Lands at the Caret, Not Always at the Token's End |

## D-121–D-180

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-121 | handleShift: Caps-Lock-Prüfung zuerst | `handleShift()` Now Checks Caps-Lock-Off First and Requires the Caret to Genuinely Be at the Word's End |
| D-122 | A-05-Split (frühe Fassung) | the German feminine-agent-noun suffix `-in` must never be split off by A-05/D-122, mirroring |
| D-123 | Chip-Leerzeichen überlebt Interpunktion | Suggestion-Bar-Tap's Trailing Space Now Survives the Punctuation Check |
| D-124 | MIME-Erkennung verworfen | Closed (Not a MIME Bug), D-116 Implemented: Unhyphenated-Compound Recognition |
| D-125 | Verbform nicht auf Grundform korrigiert | An Unknown Inflected Verb Form Must Not Be Silently "Corrected" to the Dictionary's Base Form |
| D-126 | Mini-KI-Ein/Aus-Schalter | Tier-3 Independent Enable Toggle (the Uninstall Action Already Existed) |
| D-127 | Interne Referenz (konsolidiert) | internal reference, consolidated here for future sessions |
| D-128 | Nutzerseitige Zusammenfassung | user-facing summary |
| D-129 | Rechner-Minus: Vorzeichen-Hinweis | , D-141, D-129 Confirmed Working on Device (No Code Change) |
| D-130 | Englisch-Wechsel + Leertasten-Animation | Sustained-English Real Language Switch + Space-Bar Change Animation |
| D-131 | Rohkoordinaten live beim Composing | Raw-Coordinate Correction (D-39) Now Live While Composing |
| D-132 | Reihenhintergrund entlastet | and D-137 Follow-Up Fixes: Neither Original Fix Actually Worked |
| D-133 | Untere Touchzonen: harte Untergrenze | Bottom-Row Touch Zones: a Hard, Direction-Specific Downward Bound |
| D-134 | Leertaste nach Punkt (unbestätigt) | SPACE Sometimes Seems to Get Swallowed After a Full Stop (Unconfirmed Pattern) |
| D-135 | Autofill: kein Nutzernamen-Vorschlag | Struck From the Backlog |
| D-136 | Navigationsleisten-Helligkeit | reworked |
| D-137 | Ziffern erreichten Fix-Pfad nie | and D-137 Follow-Up Fixes: Neither Original Fix Actually Worked |
| D-138 | Backspace-Ruckeln: Lookups pro Tastendruck | Backspace-Hold Jerkiness: Traced (Not Guessed) to This Session's Own Added Per-Keystroke Lookups |
| D-139 | Text-Jitter/Scramble-Bug | , Second Half: Truth Verification Replaces Positional Equality in `onUpdateSelection` |
| D-140 | Zurückgewiesenes wird verlernt | Implemented: Un-Learn on A-07 Undo (T-03 + Dictionary); D-141: "Uhr" Timing Fixed |
| D-141 | Uhr-Vorschlag zu früh | , D-141, D-129 Confirmed Working on Device (No Code Change) |
| D-142 | Zugangsdaten: Sofort-Speichern + eigene Liste | Follow-Up: Weak-Signal Detection Now Also Covers Email, Not Just Username |
| D-143 | URL-Tastaturlayout | Implemented: a Dedicated URL-Mode Bottom Row |
| D-144 | Umlaut-Entfaltung + Popup-Dedup | Extend the umlaut/diacritic unfold mechanism (D-144/D-204) to other languages |
| D-145 | Popup zentriert vorgewählte Zelle | Actually Fixed, D-145 Corrected Again: the Popup's Centre Position, Not List Order |
| D-146 | Kein Doppel-Leerzeichen beim Chip-Tap | Applying a Suggestion Mid-Text No Longer Creates a Double Space |
| D-147 | Umlaut-Präfix-Vervollständigung | Umlaut/ß-Aware Prefix Completion for Suggestions |
| D-148 | A-07: Backspace nach Chip (erfasst) | withdrawn |
| D-149 | Backspace nach Reclaim löscht zu viel | a D-62 Mid-Word Reclaim's Backspace Could Wipe the Whole Word Instead of One Character; Nine New Backlog Items … |
| D-150 | Log: 1 Minute, Passwort nie | diagnostic log window shortened to 1 minute; password fields never logged, in code and in the Settings text |
| D-151 | Log-Viewer unterm Notch | the log viewer's Share/Copy/Clear row was unreachable under the display notch |
| D-152 | Regression: Wort-Total-Löschung | (regression) |
| D-153 | Regression: Backspace-Ruckeln | (regression) |
| D-154 | uber-zu-über nach Wortende | `uber` should autocorrect to `über` once the word is *finished* (a delimiter typed) - ambiguous |
| D-155 | fur als Korrektur-Ziel grün | `fur` colours green (recognised as a correction target, i.e. `shouldHighlightComposing`/S-05 or |
| D-156 | Zonen-Visualisierung als Einstellung | a live touch-zone-visualisation toggle in the settings row |
| D-157 | due-die-Schutz Deutsch | "due" no longer protected from autocorrecting to "die" in German mode |
| D-158 | E-Mail-Tastaturlayout | Dedicated Email-Mode Keyboard Layout |
| D-159 | Robuste Zonen-Gewichtung (Huber) | Robust (Huber-Style) Downweighting for the Touch-Zone Learning Model |
| D-160 | Vorschau-Flash im Deferred-Pass | Implemented: the Expensive Suggestion Fallbacks Are Debounced Off the Hot Path |
| D-161 | WindowInsets-Nachcheck | retune D-161's WindowInsets recheck from a single one-shot check to five checks, 500ms apart |
| D-162 | Vorschlagsleiste scrollbar | fixed: the suggestion bar could not be scrolled to reach overflowing suggestions |
| D-163 | Keine Auto-Caps in E-Mail-Feldern | fixed: no auto-capitalisation inside a recognised email field |
| D-164 | sue-die beidseitig | fixed: "sue" too often replaced a typed "die" and never got corrected back |
| D-165 | Wortmitte-Trennung vs. Großschreibung | mid-word split can destroy deliberately-typed capitalisation |
| D-166 | DiecVorschlaege: kein Auto-Split | withdrawn |
| D-167 | Fall-Wiederanwendung (offen) | re-applying case. Not yet fixed - see D-167. |
| D-168 | Umlaut-Popups im Caps Lock | in Caps Lock / Shift, the umlaut alternatives in a key's long-press popup stay lower-case |
| D-169 | E-Mail-Feld: Extra-Reihe + Flash | entering a recognised email field should also open the settings row and flash the credential |
| D-170 | E-Mail-Caret-Backspace-@-Sprung | Withdrawn; D-170 Fixed (Real Device Log); D-175 Diagnostic Added for Never-Saved Credentials |
| D-171 | Ddr-statt-Der-Verdacht (Rohmodell) | /D-176: "ddr" and D-164's Cross-Language Shield Redesigned onto a Real, Data-Driven Blacklist |
| D-172 | aks-zu-als-Frage | why doesn't "aks" autocorrect to "als"? |
| D-173 | Allein erkanntes Wort: Slot 1? (Designfrage) | design question, answered directly: should a lone/near-lone recognised word take suggestion slot 1? |
| D-174 | Diagnose-Log-Runde (als D-175 gelabelt) | Closed: a Fast-Tearing-Down `InputConnection` Beat `onFinishInput`'s Own Read of It |
| D-175 | Diagnose-Kanal (weg zu lassen) | `AdaptKey` channel - to be removed once D-175 is actually closed. Not yet fixed - waiting on the next |
| D-176 | Blacklist-Seeding-Lücke | /D-176: "ddr" and D-164's Cross-Language Shield Redesigned onto a Real, Data-Driven Blacklist |
| D-177 | Wiederholungs-Eskalation zur Blacklist | Learned Words Moved to a Separate Store; Two-Stage Unlearn-Then-Provisional-Blacklist; a New Learned-Words Editor |
| D-178 | aks fehlt (Prüfung) | /D-179: One-Time Bundled-Dictionary Reimport to Flush Pre-D-177 Contamination; Clearer Remove-Confirmation Dialogs |
| D-179 | Entfernungs-Dialoge bestätigt | /D-179: One-Time Bundled-Dictionary Reimport to Flush Pre-D-177 Contamination; Clearer Remove-Confirmation Dialogs |
| D-180 | Zugangsdaten: pro Eintrag vergessen | Saved Credentials Get Their Own Reviewable List; a Shared "Copy" Option on Every Remove Dialog |

## D-181–D-240

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-181 | Nebenpunkt ohne Code | side item (no code change) |
| D-182 | Gelernt-Flut: der echte Fix | `reclaimSurroundingWord()` Stops Deleting Text It Only Needs to Recolour |
| D-183 | Chip-Leerzeichen vor Punkt | Fixed - a Mid-Word Reclaim Leaves the Real Cursor Inside the Token, Not at Its End |
| D-184 | Flash-Highlight kürzer | device feedback (2026-07-19): no visible effect yet |
| D-185 | URL-Tastatur umschaltbar | /D-181 Device-Confirmed; D-184/D-185 Implemented |
| D-186 | Gelernt-Overlay-Flut | /D-187/D-188: Learned-Overlay Flooding, Settings-Row Auto-Close, List-Activity Insets |
| D-187 | Extra-Reihe schloss nach jedem Tipp | /D-187/D-188: Learned-Overlay Flooding, Settings-Row Auto-Close, List-Activity Insets |
| D-188 | Listen-Editor unterm Notch | /D-187/D-188: Learned-Overlay Flooding, Settings-Row Auto-Close, List-Activity Insets |
| D-189 | SettingsRow-Wegzug (ExtraRowView) | `SettingsRowView` Renamed to `ExtraRowView`; Credential-Mode Reveal + Delayed Flash |
| D-190 | Zugangsdaten-Speicher-Flag | "Never Save Credentials" Setting |
| D-191 | Kontakt-Vorschläge (Schalter) | Opt-In Contact-Derived Email Suggestions |
| D-192 | Feature-Katalog veraltet (Nachzieh-Fix) | Feature Catalog Refreshed; "Learn More" Hint; D-191 Summary Trimmed |
| D-193 | Vibration-Enddiagnose | Diagnostic Logging for the Still-Never-Working Key Vibration |
| D-194 | Trägheit: drei ungefilterte Lookups | Typing/Backspace-Hold Sluggishness Traced to Three Unthrottled Per-Keystroke Lookups |
| D-195 | Vierte Idee (geschoben) | ith no behavioural change either way. D-195 (a fourth idea, raised and shelved, not |
| D-196 | Chip-Großschreibungs-Mismatch | suggestion-chip case mismatch |
| D-197 | Gruße-zu-Grüße-Wiederherstellung | "Gruße" never restored to "Grüße"; wrong suggestion "Große" instead |
| D-198 | E-Mail-Punkt: .net statt .de | email keyboard's period-key popup pre-selects `.net` instead of the locale's own `.de` |
| D-199 | Autokorrektur-Reapply (Strategievorschlag) | (strategy proposal, explicitly requested, not implemented): autocorrect silently reapplying a |
| D-200 | B-03-Designabsicht (nie gebaut) | B-03 / D-200 (spec-documented design intent since the original spec, never implemented; revisited with |
| D-201 | Kleiner Fix (D-201) | fixed |
| D-202 | Design-Diskussion (nicht gebaut) | captured as a design discussion, not implemented |
| D-203 | MIN_SPLIT_BIGRAM-Gate neu | The MIN_SPLIT_BIGRAM Gate Replaced After a Live Design Discussion |
| D-204 | Umlaut-Entfaltung (Kern) | Extend the umlaut/diacritic unfold mechanism (D-144/D-204) to other languages |
| D-205 | Korrektur-Fuzzy-Designgespräch | Designed and Implemented: Suggestion-Bar Ranking Now Discounts by Edit Cost |
| D-206 | Datenbasiertes Designgespräch | Designed and Implemented: Archaic-Spelling Relics Blacklisted, Not Purged |
| D-207 | Doppelsuche zum Commit | the commit-time double search |
| D-208 | Live-Fuzzy skaliert mit Tokenlänge | live per-keystroke fuzzy search scaling with token length |
| D-209 | Kita-Bucket-Cap-Verhungerung | the "Kita" bucket-cap starvation, found independently while discussing the felt lag |
| D-210 | A-05-Split-Regression (geschoben) | half addressed. D-210 (the A-05 split regression) remains its own, separately deferred item. |
| D-211 | Hintergrundsuche abbrechbar (Polling) | The Expensive Suggestion Search Moves Off the Main Thread Entirely |
| D-212 | SQLite-Abort/WAL-Diskussion | WAL Enabled - D-211 Alone Showed No Improvement, Root-Caused to Connection-Level Locking |
| D-213 | Cancel-on-Restart-Mechanik | The Real Dominant Cost Found - trySplit()'s Own Main-Thread Dictionary Reads |
| D-214 | SQLite-Refetches in candidateAt | /D-215/D-216: The Real Cost Was O(n×m) Computation, Not Stale Requests - Redundant Queries |
| D-215 | Debounce wiedereingeführt | /D-215/D-216: The Real Cost Was O(n×m) Computation, Not Stale Requests - Redundant Queries |
| D-216 | Suchabbruch (mit D-211) | /D-215/D-216: The Real Cost Was O(n×m) Computation, Not Stale Requests - Redundant Queries |
| D-217 | Diagnose: Verarbeitungs-/Flash-Zeiten | Diagnostics Now Show Actual Processing Time and Actual Flash-Paint Latency, Not Just Typing |
| D-218 | Restkosten: Preview lief immer | The Real Remaining Cost Found - the Impending-Autocorrect/Diacritic Preview Ran Unconditionally |
| D-219 | Gerätebestätigter Erfolg (217-219) | the Flash Effect Decoupled from Key Processing and Shortened, at the User's Own Explicit |
| D-220 | Diagnose: Commit-Suche | Device-Confirmed, D-221 Fixed: diacriticRestoration() Uncapped Every Bucket, Not Just the Ones |
| D-221 | Synchrone Berechnung als Kostenursache | D-221 Sluggishness Investigation Device-Confirmed Closed (No Code Change - Confirmation Only) |
| D-222 | Debug-Ziel bis D-223 | the URL-Mode Protocol and www Keys Merged, Restoring the Space Key to Full Width |
| D-223 | assembleRelease als Standard | `:app:assembleRelease` is now the default build target |
| D-224 | Backup: saveCredentials-Flag | the Inverted "Never Save Credentials" Toggle Flipped to a Positively-Phrased "Save Credentials" |
| D-225 | Reclaim-Backspace-Design umgesetzt | `_` is Now Word-Extending and a Token Containing It Is Fully Shielded From |
| D-226 | Split-Fehler com/its | trySplit() Never Respected suppressAutocorrect/knownInOtherLanguage - "commits" Torn Into |
| D-227 | Non-Nomen Cost-1-Regel | the D-114 Frequency Floor Was Cost-Blind - Fixed by Also Consulting Part-of-Speech, not Cost |
| D-228 | Wiedereröffnet (Paar mit D-230) | and D-230 are both reopened |
| D-229 | Gerätebestätigt (Paar 228/230) | confirmed working on device |
| D-230 | Wiedereröffnet (Paar mit D-228) | and D-230 are both reopened |
| D-231 | Touch-Zonen-Familie (Punkt-Taste) | Enter's Learned Touch Zone Capped Against Drifting Into Backspace, Mirroring D-109/D-133's |
| D-232 | Wörterbuch-Zugaben Alltag | D-230's "darfst"-Class Regression and D-228's IT-Vocabulary Gap Closed via Dictionary Data, |
| D-233 | Touch-Zonen-Familie (Teil) | `m`'s Learned Touch Zone Capped Against Drifting Into Backspace (Horizontal Axis); D-234 |
| D-234 | Autokorrektur-Hauptschalter | user asked for a settings toggle disabling autocorrect generally (default on) - with the S-05 |
| D-235 | Punkt-Zone zu weit links | Seeded Touch-Zone Spread No Longer Scales Against a Widened Key's Own Size; D-236: T-06 |
| D-236 | T-06-Visualisierungs-Bugfund | while investigating the above, found (via a dedicated Explore pass) that the T-06 visualisation |
| D-237 | Reset-Flow wirkungslos | `CalibrationActivity` and `TouchModelActivity` Merged Into One Screen; the Reset Action No |
| D-238 | Frühe Implementierung (Referenz) | a Position-1 Suggestion Chip Closes D-234's Own Disclosed Gap - A-05 Split/A-06 Merge Now |
| D-239 | onStartInput zweimal (restarting) | a Touch-Calibration Reset From `CalibrationActivity` Never Reliably Reached the Already |
| D-240 | Chip-Farbe sattes Grün | the S-05 Highlight Colour Moved From a Dark, Muted Tone to a Brighter, More Saturated One |

## D-241–D-300

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-241 | Farb-Bestätigung (nach D-240) | Confirmed the Brighter Green, Settled on a More Subdued Shade; D-242: the S-05 Highlight No |
| D-242 | Test-Langsam-Bug (durch neuen Farbton sichtbar) | the brighter colour made a second, pre-existing bug newly obvious - typing "Test" slowly, the |
| D-243 | rawTap-Diagnosezeilen | Raw Touch Coordinates Now Feed the X-01 Diagnostic Log; D-244: "Ohren" -> "Ihren" Root-Caused |
| D-244 | Doku-Referenz-Fix | for `## §142` or a `D-244` reference). History.md is explicitly append-only and never rewritten, so nothing |
| D-245 | Editor-Batching-Präzedenz | A Mid-Word D-122 Split Chip's Own Trailing Space Silently Escaped D-29's Punctuation-Eating |
| D-246 | Persönliche Trigramm-Tabelle (S-07) | S-07 Next-Word Prediction Elevated by a Personal, Self-Growing Trigram Table |
| D-247 | Farbpaar Vergessen | A "Gelernt: X" Promotion Confirmation With Its Own Two-Zone Drag, Closing the Silent-Learning |
| D-248 | Verlernen per Backspace | Backspacing Back Into a Recently-Learned Word Un-Teaches It |
| D-249 | un-/ent- nie abtrennen | Inseparable German Verb/Negation Prefixes Protected From A-05/D-122 Splitting |
| D-250 | Insets-Recheck: fünf Checks | retune D-161's WindowInsets recheck from a single one-shot check to five checks, 500ms apart |
| D-251 | Reclaim öffnet Pipeline nicht neu (Plan) | a rough, not-yet-refined plan: a word-end reclaim should not reopen the full suggestion/autocorrect |
| D-252 | Regular-case-only-Schutz | "zuversichtlicher" -> "zuversichtlich er" Closed via Comparative/Superlative Recognition, |
| D-253 | Blacklist-Relearn-Symptom-Fix | A Blacklisted Word Could Silently Get Re-Learned by Ordinary Retyping |
| D-254 | Absichtssignal Gelernt-Markierung | The "Gelernt: X" Chip's "Vergessen" Zone No Longer Marks Pending-Blacklist |
| D-255 | Backspace-Hold-Cadence-Bug | Fixed; D-256/D-257/D-258/D-259 Captured: Five Real Device Logs Triaged |
| D-256 | Composing-Mismatch: externer Guard | Fixed; D-256/D-257/D-258/D-259 Captured: Five Real Device Logs Triaged |
| D-257 | Composing-Mismatch 257 | Fixed; D-256/D-257/D-258/D-259 Captured: Five Real Device Logs Triaged |
| D-258 | Composing-Mismatch 258 | Fixed; D-256/D-257/D-258/D-259 Captured: Five Real Device Logs Triaged |
| D-259 | Leertasten-Touchzone-Fix | Fixed; D-256/D-257/D-258/D-259 Captured: Five Real Device Logs Triaged |
| D-260 | Leertasten-Zone im Gesture-Inset | The Space Key's Own Touch Zone Now Extends Into the Reclaimable Part of the Gesture Inset |
| D-261 | -in-Suffix nie abtrennen | the German feminine-agent-noun suffix `-in` must never be split off by A-05/D-122, mirroring |
| D-262 | Auto-Leerzeichen nach Satzzeichen | automatic space insertion after sentence punctuation, with a punctuation-run mode that lets |
| D-263 | Retro-Split-Timing-Race | a real timing race in the A-05 retroactive-split pipeline |
| D-264 | Gelernte Groß-/Klein-Overrides | remains open, explicitly deferred at the user's own request |
| D-265 | Undo-Fenster vs. versehentliches Enter | A-07's post-commit undo window is destroyed by an accidental Enter (or any keystroke) instead of |
| D-266 | Paste-Chips Erste Zeile/Erster Code | two new clipboard-paste chip kinds, "Erste Zeile" and "Erster Code", alongside V-01's existing |
| D-267 | Clear-Clipboard in die Leiste | The Clear-Clipboard Button Moves From The Extra Row Into The Suggestion Bar |
| D-268 | Split: umlaut-restaurierte Form | A-05 Split Now Commits The Umlaut/ß-Restored Spelling, Not The Literal Typed One |
| D-269 | Auswahl schlägt Undo/Pending-Space | A Genuine Selection Now Overrides Every Special Mechanism; D-262's Punctuation-Run Fixed |
| D-270 | Batching koalesziert Sequenz (KDoc) | Three More A-12 Bugs Found From A Real Device Log - §188's Fix Was Incomplete |
| D-271 | Satzanfangs-Großform gelernt | a sentence-start-capitalised ordinary word was being learned in that capitalised spelling |
| D-272 | natürlich hinter natürliche gerankt | "natürlich" ranked behind "natürliche"/"natürlicher" in suggestions |
| D-273 | Undo kaperte Backspace nach Split | /D-272/D-273: Three More Bugs From The §190 Device Test |
| D-274 | Insets-Selbstheilung vs. Padding-Split | The D-161 Window-Insets Self-Heal Has Permanently Disagreed With D-260's Own Padding Split Since D-260 Shipped, … |
| D-275 | Insets-Retry-Kette stirbt | The D-161 Retry Chain Was Silently Dying On Its First Attempt, Explaining Why The Self-Heal Never Actually Caught … |
| D-276 | suppressNextUndoClear Single-Shot | A-07's Undo Redesigned - Whitespace Consumed Before The Revert Fires, A Fixed Deletion Range Instead Of A Counter, … |
| D-277 | Undo-Form nach 276/277 | §195's Caret-Move Guard (suppressNextUndoClear) Broke A-07 Outright - Replaced With Ground-Truth Verification At The … |
| D-278 | Gerätekopplung: persönlicher Zustand | Export / Import Backup, Designed With The User First Then Built In Full |
| D-279 | Gerätebestätigung (D-279) | device-confirmed |
| D-280 | Nur English gebündelt | follow-up - RESOLVED, all four languages now have real, hosted packs |
| ↳ D-280/281-followup | Gehostete Pakete (4 Sprachen) | folgt der Basis-Nummer |
| D-281 | Installierbare Pakete (Architektur) | device-confirmed |
| D-282 | Popup-Umkehr am Rand (alt) | device-confirmed |
| D-283 | Versionssprung 0.9.0 | Minor Version Bump To 0.9.0, No Code Change |
| D-284 | ß/ƒ-Großform-Bug | A Key's Static Corner Hint Glyph Now Uppercases Under Shift/Caps Lock Too, Matching The Popup |
| D-285 | ß ohne SS, ƒ bleibt klein | D-284 Follow-Up - ß No Longer Shows "SS", And ƒ (Like π) Stays Lower-Case Under Shift |
| D-286 | Split-Revert bestätigt (§209) | A-07's Own Revert Silently Broken For Every Ordinary Space-Delimited Commit, Traced From The "wird" Split Report's … |
| D-287 | Sprachpipeline-Fix (§210) | A-05/A-06/A-10 Silently Checked Every Token Against The English Dictionary Regardless Of The Active Language |
| D-288 | Bewusst enger Fall offen (TODO) | URL-Field Auto-Capitalisation At Field Entry, A Narrower Guard Than Its Own Established Sibling Condition |
| D-289 | Ende-zu-Ende bestätigt (§214) | B-03 Built - Proactive Hyphen-Compound Completion With Its Own Dedicated Undo |
| D-291 | Bindestrich-Komposit-Schwelle | the hyphen-compound promotion threshold is too high |
| D-292 | Gelernt-Editor: Casus korrigierbar | the Learned Words editor's per-entry dialog should allow fixing a casing mistake |
| D-293 | Editieren: Lernen ganz aus | /D-294: Learned Words Edit Dialog Redesigned Around The Casing Field, Plus A Real Gap It Surfaced |
| D-294 | Layout-Redesign-Frage | /D-294: Learned Words Edit Dialog Redesigned Around The Casing Field, Plus A Real Gap It Surfaced |
| D-295 | Sprachauswahl-Default (§217) | Learned Words / Blacklist Language Selector Now Defaults To The Keyboard's Own Active Language |
| D-296 | Gelernt-Editor: quadratische Knöpfe | D-295 Confirmed; Learned Words' Copy/Save Buttons Made Square, Not Wide |
| D-297 | Mini-KI-Eintrag entfernt | "Mini KI aktivieren" (`d126_tier3_enabled`) removed entirely, folded into a new "Disabled" entry on |
| D-298 | Highlighting-Optionen reduziert | "Highlighting" (`c04_highlight_enabled`) removed entirely, folded into a new "No highlighting" |
| D-299 | Reihenfolge: Rest (mechanisch) | the rest, mechanical |
| D-300 | Zwei Reorder-Wünsche (§219) | Two Further Settings Reorder Requests From §219's Own Round |

## D-301–D-360

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-301 | Slider-Werte nie umschreiben | C-08 Per-Key Corner-Symbol Override Removed Entirely - Only The Per-Language Default Remains |
| D-302 | Bunter Sprachdialog | C-04 Highlight-Colour Preference Now Previews Each Choice As Its Own Text Colour |
| D-303 | Google-Keep-Retest bestätigt | Confirmed; D-304: Backup Export Now Follows The Settings Screen's Own Order And Never Carries The Diagnostics Toggle |
| D-304 | Export in Anzeigesprache | Confirmed; D-304: Backup Export Now Follows The Settings Screen's Own Order And Never Carries The Diagnostics Toggle |
| D-305 | Kalibrierungs-Flag nicht exportiert | The Flagged `k01_calibration_offered` TODO From §224 Actioned The Same Way As The Diagnostics Toggle |
| D-306 | Wörterbuch-Tagging-Fix | /D-306-followup/D-345: Dictionary Garbage Cleanup - 348 Noise Entries Removed |
| ↳ D-306-followup | Cleanup: getaggte Zeilen | folgt der Basis-Nummer |
| D-307 | Paket-Update-Mechanik | Language-Pack Update Mechanism |
| D-308 | Update-Erkennung aus Archiv | Language-Pack Updates Now Authoritatively Detected From The Archive Itself, Not Just This App's Own Compiled-In Hint |
| D-309 | version.txt-Umbenennung | The D-308 Version File Renamed `version.txt`, Dropping The Redundant `<code>` Suffix |
| D-310 | Sprachidentität-Flag | Every Language-Pack File Now Uses A Fixed Name In Its Own Per-Language Folder, Not A `_<code>` Suffix In One Shared … |
| D-311 | Legacy-Flat-File-Cleanup entfernt | D-310's Legacy-Flat-File Cleanup Removed - No Longer Needed |
| D-312 | Rasche Doppel-Shift-Bugs | /D-313: Two Shift-State Bugs - Rapid Double-Tap Losing To G-05, And A Caret Tap Into Existing Text Not Re-Deriving … |
| D-313 | Shift-Arm aus Caret-Position | /D-313: Two Shift-State Bugs - Rapid Double-Tap Losing To G-05, And A Caret Tap Into Existing Text Not Re-Deriving … |
| D-314 | Französisch-AZERTY-Tippen | RESOLVED (§413 + §415, v1.1.54): D-314's AZERTY geometry now has a real `dictionaries/fr/` |
| D-315 | Versionssprung 1.0.0 | Version Bumped To 1.0.0, A Milestone Marker - No Code Change |
| D-316 | Feature-Katalog veraltet (D-192-Folge) | Confirmed: Feature-Overview Screen Checked On Device |
| D-317 | Emoji-Suche (SwiftKey-Stil) | Emoji Search, SwiftKey-Style ("poop" Finds 💩) |
| D-318 | Zwei Sichtbarkeits-Fixes bestätigt | Confirmed: Framed Action Icons + Query Chip Checked On Device |
| D-319 | Nur ein Schreiber-Chip sichtbar | Fixed a Real Regression - Suggestion Bar "Doubled" Whenever Autofill Inline Suggestions Were Left Over From a … |
| D-320 | Komma armiert Auto-Space | Confirmed: Comma Parity and the Digit-Glue Decimal Case Both Checked On Device |
| D-321 | App-Icon (Entwurf) | Real App Icon + F-Droid Store-Listing Icon - an "A" Keycap With the T-06 Touch-Zone Visualisation, Replacing the … |
| D-322 | App-Icon (Umsetzung) | Card Size Reverted 66x66 -> 46x46 - Real-Device Evidence Beat the Documented Safe Zone |
| D-323 | Doppelte Leiste: Row kollabierte nicht | The D-319 "Doubled Bar" Bug Had a Second Half - suggestionRow Itself Never Collapsed When Autofill Took Over the Slot |
| D-324 | Clipboard-Chip-Verdrängung | /D-325: Root Cause Confirmed From a Real Log (an Unstable Autofill Responder in Signal), Debounced Instead of … |
| D-325 | Autofill-Inline nur Credential-Felder | /D-325: Root Cause Confirmed From a Real Log (an Unstable Autofill Responder in Signal), Debounced Instead of … |
| D-326 | Signal-Chip bleibt (Abschluss) | Confirmed: Signal's Clipboard Chip Now Stays Put, Closing the D-323..D-326 Suggestion-Bar/Autofill Saga |
| D-327 | Bigram für Gebündeltes (Purge) | Bigram/Trigram Context Learning Was Silently Skipped for Every Bundled Word in Canonical Casing; "mein kampf" Bundled … |
| D-328 | Früh-Typo-Nachbar-Eskalation | Early Typo Recovery: Neighbour-Substituted Prefix Completion Escalation |
| D-329 | Deutsch-Paket neu gebaut | German Language Pack Rebuilt and Version-Bumped - D-327's Fix Had Only Reached the Repo Source and the Runtime Purge, … |
| D-330 | deine-Registerkorrektur | RESOLVED by §304 (v1.0.57) |
| ↳ D-330-followup | Possessivpronomen-Audit | folgt der Basis-Nummer |
| D-331 | Backspace-Verlern-Konflikt | Reverted Word Was Immediately Unlearned by the Next Backspace - maybeUnlearnOnBackspaceReturn (D-248) Fired on a Word … |
| D-332 | x als unbeabsichtigter Trenner | "x" Was Not Treated as a Possible Unintended Separator - Both Over-Space Sets Hardcoded `c v b n m` Only |
| D-333 | Caps-Lock-Satzanfang-Fenster | Double-Tap Shift for Caps Lock Did Not Engage at a Sentence Start (Auto-Capitalisation Active) |
| D-334 | Sprachpaket-Update-Sicherheit | Language Pack Update Safety (D-334) |
| D-335 | Backspace-Shift-Übertrag | Backspace-Shift Carry (D-335) |
| D-336 | Erweiterte Buchstaben-Popups | Extended Letter Long-Press Popups (D-336) |
| D-337 | Caps-Lock-Rand-Highlight | Caps-Lock Border Highlight (D-337) |
| D-338 | Doppel-Tipp Wortanfang wiederholt | The double-tap word-start toggle (§264/D-338) worked for the first toggle but not for a repeated one: a |
| D-339 | Doppel-Tipp wiederholt (Composing leer) | Double-Tap Word Toggle: Repeated Toggle Did Not Work (composing Emptied After First Toggle) |
| D-340 | Backspace-Halt: Slide-Off | Backspace Hold — Slide-Off Continuation (D-340) |
| D-341 | à/À auf der a-Taste | Additional Long-Press `à`/`À` on the 'a' Key (D-341) |
| D-342 | NOUN_OR_VERB-Rework Deutsch | German Dictionary — NOUN_OR_VERB Rework (D-342) |
| D-343 | Caps-Lock-Vibration subtil | Caps Lock Vibration — Direct Vibrator, Subtle Effect (D-343) |
| D-344 | Download-Ordner-Steuerung | (download directory control, spec §30) - RESOLVED, mechanism changed by D-413 (see below) |
| D-345 | Wörterbuch-Rausch-Scan | (dictionary noise scan, spec §31) - RESOLVED by §301 (v1.0.54); the "Bri" loose end struck |
| D-346 | Fuzzy-Ladeindikator | Fuzzy Search Loading Indicator in the Suggestion Bar (D-346) |
| D-347 | Gemini-Cursor-Nub | Gemini App — Cursor Nub Keeps Disappearing (D-347) |
| D-348 | Doppel-Backspace-Rückgängig | Optional Double-Tap Backspace for Autocorrect Revert (D-348) |
| D-350 | Debounce leeres Composing | The Nub-Drag Corruption Is Fixed - But Reactively Reclaiming Every Intermediate Drag Position Stalls the Gemini … |
| D-351 | D-351 (erneut geschlossen) | CLOSED AGAIN (2026-09-03, no code change - not reproducible) |
| ↳ D-351-followup | Erneut geschlossen (nicht reproduziert) | folgt der Basis-Nummer |
| D-352 | Gelernte über Override + Split-Dreiweg | RESOLVED (§280 v1.0.37, §285 v1.0.41) |
| D-353 | Vertrauen: Grundlagen | RESOLVED (§281 v1.0.38, §284 v1.0.40, part of §280 v1.0.37) |
| D-354 | Vertrauen: Ausbaustufe | RESOLVED (§281 v1.0.38) |
| D-355 | Nutzerbestätigung (D-355) | RESOLVED (2026-09-01, no code change - user confirmation) |
| D-356 | Umlaut bricht Korrektur-Gleichstand | RESOLVED, device-confirmed (§456, v1.2.16) |
| D-357 | Wortmitte + Doppel-Shift (wieder gefixed) | REOPENED then RESOLVED for real, device-confirmed (§449, v1.2.9) |
| D-358 | Revert: ein ungehinderter Retry | RESOLVED (§289 v1.0.44) |
| D-359 | Revert-Arc (mit D-358/D-403) | RESOLVED (§289 v1.0.44) |
| D-360 | Abgelehnt (D-360) | WON'T FIX (2026-09-04, no code change - discussed and declined) |

## D-361–D-420

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-361 | Sticky-Shift-Fenster (View) | RESOLVED and device-confirmed (§391-§399, v1.1.30-v1.1.38); the retroactive-reinterpretation |
| D-362 | Lade-Chip auffällig/animiert | RESOLVED (§338, v1.0.90) |
| D-363 | Auto-Space für Doppelpunkt/Semikolon | WON'T FIX (2026-08-31, no code change - discussed and declined) |
| D-364 | Doppelter Text-Chip (Dedup-Bug) | RESOLVED (§339, v1.0.91) |
| D-365 | Bigram fair im Wettbewerb | RESOLVED (§340, v1.0.92) |
| D-366 | Bigram überlebt beim Tippen | RESOLVED (§340, v1.0.92) |
| D-367 | natu-Präfix-Ranking | `"natu"` prefix-completion ranking needs several corrections |
| D-368 | Homograph-Tags case-neutral | (case-neutral homograph tagging) - COMPLETE, eight rounds done |
| D-369 | Chip-Leerzeichen wächst nicht | RESOLVED (§374, v1.1.13) |
| D-370 | Quote nach Satzzeichen | RESOLVED for the actually-wanted core (§359, v1.0.111); the "more ambitious" half explicitly |
| D-371 | Vertrauen: Feinschliff | RESOLVED (§351, v1.0.103) |
| D-372 | Diagonaler Nachbar zählt | RESOLVED (2026-09-01, no code change - already implemented) |
| D-373 | Bindestrichkette: zweite Ausnahme | RESOLVED (§356, v1.0.108) |
| D-374 | Von D-416 gelöst | RESOLVED by D-416 (§333) |
| D-375 | sollendafur-Silent-Fix | RESOLVED (2026-08-31, no dedicated fix identified) |
| D-376 | S-12: Tempo-Chip (km/h) | RESOLVED (§366, v1.1.5) |
| D-377 | A-13: Missed-Backspace-Rettung | RESOLVED (§367, v1.1.6) |
| D-378 | Einmal-Guard (Familie) | RESOLVED (§357, v1.0.109) |
| D-379 | bzgl.-Abkürzung | RESOLVED (§330, v1.0.82) |
| D-380 | Smear über Tastenrand | RESOLVED and device-confirmed (§403, v1.1.42) |
| D-381 | POS-Tag editierbar | RESOLVED, device-confirmed (2026-08-31, no code change) |
| D-382 | 2-Taste: Apostroph/Tiefzahl | RESOLVED (§330, v1.0.82) |
| D-383 | Keep-Listenmodus: Caret+Enter | RESOLVED (§389, v1.1.28) |
| D-384 | Abgelehnt (D-384) | WON'T FIX (2026-09-01, no code change - discussed and declined) |
| D-385 | Import: atomares Schreiben | RESOLVED (2026-08-31 decision + §458/v1.2.18 implementation) |
| D-386 | Import-Cleanup 60 s | SUPERSEDED BY D-413 (2026-08-31, no version bump - a decision, not new code) |
| ↳ D-386-followup | Ist die geladene Datei die richtige? | folgt der Basis-Nummer |
| D-387 | Umlaut-Entfaltung andere Sprachen | RESOLVED (§401 architecture + §402 data mechanism, v1.1.40 + v1.1.41) |
| D-388 | Editor-Sortierwahlschalter | RESOLVED (§291 v1.0.45) |
| D-389 | Verlern-Ablauf (C-24) | RESOLVED (§344 v1.0.96 + §345 v1.0.97 + §347 v1.0.99 + §348 v1.0.100 + §349 v1.0.101) |
| ↳ D-389-followup | C-24-Nachzüge (Nie/Dauern/Familien) | folgt der Basis-Nummer |
| D-390 | Versal-Toleranz Abkürzungen | WON'T FIX (2026-09-01, no code change - discussed and dropped) |
| D-391 | Kreuz-Wort-Fusion | RESOLVED (§454, v1.2.14) |
| D-392 | Versal-Fix (dritter) | RESOLVED (§353, v1.0.105) |
| D-393 | Play-Store: Enter ohne Submit | RESOLVED (§390, v1.1.29) |
| D-394 | Rechner-Ziffern wie Telefon | RESOLVED, digit-mirror only (§330, v1.0.82) |
| D-395 | Gestenleisten-Abstand einstellbar | WON'T FIX (2026-09-03, no code change - user's own root-cause correction) |
| D-396 | Haptik folgt OS-Regler | RESOLVED and device-confirmed (§405-§409, v1.1.44-v1.1.48) |
| D-397 | Reihen-Drift-Kappe | RESOLVED (§453, v1.2.13) |
| D-398 | C-23: Sprachwechsel-Schwelle | RESOLVED (§330, v1.0.82) |
| D-399 | C-03: Minimum 3 | RESOLVED (§330, v1.0.82) |
| D-400 | Layout an Systemsprache gekoppelt | RESOLVED (§331, v1.0.83) |
| D-401 | Cursor-Control-Reihe | RESOLVED, implemented (§460, v1.2.20) |
| D-402 | Wörterbuch-Cleanup-Paket | /D-345/D-330-followup/D-367 - ALL COMPLETE (§301 v1.0.54 + §302 v1.0.55 + §303 |
| D-403 | Akronym-Schild (learnedCasingOf) | RESOLVED (§280 v1.0.37, §289 v1.0.44, §325 v1.0.78) |
| D-404 | Gelernt-Flut: Drei-Tier-Plan | is now fully closed, all three tiers (§388 closed tier 2, the last one open) |
| ↳ D-404-followup | Akronym-Schutz | folgt der Basis-Nummer |
| D-405 | Satzanfang nur live, nie am Commit | Auto-Caps Is A Position Property, Not An Event - Closing D-405's Predicted Live-Arming Gaps (D-406) |
| D-406 | Auto-Caps: Positions-Eigenschaft | Auto-Caps Is A Position Property, Not An Event - Closing D-405's Predicted Live-Arming Gaps (D-406) |
| D-407 | Vertrauen: vereinheitlicht | Autocorrect Confidence — A Unified, Graduated Measure Replacing Ad Hoc Gates (D-353/D-354/D-371/D-407) |
| D-408 | Slider-Clipping + Split-Slider | /D-408/D-409 Device-Confirmed (No Code Change) |
| D-409 | Split-Slider-Ordnung | /D-408/D-409 Device-Confirmed (No Code Change) |
| D-410 | LanguageRules-Schnitt | / D-410: German compound-forming-particle veto against a false split (A-05) |
| D-411 | Gelernt-Boost (Log + Recency) | Implemented: A Log-Scaled, Recency-Aware Boost For Learned Words' Live Suggestion Ranking |
| D-412 | Lemma-Spalte + Verb-Sweep | RESOLVED, fully closed (§305 schema, §306-§315 verb-tagging sweep, §320-§322 lemma |
| D-413 | Download-Mechanik (neu) | (download directory control, spec §30) - RESOLVED, mechanism changed by D-413 (see below) |
| D-414 | Manueller Reclaim-Knopf | RESOLVED (§337, v1.0.89) for the Reclaim half; the Cycle half explicitly shelved, not merely |
| ↳ D-414-followup | Reclaim-Chip in der Leiste | folgt der Basis-Nummer |
| D-415 | Fokus-Aufgeben (abgelehnt) | WON'T FIX (device-confirmed, 2026-09-01) |
| D-416 | Auto-Space verzögert | RESOLVED (§333, see History.md for the full account) |
| ↳ D-416-followup | Indikator-Löschen + Materialisierungs-Lücke | folgt der Basis-Nummer |
| D-417 | 2er-Popup-Reihenfolge | reorder the `2` key's popup - apostrophe ahead of the superscript |
| D-418 | Visueller Hinweis (offen) | OPEN, low priority, not started (2026-09-01) |
| D-419 | Listen zeigen aktuellen Wert | RESOLVED (§346 v1.0.98 + §349 v1.0.101) |
| ↳ D-419-followup | C-24-Speicherung entkoppelt | folgt der Basis-Nummer |
| D-420 | Split am Satzanfang mit Versal | an A-05 split at a sentence start now keeps its own capital |

## D-421–D-480

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-421 | Reclaim-Chip: Keep-Bugs |  |
| ↳ D-421-followup | Credential-Chip-Regression | folgt der Basis-Nummer |
| D-422 | Englisch-Wortfamilien | English "Wortfamilien" parity project - real POS tags, Wiktionary-sourced inflection forms, lemma-linking, for … |
| D-423 | daß/Strasse entfernt | "daß" and the Swiss ss-spelling "Strasse" removed outright from `dict_de.tsv`, never blacklisted |
| D-424 | Griechisch-Wortfamilien | Greek "Wortfamilien" parity project - real POS tags, Wiktionary-sourced inflection forms, lemma-linking, for … |
| D-425 | Griechisch: leere POS-Zeilen | the 5,359 empty-POS `dict_el.tsv` rows flagged in §370 fixed, on explicit user request the same day |
| D-426 | Griechisch-Extraktion: Markup-Bug | a real bug in §370/D-424's own Greek extraction, found via a follow-up "check for markup/noise like the German … |
| D-427 | Englisch: LaTeX-Rauschen | `en/dict.tsv` LaTeX-markup noise cleanup, the same D-402-style follow-up that also surfaced §372/D-426's Greek bug |
| D-428 | V-04-Blitz nach Clear | the V-04 clipboard-peek button flashed back visible for one render right after its own V-03 clear button emptied the … |
| D-429 | last_touched + Recency | learned bigrams and trigrams now carry their own `last_touched` timestamp, and the recency boost D-411 already gives … |
| D-430 | Save/Forget-Tausch | the Learned Words editor's per-entry dialog swaps its Save/Forget button roles |
| D-431 | A-13-Gate (hasObviousCandidate) | closed - real device log confirmed the gate fix works, found a genuinely structural (non-bug) limitation, and caught … |
| ↳ D-431-followup | welxmche-Diagnose + strukturelle Grenze | folgt der Basis-Nummer |
| D-432 | Gate auf Rohkoordinaten-Tipp | applied D-431's same `hasObviousCandidate()` gate to `rawCoordinateSuggestion` (T-02/D-39's own live preview chip), … |
| D-433 | Mini-KI-Erklärtext | (v5) - the last remaining gap: "Mehr erfahren" still sat flush left, not indented with the title/summary column above … |
| ↳ D-433-followup | Mini-KI-Reihe (fünf Nachzüge) | folgt der Basis-Nummer |
| D-434 | Abkürzungsliste sprachspezifisch | the known-abbreviation list itself is the active language's own - not, as before this fix, always |
| D-435 | Umlaut-Mechanik-Bug (§401) | enuine bug it was resting on first (D-435, §401): the umlaut/diacritic mechanism |
| D-436 | Varianten-Stapel (Base zu Varianten) | ack (base letter -> known variants, D-436), loaded exactly like `hints.tsv`/ |
| D-437 | Smear ins Zwischenfeld (mit D-380) | pressed key's own bounds; D-380/D-437: a smear that *does* leave the key's own bounds (e.g. into the inter |
| D-438 | Bigram-Sichtbarkeit | recording a learned bigram (D-327) is not by itself enough to ever *see* it suggested - the candidate |
| D-439 | Casus-Zeilen nicht migriert | ) stay. Existing rows deliberately NOT migrated (display-only difference; W-01 fixes on demand). |
| D-440 | Beide Casings in der Leiste | RESOLVED and device-confirmed (2026-09-04, §411 display fix v1.1.50 + §412 commit fix |
| D-441 | Französisch-Pack | RESOLVED (§413 + §415, v1.1.54): D-314's AZERTY geometry now has a real `dictionaries/fr/` |
| D-442 | Konfusables-Scan Englisch | 's own two-item backlog (frequency-correction + noise audit for `en/dict.tsv`, both named-but-not-started in … |
| ↳ D-442-followup | Konfusable-Nebenwirkungen (en) | folgt der Basis-Nummer |
| D-443 | Spanisch-Pack | RESOLVED (§416, v1.1.55): first Spanish `dictionaries/es/` pack, built fully autonomously and |
| D-444 | Spanisch-POS-Quelle | (§417, v1.1.56): the POS-tagging source above (kaikki.org's Spanish |
| D-445 | Portugiesisch-Pack | a conservative, rule-based Portuguese noun/adjective plural generator, plus a preposition-tagging audit across every … |
| ↳ D-445-followup | pt-Pluralgenerator | folgt der Basis-Nummer |
| D-446 | Italienisch-Pack | first Italian language pack, second of the three-language (pt/it/nl) overnight one-shot round - full … |
| D-447 | Niederländisch-Pack | first Dutch language pack, third and final of the pt/it/nl overnight one-shot round - a real, serious calibration bug … |
| D-448 | Polnisch-Pack | first Polish language pack, first of a second two-language (pl/tr) autonomous round, requested only after checking … |
| D-449 | Türkisch-Pack | Turkish's dotted/dotless İ/I capitalisation, closing the "still-open design question" named at the end of D-449 … |
| ↳ D-449-followup | Türkisch İ/I | folgt der Basis-Nummer |
| D-450 | 18-Sprachen-Runde | Cyrillic-vs-Cyrillic auto-detection, closing the Open TODO §441 first flagged |
| ↳ D-450-followup | Sprachprofile + Kyrillisch-Erkennung | folgt der Basis-Nummer |
| D-451 | AltGr-Popup-Haptik | RESOLVED (§448, v1.2.8) |
| D-452 | Gemini-Dauer-Performance-Problem | RESOLVED, device-confirmed by the user on 2026-09-08 (§477, v1.2.37) - including the long-token |
| ↳ D-452-followup | Performance-Problem: Nachzüge | folgt der Basis-Nummer |
| D-453 | Doppel-Konsonant-Entfaltung | RESOLVED (§448, v1.2.8) |
| D-454 | Sprachpaket-Bildschirm umgebaut | RESOLVED (§447, v1.2.7) |
| D-455 | Reclaim-Sperre (Gemini-Felder) | RESOLVED, device-confirmed (§450, v1.2.10) |
| D-456 | Verlernen deckt Fall ab | CLOSED, device-confirmed working (2026-09-08, no code change) |
| D-457 | Akronym: Kanon-Restore | RESOLVED, device-confirmed (§450 + §452 followup, v1.2.10/v1.2.12) |
| D-458 | Ab Wort 2 ohne Chips | RESOLVED, device-confirmed (§451, v1.2.11) |
| D-459 | Backspace-Davor-Löschung (beobachtet) | WON'T FIX for now, deliberately deferred (2026-09-07, no code change) |
| D-460 | Chip-Verschwinden (Race, erledigt) | CLOSED, device-confirmed (2026-09-08); no dedicated fix was ever written, so the mechanism |
| D-461 | Auto-Caps nur eindeutig | RESOLVED, device-confirmed (§478, v1.2.38; confirmed 2026-09-09) |
| D-462 | Lemma-Reparatur (konservativ) | RESOLVED for the conservative scope (§483, v1.2.43), device-confirmed (2026-09-24); the wider |
| D-463 | Gerätebestätigter Punkt (D-463) | device-confirmed (2026-09-09) |
| D-464 | quality_gate: Nomen-Marker | RESOLVED (§479, v1.2.39) |
| D-465 | Präfix-Suche indiziert | Prefix Lookup Is Index-Backed (D-465) |
| D-466 | zum-gluck-Bogus-Zeile | "zum gluck" autocorrected to "Gluck" instead of "Glück"; root-caused to a single bogus dictionary row, fixed by … |
| D-467 | Lemma-Fehler Deutsch | RESOLVED, device-confirmed (§482, v1.2.42; confirmed 2026-09-17) |
| D-468 | Lemma-Reparatur: 31 Pakete | RESOLVED, device-confirmed (§484, v1.2.44; confirmed 2026-09-17) |
| D-469 | Wörterbuch-Bug (Paar mit D-470) | device-confirmed (2026-09-12) |
| D-470 | Zweitbug (Paar mit D-469) | /D-470 - two independently-reported bugs closed in one round. D-469 (dictionary) |
| D-471 | Analysiert und abgelehnt | WON'T FIX, analysed and explicitly declined (2026-09-12), not guessed at |
| D-472 | Emoji-Return in Extra-Reihe | the emoji panel's own "return to keyboard" tab moved into the extra row, replacing the emoji button's own slot with … |
| D-473 | Acht-Symptome-Runde (dich/dir) | CLOSED IN FULL, all eight reported symptoms device-confirmed fixed (2026-09-17) |
| ↳ D-473-followup | fragt/fragte + aber-Mysterium | folgt der Basis-Nummer |
| D-474 | Touch-Zonen einfrieren | Touch-Zone Learning Can Be Frozen (D-474) |
| D-475 | Sauberer Lern-Fix (offen) | OPEN, not started (2026-09-20): the "clean fix" for the touch-zone learning signal - needs its |
| D-476 | Drum-zu-drum-Tag | FIXED and device-confirmed (§500, v1.2.60; confirmed 2026-09-24): `Drum` was tagged bare `NOUN` in |
| D-477 | Fusions-Metrik: Wortmodell | FIXED and device-tested by the user (§498 v1.2.58 + §499 v1.2.59; confirmed 2026-09-20): the A-06/D-391 |
| D-478 | UI-Übersetzungen (32 Sprachen) | RESOLVED (§501-§503, 2026-09-20; the recommendation below on (1) was overruled by the user) |
| D-479 | Spurious-Space-Position nutzen (Idee) | OPEN, idea only, deliberately not started (2026-09-20): use the spurious space's own tap position |
| D-480 | wer-Frequenzkorrektur | `wer` was silently autocorrected to `der`; its German frequency raised (German data pack only) |

## D-481–D-540

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-481 | F-Droid-reproduzierbares APK | release APK made acceptable to F-Droid's reproducible-build pipeline: no Google "Dependency metadata" signing block, … |
| D-482 | R8 aktiviert | R8 enabled for the release build at F-Droid maintainer linsui's request (MR 44142, "Enable r8") |
| D-483 | Launcher-Label | RESOLVED (§507, v1.2.67; reported 2026-09-28, device-confirmed 2026-09-28): the launcher label "AdaptKey Settings" is … |
| D-484 | Zurück-Pfeil (Einstellungen) | RESOLVED (§507, v1.2.67; reported 2026-09-28, device-confirmed 2026-09-28): Settings sub-screens have no visible Back … |
| D-485 | Kalibrierung: OK-Knopf | RESOLVED (§507, v1.2.67; reported 2026-09-28, device-confirmed 2026-09-28): the calibration screen has no OK/Done … |
| D-486 | Raum unter der Tastatur | RESOLVED and fully closed (§508 - §510, v1.2.70): the keyboard sat a gesture-bar-height too high on a Chinese phone … |
| ↳ D-486-followup | Klappmenü-Verwerfung | folgt der Basis-Nummer |
| D-487 | Dropdown-Prototyp verworfen | WITHDRAWN and closed (2026-09-28, §510; user confirmed the c06 look as final): the inline `DropDownPreference` … |
| D-488 | Blacklist-Bestätigungs-Chip | blacklist confirmation chip + per-entry provenance + the no-mark rule for bundled-casing words; the reported "normal … |
| D-489 | Doppel-Shift-Brecher | RESOLVED (§512, v1.2.72): an intervening keystroke on another key (letter/space/delimiter/backspace/ |
| D-490 | Doppel-Backspace-Default | RESOLVED (§536, v1.2.96), device-confirmed 2026-10-31 ("sieht gut aus"): the D-348 double-tap-Backspace revert is the … |
| D-491 | Shift-Arm schützt manuellen Press | RESOLVED (§513, v1.2.73), device-confirmed 2026-10-26: a manual Shift press (arm or deliberate |
| D-492 | Paste-Form (Span-Liveness) | OPEN, reported 2026-10-23, root-caused by code reading (2026-10-23, refined same day with the |
| D-493 | schmeißen-Meldung | RESOLVED (§529, v1.2.92), closed 2026-10-31 without its own code round: the reported "schmeißen" -> "sch Meißen" … |
| D-494 | Fremdwörter nicht lernen | RESOLVED, device-confirmed (§513, v1.2.73; confirmed 2026-10-26): foreign-dictionary words (English |
| D-495 | Wisch = eine Bewegung | individual taps can never compose into a swipe any more; a swipe is now, by construction, one continuous … |
| D-496 | Review-Korrekturen (fünf) | review follow-ups to §513: five corrections to the D-489/D-491/D-494 code, found by a six-track review of the … |
| D-497 | Varianten im Pack-Hint | RESOLVED (§518, v1.2.77) |
| ↳ D-497-followup | Varianten-Rollout (15 Pakete) | folgt der Basis-Nummer |
| D-498 | Backslash neben Slash | RESOLVED (§517, v1.2.76) |
| D-499 | Slash ins Komma-Popup | RESOLVED (§516, v1.2.75) |
| D-500 | C-28 Auto-Caps-Schalter | RESOLVED (§537, v1.2.97), device-confirmed 2026-10-31 ("funktioniert wunderbar"): the C-28 "Auto-capitalisation" … |
| D-501 | Dark Theme folgt System | RESOLVED as simplified (§527, v1.2.90; closed 2026-10-28 - on-device confirmation deliberately waived by the user): a … |
| D-502 | Sprachwechsel sichtbar | RESOLVED, device-confirmed (§521+§522, v1.2.81/v1.2.82; confirmed 2026-10-26): the G-01 language switch is now … |
| D-503 | n-gram-Pinning | RESOLVED, device-confirmed (§524, v1.2.84; confirmed 2026-10-27): after a function word there is no |
| ↳ D-503-followup | Funktionswort: kein Pinning | folgt der Basis-Nummer |
| D-504 | Auto-Split entschärft | RESOLVED (§529, v1.2.92), device-confirmed 2026-10-29: auto-split tuning - the A-05 split no longer scales with the … |
| D-505 | Import-Streaming | RESOLVED, end-to-end verified (§528, v1.2.91; implemented 2026-10-28, NOT yet device-confirmed): the whole … |
| D-506 | Basis-Paket-Hinweis | IMPLEMENTED (2026-10-29, same session as D-504's data half; rides the same 1.2.92/588 release; a static text, so … |
| D-507 | Combined-Slot dreistufig | RESOLVED (§531-§532, v1.2.93), device-confirmed 2026-10-29: the bottom-row combined key's slot becomes one … |
| D-508 | Gesten-Kalibrierung langsam | RESOLVED (§534, v1.2.94), device-confirmed 2026-10-31: the gesture/long-press calibration for slow users - the … |
| D-509 | Alt-Belegung: Ecken-Modell | the Alt-Belegungs-Reform - RESOLVED, device-confirmed (§538; v1.2.98-v1.2.104, versionCode 594-600; closed … |
| D-510 | Alt-Belegung: Popup-Geometrie | (v1.2.99, placement corrected v1.2.103/v1.2.104) - the popup geometry |
| D-511 | Alt-Belegung: Zahlenreihe | (v1.2.100) - the number row |
| D-512 | Alt-Belegung: Buchstabenseite | (v1.2.101) - the letters page |
| D-513 | Alt-Belegung: Hinweis-Daten + Merge | (v1.2.102) - the hints data |
| D-514 | Alt-Belegung: Doku-Phase | (rides v1.2.102, docs-only, no bump) |
| D-515 | Apostroph Opt-in | IMPLEMENTED (§539, v1.2.105), NOT yet device-confirmed: the apostrophe key's opt-in for the non-English layouts … |
| D-516 | Glide-Typing (Kern) | G-09 glide-typing ("Gesteeneingabe"/Glide-Typing) - R3 (D-518, §542) OPEN as the community-feedback round since … |
| D-517 | Glide R2 (erste volle Version) | , R2: glide typing's complete first release - switch, stroke classification, commit path, live preview, pinned chip, … |
| D-518 | Glide R3 (Community-Runde) | , R3 opened: the user's own verdict hands glide typing to the community; version scheme switches to 1.3.3+; the … |
| D-519 | Tilde vorne (Seite 2) | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-520 | Währung auf d (verworfen) | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-521 | Und auf k | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-522 | Caret-Platz (Zirkumflex) | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-523 | Währung auf $-Platz | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-524 | Apostroph-Ecke: Doppel-Quote | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-525 | Weite Popups: Interleave (kurz) | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-526 | Popup-Anordnung bestätigt | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-527 | Währungstaste als Symbol | RESOLVED, device-confirmed (§543+§544+§545, v1.3.6; closed 2026-11-12: "Perfekt, so können wir das Thema abhaken"): … |
| D-528 | Geretteter Parked-Press | the rescue (gated parked press) + full drop diagnostics |
| D-529 | Doppelte Pushes übersprungen | identical suggestion-bar pushes skipped |
| D-530 | Geteilte Lookback-Lesung | one shared lookback read for the time/speed-unit triggers |
| D-531 | !?#-Kaskade (Seitennavigation) | /D-533/D-534 - IMPLEMENTED (§547+§548, v1.3.8/608 + v1.3.9/609) - D-531 DEVICE-CONFIRMED (2026-11-13: "die neuen … |
| D-532 | Glide: Icon + Extra-Reihen-Knopf | (v1.3.9) |
| D-533 | Glide: Commit am Finger-Lift | (v1.3.9) |
| D-534 | Glide: Feld-Debug-Logging | (v1.3.9) |
| D-535 | Multi-Touch-Trennung | IMPLEMENTED (§549, v1.3.10/610) - D-535 IS THE PROVEN MULTI-TOUCH FIX (code-proven, device-side overlap repro … |
| D-536 | Glide-Commit aufs Wortende | the glide commit model, corrected as the user specified |
| D-537 | UPs bergen + loggen | IMPLEMENTED (§550, v1.3.11/611) - SAFETY NET + OBSERVABILITY (inert under the healthy delivery the D-540 fix … |
| D-538 | Ungepuffertes Dispatch | IMPLEMENTED (§551, v1.3.12/612) - SAFETY NET, inert under healthy delivery: unbuffered touch dispatch + recovery … |
| D-539 | Degraded-Space-Sicherung | IMPLEMENTED (§552, v1.3.13/613) - SAFETY NET, inert under healthy delivery (the D-540/D-543 round confirmed healthy … |
| D-540 | Wurzel: beginPress-Return | IMPLEMENTED (§553, v1.3.14/614), DEVICE-CONFIRMED (2026-11-13: "Es geht endlich wieder" - every touch delivered, … |

## D-541–D-600

| Nr. | Kurzbezeichner | Thema (Originaltitel, gekürzt) |
|-----|----------------|--------------------------------|
| D-541 | Glide-Knopf gehärtet + Haptik | IMPLEMENTED (§554, v1.3.15/615) - DEVICE-CONFIRMED via the extended test log (2026-11-13): the extraRowToggle line … |
| D-542 | Glide: persönliche Geometrie | the R3 calibration finding from the first real glide log - the claim's passing radius was the gate, and the geometry … |
| D-543 | Einstellungen: Live-Sync | IMPLEMENTED (§556, v1.3.18/618) - DEVICE-CONFIRMED (2026-11-13: "das können wir jetzt so abschließen. Das hat … |
| D-544 | Apostroph = Wortzeichen | IMPLEMENTED (§557, v1.3.19/619) - DEVICE-CONFIRMED (2026-11-18: "Gerätebestätigt. Das hat geklappt."): the dedicated … |
| D-545 | jetzt-Frequenz + Provider-Nähe | IMPLEMENTED (§558, v1.3.20/620) - NOT yet device-confirmed: the German "jetzt" frequency recalibrated for the … |
| D-546 | jetzt-Chips: eigener Ranked-Slot | IMPLEMENTED (§559, v1.3.21/621) - NOT yet device-confirmed: the S-11 dual-casing chips take their candidate's own … |
| D-547 | Gelerntes mit gebündelter Lesart | IMPLEMENTED (§560, v1.3.22/622) - NOT yet device-confirmed: a freshly learned entry for an already-bundled word is … |
| D-548 | Gepinnt am Leading-Slot | IMPLEMENTED (§561, v1.3.23/623) - DEVICE-CONFIRMED (2026-11-19: "Ole! Das ist behoben. Jetzt ist es wie gewünscht." - … |
| D-554 | Wisch-Hoch: Zahlenreihe + Extra-Reihe | IMPLEMENTED (§562, v1.3.24/624) - NOT yet device-confirmed: the C-09-off letters surface gets its digits back by … |
| D-555 | Bottom-Row-Emoji-Taste | IMPLEMENTED (§563, v1.3.25/625) - DEVICE-CONFIRMED (2026-11-19: "Perfekt. So läuft das. Verhält sich genau richtig." … |
| D-556 | Re-Sort-Delay 0 (Key-Rename) | IMPLEMENTED (§564, v1.3.26/626) - DEVICE-CONFIRMED (2026-11-19: "Ja, so passt das."): the C-02 re-sort delay defaults … |
| D-557 | A-03: Chips bleiben | IMPLEMENTED (§565, v1.3.27/627) - DEVICE-CONFIRMED (2026-11-21): an A-03 foreign verdict no longer empties the suggestion bar … |
| D-558 | Konkurrenz-Kleinbuchstaben-Gate | IMPLEMENTED (§566, v1.3.28/628) - NOT yet device-confirmed: §6 rule 3 gains a competing- lowercase gate - "dur" no … |
| D-559 | Feature-Tipp: Auslöser | Trigger definition - what counts as auto-correction | ← Plan `feature-tips.md` |
| D-560 | Feature-Tipp: Mechanismus/Katalog | Generic FeatureTips mechanism + catalog | ← Plan `feature-tips.md` |
| D-561 | Feature-Tipp: Speicherung | FeatureTipStore persistence | ← Plan `feature-tips.md` |
| D-562 | Feature-Tipp: Leisten-Einblendung | Bar integration - injection into showSuggestions() | ← Plan `feature-tips.md` |
| D-563 | Feature-Tipp: Lebensdauer | Tip lifetime (auto-hide after two commits) | ← Plan `feature-tips.md` |
| D-564 | Feature-Tipp: Chip-Design | Chip design (violet, sparkle prefix) | ← Plan `feature-tips.md` |
| D-565 | Feature-Tipp: Reset-Knopf | Reset button on the feature-overview page | ← Plan `feature-tips.md` |
| D-566 | Feature-Tipp: Texte (32 Sprachen) | Tip strings (32 locales) | ← Plan `feature-tips.md` |
| D-567 | EN-Veto-Blocker-Datenfix | the English dictionary's two D-106 veto blockers removed - en/dict.tsv loses `mut`/`Dur`/`muts`, de `Dur` 501 -> 100 (pack 53) | ← Plan `english-dictionary-quality.md` |
| D-568 | EN-Funktionswort-Flexions-Sweep | EN function-word flexion artefacts swept - `ands` (292,861, rank 6) and 52 siblings out, top-25 clean | ← Plan `english-dictionary-quality.md` |
| D-569 | DE↔EN-Überschneidungs-Sweep | EN dictionary loses 346 German-corpus rows (incl. 34 mojibake); D-106 veto victim census 380 -> reviewed 57-word whitelist | ← Plan `english-dictionary-quality.md` |
| D-570 | EN-Namen-Sweep | EN proper-name sweep - 26,897 rows out (115,846 -> 88,958), persons incl. John 10,247, occupation twins lowercase in | ← Plan `english-dictionary-quality.md` |

---


**Nächste freie Nummer: D-571.** (Höchste vergebene: D-570.)
