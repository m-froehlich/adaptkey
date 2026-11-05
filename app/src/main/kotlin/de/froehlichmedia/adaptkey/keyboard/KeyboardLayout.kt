// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.keyboard

import java.util.Locale

/**
 * QWERTZ key map (L-01).
 * 
 * Each key carries its secondary long-press symbols as corner glyphs (L-05 / L-06) - the D-509 corner
 * model: [Key.hint] is the right corner (the release default, and today still the only one every key
 * carries), [Key.leftHint] the left one. The persistent number row (L-06) uses the German-QWERTZ shifted
 * symbols and letters use the AltGr-style map (L-05 / C-08). The corner glyphs are always populated so
 * the long-press function works even when the corner glyphs are hidden; only the drawing is gated by the
 * view. Key proportions (L-02 narrower space / wider comma & full stop, L-04 wider backspace) are driven
 * by a [KeyProportions] configuration (C-01) rather than hard-coded weights.
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
    
    // D-511 (Alt-Belegungs-Reform plan §2.3/§2.4): the reformed number row - every digit carries a RIGHT
    // corner (the US shifted row, each also its digit's release default - "US holy cows stay put" as right
    // corners) and, where the user's assignment table gives one, a LEFT corner: the German-QWERTZ survivors
    // `~ " § =` plus the math singles `+ - /` (§3b's slide-left set). `7` additionally carries the division
    // sign as the row's one MIDDLE glyph (§2.10's accepted ×÷ split: × stays on x, ÷ lives here).
    private val NUMBER_RIGHT_HINTS = mapOf(
        '1' to "!", '2' to "@", '3' to "#", '4' to "$", '5' to "%",
        '6' to "^", '7' to "&", '8' to "*", '9' to "(", '0' to ")"
    )
    
    private val NUMBER_LEFT_HINTS = mapOf(
        '1' to "~", '2' to "\"", '3' to "§", '5' to "+", '6' to "-", '7' to "/", '0' to "="
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
     * D-22 / D-44: the comma key's long-press set - the clause punctuation plus `/`, laid out left to
     * right in the D-44 horizontal popup (`/ - , : ;  _`). The popup pre-selects the key's own character
     * (the comma) by value, so a straight-up release still types `,`.
     * 
     * D-499: `/` is back, leftmost - re-introducing the redundancy §39 deliberately removed when the
     * slash moved to the `7` key's hint (D-96), per the user's explicit request. Deliberate shared state:
     * [URL_SLASH_ALTERNATIVES] keeps aliasing this list (see its own KDoc for why the `/` cell it gains
     * is harmless there); [EMAIL_AT_ALTERNATIVES] was decoupled in the same round.
     */
    val COMMA_ALTERNATIVES = listOf("/", "-", ",", ":", ";", "_")
    
    /**
     * D-144 (was D-143's own first pass, corrected per direct feedback): the URL-mode `/` key's long-press
     * set - deliberately still exactly [COMMA_ALTERNATIVES], unchanged by D-499's `/` re-addition.
     * 
     * D-144's original "no `/` entry" rule is consciously dropped (user decision, 2026-10-26): the URL
     * popup keeps aliasing the comma popup, so it now shows `/` as its first cell. The redundancy that
     * rule guarded against is harmless here because of D-44's own value-based pre-selection - the key's
     * own character `/` is in the list, so a straight-up release still types `/` (the pre-selected cell
     * IS `/`), and the duplicate cell merely shifts the real alternatives one position right. The comma
     * still swaps places with the key's own primary the same way it always did.
     */
    val URL_SLASH_ALTERNATIVES = COMMA_ALTERNATIVES
    
    /**
     * D-222 (was D-143's own separate `https://` key's long-press set before the two keys merged): the
     * URL-mode `www.` key's long-press set - the everyday protocols, `https://` first so it lands as the
     * pre-selected cell (see [AdaptKeyboardView.preSelectedIndexFor]'s own "falls back to index 0" rule -
     * `www.` never appears in this list, so it always takes that fallback).
     */
    val URL_PROTOCOL_ALTERNATIVES = listOf("https://", "http://", "ftp://", "file://")
    
    // D-222 (was D-143's separate protocol + www keys, each its own key): the user rarely needs to type a
    // protocol explicitly on a phone (almost every field defaults to https anyway), so the two keys merged
    // into one - primary label "www.", the protocol only ever reached via its own long-press popup above -
    // freeing the width for a full-size space key again instead of D-143's own drastic shrink, addressing
    // the actually far more common case (typing a search query, not a URL) directly. Mirrors D-158's own
    // "conserve the row's total weight, don't grow it" pattern: the merged key plus space add up to exactly
    // what the old protocol + www + space keys did (2.5 + 1.5 + 1 = 5 = 1.8 + proportions.spaceWeight).
    private const val URL_WWW_KEY_WEIGHT = 1.8f
    
    /**
     * D-158: the email-mode `@` key's long-press set - decoupled from [COMMA_ALTERNATIVES] by D-499.
     * 
     * User rationale (2026-10-26): `,` `:` `;` are not merely uncommon in an email address, they are
     * outright nonsense there, so sharing the comma popup's punctuation set was wrong on its own merits.
     * What remains is the set that genuinely occurs in real addresses: `-` and `_` in the local part, plus
     * `+` for plus-addressing (`user+tag@provider.com`). As with the D-144 aliasing above, the key's own
     * `@` never appears in the list, so D-44's fallback pre-selects index 0 (`-`) - a straight long-press
     * release commits `-`, exactly as before this round.
     */
    val EMAIL_AT_ALTERNATIVES = listOf("-", "_", "+")
    
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
    
    /** D-336: the a key's own corner hint (the umlaut). */
    private const val A_HINT = "ä"
    
    /** D-336/D-341: the a key's long-press popup - ä plus the Nordic æ/å ligatures and the French à. */
    private val A_ALTERNATIVES = listOf(A_HINT, "æ", "å", "à")
    
    /** D-336: the e key's own corner hint (the currency symbol). */
    private const val E_HINT = "€"
    
    /** D-336: the e key's long-press popup - € plus the four common French diacritic variants. */
    private val E_ALTERNATIVES = listOf(E_HINT, "é", "è", "ê", "ë")
    
    /**
     * D-497: separates one letter's long-press variants inside a pack hints value (`e=ę,ė` builds the
     * `e` key a two-cell popup). Kept in sync with LetterHints.VARIANT_SEPARATOR, the data layer's
     * normative definition - a local constant because the dependency must not run keyboard -> settings
     * (it runs the other way: the settings layer already builds on this package).
     */
    const val HINT_VARIANT_SEPARATOR = ','
    
    /**
     * L-05 / C-08: default AltGr-style secondary symbols on selected letters - since D-512 these are the
     * reform's RIGHT corners (the release defaults) of the German base map. Exposed so the settings layer
     * can offer it as the reset baseline and as the fallback whenever a language has no hint set of its
     * own (D-281). The reform's LEFT corners live separately in [BASE_LEFT_CORNERS], and `c`/`v`/`b`/`m`
     * no longer carry a right corner at all: `c`/`v`/`b` are freed entirely (their `÷` `/` `*` live on the
     * reformed digit row, §2.5) and `m`'s `)` is its left corner.
     */
    val DEFAULT_LETTER_HINTS = mapOf(
        'q' to "@", 'e' to E_HINT, 'h' to "#",
        // D-512 §3: the d key's release default moves from ° to $ (° becomes its left corner).
        'd' to "$",
        // D-90: the math-symbol hint on p, alongside the original AltGr-style set above.
        'p' to PI_HINT,
        // D-96: × stays on x (§2.10's accepted ×÷ split - × here, ÷ on the digit row's 7-middle).
        'x' to "×",
        // §29 follow-up: the function-symbol hint on f.
        'f' to "ƒ",
        'a' to A_HINT, 'o' to O_HINT, 'u' to "ü", 's' to "ß",
        // D-512 §2.6: ñ is the universal right corner of n on every Latin language (its ( is the left one).
        'n' to "ñ"
    )
    
    /**
     * D-512: the reform's LEFT corners of the Latin letters page - fixed per key (§3): the d key's °
     * (its release default moved to $), the n key's ( beside the universal ñ, the m key's ) (the one
     * corner it has - a lone left corner is the release default, so m commits `)`), and the s key's *
     * (the one operator rescued from C-09's hidden-number-row loss). Scoped to the two Latin geometries
     * ([rows] and [AzertyLayout], which pass it explicitly): the non-Latin layouts receive only the
     * reformed digit row (§2.12), not the letters-page corners. The `+` slot is geometry-dependent
     * (the first third-row key: `y` QWERTZ, `z` QWERTY, `w` AZERTY) and is applied by [rows]/
     * [AzertyLayout] via [letterKey]'s own [plusSlot] parameter, not by this per-letter map.
     */
    val BASE_LEFT_CORNERS = mapOf('d' to "°", 'n' to "(", 'm' to ")", 's' to "*")
    
    /** D-512 §2.8: the `+` carried by the first third-row key's slot (see [BASE_LEFT_CORNERS]'s KDoc). */
    private const val PLUS_SLOT_LEFT_CORNER = "+"
    
    /**
     * D-512: the letters page's popup couplings as explicit per-key data (replacing the five hardcoded
     * when-equals branches) - each entry arms on the letter still carrying its DEFAULT right-corner hint,
     * so a pack that reassigns the key keeps its own single-symbol/variant long-press (the D-99 invariant,
     * D-301's no-override lesson). The values are the popup EXTRAS only (the arming hint itself is always
     * part of the assembled list: first on a centred key, last on a dual-corner one).
     */
    private val BASE_HINT_EXTRAS = mapOf(
        'p' to (PI_HINT to PI_ALTERNATIVES.drop(1)),
        'o' to (O_HINT to O_ALTERNATIVES.drop(1)),
        'a' to (A_HINT to A_ALTERNATIVES.drop(1)),
        'e' to (E_HINT to E_ALTERNATIVES.drop(1))
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
        result.add(topRowLetters.map { c -> letterKey(c, letterHints, leftCorners = BASE_LEFT_CORNERS) })
        // D-512 §2.2: the dedicated apostrophe key right of `l` - corners `" | '`, release `'`.
        result.add("asdfghjkl".map { c -> letterKey(c, letterHints, leftCorners = BASE_LEFT_CORNERS) } + apostropheKey())
        
        // L-04: the backspace surcharge is taken evenly from the third-row letters.
        val thirdRowLetterWeight = proportions.thirdRowLetterWeight(thirdRowLetters.length)
        result.add(buildList {
            add(Key(label = "⇧", code = KeyCode.SHIFT, weight = proportions.shiftWeight))
            thirdRowLetters.forEachIndexed { index, c ->
                // D-512 §2.8: the `+` sits as the LEFT corner of the geometry's first third-row key
                // (`y` QWERTZ, `z` QWERTY) - screen-stable as a slot, never displacing a language letter.
                add(letterKey(c, letterHints, leftCorners = BASE_LEFT_CORNERS, weight = thirdRowLetterWeight, plusSlot = index == 0))
            }
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
                    // D-22/D-44/D-499: the comma key carries `/ - , : ; _` as its long-press popup.
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
     * D-143/D-222: the URL-mode bottom row, shared by [KeyboardLayout] and [GreekLayout] (URL entry doesn't
     * depend on the active typing alphabet - both show identical URL keys). Replaces the ordinary
     * comma/space/period trio: `/` takes over the comma key's own primary position (its own alt popup
     * unchanged, just demoted to an alternative - see [URL_SLASH_ALTERNATIVES]); a new `www.` key (§53
     * [KeyCode.TEXT], its alt popup the everyday protocols, D-222) replaces D-143's own separate `https://`
     * key - typing the protocol explicitly is rare enough on a phone that it does not need a key of its own,
     * so the space key keeps its ordinary, full width instead of D-143's own drastic shrink; the full-stop
     * key's alt popup is a locale-resolved TLD list instead of the ordinary sentence terminators (a URL
     * practically never needs `!`/`?`, see [UrlLocale]).
     * 
     * @param proportions the key-proportion configuration (C-01)
     * @param locale the system locale the period key's TLD popup is resolved from ([UrlLocale])
     * @return the row as a list of [Key] from left to right
     */
    fun urlBottomRow(proportions: KeyProportions, locale: Locale): List<Key> {
        return listOf(
            Key(label = SYMBOL_KEY_LABEL, code = KeyCode.SYMBOL, hint = SYMBOL_KEY_HINT, weight = proportions.symbolWeight),
            textKey("www.", alternatives = URL_PROTOCOL_ALTERNATIVES, weight = URL_WWW_KEY_WEIGHT),
            charKey('/', alternatives = URL_SLASH_ALTERNATIVES, weight = proportions.commaWeight),
            Key(label = "space", code = KeyCode.SPACE, char = ' ', weight = proportions.spaceWeight),
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
     * the full-stop key's alt popup is the locale-resolved TLD list [urlBottomRow] uses, plus an
     * email-only `.net` entry ([UrlLocale.emailPeriodAlternatives] - explicitly not shared with URL mode).
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
            charKey('.', alternatives = UrlLocale.emailPeriodAlternatives(locale), weight = proportions.periodWeight),
            Key(label = "↵", code = KeyCode.ENTER, weight = proportions.enterWeight)
        )
    }
    
    /**
     * The symbol a long-press on [key] should emit (L-05 / L-06): a character key's default corner glyph
     * (D-509 corner model - the right corner, falling back to the left one where no right corner exists),
     * or null when the key has no secondary or is a control key.
     * 
     * @param key the pressed key
     * @return the secondary symbol to commit on long-press, or null when there is none
     */
    fun longPressSymbol(key: Key): String? {
        return if (key.code == KeyCode.CHAR) key.defaultCornerHint else null
    }
    
    /**
     * Whether holding [key] past the long-press timeout has an action at all: a character or §53 [TEXT]
     * key's single secondary symbol (L-05 / L-06, see [longPressSymbol]), its multi-alternative popup
     * (D-01, two or more [Key.alternatives]), the combined emoji / ?123 key switching to the numeric/symbol
     * layer (L-03), §31's calculator minus key, which flips the sign of the number before the caret
     * instead of committing text - it carries neither a hint nor alternatives, so it needs its own case here -
     * or the Shift key, whose long-press engages Caps Lock (G-06).
     * 
     * @param key the pressed key
     * @return true when a long-press on this key should be scheduled
     */
    fun hasLongPressAction(key: Key): Boolean {
        return ((key.code == KeyCode.CHAR || key.code == KeyCode.TEXT) && (key.defaultCornerHint != null || key.alternatives.size >= 2)) ||
            key.code == KeyCode.SYMBOL ||
            key.code == KeyCode.SHIFT ||
            (key.code == KeyCode.CHAR && key.char == SymbolLayout.MINUS_SIGN)
    }
    
    /**
     * D-99 / §29 follow-up / D-336 / D-512: builds a letter key from the reform's explicit data - the
     * key's RIGHT corner ([letterHints]'s entry, or the first D-497 variant), its LEFT corner
     * ([leftCorners], the reform's per-key map; overridden by [plusSlot] for the `+` slot), and its
     * popup alternatives assembled from that data ([BASE_HINT_EXTRAS] arming on the default hint, pack
     * variants appending after the left corner, the corners themselves forming a dual-corner key's own
     * straddle popup). A letter with no hint and no left corner carries no popup at all - the freed
     * `c`/`v`/`b` (§2.5).
     * 
     * D-497: a hint value carrying [HINT_VARIANT_SEPARATOR]-separated variants (`e=ę,ė` from a
     * language pack's own `hints.tsv`) is a multi-cell popup the same way the data-driven cases above
     * work: first variant = right corner = the popup's first cell on a centred key (which D-44's
     * fallback then pre-selects - the base letter is deliberately not among the variants). On a
     * dual-corner key the variants become the popup's extras between the two corners (D-513's data
     * shape for the s-owning languages). A single-variant value keeps the direct-commit hint behaviour.
     * 
     * D-512: the former five hardcoded hint-equality couplings (`p`=π→Greek letters, `o`=ö→Ø, `a`=ä→æ å à,
     * `e`=€→é è ê ë, `n`=+→ñ) are [BASE_HINT_EXTRAS] now - armed by the same default-hint equality, so a
     * pack that reassigns a key keeps its own long-press (the D-99 invariant); `n`'s old coupling is gone
     * outright (ñ is its right corner now, its `+` moved to the digit row).
     * 
     * D-282/D-510: `p` sits at the row's right edge, where a wide popup runs out of room to grow
     * rightward - handled dynamically by [AdaptKeyboardView.openPopup]'s drift-gated clamp;
     * [PI_ALTERNATIVES] is always passed in its natural, un-reversed order (`π` first).
     * 
     * D-314 / D-336: not private - [AzertyLayout] reuses this directly for its own letter rows, rather
     * than duplicating the special-casing; both Latin geometries pass [BASE_LEFT_CORNERS], while the
     * non-Latin layouts' letter pages stay pre-reform (§2.12). Generalised in D-336 from the top-row-only
     * `topRowKey` into a row-agnostic `letterKey` - the popup set never depended on row position, only on
     * the letter and its hint.
     * 
     * @param plusSlot D-512 §2.8: true for the geometry's first third-row key (`y` QWERTZ, `z` QWERTY,
     *        `w` AZERTY) - that key carries `+` as its left corner, screen-stable as a slot across
     *        geometries, never displacing a language letter (which stays the right corner per the
     *        standing rule).
     */
    fun letterKey(
        c: Char,
        letterHints: Map<Char, String>,
        leftCorners: Map<Char, String> = emptyMap(),
        weight: Float = 1f,
        plusSlot: Boolean = false
    ): Key {
        val rawValue = letterHints[c]
        val hint = rawValue?.substringBefore(HINT_VARIANT_SEPARATOR)
        val leftCorner = if (plusSlot) PLUS_SLOT_LEFT_CORNER else leftCorners[c]
        val variants = rawValue?.split(HINT_VARIANT_SEPARATOR)?.takeIf { it.size >= 2 }
        val extras = variants?.drop(1)
            ?: BASE_HINT_EXTRAS[c]?.takeIf { it.first == hint }?.second
        // The pre-reform data contract (Key's own KDoc): a size of two or more triggers the popup,
        // otherwise the single default corner glyph applies immediately on long-press - so a lone hint
        // (or a lone left corner, which the default-corner rule covers) stays OUT of the alternatives
        // list and only a genuine multi-cell popup carries explicit data.
        val alternatives = when {
            hint == null || (leftCorner == null && extras == null) -> emptyList()
            leftCorner != null -> buildList {
                add(leftCorner)
                // D-513 follow-up: a pre-reform installed pack's stale corner duplicate (delta d=° under
                // the new $ base, where the merged hint equals the code-side left corner) must not
                // produce the same glyph twice in one row - extras never repeat a corner, and a hint
                // equal to the left corner collapses into it.
                extras?.filter { it != leftCorner }?.let { addAll(it) }
                if (hint != leftCorner) {
                    add(hint)
                }
            }
            else -> buildList {
                add(hint!!)
                addAll(extras!!)
            }
        }
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, leftHint = leftCorner, alternatives = alternatives, weight = weight)
    }
    
    /**
     * D-512 §2.2: the dedicated apostrophe key right of `l` (QWERTY/QWERTZ) / right of `m` (AZERTY) -
     * corners `" | '`, the right corner the release default so a straight long-press release commits `'`
     * and the double quote is a slide-left away. Its popup is exactly the corner pair (the straddle
     * data [leftCorner, extras…, rightCorner] with no extras). Not private: [AzertyLayout] shares it.
     */
    fun apostropheKey(): Key = Key(
        label = "'",
        code = KeyCode.CHAR,
        char = '\'',
        hint = "'",
        leftHint = "\"",
        alternatives = listOf("\"", "'")
    )
    
    /**
     * D-314: the top-row entry point - a weight-defaulted delegate to [letterKey], kept so [AzertyLayout]'s
     * own top-row call sites read naturally and stay unchanged.
     */
    fun topRowKey(c: Char, letterHints: Map<Char, String>, leftCorners: Map<Char, String> = emptyMap()): Key =
        letterKey(c, letterHints, leftCorners)
    
    
    private fun charKey(c: Char, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = hint, alternatives = alternatives, weight = weight)
    }
    
    /** §53 / D-143: a key that commits its own multi-character [label] verbatim on a plain tap ([KeyCode.TEXT]). */
    private fun textKey(label: String, hint: String? = null, alternatives: List<String> = emptyList(), weight: Float = 1f): Key {
        return Key(label = label, code = KeyCode.TEXT, hint = hint, alternatives = alternatives, weight = weight)
    }
    
    /**
     * D-511: builds a number-row key per the reform's assignment table (plan §2.3) - a RIGHT corner (the
     * US row, the release default) plus, where assigned, a LEFT corner, with the popup as explicit data
     * in the `[leftCorner, extras…, rightCorner]` straddle order (plan §6): the left corner first, the
     * digit's superscript and specials in the middle, the right corner (pre-selected) last.
     *
     * Right-only digits (`4`/`8`/`9`) keep exactly the pre-reform popup shape `[corner, superscript]`
     * (D-105), with the right corner pre-selected by the ordinary index-0 fallback.
     *
     * D-382/D-417 kept: `2`'s apostrophe sits ahead of the superscript, its subscript `₂` last
     * (`" ' ² ₂ @`). D-498 kept: `7` still carries the backslash, now with the middle `÷` inserted
     * ahead of it (`/ ÷ \ ⁷ &`). D-282's popup-time handling was replaced by D-510's drift-gated clamp;
     * the data here is always in its natural order.
     *
     * D-314/D-498: not private - every dedicated layout's number row delegates here (their local copies
     * already documented themselves as "identical treatment" to this one), so the six number rows stay
     * byte-identical by construction instead of by six synchronized edits. §2.12: the non-Latin layouts
     * (Greek, Jcuken, Serbian, Azerbaijani) receive the reformed row too - no pinning.
     */
    fun numberKey(c: Char): Key {
        val right = requireNotNull(NUMBER_RIGHT_HINTS[c]) { "no right corner assigned for digit $c" }
        val left = NUMBER_LEFT_HINTS[c]
        val superscript = requireNotNull(NUMBER_SUPERSCRIPTS[c]) { "no superscript assigned for digit $c" }
        val extras = when (c) {
            '2' -> listOf("'", superscript, "₂")
            '7' -> listOf("÷", "\\", superscript)
            else -> listOf(superscript)
        }
        val alternatives = if (left != null) listOf(left) + extras + right else listOf(right) + extras
        return Key(label = c.toString(), code = KeyCode.CHAR, char = c, hint = right, leftHint = left, alternatives = alternatives)
    }
}
