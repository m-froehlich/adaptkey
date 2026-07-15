# AdaptKey - Requirements Specification

**Platform:** Android (Kotlin, InputMethodService)  
**Base:** AOSP LatinIME  
**Status:** Concept phase  
**Date:** June 26, 2026

---

## 1. Motivation

Every available Android keyboard implicitly assumes a particular input gesture - thumb typing, two-handed use, or a right-handed index finger. No existing product learns neutrally how a given user actually types. Workarounds such as shifted mini-layouts are not an acceptable solution.

Beyond this, off-the-shelf keyboards fail systematically at context-sensitive capitalisation, hyphenated compound words, punctuation as a prediction signal, and stability of the suggestion bar.

### Guiding Principle - Umlauts Are Ordinary Characters (the spirit of the app)

`ä ö ü ß` (and, by extension, the accented characters of every supported language) must be treated as
completely **normal characters** everywhere in the app. They must never come as a surprise to any feature,
never throw the autocorrect off its stride, and never produce an effect that a German user would find
unexpected. For a German, an umlaut is not exotic - a keyboard that stumbles over `ä`/`ö`/`ü`/`ß` (fails to
correct `konnen` → `können`, splits a word rather than restoring its umlaut, ranks a diacritic-less form
oddly, etc.) is simply broken. This principle overrides convenience shortcuts and will shape several
features (correction ranking, folding, split-vs-correct priority, suggestions).

---

## 2. Layout & Key Assignment

### L-01 - Base Layout: Gboard QWERTZ
The QWERTZ layout of Gboard is adopted as-is - key sizes, spacing, and access to special characters - except where this specification explicitly diverges.

### L-02 - Narrower Space Bar, Wider Comma & Full Stop
The space bar is made narrower than the Gboard default. The comma and full-stop keys are widened accordingly. Goal: accidental taps on the space bar instead of the comma are structurally reduced. Exact proportions are user-configurable.

### L-03 - Combined Emoji / Numeric-Layer Key
A single key in the bottom row hosts both the emoji panel and the switch to the numeric/symbol layer (`?123`). A tap opens the emoji panel - the more frequent action for the target user; a long-press or upward swipe switches to the numeric/symbol layer. The emoji panel delivers Unicode codepoints directly to the target app; no dependency on app-side support is required. Because the most common symbols are reachable directly - digits and their shifted symbols on the persistent number row (L-06), comma and full stop as dedicated keys (L-02), `?` via long-press on the full stop, and `@`, `€`, `#` and similar via L-05 - the `?123` layer is needed only rarely. Consolidating both functions onto one key frees a slot in the bottom row, which is reinvested in adjacent key proportions (L-02).

### L-04 - Enlarged Backspace
The backspace key is widened relative to the Gboard default (recommended ~ 10 %) to reduce mis-hits. The additional width is taken from its neighbours within the third row and is independent of the bottom-row layout. The exact proportion is user-configurable (C-01).

### L-05 - Secondary Symbols via Long-Press (AltGr-Style)
Frequently used symbols are placed as secondary characters on letter keys, reachable by long-press and shown as a small hint glyph in the corner of the key - analogous to the AltGr labels on a hardware QWERTZ keyboard. This uses the native per-key `moreKeys` / hint-label mechanism and requires no separate layer. The default mapping places `@` on `Q`, `€` on `E`, `#` on `H`, `-` on `M`, `+` on `N`, and `°` on `D` (`@` and `€` matching the hardware AltGr convention; `-` and `+` forming an adjacent pair). Symbols already carried by the number row (L-06) - such as `/` and `&` - are not duplicated here. The per-key symbol set and whether the hint glyphs are displayed are user-configurable (C-08). Together with L-03 and L-06 this removes the need to switch to `?123` for most everyday input.

### L-06 - Persistent Number Row
A dedicated number row (`1`-`0`) is shown at the top of the alpha view by default and can be toggled off in Settings (C-09). Each digit carries its German-QWERTZ shifted symbol as a long-press secondary with a corner hint, matching hardware muscle memory: `1`->`!`, `2`->`"`, `3`->`§`, `4`->`$`, `5`->`%`, `6`->`&`, `7`->`/`, `8`->`(`, `9`->`)`, `0`->`=`. Because these symbols are then directly reachable on the alpha view, the `?123` layer (L-03) is rarely needed.

---

## 3. Touch Capture & Learning Model

### T-01 - ACTION_DOWN as the Canonical Tap Point
The initial contact point (`MotionEvent.ACTION_DOWN`) is used as the authoritative tap coordinate. Subsequent movement up to `ACTION_UP` does not influence key resolution and does not trigger implicit swipe behaviour.

### T-02 - Retroactive Character Correction
The most recently entered character is held in composing state until sufficient context is available (e.g. the start of the next word). If context subsequently points to a more probable neighbouring character, the entry is corrected via `setComposingText` / `commitText`. The raw touch coordinate is retained until the final decision is made.

### T-03 - Personal Offset Model (Typing-Style Agnostic)
All tap events are persisted as tuples `(touch_x, touch_y, confirmed_key)`. A 2D Gaussian offset kernel is computed per key to compensate for each user's individual systematic deviation. The model makes no assumptions about handedness or finger - it learns exclusively from observed deviations. It is updated continuously and improves with every confirmed word.

### T-04 - Typing Pattern Detection
The dominant typing pattern is derived automatically from the accumulated offset model:

- **Left index finger:** Systematic offset to the right, more pronounced for keys on the right half of the keyboard; narrow contact area.
- **Right index finger:** Systematic offset to the left, more pronounced for keys on the left half of the keyboard; narrow contact area.
- **Thumb:** Low lateral offset; larger contact area (touch radius from `MotionEvent.getSize()`); hits tend to be vertically centred.

The detected pattern is shown to the user as information only - not as a constraint - and can be overridden manually. Pattern detection has no effect on functionality; it serves transparency and optional upfront configuration.

### T-05 - Space/Letter Confusion in the Bottom Row
The boundary between the space bar and the bottom letter row (`c v b n m`) is treated as a high-risk zone for swapped space/letter input - a risk aggravated by the narrower space bar (L-02). Two ambiguous bands are defined:

- **Space-ambiguous tap:** A `MotionEvent.ACTION_DOWN` in the lower edge band of `c`, `v`, `b`, `n`, or `m`. The committed letter may in fact be an intended space.
- **Letter-ambiguous tap:** An `ACTION_DOWN` in the upper edge band of the space bar. The committed space may in fact be an intended letter; the nearest letter key is inferred from the tap's x-coordinate via the offset model (T-03).

Taps in either band are flagged accordingly and their raw coordinates are retained (T-02), so that a later token-level decision can retroactively replace a letter with a space (A-05) or a space with a letter (A-06). The band widths scale with the configured key proportions (C-01) and are refined by the personal offset model.

### K-01 - Initial Calibration Mode *(skippable)*
An optional calibration step is offered on first launch. The user types 2-3 provided sentences exactly as they would normally - with autocorrect disabled, no suggestion pressure, and no time limit. The raw touch coordinates are recorded and form the initial offset model (T-03).

On completion, the app analyses the pattern and gives brief feedback, for example:  
*"It looks like you type with your left index finger. I'll adjust accordingly."*  
or: *"It looks like you type with your thumb. I'll tune my recognition to match."*

The calibration mode can be repeated at any time from Settings. Without calibration the model still converges during normal use - just somewhat more slowly, as early inputs are partially obscured by autocorrect artefacts.

---

## 4. Gestures

### G-01 - Language Switch
Swiping left or right on the space bar switches the active input language.

### G-02 - Delete Whole Word
Swiping left on the backspace key deletes the entire previous word rather than a single character.

### G-03 - Dismiss Keyboard
Swiping downward on the keyboard dismisses it.

### G-04 - Permanently Remove a Suggestion (Drag-to-Trash)
A suggestion in the suggestion bar can be dragged upward into a trash area that appears during the drag; releasing it there permanently adds the word to the blacklist (A-04). A simple swipe is not sufficient - the deliberate drag-and-drop action prevents accidental deletion.

### G-05 - Retroactive Capitalisation at Word End
Pressing Shift at the end of a fully typed word toggles the capitalisation of its first character - in both directions: "Upper" becomes "upper", "lower" becomes "Lower". The outcome depends on the next key pressed:

- **Next key is a space, punctuation, or line break:** The first character is toggled as described; the rest of the word is unchanged.
- **Next key is a letter:** The toggle of the first character is discarded; instead, the new letter is inserted as an uppercase character within the word (camelCase / PascalCase).

---

## 5. Suggestion Bar

### S-01 - Horizontally Scrollable, Configurable Maximum
The bar shows as many suggestions as are meaningful and scrolls horizontally. Default: 8 entries. Maximum is user-configurable (recommended range: 6-10). Sorted by descending probability; the most probable suggestion appears at the far left.

### S-02 - No Duplicate of the Current Input
The word exactly as typed does not appear as a suggestion. That slot belongs to genuine alternatives. Exception: when an autocorrection is pending, the verbatim input is surfaced as defined in S-06. This is the only case in which the typed word appears in the bar.

### S-03 - Position Stabilisation
The visible top-N suggestions may not change position while the user is actively typing. New suggestions are appended at the right end of the scrollable list. Re-sorting only occurs after a configurable pause with no input.

### S-04 - Re-sort Delay *(configurable)*
Slider from 0 to 600 ms. A value of 0 means immediate re-sorting (today's behaviour). Default: 300 ms.

### S-05 - Word Confirmation Highlight *(configurable)*
The word currently being typed is highlighted as soon as the system recognises it as correct and complete (no correction planned). Implemented via a coloured span through `setComposingText`. The highlight clears on the next delimiter (space, punctuation, hyphen). Feature is toggleable on/off; colour is user-selectable.

### S-06 - Verbatim "Keep As Typed" Affordance
Whenever autocorrect intends to replace the current token on the next delimiter, the literal typed string is surfaced as a dedicated, visually distinct chip (e.g. in quotation marks), pinned at the far left of the suggestion bar and exempt from position stabilisation (S-03). The pending replacement is shown alongside it, so the user sees both the proposed correction and how to decline it. Tapping the verbatim chip commits the literal input and cancels the pending autocorrection for this occurrence. This is the only case in which S-02 is overridden. Repeated verbatim confirmation of the same string feeds a learning signal and may add it to the personal dictionary (cf. B-03), after which it is no longer autocorrected.

---

## 6. Capitalisation

### Autocorrect Hierarchy

1. **Explicit user input always wins.** An uppercase letter is never corrected to lowercase. A lowercase letter is only corrected when rules 2-4 apply unambiguously.
2. **Sentence start:** The first word after `.`, `!`, or `?` followed by a space is capitalised.
3. **Pure nouns:** A word that exists exclusively as a noun (no verb, adjective, or preposition form) is capitalised automatically.
4. **Known proper nouns:** Are capitalised.
5. **Ambiguous words** (existing as both a noun and another part of speech): No automatic correction. Capitalisation is offered as a suggestion in the bar. Example: "gegenüber" (opposite/facing) - its prepositional use is far more frequent than its nominal use, so no intervention occurs.
6. **LLM exception:** If the sentence context is unambiguously nominal and LLM confidence is high, the LLM may capitalise - never as a default, only at high certainty.

Part-of-speech annotations and frequency distributions are loaded from freely available German lexicon dumps (DWDS / Wiktionary) at install time.

### Addendum to G-05 - Shift State After Backspace
When an uppercase character is deleted, Shift remains active - the next keystroke will produce an uppercase character. The information is contained in the deleted character itself; no separate action stack is required. When a lowercase character is deleted, Shift behaves as usual (context-driven by the autocorrect hierarchy).

### Editor-Mandated Capitalisation
When the target field declares `TYPE_TEXT_FLAG_CAP_SENTENCES`, `TYPE_TEXT_FLAG_CAP_WORDS`, or `TYPE_TEXT_FLAG_CAP_CHARACTERS` (read from `EditorInfo` and queried per position via `InputConnection.getCursorCapsMode()`), the case the field requires is treated as an authoritative baseline, ranking directly below explicit user input (hierarchy rule 1). The linguistic rules (sentence start, nouns, proper nouns, ambiguous words) never lowercase a position the field requires to be capitalised.

### Delayed Shift Against Surprising Field Capitalisation *(configurable)*
With `TYPE_TEXT_FLAG_CAP_WORDS` or `TYPE_TEXT_FLAG_CAP_CHARACTERS`, every word start is auto-armed to uppercase - including mid-sentence words, which is surprising relative to normal typing. A user who reflexively presses Shift to capitalise the next word would instead toggle the already-armed uppercase back to lowercase. To prevent this, a Shift press that would switch from a field-mandated uppercase to lowercase is ignored during a short grace window after the word start. Once the window elapses, Shift toggles normally, so a deliberate lowercase override remains possible. The guard applies only to field-mandated capitalisation outside a regular sentence start; ordinary sentence-start capitalisation is unaffected. The window length is configurable via a slider from 0 to 500 ms (C-07); a value of 0 disables the guard entirely.

### No Sentence Start After a Comma-Terminated Line *(configurable, default on)*
In multi-line text (typically e-mails), a line whose last non-whitespace character is a comma does **not** end a sentence: the next line that contains text - blank lines in between are skipped - is **not** treated as a sentence start and is therefore **not** auto-capitalised. This covers the standard German salutation, where the greeting line ends in a comma and the body continues in lower case:

```
Hallo Max Mustermann,

danke für deine schnelle Antwort.
```

Here "danke" must stay lower case. No semantic recognition of the salutation is required; the purely structural rule *"a content line immediately following a comma-terminated line is not a sentence start"* is sufficient, and this pattern does not occur in any other everyday context. The remaining linguistic rules still apply (nouns and proper nouns are still capitalised; explicit user input still wins). This behaviour is bound to a boolean setting (C-10) so it can be disabled, but the **default is on**.

For this rule to be meaningful, the baseline is that the **first word of a new line is treated as a sentence start** (extending hierarchy rule 2 beyond `.`/`!`/`?`): a deliberate line break normally begins a new sentence and is capitalised, as on common mobile keyboards. The comma-terminated-line rule above is the one exception.

### No Sentence Start After Known Abbreviations and Enumerators
A period does **not** start a new sentence when it terminates a known abbreviation or an enumerator, so the following word is **not** auto-capitalised (unless another rule - a noun, a proper noun, or explicit user input - independently applies).

- **Known abbreviations:** abbreviations whose canonical form always ends in a period, either already present in the dictionary (e.g. `usw.`, `z. B.`, `d. h.`, `ca.`, `Nr.`) or learned from the user's own input. Such an entry is flagged as *abbreviation* so its trailing period is understood as part of the token rather than a sentence terminator.
- **Enumerators:** a run of digits followed by a period at a list position (e.g. `1.`, `2.`, `10.`) is an ordinal / list marker, not a sentence end.

This complements the sentence-start rule in the capitalisation hierarchy (rule 2): the period after `usw.` or after `1.` is recognised as non-terminal, so the next word keeps its natural case.

---

## 7. Autocorrect Policy

### A-01 - Valid Words Are Not Overwritten
If the typed word exists in the dictionary, no automatic substitution occurs - even if a similar word is more frequent. "hatten" (had) is not corrected to "hätten" (would have); the alternative merely appears as a suggestion in the bar.

### A-02 - Punctuation Is Not a Context Reset
A comma does not clear the prediction context. N-gram patterns such as `", dass"` or `", die"` are trained and suggested as independent entries. The mini-LLM sees the full sentence and is not subject to this limitation in any case.

### A-03 - Language Detection from Context
An embedded fastText model (~1 MB, on-device) identifies the current writing language from the last 3-5 words. Suggestions and autocorrect are filtered per language. Examples: "sie" is not corrected to "Sue", "so" is not corrected to "do", when the context is German.

### A-04 - Blacklist for Unwanted Words
Words can be permanently excluded from suggestions and autocorrect. The blacklist is persisted in SQLite and survives app updates. Blanket exclusions (e.g. all words from old German spelling conventions) are supported via a category flag.

### A-05 - Retroactive Word Split on Missed Space
When the current token is not a valid word but can be split into two dictionary or high-probability bigram words at the position of a space-ambiguous tap (T-05), the offending character is replaced with a space and the token is committed as two words. Spatial proximity alone is never sufficient - a valid linguistic split is required. The same mechanism also covers a fully missed space with no spurious character (e.g. `aberdas` -> `aber das`).

### A-06 - Retroactive Word Merge on Spurious Space
The inverse of A-05. When a space was registered from a letter-ambiguous tap (T-05) and the following token is not a valid word, the system tests whether removing that space and prepending the letter inferred from the tap's x-coordinate yields a valid or high-probability word. If so, the spurious space is removed and the reconstructed word is committed (e.g. `aber  ald` -> `aber bald`, where the intended `b` landed on the space bar). As with A-05, a valid linguistic result is mandatory; the spatial signal only nominates the candidate.

### A-07 - Post-Commit Autocorrect Undo
After an autocorrection has been committed by a delimiter - a space, a punctuation mark, or a hyphen (the latter as a segment separator per B-01) - a single backspace **tap** issued immediately afterwards restores the originally typed word instead of deleting a character. This applies only to a plain tap directly after the commit; it does not affect the whole-word delete gesture (G-02, swipe-left on backspace) or the shift-state behaviour on character deletion (Addendum to G-05). Any subsequent input then proceeds normally. This complements the pre-commit verbatim affordance (S-06) as a post-hoc escape hatch.

---

## 8. Hyphen Handling

### B-01 - Hyphen as a Segment Separator
A hyphen splits a compound word into independent tokens for dictionary lookup, the learning model, and the suggestion bar. "XYZ-Anteil" trains "Anteil", not "XYZ-Anteil" as a unit.

### B-02 - No Automatic Capitalisation After a Hyphen
The segment following a hyphen is lowercase by default. Exception: the segment is a known proper noun.

### B-03 - Optional Learning of Full Compounds
If the user confirms the same hyphenated combination multiple times, it may optionally be added to the personal dictionary as a whole - not automatically, only on the basis of repeated confirmation.

---

## 9. Word Prediction & Adaptive Learning

### Three-Tier Architecture

| Tier | Technology | Latency | Activation |
|---|---|---|---|
| 1 - N-gram | Personal dictionary (SQLite) | <5 ms | Always active |
| 2 - Language detection | fastText (~1 MB, NPU) | <10 ms | After punctuation, on suspected language switch |
| 3 - Mini-LLM | ONNX Runtime (e.g. Gemma Nano) | variable | When N-gram confidence falls below threshold |

### Adaptive Learning
LLM results feed back into the N-gram model as learning signals. The better the N-gram becomes through LLM-assisted learning, the less frequently the LLM is invoked - an emergent behaviour, not an explicit schedule. Battery load therefore decreases automatically as usage grows.

---

## 10. Configurable Parameters

| ID | Parameter | Type | Default |
|---|---|---|---|
| C-01 | Key proportions (space bar / comma / full stop / backspace) | Slider | Gboard-like |
| C-02 | Suggestion re-sort delay | 0-600 ms | 300 ms |
| C-03 | Maximum number of suggestions | Integer (6-10) | 8 |
| C-04 | Word confirmation highlight | On/Off + colour | Off |
| C-05 | Word blacklist | List + categories | empty |
| C-06 | LLM activation threshold | N-gram confidence value | medium |
| C-07 | Shift grace window vs. surprising field capitalisation | 0-500 ms | 300 ms |
| C-08 | Secondary long-press symbols & corner hint labels | Per-key map + on/off | letters `@`/`Q`, `€`/`E`, `#`/`H`, `-`/`M`, `+`/`N`, `°`/`D` |
| C-09 | Persistent number row (with shifted-symbol long-press) | On/Off | On |
| C-10 | No sentence start after a comma-terminated line (§6, e-mail salutation) | On/Off | On |

---

## 11. Estimated Effort

| Component | Sessions |
|---|---|
| Android IME scaffold (Kotlin, InputMethodService) | 1-2 |
| Gboard layout + adjusted key proportions | 1 |
| Touch capture (ACTION_DOWN, offset model) | 1 |
| Scrollable suggestion bar + position stabilisation | 1 |
| Dictionary infrastructure (SQLite, blacklist, part-of-speech annotation) | 1-2 |
| Autocorrect policy (all rules in this specification) | 1-2 |
| Hyphen tokenisation, capitalisation logic | 1 |
| Emoji panel | 1 |
| Settings screen | 1 |
| fastText language detection | 1 |
| **Total without LLM** | **~10-12** |
| Mini-LLM integration (ONNX Runtime, tier-3 architecture) | +2-3 |

One session corresponds to approximately 1 hour of focused work. The LLM integration is designed as a separate follow-on project and can be added later without changes to the core architecture.

**Prerequisite:** Android Studio with a configured Android SDK.

---

## 12. Device-Feedback Additions (from real-device testing, v0.7.x)

Requirements added after testing on hardware. IDs continue the existing scheme.

### D-01 - Multi-Alternative Long-Press Popup
A key with **more than one** secondary alternative shows, on long-press, a small popup (context menu) of all
alternatives, Gboard-style. The most common alternative is pre-selected; sliding the finger changes the
selection and releasing commits the highlighted one. A key with **exactly one** alternative shows **no** popup
and applies that alternative immediately on long-press (current behaviour, e.g. the umlauts on a/o/u, ß on s).

### D-02 - Full-Stop Long-Press Punctuation List
Long-press on the full-stop key presents (via D-01) the punctuation set in this order: `.` `!` `?` `,` `;` `:`
`-` `_` `/`. The full stop is pre-selected.

### D-03 - Space Bar Shows the Language
The space bar is **not** labelled "Space". It shows the currently selected input language (e.g. "Deutsch",
"English", "Ελληνικά"), which also doubles as a language indicator.

### D-04 - Space Bar Tap Feedback
The space bar briefly flashes/highlights on each key press, like the other keys, so a press is visibly
acknowledged.

### D-05 - Optional Key Sound *(configurable, default off)*
An optional click sound on key presses, off by default, toggled in the settings.

### D-06 - Optional Haptic Feedback *(configurable, default off)*
An optional short vibration on key presses, off by default, toggled in the settings.

### D-07 - Accelerating Backspace Repeat on Hold
Holding backspace deletes, after a short initial delay, character by character; the rate **starts slow and
accelerates** so the user is not surprised into deleting too much. After roughly three words deleted it switches
to **word-wise** deletion until backspace is released.

### D-08 - Backspace Shift-Restore Refinement (extends the G-05 addendum)
When an uppercase character is deleted and then the space immediately to its left is deleted, that space
deletion counts as deleting a **lowercase** character and shifts back to lowercase (the uppercase context ended
at the word boundary).

### D-09 - Raw-Tap Recording (diagnostic, opt-in)
The raw tap coordinates during a calibration/onboarding session can be recorded and exported, so mis-resolution
of touches (e.g. an uncalibrated finger) can be analysed offline.

### D-10 - Backspace at Start of a Line/Entry
A backspace at the very start of an entry should, where the editor allows, delete backwards across the boundary
(joining with the previous entry / line) as common keyboards do, rather than being a no-op.

---

## 13. Second Device-Feedback Round (from v0.7.7 testing)

A further round of on-device testing. Overall the tap precision is now *substantially* better and close to
usable. The following refinements to existing requirements and new requirements (IDs continue the D-series)
came out of it.

### Refinements to existing requirements

- **K-01 / edge-to-edge (blocker):** the **calibration screen** does not push its embedded keyboard up by the
  gesture-bar / navigation inset the way the IME view does (§ D device round 1), so the bottom row sits under
  the gesture pill and calibration could not be completed. The calibration keyboard must apply the same
  bottom inset padding as the live keyboard.
- **D-04 (flash too slow):** the key-press flash lingers far too long - a double-tap of the same key shows no
  visible second flash. The flash must be markedly shorter so rapid repeated taps each register visibly.
- **D-07 (too slow / wrong stop):** (a) backspace-on-hold deletes too slowly - the repeat must accelerate to a
  clearly faster rate; (b) word-wise deletion **stops before the last word of the line** instead of deleting
  it - the word-boundary handling at the start of a line must remove that final word too.
- **A-07 (undo misses splits):** a backspace immediately after an A-05 retroactive split does **not** undo the
  split (rejoin the two words). The post-commit undo must also revert a split, not only a plain autocorrect.
- **C-04 (defaults):** the recognised-word highlight must default **on** (many users watch for it), and the
  default green is too dark - use a lighter shade with better contrast.

### D-11 - Earlier and More Frequent Suggestions
Word suggestions currently appear too late and too rarely. Sensible completions must be offered already after
the **first or second letter** and consistently thereafter - many users type only until the intended word is
offered and then pick it, which must work reliably here.

### D-12 - Fuzzy, Umlaut-Aware Correction Candidates
Suggestions and autocorrect must include close single-edit and umlaut variants: `mut` must offer `mit`, `grun`
must offer `grün`, and a clearly mistyped `Defaukt` must be corrected/offered as `Default` rather than being
accepted. Relatedly, the word-recognition highlight (S-05) must **not** treat nonsense like `Defaukt` as a
known word.

### D-13 - User Word Training / Add-to-Dictionary
There must be a way for the user to teach the keyboard a word (e.g. `Backspace`), so it is offered, kept, and
in particular **not** retroactively split (A-05) into `Back Space`. This includes a discoverable action to add
the current/typed word to the personal dictionary.

### D-14 - In-Keyboard Long-Press Feedback
A long-press must produce visible feedback **on the keyboard itself** (a key flash/flicker, or - Gboard-style -
the popup briefly rising), plus optionally the D-05/D-06 sound/haptic, so the user sees that the long-press
registered even when looking at the keys rather than the text. This applies to single-alternative keys too,
which currently show nothing on the keyboard.

### D-15 - Double-Tap Shift = Caps Lock
Pressing Shift twice in quick succession engages Caps Lock (persistent uppercase) until Shift is pressed again.

### D-16 - Typing-Pattern-Driven Default Key Enlargement
The enlarged-backspace idea (L-04) should be mirrored for the Shift key. After calibration detects the typing
hand (T-04), the default enlargement is pre-set accordingly: a **left**-index typist gets the enlarged
**backspace** (right side), a **right**-index typist gets an enlarged **Shift** (left side). Both remain
user-adjustable afterwards.

### D-17 - Expanded Onboarding USP Text
The onboarding core-feature list is too terse. It should be expanded with the strongest USPs from this spec
(provably offline / no INTERNET permission, adaptive per-finger touch model, retroactive split/merge, the
on-device mini-LLM, real multilingual dictionaries, etc.) so a new user immediately understands what AdaptKey is.

### D-18 - Emoji Panel Toggle *(configurable, default on)*
The emoji panel is of limited use (messengers bring their own). A setting must allow disabling it - default on
so it is discoverable, but switchable off. When off, the combined key (L-03) is purely a `?123` numeric/symbol
key with no emoji panel.

### D-19 - Full-Field Swipe to Switch Pages
A left/right swipe **anywhere on the key field** switches between the surfaces/pages (letters ↔ symbols ↔
numbers). This must be cleanly separated from the space-bar left/right language swipe (G-01). The `?123` key is
kept for accessibility even if this makes it partly redundant.

### D-20 - Larger Gesture Thresholds
Swipe-down to dismiss (G-03) and the full-field left/right page swipe (D-19) currently trigger on too small a
motion - a faint downward swipe already hides the keyboard. They must require a clearly larger travel to
activate while staying practical. The space-bar language swipe (G-01) must stay proportional to the small space
bar and remain easy.

### D-21 - Key Cell Padding
Add a few pixels of cell padding (spacing) between keys, Gboard-style, to visually separate them.

---

## 14. Third Device-Feedback Round (from v0.7.8 testing)

More on-device findings. Several refine features from §13; the rest are new requirements (IDs continue the
D-series). Items marked *(bug)* are regressions/defects, not new scope.

### Refinements to existing requirements

- **D-04 - flash still too slow *(bug)*:** even after shortening, the key-press flash feels sluggish. It must
  be *much* shorter, matching Gboard's very brief key flash, so typing feels snappy.
- **D-05/D-06 - sound & haptic do not fire *(bug)*:** with both toggles enabled on device, no click sound is
  heard and no vibration is felt. The feedback path is not actually working and must be fixed (audio focus /
  `playSoundEffect` routing; haptic flags / an explicit `Vibrator` fallback).
- **C-04 / S-05 - colour the text, not the background (D-25):** a recognised word should be shown by changing
  its **font colour**, not a background highlight - it looks better. (Supersedes the background-span approach.)
- **T-04 / K-01 - calibration produced no result:** a full calibration run detected **UNKNOWN** (no pattern).
  The calibration may be too short and the classifier too conservative. Calibration must reliably produce a
  concrete, visible result and state it plainly ("I detected that you type with your left index finger").
  Investigate sample count / thresholds / sentence length.

### D-22 - Punctuation Reorganisation (period vs comma)
Split the current full-stop long-press set into two keys by meaning. The **full-stop key** carries the
sentence terminators only: primary `.` with alternatives `!` `?`. The **comma key** carries the rest as its
long-press set: primary `,` with alternatives `;` `:` `-` `_` `/`.

### D-23 - Vertical Long-Press Popup Layout
The long-press popup is laid out **vertically** above the finger, not horizontally. The primary character is
pre-selected but sits **top-left** of the key (offset, not directly above the finger). The remaining
alternatives are stacked **vertically above the finger**, ordered by priority bottom-to-top. So on the
full-stop key the cell directly above the finger is `!`, with `?` above it; on the comma key the comma is the
pre-selected top-left default and the alternatives run bottom-to-top `;` `:` `-` `_` `/`.

### D-24 - Touch-Pattern Visualisation *(new feature)*
Visualise the learned touch model: show the keyboard with a coloured circle over each key at the **expected
strike point** (the T-03 per-key mean offset, radius ~ its variance). Offer it as the **result screen of a
calibration** and as a toggle in the settings, so the user can see how their personal offsets look.

### I18N - Multilingual App Texts *(new feature)*
The app's own UI strings (settings, onboarding, dialogs, calibration) must be localised. At minimum **English**
and **Greek** in addition to German, selected by the **system language**. (The keyboard's *typing* languages
DE/EN/EL already exist; this is about the app-chrome strings via `res/values-en`, `values-el`.)

### D-26 - Mid-Word Correction Colours the Inserted Characters *(bug)*
When correcting inside an existing word, the two freshly typed correction characters can get coloured
(highlighted). The recognised-word colouring must not apply to a mid-word edit like this.

### D-27 - Space Bar Top-Edge Hit Target *(bug, high priority)*
Tapping the **top edge of the space bar** registers the key **above** it (c / v …), producing many wrong
letters instead of spaces. The space bar must reliably win at its own top edge. (This is the inverse of the
T-05 space/letter band and must not be swamped by it - a genuine space tap must stay a space.)

### D-28 - Proximity- and Distance-2-Aware Correction
Correction must consider **physical key adjacency** and a larger edit budget. `komplezz` must suggest
`komplett` - obvious to a human, and especially because `z` neighbours `t`. A single edit is not enough here
(two characters differ); the matcher needs edit-distance-2 and/or an adjacency-weighted cost so a typo on a
neighbouring key is cheap. (Open question the user raised: how much of this improves only once the optional
tier-3 LLM model is imported - to be answered.)

### D-29 - Punctuation After an Accepted Suggestion Eats the Trailing Space
Accepting (tapping) a suggestion inserts a trailing space, which is correct. But if the **very next** input is
a punctuation mark, that trailing space must be removed so the punctuation attaches to the word. This must
apply **only** immediately after a "suggestion accepted" action - spaces before a typed punctuation mark must
**not** be stripped in general.

---

## 15. Fourth Device-Feedback Round (from v0.7.16 testing)

Further on-device findings. `*(bug)*` marks defects.

### D-30 - Long-Press Backspace Freezes Input *(bug, critical)*
After using long-press (held) backspace and stopping, **no further input is possible** - not even a normal
backspace, so the line's last word can no longer be deleted either (this was the real cause behind the
earlier "stops before the last word" report). The keyboard must remain fully responsive after a held
backspace (the repeat-suppression state must be reset on the next touch).

