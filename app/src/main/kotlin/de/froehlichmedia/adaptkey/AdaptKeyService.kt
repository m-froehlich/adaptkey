// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import de.froehlichmedia.adaptkey.dictionary.PendingLearnStore
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
import de.froehlichmedia.adaptkey.keyboard.BackspaceRepeat
import de.froehlichmedia.adaptkey.keyboard.InputSurface
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.keyboard.PanelNavigation
import de.froehlichmedia.adaptkey.keyboard.SymbolLayout
import de.froehlichmedia.adaptkey.language.ActiveLanguageStore
import de.froehlichmedia.adaptkey.language.Language
import de.froehlichmedia.adaptkey.language.LanguageClassifier
import de.froehlichmedia.adaptkey.language.LanguageProfileLoader
import de.froehlichmedia.adaptkey.prediction.AdaptiveLearning
import de.froehlichmedia.adaptkey.prediction.CapitalisationProposal
import de.froehlichmedia.adaptkey.prediction.HighCertaintyCapitalisation
import de.froehlichmedia.adaptkey.prediction.Tier3Orchestrator
import de.froehlichmedia.adaptkey.prediction.Tier3Outcome
import de.froehlichmedia.adaptkey.prediction.Tier3Result
import de.froehlichmedia.adaptkey.onboarding.OnboardingStore
import de.froehlichmedia.adaptkey.onboarding.OnboardingView
import de.froehlichmedia.adaptkey.prediction.onnx.OnnxTier3Provider
import de.froehlichmedia.adaptkey.prediction.onnx.Tier3ModelStorage
import de.froehlichmedia.adaptkey.settings.AdaptSettings
import de.froehlichmedia.adaptkey.settings.CalibrationActivity
import de.froehlichmedia.adaptkey.settings.SettingsStore
import de.froehlichmedia.adaptkey.settings.Tier3ModelActivity
import de.froehlichmedia.adaptkey.suggestion.ClipboardPreview
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
    private var onboardingView: OnboardingView? = null
    private var inputRoot: LinearLayout? = null
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
    // D-18: whether the emoji panel is enabled (default on). When off, the combined key is a ?123-only key.
    private var emojiPanelEnabled = true
    private lateinit var emojiDataset: EmojiDataset
    private var recentEmojis: List<String> = emptyList()
    
    // D-36: system clipboard, for the direct-paste chip.
    private val clipboardManager by lazy { getSystemService(ClipboardManager::class.java) }
    
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
    
    // D-15: time of the last Shift press, for detecting a double-tap that engages Caps Lock.
    private var lastShiftTime = 0L
    
    // D-07: characters removed during the current accelerating backspace hold; drives the switch from
    // character-wise to word-wise deletion once roughly three words have gone.
    private var backspaceHeldChars = 0
    
    // D-29: armed right after a suggestion is accepted (which appends a space); an immediately following
    // punctuation mark removes that space. Cleared as soon as anything else is typed.
    private var pendingSuggestionSpace = false
    
    // A-07 post-commit autocorrect undo state, armed only for the keystroke directly after a commit.
    private var undoTyped: String? = null
    private var undoCommitted = ""
    private var undoDelimiter = ""
    // D-13: set when the armed undo would revert an A-05 split; undoing it then learns the rejoined word,
    // so a real word the split mangled (e.g. "Backspace" -> "Back Space") is trained and never split again.
    private var undoWasSplit = false
    
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
        // Restore the alphabet the user last switched to (G-01), so it survives a service restart.
        activeLanguage = ActiveLanguageStore.load(this)
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
        view.onBackspaceRepeatListener = AdaptKeyboardView.OnBackspaceRepeatListener { step -> handleBackspaceRepeat(step) }
        view.onLongPressPopupListener = AdaptKeyboardView.OnLongPressPopupListener { _, alternative -> handleLongPressAlternative(alternative) }
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
        
        // First-run onboarding panel, stacked above the keyboard (§2). When shown, the whole input view is
        // forced to the screen height (root minimumHeight) and the panel takes all the space above the keys
        // (weight 1) — i.e. 100% minus the keyboard height; when hidden, the root collapses to the keyboard.
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        inputRoot = root
        val onboarding = OnboardingView(this)
        onboarding.onFinished = { hideOnboarding() }
        onboarding.onOpenModelImport = { launchFromKeyboard(Tier3ModelActivity::class.java) }
        onboarding.onOpenCalibration = { launchFromKeyboard(CalibrationActivity::class.java) }
        onboardingView = onboarding
        
        // Suggestion bar (S-01…S-06) embedded as a row directly above the keyboard, rather than the
        // legacy onCreateCandidatesView / setCandidatesViewShown mechanism, which is unreliable on modern
        // Android (edge-to-edge, gesture nav) and left the bar missing entirely on device. D-50: the bar
        // stays permanently visible (even when empty) so the row never appears/disappears and the keyboard
        // below it never jumps.
        val bar = SuggestionBarView(this)
        bar.onItemClick = SuggestionBarView.OnItemClickListener { item -> onSuggestionClicked(item) }
        bar.onBlacklist = SuggestionBarView.OnBlacklistListener { word -> onBlacklistWord(word) }
        bar.visibility = View.VISIBLE
        suggestionBar = bar
        
        val barHeight = (SUGGESTION_BAR_HEIGHT_DP * resources.displayMetrics.density).toInt()
        root.addView(onboarding, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight))
        root.addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setOnboardingShown(!OnboardingStore.isCompleted(this))
        
        // Android 15 (targetSdk 35) draws edge-to-edge, so the input view would otherwise extend under the
        // gesture navigation pill / IME-switch button, which then overlap the bottom row (space / full stop)
        // and steal taps. Pad the whole input view up by the bottom system-bar + gesture inset.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            v.setPadding(0, 0, 0, maxOf(bars.bottom, gestures.bottom))
            insets
        }
        applySettings()
        // D-03: show the input language on the space bar from the first frame.
        updateSpaceLabel()
        return root
    }
    
    /**
     * Hides the onboarding panel and records the flow as completed, so it does not reappear on its own
     * (it stays reachable via the settings screen).
     */
    private fun hideOnboarding() {
        OnboardingStore.setCompleted(this, true)
        setOnboardingShown(false)
    }
    
    /**
     * Shows or hides the onboarding panel and resizes the input view accordingly: forced to the screen
     * height while shown (so the panel fills everything above the keyboard), collapsed to the keyboard
     * height while hidden.
     */
    private fun setOnboardingShown(show: Boolean) {
        onboardingView?.visibility = if (show) View.VISIBLE else View.GONE
        inputRoot?.let { root ->
            root.minimumHeight = if (show) resources.displayMetrics.heightPixels else 0
            root.requestLayout()
        }
    }
    
    /**
     * Launches one of the app's activities from the keyboard (a service context needs a new task).
     */
    private fun launchFromKeyboard(activity: Class<*>) {
        startActivity(Intent(this, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
            // D-05 / D-06: optional key-press sound + haptics (both default off).
            view.soundEnabled = s.keySoundEnabled
            view.hapticsEnabled = s.keyHapticsEnabled
            // D-32: configurable long-press delay.
            view.longPressDelayMs = s.longPressDelayMs
            // D-47: when the emoji panel is off, the combined key must also drop its 😊 glyph and read as
            // a plain ?123 key.
            view.emojiEnabled = s.emojiPanelEnabled
        }
        // D-18: emoji panel on/off (off makes the combined key a ?123-only key).
        emojiPanelEnabled = s.emojiPanelEnabled
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
        pendingSuggestionSpace = false
        previousWord = null
        tokenContextBefore = ""
        capsMode = capsModeFor(info)
        clearUndo()
        keyboardView?.shifted = false
        // D-15: a new field starts without Caps Lock.
        keyboardView?.capsLock = false
        setSurface(InputSurface.LETTERS)
        clearSuggestions()
    }
    
    /**
     * Keeps the in-progress composing token in sync when the caret moves for a reason other than our own
     * typing — e.g. the user taps into the middle of the text to correct something. Without this the
     * composing region is left stale and further edits jump the caret to the old position (mid-sentence
     * editing was impossible). When the caret is no longer at the end of our composing region, we finish
     * composing (leaving the text in place) and reset the token state so the next keystroke starts fresh
     * at the new caret.
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composing.isEmpty()) {
            return
        }
        // Our own edits leave the caret collapsed at the end of the composing region; anything else is a
        // user-initiated caret move or selection.
        val ownEdit = newSelStart == newSelEnd && candidatesEnd >= 0 && newSelStart == candidatesEnd
        if (!ownEdit) {
            currentInputConnection?.finishComposingText()
            composing.setLength(0)
            composingFlags.clear()
            pendingMergeChar = null
            resetWordEndShift()
            clearUndo()
            previousWord = null
            clearSuggestions()
        }
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Pick up any changes made in the settings screen since the keyboard was last shown.
        settings = SettingsStore.load(this)
        applySettings()
        // Reflect the active alphabet (G-01) on the freshly (re)created keyboard view.
        keyboardView?.greek = activeLanguage == Language.GREEK
        // D-03: label the space bar with the current input language.
        updateSpaceLabel()
        // Pick up an offset model seeded by the calibration screen (K-01). Safe on a fresh field (not a
        // restart): the live model was persisted on the previous onFinishInput, so storage is current.
        if (!restarting) {
            reloadOffsetModel()
        }
        reconcileTier3Provider()
        reconcileOnboarding()
        currentInputConnection?.let { armShiftForNextWord(it) }
        // D-36: offer a direct-paste chip when the field opens and the clipboard holds text.
        showClipboardChipIfAvailable()
    }
    
    /**
     * D-36: shows a direct-paste chip in the suggestion bar when a fresh field opens and the clipboard
     * holds text; a tap pastes it. Sensitive content (e.g. a password) is masked in the preview. Typing
     * anything replaces the chip with the normal suggestions.
     */
    private fun showClipboardChipIfAvailable() {
        if (composing.isNotEmpty()) {
            return
        }
        val clip = clipboardManager?.takeIf { it.hasPrimaryClip() }?.primaryClip ?: return
        if (clip.itemCount == 0) {
            return
        }
        val sensitive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            clip.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        val label = ClipboardPreview.label(clip.getItemAt(0).coerceToText(this), sensitive) ?: return
        val chip = SuggestionController.DisplayItem("📋 $label", SuggestionController.Kind.CLIPBOARD, "")
        suggestionBar?.setItems(listOf(chip))
        suggestionBar?.visibility = View.VISIBLE
    }
    
    /**
     * D-36: clears the clipboard after a paste, so pasted content - especially a password - does not linger.
     */
    private fun clearClipboard() {
        val cm = clipboardManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
    
    /**
     * Shows or hides the first-run onboarding panel to match the persisted completion flag, restarting the
     * flow from the top when it reappears (e.g. after the user re-triggered it from the settings screen).
     */
    private fun reconcileOnboarding() {
        val panel = onboardingView ?: return
        val show = !OnboardingStore.isCompleted(this)
        if (show && panel.visibility != View.VISIBLE) {
            panel.restart()
        }
        setOnboardingShown(show)
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
                // D-40: a digit typed between letters (composing non-empty) is almost certainly an unwanted
                // key, so it is kept in the token and corrected like any other typo ("W8rt" -> "Wort");
                // a leading or standalone digit keeps its normal delimiter behaviour.
                val extendsToken = raw.isLetter() || (raw.isDigit() && composing.isNotEmpty())
                if (extendsToken) {
                    if (composing.isEmpty()) {
                        captureTokenContext(ic)
                        resetWordEndShift()
                        // D-29: a new word after the accepted suggestion keeps its leading space (correct);
                        // the eat-the-space rule was only for an immediately following punctuation.
                        pendingSuggestionSpace = false
                    }
                    val ch = if (raw.isLetter() && isUpperArmed()) raw.uppercaseChar() else raw
                    composing.append(ch)
                    // T-05: retain this letter's space-ambiguity flag in step with the composing token (A-05).
                    composingFlags.add(ambiguity.kind)
                    consumeShift()
                    updateComposing(ic)
                    refreshSuggestions()
                } else {
                    // Punctuation and a leading digit are delimiters; they finalise the current token.
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
            
            // L-03 / D-18: a tap opens the emoji panel from the letter view (or the ?123 layer when the
            // emoji panel is disabled), and returns to letters from anywhere else.
            KeyCode.SYMBOL -> setSurface(PanelNavigation.onCombinedKeyTap(surface, emojiPanelEnabled))
            
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
                commitLongPressSymbol(ic, symbol)
            }
            
            KeyCode.SYMBOL -> setSurface(PanelNavigation.onSwitchToSymbols())
            
            else -> Unit
        }
    }
    
    /**
     * Handles a D-01 popup selection: commits the chosen [alternative] exactly like a single long-press
     * secondary (D-02 full-stop punctuation is a delimiter; a letter alternative would extend the word).
     */
    private fun handleLongPressAlternative(alternative: String) {
        val ic = currentInputConnection ?: return
        clearUndo()
        commitLongPressSymbol(ic, alternative)
    }
    
    /**
     * Commits a long-press secondary [symbol]: a letter secondary (a Greek accented vowel, G-01) extends
     * the composing word exactly like typing that letter, while any other symbol (an AltGr glyph or a
     * D-02 punctuation mark) finalises the current token and commits like a delimiter.
     */
    private fun commitLongPressSymbol(ic: InputConnection, symbol: String) {
        if (symbol.isNotEmpty() && symbol.all { it.isLetter() }) {
            appendLongPressLetter(ic, symbol)
        } else {
            finalizeAndCommit(ic, symbol)
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
        val upper = isUpperArmed()
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
        // Persist the switch so the chosen alphabet survives a service restart.
        ActiveLanguageStore.save(this, activeLanguage)
        keyboardView?.greek = activeLanguage == Language.GREEK
        // D-03: keep the space-bar language label in sync with the switch.
        updateSpaceLabel()
        clearSuggestions()
        Toast.makeText(this, languageLabel(activeLanguage), Toast.LENGTH_SHORT).show()
        armShiftForNextWord(ic)
    }
    
    /**
     * D-03: pushes the current input language's display label onto the space bar.
     */
    private fun updateSpaceLabel() {
        keyboardView?.spaceLabel = languageLabel(activeLanguage)
    }
    
    /**
     * The human-readable name of an input language, shown on the space bar (D-03) and in the G-01
     * switch toast. Only German and Greek are user-selectable alphabets today; English is listed for
     * completeness (it is auto-detected for autocorrect but never becomes the active alphabet).
     *
     * @param language the active input language
     * @return the label to display
     */
    private fun languageLabel(language: Language): String {
        return when (language) {
            Language.GREEK -> "Ελληνικά"
            Language.ENGLISH -> "English"
            else -> "Deutsch"
        }
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
            
            // D-19: a full-field horizontal swipe cycles the surface/page (letters ↔ symbols ↔ numbers).
            GestureAction.SWITCH_SURFACE_NEXT -> {
                applySwipePage(PanelNavigation.swipePage(surface, symbolPage, forward = true))
                true
            }
            
            GestureAction.SWITCH_SURFACE_PREV -> {
                applySwipePage(PanelNavigation.swipePage(surface, symbolPage, forward = false))
                true
            }
            
            GestureAction.NONE -> false
        }
    }
    
    /**
     * Applies a D-19 surface/page switch from the horizontal-swipe cycle: shows the target surface and,
     * for the numeric/symbol layer, its specific page.
     *
     * @param page the surface/page to switch to
     */
    private fun applySwipePage(page: PanelNavigation.Page) {
        setSurface(page.surface)
        if (page.surface == InputSurface.SYMBOLS) {
            symbolPage = page.symbolPage
            keyboardView?.symbolPage = symbolPage
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
            deleteComposingChar(ic)
        } else {
            deleteOneBefore(ic)
            // D-45: deleting a character (typically the punctuation just typed at a line/sentence start)
            // can leave the cursor back at a sentence start, where auto-capital must re-arm — otherwise the
            // first letter of the fresh sentence stays lowercase.
            if (composing.isEmpty() && sentenceStartBefore(ic)) {
                keyboardView?.shifted = ShiftGrace.autoArmAtWordStart(capsMode, true)
            }
        }
    }
    
    /**
     * Removes the last character of the in-progress composing token, keeping the ambiguity flags (A-05)
     * in step and refreshing the composing text / suggestions. Re-arms Shift when the removed character
     * was uppercase (G-05 addendum).
     */
    private fun deleteComposingChar(ic: InputConnection) {
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
        applyShiftAfterDelete(deleted)
    }
    
    /**
     * Deletes the single character before the cursor. When there is nothing to delete in the current
     * editable (the cursor is at the very start of the entry), a real DEL key event is sent instead so
     * the editor can join with the previous line/entry if it supports it (D-10).
     *
     * @return true when a character was removed from the editable, false when the DEL fallback was used
     */
    private fun deleteOneBefore(ic: InputConnection): Boolean {
        val before = ic.getTextBeforeCursor(1, 0)
        if (before.isNullOrEmpty()) {
            // D-10: nothing to delete here; let the editor decide (e.g. merge list items / lines).
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            return false
        }
        val deleted = before[0]
        ic.deleteSurroundingText(1, 0)
        applyShiftAfterDelete(deleted)
        return true
    }
    
    /**
     * Handles one tick of an accelerating backspace hold (D-07 / D-31). The first tick (step 0) resets the
     * hold state. While a composing token is present its characters are removed first; afterwards the
     * committed text is deleted character-wise, switching to word-wise once [BackspaceRepeat.deletesWord].
     *
     * @param step the 0-based repeat index (0 resets the hold)
     * @return the delay in milliseconds before the next repeat tick
     */
    private fun handleBackspaceRepeat(step: Int): Long {
        val ic = currentInputConnection ?: return BackspaceRepeat.WORD_DELAY_MS
        if (step == 0) {
            backspaceHeldChars = 0
            clearUndo()
        }
        pendingMergeChar = null
        if (composing.isNotEmpty()) {
            deleteComposingChar(ic)
            backspaceHeldChars++
        } else if (BackspaceRepeat.deletesWord(backspaceHeldChars)) {
            val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0) ?: ""
            val count = WordBoundary.wordDeleteLength(before)
            if (count > 0) {
                ic.deleteSurroundingText(count, 0)
                backspaceHeldChars += count
            } else {
                // Nothing left in this editable: fall back to a DEL key event (D-10).
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        } else if (deleteOneBefore(ic)) {
            backspaceHeldChars++
        }
        // D-31: the next-tick delay follows the char/word-wise phase from the running deletion count.
        return BackspaceRepeat.nextDelayMs(backspaceHeldChars)
    }
    
    /**
     * Addendum to G-05 (+ D-08): when the deleted character was uppercase, Shift is re-armed so the next
     * keystroke reproduces an uppercase character — the case information is carried by the deleted character
     * itself. Deleting whitespace (e.g. the space to the left of a just-deleted capital) counts as deleting a
     * lowercase character and shifts back to lowercase. A deleted lowercase letter leaves Shift as it was.
     */
    private fun applyShiftAfterDelete(deleted: Char) {
        when {
            deleted.isUpperCase() -> keyboardView?.shifted = true
            deleted.isWhitespace() -> keyboardView?.shifted = false
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
            // D-29: a punctuation mark right after an accepted suggestion removes the auto-added trailing
            // space, so it attaches to the word. Only in this armed state - spaces before a typed
            // punctuation are never stripped in general.
            if (pendingSuggestionSpace && isSpaceEatingPunctuation(delimiter)) {
                if (ic.getTextBeforeCursor(1, 0) == " ") {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            pendingSuggestionSpace = false
            ic.commitText(delimiter, 1)
            clearUndo()
            clearSuggestions()
            // A standalone letter-ambiguous space is a spurious space the next token may merge back onto.
            pendingMergeChar = spaceInferred
            armShiftForNextWord(ic)
            return
        }
        pendingSuggestionSpace = false
        
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
            applySplit(ic, split, delimiter, typed)
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
     * separated by a space, followed by [delimiter]. A-07: the split is armed for undo, so a single
     * backspace immediately after rejoins the two words back into the originally typed token.
     *
     * @param typed the original token as typed, restored if the following key is a backspace undo
     */
    private fun applySplit(ic: InputConnection, split: SplitResult, delimiter: String, typed: String) {
        ic.setComposingText("", 1)
        ic.finishComposingText()
        composing.setLength(0)
        composingFlags.clear()
        val left = capitalisation.capitalise(split.left, contextFor(split.left))
        val right = capitalisation.capitalise(split.right, followingPartContext())
        val committed = left + " " + right
        ic.commitText(committed + delimiter, 1)
        learnWord(left)
        learnWord(right)
        // A-07: arm the undo so the next backspace reverts the split (see performAutocorrectUndo).
        undoTyped = typed
        undoCommitted = committed
        undoDelimiter = delimiter
        // D-13: mark this as a split, so undoing it trains the rejoined word.
        undoWasSplit = true
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
        val wasSplit = undoWasSplit
        val committed = undoCommitted
        ic.deleteSurroundingText(undoCommitted.length + undoDelimiter.length, 0)
        ic.commitText(typed + undoDelimiter, 1)
        previousWord = typed
        clearUndo()
        if (wasSplit) {
            // D-13: undoing a wrong A-05 split trains the rejoined word at once, so it is never split again.
            learnWordStrong(typed)
        } else {
            // D-37: undoing an autocorrect un-learns (counts down) the rejected correction, and counts up
            // the word the user insisted on (promoted after repeated insistence).
            PendingLearnStore.decrement(this, committed)
            learnWord(typed)
        }
        previousWord = typed
        clearSuggestions()
        armShiftForNextWord(ic)
    }
    
    private fun updateComposing(ic: InputConnection) {
        val text = composing.toString()
        // S-05 / D-25: colour the recognised word's TEXT (not its background) while composing (C-04).
        if (shouldHighlightComposing(ic, text)) {
            val span = SpannableString(text)
            span.setSpan(ForegroundColorSpan(config.highlightColor), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ic.setComposingText(span, 1)
        } else {
            ic.setComposingText(text, 1)
        }
    }
    
    /**
     * Whether the composing token should be shown in the recognised-word colour (C-04 / S-05): it must be
     * enabled and a known word, and (D-26) the edit must not be inside an existing word - if a letter
     * immediately follows the cursor we are correcting mid-word, and the fragment must not be coloured.
     *
     * @param ic the current input connection
     * @param text the composing token
     * @return true when the token should be coloured
     */
    private fun shouldHighlightComposing(ic: InputConnection, text: String): Boolean {
        if (!config.highlightEnabled || !provider.isKnownWord(text)) {
            return false
        }
        val after = ic.getTextAfterCursor(1, 0)
        return after.isNullOrEmpty() || !after[0].isLetter()
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
        // D-50: the bar stays visible even when empty, so its slot never collapses and the keyboard below
        // it never jumps.
        suggestionBar?.visibility = View.VISIBLE
    }
    
    private fun clearSuggestions() {
        handler.removeCallbacks(resortRunnable)
        controller.clear()
        lastTier3Result = Tier3Result.EMPTY
        lastCapProposal = null
        suggestionBar?.setItems(emptyList())
        // D-50: keep the (now empty) bar visible rather than hiding it.
        suggestionBar?.visibility = View.VISIBLE
    }
    
    private fun learnWord(word: String?) {
        // Adaptive learning: only learn pure-letter tokens; updates the n-gram context (tier 1).
        if (word.isNullOrEmpty() || !word.all { it.isLetter() }) {
            return
        }
        // D-37: a word already in the dictionary is reinforced immediately; a genuinely new word is only
        // counted up and promoted once it has been committed LEARN_THRESHOLD times, so a one-off typo is
        // not eagerly learned as a real word.
        if (provider.isKnownWord(word)) {
            dictionaryStore.learn(word, previousWord)
        } else if (PendingLearnStore.increment(this, word) >= LEARN_THRESHOLD) {
            dictionaryStore.learn(word, previousWord)
            PendingLearnStore.clear(this, word)
        }
        previousWord = word
    }
    
    /**
     * Learns [word] authoritatively (D-13): a deliberate user correction (undoing a wrong split) promotes
     * the word to the dictionary immediately, bypassing the D-37 count-up threshold.
     *
     * @param word the word to promote
     */
    private fun learnWordStrong(word: String?) {
        if (word.isNullOrEmpty() || !word.all { it.isLetter() }) {
            return
        }
        dictionaryStore.learn(word, previousWord)
        PendingLearnStore.clear(this, word)
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
                // D-29: arm the trailing space added here so an immediately following punctuation removes it.
                pendingSuggestionSpace = true
                armShiftForNextWord(ic)
            }
            
            // D-36: run the exact system paste action, then clear the clipboard (esp. passwords).
            SuggestionController.Kind.CLIPBOARD -> {
                ic.performContextMenuAction(android.R.id.paste)
                clearClipboard()
                clearSuggestions()
            }
        }
    }
    
    /**
     * D-29: whether [delimiter] is a punctuation mark that should absorb the trailing space left by an
     * accepted suggestion (sentence / clause punctuation, not a space, newline or opening bracket).
     *
     * @param delimiter the committed delimiter
     * @return true when it should eat a preceding accepted-suggestion space
     */
    private fun isSpaceEatingPunctuation(delimiter: String): Boolean {
        return delimiter.length == 1 && delimiter[0] in SPACE_EATING_PUNCTUATION
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
        val now = SystemClock.uptimeMillis()
        // D-15: a press while Caps Lock is on releases it; two quick presses engage it.
        if (view.capsLock) {
            view.capsLock = false
            view.shifted = false
            lastShiftTime = now
            return
        }
        if (now - lastShiftTime <= DOUBLE_TAP_SHIFT_MS) {
            view.capsLock = true
            view.shifted = false
            lastShiftTime = now
            return
        }
        lastShiftTime = now
        val elapsed = now - shiftArmTime
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
    
    /**
     * Whether the next typed letter should be uppercase: either a one-shot Shift is armed or Caps Lock
     * is engaged (D-15).
     *
     * @return true when the letter should be capitalised
     */
    private fun isUpperArmed(): Boolean {
        val view = keyboardView ?: return false
        return view.shifted || view.capsLock
    }
    
    private fun consumeShift() {
        // D-15: Caps Lock is persistent, so only the one-shot Shift is cleared after a letter.
        if (keyboardView?.shifted == true) {
            keyboardView?.shifted = false
        }
    }
    
    private fun clearUndo() {
        undoTyped = null
        undoCommitted = ""
        undoDelimiter = ""
        undoWasSplit = false
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
        
        // Height of the embedded suggestion strip.
        private const val SUGGESTION_BAR_HEIGHT_DP = 44
        
        // D-15: two Shift presses within this window engage Caps Lock.
        private const val DOUBLE_TAP_SHIFT_MS = 300L
        
        // D-37: how many times a new word must be committed (without being reverted) before it is promoted
        // to the learned dictionary, so a one-off typo is not eagerly learned.
        private const val LEARN_THRESHOLD = 2
        
        // D-29: sentence / clause punctuation that absorbs an accepted suggestion's trailing space.
        private const val SPACE_EATING_PUNCTUATION = ".,!?;:)"
    }
}
