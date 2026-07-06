// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.touch

/**
 * The sentences offered in the K-01 calibration flow.
 *
 * The defaults are ASCII-only German pangrams: together they exercise every letter {@code a}-{@code z}
 * at least once, so the calibration seeds the personal offset model (T-03) across the whole alphabet.
 * They deliberately avoid umlauts, {@code ß} and punctuation, since those are not on the base QWERTZ
 * layout (L-01) and would not be directly typeable during calibration.
 */
object CalibrationSentences {
    
    /**
     * The default calibration sentences; their union covers every letter of the alphabet. Five sentences
     * (T-04 / K-01) give enough taps for a stable per-hand bias estimate, so the calibration reliably
     * detects a typing pattern rather than stopping at UNKNOWN.
     */
    val DEFAULT: List<String> = listOf(
        "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern",
        "Vogel Quax zwickt Johnys Pferd Bim",
        "Sylvia wagt quick den Jux bei Pforzheim",
        "Zwolf Boxkampfer jagen Viktor quer uber den Sylter Deich",
        "The quick brown fox jumps over the lazy dog"
    )
}
