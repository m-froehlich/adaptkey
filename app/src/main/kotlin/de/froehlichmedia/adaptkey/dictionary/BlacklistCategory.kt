// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.dictionary

/**
 * Category of a blacklist entry (A-04).
 *
 * The category enables blanket exclusions: e.g. importing all [OLD_SPELLING] words from the
 * pre-reform German orthography excludes them as a group.
 */
enum class BlacklistCategory {
    USER,
    OLD_SPELLING,
    PROFANITY,
    
    /**
     * D-176: shipped with the app itself, not user-added - a small, reviewed set of words that
     * technically exist in some bundled dictionary but are an all-too-common mistype of a much more
     * common word in another currently-consulted language ("due"/"sue" losing to "die"/"sie" while
     * German is active) or, entirely within one language, of a far more frequent word ("ddr" losing to
     * "der"). Seeded idempotently into the relevant language's own store on every service start (see
     * `AdaptKeyService.installStores()`), so it reaches an already-installed device too, not only a
     * fresh one.
     */
    BUNDLED,
    
    OTHER
}
