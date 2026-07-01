package de.froehlichmedia.adaptkey.prediction

/**
 * The pure C-06 activation policy (§9): decides whether the mini-LLM (tier 3) should be consulted for
 * the current token.
 *
 * Tier 3 is consulted only when a backend is actually available and the tier-1 confidence is strictly
 * below the configured threshold. Keeping this a tiny pure function makes the "when does the LLM wake
 * up" decision independently testable and impossible to get subtly wrong in the Android glue.
 */
object Tier3Activation {
    
    /**
     * @param tier1Confidence the tier-1 dominance from [Tier1Confidence], in [0, 1]
     * @param threshold the configured C-06 activation threshold
     * @param providerAvailable whether a real tier-3 backend is loaded ([Tier3Provider.isAvailable])
     * @return true when tier 3 should be consulted for this token
     */
    fun shouldActivate(
        tier1Confidence: Double,
        threshold: LlmActivationThreshold,
        providerAvailable: Boolean
    ): Boolean {
        if (!providerAvailable) {
            return false
        }
        return tier1Confidence < threshold.confidenceThreshold
    }
}
