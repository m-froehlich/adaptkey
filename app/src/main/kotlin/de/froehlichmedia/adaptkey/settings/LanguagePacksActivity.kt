// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog
import de.froehlichmedia.adaptkey.dictionary.LanguagePackInstaller
import de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage
import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
import de.froehlichmedia.adaptkey.language.Language

/**
 * D-280: install/remove screen for the languages beyond English (always bundled) - reachable from Settings
 * at any time, and also opened directly from the onboarding language-selection step.
 *
 * Mirrors [Tier3ModelActivity]'s own browser-download + SAF-import flow exactly (the app has no internet
 * permission, so the network step is delegated to the browser: "download" opens the pack's URL, "import"
 * then picks the downloaded file with the system file picker and unzips it via [LanguagePackInstaller]),
 * just for a list of languages ([LanguagePackCatalog]) instead of a single model file. A successful
 * install/removal writes [InstalledLanguagesStore], whose own listener in `AdaptKeyService` reloads the
 * dictionary stores immediately - no manual keyboard restart needed. Android-view glue, covered by
 * instrumented rather than unit tests, like every other settings screen here.
 */
class LanguagePacksActivity : AppCompatActivity() {
    
    private lateinit var container: LinearLayout
    private var pendingImportEntry: LanguagePackCatalog.Entry? = null
    private var busy = false
    
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val entry = pendingImportEntry
        if (uri != null && entry != null) {
            importPack(uri, entry)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_packs)
        title = getString(R.string.d280_title)
        
        // D-310: opportunistic, harmless-if-nothing-to-do cleanup of any pre-D-310 flat pack files - this
        // screen is the one place a stale install actually gets noticed/reinstalled, so it is the natural
        // place to also sweep up their now-unused old-layout leftovers.
        LanguagePackStorage.cleanupLegacyFlatFiles(this)
        
        // D-188: same edge-to-edge inset fix as BlacklistActivity's own K-01-derived fix (§13).
        val root = findViewById<View>(R.id.language_packs_root)
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
        
        container = findViewById(R.id.language_packs_container)
        rebuild()
    }
    
    private fun rebuild() {
        container.removeAllViews()
        val installed = InstalledLanguagesStore.load(this)
        for (entry in LanguagePackCatalog.ENTRIES) {
            container.addView(buildRow(entry, entry.language in installed))
        }
    }
    
    /**
     * D-307/D-308: a language pack row shows three status texts - not installed, installed and current, or
     * installed with a newer [LanguagePackCatalog.Entry.version] than what
     * [InstalledLanguagesStore.installedVersion] recorded (a lightweight, no-download-needed hint; only
     * moves when this app itself is updated, see that property's own KDoc). Download+Import are always
     * shown regardless of that hint - D-308's own point is that the *authoritative* version lives inside
     * the archive itself, not in this app's compiled-in catalog, so a manual re-check must always be
     * possible even when the hint says "current" (e.g. a community contributor revised the hosted pack
     * without a matching app release).
     */
    private fun buildRow(entry: LanguagePackCatalog.Entry, installed: Boolean): View {
        val updateAvailable = installed && InstalledLanguagesStore.installedVersion(this, entry.language) < entry.version
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
            }
        }
        row.addView(TextView(this).apply {
            text = entry.language.endonym
            setTypeface(typeface, Typeface.BOLD)
        })
        row.addView(TextView(this).apply {
            setText(
                when {
                    updateAvailable -> R.string.d280_status_update_available
                    installed -> R.string.d280_status_installed
                    else -> R.string.d280_status_not_installed
                }
            )
        })
        if (installed) {
            row.addView(Button(this).apply {
                setText(R.string.d280_remove)
                isEnabled = !busy
                setOnClickListener { removePack(entry.language) }
            })
        }
        row.addView(Button(this).apply {
            setText(R.string.d280_download)
            isEnabled = !busy
            setOnClickListener { openDownloadPage(entry.downloadUrl) }
        })
        row.addView(Button(this).apply {
            setText(R.string.d280_import)
            isEnabled = !busy
            setOnClickListener {
                pendingImportEntry = entry
                openDocument.launch(arrayOf("*/*"))
            }
        })
        return row
    }
    
    private fun openDownloadPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.d280_no_browser, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * D-308: the authoritative version is now the freshly-picked archive's own [LanguagePackInstaller.
     * ParsedPack.version] (its `version.txt` entry), never [LanguagePackCatalog.Entry.version] (that stays
     * only a compiled-in "you might want to check" hint, see [buildRow]). For a language already installed,
     * the archive is only actually applied when its version is strictly newer than
     * [InstalledLanguagesStore.installedVersion] - otherwise nothing on disk changes and the user is told
     * it is already current, so a re-check never silently downgrades or redundantly reseeds the dictionary
     * database. A language not yet installed at all always applies unconditionally - there is nothing to
     * compare against yet.
     *
     * @param uri the picked archive
     * @param entry the row whose Download/Import buttons were tapped - only [LanguagePackCatalog.Entry.
     *        language] is actually used for the install itself; [LanguagePackCatalog.Entry.version] is not
     *        consulted here at all
     */
    private fun importPack(uri: Uri, entry: LanguagePackCatalog.Entry) {
        if (busy) {
            return
        }
        val language = entry.language
        val alreadyInstalled = language in InstalledLanguagesStore.load(this)
        setBusy(true)
        Thread {
            // null result = skipped, the picked archive was not newer than what is already installed.
            val result = runCatching<Int?> {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "cannot open $uri" }
                    val pack = LanguagePackInstaller.parse(input, language)
                    if (alreadyInstalled && pack.version <= InstalledLanguagesStore.installedVersion(this, language)) {
                        return@use null
                    }
                    LanguagePackInstaller.write(LanguagePackStorage.packDir(this), pack)
                    // Deletes any stale database from a previous install of this same language, so the next
                    // dictionary load reseeds cleanly from the file just imported rather than reusing old data.
                    deleteDatabase(DictionaryLoader.databaseName(language))
                    InstalledLanguagesStore.add(this, language, pack.version)
                    pack.version
                }
            }
            runOnUiThread {
                setBusy(false)
                val message = when {
                    result.isFailure -> R.string.d280_import_failed
                    result.getOrNull() == null -> R.string.d280_already_current
                    else -> R.string.d280_imported
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                rebuild()
            }
        }.start()
    }
    
    private fun removePack(language: Language) {
        LanguagePackInstaller.clear(LanguagePackStorage.packDir(this), language)
        deleteDatabase(DictionaryLoader.databaseName(language))
        InstalledLanguagesStore.remove(this, language)
        Toast.makeText(this, R.string.d280_removed, Toast.LENGTH_SHORT).show()
        rebuild()
    }
    
    private fun setBusy(value: Boolean) {
        busy = value
        rebuild()
    }
    
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
