package de.froehlichmedia.adaptkey

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationContext
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationEngine
import de.froehlichmedia.adaptkey.capitalisation.CapsMode
import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider
import de.froehlichmedia.adaptkey.dictionary.SeedData
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.settings.AdaptSettings
import de.froehlichmedia.adaptkey.settings.SettingsStore
import de.froehlichmedia.adaptkey.suggestion.SuggestionBarView
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig
import de.froehlichmedia.adaptkey.suggestion.SuggestionController
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore

/**
 * AdaptKey input method.
 *
 * Letters accumulate in a composing token; on a delimiter the token is finalised by applying the
 * pending autocorrect (A-01: known words are never overwritten) and the capitalisation hierarchy
 * (§6, using the dictionary's part-of-speech tags), then committed. A single backspace tap directly
 * after such a commit restores the originally typed word (A-07). Raw taps continuously train the
 * personal offset model (T-03). Suggestions follow the stabilisation policy (S-01 … S-06) and are
 * fed by the SQLite personal dictionary (tier-1 n-gram), which learns committed words.
 *
 * Retroactive split/merge (A-05 / A-06, depending on the T-05 ambiguity bands), the emoji / numeric
 * panel (L-03) and gestures are not part of this stage yet.
 */
class AdaptKeyService : InputMethodService() {
    
    private var keyboardView: AdaptKeyboardView? = null
    private var suggestionBar: SuggestionBarView? = null
    private var offsetModel: OffsetModel? = null
    
    private var settings = AdaptSettings.DEFAULT
    private var config = SuggestionConfig()
    private var controller = SuggestionController(config)
    private lateinit var dictionaryStore: DictionaryStore
    private lateinit var provider: SuggestionProvider
    private lateinit var capitalisation: CapitalisationEngine
    private var previousWord: String? = null
    
