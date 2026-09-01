# AdaptKey - D-416 Rollback Notes: Deferred Auto-Space After Punctuation

Written **during** the D-416 implementation itself, per the user's own explicit instruction
(2026-09-01): no settings toggle for old-vs-new behaviour (would fan complexity out across every touch
point - see [`AdaptKey-Plan-D416-Deferred-Space.md`](AdaptKey-Plan-D416-Deferred-Space.md)'s §1). Instead,
this file documents precisely what a future rollback would need to do, kept current with the actual
implementation as it happened, not reconstructed from memory afterward.

**If you are reading this to actually roll the migration back:** the single cleanest path is reverting the
implementation commit(s) below with `git revert` - everything in this document exists to make that safe to
do even if other, unrelated work has landed on top of it since, by spelling out exactly what belongs to this
migration and what a plain revert might miss or need to reconcile by hand.

## Implementation commit(s)

- `2515fc3` ("D-416: A-12's auto-space after sentence punctuation, from eager to deferred (v1.0.85)") - the
  whole migration, per explicit user instruction ("Bitte alles auf einmal."). A single `git revert 2515fc3`
  is expected to undo the code side of this migration cleanly, as long as it is still the tip of the
  relevant files' own history when reverted - see the file-by-file list below to reconcile if not.

## What changed, file by file (for reconciling a revert against later work)

- **`app/src/main/kotlin/de/froehlichmedia/adaptkey/PunctuationSpaceGlue.kt`** - `gluesDigit()`'s contract
  changed from a 3-character pattern (`digit, punctuation, an already-inserted space`) to 2 (`digit,
  punctuation` directly adjacent to the cursor - no space involved at all, since the deferred model never
  inserts one before this decision runs). **Reverting this file alone is not safe** without also reverting
  its one call site in `AdaptKeyService.kt` (`handlePunctuationDelimiter`) - the two must move together, the
  old 3-char contract only makes sense paired with the old eager-insertion call site.

- **`app/src/main/kotlin/de/froehlichmedia/adaptkey/capitalisation/SentenceBoundary.kt`** - purely additive:
  a new `withPendingTerminatorSpace(before: String): String` function. `isSentenceStart()` itself is
  **unchanged** - this migration deliberately did not touch its contract (see the design plan §4.3 for why).
  Reverting is safe in isolation: deleting this one function and its caller (below) fully undoes this file's
  side of the change.

- **`app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt`** - the bulk of the migration. Every
  touch point is tagged `D-416` in its own comment/KDoc, searchable directly (`grep -n "D-416"`). In order of
  appearance:
  1. Field declarations: `pendingPunctuationSpace`/`pendingPunctuationSpacePos` removed outright (no
     replacement field - the new model is stateless, re-derived live from the document each time).
  2. `onStartInput()`: the `pendingPunctuationSpace = false` reset line removed (nothing to reset).
  3. `reclaimWordAtCaret()`: the `consumeStrandedPunctuationSpace(ic)` call and its guarding comment removed;
     `suppressNextReclaimSpaceReset`'s own KDoc narrowed to describe only its remaining (D-29) purpose.
  4. `consumeStrandedPunctuationSpace()` - the whole function **deleted**. To reconstruct: it used to
     ground-truth-capture the auto-space's absolute document position at arm time
     (`pendingPunctuationSpacePos`, via `ComposingAnchor.resolve`) and delete it on an abandoned caret
     move/field-leave, but only when nothing already followed it (mid-text load-bearing separator
     exception). See the pre-D-416 version of this file in git history for the exact body if ever needed.
  5. `handleKey()`'s `KeyCode.DELETE` branch: the whole D-273 priority carve-out ("a still-pending A-12
     auto-space takes priority over A-07's undo") **deleted**, not replaced - the plan's §4.4 explains why
     the ordinary undo-window logic already produces the correct behaviour unassisted once there is no
     physical space to compete over.
  6. `handleKey()`'s `KeyCode.CHAR` branch, `composing.isEmpty()` block: `pendingPunctuationSpace = false`
     replaced with the new live materialisation check (reads `getTextBeforeCursor(1, 0)`, commits a real
     space when a bare pending mark is found).
  7. `KeyCode.SPACE` branch: simplified to an unconditional `finalizeAndCommit(ic, " ", inferred)` - the old
     `spaceDelimiter`/empty-string special case removed.
  8. `handleEnter()`: the D-270 "remove the pending space first" block **deleted** outright.
  9. `consumeSelection()`: the `pendingPunctuationSpace = false` reset line removed.
  10. `handleBackspace()`: its own D-262/D-406 guard block (removing only the phantom space) **deleted**
      outright - falls through to the ordinary delete path unconditionally now.
  11. `handlePunctuationDelimiter()` - the core rewrite. Old body preserved verbatim in git history (the
      pre-D-416 revision of this file) if ever needed to reconstruct by hand rather than via `git revert`.
  12. `armShiftForNextWord()`: gained the `keyboardView?.pendingSpaceIndicator = ...` push (two spots - the
      early-return login/URL branch, and the normal-path end). Purely additive with respect to Shift-arming
      itself; safe to strip in isolation if only the visual indicator is being reverted, without touching the
      rest of the migration.
  13. `sentenceStartBefore()`: wraps its `before` string through `SentenceBoundary.withPendingTerminatorSpace`
      before delegating. Purely additive.

- **`app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/AdaptKeyboardView.kt`** - purely additive, and
  independently revertible from everything else: a new public `pendingSpaceIndicator: Boolean` property, a
  new `pendingSpaceIndicatorPaint`, and one new `canvas.drawCircle(...)` call gated on
  `key.code == KeyCode.SPACE && pendingSpaceIndicator`. Deleting these three additions alone reverts only the
  visual indicator (§4.9's "quiet confirmation" idea) while leaving the underlying deferred-space mechanism
  itself intact - the two are independent if a future rollback only wants to undo one.

- **Tests**: `PunctuationSpaceGlueTest.kt` rewritten for the new 2-character contract (paired 1:1 with the
  production file above - revert together). `SentenceBoundaryTest.kt` gained new `D-416` cases for
  `withPendingTerminatorSpace`; its existing cases are all unchanged (the pure `isSentenceStart` contract
  itself was never touched).

## What a rollback would look like in practice

1. `git revert` the implementation commit(s) once identified above. If clean, done - re-run the full test
   suite to confirm.
2. If unrelated work has since built on top of any of the touched functions (most likely
   `handlePunctuationDelimiter`, `armShiftForNextWord`, or `handleBackspace`, given how central they are),
   the revert will conflict there - resolve by hand using the "what changed, file by file" list above as the
   map of what belongs to this migration versus what does not.
3. Bump the version (this project's own convention - a rollback is itself a real, distinct piece of work).
4. Append a fresh `AdaptKey-History.md` entry recording the rollback and, ideally, *why* - the whole point of
   this document existing is that a future rollback should not need to silently guess.

## The one thing no code rollback can undo

Per the original design discussion: even a clean code revert does not undo the muscle-memory adjustment
users will have made to the one permanent behaviour change (Backspace right after punctuation deletes the
mark directly, not a phantom space first) while the deferred model was live. This is a real cost of reverting
late, not a code-quality concern - the earlier a genuine problem with the new model surfaces, the cheaper any
eventual rollback actually is, regardless of how precise these notes are.
