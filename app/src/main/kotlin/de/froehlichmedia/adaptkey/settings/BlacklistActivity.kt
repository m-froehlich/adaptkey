// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.dictionary.BlacklistCategory
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.language.Language

/**
 * Minimal blacklist editor (C-05 / A-04): lists the persisted blacklist, adds a word under a chosen
 * category, and removes an entry on tap. Language-aware - each dictionary language (DE / EN / EL) has its
 * own SQLite store and blacklist, so a language selector picks which one is edited, and it opens the very
 * store the running keyboard uses for that language. Backed directly by SQLite, so - like the other
 * Android-facing store layers - it is covered by instrumented rather than unit tests.
 */
class BlacklistActivity : AppCompatActivity() {
    
    private lateinit var store: SqliteDictionaryStore
    private var language: Language = DictionaryLoader.LANGUAGES.first()
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private val words = ArrayList<String>()
    // D-206: bundled entries (e.g. the archaic-spelling seed) are rarely of interest and should rarely be
    // removed at all - the list starts filtered to user-added entries only; this reveals bundled ones too.
    private var showBundled = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)
        title = getString(R.string.blacklist_title)
        
        // D-188: same edge-to-edge inset fix as CalibrationActivity's own K-01 fix (§13) - Android 15
        // (targetSdk 35) draws this activity edge-to-edge too, so its top/bottom controls (language
        // spinner, add row, list) were sliding under the status bar/display cutout and the gesture-nav
        // area, unusable there. Adds the inset on top of the layout's own static 16dp padding rather than
        // replacing it, so the existing spacing is unchanged everywhere the inset itself is zero.
        val root = findViewById<View>(R.id.blacklist_root)
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
        
        openStore(language)
        listView = findViewById(R.id.blacklist_list)
        emptyView = findViewById(R.id.blacklist_empty)
        
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        listView.adapter = adapter
        
        val languageSpinner = findViewById<Spinner>(R.id.blacklist_language_spinner)
        languageSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            DictionaryLoader.LANGUAGES.map { languageName(it) }
        )
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = DictionaryLoader.LANGUAGES[position]
                if (selected != language) {
                    openStore(selected)
                    refresh()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        
        findViewById<CheckBox>(R.id.blacklist_show_bundled).setOnCheckedChangeListener { _, checked ->
            showBundled = checked
            refresh()
        }
        
        val categorySpinner = findViewById<Spinner>(R.id.blacklist_category)
        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            BlacklistCategory.entries.map { it.name }
        )
        
        val wordField = findViewById<EditText>(R.id.blacklist_word)
        findViewById<Button>(R.id.blacklist_add).setOnClickListener {
            val word = wordField.text.toString().trim()
            if (word.isEmpty()) {
                return@setOnClickListener
            }
            val category = BlacklistCategory.entries[categorySpinner.selectedItemPosition]
            store.blacklist(word, category)
            wordField.text.clear()
            refresh()
        }
        
        listView.setOnItemClickListener { _, _, position, _ -> confirmRemove(words[position]) }
        
        refresh()
    }
    
    private fun confirmRemove(word: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.blacklist_remove_confirm_title, word))
            .setPositiveButton(R.string.blacklist_remove_confirm_action) { _, _ ->
                store.unblacklist(word)
                Toast.makeText(this, getString(R.string.blacklist_removed, word), Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.copy_to_clipboard_action) { _, _ -> copyToClipboard(word) }
            .show()
    }
    
    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.blacklist_title), value))
        Toast.makeText(this, getString(R.string.copy_to_clipboard_done, value), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * (Re)opens the SQLite store for [language], closing any previously open one. The store name matches
     * the one the running keyboard uses for that language ([DictionaryLoader]), so edits take effect there.
     *
     * @param language the language whose blacklist to edit
     */
    private fun openStore(language: Language) {
        if (this::store.isInitialized) {
            store.close()
        }
        this.language = language
        store = SqliteDictionaryStore(this, DictionaryLoader.databaseName(language))
    }
    
    /**
     * The display (endonym) name for a dictionary language in the selector.
     *
     * @param language the language
     * @return its native name
     */
    private fun languageName(language: Language): String {
        return when (language) {
            Language.GREEK -> "Ελληνικά"
            Language.ENGLISH -> "English"
            Language.GERMAN -> "Deutsch"
            else -> language.name
        }
    }
    
    private fun refresh() {
        words.clear()
        // D-206: hide BUNDLED entries by default (showBundled = false) - a bundled entry (e.g. the
        // archaic-spelling seed, or the cross-language confusables of §107/§113) is rarely of interest and
        // should rarely be removed at all, unlike a USER entry a person deliberately added themselves.
        words.addAll(store.blacklistedWords().filter { showBundled || store.blacklistCategory(it) == BlacklistCategory.USER })
        adapter.clear()
        adapter.addAll(words.map { word -> "$word  (${store.blacklistCategory(word)?.name ?: ""})" })
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (words.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
    
    override fun onDestroy() {
        store.close()
        super.onDestroy()
    }
}
