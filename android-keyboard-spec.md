# AdaptKey - Requirements Specification

**Platform:** Android (Kotlin, InputMethodService)  
**Base:** AOSP LatinIME  
**Status:** Concept phase  
**Date:** June 26, 2026

---

## 1. Motivation

Every available Android keyboard implicitly assumes a particular input gesture - thumb typing, two-handed use, or a right-handed index finger. No existing product learns neutrally how a given user actually types. Workarounds such as shifted mini-layouts are not an acceptable solution.

Beyond this, off-the-shelf keyboards fail systematically at context-sensitive capitalisation, hyphenated compound words, punctuation as a prediction signal, and stability of the suggestion bar.

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
