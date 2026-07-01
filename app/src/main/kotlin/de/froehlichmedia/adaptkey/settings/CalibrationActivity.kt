package de.froehlichmedia.adaptkey.settings

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.froehlichmedia.adaptkey.R
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.touch.CalibrationSentences
import de.froehlichmedia.adaptkey.touch.CalibrationSession
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.TypingPattern
import de.froehlichmedia.adaptkey.touch.TypingPatternAnalysis

/**
 * Optional onboarding / calibration screen (K-01, skippable).
 *
 * The user types 2-3 provided sentences ([CalibrationSentences]) "as they normally would" - with no
 * autocorrect, no suggestion pressure and no time limit. The raw {@code ACTION_DOWN} coordinates feed a
 * dedicated [OffsetModel] hosted by an embedded [AdaptKeyboardView]; on completion that model is merged
 * into the persisted one (T-03), the dominant typing pattern is re-derived (T-04) and the user is given
 * brief feedback. Reachable any time from the settings screen and offered once on first launch.
 *
 * As an Android-facing layer it is left to instrumented tests; the testable logic lives in the pure
 * [CalibrationSession] / [CalibrationSentences] and in [OffsetModel.merge].
 */
class CalibrationActivity : AppCompatActivity() {
    
    private val session = CalibrationSession(CalibrationSentences.DEFAULT)
    
    // A practically infinite warm-up keeps the model in pure-geometry resolution for the whole session,
    // so every recorded tap trains the key the finger physically hit rather than a half-learned guess.
    private val calibrationModel = OffsetModel(warmupSamples = Long.MAX_VALUE)
    
    private lateinit var keyboard: AdaptKeyboardView
    private lateinit var counterView: TextView
    private lateinit var targetView: TextView
    private lateinit var typedView: TextView
    private lateinit var nextButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        title = getString(R.string.k01_activity_title)
        
        counterView = findViewById(R.id.calibration_counter)
        targetView = findViewById(R.id.calibration_target)
        typedView = findViewById(R.id.calibration_typed)
        nextButton = findViewById(R.id.calibration_next)
        
        keyboard = findViewById(R.id.calibration_keyboard)
        keyboard.showNumberRow = false
        keyboard.offsetModel = calibrationModel
        keyboard.onKeyListener = AdaptKeyboardView.OnKeyListener { key, _, _, _ -> onKey(key) }
        
        nextButton.setOnClickListener { onNext() }
        findViewById<Button>(R.id.calibration_skip).setOnClickListener { finish() }
        
        refresh()
    }
    
    private fun onKey(key: Key) {
        when (key.code) {
            KeyCode.CHAR -> {
                val ch = key.char ?: return
                session.append(if (keyboard.shifted && ch.isLetter()) ch.uppercaseChar() else ch)
                consumeShift()
            }
            KeyCode.SPACE -> session.append(' ')
            KeyCode.DELETE -> session.backspace()
            // Enter doubles as "next sentence", so a long sentence can be confirmed from the keyboard.
            KeyCode.ENTER -> { onNext(); return }
            KeyCode.SHIFT -> { keyboard.shifted = !keyboard.shifted; return }
            // L-03: the emoji panel and numeric/symbol layer are irrelevant to calibration (letters only).
            KeyCode.SYMBOL, KeyCode.LETTERS, KeyCode.SYMBOL_PAGE -> return
        }
        refreshTyped()
    }
    
    private fun consumeShift() {
        if (keyboard.shifted) {
            keyboard.shifted = false
        }
    }
    
    private fun onNext() {
        if (session.advance()) {
            refresh()
        } else {
            finishCalibration()
        }
    }
    
    private fun refresh() {
        counterView.text = getString(R.string.k01_counter, session.currentNumber, session.sentenceCount)
        targetView.text = session.currentSentence()
        nextButton.setText(if (session.isOnLastSentence) R.string.k01_finish else R.string.k01_next)
        refreshTyped()
    }
    
    private fun refreshTyped() {
        typedView.text = session.typedText()
    }
    
    /**
     * Merges the calibration samples into the persisted offset model (T-03), re-derives the typing
     * pattern (T-04) and shows the feedback dialog. Closing the dialog ends the activity.
     */
    private fun finishCalibration() {
        val model = OffsetStore.load(this)
        model.merge(calibrationModel)
        OffsetStore.save(this, model)
        val pattern = detectPattern(model)
        OffsetStore.saveDetectedPattern(this, pattern)
        showFeedback(pattern)
    }
    
    private fun detectPattern(model: OffsetModel): TypingPattern {
        val width = keyboard.width
        if (width <= 0) {
            return TypingPattern.UNKNOWN
        }
        return TypingPatternAnalysis.classify(model, keyboard.charKeyGeometry(), width.toFloat())
    }
    
    private fun showFeedback(pattern: TypingPattern) {
        AlertDialog.Builder(this)
            .setTitle(R.string.k01_done_title)
            .setMessage(feedbackText(pattern))
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
    
    private fun feedbackText(pattern: TypingPattern): Int {
        return when (pattern) {
            TypingPattern.LEFT_INDEX_FINGER -> R.string.k01_feedback_left_index
            TypingPattern.RIGHT_INDEX_FINGER -> R.string.k01_feedback_right_index
            TypingPattern.THUMB -> R.string.k01_feedback_thumb
            TypingPattern.UNKNOWN -> R.string.k01_feedback_unknown
        }
    }
}
