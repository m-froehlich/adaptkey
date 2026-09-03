// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
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
import de.froehlichmedia.adaptkey.dictionary.LearnedWordEntry
import de.froehlichmedia.adaptkey.dictionary.PartOfSpeech
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.language.ActiveLanguageStore
import de.froehlichmedia.adaptkey.language.Language
import java.text.Collator
import java.util.Date
import java.util.Locale

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
 *
 * D-404: the list itself is consolidated - only entries with no base-form link ([LearnedWordEntry.lemma] ==
 * null) are shown as their own row, so an inflected form the keyboard already linked to a base (e.g.
 * "Hundes" once "Hund" is also known) no longer clutters the list on its own. An entry whose category is
 * still undetermined ("unbekannt") is marked with a trailing asterisk. The same tap-to-edit dialog now also
 * offers a category multi-select and a "Grundform" dropdown, so a power user can review or correct either.
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
    // D-404: every learned word, unfiltered - the "Grundform" dropdown's own candidate list needs every
    // entry, not just the base-level ones [words] itself is filtered down to for display.
    private val allWords = ArrayList<LearnedWordEntry>()
    // D-404: only entries with no base-form link ([LearnedWordEntry.lemma] == null) - see this class's own
    // KDoc.
    private val words = ArrayList<LearnedWordEntry>()
    // D-388: how the list is currently ordered - see the sort spinner in onCreate(). Alphabetical by
    // default (browsing/searching for a specific word, this screen's primary use, per the user's own
    // framing - frequency was explicitly ruled out as a user-facing criterion for this list).
    private var sortMode = SortMode.ALPHA
    
    private enum class SortMode { RECENT, ALPHA }
    
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
        // D-295: defaults to whichever language the keyboard itself currently/last had active, not always
        // the first entry (English) - falls back to the first available language on the vanishingly rare
        // chance the persisted active language's own pack was removed without AdaptKeyService having run
        // since (it would otherwise already have self-corrected ActiveLanguageStore back to English itself,
        // see installStores()'s own KDoc).
        val initialLanguage = ActiveLanguageStore.load(this).takeIf { it in languages } ?: languages.first()
        openStore(initialLanguage)
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
        languageSpinner.setSelection(languages.indexOf(initialLanguage))
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
        
        // D-388: short, user-facing labels only - "Recent" (last touched, newest first) and "A-Z"
        // (alphabetical, case-insensitive/locale-folded via Collator in refresh()). Frequency is
        // deliberately not offered here - see this class's own KDoc update / the design discussion this
        // came out of.
        val sortSpinner = findViewById<Spinner>(R.id.learned_words_sort_spinner)
        sortSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.learned_words_sort_recent), getString(R.string.learned_words_sort_alpha))
        )
        sortSpinner.setSelection(SortMode.entries.indexOf(sortMode))
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = SortMode.entries[position]
                if (selected != sortMode) {
                    sortMode = selected
                    refresh()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        
        listView.setOnItemClickListener { _, _, position, _ -> showEntryDialog(words[position]) }
        
        refresh()
    }
    
    /**
     * D-292/D-294: the per-entry dialog. [AlertDialog.Builder]'s own two built-in buttons are Forget
     * (positive) / Cancel (negative), matching the original single-purpose confirm dialog exactly; Copy and
     * Save moved into the custom view instead, as two compact buttons directly beside the (weighted, so it
     * fills the remaining width) casing field - Save still needs to visibly grey out when disabled, which a
     * real [Button] already does automatically, unlike a hand-rolled icon-only touch target.
     *
     * D-293: the field itself opts out of [de.froehlichmedia.adaptkey.AdaptKeyService]'s own suggestion/
     * learning pipeline entirely via `TYPE_TEXT_FLAG_NO_SUGGESTIONS` - editing a word's casing here must
     * never itself feed back into the dictionary or show a suggestion bar.
     *
     * D-404: also offers a category multi-select ([CATEGORY_CHOICES], one [CheckBox] per tag) and a
     * "Grundform" [Spinner] (every other learned word, plus [R.string.learned_words_lemma_unknown] for "no
     * link") - a power-user-only correction surface for whatever the capitalisation heuristic / conservative
     * lookup-linker got wrong or missed. Both save unconditionally on Save (independent of the casing-only
     * gate below, which only concerns the text field itself).
     *
     * @param entry the learned-word entry tapped
     */
    private fun showEntryDialog(entry: LearnedWordEntry) {
        val editText = EditText(this).apply {
            setText(entry.word)
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        // D-294/D-296: plain emoji glyphs rather than new vector-drawable icon assets - this app has none
        // yet, and an emoji needs no per-locale string resource of its own; the localised action name still
        // carries the accessible label via contentDescription. Square (width == height), not the ordinary
        // wide/padded Button look - borderlessButtonStyle drops the raised Material chrome that would
        // otherwise look wrong squeezed this small, and the explicit square LayoutParams (not WRAP_CONTENT)
        // is what actually guarantees the square shape regardless of the glyph's own measured width.
        val copyButton = squareIconButton(COPY_GLYPH, getString(R.string.copy_to_clipboard_action)).apply {
            setOnClickListener { copyToClipboard(editText.text.toString()) }
        }
        // D-404-followup: Save used to live here too, right next to the text field - moved down into the
        // dialog's own neutral button (see setNeutralButton below), next to Forget/Cancel, after the user
        // kept mis-tapping Forget while reaching for it up here.
        val fieldRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(editText)
            addView(copyButton)
        }
        // D-404-followup: frequency and last-used date, so a power user can tell an entry's real standing
        // (and how stale it is relative to C-24's own expiry window) without leaving this dialog.
        val infoLine = TextView(this).apply {
            text = getString(R.string.learned_words_frequency_label, entry.frequency) + "  ·  " +
                getString(R.string.learned_words_last_used_label, DateFormat.getDateFormat(this@LearnedWordsActivity).format(Date(entry.lastTouched)))
            setPadding(0, dp(8), 0, 0)
        }
        val hint = TextView(this).apply {
            text = getString(R.string.learned_words_edit_hint)
            setPadding(0, dp(4), 0, 0)
        }
        val categoryLabel = TextView(this).apply {
            text = getString(R.string.learned_words_category_label)
            setPadding(0, dp(12), 0, 0)
        }
        val categoryCheckboxes = CATEGORY_CHOICES.associateWith { pos ->
            CheckBox(this).apply {
                text = getString(categoryLabelRes(pos))
                isChecked = pos in entry.partsOfSpeech
            }
        }
        val categoryRows = categoryCheckboxes.values.chunked(CATEGORY_ROW_SIZE).map { rowBoxes ->
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                rowBoxes.forEach { addView(it) }
            }
        }
        val lemmaLabel = TextView(this).apply {
            text = getString(R.string.learned_words_lemma_label)
            setPadding(0, dp(12), 0, 0)
        }
        // D-404: every other learned word (base or not - a mis-linked child can itself be relinked), sorted,
        // with "unbekannt"/"unknown" prepended as the "no link" option at index 0.
        val lemmaChoices = listOf(getString(R.string.learned_words_lemma_unknown)) +
            allWords.filter { it.word != entry.word }.map { it.word }.sorted()
        val lemmaSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@LearnedWordsActivity, android.R.layout.simple_spinner_dropdown_item, lemmaChoices)
            setSelection(entry.lemma?.let { lemmaChoices.indexOf(it) }.takeIf { it != null && it >= 0 } ?: 0)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dp(20)
            setPadding(padding, dp(8), padding, 0)
            addView(fieldRow)
            addView(infoLine)
            addView(hint)
            addView(categoryLabel)
            categoryRows.forEach { addView(it) }
            addView(lemmaLabel)
            addView(lemmaSpinner)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(entry.word)
            .setView(container)
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
            // D-404-followup: Save as the dialog's own neutral button (bottom row, next to Forget/Cancel) -
            // see fieldRow's own note above for why it moved down from the text field.
            .setNeutralButton(R.string.learned_words_save_action) { _, _ ->
                store.recaseLearnedWord(entry.word, editText.text.toString())
                val selectedCategories = categoryCheckboxes.filterValues { it.isChecked }.keys
                store.setLearnedCategories(entry.word, selectedCategories)
                val lemmaSelection = lemmaSpinner.selectedItemPosition
                store.setLearnedLemma(entry.word, if (lemmaSelection <= 0) null else lemmaChoices[lemmaSelection])
                refresh()
            }
            .create()
        fun updateSaveEnabled() {
            // D-292: case-insensitively identical to the original only - the whole point is that this can
            // fix casing alone, never sneak an entirely different word in under this entry's own
            // frequency/history. Does not gate the category/Grundform fields below - those are independent
            // corrections, not a casing edit.
            // D-404-followup: the neutral button only exists once the dialog is actually showing (getButton()
            // returns null before then) - the null-safe call is a no-op until setOnShowListener's own call
            // below runs, which is also why that call exists at all (to set the correct initial state).
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled =
                editText.text.toString().equals(entry.word, ignoreCase = true)
        }
        dialog.setOnShowListener { updateSaveEnabled() }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateSaveEnabled()
        })
        dialog.show()
    }
    
    /**
     * D-404: the localised label for [pos] in the category multi-select.
     *
     * @param pos the tag to label
     * @return its string resource id
     */
    private fun categoryLabelRes(pos: PartOfSpeech): Int = when (pos) {
        PartOfSpeech.NOUN -> R.string.learned_words_category_noun
        PartOfSpeech.VERB -> R.string.learned_words_category_verb
        PartOfSpeech.ADJECTIVE -> R.string.learned_words_category_adjective
        PartOfSpeech.PREPOSITION -> R.string.learned_words_category_preposition
        PartOfSpeech.PROPER_NOUN -> R.string.learned_words_category_proper_noun
        PartOfSpeech.OTHER -> R.string.learned_words_category_other
    }
    
    private fun copyToClipboard(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.learned_words_title), value))
        Toast.makeText(this, getString(R.string.copy_to_clipboard_done, value), Toast.LENGTH_SHORT).show()
    }
    
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    
    /**
     * D-296: a compact, square (width == height) [Button] for a glyph-only action - the ordinary [Button]
     * style's own minimum width/generous padding is exactly what made [showEntryDialog]'s Copy/Save pair
     * needlessly wide; still a real [Button] (not a hand-rolled touch target), so [Button.isEnabled] still
     * greys the glyph out automatically, matching D-294's own reasoning.
     *
     * @param glyph the button's own label (an emoji glyph)
     * @param description the accessible name ([View.setContentDescription]), since the visible label itself
     *        is a glyph, not descriptive text
     * @return the configured button, side length [SQUARE_ICON_BUTTON_SIZE_DP], not yet added to any parent
     */
    private fun squareIconButton(glyph: String, description: String): Button {
        return Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = glyph
            contentDescription = description
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dp(SQUARE_ICON_BUTTON_SIZE_DP), dp(SQUARE_ICON_BUTTON_SIZE_DP))
        }
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
    private fun languageName(language: Language): String = language.endonym
    
    /**
     * D-388: re-reads and re-sorts the list per [sortMode] - "Recent" (newest [LearnedWordEntry.lastTouched]
     * first) or "A-Z" (a [Collator] for the currently open [language], not raw [String] comparison, so
     * umlauts/accents sort at their natural alphabetic position rather than by raw UTF-8 byte value).
     * Frequency is deliberately not shown - it is internal bookkeeping (the promotion/reinforcement count),
     * not something a normal user reviewing this list needs to see.
     *
     * D-404: [words] (what is actually shown) is filtered down to entries with no base-form link - see this
     * class's own KDoc; [allWords] keeps every entry, for the "Grundform" dropdown's own candidate list. An
     * entry with a still-undetermined category ("unbekannt") is shown with a trailing asterisk.
     */
    private fun refresh() {
        allWords.clear()
        allWords.addAll(store.learnedWordsWithTimestamp())
        words.clear()
        words.addAll(allWords.filter { it.lemma == null })
        when (sortMode) {
            SortMode.RECENT -> words.sortByDescending { it.lastTouched }
            SortMode.ALPHA -> {
                val collator = Collator.getInstance(Locale(language.code))
                words.sortWith(compareBy(collator) { it.word })
            }
        }
        adapter.clear()
        adapter.addAll(words.map { entry -> if (entry.partsOfSpeech.isEmpty()) "${entry.word} *" else entry.word })
        adapter.notifyDataSetChanged()
        emptyView.visibility = if (words.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
    
    override fun onDestroy() {
        store.close()
        super.onDestroy()
    }
    
    private companion object {
        private const val COPY_GLYPH = "📋"
        
        // D-296: a standard-sized touch target (matches the Android accessibility guideline minimum),
        // just square instead of the ordinary Button's own wide/padded shape.
        private const val SQUARE_ICON_BUTTON_SIZE_DP = 48
        
        // D-404: every category offered in the edit dialog's multi-select, in a fixed, stable order.
        private val CATEGORY_CHOICES = listOf(
            PartOfSpeech.NOUN,
            PartOfSpeech.VERB,
            PartOfSpeech.ADJECTIVE,
            PartOfSpeech.PREPOSITION,
            PartOfSpeech.PROPER_NOUN,
            PartOfSpeech.OTHER
        )
        
        // D-404: how many category checkboxes share one row in the multi-select.
        private const val CATEGORY_ROW_SIZE = 3
    }
}
