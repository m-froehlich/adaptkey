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
    
    /** L-03: the "ABC" key on the numeric layer (calculator page), returning to the letter view. */
    LETTERS,
    
    /**
     * D-531: the `?123` key on the special-characters page (the reform's second page), switching to
     * the numeric layer - the calculator page the reform re-scoped to genuinely numeric input
     * (number/date/phone fields, which open it on their own).
     */
    NUMERIC,
    
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
 * @property hint the key's RIGHT corner glyph (L-05 / L-06) - the long-press secondary and, while it is
 *           present, the release-default corner of the D-509 corner model. During the Alt-Belegungs-Reform
 *           migration (D-509..D-512) this legacy property stays exactly what it always was; every existing
 *           key is still right-corner-only.
 * @property leftHint D-509: the key's LEFT corner glyph - the second half of the corner pair, drawn at the
 *           key's left edge and committed on long-press release only where no right corner exists (see
 *           [defaultCornerHint]). null on every key until the reform's data phases fill it.
 * @property alternatives the ordered secondary alternatives shown in the D-01 long-press popup (most
 *           common first, which is pre-selected); a size of two or more triggers the popup, otherwise
 *           the single default corner glyph is applied immediately on long-press
 * @property alternatingPopup D-520: the popup is the currency key's alternating arrangement - the
 *           anchor cell (the first entry past the key's own left corner, if any) is kept centred
 *           directly over the host key with the remaining cells alternating left/right around it,
 *           and NO cell is pre-selected: the key's own release default ([defaultCornerHint], or -
 *           corner-less - the key's own symbol) is deliberately not among the cells, so a straight
 *           long-press release commits it, while a slide commits whichever cell the finger
 *           reached. Only the special-characters page's currency key sets this.
 * @property specialStyling D-527: whether the key wears the special function-key styling (the muted
 *           background every non-[KeyCode.CHAR] key gets - Shift, ABC, the calculator's sin/deg, the
 *           URL/email keys). The special-characters page's currency key is a §53 [KeyCode.TEXT] key
 *           but an ordinary symbol key in role, so it opts out and paints like any character key.
 * @property weight relative horizontal weight within its row; drives the configurable key proportions (L-02 / L-04, C-01)
 */
data class Key(
    val label: String,
    val code: KeyCode,
    val char: Char? = null,
    val hint: String? = null,
    val leftHint: String? = null,
    val alternatives: List<String> = emptyList(),
    val alternatingPopup: Boolean = false,
    val specialStyling: Boolean = true,
    val weight: Float = 1f
) {
    
    /**
     * D-509 corner model: the glyph a plain long-press release commits - the RIGHT corner ([hint]) when
     * the key carries one, otherwise the LEFT corner ([leftHint]) - one rule for every layout: the right
     * corner is always the default, a lone left corner becomes it by fallback. null when the key has no
     * corner at all. Behaviour-identical today (every existing key is right-corner-only); the reform's
     * later phases (D-511/D-512) are what first puts a left-only key on a layout.
     */
    val defaultCornerHint: String?
        get() = hint ?: leftHint
    
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