### D-31 - Backspace-Hold Speed Re-tuning
Held backspace starts at a reasonable rate but then **accelerates far too much**, and it keeps that fast rate
when it switches to word-wise deletion - so half the text is gone in an instant. It must accelerate only
**very moderately** after the first few characters, and on the transition to **word-wise** deletion it must
slow down **clearly**, so the user can follow the deletion and stop at the right moment.

### D-32 - Long-Press Delay Too Long *(+ setting)*
The long-press (to reach the alt char / popup) takes too long. Shorten it by roughly **20 %**, and add a
**setting** for the long-press delay.

### D-33 - Popup Primary Cell Bottom-Aligned
In the D-23 popup, the offset primary cell (the key's own char) should be **bottom-aligned** - level with the
bottom of the secondary column, near the finger - rather than top-left. It works the same but feels more
intuitive.

### D-34 - Key Vibration Does Not Fire; Permission Half-Automatic *(bug)*
The key-press vibration (D-06) still does not fire. VIBRATE is a normal, install-time permission (no runtime
prompt), so enabling the setting must simply work - if any acquisition is needed it must happen
half-automatically when the toggle is enabled, never as a manual step. The vibration pulse must also be long
enough to actually be felt.

### D-35 - Swipe Thresholds Still Too Sensitive
Swipe-down (dismiss, G-03) and left/right (space-bar language G-01, full-field page D-19) still trigger on a
**tiny** motion - a faint down-swipe hides the keyboard, a faint right-swipe on space switches language, a
faint right-swipe elsewhere switches page. All must require a **clearly larger** (but still practical) travel.

### D-36 - Direct Paste from the Clipboard *(new feature)*
Like Gboard: when the clipboard holds content, show (part of) it where the suggestion bar is; a tap pastes it.
Passwords appear as `•••••`. Two improvements over Gboard: (1) **clear the clipboard after pasting**,
especially for passwords; (2) perform the **exact system paste action** (Gboard pastes in a way that behaves
subtly wrong).

### D-37 - Less Eager Learning *(with un-learn on undo)*
A word must **not** be fully learned after a single autocorrect/accept (that also learns wrongly-corrected
words). Instead: (1) a corrected word that is **reverted** (backspace-undo) must be **un-learned /
decremented**; (2) an accepted/autocorrected word is only **counted up**, and counts as learned once it has
been accepted **two or three times without being reverted**. It must stay comfortable and learn quickly, but
not over-eagerly. (Refines D-13; design the count/threshold sensibly.)

### D-38 - Correction Quality: First-Char, Umlaut-Initial, Cost-Ranked
Real misses to fix, beyond D-28: first-character typos and umlaut-initial words are never corrected because
the candidate set is keyed on the exact first character - broaden it (first-char neighbours + umlaut-folded
first char). And ranking must prefer a **lower-edit-cost** correction over a more frequent higher-cost one.
Examples (recognised input -> intended): `Stabdsrx` -> `Standard`, `Uberblick` -> `Überblick`,
`eerden` -> `werden`, `W8rt` -> `Wort`, and `dasy` -> `dass` (currently wrongly `das`).

### D-39 - Raw-Coordinate Per-Character Correction
Correction should not work from the committed characters alone but from the **retained raw tap coordinates**
per character (T-02 / T-05). For an unknown word, walk it character by character and, using each tap's raw
position and the offset model (T-03), consider the geometrically nearest **neighbouring** key that was
likely intended, generating candidate spellings to validate against the dictionary. Most single-key slips
are one position off, so this recovers many multi-typo words. Heavily garbled cases (e.g. `Stabdsrx` ->
`Standard`, where much went wrong at once) may instead be left to the tier-3 LLM, which tends to recognise
the intended word from context.

### D-40 - A Digit Inside a Word Is a Likely Typo
A digit typed **between letters** (mid-word, no separating space) is almost certainly an unwanted key -
digits are rarely intended mid-word. Such a digit must be kept in the composing token (not treated as a
delimiter) and corrected to a neighbouring letter like any other typo, so `W8rt` -> `Wort`. A digit at the
start of a token (or a standalone digit) keeps its normal behaviour.

---

## 16. Fifth Device-Feedback Round (from v0.7.20 testing)

More findings. `*(bug)*` marks defects. The overarching theme: digits and umlauts must be treated as
perfectly ordinary characters/keys, never as exotic special cases.

### D-41 - Digits Are Ordinary Neighbour Characters in Correction
A stray digit inside a word is nothing more than a mis-hit neighbouring key and must be corrected exactly
like a wrong neighbouring letter - not as a word separator that derails the suggestions into random
uppercase acronyms. Include the number row in the keyboard-adjacency map so a digit is a **cheap**
neighbour substitution (`8`↔`o` etc.), and make sure a mid-word digit no longer produces nonsense live
suggestions. (This should ideally fall out of D-39's raw-coordinate approach; the special-casing in D-40 was
the wrong direction.)

### D-42 - Number-Row Hit Zone Too Tall *(bug)*
The number-row keys react too far down - their touch zone feels like it reaches into the letter row below,
so a tap meant for the top letter row lands on the digit above it. Tighten the number-row hit target.

### D-43 - Next-Word Prediction in the Suggestion Bar
The suggestion bar should predict and offer the **next** word before it is typed - this is what makes a
keyboard feel alive. A bigram baseline (the stored n-gram continuations) works offline today; the tier-3 LLM
elevates it to real context-aware prediction.

### D-44 - Alt-Key Popups: Horizontal, Centred, Finger-Below Selection
Return the long-press popup to a **horizontal** layout, but better than the first version:
- It is centred **above the stem key**, and grows to the right (where there is room), extending at most one
  cell to the **left** of the stem.
- The **primary** character sits directly above the stem key and is pre-selected. For the full stop the row
  is `?` `.` `!` (full stop centred, `?` upper-left, `!` upper-right). For the comma the row is
  `-` `,` `:` `;` `/` `_` (comma centred over the stem, `-` the one cell to its left, the rest to the right).
- Selection follows the finger **swiping underneath** the popup (along the key row), not resting on the
  cells - so the finger never hides the choice; the highlight makes the selection clear.

### D-45 - Restore Auto-Capitalisation After Deleting a Punctuation at Line Start *(bug)*
At the start of a line the keyboard auto-shifts to uppercase. If the user types a punctuation mark there and
then deletes it, the auto-uppercase is **not** restored - which is intuitively wrong. This needs an explicit
exception to the general shift rules: deleting back to a sentence/line start re-arms the auto-capital.

### D-46 - Swipe Length Still Too Short
Even after D-35, a swipe still triggers on too little travel - a slight smear while pressing Shift already
switches the page. The surface/language swipe should require at least roughly **three key-widths** of travel.
To be tuned on device.

### D-47 - Combined Key Must Not Show the Emoji Glyph When the Panel Is Off *(bug)*
With the emoji panel disabled (D-18) the combined key correctly acts as `?123` only, but it still **displays
the 😊 emoji**. Its label must reflect the state - show `?123` (or similar), not the emoji.

### D-48 - Umlaut Words Are First-Class; a Fold Match Beats a Split
`konnen` must autocorrect to `können` - an obvious, missing-umlaut case (the umlaut fold makes it the exact
word). Instead the A-05 split wrongly wins and produces `ko nen`. A strong umlaut-fold / low-cost correction
must take priority over a retroactive split. Umlauts must be treated as entirely normal characters, never as
exotic - for a German user anything else is incomprehensible. Reorder the correction pipeline accordingly.

### D-49 - Raw Touch-Recording as a Headline Onboarding USP
The personal raw touch-recording / per-finger learning (T-03) - and, specifically, that it lets AdaptKey
recover a word from where a tap actually landed even when the wrong key was picked outright (D-39), not just
a neighbouring letter - is one of the app's absolute core features and USPs and must appear prominently on the
onboarding page, phrased so an ordinary user understands it (not in technical terms). The onboarding text must
also make clear that the continuous learning (T-03/T-04, D-37) is deliberately gentle/damped: it improves from
a steady pattern over time, but a single slip never noticeably shifts anything.

### D-50 - Suggestion Bar Stays Permanently Visible
The suggestion bar must keep its row at all times, even when it is momentarily empty, so the prompt slot
above it does not jump up and down as suggestions come and go. (With next-word prediction (D-43) it will
rarely be empty anyway.)

## §17 - Device-Feedback Round 6 (v0.7.22 / v0.7.23 testing)

The horizontal alt-key popups (D-44) are now excellent and highly usable. This round refines the keyboard
geometry, doubles down on the umlaut-first-class principle, and adds several bugs found on device.

### D-51 - Period Long-Press Order Is `! . ?`
The full-stop key's horizontal alt popup should read `! . ?` (exclamation, full stop, question mark), left to
right, with the full stop still the centred, pre-selected default. (Supersedes the D-44 `? . !` order.)

### D-52 - The Number Row Is a Full-Height Key Row Again
D-42 made the number row shorter, which only means the digits must be hit more precisely - the opposite of
helpful, and there was no real benefit. Revert it: the number row is exactly as tall as the letter rows.

### D-53 - The Number Row Long-Presses Like Every Other Key
The number row is not an add-on or a foreign body; it is an ordinary part of the whole keyboard and must
behave like one. In particular each digit must produce a long-press alt popup exactly as the letters do,
offering its shifted symbol (the QWERTZ `! " § $ % & / ( ) =` etc. already shown as the corner hint). Right
now the digits show no popup at all. Treat the row as first-class throughout, not as something bolted on.

### D-54 - Single-Alternative Popups Are Nudged Toward the Keyboard Centre
A one-cell alt popup should sit slightly offset from directly-above-the-key so the finger does not hide it:
about two units to the right for keys on the left half of the keyboard, and two units to the left for keys on
the right half. (Multi-cell popups keep the D-44 centred-over-stem behaviour.)

### D-55 - Two New Spacing Settings: Below the Number Row and Above the Space Row
Add two independent settings that insert extra vertical space (a) directly below the number row and (b)
directly above the space row. Default 7 (units/dp), slider range 0-25. This is expected to remove a great
many mis-taps and, above the space row, many accidental Enter presses.

### D-56 - Backspace Hold: Single-Character Phase Slightly Faster
The accelerating backspace hold (D-31) is almost perfect. Only the single-character deletion phase may be a
touch faster; the word-wise phase stays as it is.

### D-57 - Page-Swipe Width Retune (Space Bar Gets More)
The full-field page swipe (D-46) is now a bit too wide. Reduce the required travel by ~15% - except on the
space bar, where it may be ~15% more (its horizontal swipe is still too easy to trigger by accident). The
swipe-down-to-hide gesture is perfect as it is and must not change.

### D-58 - Page-Change Animation
Give the surface/page change a slide animation, like the swipe-down animation. It looks much better and, more
importantly, it makes the page change perceptible - right now the change is easy to miss.

### D-59 - Optional `?123` Key (Default On)
Add a setting to disable the combined `?123` key. It mostly gets in the way - it is easy to hit by accident
when reaching for Shift (the new D-55 spacing above the row will already help). Default on. When this setting
is **off AND** the emoji panel is also off, the button disappears entirely, but its slot stays reserved
(the surrounding keys do not grow into the gap).

### D-60 - Quick Paste Does Nothing (Bug)
The direct-paste chip (D-36) currently has no effect. Likely the clipboard is cleared too early (before the
paste action actually consumes it). Fix the ordering so the paste happens, then the clipboard is cleared.

### D-61 - Enter Does Not Submit in Browser Address Bars / Search Fields (Bug)
Pressing Enter in a browser address bar or a search field (e.g. YouTube) inserts a newline instead of
submitting. The Enter key must honour the editor's requested IME action (Go/Search/Send/Done) instead of
always emitting a newline.

### D-62 - Mid-Word Editing Produces Live Suggestions
When the caret is placed inside an existing word and the user types, the autocorrect / suggestions must behave
as if the whole word were being composed right now - the full extent of the word (and, where useful, the
surrounding context for the LLM) is taken into account, not just the fragment after the caret.

### D-63 - Missing Umlauts / ß: Suggest-Up-Front, Auto-Correct When Unambiguous
Every word typed without its umlaut or ß (`konnen`→`können`, `mussen`→`müssen`, `hoflich`→`höflich`,
`weis`→`weiß`) must (1) be recognised with its diacritics and offered near the front of the suggestion bar,
and (2) be auto-corrected when it is unambiguous - i.e. when no sensible non-diacritic word exists. So
`mussen` auto-corrects to `müssen`. But `konnten` must only *suggest* `könnten`, because `konnten` is itself a
valid word. Being able to skip the umlauts while typing and have the app restore them is a core comfort
feature and must be supported as strongly as possible. (Extends D-48 from "beats a split" to full
suggest+auto-correct behaviour.)

### D-64 - Suggestion-Bar Word Cannot Be Dragged to Trash (Bug)
Dragging a word out of the suggestion bar (to the trash / blacklist, G-04 / D-36-era gesture) does not work
any more. Restore the drag-to-trash from the suggestion bar.

### D-65 - `konnen` Auto-Corrects to `kannen` (Bug, HIGH)
`konnen` is currently auto-corrected to `kannen`, which is not a word; it must become `können`. Possibly a
ricochet from the D-41 digit-adjacency change or the D-48 pipeline reorder. This is the exact umlaut-first-
class case from D-63 and must be fixed together with it.

### D-66 - Key Vibration Does Not Fire At All (Bug)
The key-press vibration (D-06 / D-34) definitely does not fire on device, despite the earlier fixes. Low
priority for now, but the haptic path needs to be re-investigated (permission, VibratorManager vs Vibrator,
amplitude, or the toggle wiring).

## §18 - Device-Feedback Round 7 (v0.7.27 testing)

### D-67 - Word Splitting Fires Too Eagerly; Must Require High Confidence (Bug)
`kleiben` (a typo of `kleinen`) was split into `klei` + `en` instead of being auto-corrected to `kleinen`.
Neither `klei` nor `en` are words an ordinary user would recognise as legitimate splits, yet the split still
won. Root cause: the A-05 "drop a character" split path ([TokenRepair.trySplit], the branch keyed off
[TokenRepair.OVER_SPACE_LETTERS]) accepts a split purely because both halves are *technically* known
dictionary words - it has no bigram/frequency confidence gate at all (unlike the "fully missed space" branch,
which does require a minimum bigram count). A split must only win when its confidence is clearly higher than
the best available whole-word correction - in particular, a split must never beat a low-cost (single adjacent-
key edit or better) autocorrect candidate for the un-split token. `kleiben` → `kleinen` is exactly such a case
(`b` and `n` are adjacent keys). Generalise the D-48 "a fold match beats a split" veto: before accepting any
split, check whether the token already has a high-confidence single-word correction (low edit cost, e.g. a
proximity-aware cost of 0-1, and/or reasonable frequency) and prefer that over splitting.

### D-68 - Rethink Calibration: Ask the Typing Pattern Directly, Refine Only Slowly Over Time (Big Idea, Deferred)
The current three-sentence calibration (T-03/T-04) misclassified the user's typing pattern outright ("right
index finger" for someone who is not), which visibly skews the touch zones (e.g. `j`'s effective zone drifting
half onto `i` above it) and is a plausible cause of many reported mis-taps. Proposal to explore later: replace
automatic calibration with an explicit, simple onboarding question - "how do you type?" (two-thumb, left-index,
right-index, etc.) - and derive sensible **initial** touch zones directly from the chosen pattern, without
relying on a noisy few-sentence sample:
- For a one-handed/one-finger pattern (e.g. left index finger), keys in that hand's home third of the keyboard
  get a tight, centred zone; zones widen and smear away from the centre (toward the far side of the keyboard)
  the further a key is from that third, reflecting the increasing awkwardness of the reach. Keys at the far
  edge from the typing hand may even have their effective zone extend noticeably into the visually adjacent
  key's territory.
- The mirrored shape applies to the opposite-hand / opposite-finger patterns, and a thumb pattern gets its own
  (narrower, more central) shape.
- The "two thumbs" pattern is the exception: zones stay fairly centred/tight on every key, since a two-thumb
  typist reaches across the whole width symmetrically.
- The keyboard may still *refine* these initial zones from real usage over time (as today), but this drift
  must be made **much more sluggish** than currently - a single mistyped tap must never noticeably shift a
  zone; only a sustained, consistent pattern over many taps should move it, and only gradually.

This is a substantial redesign of the T-03/T-04 onboarding + calibration flow and is explicitly **not** meant
to be implemented immediately - captured here as the guiding idea; the concrete approach (exact shape
functions, migration/reset story for existing calibrations, whether calibration disappears entirely or
becomes an optional refinement step) needs its own design pass later.

## §19 - Device-Feedback Round 8 (v0.7.32 testing, D-68 follow-up)

### D-69 - Word Split Wins Merely By Being Found First, Not By Being the Best Match (Bug)
`immernoch` (the user's own deliberate, correct compound spelling of "immer noch") auto-corrects to
`immer och` instead of splitting as `immer noch`. Root cause: [TokenRepair.trySplit] tried the "drop a
character" strategy first and returned its result unconditionally whenever it found *any* valid candidate,
without ever trying (let alone comparing against) the "fully missed space" strategy - `n` sits over the
space bar, so `immer` + `och` is a technically valid drop candidate that already satisfies the drop path
and short-circuits it, even though `immer` + `noch` (found by the missed-space path, an extremely strong
bigram) would score far higher. This is a distinct bug from D-67 (there is no high-confidence single-word
correction for `immernoch`, so that veto does not apply here) - the fix is that both split strategies must
be evaluated and the single best-scoring candidate across both must win, not whichever strategy happens to
run first.

### D-70 - Key-Press Sound Should Read as a Typewriter, Not a Digital Beep
The optional key-press sound (D-05) is currently a `ToneGenerator` DTMF-style tone, which only reads as an
electronic "beep" - `ToneGenerator` cannot render anything but a pure/dual sine tone, so no tone selection
can make it sound mechanical. Replace it with a short, bundled percussive click sample (a broadband noise
burst plus a couple of fast-decaying resonant partials) reminiscent of a typewriter key strike, played via
`SoundPool` for low-latency, overlap-safe playback.

### D-71 - Pattern Seeding Must Also Predefine the Systematic Offset *Direction*, Not Just the Spread
D-68 deliberately left the systematic-offset direction at zero, reasoning that whether a user over- or
undershoots a far key was a personal habit with no evidence to seed from. On reflection this was the wrong
call for a one-sided finger/thumb pattern specifically: reaching for a key far from the typing hand's home
position is a fixed-pivot arc (the wrist for a finger, the base of the thumb for a thumb), and that arc
physically tends to fall *short* of the visual target the farther it reaches - not a personal habit to wait
and learn, but close to a physical constant for anyone typing this way. [PatternSeed] must therefore also
seed a directional bias (`meanDx`, and for a thumb reaching the top row also `meanDy`) shifting the expected
strike point back towards home, growing with reach, on top of the existing spread widening. Two-thumb
typing keeps no directional bias, matching its flat spread.

### D-72 - The Number Row Is Missing From Calibration Seeding (Bug)
[PatternSeed] never seeds the number row's keys, because [CalibrationActivity]'s embedded preview keyboard
explicitly hides the number row (`showNumberRow = false`) while reading `charKeyGeometry()` for seeding -
the row's keys simply never appear in the geometry list handed to `PatternSeed.seed()`. Since the live
keyboard shows the number row by default (C-09), most users end up with digit keys that have never been
calibrated at all. The calibration preview must show the number row too.

### D-73 - Reorder the Typing Patterns by Real-World Prevalence; Skip Also Applies a Sensible Default
Both thumbs is by far the most common typing pattern and should lead the list (currently ordered
finger-patterns-first, ending with two thumbs) - order: both thumbs, right thumb, left thumb, right index
finger, left index finger. Skipping the picker must not leave the model unseeded either: it should apply
"both thumbs" quietly (no confirmation dialog, unlike an explicit choice) as the sensible default for an
undecided user.

### D-74 - Touch-Pattern Visualisation Shows the Previous, Stale Pattern After a Pattern Switch (Bug)
After switching the typing pattern, opening "Show typing pattern" from the settings screen (D-24) still
shows the *previous*, now-replaced pattern's (skewed) zones, not the freshly seeded ones. Root cause: the
live keyboard service is long-lived and holds its own in-memory copy of the personal offset model, only
refreshed when a genuinely new input field is focused; if the calibration screen replaces the persisted
model on disk while the service is resident but never refocuses a field in between, the service's next
`onFinishInput` / `onDestroy` save silently clobbers the fresh calibration with its stale in-memory copy.
The service must detect that the persisted pattern has changed since it last loaded its model and adopt the
fresh one instead of saving over it.

### D-75 - Key-Press Vibration Still Does Not Fire (Bug, D-66 Follow-Up)
The D-66 fix (migrating from the deprecated `Context.getSystemService(Vibrator::class.java)` to
`VibratorManager` on API 31+) did not resolve it - vibration still does not fire on device. Further
suspect: a plain `Vibrator.vibrate(VibrationEffect)` call with no explicit usage attributes falls into an
unclassified vibration category that some OEM vibration-intensity settings scale to zero, independently of
the (already-bypassed) "touch vibration" toggle. `VibrationAttributes.USAGE_TOUCH` (API 33+) is the category
Android itself documents for on-screen-keyboard-style UI feedback and should be requested explicitly where
available. Still needs device confirmation - this may ultimately be a device/OS-level vibration-intensity
restriction outside the app's control.

## §20 - Device-Feedback Round 9 (v0.7.33 testing, D-58 follow-up)

### D-76 - Page-Slide Animation Runs Backwards and Resizes Mid-Slide (Bug)
Two bugs in the D-58 slide animation:
- The slide direction is inverted relative to the usual "content follows the finger" page-swipe feel (as
  in a photo gallery or any carousel): a forward swipe/tap must slide the outgoing page off in the swipe
  direction and bring the new page in from the *opposite* edge, not the other way around.
- The whole keyboard does not animate as one piece - at minimum the space row visibly moves separately,
  looking wobbly. Root cause: the letter and numeric/symbol surfaces do not have the same row count (the
  number row is optional on letters, C-09, but always shown on symbols), so `AdaptKeyboardView.rebuildRows()`
  calling `requestLayout()` immediately on a page switch resizes the whole view mid-slide - everything below
  the row-count difference, most visibly the bottom-most space row, visibly jumps out from under the
  still-running animation. The resize must be deferred until the slide animation completes.

## §21 - Device-Feedback Round 10 (v0.7.34 testing, D-73/D-74 follow-up)

### D-77 - Remove the "(Recommended)" Label From the Two-Thumbs Button
It helps nobody and is actively unhelpful for anyone whose real pattern is different (e.g. a left-handed
single-finger typist) - they still have to read every option regardless. Silently defaulting to "both
thumbs" when the picker is skipped (D-73) already covers the intent for the ~90% majority; anyone who types
differently makes their own explicit choice anyway. Plain "Both thumbs" label, no suffix.

### D-78 - Dead Duplicate "Typing Pattern" Settings Row (Cleanup, Found During D-74 Follow-Up Investigation)
The settings screen had two separate "Typing pattern" rows in different categories: `k01_calibration`
(Calibration category), properly wired to open [CalibrationActivity] and the one users actually use, and
`t04_detected` (a leftover "Typing pattern (T-04)" category), `android:selectable="false"` with no click
handler at all - inert, and confusingly showed the same title as the working row. Removed entirely
(preference, its now-empty category, the `SettingsActivity` code that kept its summary in sync, and the
now-unused strings) rather than wired up, since `k01_calibration` already does the job.

### D-74 Follow-Up - Stale Touch-Zone Mask After a Pattern Switch: Root Cause Still Not Found
Re-investigated after the D-74 fix (service-side stale-save guard) did not resolve it. Established via
direct questioning that the reported repro is entirely within the Settings app (Settings → "Typing pattern"
→ pick a pattern → dialog OK → Settings → "Show typing pattern"/D-24), with **no app switch and no live
keyboard/IME involvement at any point** - meaning D-74's fix (which only guards the long-lived `AdaptKeyService`
instance) cannot be the relevant mechanism for this specific repro, since the IME is never in the loop. A new
regression test ([OffsetStoreRoboTest]) reproduces the exact `CalibrationActivity.persistPattern()` →
`TouchModelActivity` load sequence, seeding a model with substantial prior *learned* data (500 recorded taps,
not just a previous seed) before switching pattern, and passes: the persisted model is provably fully
replaced and the fresh load reflects it, not the old data. The bug, if still present, must be somewhere
neither direct code review nor this test have reached yet - needs a more specific repro (e.g. does it
reproduce on the very first pattern switch of a session, or only on a second/later switch; which exact key
still looks wrong and how).

