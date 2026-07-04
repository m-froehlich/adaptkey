// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationContext
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationEngine
import de.froehlichmedia.adaptkey.capitalisation.CapsMode
import de.froehlichmedia.adaptkey.capitalisation.SentenceBoundary
import de.froehlichmedia.adaptkey.capitalisation.ShiftGrace
import de.froehlichmedia.adaptkey.capitalisation.WordEndShift
import de.froehlichmedia.adaptkey.dictionary.BlacklistCategory
import de.froehlichmedia.adaptkey.dictionary.DictionaryLoader
import de.froehlichmedia.adaptkey.dictionary.DictionaryStore
import de.froehlichmedia.adaptkey.dictionary.DictionarySuggestionProvider
import de.froehlichmedia.adaptkey.dictionary.InMemoryDictionaryStore
import de.froehlichmedia.adaptkey.dictionary.SplitResult
import de.froehlichmedia.adaptkey.dictionary.TokenRepair
import de.froehlichmedia.adaptkey.emoji.EmojiDataset
import de.froehlichmedia.adaptkey.emoji.EmojiDatasetLoader
import de.froehlichmedia.adaptkey.emoji.EmojiPanelView
import de.froehlichmedia.adaptkey.emoji.RecentEmojiStore
import de.froehlichmedia.adaptkey.emoji.RecentEmojis
import de.froehlichmedia.adaptkey.gesture.GestureAction
import de.froehlichmedia.adaptkey.gesture.KeyGesture
import de.froehlichmedia.adaptkey.gesture.SwipeDirection
import de.froehlichmedia.adaptkey.gesture.WordBoundary
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.InputSurface
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.keyboard.PanelNavigation
import de.froehlichmedia.adaptkey.keyboard.SymbolLayout
import de.froehlichmedia.adaptkey.language.Language
import de.froehlichmedia.adaptkey.language.LanguageClassifier
import de.froehlichmedia.adaptkey.language.LanguageProfileLoader
import de.froehlichmedia.adaptkey.prediction.AdaptiveLearning
import de.froehlichmedia.adaptkey.prediction.CapitalisationProposal
import de.froehlichmedia.adaptkey.prediction.HighCertaintyCapitalisation
import de.froehlichmedia.adaptkey.prediction.Tier3Orchestrator
import de.froehlichmedia.adaptkey.prediction.Tier3Outcome
import de.froehlichmedia.adaptkey.prediction.Tier3Result
import de.froehlichmedia.adaptkey.prediction.onnx.OnnxTier3Provider
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelStorage
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
import de.froehlichmedia.adaptkey.touch.TypingPatternAnalysis

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
 * bar switches the input alphabet between German and Greek (G-01), with Greek vowels taking their tonos
 * form via long-press. Pressing
 * Shift at the end of a word toggles its first-letter case or starts camelCase depending on the next
 * key (G-05), and dragging a suggestion into the trash zone blacklists it (G-04 / A-04). The combined
 * emoji / ?123 key (L-03) opens the emoji panel on a tap, or the numeric/symbol layer on a long-press
 * or an upward swipe; the emoji panel commits Unicode codepoints directly and finalises any
 * in-progress composing token first, exactly like a delimiter. The on-device language detector (A-03)
 * watches the recent context and switches the active lexicon per language — German by default, English
 * when the context is confidently English, Greek in the G-01 Greek mode — so each language gets its own
 * real Wikipedia-derived suggestions, autocorrect and capitalisation; a confidently-foreign but
 * unsupported language simply leaves the text as typed. A tier-3 mini-LLM orchestration (§9 / C-06) is
 * wired in behind a pluggable backend: when the tier-1 confidence falls below the configured threshold
 * it may merge LLM suggestions, capitalise at high certainty (§6 rule 6) and feed results back as an
 * n-gram learning signal — all inert with the default no-op backend until a real model is installed.
 */
class AdaptKeyService : InputMethodService() {
    
    private var keyboardView: AdaptKeyboardView? = null
    private var suggestionBar: SuggestionBarView? = null
    private var emojiPanel: EmojiPanelView? = null
    private var offsetModel: OffsetModel? = null
    
