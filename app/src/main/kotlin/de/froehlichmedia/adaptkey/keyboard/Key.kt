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
    SYMBOL
}

/**
 * A single immutable key description.
 *
 * @property label the primary glyph drawn on the key
 * @property code the functional category, see [KeyCode]
 * @property char the character emitted for [KeyCode.CHAR] keys, otherwise null
 * @property hint optional corner hint glyph for the long-press secondary (L-05 / L-06); not yet active in the scaffold
 * @property weight relative horizontal weight within its row; drives the configurable key proportions (L-02 / L-04, C-01)
 */
data class Key(
    val label: String,
    val code: KeyCode,
    val char: Char? = null,
    val hint: String? = null,
    val weight: Float = 1f
) {
    
    /**
     * Stable identifier used to key the personal offset model (T-03). Char keys are keyed by their
     * character, control keys by their [KeyCode] name. Unique within a single layout.
     */
    val id: String
        get() = if (code == KeyCode.CHAR && char != null) "c:$char" else code.name
}