## §22 - Device-Feedback Round 11 (v0.7.35 testing)

### D-79 - Rename "Typing Pattern" to "Typing Style"
"Typing pattern" (German "Tippmuster") is ambiguous - unclear whether it means how you hold/type (T-04:
finger/thumb) or the learned touch zones themselves (D-24). Rename the T-04 concept to "Typing style"
("Tipp-Stil" in German) everywhere it appears as user-facing text (onboarding, the calibration screen, the
settings entry, the reset-learning dialogs). The German D-24 "show learned touch zones" screen, which had
confusingly reused the very same word ("Tippmuster anzeigen") for a different concept, is renamed to
"Trefferzonen anzeigen" (matching the English/Greek versions, which already used a different word - "touch
pattern" / «μοτίβο αφής» - for D-24 than for T-04).

### D-74 Follow-Up 2 - Confirmed: Reproduces on the Very First Switch, Every Time
User confirmed the bug reproduces immediately on the first-ever attempt to switch typing style, and on
every subsequent attempt - ruling out any "only after a second switch" or "only with substantial prior real
usage data" theory. Extended the investigation with a second regression test
([CalibrationActivityRoboTest]) that drives the *real* `CalibrationActivity` UI end to end (via Robolectric's
`ActivityController`, not just the pure store/model layer): builds the real activity, taps the real "Both
Thumbs" button via `performClick()`, then opens a real `TouchModelActivity` and inspects its keyboard's
`offsetModel` - exercising real layout/measurement timing, `findViewById`, and the actual click listener
wiring, none of which a plain unit test can catch. **This test also passes** - the number row is seeded
(confirming D-72), the persisted pattern and model are both correct, and the freshly opened
`TouchModelActivity` reflects them accurately. Two independent regression tests at different levels (pure
store round-trip, and full real-Activity-with-a-real-click) both pass; the root cause remains unfound in
this code path. Reported this back to the user and asked for a much more specific repro (ideally a
screenshot, or an exact description of which key's zone looks wrong and how).

### D-74 - Resolved (Unconfirmed Cause)
User confirmed the switch now works correctly and the seeded zones look right (including the deliberate
D-68/D-71 overlap into neighbouring keys' territory at the far reach - by design, not a bug). Neither of
this round's changes (§21/§22: label removal, dead-row cleanup, the rename, two new tests) touched the
actual persist/load mechanism, so the fix cannot be attributed with certainty to anything in this
conversation - most likely D-74's original service-side staleness guard, if the user's device had not yet
picked up that build during the earlier failing attempts, though a one-off device/render quirk cannot be
ruled out either.

## §23 - Device-Feedback Round 12 (v0.7.36 testing)

### D-80 - "Show Touch Zones" Intro Text Sits Under the Front-Camera Cutout
The [TouchModelActivity] (D-24) intro text starts right at the very top of an edge-to-edge screen, so it is
partially obscured by a front camera cutout. Root cause: the screen's window-insets handling (matching
[CalibrationActivity], which has the identical bug for its own top-of-screen intro text) only accounted for
the bottom inset (navigation bar / gesture pill, for the keyboard preview at the bottom); nothing padded the
top for the status bar / display cutout. Both screens now also inset for `WindowInsetsCompat.Type.statusBars()`
and `WindowInsetsCompat.Type.displayCutout()`. While fixing this, also gave the D-24 intro a more generous,
readable presentation as requested: split into two clearly separated paragraphs (`d24_intro_1` /
`d24_intro_2`, replacing the single `d24_intro` string), a larger font size, and more breathing room -
there being little text and plenty of empty space above the keyboard preview to use.

### D-81 - The Onboarding Panel Has the Same Cutout Gap
The same class of bug (D-80) also applies to the first-run onboarding panel: while it is shown,
`AdaptKeyService` stretches the whole input view (`root`) to the full screen height
(`setOnboardingShown()`), so its top can now also reach the status bar / a front-camera cutout - but its
`setOnApplyWindowInsetsListener` only ever padded the bottom (for the gesture-nav-anchored keyboard below).
Fixed the same way as D-80: also inset for `statusBars()` / `displayCutout()`. During ordinary typing
`root` only wraps the (bottom-anchored) keyboard and suggestion bar and never reaches the top inset region,
so this reports (and adds) zero extra padding there then, exactly mirroring how the bottom inset already
behaved regardless of onboarding state - no risk of adding unwanted blank space above the keyboard during
normal typing.

## §24 - Device-Feedback Round 13 (v0.7.38 testing)

### D-82 - Page-Slide Still Bleeds Into the Gesture Area (D-76 Follow-Up)
The D-76 wobble is gone, but the slide can still visibly draw into the bottom gesture-nav inset padding.
Root cause: D-76 deferred `requestLayout()` until the slide animation ends, so the view's own measured
height stays frozen at the *outgoing* page's height for the whole animation - when sliding into a page with
*more* rows (e.g. symbols, 4 rows, into letters, 5 rows with the number row shown), the incoming page's
extra row is positioned below the view's still-old, shorter bounds. Because the container disables
`clipChildren` (D-53, so the long-press popup can overflow upward over the suggestion bar), that overflow
was never clipped and bled straight through into the reserved bottom-inset padding. Fixed by explicitly
clipping the key-drawing canvas to the view's own current bounds during `onDraw` (both the slide and the
plain case), leaving the long-press popup draw call outside that clip so its own D-53 overflow keeps
working. The visible cost: a page that grows by a row briefly clips that row until the deferred resize
lands at the end of the slide, rather than letting it bleed into the gesture area.

### D-83 - Key-Click Sound: Too Loud and Slightly Delayed
Two follow-ups to D-70's typewriter-click sample, both confirmed good in character:
- **Volume**: `CLICK_VOLUME` cut from `0.9` to `0.3` (SoundPool's volume range is linear, not dB, so this
  is a much bigger perceived cut than the raw numbers suggest) - comfortably past the requested "at least
  50% quieter".
- **Perceived delay**: three changes, each addressing a plausible source of `SoundPool` startup/output
  latency: (1) the bundled sample is re-encoded at 48 kHz (many devices mix natively at 48 kHz; the
  previous 44.1 kHz file could need on-the-fly resampling), (2) the async decode is now kicked off the
  moment the D-05 sound setting turns on rather than lazily on the first key press, so there is no
  decode-in-progress window during actual typing, and (3) `AudioAttributes.USAGE_GAME` replaces
  `USAGE_ASSISTANCE_SONIFICATION` - the usage `SoundPool`'s own reference documentation recommends for
  low-latency one-shot effects, and measurably faster to start on device for this exact class of sound.

### D-84 - Mid-Word Editing (D-62) Produces No Suggestions At All (Bug)
D-62's `reclaimSurroundingWord()` captures `tokenContextBefore` (via `captureTokenContext()`) *before*
deleting the reclaimed "before" fragment from the real editable, but never trims it back afterwards - so
the reclaimed fragment ends up duplicated: once as `tokenContextBefore`'s trailing text, once inside
`composing` itself. Every `"$tokenContextBefore $typed"`-style string built later
(`refreshSuggestions()`, `finalizeAndCommit()`) therefore contained a repeated word fragment, which was
silently confusing the A-03 language classifier into misreading the context as foreign and suppressing
suggestions *and* autocorrect entirely for the rest of the token - exactly the reported "no suggestions
appear" symptom, and specific to the mid-word reclaim path (an ordinary end-of-word token never has this
duplication). Fixed by trimming the reclaimed length off `tokenContextBefore` right after the reclaim.

### D-09 - Raw-Tap Recording: Removed Entirely
Per explicit instruction ("bitte komplett aufräumen"), the D-09 diagnostic - orphaned since D-68 replaced
the sentence-based calibration it recorded against - is now gone rather than repurposed: `RawTapRecorder`,
`CalibrationSentences`, `CalibrationSession` and their tests, `AdaptKeyboardView.OnRawTapListener` /
`onRawTapListener` and its D-09 forwarding call, the `recordRawTaps` setting (`AdaptSettings`,
`RawSettings`, `SettingsMapper`, `SettingsStore`), and the `d09_*` strings (all 3 locales) are all deleted.

## §25 - Device-Feedback Round 14 (v0.7.39 testing)

### D-82 - Confirmed: No More Bleed
User confirmed the D-82 clip fix produces no more visible bleed into the gesture area. Also raised, purely
as a design discussion (not a defect report): why not always draw pages bottom-aligned so the space row
sits at a fixed position and only the top edge moves with row count, or - if that alone doesn't solve it -
resize *before* the slide when the target page is taller and *after* it when the target page is shorter
(instead of D-76's blanket "always defer, clip during"), so a growing page never needs to be clipped at
all. Flagged as a worthwhile refinement to pick up if the user wants it (see progress.md) - not implemented
yet, pending confirmation.

### D-85 - Key-Click Sound: Halved Again
D-83's 0.3 volume was a clear improvement but still not "dezent" (subtle) enough. `CLICK_VOLUME` halved
again to `0.15`. **Confirmed by the user: "perfekt", and the D-83 latency work "ziemlich erträglich"
(quite tolerable)** - the sound-feedback thread is done unless something regresses.

### D-86 - Page-Slide: Resize Before Growing, After Shrinking; Bottom-Aligned Rows
D-82's clip fix confirmed no more bleed into the gesture area, but user pointed out a related artifact it
doesn't address: shrinking into a page with *fewer* rows can make that page suddenly appear pinned to the
*top* edge of the still-larger (not yet resized) container, instead of staying anchored to the bottom where
the user expects the space/enter row to be. Two changes, layered on top of D-82's clip (kept as a
last-resort safety net against `requestLayout()`'s inherent async re-measure latency):
- `switchPage()` now compares the target page's row count against the current one (`rowsFor()`, extracted
  from `rebuildRows()`) and resizes *immediately*, before starting the slide, when growing into more rows -
  the incoming page is never short on space in the first place. Shrinking into fewer rows still defers the
  resize until the slide ends, exactly as D-76 originally fixed (avoiding a visible mid-slide jump).
- `layoutKeys()` now lays rows out bottom-up (anchored just above `paddingBottom`) instead of top-down.
  Whenever the view's current height does not (yet) match what `rows` needs - the transient window either
  direction change leaves during its animation - the bottom-most row stays pinned to its usual position,
  with any slack appearing as blank space above the top row. Exactly reproduces the previous top-down
  result once the height matches again (the steady state, which is when `onMeasure()` already sizes the
  view to that same content height), so this is a no-op outside the transient window.

## §26 - Backlog Captured, Not Yet Implemented (v0.7.41 planning round)

D-86 confirmed working ("Animation ist jetzt perfekt"); touch-zone precision from the D-71/D-73 typing-style
work is confirmed good. Next focus area named by the user: making autocorrect quality genuinely excellent.
This round's items are captured here as backlog, per the usual rule for a larger batch - **not started**
until explicitly released, except where noted.

### D-87 - Mid-Word Live Correction (D-62) Still Does Not Activate At All (Bug, Re-Opened)
Despite the D-84 fix (which addressed a real, confirmed bug - the duplicated `tokenContextBefore` silencing
suggestions/autocorrect), the user reports mid-word editing still never activates the live-correction
behaviour at all. D-84's fix was verified logically (the duplication bug it targeted was real and clearly
would have caused exactly the "no suggestions" symptom reported at the time), but something else is
apparently still preventing D-62 from working, or D-84 did not fully resolve the reported case. Needs a
fresh investigation pass - likely starting with a very precise repro description (exact keyboard/app used,
exact tap sequence, whether *any* visible reaction happens at all e.g. does the word even get reclaimed
into a colourable/editable token) rather than another blind fix, given the previous "fix" that logically
should have worked apparently didn't (fully) resolve it.

### D-88 - Feedback When a Suggestion Is Accepted
Currently silent and visually unremarkable - "das ist etwas trocken und man kriegt es nicht ausreichend
mit" (too dry, not noticeable enough). Two additions, chosen by whether D-05 key sound is enabled:
- **Sound on**: a distinct short "plop" sample (separate from the D-70/D-83/D-85 key-click sample) plays
  when a correction is accepted - i.e. on the autocorrect commit path in `finalizeAndCommit()` when the
  committed word differs from what was typed, and/or when a suggestion-bar item is tapped.
- **Sound off**: some kind of brief micro-animation on acceptance instead, so the change is still
  noticeable without audio. Exact treatment (e.g. a quick colour flash or scale pulse on the committed
  word, or in the suggestion bar itself) to be designed during implementation.

### D-89 - Settings: a Full Feature Overview Page
A new settings screen (or section) that lists every feature the keyboard actually has - "nur, wenn man von
den Features weiß, kann man auch alle davon richtig nutzen" (you can only really use a feature if you know
it exists). AdaptKey has accumulated a large, easy-to-miss feature surface (raw-coordinate correction,
diacritic-aware autocorrect, drag-to-trash, the D-19 swipe cycle, G-01…G-05 gestures, D-36 quick paste,
D-43 next-word prediction, the D-01 long-press popups, etc.) with no single place a user can discover them
all. Needs its own content/copy pass, not just a settings-row stub.

### D-90 - New Alt-Key Hints on the Main Letter Page: P → π, B → ×, V → ÷
Extends [KeyboardLayout.DEFAULT_LETTER_HINTS] (L-05 / C-08) with three more math symbols, reachable by
long-press exactly like the existing `q→@`, `e→€`, `h→#`, `m→-`, `n→+`, `d→°` hints: `p→π` (pi), `b→×`
(multiplication sign), `v→÷` (division sign). Independent of, and complementary to, the calculator-page
redesign below (D-92) - these stay reachable without leaving the letter page at all, and are unrelated to
whatever the calculator page's own `×` / `÷` keys look like.

### D-91 - Reverse the D-19 Horizontal Swipe Direction
Currently a **right** swipe (`SwipeDirection.RIGHT`) advances forward through the D-19 cycle (letters →
symbols page 1 → symbols page 2) and a **left** swipe goes back ([KeyGesture.surfaceSwipe]). User wants
this reversed: reaching the first special/symbol page should be the **right**-swipe direction instead of
the left. A small, self-contained, easily-reversible change - swap which [SwipeDirection] maps to
`SWITCH_SURFACE_NEXT` vs `SWITCH_SURFACE_PREV` in `KeyGesture.surfaceSwipe()`. Does **not** touch the
space-bar language-swipe direction (G-01, `LANGUAGE_PREV` / `LANGUAGE_NEXT`), which is unrelated and not
mentioned. D-76/D-86's `forward` slide-direction plumbing in `AdaptKeyboardView.switchPage()` already
mirrors whatever `forward` value it's given, so the visual slide direction will automatically follow this
change with no separate adjustment needed there.

### D-92 - Redesign Both Symbol Pages: a Real Calculator Layout, Then a Leftover-Character Catch-All
The two `?123` pages get properly differentiated instead of being two similar-looking generic symbol grids,
and the two separate currency keys (`€` as an `e` alt-hint, `£` as a dedicated page-2 key) become **one**
locale-aware currency key with a popup for the rest (D-92 folds in what was originally a separate ask).

**First page - a real calculator layout** (proposed starting design, explicitly meant to be refined
further once it's actually being built):

```
Row 1:  (    )    °    √    π    ~    &    |    [1/2]  ⌫
Row 2:  7    8    9    ÷
Row 3:  4    5    6    ×
Row 4:  1    2    3    −
Row 5:  ABC  [¤]  0    [,]  +    =    [space]  ⏎
```

- Digits large and grouped as a real block (7-8-9 / 4-5-6 / 1-2-3 / 0), not spread across a single row -
  "ein echtes Taschenrechner-Layout" (a genuine calculator layout).
- `×`'s own alt-popup: `*` (asterisk) and `·` (middle dot, the German "Malpunkt") - explicitly requested.
  `÷`'s alt-popup was not fully specified; proposing `/` and `:` to mirror it (needs confirmation).
- `=`'s alt-popup: `→` (arrow), `≈` (approximately equal, "= mit ~"), `≙` (U+2259 ESTIMATES, the German
  "entspricht"-sign, "Gleich mit Dach").
- This page's own `2` and `3` keys (not the main page's number row, which is untouched) get `²` / `³` as
  alt-hints.
- `[¤]` is the new consolidated currency key (see below): base glyph depends on the system/keyboard
  language (`€` for German), alt-popup offers the other common currencies - `$` and `£` foremost, plus a
  few more common ones (e.g. `¥`).
- `[,]` is the decimal separator, language-dependent (`,` in German, `.` in English); its alt is the
  language-dependent thousands separator (`.` in German, `,` in English).
- `[1/2]` keeps its existing role (D-19 / L-03, toggles the two special pages - now "calculator" and
  "extra characters" rather than "symbols page 1/2").
- `[space]`: explicitly requested smaller and "directly above the Enter key", staying bottom-right. Placed
  inline directly left of Enter in this draft instead, since a true stacked-above-Enter placement (a
  partial-row-height key) is not something the current row/weight-based layout engine supports yet - would
  need a small layout-engine extension. Flagging this as an open question to resolve during implementation:
  is the inline-adjacent placement an acceptable interpretation, or is the stacked layout worth building?

**Second page - leftover / catch-all characters, no number row** (independent of the C-09 number-row
setting, which only ever applies to the main letters page): everything not already reachable as an alt
somewhere else (a main-page letter hint, a comma/period popup entry, or now the calculator page) is largely
already covered once the above lands - the German-locale audit found only `@`, `_`, `"`, `'` genuinely left
over from the old two symbol pages (`:` `;` `!` `?` already live on the comma/period popups; `#` `§` `&`
already live as alt-hints or on the number row; `*` moves to the `×` popup; `(` `)` `°` `√` `π` `~` `|` `=`
`£` `¥` all move to the calculator page). Per the invitation to add common characters to fill the now
sparser page: proposing `•` `©` `±` and the bracket family `{ }` `[ ]` `< >` (useful for anyone typing code
snippets or math outside the calculator page) - open for adjustment once this page is actually in front of
someone. No sentence punctuation here by design - already well reachable on the main page.

### D-93 - Symbol Pages Drop `1/2` and `ABC` When the Combined `?123` Key Is Disabled *(implemented, v0.7.42)*
When D-59's `symbolKeyEnabled` setting is off, the dedicated page-toggle (`1/2`/`2/2`) and back-to-letters
(`ABC`) keys on the symbol/numeric pages (L-03) are redundant with the D-19 full-field swipe, which already
reaches every surface (letters, symbols page 1, symbols page 2) regardless of that setting. `SymbolLayout.rows()`
now takes a `symbolKeyEnabled` parameter (default `true`, matching prior behaviour) and simply omits those two
keys from their rows when it is `false`; the remaining keys in each row grow to fill the freed space
automatically, since row layout already divides row width proportionally by the sum of the row's key weights
(`AdaptKeyboardView.layoutKeys()`) - no separate weight recalculation was needed. A small, clearly-scoped,
directly-approved fix, implemented immediately rather than queued in the §26 backlog.

### D-89 - Implemented: Settings Feature Overview (v0.7.43)
Added a new `FeatureOverviewActivity` ("Was AdaptKey alles kann" / "What AdaptKey can do"), launched from a
new entry in the settings screen's Info & Privacy category, right after "Show introduction again". It renders
a scrollable list of 18 curated feature entries (title + one/two-sentence description each), covering the
adaptive touch model, diacritic-aware autocorrect, neighbour-key/space correction, post-commit autocorrect
undo, multi-alternative long-press popups, alt-key symbol hints, the persistent number row, the D-19
letters/symbols swipe, the G-01…G-05 gesture set (language switch, whole-word delete, dismiss, drag-to-trash,
retroactive capitalisation), quick paste, next-word prediction, the optional tier-3 mini-AI, the three
auto-detected languages, and the provably-offline guarantee. The entry list itself lives in a pure
`FeatureCatalog` object (a list of string-resource-id pairs) so it is unit-testable without Robolectric;
`FeatureOverviewActivity` just inflates it into simple title/description view pairs, covered by a Robolectric
test that checks one view pair per entry is actually rendered. Full text in all three locales (English,
German, Greek).

### D-90 - Implemented: New Alt-Key Hints P → π, B → ×, V → ÷ (v0.7.44)
Extended `KeyboardLayout.DEFAULT_LETTER_HINTS` with the three new math-symbol long-press hints exactly as
specced: `p`→`π`, `b`→`×`, `v`→`÷`, alongside the existing `q→@`, `e→€`, `h→#`, `m→-`, `n→+`, `d→°`. `π` was
also added to the C-08 editor's `LetterHints.PALETTE` (next to the already-present `×`/`÷`), so a user who
reassigns `p` elsewhere can still pick `π` for another key, and so the existing
`PALETTE.containsAll(DEFAULT_LETTER_HINTS.values)` invariant test keeps holding.

### D-91 - Implemented: Reversed the D-19 Swipe Direction (v0.7.45)
Swapped which `SwipeDirection` maps to `SWITCH_SURFACE_NEXT` vs. `SWITCH_SURFACE_PREV` in
`KeyGesture.surfaceSwipe()`: a **left** swipe now advances forward through the letters -> symbols-page-1 ->
symbols-page-2 cycle, and **right** goes back. The space-bar language swipe (G-01) and the delete-word swipe
(G-02) are untouched - only the `else` branch of `KeyGesture.resolve()` (ordinary letter keys and the
combined `?123` key's horizontal swipe) is affected. The visual slide direction
(`AdaptKeyboardView.switchPage()`'s `forward` plumbing from D-76/D-86) automatically follows, since it already
mirrors whatever `forward` value it is given - no separate change needed, exactly as anticipated when D-91
was captured.

### D-92 - Implemented: Redesigned Both Symbol Pages (v0.7.46)
Rebuilt `SymbolLayout` per the §26 draft. Before implementing, the user resolved the one open question the
draft had flagged: the space key stays **inline, directly left of Enter** (no layout-engine extension for
partial-height rows) - "machen wir das erstmal inline neben Enter und schauen, wie es wirkt. Umbauen können
wir es immer noch."

**Page 1 - calculator** (5 rows): row 1 is `( ) ° √ π ~ & | [1/2] ⌫` (8 everyday symbols, then the page
toggle, then backspace - reordered from the old convention of leading with the toggle, to match the D-92
draft exactly); rows 2-4 are the digit block `7 8 9 ÷` / `4 5 6 ×` / `1 2 3 −`, each cell equally weighted so
the three rows form a genuine grid; row 5 is `ABC ¤ 0 , + = [space] ⏎`. `×`'s alt-popup is `* × ·` (asterisk
and the German "Malpunkt" middle dot, as requested); `÷`'s mirrors it with `/ ÷ :` (not fully specified in the
draft, decided per the "needs confirmation" note there); `=`'s is `= → ≈ ≙` (arrow, "≈", and the German
"Gleich mit Dach"). This page's own `2`/`3` keys carry `²`/`³` as long-press hints (the main number row is
untouched). `¤` is the new consolidated currency key, and `,` is the decimal separator with `.` as its
thousands-separator hint - both **resolved from the device's system locale** (see the D-92 follow-up below;
an earlier version of this entry hardcoded both to `€`/comma, which the user corrected). The bottom row's
`[space]` is deliberately smaller than the normal space bar (a local `CALC_SPACE_WEIGHT = 1.5f`, not exposed
via `KeyProportions` since it is a fixed per-page design choice, not a user setting) - this is the most
crowded row in the whole keyboard.

