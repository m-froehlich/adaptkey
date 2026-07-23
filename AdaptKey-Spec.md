# AdaptKey - Requirements Specification

**Platform:** Android (Kotlin, InputMethodService)
**Base:** AOSP LatinIME (conceptual)
**Status:** Implemented, actively maintained (not a concept document - see [`AdaptKey-Progress.md`](AdaptKey-Progress.md) for the current build/version/test-count status; never frozen here)

This document describes the app's **current, crystallised feature set only** - what AdaptKey does today.
The reasoning behind *why* each feature looks the way it does - device-feedback rounds, rejected
alternatives, bugs traced and fixed - lives separately in [`AdaptKey-History.md`](AdaptKey-History.md),
an append-only design journal. Requirement IDs (`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`, plus the newer
`E-`/`U-`/`P-`/`W-`/`R-`/`V-`/`N-`/`X-` domains below) are shared between both documents, so a `D-xx`/`§N`
citation in the history can always be traced back to the requirement it ultimately shaped.

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

Two independent, equally legitimate conventions exist for typing a diacritic character without holding for
it, and both must be recognised as an exact (not merely close) match: the formal orthographic substitution
(`ß` → `"ss"`, e.g. `"straße"`/`"strasse"`, the spelling used when `ß` is genuinely unavailable) **and** this
app's own long-press-alternative shorthand (`ß` sits as the long-press alternative directly on the `s` key,
mirroring `ä`/`ö`/`ü` on `a`/`o`/`u` - a lazily-tapped bare `s` is a direct stand-in for it, e.g.
`"gruse"` for `"Grüße"`). Neither convention may crowd out the other, and a match under either must never be
demoted to a frequency-ranked fuzzy candidate - it is exact, and must be offered as such (see A-01/S-06).

### Guiding Principle - `onUpdateSelection`'s Self-Recognition Mechanism Is Foundational

