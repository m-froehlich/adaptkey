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
 * @property weight relative horizontal weight within its row; drives the key-proportion stubs (L-02 / L-04)
 */
data class Key(
    val label: String,
    val code: KeyCode,
    val char: Char? = null,
    val hint: String? = null,
    val weight: Float = 1f
)
