// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * QWERTZ key map (L-01).
 *
 * Each key carries its secondary long-press symbol as [Key.hint]: the persistent number row (L-06)
 * uses the German-QWERTZ shifted symbols and letters use the AltGr-style map (L-05 / C-08). The hint
 * is always populated so the long-press function works even when the corner glyphs are hidden; only
 * the drawing is gated by the view. Key proportions (L-02 narrower space / wider comma & full stop,
 * L-04 wider backspace) are driven by a [KeyProportions] configuration (C-01) rather than hard-coded
 * weights.
 */
object KeyboardLayout {
    
    // D-106 stage 1: German uses QWERTZ (y/z swapped versus the English QWERTY convention); only the
    // position of these two letters differs, everything else (hints, alternatives, weights) is shared.
    private const val THIRD_ROW_LETTERS_QWERTZ = "yxcvbnm"
    private const val THIRD_ROW_LETTERS_QWERTY = "zxcvbnm"
    private const val TOP_ROW_QWERTZ = "qwertzuiop"
    private const val TOP_ROW_QWERTY = "qwertyuiop"
    
    // L-03: the combined emoji / ?123 key - tap opens the emoji panel, long-press or an upward swipe
    // switches to the numeric/symbol layer (corner-hinted like the other secondary symbols).
    private const val SYMBOL_KEY_LABEL = "😊"
    private const val SYMBOL_KEY_HINT = "123"
    
    // L-06: German-QWERTZ shifted symbol per digit, shown as a corner hint.
    private val NUMBER_HINTS = mapOf(
        '1' to "!", '2' to "\"", '3' to "§", '4' to "$", '5' to "%",
        '6' to "&", '7' to "/", '8' to "(", '9' to ")", '0' to "="
    )
    
    // D-105: every digit's own superscript form, offered as a second D-01 popup alternative alongside
    // its existing shifted symbol above (so a digit's own D-01 popup can offer ² / ³ etc., not only the
    // calculator page's dedicated 2/3 keys, D-92).
    private val NUMBER_SUPERSCRIPTS = mapOf(
        '1' to "¹", '2' to "²", '3' to "³", '4' to "⁴", '5' to "⁵",
        '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹", '0' to "⁰"
    )
    
    /**
     * D-02 / D-22 / D-44 / D-51: the full-stop key's long-press set - the sentence terminators, laid out left
     * to right in the horizontal popup with the full stop centred (`! . ?`). The popup pre-selects the key's
     * own character (the full stop), so a straight-up release still types `.`.
     */
    val PERIOD_ALTERNATIVES = listOf("!", ".", "?")
    
    /**
     * D-22 / D-44: the comma key's long-press set - the remaining clause punctuation, laid out left to
     * right in the D-44 horizontal popup (`- , : ;  _`). The popup pre-selects the key's own character
     * (the comma), so a straight-up release still types `,`. §39: `/` dropped - now reachable directly on
     * the `7` key's long-press hint (D-96), redundant here.
     */
    val COMMA_ALTERNATIVES = listOf("-", ",", ":", ";", "_")
    
    /**
     * D-144 (was D-143's own first pass, corrected per direct feedback): the URL-mode `/` key's long-press
     * set - exactly [COMMA_ALTERNATIVES] unchanged, with **no** `/` entry - a plain tap already commits `/`,
     * so repeating it in its own popup would be a redundant entry offering nothing new. The comma still
     * swaps places with `/` (primary ↔ alternative), just without also duplicating the key's own character.
     */
    val URL_SLASH_ALTERNATIVES = COMMA_ALTERNATIVES
    
    /** D-143: the URL-mode `https://` key's long-press set - the other everyday protocols. */
    val URL_PROTOCOL_ALTERNATIVES = listOf("https://", "http://", "ftp://", "file://")
    
    // D-143: a URL practically never needs a space, so the URL-mode bottom row shrinks it drastically and
    // reinvests the freed width in the two new protocol / www keys instead - an eight-character label
    // ("https://") needs noticeably more room than a plain glyph, "www." somewhat less.
    private const val URL_PROTOCOL_KEY_WEIGHT = 2.5f
    private const val URL_WWW_KEY_WEIGHT = 1.5f
    private const val URL_SPACE_WEIGHT = 1f
    
    /**
     * D-158: the email-mode `@` key's long-press set - exactly [COMMA_ALTERNATIVES] unchanged (comma
     * itself is already in there), mirroring how [URL_SLASH_ALTERNATIVES] demotes comma the same way for
     * `/` without duplicating the key's own new primary character.
     */
    val EMAIL_AT_ALTERNATIVES = COMMA_ALTERNATIVES
    
