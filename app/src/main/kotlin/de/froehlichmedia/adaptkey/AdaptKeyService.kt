package de.froehlichmedia.adaptkey

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.suggestion.StubSuggestionProvider
import de.froehlichmedia.adaptkey.suggestion.SuggestionBarView
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig
import de.froehlichmedia.adaptkey.suggestion.SuggestionController
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore

/**
 * AdaptKey input method.
 *
 * Provides a self-drawn QWERTZ keyboard (L-01). Letters are accumulated in a composing token so
 * suggestions can complete or replace it; delimiters finalise the token. Raw taps continuously
 * train the personal offset model (T-03), which is persisted across sessions. The suggestion bar
 * applies the stabilisation policy (S-01 … S-06) and is fed by a placeholder provider until the
 * dictionary and prediction sessions land. The autocorrect commit policy (applying a pending
 * replacement on a delimiter), the emoji / numeric panel (L-03) and gestures are not part of this
 * stage yet.
 */
class AdaptKeyService : InputMethodService() {
    
    private var keyboardView: AdaptKeyboardView? = null
    private var suggestionBar: SuggestionBarView? = null
    private var offsetModel: OffsetModel? = null
    
    private val config = SuggestionConfig()
    private val provider: SuggestionProvider = StubSuggestionProvider()
    private val controller = SuggestionController(config)
    
    private val composing = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private val resortRunnable = Runnable {
        controller.resort()
        showSuggestions()
    }
    
    override fun onCreate() {
        super.onCreate()
        offsetModel = OffsetStore.load(this)
    }
    
    override fun onCreateInputView(): View {
        val view = AdaptKeyboardView(this)
        view.offsetModel = offsetModel
        view.onKeyListener = AdaptKeyboardView.OnKeyListener { key, _, _ -> handleKey(key) }
        keyboardView = view
        return view
    }
    
    override fun onCreateCandidatesView(): View {
        val bar = SuggestionBarView(this)
        bar.onItemClick = SuggestionBarView.OnItemClickListener { item -> onSuggestionClicked(item) }
        suggestionBar = bar
        return bar
    }
    
    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // Reset transient state on every new field; auto-shift heuristics arrive in a later session.
        composing.setLength(0)
        keyboardView?.shifted = false
        clearSuggestions()
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
        offsetModel?.let { OffsetStore.save(this, it) }
        super.onDestroy()
    }
    
    private fun handleKey(key: Key) {
        val ic = currentInputConnection ?: return
        when (key.code) {
            KeyCode.CHAR -> {
                val raw = key.char ?: return
                if (raw.isLetter()) {
                    val ch = if (keyboardView?.shifted == true) raw.uppercaseChar() else raw
                    composing.append(ch)
                    consumeShift()
                    updateComposing(ic)
                    refreshSuggestions()
                } else {
                    // Digits and punctuation finalise the current token and commit verbatim.
                    finishComposing(ic)
                    ic.commitText(raw.toString(), 1)
                    clearSuggestions()
                }
            }
            
            KeyCode.SPACE -> {
                finishComposing(ic)
                ic.commitText(" ", 1)
                clearSuggestions()
            }
            
            KeyCode.ENTER -> {
                finishComposing(ic)
                ic.commitText("\n", 1)
                clearSuggestions()
            }
            
            KeyCode.DELETE -> handleBackspace(ic)
            
            KeyCode.SHIFT -> keyboardView?.let { it.shifted = !it.shifted }
            
            // L-03 stub: the combined emoji / ?123 key has no panel yet.
            KeyCode.SYMBOL -> Unit
        }
    }
    
    private fun handleBackspace(ic: android.view.inputmethod.InputConnection) {
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
    
    private fun updateComposing(ic: android.view.inputmethod.InputConnection) {
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
        val candidates = provider.suggestionsFor(input)
        val pending = provider.autocorrectFor(input)
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
    
    private fun finishComposing(ic: android.view.inputmethod.InputConnection) {
        if (composing.isNotEmpty()) {
            ic.finishComposingText()
            composing.setLength(0)
        }
    }
    
    private fun onSuggestionClicked(item: SuggestionController.DisplayItem) {
        val ic = currentInputConnection ?: return
        when (item.kind) {
            // S-06: keep exactly what was typed and cancel the pending autocorrect for this occurrence.
            SuggestionController.Kind.VERBATIM -> {
                ic.finishComposingText()
                composing.setLength(0)
                controller.declineAutocorrect()
                clearSuggestions()
            }
            
            // Complete the token with the chosen word and start a new one.
            SuggestionController.Kind.NORMAL -> {
                ic.commitText(item.word + " ", 1)
                composing.setLength(0)
                clearSuggestions()
            }
        }
    }
    
    private fun consumeShift() {
        if (keyboardView?.shifted == true) {
            keyboardView?.shifted = false
        }
    }
}
