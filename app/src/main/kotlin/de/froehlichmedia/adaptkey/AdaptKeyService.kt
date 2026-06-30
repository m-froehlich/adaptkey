package de.froehlichmedia.adaptkey

import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import de.froehlichmedia.adaptkey.capitalisation.ShiftGrace
import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider
import de.froehlichmedia.adaptkey.dictionary.SeedData
import de.froehlichmedia.adaptkey.dictionary.SplitResult
import de.froehlichmedia.adaptkey.dictionary.SqliteDictionaryStore
import de.froehlichmedia.adaptkey.dictionary.TokenRepair
import de.froehlichmedia.adaptkey.gesture.GestureAction
import de.froehlichmedia.adaptkey.gesture.KeyGesture
import de.froehlichmedia.adaptkey.gesture.SwipeDirection
import de.froehlichmedia.adaptkey.gesture.WordBoundary
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.settings.AdaptSettings
import de.froehlichmedia.adaptkey.settings.SettingsStore
import de.froehlichmedia.adaptkey.suggestion.SuggestionBarView
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig
import de.froehlichmedia.adaptkey.suggestion.SuggestionController
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider
import de.froehlichmedia.adaptkey.touch.AmbiguityResult
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.TapAmbiguity

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
 * Taps in the bottom-row space/letter ambiguity bands (T-05) are flagged per character; on a delimiter
 * the token may be retroactively split (A-05) or merged onto a spurious space (A-06) when the
 * dictionary confirms a valid result. Swipe gestures (§4) are handled too: swipe-left on backspace
 * deletes a word (G-02), swipe-down dismisses the keyboard (G-03), and a horizontal swipe on the space
 * bar is the language-switch gesture (G-01, a no-op stub until multilingual input exists). The emoji /
 * numeric panel (L-03) is not part of this stage yet.
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
    private lateinit var tokenRepair: TokenRepair
    private var previousWord: String? = null
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        settings = SettingsStore.load(this)
        applySettings()
    }
    
    private val composing = StringBuilder()
    private val composingFlags = ArrayList<TapAmbiguity>()
    private var capsMode = CapsMode.NONE
    private var tokenSentenceStart = false
    private var tokenAfterHyphen = false
    
    // T-05 / A-06: the letter inferred for the most recently committed letter-ambiguous space; armed for
    // the immediately following token, which may be merged back onto it (e.g. "aber  ald" -> "aber bald").
    private var pendingMergeChar: Char? = null
    
    // C-07: shift-grace state. The current word start may be auto-armed to uppercase by a field mandate
    // (§6); shiftGuardedArm marks a "surprising" mid-sentence arm whose disarming Shift press is ignored
    // during settings.shiftGraceWindowMs (measured from shiftArmTime). A press accepted after the window
    // that lowers a guarded arm is a deliberate override and neutralises the mandate for this token.
    private var shiftArmTime = 0L
    private var shiftGuardedArm = false
    private var fieldMandateOverridden = false
    
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
        tokenRepair = TokenRepair(store)
        SettingsStore.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    override fun onCreateInputView(): View {
        val view = AdaptKeyboardView(this)
        view.offsetModel = offsetModel
        view.onKeyListener = AdaptKeyboardView.OnKeyListener { key, _, _, ambiguity -> handleKey(key, ambiguity) }
        view.onLongPressListener = AdaptKeyboardView.OnLongPressListener { symbol -> handleLongPress(symbol) }
        view.onSwipeListener = AdaptKeyboardView.OnSwipeListener { key, direction -> handleSwipe(key, direction) }
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
        composingFlags.clear()
        pendingMergeChar = null
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
        currentInputConnection?.let { armShiftForNextWord(it) }
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
    
    private fun handleKey(key: Key, ambiguity: AmbiguityResult) {
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
                    // T-05: retain this letter's space-ambiguity flag in step with the composing token (A-05).
                    composingFlags.add(ambiguity.kind)
                    consumeShift()
                    updateComposing(ic)
                    refreshSuggestions()
                } else {
                    // Digits and punctuation are delimiters; they finalise the current token.
                    finalizeAndCommit(ic, raw.toString())
                }
            }
            
            KeyCode.SPACE -> {
                // T-05: a space tapped in the upper band carries the letter inferred for a possible merge (A-06).
                val inferred = ambiguity.inferredChar.takeIf { ambiguity.kind == TapAmbiguity.LETTER_AMBIGUOUS }
                finalizeAndCommit(ic, " ", inferred)
            }
            
            KeyCode.ENTER -> finalizeAndCommit(ic, "\n")
            
            KeyCode.DELETE -> handleBackspace(ic)
            
            KeyCode.SHIFT -> handleShift()
            
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
    
    /**
     * Handles a swipe gesture (§4 G-01 … G-03) reported by the keyboard view. Resolves it to an
     * action via [KeyGesture] and executes it.
     *
     * @param key the key the swipe started on (T-01 contact point)
     * @param direction the recognised swipe direction
     * @return true when the swipe carried an action and was consumed (suppressing the tap), false
     *         when it should fall back to a normal tap
     */
    private fun handleSwipe(key: Key, direction: SwipeDirection): Boolean {
        val ic = currentInputConnection ?: return false
        return when (KeyGesture.resolve(key.code, direction)) {
            // G-02: delete the whole previous word.
            GestureAction.DELETE_WORD -> {
                clearUndo()
                deleteWord(ic)
                true
            }
            
            // G-03: dismiss the keyboard.
            GestureAction.DISMISS_KEYBOARD -> {
                requestHideSelf(0)
                true
            }
            
            // G-01: language switch. Multilingual input is not available yet (it needs a second language
            // dictionary, cf. A-03), so the swipe is recognised and consumed but is intentionally a no-op.
            GestureAction.LANGUAGE_PREV, GestureAction.LANGUAGE_NEXT -> true
            
            GestureAction.NONE -> false
        }
    }
    
    /**
     * Deletes the whole previous word (G-02). An in-progress composing token is dropped outright;
     * otherwise the committed previous word (and the whitespace before the cursor) is removed via
     * [WordBoundary].
     */
    private fun deleteWord(ic: InputConnection) {
        pendingMergeChar = null
        if (composing.isNotEmpty()) {
            composing.setLength(0)
            composingFlags.clear()
            ic.setComposingText("", 1)
            ic.finishComposingText()
            clearSuggestions()
        } else {
            val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0) ?: ""
            val count = WordBoundary.wordDeleteLength(before)
            if (count > 0) {
                ic.deleteSurroundingText(count, 0)
            }
        }
        previousWord = null
    }
    
    private fun handleBackspace(ic: InputConnection) {
        // A backspace breaks the immediate context a pending merge (A-06) would have relied on.
        pendingMergeChar = null
        if (composing.isNotEmpty()) {
            if (composingFlags.isNotEmpty()) {
                composingFlags.removeAt(composingFlags.size - 1)
            }
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
     * Finalises the composing token. First the retroactive token-repair rules are tried: a merge back
     * onto a preceding spurious letter-ambiguous space (A-06), then a split at a space-ambiguous tap or
     * a fully missed space (A-05). Failing those, the pending autocorrect and the capitalisation
     * hierarchy are applied; the result is committed followed by [delimiter], the committed word is
     * learned and the A-07 undo is armed when the committed form differs from what was typed.
     *
     * @param spaceInferred when this delimiter is a standalone letter-ambiguous space, the letter
     *        inferred from the tap; armed for a possible merge of the next token (A-06)
     */
    private fun finalizeAndCommit(ic: InputConnection, delimiter: String, spaceInferred: Char? = null) {
        val mergeChar = pendingMergeChar
        pendingMergeChar = null
        
        if (composing.isEmpty()) {
            ic.commitText(delimiter, 1)
            clearUndo()
            clearSuggestions()
            // A standalone letter-ambiguous space is a spurious space the next token may merge back onto.
            pendingMergeChar = spaceInferred
            armShiftForNextWord(ic)
            return
        }
        val typed = composing.toString()
        
        // A-06: merge the token onto a preceding spurious letter-ambiguous space, when linguistically valid.
        if (mergeChar != null) {
            val merged = tokenRepair.tryMerge(previousWord, mergeChar, typed)
            if (merged != null) {
                applyMerge(ic, merged, delimiter)
                armShiftForNextWord(ic)
                return
            }
        }
        
        // A-05: split the token at a space-ambiguous tap or a fully missed space, when valid.
        val split = tokenRepair.trySplit(typed, spaceAmbiguousIndices(), previousWord)
        if (split != null) {
            applySplit(ic, split, delimiter)
            armShiftForNextWord(ic)
            return
        }
        
        val corrected = provider.autocorrectFor(typed, previousWord) ?: typed // A-01 enforced in provider
        val finalWord = capitalisation.capitalise(corrected, contextFor(typed))
        
        ic.setComposingText(finalWord, 1)
        ic.finishComposingText()
        composing.setLength(0)
        composingFlags.clear()
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
        armShiftForNextWord(ic)
    }
    
    /**
     * Applies an A-06 merge: drops the composing token, removes the spurious preceding space and
     * commits the reconstructed word (cased per §6) followed by [delimiter].
     */
    private fun applyMerge(ic: InputConnection, merged: String, delimiter: String) {
        ic.setComposingText("", 1)
        ic.finishComposingText()
        composing.setLength(0)
        composingFlags.clear()
        ic.deleteSurroundingText(1, 0)
        val cased = capitalisation.capitalise(merged, contextFor(merged))
        ic.commitText(cased + delimiter, 1)
        learnWord(cased)
        clearUndo()
        clearSuggestions()
    }
    
    /**
     * Applies an A-05 split: drops the composing token and commits the two words (each cased per §6)
     * separated by a space, followed by [delimiter].
     */
    private fun applySplit(ic: InputConnection, split: SplitResult, delimiter: String) {
        ic.setComposingText("", 1)
        ic.finishComposingText()
        composing.setLength(0)
        composingFlags.clear()
        val left = capitalisation.capitalise(split.left, contextFor(split.left))
        val right = capitalisation.capitalise(split.right, followingPartContext())
        ic.commitText(left + " " + right + delimiter, 1)
        learnWord(left)
        learnWord(right)
        clearUndo()
        clearSuggestions()
    }
    
    private fun spaceAmbiguousIndices(): Set<Int> {
        val indices = HashSet<Int>()
        composingFlags.forEachIndexed { index, flag ->
            if (flag == TapAmbiguity.SPACE_AMBIGUOUS) {
                indices.add(index)
            }
        }
        return indices
    }
    
    private fun performAutocorrectUndo(ic: InputConnection) {
        val typed = undoTyped ?: return
        ic.deleteSurroundingText(undoCommitted.length + undoDelimiter.length, 0)
        ic.commitText(typed + undoDelimiter, 1)
        previousWord = typed
        clearUndo()
        clearSuggestions()
        armShiftForNextWord(ic)
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
        pendingMergeChar = null
        when (item.kind) {
            // S-06: keep exactly what was typed and cancel the pending autocorrect for this occurrence.
            SuggestionController.Kind.VERBATIM -> {
                val typed = composing.toString()
                ic.finishComposingText()
                composing.setLength(0)
                composingFlags.clear()
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
                composingFlags.clear()
                learnWord(word)
                clearSuggestions()
                armShiftForNextWord(ic)
            }
        }
    }
    
    private fun captureTokenContext(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0)?.toString() ?: ""
        tokenSentenceStart = before.isBlank() || endsAtSentenceBoundary(before)
        tokenAfterHyphen = before.endsWith("-")
    }
    
    private fun contextFor(typed: String): CapitalisationContext {
        // C-07: once the user has deliberately overridden a surprising field mandate for this word, the
        // mandate no longer forces an uppercase; the linguistic rules (sentence start, nouns) still apply.
        val effectiveCaps = if (fieldMandateOverridden) CapsMode.NONE else capsMode
        return CapitalisationContext(
            explicitFirstUpper = typed.firstOrNull()?.isUpperCase() == true,
            sentenceStart = tokenSentenceStart,
            capsMode = effectiveCaps,
            afterHyphen = tokenAfterHyphen
        )
    }
    
    /**
     * Capitalisation context for the second word of an A-05 split: never a sentence start or explicit
     * capital, but still subject to any field mandate (§6) unless the user overrode it (C-07).
     *
     * @return the context for a split's following part
     */
    private fun followingPartContext(): CapitalisationContext {
        val effectiveCaps = if (fieldMandateOverridden) CapsMode.NONE else capsMode
        return CapitalisationContext(
            explicitFirstUpper = false,
            sentenceStart = false,
            capsMode = effectiveCaps,
            afterHyphen = false
        )
    }
    
    /**
     * Arms Shift for the word about to start, per the field mandate (§6 / C-07), and resets the grace
     * state: records whether the arm is guarded and the time it was armed, and clears any previous
     * override. Called after every commit and when the input view (re)starts.
     */
    private fun armShiftForNextWord(ic: InputConnection) {
        val sentenceStart = sentenceStartBefore(ic)
        keyboardView?.shifted = ShiftGrace.autoArmAtWordStart(capsMode, sentenceStart)
        shiftGuardedArm = ShiftGrace.isGuardedArm(capsMode, sentenceStart)
        shiftArmTime = SystemClock.uptimeMillis()
        fieldMandateOverridden = false
    }
    
    /**
     * Handles a Shift key press through the C-07 grace guard: ignores a press that would lower a
     * field-mandated uppercase within the grace window, otherwise toggles. A press that deliberately
     * lowers a guarded arm after the window marks the field mandate as overridden for this word.
     */
    private fun handleShift() {
        val view = keyboardView ?: return
        val elapsed = SystemClock.uptimeMillis() - shiftArmTime
        if (ShiftGrace.suppressesShiftPress(shiftGuardedArm, view.shifted, settings.shiftGraceWindowMs, elapsed)) {
            return
        }
        val wasUpper = view.shifted
        view.shifted = !view.shifted
        if (shiftGuardedArm && wasUpper && !view.shifted) {
            fieldMandateOverridden = true
        }
    }
    
    private fun sentenceStartBefore(ic: InputConnection): Boolean {
        val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0)?.toString() ?: ""
        return before.isBlank() || endsAtSentenceBoundary(before)
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
