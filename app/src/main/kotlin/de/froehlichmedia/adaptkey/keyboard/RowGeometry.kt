// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

/**
 * D-397: the single shared row/column position model for a row-based keyboard geometry - which
 * character sits in which row, for a given [LayoutKind]. Extracted from
 * [de.froehlichmedia.adaptkey.suggestion.KeyboardProximity]'s own per-layout `ROWS` lists (D-442), which
 * used to duplicate this exact data purely for typo-adjacency scoring (D-28/D-38) - now the one place both
 * that purpose and [AdaptKeyboardView]'s own T-03 vertical touch-drift capping (D-397) read from, so the
 * two can never silently disagree about a layout's own geometry.
 */
object RowGeometry {
    
    /** The persistent number row (L-06) is identical across every layout - digits never move. */
    const val DIGIT_ROW = "1234567890"
    
    /**
     * Each [LayoutKind]'s own three letter rows, top to bottom - exactly the literal strings
     * [de.froehlichmedia.adaptkey.suggestion.KeyboardProximity]'s per-layout objects used to hardcode
     * individually; kept here as the one shared source. See each layout's own key-building object
     * ([KeyboardLayout], [AzertyLayout], [AzerbaijaniLayout], [GreekLayout], [SerbianLayout],
     * [JcukenLayout]) for why a given row reads the way it does.
     */
    private val LETTER_ROWS: Map<LayoutKind, List<String>> = mapOf(
        LayoutKind.LATIN_QWERTZ to listOf("qwertzuiop", "asdfghjkl", "yxcvbnm"),
        LayoutKind.LATIN_QWERTY to listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
        LayoutKind.LATIN_AZERTY to listOf("azertyuiop", "qsdfghjklm", "wxcvbn"),
        LayoutKind.LATIN_AZERBAIJANI to listOf("püşudbmyohxj", "gısarnəlicğ", "çfvtkeqzö"),
        LayoutKind.GREEK to listOf("ςερτυθιοπ", "ασδφγηξκλ", "ζχψωβνμ"),
        LayoutKind.SERBIAN_CYRILLIC to listOf("љњертзуиопшђж", "асдфгхјклчћ", "џцвбнм"),
        LayoutKind.RUSSIAN_CYRILLIC to listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю"),
        LayoutKind.UKRAINIAN_CYRILLIC to listOf("йцукенгшщзхї", "фівапролджє", "ґячсмитьбю")
    )
    
    /**
     * The full row list for [layoutKind]: the digit row first when [includeDigitRow], then each of the
     * three letter rows top to bottom.
     *
     * @param layoutKind the active row geometry
     * @param includeDigitRow whether the persistent number row (C-09) is currently shown
     * @return the rows, top to bottom; falls back to [LayoutKind.LATIN_QWERTY] for a future [LayoutKind]
     *         added without a matching entry here, mirroring [de.froehlichmedia.adaptkey.suggestion.
     *         KeyboardProximityRegistry]'s own fallback
     */
    fun rowsFor(layoutKind: LayoutKind, includeDigitRow: Boolean): List<String> {
        val letters = LETTER_ROWS[layoutKind] ?: LETTER_ROWS.getValue(LayoutKind.LATIN_QWERTY)
        return if (includeDigitRow) listOf(DIGIT_ROW) + letters else letters
    }
    
    /**
     * @param layoutKind the active row geometry
     * @param includeDigitRow whether the persistent number row (C-09) is currently shown
     * @param c the character to locate
     * @return the 0-based row index [c] sits in (0 = topmost), or null when [c] is not part of this
     *         layout's own character rows at all (a control key, or a symbol/calculator-page character)
     */
    fun rowIndexOf(layoutKind: LayoutKind, includeDigitRow: Boolean, c: Char): Int? {
        val index = rowsFor(layoutKind, includeDigitRow).indexOfFirst { it.contains(c) }
        return if (index >= 0) index else null
    }
    
    /**
     * D-397: whether [c] has a genuine row above it in [layoutKind]'s own geometry - used by
     * [AdaptKeyboardView]'s generic vertical touch-drift cap so a key's learned zone reaches less far into
     * whichever row happens to sit above it, on any layout, without a per-key/per-language list to maintain.
     *
     * @return true when a row above exists (a letter row, or the digit row when [includeDigitRow])
     */
    fun hasRowAbove(layoutKind: LayoutKind, includeDigitRow: Boolean, c: Char): Boolean {
        val index = rowIndexOf(layoutKind, includeDigitRow, c) ?: return false
        return index > 0
    }
    
    /**
     * D-397: the downward mirror of [hasRowAbove] - true when [c] has a genuine row below it among
     * [layoutKind]'s own character rows. Deliberately does **not** know about the space/control row below
     * the third letter row - that boundary already has its own, tighter, device-confirmed override
     * ([AdaptKeyboardView]'s `BOTTOM_ROW_DOWNWARD_OFFSET_FACTOR`, D-133), checked ahead of this generic rule.
     */
    fun hasRowBelow(layoutKind: LayoutKind, includeDigitRow: Boolean, c: Char): Boolean {
        val rows = rowsFor(layoutKind, includeDigitRow)
        val index = rowIndexOf(layoutKind, includeDigitRow, c) ?: return false
        return index < rows.size - 1
    }
}
