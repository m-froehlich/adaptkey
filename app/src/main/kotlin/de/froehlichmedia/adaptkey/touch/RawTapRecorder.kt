// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

import kotlin.math.roundToLong

/**
 * A single recorded raw tap (D-09 diagnostic): the character the calibration sentence expected at that
 * point, the key the touch actually resolved to, the key's geometric centre and the raw contact point.
 * The per-axis deviation (contact minus centre) is what reveals a systematic finger offset - e.g. an
 * uncalibrated left index finger landing consistently left of every key.
 *
 * @property expectedChar the character the calibration sentence expected, or null past the sentence end
 * @property resolvedKeyId the id of the key the tap resolved to (T-01 / T-03)
 * @property keyCenterX the resolved key's centre x, in view pixels
 * @property keyCenterY the resolved key's centre y, in view pixels
 * @property tapX the raw contact x, in view pixels
 * @property tapY the raw contact y, in view pixels
 */
data class RawTap(
    val expectedChar: Char?,
    val resolvedKeyId: String,
    val keyCenterX: Float,
    val keyCenterY: Float,
    val tapX: Float,
    val tapY: Float
) {
    
    /** Horizontal deviation of the contact from the key centre (positive = right of centre). */
    val deviationX: Float
        get() = tapX - keyCenterX
    
    /** Vertical deviation of the contact from the key centre (positive = below centre). */
    val deviationY: Float
        get() = tapY - keyCenterY
}

/**
 * Pure, Android-free collector for the D-09 raw-tap diagnostic recorded during a calibration session.
 *
 * The calibration screen feeds each resolved tap into [record]; on completion the collected taps are
 * serialised to a tab-separated table via [toTsv] and exported (e.g. shared as text) so a systematic
 * mis-resolution can be analysed offline. Kept free of Android dependencies so the collection and
 * formatting are unit-testable.
 */
class RawTapRecorder {
    
    private val taps = ArrayList<RawTap>()
    
    /** The taps recorded so far, in order. */
    val entries: List<RawTap>
        get() = taps
    
    /** Whether no taps have been recorded yet. */
    fun isEmpty(): Boolean {
        return taps.isEmpty()
    }
    
    /**
     * Records one resolved tap.
     *
     * @param expectedChar the character expected at this position, or null past the sentence end
     * @param resolvedKeyId the id of the key the tap resolved to
     * @param keyCenterX the resolved key's centre x
     * @param keyCenterY the resolved key's centre y
     * @param tapX the raw contact x
     * @param tapY the raw contact y
     */
    fun record(expectedChar: Char?, resolvedKeyId: String, keyCenterX: Float, keyCenterY: Float, tapX: Float, tapY: Float) {
        taps.add(RawTap(expectedChar, resolvedKeyId, keyCenterX, keyCenterY, tapX, tapY))
    }
    
    /** Discards all recorded taps. */
    fun clear() {
        taps.clear()
    }
    
    /**
     * Serialises the recorded taps as a tab-separated table with a header row: index, expected char,
     * resolved key id, key centre, contact point and per-axis deviation. Coordinates are rounded to two
     * decimals with a locale-independent decimal point so the output is stable across devices.
     *
     * @return the TSV text (header only when nothing was recorded)
     */
    fun toTsv(): String {
        val builder = StringBuilder()
        builder.append("index\texpected\tresolved\tkeyCenterX\tkeyCenterY\ttapX\ttapY\tdeviationX\tdeviationY\n")
        taps.forEachIndexed { index, tap ->
            builder.append(index).append('\t')
            builder.append(expectedField(tap.expectedChar)).append('\t')
            builder.append(tap.resolvedKeyId).append('\t')
            builder.append(fmt(tap.keyCenterX)).append('\t')
            builder.append(fmt(tap.keyCenterY)).append('\t')
            builder.append(fmt(tap.tapX)).append('\t')
            builder.append(fmt(tap.tapY)).append('\t')
            builder.append(fmt(tap.deviationX)).append('\t')
            builder.append(fmt(tap.deviationY)).append('\n')
        }
        return builder.toString()
    }
    
    private fun expectedField(ch: Char?): String {
        return when {
            ch == null -> ""
            ch == ' ' -> "space"
            else -> ch.toString()
        }
    }
    
    private fun fmt(value: Float): String {
        // Round to two decimals; Double.toString is locale-independent (always a dot), unlike String.format.
        return ((value * 100f).roundToLong() / 100.0).toString()
    }
}
