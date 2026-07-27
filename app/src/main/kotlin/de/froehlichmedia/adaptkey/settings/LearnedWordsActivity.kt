// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
 *
 * D-292: tapping an entry now also lets its own casing be corrected in place ([SqliteDictionaryStore.
 * recaseLearnedWord]) - deliberately restricted to a casing-only edit (Save stays disabled unless the edited
 * text is case-insensitively identical to the original), so this can never be used to sneak an entirely
 * different word into the learned lexicon under someone else's frequency/history; a genuinely different word
 * still has to be typed and learned normally.
 */
class LearnedWordsActivity : AppCompatActivity() {
    
    private lateinit var store: SqliteDictionaryStore
    private var language: Language = Language.ENGLISH
    // D-280: the selectable languages - English (always bundled) plus whatever is currently installed -
    // resolved once in onCreate() rather than DictionaryLoader's own fixed constant, since which languages
    // exist is now a runtime, per-device fact rather than a fixed 3-entry list.
    private lateinit var languages: List<Language>
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private val words = ArrayList<WordEntry>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learned_words)
        title = getString(R.string.learned_words_title)
        
        // D-188: same edge-to-edge inset fix as CalibrationActivity's own K-01 fix (§13) - see
        // BlacklistActivity's identical block for the full reasoning.
        val root = findViewById<View>(R.id.learned_words_root)
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
        
        languages = DictionaryLoader.activeLanguages(this)
        openStore(language)
        listView = findViewById(R.id.learned_words_list)
        emptyView = findViewById(R.id.learned_words_empty)
        
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        listView.adapter = adapter
        
        val languageSpinner = findViewById<Spinner>(R.id.learned_words_language_spinner)
        languageSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages.map { languageName(it) }
        )
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = languages[position]
                if (selected != language) {
                    openStore(selected)
                    refresh()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        
        listView.setOnItemClickListener { _, _, position, _ -> showEntryDialog(words[position]) }
        
        refresh()
    }
    
    /**
     * D-292: the per-entry dialog - an editable casing field (Save/Cancel, the dialog's own built-in
     * buttons) plus Copy/Forget as two extra buttons inside the custom view, since [AlertDialog.Builder]
     * itself only ever offers three button slots and this dialog needs four actions.
     *
     * @param entry the learned-word entry tapped
     */
    private fun showEntryDialog(entry: WordEntry) {
        lateinit var dialog: AlertDialog
        val editText = EditText(this).apply {
            setText(entry.word)
            setSelection(text.length)
        }
        val hint = TextView(this).apply {
            text = getString(R.string.learned_words_edit_hint)
            setPadding(0, dp(4), 0, dp(12))
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(this@LearnedWordsActivity).apply {
                text = getString(R.string.copy_to_clipboard_action)
                setOnClickListener { copyToClipboard(editText.text.toString()) }
            })
            addView(Button(this@LearnedWordsActivity).apply {
                text = getString(R.string.learned_words_remove_confirm_action)
                setOnClickListener {
                    // D-177: mirrors AdaptKeyService.onBlacklistWord()'s own learned-word branch exactly -
                    // every word listed here is by definition already in the learned lexicon (learnedWords()
                    // itself never returns a bundled one), so there is no isBundledWord() branch to make here.
                    store.forget(entry.word)
                    store.markPendingBlacklist(entry.word, System.currentTimeMillis())
                    Toast.makeText(this@LearnedWordsActivity, getString(R.string.learned_words_removed, entry.word), Toast.LENGTH_SHORT).show()
                    refresh()
                    dialog.dismiss()
                }
            })
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dp(20)
            setPadding(padding, dp(8), padding, 0)
            addView(editText)
            addView(hint)
            addView(actions)
        }
        dialog = AlertDialog.Builder(this)
            .setTitle(entry.word)
            .setView(container)
            .setPositiveButton(R.string.learned_words_save_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            fun updateSaveEnabled() {
                // D-292: case-insensitively identical to the original only - the whole point is that this
                // can fix casing alone, never sneak an entirely different word in under this entry's own
                // frequency/history.
                saveButton.isEnabled = editText.text.toString().equals(entry.word, ignoreCase = true)
            }
            updateSaveEnabled()
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) = updateSaveEnabled()
            })
            saveButton.setOnClickListener {
                store.recaseLearnedWord(entry.word, editText.text.toString())
                refresh()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    
    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.learned_words_title), value))
        Toast.makeText(this, getString(R.string.copy_to_clipboard_done, value), Toast.LENGTH_SHORT).show()
    }
    
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    
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
    private fun languageName(language: Language): String = language.endonym
    
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