**Page 2 - leftover catch-all** (3 rows, no number row regardless of C-09): row 1 is `@ _ " ' • © ± [1/2] ⌫`
(the German-locale audit's leftover characters); row 2 is the bracket family `{ } [ ] < >`; row 3 is
`ABC [space] ⏎` with the normal-sized space bar (this row is not crowded) and deliberately no comma/period -
already well reachable on the main page.

Both pages keep D-93's `symbolKeyEnabled` gating: the page-toggle and `ABC` keys are omitted when the
combined `?123` key is disabled, on both pages' row 1 and bottom row respectively.

**D-92 addendum, raised by the user while resolving the space-key question:** "Der Sprachwechsel ist hier
natürlich nicht nötig. Den brauchen wir nur auf der Hauptseite" - the G-01 space-bar language swipe should
only apply on the letters surface, not on the (now also present, and smaller) space key on the symbol pages.
`KeyGesture.resolve()` gained a `surface: InputSurface = InputSurface.LETTERS` parameter; on any surface other
than `LETTERS`, a horizontal swipe on `KeyCode.SPACE` now falls through to the ordinary D-19 surface-swipe
behaviour instead of switching language. `AdaptKeyService.handleSwipe()` passes its current `surface` through.
This also fixes the same latent behaviour on the pre-D-92 symbol pages, which already had a space key that
inadvertently switched language on a horizontal swipe.

### D-92 follow-up - Implemented: Currency/Decimal Separator Now Follow the System Locale (v0.7.47)
The initial D-92 implementation hardcoded the currency key to `€` and the decimal separator to comma,
reasoning that the app's two selectable keyboard alphabets (German, Greek) never disagree on either point.
The user corrected this: both the **keyboard-selected language** and the **system-selected language/region**
were meant to influence these keys, with an explicit fallback rule - "bei Tastatur-sprachlicher Gleichheit
(egal) auf das System als Entscheidung zurückfallen" (when the keyboard language is a tie, fall back to the
system for the decision). Since German and Greek always tie on this specific point, the practical result is
that the **system locale now always decides** - "Ein Amerikaner wird kein Verständnis für ein Komma und ein
€ Zeichen haben," and English already exists in AdaptKey as a language, "wenn auch subtiler" (via A-03
auto-detection, even though it is never a directly selectable keyboard alphabet).

Implementation: a new pure, JVM-testable `CalculatorLocale` object (`keyboard` package) resolves a
`java.util.Locale` into a `Format` (currency glyph + popup, decimal separator, thousands-separator hint)
using the JDK's own locale data - `java.text.DecimalFormatSymbols` and `java.util.Currency` - rather than a
hand-maintained table, so it is correct for locales well beyond the app's own DE/EN/EL dictionaries (verified
for `en_US` → `$`/`.`, `en_GB` → `£`/`.`, `de_DE` → `€`/`,`, `el_GR` → `€`/`,`, `ja_JP` → the fullwidth yen
sign). `Key.char` is a single `Char`, but not every locale's currency resolves to one glyph (a bare ISO code,
or a locale without a resolvable country); those fall back to `€` rather than truncating or crashing.
`SymbolLayout.rows()` gained a `locale: Locale = Locale.getDefault()` parameter (calculator page only);
`AdaptKeyboardView` gained a `systemLocale: Locale` property threaded through the same way as
`symbolKeyEnabled`; `AdaptKeyService.applySettings()` sets it from `resources.configuration.locales[0]` - the
device's actual system locale, deliberately not the app's own DE/EL keyboard-alphabet toggle.

---

## §27 - Device-Feedback Round 15 (v0.7.47 testing)

D-91 confirmed working ("Das funktioniert genau richtig") - a real device round on the D-89…D-92 batch,
mostly focused on the D-92 calculator/catch-all pages. Captured here as backlog, per the usual rule -
**not started** until explicitly released.

### D-94 - Bug: the D-91 Swipe Reversal Didn't Reach the Slide Animation
D-91 correctly reversed which direction (`SwipeDirection`) advances vs. goes back through the letters →
symbols-1 → symbols-2 cycle, and the user confirms the *function* is now exactly right. But the *visual*
slide animation still moves the old way. D-91's implementation assumed `AdaptKeyboardView.switchPage()`'s
`forward` plumbing (from D-76/D-86) would automatically follow the new mapping with no separate change - that
assumption was apparently wrong, or incomplete. Needs investigation: likely `applySwipePage()` /
`PanelNavigation.swipePage()` in `AdaptKeyService` still derive the slide's `forward` flag from the page-index
comparison (`targetSymbolPage > symbolPage` or similar) rather than from which *swipe direction* the user
actually made, so the two got decoupled once D-91 remapped direction to action independently of page order.

### D-95 - Idea, Parked: a Settings-Shortcut Key Somewhere on the Keyboard
A key that jumps straight to Settings would be convenient, but the user has no placement idea yet and
explicitly wants this only kept in mind for now ("behalten wir das erstmal nur im Hinterkopf") - not an
actionable backlog item until a placement makes sense (a spare slot might open up once D-100 below reshuffles
the calculator page, worth revisiting then).

### D-96 - Main Page: Reorganise the v / b (and New x / c) Alt-Hints
Current (`KeyboardLayout.DEFAULT_LETTER_HINTS`, from D-90): `b`→`×`, `v`→`÷`. New assignment:
- `x` → `×` (multiplication moves here)
- `c` → `÷` (division moves here)
- `v` → `/` (slash, new)
- `b` → `*` (asterisk, new)

`x` and `c` currently carry no hint at all, so this is a clean four-way reassignment, not a conflict. `π` on
`p` (D-90) is untouched.

### D-97 - Calculator Page: Space Key Shouldn't Show the Language Label
D-03 makes every `KeyCode.SPACE` key show the active input language ("Deutsch") via
`AdaptKeyboardView.labelFor()`'s `spaceLabel` branch - correct for the main letters page, but the calculator
page's smaller, inline space key (D-92) inherits it too, which makes no sense there (mirrors the same
surface-scoping mistake G-01 had before the D-92 follow-up fixed the *gesture* side). Wants the plain,
Gboard-style long rounded-rectangle space glyph ("▭"-like, exact glyph/icon TBD during implementation)
instead, regardless of active language. Likely needs `labelFor()` (or the caller) to know which surface a
given space key belongs to, similar to how `KeyGesture.resolve()` gained a `surface` parameter for D-92.

### D-98 - Calculator Page: Currency Popup Order + a "More Alternatives" Corner Indicator
Two asks:
1. Reorder the currency popup to `€ $ £ ¥` (currently `$ £ € ¥` from the original D-92 implementation).
2. A corner-hint glyph that signals "this key has more long-press alternatives" - **does not exist yet**: today
   `AdaptKeyboardView` only ever draws a key's own [Key.hint] string as the corner glyph (L-05/C-08); a
   multi-alternative key (D-01) with `alternatives.size >= 2` but `hint == null` - comma, period, the currency
   key, `×`/`÷`/`=` on the calculator page, etc. - currently draws **no** corner indicator at all. This would be
   a new, generically-applicable visual (small dot/triangle badge, exact treatment TBD), not specific to the
   currency key alone.

Also flagged, tied to D-100's layout move below: once the currency key relocates into the new right-hand
column, a popup that always grows rightward (D-44) may run out of room against the screen edge - the order
(or growth direction) may need to flip depending on the key's on-screen position. Needs resolving once D-100's
exact column position is implemented and actually on screen.

### D-99 - Main Page: Greek-Letter Alt-Popup on the π Key
The `p`→`π` key (D-90) gets a multi-alternative long-press popup (D-01) offering the Greek letters
`α β γ δ λ ω` (Alpha, Beta, Gamma, Delta, Lambda, Omega) alongside `π` itself.

### D-100 - Calculator Page: Narrower Digit Block, New Persistent Right-Hand Column, Digit-Grid 4th Row, Page-Toggle Key Removed
The biggest item this round - a further reshuffle of D-92's calculator page:

- **Narrow the digit block** to free a new column on the **right edge**, running the full height of the page
  (row 1 down to the bottom row), holding, top to bottom: `⌫` (already there), `[space]` (moved here, under
  backspace), `¤` (currency, moved here, under space), the optional `ABC` key (D-59-gated, moved here, under
  currency), `⏎` (already there, at the bottom). **All cells in this right-hand column must render at the
  same width as each other** ("alle Tasten bis runter zum Enter in derselben Breite").
  - Technically non-trivial: `AdaptKeyboardView.layoutKeys()` divides each row's own width proportionally by
    the *sum of that row's own key weights* - rows with very different total content (row 1's ~7-8 symbols vs.
    a 4-cell digit row) will not automatically give a same-*weight* trailing cell the same *pixel* width. The
    right column's weight per row will likely need computing from each row's own total content weight (so the
    column occupies the same *fraction* of every row), not just reusing one constant weight value.
- **The digit block gains a 4th row**, turning the operators + digits area into a genuine 4×4 grid:
  ```
  7  8  9  ÷
  4  5  6  ×
  1  2  3  −
  0  ,  =  +
  ```
  `0` under `1`, the decimal separator under `2`, `=` under `3`, and `+` under the operator column (÷/×/−).
  Every cell in this 4×4 grid renders at the same width as the column above it.
- **The page-toggle key (`1/2` on this page, `2/2` on page 2 - the user's note names it "die 2/2 Taste" but the
  surrounding text is entirely about page 1's layout, so this is read as shorthand for "the page-toggle key",
  to be confirmed) is removed entirely from both pages, without D-93's `symbolKeyEnabled` gating** - it's just
  gone, freeing its slot unconditionally. The two symbol pages become reachable **only** via the D-19/D-91
  full-field swipe, or the documented fallback detour: `ABC` back to the main letters page, then the combined
  `?123` key back to symbols page 1, then swipe to page 2.

### D-101 - Calculator Page: Bracket Family as the `(` Key's Alt-Popup
The separate `(` and `)` keys (row 1) become **one** key (exact base glyph/label TBD - `(` is the natural
choice) whose D-01 multi-alternative popup offers `)` plus the rest of the bracket family: `{` `}` `[` `]`
`<` `>`. This is what makes page 2's bracket row (D-102 below) redundant.

### D-102 - Page 2: a Real Number-Row Alternative + Full Letter-Hint Distribution
Page 2 gets upgraded into something genuinely useful for anyone who has turned the main page's number row off
(C-09) - not just a leftover catch-all:

- **Remove the bracket row** (`{ } [ ] < >`) - redundant once D-101 lands (reachable via the `(` key's popup).
- **Add back a fixed digit row** (`1234567890`), shown **unconditionally, independent of C-09** - this
  explicitly **reverses** D-92's original "no number row regardless of C-09" decision for this page. The
  intent: C-09 stays a main-page-only declutter option, and page 2 becomes the fallback place to reach digits
  (and, per the next point, their shifted symbols) when it's off.
- **A new row directly under the digit row**, holding all of the main number row's shifted-symbol long-press
  hints (`L-06`/`NUMBER_HINTS`: `! " § $ % & / ( ) =`) as plain, directly-tappable characters - not hidden
  behind a long-press, since the whole point is restoring easy access to them when the main number row (and
  its long-press hints) are hidden.
- **Distribute the main letter page's alt-hint symbols** (everything in `KeyboardLayout.DEFAULT_LETTER_HINTS`
  - `@ € # - + °` plus D-96's reorganised `× ÷ / *`, and `π`'s own new Greek-letter popup from D-99 stays
    where it is) across the rest of page 2's available space "sinnvoll" (sensibly) - **the user explicitly
    flags this as provisional**: "schauen wir, wie sich das ergibt und sortieren ggf. nochmal um" (we'll see
    how it turns out and re-sort if needed). Treat the first implementation as a draft layout to review
    together, not a final answer.

### D-103 - Calculator Page: a Trig-Function Key (sin, alt: cos / tan / log)
A key labelled `sin` whose D-01 multi-alternative long-press popup offers `cos`, `tan` and `log`. No
placement decided yet - the user explicitly wants it fit in wherever it reasonably ends up once D-100's
reshuffled calculator page exists ("versuche einfach, sie sinnvoll unterzubringen... dann schaue ich, ob mir
das gefällt" - just try to place it sensibly, then I'll see if I like it). Worth considering together with
D-100's new right-hand column and 4×4 grid, and with D-104 below, since both are calculator-page additions
competing for the same likely-tight space. Multi-character labels (`sin`/`cos`/`tan`/`log`) are new territory
for this key grid - every existing key is a single glyph - so this may also need a small label-width sanity
check once it's actually laid out.

### D-104 - Calculator Page: a deg/rad Toggle Key
Same status as D-103 - no placement decided, park it alongside D-103 and fit both in wherever makes sense once
D-100 lands, then get the user's read on it. Toggles the trig-function key's (D-103) angle unit; exact
key behaviour (a two-state toggle vs. its own popup) to be decided during implementation.

---

## §28 - §27 Release Round 1 (v0.7.48+)

User confirmed the D-100 "die 2/2 Taste" reading (§27) as correct, and clarified D-100 further: since every
digit-grid row except the first has the same cell count, the right-hand column's width will line up "optimal"
without needing special per-row weight math - noted for whenever D-100 is released. Released this round, in
this order: D-96, D-97, D-98, D-99, D-101, then D-94.

### D-96 - Implemented: Reorganised the v / b / x / c Alt-Hints (v0.7.48)
`KeyboardLayout.DEFAULT_LETTER_HINTS` now maps `x`→`×`, `c`→`÷`, `v`→`/`, `b`→`*`, replacing D-90's `b`→`×` /
`v`→`÷`. `x` and `c` carried no hint before, so this was a clean four-way reassignment. `p`→`π` (D-90) is
unaffected. All four reassigned letters live on the third row (`yxcvbnm`), so nothing moved between rows.

### D-99 - Implemented: Greek-Letter Popup on the π Key (v0.7.48)
The `p`→`π` key now carries a D-01 multi-alternative popup, `KeyboardLayout.PI_ALTERNATIVES = listOf("π", "α",
"β", "γ", "δ", "λ", "ω")`, alongside its existing corner hint. `KeyboardLayout.rows()` gained a private
`topRowKey()` helper that only attaches the popup when `letterHints['p']` still equals the default `π` hint -
a user who has reassigned `p` to something else via the C-08 editor keeps their own plain single-symbol
long-press instead of an unrelated Greek-letter popup. The underlying popup mechanism (`Key.alternatives`,
`AdaptKeyboardView.popupAlternativesFor`) is already fully generic across every layout (letters, symbol
pages), so no view-layer change was needed - only the key data itself.

### D-97 - Implemented: Symbol Pages' Space Key No Longer Shows the Language Label (v0.7.49)
`AdaptKeyboardView.labelFor()`'s D-03 branch (space bar shows the active input language) now checks
`surface == InputSurface.LETTERS` before using `spaceLabel`; on any other surface, `KeyCode.SPACE` falls
through to a new plain glyph, `SPACE_GLYPH = "␣"` (U+2423 OPEN BOX - the usual elongated-open-rectangle space
symbol the user described). Scoped to "not the letters surface" rather than "the calculator page
specifically", for consistency with the same surface-scoping the D-92 follow-up already applied to the G-01
gesture - this also fixes page 2's space key, which had the same latent issue even though only the calculator
page was explicitly mentioned. Purely a `labelFor()`/drawing change; no test added (private, drawing-only View
glue - an existing, documented testing gap for this class, not a new one).

### D-98 - Implemented: Currency Popup Order + "More Alternatives" Corner Indicator (v0.7.50)
1. `CalculatorLocale.COMMON_CURRENCY_SYMBOLS` reordered to `€ $ £ ¥` (was `$ £ € ¥`).
2. New generic corner indicator: `AdaptKeyboardView.drawKeys()` now draws a small triangle,
   `MORE_ALTERNATIVES_GLYPH = "◢"`, in a key's corner whenever it has no single [Key.hint] of its own but does
   have a D-01 multi-alternative popup (`alternatives.size >= 2`) - comma, period, and the calculator page's
   `×`/`÷`/`=`/currency/`(` keys all previously drew no corner cue at all. Applies generically to every layout,
   not just the currency key. Gated by the same `hintsEnabled`/`suppressHint` checks as the existing hint
   glyph. The right-to-left popup-growth question flagged alongside this item in §27 (tied to D-100's column
   move) is deferred until D-100 actually lands and the key's on-screen position is known.

### D-101 - Implemented: Bracket Family as the `(` Key's Alt-Popup (v0.7.51)
Page 1's row 1 drops the separate `)` key; `(` now carries `BRACKET_ALTERNATIVES = listOf("(", ")", "{", "}",
"[", "]", "<", ">")` as its D-01 popup (base included first, matching the `=`/period/comma convention). Row 1
goes from 8 symbols to 7 (`CALC_ROW1_SYMBOL_COUNT` updated accordingly, so `thirdRowLetterWeight` still
accounts for the actual cell count). Page 2's bracket row is untouched for now - removing it is D-102, not yet
released, so it stays as a (now momentarily redundant) fallback until then.

### D-94 - Implemented: Fixed the Slide-Animation Direction After D-91 (v0.7.52)
Root cause confirmed as suspected: `AdaptKeyService.handleSwipe()` derived the slide animation's `forward`
flag from the resulting *action* (`SWITCH_SURFACE_NEXT` → `true`, `SWITCH_SURFACE_PREV` → `false`), not from
the *physical swipe direction*. Before D-91 these always coincided (right swipe was always NEXT), so the bug
was invisible; D-91 decoupled them (left swipe is now NEXT), and the animation kept following the old
action-based logic - so it played backwards relative to the finger for exactly the cases D-91 changed.

Fix: both `SWITCH_SURFACE_NEXT`/`SWITCH_SURFACE_PREV` branches now pass `forward = direction ==
SwipeDirection.RIGHT` to `applySwipePage()` for the animation, while `PanelNavigation.swipePage()`'s own
`forward` argument (which page to land on) stays action-based, unchanged. These are two genuinely different
"forward" concepts that happened to share one boolean pre-D-91: which page to land on (action-based, D-91's
concern) and which way the slide visually plays (physical-direction-based, D-76's concern, per its own
comment - "forward slides the outgoing page off to the right ... matching a right swipe"). No new test - this
is `AdaptKeyService`'s swipe-handling glue, already a documented Android-service testing gap, not a new one.

### D-100 - Implemented: Narrower Digit Block, New Right-Hand Column, Page-Toggle Key Removed (v0.7.53)
Page 1 (calculator) restructured to the confirmed reading of the §27 draft:

- **Row 1**: the row-1 symbols (`( ° √ π ~ & |`), then `⌫` - the top of the new right-hand column. No more
  page-toggle key.
- **Rows 2-4**: the digit/operator grid (`7 8 9 ÷` / `4 5 6 ×` / `1 2 3 −`), each row ending with its column
  cell - `space` (row 2, moved here from the old bottom row), `¤` currency (row 3, likewise moved), the
  optional `ABC` (row 4, D-59/D-93-gated, likewise moved).
- **Row 5 (new)**: `0` under `1`, the decimal separator under `2`, `=` under `3`, `+` under the operator
  column, ending with `⏎`.

Column-width uniformity: rows 2-5 each have the same number of other cells (4), so giving the column cell in
each of those rows the same weight (`CALC_COLUMN_WEIGHT = 1f`, matching the plain digit-cell weight) makes
them all render at the same pixel width automatically - exactly the "wird sich das optimal ergeben" the user
predicted. Row 1's `⌫` is the confirmed exception (different cell composition) and keeps
`proportions.backspaceWeight` (L-04) rather than trying to force a match. The decimal separator also drops its
old `proportions.commaWeight` (previously wider, matching the letter page's comma) in favour of the same
uniform column/grid weight as its row-5 neighbours (`0`, `=`, `+`) and the digits above it.

Page-toggle key removed entirely from **both** pages (D-93's `symbolKeyEnabled` gating no longer applies to
it - it's just gone, unconditionally). Both pages now reachable only via the D-19/D-91 swipe or the `ABC`
detour back to letters. Dead code removed along with it: `KeyCode.SYMBOL_PAGE`, `SymbolLayout.togglePage()`,
`SymbolLayout.pageLabel()`, and the corresponding tap-handling branch in `AdaptKeyService`
(`PanelNavigation`'s own independent page-cycling logic was already the only thing driving D-19/D-91 - this
key's handler was its sole other caller).

**Currency popup order, addressed proactively per the user's own flag:** now that `¤` sits in the far-right
column, a D-44 popup pre-selected near the *start* of its list (the old `€ $ £ ¥` order, with the typical
German-locale base `€` at index 0) would want to grow almost entirely rightward from a key already at the
screen edge - `AdaptKeyboardView.openPopup()`'s existing edge-clamp (`HorizontalLongPressPopup.rowLeft()`)
keeps it on-screen, but shoves the whole row away from the stem key, decoupling the visible popup from where
the finger is. `CalculatorLocale.COMMON_CURRENCY_SYMBOLS` reversed to `¥ £ $ €` so the base glyph tends to
land near the *end* of the list instead, biasing the popup to grow leftward - into the room that's actually
there. The uncommon-currency fallback (`resolveCurrencyGlyph` returning something outside the common four)
now appends rather than prepends, for the same reason.

### D-102 - Implemented: Page 2 Upgraded into a Real Number-Row Alternative (v0.7.54)
Page 2 grew from 3 rows to 5, per the confirmed reading of the §27 draft:

- **Row 1** (leftover symbols): `@ _ ' • © ±` (six, not seven - the double quote moved to row 3 below to
  avoid the same glyph appearing twice on this page), then `⌫`. Still no page-toggle key (D-100).
- **Row 2 (new)**: a fixed digit row `1234567890`, shown **unconditionally, independent of C-09** - this
  deliberately reverses D-92's original "no number row regardless of C-09" decision for this page. Plain
  digits, no long-press hints - row 3 already puts their shifted symbols directly on the surface.
- **Row 3 (new)**: the main number row's shifted symbols in `1..0` order - `! " § $ % & / ( ) =` - as plain,
  directly-tappable characters rather than hidden behind a long-press, restoring easy access to them when the
  main number row (and its hints) are hidden.
- **Row 4 (new)**: the main letter page's alt-hint symbols, distributed here: `€ # - + ° × ÷ *`. Two
  exclusions to avoid a same-page duplicate: `@` (q's hint) is skipped, already covered by row 1; `/` (v's
  hint, D-96) is skipped, already covered by row 3 (7's shifted symbol). `π`'s own Greek-letter popup (D-99)
  stays on the letters page rather than being duplicated here. **Per the user's own framing, this is a first
  draft** - "schauen wir, wie sich das ergibt und sortieren ggf. nochmal um" - explicitly open to re-sorting
  once seen on a real device.
- **Row 5** (unchanged): optional `ABC`, `space`, `⏎`.

Verified no character repeats anywhere on the page (new test, `SymbolLayoutTest`). Not yet device-tested - the
letter-hint distribution (row 4) in particular is exactly the part flagged as provisional.

---

## §29 - Correction Round on D-101 / D-100 / D-102 (v0.8.4)

Before any device testing of §28's batch, the user caught a misreading of D-101 and requested a few more
refinements to D-100/D-102 while reviewing. All implemented in this round; no separate spec-then-wait cycle
since each point is small, self-contained, and directly approved in the same message.

### D-101 - Corrected: `(` and `)` Stay Two Separate Keys
The original D-101 implementation merged `(`/`)` into one key. That was a misunderstanding - the intent was
always **two** keys, each keeping its own D-01 popup for its own bracket family (opening vs. closing):
`OPEN_BRACKET_ALTERNATIVES = listOf("(", "{", "[", "<")`,
`CLOSE_BRACKET_ALTERNATIVES = listOf(")", "}", "]", ">")`. Page 1's row 1 goes back to 8 symbols (`()°√π~&|`).
The page 2 correction below applies the same treatment to the `(`/`)` that live in its shifted-symbols row.

### D-100 - Corrected: Page 1's `ABC` Slot Must Stay Reserved When Hidden, Not Collapse
D-93's established pattern - just omit a disabled key and let its row's remaining cells grow - is *wrong* for
page 1's `ABC` key specifically, because D-100 made every row's cell count part of a cross-row column/grid
alignment: omitting `ABC` narrows its row relative to its neighbours, breaking the grid. `SymbolLayout` now
always emits the `ABC` key on page 1 regardless of `symbolKeyEnabled`; `AdaptKeyboardView` hides it (drawing
nothing, taps inert) exactly the way it already treats the disabled combined `?123` key (D-59) - the shared
predicate was renamed `isHiddenKey` and gained a second condition: `KeyCode.LETTERS` on the `SYMBOLS` surface,
`symbolPage == 1`, `!symbolKeyEnabled`. Page 2's own (unrelated) `ABC` key keeps the D-93 omit-and-grow
behaviour unchanged - it isn't part of a column layout, so nothing there needed to change.

### D-100 - Also: Swapped `ABC` and `=`
While confirming the reserved-slot fix, the user asked to swap `ABC` and `=`: `=` now sits in the persistent
right-hand column (row 4, always visible, no gating - fitting, since a real calculator's `=` usually lives in
a prominent bottom-right position), and `ABC` moved into the digit grid instead, at row 5 under `3` (still
`symbolKeyEnabled`-gated via the reserved-slot mechanism above, just at a different position). Row 4 is now
`1 2 3 − =`; row 5 is `0 <decimal-sep> ABC + ⏎`.

### D-102 - Corrected/Extended: Bracket, Currency and Underscore Alts on Page 2
Three refinements to page 2, all additive (no structural change):
- The `(`/`)` in row 3 (the shifted-symbols row) get the same `OPEN_BRACKET_ALTERNATIVES` /
  `CLOSE_BRACKET_ALTERNATIVES` popups as page 1's bracket keys, per the D-101 correction above. This doesn't
  reduce their direct (short-tap) accessibility - row 3's whole point (D-102) - since the base character still
  commits on a plain tap; the popup is a purely additive long-press extra.
- Row 4's `€` (distributed from the letters page's `e` hint) also gets the common-currency popup - redundant
  with page 1's dedicated currency key, by explicit request. It sits on the *left* of the screen here, unlike
  page 1's, so its alternatives stay in the **un**-reversed order (`€ $ £ ¥`, base first) so the popup grows
  rightward, into the room that's actually there on this side.
- Row 4's `-` (distributed from the letters page's `m` hint) gets `_` as a single long-press alt.

---

## §30 - §29 Follow-Up: Bracket Order, Page-2 Reshuffle, New Symbols (v0.8.5)

Continued review of §29, before device testing.

### Bracket popup order - square before curly
`OPEN_BRACKET_ALTERNATIVES` / `CLOSE_BRACKET_ALTERNATIVES` (shared by page 1's `(`/`)` and page 2's `(`/`)`
in the shifted-symbols row) reordered to `( [ { <` / `) ] } >` - square brackets before curly braces.

### Page 2: dedicated underscore key dropped
Now redundant with `-`'s own `_` alt (added last round) - removed from row 1.

### Page 2: `+` to the end of row 4, `€` to the start of row 1
`+` moves to the last position of row 4 (the last content row); `€` moves out of row 4 entirely, to the
first position of row 1, keeping its common-currency popup.

### Page 2: apostrophe moved to row 4, second position
Was in row 1; now sits right after `#` in row 4 - which happens to put it roughly under row 3's own `"`,
directly above it.

### Page 2: `®` added next to `©`
The circled C and circled R sit together in row 1.

### Page 2 + letters page: `Ø` added ("Durchschnitt")
Added to page 2's row 1 (room was made by the removals above). Also added as a second long-press alternative
on the letters page's `o` key (`KeyboardLayout.O_ALTERNATIVES = listOf("ö", "Ø")`) - chosen as the host key
since Ø is visually a stylised O; gated the same reassignment-aware way as `p`'s Greek-letter popup, so a
user who has reassigned `o` via C-08 keeps their own plain hint. **Bug fixed while implementing this**: the
o key is in the *top* row (`qwertzuiop`), not the middle row (`asdfghjkl`) - an internal test caught a wrong
row index before this shipped.

