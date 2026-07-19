// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.dictionary.WordEntry
import de.froehlichmedia.adaptkey.language.Language

/**
 * D-177: lists the words the keyboard has learned purely from the user's own typing (never the bundled
 * dictionary asset - see [SqliteDictionaryStore]'s own KDoc for the split), so any of them can be removed
 * directly - including one that could never be reached via the ordinary G-04 drag-to-trash gesture at all,
 * because it happens to match the current input and S-02 then never shows it as its own suggestion.
 *
 * Removing an entry here runs the exact same action as G-04's own learned-word branch
 * ([de.froehlichmedia.adaptkey.AdaptKeyService.onBlacklistWord]): forget it outright, then mark it
 * provisionally pending rather than blacklisting it immediately - see [SqliteDictionaryStore.markPendingBlacklist]'s
 * own KDoc for why. Structurally mirrors [BlacklistActivity] (language spinner, list, tap-to-remove with
 * confirmation); backed directly by SQLite, so - like the other Android-facing store layers - it is covered
 * by instrumented rather than unit tests.
 */
class LearnedWordsActivity : AppCompatActivity() {
    
    private lateinit var store: SqliteDictionaryStore
    private var language: Language = DictionaryLoader.LANGUAGES.first()
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private val words = ArrayList<WordEntry>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learned_words)
        title = getString(R.string.learned_words_title)
        
        openStore(language)
        listView = findViewById(R.id.learned_words_list)
        emptyView = findViewById(R.id.learned_words_empty)
        
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        listView.adapter = adapter
        
        val languageSpinner = findViewById<Spinner>(R.id.learned_words_language_spinner)
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
        
        listView.setOnItemClickListener { _, _, position, _ -> confirmRemove(words[position]) }
        
        refresh()
    }
    
    private fun confirmRemove(entry: WordEntry) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.learned_words_remove_confirm_title, entry.word))
            .setPositiveButton(R.string.learned_words_remove_confirm_action) { _, _ ->
                // D-177: mirrors AdaptKeyService.onBlacklistWord()'s own learned-word branch exactly - every
                // word listed here is by definition already in the learned lexicon (learnedWords() itself
                // never returns a bundled one), so there is no isBundledWord() branch to make here at all.
                store.forget(entry.word)
                store.markPendingBlacklist(entry.word, System.currentTimeMillis())
                Toast.makeText(this, getString(R.string.learned_words_removed, entry.word), Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    /**
     * (Re)opens the SQLite store for [language], closing any previously open one. The store name matches
     * the one the running keyboard uses for that language ([DictionaryLoader]), so edits take effect there.
     *
     * @param language the language whose learned words to edit
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
        words.addAll(store.learnedWords())
        adapter.clear()
        adapter.addAll(words.map { entry -> "${entry.word}  (${entry.frequency})" })
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (words.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
    
    override fun onDestroy() {
        store.close()
        super.onDestroy()
    }
}
