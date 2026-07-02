// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.ByteOrder
import java.nio.LongBuffer
import java.nio.ShortBuffer

/**
 * Greedy autoregressive decoder for the tier-3 SmolLM2 ONNX graph (§9), on ONNX Runtime.
 *
 * The exported graph is a decoder-with-past: inputs `input_ids`, `attention_mask` and
 * `past_key_values.{0..L-1}.key/value`; outputs `logits` and `present.{0..L-1}.key/value` (Llama, 32
 * layers, GQA 5 KV-heads, head-dim 64, **fp16** KV-cache and logits; no `position_ids` input). Each step
 * feeds the previous step's `present.*` tensors straight back as `past_key_values.*`, so the fp16 KV
 * cache is only ever shuttled, never read/converted on the JVM — only the last-position logits are read
 * (fp16→float) to pick the greedy argmax.
 *
 * **Device-only / runtime-unverified:** this environment has no emulator and no ONNX runtime, so this is
 * only compile-checked here. Tensor lifecycle, the empty-past shape and the fp16 logits read must be
 * validated on a real device; iterate against device logs.
 */
class OnnxCausalLmSession(modelPath: String) : AutoCloseable {
    
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(modelPath, OrtSession.SessionOptions())
    
    /**
     * Greedily generates continuation tokens for [promptIds].
     *
     * @param promptIds the prompt token ids
     * @param maxNewTokens the maximum number of tokens to generate
     * @param stopIds token ids that end generation (e.g. eos) when produced
     * @return the generated token ids (excluding the prompt)
     */
    fun generate(promptIds: IntArray, maxNewTokens: Int, stopIds: Set<Int>): IntArray {
        val generated = ArrayList<Int>(maxNewTokens)
        var currentIds = LongArray(promptIds.size) { promptIds[it].toLong() }
        var pastLength = 0
        var pastResult: OrtSession.Result? = null
        var pastKv: Map<String, OnnxTensor>? = null
        try {
            for (step in 0 until maxNewTokens) {
                val seqLen = currentIds.size
                val totalLength = pastLength + seqLen
                val inputs = HashMap<String, OnnxTensor>()
                val idsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(currentIds), longArrayOf(1, seqLen.toLong()))
                val maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(LongArray(totalLength) { 1L }), longArrayOf(1, totalLength.toLong()))
                inputs["input_ids"] = idsTensor
                inputs["attention_mask"] = maskTensor
                val freshEmptyPast = ArrayList<OnnxTensor>()
                if (pastKv == null) {
                    for (layer in 0 until LAYERS) {
                        val key = emptyKvTensor()
                        val value = emptyKvTensor()
                        inputs["past_key_values.$layer.key"] = key
                        inputs["past_key_values.$layer.value"] = value
                        freshEmptyPast.add(key)
                        freshEmptyPast.add(value)
                    }
                } else {
                    inputs.putAll(pastKv)
                }
                
                val result = session.run(inputs)
                val nextToken = argmaxLastLogit(result.get("logits").get() as OnnxTensor, seqLen)
                
                idsTensor.close()
                maskTensor.close()
                freshEmptyPast.forEach { it.close() }
                pastResult?.close()
                
                val nextPast = HashMap<String, OnnxTensor>(LAYERS * 2)
                for (layer in 0 until LAYERS) {
                    nextPast["past_key_values.$layer.key"] = result.get("present.$layer.key").get() as OnnxTensor
                    nextPast["past_key_values.$layer.value"] = result.get("present.$layer.value").get() as OnnxTensor
                }
                pastKv = nextPast
                pastResult = result
                pastLength = totalLength
                
                generated.add(nextToken)
                if (nextToken in stopIds) {
                    break
                }
                currentIds = longArrayOf(nextToken.toLong())
            }
        } finally {
            pastResult?.close()
        }
        return generated.toIntArray()
    }
    
    private fun emptyKvTensor(): OnnxTensor {
        val shape = longArrayOf(1, KV_HEADS.toLong(), 0, HEAD_DIM.toLong())
        return OnnxTensor.createTensor(env, ShortBuffer.wrap(ShortArray(0)), shape, OnnxJavaType.FLOAT16)
    }
    
    /**
     * Argmax over the vocabulary of the last position of a `[1, seqLen, vocab]` fp16 logits tensor.
     */
    private fun argmaxLastLogit(logits: OnnxTensor, seqLen: Int): Int {
        val halves = logits.byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val vocab = halves.capacity() / seqLen
        val base = (seqLen - 1) * vocab
        var bestIndex = 0
        var bestValue = Float.NEGATIVE_INFINITY
        for (i in 0 until vocab) {
            val value = Fp16.toFloat(halves.get(base + i))
            if (value > bestValue) {
                bestValue = value
                bestIndex = i
            }
        }
        return bestIndex
    }
    
    override fun close() {
        session.close()
        // env is a shared process-wide singleton; do not close it here.
    }
    
    companion object {
        
        private const val LAYERS = 32
        private const val KV_HEADS = 5
        private const val HEAD_DIM = 64
    }
}