Getting `AdaptKeyService.onUpdateSelection()` to correctly recognise the IME's own edits took three full
device-log tracing rounds against two independently observed, genuinely different race shapes (a lagging
commit echo; an un-coalesced batch edit's transient intermediate state, from an editor that does not merge
the reclaim's delete/set/select sequence into one callback). Not a blanket "don't touch this file" - but
any future change that touches composing state, `onUpdateSelection`, `reclaimSurroundingWord`, or the
batch-edit sequencing in `AdaptKeyService` must keep these three properties intact, and should be weighed
against them before shipping (the full history entries for this fix are the cheap, sufficient check - not
an exhaustive whole-file re-audit every time):

1. `composingAnchor` (the absolute document offset of the composing region's start) must be resolved for
   *every* composing token, not only a mid-word one - `reclaimSurroundingWord()` reads it unconditionally,
   before any mutation in that same call.
2. `onUpdateSelection`'s `ownEdit`/truth check must never decide anything from the target editor's own
   remotely-reported `candidatesStart`/`candidatesEnd` - that value is not guaranteed to arrive in sync
   with the selection-update stream.
3. A callback whose reported positions mismatch the expected caret must be verified against a synchronous
   ground-truth read (`getExtractedText()`) before the composing state is torn down - simple positional
   equality against only the latest expected value is insufficient, proven by two distinct real echo
   shapes that a naive "belated update" interval heuristic (considered and rejected) would *not* both have
   caught.

If a change would make any of these three no longer hold, stop and re-derive against the original device
logs (see `AdaptKey-History.md`) rather than guessing - this class of bug reproduces silently and took real
device logs, not code review alone, to find each time.

---

## 2. Layout & Key Assignment

### L-01 - Base Layout: Language-Conditional
German uses the Gboard QWERTZ layout as its base - key sizes, spacing, and access to special characters -
except where this specification explicitly diverges. English uses its own QWERTY row layout (differing
from German only in the `y`/`z` position). Greek uses a dedicated layout (see L-02 in §4/G-01 for language
switching). Because the personal offset model (T-03) is keyed by character identity, `y`/`z` carry over
their German-position offsets for a short, self-healing period immediately after switching to English.

### L-02 - Narrower Space Bar, Wider Comma & Full Stop
The space bar is narrower than the Gboard default; the comma and full-stop keys are widened accordingly.
Two additional spacing sliders control the gap below the number row and the gap above the space-bar row
(default 7 dp each, range 0-25 dp), specifically to reduce mis-taps in that zone. The full-stop key's
long-press popup and the comma key's long-press popup are separate lists: full stop offers `! . ?`, comma
offers `- , : ; / _`. All proportions and spacings are user-configurable (C-01).

### L-03 - Symbol/Numeric-Layer Key
A single key in the bottom row switches to the numeric/symbol layer (`?123`) - a tap opens it, a further
tap on its "ABC" key returns to letters. The key's slot is governed solely by a setting (`symbolKeyEnabled`,
default on); when disabled, the slot stays reserved but inert rather than collapsing the row. The emoji
function does **not** live on this key - it is reached exclusively via the always-visible emoji button in
the swipe-up extra row (§14, R-01), which needs no setting of its own since it costs no keyboard-row space
while closed.

### L-04 - Enlarged Backspace
The backspace key is widened relative to the Gboard default (recommended ~10%) to reduce mis-hits. The
additional width is taken from its neighbours within the third row and is independent of the bottom-row
layout. The exact proportion is user-configurable (C-01). The chosen typing style (T-04) pre-selects
whether the extra width favours the backspace side or the Shift side by default, matching which hand/finger
is expected to reach that corner.

### L-05 - Secondary Symbols via Long-Press (AltGr-Style)
Frequently used symbols are placed as secondary characters on letter keys, reachable by long-press and shown
as a small hint glyph in the corner of the key - analogous to the AltGr labels on a hardware QWERTZ
keyboard. The current default mapping: `@`/Q, `€`/E, `#`/H, `-`/M, `+`/N, `°`/D, `×`/X, `÷`/C, `/`/V, `*`/B,
`ƒ`/F, `π`/P, plus a Greek-letter popup (`α β γ δ λ ω`) and a second alternative on `O` (`ö` then `Ø`).
Symbols already carried by the number row (L-06) - such as `/` and `&` - are not duplicated here. Keys with
more than one secondary alternative show a full popup (horizontal, centred over the stem key, growing into
available screen space; an edge key's list is reversed so the key's own glyph stays nearest the finger), not
just a single-tap hint; a key with several alternatives but no natural single hint glyph shows a small
corner "more alternatives" triangle instead. The popup tolerates a small amount of finger movement during
the hold without cancelling or mis-selecting. The per-key symbol set and whether hint glyphs are displayed
are user-configurable (C-08). Together with L-03 and L-06 this removes the need to switch to `?123` for most
everyday input.

### L-06 - Persistent Number Row
A dedicated number row (`1`-`0`) is shown at the top of the alpha view by default and can be toggled off in
Settings (C-09). It behaves like an ordinary key row (full height, its own long-press popups): each digit
carries two long-press alternatives - its German-QWERTZ shifted symbol (`1`→`!`, `2`→`"`, `3`→`§`, `4`→`$`,
`5`→`%`, `6`→`&`, `7`→`/`, `8`→`(`, `9`→`)`, `0`→`=`) and its own superscript form (e.g. `2`→`²`) - reached
via the same multi-alternative popup as L-05. C-09 governs only the main letters page; the dedicated
URL-mode and email-mode bottom rows never show a number row at all, and symbol page 2 (§15) always shows a
digit row regardless of C-09, as a guaranteed fallback when the main row is off.

### L-07 - Calculator Layer
A dedicated calculator layout is reachable from the symbol pages (§15): a 4×4 digit/operator grid with a
persistent right-hand column (backspace, space, currency glyph, `ABC`, Enter), plus `sin`/`cos`/`tan`/`log`
and `deg`/`rad` keys. The minus key's sign flips via long-press. Currency glyph and decimal/thousands
separators are resolved from the device locale.

### L-08 - Catch-All Symbol Page
A second symbol page always shows a digit row plus shifted symbols as a guaranteed fallback for when L-06's
main-page number row is disabled.

---

## 3. Touch Capture & Learning Model

### T-01 - ACTION_DOWN as the Canonical Tap Point
The initial contact point (`MotionEvent.ACTION_DOWN`) is used as the authoritative tap coordinate. Subsequent movement up to `ACTION_UP` does not influence key resolution and does not trigger implicit swipe behaviour.

### T-02 - Retroactive Character Correction
The most recently entered character is held in composing state until sufficient context is available (e.g.
the start of the next word). If context subsequently points to a more probable neighbouring character, the
entry is corrected via `setComposingText`/`commitText`. The raw touch coordinate is retained until the final
decision is made. This extends to a raw-coordinate fallback: while composing, the token's own retained raw
taps are walked and the geometrically nearest neighbouring key is considered live, not only at commit time.

### T-04 - Typing Style (Explicit Selection, Load-Bearing for T-03)
The typing style is **not** auto-detected from taps - it is chosen explicitly during onboarding/calibration
(K-01) from an ordered list: Both Thumbs, Right Thumb, Left Thumb, Right Index, Left Index (ordered by
real-world prevalence; no option is marked "recommended"). Skipping the picker silently seeds "Both Thumbs"
rather than leaving the model cold. The chosen style directly seeds T-03's per-key offset model: home-row
keys get tight, centred zones; keys further from the typing hand get progressively wider zones with a
directional bias shifting the expected strike point back toward home, reflecting the physical arc of a
reaching finger/thumb ("Both Thumbs" keeps a flat, unbiased spread). This is a load-bearing input to the
touch model, not merely informational - it materially shapes T-03's starting state and is listed before
T-03 here for that reason.

### T-03 - Personal Offset Model
All tap events are persisted as tuples `(touch_x, touch_y, confirmed_key)`, seeded initially by T-04's chosen
typing style. A 2D Gaussian offset kernel is computed per key to compensate for each user's individual
systematic deviation, using a Huber-style robust weighted update: a tap far from the currently-learned
expected point contributes less to the mean (weight floored at 0.1, so a sustained pattern of similar
off-taps still eventually moves the zone, just more gradually than an ordinary running mean). The isotropic
offset is capped (`maxOffsetFactor = 0.5` of key size); the bottom row (`c v b n m`) additionally has a
tighter, direction-specific downward cap (0.25) so a key's zone can never drift down into the space bar's
territory. A tap flagged as T-05-ambiguous is never used as training data. The model exposes an exact
algebraic inverse (`unrecord()`) of a single weighted update, used narrowly by A-07's undo to reverse one
specific raw-coordinate correction. The model makes no assumption about handedness or finger beyond its
T-04 seed and continues to adapt from every confirmed word.

### T-05 - Space/Letter Confusion in the Bottom Row
The boundary between the space bar and the bottom letter row (`c v b n m`) is treated as a high-risk zone for swapped space/letter input - a risk aggravated by the narrower space bar (L-02). Two ambiguous bands are defined:

- **Space-ambiguous tap:** A `MotionEvent.ACTION_DOWN` in the lower edge band of `c`, `v`, `b`, `n`, or `m`. The committed letter may in fact be an intended space.
- **Letter-ambiguous tap:** An `ACTION_DOWN` in the upper edge band of the space bar. The committed space may in fact be an intended letter; the nearest letter key is inferred from the tap's x-coordinate via the offset model (T-03).

Taps in either band are flagged accordingly and their raw coordinates are retained (T-02), so that a later
token-level decision can retroactively replace a letter with a space (A-05) or a space with a letter (A-06).
The band widths scale with the configured key proportions (C-01) and are refined by the personal offset
model. A flagged tap is excluded from T-03's own training data (see T-03).

### K-01 - Initial Calibration / Typing-Style Selection *(skippable)*
On first launch, the user is offered a short setup step: pick a typing style from T-04's list. There is no
sentence-typing exercise - the style choice alone seeds the offset model (T-03). Skipping silently applies
"Both Thumbs"; the model still converges during normal use, just somewhat more slowly. The step can be
repeated at any time from Settings. The touch-zone visualisation screen (§17, T-06) doubles as the result
view after a (re-)selection.

---

## 4. Gestures

### G-01 - Language Switch
Swiping left or right on the space bar cycles the active input language through German → English → Greek
(direction determines forward/back). The gesture only fires on the letters surface's own space key; the
symbol/calculator pages and the dedicated URL-mode/email-mode space keys have no language of their own, so a
swipe there falls through to the ordinary page-switch gesture (G-06... see D-19-style full-field swipe,
§4 note below) instead. In addition to this manual switch, the active language switches automatically after
five consecutive words committed in a different language than the one currently active; every switch -
manual or automatic - cross-fades the space-bar's language label (260 ms) as a visible acknowledgement.

### G-02 - Delete Whole Word
Swiping left on the backspace key deletes the entire previous word rather than a single character. This is independent of - and takes priority in recognition over - the accelerating repeat-on-hold behaviour of a plain backspace press/hold.

### G-03 - Dismiss Keyboard
Swiping downward dismisses the keyboard - recognised on the keyboard body, the suggestion bar, and the
swipe-up extra row (§14) alike. When the extra row is open, a downward swipe closes the row first; only a
second downward swipe (with the row already closed) dismisses the keyboard.

### G-04 - Remove/Un-learn a Suggestion (Drag-to-Trash)
A suggestion in the suggestion bar can be dragged upward into a trash area that appears during the drag; a
simple swipe is not sufficient - the deliberate drag-and-drop action prevents accidental removal. The
outcome depends on the word's origin: a bundled dictionary word is blacklisted immediately (A-04). A word
the keyboard learned from the user's own typing is instead un-learned and marked provisionally pending
blacklist for a configurable window (default 7 days, editable 1-30); only a genuine recurrence within that
window escalates it to a permanent blacklist entry, so a single accidental drag does not permanently condemn
a self-taught word. Because a word matching the current input can never appear as a suggestion (S-02), a
dedicated review screen (§13, W-01) exists as the only way to remove such a word directly. The W-03 promotion
confirmation chip is the one exception to this single-zone behaviour, offering an explicit two-zone drag of
its own instead - see §13.

### G-05 - Retroactive Capitalisation at Word End
Pressing Shift at the end of a fully typed word toggles the capitalisation of its first character - in both directions: "Upper" becomes "upper", "lower" becomes "Lower". The outcome depends on the next key pressed:

- **Next key is a space, punctuation, or line break:** The first character is toggled as described; the rest of the word is unchanged.
- **Next key is a letter:** The toggle of the first character is discarded; instead, the new letter is inserted as an uppercase character within the word (camelCase / PascalCase).

The gesture only triggers when the caret is genuinely at the composing token's own end (not merely while
some token happens to be composing), and only after a Caps-Lock-release check has first been ruled out.

