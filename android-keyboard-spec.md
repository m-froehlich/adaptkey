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

### T-04 - Typing Pattern Detection
The dominant typing pattern is derived automatically from the accumulated offset model:

- **Left index finger:** Systematic offset to the right, more pronounced for keys on the right half of the keyboard; narrow contact area.
- **Right index finger:** Systematic offset to the left, more pronounced for keys on the left half of the keyboard; narrow contact area.
- **Thumb:** Low lateral offset; larger contact area (touch radius from `MotionEvent.getSize()`); hits tend to be vertically centred.

The detected pattern is shown to the user as information only - not as a constraint - and can be overridden manually. Pattern detection has no effect on functionality; it serves transparency and optional upfront configuration.

### T-03 - Personal Offset Model (Typing-Style Agnostic)
All tap events are persisted as tuples `(touch_x, touch_y, confirmed_key)`. A 2D Gaussian offset kernel is computed per key to compensate for each user's individual systematic deviation. The model makes no assumptions about handedness or finger - it learns exclusively from observed deviations. It is updated continuously and improves with every confirmed word.

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

## §64 - Batch Release: D-105, D-108, D-109, D-111–D-115, D-119–D-121, D-123 (v0.8.35)

Released together per explicit user instruction, following the §63 discussion. **D-118 struck outright** (not
deferred) - confirmed not reproducible as described, no further repro was forthcoming. **D-122 deliberately not
implemented this round** - it was captured as an unconfirmed design idea, not a traced bug, and this round's
priority was items already root-caused; still open, needs a concrete repro/trace before an implementation
attempt, consistent with this project's "trace before fixing" rule.

### D-105 - Every Main-Page Digit Key Gets a Superscript Second Alternative
`KeyboardLayout.rows()`/`GreekLayout.rows()` both gained a `numberKey(c)` builder: every digit's existing
shifted-symbol hint (L-06) stays index 0 (pre-selected, unchanged behaviour for a straight long-press), with
its own superscript form (`¹²³⁴⁵⁶⁷⁸⁹⁰`) added as index 1 - every digit now opens a real D-01 two-cell popup
instead of applying its shifted symbol immediately. `0` sits at the number row's right edge - the same problem
§34 fixed for the letters page's `p` key - so its list is reversed at this one call site (not the shared
constants), keeping its own glyph nearest the finger once its popup clamps leftward. Applied identically to
both the German/English Latin layout and the Greek layout, since L-06's number row is otherwise shared/mirrored
between them. 4 new tests (`KeyboardLayoutTest`, `GreekLayoutTest`).

### D-108 - Long-Press Popup Now Tolerates a Small In-Key Smear
`AdaptKeyboardView` gained `pressedKeyRect` (the pressed key's own bounds, captured on `ACTION_DOWN` alongside
`pressedKey`) and a new `movedOutsideKey(x, y)` check. `ACTION_MOVE` now cancels a pending long-press only once
the touch has actually left the pressed key's rect, not once it clears the much smaller system touch-slop
(`movedBeyondSlop`) - which could cancel a long-press well before the finger left the key at all. Backspace-hold
cancellation is deliberately left on the original, tighter slop-based check (`movedBeyondSlop`), since a swipe
on backspace is a distinct, deliberate G-02 word-delete gesture that should still be recognised promptly, not
tolerated as a "smear". Android touch-handling glue, no new test - consistent with this project's documented
testing gap (no emulator/device here), same class as D-64's touch-arbitration fix.

### D-109 - T-03 Learning Now Has Bounds
Two independent changes, both aimed at the reported drift (bottom-row keys creeping toward space; the user's
own `j` key drifting too far left):
1. **Ambiguous taps no longer train the model at all.** `AdaptKeyboardView.onTouchEvent()`'s `ACTION_DOWN`
   branch now runs the T-05 `ambiguityBands.classify(...)` call *before* `offsetModel?.record(...)`, and only
   records the tap when `pendingAmbiguity.kind == TapAmbiguity.NONE`. A tap flagged `SPACE_AMBIGUOUS` (a
   bottom-row letter that might really have meant space) or `LETTER_AMBIGUOUS` (the reverse) is, by
   definition, not confidently the key it resolved to - training that key on it anyway was reinforcing exactly
   the wrong lesson every time a mistap got silently corrected elsewhere (T-05/A-05). This directly targets the
   reported bottom-row-into-space drift at its source, rather than trying to bound it after the fact.
2. **`OffsetModel.DEFAULT_MAX_OFFSET_FACTOR` tightened from 0.9 to 0.5** - the existing cap on the learned mean
   offset, as a fraction of a key's half-size. 0.9 let a learned strike point sit within 10% of a key's own
   edge, effectively crowding into the neighbouring key's territory; 0.5 is a more conservative starting point,
   not yet device-tuned further (a single constant, easy to retune later, same precedent as several other
   threshold tunings this project has needed). Also: `AdaptKeyboardView.drawTouchModel()` (the D-24
   visualisation) previously drew the *raw, uncapped* `Spread.meanDx`/`meanDy` as the strike-point centre - so
   the very visualisation the user was judging the drift from could show a **more extreme** drift than the
   model's own `resolve()` would ever actually apply (which already caps it via `logLikelihood()`). Fixed by
   having the visualisation use `OffsetModel.cappedMeanOffset()` (newly exposed `maxOffsetFactor` property) with
   the same per-key bound `logLikelihood()` computes, so the visualisation now accurately reflects what the
   model will really do. `spreadFor()` itself is left returning raw, uncapped statistics unchanged - its
   existing tests (`OffsetModelTest`, `CalibrationActivityRoboTest`, `OffsetStoreRoboTest`) rely on that exact
   contract for verifying the underlying sufficient statistics, independent of any display-time capping.

### D-111 / D-112 - A Pending Capitalisation Change Is Now Visible Before It Applies, in the Correct Case
`AdaptKeyService.refreshSuggestions()`'s `pending` (the S-06 "impending autocorrect" signal, previously only
ever a spelling correction from `provider.autocorrectFor()`) now runs that correction candidate (or the raw
input, if none) through the *same* §6 `capitalisation.capitalise(...)` call `finalizeAndCommit()` will
eventually apply, using the *already-tracked* live context (`contextFor(input)` - sentence-start/field-mandate
state that already exists per-token, no new tracking needed). Reusing the *existing* S-06 mechanism
(`SuggestionController.update(input, candidates, pending)`, unchanged) means no new UI code was needed at all:
- **D-111**: an ordinary noun about to be auto-capitalised (§6 rule 3/4) - previously invisible until the
  delimiter silently applied it - now shows as the S-06 verbatim/pending chip pair *while still composing*.
- **D-112**: a correction whose own case must follow the sentence/field context ("Fur" at a sentence start) now
  previews as "Für", not the dictionary's plain canonical "für" - the capitalisation transform runs on the
  *corrected* candidate, not only ever on the raw typed text.

Deliberately narrower than a byte-perfect preview of `finalizeAndCommit()`'s full pipeline: only
`autocorrectFor()`'s own candidate search is mirrored (umlaut/diacritic restoration is already free within it,
cost 0 once folded - see D-48/D-63), not the rarer raw-coordinate-correction fallback (D-39, needs real
composing taps/geometry, not worth computing every keystroke for a preview) or the §9 tier-3 `llmForcesUpper`
exception (defaults to `false` in the preview; the real commit-time value still applies correctly at commit).

**Heads-up, not yet device-tested**: because German capitalises essentially every common noun, this makes the
S-06 chip pair appear far more often than before - literally on most nouns typed lowercase, not only the
ambiguous/surprising cases that originally motivated the request. This is the deliberate, direct consequence of
what was asked ("es darf nicht überraschend geschehen, ohne je in den Vorschlägen aufzutauchen") rather than an
oversight - flagged clearly since it is a real, broad change to how often the suggestion bar's S-06 pair shows
up, worth confirming feels right in everyday typing rather than noisy, the same way several other features in
this project were refined after real-device feedback.

### D-113 - §44's Ratio-Override Restricted to Genuine Cost-1 Typos
`DictionarySuggestionProvider.bestCorrection()`'s A-01 override check now additionally requires
`best.cost <= ADJACENT_SUB_COST` (a single neighbouring-key substitution) before `shouldOverrideKnownWord()`'s
frequency-ratio check is even consulted - previously the ratio alone (`shouldOverrideKnownWord`, unchanged) could
override A-01 for *any* candidate within the full two-edit `MAX_CORRECTION_COST` budget. Verified against the
real bundled `KeyboardProximity` map: "due"→"die" and "ddr"→"der" (§44's original motivating cases) are both
cost-1 (u/i and d/e are QWERTZ-adjacent) and are unaffected; "spreche"→"Sprache" (this round's reported false
positive) is cost-2 (e/a are not adjacent) and no longer overrides, despite an even more extreme frequency ratio
(≈147x) than either original case. 2 new tests.

### D-114 - A Low-Confidence Autocorrect Candidate Is Never Silently Applied + the Missing Word Added
Two independent parts:
1. **New `minAutocorrectFrequency` constructor parameter on `DictionarySuggestionProvider`** (default `0`, so a
   plain `DictionarySuggestionProvider(store)` - including the shared test-suite instance - is entirely
   unaffected): a correction candidate below this absolute frequency is dropped from `bestCorrection()`'s
   candidate search outright, however good its edit cost otherwise looks. `AdaptKeyService`'s two real
   construction sites now pass `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY = 300L`, calibrated against the bundled
   `dict_de.tsv`: every legitimate correction target already relied on this session sits far above it
   (komplett 881, Sankt 968, Standard 1534, kleinen 3748, Wort 4084, können 23227, werden 93866), while the
   reported bad candidates sit far below it (Virgin 62, Vorhinein 11) - directly fixes "vorhin"→"Virgin"
   without needing any per-word special case. A candidate below the floor can still appear in the broader
   suggestion-bar prefix/fuzzy list (`suggestionsFor`, unaffected) - only the *silent autocorrect* path is
   gated. Deliberately a constructor parameter rather than a hardcoded constant: an absolute, corpus-scale
   floor would have broken over a dozen existing tests that use small, illustrative synthetic frequencies (40,
   50, 80, 200...) to test *relative* ranking, not realistic absolute frequencies - making it opt-in keeps that
   whole existing test suite meaningful unchanged, while production explicitly opts into the real floor. 2 new
   tests, including one confirming the no-floor default is unaffected.
2. **`vorhin` added to `dict_de.tsv`** (frequency 900, `OTHER`) - confirmed genuinely absent from the bundled
   dictionary (only `Virgin` and the correctly-capitalised `Vorhinein`, an "im Vorhinein" idiom, existed
   nearby). Calibrated against comparable temporal adverbs already in the corpus (`vorher` 1437, `jetzt` 1499)
   - a hand-inserted, clearly-documented data patch (no dictionary-build pipeline exists in this environment to
   regenerate a real corpus-derived frequency, see D-115 below), not a re-run of the actual build.

### D-115 - Two Different Root Causes, Not One: a Ranking Bug vs. a True Data Gap
Split into what the evidence actually showed, not treated as one problem:
- **"merke"→"Marke"**: `merke` (the verb form) was genuinely absent from `dict_de.tsv` entirely - not a
  case-collapse issue (the words differ by more than case: `merke` vs. `Marke`), a plain coverage gap. Added
  `merke	65	OTHER` next to its conjugation siblings (`merkt` 79, `merken` 72, `merkte` 56 - all similarly rare
  in this Wikipedia-derived, mostly-third-person corpus), comfortably below `Marke`'s 578 but a small enough
  ratio (≈8.9x, far under §44's 50x) that A-01 protects it outright once it exists as a known word at all - no
  override, no special rule needed.
- **"stimmen"→"Stimmen"**: **not** the same mechanism, and **not** something the §63-discussed case-only
  correction idea could ever have fixed - traced end-to-end (see §63's own correction note) to
  `CapitalisationEngine.capitalise()`'s §6 rule 3 (pure noun → auto-capitalise) trusting `store.partsOfSpeech()`,
  itself a case-insensitive lookup. `Stimmen` was tagged plain `NOUN` in the dictionary, not the documented
  "mixed → ambiguous `{NOUN, OTHER}`" case the build is supposed to produce for exactly this kind of
  case-homograph (the verb "stimmen" and the noun "Stimmen" are both genuinely common) - so §6 rule 5
  ("ambiguous: no auto-capitalisation, offered as a suggestion instead" - which **already exists and already
  does exactly what was wanted**) never got a chance to fire. Fixed with a one-line data change:
  `Stimmen	2748	NOUN` → `Stimmen	2748	NOUN,OTHER` in `dict_de.tsv`, directly answering the question raised in
  discussion (does the dictionary keep both forms, or do they collapse? - confirmed: they collapse into one
  row/one POS set, and the fix is exactly the "at least a marker that both exist" floor proposed) - no runtime
  logic changed at all for this one; §6's rule 5 now reaches it because the data correctly says "ambiguous".
- **Ablaut/irregular-verb generation, and any broader dictionary rebuild** remain explicitly deferred - noted in
  §63, not touched this round. No dictionary-build pipeline exists in this environment (`scratchpad/build_dict.py`
  was a throwaway dev tool, never committed) - both data edits above are hand-inserted, clearly-documented
  patches to the shipped TSV assets, not a re-run of the actual Wikipedia-corpus build.

### D-119 - SPACE Mid-Word Now Inserts a Literal Space Instead of Finalising the Whole Token
New `AdaptKeyService.splitComposingAtCaretAndCommit(ic, delimiter, spaceInferred)`, invoked from inside
`finalizeAndCommit()` itself (see D-120 below - the same fix covers both) whenever `composingCursor !=
composing.length`. Splits the composing token in two at the caret: the "before" half is finalised exactly like
an ordinary word (autocorrect, capitalisation, committed with the delimiter) via a **recursive**
`finalizeAndCommit()` call; the "after" half becomes a fresh composing token, re-seeded at the caret's own new
position via the same `composingAnchor`/`composingCursor` mid-word-edit-point mechanism `reclaimSurroundingWord()`
already established (D-62/§58) - so editing continues right where the caret was, never jumped to the token's
end. The real composing region is shrunk to just the "before" text first (`ic.setComposingText(beforeText, 1)`)
before any of this, so finalising it does not silently replace the *whole* existing before+after composing span
and discard the "after" half, which exists only in memory until re-seeded. `pendingMergeChar` is explicitly
restored before delegating, so the recursive "before" finalisation still sees it correctly (an edge case not
otherwise reachable, kept consistent rather than silently dropped). Android service glue, no new test -
consistent with this project's established `AdaptKeyService` testing gap (no `InputConnection` Robolectric
shadow, see §46).

### D-120 - Mid-Word Punctuation Now Lands at the Caret, Not Always at the Token's End
Same fix as D-119, generalised: the mid-word check now lives *inside* `finalizeAndCommit()` itself (moved from
being SPACE-specific), so **every** delimiter that funnels through it - typed punctuation, a long-press symbol
via `commitLongPressSymbol()` (this round's reported repro: a quote typed before an existing word, via long-press,
previously landed after it) - is fixed for free, with no change needed at any of those call sites. Root cause
confirmed via trace: `ic.setComposingText(finalWord, 1)` always places Android's cursor *after* the whole
composed text (`newCursorPosition = 1`), regardless of where the logical caret actually was - `finalizeAndCommit()`
never had a concept of "the caret is mid-token" before this round at all.

### D-121 - `handleShift()` Now Checks Caps-Lock-Off First and Requires the Caret to Genuinely Be at the Word's End
One bug, two reported repros (a mid-word Backspace-then-Shift wrongly re-triggering G-05; a Caps-Lock-off Shift
press mid-word wrongly re-triggering G-05 instead of releasing Caps Lock) - both traced to the same function.
`handleShift()` reordered so the Caps-Lock-release branch (D-15) is checked *before* the G-05 word-end branch,
and the G-05 branch itself now requires `composingCursor == composing.length` (genuinely at the token's own
end), not merely `composing.isNotEmpty()` - a mid-word Backspace can leave composing non-empty with the caret
elsewhere, which previously still (wrongly) triggered `handleWordEndShift()`'s first-letter flip. Android
service glue, no new test - same established gap as D-119/D-120.

### D-123 - Suggestion-Bar-Tap's Trailing Space Now Survives the Punctuation Check
New one-shot guard field `suppressNextReclaimSpaceReset`: set alongside `pendingSuggestionSpace = true` in
`onSuggestionClicked()`'s `Kind.NORMAL` branch, consumed (and cleared) by `reclaimWordAtCaret()` instead of that
function's previous unconditional `pendingSuggestionSpace = false` reset. Confirmed via trace (§63) that a
suggestion-bar tap's own `commitText()` generates an asynchronous `onUpdateSelection` callback which - composing
already empty by then - calls `reclaimWordAtCaret()` as a plain side effect of its *own* commit, not a genuine
subsequent tap elsewhere; without the guard, that call cleared D-29's space-eating flag before the user's next
keystroke (the punctuation mark it exists to react to) ever arrived. Scoped to exactly one reclaim call, so a
genuine subsequent caret move (a real tap elsewhere) still resets the flag normally, same as before. Android
service glue, no new test - same established gap.

567 unit tests (was 560; +7: 4 `KeyboardLayoutTest`/`GreekLayoutTest` D-105 cases, 3
`DictionarySuggestionProviderTest` D-113/D-114 cases). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
**None of this round's items have been confirmed on a real device yet** - several (D-108, D-109, D-111/D-112,
D-119-D-121, D-123) are Android view/service glue this environment cannot exercise end-to-end (no
emulator/device, no `InputConnection` Robolectric shadow, per the established, repeatedly-documented limitation
of this environment).

## §65 - Backlog Round 17: Post-§64 Device Feedback

Captured, **not started**, per the usual rule - the user explicitly asked for these to be collected now, with
implementation planned as a separate, later round.

### D-124 - §60's Clipboard MIME-Type Filter Doesn't Actually Block a Non-Text File (Bug, Re-Opened)
§60 (v0.8.33) was left deliberately unconfirmed pending broader real-world use - this is that use, and it
surfaced a real failure: copying the app's own APK file offers it via the D-36 Quick Paste chip, which
`resolveClipboardText()`'s `clip.description.hasMimeType("text/*")` check should have suppressed entirely (an
APK is `application/vnd.android.package-archive`, not `text/*`). Not yet re-traced against the current code -
possible leads for the next pass: whether the specific source app (wherever the APK was copied from) declares a
`ClipDescription` MIME type at all, whether it reports something unexpected `hasMimeType` still matches, or
whether the `item.uri == null` fast path (plain-text items, unconditionally allowed) is firing instead of the
URI branch for this particular copy for some reason.