    // D-158: the new dash key is funded from the space key's own width, unlike D-143's protocol/www keys
    // (which grew the row overall) - the row's total weight matches the ordinary bottom row's exactly
    // (1.4 + 1.8 = 3.2 = the ordinary spaceWeight), just redistributed between the two keys.
    private const val EMAIL_DASH_KEY_WEIGHT = 1.4f
    private const val EMAIL_SPACE_WEIGHT = 1.8f
    
    /** D-90: the π key's own corner hint. */
    private const val PI_HINT = "π"
    
    /** D-99: the π key's long-press popup - the Greek letters, alongside π itself. Also reused by the
     *  calculator page's own π key ([SymbolLayout]), which carries the identical popup (§29 follow-up). */
    val PI_ALTERNATIVES = listOf(PI_HINT, "α", "β", "γ", "δ", "λ", "ω")
    
    /** The o key's own corner hint (the umlaut). */
    private const val O_HINT = "ö"
    
    /** §29 follow-up: the o key's long-press popup - ö plus Ø, the German shorthand for "Durchschnitt"
     *  (average) - chosen as the most intuitive host since Ø is visually a stylised O. */
    private val O_ALTERNATIVES = listOf(O_HINT, "Ø")
    
    /**
     * L-05 / C-08: default AltGr-style secondary symbols on selected letters. Exposed so the settings
     * layer can offer it as the reset baseline for the configurable per-key map.
     */
    val DEFAULT_LETTER_HINTS = mapOf(
        'q' to "@", 'e' to "€", 'h' to "#", 'm' to "-", 'n' to "+", 'd' to "°",
        // D-90: the math-symbol hint on p, alongside the original AltGr-style set above.
        'p' to PI_HINT,
        // D-96: reorganised math-symbol hints - × moved to x, ÷ moved to c, v gets /, b gets *.
        'x' to "×", 'c' to "÷", 'v' to "/", 'b' to "*",
        // §29 follow-up: f was still free - the function symbol goes here.
        'f' to "ƒ",
        'a' to "ä", 'o' to O_HINT, 'u' to "ü", 's' to "ß"
    )
    
    /**
     * Builds the keyboard for the given configuration.
     *
     * @param proportions the key-proportion configuration (C-01); defaults to [KeyProportions.DEFAULT]
     * @param showNumberRow whether the persistent number row is included (L-06 / C-09); defaults to true
     * @param letterHints the per-letter secondary-symbol map (L-05 / C-08); defaults to [DEFAULT_LETTER_HINTS]
     * @param qwerty D-106 stage 1: true for the English active language (QWERTY, y/z swapped versus
     *        German); false for German (QWERTZ). Only the two letters' screen position differs - hints,
     *        alternatives and every other key are shared between both variants.
     * @param urlMode D-143: true for a recognised URL-entry field - replaces the bottom row's comma/space/
     *        period trio with [urlBottomRow]; defaults to false (the ordinary bottom row).
     * @param emailMode D-158: true for a recognised email-address field - replaces the bottom row's
     *        comma/space/period trio with [emailBottomRow]; defaults to false. Ignored when [urlMode] is
     *        also true (cannot both apply to the same field in practice).
     * @param locale D-143 / D-158: the system locale [urlBottomRow]'s/[emailBottomRow]'s period key
     *        resolves its TLD popup from ([UrlLocale]); only meaningful when [urlMode] or [emailMode] is
     *        true.
     * @return the keyboard as a list of rows, each a list of [Key] from left to right
     */
    fun rows(
        proportions: KeyProportions = KeyProportions.DEFAULT,
        showNumberRow: Boolean = true,
        letterHints: Map<Char, String> = DEFAULT_LETTER_HINTS,
        qwerty: Boolean = false,
        urlMode: Boolean = false,
        emailMode: Boolean = false,
        locale: Locale = Locale.getDefault()
    ): List<List<Key>> {
        val result = ArrayList<List<Key>>()
        
        if (showNumberRow) {
            // L-06: persistent number row with shifted-symbol hints.
            result.add("1234567890".map { c -> numberKey(c) })
        }
        
        val topRowLetters = if (qwerty) TOP_ROW_QWERTY else TOP_ROW_QWERTZ
        val thirdRowLetters = if (qwerty) THIRD_ROW_LETTERS_QWERTY else THIRD_ROW_LETTERS_QWERTZ
        result.add(topRowLetters.map { c -> topRowKey(c, letterHints) })
        result.add("asdfghjkl".map { c -> charKey(c, letterHints[c]) })
        
        // L-04: the backspace surcharge is taken evenly from the third-row letters.
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(thirdRowLetters.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            thirdRowLetters.forEach { c -> add(charKey(c, letterHints[c], weight = thirdRowLetterWeight)) }
            add(Key(label = "⌫", code = KeyCode.DELETE, weight = proportions.backspaceWeight))
        })
        