### G-06 - Double-Tap Shift for Caps Lock
Double-tapping Shift engages Caps Lock.

### Addendum to G-05 - Shift State After Backspace
When an uppercase character is deleted, Shift remains active - the next keystroke will produce an uppercase character. Deleting the space immediately to the left of a just-deleted uppercase character also counts as "deleting uppercase" for this purpose, since the uppercase context ended at that word boundary. When a lowercase character is deleted, Shift behaves as usual (context-driven by the autocorrect hierarchy).

---

## 5. Suggestion Bar

### S-01 - Horizontally Scrollable, Configurable Maximum
The bar shows as many ordinary (tier-1/tier-3) suggestions as are meaningful and scrolls horizontally.
Default: 8 entries. Maximum is user-configurable (recommended range: 6-10). Sorted by descending
probability; the most probable suggestion appears at the far left. For a correction/fuzzy candidate (as
opposed to a plain prefix completion), "probability" is not raw dictionary frequency alone: how close the
candidate actually is to the typed token weighs in too, as a soft preference (a much closer candidate
generally outranks a farther one even when the farther one is far more frequent, though an overwhelmingly
more frequent farther candidate can still occasionally win - never an absolute cost-first rule). The bar's
row is always reserved even when momentarily empty. Depending on the focused field, the bar's slot can
instead show a platform-rendered Autofill inline-suggestions row, or a credential-suggestion list (§12) -
S-01's own scrolling/sorting behaviour describes the ordinary dictionary/tier-3 case specifically.