### D-125 - An Unknown Inflected Verb Form Must Not Be Silently "Corrected" to the Dictionary's Base Form
Reported: "beurteilst" (2nd person singular of "beurteilen") auto-corrects to "beurteilt" (a different real
inflected form, 3rd person singular / past participle) - the same class of bug as D-115's "merke"/"stimmen",
but framed as a systemic morphology gap rather than one more missing word. The user's own proposed scope: **not**
every inflection of every verb needs to be in the dictionary (95% of the value, per their own estimate, doesn't
need that) - but the regular, highly-productive German conjugation *patterns* (weak/regular verb endings:
-e/-st/-t/-en/-t/-en present tense, the regular preterite/participle patterns) should be recognised as
"plausible enough to leave alone" even when the exact surface form isn't a dictionary entry, without necessarily
being *offered* as a suggestion. This is a generalisation of D-115's fix (which patched two specific words) into
an actual rule: something that recognises "this token is a regular conjugation of a stem the dictionary already
knows" and extends A-01-style protection to it, distinct from (and a real alternative to) a full corpus rebuild
or hand-adding every inflection. Not yet designed - needs its own pass on what "regular enough" means precisely
and where in the pipeline (a new check alongside `isKnownWord`, most likely) this would plug in.

### D-126 - Tier-3 Mini-LLM Needs an Uninstall Action and an Independent Enable/Disable Toggle
Once a model has been imported (`Tier3ModelActivity`/`Tier3ModelInstaller`/`Tier3ModelStorage`), there is
currently no way to remove it again, and no toggle to turn the LLM off/on independently of whether a model file
happens to be present - "installed" and "active" are currently the same state. Two asks: (1) a settings action
to delete the imported model file (freeing the ~270+ MB it occupies) and (2) a switch that disables tier-3
inference even while a model remains installed (e.g. to save battery/latency without having to re-import later).
Ties into `Tier3ModelStorage`/the C-06 settings plumbing - not yet scoped against the actual current code.

### D-127 - Document "What Learns When" (Dev Docs)
Requested as its own task: a clear, consolidated write-up of exactly which user actions feed which learning
mechanism and when (T-03 offset model per confirmed tap per D-109's new ambiguous-tap exclusion; D-37's
count-based dictionary promotion/un-learn on undo; T-04 pattern seeding; the tier-3 adaptive-learning signal,
§9). This is meant to land in the project's own docs (this spec/progress file), not user-facing copy - a
reference for future sessions and for writing D-128's user-facing summary accurately.

