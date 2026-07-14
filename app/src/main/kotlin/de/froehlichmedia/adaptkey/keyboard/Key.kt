// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * The functional category of a key. Char keys carry an actual [Key.char];
 * the remaining values are control keys handled by the service.
 */
enum class KeyCode {
    CHAR,
    SHIFT,
    DELETE,
    ENTER,
    SPACE,
    SYMBOL,
    
    /** L-03: the "ABC" key on the numeric/symbol layer, returning to the letter view. */
    LETTERS,
    
    /**
     * §53 (D-103/D-104): a key whose tap commits a literal multi-character [Key.label] verbatim (e.g. the
     * calculator page's `sin`/`deg` keys) - unlike [CHAR], which is limited to a single [Key.char].
     */
    TEXT
}

/**
 * A single immutable key description.
 *
 * @property label the primary glyph drawn on the key
 * @property code the functional category, see [KeyCode]
 * @property char the character emitted for [KeyCode.CHAR] keys, otherwise null
 * @property hint optional corner hint glyph for the long-press secondary (L-05 / L-06); not yet active in the scaffold
 * @property alternatives the ordered secondary alternatives shown in the D-01 long-press popup (most
 *           common first, which is pre-selected); a size of two or more triggers the popup, otherwise
 *           the single [hint] is applied immediately on long-press
 * @property weight relative horizontal weight within its row; drives the configurable key proportions (L-02 / L-04, C-01)
 */
data class Key(
    val label: String,
    val code: KeyCode,
    val char: Char? = null,
    val hint: String? = null,
    val alternatives: List<String> = emptyList(),
    val weight: Float = 1f
) {
    
    /**
     * Stable identifier used to key the personal offset model (T-03). Char keys are keyed by their
     * character, [TEXT] keys by their (unique-per-layout) label, control keys by their [KeyCode] name.
     * Unique within a single layout.
     */
    val id: String
        get() = when {
            code == KeyCode.CHAR && char != null -> "c:$char"
            code == KeyCode.TEXT -> "t:$label"
            else -> code.name
        }
}
