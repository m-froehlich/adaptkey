package de.froehlichmedia.adaptkey.prediction

/**
 * Pure, validated decoding parameters for a tier-3 generation (§9).
 *
 * These bound how much work the mini-LLM does per activation: a keyboard only needs a few tokens of
 * continuation, so the defaults are deliberately tiny (latency + battery). Kept Android-free so the
 * limits and their validation are unit-testable; the ONNX backend reads them when it runs a generation.
 *
 * @property maxNewTokens the maximum number of tokens to generate per activation (1..64)
 * @property numCandidates the maximum number of candidate words to surface (1..[MAX_CANDIDATES])
 */
data class Tier3Decoding(
    val maxNewTokens: Int = 8,
    val numCandidates: Int = 3
) {
    
    init {
        require(maxNewTokens in 1..MAX_NEW_TOKENS) { "maxNewTokens out of range: $maxNewTokens" }
        require(numCandidates in 1..MAX_CANDIDATES) { "numCandidates out of range: $numCandidates" }
    }
    
    companion object {
        
        /** Hard upper bound on generated tokens (a keyboard never needs more than a short continuation). */
        const val MAX_NEW_TOKENS = 64
        
        /** Hard upper bound on surfaced candidates. */
        const val MAX_CANDIDATES = 10
        
        /** The default decoding parameters. */
        val DEFAULT = Tier3Decoding()
    }
}
