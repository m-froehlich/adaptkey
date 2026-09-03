// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.language

import de.froehlichmedia.adaptkey.dictionary.AdjectiveInflection
import de.froehlichmedia.adaptkey.dictionary.CompoundSplit
import de.froehlichmedia.adaptkey.dictionary.RegularVerbInflection
import de.froehlichmedia.adaptkey.suggestion.Umlaut

/**
 * D-410: the German [LanguageRules] implementation - consolidates every genuinely German-specific
 * grammar/orthography rule this app implements today. Previously scattered across [TokenRepair]'s own
 * companion object, [de.froehlichmedia.adaptkey.AdaptKeyService]'s own constants, and unconditional calls
 * into [CompoundSplit]/[RegularVerbInflection]/[AdjectiveInflection] regardless of the active language - see
 * [LanguageRules]'s own KDoc and AdaptKey-History.md D-410 for the full background.
 */
object GermanRules : LanguageRules {
    
    override fun blocksAsSplitPrefix(candidate: String, frequency: Long): Boolean {
        return candidate in INSEPARABLE_PREFIXES && frequency <= PREFIX_COMMON_WORD_FREQUENCY_CEILING
    }
    
    override fun blocksAsFeminineAgentException(rightHalf: String, leftHalf: String, leftIsNoun: Boolean): Boolean {
        if (rightHalf != FEMININE_AGENT_SUFFIX || !leftIsNoun) {
            return false
        }
        if (leftHalf.endsWith(FEMININE_ER_SUFFIX)) {
            return true
        }
        return Umlaut.unfoldCandidates(leftHalf).any { it in FEMININE_AGENT_NOUN_STEMS }
    }
    
    override fun blocksAsCompoundPrefix(candidate: String, rightIsNoun: Boolean): Boolean {
        return rightIsNoun && candidate in COMPOUND_FORMING_PARTICLES
    }
    
    override fun isPlausibleVerbInflection(token: String, isKnownWord: (String) -> Boolean): Boolean {
        return RegularVerbInflection.isPlausibleInflection(token, isKnownWord)
    }
    
    override fun isPlausibleAdjectiveComparative(token: String, isPlausiblePositive: (String) -> Boolean): Boolean {
        return AdjectiveInflection.isPlausibleComparative(token, isPlausiblePositive)
    }
    
    override fun splitCompound(token: String, isKnownNoun: (String) -> Boolean, resolveRest: (String) -> String?): CompoundSplit.Result? {
        return CompoundSplit.split(token, isKnownNoun, resolveRest)
    }
    
    override fun timeSuggestionWord(): String = TIME_SUGGESTION_WORD
    
    override fun bundledConfusablesBlacklist(): Set<String> = BUNDLED_CONFUSABLES_BLACKLIST
    
    override fun decimalCommaGluesDigits(): Boolean = true
    
    /**
     * D-249: the German inseparable verb prefixes and productive negation/intensifying prefixes that
     * must never be accepted as the left half of a split - splitting either off is "so gut wie immer
     * falsch" (user's own assessment), e.g. "unglücklich" -> "un glücklich", "widersagen" -> "wider
     * sagen". Confirmed against the real dict_de.tsv, not guessed: several of these (e.g. "widersagen",
     * "entkoppeln") are not themselves dictionary entries while both the bare prefix ("wider", freq 598)
     * and the remaining stem ("sagen", freq 775) individually clear
     * [de.froehlichmedia.adaptkey.dictionary.TokenRepair.MIN_SPLIT_HALF_FREQUENCY] and neither is tagged a
     * noun, so [de.froehlichmedia.adaptkey.dictionary.TokenRepair]'s pre-existing gates alone do not catch
     * this shape. Deliberately excludes the Wechselpräfixe (über-/um-/durch-/unter-/voll-/hinter-/wieder-) -
     * each of those is also, itself, a common standalone German preposition/adverb/adjective (e.g. "wieder
     * holen" vs. "wiederholen" is the textbook case), so blocking them here would reject far more
     * genuine two-word missed-space splits than the compound-prefix false positives it would prevent -
     * left out deliberately, not merely forgotten.
     */
    private val INSEPARABLE_PREFIXES = setOf("ver", "zer", "ent", "emp", "be", "ge", "miss", "er", "un", "ur", "wider")
    
    /**
     * D-249: a member of [INSEPARABLE_PREFIXES] is exempted from the prefix-block rule entirely once its
     * own standalone dictionary frequency exceeds this ceiling - "er" (the personal pronoun, frequency
     * 120,975 in dict_de.tsv, by two-plus orders of magnitude the most frequent entry among the set) is
     * the one confirmed case: blocking it unconditionally would reject far more genuine two-word missed-
     * space splits (e.g. "erkommt" -> "er kommt") than the rare compound-verb false positive it would
     * catch. The other three dictionary hits in the set ("ver" 131, "ge" 250, "wider" 598) sit well below
     * this ceiling and are blocked normally; the remaining seven prefixes are not dictionary entries at
     * all (frequency 0) and are blocked unconditionally too. Calibrated against the real dictionary's own
     * frequency gap, not an arbitrary round number.
     */
    const val PREFIX_COMMON_WORD_FREQUENCY_CEILING = 5_000L
    
