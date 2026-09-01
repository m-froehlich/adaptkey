# AdaptKey - D-416 Design Plan: Deferred Auto-Space After Punctuation

**Status: implemented (§333, v1.0.85) - kept in the repo as the design record, not deleted.** Written per
this project's own convention for non-trivial design work (see
[`AdaptKey-Plan-Wortfamilien.md`](AdaptKey-Plan-Wortfamilien.md)'s precedent, since deleted once superseded -
this file follows the same shape but is being kept, since it still documents reasoning not fully repeated in
`AdaptKey-History.md`'s own §333 entry). See
[`AdaptKey-Rollback-D416-Deferred-Space.md`](AdaptKey-Rollback-D416-Deferred-Space.md) for the precise
rollback account, written alongside the implementation itself.

## 1. Goal

Replace A-12's current *eager* auto-space-after-punctuation (`.`/`!`/`?`/`,`) with a *deferred* one: typing
one of these marks inserts nothing physically. It only arms a state ("a space and, for `.`/`!`/`?`, a
capital both belong right after this position"). The space (and the capital) are materialised only once a
real next character is actually typed. Motivating wins (agreed with the user, see conversation):

- D-374 (trailing auto-space not cleaned up on field-leave, e.g. Google Keep) is eliminated **structurally**
  rather than patched - nothing is ever physically written that could be abandoned.
- D-363's declined colon/semicolon-vs-emoticon collision becomes resolvable in principle (not decided here -
  see §6 below, explicitly out of scope for this round).
- Unifies with the capitalisation engine's own existing "live pre-arm, no physical mutation until the next
  keystroke" philosophy ([armShiftForNextWord]) instead of running a separate, eager mechanism for the space.
- **Agreed explicitly:** no settings toggle for old-vs-new behaviour - the fan-out into every call site would
  defeat much of the simplification this migration is for. The safety net instead is (a) a clean, isolated
  commit sequence so `git revert` stays a real option, and (b) a hand-maintained rollback doc, written
  **during** implementation (not this design doc), describing precisely how to walk each change back. That
  rollback doc is a separate deliverable from this one - this file is the *design* pass, the rollback doc is
  an *implementation-time* artifact.

## 2. Current mechanism (verified against the real code, not recalled)

Lives almost entirely in `AdaptKeyService.kt`:

- `pendingPunctuationSpace: Boolean` + `pendingPunctuationSpacePos: Int` (ground-truth-captured absolute
  offset) are the live state.
- `handlePunctuationDelimiter(ic, raw)` (~line 3439): on committing a `SENTENCE_PUNCTUATION` mark
  (`.!?,` - comma added by D-320) at token end, physically `ic.commitText(" ", 1)`, arms
  `pendingPunctuationSpace`, ground-truth-captures its position, and **explicitly re-calls**
  `armShiftForNextWord(ic)` a second time - documented in its own KDoc as necessary because
  `finalizeAndCommit()`'s own internal call reads the text *before* the auto-space exists, so it can never
  see the sentence-ending mark. **This second call exists only because the space must physically land before
  the arm can see it - the deferred model removes the reason for it (see §4.3 below).**
