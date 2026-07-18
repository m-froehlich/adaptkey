// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.diagnostics

import android.os.SystemClock

/**
 * D-139/D-110: the app-wide, in-memory diagnostic log - opt-in (default off, see the `d_diag_enabled`
 * setting), a rolling 1-minute window ([DiagnosticRingBuffer]), never written to disk, never transmitted
 * anywhere. [AdaptKeyService] records into it while typing; [de.froehlichmedia.adaptkey.settings.
 * DiagnosticLogActivity] reads it back for viewing/sharing. A plain singleton `object`, not a bound
 * service - both call sites run in the same process, so a shared in-memory instance is all that is needed.
 *
 * [enabled] must be kept in sync with the persisted setting by whoever reads settings (`AdaptKeyService.
 * applySettings()`) - this object does not read `SharedPreferences` itself, keeping it free of any
 * `Context` dependency. D-150: password-field text must never reach this log at all, regardless of
 * [enabled] - that exclusion is enforced by the caller (`AdaptKeyService.diag()`), not here, since only the
 * caller knows the currently focused field's kind.
 */
object DiagnosticLog {
    
    // D-150: a single repro fits comfortably within a much shorter window than the original 5 minutes -
    // shortened at the user's own request, since 5 minutes of typing produced far more log volume (and
    // far more to read through/share) than a repro actually needs.
    /** Entries older than this (relative to the newest one) are dropped automatically. */
    private const val RETENTION_MS = 60 * 1000L
    
    private val buffer = DiagnosticRingBuffer(RETENTION_MS)
    
    /** Whether recording is currently active - mirrors the settings toggle; cleared entries when turned off. */
    @Volatile
    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                clear()
            }
        }
    
    /**
     * Records [message] if [enabled] - a cheap no-op otherwise, so call sites can call this unconditionally
     * without their own enabled-check.
     *
     * @param message the diagnostic text
     */
    @Synchronized
    fun record(message: String) {
        if (!enabled) {
            return
        }
        buffer.record(SystemClock.elapsedRealtime(), message)
    }
    
    /** @return every currently retained entry, oldest first */
    @Synchronized
    fun snapshot(): List<DiagnosticEntry> = buffer.entries()
    
    /** Discards every entry (e.g. when the toggle is turned off, or on explicit request from the viewer). */
    @Synchronized
    fun clear() {
        buffer.clear()
    }
}
