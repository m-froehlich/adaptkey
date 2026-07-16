// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

/**
 * D-139: a defensive circuit breaker against a runaway callback cascade - **not** a confirmed fix for the
 * reported "text jitters, characters get scrambled" glitch (no repro exists yet), but a structural risk
 * found directly in [AdaptKeyService.onUpdateSelection] while investigating it: that function reacts to a
 * caret move by issuing its own further `InputConnection` mutations
 * ([AdaptKeyService.reclaimWordAtCaret]/`finishComposingText()`), either of which could in principle
 * re-trigger the very same callback - and this codebase has hit a genuinely self-triggering
 * `onUpdateSelection` cascade before (§32's D-87 investigation, a different specific bug but the same class
 * of risk). If that ever happens, [isBurst] lets the caller stop reacting rather than let the cascade
 * continue to escalate, which fits the reported symptom (a runaway sequence of small edits) better than
 * doing nothing.
 *
 * A plain sliding counter, deliberately simple and Android-free so it is unit-testable: [isBurst] takes the
 * current time explicitly rather than reading a clock itself.
 *
 * @property windowMs the rolling window length
 * @property limit calls beyond this many within [windowMs] count as a burst
 */
class CallbackBurstGuard(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val limit: Int = DEFAULT_LIMIT
) {
    
    private var windowStartMs = 0L
    private var count = 0
    
    /**
     * Records one call and reports whether it is part of a burst. The window resets on the first call after
     * it elapses, so an ordinary, sparse sequence of calls (any legitimate caret move or keystroke) never
     * trips this - only a sequence tight enough to blow past [limit] within [windowMs] does.
     *
     * @param nowMs the current time (any monotonic clock, e.g. `SystemClock.uptimeMillis()`)
     * @return true once more than [limit] calls have landed within the current [windowMs] window
     */
    fun isBurst(nowMs: Long): Boolean {
        if (nowMs - windowStartMs > windowMs) {
            windowStartMs = nowMs
            count = 0
        }
        count++
        return count > limit
    }
    
    companion object {
        
        // D-139: deliberately conservative (high limit, short window) to stay far above anything legitimate
        // use could produce - a held backspace repeats as fast as every 45ms (§59), so even a sustained hold
        // manages at most ~7 ticks in a 200ms window; 40 is a wide safety margin above that, while a genuine
        // unthrottled cascade (no deliberate delay between steps at all, unlike a timer-driven repeat) would
        // be expected to blow past it almost immediately. A considered starting point, not device-tuned -
        // easy to retune later, same precedent as this project's other threshold tunings.
        const val DEFAULT_WINDOW_MS = 200L
        const val DEFAULT_LIMIT = 40
    }
}
