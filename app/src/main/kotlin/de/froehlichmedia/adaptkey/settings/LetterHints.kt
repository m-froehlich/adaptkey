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
 * - a value is one or more [VARIANT_SEPARATOR]-separated variants (D-497); every variant is non-blank,
 *   at most [MAX_SYMBOL_LENGTH] characters, and may not contain the [ENTRY_SEPARATOR] or
 *   [KEY_VALUE_SEPARATOR] used by the storage format,
 * - invalid or duplicate entries are discarded (last valid wins per key).
 */
object LetterHints {
    
    /** Separates entries in the persisted string. A variant may never contain this character. */
    const val ENTRY_SEPARATOR = ';'
    
    /** Separates a key from its value in the persisted string. A variant may never contain this character. */
    const val KEY_VALUE_SEPARATOR = '='
    
    /**
     * D-497: separates one key's long-press variants inside a single value - `e=ę,ė` makes the `e` key a
     * two-cell popup (first variant = corner hint = pre-selected cell). A single-variant value behaves
     * exactly as before D-497. Reserved: a variant may never contain this character.
     */
    const val VARIANT_SEPARATOR = ','
    
    /** Maximum length of a single variant; the spec calls for a single or short glyph. */
    const val MAX_SYMBOL_LENGTH = 2
    
    /**
     * Tests whether a single key/value pair is a storable secondary-symbol assignment.
     * 
     * @param key the letter the symbol is assigned to (case-insensitive)
     * @param symbol the secondary symbol, or comma-separated variant list (D-497)
     * @return true when the pair passes validation and may be persisted
     */
    fun isValidEntry(key: Char, symbol: String): Boolean {
        val lower = key.lowercaseChar()
        if (lower < 'a' || lower > 'z') {
            return false
        }
        if (symbol.isEmpty()) {
            return false
        }
        // D-497: every comma-separated variant is validated exactly like the pre-D-497 whole-value
        // single symbol was - an empty variant (a stray leading/trailing/doubled comma) fails the same way.
        return symbol.split(VARIANT_SEPARATOR).all { variant ->
            variant.isNotEmpty() && variant.length <= MAX_SYMBOL_LENGTH &&
                variant.none { it == ENTRY_SEPARATOR || it == KEY_VALUE_SEPARATOR }
        }
    }
    
    /**
     * Normalises a raw per-key map: lower-cases keys, drops every invalid entry, and returns the
     * survivors ordered by key for a deterministic encoding. Values may carry comma-separated variants
     * (D-497) - they pass through unchanged here; the popup split happens where the [Key][de.
     * froehlichmedia.adaptkey.keyboard.Key] is built ([KeyboardLayout.letterKey]).
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
    
    /**
     * D-513: parses a pack's hints DELTA - the same `key=value` persisted string [parse] reads, with one
     * addition: an entry with an EMPTY value (`a=`) is an explicit REMOVAL, dropping the compiled-in
     * base's right-corner/extras data for that key (the Alt-Belegungs-Reform's §2.10 umlaut policy - it
     * is how English keeps ä ö ü ß out entirely, and how a pack retires a stale German-derived symbol).
     * The reform's left corners are compiled-in per-key data ([KeyboardLayout.BASE_LEFT_CORNERS]) and
     * are never affected by a removal.
     *
     * A missing value is representable only here, never in [parse]'s own validated map - a null marks
     * the removal, and [unionMerge] is the only consumer.
     *
     * @param value the persisted delta string, or null
     * @return the validated per-key delta: the key's own variant list, or null for an explicit removal
     */
    fun parseDelta(value: String?): Map<Char, String?> {
        if (value.isNullOrBlank()) {
            return emptyMap()
        }
        val raw = HashMap<Char, String?>()
        for (entry in value.split(ENTRY_SEPARATOR)) {
            val parts = entry.split(KEY_VALUE_SEPARATOR, limit = 2)
            if (parts.size == 2 && parts[0].length == 1) {
                val key = parts[0][0].lowercaseChar()
                if (key < 'a' || key > 'z') {
                    continue
                }
                val symbol = parts[1]
                raw[key] = if (symbol.isEmpty()) null else symbol
            }
        }
        // A non-removal value passes the same validation as always; a removal entry is valid by
        // definition. A malformed variant value demotes to a plain removal rather than resurrecting
        // the base data - dropping towards the base is the safe direction.
        val result = sortedMapOf<Char, String?>()
        for ((key, symbol) in raw) {
            result[key] = if (symbol == null || isValidEntry(key, symbol)) symbol else null
        }
        return result
    }
    
    /**
     * The four German umlaut/scharf-s letters (§2.10's per-language-data policy): the only base values
     * that never append to a key a language owns with its own entry. L-05's own founding rationale is
     * that a set tuned for German is not what a language without those characters needs, so `a=á` keeps
     * Czech a Czech (no silent ß/ä extras), while the genuinely universal base symbols (@ # $ € π × ƒ)
     * and the explicitly-universal ñ (§2.6) still append - exactly the two merged orders §5 pins: French
     * `e=é,è,ê,ë` merges to `é,è,ê,ë,€` and Polish `n=ń` to the `[( | ñ | ń ]` straddle.
     */
    private val GERMAN_SPECIFIC_LETTERS = setOf("ä", "ö", "ü", "ß")
    
    /**
     * D-513: the base+delta UNION merge - the reform's operational "alle lateinischen Layouts übernehmen
     * die deutsche Alt-Belegung". Per key:
     * - an explicit delta REMOVAL (a null from [parseDelta]) drops the key entirely,
     * - a delta entry makes its first variant the right corner, with the delta's remaining variants as
     *   the first extras and the base's own value appended LAST when it is genuinely new (never
     *   duplicated, never one of [GERMAN_SPECIFIC_LETTERS]),
     * - a key only the base carries stays exactly the base's.
     *
     * The result is the same variant-string form [parse] produces, so [KeyboardLayout.letterKey]'s own
     * corner/extras assembly consumes it unchanged - including the dual-corner straddle case, where the
     * appended base value (e.g. ñ under Polish `n=ń`) becomes the popup's middle cell.
     *
     * @param base the compiled-in universal base map (the reform's German right corners)
     * @param delta the language's own delta, removals included
     * @return the merged per-key variant map, sorted by key
     */
    fun unionMerge(base: Map<Char, String>, delta: Map<Char, String?>): Map<Char, String> {
        val result = sortedMapOf<Char, String>()
        for ((key, baseValue) in base) {
            if (!delta.containsKey(key)) {
                result[key] = baseValue
            }
            // An explicit removal (key present, value null) drops the key entirely - the reform's left
            // corners are code-side data and are never affected.
        }
        for ((key, deltaValue) in delta) {
            val variants = deltaValue?.split(VARIANT_SEPARATOR) ?: continue
            val extras = variants.drop(1).toMutableList()
            val baseValue = base[key]
            if (baseValue != null && !variants.contains(baseValue) && baseValue !in GERMAN_SPECIFIC_LETTERS) {
                extras.add(baseValue)
            }
            result[key] = (listOf(variants.first()) + extras).joinToString("$VARIANT_SEPARATOR")
        }
        return result
    }
}
