// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

import android.content.Context

/**
 * Tracks how often the user has committed a word that is not (yet) in the dictionary, so a genuinely new
 * word is only promoted to the learned lexicon after it has been seen a few times - not eagerly on the
 * first accept/autocorrect, which would otherwise learn typos as well (D-37). Backed by a small private
 * SharedPreferences file of its own, separate from the SQLite dictionary, so no schema migration is
 * needed; the counts are transient and cleared on promotion.
 *
 * Kept in its own object (Android glue, like the other stores); the promotion/threshold policy is applied
 * by the service.
 */
object PendingLearnStore {
    
    private const val PREFS = "adaptkey_pending_learn"
    
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    private fun key(word: String): String = word.lowercase()
    
    /**
     * Increments the pending count for [word].
     *
     * @param context any valid context
     * @param word the just-committed unknown word
     * @return the new pending count
     */
    fun increment(context: Context, word: String): Int {
        val p = prefs(context)
        val next = p.getInt(key(word), 0) + 1
        p.edit().putInt(key(word), next).apply()
        return next
    }
    
    /**
     * Decrements the pending count for [word] (a rejected correction), removing it at zero.
     *
     * @param context any valid context
     * @param word the word to count down
     */
    fun decrement(context: Context, word: String) {
        val p = prefs(context)
        val next = p.getInt(key(word), 0) - 1
        if (next <= 0) {
            p.edit().remove(key(word)).apply()
        } else {
            p.edit().putInt(key(word), next).apply()
        }
    }
    
    /**
     * Clears the pending count for [word] (e.g. once it has been promoted to the dictionary).
     *
     * @param context any valid context
     * @param word the word to clear
     */
    fun clear(context: Context, word: String) {
        prefs(context).edit().remove(key(word)).apply()
    }
    
    /**
     * @param context any valid context
     * @param word the word to look up
     * @return the current pending count, or 0 when none
     */
    fun count(context: Context, word: String): Int {
        return prefs(context).getInt(key(word), 0)
    }
}
