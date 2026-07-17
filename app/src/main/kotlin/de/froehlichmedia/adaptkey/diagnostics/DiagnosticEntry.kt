// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.diagnostics

/**
 * D-139/D-110: one recorded diagnostic line, kept in [DiagnosticRingBuffer].
 *
 * @property timestampMs when this entry was recorded, on the same monotonic clock the buffer is pruned
 *           against (device-local; never wall-clock/calendar time, so nothing here can leak *when* in
 *           absolute terms the user was typing)
 * @property message the diagnostic text itself
 */
data class DiagnosticEntry(
    val timestampMs: Long,
    val message: String
)
