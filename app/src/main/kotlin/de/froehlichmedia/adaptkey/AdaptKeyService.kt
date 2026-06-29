package de.froehlichmedia.adaptkey

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore

/**
 * AdaptKey input method (scaffold).
 *
 * Provides a self-drawn QWERTZ keyboard (L-01) that commits characters straight to the
 * target field via the active [android.view.inputmethod.InputConnection]. Control keys
 * (shift, delete, enter, space) are handled here, and raw taps continuously train the
 * personal offset model (T-03), which is persisted across sessions. The suggestion bar,
 * autocorrect and emoji/symbol panels described in the specification are intentionally not
 * part of this stage yet.
 */
class AdaptKeyService : InputMethodService() {
    
    private var keyboardView: AdaptKeyboardView? = null
    private var offsetModel: OffsetModel? = null
    
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
    
    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // Reset transient state on every new field; auto-shift heuristics arrive in a later session.
        keyboardView?.shifted = false
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        // Persist what the model learned while this field was focused (T-03).
        offsetModel?.let { OffsetStore.save(this, it) }
    }
    
    override fun onDestroy() {
        offsetModel?.let { OffsetStore.save(this, it) }
        super.onDestroy()
    }
    
    private fun handleKey(key: Key) {
        val ic = currentInputConnection ?: return
        when (key.code) {
            KeyCode.CHAR -> {
                val ch = key.char ?: return
                val out = if (keyboardView?.shifted == true) ch.uppercaseChar() else ch
                ic.commitText(out.toString(), 1)
                if (keyboardView?.shifted == true) {
                    keyboardView?.shifted = false
                }
            }
            
            KeyCode.SPACE -> ic.commitText(" ", 1)
            
            KeyCode.DELETE -> ic.deleteSurroundingText(1, 0)
            
            KeyCode.ENTER -> ic.commitText("\n", 1)
            
            KeyCode.SHIFT -> keyboardView?.let { it.shifted = !it.shifted }
            
            // L-03 stub: the combined emoji / ?123 key has no panel yet.
            KeyCode.SYMBOL -> Unit
        }
    }
}