    private var settings = AdaptSettings.DEFAULT
    private var config = SuggestionConfig()
    private var controller = SuggestionController(config)
    // The active-language views of the dictionary pipeline; re-pointed per token by selectActiveDictionary.
    private lateinit var dictionaryStore: DictionaryStore
    private lateinit var provider: SuggestionProvider
    private lateinit var capitalisation: CapitalisationEngine
    private lateinit var tokenRepair: TokenRepair
    // Real per-language lexicons (A-03): German default, English auto-detected, Greek via the G-01 mode.
    private lateinit var stores: Map<Language, DictionaryStore>
    private lateinit var providers: Map<Language, DictionarySuggestionProvider>
    private lateinit var engines: Map<Language, CapitalisationEngine>
    private var previousWord: String? = null
    
    // A-03: on-device language detector. Starts empty (every result UNKNOWN -> guard is a no-op) and is
    // replaced with the profile-backed classifier in onCreate. When the recent context is confidently
    // non-German, the German autocorrect is held back so foreign words are not mangled.
    private var languageClassifier = LanguageClassifier(emptyMap())
    
    // §9 / C-06: tier-3 mini-LLM orchestration. Defaults to the inert no-op backend; when the user has
    // imported a model, onnxProvider is built off-thread and swapped in. When a real backend is active,
    // the orchestrator (and thus the heavy inference) is run on a background thread (tier3Async) so the
    // IME thread never blocks; the tier-1 suggestions are shown immediately and refined when it returns.
    private var tier3 = Tier3Orchestrator()
    private var onnxProvider: OnnxTier3Provider? = null
    private var tier3Async = false
    // A single-thread executor serialises access to the one OrtSession (its run() is not concurrent-safe)
    // and coalesces bursts of typing. The volatile sequence lets a queued task skip work for a token that
    // is already stale, and guards a late result from being applied after the token changed.
    private val tier3Executor = Executors.newSingleThreadExecutor()
    
    @Volatile
    private var tier3RequestSeq = 0
    // The tier-3 output for the token currently being composed, consulted when it is finalised: the §6
    // rule-6 capitalisation proposal and the raw result feeding the adaptive-learning signal (§9).
    private var lastTier3Result = Tier3Result.EMPTY
    private var lastCapProposal: CapitalisationProposal? = null
    
    // G-01: the active alphabet/input language, toggled by the space-bar swipe. GERMAN shows the Latin
    // QWERTZ layout with the German dictionary pipeline; GREEK shows the Greek alphabet and commits raw
    // text (no Greek dictionary yet). Kept for the service lifetime; defaults to German on each start.
    private var activeLanguage = Language.GERMAN
    
