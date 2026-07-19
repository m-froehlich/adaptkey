// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)
        title = getString(R.string.blacklist_title)
        
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
            .show()
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
        words.addAll(store.blacklistedWords())
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
