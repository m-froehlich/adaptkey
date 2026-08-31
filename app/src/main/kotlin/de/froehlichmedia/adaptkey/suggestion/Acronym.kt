// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-404-followup: recognises a word typed entirely in capitals ("ETF", "AVD") as a deliberate acronym -
 * the explicit signal a user gives by typing that way, distinct from an ordinary word that merely happens
 * to carry one embedded capital partway through (a much weaker, coincidental signal a genuine unsplit
 * compound like "MeinAuto" also produces). Two independent call sites share this exact check and must stay
 * in lockstep, which is why it lives here rather than being duplicated: [de.froehlichmedia.adaptkey.
 * AdaptKeyService.learnThresholdFor] exempts an acronym from the compound learn-threshold (it promotes
 * after the ordinary two repetitions, not four - being an acronym is not evidence of a missing space), and
 * [de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider.bestCorrection] refuses to ever
 * autocorrect one away, known or not - deliberately typing in capitals is itself strong enough evidence
 * that silent replacement would be unwelcome.
 *
 * Deliberately a pure shape check with no dictionary lookup - length alone (at least two letters, so a
 * lone capitalised initial is not itself treated as an acronym) plus "every character is uppercase". A
 * stuck Caps Lock would also match this shape (see both call sites' own KDoc for that accepted trade-off).
 */
object Acronym {
    
    /** The shortest input length still eligible to count as an acronym. */
    private const val MIN_LENGTH = 2
    
    /**
     * @param word the word to check (any case)
     * @return true when [word] is at least [MIN_LENGTH] characters and every one of them is uppercase
     */
    fun isAcronym(word: String): Boolean {
        return word.length >= MIN_LENGTH && word.all { it.isUpperCase() }
    }
}