- Exit paths, each independently handled at its own call site: explicit Space (absorbed), Backspace
  (`pendingPunctuationSpace && key.code == KeyCode.DELETE`, ~line 2390), Enter (removes the pending space
  first, so it isn't left dangling at end-of-line), an explicit caret move / field-left with nothing typed
  since (D-279, `removeAbandonedPunctuationAutoSpace` ~line 1605 - only when genuinely at the end of typed
  text, never when real content already follows).
- A-07's undo has its own explicit priority rule against this state (§ A-12 in spec: "A-12's own still-pending
  auto-space also takes priority... whenever both are armed by the same commit at once").
- D-320's digit-glue (`3.14` not `3. 14`) works by inserting the space eagerly, then **detecting and
  retracting** it when a digit follows a digit-before-punctuation pattern (`PunctuationSpaceGlue.gluesDigit`).
- `armShiftForNextWord()` (~line 5910) re-derives Shift state by reading real document text
  (`sentenceStartBefore(ic)` -> `ic.getTextBeforeCursor(...)` -> the pure `SentenceBoundary.isSentenceStart`).

**Key finding from reading `SentenceBoundary.isSentenceStart` directly** (not assumed): it requires
*non-empty trailing whitespace* before it will even consider whether the preceding mark ends a sentence -
`trailingWhitespace.isEmpty()` returns `false` (not a sentence start) unconditionally, treating the position
as "still inside a token/line - no delimiter yet". **This means a naive deferred-space implementation that
calls the existing `armShiftForNextWord`/`sentenceStartBefore` immediately after committing `.` with zero
characters after it would silently fail to arm Shift** - the exact kind of silent regression this design pass
exists to catch before it ships. Resolution proposed in §4.3.

## 3. New model - precise semantics

On committing `.`/`!`/`?`/`,` at token end (same gating as today: not mid-word, not in a login/URL field):

- Insert **nothing**. Set a state `pendingSpaceCaps` recording: the position, which mark it was, and whether
  it carries a capitalisation arm (`.`/`!`/`?` do; `,` does not - matches today's asymmetry exactly).
- Arm Shift for the next word **now**, via the `SentenceBoundary` virtual-space technique in §4.3 - not
  waiting for any physical mutation.

On the **next** keystroke, while `pendingSpaceCaps` is armed:

- **A letter or digit** (anything that starts a new token): materialise - commit a real space, then the
  character (capitalised per the already-armed Shift state, exactly as today). This is the single new
  "resolve the deferred decision" choke point - see §4.2 for where it lives.
- **Another mark from `SENTENCE_PUNCTUATION`**: glue directly (no space materialised at all - there was never
  anything to remove first, unlike today's run-continuation logic which has to *delete* the previous
  auto-space before gluing). Re-arm `pendingSpaceCaps` at the new position. Net effect identical to today's
  run behaviour (`"!?!"`, `"..."`, `".,"` still glue into one run, trailing space/arm only after the whole
  run) but reached with less machinery, since there's no physical space to manage mid-run any more.
- **A digit, when the mark was `.`/`,` and the character before the mark was itself a digit** (D-320's
  decimal case): glue directly, exactly like the punctuation-run case above - this is no longer a distinct
  "insert then detect and retract" mechanism, it collapses into the same "does the next real character want a
  space before it" decision every other case already needs. Concretely simpler, not merely equivalent.
- **An explicit Space**: nothing was physically written, so this is now just an ordinary Space keystroke -
  clear `pendingSpaceCaps` (its capitalisation arm should already have been consumed the moment the word
  after it gets typed, same as always) and proceed normally. **No special-case code needed at all** for this
  path any more - a real behavioural simplification versus today's explicit "absorb, don't double" logic.
- **Backspace**: nothing was physically written after the mark, so Backspace deletes the mark itself directly
  and clears `pendingSpaceCaps`. **This is the one user-visible, permanent behaviour change** (see §5) - today
  this same keystroke only removes the phantom space, requiring a second Backspace to reach the mark itself.
- **Enter**: nothing was physically written, so there is nothing to remove first (today's whole "Enter
  removes the pending auto-space before proceeding, to avoid a dangling trailing space" exit path disappears
  entirely - not simplified, *eliminated*). `pendingSpaceCaps` is simply cleared - a space at the very start
  of the next line is never wanted regardless of what preceded the line break.
- **Caret moved away explicitly, or the field is left, with `pendingSpaceCaps` still armed and nothing typed
  since**: nothing was physically written, so there is nothing to clean up - D-279's entire distinction
  ("only remove when genuinely at the end, never when real content already follows, since it would then be a
  load-bearing separator") **disappears**, because a virtual pending-space can never accidentally become a
  load-bearing separator - it was never physically there to begin with. This is the direct mechanism by which
  D-374 is eliminated, not merely mitigated.

## 4. Rule-by-rule mapping against every existing A-12-adjacent mechanism

### 4.1 D-320 decimal-glue - see §3 above. Folds into the general "does the next char want a space" decision.
No separate detect-and-retract logic needed; `PunctuationSpaceGlue.gluesDigit`'s actual predicate logic is
still needed and still reusable, just called *before* deciding to materialise a space rather than *after*
inserting one.

### 4.2 Where "materialise on next real keystroke" lives
`handleKey`'s `CHAR` branch (and `appendLongPressLetter`, per D-351's own KDoc listing these as the two
typing-triggered entry points already) is the natural single choke point - every letter/digit keystroke
already passes through here regardless of source (physical key, long-press popup, raw-coordinate fallback).
Check `pendingSpaceCaps` at the very top of that path, before any other handling, and materialise the space
first if armed. This keeps the "decide whether a space belongs here" logic in exactly one place, rather than
duplicated across every call site that can produce a letter/digit.

### 4.3 Capitalisation arming without a physical space (the `SentenceBoundary` finding from §2)
Do **not** change `SentenceBoundary.isSentenceStart` itself - it is a shared pure function with many other
callers who correctly rely on "no trailing whitespace = not a delimiter yet". Instead, add a narrow,
dedicated variant used only by the punctuation-commit path: read the real `getTextBeforeCursor(...)` (which,
under the deferred model, ends directly in `.`/`!`/`?`/`,` with **no** trailing whitespace) and pass
`"$before "` (a synthetic, never-written single space appended only for this one evaluation) into the
existing `SentenceBoundary.isSentenceStart`. This reuses the abbreviation-veto logic
(`Abbreviations.isNonTerminalPeriod`) that already lives inside `isSentenceStart` completely unchanged - no
duplication, no risk of the deferred path and the physical-document path disagreeing about what counts as an
abbreviation. This also directly removes the documented double-call workaround in `handlePunctuationDelimiter`
(§2) - under the deferred model there is only ever one arm call, made immediately on committing the mark,
because it no longer needs the physical space to exist first.

### 4.4 A-07 (undo) interaction
Today's explicit priority rule ("A-12's own still-pending auto-space also takes priority over this mechanism
... whenever both are armed by the same commit at once") **disappears as a rule that needs enforcing**, not
merely as something to re-derive: under the deferred model there is no physical auto-space character that
could compete with the undo-window's own target character for the same Backspace keystroke. A Backspace right
after a commit that both ended a sentence and is undo-eligible (`"ehvnicht."` -> `"eh nicht. "`) simply deletes
the punctuation mark (per §3's own Backspace rule) on the first press - one keystroke sooner than today, since
today's first Backspace only ever removed the phantom space. A second Backspace then lands exactly at the
split boundary and triggers A-07's revert normally, matching the existing "keeps working through intervening
whitespace deletions" pattern in spirit, just with the phantom-space step removed rather than walked through.
**Net effect: the priority rule is deleted, not reimplemented** - genuinely fewer states to reason about, not
just relocated complexity.

### 4.5 A-05 (retroactive split) interaction
A-05's own split-finding logic (peeling `"ehvnicht"` into `"eh"` + `"nicht"`) is untouched - it operates on the
token itself, never on the trailing punctuation/space. Only its *interaction* with the trailing state changes,
as covered by §4.4 above.

### 4.6 Comma-terminated-line exception (§6, "No Sentence Start After a Comma-Terminated Line")
Operates on structural line content (`SentenceBoundary`'s own `lineStart`/`suppressAfterCommaLine` branch,
`trimmed.last() == ','`) after a real newline has already been reached - it does not depend on whether the
comma's own trailing space was ever physically present, since it reads the *last non-whitespace character* of
the previous line either way. Should be unaffected. **Flagged to verify directly against the deferred-model
code once written** (not just assumed) - specifically: confirm the comma-then-Enter sequence (comma commits,
`pendingSpaceCaps` armed with no capitalisation half, then Enter clears it per §3) still leaves the previous
line's own last-non-whitespace character correctly readable as `,` for this check, exactly as intended.

### 4.7 Field-type exclusions (login/URL fields, mid-word delimiter)
Both are gates on *whether A-12 fires at all*, independent of the eager-vs-deferred timing question - carry
over completely unchanged; no interaction with this migration.

### 4.8 B-01/B-02/B-03 (hyphen handling)
No interaction. A-12's punctuation set is `.!?,` only; hyphens are not part of it today and this migration
does not add them. B-03's own dedicated undo window (for accepting a hyphen-compound chip) is independent
machinery, untouched.

### 4.9 S-05 (word confirmation highlight) / S-06 (verbatim chip)
No interaction - both concern the *current composing word*, not the punctuation/space state that follows a
commit. No change needed.

**Optional polish idea, explained in full (was too compressed in the first draft):** under the current
eager model, the moment you type `.`, a real space visibly appears - you get instant, physical confirmation
that the auto-space mechanism fired. Under the deferred model, typing `.` produces **no visible change at
all** until the next letter is typed - the mark just sits there like an ordinary committed character, with an
invisible "space + capital pending" state attached to it that has no on-screen representation. For most
typing this is harmless (the space appears a moment later, exactly where expected, once you keep typing) -
but it removes a small piece of implicit feedback the eager model gave for free: nothing on screen currently
distinguishes "punctuation committed, space pending" from "punctuation committed, nothing pending" (e.g. a
`.` that lands mid-sentence-abbreviation territory some other way, or once the mode has already exited).

The idea: give the pending state its *own* small, S-05-style visual cue - a faint/ghost marker right after
the punctuation mark (a thin cursor-adjacent hint, or a subtly different caret rendering) that disappears the
instant a real character is typed and the space materialises for real - the same "quietly confirm what the
system is about to do automatically" philosophy S-05 already applies to word correctness and A-05's live
split preview, just applied to this new invisible state instead of to a word.

This is a genuine, but *optional*, UX nicety - not required for the mechanism to work correctly, and not
something the spec or the user asked for originally. Flagged only because it occurred to me while mapping
S-05 against the new model, not because it's a known pain point. Decision: see §6.

### 4.10 D-373 (open backlog item) - **correction to the original backlog note**
D-416's own backlog bullet speculated D-373 ("hyphen after a capitalised word should re-arm capitalisation")
might collapse into this migration. On closer reading of B-01/B-02, that's not accurate: a hyphen never gets
its own auto-space under A-12 today (hyphens are §8's own territory, not `SENTENCE_PUNCTUATION`), so D-373 is
about extending the *capitalisation pre-arm* concept to a delimiter this migration does not touch, not about
deferred space at all. D-373 remains its own, independent, still-open backlog item - **not** in scope here.
(Progress.md's D-416 bullet should be corrected to drop this claim once this plan is reviewed.)

### 4.11 D-384 (open backlog item) - genuinely eased by this migration
D-384 asks for a trailing auto-space after a minus sign, but only when a space already precedes the minus.
Under the deferred model this is a small, natural extension: add "space-preceded minus" as one more trigger
that arms `pendingSpaceCaps` (no capitalisation half, mirroring comma) - no eager-insert-then-correct
machinery needs replicating for it, since the deferred model's whole point is that adding a new trigger no
longer requires its own physical-insertion dance. Worth keeping in mind as a natural, cheap follow-up once
D-416 lands, but still its own separate decision/round - not bundled into this migration's own scope.

### 4.12 D-370 (open backlog item) - related in spirit, not solved by this migration
D-370 (space-before/after-a-closing-quote should be retroactively removable) is a *different* kind of deferred
decision - it is about a space that was already committed needing later, conditional removal once a closing
quote appears, not about withholding a space until the next character is known. The "look at what actually
comes next before deciding" philosophy behind D-416 is a reasonable inspiration for a similar redesign of
quote-space handling, but this migration does not implement or resolve D-370 as a side effect. Stays open,
separate.

### 4.13 D-363 (Won't-Fix) and colon/semicolon/emoji - explicitly out of scope for this round
The user's own original framing floated that D-416 might make D-363 (colon/semicolon as auto-space triggers,
declined for colliding with emoticons) newly viable, since the collision was specifically caused by inserting
the space *before* knowing what follows. That technical objection would indeed no longer apply once §3's model
ships. **This plan deliberately does not decide that question.** Whether to add `:`/`;` to
`SENTENCE_PUNCTUATION` is a separate, later decision the user should make explicitly once the deferred model
is live and proven - not an automatic consequence of this migration. Flag it as worth revisiting then, not
now.

## 5. The one permanent, visible behaviour change

A Backspace pressed immediately after `.`/`!`/`?`/`,` with nothing typed since now deletes the mark itself
directly on the first press (today: first press removes the phantom space, second press removes the mark).
This is real, permanent muscle-memory adjustment, not an implementation detail - flagged explicitly per the
user's own "Umgewöhnung, aber insgesamt richtige Richtung" framing. No mitigation proposed beyond documenting
it clearly in the eventual History.md entry and spec update, and validating it on-device early (per the
earlier discussion: the muscle-memory risk is the one thing no rollback doc can undo, so catching a genuine
problem with it early, before it's lived with for weeks, is the real mitigation).

## 6. Open questions - resolved 2026-09-01

1. **Agreed.** §4.3's `SentenceBoundary` virtual-space approach - a dedicated variant/overload at the
   punctuation-commit call site, the shared pure function itself stays untouched.
2. **Deferred, not blocking.** User's own read: `SentenceBoundary.isSentenceStart` already just strips/
   ignores whitespace generically rather than depending on any specific amount of it, so the
   comma-terminated-line exception is likely already flexible enough to carry over unchanged. Verify once the
   code exists (§4.6), adjust then if actually needed - not a precondition for starting.
3. Explained in conversation (see chat) - decision pending the user's read of the explanation.
4. **Confirmed.** Scope is exactly today's `SENTENCE_PUNCTUATION` set (`.!?,`). D-384/D-363 stay separate,
   later decisions.
5. **One single combined round**, per explicit user instruction ("Bitte alles auf einmal. Sonst gibt es
   Chaos.") - matches this project's established bulk-round convention (build/test once at the end, not per
   sub-step). The rollback-notes doc is written alongside this one round as it happens, not retroactively.

## 7. Not part of this plan (reminder)

The rollback-notes markdown file the user asked for is an **implementation-time** artifact, written as the
migration actually happens - not produced by this design pass. This document's job is to make sure the
migration doesn't quietly break something *not yet noticed*, not to describe how to undo it once it exists.