### Letters page: `f` gets the function symbol, also added to page 2
`f` was still unassigned - `KeyboardLayout.DEFAULT_LETTER_HINTS` gained `f`→`ƒ` (also added to
`LetterHints.PALETTE`, keeping the C-08-editor-can-reproduce-every-default invariant). Also added to page
2's row 1.

### Calculator page: the π key gets its Greek-letter popup too
Previously plain (no `hint`, no `alternatives`) on page 1, unlike the letters page's `p`→π. Now reuses
`KeyboardLayout.PI_ALTERNATIVES` directly (exposed, was `private`) so both π keys stay in sync. The user
described an "obscure case-toggle function" firing instead of a proper popup on long-press - no separate
bug was found in the case/shift-handling code, so the working theory is that this was simply the absence of
a defined long-press action; giving the key `alternatives.size >= 2` routes it through the same D-01 popup
mechanism as every other multi-alternative key. Flagged as something to confirm is actually resolved once
next tested on a device, since the exact prior behaviour wasn't independently reproduced first.

### Calculator page: reordered the operator column - plus, minus, times, divide (v0.8.6)
The right-hand operator column (the 4th cell of rows 2-5, alongside the digit block) reordered top to bottom
from `÷ × − +` to `+ − × ÷`. Only the operator identity per row changed - the digits (`7 8 9` / `4 5 6` /
`1 2 3` / `0`) and each row's own right-hand-column cell (`space` / currency / `=` / `⏎`) stay exactly where
they were. Each operator keeps its own alt-popup wherever it now sits: `×` still offers `* × ·`, `÷` still
offers `/ ÷ :`. New row layout: row 2 `7 8 9 +`, row 3 `4 5 6 −`, row 4 `1 2 3 ×`, row 5
`0 <decimal-sep> ABC ÷`.

### Page 2: corrected the wrong symbol moved in the row-4 reshuffle (v0.8.7)
§30's "`+` moved to the end of row 4" was a misreading of the original ask - it was always meant to be `±`
(plus-minus), not the plain `+`. Corrected: `+` moves back to its original spot in row 4 (right after `-`,
before `°`); `±` moves out of row 1 to the very end of row 4 instead. Row 1 is now `€ @ • © ® Ø ƒ` (seven,
not eight); row 4 is now `# ' - + ° × ÷ * ±` (nine, not eight).

### Page 2: `ABC`'s slot next to `space` also reserved when hidden (v0.8.8)
Page 2's bottom row previously used D-93's original omit-and-grow pattern for its `ABC` key: hidden entirely
when the combined `?123` key is off, letting `space` grow to fill the gap. Per request, this now matches
page 1's D-100-corrected treatment instead - `ABC`'s slot stays reserved (drawn as nothing, tap inert) rather
than collapsing, so `space` stays its normal size instead of looking oversized when `ABC` is hidden.

