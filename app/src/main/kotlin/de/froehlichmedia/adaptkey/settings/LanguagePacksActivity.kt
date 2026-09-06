// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog
import de.froehlichmedia.adaptkey.dictionary.LanguagePackInstaller
import de.froehlichmedia.adaptkey.dictionary.LanguagePackStorage
import de.froehlichmedia.adaptkey.download.DownloadFileSupport
import de.froehlichmedia.adaptkey.language.InstalledLanguagesStore
import de.froehlichmedia.adaptkey.language.Language
import java.text.Collator

/**
 * D-280: install/remove screen for the languages beyond English (always bundled) - reachable from Settings
 * at any time, and also opened directly from the onboarding language-selection step.
 *
 * Mirrors [Tier3ModelActivity]'s own browser-download + SAF-import flow exactly (the app has no internet
 * permission, so the network step is delegated to the browser: "download" opens the pack's URL, "import"
 * then locates the downloaded file and unzips it via [LanguagePackInstaller]), just for a list of languages
 * ([LanguagePackCatalog]) instead of a single model file. A successful install/removal writes
 * [InstalledLanguagesStore], whose own listener in `AdaptKeyService` reloads the dictionary stores
 * immediately - no manual keyboard restart needed. Android-view glue, covered by instrumented rather than
 * unit tests, like every other settings screen here.
 *
 * D-413: "import" opens a plain `ACTION_OPEN_DOCUMENT` single-file picker again - reverting D-386's
 * folder-grant automation, which turned out to be unusable on a real device (Samsung One UI, a recent
 * Android version refuses to grant the Downloads folder itself via `ACTION_OPEN_DOCUMENT_TREE` at all:
 * "Dieser Ordner kann nicht verwendet werden..."). The user picks the exact archive again, every time, the
 * way the app worked before D-386; [DownloadFileSupport.deleteIfRecentlyCreated] still cleans up the picked
 * file afterward when it is no older than [DownloadFileSupport.DELETE_MAX_AGE_MILLIS], and the D-386-followup
 * staleness/language checks in [LanguagePackInstaller] remain in place as a safety net.
 */
class LanguagePacksActivity : AppCompatActivity() {
    
    private lateinit var container: LinearLayout
    private var pendingImportEntry: LanguagePackCatalog.Entry? = null
    private var busy = false
    
    private val openDocument = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        val entry = pendingImportEntry
        if (result.resultCode == RESULT_OK && uri != null && entry != null) {
            setBusy(true)
            Thread { importPack(uri, entry) }.start()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_packs)
        title = getString(R.string.d280_title)
        
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
    
    /**
     * D-454: English (always built-in, never in [LanguagePackCatalog.ENTRIES]) is folded into the same
     * sorted list as every downloadable pack rather than described separately above it - see
     * [buildBuiltInRow]. Sort order: available-for-typing-right-now (English, or an installed pack) before
     * everything else, each half then alphabetical by [Language.endonym] via a locale-independent
     * [Collator] (this list mixes many languages/scripts at once, so no single language's own collation
     * rules would be more "correct" here than any other's).
     */
    private fun rebuild() {
        container.removeAllViews()
        val installed = InstalledLanguagesStore.load(this)
        val collator = Collator.getInstance()
        val entries = (LanguagePackCatalog.ENTRIES.map { it.language to it } + (Language.ENGLISH to null))
            .sortedWith(
                compareByDescending<Pair<Language, LanguagePackCatalog.Entry?>> { (language, entry) ->
                    entry == null || language in installed
                }.thenBy(collator) { (language, _) -> language.endonym }
            )
        for ((language, entry) in entries) {
            container.addView(if (entry == null) buildBuiltInRow(language) else buildRow(entry, language in installed))
        }
    }
    
