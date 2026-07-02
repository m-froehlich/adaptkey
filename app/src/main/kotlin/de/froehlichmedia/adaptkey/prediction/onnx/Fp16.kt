// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.prediction.onnx

/**
 * IEEE-754 half-precision (fp16) → single-precision float conversion (§9).
 *
 * The tier-3 ONNX graph emits fp16 logits; picking the greedy next token means reading those halves and
 * comparing them as floats. Kept as a tiny pure function (no ONNX dependency) so the conversion — a
 * correctness-critical detail with subnormal/inf/NaN edge cases — is unit-testable on the JVM.
 */
object Fp16 {
    
    /**
     * Converts an fp16 bit pattern to a float.
     *
     * @param half the 16-bit half-precision value
     * @return the equivalent float
     */
    fun toFloat(half: Short): Float {
        val bits = half.toInt() and 0xFFFF
        val sign = bits and 0x8000 shl 16
        val exponent = bits and 0x7C00 shr 10
        val mantissa = bits and 0x03FF
        val result = when (exponent) {
            0 -> {
                if (mantissa == 0) {
                    sign
                } else {
                    var e = -1
                    var m = mantissa
                    do {
                        e++
                        m = m shl 1
                    } while (m and 0x0400 == 0)
                    sign or (127 - 15 - e shl 23) or (m and 0x03FF shl 13)
                }
            }
            
            0x1F -> sign or 0x7F800000 or (mantissa shl 13)
            else -> sign or (exponent - 15 + 127 shl 23) or (mantissa shl 13)
        }
        return Float.fromBits(result)
    }
}