        result.add(
            if (urlMode) {
                urlBottomRow(proportions, locale)
            } else if (emailMode) {
                emailBottomRow(proportions, locale)
            } else {
                buildList {
                    // L-03: combined emoji / numeric-layer key - tap opens the emoji panel, long-press /
                    // swipe-up switches to ?123.
                    add(Key(label = SYMBOL_KEY_LABEL, code = KeyCode.SYMBOL, hint = SYMBOL_KEY_HINT, weight = proportions.symbolWeight))
                    // L-02: comma & full stop widened, space narrowed.
                    // D-22: the comma key carries the clause punctuation (; : - _ /) as its long-press popup.
                    add(charKey(',', alternatives = COMMA_ALTERNATIVES, weight = proportions.commaWeight))
                    add(Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight))
                    // D-02 / D-22: the full-stop key carries the sentence terminators (. ! ?).
                    add(charKey('.', alternatives = PERIOD_ALTERNATIVES, weight = proportions.periodWeight))
                    add(Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight))
                }
            }
        )
        
        return result
    }
    
    /**
     * D-143: the URL-mode bottom row, shared by [KeyboardLayout] and [GreekLayout] (URL entry doesn't
     * depend on the active typing alphabet - both show identical URL keys). Replaces the ordinary
     * comma/space/period trio: `/` takes over the comma key's own primary position (its own alt popup
     * unchanged, just demoted to an alternative - see [URL_SLASH_ALTERNATIVES]); a new `https://` key
     * (§53 [KeyCode.TEXT], its alt popup the other everyday protocols) and a `www.` key are funded by
     * shrinking the now barely-needed space key; the full-stop key's alt popup is a locale-resolved TLD
     * list instead of the ordinary sentence terminators (a URL practically never needs `!`/`?`, see
     * [UrlLocale]).
     *
     * @param proportions the key-proportion configuration (C-01)
     * @param locale the system locale the period key's TLD popup is resolved from ([UrlLocale])
     * @return the row as a list of [Key] from left to right
     */
    fun urlBottomRow(proportions: KeyProportions, locale: Locale): List<Key> {
        return listOf(
            Key(label = SYMBOL_KEY_LABEL, code = KeyCode.SYMBOL, hint = SYMBOL_KEY_HINT, weight = proportions.symbolWeight),
            textKey("https://", alternatives = URL_PROTOCOL_ALTERNATIVES, weight = URL_PROTOCOL_KEY_WEIGHT),
            textKey("www.", weight = URL_WWW_KEY_WEIGHT),
            charKey('/', alternatives = URL_SLASH_ALTERNATIVES, weight = proportions.commaWeight),
            Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = URL_SPACE_WEIGHT),
            charKey('.', alternatives = UrlLocale.periodAlternatives(locale), weight = proportions.periodWeight),
            Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight)
        )
    }
    
    /**
     * D-158: the email-mode bottom row, shared by [KeyboardLayout] and [GreekLayout] (email entry doesn't
     * depend on the active typing alphabet, mirroring [urlBottomRow]'s own reasoning). Replaces the
     * ordinary comma/space/period trio: `@` takes over the comma key's own primary position (its own alt
     * popup unchanged, just demoted to an alternative - see [EMAIL_AT_ALTERNATIVES]); a new dash key (`-`,
     * `_` as its single D-01 long-press secondary) is funded by narrowing the now less-needed space key;
     * the full-stop key's alt popup is the same locale-resolved TLD list [urlBottomRow] uses, since an
     * email address ends in a domain/TLD exactly like a URL does.
     *
     * @param proportions the key-proportion configuration (C-01)
     * @param locale the system locale the period key's TLD popup is resolved from ([UrlLocale])
     * @return the row as a list of [Key] from left to right
     */
    fun emailBottomRow(proportions: KeyProportions, locale: Locale): List<Key> {
        return listOf(
            Key(label = SYMBOL_KEY_LABEL, code = KeyCode.SYMBOL, hint = SYMBOL_KEY_HINT, weight = proportions.symbolWeight),
            charKey('@', alternatives = EMAIL_AT_ALTERNATIVES, weight = proportions.commaWeight),
            charKey('-', hint = "_", weight = EMAIL_DASH_KEY_WEIGHT),
            Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = EMAIL_SPACE_WEIGHT),
            charKey('.', alternatives = UrlLocale.periodAlternatives(locale), weight = proportions.periodWeight),
            Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight)
        )
    }
    
    /**
     * The symbol a long-press on [key] should emit (L-05 / L-06): a character key's secondary
     * [Key.hint], or null when the key has no secondary or is a control key.
     *
     * @param key the pressed key
     * @return the secondary symbol to commit on long-press, or null when there is none
     */
    fun longPressSymbol(key: Key): String? {
        return if (key.code == KeyCode.CHAR) key.hint else null
    }
    
    /**
     * Whether holding [key] past the long-press timeout has an action at all: a character or §53 [TEXT]
     * key's single secondary symbol (L-05 / L-06, see [longPressSymbol]), its multi-alternative popup
     * (D-01, two or more [Key.alternatives]), the combined emoji / ?123 key switching to the numeric/symbol
     * layer (L-03), or §31's calculator minus key, which flips the sign of the number before the caret
     * instead of committing text - it carries neither a hint nor alternatives, so it needs its own case here.
     *
     * @param key the pressed key
     * @return true when a long-press on this key should be scheduled
     */
    fun hasLongPressAction(key: Key): Boolean {
        return ((key.code == KeyCode.CHAR || key.code == KeyCode.TEXT) && (key.hint != null || key.alternatives.size >= 2)) ||
            key.code == KeyCode.SYMBOL ||
            (key.code == KeyCode.CHAR && key.char == SymbolLayout.MINUS_SIGN)
    }
    
    /**
     * D-99 / §29 follow-up: builds a top-row key, giving `p` its Greek-letter popup ([PI_ALTERNATIVES])
     * and `o` its Ø popup ([O_ALTERNATIVES]) - but only while each still carries its own default hint, so
     * a user who has reassigned `p` or `o` via the C-08 editor keeps their own single-symbol long-press
     * instead of an unrelated popup.
     *
     * §34: `p` is the last key of the top row, hard against the keyboard's right edge - the popup has no
     * room to grow rightward from its stem, so [AdaptKeyboardView] clamps the whole row leftward instead,
     * which (since the row is always drawn in list order, left to right) puts [PI_HINT] itself at the far
     * end, away from the finger, and the least-relevant alternative right next to it. [PI_ALTERNATIVES] is
     * shared with the calculator page's `π` key ([SymbolLayout]), which sits mid-row there and does not
     * have this problem, so only this call site reverses the order rather than the shared constant itself.
     */
    private fun topRowKey(c: Char, letterHints: Map<Char, String>): Key {
        val hint = letterHints[c]
        return when {
            c == 'p' && hint == PI_HINT -> charKey(c, hint, alternatives = PI_ALTERNATIVES.reversed())
            c == 'o' && hint == O_HINT -> charKey(c, hint, alternatives = O_ALTERNATIVES)
            else -> charKey(c, hint)
        }
    }
    
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
    
    /** §53 / D-143: a key that commits its own multi-character [label] verbatim on a plain tap ([KeyCode.TEXT]). */
    private fun textKey(label: String, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = label, code = KeyCode.TEXT, hint = hint, alternatives = alternatives, weight = weight)
    }
    
    /**
     * D-105: builds a number-row key with its existing shifted symbol (index 0, pre-selected - unchanged
     * from before) and its own superscript form (index 1) as a real D-01 two-cell popup, instead of the
     * shifted symbol applying immediately on long-press. `0` sits at the row's right edge - the same
     * problem §34 already fixed for the letters page's `p` key - so its popup has no room to grow
     * rightward and gets clamped leftward instead; reversing its list keeps its own glyph nearest the
     * finger, matching that precedent (only this one call site, not the shared constants).
     */
    private fun numberKey(c: Char): Key {
        val hint = NUMBER_HINTS[c]
        val superscript = NUMBER_SUPERSCRIPTS[c]
        val alternatives = if (hint != null && superscript != null) listOf(hint, superscript) else emptyList()
        return charKey(c, hint, alternatives = if (c == '0') alternatives.reversed() else alternatives)
    }
}
