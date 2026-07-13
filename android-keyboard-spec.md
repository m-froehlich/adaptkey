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
