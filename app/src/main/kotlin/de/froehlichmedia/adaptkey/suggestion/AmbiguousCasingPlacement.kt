// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.suggestion

/**
 * D-546: the typing-time placement for the S-11 dual-casing chips - in place at the ambiguous
 * candidate's own ranked slot, replacing D-404-followup's append-at-the-back shape for the typing
 * path. Pure bar-shaping logic over [SuggestionController.DisplayItem], deliberately free of any
 * Android dependency so the placement contract is unit-tested on the JVM.
 *
 * Why the shape changed: the back-append was designed when an ambiguous word was an ordinary ranked
 * entry one could afford to demote ("a better ordinary suggestion may crowd them out"). D-463 already
 * recognised the flaw for next-word predictions - the bar's best entry must not be demoted to last
 * place behind its own two casings - and replaced that path's append with in-place expansion. The
 * reported D-546 case ("jetzt" learned as "Jetzt", so the merged entry carries a nominal tag and
 * becomes §6-rule-5-ambiguous) showed the typing path has the identical defect with sharper teeth:
 * the word the user is typing is very often the bar's single best candidate (its own prefix family
 * ranks it first), the two casing chips land behind every fuzzy neighbour, and a full bar cuts them
 * entirely - the word vanishes from its own suggestions. The chips therefore take their candidate's
 * slot; a candidate that is not displayed (an exact-match alternate whose word the controller's own
 * S-02 filter removed case-sensitively, the typed spelling itself) has no slot to take and keeps the
 * historical back-append shape.
 *
 * The chips themselves are computed elsewhere ([de.froehlichmedia.adaptkey.AdaptKeyService]'s
 * ambiguous-casing scan) with every confirmed semantic intact: prefix match offers both casings,
 * lower first; an exact match offers only the other casing; a tap commits verbatim. D-548: pinned
 * continuations participate fully - their ambiguous candidate's pair takes their own (leading) slot,
 * superseding v1.2.85's single-casing exemption once the placement itself was in place (D-546); a
 * non-pinned twin of the same key collapses into that one pair instead of duplicating it.
 */
object AmbiguousCasingPlacement {

    /**
     * Inserts [chips] into [items] at each chip group's own candidate slot.
     *
     * @param items the already-rendered display items for this bar (an ambiguous candidate's text may
     *        already carry its context-derived casing; the chips replace the item wholesale, so the
     *        derived text is never shown alongside its own chips)
     * @param chips the computed dual-casing chips, grouped by their word's lower-case key; a group's
     *        internal order (lower before upper for a prefix pair, the alternate alone for an exact
     *        match) is preserved
     * @param maxSuggestions the bar's own C-03 slot budget
     * @return the bar with every displayed ambiguous candidate replaced by its chips at its rank -
     *         pinned continuations included, at their own leading slot (D-548) - every other item
     *         untouched, and any chip whose word was never displayed appended at the back, all capped
     *         at [maxSuggestions]. A chip duplicating a word the bar already shows is swallowed,
     *         never shown twice.
     */
    fun insertInPlace(
        items: List<SuggestionController.DisplayItem>,
        chips: List<SuggestionController.DisplayItem>,
        maxSuggestions: Int
    ): List<SuggestionController.DisplayItem> {
        if (chips.isEmpty()) {
            return items
        }
        val groups = chips.groupBy { it.word.lowercase() }
        val out = mutableListOf<SuggestionController.DisplayItem>()
        val insertedKeys = HashSet<String>()
        val shownTexts = HashSet<String>()
        for (item in items) {
            if (out.size >= maxSuggestions) {
                break
            }
            val group = if (item.kind == SuggestionController.Kind.NORMAL) {
                groups[item.word.lowercase()]
            } else {
                null
            }
            if (group == null) {
                if (shownTexts.add(item.word)) {
                    out.add(item)
                }
                continue
            }
            if (insertedKeys.add(item.word.lowercase())) {
                // A spelled-out fuzzy twin of the same key (both casings can rank as separate
                // candidates) is covered by the one chip pair already inserted - never duplicated.
                out.addAll(group.take(maxSuggestions - out.size))
            }
        }
        for (chip in chips) {
            if (out.size >= maxSuggestions) {
                break
            }
            // A chip whose group was already placed in the main loop is done; a chip duplicating a
            // text the bar already shows (a pinned continuation's canonical casing, a non-rankable
            // chip's own preview word) is swallowed, never shown twice.
            if (chip.word.lowercase() in insertedKeys) {
                continue
            }
            if (shownTexts.add(chip.word)) {
                out.add(chip)
            }
        }
        return out
    }
}
