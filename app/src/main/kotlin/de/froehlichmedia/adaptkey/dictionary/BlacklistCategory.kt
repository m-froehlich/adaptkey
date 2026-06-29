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
    OTHER
}
