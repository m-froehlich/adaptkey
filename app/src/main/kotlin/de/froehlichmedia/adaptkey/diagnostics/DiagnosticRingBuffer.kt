// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.diagnostics

/**
 * D-139/D-110: an in-memory, time-windowed FIFO of diagnostic lines - entries older than [retentionMs]
 * relative to the most recently recorded one are dropped automatically, so the log never grows unbounded
 * and never retains anything for long. Pure and Android-free (the caller supplies the timestamp, so this
 * is fully unit-testable with synthetic clocks) - the Android-facing [DiagnosticLog] wraps this with the
 * real clock, the settings-driven enable flag and thread safety.
 *
 * Requested directly, as an alternative to `adb logcat`: the phone does not need to stay tethered to a PC
 * to capture a repro - the log lives entirely in memory, is never written to disk, and never leaves the
 * device unless the user explicitly shares it.
 *
 * @property retentionMs how long an entry is kept, measured from the timestamp of the newest entry
 */
class DiagnosticRingBuffer(private val retentionMs: Long) {
    
    private val entries = ArrayDeque<DiagnosticEntry>()
    
    /**
     * Appends one entry and prunes anything now older than [retentionMs].
     *
     * @param timestampMs the entry's own timestamp (monotonic, e.g. `SystemClock.elapsedRealtime()`) -
     *        also the reference point pruning measures every other entry's age against
     * @param message the diagnostic text
     */
    fun record(timestampMs: Long, message: String) {
        entries.addLast(DiagnosticEntry(timestampMs, message))
        prune(timestampMs)
    }
    
    /** @return every currently retained entry, oldest first */
    fun entries(): List<DiagnosticEntry> = entries.toList()
    
    /** Discards every entry. */
    fun clear() {
        entries.clear()
    }
    
    private fun prune(nowMs: Long) {
        while (entries.isNotEmpty() && nowMs - entries.first().timestampMs > retentionMs) {
            entries.removeFirst()
        }
    }
}