### S-02 - No Duplicate of the Current Input
The word exactly as typed does not appear as a suggestion. That slot belongs to genuine alternatives.
Exception: when an autocorrection is pending, the verbatim input is surfaced as defined in S-06. This is the
only case in which the typed word appears in the bar. (A direct consequence: a word matching the current
input can never be reached via drag-to-trash - see G-04/W-01.)

### S-03 - Position Stabilisation
The visible top-N ordinary suggestions may not change position while the user is actively typing. New
suggestions are appended at the right end of the scrollable list. Re-sorting only occurs after a
configurable pause with no input. Several newer suggestion kinds are deliberately built outside this
mechanism so as not to distort it: a mid-word connector-split suggestion (A-10), the raw-coordinate
suggestion (T-02), the time-pattern "Uhr" suggestion (S-08), and the credential list (§12) are all appended
at display time with a maximal score rather than participating in the ordinary ranking/stabilisation.

### S-04 - Re-sort Delay *(configurable)*
Slider from 0 to 600 ms. A value of 0 means immediate re-sorting. Default: 300 ms.

### S-05 - Word Confirmation Highlight *(configurable, default on)*
The word currently being typed is highlighted, via font colour, as soon as the system recognises it as
correct and complete (no correction planned). The highlight clears on the next delimiter (space,
punctuation, hyphen). While a retroactive split (A-05) is pending, a live two-span colour preview is shown
instead - both halves coloured, the dropped/boundary character left uncoloured - mutually exclusive with the
ordinary single-word highlight. During a fast continuous typing burst, recomputation is debounced (~200 ms
after the token stops changing) for performance, so the highlight can lag briefly in that case. Feature is
toggleable; colour is user-selectable. Confirmed by the user (see `AdaptKey-History.md`) that green meaning
"safe, no correction planned" is the correct, settled reading - the alternative ("about to be
auto-corrected") is not adopted.

### S-06 - Verbatim "Keep As Typed" Affordance
Whenever autocorrect intends to replace the current token on the next delimiter - including a pending
capitalisation-only change (e.g. an ordinary noun about to be auto-capitalised, or a correction whose case
must match sentence-start context) - the literal typed string is surfaced as a dedicated, visually distinct
chip (e.g. in quotation marks), pinned at the far left of the suggestion bar and exempt from position
stabilisation (S-03). The pending replacement is shown alongside it. Tapping the verbatim chip commits the
literal input and cancels the pending change for this occurrence. This is the only case in which S-02 is
overridden. Repeated verbatim confirmation of the same string feeds a learning signal and may add it to the
personal dictionary (cf. B-03), after which it is no longer autocorrected. This affordance mirrors the
ordinary autocorrect candidate and a pending diacritic restoration alike (see the "Umlauts Are Ordinary
Characters" guiding principle) - the latter takes precedence whenever both would apply, matching what
actually wins at commit time - but it does not preview the rarer raw-coordinate fallback (T-02) or a
high-certainty tier-3 capitalisation exception.

### S-07 - Next-Word Prediction
A genuine next-word suggestion (not merely a completion of the current token) is offered before the next
word is typed, using a bigram baseline elevated by tier-3 when available. When a personal (learned-only, no
bundled seed) two-word-context trigram match also exists, it is preferred over the bigram baseline via a
Stupid Backoff blend (D-246): a candidate with a real trigram match scores by its own raw trigram count; a
candidate reached only through the bigram signal is discounted, so a trigram match generally - but not
absolutely - wins over a merely more frequent bigram-only candidate (an overwhelmingly more frequent
bigram-only word can still outrank a barely-seen trigram one, matching this app's established soft-preference
philosophy for a more-specific-but-sparser signal, see S-01/A-05). The trigram table starts empty and grows
purely from the user's own typing (no bundled trigram data is shipped); a two-word context resets everywhere
the existing one-word bigram context already does (field change, an external caret move, the G-02 whole-word
delete).

### S-08 - Time-Pattern "Uhr" Suggestion
A typed time in `HH:MM ` form (trailing space required) always suggests the German word "Uhr" as a
completion, independent of the ordinary dictionary/n-gram ranking.

---

## 6. Capitalisation

### Autocorrect Hierarchy

1. **Explicit user input always wins.** An uppercase letter is never corrected to lowercase. A lowercase letter is only corrected when rules 2-4 apply unambiguously.
2. **Sentence start:** The first word after `.`, `!`, or `?` followed by a space is capitalised. A deliberate line break also begins a new sentence and is capitalised (see the comma-terminated-line exception below).
3. **Pure nouns:** A word that exists exclusively as a noun (no verb, adjective, or preposition form) is capitalised automatically.
4. **Known proper nouns:** Are capitalised.
5. **Ambiguous words** (existing as both a noun and another part of speech): No automatic correction. Capitalisation is offered as a suggestion in the bar (S-06). Example: "gegenüber" (opposite/facing) - its prepositional use is far more frequent than its nominal use, so no intervention occurs.
6. **LLM exception:** If the sentence context is unambiguously nominal and LLM confidence is high, the LLM may capitalise - never as a default, only at high certainty.

Part-of-speech annotations and frequency distributions are derived from the bundled dictionary. Rules 3/4
depend on this tagging being unambiguous; a genuine noun/verb homograph mis-tagged as a pure noun will
mis-fire rule 3. This is a recognised, only partially addressed data-quality issue (a handful of confirmed
homograph pairs have been re-tagged to the ambiguous case; a scan found roughly 140 further candidate words
of the same shape, deliberately left unfixed as an open safety-vs-fluency product decision, not a bug to be
silently resolved).

Rules 2-4's outcome is now visible in the suggestion bar *before* it silently applies, via S-06's extended
scope covering pending capitalisation-only changes - not only pending spelling substitutions.

This entire section (all of §6) is bypassed for email-mode, URL-mode, and login-field fragments (§10-§12):
those commit verbatim, with no capitalisation transform of any kind.

### Addendum to G-05 - Shift State After Backspace
See §4.

### Editor-Mandated Capitalisation
When the target field declares `TYPE_TEXT_FLAG_CAP_SENTENCES`, `TYPE_TEXT_FLAG_CAP_WORDS`, or `TYPE_TEXT_FLAG_CAP_CHARACTERS` (read from `EditorInfo` and queried per position via `InputConnection.getCursorCapsMode()`), the case the field requires is treated as an authoritative baseline, ranking directly below explicit user input (hierarchy rule 1). The linguistic rules (sentence start, nouns, proper nouns, ambiguous words) never lowercase a position the field requires to be capitalised. A field reporting no caps flag at all (`CapsMode.NONE`) is still armed at a genuine sentence start, exactly as if the field had requested sentence-case - AdaptKey's own rules apply regardless of what the target field requests, unless overridden by the email/URL/login bypass above.

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

---

## 7. Autocorrect Policy

### A-01 - Valid Words Are Protected, With a Bounded Override
If the typed word exists in the dictionary, no automatic substitution occurs by default - even if a similar
word is more frequent. This is not an absolute veto: a known word can be overridden by a correction
candidate that is (a) a cost-1 (single adjacent-key) edit, **and** (b) at least 50× more frequent - a
frequency floor additionally prevents any low-confidence candidate from winning regardless of edit cost. An
unknown-but-plausible regular-verb inflection of a known infinitive (e.g. "beurteilst" from "beurteilen")
is protected unconditionally, with no ratio check, since it has no independent frequency to compare. The
same protection applies to an unknown-but-plausible regular adjective comparative/superlative of a known
positive (e.g. "zuversichtlicher" from "zuversichtlich") - excluding a positive that resolves to a noun,
since German nouns take no comparative/superlative degree at all. This
protection is removed entirely for a blacklisted word (A-04) - the bundled blacklist deliberately includes a
few cross-language confusables (e.g. `due`, `sue`, `ddr`, `aks`) specifically so ordinary autocorrect can win
over them despite each being a genuine dictionary entry in some language.

### A-02 - Punctuation Is Not a Context Reset
A comma does not clear the prediction context. N-gram patterns such as `", dass"` or `", die"` are trained and suggested as independent entries. The mini-LLM sees the full sentence and is not subject to this limitation in any case.

### A-03 - Language Detection from Context
A character-trigram classifier identifies the current writing language from recent words, gated to at least
two words of context (a single word's own sample is too statistically noisy). Suggestions and autocorrect
are filtered per language. The classifier's own per-token routing interacts with the explicit active-language
toggle (G-01): five consecutive words classified in a different language than the one currently active
triggers a real language switch, not merely per-token filtering.

### A-04 - Blacklist for Unwanted Words
Words can be permanently excluded from suggestions and autocorrect. The blacklist is persisted in SQLite and
survives app updates, with a category distinction between a bundled entry and a user-added one. A small
bundled cross-language-confusables set (`due`, `sue`, `ddr`, `aks`) is seeded by default (see A-01), alongside
a curated set of pre-1996-spelling-reform relics of otherwise ordinary common words (e.g. `daß`, `muß`,
`Rußland`) - hand-picked against the bundled dictionary's own real corpus frequencies (kept only where the
modern spelling is demonstrably the dominant, living form in the same corpus), deliberately excluding any
proper noun/surname/place name sharing the same ß-vs-ss shape, whose own spelling is not an error to correct.
Blacklisting keeps a word typeable/known (quoting genuinely old text still works) while it can never surface
as its own suggestion again. The blacklist editor (C-05) shows only user-added entries by default - a bundled
entry is rarely of interest and should rarely be removed at all - with an explicit toggle to reveal bundled
entries too. Also interacts with the provisional pending-blacklist mechanism for a self-taught word
(G-04/W-01).

### A-05 - Retroactive Word Split on Missed Space
When the current token is not a valid word and is not a plausible regular-verb inflection or adjective
comparative/superlative (see A-01), and it can be split into two dictionary words at (or near) the position
of a space-ambiguous tap (T-05) or a fully missed
space, the offending character is replaced with a space (or a space is inserted) and the token is committed
as two words. The current gating: each half must individually clear a minimum frequency floor; a split where
**both** halves are nouns is rejected (a function-word-plus-noun pair, e.g. "und"/"das", is accepted); prior
bigram co-occurrence between the two halves is a scoring signal only, never a gate, so a first-time-typed
compound typo can still split correctly even with no recorded history. Half resolution also tries
umlaut/ß-unfolding (e.g. "uber" resolves via "über") before giving up, per the umlaut guiding principle - the
committed text itself still carries the literal typed substring, not the unfolded form. A split is vetoed if
it would lose to a high-confidence single-word correction instead. A live two-span colour preview is shown
while composing (S-05). Spatial proximity alone is never sufficient - a valid linguistic split is required.

> **Open design question (unresolved):** a split currently re-derives the two halves' capitalisation from
> the generic rules rather than preserving whatever mid-word capitalisation the user had actually typed.
> Flagged, not fixed.

### A-06 - Retroactive Word Merge on Spurious Space
The inverse of A-05. When a space was registered from a letter-ambiguous tap (T-05) and the following token is not a valid word, the system tests whether removing that space and prepending the letter inferred from the tap's x-coordinate yields a valid or high-probability word. If so, the spurious space is removed and the reconstructed word is committed (e.g. `aber  ald` -> `aber bald`, where the intended `b` landed on the space bar). As with A-05, a valid linguistic result is mandatory; the spatial signal only nominates the candidate.

### A-07 - Post-Commit Autocorrect Undo
After a commit that involved any correction - spelling autocorrect, diacritic/umlaut restoration, or an A-05
split - a single backspace **tap** issued immediately afterwards restores the originally typed text,
including rejoining a split back into one word. This applies only to a plain tap directly after the commit;
it does not affect the whole-word delete gesture (G-02) or the ordinary shift-state-after-backspace
behaviour (Addendum to G-05). The undo also:

- **Un-learns the dictionary side** exactly, symmetrically reversing whatever unigram/bigram reinforcement
  or brand-new-entry creation that commit had just caused, deleting an entry outright if its count reaches
  zero rather than leaving a zero-frequency ghost. A split's undo reverses both halves.
- **Un-learns the touch model, but only narrowly:** only if the correction came from the raw-coordinate
  fallback (T-02) is the one substituted tap position reversed via T-03's exact algebraic inverse. An
  ordinary spelling/diacritic/split correction never touches T-03, since it carries no information about
  which physical key was mistouched.
- **Decrements the pending-learn counter** correctly, respecting the variable threshold (W-02: 2 for an
  ordinary word, 4 for a token recognised as a suspected unsplit compound or carrying an embedded mid-word
  capital).

This mechanism depends on the `onUpdateSelection` self-recognition guiding principle (§1) and on a correct
single-character delete immediately after a mid-word reclaim (never deleting the whole reclaimed word) - both
must be kept intact by any future change in this area.

### A-08 - Compound-Word Peeling (Suggestion Only)
For an unrecognised token, a known noun (4+ characters), optionally preceded by a German linking element
(Fugenelement), is peeled off the token's front and offered as a compound-split suggestion. Never applied
automatically.

### A-09 - Wide Fuzzy Match for Long Unknown Tokens (Suggestion Only)
A long unrecognised token is additionally matched against the dictionary with a wider edit-cost budget
(cost-4) than the ordinary correction search uses. Suggestion only, never auto-applied.

### A-10 - Mid-Word Connector-Split Suggestion
While re-editing mid-word, an unresolved bottom-row connector character (`c v b n m`, per T-05) between two
otherwise-recognisable halves is offered directly as a split suggestion, bypassing the ordinary bar-evidence
ranking entirely.

---

## 8. Hyphen Handling

### B-01 - Hyphen as a Segment Separator
A hyphen splits a compound word into independent tokens for dictionary lookup, the learning model, and the
suggestion bar. "XYZ-Anteil" trains "Anteil", not "XYZ-Anteil" as a unit. A hyphen-committed fragment is
treated identically to a space-committed one by both bigram learning and next-word prediction (S-07).

### B-02 - No Automatic Capitalisation After a Hyphen
The segment following a hyphen is lowercase by default at commit time. Exception: the segment is a known
proper noun. A suggestion-bar chip for any candidate always displays its capitalised form through the same
§6 pipeline the commit path uses, so a chip can never visually disagree with what will actually be committed.

### B-03 - Optional Learning of Full Compounds *(designed, not built)*
Not yet implemented. If the user confirms the same hyphenated combination multiple times, it could
optionally be added to the personal dictionary as a whole. Two candidate designs exist (chaining next-word
predictions across the hyphen boundary vs. learning the whole combination as one unit after repeated
confirmation) with no decision yet on hop depth, promotion threshold, or UI mechanics.

### B-04 - Underscore as a Word-Joining Character, Never a Separator
Unlike every other punctuation mark - including the hyphen (B-01) - the underscore never ends the composing
token: it extends it exactly like a letter, so a technical identifier (`MEINE_VARIABLE`) composes as a single
token rather than being torn into fragments at every underscore. A token containing `_` is additionally treated
as fully opaque technical content: no autocorrect, no fuzzy/compound-split suggestion, no §6 capitalisation
transform, and no personal-dictionary learning of any kind applies to it - stronger than the hyphen's own
segment-by-segment treatment, and closer in spirit to how an email/URL/login fragment bypasses §6 entirely
(§10-§12), but scoped by token shape (contains `_`) rather than by field type.

---

## 9. Word Prediction & Adaptive Learning

### Three-Tier Architecture

| Tier | Technology | Latency | Activation |
|---|---|---|---|
| 1 - N-gram | Personal dictionary (SQLite) | <5 ms | Always active |
| 2 - Language detection | Character-trigram classifier (on-device) | <10 ms | After punctuation, on suspected language switch |
| 3 - Mini-LLM | ONNX Runtime | variable | When N-gram confidence falls below threshold, and only if enabled and installed |

The tier-3 model is not bundled with the app: it is installed by the user via a browser download plus a
system file picker (no `INTERNET` permission is used by the app itself), and can be independently enabled
or disabled (default on) regardless of whether a model file happens to be installed.

### Adaptive Learning
A tier-3-confident-but-tier-1-unknown word feeds back into the same dictionary-learning pipeline as any
other confirmed word. Because that learning signal is only generated when tier-1 didn't already know the
word, the LLM is invoked less and less often as the dictionary absorbs what it knows - an emergent behaviour,
not an explicit schedule. Battery load therefore decreases automatically as usage grows.

---

## 10. Email Mode

### E-01 - Email-Mode Keyboard
A dedicated bottom row (`?123`, `@`, `-`, a narrow space, `.`, Enter) replaces the ordinary bottom row for a
field detected as an email address. Detection is reliable for a field whose `InputType` variation declares
`TYPE_TEXT_VARIATION_EMAIL_ADDRESS`/`WEB_EMAIL_ADDRESS`; for an ambiguous field, a weak hint-text keyword
signal (checked across supported UI languages) nudges the extra row open and flashes a manual toggle rather
than auto-activating. `@` takes the comma key's primary slot (comma is demoted to its own popup); the dash
key is funded from the space key's own width, not by growing the row, and carries `_` as its single
alternative. The period key's long-press popup is a locale-resolved TLD list (`.com`, the device country's
own ccTLD, `.net`, `.org`) with the ccTLD pre-selected at the popup's centred position. The space key shows
only a plain glyph (never the language label) and the language-switch swipe (G-01) is disabled on it. All
fragments commit verbatim: no autocorrect, no §6 capitalisation, no learning, and Shift is never auto-armed.

---

## 11. URL Mode

### U-01 - URL-Mode Keyboard
A dedicated bottom row (`?123`, `https://`, `www.`, `/`, a narrow space, `.`, Enter) replaces the ordinary
bottom row for a field detected via `TYPE_TEXT_VARIATION_URI`, sharing the same locale-resolved TLD popup
mechanism as email mode (E-01). A manual toggle lets the user switch back to the ordinary letter keyboard
while still inside a URL field (e.g. to type a search query in a browser's address bar); it defaults on
whenever a URL field is entered and resets per field visit. Autocorrect and suggestions are fully suppressed
(the bar stays empty), but the clipboard-paste chip (§16, V-01) is not suppressed.

---

## 12. Credential & Privacy Management

### P-01 - Login-Field Detection
A focused field is classified as one of: none, username, email, or password. Email and password variants
are detected reliably from `InputType`; username has no reliable field-level signal and relies on a weak
hint-text keyword match or a manual extra-row toggle (§14, R-01).

### P-02 - Separate Credential Learning & Storage
Username/email values typed into a detected login field are learned immediately (no threshold) into a
credential store entirely separate from the ordinary dictionary; while such a field is focused, its
suggestions are drawn exclusively from this store, bypassing the ordinary dictionary/tier-1/tier-3 pipeline.
A password field is never learned from and never offered suggestions, unconditionally. After `@` in an
email-shaped login field, live frequency-ranked domain completion is offered from previously seen domains.

### P-03 - Opt-In Contact-Derived Email Suggestions *(default off)*
When enabled, on-device contacts are read live (never persisted) to widen email-suggestion ranking. The
permission is requested only from the Settings screen, never proactively while typing.

### P-04 - Never Save Credentials *(default off)*
When enabled, stops recording any *new* credential entries; does not remove or hide already-stored entries
or suggestions.

### P-05 - Credentials Review Screen
A dedicated settings screen lists stored credential entries, with a "Delete all" action and a per-entry
long-press remove action offering a "Copy" option first.

### P-06 - Autofill Inline Suggestions
On API 30+, a field eligible for Android Autofill can render the platform's own opaque, platform-drawn
inline-suggestions view in the suggestion bar's slot (e.g. surfacing a password manager's own entries)
instead of AdaptKey's ordinary suggestions.

---

## 13. Learned-Words Management

### W-01 - Separate Learned-Words Store
Every word the keyboard has personally learned from typing lives in its own store, structurally isolated
from the bundled dictionary so that a bundled-dictionary update or reimport can never affect it. A dedicated
review/edit screen lists and allows removing learned words directly - the only way to remove a word that
currently matches the live input, since S-02 prevents such a word from ever appearing as a suggestion (and
therefore from ever being reachable via drag-to-trash, G-04).

### W-02 - Variable Learn-Promotion Threshold
A word is ordinarily promoted from "pending" to permanently learned after 2 uncorrected occurrences. A token
recognised as a suspected unsplit compound, or one carrying an embedded mid-word capital, instead requires 4
occurrences before promotion - both checks are re-evaluated fresh each time (nothing cached), so a later
change in the detection logic reclassifies an already-pending word retroactively. A-07's undo correctly
decrements this counter regardless of which threshold applies. A single letter is never eligible for learning
at all (D-247) - the most common source is a fragment left behind by an unintended Enter mid-word, not
anything meant to be learned.

### W-03 - "Gelernt: X" Promotion Confirmation *(D-247)*
The moment a word is genuinely promoted to the learned dictionary (W-02's threshold crossed this exact
commit - never on an ordinary reinforcement of an already-learned word, which would fire constantly and add
no information) shows a dedicated, distinctly-coloured "Gelernt: X" chip in the suggestion bar, pinned ahead
of the ordinary next-word predictions rather than participating in their ranking. A plain tap dismisses it
(nothing changes - "doing nothing" already means "stays learned", matching the rest of this affordance's own
logic). Dragging it upward arms a two-zone variant of G-04's own drag-to-trash gesture, distinct from the
single-zone behaviour every ordinary suggestion keeps: a shallow zone unlearns the word only (no blacklist
mark, mirroring G-04's own self-taught-word branch exactly); a deeper zone blacklists it immediately and
permanently, bypassing G-04's own bundled-vs-self-taught origin check entirely (a freshly-promoted word is
never bundled, so that check would otherwise always resolve to the shallow outcome regardless of how far the
drag travelled) - deliberately stronger than the shallow zone, for when the user is certain the word should
never be reconsidered even if it recurs.

---

## 14. Extra Row (Swipe-Up Panel)

### R-01 - Extra Row
An upward swipe anywhere on the keyboard reveals a row sitting above the suggestion bar, hosting: an
always-visible emoji-panel button (no setting gates it - opening the emoji panel is described under L-03),
a settings-app shortcut, a clear-clipboard button (§16), a session-only touch-zone-visualisation toggle
(§17, T-06), a manual credential-mode toggle (also flashes on a weak login-field signal, §12), and a manual
URL-mode toggle (visible only while a URL field is focused, §11). A downward swipe closes the row first,
and a second downward swipe (with the row already closed) dismisses the keyboard (G-03). Activating a button
in the row does not auto-close it.

---

## 15. Symbol Layer & Calculator

See L-03 (`?123` key), L-06/L-08 (number row / catch-all page), and L-07 (calculator layer) in §2.

---

## 16. Clipboard Paste

### V-01 - Direct-Paste Chip
When the clipboard holds content, a direct-paste chip occupies the suggestion bar's slot, using the
target field's native paste action (rather than committing the clipboard text as a literal string) so that
app-specific paste behaviour is preserved. The chip is suppressed for a stale clip (older than 5 minutes)
and for a genuine non-text file clip. A sensitive clip (flagged by the platform) shows a masked preview.
Clipboard content is auto-cleared after a deferred, content-matched check for sensitive material. A
dedicated clear-clipboard button lives in the extra row (§14, R-01).

---

## 17. Touch-Pattern Visualisation

### T-06 - Touch-Zone Overlay
A settings screen and a live, on-keyboard toggle draw a coloured circle at each key's currently-learned
expected strike point (radius proportional to variance), sourced directly from T-03's model. Doubles as the
result view after a typing-style (re-)selection (K-01).

---

## 18. App Localisation (I18N)

### N-01 - App-Chrome Localisation
The app's own UI strings (settings, onboarding, dialogs, calibration) are localised into English and Greek
in addition to German, selected by the system's own language setting - a separate concern from which
language(s) the keyboard itself types in (§9/G-01).

---

## 19. Diagnostic Logging

### X-01 - In-App Diagnostic Log
A settings-gated (default off) ring buffer logs composing/selection/commit state and key-vibration outcomes,
retained for a short rolling window, viewable/copyable/exportable entirely on-device without a PC or `adb`.
Unconditionally excludes any content typed into a password field, regardless of the toggle state.

---

## 20. Configurable Parameters

| ID | Parameter | Type | Default |
|---|---|---|---|
| C-01 | Key proportions (space bar / comma / full stop / backspace) + the two L-02 spacing sliders | Slider | Gboard-like |
| C-02 | Suggestion re-sort delay | 0-600 ms | 300 ms |
| C-03 | Maximum number of suggestions | Integer (6-10) | 8 |
| C-04 | Word confirmation highlight | On/Off + colour | **On** |
| C-05 | Word blacklist | List + categories (bundled/user), editor defaults to user-only view | Seeded with a small bundled confusables + archaic-spelling set |
| C-06 | LLM activation threshold | N-gram confidence value | medium |
| C-07 | Shift grace window vs. surprising field capitalisation | 0-500 ms | 300 ms |
| C-08 | Secondary long-press symbols & corner hint labels | Per-key map + on/off | see L-05 |
| C-09 | Persistent number row (with shifted-symbol long-press) | On/Off | On |
| C-10 | No sentence start after a comma-terminated line (§6, e-mail salutation) | On/Off | On |
| C-11 | Long-press popup delay | Slider | see platform default |
| C-12 | Key sound | On/Off | Off |
| C-13 | Haptic feedback | On/Off | Off |
| C-14 | Tier-3 (mini-LLM) enabled | On/Off | On |
| C-15 | Diagnostic log enabled | On/Off | Off |
| C-16 | Never save credentials | On/Off | Off |
| C-17 | Contact-derived email suggestions | On/Off (permission-gated) | Off |
| C-18 | Pending-blacklist expiry window (G-04) | Days (1-30) | 7 |

Individual feature sections above also document domain-specific, non-configurable defaults (e.g. the
calculator layout's fixed key weights) that intentionally are not exposed here.

---

## Prerequisite

Android Studio with a configured Android SDK.
