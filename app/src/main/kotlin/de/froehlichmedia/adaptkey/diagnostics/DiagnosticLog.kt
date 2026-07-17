// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.diagnostics

import android.os.SystemClock

/**
 * D-139/D-110: the app-wide, in-memory diagnostic log - opt-in (default off, see the `d_diag_enabled`
 * setting), a rolling 5-minute window ([DiagnosticRingBuffer]), never written to disk, never transmitted
 * anywhere. [AdaptKeyService] records into it while typing; [de.froehlichmedia.adaptkey.settings.
 * DiagnosticLogActivity] reads it back for viewing/sharing. A plain singleton `object`, not a bound
 * service - both call sites run in the same process, so a shared in-memory instance is all that is needed.
 *
 * [enabled] must be kept in sync with the persisted setting by whoever reads settings (`AdaptKeyService.
 * applySettings()`) - this object does not read `SharedPreferences` itself, keeping it free of any
 * `Context` dependency.
 */
object DiagnosticLog {
    
    /** Entries older than this (relative to the newest one) are dropped automatically. */
    private const val RETENTION_MS = 5 * 60 * 1000L
    
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