Implementation: `SymbolLayout` no longer takes a `symbolKeyEnabled` parameter at all - both pages'
`ABC` key is now unconditionally present in the returned rows (page 1 already worked this way since the
D-100 correction; page 2's row 5 now does too). All of the visibility/interactivity logic lives in
`AdaptKeyboardView.isHiddenKey()`, whose `KeyCode.LETTERS` condition dropped its `symbolPage == 1`
restriction - it now applies uniformly to whichever symbol page is showing. Since `SymbolLayout` no longer
needs the setting, `AdaptKeyboardView.rowsFor()`'s call into it dropped the argument too.

---

## §31 - Case-Toggle Bug Fix, Page-2 Reorders, Phone-Field Auto-Select (v0.8.9)

### Page 2 row 1: bullet moved ahead of `@`
`CATCHALL_ROW1_SYMBOLS` reordered from `€ @ • © ® Ø ƒ` to `€ • @ © ® Ø ƒ`.

### Page 2 row 4: `°` moved ahead of `+`, `^` inserted at position 3
So the arithmetic operators (`- + × ÷`) aren't split apart by `°` sitting between them, and to fill the row
out to 10 keys - matching rows 2 and 3's width, since they now all have the same key count.
`CATCHALL_LETTER_HINTS` reordered/extended from `# ' - + ° × ÷ * ±` to `# ' ^ - ° + × ÷ * ±`.

### Bug fixed: symbol-page character keys were getting auto-capitalised
The calculator page's `π` key always produced `Π`; the same happened to page 2's `ƒ` key. Root cause,
confirmed by investigation: this is **not** a per-key case-toggle mechanic - it's the ordinary
auto-capitalise-at-sentence/field-start feature (`armShiftForNextWord`) arming `shifted = true`
automatically (no user Shift tap involved), combined with two independent, insufficiently-guarded
`uppercaseChar()` call sites that only checked `Char.isLetter()` - true for `π` and `ƒ` because they are
genuine Unicode letters (Greek `Ll`, Latin `Ll` respectively), even though on the `SYMBOLS` surface they
represent a constant and a symbol, not alphabetic text:
- **Commit path** (`AdaptKeyService.kt`, `handleKey()`'s `KeyCode.CHAR` branch): `raw.isLetter()` decided
  both whether the character continues a composing "word" (`extendsToken`) and whether it gets
  uppercased. Fixed by introducing `isWordLetter = raw.isLetter() && surface == InputSurface.LETTERS` and
  using that instead of the bare `isLetter()` check in both places. This also fixes a related, distinct gap:
  `π` was being treated as continuing a composing word (subject to autocorrect/suggestions) rather than
  committing immediately like the calculator page's other symbols (`°`, `√`, `~`, `&`, `|`).
- **Display-only path** (`AdaptKeyboardView.labelFor()`): had no `isLetter()` guard at all, just
  `shifted || capsLock`. Fixed by adding both `surface == InputSurface.LETTERS` and `ch.isLetter()` to the
  condition.

The fix is surface-based, not character-based - it doesn't special-case `π`/`ƒ`, so it also covers any future
symbol/calculator-page character that happens to have a Unicode case mapping. The letters-page `f`→`ƒ`
long-press hint is untouched - case genuinely applies there, same as every other letter hint (`a`→`ä` etc.).
No new test - this is `AdaptKeyService`/`AdaptKeyboardView` glue, an existing documented testing gap.

### Calculator page: `0` key gets a `#` long-press hint
For fields marked as a phone number (dial pads commonly need `#`/`*`).

### New: fields wanting digits auto-open the calculator page
`AdaptKeyService.onStartInput()` previously reset every field unconditionally to `InputSurface.LETTERS`. A
new `initialSurfaceFor(info: EditorInfo?)` inspects `info.inputType and InputType.TYPE_MASK_CLASS` and opens
straight to the calculator page (`InputSurface.SYMBOLS`, page 1) for `TYPE_CLASS_PHONE`, `TYPE_CLASS_NUMBER`,
and `TYPE_CLASS_DATETIME` fields - the three non-text input classes, all of which primarily want digits and
gain nothing from the full alphabetic keyboard, mirroring how stock Android/Gboard show a numeric-style
keyboard for these. `TYPE_CLASS_TEXT` (and no `EditorInfo` at all) still opens on `LETTERS` as before. No new
test - `onStartInput` is service-glue, an existing documented gap; the same pattern already applies to
`capsModeFor()` right next to it.

### Implemented: sign-flip on long-press of the calculator's minus key (v0.8.10)
Originally raised tentatively in §31, then explicitly confirmed and requested. Long-pressing `−` on the
calculator page now flips the sign of the number immediately before the caret (removes a leading
[`SymbolLayout.MINUS_SIGN`] if present, otherwise inserts one) - operating directly on the *committed* text
via `InputConnection`, not composing state, since digits on the calculator page commit immediately rather
than entering a composing token (confirmed during investigation: with `composing` always empty there, this
is the only viable approach).

The minus key carries neither a [Key.hint] nor [Key.alternatives] - unlike every other long-press secondary,
this is an *edit* action, not "commit this text", so routing it through the existing
hint/alternatives-commit pipeline (`commitLongPressSymbol`) would be wrong (it would literally type a `±`
character rather than editing the number). Instead: `SymbolLayout.MINUS_SIGN` (`'−'`, U+2212, not the ASCII
hyphen) is now a public constant; `KeyboardLayout.hasLongPressAction()` gained a dedicated case recognising
this specific key (mirroring how it already special-cases the combined `?123` key); `AdaptKeyService.
handleLongPress()` intercepts it before the generic hint-reading branch and calls a new
`flipSignBeforeCaret()`. The actual text-scanning logic lives in a new pure, unit-tested `SignFlip` object
(`keyboard` package, mirroring the existing `WordBoundary`/`SentenceBoundary` pattern) - `resultFor(before:
CharSequence)` scans back over digits and `,`/`.` (either decimal-separator character, since only the
number's *extent* matters here) to find where the number starts, and returns a `(deleteLength, insertText)`
edit, or null when there's no number directly before the caret. One accepted trade-off, noted rather than
worked around: since the key carries no hint, it gets no corner-glyph visual cue that long-press does
something - acceptable for a power-user bonus gesture the user already knows exists, and forcing one through
the existing hint-is-commit-text machinery would have caused the exact bug this design avoids.

## §32 - D-87 Root-Cause Fixes: Mid-Word Correction (v0.8.11)

Investigated the report that D-62 mid-word live correction "still doesn't activate", with three symptoms:
no suggestions while typing mid-word, the caret jumping to word/sentence end, and whole words being
swallowed. Found and fixed two independent bugs in the same code path, both around `reclaimSurroundingWord()`
(the D-62 mechanism that pulls a word's letters on both sides of the caret into `composing` when a new
composing token starts mid-word). No Robolectric integration test was built for either fix - a realistic
fake `InputConnection` that reproduces windowed `ExtractedText` and batch-edit callback timing is out of
scope for this environment (see below); both fixes were verified by careful manual trace against the
`InputConnection`/`ExtractedText` API contract, plus new pure unit tests for the extracted logic they touch.
**Neither fix has been confirmed on a real device yet** - this environment has no Android emulator (no
hardware virtualization available) and Robolectric cannot exercise real IPC timing.

### Bug 1: `ExtractedText.selectionStart` used as an absolute position
`reclaimSurroundingWord()` computed the mid-word composing anchor from
`ic.getExtractedText(ExtractedTextRequest(), 0)?.selectionStart` directly. Per the `ExtractedText` API
contract, `selectionStart` is relative to the *extracted chunk* (`ExtractedText.text`), not the whole
document - `ExtractedText.startOffset` must be added to get an absolute document position. Using
`selectionStart` alone happens to work in short/simple fields, where `startOffset` is always 0, but silently
miscomputes the anchor - and with it every later "is this our own edit" check in `onUpdateSelection` - in any
field long or paginated enough for the framework to window the extraction instead of returning the whole
document. This explains why the bug was previously reported as intermittent rather than reliably
reproducible (§27/§28's D-84 fix addressed a real, different bug - a `tokenContextBefore` duplication that
silenced suggestions entirely - but did not touch this anchor computation).

Fixed by extracting the arithmetic into a new pure, unit-tested `ComposingAnchor.resolve(extractedStartOffset,
extractedSelectionStart, reclaimedBeforeLength)` object (top-level package, mirroring `WordBoundary`/
`SentenceBoundary`), and using it in `reclaimSurroundingWord()`. Confirmed via `grep` this was the only
occurrence of the `getExtractedText()...selectionStart` pattern in `AdaptKeyService.kt`.

### Bug 2: the reclaim's `deleteSurroundingText()` was not batched with the composing update
`reclaimSurroundingWord()` calls `ic.deleteSurroundingText()` directly (to pull the reclaimed letters out of
the real, committed text) as a standalone `InputConnection` call, *before* `insertComposingChar()` and
`updateComposing()` run - and `updateComposing()`'s own `beginBatchEdit()`/`endBatchEdit()` wraps only its
own `setComposingText()`/`setSelection()` pair, not the earlier delete. Since `InputConnection` calls are
typically dispatched asynchronously to the app and `onUpdateSelection` callbacks arrive back on a later
message-loop turn, this unbatched delete can produce its own, independent `onUpdateSelection` callback that
arrives *after* `composing`/`composingAnchor`/`composingCursor` have already advanced past it (both the
delete and the later composing update happen synchronously within the same `handleKey()` call, well before
either callback fires) - reporting a stale, delete-time-only cursor position that no longer matches
`onUpdateSelection`'s `ownCursor` expectation. The mismatch trips the "not our edit" branch, which calls
`finishComposingText()` and `clearComposing()` - wiping the token this very keystroke was building - even
though nothing the user did caused it. Unlike Bug 1, this is timing-dependent rather than field-length
dependent, so it plausibly explains a more reliably-reproducible version of all three symptoms: suggestions
that flash and vanish (or never appear) as the wipe races the suggestion refresh, the caret landing wherever
the stale callback's cursor position happened to be (which reads as "jumping to word/sentence end"), and
letters already reclaimed into `composing` disappearing along with it (reads as "words being swallowed").

Fixed by wrapping the whole reclaim-through-`updateComposing()` sequence in one outer
`beginBatchEdit()`/`endBatchEdit()` at both call sites that invoke `reclaimSurroundingWord()` - the ordinary
character path in `handleKey()`'s `KeyCode.CHAR` branch, and the long-press-letter path in
`appendLongPressLetter()`. Nesting batch edits is an explicitly supported Android pattern (the app only
dispatches once the outermost `endBatchEdit()` completes), so this composes safely with `updateComposing()`'s
own conditional inner batch. The app now coalesces the delete and the composing update into a single
transaction and reports only the final, consistent selection - eliminating the intermediate stale callback
entirely.

## §33 - Page 2 Row 4: `°`/`-` Swap, Phone-Field Auto-Select Confirmed (v0.8.12)

### Page 2 row 4: `°` moved ahead of `-`
`CATCHALL_LETTER_HINTS` reordered from `# ' ^ - ° + × ÷ * ±` to `# ' ^ ° - + × ÷ * ±`. `-` keeps its `_`
long-press alt regardless of position.

### Confirmed: phone-number (and similar) fields already auto-select the calculator page
Answering the question raised alongside this round's request: yes, and it was already implemented - §31
(v0.8.9) added `AdaptKeyService.initialSurfaceFor(info: EditorInfo?)`, called from `onStartInput()`, which
inspects `info.inputType and InputType.TYPE_MASK_CLASS` and opens straight to the calculator page
(`InputSurface.SYMBOLS`, page 1) for `TYPE_CLASS_PHONE`, `TYPE_CLASS_NUMBER`, and `TYPE_CLASS_DATETIME`
fields - covering phone-number fields and any other field an app marks as one of these non-text classes.
`TYPE_CLASS_TEXT` (and fields with no `EditorInfo` at all) still open on `LETTERS` as before. No action
needed this round; nothing changed.

## §34 - Letters-Page `p` Key: Reversed Alternatives Order (v0.8.13)

### Bug fixed: the `p` key's Greek-letter popup grew away from `π`, not towards it
`p` is the last key of the letters page's top row (`qwertzuiop`), hard against the keyboard's right edge.
`AdaptKeyboardView.openPopup()`/`HorizontalLongPressPopup.rowLeft()` centre the popup row over the pressed
key's stem, then clamp the whole row to stay on screen - for an edge key like `p`, the ideal (centred)
position always overflows off-screen, so the row gets clamped and visually grows leftward instead. Since the
row is always drawn in `Key.alternatives` list order (left to right, unconditionally), and `π` sat first in
`KeyboardLayout.PI_ALTERNATIVES` (`π α β γ δ λ ω`), clamping put `π` itself - the character actually printed
on the key - at the far end of the row, away from the finger, with the least-relevant alternative (`ω`)
right next to it.

Fixed by reversing the order specifically at the letters-page call site (`topRowKey()`:
`alternatives = PI_ALTERNATIVES.reversed()`), not the shared `PI_ALTERNATIVES` constant itself - the
calculator page's `π` key ([SymbolLayout], `CALC_ROW1_SYMBOLS = "()°√π~&|"`) sits roughly mid-row there, not
against an edge, so it doesn't have this problem and was left untouched. Pre-selection (which cell is
highlighted the instant the popup opens) is unaffected either way - `AdaptKeyboardView.preSelectedIndexFor()`
looks up the key's own character's *current* index in whatever list it's given, so it finds `π` correctly
regardless of order.

### Design question: could the layout itself sort outward from the pivot automatically?
Raised alongside this fix: instead of manually reversing an alternatives list per key based on where it
happens to land on screen, could `AdaptKeyboardView`'s popup layer sort/display outward from the anchor
automatically, so authors never have to think about screen position when writing a `Key.alternatives` list?

In principle yes: the row is already clamped for edge overflow (`rowLeft()`), so the same code already knows
whether a row got pushed away from its ideal (unclamped) centred position, and in which direction. The
missing piece is a de-coupling between *logical* order (as authored - "the base character first, then
alternatives in relevance order") and *visual* order (as drawn) that the view could resolve by mirroring the
list whenever the row is clamped opposite its usual growth direction. This would remove one recurring class
of authoring mistake (an edge key's alternatives silently facing the wrong way) at the cost of a real design
change: `drawLongPressPopup()`/`updatePopupSelection()`/`commitPopupSelection()` all currently index directly
into `popupAlternatives` in stored order, so "visual order" and "list order" would need to diverge somewhere
in that pipeline, and every existing edge-key alternatives list (there is currently exactly one, `p`) would
need re-auditing once the auto-mirroring lands, since the manual reversal done here would then double-flip.
**Not implemented this round** - worth doing if a second or third edge-anchored multi-alternative key shows
up and this manual-reversal-per-key pattern starts repeating; for a single instance, the explicit
`.reversed()` at the one call site that needs it (with the comment explaining why) is the smaller, more
inspectable change.

## §35 - Bug Fixed: Greek Math-Symbol Popup Got Auto-Capitalised (v0.8.14)

### Bug fixed: picking π/α/β/γ/δ/λ/ω from the `p` key's popup could insert an upper-case letter
Reported: inserting a Greek letter as a math symbol (not while in Greek input mode) at, say, the start of a
line produced the upper-case form (`Π`) even though the key itself clearly shows the lower-case glyph - and
for a mathematical constant/variable, case is meaningful (`Π` and `π` are different symbols), unlike an
ordinary word where auto-capitalising the first letter is invisible/correct.

Root cause: `AdaptKeyService.commitLongPressSymbol()` decided whether a picked long-press alternative should
extend the composing word (and so become subject to §6 capitalisation / Shift-arming, via
`appendLongPressLetter()`) purely by `symbol.all { it.isLetter() }`. That check was written for G-01 - a
Greek accented vowel picked while genuinely typing Greek text, where extending the word and following normal
case rules is exactly right - but the same `isLetter()` test is equally true for π/α/β/γ/δ/λ/ω, D-99's
Greek-letter popup added to the **Latin** keyboard's `p` key as a shortcut for mathematical symbols. Nothing
in the check distinguished "genuine language letter" from "a letter borrowed from a different script purely
as a symbol".

Fixed with a new pure, unit-tested `AlternativeScript.extendsWord(symbol, activeLanguageIsGreek)` (`keyboard`
package): true only when every character is a letter *and* either the keyboard is actually in Greek mode
(§28's `activeLanguage == Language.GREEK`, only ever true while `GreekLayout` - not `KeyboardLayout` - is on
screen, so this can never misfire the other way), or the letter is not itself Greek script
(`Character.UnicodeScript.of(...) == GREEK`, via the JDK's own Unicode script table - no hand-maintained
character list to keep in sync). `commitLongPressSymbol()` now calls this instead of the bare `isLetter()`
check. A German umlaut (`ä`, `ö`, `ü`) is unaffected either way - it is Latin script, so the Greek-script
exclusion never applies to it, and it keeps extending the word with normal case behaviour as before.

When the check now returns false, the symbol routes through `finalizeAndCommit()` instead - the same path
plain punctuation and the calculator page's non-letter symbols already use. This finalises whatever word was
in progress first (autocorrect/capitalisation apply to *that* normally), then commits the Greek symbol via
`ic.commitText(delimiter, 1)` - which every branch of `finalizeAndCommit()` already does completely verbatim,
with no case transform ever applied to the delimiter argument itself. This also fixes a second, related gap
noted only in passing here: previously such a symbol would have been folded into the *same* composing token
as whatever was typed around it (subject to autocorrect/suggestions as if it were part of a word); now it
commits standalone, matching how the calculator page's own π key (§31, direct tap) already behaves.

## §36 - Calculator Row 5: `0` Centred Under `2` (v0.8.15)

### Row 5 rearranged: `ABC` narrow under `1`, `0` centred (widened), decimal separator narrow under `3`
Raised via a friend's suggestion that `0` belongs centred under `2`, matching a phone dial pad / physical
numpad. Previous order was `0` (under `1`) `,` (under `2`) `ABC` (under `3`); now `ABC` `0` `,`, with `ABC`
and the decimal separator both narrowed to the same weight and `0` widened to fill the space between them.

This is a pure [Key.weight] change, not a structural one - [AdaptKeyboardView] already lays out each row as a
plain proportional (flexbox-style) split (`keyWidth = unit * key.weight`, `unit = usableWidth / totalWeight`
per row), so no per-key pixel math or hand-hacked positioning was needed. The centring itself rests on one
fact worth recording: the sum of two *equal* flanking weights always lands the cell between them on the
row's own centre, regardless of what that shared weight actually is - so the exact value chosen
(`ABC_DECIMAL_WEIGHT = 0.5f`, `SymbolLayout`) is somewhat arbitrary and could be tuned freely later without
re-deriving the centring; only the *equality* of the two flanking weights matters. `0`'s own weight is
computed as `CALC_DIGIT_COLUMN_WEIGHT - 2 * ABC_DECIMAL_WEIGHT` (`3 - 2*0.5 = 2`) rather than hardcoded, so
changing `ABC_DECIMAL_WEIGHT` alone keeps the row's total consistent with rows 2-4's three digit/operator
cells (still 3, so the right-hand column stays aligned) without a second edit. `ABC`'s existing reserved-slot
hiding (D-59/D-93/§29-follow-up: drawn as empty space, inert, when the combined `?123` key setting is off)
is unaffected by its new narrower weight - that mechanism only ever cared about the key's own slot staying
present, never its width.

## §37 - Row 5 Retuned: `0` Less Oversized (v0.8.16)

§36's `ABC_DECIMAL_WEIGHT = 0.5f` made `0` twice as wide as an ordinary digit key (weight `2f` against their
`1f`) - reported as visibly too large. Retuned to `0.75f`, giving `0` a more modest `1.5f` (1.5x) width
instead. As noted in §36, only one constant needed changing - `0`'s own weight is derived
(`CALC_DIGIT_COLUMN_WEIGHT - 2 * ABC_DECIMAL_WEIGHT`), and the centring under `2` holds for any value as long
as `ABC` and the decimal separator stay equal to each other, which they still do.

## §38 - D-36 Clipboard Paste Reworked: Native Paste Action, Sensitive-Only Auto-Clear (v0.8.17)

### Reported: the D-36 paste chip's `commitText()` approach loses app-specific paste behaviour
Pasting a multi-line list into Google Keep's note editor via the D-36 direct-paste chip produced a single
multi-line entry; pasting the same text via the field's own long-press "Paste" produced one list entry per
line. Investigated and confirmed: this is a real difference in *how* text arrives at the target app, not a
Keep bug. `InputConnection.commitText()` (D-36/D-60's approach since it landed) just inserts characters -
indistinguishable, from the target app's point of view, from someone typing very fast including newlines.
Keep's per-line list-entry splitting is almost certainly wired to its own native paste-handling code path
(the one `InputConnection.performContextMenuAction(android.R.id.paste)` would trigger, since that asks the
target editor to run its own "Paste" context-menu action - the same thing a long-press "Paste" runs), not to
"multi-line text arrived from somewhere." A related ask - pasting into a Keep list-note's *title* field
splitting the first line into the title and the rest into list entries - is confirmed **not achievable from
the keyboard side at all**: an IME's entire view of an app is "there is a text field, I can commit text into
it" - it has no channel to know an app maintains multiple separate UI elements (title + individually-rendered
list rows) a paste should be distributed across. That would need to be built into Keep's own paste handler.

### Reverted to `performContextMenuAction(paste)`, with the D-36/D-60 concerns addressed differently
D-36/D-60 originally moved *away* from `performContextMenuAction(paste)` for two reasons, both still true as
general facts, addressed (or knowingly accepted) differently now:

1. **"Not honoured by every field"** - in some fields/apps, the call silently does nothing. **Not fixed** -
   there is no reliable way for an IME to detect after the fact whether a native paste actually happened, so
   there is no way to build an automatic fallback to `commitText()`. Accepted as a known, open risk in
   exchange for the Notes-app benefit and (below) no longer wiping ordinary clipboard content; the user will
   report back if it causes trouble in practice.
2. **"Races the immediate `clearClipboard()`"** - the old code cleared the clipboard synchronously right
   after firing the paste action, but `performContextMenuAction()` triggers the target app's *own*
   asynchronous paste handling, which often had not actually read the clipboard yet by the time it was
   wiped - the original reason paste appeared to silently fail in several apps. **Fixed**: the clear is now
   deferred `CLIPBOARD_CLEAR_DELAY_MS` (300 ms) via the service's existing `Handler` (the same
   `postDelayed()` pattern already used for `resortRunnable`) instead of firing synchronously -
   `scheduleClipboardClear()`. 300 ms is generous relative to how quickly a target app's UI thread actually
   processes a plain-text paste (typically single-digit milliseconds), while still keeping a sensitive
   value's exposure window short. Before clearing, the callback re-reads the current clipboard text and
   compares it against what was pasted - if it no longer matches (the user copied something else in the
   meantime), the clear is skipped rather than silently wiping unrelated newer content.

### Also requested and implemented: only ever auto-clear *sensitive* content
Previously every paste cleared the clipboard unconditionally, "so pasted content - especially a password -
does not linger" (the original D-36 rationale) - which also discarded ordinary, harmless clipboard content
the user might have wanted to paste again elsewhere. Now `clearClipboard()` (via `scheduleClipboardClear()`)
only ever runs when the clip is flagged sensitive.

**How "sensitive" is determined** (a question raised alongside this fix): via the clipboard's own
[ClipDescription.EXTRA_IS_SENSITIVE] flag (Android 13/Tiramisu+), set by whichever app *copied* the content
(a password manager or browser flags it when the source field was a password field) - not by inspecting the
clipboard text's own content for password-like patterns, and not by the *current* paste target field's
`inputType`. There is no reliable signal at all pre-Tiramisu, or from a copying app that does not set the
flag. This is almost certainly also how GBoard decides to show a run of bullets instead of a text preview for
clipboard content - the masking has to happen the moment the clip preview is offered, before the user has
even chosen a target field, so it cannot be based on where the text will land. AdaptKey already read this
same flag for the D-36 chip's own bullet-masked preview ([ClipboardPreview]); the check is now factored into
a shared `isSensitiveClip()` used by both the preview and the new auto-clear decision.

## §39-§42 - Batch: Comma Popup, Clipboard Age, Selection Backspace, Gesture-Area Background (v0.8.18)

Four independent, small, low-risk fixes from the same feedback round, batched into one version bump/commit
given their size.

### §39 - Letters page: `/` dropped from the comma key's popup
`KeyboardLayout.COMMA_ALTERNATIVES` was `- , : ; / _`; `/` is redundant now that it also lives on the `7`
key's long-press hint (D-96), so it is dropped: `- , : ; _`.

### §40 - D-36 paste chip no longer offered once the clip is older than 5 minutes
A stale clipboard entry (copied long ago, likely forgotten about) should not keep resurfacing as a paste
suggestion every time a field opens. `ClipDescription.getTimestamp()` (`System.currentTimeMillis()` time
base, available since API 26 - AdaptKey's own minSdk, so no version gate is needed, unlike §38's
Tiramisu-only `EXTRA_IS_SENSITIVE`) gives the clip's creation time. New pure, unit-tested
`ClipboardPreview.isFresh(clipTimestampMs, nowMs)` (`MAX_AGE_MS = 5 * 60 * 1000L`), checked in
`showClipboardChipIfAvailable()` before anything else - a stale clip now shows no chip at all, exactly as if
the clipboard were empty.

### §41 - Bug fixed: Backspace on a text selection deleted a character instead of the selection
Selecting text (long-press + drag) and pressing Backspace deleted the single character before the cursor,
not the selection itself, unlike every other text editor. `handleBackspace()` never checked for an active,
non-collapsed selection at all - `deleteOneBefore()`'s unconditional `getTextBeforeCursor(1, 0)` /
`deleteSurroundingText(1, 0)` doesn't know or care whether a selection exists elsewhere in the field. Fixed
by checking `InputConnection.getSelectedText(0)` first, before either of `handleBackspace()`'s existing
branches (composing vs. not): a non-empty result means a real selection exists, which is replaced via
`commitText("", 1)` - the standard InputConnection idiom for "replace the current selection with nothing",
exactly what any ordinary keypress does to a selection. Any in-progress composing token is finished and
cleared first, since it cannot correspond to the (out-of-band) selected range. No new test - `InputConnection`
selection state is service glue, an existing documented gap.

### §42 - Fixed: the system-gesture-inset area below the keyboard showed whatever was behind the IME window
`AdaptKeyboardView` already paints its own background (`R.color.keyboard_background`) within its own bounds,
but the bottom padding added to `root` (the outer `LinearLayout`) to clear the Android 15 edge-to-edge
gesture-navigation pill sits *outside* every child's bounds - `root` itself had no background, so that strip
let whatever sat behind the IME window show through, looking wrong against the keyboard immediately above it.
Fixed with one line: `root.setBackgroundColor(R.color.keyboard_background)`, matching the keyboard's own.

## §43-§44 - Two Systemic Autocorrect Priority Bugs (v0.8.19)

Two concrete, reported failures ("Hallo" → "Hall" + apparent English-mode switch; "die"/"der" losing to
"due"/"ddr") were investigated and confirmed to share the session's stated strategy: both traced to a real,
generalisable rule defect, not a word-specific quirk - fixed at the rule level, verified against the actual
bundled dictionary/profile assets (not guessed), with regression tests capturing the mechanism rather than
the individual reported words.

### §43 - Bug fixed: a single common word could misclassify as a foreign language
Root cause, confirmed empirically against the bundled `language_profiles.tsv`: `LanguageClassifier.isForeign()`
/`classify()` only gated on raw n-gram *count* (`minNgrams = 8`) before attempting a Cavnar-Trenkle
out-of-place language decision - but a single isolated word's own n-gram sample is too statistically noisy to
trust for this comparison, *regardless of how many n-grams that one word happens to produce*. Empirical sweep
over 27 common German words (single word, no context) found roughly 30% misclassified as foreign: `Bitte` →
Italian, `heute` → Dutch, `Auto` → Italian, `Straße` → Italian, `werden` → Dutch, `trinkst` → English (this
one has 15 n-grams, nearly double the minimum), and `Hallo` → English (confidence only 0.20) - reproducing
the reported bug exactly, including the second-order symptom: once `resolveDict()` re-points `provider`/
`capitalisation`/`dictionaryStore` to English for the token, German capitalisation rules and suggestions stop
applying for it, which reads as "the keyboard switched to English mode" even though the persistent
letters/Greek toggle (`activeLanguage`) never actually changes - there is no third, persisted "English mode".
Every one of the misfiring words classified correctly the moment a second word joined it in the input.

Fixed with a new `minWords` constructor parameter (default 2, independent of `minNgrams`) on
`LanguageClassifier`, checked in both `classify()` and `isForeign()` right after the (unaffected) Greek
script fast-path - a lone word, however long, never reaches the Latin-script ranking at all. 2 new unit tests
demonstrate the mechanism directly: a single word that would unambiguously win a toy English profile
(`"over"`, literally present in the test's training text) is blocked with the default `minWords = 2`, and
correctly wins once paired with a second word.

### §44 - Bug fixed: A-01's "known word" protection had no sense of proportion
Root cause, confirmed against the bundled `dict_de.tsv`: `die` (frequency 889,897) and `der` (1,004,234) are
the two most common words in the corpus, yet autocorrect refused to ever propose them once the composing
token happened to land on `due` (frequency 24 - a rare loanword entry) or `ddr` (4,405 - the GDR
abbreviation) - both of which are themselves real, if obscure, dictionary entries. `DictionarySuggestionProvider.
bestCorrection()`'s A-01 guard (`if (isKnownWord(token)) return null`) is an absolute veto with no concept of
*how* known the word is - a coincidental 24-frequency loanword got exactly the same ironclad protection as a
genuinely common word, permanently shielding it from a single-substitution neighbour nearly 40,000x more
frequent. `u`/`i` are QWERTZ-adjacent keys, so a `due`-for-`die` mistap is a completely ordinary slip this
system should already handle - A-01 was actively preventing it.

Separately confirmed: this is also why the user never noticed `AdaptKeyService.rawCoordinateCorrection()`
(D-39/T-02/T-03) engaging for these cases - it carried its own, independent copy of the exact same absolute
"known word" veto, so it was blocked for precisely the same reason, for precisely the same words. (The
underlying raw-coordinate machinery itself is fine: `OffsetModel` trains continuously from ordinary typing via
`AdaptKeyboardView`'s `offsetModel?.record(...)` on every resolved tap, not only from the K-01 calibration
screen, and clears its warm-up threshold - 20 taps - almost immediately in normal use.)

Fixed by making the veto *relative* instead of absolute: a new `SuggestionProvider.shouldOverrideKnownWord
(word, candidate)` (default `false`, so `StubSuggestionProvider` is unaffected) returns true only when
`candidate` is at least `KNOWN_WORD_OVERRIDE_RATIO` (50x) more frequent than `word` -
`DictionarySuggestionProvider` implements it from the store's real frequencies; `bestCorrection()` and
`rawCoordinateCorrection()` both now consult this single, shared rule instead of each carrying their own
copy of the old absolute check, so the two paths can no longer silently disagree. The ratio is deliberately
extreme - verified against a "Bad" (100) vs. "Bat" (200) pair (only 2x apart) that an ordinary, comparably-
common word pair stays fully protected by A-01 exactly as before; only a genuinely lopsided, near-certain
"this was a coincidence" gap like `due`/`die` (~37,000x) or `ddr`/`der` (~228x) crosses the threshold.

## §45 - Bug Fixed: A-05 Word Splitting Could Chop a Word into Meaningless Fragments (v0.8.20)

### Reported: "meinst" (a normal German verb form) split into "mei" + "St"
Described as a general problem, not a one-off - "die Worttrennung muss wohl erst mit einer höheren Confidence
ausgeführt werden" (word splitting needs a higher confidence bar). Investigated and confirmed systemic, with
a precise, evidence-based root cause rather than a guess.

`TokenRepair.trySplit()` tries two independent strategies - dropping a single "hit letter instead of space"
character (`OVER_SPACE_LETTERS = c/v/b/n/m`, or a T-05-flagged tap), or inserting a space with nothing
dropped - and picks whichever valid candidate scores highest. The missed-space strategy already required the
two halves to be a real, co-occurring bigram (`MIN_SPLIT_BIGRAM`), specifically to stop a typo being cut into
two coincidentally-real dictionary fragments. **The drop-a-character strategy had no such requirement at
all** - only that both halves individually be *some* known dictionary word, however obscure.

`meinst` (m-e-i-n-s-t) has `n` at index 3, an over-space letter - dropping it yields `mei` + `st`. Verified
against the bundled `dict_de.tsv`: `mei` is a real but rare entry (frequency 16 - a proper noun / dialect
word), `st` a common abbreviation (5,939, "Sankt"). Both pass the "is this a known word" check individually,
so the drop candidate was accepted outright - and verified against `bigram_de.tsv`: `mei`+`st` has **zero**
recorded co-occurrences. (`mein`+`st`, the plausible "correct" split, also has zero - `meinst` simply isn't
covered by this dictionary's verb-conjugation entries at all, so the right outcome is no split, not a
different split.) This generalises well beyond the one reported word: any token containing `c`/`v`/`b`/`n`/`m`
whose two sides happen to both exist somewhere in the dictionary - true of an enormous number of ordinary
German words, given how many contain those letters - was vulnerable to being cut apart on zero co-occurrence
evidence, exactly the "höhere Confidence" gap the user diagnosed.

Fixed by applying the same bigram-co-occurrence filter to *both* strategies' candidates uniformly, instead of
only the missed-space one, before picking the highest-scoring survivor - removing the special case rather
than patching around the one reported word. Confirmed the fix doesn't regress the strategy's own motivating
case (`und<c>das` -> `und` + `das`, a real, extremely common bigram - 10,165 occurrences in the bundled
corpus) or the existing `immernoch` -> `immer noch` comparison test (the weaker `immer`+`och` drop candidate
now gets filtered out outright rather than merely losing a score comparison - same outcome, tighter
mechanism). New regression test reproduces the exact reported failure (`mei`/`st` known but never
co-occurring) rather than encoding the specific word as a special case.

## §46 - Mid-Word Live Correction: Third Investigation Pass, No Further Code Defect Found (v0.8.21)

Reported: mid-word live correction (D-62/D-87) "just doesn't work at all" even after §32's two D-87 fixes
this session (the `ExtractedText.startOffset` anchor bug and the unbatched-`deleteSurroundingText` race).
Asked to test intensively.

Re-audited the entire path end to end a third time - `WordExtent.reclaim()`, `reclaimSurroundingWord()`,
`insertComposingChar()`, `updateComposing()` (both its conditional inner batch and the outer batch §32 added),
`deleteComposingChar()`'s two branches (mid-composing and pulling in a real character before the reclaim),
and `onUpdateSelection()`'s `ownEdit` check - specifically looking for a *third* defect in the same family.
Found none: every read (`getTextBeforeCursor`/`getTextAfterCursor`) inside a batch edit is fine (batching only
governs coalesced writes, per the `InputConnection` contract, not read semantics); every write that changes
`composingAnchor` is either accompanied by a matching `InputConnection` call in the same batch or doesn't need
one; the single-operation batches in `deleteComposingChar()`'s `composingCursor == 0` branch are self-
consistent (the anchor adjustment and the one `deleteSurroundingText()` call move the cursor by the same
amount, so no callback can observe a mismatched state).

**Could not build a genuine automated regression test for this area.** Checked directly: Robolectric 4.14.1's
`shadows-framework` jar (inspected via `unzip -l`) ships a shadow for `InputMethodManager` only - there is no
shadow for `InputMethodService` (the class `AdaptKeyService` extends) or for `BaseInputConnection`/
`InputConnection` at all. Without either, there is no way to exercise the real, timing-sensitive callback
sequence (`beginBatchEdit`/`endBatchEdit`/`onUpdateSelection` ordering) that the two already-fixed D-87 bugs
specifically hinged on - a hand-rolled fake `InputConnection` would not reproduce the actual async Binder
callback timing that caused those bugs in the first place, so it would only provide false confidence, not
real verification. This is a genuine tooling gap in this environment (no Android emulator either - see prior
entries), not a shortcut.

**Not yet resolved; needs real-device confirmation to make further progress.** If the symptom persists on the
latest build, the most useful next report would specify: which of the three original symptoms still
reproduces (no suggestions / caret jump / word swallowed - possibly not all three anymore), and which app/
field it was tested in (a hint at whether `ExtractedText` windowing, i.e. non-zero `startOffset`, is actually
involved for that specific field, which the first D-87 fix targeted).

## §47 - Backlog: Live Colour Preview of a Pending A-05 Split

Requested feature, not implemented: a token that is about to be A-05-split when finalised should already show
that outcome live, while still composing - both resulting word-parts (minus whatever character would be
dropped as the mistaken "separator") coloured in the existing recognised-word green (the C-04/S-05 highlight,
setting-gated), exactly as a single whole recognised word already gets coloured before it autocorrects
(`shouldHighlightComposing()`). So a user typing a token that will end up split sees the split coming, not
just the final result after committing.

Not a small tweak - two real gaps to close:

1. **Split detection currently only runs at commit time.** `TokenRepair.trySplit()` is called from
   `finalizeAndCommit()`, once, when a delimiter is typed - never while the token is still composing. A live
   preview needs a "would this currently split if finalised right now" check run continuously (most likely
   from `refreshSuggestions()` or `updateComposing()`, alongside the existing per-keystroke work), not just
   once at the end. `TokenRepair.trySplit()`'s own logic is already reusable for this (it needs no state
   beyond the token itself, `spaceAmbiguousIndices` and `previousWord`, all already available where
   `refreshSuggestions()` runs) - the gap is purely that nothing currently *calls* it early.
2. **The composing text currently gets one colour span for the whole token, or none.** `updateComposing()`
   applies a single `ForegroundColorSpan` over the entire composing `SpannableString` when
   `shouldHighlightComposing()` says so. A split preview needs *two* separate spans - one over each half - and
   the dropped character (drop-strategy split) or the boundary itself (missed-space split) left uncoloured, so
   the visual matches "these two pieces, not this one word". `updateComposing()`'s `SpannableString` branch
   would need to switch from "colour everything" to "colour these two ranges" when a split preview is active,
   falling back to the existing single-span behaviour otherwise.

Suggested shape (not committed to): a new pure function, e.g. `SplitPreview.previewFor(token, ...): SplitResult?`
(thin wrapper reusing `TokenRepair.trySplit()`), called from `refreshSuggestions()`, its result cached
alongside the existing suggestion state so `updateComposing()` can read it without recomputing; `updateComposing()`
then builds either the existing single span or two spans depending on whether a preview is present. Interacts
with §35's Greek math-symbol fix and D-62's mid-word reclaim only in that both also touch composing-text
spans - worth double-checking span ranges stay correct when a reclaimed "after" fragment is also present.

## §48 - Backlog: Swipe-Up Settings Row (Gear + Emoji Button)

Requested feature, not implemented - resolves an existing backlog item (§26) with a concrete design:

- **Trigger:** an upward swipe anywhere on the keyboard (mirroring G-03's existing "downward swipe anywhere
  dismisses the keyboard" - `GestureAction.DISMISS_KEYBOARD`) reveals a new row above the keyboard.
- **Row contents:** the emoji button at the left edge, a settings gear icon at the right edge that opens the
  app's settings directly (`launchFromKeyboard`, the same mechanism already used for the calibration/model-
  import screens from onboarding).
- **Animation:** the row slides up into view, but is clipped at its own bottom edge against the top of the
  existing keyboard - it should read as emerging *from* the keyboard, not sliding in over it. The existing
  D-86 "growing into more rows resizes right away" handling (`rebuildRows()`/the slide-direction logic from
  D-94's fix) is the closest existing precedent for a keyboard-height change with animation and is the
  natural place to extend, though this is a genuinely new row *above* the keyboard rather than a change in
  row count within it, so it may need its own transition rather than reusing that path directly.
- **Dismiss interaction, layered on G-03:** a downward swipe while the row is showing animates the row away
  (closing it), *not* the whole keyboard. Only a *second* downward swipe - now that the row is already closed
  - falls through to the existing G-03 dismiss-keyboard behaviour. `GestureAction` would need a new value
  (e.g. `CLOSE_SETTINGS_ROW` or similar) that `handleSwipe()`'s downward-swipe handling checks for before
  falling back to `DISMISS_KEYBOARD`, gated on whether the row is currently open.
- **Settings consequence:** the standalone `AdaptSettings.emojiPanelEnabled` setting (and its corresponding UI
  toggle) can be retired - the emoji button's visibility becomes governed solely by the existing
  `symbolKeyEnabled` (?123/ABC key) setting, removing the dependency `PanelNavigation`/`AdaptKeyboardView`
  currently have on `emojiPanelEnabled` for deciding what the combined key does (L-03: "opens the emoji panel
  from the letter view, or the ?123 layer when the emoji panel is disabled"). Worth checking whether the
  combined key's dual-purpose behaviour still makes sense once the emoji button has its own dedicated,
  always-reachable home in the new row, or whether it simplifies to "always ?123" once this lands.

## §49 - §47 Implemented: Live Colour Preview of a Pending A-05 Split

Implements §47. Two deliberate deviations from that section's "suggested shape", both root-caused against
the actual call sites rather than guessed:

1. **No caching between `refreshSuggestions()` and `updateComposing()`.** §47 suggested computing the split
   preview in `refreshSuggestions()` and caching it for `updateComposing()` to read. Every call site
   (the `CHAR` key handler, `appendLongPressLetter()`, `deleteComposingChar()`) calls `updateComposing(ic)`
   *before* `refreshSuggestions()` for the same keystroke - a cache written by the latter would always be
   one keystroke stale when the former reads it. Instead, the new `AdaptKeyService.splitPreview(ic, text)`
   calls `tokenRepair.trySplit()` directly from inside `updateComposing()`, mirroring how
   `shouldHighlightComposing()` already computes its own answer inline rather than reading a cached one. This
   costs the same per-keystroke dictionary queries `trySplit()` already pays once at commit time
   (`finalizeAndCommit()`), now paid continuously while composing - bounded by token length, same order of
   magnitude as the lookups `refreshSuggestions()` already does every keystroke, so no new caching
   infrastructure was warranted.
2. **No new `SplitPreview` wrapper type.** §47 sketched a `SplitPreview.previewFor(...)` function reusing
   `TokenRepair.trySplit()`. `trySplit()` was reusable as-is; the only real gap was the two colour-span
   ranges within the *original* token, which needed the strategy (drop-a-character vs. missed-space) to
   know whether a one-character gap sits between the halves. Rather than have `trySplit()` track and return
   which strategy won, `SplitResult.spanRanges(token)` recovers it from length arithmetic alone: a
   drop-strategy split's `left.length + right.length` is always exactly `token.length - 1` (one dropped
   character), a missed-space split's is exactly `token.length` (contiguous) - see
   [TokenRepair.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/dictionary/TokenRepair.kt). No new file,
   tested directly on `SplitResult`.

`updateComposing()`'s single-span branch is now the `else` of a new `splitPreview(ic, text) != null` check;
the split-preview branch applies two `ForegroundColorSpan`s from `SplitResult.spanRanges()`. `splitPreview()`
is gated exactly like `shouldHighlightComposing()` (`config.highlightEnabled`, and D-26's mid-word rule) -
both now share the extracted `isEditingMidWord(ic)` helper instead of duplicating the "letter immediately
after the cursor" check. Mutual exclusivity between the split preview and the existing single-word highlight
falls out for free: `trySplit()` already returns null whenever the whole token is itself a known word, which
is `shouldHighlightComposing()`'s own trigger condition.

## §50 - §48 Implemented: Swipe-Up Settings Row (Gear + Emoji Button)

Implements §48. Follows the suggested shape's gesture design closely (an upward swipe anywhere but the
combined key opens the row via `KeyGesture`/`GestureAction.OPEN_SETTINGS_ROW`; a downward swipe while it is
open closes it first, gated in `AdaptKeyService.handleSwipe()` before falling back to `DISMISS_KEYBOARD`),
but resolves the two points §48 left open, and lands the animation differently from the D-86 precedent it
flagged as a possible mismatch:

1. **No new `CLOSE_SETTINGS_ROW` `GestureAction`.** `KeyGesture.resolve()` is a pure function with no row-
   open state to gate on - it always reports the plain `DISMISS_KEYBOARD` for a downward swipe, exactly as
   before. `AdaptKeyService.handleSwipe()`'s existing `DISMISS_KEYBOARD` branch re-routes to
   `closeSettingsRow()` when `settingsRow?.isOpen == true`, and only falls through to `requestHideSelf(0)`
   otherwise - the state check lives where the state actually is, without inventing an action value
   `KeyGesture` itself never produces.
2. **The combined `?123`/emoji key's dual purpose is retired - it is now always a plain `?123` toggle.**
   §48 flagged this as worth checking once the emoji button has its own dedicated home; keeping the old
   D-18 behaviour would have needed a replacement setting for something §48's own instructions retire
   outright (`emojiPanelEnabled`), so the combined key drops its emoji affordance entirely.
   `PanelNavigation.onCombinedKeyTap()` lost its `emojiEnabled` parameter (its body is now exactly D-18's
   old "disabled" branch, unconditionally); `AdaptKeyboardView` lost its own `emojiEnabled` field, and its
   three call sites (label, corner-hint suppression, hidden-slot check) collapsed to the `?123` case
   unconditionally. `emojiPanelEnabled` is removed end to end - `AdaptSettings`, `SettingsMapper`,
   `SettingsStore` (`KEY_EMOJI_PANEL`), and the `settings_preferences.xml` toggle (all three locale
   `strings.xml` variants updated to match) - and the settings row's emoji button visibility is now driven
   by `symbolKeyEnabled` instead, per §48's own instruction.

**The animation does not reuse `AdaptKeyboardView`'s D-86/D-94 row-growth machinery**, confirming §48's own
suspicion that it might not transfer directly: that machinery lives inside `AdaptKeyboardView`'s self-drawn
Canvas engine and animates *row count within the same view*, not a second, independent view stacked above
it. The new `SettingsRowView` ([SettingsRowView.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/SettingsRowView.kt))
is instead an ordinary `FrameLayout` inserted into `AdaptKeyService`'s root `LinearLayout` between the
suggestion bar and the keyboard container. `open()`/`close()` jump the reserved layout space to its target
height immediately (still the D-86 precedent in spirit - growing resizes right away, so nothing waits for
space) and then animate only the row's *inner content* via `translationY`, clipped to the row's own bounds
by a plain `ViewGroup`'s default `clipChildren` - no explicit `clipBounds` needed. This reads as the content
sliding up out of / back down into the space right above the keyboard, matching "clipped at its own bottom
edge against the top of the keyboard, emerging from it" without needing a bespoke transition class.

Both row buttons close the row before acting (`openEmojiPanelFromSettingsRow()` /
`openSettingsAppFromSettingsRow()` in `AdaptKeyService.kt`), a deliberate "tap an item, the menu closes"
UX call not spelled out in §48; `SettingsRowView.closeImmediately()` (no animation) resets the row on every
fresh `onStartInputView()`, so a row left open when the keyboard was dismissed some other way (e.g. the
Home button, not a down-swipe) never carries over into the next field.

Pure logic (`KeyGesture`/`PanelNavigation`) is unit-tested (`KeyGestureTest`, `PanelNavigationTest`);
`SettingsRowView` itself is Android view/animation glue with no decision logic of its own beyond what those
two already cover, left untested per this project's established, documented limitation (no emulator/device
in this environment).

## §51 - §48 Refined: Row Position + Unconditional Emoji Button (User Feedback)

Two corrections to §50's implementation, both direct user feedback after trying the built feature (device
build, not this environment's own testing - confirms §50's row-auto-closes-on-tap design call, made without
being spelled out in §48, was wanted as built):

1. **The settings row now sits above the suggestion bar, not between it and the keyboard.** §48's own text
   ("clipped at its own bottom edge against the top of the existing keyboard... emerging from the keyboard")
   was itself just the implementer's working assumption for "a new row above the keyboard" at spec-writing
   time, not a constraint anyone had weighed against alternatives - the user tried the shipped placement and
   asked for the row to be the topmost element instead, above the word-suggestion row. No technical reason
   favours the old placement: `SettingsRowView.open()`/`close()` reserve their own height and animate their
   own content via `translationY` regardless of where they sit in `AdaptKeyService`'s root `LinearLayout` -
   the reveal animation is entirely self-contained and position-independent. The only change is
   `root.addView()` order (`onboardingView`, `row`, `bar`, `container`) in `onCreateInputView()`; the
   "emerges from the keyboard" framing in `SettingsRowView`'s KDoc no longer quite fits (it now emerges from
   the very top of the input view, with the suggestion bar below it, not from the keyboard's edge) and was
   reworded rather than left stale.
2. **The row's emoji button is now unconditionally visible - `symbolKeyEnabled` no longer gates it.**
   §48's own text said the button's visibility "should run exclusively via `AdaptSettings.symbolKeyEnabled`"
   - implemented that way in §50 (`SettingsRowView.emojiButtonEnabled`, wired from `applySettings()`) - but
   the user reconsidered once they saw it built: with the emoji button now living in its own dedicated row
   (not the combined keyboard key `symbolKeyEnabled` actually governs), tying its visibility to that
   unrelated setting no longer made sense to them, and it is dropped entirely. `emojiButtonEnabled` is
   removed from `SettingsRowView` along with its `applySettings()` wiring; the button (and `settingsButton`)
   are always shown once the row is open. `AdaptSettings.symbolKeyEnabled`'s doc comment updated to drop the
   now-incorrect claim; the `d59_summary` string (all three locales) reverted to no longer mention the
   settings row.

No spec-writing lesson beyond the usual one: flavour text describing *how something should look* (§48's
"emerging from the keyboard", "exclusively via symbolKeyEnabled") is a proposal to validate against the
real, built thing, not a requirement to defend once it turns out the user pictured something else once they
could actually try it.

## §52 - Why the Emoji Button Needs No Setting (User Rationale for §51)

§51 dropped `SettingsRowView`'s emoji-button gate on `symbolKeyEnabled` as a user correction; this section
records the actual *why* behind it, as user-supplied reasoning, not just "the user asked" - so a future
reader (or session) does not misread the empty settings screen slot as an oversight or re-introduce a gate.

**Why the old `emojiPanelEnabled` toggle (D-18) existed at all:** the combined `?123`/emoji key sat on the
main keyboard, permanently occupying one of its limited key slots (L-03), while the emoji panel itself is
low-value to begin with - most messaging apps ship their own emoji picker, so AdaptKey's is rarely the one
actually used. That is a real space/value trade-off worth a toggle: a user who never opens AdaptKey's emoji
panel could reclaim the slot for `?123` alone instead.

**Why that trade-off no longer exists once the emoji button lives in the §48 settings row:** the row is
reached by an upward swipe and takes no permanent keyboard real estate - it exists only while open. There is
no scarce slot to reclaim, so the original reason for a toggle is gone; gating the row's emoji button behind
a setting anyway would just be an unnecessary extra decision for the user with no actual trade-off behind it.
Confirms `emojiButtonEnabled` (§51) was correctly removed outright, not merely defaulted differently, and
that no replacement setting of any kind should be reintroduced later without this same trade-off returning.

**Consequence for the combined key:** `symbolKeyEnabled` (D-59, still the one setting hiding the combined
`?123` key and reserving its slot) is now the *only* thing the main keyboard's combined key depends on -
confirmed in code, not just in this write-up: `AdaptKeyboardView.isHiddenKey()` gates the `SYMBOL` key on
`!symbolKeyEnabled` alone (no `emojiEnabled` counterpart left, per §51), and `PanelNavigation.onCombinedKeyTap()`
takes no emoji-related parameter at all (per §50). `symbolKeyEnabled` itself is unaffected by any of this and
remains exactly as documented (D-59) - it governs the combined key's own numeric/symbol function, which is
an entirely separate concern from the emoji button's now-settingless existence in the row.

## §53 - D-103 / D-104: Trig-Function and Angle-Unit Keys on the Calculator Page (v0.8.26)

Confirmed D-95 (§27's "settings-shortcut key somewhere on the keyboard" idea) is resolved by §48's swipe-up
settings row (the gear icon) - no separate key needed, closing that backlog item without further work.

### New: `sin` key (D-103) and `deg` key (D-104), appended to calculator row 1
`sin`'s D-01 popup offers `cos`/`tan`/`log` (base included, matching every other multi-alternative key's
convention); `deg`'s single hint offers `rad` - a plain single-hint key like the calculator's `0`→`#`, not a
stateful toggle remembering which unit was last chosen, so either unit is always reachable directly without
first cycling through the other (D-104 explicitly left "two-state toggle vs. its own popup" open; a
hint-based key reuses the existing single-hint mechanism outright rather than adding new toggle state).
Placed at the end of row 1 (`CALC_ROW1_SYMBOLS`'s "everyday symbols not already reachable" row), each given
double weight (`FUNCTION_KEY_WEIGHT = 2f`) relative to that row's single-glyph keys, since a three-letter
label needs more room to stay legible - a starting guess, not verified on a device, easy to retune later
(same approach as §36/§37's row-5 weight tuning).

### New `KeyCode.TEXT`: a key whose tap commits a literal multi-character label, not a single `Key.char`
Neither existing `KeyCode` fit: `CHAR` is hard-limited to one `Char` (`handleKey()`'s `CHAR` branch reads
`key.char ?: return` - a plain tap on a null-char key would be a silent no-op), and every other code is a
fixed control action. Added `KeyCode.TEXT`, whose plain tap commits `key.label` verbatim via
`finalizeAndCommit()` (finalising any in-progress token first, exactly like a punctuation delimiter or the
calculator page's other symbol keys - never word-extending). Four places already generic over "does this key
have a hint/alternatives" needed a small extension to also recognise `TEXT`, not just `CHAR`:
- `KeyboardLayout.hasLongPressAction()` - a `TEXT` key with a hint or 2+ alternatives now schedules a
  long-press exactly like a `CHAR` key does.
- `AdaptKeyboardView.popupAlternativesFor()` - a `TEXT` key's single `hint` is now offered as a one-item
  popup too, not only a `CHAR` key's.
- `AdaptKeyboardView.preSelectedIndexFor()` - falls back to `key.label` (not just `key.char?.toString()`)
  to find the key's own value within its alternatives, so `sin` pre-selects correctly among
  `sin`/`cos`/`tan`/`log` despite having no `Key.char` at all.
- `Key.id` (T-03 personal offset model key) - a bare `code.name` would have collided (`"TEXT"` for both `sin`
  and `deg`, violating the "unique within a single layout" contract); now `"t:$label"` for `TEXT` keys,
  mirroring `CHAR`'s `"c:$char"`.

**A real, non-obvious bug caught before it shipped:** `cos`/`tan`/`log` are ordinary Latin letters, so
`AlternativeScript.extendsWord()` (§35's Greek-vs-symbol guard) would have judged them "genuine language
text" and routed them through `appendLongPressLetter()` - extending the composing token and exposing them to
autocorrect/suggestions, exactly the bug §35 fixed for the Greek math-symbol popup, just triggered by a
different, non-Greek route this time. Fixed by threading the popup's originating key's `code` through
`handleLongPressAlternative()`/`commitLongPressSymbol()` (previously the key was discarded at the listener
call site - `{ _, alternative -> ... }`) so a `TEXT` key's alternatives always commit as symbols, regardless
of script - the `AlternativeScript` check is skipped outright for `sourceCode == KeyCode.TEXT`, not merely
one more input to it.

New tests: `SymbolLayoutTest` (row-1 contents, alternatives/hint, `Key.id` uniqueness) and
`KeyboardLayoutTest` (`hasLongPressAction` for both the multi-alternative and single-hint `TEXT` cases). The
`commitLongPressSymbol`/`AlternativeScript`-bypass fix itself is `AdaptKeyService` glue with no independently
testable pure logic beyond what `AlternativeScriptTest` (§35) already covers - not re-tested in isolation.

## §54 - D-88: Feedback When a Suggestion Is Accepted (v0.8.27)

Previously silent and visually unremarkable ("das ist etwas trocken und man kriegt es nicht ausreichend
mit"). Implemented both of D-88's two additions, chosen by the same D-05 key-sound setting the spec named:

**Sound on:** a distinct new "plop" sample, separate from the D-70/D-83/D-85 key-click sample, plays via
`AdaptKeyboardView.playSuggestionAcceptedSound()` - loaded into the same shared `SoundPool` lazily, exactly
like the click sample. No suitable sample existed in this environment (no audio-editing tooling, no network
fetch of third-party assets), so one was synthesised from scratch with a short Python script (stdlib `wave`
module only, no external dependencies or copyrighted material): a 130 ms exponential pitch sweep from 900 Hz
down to 140 Hz with a fast attack and exponential decay envelope - the classic "water-drop" plop shape -
matching the existing sample's format (mono, 16-bit PCM, 48 kHz). Bundled as `res/raw/suggestion_accept.wav`.

**Sound off:** a brief highlight flash on the suggestion bar instead (`SuggestionBarView.flashAccepted()`) -
fades from a peak alpha to fully transparent over 280 ms, painted as a full-bar overlay rect after the chips
(and the G-04 trash zone, when armed) so it is always visible. Uses the same green as the existing C-04/S-05
recognised-word highlight for visual consistency, via a new `flashColor` property the service pushes from the
user's actual configured `highlightColor` (not just its default) in `applySettings()`.

**Found and fixed one real bug in the existing `SoundPool` setup while adding the second sample:**
`SoundPool.setOnLoadCompleteListener()` keeps only the single most-recently-registered listener - the
original `ensureClickSoundLoaded()` installed one checking only `clickSoundId` each time it ran, which a
naive `ensureAcceptSoundLoaded()` following the exact same pattern would have silently replaced with one
checking only `acceptSoundId`, permanently losing the ability to ever detect the *click* sample finishing its
decode if the accept sample's loader happened to run afterward (order-dependent, so not always the same
failure). Fixed by installing one shared listener - checking both ids - once, at `soundPool`'s own lazy
creation, rather than repeatedly inside each `ensure*Loaded()`.

**Trigger points:** `finalizeAndCommit()`'s existing `finalWord != typed` branch (the same check that already
drives the S-05 undo-tracking, so "an accepted correction" is defined identically in both places) and the
`SuggestionController.Kind.NORMAL` suggestion-bar-tap branch (any bar suggestion, even one that happens to
match what was typed, per D-88's "and/or when a suggestion-bar item is tapped").

No new unit tests - `notifySuggestionAccepted()`, `playSuggestionAcceptedSound()` and `flashAccepted()` are
all Android `SoundPool`/animation/view glue with no independently testable pure logic, matching this
project's established, documented limitation (no emulator/device in this environment).

## §55 - Bug Fixed: §54's Flash Overlay Made the Suggestion Bar Solid Black (v0.8.28)

Reported immediately after §54: the suggestion bar was fully black, unreadable, regardless of whether an
acceptance had actually happened yet.

Root cause: `flashPaint` (`SuggestionBarView`) was declared as `Paint(Paint.ANTI_ALIAS_FLAG)` with no initial
colour or alpha. `android.graphics.Paint()` defaults to fully opaque black (`0xFF000000`, alpha 255) until
something sets it otherwise - `draw()`'s `if (flashPaint.alpha > 0)` was therefore true from the very first
frame the view ever drew, long before `flashAccepted()` had ever run once, painting a solid opaque black rect
over the whole bar permanently. `flashAccepted()`'s own animator only ever set `flashPaint`'s colour/alpha
*while actually animating* - there was no code path that ever left it transparent by default.

Fixed with one line: `Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 0 }` at declaration, so the overlay starts
fully transparent and only becomes visible for the duration of an actual flash.

## §56 - D-88 Refined: Sound Sharpened/Louder, Flash Replaced with a Flying Word, Settings Rename (v0.8.29)

Feedback on §54 after real-device testing: the plop sound works well but needed to sound more like a sharp
"Plöpp" than a duller "Blöpp" (and 10% louder); the flat bar-flash felt too minimal, not clearly conveying
"this is being applied to your text"; the animation should play independently of the sound setting, not only
as its fallback. Also: rename the D-59 combined-key setting to `?123/ABC-Taste`.

### Sound: sharper attack, 10% louder
Regenerated `suggestion_accept.wav` with the same synthesis approach (§54): attack shortened from 3 ms to
under 1 ms (a near-instant onset reads as a sharp plosive "P", not a softer "B"), a brief (~6 ms) higher-
frequency transient layered only at the very onset for the extra "crack" a true plosive attack has, and peak
amplitude raised from 0.75 to 0.825 (10% louder). Everything else (the 900→140 Hz pitch-drop shape, envelope,
sample rate/format) is unchanged.

### Animation: replaced the flat flash with a flying, shrinking, fading word
**Technical constraint clarified with the user first, since it changes what "flies into the text" can
concretely mean**: an IME can only draw within its own window - it has no way to draw into the *target app's*
text field, which is a separate window/process entirely. What is achievable, and what was built: the accepted
word rises from the middle of the suggestion bar, shrinking and fading out as it exits upward past the bar's
own top edge (`SuggestionBarView.flyAccepted(word)`, replacing `flashAccepted()`) - close to the visible top
of the keyboard, right below where the actual (external, undrawable) text field continues. Deliberately
subtle per explicit request ("dezent... damit sie nicht nach ein paar Mal nervt"): a modest 34dp rise, 380 ms
duration, DecelerateInterpolator easing. Negative y-values (above the bar's own allocated bounds) reach the
screen because `root` (`AdaptKeyService.onCreateInputView()`) already runs with `clipChildren = false` for
the same reason the existing D-53 long-press popup escapes upward over this same bar - a sibling view's
overflow past its own bounds is not clipped by a `ViewGroup` with `clipChildren = false`.

**Decoupled from sound entirely**, per explicit request: `notifySuggestionAccepted(word)` now always plays
the flight, and *additionally* plays the plop sample when key-press sound (D-05) happens to be on - no
longer an either/or gated on that setting.

### Settings: renamed the D-59 combined-key toggle
`d59_title` (`strings.xml`, all three locales) changed from "?123 key" / "?123-Taste" / "Πλήκτρο ?123" to
"?123/ABC key" / "?123/ABC-Taste" / "Πλήκτρο ?123/ABC" - the key's own two functions (numeric/symbol layer,
and the `ABC` return key it pairs with), not just one of them, now named in the setting itself.

## §57 - §56 Refined: the Flying Word Now Anchors to the Accepted Suggestion's Position (v0.8.30)

Feedback after device testing §56: the overall effect works well and the rise distance is exactly right as
is - but flying from the bar's fixed horizontal centre every time read as too disconnected from the actual
suggestion; flying from (at least approximately) the accepted suggestion's own on-screen position was asked
for instead, with an explicit allowance that an approximate position is fine if precise tracking is awkward.

New `SuggestionBarView.originXFor(word)`: looks up the currently displayed chip whose
`SuggestionController.DisplayItem.word` matches the accepted word (case-insensitively) and returns its
horizontal centre (in the bar's own coordinates, already accounting for `scrollX`); falls back to the bar's
centre - the previous, exact §56 behaviour - only when no matching chip is found, rather than trying harder
to guess a position. This single lookup naturally covers both trigger paths without needing separate
handling: a bar-tap acceptance finds its own just-tapped chip (`notifySuggestionAccepted()` fires before the
bar's next `setItems()` refresh, so the tapped chip is still there to find), and an autocorrect-on-commit
acceptance finds the top suggestion chip whenever the committed word happens to match what was already being
shown there (a common case, since the just-corrected word is usually also the top suggestion) - and falls
back gracefully to centre for whatever cases it doesn't (the bar showing something else, or being empty).

## §58 - Mid-Word Live Correction: a Real Gap Found (Not Just Bugs) - Reclaim on Caret Move (v0.8.31)

Two precise, concrete repro cases pinned down what §32/§46's investigations, focused on bugs in the
*existing* mechanism, had not: a genuine **missing trigger path**, not (only) a defect in the one that
exists.

1. Type a word (e.g. "hatte"), Space, then Backspace (to re-edit it) - the caret ends up touching the word,
   but no suggestion appears. Typing the word without ever pressing Space first works fine.
2. Type "Hatte ich", tap the caret into the middle of "Hatte" - no suggestion appears; at minimum "hätte"
   should be offered.

Traced both to the same root cause: `reclaimSurroundingWord()` (D-62) was **only ever called from a
keystroke handler** (`handleKey()`'s `CHAR` branch, `appendLongPressLetter()`) - never in response to the
caret simply *arriving* at a word with nothing typed. Confirmed by grep: no other call site existed. Case 1's
Backspace removes the space and leaves the caret touching "hatte", but backspace-when-composing-is-empty
(`deleteOneBefore()`) never reclaims anything - it is a plain delete, nothing more. Case 2's tap moves the
caret via a genuine external `onUpdateSelection` callback, which - before this fix - only ever handled the
already-composing case (deciding whether to wipe an in-progress token); the `composing.isEmpty()` branch was
a bare `return`, doing nothing at all. Neither repro case was ever reaching the reclaim mechanism, regardless
of §32's earlier fixes to that mechanism itself - the earlier "no further defect found" conclusion (§46) was
correct as far as it went, but was investigating the wrong question: not "is the reclaim broken" but "does
anything ever call it here at all".

**Fixed**: `onUpdateSelection()`'s `composing.isEmpty()` branch now calls a new `reclaimWordAtCaret()` for a
collapsed selection (`newSelStart == newSelEnd`) - it runs the exact same reclaim-then-render sequence
`handleKey()`'s `CHAR` branch runs when a new token starts mid-word (`captureTokenContext()`,
`resetWordEndShift()`, `reclaimSurroundingWord()`, `updateComposing()`, `refreshSuggestions()`), minus
inserting a character, since nothing was typed - and the same D-87 outer batch edit, for the same reason
(`reclaimSurroundingWord()`'s own `deleteSurroundingText()` must not reach the app as a standalone edit). A
no-op when the caret touches no word at all, exactly like the keystroke-triggered path already was. Self-
limiting against re-entrancy: the moment a reclaim actually happens, `composing` becomes non-empty
synchronously, so any callback this reclaim's own edits generate lands in the already-composing branch, not
back in this one.

**Also answers a separate question raised alongside these repros**: searched the dictionary/capitalisation/
suggestion/language packages for hardcoded word-specific exception logic (e.g. `if (word == "...")`-style
special-casing that should be generalised into real rules, matching this session's §43-§45 fixes). Found
none - the only literal word lists in that area are `SeedData.kt` (a tiny bootstrap dictionary used before
the real one loads) and `StubSuggestionProvider`'s fallback word list (explicitly documented as "the
placeholder" provider) - legitimate bootstrap/fallback *data*, not exception *rules* silently overriding the
general logic.

Verified by careful tracing of the exact call graph (confirmed via grep that no other reclaim call site
existed), not by observing it run at the time, consistent with this project's documented limitation (no
emulator/device in this environment) - but worth flagging as a real, accepted trade-off regardless: this
makes *every* tap into *any* existing word start a live-edit/suggestion session for it, not only a deliberate
"I want to fix this word" tap.

**CONFIRMED WORKING on a real device** (user, same day) - closes out the D-62/D-87 mid-word live correction
saga (D-84's real-but-partial fix, §32's two D-87 fixes, §46's "no further defect found" investigation, and
finally this section's actual missing-trigger fix).

## §59 - Backspace-Hold Repeat: ~9% Faster (v0.8.32)

Requested: the accelerating backspace-on-hold (D-07/D-31/D-56) felt a touch too slow, both for character-wise
and word-wise deletion, by roughly 5-10%.

`BackspaceRepeat`'s three cadence constants reduced by the same round-number proportion (~9%), so the decay
curve's *shape* is unchanged, only its overall speed: `CHAR_START_DELAY_MS` 165ms → 150ms, `CHAR_MIN_DELAY_MS`
50ms → 45ms (both scaled together, since scaling only one would have distorted how quickly the curve reaches
its floor), `WORD_DELAY_MS` 330ms → 300ms. `INITIAL_DELAY_MS` (400ms, the delay before holding starts
repeating at all) is untouched - the request was specifically about the repeat cadence once it starts, not
how long a hold takes to begin repeating. Existing `BackspaceRepeatTest` assertions reference the constants
symbolically, not their literal values, so nothing needed updating there.

**CONFIRMED WORKING** (user, same round as §58's confirmation).

## §60 - D-36 Paste Chip: File Clips No Longer Risk Pasting Binary Garbage as Text (v0.8.33)

Asked: can the keyboard tell when the clipboard holds a whole *file* (e.g. copied in a Files app), rather
than plain copied/selected text - and if so, only ever offer it via Quick Paste when it is itself a text
file (`.txt`, `.md`, and other recognisable text files), showing nothing at all otherwise.

**Yes, detectable without reading anything.** `ClipData.Item` carries either inline text (`getText()`) or a
URI (`getUri()`) pointing at real content - a plain copy (selected text, a typed string) always populates
`text`; a file copy (Files app, sharing a document) populates `uri` instead. The clip's own `ClipDescription`
already declares a MIME type for that URI - reading it needs no I/O at all, so the file-vs-text decision
itself is free. Android's `ClipDescription.hasMimeType()` supports the `"text/*"` wildcard natively (no
hand-rolled MIME parsing needed) - true for `.txt`/`.md`/`.csv`/etc., false for images, PDFs, and anything
unrecognised.

**Implemented**: new `AdaptKeyService.resolveClipboardText(clip, item)`, called from
`showClipboardChipIfAvailable()` in place of the previous direct `item.coerceToText(this)` call. A plain
text item (`item.uri == null`) behaves exactly as before - zero change to the common case. A URI item is only
offered when `clip.description.hasMimeType("text/*")`; anything else returns null, and the chip is suppressed
entirely, exactly as requested ("sollte dann einfach nichts anzeigen"). When it *is* a text file, its content
is read directly via `contentResolver.openInputStream(uri)` (bypassing `coerceToText()`'s own less-
predictable URI-resolution fallbacks), capped at `CLIPBOARD_FILE_PREVIEW_CHARS` (512) - a small, bounded read
regardless of the file's actual size, since only the chip's own already-truncated preview
(`ClipboardPreview.MAX_LENGTH`, 24 chars) needs the content at all. The *actual* paste, on tap, still goes
through §38's native `performContextMenuAction(paste)` - the target app resolves the real file itself, this
read is never used for the paste's own content, only the chip's preview label.

One real bug caught (and fixed) while writing this: the KDoc for `resolveClipboardText()` originally spelled
out the wildcard as `` `text/*` `` in backticked prose - but Kotlin block comments nest, so the literal `/*`
inside that backticked span opened a *nested* comment the actual closing `*/` only closed, leaving the outer
KDoc comment open and silently swallowing the rest of the file as commentary (a cascade of "unresolved
reference" errors at compile time, traced back to this one line). Reworded to avoid embedding a literal `/*`
inside any comment; the one real occurrence of the string `"text/*"` left in the file is inside an ordinary
string literal (`hasMimeType("text/*")`), where it is not treated as a comment token.

**Working well in initial testing, deliberately left open rather than marked confirmed** - the user wants
broader real-world use before treating this one as settled.

## §63 - D-106 Stages 1+2 Implemented: English as an Explicit Language, Cross-Dictionary Loanword Protection (v0.8.34)

Follows a design discussion resolving D-106 (§61) into three stages; stage 3 (installable additional
languages) is discussed below but deliberately not started, and its Greeklish-transliteration sub-item is
dropped outright per explicit instruction, not merely deferred.

### D-115 correction: the case-only-correction rule does not fix "stimmen"/"Stimmen" - traced to a different mechanism
Before implementing, the "never silently apply a pure-case-differing correction" idea discussed for D-115 was
checked directly against the code, not assumed: `DictionarySuggestionProvider.bestCorrection()`'s own candidate
filter (`it.lowercase() != token`) already structurally excludes a case-only-different candidate - "Stimmen"
can never even reach that function's candidate list for typed "stimmen" in the first place. The actual
mechanism is `CapitalisationEngine.capitalise()`'s §6 rule 3 ("pure noun → auto-capitalise"), which trusts
`store.partsOfSpeech(word)` - itself a case-insensitive lookup (`InMemoryDictionaryStore`/`SqliteDictionaryStore`
both `.lowercase()` the key). "stimmen" is tagged as pure `NOUN` in the shipped dictionary, not the documented
"mixed → ambiguous `{NOUN, OTHER}`" case the build is supposed to detect for exactly this kind of homograph -
so §6 rule 5 ("ambiguous words: no automatic correction, offered as a suggestion" - which already exists and
already does precisely what was wanted) never gets a chance to fire, purely because the underlying data
mis-classified the word as unambiguous. The fix for this specific mechanism is therefore a dictionary-build
ambiguity-detection improvement, not a new runtime rule; recorded here so a future session doesn't
re-implement the case-only-correction idea expecting it to also cover this case. That idea is kept for its
original purpose (D-111's visibility gap, and any future correction path that genuinely could offer a
case-only candidate) - it just isn't a fix for D-115's own reported symptom. Deferred alongside the Ablaut-
generation idea (D-115) as a data-quality item, not started this round.

### D-106 Stage 1 - English Promoted to a Real, Selectable Keyboard Language (QWERTY)
The per-language `stores`/`providers`/`engines` maps (`AdaptKeyService`) already carried a full English entry
from the earlier real-multilingual-dictionaries work - English was only ever reached via A-03's per-token
auto-detection, never as a deliberately chosen active alphabet the way German/Greek already were. Promoted it
to a third, explicit position in the G-01 cycle:

- New pure `language/LanguageCycle` object (`LanguageCycleTest`, 3 tests): the fixed order
  `German → English → Greek`, with `next()`/`previous()` wrapping around the ends - kept separate from
  `AdaptKeyService` so the stepping logic itself is JVM-unit-tested, matching this project's usual split
  between pure policy and Android glue.
- `AdaptKeyService.toggleLanguage()` now takes a `forward: Boolean` (the swipe's own direction), stepping via
  `LanguageCycle` instead of the old two-language `if (== GREEK) GERMAN else GREEK` flip;
  `handleSwipe()`'s `GestureAction.LANGUAGE_PREV`/`LANGUAGE_NEXT` branches (already distinct in `KeyGesture`,
  previously collapsed into one identical call since direction didn't matter for two languages) now pass their
  own direction through.
- `resolveDict()` gained an unconditional `activeLanguage == Language.ENGLISH` branch (mirroring the existing
  Greek one), so English becomes the *forced* dictionary/capitalisation/store while active, not merely a
  classifier fallback - the classifier-driven English routing for an auto-detected foreign context (while
  German is active) is untouched, since that's still exactly what it's for.
- **QWERTY layout**: German is QWERTZ; English keyboards are QWERTY, differing only in the `y`/`z` position.
  `KeyboardLayout.rows()` gained a `qwerty: Boolean = false` parameter selecting between two private row-string
  constants (`"qwertzuiop"`/`"qwertyuiop"` for the top row, `"yxcvbnm"`/`"zxcvbnm"` for the third) - every hint,
  alternative, weight and every other row is entirely unchanged and shared between both variants, since
  `letterHints`/`topRowKey()` key off the character itself, not its screen position.
  `AdaptKeyboardView` gained a parallel `qwerty` property (same `rebuildRows()`-on-set pattern as the existing
  `greek` property), threaded into `rowsFor()`'s `KeyboardLayout.rows(...)` call; both `.greek =` assignment
  sites in `AdaptKeyService` (`onStartInputView`, `toggleLanguage()`) gained a matching `.qwerty =` assignment.
- **Known, accepted trade-off, not fixed this round**: the personal touch-offset model (T-03) keys its learned
  per-key zones by `Key.id` (`"c:y"`/`"c:z"`), which is character-identity-based, not screen-position-based. Since
  `y` and `z` sit in different physical positions between QWERTZ and QWERTY, switching to English moves each
  letter's on-screen position while carrying over whatever offset was learned for it under German - a real, but
  narrow and self-healing effect (only these two keys, only right after a switch, and T-03's continuous learning
  already re-adapts from there exactly as it would after any other zone drift). Not worth a bigger fix (e.g.
  position-aware ids, which would also need migrating already-persisted calibration data) for two keys that
  settle back out through ordinary typing.
- `languageLabel()`'s doc comment and the surrounding KDoc updated to drop the old "only German and Greek are
  user-selectable" framing.

### D-106 Stage 2 - Cross-Dictionary Loanword Protection
New `AdaptKeyService.knownInOtherLanguage(token)`: true when the token is a known word in any *other* bundled
language's dictionary than the one currently active for it (`providers.any { (language, p) -> language !=
activeLanguage && p.isKnownWord(token) }`) - since `providers` already holds all three bundled languages
simultaneously (German/English/Greek, all reachable via the stage-1 cycle), this naturally covers "every
active-or-selectable language, plus always English" without needing a separate "which languages are installed"
concept yet (that's stage 3). Deliberately a plain existence check, not a cross-language suggestion-ranking
model - matching the scoped-down design agreed on discussion, since frequency counts from different corpora
aren't meaningfully comparable and the reported problem ("Word" wrongly autocorrected to "wird") only needs
"don't touch a word that's valid somewhere else", not a merged, ranked completion list.

Wired at the two existing A-03 `suppressAutocorrect`-style choke points, asymmetrically per what each one needs:

- `finalizeAndCommit()`: folded straight into the existing `suppressAutocorrect` flag (`selectActiveDictionary(...)
  || knownInOtherLanguage(typed)`) - every downstream check already gated on that flag (the A-05 split veto, the
  diacritic-restoration/high-confidence-correction lookups, the final `autocorrectFor`/`rawCoordinateCorrection`
  call) now also leaves a cross-language-known token completely alone, for free, with no other line needing to
  change.
- `refreshSuggestions()`: **not** folded into `suppressAutocorrect` there, since that flag's existing branch
  clears the *entire* suggestion bar (correct for "unsupported foreign language, show nothing" - A-03's original
  case - but wrong here, where the active language's own ordinary completions should keep showing). Instead only
  the pending-autocorrect chip is suppressed: `val pending = if (knownInOtherLanguage(input)) null else
  provider.autocorrectFor(...)`.

No new unit tests beyond `LanguageCycleTest` - `knownInOtherLanguage()`/the `finalizeAndCommit()`/
`refreshSuggestions()` wiring are `AdaptKeyService` glue over the already-tested `DictionarySuggestionProvider.
isKnownWord()`, consistent with this project's established Android-service testing gap (see D-39 precedent).

### D-106 Stage 3 - What's Left Without Greeklish
Confirmed still worth keeping, **without** the transliteration sub-item the user struck entirely (not just
deferred - reasoning recorded: the handful of Greek loanwords worth using can simply be learned over time
through ordinary typing, and an English/American-convention Greeklish mapping would often be outright wrong for
German-flavoured Greek transliteration anyway): **installable/activatable additional languages beyond the three
bundled ones.** Still captured as its own, later design item - reuse the tier-3 model's browser+SAF import
pattern (`Tier3ModelActivity`/`Tier3ModelInstaller`, no `INTERNET` permission needed) for delivery, define an
"installable language pack" data format, and add a settings screen to manage installed/active/cycle-reachable
languages. Not started. - Backlog Round 16, Part 1: Layout, Language Architecture, Touch-Model Bounds

Captured, **not started**, per the usual rule for a batch this size - released item by item later.

### D-105 - Every Main-Page Digit Key Gets Its Own Superscript Digit as a Second Alt
Extends L-06's existing shifted-symbol hint (each digit already carries its QWERTZ shifted symbol, e.g. `2`→`"`,
`3`→`§`) with a second long-press alternative: the digit's own superscript form (`²`, `³`, and, for
completeness, `¹`/`⁴`.../`⁰` on every other digit too - not only 2 and 3, which is the visually obvious case
since square/cube are the everyday ones, but "alle Zifferntasten" per the request). Every digit key therefore
goes from a single alternative (which - per D-14/D-53 - applies immediately on long-press with only a one-cell
preview) to **two** alternatives, which per D-01 means every digit now opens a real multi-cell selection popup
instead of applying its shifted symbol immediately. This is a genuine behaviour change for the whole number
row, not just a cosmetic addition, and needs deciding during implementation: which of the two alternatives is
index 0 (pre-selected), and in what order the two are offered. `0` sits at the number row's right edge, so -
exactly the problem §34 already fixed for the letters page's `p`→π key - its popup will want to grow left
instead of right once clamped to the screen edge; the same reversed-alternatives-at-this-one-call-site
treatment (not a change to any shared constant) applies here.

### D-106 - Language Architecture: Explicit English, Multi-Dictionary Consultation, Installable Languages (Big Idea, Deferred)
Two related asks, both pointing at the same underlying gap - AdaptKey currently has exactly one *active*
language pointer (German/Greek, toggled by the G-01 space-swipe) plus a separate, invisible A-03 classifier
that silently reassigns English per-token for autocorrect purposes, but English is never a real, user-selectable
keyboard alphabet the way German/Greek are, and there is no notion of "installing" or "activating" a language
beyond the three hardcoded, bundled ones:

1. **English as an explicit, selectable keyboard language, like Greek.** The user wants to cycle to English via
   the same G-01 mechanism (or an equivalent selector) that already switches German ↔ Greek, not have it stay a
   background auto-detection artifact.
2. **Embedded loanwords/code-switching must not get autocorrected away.** Typing English words inline in German
   text (e.g. "Word") currently has no protection - A-03's language classifier requires several words of context
   (§43's `minWords = 2` fix) before it suppresses German autocorrect for a whole *stretch* of foreign text, but
   a single embedded English word surrounded by German has no such signal and gets silently autocorrected to
   whatever German word is nearest (e.g. "Word" → "wird"). Proposed direction: the dictionaries of **every
   currently-active-or-selectable language** (i.e. everything reachable via the space-swipe cycle) **plus always
   English** should be consulted as valid-word candidates while typing, not just the one currently "active"
   language - so "Word" is found as a known word in the English dictionary and is accepted as a real candidate
   (removing the perceived ambiguity with "wird" and blocking the autocorrect), regardless of which alphabet is
   currently toggled. In the user's own case this would mean German + English (mandatory) + Greek, ideally
   including Greek transliteration (Greeklish) "if that isn't too much effort".
3. **Other languages should be installable/activatable later**, implying a real language-management concept
   (enable/disable/install a language) rather than a fixed three-language toggle baked into the code.

**Explicitly not to be implemented from this capture alone** - this is architecturally closer to D-68's
"rethink calibration" or D-92's calculator redesign: it touches the active-language toggle (G-01), the A-03
classifier/dictionary-selection plumbing (`selectActiveDictionary`/`resolveDict`), the settings surface, and
potentially the shipped dictionary asset set, and needs its own design pass (exact UI for "install/activate a
language", whether "consult N dictionaries simultaneously" changes the autocorrect/suggestion ranking model in
ways that need new tie-breaking rules, and what "always consult English" costs in per-keystroke lookup time)
before any code changes.

### D-107 - Recognised-Word Highlight (S-05/C-04) Semantics Under Reconsideration (Not Decided - Flag Only)
Raised, explicitly **not** to be designed or implemented yet ("deshalb erstmal nur vormerken") - the user is
reconsidering when the green highlight should apply at all. As currently specified (S-05), the highlight means
"this word is recognised as correct and complete, *no* correction is planned" - i.e. green currently signals
*safety*. The user's new framing is the opposite: green should apply *only when the word is about to be
auto-corrected*, i.e. signal an impending *change*, not "this is fine as typed". This is a full reversal of
current S-05 semantics, not a tweak, and the user is explicit that the exact rule isn't settled yet. Noted here
purely as a flag for a future design conversation - no interpretation, no implementation.

### D-108 - Long-Press Popup Must Tolerate a Small In-Key Smear
Reported: a slight finger smear while holding a key with an alt (e.g. the full-stop key) - without the finger
actually leaving the key's own bounds - can currently cancel the pending long-press instead of still opening
the popup. The request: as long as the touch has not left the key's own bounds, it must still count as a
held long-press and show the popup: likely the scheduled-long-press-cancellation logic reacts to the system's
small `ACTION_MOVE` touch-slop rather than to "did the finger leave this key's rect", the way the D-64
drag-to-trash investigation found a similar too-eager slop-based cancellation for a different gesture. Not
yet traced against the actual `scheduleLongPress`/move-handling code - captured as reported, to investigate
together with an implementation pass.

### D-109 - T-03 Offset-Model Learning Needs Hard Bounds
Two related reports, both about the personal touch-offset model (T-03) drifting a key's learned zone too far
from its true, safe position, purely from ordinary (if imprecise) real-world typing - not overridden
deliberately by the user, and visibly worse for them, confirmed by the D-24 touch-zone visualisation:

1. **Bottom-row keys (`c`/`v`/`b`/`n`/`m`, the row directly above the space bar) drift toward space** when the
   user repeatedly mistaps one of them instead of hitting space. Every such mistap that later gets silently
   corrected (the T-05 space-ambiguous flag already exists for exactly this class of tap, and A-05 already uses
   it to retroactively turn the wrong letter back into a space) is nonetheless still being fed into that key's
   own `OffsetModel` as if it were a genuine, confirmed hit for that letter - reinforcing exactly the wrong
   lesson. Proposed direction: a tap that is later resolved as "this was actually meant to be a space" (T-05
   space-ambiguous, subsequently split back to a space by A-05) should not be recorded as a positive training
   sample for the letter key it landed on - and even so, as a hard backstop, the model should never let a
   bottom-row key's zone drift so far toward space that it starts overlapping the space bar's own territory in
   the first place.
2. **General drift bound, beyond the specific space-adjacency case**: the user's own `j` key has drifted
   significantly leftward, to their detriment, well within whatever cap the model already applies
   ("offset capped to key size" per T-03) - the existing cap is evidently too loose in practice, or a
   directionally-biased pattern (from typing-pattern seeding, T-04/`PatternSeed`, or ordinary sustained real
   usage) can still walk a zone to an uncomfortable extreme even while staying inside it. Needs investigation
   into the current clamp value/shape and whether a materially tighter or shape-aware (not just per-axis-size)
   bound is warranted, independent of the bottom-row-specific fix above.

### D-110 - eBay Kleinanzeigen Input Field Never Auto-Capitalises (Investigate)
The message-input box in the (eBay) Kleinanzeigen app never capitalises the first letter, neither at the true
start of a line nor at a sentence start - both of which are AdaptKey's own rules (§6 hierarchy rule 2,
independent of whatever the field's own `EditorInfo` caps flags say) and should apply regardless of what the
target field requests. Not yet traced - candidate causes to check once picked up: whether this field reports an
`inputType`/variation (e.g. a filter or no-suggestions variant) that some existing capitalisation code path
mistakenly treats as "skip auto-cap entirely" rather than just "no field-mandated baseline", or whether
`getCursorCapsMode()`/`EditorInfo` from this specific app returns something AdaptKey's `CapitalisationEngine`
call site doesn't handle. Needs the actual field's reported `EditorInfo` (loggable during a debug session) to
make progress rather than guessing further.

## §62 - Backlog Round 16, Part 2: Autocorrect/Suggestion Quality - Master Plan

The user handed over a large batch of concrete autocorrect/suggestion reports with an explicit instruction on
how to work through them: **do not chase individual reported words** - find and fix the *general rules* behind
the pattern, the way §43 (a language-classifier confidence gate), §44 (a relative instead of absolute A-01
veto) and §45 (a uniform bigram-evidence requirement across both split strategies) already did this session.
Before writing this section, each report was checked directly against the bundled dictionary assets
(`app/src/main/assets/dict_de.tsv`) and/or the actual code (`AdaptKeyService.kt`,
`DictionarySuggestionProvider.kt`, `KeyboardProximity.kt`) rather than guessed at - several turned out to have a
concrete, confirmable cause already visible in the current build; others are dictionary-data findings; a couple
remain open questions needing a design decision. **Nothing in this section is implemented yet** - captured as a
batch, to be worked through and released deliberately, most likely roughly in the theme order below (visibility
gaps and the two confirmed regressions first, since those are the most self-contained; the mid-word-correction
cluster together, since all four share one function/mechanism; the dictionary-coverage questions last, since
those may need a data/build-pipeline change rather than a pure logic fix).

### Theme A - A Pending Change Must Be Visible Before It Applies

#### D-111 - A Silent Case-Changing Correction Is Never Shown as a Suggestion First
When autocorrect is about to change a word's *case* (most often: silently upper-casing a word the user
deliberately typed lower-case), that outcome currently never appears in the suggestion bar in advance - S-02's
"the word exactly as typed does not appear as a suggestion" rule (built for the ordinary "don't waste a slot on
a no-op" case) ends up hiding a case change that is very much *not* a no-op from the user's point of view. S-06
already carved out the one existing exception (a pending literal-substitution autocorrect gets a verbatim
"keep as typed" chip) - this needs a comparable exception for a pending *case-only* change: the target casing
must be visible in the bar before it is silently applied, not discovered only after the fact.

#### D-112 - A Correction's Own Case Must Match the Target Casing Context, Not Just the Dictionary's Canonical Form
Example given: typing "Fur" at a position that will be capitalised (sentence start, or an explicit mid-word
capital) currently offers/applies the dictionary's plain canonical form "für" - the correction should instead
be "Für", matching the same capitalisation context that would apply to any other word typed there. This looks
like an ordering problem between whichever step resolves the *best-matching dictionary word* (case-folded,
naturally, since umlaut/case folding is how the fuzzy match works at all) and whichever step applies §6's
capitalisation transform - the transform needs to run on the *chosen correction*, not only ever on the *raw
typed text*, or a corrected word can silently skip the capitalisation the identical, uncorrected word would
have received. Not yet traced to a specific call site; likely close to where `TokenRepair`/`autocorrectFor`
hands its answer back to `finalizeAndCommit()`, which is also where §6's `CapitalisationEngine` runs.

### Theme B - False-Positive Corrections: Ambiguity/Confidence Gating

#### D-113 - "spreche" → "Sprache": §44's Own Ratio-Override Fires on an Unrelated Homograph Pair
Checked directly against `dict_de.tsv`: **both words are separately known** - `Sprache` (freq 7228, NOUN) and
`spreche` (freq 49, OTHER/verb-form) each have their own dictionary row, so this is not a missing-word case like
D-115 below. The frequency ratio is ≈147x, comfortably past §44's `KNOWN_WORD_OVERRIDE_RATIO` (50x) - the very
rule §44 introduced specifically to let a hugely-more-frequent *typo neighbour* (`die` vs. `due`, `der` vs.
`ddr`) override A-01's known-word protection. Here it fires on two words that are not typo variants of each
other at all, merely a common noun and a rarer verb inflection that happen to sit within edit-distance budget
of each other (a single letter + case). §44's ratio-alone heuristic is evidently not sufficient by itself to
distinguish "this is almost certainly the intended typo-correction of a rare misspelling" (its original,
correct use) from "these are two entirely different, unrelated real words, one of which is just far more common
than the other" (this new false positive) - needs a sharper condition, not a bigger ratio or a word-specific
exception. Candidate angle to explore: a same-length/case-differs-only correction relative to a real known word
is much more likely to be this second, illegitimate class than a genuine spelling typo, and might need its own,
stricter rule (or outright exclusion from the override) rather than reusing the exact same ratio threshold as a
plain misspelling.

#### D-114 - "Vorhin" → "Virgin"/"vorhinein": Missing Dictionary Entry + No "Genuinely Ambiguous, Don't Correct" Gate
Checked directly against `dict_de.tsv`: **`vorhin` itself does not exist in the dictionary at all.** The only
near matches are `Virgin` (freq 62, NOUN - an English proper-noun artifact of the Wikipedia-derived corpus,
almost certainly from articles mentioning Virginia/Virgin Islands/the Virgin brand) and `Vorhinein` (freq 11,
NOUN, capitalised - itself a likely POS/casing-heuristic miscategorisation, since "vorhinein" is an ordinary
lower-case adverb in real German, not a proper noun; its low sample count plausibly let sentence-initial
capitalised occurrences dominate the "canonical surface case" heuristic - see A-03's build notes). Two separate
fixes fall out of this one report: (1) a straightforward dictionary-data gap (add `vorhin`, fix `Vorhinein`'s
casing/POS), and (2) the more general, more valuable fix - **there is currently no "none of the candidates is a
clearly dominant winner, so don't silently autocorrect at all" gate**. Even with the data gap fixed, the
underlying mechanism accepted a correction here despite two candidates of comparable low frequency (62 vs. 11)
and no dictionary entry for the actually-intended word - exactly the "keine Eindeutigkeit" complaint that
recurs across several of this round's reports (see also D-111-adjacent cases and D-113). Also flags a data-
quality question worth answering once the dictionary build is revisited: why does a rare English proper noun
like `Virgin` outrank a real German adverb form in the first place, and should the build pipeline filter or
down-weight low-value foreign proper-noun noise picked up incidentally from the German Wikipedia dump (ties
into D-106's broader multi-language-dictionary discussion).

### Theme C - Dictionary Coverage / Morphology

#### D-115 - "merke" → "Marke", "stimmen" → "Stimmen": Missing Verb-Inflection Entries, Not a Ranking Bug
Checked directly against `dict_de.tsv` - **neither `merke` nor `stimmen` (the verb forms) exist as their own
dictionary rows at all**; only `Marke` (freq 578, NOUN) and `Stimmen` (freq 2748, NOUN) do. Since the typed
word is not a known word in either case, A-01 never even engages - ordinary autocorrect just finds the nearest
known candidate (a one-letter substitution plus a case change, well within budget) and applies it, with nothing
to override. This directly answers the question raised alongside these two reports: **does the dictionary keep
both the noun and the verb form of a case-homograph, or do they collapse into one?** Confirmed: **they
collapse** - per A-03's build notes, part-of-speech is derived from casing statistics on a single,
case-insensitive dictionary key, with one "canonical surface case" chosen as whichever casing was more frequent
in the source corpus; the "mixed → ambiguous `{NOUN, OTHER}`" case the build is documented to detect apparently
still keeps only one *surface form* (one dictionary row) per key even when it flags the ambiguity, discarding
the minority-cased form's own separate existence as a known word entirely - exactly the gap the user suspected,
and now confirmed with real data rather than assumed. At minimum this needs the dictionary build to record that
*both* forms exist (the user's own suggested floor: "mindestens muss dort ein Vermerk stehen, dass es beide
gibt") so A-01 can protect the minority form even if only one surface spelling is ever offered as a completion;
ideally both surface forms become independently known words. This is a data/build-pipeline change
(`scratchpad/build_dict.py`, outside the shipped app), not a runtime logic fix - likely needs re-running the
dictionary build with the collapsing behaviour changed, not just patched at read time.

#### D-116 - Compound-Word/Fragment Recognition ("Beitragsjahren")
Typing "Nach 45 Beitragsjahreb" (a typo of "Beitragsjahren") is not recognised or corrected - plausibly because
the intended whole word does not exist in the dictionary at all (an uncommon compound, "Beitragsjahre" +
dative-plural "-n"), not because of any correction-quality bug. Raised by the user as a concrete example of a
broader idea: could AdaptKey recognise/correct compound words (or fragments of them) it has never seen as a
whole unit, by decomposing them the way B-01 already does for explicitly hyphenated compounds, but for
unhyphenated ones? This is a substantial, open-ended feature idea (German compound-splitting without a hyphen
to anchor on is a much harder problem than B-01's hyphen-segmented case), not a quick fix - captured here as a
question to think about, not a committed design.

#### D-117 - Words Recognised Too Late for a Garbled Prefix ("erkamm" → should already hint "erkannt")
General complaint: recognition/suggestion often kicks in far later than it could. Concrete example: typing
"erkamm" (multiple edits away from "erkannt": not a single adjacent-key substitution, more than one character
differs) should arguably already surface "erkannt" as a likely candidate at that point, the way a human reader
would guess it immediately. Flagged by the user themselves as a genuinely open question - is this achievable
with the existing tier-1 edit-distance/proximity approach at all, or does meaningfully "reading past" this many
simultaneous typos really need the tier-3 mini-LLM (still device-pending, see the "Remaining" section of
progress.md) rather than a tier-1 rule tweak? No commitment either way yet - needs a concrete decision on how
much fuzz budget/ranking change is worth trying at tier-1 before concluding this is genuinely LLM-only territory.

### Theme D - Investigated, Not Reproducible as Described

#### D-118 - Single "c" → "x": Not Reproducible in the Current Code as Described
Traced directly: `DictionarySuggestionProvider.bestCorrection()` has a hard `MIN_AUTOCORRECT_LENGTH = 2` floor
(`DictionarySuggestionProvider.kt:205,254`) that rejects any token shorter than 2 characters *before* any
candidate search, known-word check, or A-01 guard even runs - a standalone, single-character "c" cannot reach
autocorrect at all via this path. (For what it's worth, `KeyboardProximity`'s QWERTZ adjacency map does list `c`
and `x` as neighbours - `"yxcvbnm"` - so if this were reachable, `x` would indeed be a cheap substitution
candidate; it just isn't reachable as described.) Reported back rather than captured as a blind fix target -
needs a sharper repro to make progress: was the `c` truly a standalone token (bounded by spaces/punctuation on
both sides), or part of a longer word where a `c`-containing segment got corrected and only looked like an
isolated "c" in the moment it was observed?

### Theme E - Mid-Word Correction (§58) Follow-On Bugs

All four items below were traced directly against the current `AdaptKeyService.kt` and are confirmed causes,
not guesses - they all sit in or immediately around the same mid-word-editing mechanism §58 introduced
(`reclaimWordAtCaret()`, `composingCursor`, `handleShift()`), which makes sense as a cluster: §58 made "the
caret is sitting somewhere inside an existing word" a much more common, load-bearing state than it used to be,
and several other code paths were apparently never updated to account for the caret *not* being at the
composing token's end while that state is active.

#### D-119 - SPACE Mid-Word Wrongly Finalises Instead of Inserting a Literal Space
Repro: "Das ist ein Test.nächstes Wort", caret placed right after the period (mid-word relative to
`nächstes`, not at its end) specifically to insert a missing space there - pressing SPACE does not insert a
space at all; instead the whole word gets accepted/finalised and the caret jumps to its end. Confirmed:
`handleKey()`'s `KeyCode.SPACE` branch calls `finalizeAndCommit(ic, " ", ...)` **unconditionally** whenever
`composing` is non-empty, with no check of `composingCursor` against `composing.length` at all - so SPACE
always finalises the *entire* composing token as if the user had just finished typing it, regardless of where
in the word the caret actually is. The user's own generalisation looks correct: SPACE is probably never the
right trigger for "finalise this word" unless the caret genuinely sits at the word's own end (or, per the
user's more cautious floor, at least requires a letter to have just been typed at that position first) - a
mid-word SPACE should instead behave as an ordinary literal-space insertion that splits the token in two.

#### D-120 - Mid-Word Punctuation/Symbol Insertion Lands at the End of the Word, Not at the Caret
Repro: type "Ein", place the caret before the `E`, type a `"` (double quote, via a key's long-press
alternative) - the quote ends up *after* "Ein" (i.e. "Ein\""), not before the `E` where the caret visibly was.
Confirmed: `commitLongPressSymbol()`'s non-letter path (and `finalizeAndCommit()` generally, so this is not
specific to quotes or to long-press) commits the finalised word via
`ic.setComposingText(finalWord, 1)` - Android's `newCursorPosition = 1` places the cursor **after the entire
composing text**, unconditionally - and only then commits the delimiter at that (now end-of-word) cursor
position via `ic.commitText(delimiter, 1)`. There is no code path that commits a delimiter at the caret's
actual, pre-existing mid-word position (`composingCursor`) - every delimiter commit assumes "the caret is at
the end of what I'm about to finalise", which §58 broke the moment mid-word editing became a first-class,
common state.

#### D-121 - `handleShift()` Always Prefers the G-05 Word-End Gesture Over Caps-Lock-Off, and Doesn't Check the Caret Is Actually at the Word's End
Two reported symptoms trace to the exact same code (`AdaptKeyService.handleShift()`, one function), so this is
one bug with two triggers, not two:

1. **Backspace mid-word, then Shift, wrongly re-triggers G-05.** Repro: type "Verbinder", caret after the `V`,
   Backspace (removes the `V`, leaving "erbinder" with the caret now at the token's *start*, not its end), then
   Shift (intended, per the user, as an ordinary case toggle) - the following `e` gets capitalised
   unexpectedly. Confirmed: `handleShift()`'s very first check is `if (composing.isNotEmpty())`
   (`AdaptKeyService.kt:2147`) - not a comparison of `composingCursor` against `composing.length` - so *any*
   non-empty composing token routes straight into `handleWordEndShift()` → `flipFirstInComposing()`, which
   always flips `composing[0]` (the string's first character) regardless of where the caret actually sits.
   There is no "is the caret genuinely at this token's end" check anywhere in this path.
2. **Caps Lock, mid-word, Shift-to-turn-off wrongly re-triggers G-05 instead.** Repro: engage Caps Lock, type
   "MCU" (no Space yet - `composing` is still non-empty), press Shift meaning only to release Caps Lock -
   confirmed: the same `composing.isNotEmpty()` check at line 2147 is evaluated **before** the
   `view.capsLock` check further down the same function (lines 2153-2158), so the Caps-Lock-release branch is
   never reached at all while composing is non-empty; the G-05 word-end gesture fires instead, and Caps Lock
   silently remains engaged for the rest of the input.

Both symptoms need the same underlying fix: `handleShift()` needs to (a) check Caps-Lock-off intent before
falling into the G-05 branch, not after, and (b) gate the G-05 word-end gesture on the caret genuinely being at
the composing token's own end (`composingCursor == composing.length`, or equivalent), not merely on
`composing` being non-empty - the same kind of caret-position blind spot D-119/D-120 above already found in two
other call sites.

#### D-122 - Mid-Word Correction Should Raise Word-Split Priority When an Unresolved c/v/b Connector Is Detected
When A-05's word-splitting fails to catch a stray `c`/`v`/`b` (etc.) that should have been a space, the token is
left stuck together with the wrong letter as an accidental connector - the request is that entering *mid-word*
correction on such a token (§58) should recognise this specific shape (an otherwise-unresolved
`TokenRepair.OVER_SPACE_LETTERS` character sitting inside the composing token) and give the split candidate
noticeably higher priority than it would otherwise get, so the user gets an easy way to split the words apart
without much extra effort once they've gone back to fix it. Not yet investigated against
`TokenRepair`/`refreshSuggestions()`/the existing §49 live-split-preview machinery (which already calls
`trySplit()` continuously while composing, just for colouring, not yet as a promoted suggestion) - captured as
a design direction to explore together with an implementation pass, not a traced bug.

### Theme F - Suggestion-Accept vs. Immediately-Typed Punctuation

#### D-123 - Suggestion-Bar-Tap's Trailing Space May Not Survive to the Punctuation Check
D-29 (already implemented, v0.7.10) is supposed to cover exactly this: accepting a suggestion adds a trailing
space, and typing punctuation immediately afterward should eat that space. Traced the current wiring rather
than assuming it's simply missing: `onSuggestionClicked()`'s bar-tap path is the *only* site that arms
`pendingSuggestionSpace = true` (autocorrect-on-commit doesn't need to, since it commits the delimiter directly
without an extra space) and `finalizeAndCommit()`'s `composing.isEmpty()` branch is the only site that checks
and consumes it. A plausible gap: `onSuggestionClicked()` calls `commitText()` *then* `clearComposing()` *then*
sets the flag - but `commitText()` on a collapsed selection triggers the framework's `onUpdateSelection()`
callback, which (composing already empty at that point) unconditionally calls `reclaimWordAtCaret()`
(`AdaptKeyService.kt:571`), and `reclaimWordAtCaret()` resets `pendingSuggestionSpace = false`
(`AdaptKeyService.kt:606`) as one of its first steps, regardless of whether it finds anything to reclaim. If
that callback lands - as such callbacks normally do, asynchronously - before the user's next keystroke, D-29's
flag is cleared before the punctuation check ever gets a chance to run. **Plausible and code-grounded, not
runtime-confirmed** (no emulator in this environment, and the actual callback timing here is exactly the kind
of thing §32/§46 already found could only be confirmed on a real device) - worth checking first, before
assuming D-29 needs a redesign rather than this one ordering fix.