    // L-03: which layer is shown, the numeric/symbol layer's current page, the bundled emoji dataset
    // and the persisted recent/frequently-used emoji (MRU).
    private var surface = InputSurface.LETTERS
    private var symbolPage = 1
    private lateinit var emojiDataset: EmojiDataset
    private var recentEmojis: List<String> = emptyList()
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        settings = SettingsStore.load(this)
        applySettings()
    }
    
    private val composing = StringBuilder()
    private val composingFlags = ArrayList<TapAmbiguity>()
    
    // G-05: a word-end Shift is pending — the first character has been provisionally toggled and the
    // next key decides the outcome (camelCase vs. keep). composingCaseLocked marks a token whose casing
    // the user fixed explicitly, so it is committed verbatim (bypassing autocorrect and §6).
    private var wordEndShiftPending = false
    private var composingCaseLocked = false
    
    private var capsMode = CapsMode.NONE
    private var tokenSentenceStart = false
    private var tokenAfterHyphen = false
    
    // A-03: the text before the cursor captured at token start, so the language of the recent context
    // (this text plus the token being typed) can be judged without re-reading the field per keystroke.
    private var tokenContextBefore = ""
    
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
        // Start with instant empty in-memory stores so the keyboard is responsive immediately, then load
        // the real per-language lexicons off the main thread — importing ~0.5M rows into SQLite on the
        // main thread would ANR. Until the load finishes (first run only) there are simply no suggestions.
        installStores(DictionaryLoader.LANGUAGES.associateWith { InMemoryDictionaryStore() })
        loadDictionariesAsync()
        emojiDataset = EmojiDatasetLoader.load(this)
        recentEmojis = RecentEmojiStore.load(this)
        languageClassifier = LanguageProfileLoader.loadClassifier(this)
        loadTier3ProviderAsync()
        SettingsStore.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    /**
     * Builds the real tier-3 ONNX backend (§9 / C-06) off the main thread when the user has imported a
     * model, and swaps it into the orchestrator on the main thread. A no-op when no model is present or
     * when construction fails (the keyboard then stays on the inert no-op backend). Loading the ONNX
     * session is heavy, so it never runs on the IME thread.
     */
    private fun loadTier3ProviderAsync() {
        if (onnxProvider != null) {
            return
        }
        Thread {
            val built = runCatching { OnnxTier3Provider.createIfAvailable(this) }.getOrNull()
            if (built != null) {
                handler.post {
                    onnxProvider?.close()
                    onnxProvider = built
                    tier3 = Tier3Orchestrator(built)
                    tier3Async = true
                }
            }
        }.start()
    }
    
    /**
     * Points the dictionary pipeline at [newStores]: builds a provider and a capitalisation engine per
     * language and selects German as the initial active language. Called on the main thread with the
     * instant empty stores first, then again once the real lexicons have loaded.
     */
    private fun installStores(newStores: Map<Language, DictionaryStore>) {
        stores = newStores
        providers = newStores.mapValues { (_, store) -> DictionarySuggestionProvider(store, config.maxSuggestions * 2) }
        engines = newStores.mapValues { (_, store) -> CapitalisationEngine(store) }
        val german = newStores.getValue(Language.GERMAN)
        dictionaryStore = german
        provider = providers.getValue(Language.GERMAN)
        capitalisation = engines.getValue(Language.GERMAN)
        tokenRepair = TokenRepair(german)
    }
    
    /**
     * Loads the real per-language SQLite lexicons (importing the bundled assets on first run) on a
     * background thread and swaps them in on the main thread, so the heavy first-run import never
     * blocks the UI thread.
     */
    private fun loadDictionariesAsync() {
        Thread {
            val loaded = DictionaryLoader.loadStores(this)
            handler.post { installStores(loaded) }
        }.start()
    }
    
    override fun onCreateInputView(): View {
        val container = FrameLayout(this)
        
        val view = AdaptKeyboardView(this)
        view.offsetModel = offsetModel
        view.onKeyListener = AdaptKeyboardView.OnKeyListener { key, _, _, ambiguity -> handleKey(key, ambiguity) }
        view.onLongPressListener = AdaptKeyboardView.OnLongPressListener { key -> handleLongPress(key) }
        view.onSwipeListener = AdaptKeyboardView.OnSwipeListener { key, direction -> handleSwipe(key, direction) }
        keyboardView = view
        
        val panel = EmojiPanelView(this)
        panel.dataset = emojiDataset
        panel.setRecentEmojis(recentEmojis)
        panel.onEmojiSelectedListener = EmojiPanelView.OnEmojiSelectedListener { emoji -> commitEmoji(emoji) }
        panel.onBackListener = EmojiPanelView.OnBackListener { setSurface(InputSurface.LETTERS) }
        panel.visibility = View.GONE
        emojiPanel = panel
        
        container.addView(view)
        container.addView(panel)
        // Android 15 (targetSdk 35) draws edge-to-edge, so the input view would otherwise extend under the
        // gesture navigation pill / IME-switch button, which then overlap the bottom row (space / full stop)
        // and steal taps. Pad the whole keyboard up by the bottom system-bar + gesture inset so it sits
        // above them.
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            v.setPadding(0, 0, 0, maxOf(bars.bottom, gestures.bottom))
            insets
        }
        applySettings()
        return container
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
            if (this::stores.isInitialized) {
                providers = stores.mapValues { (_, store) -> DictionarySuggestionProvider(store, config.maxSuggestions * 2) }
                // Re-point the active provider; selectActiveDictionary corrects the language per token.
                provider = providers.getValue(Language.GERMAN)
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
        bar.onBlacklist = SuggestionBarView.OnBlacklistListener { word -> onBlacklistWord(word) }
        suggestionBar = bar
        return bar
    }
    
    /**
     * Handles a G-04 drag-to-trash: permanently blacklists [word] (A-04, category
     * [BlacklistCategory.USER]) and refreshes the bar so the now-excluded word disappears immediately.
     */
    private fun onBlacklistWord(word: String) {
        if (word.isBlank() || !this::dictionaryStore.isInitialized) {
            return
        }
        dictionaryStore.blacklist(word, BlacklistCategory.USER)
        refreshSuggestions()
    }
    
    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        composing.setLength(0)
        composingFlags.clear()
        resetWordEndShift()
        pendingMergeChar = null
        previousWord = null
        tokenContextBefore = ""
        capsMode = capsModeFor(info)
        clearUndo()
        keyboardView?.shifted = false
        setSurface(InputSurface.LETTERS)
        clearSuggestions()
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Pick up any changes made in the settings screen since the keyboard was last shown.
        settings = SettingsStore.load(this)
        applySettings()
        // Reflect the active alphabet (G-01) on the freshly (re)created keyboard view.
        keyboardView?.greek = activeLanguage == Language.GREEK
        // Pick up an offset model seeded by the calibration screen (K-01). Safe on a fresh field (not a
        // restart): the live model was persisted on the previous onFinishInput, so storage is current.
        if (!restarting) {
            reloadOffsetModel()
        }
        reconcileTier3Provider()
        currentInputConnection?.let { armShiftForNextWord(it) }
    }
    
    /**
     * Reconciles the tier-3 backend with the model the user may have imported or removed in the settings
     * screen since the keyboard was last shown (§9 / C-06): builds the ONNX backend off-thread when a
     * model has appeared, or drops it (reverting to the inert no-op backend) when the model is gone.
     */
    private fun reconcileTier3Provider() {
        val installed = Tier3ModelStorage.isModelInstalled(this)
        if (installed && onnxProvider == null) {
            loadTier3ProviderAsync()
        } else if (!installed && onnxProvider != null) {
            tier3Executor.execute {
                val stale = onnxProvider
                handler.post {
                    onnxProvider = null
                    tier3Async = false
                    tier3 = Tier3Orchestrator()
                    stale?.close()
                }
            }
        }
    }
    
    /**
     * Reloads the personal offset model from storage and re-attaches it to the view, so a model the
     * calibration screen merged (T-03 / K-01) is adopted even when this service instance was already
     * resident. Only called on a fresh field, where the persisted model is up to date.
     */
    private fun reloadOffsetModel() {
        val model = OffsetStore.load(this)
        offsetModel = model
        keyboardView?.offsetModel = model
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        composing.setLength(0)
        clearSuggestions()
        // Persist what the model learned while this field was focused (T-03) and re-derive T-04.
        offsetModel?.let { OffsetStore.save(this, it) }
        persistTypingPattern()
    }
    
    override fun onDestroy() {
        handler.removeCallbacks(resortRunnable)
        SettingsStore.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        offsetModel?.let { OffsetStore.save(this, it) }
        persistTypingPattern()
        tier3Executor.shutdownNow()
        onnxProvider?.close()
        onnxProvider = null
        super.onDestroy()
    }
    
    /**
     * Re-derives the dominant typing pattern (T-04) from the live offset model and the laid-out
     * keyboard geometry, and persists it for the settings screen. A no-op while the keyboard has not
     * been laid out, so a previously detected pattern is never overwritten with an unknown result.
     */
    private fun persistTypingPattern() {
        val model = offsetModel ?: return
        val view = keyboardView ?: return
        if (view.width <= 0) {
            return
        }
        val pattern = TypingPatternAnalysis.classify(model, view.charKeyGeometry(), view.width.toFloat())
        OffsetStore.saveDetectedPattern(this, pattern)
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
        // G-05: resolve a pending word-end Shift against this key before it is handled normally. A Shift
        // is left to fall through (it re-toggles via handleShift); every other key resolves here.
        if (wordEndShiftPending && key.code != KeyCode.SHIFT) {
            resolvePendingWordEndShift(key)
        }
        when (key.code) {
            KeyCode.CHAR -> {
                val raw = key.char ?: return
                if (raw.isLetter()) {
                    if (composing.isEmpty()) {
                        captureTokenContext(ic)
                        resetWordEndShift()
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
            
            // L-03: a tap opens the emoji panel from the letter view, or returns to letters from anywhere else.
            KeyCode.SYMBOL -> setSurface(PanelNavigation.onCombinedKeyTap(surface))
            
            // L-03: the "ABC" key on the numeric/symbol layer returns to letters.
            KeyCode.LETTERS -> setSurface(InputSurface.LETTERS)
            
            // L-03: toggles between the numeric/symbol layer's two pages.
            KeyCode.SYMBOL_PAGE -> {
                symbolPage = SymbolLayout.togglePage(symbolPage)
                keyboardView?.symbolPage = symbolPage
            }
        }
    }
    
    /**
     * Handles a long-press (L-05 / L-06 secondary symbols: finalises the current token, so a held key
     * mid-word commits the word first, then commits the symbol exactly like typing a delimiter; or
     * L-03: holding the combined emoji / ?123 key switches straight to the numeric/symbol layer).
     */
    private fun handleLongPress(key: Key) {
        when (key.code) {
            KeyCode.CHAR -> {
                val symbol = key.hint ?: return
                val ic = currentInputConnection ?: return
                clearUndo()
                if (symbol.isNotEmpty() && symbol.all { it.isLetter() }) {
                    // A letter secondary (a Greek accented vowel, G-01) extends the word rather than
                    // delimiting it - it behaves exactly like typing that letter.
                    appendLongPressLetter(ic, symbol)
                } else {
                    finalizeAndCommit(ic, symbol)
                }
            }
            
            KeyCode.SYMBOL -> setSurface(PanelNavigation.onSwitchToSymbols())
            
            else -> Unit
        }
    }
    
    /**
     * Appends a letter secondary (a Greek accented vowel, G-01) into the composing token, mirroring
     * the normal character path: it starts a token if none is open, honours a pending Shift for the
     * upper-case accented form, and refreshes the composing text and suggestions.
     */
    private fun appendLongPressLetter(ic: InputConnection, letters: String) {
        if (composing.isEmpty()) {
            captureTokenContext(ic)
            resetWordEndShift()
        }
        val upper = keyboardView?.shifted == true
        for (ch in letters) {
            composing.append(if (upper) ch.uppercaseChar() else ch)
            composingFlags.add(TapAmbiguity.NONE)
        }
        consumeShift()
        updateComposing(ic)
        refreshSuggestions()
    }
    
    /**
     * Delivers [emoji] as a raw Unicode codepoint via `commitText` (L-03): no app-side support is
     * needed. Any in-progress composing token is finalised first, exactly like a delimiter, so it is
     * not silently dropped, then the emoji itself is committed and recorded as the most recent use.
     */
    private fun commitEmoji(emoji: String) {
        val ic = currentInputConnection ?: return
        finalizeAndCommit(ic, "")
        ic.commitText(emoji, 1)
        recentEmojis = RecentEmojis.recordUse(recentEmojis, emoji)
        RecentEmojiStore.save(this, recentEmojis)
        emojiPanel?.setRecentEmojis(recentEmojis)
    }
    
    /**
     * Switches the visible input surface (L-03): shows/hides the letter+symbol keyboard vs. the emoji
     * panel, and resets the numeric/symbol layer back to its first page whenever it is not the target.
     */
    private fun setSurface(next: InputSurface) {
        surface = next
        if (next != InputSurface.EMOJI) {
            keyboardView?.surface = next
        }
        keyboardView?.visibility = if (next == InputSurface.EMOJI) View.GONE else View.VISIBLE
        emojiPanel?.visibility = if (next == InputSurface.EMOJI) View.VISIBLE else View.GONE
        if (next == InputSurface.EMOJI) {
            emojiPanel?.setRecentEmojis(recentEmojis)
        }
        if (next != InputSurface.SYMBOLS) {
            symbolPage = 1
            keyboardView?.symbolPage = 1
        }
    }
    
    /**
     * Toggles the input language between German and Greek (G-01). Any in-progress token is finalised
     * first in the current language (so a German word being typed is committed with its rules before
     * the switch), then the keyboard shows the other alphabet and a short toast confirms the change.
     */
    private fun toggleLanguage(ic: InputConnection) {
        finalizeAndCommit(ic, "")
        activeLanguage = if (activeLanguage == Language.GREEK) Language.GERMAN else Language.GREEK
        keyboardView?.greek = activeLanguage == Language.GREEK
        clearSuggestions()
        val label = if (activeLanguage == Language.GREEK) "Ελληνικά" else "Deutsch"
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
        armShiftForNextWord(ic)
    }
    
    /**
     * Handles a swipe gesture (§4 G-01 … G-03, plus the L-03 upward swipe to the symbol layer)
     * reported by the keyboard view. Resolves it to an action via [KeyGesture] and executes it.
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
            
            // G-01: switch the input language / alphabet. With two languages (German, Greek), a left or
            // right swipe both simply toggle between them.
            GestureAction.LANGUAGE_PREV, GestureAction.LANGUAGE_NEXT -> {
                toggleLanguage(ic)
                true
            }
            
            // L-03: upward swipe on the combined key switches to the numeric/symbol layer.
            GestureAction.OPEN_SYMBOL_LAYER -> {
                setSurface(PanelNavigation.onSwitchToSymbols())
                true
            }
            
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
        resetWordEndShift()
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
            val deleted = composing.last()
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
            armShiftIfDeletedUpper(deleted)
        } else {
            val deleted = ic.getTextBeforeCursor(1, 0)?.firstOrNull()
            ic.deleteSurroundingText(1, 0)
            if (deleted != null) {
                armShiftIfDeletedUpper(deleted)
            }
        }
    }
    
    /**
     * Addendum to G-05: when the deleted character was uppercase, Shift is re-armed so the next keystroke
     * reproduces an uppercase character — the case information is carried by the deleted character itself.
     * A deleted lowercase character leaves the Shift state as it was (context-driven).
     */
    private fun armShiftIfDeletedUpper(deleted: Char) {
        if (deleted.isUpperCase()) {
            keyboardView?.shifted = true
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
        
        // G-05: a word-end Shift made this token's casing explicit; commit it exactly as composed,
        // bypassing autocorrect, capitalisation (§6) and token repair — the user has hand-finished it.
        if (composingCaseLocked) {
            commitVerbatim(ic, delimiter)
            return
        }
        val typed = composing.toString()
        // A-03: pick the dictionary/capitalisation for the recent context (German default, English
        // auto-detected, Greek in Greek mode); suppress = a confidently-foreign but unsupported language.
        val suppressAutocorrect = selectActiveDictionary("$tokenContextBefore $typed")
        
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
        
        // A-03: an unsupported foreign context leaves the token as typed; otherwise the selected
        // language's autocorrect applies (A-01 enforced in provider).
        val corrected = if (suppressAutocorrect) typed else provider.autocorrectFor(typed, previousWord) ?: typed
        // §6 rule 6: a high-certainty tier-3 nominal proposal may lift an otherwise-lowercased word to
        // upper-case (never with the no-op backend, where lastCapProposal is null).
        val llmForcesUpper = HighCertaintyCapitalisation.forcesUpper(lastCapProposal, corrected)
        // §9 adaptive learning: capture the tier-3 result and pre-commit knowledge before learnWord mutates it.
        val tier3Result = lastTier3Result
        val contextWord = previousWord
        val tier1KnewCorrected = provider.isKnownWord(corrected)
        val finalWord = capitalisation.capitalise(corrected, contextFor(typed), llmForcesUpper)
        
        ic.setComposingText(finalWord, 1)
        ic.finishComposingText()
        composing.setLength(0)
        composingFlags.clear()
        ic.commitText(delimiter, 1)
        learnWord(finalWord)
        reinforceFromTier3(finalWord, tier3Result, contextWord, tier1KnewCorrected)
        
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
     * Commits the composing token exactly as composed, followed by [delimiter] (G-05): no autocorrect,
     * no §6 capitalisation and no token repair. Used when the user fixed the token's casing explicitly
     * via a word-end Shift, which ranks as explicit input and must be preserved in both directions.
     */
    private fun commitVerbatim(ic: InputConnection, delimiter: String) {
        val word = composing.toString()
        ic.setComposingText(word, 1)
        ic.finishComposingText()
        composing.setLength(0)
        composingFlags.clear()
        resetWordEndShift()
        ic.commitText(delimiter, 1)
        learnWord(word)
        clearUndo()
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
        // A-03: pick the dictionary for the recent context; an unsupported foreign context shows nothing.
        val suppressAutocorrect = selectActiveDictionary("$tokenContextBefore $input")
        if (suppressAutocorrect) {
            clearSuggestions()
            return
        }
        val candidates = provider.suggestionsFor(input, previousWord)
        val pending = provider.autocorrectFor(input, previousWord)
        val previous = previousWord
        val sentence = "$tokenContextBefore$input"
        // §9 / C-06: consult tier 3 when the tier-1 confidence is below the threshold (never with the
        // no-op backend, where the outcome is the tier-1 list unchanged and no capitalisation proposal).
        // A-02: the mini-LLM sees the whole running context, not a punctuation-truncated fragment.
        val seq = ++tier3RequestSeq
        if (!tier3Async) {
            // No-op (or absent) backend: the orchestrator is instant, run it inline.
            applyTier3Outcome(input, pending, tier3.predict(input, previous, sentence, candidates, settings.llmActivationThreshold, config.maxSuggestions))
            return
        }
        // A real backend runs the LLM: show the tier-1 suggestions immediately, then refine off-thread so
        // the IME never blocks. A stale result (the token changed meanwhile) is discarded via the sequence.
        controller.update(input, candidates, pending)
        showSuggestions()
        scheduleResort()
        val threshold = settings.llmActivationThreshold
        val limit = config.maxSuggestions
        // Capture the orchestrator on the main thread so the executor never reads the reassignable field.
        val orchestrator = tier3
        tier3Executor.execute {
            // Skip inference outright when the token already moved on while queued.
            if (seq != tier3RequestSeq) {
                return@execute
            }
            val outcome = orchestrator.predict(input, previous, sentence, candidates, threshold, limit)
            handler.post {
                if (seq == tier3RequestSeq && composing.toString() == input) {
                    applyTier3Outcome(input, pending, outcome)
                }
            }
        }
    }
    
    /**
     * Applies a tier-3 orchestration outcome to the suggestion bar: stores the §6 capitalisation proposal
     * and the raw result (for the adaptive-learning feedback) and refreshes the bar.
     */
    private fun applyTier3Outcome(input: String, pending: String?, outcome: Tier3Outcome) {
        lastTier3Result = outcome.tier3
        lastCapProposal = outcome.capitalisation
        controller.update(input, outcome.suggestions, pending)
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
        lastTier3Result = Tier3Result.EMPTY
        lastCapProposal = null
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
    
    /**
     * §9 adaptive learning: when the committed word was a confident tier-3 suggestion the tier-1 n-gram
     * did not know, reinforce it in the active dictionary so the n-gram improves and the mini-LLM is
     * needed less over time. Inert with the no-op backend ([tier3Result] is then empty, so no signal
     * fires); when it fires it is an extra reinforcement on top of the normal commit-time [learnWord].
     *
     * @param committedWord the finally committed word
     * @param tier3Result the tier-3 result shown for this token
     * @param contextWord the previous word at commit time (n-gram context for the reinforcement)
     * @param tier1KnewWord whether the tier-1 n-gram already knew the word before the commit
     */
    private fun reinforceFromTier3(
        committedWord: String,
        tier3Result: Tier3Result,
        contextWord: String?,
        tier1KnewWord: Boolean
    ) {
        val signal = AdaptiveLearning.learningSignal(committedWord, tier3Result, tier1KnewWord) ?: return
        dictionaryStore.learn(signal, contextWord)
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
        tokenSentenceStart = SentenceBoundary.isSentenceStart(before, settings.commaLineNotSentenceStart)
        tokenAfterHyphen = before.endsWith("-")
        tokenContextBefore = before
    }
    
    /** Which language's dictionary a token should use, and whether autocorrect must be held back. */
    private data class DictChoice(val language: Language, val suppressAutocorrect: Boolean)
    
    /**
     * A-03: chooses the dictionary for the recent [context]. Greek mode (G-01) uses the Greek lexicon;
     * otherwise the detector decides — a confidently English context uses the English lexicon, a
     * confidently other-foreign context (e.g. French, which has no bundled lexicon) keeps the German
     * store but holds back autocorrect so the text is left as typed, and everything else defaults to
     * German. Conservative by construction (see [LanguageClassifier.isForeign]).
     */
    private fun resolveDict(context: String): DictChoice {
        if (activeLanguage == Language.GREEK) {
            return DictChoice(Language.GREEK, suppressAutocorrect = false)
        }
        if (!languageClassifier.isForeign(context)) {
            return DictChoice(Language.GERMAN, suppressAutocorrect = false)
        }
        val best = languageClassifier.classify(LanguageClassifier.lastWords(context, LANGUAGE_WINDOW)).language
        return if (best == Language.ENGLISH) {
            DictChoice(Language.ENGLISH, suppressAutocorrect = false)
        } else {
            DictChoice(Language.GERMAN, suppressAutocorrect = true)
        }
    }
    
    /**
     * Re-points the active dictionary pipeline (provider / capitalisation / store) to the language of
     * the recent [context] and reports whether autocorrect must be suppressed for it (A-03).
     *
     * @param context the recent text (context before the token plus the token itself)
     * @return true when autocorrect must not be applied for this token
     */
    private fun selectActiveDictionary(context: String): Boolean {
        val choice = resolveDict(context)
        provider = providers.getValue(choice.language)
        capitalisation = engines.getValue(choice.language)
        dictionaryStore = stores.getValue(choice.language)
        return choice.suppressAutocorrect
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
        // G-05: Shift at the end of a fully typed word (a non-empty composing token) toggles the word's
        // first-letter case; the next key decides whether the toggle is kept or turns into camelCase.
        if (composing.isNotEmpty()) {
            handleWordEndShift(view)
            return
        }
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
    
    /**
     * Applies a word-end Shift (G-05): provisionally toggles the case of the composing token's first
     * character, arms the next letter to uppercase (so a following letter produces camelCase) and marks
     * the token's casing as explicitly user-set. Pressing Shift again simply re-toggles.
     */
    private fun handleWordEndShift(view: AdaptKeyboardView) {
        flipFirstInComposing()
        updateComposing(currentInputConnection ?: return)
        wordEndShiftPending = true
        composingCaseLocked = true
        view.shifted = true
    }
    
    /**
     * Resolves a pending word-end Shift (G-05) against the next key: a letter discards the first-char
     * toggle and continues as camelCase, a delimiter keeps the toggle, anything else cancels the gesture.
     * The token stays case-locked for the camelCase and keep outcomes, so it is committed verbatim.
     */
    private fun resolvePendingWordEndShift(key: Key) {
        when (WordEndShift.resolveNextKey(nextKeyClass(key))) {
            // The provisional toggle is discarded; the upcoming letter is inserted uppercase (camelCase).
            WordEndShift.Resolution.CAMEL_CASE -> {
                flipFirstInComposing()
                currentInputConnection?.let { updateComposing(it) }
                wordEndShiftPending = false
            }
            
            // The toggle stands; the token will be committed verbatim by the following delimiter.
            WordEndShift.Resolution.KEEP -> wordEndShiftPending = false
            
            // Backspace or other keys abandon the gesture; the token is no longer case-locked.
            WordEndShift.Resolution.CANCEL -> resetWordEndShift()
            
            // Re-toggling is handled by handleShift, never reached here (Shift is excluded by the caller).
            WordEndShift.Resolution.RETOGGLE -> Unit
        }
    }
    
    private fun nextKeyClass(key: Key): WordEndShift.NextKey {
        return when (key.code) {
            KeyCode.CHAR -> if (key.char?.isLetter() == true) WordEndShift.NextKey.LETTER else WordEndShift.NextKey.DELIMITER
            KeyCode.SPACE, KeyCode.ENTER -> WordEndShift.NextKey.DELIMITER
            KeyCode.SHIFT -> WordEndShift.NextKey.SHIFT
            else -> WordEndShift.NextKey.OTHER
        }
    }
    
    private fun flipFirstInComposing() {
        if (composing.isEmpty()) {
            return
        }
        val flipped = WordEndShift.flipFirst(composing.toString())
        composing.setLength(0)
        composing.append(flipped)
    }
    
    private fun resetWordEndShift() {
        wordEndShiftPending = false
        composingCaseLocked = false
    }
    
    private fun sentenceStartBefore(ic: InputConnection): Boolean {
        val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0)?.toString() ?: ""
        return SentenceBoundary.isSentenceStart(before, settings.commaLineNotSentenceStart)
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
    
    companion object {
        
        private const val MAX_CONTEXT_LOOKBACK = 80
        
        // A-03: how many trailing words of context feed the language detector (spec: last 3-5 words).
        private const val LANGUAGE_WINDOW = 5
    }
}
