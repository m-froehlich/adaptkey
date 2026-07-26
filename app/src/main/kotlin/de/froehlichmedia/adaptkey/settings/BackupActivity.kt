// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.backup.BackupExporter
import de.froehlichmedia.adaptkey.backup.BackupImporter
import de.froehlichmedia.adaptkey.backup.BackupJsonCodec
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * D-278: export/import screen (§21), reachable from Settings at any time.
 *
 * Mirrors [LanguagePacksActivity]'s own busy-flag + background-[Thread] + SAF pattern: export uses
 * [androidx.activity.result.contract.ActivityResultContracts.CreateDocument] to let the user pick a save
 * location, import uses [androidx.activity.result.contract.ActivityResultContracts.OpenDocument] to pick the
 * file back up - both entirely on-device, no network permission involved. The actual gather/apply logic
 * lives in [BackupExporter]/[BackupImporter] (Android-facing but Activity-free) and [BackupJsonCodec] (pure);
 * this class is UI glue only, covered by instrumented rather than unit tests, like every other settings
 * screen here.
 */
class BackupActivity : AppCompatActivity() {
    
    private lateinit var resultView: TextView
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private var busy = false
    
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            doExport(uri)
        }
    }
    
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            doImport(uri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        title = getString(R.string.d278_title)
        
        // D-188: same edge-to-edge inset fix as LanguagePacksActivity's own K-01-derived fix (§13).
        val root = findViewById<View>(R.id.backup_root)
        val basePadding = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            v.setPadding(
                basePadding,
                basePadding + maxOf(statusBars.top, cutout.top),
                basePadding,
                basePadding + maxOf(navBars.bottom, gestures.bottom)
            )
            insets
        }
        
        resultView = findViewById(R.id.backup_result)
        exportButton = findViewById(R.id.backup_export_button)
        importButton = findViewById(R.id.backup_import_button)
        exportButton.setOnClickListener { createDocument.launch(suggestedFileName()) }
        importButton.setOnClickListener { openDocument.launch(arrayOf("*/*")) }
    }
    
    private fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(System.currentTimeMillis())
        return "adaptkey-backup-$stamp.json"
    }
    
    private fun doExport(uri: Uri) {
        if (busy) {
            return
        }
        setBusy(true)
        Thread {
            val result = runCatching {
                val bundle = BackupExporter.export(this)
                val json = BackupJsonCodec.encode(bundle)
                contentResolver.openOutputStream(uri).use { output ->
                    requireNotNull(output) { "cannot open $uri" }
                    output.write(json.toByteArray(Charsets.UTF_8))
                }
            }
            runOnUiThread {
                setBusy(false)
                resultView.text = getString(if (result.isSuccess) R.string.d278_export_success else R.string.d278_export_failed)
                Toast.makeText(
                    this,
                    if (result.isSuccess) R.string.d278_export_success else R.string.d278_export_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }
    
    private fun doImport(uri: Uri) {
        if (busy) {
            return
        }
        setBusy(true)
        Thread {
            val json = runCatching {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "cannot open $uri" }
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
            }.getOrNull()
            val bundle = json?.let { BackupJsonCodec.decode(it) }
            val text = when {
                bundle == null -> getString(R.string.d278_import_failed)
                else -> {
                    val importResult = BackupImporter.import(this, bundle)
                    formatImportResult(importResult)
                }
            }
            runOnUiThread {
                setBusy(false)
                resultView.text = text
            }
        }.start()
    }
    
    private fun formatImportResult(result: BackupImporter.Result): String {
        if (result.formatTooNew) {
            return getString(R.string.d278_import_too_new)
        }
        val lines = mutableListOf(getString(R.string.d278_result_settings))
        if (result.importedLanguages.isNotEmpty()) {
            lines.add(getString(R.string.d278_result_imported, result.importedLanguages.joinToString(", ") { it.endonym }))
        }
        if (result.skippedLanguages.isNotEmpty()) {
            lines.add(getString(R.string.d278_result_skipped, result.skippedLanguages.joinToString(", ") { it.endonym }))
        }
        return lines.joinToString("\n")
    }
    
    private fun setBusy(value: Boolean) {
        busy = value
        exportButton.isEnabled = !value
        importButton.isEnabled = !value
    }
}