    /** D-261: the German feminine-agent-noun-forming suffix - see [blocksAsFeminineAgentException]. */
    private const val FEMININE_AGENT_SUFFIX = "in"
    
    /** D-261: the most productive German feminisation ending - see [blocksAsFeminineAgentException]. */
    private const val FEMININE_ER_SUFFIX = "er"
    
    /**
     * D-261: common German masculine agent/relation nouns, in their bare (un-umlauted) stem spelling,
     * whose feminine counterpart is formed with "+in" but which do not end in [FEMININE_ER_SUFFIX] - see
     * [blocksAsFeminineAgentException]. Calibrated, not exhaustive.
     */
    private val FEMININE_AGENT_NOUN_STEMS = setOf(
        "arzt", "chef", "koch", "nachbar", "student", "freund", "patient",
        "präsident", "polizist", "journalist", "könig", "graf", "gott"
    )
    
    /**
     * D-404-followup: common German adverbs/particles that frequently form the first element of a
     * compound noun ("Schonfenster", "Hochhaus", "Volltreffer", "Fernglas") - unlike D-249's
     * [INSEPARABLE_PREFIXES], each of these is also an entirely ordinary standalone word, so
     * [blocksAsCompoundPrefix] only ever blocks them when the right half is itself a noun: an uninflected
     * adjective/adverb glued directly onto a noun with nothing else in between is essentially always this
     * compound shape, never a genuine German phrase (normal phrase syntax requires an inflected ending
     * before a noun, e.g. "volles Glas", never bare "voll Glas"). Confirmed against the real repro
     * ("schon" `OTHER`, freq 11,685 - far too common for [PREFIX_COMMON_WORD_FREQUENCY_CEILING]'s own
     * exemption to help here, and rightly so: unlike D-249's verb prefixes, this rule was never meant to
     * exempt a common candidate, only to scope *when* it applies). Curated by hand, not algorithmically
     * derived - see [blocksAsCompoundPrefix]'s own KDoc for why no clean formal signal (bigram absence,
     * candidate length) reliably tells a genuine two-word phrase apart from this compound shape; not
     * claimed to be exhaustive, extend as further real cases turn up.
     */
    private val COMPOUND_FORMING_PARTICLES = setOf(
        "schon", "wohl", "hoch", "tief", "voll", "halb", "fern", "nah", "kaum", "fast"
    )
    
    /** S-08 / D-137: a typed time in German is essentially always followed by this word. */
    private const val TIME_SUGGESTION_WORD = "Uhr"
    
    // D-176/D-181: seeded once per installStores() call into the German store - see
    // AdaptKeyService.knownInOtherLanguage()'s own KDoc for the full reasoning. "aks" (D-172): a genuine
    // bundled English dictionary entry ("AKS", a Wikipedia-derived acronym, freq 18, PROPER_NOUN) was
    // tripping knownInOtherLanguage()'s cross-language shield and blocking "Aks" -> "als" - the identical
    // failure mode as "due"/"sue", fixed the identical way.
    //
    // D-206: pre-1996-spelling-reform relics of otherwise ordinary, high-frequency common words - a
    // curated subset of dict_de.tsv's own ß-containing entries, hand-picked (not a blanket rule) by
    // checking each candidate against the real corpus frequencies: kept only where the modern ss-form
    // is the dominant, living spelling in the very same corpus (e.g. "daß" 868 vs. "dass" 61892) -
    // never a genuinely modern long-vowel ß word that merely has a rarer Swiss-spelling ss-counterpart
    // present too (e.g. "große"/"grosse", "außerdem"/"ausserdem" - those stay untouched, ß is correct
    // and current there). Deliberately excludes proper nouns/surnames/place names sharing the same
    // ß-vs-ss shape (e.g. "Keßler", "Reuß", "Elsaß") - a person's or place's own spelling is not an
    // error to silently correct - and excludes two outright coincidental collisions between different
    // words that the naive ß->ss substitution alone cannot tell apart ("Maße" != "Masse", "Buße" !=
    // "Busse"). Blacklisting (not purging from the dictionary) keeps each word typeable/known - so
    // quoting genuinely old text still works - while it can never surface as its own suggestion again;
    // the existing ß->"ss" fold (Umlaut.fold, unrelated to D-204's own newer host-key fold) already
    // makes each of these a cost-0 match for its modern form, so autocorrect can still silently fix a
    // live typing of one of these to the modern spelling via the existing §44 known-word override.
    private val BUNDLED_CONFUSABLES_BLACKLIST = setOf(
        "due", "sue", "ddr", "aks",
        "daß", "muß", "mußt", "mußte", "müßte", "wußte", "läßt", "laß", "laßt",
        "einfluß", "anschluß", "schluß", "fluß", "prozeß", "kongreß", "rußland",
        "bewußt", "bewußtsein", "bewußtseins", "unbewußten",
        "haß", "gewiß", "kuß", "bißchen", "häßlich"
    )
}
