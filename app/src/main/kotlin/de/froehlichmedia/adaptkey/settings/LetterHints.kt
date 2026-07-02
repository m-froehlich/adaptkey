// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import de.froehlichmedia.adaptkey.keyboard.KeyboardLayout

/**
 * Pure, Android-free encode/decode/validation core for the C-08 per-key secondary-symbol map (L-05).
 *
 * The map is persisted as a single string of the form {@code "q=@;e=€;h=#"} under
 * [SettingsStore.KEY_LETTER_HINTS]. This object owns both directions of that translation plus the
 * validation that protects the keyboard from corrupt or hostile stored values, and it exposes the
 * curated symbol [PALETTE] the editor offers. Like [SettingsMapper], it is the unit-tested core of
 * the settings layer; the Android-facing IO around it ([SettingsStore]) is left to instrumented tests.
 *
 * Validation (applied in [sanitize], hence to every parse and encode):
 * - keys are letters {@code a}-{@code z}; an upper-case key is lower-cased, anything else is dropped,
 * - symbols are non-blank, at most [MAX_SYMBOL_LENGTH] characters, and may not contain the
 *   [ENTRY_SEPARATOR] or [KEY_VALUE_SEPARATOR] used by the storage format,
 * - invalid or duplicate entries are discarded (last valid wins per key).
 *
 * An empty result falls back to [KeyboardLayout.DEFAULT_LETTER_HINTS] in [decodeOrDefault], matching
 * the spec decision that the keyboard never ends up with no secondary symbols at all; removing every
 * symbol therefore reverts to the default mapping on the next load.
 */
object LetterHints {
    
    /** Separates entries in the persisted string. A symbol may never contain this character. */
    const val ENTRY_SEPARATOR = ';'
    
    /** Separates a key from its symbol in the persisted string. A symbol may never contain this character. */
    const val KEY_VALUE_SEPARATOR = '='
    
    /** Maximum length of a secondary symbol; the spec calls for a single or short glyph. */
    const val MAX_SYMBOL_LENGTH = 2
    
    /**
     * The curated set of secondary symbols the editor offers (no free-text entry). All entries are
     * single glyphs and none contain the storage delimiters, so every palette symbol is a valid
     * assignment for any letter. Ordered roughly by everyday usefulness for German input.
     */
    val PALETTE: List<String> = listOf(
        "@", "€", "£", "$", "#", "%", "&", "§", "°", "*",
        "+", "-", "_", "~", "^", "|", "/", "\\", "(", ")",
        "[", "]", "{", "}", "<", ">", "!", "?", "\"", "'",
        "…", "•", "©", "®", "™", "µ", "±", "×", "÷", "«", "»"
    )
    
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
     * Encodes a per-key map into its persisted string form. The map is sanitised first, so the output
     * is always valid and deterministically ordered; an empty (or fully invalid) map encodes to "".
     *
     * @param map the per-key map to encode
     * @return the persisted string, e.g. {@code "e=€;q=@"}
     */
    fun encode(map: Map<Char, String>): String {
        return sanitize(map).entries.joinToString(ENTRY_SEPARATOR.toString()) { (key, symbol) ->
            "$key$KEY_VALUE_SEPARATOR$symbol"
        }
    }
    
    /**
     * Parses a persisted string back into a validated per-key map, without applying the default
     * fallback. A null, blank, or fully invalid input yields an empty map.
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
    
    /**
     * Parses a persisted string and falls back to [KeyboardLayout.DEFAULT_LETTER_HINTS] when the result
     * is empty, so the keyboard always has a usable secondary-symbol mapping.
     *
     * @param value the persisted string, or null
     * @return the validated per-key map, or the default mapping when empty
     */
    fun decodeOrDefault(value: String?): Map<Char, String> {
        val parsed = parse(value)
        return parsed.ifEmpty { KeyboardLayout.DEFAULT_LETTER_HINTS }
    }
}