    /**
     * D-454: the built-in English row - same heading style as [buildRow], but a single, permanently
     * disabled button in place of the usual status text + action buttons, since English can be neither
     * installed nor removed.
     */
    private fun buildBuiltInRow(language: Language): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
            }
        }
        row.addView(TextView(this).apply {
            text = "${flagFor(language)} ${language.endonym}"
            setTypeface(typeface, Typeface.BOLD)
        })
        row.addView(Button(this).apply {
            setText(R.string.d280_builtin)
            isEnabled = false
        })
        return row
    }
    
    /**
     * D-454: the single, editorially-chosen flag glyph shown before a language's own label - not a claim
     * about where a language is spoken, just a recognisable visual anchor per row. Every language gets
     * exactly one flag, chosen for the country most closely associated with it (usually its own eponymous
     * country), except [Language.ENGLISH] - the user's own explicit call - which shows both the UK's and
     * the US's flags side by side rather than picking one.
     */
    private fun flagFor(language: Language): String = LANGUAGE_FLAGS[language].orEmpty()
    
    /**
     * D-307/D-308: a language pack row shows three status texts - not installed, installed and current, or
     * installed with a newer [LanguagePackCatalog.Entry.version] than what
     * [InstalledLanguagesStore.installedVersion] recorded (a lightweight, no-download-needed hint; only
     * moves when this app itself is updated, see that property's own KDoc). Download+Import are always
     * shown regardless of that hint - D-308's own point is that the *authoritative* version lives inside
     * the archive itself, not in this app's compiled-in catalog, so a manual re-check must always be
     * possible even when the hint says "current" (e.g. a community contributor revised the hosted pack
     * without a matching app release).
     *
     * D-334: the hint is additionally suppressed when [InstalledLanguagesStore.suppressedCatalogVersion]
     * matches or exceeds the catalog's own version - the user already attempted an import that turned out
     * to be already current (the hosted archive was stale), and the hint must not reappear until a future
     * app release raises the catalog version past what was dismissed.
     */
    private fun buildRow(entry: LanguagePackCatalog.Entry, installed: Boolean): View {
        val updateAvailable = installed &&
            InstalledLanguagesStore.installedVersion(this, entry.language) < entry.version &&
            InstalledLanguagesStore.suppressedCatalogVersion(this, entry.language) < entry.version
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
            }
        }
        row.addView(TextView(this).apply {
            text = "${flagFor(entry.language)} ${entry.language.endonym}"
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
                setOnClickListener { confirmRemove(entry.language) }
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
            setOnClickListener { startImport(entry) }
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
     * D-413: begins the import - launches a plain `ACTION_OPEN_DOCUMENT` single-file picker every time (no
     * more persisted folder grant to check first).
     *
     * @param entry the row whose Import button was tapped
     */
    private fun startImport(entry: LanguagePackCatalog.Entry) {
        if (busy) {
            return
        }
        pendingImportEntry = entry
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            // D-413: write access is needed for DownloadFileSupport's own post-import cleanup below.
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            // Best-effort hint to open directly in Downloads - silently ignored by providers that don't
            // recognise this exact document ID shape (e.g. some OEM pickers), never an error.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, DownloadFileSupport.downloadsInitialUriHint())
        }
        openDocument.launch(intent)
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
     * D-334: the old [android.content.Context.deleteDatabase] call that preceded the install is gone - it
     * wiped the entire SQLite database including every learned word, blacklist entry, and pending-blacklist
     * mark on every real pack update. The reseed itself now lives in [DictionaryLoader.loadStores], which
     * wipes only the seeded tables (the same [SqliteDictionaryStore.resetBundledWords] path the bundled
     * languages already use), leaving the learned overlay intact. When the picked archive turns out not to
     * be newer (the stale-hosted-archive case), [InstalledLanguagesStore.suppressCatalogVersion] records the
     * dismissed catalog version so the "update available" hint does not reappear until the catalog itself
     * moves past it in a future app release.
     *
     * D-386/D-413: now called already on the picker callback's own background thread (never spawns its own),
     * and deletes the picked archive afterward when it is recent enough ([DownloadFileSupport.
     * deleteIfRecentlyCreated]) - on every outcome except a hard read/parse failure, so a stale archive that
     * genuinely failed to import is left behind for inspection rather than silently destroyed.
     *
     * @param uri the resolved archive to import
     * @param entry the row whose Import button was tapped - only [LanguagePackCatalog.Entry.language] is
     *        actually used for the install itself; [LanguagePackCatalog.Entry.version] is not consulted for
     *        the apply-or-skip decision, only recorded as the suppressed version on a skip
     */
    private fun importPack(uri: Uri, entry: LanguagePackCatalog.Entry) {
        val language = entry.language
        val alreadyInstalled = language in InstalledLanguagesStore.load(this)
        val result = runCatching {
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "cannot open $uri" }
                val pack = LanguagePackInstaller.parse(input, language)
                val installedVersion = if (alreadyInstalled) InstalledLanguagesStore.installedVersion(this, language) else null
                val check = LanguagePackInstaller.compareVersions(pack.version, installedVersion)
                when (check) {
                    LanguagePackInstaller.VersionCheck.INSTALL -> {
                        LanguagePackInstaller.write(LanguagePackStorage.packDir(this), pack)
                        InstalledLanguagesStore.add(this, language, pack.version)
                    }
                    LanguagePackInstaller.VersionCheck.ALREADY_CURRENT, LanguagePackInstaller.VersionCheck.OLDER_THAN_INSTALLED -> {
                        // D-334: the hosted archive is stale relative to the catalog's claimed version (or,
                        // D-386-followup, older than what's installed) - suppress the "update available"
                        // hint for this exact catalog version so it does not reappear on every rebuild()
                        // until a future app release raises the catalog further.
                        InstalledLanguagesStore.suppressCatalogVersion(this, language, entry.version)
                    }
                }
                check
            }
        }
        if (!result.isFailure) {
            DownloadFileSupport.deleteIfRecentlyCreated(this, uri, DownloadFileSupport.DELETE_MAX_AGE_MILLIS)
        }
        runOnUiThread {
            setBusy(false)
            val mismatch = result.exceptionOrNull() as? LanguagePackInstaller.LanguageMismatchException
            val message = when {
                mismatch != null -> getString(R.string.d280_language_mismatch, mismatch.declaredCode, mismatch.expectedCode)
                result.isFailure -> getString(R.string.d280_import_failed)
                result.getOrNull() == LanguagePackInstaller.VersionCheck.OLDER_THAN_INSTALLED -> getString(R.string.d280_older_than_installed)
                result.getOrNull() == LanguagePackInstaller.VersionCheck.ALREADY_CURRENT -> getString(R.string.d280_already_current)
                else -> getString(R.string.d280_imported)
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            rebuild()
        }
    }
    
    /**
     * D-454: a simple confirmation before [removePack] actually runs - mirrors [BlacklistActivity]'s own
     * [AlertDialog] pattern for the identical "the button itself is the only warning otherwise" concern.
     */
    private fun confirmRemove(language: Language) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.d280_remove_confirm_title, language.endonym))
            .setPositiveButton(R.string.d280_remove) { _, _ -> removePack(language) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
    
    companion object {
        
        /**
         * D-454: one editorially-chosen flag per language (two for [Language.ENGLISH], the user's own
         * explicit exception - see [flagFor]'s own KDoc). Not present here means no flag is shown
         * ([flagFor] falls back to an empty string) - every [Language] this app currently ships a layout or
         * pack for is covered; a future new language needs its own entry added here too.
         */
        private val LANGUAGE_FLAGS: Map<Language, String> = mapOf(
            Language.GERMAN to "🇩🇪",
            Language.ENGLISH to "🇬🇧🇺🇸",
            Language.GREEK to "🇬🇷",
            Language.FRENCH to "🇫🇷",
            Language.SPANISH to "🇪🇸",
            Language.ITALIAN to "🇮🇹",
            Language.DUTCH to "🇳🇱",
            Language.PORTUGUESE to "🇵🇹",
            Language.POLISH to "🇵🇱",
            Language.TURKISH to "🇹🇷",
            Language.SWEDISH to "🇸🇪",
            Language.NORWEGIAN to "🇳🇴",
            Language.DANISH to "🇩🇰",
            Language.FINNISH to "🇫🇮",
            Language.CZECH to "🇨🇿",
            Language.SLOVAK to "🇸🇰",
            Language.HUNGARIAN to "🇭🇺",
            Language.ROMANIAN to "🇷🇴",
            Language.CROATIAN to "🇭🇷",
            Language.BOSNIAN to "🇧🇦",
            Language.SERBIAN to "🇷🇸",
            Language.ESTONIAN to "🇪🇪",
            Language.LATVIAN to "🇱🇻",
            Language.LITHUANIAN to "🇱🇹",
            Language.INDONESIAN to "🇮🇩",
            Language.MALAY to "🇲🇾",
            // Swahili is the sole official national language across the whole of Tanzania (unlike Kenya,
            // where it shares that role with English) - the more defensible single-country pick of the two.
            Language.SWAHILI to "🇹🇿",
            Language.TAGALOG to "🇵🇭",
            Language.RUSSIAN to "🇷🇺",
            Language.UKRAINIAN to "🇺🇦",
            Language.AZERBAIJANI to "🇦🇿",
            Language.UZBEK to "🇺🇿"
        )
    }
}
