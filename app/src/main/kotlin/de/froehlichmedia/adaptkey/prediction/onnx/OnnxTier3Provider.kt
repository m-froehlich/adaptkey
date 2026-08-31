// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import android.content.Context
import de.froehlichmedia.adaptkey.prediction.Tier3Decoding
import de.froehlichmedia.adaptkey.prediction.Tier3FamilyPrompt
import de.froehlichmedia.adaptkey.prediction.Tier3FamilyRequest
import de.froehlichmedia.adaptkey.prediction.Tier3FamilyResponseParser
import de.froehlichmedia.adaptkey.prediction.Tier3FamilyResult
import de.froehlichmedia.adaptkey.prediction.Tier3ModelFiles
import de.froehlichmedia.adaptkey.prediction.Tier3Prompt
import de.froehlichmedia.adaptkey.prediction.Tier3Provider
import de.froehlichmedia.adaptkey.prediction.Tier3Request
import de.froehlichmedia.adaptkey.prediction.Tier3ResponseParser
import de.froehlichmedia.adaptkey.prediction.Tier3Result
import java.io.File

/**
 * The real ONNX Runtime tier-3 backend (§9): prompt → tokenize → greedy generate → decode → parse.
 *
 * It composes the pure, unit-tested pieces (`Tier3Prompt`, `BpeTokenizer`, `Tier3ResponseParser`) with
 * the ONNX decode loop ([OnnxCausalLmSession]). It only exists when a model has been imported, so
 * [isAvailable] is always true for a constructed instance; [createIfAvailable] returns null when no
 * model is installed, leaving the keyboard on the inert [de.froehlichmedia.adaptkey.prediction.NoopTier3Provider].
 *
 * **Runtime-unverified here** (no device/ONNX runtime in this environment): the ONNX session is only
 * compile-checked. Also note [predict] is synchronous and heavy — the caller must invoke it off the IME
 * main thread on a real device to avoid an ANR.
 */
class OnnxTier3Provider private constructor(
    private val tokenizer: BpeTokenizer,
    private val session: OnnxCausalLmSession,
    private val decoding: Tier3Decoding
) : Tier3Provider, AutoCloseable {
    
    private val stopIds: Set<Int> = setOfNotNull(
        tokenizer.specialTokenId("<|endoftext|>"),
        tokenizer.specialTokenId("<|im_end|>")
    )
    
    override val isAvailable: Boolean = true
    
    override fun predict(request: Tier3Request): Tier3Result {
        val prompt = Tier3Prompt.build(request)
        val promptIds = tokenizer.encode(prompt)
        if (promptIds.isEmpty()) {
            return Tier3Result.EMPTY
        }
        val generatedIds = session.generate(promptIds, decoding.maxNewTokens, stopIds)
        val continuation = tokenizer.decode(generatedIds)
        return Tier3Result(Tier3ResponseParser.parse(request.input, continuation, decoding.numCandidates))
    }
    
    /**
     * D-404: the family-learning task (see [Tier3Provider.predictFamily]'s own KDoc). Uses the hard
     * [Tier3Decoding.MAX_NEW_TOKENS] budget regardless of [decoding]'s own (much smaller) next-word value -
     * a family answer needs far more tokens than an ordinary continuation, and this call is already only
     * ever made once per learn event (or once per backfilled word during reprocessing), not per keystroke,
     * so the extra latency is an accepted, deliberate trade-off.
     */
    override fun predictFamily(request: Tier3FamilyRequest): Tier3FamilyResult {
        val prompt = Tier3FamilyPrompt.build(request)
        val promptIds = tokenizer.encode(prompt)
        if (promptIds.isEmpty()) {
            return Tier3FamilyResult.EMPTY
        }
        val generatedIds = session.generate(promptIds, Tier3Decoding.MAX_NEW_TOKENS, stopIds)
        val continuation = tokenizer.decode(generatedIds)
        return Tier3FamilyResponseParser.parse(continuation)
    }
    
    override fun close() {
        session.close()
    }
    
    companion object {
        
        /**
         * Builds the backend when a model is installed, else returns null.
         *
         * @param context any valid context
         * @param decoding the decoding parameters
         * @return the backend, or null when no model is present (keyboard stays tier-1-only)
         */
        fun createIfAvailable(context: Context, decoding: Tier3Decoding = Tier3Decoding.DEFAULT): OnnxTier3Provider? {
            if (!Tier3ModelStorage.isModelInstalled(context)) {
                return null
            }
            val tokenizer = Tier3TokenizerLoader.load(context)
            val modelPath = File(Tier3ModelStorage.modelDir(context), Tier3ModelFiles.MODEL_FILE).absolutePath
            val session = OnnxCausalLmSession(modelPath)
            return OnnxTier3Provider(tokenizer, session, decoding)
        }
    }
}
