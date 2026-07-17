// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.diagnostics.DiagnosticEntry
import de.froehlichmedia.adaptkey.diagnostics.DiagnosticLog

/**
 * D-139/D-110: views and exports the current in-memory diagnostic log ([DiagnosticLog]) - the requested
 * alternative to `adb logcat` that needs no PC/USB tether: the phone can simply be brought back to this
 * screen after a repro, within the log's own 5-minute rolling window.
 *
 * Content is refreshed on every [onResume] (not just [onCreate]), so leaving the keyboard to reproduce an
 * issue and then returning here picks up whatever was recorded meanwhile, without needing to relaunch the
 * screen. Export mirrors the existing D-09 raw-tap-recording precedent: [Intent.ACTION_SEND] plain text,
 * no storage permission needed - plus a plain clipboard copy for pasting directly into a chat.
 */
class DiagnosticLogActivity : AppCompatActivity() {
    
    private lateinit var contentView: TextView
    private lateinit var emptyView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic_log)
        title = getString(R.string.d_diag_activity_title)
        
        contentView = findViewById(R.id.diag_content)
        emptyView = findViewById(R.id.diag_empty)
        
        findViewById<Button>(R.id.diag_share).setOnClickListener { share() }
        findViewById<Button>(R.id.diag_copy).setOnClickListener { copy() }
        findViewById<Button>(R.id.diag_clear).setOnClickListener {
            DiagnosticLog.clear()
            refresh()
            Toast.makeText(this, R.string.d_diag_cleared, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        refresh()
    }
    
    private fun refresh() {
        val text = formattedLog()
        contentView.text = text
        emptyView.visibility = if (text.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
    
    private fun share() {
        val text = formattedLog()
        if (text.isEmpty()) {
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.d_diag_share)))
    }
    
    private fun copy() {
        val text = formattedLog()
        if (text.isEmpty()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.d_diag_activity_title), text))
        Toast.makeText(this, R.string.d_diag_copied, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Renders every retained entry, oldest first, each prefixed with how long ago it was recorded (an
     * elapsed-time offset, e.g. `-4.2s`) rather than a wall-clock timestamp - directly answers "how long
     * before I opened this screen did that happen", without needing to correlate calendar time by hand.
     *
     * @return the formatted log, or an empty string when there is nothing to show
     */
    private fun formattedLog(): String {
        val entries = DiagnosticLog.snapshot()
        if (entries.isEmpty()) {
            return ""
        }
        val nowMs = SystemClock.elapsedRealtime()
        return entries.joinToString("\n") { entry -> formatEntry(entry, nowMs) }
    }
    
    private fun formatEntry(entry: DiagnosticEntry, nowMs: Long): String {
        val ageSeconds = (nowMs - entry.timestampMs) / 1000.0
        return "-%.1fs  %s".format(ageSeconds, entry.message)
    }
}
