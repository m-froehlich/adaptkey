// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.download.DownloadFolderResolver
import de.froehlichmedia.adaptkey.download.DownloadFolderStore
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelInstaller
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelStorage

/**
 * Model-import screen for the optional tier-3 mini-LLM (§9 / C-06).
 *
 * The app has no internet permission, so the network step is delegated to the browser: "download" opens
 * the model URL, the user's browser fetches the file, and "import" then locates it and copies it into
 * app-private storage via [Tier3ModelInstaller]. The large copy runs off the UI thread. Like the other
 * Android-facing layers it is covered by instrumented rather than unit tests; the copy/validation logic it
 * delegates to is unit-tested.
 *
 * D-386: "import" no longer opens a single-file picker - see [de.froehlichmedia.adaptkey.settings.
 * LanguagePacksActivity]'s own identical KDoc for the full reasoning (duplicate-file resolution shared via
 * [DownloadFolderStore]/[DownloadFolderResolver], the same granted folder reused by both screens).
 */
class Tier3ModelActivity : AppCompatActivity() {
    
    private lateinit var statusView: TextView
    private lateinit var downloadButton: Button
    private lateinit var importButton: Button
    private lateinit var removeButton: Button
    private var busy = false
    
    private val openTree = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            DownloadFolderStore.save(this, uri)
            resolveAndImport(uri)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tier3_model)
        title = getString(R.string.c06_model_title)
        
        statusView = findViewById(R.id.tier3_status)
        downloadButton = findViewById(R.id.tier3_download)
        importButton = findViewById(R.id.tier3_import)
        removeButton = findViewById(R.id.tier3_remove)
        
        downloadButton.setOnClickListener { openDownloadPage() }
        importButton.setOnClickListener { startImport() }
        removeButton.setOnClickListener { removeModel() }
        
        refresh()
    }
    
    private fun openDownloadPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MODEL_URL))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.c06_model_no_browser, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * D-386: begins the import - reuses the already-granted download folder when one exists, otherwise
     * explains why one is needed and requests it first. See [de.froehlichmedia.adaptkey.settings.
     * LanguagePacksActivity]'s own identical method for the full reasoning.
     */
    private fun startImport() {
        if (busy) {
            return
        }
        val treeUri = DownloadFolderStore.treeUri(this)
        if (treeUri == null) {
            showGrantFolderExplanation()
        } else {
            resolveAndImport(treeUri)
        }
    }
    
    private fun showGrantFolderExplanation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.d386_grant_folder_title)
            .setMessage(R.string.d386_grant_folder_message)
            .setPositiveButton(R.string.d386_grant_folder_action) { _, _ -> launchTreePicker() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun launchTreePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, DownloadFolderResolver.downloadsInitialUriHint())
        }
        openTree.launch(intent)
    }
    
    /**
     * D-386: resolves the newest matching file in [treeUri] for the model's own expected download name
     * (the model URL's last path segment) and imports it - or, when no match is found, forgets the stale
     * grant and re-prompts.
     */
    private fun resolveAndImport(treeUri: Uri) {
        val expectedFileName = Uri.parse(MODEL_URL).lastPathSegment
        if (expectedFileName == null) {
            Toast.makeText(this, R.string.d386_file_not_found, Toast.LENGTH_LONG).show()
            return
        }
        setBusy(true)
        statusView.setText(R.string.c06_model_importing)
        Thread {
            val matched = DownloadFolderResolver.findNewestMatch(this, treeUri, expectedFileName)
            if (matched == null) {
                DownloadFolderStore.clear(this)
                runOnUiThread {
                    setBusy(false)
                    Toast.makeText(this, R.string.d386_file_not_found, Toast.LENGTH_LONG).show()
                    refresh()
                }
                return@Thread
            }
            importModel(matched)
        }.start()
    }
    
    /**
     * D-386: now called already on [resolveAndImport]'s own background thread (never spawns its own), and
     * deletes the resolved model file afterward when it is recent enough ([DownloadFolderResolver.
     * deleteIfRecentlyCreated]).
     *
     * @param uri the resolved model file to import
     */
    private fun importModel(uri: Uri) {
        val result = runCatching {
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "cannot open $uri" }
                Tier3ModelInstaller.install(input, Tier3ModelStorage.modelDir(this))
            }
        }
        if (result.isSuccess) {
            DownloadFolderResolver.deleteIfRecentlyCreated(this, uri, DownloadFolderResolver.DELETE_MAX_AGE_MILLIS)
        }
        runOnUiThread {
            setBusy(false)
            val message = if (result.isSuccess) R.string.c06_model_imported else R.string.c06_model_import_failed
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            refresh()
        }
    }
    
    private fun removeModel() {
        Tier3ModelInstaller.clear(Tier3ModelStorage.modelDir(this))
        Toast.makeText(this, R.string.c06_model_removed, Toast.LENGTH_SHORT).show()
        refresh()
    }
    
    private fun setBusy(value: Boolean) {
        busy = value
        downloadButton.isEnabled = !value
        importButton.isEnabled = !value
        removeButton.isEnabled = !value
    }
    
    private fun refresh() {
        val installed = Tier3ModelStorage.isModelInstalled(this)
        statusView.text = getString(if (installed) R.string.c06_model_present else R.string.c06_model_absent)
        removeButton.isEnabled = installed && !busy
    }
    
    companion object {
        
        // Public, stable Hugging Face URL of the quantised SmolLM2-360M model graph (Apache-2.0).
        private const val MODEL_URL =
            "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct/resolve/main/onnx/model_q4f16.onnx?download=true"
    }
}
