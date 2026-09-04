// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

import de.froehlichmedia.adaptkey.keyboard.LayoutKind

/**
 * Resolves each [LayoutKind]'s own [KeyboardProximity] - mirrors
 * [de.froehlichmedia.adaptkey.language.LanguageRulesRegistry]/
 * [de.froehlichmedia.adaptkey.language.DiacriticFoldingRegistry]'s identical `Map<key, implementation>`
 * shape. Keyed by [LayoutKind] rather than
 * [de.froehlichmedia.adaptkey.language.Language] directly, since physical key adjacency is a property of
 * the row geometry itself, not the language - several languages already share one geometry (every
 * ordinary Latin language shares [LayoutKind.LATIN_QWERTY] via
 * [de.froehlichmedia.adaptkey.keyboard.LayoutRegistry]'s own default), and this registry should not need
 * a new entry merely because a new language adopts an already-covered layout.
 */
object KeyboardProximityRegistry {
    
    private val PROXIMITY: Map<LayoutKind, KeyboardProximity> = mapOf(
        LayoutKind.LATIN_QWERTZ to KeyboardProximityQwertz,
        LayoutKind.LATIN_QWERTY to KeyboardProximityQwerty,
        LayoutKind.LATIN_AZERTY to KeyboardProximityAzerty,
        LayoutKind.GREEK to KeyboardProximityGreek
    )
    
    /**
     * @param layoutKind a row geometry
     * @return its own [KeyboardProximity] - every [LayoutKind] has a real implementation today, so this
     *         never falls back to a default the way [de.froehlichmedia.adaptkey.language.
     *         LanguageRulesRegistry.rulesFor] does for an unimplemented language; the `?:` below only
     *         guards a future [LayoutKind] added without updating this map in the same change.
     */
    fun forLayoutKind(layoutKind: LayoutKind): KeyboardProximity = PROXIMITY[layoutKind] ?: KeyboardProximityQwerty
}