    // C-07 (shift grace window) is persisted and available via settings.shiftGraceWindowMs; the
    // consuming shift-grace logic does not exist yet, so nothing reads it for now.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        settings = SettingsStore.load(this)
        applySettings()
    }
    
    private val composing = StringBuilder()
    private var capsMode = CapsMode.NONE
    private var tokenSentenceStart = false
    private var tokenAfterHyphen = false
    
    // A-07 post-commit autocorrect undo state, armed only for the keystroke directly after a commit.
    private var undoTyped: String? = null
    private var undoCommitted = ""
    private var undoDelimiter = ""
    
    private val handler = Handler(Looper.getMainLooper())
    private val resortRunnable = Runnable {
        controller.resort()
        showSuggestions()
    }
    
    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore.load(this)
        config = settings.suggestionConfig
        controller = SuggestionController(config)
        offsetModel = OffsetStore.load(this)
        val store = SqliteDictionaryStore(this)
        if (store.isEmpty()) {
            SeedData.seed(store)
        }
        dictionaryStore = store
        provider = DictionarySuggestionProvider(store, config.maxSuggestions * 2)
        capitalisation = CapitalisationEngine(store)
        SettingsStore.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    override fun onCreateInputView(): View {
        val view = AdaptKeyboardView(this)
        view.offsetModel = offsetModel
        view.onKeyListener = AdaptKeyboardView.OnKeyListener { key, _, _ -> handleKey(key) }
        view.onLongPressListener = AdaptKeyboardView.OnLongPressListener { symbol -> handleLongPress(symbol) }
        keyboardView = view
        applySettings()
        return view
    }
    
    /**
     * Reloads the persisted configuration (C-01 … C-09) and pushes it into the live keyboard: the view
     * proportions / number row / corner hints, and the suggestion pipeline. The suggestion controller
     * and provider are only rebuilt when the relevant config actually changed, so applying settings
     * mid-session is cheap and does not disturb an in-progress token.
     */
    private fun applySettings() {
        val s = settings
        if (config != s.suggestionConfig) {
            config = s.suggestionConfig
            controller = SuggestionController(config)
            if (this::dictionaryStore.isInitialized) {
                provider = DictionarySuggestionProvider(dictionaryStore, config.maxSuggestions * 2)
            }
        }
        keyboardView?.let { view ->
            view.proportions = s.keyProportions
            view.showNumberRow = s.showNumberRow
            view.letterHints = s.letterHints
            view.hintsEnabled = s.hintsEnabled
        }
    }
    
    override fun onCreateCandidatesView(): View {
        val bar = SuggestionBarView(this)
        bar.onItemClick = SuggestionBarView.OnItemClickListener { item -> onSuggestionClicked(item) }
        suggestionBar = bar
        return bar
    }
    
    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        composing.setLength(0)
        previousWord = null
        capsMode = capsModeFor(info)
        clearUndo()
        keyboardView?.shifted = false
        clearSuggestions()
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Pick up any changes made in the settings screen since the keyboard was last shown.
        settings = SettingsStore.load(this)
        applySettings()
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        composing.setLength(0)
        clearSuggestions()
        // Persist what the model learned while this field was focused (T-03).
        offsetModel?.let { OffsetStore.save(this, it) }
    }
    
    override fun onDestroy() {
        handler.removeCallbacks(resortRunnable)
        SettingsStore.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        offsetModel?.let { OffsetStore.save(this, it) }
        super.onDestroy()
    }
    
    private fun handleKey(key: Key) {
        val ic = currentInputConnection ?: return
        // A-07: a plain backspace tap immediately after an autocorrect commit restores the typed word.
        if (undoTyped != null) {
            if (key.code == KeyCode.DELETE) {
                performAutocorrectUndo(ic)
                return
            }
            clearUndo()
        }
        when (key.code) {
            KeyCode.CHAR -> {
                val raw = key.char ?: return
                if (raw.isLetter()) {
                    if (composing.isEmpty()) {
                        captureTokenContext(ic)
                    }
                    val ch = if (keyboardView?.shifted == true) raw.uppercaseChar() else raw
                    composing.append(ch)
                    consumeShift()
                    updateComposing(ic)
                    refreshSuggestions()
                } else {
                    // Digits and punctuation are delimiters; they finalise the current token.
                    finalizeAndCommit(ic, raw.toString())
                }
            }
            
            KeyCode.SPACE -> finalizeAndCommit(ic, " ")
            
            KeyCode.ENTER -> finalizeAndCommit(ic, "\n")
            
            KeyCode.DELETE -> handleBackspace(ic)
            
            KeyCode.SHIFT -> keyboardView?.let { it.shifted = !it.shifted }
            
            // L-03 stub: the combined emoji / ?123 key has no panel yet.
            KeyCode.SYMBOL -> Unit
        }
    }
    
    /**
     * Handles a long-press secondary symbol (L-05 / L-06): finalises the current token (so a held key
     * mid-word commits the word first) and then commits the symbol, exactly like typing a delimiter.
     */
    private fun handleLongPress(symbol: String) {
        val ic = currentInputConnection ?: return
        clearUndo()
        finalizeAndCommit(ic, symbol)
    }
    
    private fun handleBackspace(ic: InputConnection) {
        if (composing.isNotEmpty()) {
            composing.setLength(composing.length - 1)
            if (composing.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
                clearSuggestions()
            } else {
                updateComposing(ic)
                refreshSuggestions()
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }
    
    /**
     * Finalises the composing token: applies the pending autocorrect and the capitalisation
     * hierarchy, commits the result followed by [delimiter], learns the committed word and arms the
     * A-07 undo when the committed form differs from what was typed.
     */
    private fun finalizeAndCommit(ic: InputConnection, delimiter: String) {
        if (composing.isEmpty()) {
            ic.commitText(delimiter, 1)
            clearUndo()
            clearSuggestions()
            return
        }
        val typed = composing.toString()
        val corrected = provider.autocorrectFor(typed, previousWord) ?: typed // A-01 enforced in provider
        val finalWord = capitalisation.capitalise(corrected, contextFor(typed))
        
        ic.setComposingText(finalWord, 1)
        ic.finishComposingText()
        composing.setLength(0)
        ic.commitText(delimiter, 1)
        learnWord(finalWord)
        
        if (finalWord != typed) {
            undoTyped = typed
            undoCommitted = finalWord
            undoDelimiter = delimiter
        } else {
            clearUndo()
        }
        clearSuggestions()
    }
    
    private fun performAutocorrectUndo(ic: InputConnection) {
        val typed = undoTyped ?: return
        ic.deleteSurroundingText(undoCommitted.length + undoDelimiter.length, 0)
        ic.commitText(typed + undoDelimiter, 1)
        previousWord = typed
        clearUndo()
        clearSuggestions()
    }
    
    private fun updateComposing(ic: InputConnection) {
        val text = composing.toString()
        // S-05: highlight a recognised, complete word via a coloured composing span (toggleable, C-04).
        if (config.highlightEnabled && provider.isKnownWord(text)) {
            val span = SpannableString(text)
            span.setSpan(BackgroundColorSpan(config.highlightColor), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ic.setComposingText(span, 1)
        } else {
            ic.setComposingText(text, 1)
        }
    }
    
    private fun refreshSuggestions() {
        val input = composing.toString()
        if (input.isEmpty()) {
            clearSuggestions()
            return
        }
        val candidates = provider.suggestionsFor(input, previousWord)
        val pending = provider.autocorrectFor(input, previousWord)
        controller.update(input, candidates, pending)
        showSuggestions()
        scheduleResort()
    }
    
    private fun scheduleResort() {
        handler.removeCallbacks(resortRunnable)
        // S-04: 0 ms means immediate re-sorting; otherwise re-sort after the configured input pause.
        if (config.reSortDelayMs == 0L) {
            controller.resort()
            showSuggestions()
        } else {
            handler.postDelayed(resortRunnable, config.reSortDelayMs)
        }
    }
    
    private fun showSuggestions() {
        val items = controller.displayed()
        suggestionBar?.setItems(items)
        setCandidatesViewShown(items.isNotEmpty())
    }
    
    private fun clearSuggestions() {
        handler.removeCallbacks(resortRunnable)
        controller.clear()
        suggestionBar?.setItems(emptyList())
        setCandidatesViewShown(false)
    }
    
    private fun learnWord(word: String?) {
        // Adaptive learning: only learn pure-letter tokens; updates the n-gram context (tier 1).
        if (word.isNullOrEmpty() || !word.all { it.isLetter() }) {
            return
        }
        dictionaryStore.learn(word, previousWord)
        previousWord = word
    }
    
    private fun onSuggestionClicked(item: SuggestionController.DisplayItem) {
        val ic = currentInputConnection ?: return
        clearUndo()
        when (item.kind) {
            // S-06: keep exactly what was typed and cancel the pending autocorrect for this occurrence.
            SuggestionController.Kind.VERBATIM -> {
                val typed = composing.toString()
                ic.finishComposingText()
                composing.setLength(0)
                controller.declineAutocorrect()
                // S-06: repeated verbatim confirmation is a learning signal (cf. B-03).
                learnWord(typed)
                clearSuggestions()
            }
            
            // Complete the token with the chosen word (cased per §6) and start a new one.
            SuggestionController.Kind.NORMAL -> {
                val word = capitalisation.capitalise(item.word, contextFor(composing.toString()))
                ic.commitText(word + " ", 1)
                composing.setLength(0)
                learnWord(word)
                clearSuggestions()
            }
        }
    }
    
    private fun captureTokenContext(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0)?.toString() ?: ""
        tokenSentenceStart = before.isBlank() || endsAtSentenceBoundary(before)
        tokenAfterHyphen = before.endsWith("-")
    }
    
    private fun contextFor(typed: String): CapitalisationContext {
        return CapitalisationContext(
            explicitFirstUpper = typed.firstOrNull()?.isUpperCase() == true,
            sentenceStart = tokenSentenceStart,
            capsMode = capsMode,
            afterHyphen = tokenAfterHyphen
        )
    }
    
    private fun consumeShift() {
        if (keyboardView?.shifted == true) {
            keyboardView?.shifted = false
        }
    }
    
    private fun clearUndo() {
        undoTyped = null
        undoCommitted = ""
        undoDelimiter = ""
    }
    
    private fun capsModeFor(info: EditorInfo?): CapsMode {
        val type = info?.inputType ?: return CapsMode.NONE
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
            return CapsMode.NONE
        }
        return when {
            type and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0 -> CapsMode.CHARACTERS
            type and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0 -> CapsMode.WORDS
            type and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 -> CapsMode.SENTENCES
            else -> CapsMode.NONE
        }
    }
    
    private fun endsAtSentenceBoundary(before: String): Boolean {
        // Sentence start = previous text ends with '.', '!' or '?' followed by whitespace.
        if (before.isEmpty() || !before.last().isWhitespace()) {
            return false
        }
        val trimmed = before.trimEnd()
        val last = trimmed.lastOrNull() ?: return false
        return last == '.' || last == '!' || last == '?'
    }
    
    companion object {
        
        private const val MAX_CONTEXT_LOOKBACK = 80
    }
}