### D-128 - Settings Needs a Prominent, Compact Explanation of What Adapts and Learns
The app is named **AdaptKey** - the user wants the settings screen to prominently (but compactly) explain what
justifies that name: which actions teach the keyboard something and how, explicitly covering the T-03 raw-touch
model and the tier-3 mini-LLM's adaptive-learning fade-out (§9 - the LLM is consulted less as the n-gram model
learns from it). Possible shape floated by the user: extend the existing D-89 `FeatureOverviewActivity` ("Was
AdaptKey alles kann") with a short, dedicated summary of these core learning/adaptation features, rather than
building an entirely separate screen. Depends on D-127's consolidated internal write-up existing first, so the
user-facing summary is accurate rather than reconstructed from memory.

### D-129 - Calculator's Minus Key Needs a Corner-Hint Glyph for Its Sign-Flip Function
§31 (v0.8.10) added the sign-flip-on-long-press feature to the calculator page's `−` key, explicitly accepting
"no corner-glyph visual cue" as a trade-off at the time (the key carries neither a `hint` nor `alternatives`, so
routing it through the ordinary hint-drawing path would have wrongly turned the gesture into a text-commit
action - see §31's own write-up). Revisiting that trade-off: the user now wants some corner indicator so the
long-press function is discoverable, without accidentally routing it back through the commit-text machinery
that was the reason it has none today. Needs its own small design pass (a corner glyph driven by a condition
other than "has a hint/alternatives", most likely).

### D-130 - A Real Language Switch After Sustained English, Plus a Space-Bar Acknowledgement Animation
Two related asks:
1. **Promote the A-03 per-token English routing to a real D-106-stage-1 active-language switch after
   sustained English input.** Today, typing English inline while German is active only ever routes
   individual tokens to the English dictionary (A-03's classifier, `LANGUAGE_WINDOW = 5`) - it never actually
   flips `activeLanguage` the way the G-01 swipe does. The user observes this already works well for the one
   thing it was built for (D-106 stage 2's cross-language autocorrect suppression), but questions whether the
   current "soft" per-token routing is still worth keeping *as well* now that stage 2 exists, and proposes:
   after **five** consecutive English words (not the classifier's own 2-word window), perform a genuine
   `LanguageCycle`-style switch to English as the *active* language, not just a per-token dictionary
   substitution. This is a real behavioural change to A-03/D-106's interaction, not yet designed - needs
   deciding whether this replaces or layers on top of the existing classifier routing.
2. **Acknowledge every language change - automatic or manual (G-01 swipe) - with a visible animation on the
   space bar**, not just the current text label swap. Two alternatives floated: a glow sweep travelling
   bottom-to-top across the space key's text, or a fade-out of the current label followed by a fade-in of the
   new one. Purely a visual-polish request ("sieht nett aus und ist auffällig") - no functional change beyond
   making the existing D-03 space-bar label swap more noticeable.

### D-131 - Raw-Coordinate Correction Should Become Visible Earlier While Still Typing
D-39's raw-coordinate correction is confirmed working well now, but currently only ever applies at
`finalizeAndCommit()` time (as the last fallback, once ordinary edit-distance autocorrect finds nothing) - it
has no live presence during composing the way the ordinary suggestion list and the §49 split preview do. The
user wants its effect visible earlier - ideally before the last letter of the word is even typed - so a
raw-coordinate respelling becomes a live, incremental confidence signal rather than something that only ever
resolves at the final delimiter. Not yet designed - would need `refreshSuggestions()` to also consult
`RawCoordinateCorrection.respellings(...)` against the in-progress token (mirroring how D-111/D-112 already
pulled a piece of `finalizeAndCommit()`'s own logic forward into live suggestions), which needs `composingTaps`
data for the token-so-far rather than the whole finished word.

### D-132 - Settings-Row Slide-In Animation Needs Polish
§50's `SettingsRowView` currently reserves its height immediately and animates only its inner content via
`translationY` - per the user, this reads as the row and its buttons "popping in" instantly rather than
sliding, which "looks odd". Requested: the buttons together with the whole row should slide up from below as
one piece, or - the user's own pragmatic alternative - achieve the same visual via a clip-based reveal rather
than a translation animation. Needs its own implementation pass against the current `SettingsRowView.open()`.

### D-133 - Bottom-Row Touch Zones Need a Harder, Direction-Specific Bound (D-109 Follow-Up)
D-109 (§64) already stopped ambiguous taps from training the model at all and tightened the general
`maxOffsetFactor` cap from 0.9 to 0.5 - the user wants a stronger, explicitly *directional* guarantee on top of
that general isotropic bound specifically for the bottom row (`c v b n m`): its keys' effective zones must never
be allowed to drift downward past the vertical middle of the key, keeping a hard, guaranteed margin against the
space bar regardless of how the general cap is tuned. This is a stricter, axis-specific version of D-109's
existing fix, not yet designed against `OffsetModel`/`logLikelihood()`'s current isotropic-fraction cap.

### D-134 - SPACE Sometimes Seems to Get Swallowed After a Full Stop (Unconfirmed Pattern)
The user notices a recurring pattern - pressing SPACE right after a `.` sometimes has no visible effect - but
is not fully certain it isn't their own mis-tap, and has not pinned down a precise repro yet. Flagged as
"never happened on Gboard". Possible leads for whenever this gets a proper trace: interaction with D-29's
space-eating-punctuation logic (though that only ever removes a space *before* punctuation, not after), the
auto-capitalisation-arming that follows a sentence-ending period, or an interaction with this round's own new
D-119/D-120 mid-word-split logic (shipped the same week this pattern was first noticed) - genuinely unknown
which, if any, of these is responsible without a precise repro.

### D-135 - Password-Manager (Autofill) Suggestions in the Bar - Big Idea, Feasibility Unknown
Requested: when typing into a recognised username/email field, offer a saved username from the Google Password
Manager as a suggestion; in a recognised password field, offer the matching saved password. The user
explicitly flags their own uncertainty about feasibility ("ich weiß nicht, wie Gboard das genau macht... 
vielleicht benötigen wir hier Schnittstellen, die wir nicht haben"). This is almost certainly the Android
**Autofill Framework** (`InlineSuggestion`s an IME can render inline in its own suggestion strip, the mechanism
Gboard is known to use for exactly this) rather than anything the keyboard could build unassisted - needs
dedicated research into that API's actual capabilities/constraints (what a non-Gboard IME can access, whether
it needs any special OS-level trust/permission Gboard has and a third-party app wouldn't) before any design or
implementation commitment. Captured as a large, speculative idea, not a scoped task yet.

### D-136 - Gesture-Area Control Contrast Is Poor Compared to Gboard
§42 already fixed the gesture-nav-inset strip's *background* colour (it previously showed whatever sat behind
the IME window). The user now flags a related but distinct issue: on Gboard, the system's gesture-area controls
(the pill/back-indicator etc.) render as dark icons on a light-grey background with good contrast; on AdaptKey
they are barely visible. Suspected mechanism, not yet confirmed: Android's system bars have a light/dark
*appearance* flag (`WindowInsetsController.setSystemBarsAppearance`/`APPEARANCE_LIGHT_NAVIGATION_BARS`) that
controls whether the OS draws these control icons dark-on-light or light-on-dark, independent of whatever
background colour the app itself paints beneath them - if AdaptKey's window never sets this (or sets it
opposite to what its own light keyboard background needs), the system would keep drawing light-on-light or an
otherwise mismatched combination. Needs verifying against the actual `AdaptKeyService`/window setup before
concluding this is really the mechanism.

### D-137 - A Typed Time Should Always Suggest "Uhr" as the Next Word
German convention: a time written as 1-2 digits, a colon, then 2 digits (e.g. "14:30") is essentially always
followed by "Uhr". Requested: detect this pattern and always offer "Uhr" as a D-43 next-word prediction
regardless of what the bigram table happens to know about this exact digit token (which, being effectively
unique per occurrence, likely has weak or no bigram evidence today). Needs a pattern-detection addition to
`showNextWordPredictions()`/`DictionaryStore.nextWords()`'s call site, not a data fix.

### D-138 - Backspace-Hold Now Feels Jerky/Stuttery (Possible Regression - Needs Investigation, Not Guessed)
Reported right after this session's own §64 batch shipped: held backspace now feels like it "checks something
in between" rather than repeating smoothly. Flagged plainly as a suspect, not a conclusion: this same round
touched `AdaptKeyboardView.onTouchEvent()`'s `ACTION_DOWN`/`ACTION_MOVE` handling twice - D-108 added a new
`pressedKeyRect`/`movedOutsideKey()` check (though it only gates the long-press cancellation branch, not
backspace-repeat's own `movedBeyondSlop`-based cancellation, which was left unchanged) and D-109 reordered the
T-05 `ambiguityBands.classify()` call ahead of `offsetModel?.record()` and made the record call conditional.
Neither change was intended to touch backspace-repeat's own cadence (`BackspaceRepeat`/`scheduleBackspaceRepeat`
are untouched code), but the timing coincidence with this exact area of `onTouchEvent()` having just changed is
too close to ignore. Needs an actual trace through `onTouchEvent`'s `ACTION_DOWN` handling for `KeyCode.DELETE`
next round, not a blind revert or guess.

## §66 - Release Round: D-124, D-115/D-125, D-130, D-138 + D-135 Feasibility Research (v0.8.36)

### D-138 - Backspace-Hold Jerkiness: Traced (Not Guessed) to This Session's Own Added Per-Keystroke Lookups
Before touching anything, traced the actual call path rather than reverting blindly: `handleBackspaceRepeat()`
calls `deleteComposingChar()` on every repeat tick, which calls `refreshSuggestions()` same as an ordinary
keystroke. This session's own §64/§63 changes added real per-keystroke cost to that function - D-106 stage 2's
`knownInOtherLanguage()` (up to 3 dictionary lookups) and D-111/D-112's `capitalizedPreview` (1 more
`store.partsOfSpeech()` lookup) - none of which existed before this session. `SqliteDictionaryStore`'s
`wkey TEXT PRIMARY KEY` schema makes each individual lookup cheap (an indexed point lookup), but stacking up to
4 extra SQLite round-trips onto *every* tick of a hold whose fastest interval is 45 ms (§59) is a concrete,
plausible mechanism for exactly the reported "jerky, seems to check things in between" - not a guess, but also
not something this environment can profile to *prove* is the (sole) cause.

**Fix**: `deleteComposingChar()` gained a `duringRepeat: Boolean` parameter, threaded from
`handleBackspaceRepeat()`'s call site only (the ordinary single-tap `handleBackspace()` path is unaffected);
`refreshSuggestions(duringRepeat: Boolean = false)` skips `knownInOtherLanguage()`/`capitalizedPreview()`
entirely when `true`, falling back to the plain `provider.autocorrectFor()` result - matching this function's
pre-§63/§64 behaviour for exactly this one call path. Nothing about D-106 stage 2 or D-111/D-112's actual
*protection* is weakened - `finalizeAndCommit()`'s own checks (unaffected by this parameter) still fully apply
once the hold stops and a real commit happens; only the *live bar preview* during a fast-changing, transient
hold is skipped, which nobody is meaningfully reading mid-repeat anyway.

### D-124 - Clipboard MIME-Type Filter: the Real Bug Was `hasMimeType`'s Own Wildcard Semantics
Traced precisely, not re-guessed: `ClipDescription.hasMimeType(String)` is documented to treat a clip whose own
declared concrete type is the generic wildcard (`ClipDescription.MIMETYPE_UNKNOWN`, many file-sharing/share-sheet
paths use this instead of a specific type) as matching *any* requested pattern - including a `text` request.
Copying the app's own APK (or plausibly any file shared via a path that only declares this generic wildcard)
made `resolveClipboardText()`'s `hasMimeType("text/*")` check return true for content that is not text at all.

**Fix**: new `isTextMimeType(clip)` reads the clip's own declared MIME type(s) directly
(`ClipDescription.mimeTypeCount`/`getMimeType(i)`) and requires a genuine `text/`-prefixed type, explicitly
excluding `MIMETYPE_UNKNOWN` - bypassing `hasMimeType`'s wildcard-matching behaviour entirely rather than trying
to out-guess it. (While writing this fix, avoided re-introducing §60's own already-once-caught KDoc bug:
writing the literal wildcard glyph sequence inside a `/** */` doc comment opens a nested Kotlin block comment
and swallows the rest of the file - worded around it this time by referencing `ClipDescription.MIMETYPE_UNKNOWN`
by name instead of spelling the literal pattern in prose.)

### D-115 / D-125 - Generalised: Unknown Regular Verb Inflections Are Protected, Not Just Two Patched Words
New pure `dictionary/RegularVerbInflection` object (7 tests): recognises a token as a plausible regular ("weak")
German verb inflection of a *known* infinitive by stripping a candidate personal ending (present tense
`-e/-est/-st/-et/-t`, preterite `-te/-test/-ten/-tet`) and checking whether the reconstructed `stem + "en"` is
itself a known dictionary word - e.g. `beurteilst` strips `-st` to `beurteil`, and `beurteil` + `en` =
`beurteilen`, a known word, so `beurteilst` is protected even though it is not itself in the dictionary. This is
the generalisation D-125 asked for: not every inflection needs its own dictionary row (D-115's approach for
`merke`/`stimmen`), just recognising the *productive pattern* is enough to stop a wrong autocorrect, without
necessarily surfacing the form as a suggestion. Deliberately data-free (no dictionary changes) and pattern-only
- a coincidental match against an unrelated `-en`-ending known word is possible in principle but low-probability,
an accepted trade-off of a simple heuristic; strong/irregular ("ablaut") verbs and participles stay out of
scope, matching the "95% via patterns, not exhaustive coverage" framing the user gave this.

Wired into `DictionarySuggestionProvider.bestCorrection()`'s A-01 guard: a plausible inflection is protected
*unconditionally* (no §44/D-113 ratio-override applies), since - unlike a literal known word - it has no
recorded frequency of its own to compare against a correction candidate's; applying the ratio check to it would
trivially always fire (`0 * ratio <= anything`).

### D-130 - Sustained-English Real Language Switch + Space-Bar Change Animation
Two parts, both building directly on already-shipped mechanisms (D-106 stage 1's `LanguageCycle`/`qwerty`
layout, A-03's classifier):

1. **`AdaptKeyService.trackSustainedEnglishUsage()`**: a new `consecutiveEnglishWords` counter increments each
   time `finalizeAndCommit()` routes a commit to English while German/Greek stays the *active* alphabet (via
   `resolveDict()`'s classifier branch, now exposed by changing `selectActiveDictionary()`'s return type from a
   bare `Boolean` to the full `DictChoice` so the caller can read which language was actually chosen, not only
   whether autocorrect was suppressed); resets to `0` the moment a non-English word commits. At
   `SUSTAINED_ENGLISH_WORD_THRESHOLD = 5` consecutive English commits, it performs a real active-language
   switch - the same state changes `toggleLanguage()` already makes (`activeLanguage`, `ActiveLanguageStore`,
   `keyboardView.qwerty`, the space label, a toast) - rather than continuing to rely on per-token routing
   indefinitely. D-106 stage 2's cross-language autocorrect protection (the mechanism the user confirmed
   already "funktioniert wunderbar") is completely untouched by this - this is purely about *when the active
   alphabet itself flips*, a different, additive concern.
2. **`AdaptKeyboardView.beginLanguageChangeFade()`**: a 260 ms cross-fade of the space bar's own label text -
   the previous label fades out over the first half, the new one (already pushed via the existing `spaceLabel`
   property by the time this runs) fades back in over the second half - implemented as a plain alpha animation
   on the shared `textPaint` (reset to full opacity immediately after each draw, since the paint is shared
   across every key's label), not a custom shader/glow shader. Chosen over the alternative "glow sweep" framing
   the user offered, since a straightforward two-stage alpha fade is robust to implement correctly without a
   bespoke gradient/shader animation and the user explicitly offered it as an acceptable alternative. Wired
   into both `toggleLanguage()` (the manual G-01 swipe) and the new automatic switch above, so both trigger
   paths get the same acknowledgement.

### D-135 - Google Password Manager / Autofill Suggestions: Confirmed Feasible (Researched, Not Implemented)
Researched against the current, official Android documentation rather than assumed from memory (this is
exactly the kind of API surface that's easy to misremember or that could have changed): **yes, this is
genuinely feasible for a third-party IME**, via the public, documented Android **Autofill Framework inline
suggestions** mechanism (not anything Gboard-specific or requiring undocumented platform access) - and it would
work with whatever autofill provider the *device* has configured (Google Password Manager by default on most
phones, but equally Bitwarden/1Password/etc. if the user has set a different one), not something tied
specifically to Google's own service.

**How it works, and what it would take:**
- The IME's method metadata (`res/xml/method.xml`) needs `android:supportsInlineSuggestions="true"`.
- `AdaptKeyService` would need to override two `InputMethodService` methods:
  `onCreateInlineSuggestionsRequest()` (declares how many suggestions are wanted and an `InlinePresentationSpec`
  - size/style constraints only) and `onInlineSuggestionsResponse()` (receives an `InlineSuggestionsResponse`,
  inflates each `InlineSuggestion` into a real `InlineContentView` to display).
- **The IME never sees the actual credential data at all** - each suggestion is an opaque, platform-rendered
  `View` the autofill service itself draws (its own branding/lock-icon/biometric-prompt behaviour and all);
  AdaptKey could only control *where* and *how large* to place these views (and loosely nudge their text/colour
  style via the presentation spec), not read or restyle their content into AdaptKey's own chip look. This is a
  deliberate platform privacy boundary, not a limitation specific to a third-party IME versus Gboard.
- The platform decides *when* to call `onCreateInlineSuggestionsRequest()` at all, based on the target field's
  own declared autofill hints/`inputType` - AdaptKey would not need to build its own username/password field
  detection for this to trigger correctly.
- **Minimum API level: 30 (Android 11)** - AdaptKey's `minSdk` is 26, so this would need to be implemented as an
  optional, version-gated feature (a no-op below API 30), not a `minSdk` bump.
- Entirely dependent on the device actually having an autofill service configured/enabled at all; a no-op,
  graceful fallback when it isn't.

**Not implemented this round** - confirmed genuinely buildable with a clear shape, but a real scoped feature of
its own size (a new settings toggle likely wanted too, given the visual "foreign" rendering these suggestions
would necessarily have compared to AdaptKey's own chips) - a candidate for its own future implementation round,
not folded into this one.

Sources: [Integrate autofill with IMEs and autofill services (Android Developers)](https://developer.android.com/identity/autofill/ime-autofill).

575 unit tests (was 567 before this round; +8: 7 `RegularVerbInflectionTest` + 1
`DictionarySuggestionProviderTest` D-115/D-125 case). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
D-124/D-130/D-138 are Android service/view glue, no new tests, consistent with this project's established
testing gap; D-115/D-125's pure recognition logic is fully unit-tested.

## §67 - D-135 Implemented: Autofill Inline Suggestions (Password Manager) (v0.8.37)

Builds directly on §66's feasibility research - the documented, public Android Autofill inline-suggestions
API (API 30+), not anything Gboard-specific.

### New dependency and manifest declaration
`androidx.autofill:autofill:1.3.0` added (a small, plain compat helper library - `UiVersions`/
`InlineSuggestionUi` style-bundle builders; does not raise `minSdk`). `res/xml/method.xml` gained
`android:supportsInlineSuggestions="true"`, the flag that tells the platform AdaptKey wants to render these
inline rather than falling back to a system dropdown.

### `AdaptKeyService` - two new `@RequiresApi(Build.VERSION_CODES.R)` overrides
- `onCreateInlineSuggestionsRequest(uiExtras)`: builds one shared `InlinePresentationSpec` sized to the
  ordinary suggestion bar's own height (min `Size(0, 0)`, max full screen width × bar height) with the
  standard `UiVersions.INLINE_UI_VERSION_1` style bundle (`UiVersions.newStylesBuilder().addStyle(
  InlineSuggestionUi.newStyleBuilder().build()).build()` - **note**: the real class lives at
  `androidx.autofill.inline.v1.InlineSuggestionUi`, not the `androidx.autofill.inline.suggestions` package
  guessed initially from memory before verifying against the actual downloaded AAR's class list
  (`unzip`/`javap` against the resolved `autofill-1.3.0.aar` - checked directly rather than trusting
  recollection of an API surface this project had never used before). Requests up to
  `INLINE_SUGGESTION_MAX_COUNT` (3) suggestions.
- `onInlineSuggestionsResponse(response)`: for each returned `InlineSuggestion`, calls `.inflate(...)` (async,
  `mainExecutor`) and adds the resulting opaque `InlineContentView` to the new `inlineSuggestionsBar` once
  ready; returns `false` when the response is empty (nothing to show - reverts to the ordinary suggestion bar)
  and `true` once at least one inflated view has been added, per the API's own contract for "the IME is
  handling display itself".

### New `keyboard/InlineSuggestionsBarView`
A plain `HorizontalScrollView`-wrapped `LinearLayout` container (`addSuggestion(view)`/`clearSuggestions()`/
`hasSuggestions`) - deliberately an ordinary `ViewGroup`, not a custom `Canvas`-drawn view like
`AdaptKeyboardView`/`SuggestionBarView`, since each suggestion is a real, opaque `View` the platform itself
inflates and owns; AdaptKey can only place it, never draw or restyle its content (the documented privacy
boundary - AdaptKey never receives the underlying username/password text at any point). Occupies the exact
same row/slot as the ordinary suggestion bar (`root.addView(inlineBar, ...barHeight)`, right after `bar`),
toggling visibility with it: showing inline suggestions hides the ordinary bar and vice versa, since both
would otherwise compete for the same slot.

### Lifecycle wiring
New `resetInlineSuggestions()` (clears and hides the row, restores the ordinary bar) called from
`onStartInputView()` - every fresh field starts clean rather than carrying over a previous field's stale
suggestions; a new `onCreateInlineSuggestionsRequest()`/`onInlineSuggestionsResponse()` round-trip (driven
entirely by the platform, not by AdaptKey) supplies fresh ones, or none, for whatever is now focused.

### Scope notes
No new settings toggle this round (§66's write-up had speculated one might be wanted) - the feature is
purely additive and invisible unless the platform actually has a real suggestion to offer for the focused
field, so there is no real annoyance/behaviour-change downside to weigh against a toggle's own complexity;
can be added later if real-device use suggests otherwise. No new tests - both the view (a thin `ViewGroup`
wrapper with no decision logic) and the service overrides (platform callback glue, `@RequiresApi`-gated,
calling into `InlineSuggestion.inflate()` which cannot be exercised without a real device/autofill service)
are consistent with this project's established Android-glue testing gap - **this entire feature is
unverified beyond compiling and the API contract read from documentation + the actual resolved AAR's class
list; it needs a real Android 11+ device with an autofill service configured to confirm anything renders at
all.**

575 unit tests (unchanged - no new pure/testable logic this round). `:app:assembleDebug`/
`:app:testDebugUnitTest` green.

## §68 - Device Feedback on §64-§67: D-124 Follow-Up, D-111 Clarified as D-115's Pattern (v0.8.38)

Device confirmation on the §64-§67 batch: **D-105, D-108, D-130, D-138 confirmed working.** Two follow-ups:

### D-124 - Real Gap Found: the MIME Check Only Ever Covered the URI Branch
§66's fix (`isTextMimeType()`, checked in place of `ClipDescription.hasMimeType()`'s wildcard-matching
gotcha) did not fix the reported case - confirmed still broken on device. Re-traced rather than re-guessing:
`resolveClipboardText()`'s structure was `val uri = item.uri ?: return item.coerceToText(this)` *before* the
MIME check - so the check only ever ran on the URI branch. Whatever copied the APK onto the clipboard
apparently populates `ClipData.Item.getText()` directly (a filename, a content-URI string, or similar)
rather than `getUri()`, which takes the `?:` fallback straight to `item.coerceToText(this)` - completely
bypassing `isTextMimeType()`. Fixed by moving the MIME check to run first, unconditionally, before branching
on which of `getUri()`/`getText()` this item actually has. An ordinary text copy (`ClipData.newPlainText`, what
any normal "Copy" action produces) already always declares `text/plain`, so this costs the common case
nothing. **Not yet re-confirmed on device** - if this still doesn't resolve it, the next step needs to know
exactly which app/action was used to copy the APK, since two independent theories have now been tried and
ruled out or fixed.

### D-111 - Not a Preview Bug: "stelle"/"Stelle" Is D-115's Exact Mechanism, Confirmed
Reported: "stelle" previews as "Stelle" (D-111 working as designed) but still auto-applies - read at first as
"the preview doesn't prevent auto-apply", but traced before assuming a behavioural change was needed. Checked
`dict_de.tsv` directly: `Stelle` (freq 3497) was tagged plain `NOUN`, and `stelle` (the "ich"-form of
"stellen", a genuine, comparably common verb reading - "die Stelle" / "ich stelle") has no separate entry -
**exactly** D-115's `stimmen`/`Stimmen` mechanism (§6 rule 3 auto-capitalises because the dictionary never
flagged the ambiguity), not a gap in D-111's preview mechanism at all - the preview is doing its job (showing
what §6 will do); the thing it shows is what's wrong, for the same reason as before. Fixed identically:
`Stelle	3497	NOUN` -> `Stelle	3497	NOUN,OTHER` in `dict_de.tsv`.

**Systematic follow-up, not yet acted on beyond `Stelle`:** given this is now the second real report of the
identical pattern, scanned the whole bundled `dict_de.tsv` for the general shape (a `NOUN`-tagged word whose
lower-cased form, minus a trailing `e` plus `en`, is a separately known, non-`NOUN`-only word - i.e. every
noun that is *also* the plausible "ich"-form of a real verb). **Found 141 candidates.** Many are exactly the
`stimmen`/`stelle` class (comparably common in both readings - `Liebe`/`liebe`, `Sorge`/`sorge`,
`Suche`/`suche`, `Pflege`/`pflege`); some are much more lopsided (`Ende` 15847 vs. `enden` 630, `Ware` 263 vs.
the unrelated-but-textually-matching `waren` 31549) where the noun reading so overwhelmingly dominates that
blanket-marking them ambiguous would trade a rare false-positive fix for real, felt friction on very common,
currently-correct auto-capitalisation. **Deliberately not bulk-applied** - this is a real product trade-off
(safety against a rare mis-capitalisation vs. typing fluency for an overwhelmingly-common word), not a pure
correctness fix like the two already-confirmed cases, so it needs the user's own call on how far to go (fix
only as individually reported; apply everywhere regardless of the lopsidedness; or apply above some
frequency-ratio cutoff) rather than a unilateral bulk data change.

## §69 - D-124 Closed (Not a MIME Bug), D-116 Implemented: Unhyphenated-Compound Recognition (v0.8.39)

Device confirmation on the §64 batch: **D-114 confirmed working; D-119/D-120/D-121/D-123 provisionally
confirmed** ("scheinen zu klappen" - not yet stress-tested as thoroughly as the already-CONFIRMED §68 items).

### D-124 - Closed: Not the Suspected Mechanism at All, Not Fixable via MIME Detection
Traced with the concrete repro this time (copying the app's own APK via **Total Commander**): the Quick
Paste chip's own preview showed a plain, readable text label ("Total commander file li…", truncated at
`ClipboardPreview.MAX_LENGTH`) - not garbled binary content. This rules out both of the mechanisms the two
prior fixes (§66, §68) addressed: there is no non-text file being coerced into text here at all. Total
Commander evidently puts a **plain-text placeholder string** (most likely for its own internal drag/paste
protocol between TC instances) directly into `ClipData.Item`'s text, declared as genuine `text/plain` -
which, from the OS's and AdaptKey's point of view, **is** exactly what it claims to be: ordinary pasteable
text. No `ClipDescription` MIME check, however precise, can distinguish "genuine text a user wants to paste"
from "an app's own internal placeholder string that happens to also be plain text" - the two are
indistinguishable at the API level.

The only way to suppress this specific case would be a hardcoded match against Total Commander's own
placeholder string - explicitly rejected as exactly the kind of app-/string-specific hack this project
avoids elsewhere (D-118's "trace before fixing" and the general-rule-over-individual-word-patching
philosophy of §62 apply equally to app-specific patching). **Closed as a known, accepted limitation**, not a
bug: the D-36/§60 mechanism (detect and block a real file being offered as text) works correctly and remains
in place; a source app choosing to expose its own internal placeholder text as plain clipboard text is
outside what any MIME-based filter can address.

### D-116 - Unhyphenated Compound Recognition: Known Noun First Part + Correctable Rest (Suggestion-Only)
The reported case ("Beitragsjahreb", a typo of "Beitragsjahren") was never actually a correction-quality
bug: `b`/`n` is an ordinary QWERTZ-adjacent, cost-1 typo relationship (the same class D-67 already
autocorrects elsewhere) - the real obstacle is that "Beitragsjahren" itself is too rare a compound to be in
the bundled dictionary at all (unlike B-01's hyphenated case, there is no explicit compound boundary marker
to anchor a fix on). Full recursive decomposition-plus-fuzzy-matching was discussed and deliberately
rejected as disproportionately expensive and error-prone for the value it would add; the released design is
a narrower, single-split-point approach agreed on directly with the user:

**New pure `dictionary/CompoundSplit`** (8 tests): tries to peel a **known noun** (≥4 characters) off the
front of an unknown token, optionally followed by a German Fugenelement (`s`/`es`/`en`/`er`/`n`/`e`, or none
at all for a direct concatenation like `Haus`+`schuh`), such that the remainder (≥2 characters) is itself
accepted by a caller-supplied `resolveRest` callback. Searches the **longest** possible first part first (a
shorter one is more likely a spurious coincidental match - the classic German compound-splitting ambiguity,
e.g. "Wachstube" as "Wachs"+"tube" vs. "Wach"+"Stube", both individually valid), and only a single split
point is tried, deliberately not recursing into further parts - a typo inside the first part itself is out
of scope, matching how the request was framed ("den ersten Teil sauber erkannt").

**Wired into `DictionarySuggestionProvider.suggestionsFor()`** (3 new tests) via a new private
`compoundCandidate()`: the first-part check requires the candidate to be tagged `PartOfSpeech.NOUN` (the
user's own explicit scoping call - unsure whether nouns alone will cover enough cases long-term, but a
reasonable, revisitable starting point, and Fugenelemente are grammatically a noun-compounding phenomenon
anyway); the rest is resolved via `isKnownWord()` when already known, else `highConfidenceCorrection()` -
reusing D-67's existing cost-1 ceiling and D-114's frequency floor rather than inventing a separate
confidence policy for this feature. **Deliberately gated on `candidates.isEmpty()`** - the compound search
only ever runs once ordinary prefix/fuzzy matching found nothing at all, both because that is genuinely when
it is needed (a real compound has no single-word neighbour close enough for D-12 to find) and to keep its
handful of extra store lookups off the common keystroke path entirely - D-138 is this project's own standing
reminder that stacking several per-keystroke lookups onto every composing step is a real, previously-felt
cost, not a theoretical one.

**Suggestion-only by explicit user decision, never wired into `autocorrectFor()`/`highConfidenceCorrection()`
itself**: given the split point can be genuinely ambiguous (the Wachstube case above), a wrong guess must
only ever be offered, never silently applied - "dann ist man auf der sicheren Seite". Verified directly: a
constructed compound candidate appears in `suggestionsFor()` but `autocorrectFor()`/`highConfidenceCorrection()`
both still return null for the same input.

586 unit tests (was 575; +11: 8 `CompoundSplitTest`, 3 `DictionarySuggestionProviderTest`).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-tested - `CompoundSplit` itself is fully
pure/unit-tested, but real German compound coverage/false-positive rate can only really be judged against
everyday typing on a real device.

**D-116 CONFIRMED WORKING on device** (user confirmation, no code change).

## §70 - Settings Row: New Clear-Clipboard Button (v0.8.40)

New idea raised directly, not part of any prior backlog entry: a third button in the §48/§50 swipe-up
settings row, immediately left of the settings gear, that wipes the clipboard on tap.

`SettingsRowView` gained `clearClipboardButton` (glyph `🗑`, the standard wastebasket emoji - matching the
row's existing style of plain `TextView`s showing a Unicode glyph rather than a drawable asset, exactly like
the emoji button's `😊` and the gear's `⚙`; this project draws its own keyboard rather than depending on
bundled icon assets, and this row already followed that convention before this round) and a new
`OnClearClipboardClickListener`. Positioned via the same `Gravity.END`-anchored `FrameLayout` approach as the
settings gear, offset by the gear's own width plus one more button-margin so the two read as a distinct pair
at the row's right edge, leaving the emoji button alone at the left edge unchanged.

`AdaptKeyService` wires it to a new `clearClipboardFromSettingsRow()`, which closes the row and calls the
*already-existing* `clearClipboard()` - the same function the D-36/D-38 quick-paste flow already calls
automatically after a sensitive paste (`ClipboardManager.clearPrimaryClip()` on API 29+, a
`ClipData.newPlainText("", "")` overwrite below that) - now also reachable directly, on demand, via this
button. No new clearing logic was needed at all, only a new trigger for the existing one. Follows the
established "tapping either row button closes the row" convention (§50/§51) unchanged.

**Known, accepted minor edge case, not addressed this round**: the D-36 clipboard-paste chip is only
(re-)evaluated once, when a field opens (`showClipboardChipIfAvailable()` in `onStartInputView`) - clearing
the clipboard via this new button does not retroactively hide an already-showing paste chip for the current
field. Tapping a stale chip afterwards is harmless (it pastes nothing, since the clipboard is genuinely
empty by then) - the same latent behaviour already existed for any clipboard clear happening via another
route (a different app, the system's own clipboard UI); not a new risk introduced by this button, and not
worth adding a live clipboard-change listener for, unless device use suggests it is actually felt in
practice.

No new unit tests - `SettingsRowView` is Android view/animation glue with no decision logic of its own
(same established gap as the rest of the row, see §50), and `clearClipboardFromSettingsRow()` is a one-line
service-glue wrapper over the already-tested-by-precedent (Android glue, not pure logic) `clearClipboard()`.
586 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-tested.

## §71 - D-122 Implemented: Mid-Word Connector-Split Suggestion (v0.8.41)

First real repro for D-122, traced end-to-end before implementing anything. Repro: type "Testvwort hallo",
then place the caret inside "Testvwort" (mid-word re-edit) - expected a "Test Wort" suggestion (the
accidental `v` connector, one of [TokenRepair.OVER_SPACE_LETTERS]); the bar stayed completely empty instead.

**Root cause, two compounding, individually correct behaviours:** (1) `TokenRepair.trySplit()`'s §45
bigram-co-occurrence gate applies uniformly to the connector-drop strategy too - "test" and "wort" never
co-occur in the bundled corpus, so `trySplit("testvwort", …)` correctly finds nothing, by the same design
that keeps ordinary typos (`"meinst"` -> `"mei st"`) from being cut apart on zero evidence. (2) Even where
`trySplit()` *does* find a candidate, its result was never wired into the suggestion bar at all - only into
the live composing-text colour preview (§49's `splitPreview()`, itself deliberately suppressed during
mid-word editing by D-26's `isEditingMidWord` gate, to avoid mis-colouring a reclaimed fragment) or the
silent A-05 auto-commit at delimiter time. There was no middle path of "offer it, let the user decide" for a
split candidate at all, unlike ordinary autocorrect.

**Fix, matching the design agreed on directly:**

New `TokenRepair.splitAtUnresolvedConnector(token, previousWord)` (6 new tests): the connector-drop
strategy only, deliberately **without** `trySplit()`'s bigram-co-occurrence requirement - both halves must
still be known, non-blacklisted words (the existing `candidateAt`/`isWord`/`score` helpers, unchanged).
Consulted **only** while `AdaptKeyService.isEditingMidWord()` is true (mirroring D-26/§49's own gate) - the
user having deliberately tapped back into an existing word is a far stronger "please fix this" signal than
ordinary forward typing, which is exactly why the bigram gate can be relaxed here without reopening §45's
"any two known fragments get cut apart" false-positive problem for everyday typos.

New `AdaptKeyService.midWordConnectorSplitSuggestion(input)`, consulted from `refreshSuggestions()` (skipped
during `duringRepeat`, consistent with D-138): pre-capitalises the split exactly as `applySplit()` would
(`contextFor(split.left)` / `followingPartContext()`) and wraps it as a `Suggestion("$left $right",
SPLIT_SUGGESTION_SCORE)` with a deliberately maximal score (1e9 - comfortably above any real dictionary
frequency, but far below `Double.MAX_VALUE` to avoid any risk in downstream arithmetic) so it sorts first -
"noticeably higher priority", as D-122 originally asked. **Kept out of the `candidates` list itself** and
appended only at the point of display (`withSplitSuggestion()`, called at every `controller.update()` site):
feeding it into `candidates` would have made it `SuggestionMerger`'s own `maxTier1` normalisation baseline,
compressing every ordinary tier-1 score toward zero - a real, if subtle, side effect worth avoiding rather
than accepting.

**Suggestion-only, exactly like D-116** - never wired into `autocorrectFor()`/`highConfidenceCorrection()`,
so it can never silently apply on its own. Tapping it (`AdaptKeyService.onSuggestionClicked()`'s `NORMAL`
branch, extended to recognise a suggestion word containing a space - no other suggestion source in this
codebase ever produces one, a safe, unique signal) delegates to the *existing* `applySplit()` via new
`applyMidWordSplitSuggestion()`, so per-half capitalisation, A-07 undo arming (a backspace immediately after
reverts to "Testvwort") and D-13 learning all match the already-established, already-tested A-05 commit path
exactly, rather than a parallel reimplementation.

592 unit tests (was 586; +6 `TokenRepairTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-tested - the pure split-recognition logic is fully unit-tested, but the suggestion-bar wiring and tap
handling are `AdaptKeyService`/`InputConnection` glue, the established testing gap for this layer.

## §72 - Bug Fixed: Mid-Word SPACE Left the Caret Before, Not After, the Inserted Space (v0.8.42)

Reported directly, with a precise repro: type "Testcwort" (an unsplit double-word, `c` the accidental
connector), place the caret right after the `c`, delete it with Backspace, then press SPACE to re-insert it
as a real space - the space is correctly inserted, but the caret afterwards sits **before** it instead of
after it (i.e. still between "Test" and the space, not between the space and "wort").

**Root cause, traced (not guessed):** this is the D-119/D-120 `splitComposingAtCaretAndCommit()` path (a
mid-word SPACE splits the composing token at the caret). After the "before" half ("Test") is finalised via a
recursive `finalizeAndCommit()` call, the "after" half's new anchor was computed by **re-reading**
`InputConnection.getExtractedText()` - still inside the same outer batch edit, and *after* several prior
`InputConnection` mutations already issued within that same call (the recursive finalise's own
`setComposingText()`/`finishComposingText()`/`commitText()`). Every *other* same-batch state read in this
class reads *before* it mutates anything (`reclaimSurroundingWord()`'s own anchor read, for instance) - this
was the one place reading *after* several prior edits in the same batch, a markedly riskier pattern. Confirmed
this is the real mechanism (not a coincidence) by checking whether the reported repro could instead be an
autocorrect-length-mismatch artefact: it cannot - "Test" does not change length under §6 capitalisation here
(`test` -> `Test` is a pure case change), so even in this exact zero-length-delta case the caret still landed
one position short of where the space actually was - pointing squarely at the anchor read itself being stale
by exactly the space's own width, not at anything downstream.

**Fix:** the "after" half's anchor is now computed **arithmetically**, with no read-back at all.
`finalizeAndCommit()` (and every path it can delegate to - `commitVerbatim()`, `applyMerge()`, `applySplit()`,
and its own default inline commit) now returns the number of characters it actually, net, inserted into the
document (word length + delimiter length, minus one for `applyMerge()`'s own preceding-space deletion).
`splitComposingAtCaretAndCommit()` captures the "before" half's own anchor *before* recursing into
`finalizeAndCommit()`, then computes the "after" half's anchor as `beforeAnchor + committedLength` - a value
derived entirely from state this function already controls, immune to any question of whether a same-batch
`InputConnection` read reflects the edits just issued to it. This is a stronger fix than merely restructuring
the batching to force a flush before reading, since it removes the read-after-write dependency altogether
rather than trying to make it reliable.

No new unit tests - this is `AdaptKeyService`/`InputConnection` glue with no new pure logic (the changed
functions were already Android-glue, the established testing gap for this layer; the arithmetic itself is a
one-line addition, not a standalone unit worth extracting). 592 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet re-confirmed on device.

## §73 - Backlog Capture: D-139 (Intermittent Text Jitter / Character Scramble, No Repro Yet)

Captured, **not started** - no code change this entry, no repro yet either.

### D-139 - Occasionally the Whole Text Jitters and Its Characters Get Scrambled, Recoverable Only by Clearing Everything
Reported as an intermittent, disruptive glitch first noticed roughly four or five builds ago (no precise
version pinned yet): occasionally the entire text in the field visibly jitters and its characters end up
wildly out of order: the only practical recovery is to delete everything and start over. **No reliable repro
exists yet** - explicitly flagged as such by the user, who will report back once one is found. Nothing here
is implemented or even root-caused; this section only records the report plus honest, clearly-labelled
speculation about plausible mechanisms, to make it easier to recognise the cause quickly once a repro exists.

**Plausible mechanisms (speculation, not a diagnosis):**

- **An `onUpdateSelection()` feedback loop.** The D-62/D-87 mid-word-reclaim machinery
  (`reclaimWordAtCaret()`/`reclaimSurroundingWord()`) already depends on an "is this cursor movement genuinely
  the user's, or just the echo of our own just-issued edit" check to avoid re-triggering itself - this exact
  class of self-triggering has been the root cause of several past bugs in this area (§32's D-87 investigation,
  D-123's suggestion-tap echo). If that check ever fails to recognise a self-issued edit as its own, the
  callback could re-invoke `reclaimWordAtCaret()`, which edits the text again, which fires the callback again -
  an unbounded (or slowly-diverging) loop of small edits. This would plausibly *look* like the reported
  "jitter", and if the reclaimed range drifts wrong on each iteration, could plausibly scramble character
  order the way "durcheinander gewürfelt" describes. This mid-word-editing machinery has seen real churn very
  recently (D-116, D-122, §72's anchor-arithmetic fix) - not evidence those introduced this bug (the user's own
  timeline of "four or five builds ago" points earlier, and §72 specifically *reduced* risk in this exact area
  by removing a read-after-write dependency), but it is the area with the most recent activity and the
  strongest structural resemblance to the symptom, so it is the first place worth looking once a repro exists.
- **A specific target app/editor's `InputConnection` behaving unexpectedly under batched edits.** Precedent:
  D-110 (eBay Kleinanzeigen's auto-cap failure) is a confirmed instance of a specific app's `InputConnection`
  not behaving as this IME assumes. If the jitter only ever happens in one particular app, that would point
  here instead of at AdaptKey's own state machine.
- **Less likely, noted for completeness:** a stale async tier-3 result slipping past the `tier3RequestSeq`
  guard - the guard checks `composing.toString() == input` at apply time, which could coincidentally still
  match after the token changed and changed back. This would more plausibly cause a wrong *suggestion* than
  actual text corruption, so it is a weaker candidate than the two above.

**What would help most, once a repro is found:** which app/field it happened in; whether it correlates with
mid-word editing (tapping back into an already-typed word) specifically, or can happen during plain forward
typing too; whether it correlates with a held key (backspace-repeat) or a fast burst of keystrokes; and
whether disabling C-06 (the tier-3 mini-LLM, if a model is installed) changes anything. Per this project's own
rule, no fix will be attempted from speculation alone.

## §74 - Device Confirmations: D-106/D-111-D-113/D-122/§72; D-135 Negative Result (finanzen.net zero)

No code change this entry - device-feedback status update only.

**Confirmed working:** D-122 (mid-word connector-split suggestion), the §72 caret-position fix, and (a
blanket confirmation covering the remaining older, previously-unconfirmed items) D-106 stages 1+2 (English as
an explicit language, cross-dictionary loanword protection), D-111/D-112 (pending-capitalisation-change
preview, correction-follows-context casing) and D-113 (§44 ratio-override restricted to cost-1 typos).

### D-135 - Autofill Inline Suggestions: No Suggestion Shown for the Username Field in "finanzen.net zero"
Tested against a real login field (finanzen.net zero, a brokerage app) - no inline suggestion appeared for
the username field. Not yet traced - no device logs available in this environment, and several genuinely
different causes would all look identical from the outside ("nothing shows up"), so guessing at a fix would
likely just be wrong:

1. **No saved credential for this app in the configured autofill service** - the most mundane explanation
   and worth ruling out first: does the device's autofill service (Settings → Passwords/Autofill) actually
   have a saved login for finanzen.net zero already? With nothing saved, there is nothing for the platform to
   ever offer, regardless of anything AdaptKey does.
2. **The field itself may not be autofill-relevant per the app's own declaration** - per the Autofill
   Framework's own design (§66/§67), the platform decides *whether* to call
   `onCreateInlineSuggestionsRequest()` at all based on the target field's own declared `autofillHints`/
   `inputType`; AdaptKey has no influence over this. Notably, **finance/brokerage apps quite commonly disable
   autofill deliberately on login fields**, as an explicit anti-phishing/security measure - this would be a
   real, intentional restriction on the app's side, not a bug in AdaptKey at all, and is arguably the single
   most likely explanation given the specific *kind* of app this is.
3. **The configured autofill service might not support inline suggestions specifically** - some autofill
   providers only implement the older dropdown-style UI, not the inline-in-IME mechanism `onInlineSuggestions
   Response()` depends on; autofill being "enabled" is not the same as inline suggestions being supported.
4. **An actual bug in AdaptKey's own integration** - possible, though §67's implementation was checked against
   the real resolved `autofill-1.3.0.aar` class list rather than assumed; this whole feature was explicitly
   flagged as "unverified beyond compiling" pending exactly this kind of real-device test.

**Not fixed or further investigated this round** - deliberately not guessing among four candidates that would
look identical from the outside. To make progress, the next report would ideally answer: does the device's
autofill service have a saved login for this app already; what autofill service is configured (Google
Password Manager / a third-party one); and - the most diagnostic single test - does an inline suggestion
appear for *any* login field in *any* app with AdaptKey active, or has it never been observed working at all
yet. If it turns out to be case 2 (the app itself opts out), that would not be an AdaptKey bug to fix at all.

## §75 - Batch Release: D-117, D-126, D-127/D-128, D-129, D-131, D-132, D-133, D-136, D-137 (v0.8.43)

Released together per explicit user instruction, one round mirroring §64's own batch-release shape.

### D-117 - A Multi-Typo Word Beyond D-28's Budget Still Surfaces as a Suggestion
New `DictionarySuggestionProvider.wideFuzzyNeighbours()`: the same first-character-bucket search as D-12's
`fuzzyNeighbours()`, but at a wider edit-cost budget (`WIDE_CORRECTION_COST = 4` vs. the ordinary
`MAX_CORRECTION_COST = 2`), restricted to tokens of at least `MIN_WIDE_FUZZY_LENGTH = 6` characters (a wide
budget on a short token would match almost anything) and only ever tried once every cheaper search above
found nothing at all - the same `candidates.isEmpty()` fallback gate as D-116. Reaches the motivating example,
`erkamm` -> `erkannt` (two adjacent-key substitutions plus an insertion, cost 4). **Suggestion-only**: D-28's
own tight, autocorrect-grade budget is completely untouched, so this never silently applies on its own -
verified directly (`autocorrectFor`/`highConfidenceCorrection` both still return null for the same input).
Scope limitation, noted honestly rather than solved: still only searches the token's own first-character
bucket (its letter or a keyboard neighbour), so a token whose very first letter is *also* badly garbled stays
out of reach - a genuinely open question per D-117's own spec entry, not attempted here. 4 new tests.

### D-126 - Tier-3 Independent Enable Toggle (the Uninstall Action Already Existed)
Traced before implementing: `Tier3ModelActivity` already had a working "remove model" button
(`removeModel()` → `Tier3ModelInstaller.clear()`) - the uninstall half of D-126 was already done, just never
marked confirmed in the backlog. Implemented the other half: new `tier3Enabled: Boolean` (default on) through
the full settings pipeline (`RawSettings`/`AdaptSettings`/`SettingsMapper`/`SettingsStore`,
`d126_tier3_enabled`) plus a `SwitchPreferenceCompat` right below the model-import entry.
`AdaptKeyService.reconcileTier3Provider()` now gates on `settings.tier3Enabled && Tier3ModelStorage.
isModelInstalled(this)` instead of installed-state alone, and is called from `applySettings()` itself (removed
the now-redundant standalone call in `onStartInputView()`) so toggling the switch takes effect immediately
while the service is resident, not only on the next field focus - a model can now stay imported while
inference is switched off, e.g. to save battery/latency without deleting and later re-importing the ~270+ MB
file. 1 new test (`SettingsMapperTest`).

### D-127 / D-128 - What Learns When (Internal Reference + a User-Facing Settings Summary)

**D-127 - internal reference, consolidated here for future sessions:**
- **T-03 (`OffsetModel`)**: learns from every confirmed tap (`AdaptKeyboardView.onTouchEvent`'s `ACTION_DOWN`
  → `record()`) - except a tap D-109 flags as space/letter-ambiguous, which is never recorded at all (the
  root cause D-109 fixed). Bounded by D-109's isotropic cap and now D-133's bottom-row-specific downward cap.
- **D-37 (dictionary, `learnWord`)**: a word already known is reinforced immediately (frequency + bigram bump).
  A genuinely new word is only counted via `PendingLearnStore` and promoted to the real dictionary after
  `LEARN_THRESHOLD = 2` commits, so a one-off typo is never learned as if it were a real word on the first go.
- **D-13 (`learnWordStrong`)**: a *deliberate* correction (undoing a wrong A-05 split) promotes the rejoined
  word immediately, bypassing D-37's threshold - a deliberate correction is a much stronger, unambiguous
  signal than merely typing the same word twice.
- **T-04 (`PatternSeed`)**: not continuous learning - a one-time initial seed applied when a typing pattern is
  chosen in Settings (K-01), front-loading a plausible starting shape for T-03; the model keeps learning from
  real taps afterwards regardless of the seed.
- **§9 tier-3 adaptive learning (`reinforceFromTier3`)**: after a commit, a word the mini-LLM was confident
  about but tier-1's own dictionary didn't already know is fed back into the *same* D-37 pipeline as a
  reinforcement signal. This is the actual mechanism behind "the LLM is consulted less over time" - as the
  dictionary absorbs what the LLM already knows, tier-1's own confidence rises, and C-06's activation
  threshold (`Tier3Activation.shouldActivate`) simply triggers tier-3 less often for words it has already
  reinforced. Not a schedule, an emergent property of the two mechanisms feeding each other.

**D-128 - user-facing summary**: extended the existing D-89 `FeatureOverviewActivity` per the user's own
suggested shape, rather than a new screen - a compact, visually distinct callout (`key_background_special`
background, its own heading) inserted above the ordinary scrolling feature list, summarising D-127's write-up
in plain language: the touch model adjusts with every confirmed keystroke, corrected/new words join the
dictionary, and the optional mini-AI is consulted less as the dictionary learns to predict the same words
itself. New strings (`d128_title`/`d128_summary`, all three locales). No new tests - static layout/string
content, no decision logic.

### D-129 - Corner-Hint Glyph for the Calculator Minus Key's Sign-Flip
§31 (v0.8.10) deliberately shipped this key with neither a `hint` nor `alternatives`, since either would
have wrongly routed its long-press through the hint/alternatives commit-text pipeline instead of `SignFlip`.
New `AdaptKeyboardView.isSignFlipKey()` recognises the key by its own identity (`char == SymbolLayout.
MINUS_SIGN`, mirroring `KeyboardLayout.hasLongPressAction()`'s existing special case) and reuses D-98's
existing generic "more on long-press" corner glyph (`MORE_ALTERNATIVES_GLYPH`, "◢") - deliberately not a
bespoke sign-flip glyph a user might mistake for a committable character. No new tests - pure `onDraw` glue,
the established gap for this view.

### D-131 - Raw-Coordinate Correction (D-39) Now Live While Composing
`AdaptKeyService.refreshSuggestions()` now also consults the existing `rawCoordinateCorrection()` fallback -
previously only ever reached at commit time - once ordinary prefix/fuzzy/compound/wide-fuzzy candidates are
all still empty (`!duringRepeat`, consistent with D-138's own per-keystroke-cost concern for exactly this
class of extra lookup). Refactored the suggestion-assembly in `refreshSuggestions()`/`applyTier3Outcome()`:
D-122's split suggestion and this new raw-coordinate suggestion are both collected into one `extras: List
<Suggestion>`, appended only at display time (`candidates + extras` / `outcome.suggestions + extras`) rather
than fed into the tier-1 candidates `SuggestionMerger` itself normalises against - avoids exactly the
score-normalisation distortion a synthetic maximal score would otherwise cause there (a lesson already applied
to D-122, now generalised instead of duplicated). No new tests - `AdaptKeyService`/`InputConnection` glue over
the already-tested `RawCoordinateCorrection`, the established gap for this layer.

### D-132 - Settings Row: the Whole Reveal Now Slides as One Piece
Root cause of the "pops in" complaint: the row's own background lived on the *outer* view, whose
`layoutParams.height` jumps to its target instantly (the D-86 precedent, avoided for good reason - an
animated live-relayout would be expensive) - so the solid background appeared at full height immediately,
while only the buttons inside then slid up over 180ms. Fix: moved the background from the outer
`SettingsRowView` onto `content` (the child whose `translationY` already animates) - the instant height jump
is now invisible (nothing is painted at the outer view's own bounds), so the background and the buttons slide
together as one continuous reveal, matching what was asked without needing a live-relayout animation at all.
One-line change; no new tests - pure View/animation glue, the established gap for this view.

### D-133 - Bottom-Row Touch Zones: a Hard, Direction-Specific Downward Bound
New `OffsetModel.Candidate.maxDownwardOffsetFactor` (nullable, defaults to "use the model's own isotropic
`maxOffsetFactor`, same as every other direction") lets a caller tighten a specific candidate's downward (+y)
cap independently of D-109's general isotropic one; `cappedMeanOffset()` gained a matching optional
`maxAbsYDown` parameter for the D-24 visualisation to stay truthful (D-109's own established principle).
`AdaptKeyboardView` sets `BOTTOM_ROW_DOWNWARD_OFFSET_FACTOR = 0.25` (a considered, not-yet-device-tuned
starting point - the vertical midpoint between a bottom-row key's own centre and its bottom edge) for
`c`/`v`/`b`/`n`/`m`, reusing the already-existing `BOTTOM_ROW_LETTERS` constant (§64's T-05 ambiguity-band
set) rather than duplicating it. 3 new tests, including one proving the effect end-to-end via `resolve()`
(not merely that the parameter exists).

### D-136 - Gesture-Area System Control Contrast
Confirmed the suspected mechanism before fixing it (checked the actual window/insets setup, not assumed):
`AdaptKeyService` never touched the system-bars appearance flag at all, so the OS gesture-area controls
(pill/back indicator) rendered with whatever the window would otherwise inherit, independent of AdaptKey's
own painted background. Since `R.color.keyboard_background` has no dark-theme variant (always light), the
fix is unconditional: `WindowInsetsControllerCompat(window.window, root).isAppearanceLightNavigationBars =
true` in `onCreateInputView()`, alongside the existing D-42/edge-to-edge inset handling. No new tests -
window/service glue, the established gap for this layer.

### D-137 - A Typed Time Always Suggests "Uhr"
New pure `suggestion/TimePattern.endsWithTime()` (7 tests): a plain shape check (`\d{1,2}:\d{2}` at the end
of the text, optional trailing whitespace) - deliberately not range-validating real hours/minutes, since a
shape-only false positive is harmless and range-validation would add complexity for no real gain.
`AdaptKeyService.showNextWordPredictions()` now also checks the text immediately before the cursor and, when
it ends in a time, adds `Suggestion("Uhr", MAX_PRIORITY_SUGGESTION_SCORE)` - the same deliberately maximal,
tier-3-normalisation-avoiding scoring approach as D-122, generalised into a shared `MAX_PRIORITY_SUGGESTION_
SCORE` constant now that two independent features use it.

607 unit tests (was 592; +15: 7 `TimePatternTest`, 3 `OffsetModelTest`, 4 `DictionarySuggestionProviderTest`,
1 `SettingsMapperTest`). `:app:assembleDebug`/`:app:testDebugUnitTest` green. None of this round's Android/
service/view-glue items (D-126, D-128, D-129, D-131, D-132, D-136) have been confirmed on a real device yet -
this environment's established, repeatedly-documented limitation (no emulator, no `InputConnection`
Robolectric shadow, no way to render the settings screen or observe system-bar icon contrast).

## §76 - D-139 Investigated: No Root Cause Confirmed, a Defensive Circuit Breaker Added (v0.8.44)

Followed up on D-139 (§73) by request. **No repro exists yet - nothing below is a confirmed fix.** This is a
code-reading pass over the most plausible mechanism identified in §73 (a self-triggering `onUpdateSelection`
cascade in the D-62/D-87 mid-word-reclaim machinery), reported honestly as what it is: a real, structurally
identifiable risk, not proof of what actually happened on-device.

### What was traced
Read `AdaptKeyService.onUpdateSelection()`, `reclaimWordAtCaret()`, `reclaimSurroundingWord()`,
`splitComposingAtCaretAndCommit()` and the mid-word composing-mutation paths end to end, specifically
looking for a way this project's own established failure mode (§32's D-87: a `deleteSurroundingText()` call
reaching the app as a standalone edit, its callback arriving after `composing`/`composingAnchor` had already
advanced, wiping the token it was itself building) could recur in a form severe enough to look like the
reported "text jitters, characters get scrambled" symptom.

**Found, concretely:**
- `onUpdateSelection()`'s `composing.isEmpty()` branch calls `reclaimWordAtCaret()` **unconditionally**
  whenever the selection collapses - unlike the non-empty branch, which gates on an explicit `ownEdit` check,
  this one has no "is this genuinely a new user action, or an echo of our own prior edit" guard at all.
- The `!ownEdit` branch itself calls `currentInputConnection?.finishComposingText()` - a **mutating** call
  issued from *inside* the callback handler, which could in principle produce its own follow-up
  `onUpdateSelection` invocation.
- Chained together, a caret-move callback could in principle re-trigger `reclaimWordAtCaret()` /
  `finishComposingText()` repeatedly, each iteration mutating the document again - which would plausibly
  present exactly as "the text jitters and its characters end up scrambled", the reported symptom.

**Not found:** a concrete, provable trigger for this chain actually re-entering itself - that depends on
Android's actual callback-delivery timing and the specific target app's `InputConnection` implementation,
neither of which this environment (no emulator, no `InputConnection` Robolectric shadow) can exercise to
prove one way or the other. Recomputing `composingAnchor` arithmetically (§72) rules out one instance of the
D-87 stale-read pattern but does not rule out this one, which is structurally different (a callback
triggering more `InputConnection` mutations, not a mutation being read back too early).

### What was added: a circuit breaker, not a fix
New pure `CallbackBurstGuard` (5 tests): a plain sliding-window call counter (`isBurst(nowMs)`), Android-free
so it is directly unit-testable. Wired into the very top of `onUpdateSelection()` - if this callback fires
more than `DEFAULT_LIMIT = 40` times within `DEFAULT_WINDOW_MS = 200`, every reactive branch below is skipped
for that call, stopping a possible cascade from continuing to escalate rather than letting it run unbounded.

Threshold reasoning, so a future session can retune it rather than guess again: a held backspace repeats as
fast as every 45ms (§59), so even a sustained hold manages only ~4-5 ticks within a 200ms window - comfortably
under 40, so ordinary heavy backspace use is untouched (verified directly: a test simulates 200ms of 45ms-tick
backspace-repeat and confirms it never trips the guard). A genuine unthrottled cascade - no deliberate delay
between steps at all, unlike a timer-driven repeat - would be expected to blow past 40 within the same 200ms
almost immediately. Deliberately conservative (wide margin against false positives) since a wrongly-tripped
guard would itself be a new, self-inflicted bug in ordinary use.

**Explicitly not claimed:** that this is what caused the reported symptom, or that it is now fixed. This is a
structural hardening measure for a real risk found by reading the code, kept clearly separate from a
confirmed diagnosis - D-139 stays open pending an actual repro.

5 new unit tests (`CallbackBurstGuardTest`). 612 unit tests total (was 607).
`:app:assembleDebug`/`:app:testDebugUnitTest` green.

## §77 - D-132 and D-137 Follow-Up Fixes: Neither Original Fix Actually Worked (v0.8.45)

Device feedback on §75: neither D-132 nor D-137 changed anything observable. Both re-traced from scratch
rather than adjusted blindly.

### D-137 - Root Cause: Digits Never Reach the Code Path That Was Fixed
The §75 fix wired the "Uhr" check into `showNextWordPredictions()` - but that function is only called from
deep inside `finalizeAndCommit()`'s *normal word-commit* path, which requires an actual composing **token**
to finalise. Traced the `KeyCode.CHAR` handler directly: a digit only ever extends a composing token when
`composing` is already non-empty (D-40, the mid-word-typo case); a **standalone** digit - typing "1", "4",
":", "3", "0" one at a time to form "14:30" - has `composing` empty at every single keystroke, so each one
instead calls `finalizeAndCommit(ic, raw.toString())` **treating the digit itself as the delimiter**, which
takes `finalizeAndCommit()`'s *separate* empty-composing branch (commit the character, `clearSuggestions()`,
return early) - the branch where `showNextWordPredictions()` is never called at all. The §75 fix was wired
into a code path a typed time structurally never reaches.

**Fix:** new `AdaptKeyService.showTimeSuggestion(ic)`, called from *inside* the empty-composing branch itself
(right after the character commits), replacing the unconditional `clearSuggestions()` with "show the Uhr
prediction if the just-committed text ends in a time, else clear as before". The original `showNextWordPredictions()`
wiring is left in place (harmless, not reachable by pure digit typing, but a small correctness margin for
any other route that might reach it).

### D-132 - Root Cause: the Row's Background Was Never the Problem
Re-examined the actual visible complaint ("poppt sofort rein und die Buttons sliden darauf nach oben") rather
than assuming the §75 diagnosis just needed a bigger version of the same fix. The row sits in the **same
parent `LinearLayout`** as the suggestion bar and keyboard below it - jumping `layoutParams.height` to the
target instantly (the D-86-inspired shortcut §75 kept) makes those *sibling views* visibly jump down by the
row's full height the moment the swipe is recognised, regardless of what happens to the row's own background
afterwards. Worse, `content`'s background (moved there in §75) is `R.color.keyboard_background` - **the
exact same colour as the root view's own background** (§42) - so it was never visually distinguishable from
what was already showing through in the first place; the §75 fix changed nothing a user could actually see.
D-86's "resize right away" shortcut is correct for `AdaptKeyboardView`'s own self-contained page-slide (no
siblings to disturb) but does not transfer to a row stacked above other views.

**Fix:** `SettingsRowView.animateHeight()` now genuinely animates `layoutParams.height` itself
(`ValueAnimator.ofInt` + `requestLayout()` on every frame - the standard Android pattern for an animatable
view height, since there is no dedicated height property) from 0 to `ROW_HEIGHT_DP` and back, so the
suggestion bar/keyboard shift down/up in the *same* motion as the row's own growth. `content` no longer
tracks the row's own (now-animating) bounds via `MATCH_PARENT`; it keeps a **fixed** `ROW_HEIGHT_DP` height,
anchored to the row's bottom edge (`Gravity.BOTTOM`). As the row's own smaller, animating bounds clip
`content` (a `ViewGroup`'s default `clipChildren`), progressively more of it becomes visible from the bottom
up as the row grows - which alone produces the "buttons rising into place" effect, with no separate
`translationY` animation needed at all (removed).

No new tests for either fix - both are Android view/animation glue, the established gap for this layer; 612
unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Neither has been re-confirmed on
device yet.

## §78 - Version Shown in Settings; D-128 Relocated and Rewritten (v0.8.46)

### New: App Version Shown in Settings
Requested directly. New static `info_version` `Preference` in the settings screen's Info category
(`selectable="false"`, same style as `info_privacy`/`info_license`), its summary set at runtime in
`SettingsFragment.onCreatePreferences()` from `PackageManager.getPackageInfo(packageName, 0).versionName` -
read live from the actual installed package rather than a hand-maintained string resource, which would
inevitably drift out of sync with the real `versionName` in `app/build.gradle.kts`. New `info_version_title`
string (all three locales).

### D-128 - Two Corrections, Both From Direct Feedback
**Placement**: the §75 callout lived inside `FeatureOverviewActivity`, reachable only after tapping into it -
not what was asked ("direkt in den Settings auf dem Haupt-Screen ... am besten im Text unter dem Button, der
in die Feature-Liste führt"). Removed that callout entirely (the `LinearLayout` block, `d128_title`/
`d128_summary` strings - both now unused) and moved its role onto the **existing** `d89_feature_overview`
preference's own `summary` text - the text already shown directly under that button on the settings screen,
with no extra navigation required.

**Content**: the §75 text described dictionary word-learning and the tier-3 LLM fading out - real mechanisms,
but not distinctive: most keyboards claim some version of "learns your words". Rewritten to name what is
actually unique to AdaptKey and was specifically asked for: the raw per-character touch coordinate is
recorded (T-02), not just which key it resolved to; each key's own hit zone is continuously, individually
re-shaped from that data (T-03); and the starting shape for that model comes from the typing style chosen
during setup (K-01/T-04's `PatternSeed`), not a blank slate. New `d89_summary` text (all three locales),
replacing the previous generic "a full overview of every feature" line.

No new tests - static settings-screen content and a runtime `PackageManager` read, no decision logic. 612
unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

## §79 - D-135 Struck From the Backlog

Per explicit user instruction, D-135 (password-manager/Autofill suggestions in the suggestion bar) is struck
from the open backlog for now - not investigated further, not scheduled. §67's already-shipped Autofill
inline-suggestions implementation and §74's negative device result (no inline suggestion for the username
field in "finanzen.net zero") are left exactly as documented; only the open backlog entry itself is dropped.

## §80 - Backlog Captured, Not Yet Implemented (Two New Requests)

### D-142 - Credential-Field Learning: Immediate Save, Its Own Suggestion List, Live Email-Domain Ranking
Raised directly, following a review of the T-03/D-37/tier-3 learning write-up (D-127): the ordinary learning
pipeline is wrong for login-type fields (username/email). Three related asks, explicitly scoped as a single
new feature, not started this round:

1. **Immediate save, no D-37 threshold.** Typing a username or email address into a recognised login field
   must be learned on the very first commit, not counted up over `LEARN_THRESHOLD` (2) occurrences like an
   ordinary new dictionary word (D-37) - a login value is deliberate, precise input by construction, the
   opposite of the "don't eagerly learn a one-off typo" reasoning `PendingLearnStore` exists for.
2. **A separate, specially-flagged store - not the ordinary dictionary.** These values need their own list
   (or an explicit flag on existing entries) so they never get mixed into everyday-word suggestions and
   autocorrect. Ordinary dictionary words are never something a user would want to type into a username/email
   field, and vice versa - offering them there is "sinnbefreit" (pointless) per the user's own framing.
3. **Login-field suggestions are exclusively this list**, not the ordinary dictionary/n-gram/tier-3
   suggestion pipeline, whenever the target field is recognised as a login-type field (candidate signal:
   `EditorInfo.inputType`'s `TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS`/
   `TYPE_TEXT_VARIATION_USERNAME`/`TYPE_TEXT_VARIATION_PASSWORD`/`autofillHints`, needs confirming against
   real login fields, not assumed).
4. **Live, frequency-ranked email-domain completion after `@`.** Once the user has typed `@` in a
   not-yet-known email address, offer the domains from their own saved credential entries, most-frequent
   first, as completions - directly motivated by the user's own workflow (one personal domain, many aliases
   on it).

Needs its own design pass before implementation - at minimum: the exact field-type detection signal (and its
false-positive/false-negative behaviour on real apps, the same caution D-135's writeup already flagged for
`EditorInfo`-based field detection in general); the storage shape (a new SQLite table vs. a flagged column on
the existing dictionary schema); whether "domain" parsing is naive (`substringAfter('@')`) or needs more
care; and privacy handling (this store would contain real usernames/email addresses, unlike the ordinary
dictionary - likely needs its own place in the existing "Info & Privacy" settings copy, and probably its own
clear/reset action alongside the existing T-03/dictionary reset). Not designed or scoped further than this.

### D-143 - A Dedicated Keyboard Layout for URL Entry Fields
Raised directly, explicitly to be captured only for now ("merke dir das bitte einmal vor als noch zu
planendes Feature") - no design or implementation this round. A URL-specific layout, in the spirit of how a
phone/number-class field already auto-opens the calculator page (`AdaptKeyService.initialSurfaceFor()`, §31):
likely direct access to `/`, `.`, `www`, `.com` and similar without a long-press detour, auto-selected for
`TYPE_TEXT_VARIATION_URI`-class fields. Needs its own design pass (exact key set, placement, whether it
reuses `SymbolLayout`'s machinery or is a genuinely new layout) before implementation.

## §81 - D-140 Implemented: Un-Learn on A-07 Undo (T-03 + Dictionary); D-141: "Uhr" Timing Fixed (v0.8.47)

### D-140 - Rejected Corrections Are Now Un-Learned, Not Just Reverted On Screen
Raised directly, following a review of the D-127 "what learns when" write-up: A-07's post-commit undo (a
single backspace right after a corrected/split commit) restored the visible text, but never reversed whatever
training that commit had already persisted - a rejected correction kept reinforcing the dictionary
frequency/bigram of the *wrong* word, and kept the T-03 touch model's (possibly wrong) key resolution,
forever. Both must be un-learned, or - per the user's own framing - the A-07 safety net "kann man sich auch
sparen" (might as well not exist).

**Dictionary (every autocorrect/split undo path):** new `DictionaryStore.unlearn(word, previousWord)` - the
exact symmetric inverse of `learn()`: decrements the unigram frequency and bigram count by one each, removing
the entry entirely once a count reaches zero (so a word `learn()` had just created, frequency 1, is fully
un-learned again, rather than left behind as a zero-frequency ghost `isKnownWord()` would still report as
known). `learnWord()` now returns a `LearnOutcome` (`SKIPPED`/`LEARNED`/`PENDING`) describing exactly what it
did, captured into the armed A-07 undo state (`undoLearnRecords` - one entry for a plain correction, two
(left/right) for a split, each with the context word it used); `performAutocorrectUndo()` reverses each via
the new `unlearnWord()` before re-learning the word the user insisted on, replacing the old, incomplete
`PendingLearnStore.decrement(committed)` call (which only ever helped the rare case where the wrongly-
committed word was itself still unpromoted - the *common* case, an already-known correction target getting
silently reinforced by the original commit, was never touched at all).

**T-03 touch model - scoped precisely, not blanket-applied:** the touch-offset model is *not* wrong every
time an autocorrect fires - most corrections (spelling, diacritic restoration, splits) are purely linguistic
decisions with no bearing on which physical key was actually touched, and un-learning correctly-resolved taps
just because a linguistic correction was rejected would quietly discard good training data. The model can
only be genuinely wrong in one specific path: D-39's raw-coordinate fallback, whose entire premise is "the
runner-up key under this exact tap was more likely than the one that got resolved." New
`OffsetModel.unrecord(id, centerX, centerY, x, y, size)` is the exact algebraic inverse of the Welford update
`record()` performs (not a heuristic - `record` then `unrecord` with the same arguments restores the prior
mean/variance/contact-area state precisely, including removing the key's entry entirely once its last sample
is reversed, so it reads as genuinely untrained again). `finalizeAndCommit()` now distinguishes whether
`autocorrectFor()` or the `rawCoordinateCorrection()` fallback produced the correction (previously collapsed
into one `?:` chain); when it was the raw-coordinate path, new `rawCorrectionUndoFor()` finds the single
character position where the correction actually differs from what was typed (D-39 only ever substitutes one
position) and captures that position's raw tap, resolved key id and key geometry, before `clearComposing()`
wipes `composingTaps`. On undo, only that one sample is reversed via `offsetModel.unrecord()` - never the
rest of the token's (correctly resolved) taps, and never anything at all for an ordinary
dictionary/diacritic/split correction, which sets no raw-correction undo in the first place.

New tests: `OffsetModelTest` (4, proving `record`/`unrecord` round-trip exactly, including the contact-area
mean), `InMemoryDictionaryStoreTest`/`SqliteDictionaryStoreRoboTest` (5, `learn`/`unlearn` symmetry for both a
brand-new word and a reinforced known word). The `AdaptKeyService` wiring itself (`rawCorrectionUndoFor`,
`unlearnWord`, the extended `performAutocorrectUndo`) is service/`InputConnection` glue with no independently
testable pure logic beyond what these cover - the established testing gap for this layer (no emulator/
`InputConnection` Robolectric shadow in this environment).

### D-141 - "Uhr" Suggestion No Longer Appears (or Applies) Before the Trailing Space
Reported directly: the D-137/§77 "Uhr" prediction appeared immediately after the last digit of a typed time
(e.g. right after "14:30"'s `0`), before any space had been typed - and tapping it then concatenated straight
onto the digits with no separating space at all ("14:30Uhr "). `TimePattern`'s regex previously allowed zero
or more trailing whitespace characters (`\s*$`), so it matched the bare digits alone. Changed to require **at
least one** trailing whitespace character (`\s+$`) - the format is deliberately not even checked until the
actual delimiting space has been typed, per the user's own framing ("vorher muss das Format gar nicht geprüft
werden"). Since the space is always already committed to the field by the time either call site
(`showTimeSuggestion()`'s empty-composing branch, and the `timeSuggestion()` fallback inside
`showNextWordPredictions()`) checks the pattern, this single regex change fixes both complaints at once: the
suggestion no longer appears prematurely, and applying it (`onSuggestionClicked()`'s unchanged
`commitText(word + " ", 1)`) now always lands after a real, already-present space. 2 `TimePatternTest` cases
updated to require trailing whitespace, 1 new case added confirming no match without it.

### Reordered T-03/T-04 in §3 (Documentation Only)
Per direct request, so the spec's own reading order matches the actual dependency: T-04 (typing pattern) now
precedes T-03 (personal offset model) in §3, since K-01's `PatternSeed` (D-68) seeds T-03 from the pattern
T-04 describes, not the reverse. Purely a section-order swap - the IDs themselves are unchanged (renumbering
would ripple through many code comments/tests that reference "T-03"/"T-04" by name), and, consistent with how
the rest of this document works, the original §3 text is left as the historical baseline; the actual current
behaviour (an explicit pattern picker, not the auto-detection §3 originally described) is already documented
correctly in D-68/§63 and later.

622 unit tests (was 612; +10). `:app:assembleDebug`/`:app:testDebugUnitTest` green. None of this round's
items have been device-confirmed yet.

## §82 - D-142 Implemented: Login-Field Credential Learning & Suggestions (v0.8.48)

Implements the §80 capture, with one real technical finding that reshaped its scope - surfaced to the user
before continuing, rather than silently narrowed or guessed around.

### Real constraint found: `EditorInfo` has no autofill-hint field at all
§80's own design leaned on `EditorInfo.autofillHints` for username detection - the compiler flagged it as
unresolved, then verified directly against the real `android-35` `android.jar` via `javap` (not assumed):
no such field exists on `EditorInfo` at all. Autofill hints live on the target app's own `View`, which an
IME never has a reference to. `InputType`'s variation bits remain the only reliable field-level signal, and
they distinguish email/password cleanly - but have no variation for "username" whatsoever, so a plain login
field is, at the `EditorInfo` level, indistinguishable from any other text field. Guessing from a bare
unvaried text field would have wrongly treated ordinary prose fields (chat, notes, search) as login fields
everywhere - explicitly rejected rather than shipped.

### Agreed compromise
- **EMAIL fields**: fully automatic ("obligatorisch") - reliably detected via
  `InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`WEB_EMAIL_ADDRESS`.
- **PASSWORD fields**: reliably detected and always excluded - never learned from, never suggested into.
- **USERNAME**: no reliable field-level signal exists, so it is never auto-activated from field type alone.
  Two ways in instead:
  1. A new **weak signal** (`LoginFieldDetector.hasWeakUsernameSignal()`) checks the field's own
     `EditorInfo.hintText`/`fieldName` (an app-supplied placeholder string, set only sometimes, often
     localised) against a small username-keyword list (EN/DE plus a couple of others). When it fires, the
     settings row auto-opens and its new credential-mode button flashes - a *nudge*, never a silent switch.
  2. A new **manual toggle**: a fourth settings-row button (leftmost, ahead of the emoji button) always lets
     the user switch the current field into credential mode by hand, for any field the detection missed
     entirely - swipe up, tap the key-glyph button.

### New `credential/` package (pure + Android glue, mirroring the dictionary/store split)
`LoginFieldKind` (NONE/USERNAME/EMAIL/PASSWORD), `LoginFieldDetector` (pure `classify()` +
`hasWeakUsernameSignal()`), `CredentialEntry`, `CredentialRanking` (pure: prefix-match suggestions + domain-
frequency ranking), `CredentialStore` (SharedPreferences/JSON, mirrors `RecentEmojiStore`'s pattern - its own
private file, entirely separate from the ordinary dictionary and every other store).

### Learning (D-142 point 1: immediate, no D-37 threshold)
`CredentialStore.learn()` always reinforces on the very first observation - deliberate credential input is
precise by construction, unlike an ordinary new dictionary word the D-37 threshold guards against learning
too eagerly. Captured from two points: `handleEnter()` (the common case, right before the field's own submit
action can navigate away or clear the field) and `onFinishInput()` (catches leaving the field some other way,
e.g. tapping an on-screen "Login" button) - a `credentialCaptured` flag stops the second from double-
reinforcing the same observation. Reads the field's own committed text via
`InputConnection.getTextBeforeCursor()`, not `composing` - necessary because `finalizeAndCommit()`'s ordinary
per-fragment delimiter model would otherwise autocorrect/capitalise an email address piecemeal as it is typed
(every `.`/`@`/`-`/`_` is its own delimiter under the existing token model). New `commitLoginFieldFragment()`
is a `finalizeAndCommit()` short-circuit for every login-relevant field (USERNAME/EMAIL/PASSWORD): commits
each fragment exactly as typed, no autocorrect, no §6 capitalisation, no dictionary learning at all - the
whole value is captured once, separately, never per-fragment into the ordinary dictionary.

### Suggestions (D-142 points 2/3: a separate list, exclusively shown)
`refreshSuggestions()` branches to a new `showCredentialSuggestions()` for any non-`NONE` `loginFieldKind`,
entirely bypassing the ordinary dictionary/tier-1/tier-3 pipeline - a normal word is never offered in a login
field. Built and pushed directly to the suggestion bar via a new `SuggestionController.Kind.CREDENTIAL`
(mirrors the existing D-36 `CLIPBOARD` chip precedent - bypasses `SuggestionController.update()`/`displayed()`
entirely, since S-03's position stabilisation exists to smooth prose-typing flicker a short, freshly-ranked
credential list has no need for). Tapping a credential suggestion deletes whatever is already typed in the
field first (the value-so-far may span several already-committed fragments plus the live composing one)
before committing the full tapped value, so it replaces rather than appends onto it. A PASSWORD field shows
nothing at all - offering suggestions while typing a password would be a pointless information leak, on top
of never being learned from in the first place.

### Domain completion (D-142 point 4)
Once the value typed so far in an EMAIL field contains `@`, `showCredentialSuggestions()` switches to
`CredentialRanking.emailDomainsFor()` - the user's own most-used domains (summed frequency across every alias
on that domain, so several less-frequent aliases on one domain still correctly outrank a single more-frequent
one elsewhere), completing the address instead of matching whole saved addresses.

### Privacy
A new settings action (Info & Privacy category, `d142_clear_credentials`) deletes the whole credential store
in one confirmation dialog (single-confirm, unlike `reset_learning`'s double-confirm - far lower-stakes,
nothing about typing quality is affected by clearing it, unlike wiping the whole learned touch model).

27 new unit tests (`LoginFieldDetectorTest`, `CredentialRankingTest`, `CredentialStoreRoboTest`).
`AdaptKeyService`'s own wiring (field capture, suggestion branching, the settings-row button) is
service/`InputConnection`/view glue with no independently testable pure logic beyond what these cover - the
established testing gap for this layer. 649 unit tests total (was 622). `:app:assembleDebug`/
`:app:testDebugUnitTest` green. **Not yet device-confirmed** - this is exactly the area (real login forms,
real apps' actual `EditorInfo`/hint-text behaviour) that can only really be judged on a real device.

## §83 - Two Icon Fixes, Reported While D-142 Was Being Device-Tested (v0.8.49)

### D-129 (Third Pass): the Sign-Flip Corner Hint Reused the Wrong Cue
Reported directly: the calculator minus key's sign-flip corner hint (§75/§31) is identical to
`MORE_ALTERNATIVES_GLYPH` (D-98's "◢", "a D-01 alternatives popup awaits") - but no popup ever appears for
this key, so the cue actively promises something that never happens. §75's own write-up already reused
`MORE_ALTERNATIVES_GLYPH` deliberately, "rather than inventing a bespoke glyph a user might mistake for a
committable character" - a reasonable-sounding reason at the time that turned out wrong in practice: reusing
the *wrong* existing cue is worse than a purpose-built one. Fixed with a new `SIGN_FLIP_GLYPH = "±"` (the
plus/minus ligature the user asked for directly) - `AdaptKeyboardView.drawKeys()`'s corner-hint branch now
checks `isSignFlipKey(key)` before the generic multi-alternative case, drawing `±` via the same `hintPaint`
position an ordinary single-hint key already uses (not the "more alternatives" triangle's styling). Private
drawing-only `View` code, no new test - the established gap for this layer.

### D-70/§70 (Follow-Up): the Clear-Clipboard Button's Trash-Can Icon Was Unclear
Reported: the settings row's clear-clipboard button (a bare 🗑) gives no indication of *what* it clears
without already knowing - "man wird diesen Knopf vermutlich nie drücken" (nobody would ever press it blind).
Asked for a "ligature" of the clipboard glyph and a small overlaid trash can. A true combined-glyph ligature
does not exist for an arbitrary emoji pair (Unicode ZWJ sequences are only defined for a curated, fixed set
of combinations - clipboard + trash is not one of them), so the practical equivalent was built instead: new
`SettingsRowView.badgedButtonFor(baseGlyph, badgeGlyph, onClick)` renders `baseGlyph` (📋) filling the whole
button with `badgeGlyph` (🗑) as a small badge - its own circular `keyboard_background`-coloured pill, for
contrast against the button's own background - in the bottom-right corner, the same visual language as an
app icon's notification badge. `clearClipboardButton` now uses this instead of the plain `buttonFor("🗑")`;
every other row button (emoji, gear, credential-mode) is unaffected. Private `View`-construction code, no new
test - same established gap.

649 unit tests (unchanged - both are Android view/drawing glue with no new pure logic).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet device-confirmed.

## §84 - §83 Refined: Badge Contrast Background Dropped (v0.8.50)

Device feedback on §83's clipboard+trash badge: the overlaid 🗑 reads clearly on its own - the circular
`keyboard_background`-coloured pill drawn underneath it was unnecessary and looks better removed.
`SettingsRowView.badgedButtonFor()`'s `badge` `TextView` no longer sets a `background` at all; everything
else (position, size, margin) is unchanged. No new tests - same established View-glue gap.

## §85 - D-142 Follow-Up: Weak-Signal Detection Now Also Covers Email, Not Just Username (v0.8.51)

### Reported: a `finanzen.net zero` field labelled "E-Mail-Adresse" did not activate credential mode at all
Device-tested directly: the field's on-screen label was unambiguously "E-Mail-Adresse", yet neither the
reliable `InputType`-based detection nor the weak-signal nudge fired. The user could not tell from the device
alone whether the app's `InputType` genuinely lacks the email variation, or whether the weak-signal fallback
simply never fires at all - both remain open questions this environment cannot resolve without further device
feedback, but a concrete, real gap was found by re-reading the code rather than guessing blindly: **§80/§82's
weak-signal detector (`LoginFieldDetector.hasWeakUsernameSignal()`) only ever checked username-style
keywords ("username", "benutzername", …), never anything email-related at all.** A field labelled purely
"E-Mail-Adresse" - with no "username"/"login"/etc. wording anywhere - could never have triggered the nudge
regardless of what its `hintText`/`fieldName` said, even on a device where the fallback mechanism itself
works perfectly. This alone fully explains the reported symptom if `InputType` genuinely did not classify the
field (plausible - plenty of real apps use a plain text field with their own client-side email validation
instead of setting the OS-provided variation); it may not be the *only* mechanism at play, so the fix is
reported back for another device round rather than claimed as a confirmed complete resolution.

### Fixed: the weak signal now recognises email keywords too, not just username ones
`LoginFieldDetector.hasWeakUsernameSignal(hintText, fieldName): Boolean` is now
`weakSignalKind(hintText, fieldName): LoginFieldKind` - checks a new `EMAIL_KEYWORDS` list ("email",
"e-mail", "mailadresse", plus the German/Greek/Spanish/French equivalents already following this class's
existing multi-language pattern) before the existing `USERNAME_KEYWORDS` list, so "E-Mail-Adresse" itself now
matches directly. Email wins when a hint mentions both (e.g. "Username or email") - the more specific,
actionable read, since only `LoginFieldKind.EMAIL` gets domain completion.

`AdaptKeyService` threads the richer result through: `weakSignalKind: LoginFieldKind` (was
`weakUsernameSignal: Boolean`) still only drives the settings-row auto-open + button flash *nudge*, never
suggestion behaviour by itself, matching the original design exactly - only what it can activate *into*
changed. New `credentialModeManuallyActivated` flag lets `toggleCredentialModeFromSettingsRow()` activate the
field as the *specific* kind the weak signal suggested (so a nudged email field gets real domain completion
once confirmed, not just a generic username fallback) while still correctly toggling back off only a
manually-activated mode - a reliably-detected EMAIL/PASSWORD field stays non-user-toggleable exactly as
before.

3 new/expanded `LoginFieldDetectorTest` cases (including one named directly after the reported repro). 652
unit tests (was 649; +3). `:app:assembleDebug`/`:app:testDebugUnitTest` green. **Still needs a device round**
to learn whether this alone resolves the reported case, or whether `InputType` detection itself also needs a
closer look for this specific app.

## §86 - D-110 Picked Up: a Real, Confirmed Bug Found and Fixed - Not Claimed as the Full Explanation

Investigated per direct request ("schau mal, was du bei D-110 tun kannst"). Traced the two candidate causes
D-110's own original write-up named - both **ruled out directly against the actual code**, not guessed:

- "a filter/no-suggestions variant treated as skip-auto-cap-entirely": grepped `AdaptKeyService.kt` for any
  `TYPE_TEXT_FLAG_NO_SUGGESTIONS`/`TYPE_TEXT_VARIATION_FILTER` handling - none exists anywhere in the file.
  There is no code path that could special-case such a variant at all, so this is not the mechanism.
- "`getCursorCapsMode()`/`EditorInfo` mishandled at the `CapitalisationEngine` call site": `CapitalisationEngine.
  capitalise()` checks `context.capsMode` only for the `CHARACTERS`/`WORDS` special cases; `context.sentenceStart`
  (from the independent `SentenceBoundary` check) is consulted regardless of `capsMode`, exactly matching the
  spec's own stated intent ("AdaptKey's own rules... apply regardless of what the target field requests"). This
  call site is not the defect either.

### A real, separate bug found instead: `ShiftGrace.autoArmAtWordStart()` silently dropped sentence-start arming for `CapsMode.NONE`
```kotlin
CapsMode.WORDS, CapsMode.CHARACTERS -> true
CapsMode.SENTENCES -> sentenceStart
CapsMode.NONE -> false   // always false, regardless of sentenceStart - the actual bug
```
A field that declares **no** caps flag of its own at all (`CapsMode.NONE`) - exactly what a plain, unadorned
message box (a very plausible shape for eBay Kleinanzeigen's message field) would report - never armed Shift
at a sentence start, even though `CapsMode.SENTENCES` (the field *explicitly* asking for sentence-case) did.
This directly contradicts the project's own stated design and was even encoded as an *intended* test
(`ShiftGraceTest`'s `` `NONE never auto-arms` ``) rather than caught as a defect - the kind of thing that only
surfaces once a concrete failing app is reported. Fixed by folding `NONE` into the same branch as `SENTENCES`:
both now arm Shift exactly when `sentenceStart` is true, matching `CapitalisationEngine`'s own already-correct
independent handling.

**Honest caveat, not glossed over**: this is a confirmed, real bug, worth fixing on its own merits regardless
of D-110 - but tracing `CapitalisationEngine.capitalise()`'s commit-time logic shows the *final committed*
word should already have been corrected independently of this bug (`context.sentenceStart` is checked there
too, unconditionally, the moment `explicitFirstUpper` is false). So this fix should visibly correct the
*live-typed* character and the Shift key's own visual state at a sentence start in any field with no caps
flag - a real, felt difference either way - but it is **not proven** to be the complete explanation for "the
committed word itself never ends up capitalised" as D-110 originally described. It may be the whole story
(if something about this app's own `InputConnection` behaviour interacts with the visual/live-typed state in
a way not modelled here) or only part of it.

### A temporary diagnostic added to close the loop either way
D-110's own original write-up already named the blocker: "needs the actual field's reported `EditorInfo`
... to make progress rather than guessing further." A one-line `Log.d("AdaptKey", ...)` in `onStartInput()`
now logs `packageName`/`fieldName`/`inputType` (hex)/`hintText` for every focused field - retrievable via
`adb logcat -s AdaptKey:D` while focusing the eBay Kleinanzeigen field. This finally supplies the concrete
data point the investigation has been missing, regardless of whether the `ShiftGrace` fix alone resolves it.
Clearly labelled as temporary in its own comment - remove once D-110 is closed for good, mirroring how D-09's
own diagnostic was eventually torn out once it had served its purpose.

1 existing `ShiftGraceTest` case corrected (was asserting the bug as intended behaviour, now asserts the fix).
652 unit tests (unchanged - a one-line change to an already-tested pure function).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. **Not yet device-confirmed either way.**

## §87 - D-139: Diagnostic Logging Added While Typing (v0.8.53)

Requested directly, following §86's `AdaptKey:D` diagnostic - the user reports D-139 ("occasionally the whole
text jitters and its characters get scrambled") happens "nicht selten" (fairly often) but is not immediately
reproducible with the exact same text again, i.e. genuinely timing-dependent, exactly the shape §73/§76's own
suspected mechanism (a self-triggering `onUpdateSelection()` cascade in the D-62/D-87 mid-word-reclaim
machinery) would produce. A new `Log.d`/`Log.w` tag, `AdaptKeyJitter` (kept separate from D-110's `AdaptKey`
tag, so either can be filtered independently - `adb logcat -s AdaptKeyJitter:D` to watch only this), logs at
every point §76's own trace identified as load-bearing for the suspected cascade:

- **`onUpdateSelection()`** - every call: timestamp, old/new selection, `candidatesStart`/`candidatesEnd`,
  and the live `composing`/`composingAnchor`/`composingCursor` state. A second line logs the `ownEdit`
  determination and, when it comes out false, the exact `composing` string about to be wiped
  (`finishComposingText()` + `clearComposing()`) - precisely the branch that would produce a visible
  jitter/scramble if it fires when it should not.
- **`CallbackBurstGuard` tripping** - previously a silent early return; now an explicit `Log.w`, since this
  firing at all is close to a smoking gun for the suspected cascade actually happening.
- **`reclaimSurroundingWord()`** - every *actual* reclaim (not the common no-op): the `before`/`after`
  fragments it is about to splice into `composing`, right before the `deleteSurroundingText()` call §32/D-87
  already found risky once before.
- **`updateComposing()`** - the exact string pushed to the field as composing text on every call. A
  scrambled/reordered result should show up directly in this sequence of log lines, in order.
- **`finalizeAndCommit()`** - `typed` vs. the actual `finalWord` permanently written to the field, so a
  corruption that only shows up in the *committed* text (not just while composing) is still caught.

Deliberately not gated behind a settings toggle or `BuildConfig.DEBUG` check (consistent with §86's own
`AdaptKey` log) - `Log.d`/`Log.w` output stays on-device in logcat, never transmitted anywhere, matching the
app's existing "provably offline" posture; capturing it requires physical/`adb` access to the device, the
same bar D-09's own (now-removed) raw-tap-recording diagnostic set. Composing/committed text content is
logged verbatim, including whatever the user is actually typing - worth knowing before sharing a captured
logcat with anyone, though it never leaves the device on its own. Clearly labelled as temporary in every
comment - to be removed once D-139 is closed for good, the same lifecycle as D-09 and (eventually) §86's own
`AdaptKey` log.

No new tests - pure Android logging calls with no decision logic of their own. 652 unit tests (unchanged).
`:app:assembleDebug`/`:app:testDebugUnitTest` green.

## §88 - D-139/D-110: In-App Diagnostic Log Replaces the Need for `adb logcat` (v0.8.54)

Requested directly: §87's `adb logcat` approach requires the phone to stay tethered to a PC while a repro
happens - impractical for D-139 specifically, since the user reports the jitter happens somewhat often during
*normal* daily use, not during a deliberate desk-testing session, and re-tethering after the fact loses
whatever logcat already discarded. Asked for an in-app, self-managing alternative instead: a FIFO log that
drops anything older than 5 minutes on its own, toggled by a setting, viewable and exportable from within the
app.

### New `diagnostics/` package
- `DiagnosticEntry` - one recorded line (a monotonic timestamp + message).
- `DiagnosticRingBuffer` - pure, JVM-unit-tested (7 tests, synthetic timestamps): appends an entry and prunes
  everything older than a configurable retention window *relative to the newest entry's own timestamp*, so
  pruning behaves correctly regardless of real wall-clock gaps between recordings.
- `DiagnosticLog` - the Android-facing singleton wrapping it with the real clock
  (`SystemClock.elapsedRealtime()` - monotonic, resets on reboot, never a calendar timestamp), a 5-minute
  retention window, and an `enabled` flag mirroring the new setting (turning it off also clears whatever was
  recorded, not just stops adding to it). `record()` is a cheap no-op unless enabled, so call sites never need
  their own guard.

### Settings: a new "Diagnostics" category
A new `diagnosticLogEnabled: Boolean` (default off - it records raw typed text) threaded through the full
existing pipeline (`RawSettings`/`AdaptSettings`/`SettingsMapper`/`SettingsStore`, `d_diag_enabled`, applied
live in `applySettings()`), plus a second row (`d_diag_log`) opening the new `DiagnosticLogActivity`. Both
sit in the settings screen's own new category, kept separate from "Info & Privacy" for visibility, though the
toggle's own summary text states plainly what it records and that it is never written to disk or transmitted
unless explicitly shared.

### `DiagnosticLogActivity` - the viewer/export screen
Refreshes its content on every `onResume()`, not just `onCreate()` - so leaving the keyboard to reproduce an
issue and coming back to Settings picks up whatever was recorded meanwhile, no relaunch needed. Each entry is
rendered with a relative age (`-4.2s`, computed against the same monotonic clock) rather than a wall-clock
timestamp - directly answers "how long before I opened this screen did that happen" without the user having
to correlate calendar time by hand. Two export paths: `Intent.ACTION_SEND` (plain text, share-sheet chooser,
no storage permission - the same D-09 raw-tap-recording precedent this project has used before) and a direct
clipboard copy for pasting straight into a chat. A `Clear` button wipes the buffer on demand.

### §87's `Log.d`/`Log.w` calls now feed both logcat and the in-app log
New private `AdaptKeyService.diag(tag, message, warn)` routes every one of §86/§87's seven diagnostic call
sites (`onStartInput`, `onUpdateSelection` ×3, `finalizeAndCommit`, `updateComposing`,
`reclaimSurroundingWord`) to both `Log.d`/`Log.w` (kept for anyone who does have `adb` set up) and
`DiagnosticLog.record()` - one shared helper instead of duplicating the routing at each site. No call site's
own diagnostic content changed, only where it goes.

8 new unit tests (7 `DiagnosticRingBufferTest`, 1 `SettingsMapperTest`). 660 unit tests total (was 652).
`:app:assembleDebug`/`:app:testDebugUnitTest` green. The Activity/settings-wiring itself is Android view/
service glue with no independently testable pure logic beyond `DiagnosticRingBuffer` - the established
testing gap for this layer. Not yet device-confirmed - next time the jitter happens, the in-app log (Settings
→ Diagnostics → "View / share diagnostic log") should have it, no PC required.

## §89 - D-143 Implemented: a Dedicated URL-Mode Bottom Row (v0.8.55)

Planned in a design conversation before implementation, per D-143's own "needs its own design pass" note
(§80). The main open questions from that capture were resolved directly with the user rather than guessed:
this is **not** a new `?123`-style page - it stays on the letters surface (a URL still needs the full
alphabet for its domain/path) and only swaps the bottom row, exactly like the existing `qwerty`/`greek`
toggles already swap other parts of the same layout without a separate `InputSurface`.

### New URL-mode bottom row, replacing the ordinary comma/space/period trio
`[?123] [https://] [www.] [/] [space, narrow] [.] [⏎]` - built by a new, shared
`KeyboardLayout.urlBottomRow()` (also called by `GreekLayout`, since URL entry doesn't depend on the active
typing alphabet - both layouts show identical URL keys):
- **`/` takes over the comma key's own primary position.** Its long-press popup is the original
  `COMMA_ALTERNATIVES` list with `/` simply prepended as the new pre-selected character - the comma swaps
  from primary to alternative, staying fully reachable, with nothing else about the popup changed.
- **A new `https://` key** (§53 `KeyCode.TEXT`, mirroring the calculator page's `sin`/`deg` keys), whose
  long-press popup offers the other everyday protocols: `http://`, `ftp://`, `file://`.
- **A new `www.` key** (also `KeyCode.TEXT`), with no long-press action of its own yet.
- **The space key shrinks drastically** (a URL practically never needs one) - the freed width funds the two
  new keys above; it is labelled with the plain space glyph (`SPACE_GLYPH`, already used by the `?123` pages
  for the same reason) rather than the active-language label, since it is both too narrow for that and not
  a meaningful place to switch language from (see the G-01 gesture point below).
- **The full-stop key's long-press popup is a locale-resolved TLD list instead of the ordinary sentence
  terminators** (`! . ?`) - a URL practically never needs `!`/`?`. New `UrlLocale` object (`keyboard`
  package) mirrors `CalculatorLocale`'s JDK-locale-driven approach rather than a hand-maintained worldwide
  table: English-language locales lead with the two generic TLDs (`.com`, `.org`), then the locale's own
  country-code TLD when one is idiomatically used for casual addresses; every other locale leads with its
  own ccTLD instead, then `.com`/`.org`. A small `CCTLD_OVERRIDES` map corrects the two cases this reasoning
  actually calls for, not an attempt at exhaustive worldwide coverage: Great Britain uses `.co.uk`, not the
  bare `.uk`, for ordinary sites, and the United States has no everyday casual ccTLD at all (`.us` is not how
  Americans think of their own sites, unlike `.de` for Germans). Resolved from the same `systemLocale`
  property `AdaptKeyboardView` already threads through for the calculator page's currency/decimal-separator
  keys (D-92) - no new locale plumbing needed.

### Trigger: auto-open only, not part of the D-19 swipe cycle
New `AdaptKeyService.isUrlField(info)` checks `EditorInfo.inputType` for `TYPE_TEXT_VARIATION_URI` (compared
against the real, unmasked `InputType` constant - see the bug note below), mirroring `initialSurfaceFor()`'s
existing phone/number/datetime-class check but as an orthogonal `urlMode` flag rather than a surface switch,
since it stays on `LETTERS`. Set fresh per field in `onStartInput()` (pushed to the view *before*
`setSurface()`, so its own `rebuildRows()` already reads the correct value) and defensively re-pushed in
`onStartInputView()` in case the view was recreated in between - the same two-point pattern
`greek`/`qwerty` already use for their own per-session state.

### Autocorrect/suggestions: suppressed entirely, reusing the D-142 credential-field precedent
`finalizeAndCommit()`'s existing login-field short-circuit (`if (loginFieldKind != LoginFieldKind.NONE)`)
now also covers `urlMode` - a domain/path is exactly as much "not prose" as a username/email value, and the
existing fragment-committer already did precisely what a URL needs (commit verbatim, no autocorrect, no §6
capitalisation, no token-repair, no dictionary learning), so no new commit logic was needed, only a wider
condition. Renamed `commitLoginFieldFragment()` → `commitVerbatimFieldFragment()` since it now serves both
features (the function itself was already login-agnostic - purely mechanical). `refreshSuggestions()` gets
its own new, simpler bypass (`if (urlMode) { clearSuggestions(); return }`) rather than reusing
`showCredentialSuggestions()` - a URL field has no candidate list of its own to offer, unlike a login field's
saved credentials, so the bar just stays empty. The D-36 clipboard-paste chip is deliberately **not**
suppressed - pasting a copied link into a URL field is exactly the kind of thing it exists for.

### G-01 language swipe: the URL-mode space key is treated like a symbol-page space key
`KeyGesture.resolve()` gained a `urlMode` parameter, extending the existing D-92 surface check
(`surface == LETTERS`) to `surface == LETTERS && !urlMode` for the space-key branch - a horizontal swipe on
the narrow URL-mode space key falls through to the ordinary D-19 surface swipe instead of switching
language, the same reasoning D-92 already applied to the smaller `?123`-page space keys ("no language of its
own to switch"), even though URL mode itself stays on the letters surface rather than a different one.

### Real bug found and fixed while building D-143's own field-variation check, not guessed
Verified `isUrlField()`'s bitmask logic against the real Android SDK first (`javap` against `android-35`'s
`InputType.class`, this project's established habit before trusting a constant): `TYPE_MASK_VARIATION`
(`0x0ff0`) does **not** include the `TYPE_CLASS_TEXT` bit (`0x0001`), so the value `classify()`-style code
actually receives from `inputType and TYPE_MASK_VARIATION` never carries that bit either. Checking
`LoginFieldDetector` for the same pattern (needed to avoid repeating whatever mistake it might have made)
found exactly that mistake: its five variation constants (`VARIATION_EMAIL_ADDRESS` etc.) had the class bit
baked in (`0x21` for EMAIL, when the real masked value is `0x20`) - so `classify()` could **never** match a
real field's variation and silently returned `LoginFieldKind.NONE` for every field, regardless of its actual
type. This plausibly explains §85's `finanzen.net zero` report on its own (not only the already-documented
"no variation exists for username at all" limitation) - the "reliable" EMAIL/PASSWORD detection was never
reliable in practice, only in its own unit tests (which passed the same class-bit-inclusive literals
directly, proving the constants self-consistent but never checking them against what a real masked
`EditorInfo.inputType` value actually looks like). Fixed: all five constants corrected to their bare,
class-bit-free values, verified against the real SDK (`TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 32`,
`_PASSWORD = 128`, `_VISIBLE_PASSWORD = 144`, `_WEB_EMAIL_ADDRESS = 208`, `_WEB_PASSWORD = 224`); the
existing `LoginFieldDetectorTest` literals corrected to match. **Not yet device-confirmed** whether this
alone resolves §85's report - the weak-signal email/username keyword fallback (§85's own fix) still applies
regardless and may have been doing all the real work on some devices; this fix means the *reliable* path can
finally also engage on devices where it was silently dead before.

15 new unit tests (`UrlLocaleTest` 6, `KeyboardLayoutTest` 6, `GreekLayoutTest` 2, `KeyGestureTest` 1). 675
unit tests total (was 660). `:app:assembleDebug`/`:app:testDebugUnitTest` green. The pure `UrlLocale`/
row-shape logic is fully unit-tested; the `AdaptKeyService` wiring (`isUrlField`/`onStartInput`/
`onStartInputView` pushes, the `finalizeAndCommit`/`refreshSuggestions` short-circuits) is service glue with
no independently testable logic beyond what those cover - the established testing gap for this layer. Not
yet device-tested - the new key weights (`https://`/`www.`/the narrower space) are a considered starting
guess, not tuned against a real screen, easy to retune later (same precedent as §36/§37/§53).

## §90 - D-144: Swipe-Down-to-Dismiss Now Also Reacts on the Suggestion Bar and the Settings Row (v0.8.56)

### Bug fixed: G-03's downward swipe only ever reacted on the keyboard's own key field
Reported directly: swiping down to close the §48 settings row (or, once already closed, to dismiss the
keyboard entirely) only worked when the swipe started on `AdaptKeyboardView` itself - not on the suggestion
bar (`SuggestionBarView`) or the settings row (`SettingsRowView`) stacked above it. Confirmed by reading the
actual touch-dispatch architecture rather than assuming: `AdaptKeyboardView.onTouchEvent()`'s own
`resolveSwipe()` is the *only* place in the whole app that ever recognised a swipe gesture at all -
`SuggestionBarView` only ever intercepted an upward drag (G-04, drag-to-trash) and otherwise deferred
everything to `HorizontalScrollView`'s own scrolling; `SettingsRowView` had no touch handling of its own
whatsoever beyond its buttons' plain click listeners. Since these are three separate sibling views in the
same root `LinearLayout`, a touch starting on either of the other two never reached
`AdaptKeyboardView`'s touch handler at all - there is no Android mechanism that would forward it there.

### Fix: the same pure `SwipeGesture` classifier, wired into both sibling views
Both `SuggestionBarView` and `SettingsRowView` gained the exact two-stage claim-early/confirm-late
interception pattern `AdaptKeyboardView.resolveSwipe()` and (for G-04) `SuggestionBarView` itself already
used: `onInterceptTouchEvent()`'s `ACTION_MOVE` branch claims the gesture (`swipeDownIntercepting = true`)
the moment `SwipeGesture.classify(dx, dy, interceptThresholdPx)` (the small system touch-slop) reports a
downward-dominant movement - stealing it away from whichever child would otherwise track it as a button
press or, for the suggestion bar, from `HorizontalScrollView`'s own horizontal-scroll interception; `
onTouchEvent()`'s `ACTION_UP` then only actually fires a new `OnSwipeDownListener.onSwipeDown()` once the
full travel re-confirms `SwipeDirection.DOWN` against a larger `swipeDownThresholdPx` (110dp, matching
`AdaptKeyboardView`'s own `fieldSwipeThresholdPx` so the gesture feels the same everywhere it can start).
No new pure logic was needed - both views reuse the already-tested `gesture.SwipeGesture`/`SwipeDirection`
classifier directly, the same one the keyboard body's own dismiss gesture is built on.

`SuggestionBarView`'s new interception fires regardless of `dragWord` (unlike G-04, which only ever arms
over an ordinary suggestion chip, never the verbatim one) - so a downward swipe starting over the verbatim
chip or an empty gap is caught too, matching "swipe down anywhere on the bar."

### Wiring: one shared dismiss function, not three copies
`AdaptKeyService.handleSwipe()`'s existing `GestureAction.DISMISS_KEYBOARD` branch (`if (settingsRow?.isOpen
== true) closeSettingsRow() else requestHideSelf(0)`) was extracted into a new private
`dismissKeyboardOrCloseSettingsRow()`, now called from all three sources: the keyboard body (unchanged
behaviour, just refactored), the new `SuggestionBarView.onSwipeDown`, and the new
`SettingsRowView.onSwipeDown` - so "close the row first, only dismiss on a second swipe once it's already
closed" is one rule in one place, not duplicated per view.

### Investigated but not changed: the reported "(exkl. Space)" exclusion
The report also described the previously-working scope as "the key field, excluding the space bar" - traced
this specifically before writing it off, since a real, separate space-bar-only exclusion would need its own
fix. Found no code path that treats `KeyCode.SPACE` any differently for a downward swipe: `KeyGesture.
resolve()`'s `if (direction == SwipeDirection.DOWN) return GestureAction.DISMISS_KEYBOARD` fires
unconditionally, before any key-specific branching (including the `KeyCode.SPACE` case further down, which
only ever special-cases *horizontal* swipes for G-01); `AdaptKeyboardView.resolveSwipe()`'s own threshold
computation is likewise keyCode-agnostic for a vertical swipe. Not dismissed as user error either - simply
not reproducible from the code as described, the same honest "traced, not found, needs a repro" outcome
this project has reached before for a code-grounded-but-unconfirmed report (D-118's precedent). If this
persists, it needs a repro isolating it from the (now fixed) suggestion-bar/settings-row gap.

No new unit tests - both changes are Android view/touch-dispatch glue reusing the already-tested,
already-pure `SwipeGesture`/`SwipeDirection` classifier, the established testing gap for this layer (same
class as D-64's own drag-to-trash interception, never itself unit-tested beyond the pure `DragToTrash`
object it wraps). 675 unit tests (unchanged). `:app:assembleDebug`/`:app:testDebugUnitTest` green. Not yet
device-confirmed.
