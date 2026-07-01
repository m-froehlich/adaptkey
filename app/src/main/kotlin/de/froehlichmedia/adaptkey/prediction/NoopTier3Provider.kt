package de.froehlichmedia.adaptkey.prediction

/**
 * The default tier-3 backend: always unavailable, never predicts anything (§9, Option A).
 *
 * With this backend installed the whole tier-3 pipeline is inert — [Tier3Activation] never activates,
 * no request is built, the suggestion merge is skipped and no capitalisation proposal is made — so the
 * keyboard's observable behaviour is identical to a tier-1-only build. It is the placeholder until a
 * real ONNX Runtime / Gemma-Nano backend is wired in behind the same [Tier3Provider] interface.
 */
object NoopTier3Provider : Tier3Provider {
    
    override val isAvailable: Boolean = false
    
    override fun predict(request: Tier3Request): Tier3Result = Tier3Result.EMPTY
}
