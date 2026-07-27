// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

/**
 * Pure, Android-free parse/validation core for the C-08 per-key secondary-symbol map (L-05).
 *
 * Each language's own default hint set is bundled as a `hints_<code>.tsv` file in exactly this
 * object's persisted string form ({@code "q=@;e=€;h=#"}), read via [LanguageLetterHintsLoader] and
 * parsed here. There is no user-facing override any more (D-281's per-language defaults are the sole
 * source); this object's job is purely to parse and validate that bundled data, protecting the
 * keyboard from a corrupt or malformed data file. Like [SettingsMapper], it is the unit-tested core of
 * the settings layer.
 *
 * Validation (applied in [sanitize], hence to every parse):
 * - keys are letters {@code a}-{@code z}; an upper-case key is lower-cased, anything else is dropped,
 * - symbols are non-blank, at most [MAX_SYMBOL_LENGTH] characters, and may not contain the
 *   [ENTRY_SEPARATOR] or [KEY_VALUE_SEPARATOR] used by the storage format,
 * - invalid or duplicate entries are discarded (last valid wins per key).
 */
object LetterHints {
    
    /** Separates entries in the persisted string. A symbol may never contain this character. */
    const val ENTRY_SEPARATOR = ';'
    
    /** Separates a key from its symbol in the persisted string. A symbol may never contain this character. */
    const val KEY_VALUE_SEPARATOR = '='
    
    /** Maximum length of a secondary symbol; the spec calls for a single or short glyph. */
    const val MAX_SYMBOL_LENGTH = 2
    
    /**
     * Tests whether a single key/symbol pair is a storable secondary-symbol assignment.
     *
     * @param key the letter the symbol is assigned to (case-insensitive)
     * @param symbol the secondary symbol
     * @return true when the pair passes validation and may be persisted
     */
    fun isValidEntry(key: Char, symbol: String): Boolean {
        val lower = key.lowercaseChar()
        if (lower < 'a' || lower > 'z') {
            return false
        }
        if (symbol.isEmpty() || symbol.length > MAX_SYMBOL_LENGTH) {
            return false
        }
        return symbol.none { it == ENTRY_SEPARATOR || it == KEY_VALUE_SEPARATOR }
    }
    
    /**
     * Normalises a raw per-key map: lower-cases keys, drops every invalid entry, and returns the
     * survivors ordered by key for a deterministic encoding.
     *
     * @param map the raw map, possibly containing invalid or duplicate entries
     * @return a validated map keyed by lower-case letters, sorted by key
     */
    fun sanitize(map: Map<Char, String>): Map<Char, String> {
        val result = sortedMapOf<Char, String>()
        for ((key, symbol) in map) {
            if (isValidEntry(key, symbol)) {
                result[key.lowercaseChar()] = symbol
            }
        }
        return result
    }
    
    /**
     * Parses a persisted string back into a validated per-key map. A null, blank, or fully invalid
     * input yields an empty map - the caller ([LanguageLetterHintsLoader]) falls back to the compiled-in
     * default in that case.
     *
     * @param value the persisted string, or null
     * @return the validated per-key map (possibly empty)
     */
    fun parse(value: String?): Map<Char, String> {
        if (value.isNullOrBlank()) {
            return emptyMap()
        }
        val raw = HashMap<Char, String>()
        for (entry in value.split(ENTRY_SEPARATOR)) {
            val parts = entry.split(KEY_VALUE_SEPARATOR, limit = 2)
            if (parts.size == 2 && parts[0].length == 1) {
                raw[parts[0][0]] = parts[1]
            }
        }
        return sanitize(raw)
    }
}
