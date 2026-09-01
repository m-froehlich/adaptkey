// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * D-389: un-learns every [DictionaryStore.learnedWords] entry whose own [DictionaryStore.learnedFrequencyOf]
 * `lastTouched` stamp is older than the configured [LearnedWordExpiryWindow] - a coarse, once-a-day
 * housekeeping sweep (the actual daily throttling lives in [de.froehlichmedia.adaptkey.AdaptKeyService],
 * which is the only caller that knows "once a day" against real wall-clock time; this object is a plain,
 * Android-free function of whatever `now`/window it is given, so it stays unit-testable against
 * [InMemoryDictionaryStore]).
 *
 * Reuses [DictionaryStore.forget] - the exact same permanent, irreversible removal G-04's drag-to-trash
 * already performs - rather than a new deletion primitive; an expired word is un-learned, not merely
 * demoted, matching the feature's own name ("entlernt") and this app's existing G-04 precedent.
 * Deliberately scoped to individual learned words alone, not the learned bigram/trigram tables - those
 * carry no `last_touched` column of their own yet (D-365/D-366 left that as a deliberately separate,
 * not-yet-built extension).
 */
object LearnedWordExpirySweep {
    
    /**
     * @param store the dictionary store to sweep
     * @param now the current time (epoch millis) - injected, not read from the wall clock, so a caller
     *        (and its own tests) controls it explicitly
     * @param window how long a word may go untouched before it expires; [LearnedWordExpiryWindow.NEVER]
     *        (`days == null`) skips the sweep entirely, never un-learning anything
     * @return the words that were un-learned, in no particular order (for logging/diagnostics; callers that
     *         only care about the sweep having happened at all can ignore the return value)
     */
    fun sweep(store: DictionaryStore, now: Long, window: LearnedWordExpiryWindow): List<String> {
        val days = window.days ?: return emptyList()
        val cutoff = now - days * DAY_MILLIS
        val expired = store.learnedWords()
            .map { it.word }
            .filter { word -> (store.learnedFrequencyOf(word)?.lastTouched ?: now) < cutoff }
        expired.forEach { store.forget(it) }
        return expired
    }
    
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
}
