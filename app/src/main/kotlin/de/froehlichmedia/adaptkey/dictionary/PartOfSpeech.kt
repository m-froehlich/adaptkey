// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * Coarse part-of-speech tags used by the capitalisation hierarchy (§6).
 *
 * A word may carry several tags; a word tagged only [NOUN] (or [PROPER_NOUN]) is an unambiguous
 * noun and is auto-capitalised, whereas a word tagged both [NOUN] and another category is the
 * ambiguous case that is offered as a suggestion rather than corrected. The full distribution is
 * loaded from German lexicon dumps (DWDS / Wiktionary) in a later session; for now only a small
 * seed set is annotated.
 */
enum class PartOfSpeech {
    NOUN,
    VERB,
    ADJECTIVE,
    PREPOSITION,
    PROPER_NOUN,
    OTHER
}
