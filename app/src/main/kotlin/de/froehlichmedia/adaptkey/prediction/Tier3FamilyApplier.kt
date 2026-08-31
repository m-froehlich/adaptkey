// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction

import de.froehlichmedia.adaptkey.dictionary.DictionaryStore

/**
 * D-404 (with-LLM path): applies a [Tier3FamilyResult] to a [DictionaryStore] - "with LLM, always learn the
 * whole family", as opposed to the non-LLM path's purely conservative, lookup-only linking
 * ([de.froehlichmedia.adaptkey.dictionary.LearnedLemmaLinking]). Kept as its own pure(-ish) unit, over the
 * [DictionaryStore] interface rather than any concrete store, so it is unit-testable with
 * [de.froehlichmedia.adaptkey.dictionary.InMemoryDictionaryStore] independently of
 * [de.froehlichmedia.adaptkey.AdaptKeyService]'s own (untestable) Android glue - the two call sites
 * ("on every learn event" and the "LLM newly installed"/"LLM already installed at migration time" backfill
 * pass) both reduce to exactly this one operation.
 */
object Tier3FamilyApplier {
    
    /**
     * Learns every form in [result] into [store], each linked back to [result]'s own lemma.
     *
     * A no-op when [result] carries no usable family at all (an empty/failed model generation -
     * [Tier3FamilyResult.EMPTY] or equivalent). Each form is learned via the ordinary [DictionaryStore.learn]
     * (creating it if new, reinforcing it if already learned - D-388's own seed-frequency/reinforcement rules
     * apply unchanged) with [result]'s own category as the hint, then explicitly linked to the lemma unless
     * it *is* the lemma - mirrors the non-LLM path's own "never overwrite an already-set link" caution:
     * [DictionaryStore.setLearnedLemma] is only called for a form that does not already carry its own link
     * ([DictionaryStore.entryOf]), so a prior manual correction (the editor's own "Grundform" dropdown) is
     * never silently overridden by a later application of the same (or a differently re-generated) family.
     *
     * @param store the store to write into (the caller's own active-language store)
     * @param result the model's own determined family (see [Tier3Provider.predictFamily])
     */
    fun apply(store: DictionaryStore, result: Tier3FamilyResult) {
        if (result.family.isEmpty() || result.lemma.isEmpty()) {
            return
        }
        val lemma = result.lemma.lowercase()
        for (form in result.family) {
            store.learn(form, null, categoryHint = result.category)
            if (!form.equals(result.lemma, ignoreCase = true) && store.entryOf(form)?.lemma == null) {
                store.setLearnedLemma(form, lemma)
            }
        }
    }
}
