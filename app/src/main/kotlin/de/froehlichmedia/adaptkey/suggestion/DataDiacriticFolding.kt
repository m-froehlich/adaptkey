// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-436: a data-driven [DiacriticFolding] for any language whose diacritics are fully described by a
 * base-letter -> known-variants table ([DiacriticTable.parse]'s own `diacritics.tsv` format) - the
 * generalisation of [Umlaut] beyond German that D-387 asked for, minus [Umlaut]'s own D-204 dual
 * ASCII-convention special case (German's `ß` -> `"ss"`/`"s"`): every diacritic this class knows about has
 * exactly one ASCII substitution, its own base letter - the L-05 AltGr "host key" is always singular by
 * construction, and dropping a diacritic mark already equals that host letter for every target language this
 * mechanism was designed for (French/Spanish/Italian/Portuguese/Polish/Turkish/Dutch). [foldVariants]
 * therefore always returns exactly one entry; a future language that genuinely needs a second, differently-
 * cased ASCII convention (the way German's `ß` does) would need its own dedicated implementation, the same
 * way [Umlaut] is German's.
 *
 * Only the base -> variants direction is ever needed: [unfoldCandidates] tries every known variant at every
 * eligible position (generalising [Umlaut]'s own private recursive `unfold` from a single umlaut per base
 * letter to an arbitrary variant list), and [fold] walks the reverse lookup built once at construction time -
 * neither operation ever needs to go from one variant to a *different* variant of the same base letter.
 *
 * @property baseToVariants a language's own base-letter -> diacritic-variants table (see [DiacriticTable])
 */
data class DataDiacriticFolding(private val baseToVariants: Map<Char, List<Char>>) : DiacriticFolding {
    
    private val variantToBase: Map<Char, Char> = buildMap {
        for ((base, variants) in baseToVariants) {
            for (variant in variants) {
                put(variant, base)
                put(variant.uppercaseChar(), base)
            }
        }
    }
    
    override fun fold(text: String): String {
        val builder = StringBuilder(text.length)
        for (c in text) {
            builder.append(variantToBase[c] ?: c)
        }
        return builder.toString()
    }
    
    override fun unfoldCandidates(text: String): List<String> {
        val results = LinkedHashSet<String>()
        results.add(text)
        unfold(text, 0, StringBuilder(), results)
        return results.toList()
    }
    
    private fun unfold(text: String, index: Int, current: StringBuilder, results: MutableSet<String>) {
        if (results.size >= MAX_CANDIDATES) {
            return
        }
        if (index >= text.length) {
            results.add(current.toString())
            return
        }
        val c = text[index]
        current.append(c)
        unfold(text, index + 1, current, results)
        current.setLength(current.length - 1)
        for (variant in baseToVariants[c].orEmpty()) {
            if (results.size >= MAX_CANDIDATES) {
                return
            }
            current.append(variant)
            unfold(text, index + 1, current, results)
            current.setLength(current.length - 1)
        }
    }
    
    override fun foldVariants(text: String): List<String> = listOf(fold(text))
    
    override fun variantsOf(char: Char): Set<Char> = baseToVariants[char]?.toSet() ?: emptySet()
    
    companion object {
        // Mirrors Umlaut.MAX_CANDIDATES - a shared safety/performance bound, not a linguistic property.
        private const val MAX_CANDIDATES = 32
    }
}
