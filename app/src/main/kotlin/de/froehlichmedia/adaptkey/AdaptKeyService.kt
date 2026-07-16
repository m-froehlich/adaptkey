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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale
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
import de.froehlichmedia.adaptkey.gesture.WordExtent
import de.froehlichmedia.adaptkey.keyboard.AdaptKeyboardView
import de.froehlichmedia.adaptkey.keyboard.AlternativeScript
import de.froehlichmedia.adaptkey.keyboard.BackspaceRepeat
import de.froehlichmedia.adaptkey.keyboard.InlineSuggestionsBarView
import de.froehlichmedia.adaptkey.keyboard.InputSurface
import de.froehlichmedia.adaptkey.keyboard.Key
import de.froehlichmedia.adaptkey.keyboard.KeyCode
import de.froehlichmedia.adaptkey.keyboard.PanelNavigation
import de.froehlichmedia.adaptkey.keyboard.SettingsRowView
import de.froehlichmedia.adaptkey.keyboard.SignFlip
import de.froehlichmedia.adaptkey.keyboard.SymbolLayout
import de.froehlichmedia.adaptkey.language.ActiveLanguageStore
import de.froehlichmedia.adaptkey.language.Language
import de.froehlichmedia.adaptkey.language.LanguageClassifier
import de.froehlichmedia.adaptkey.language.LanguageCycle
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
import de.froehlichmedia.adaptkey.settings.SettingsActivity
import de.froehlichmedia.adaptkey.settings.SettingsStore
import de.froehlichmedia.adaptkey.settings.Tier3ModelActivity
import de.froehlichmedia.adaptkey.suggestion.ClipboardPreview
import de.froehlichmedia.adaptkey.suggestion.RawCoordinateCorrection
import de.froehlichmedia.adaptkey.suggestion.SuggestionBarView
import de.froehlichmedia.adaptkey.suggestion.SuggestionConfig
import de.froehlichmedia.adaptkey.suggestion.Suggestion
import de.froehlichmedia.adaptkey.suggestion.SuggestionController
import de.froehlichmedia.adaptkey.suggestion.SuggestionProvider
import de.froehlichmedia.adaptkey.suggestion.TimePattern
import de.froehlichmedia.adaptkey.touch.AmbiguityResult
import de.froehlichmedia.adaptkey.touch.OffsetModel
import de.froehlichmedia.adaptkey.touch.OffsetStore
import de.froehlichmedia.adaptkey.touch.TapAmbiguity
import de.froehlichmedia.adaptkey.touch.TapPoint
import de.froehlichmedia.adaptkey.touch.TypingPattern

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
    private var settingsRow: SettingsRowView? = null
    private var onboardingView: OnboardingView? = null
    // D-135: platform-rendered Autofill inline suggestions (a saved username/password), shown instead of
    // the ordinary suggestion bar whenever at least one is available for the current field.
    private var inlineSuggestionsBar: InlineSuggestionsBarView? = null
    private var inputRoot: LinearLayout? = null
    private var offsetModel: OffsetModel? = null
    // D-74: the typing pattern the currently-held offsetModel was loaded under, so a later save can detect
    // whether the calibration screen has since replaced the persisted model with a different pattern from
    // some other Activity while this long-lived service instance kept its own (now stale) copy in memory -
    // see persistOffsetModel().
    private var offsetModelPattern = TypingPattern.UNKNOWN
    
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
    
    // D-130: consecutive commits routed to English by A-03 while German/Greek stays active - once this
    // reaches SUSTAINED_ENGLISH_WORD_THRESHOLD, trackSustainedEnglishUsage() promotes it to a real switch.
    private var consecutiveEnglishWords = 0
    
    // L-03: which layer is shown, the numeric/symbol layer's current page, the bundled emoji dataset
    // and the persisted recent/frequently-used emoji (MRU).
    private var surface = InputSurface.LETTERS
    private var symbolPage = 1
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
    // D-39: the raw ACTION_DOWN tap for each composing character, in lockstep with composing/composingFlags
    // (same index, mutated at every site that mutates composingFlags) - lets an unknown token be corrected
    // from where the taps actually landed (T-02), not just the committed spelling.
    private val composingTaps = ArrayList<TapPoint>()
    // D-62: the logical edit point within composing - equal to composing.length while typing normally
    // extends the token at its end, but short of it while editing mid-word (an untouched "after" fragment,
    // reclaimed from the surrounding text, still follows). composingAnchor is the absolute document offset
    // of the composing region's start, needed to place the real cursor back at composingCursor after every
    // setComposingText() call (which otherwise always leaves it at the end); -1 while no reclaimed "after"
    // fragment is in play, so the natural end-of-composing cursor placement is already correct.
    private var composingCursor = 0
    private var composingAnchor = -1
    
    // D-139: see the KDoc on its use in onUpdateSelection().
    private val selectionUpdateBurstGuard = CallbackBurstGuard()
    
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
    
    // D-123: a suggestion-bar tap's own commitText() generates an asynchronous onUpdateSelection callback
    // that, since composing is already empty by the time it lands, calls reclaimWordAtCaret() - which
    // would otherwise unconditionally clear pendingSuggestionSpace above before the very punctuation check
    // it exists for ever gets to run. Armed for exactly the one reclaim call that follows a suggestion tap.
    private var suppressNextReclaimSpaceReset = false
    
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
        offsetModelPattern = OffsetStore.loadDetectedPattern(this)
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
        providers = newStores.mapValues { (_, store) -> DictionarySuggestionProvider(store, config.maxSuggestions * 2, MIN_AUTOCORRECT_CANDIDATE_FREQUENCY) }
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
        // D-53: let the keyboard's long-press popup draw outside the keyboard bounds - a number-row popup
        // reaches up over the suggestion bar rather than being clipped or flipped below the key.
        container.clipChildren = false
        container.clipToPadding = false
        
        val view = AdaptKeyboardView(this)
        view.offsetModel = offsetModel
        view.onKeyListener = AdaptKeyboardView.OnKeyListener { key, x, y, ambiguity -> handleKey(key, x, y, ambiguity) }
        view.onLongPressListener = AdaptKeyboardView.OnLongPressListener { key -> handleLongPress(key) }
        view.onSwipeListener = AdaptKeyboardView.OnSwipeListener { key, direction -> handleSwipe(key, direction) }
        view.onBackspaceRepeatListener = AdaptKeyboardView.OnBackspaceRepeatListener { step -> handleBackspaceRepeat(step) }
        view.onLongPressPopupListener = AdaptKeyboardView.OnLongPressPopupListener { key, alternative -> handleLongPressAlternative(key, alternative) }
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
        // D-53: the keyboard container's popup overflow must also escape the root, so it can overlap the
        // suggestion bar row (the keyboard is drawn after the bar, so its popup lands on top of it).
        root.clipChildren = false
        root.clipToPadding = false
        // §42: root's own background, not just AdaptKeyboardView's - the bottom system-gesture-inset
        // padding below (see the OnApplyWindowInsetsListener below) falls outside every child's bounds, so
        // without this it let whatever sits behind the IME window show through underneath the keyboard.
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.keyboard_background))
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
        
        // D-135: the inline-suggestions row occupies the same slot as the ordinary suggestion bar, shown
        // instead of it whenever a real autofill suggestion is available (see onInlineSuggestionsResponse).
        val inlineBar = InlineSuggestionsBarView(this)
        inlineBar.visibility = View.GONE
        inlineSuggestionsBar = inlineBar
        
        // §48 / §51: the swipe-up settings row - sits above the suggestion bar (the topmost row while
        // open), reserved at zero height and hidden until an upward swipe opens it.
        val row = SettingsRowView(this)
        row.onEmojiClick = SettingsRowView.OnEmojiClickListener { openEmojiPanelFromSettingsRow() }
        row.onSettingsClick = SettingsRowView.OnSettingsClickListener { openSettingsAppFromSettingsRow() }
        row.onClearClipboardClick = SettingsRowView.OnClearClipboardClickListener { clearClipboardFromSettingsRow() }
        settingsRow = row
        
        val barHeight = (SUGGESTION_BAR_HEIGHT_DP * resources.displayMetrics.density).toInt()
        root.addView(onboarding, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0))
        root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight))
        root.addView(inlineBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight))
        root.addView(container, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        setOnboardingShown(!OnboardingStore.isCompleted(this))
        
        // Android 15 (targetSdk 35) draws edge-to-edge, so the input view would otherwise extend under the
        // gesture navigation pill / IME-switch button, which then overlap the bottom row (space / full stop)
        // and steal taps. Pad the whole input view up by the bottom system-bar + gesture inset. D-81: while
        // onboarding is shown, root is stretched to the full screen height (setOnboardingShown()), so its
        // top can now also reach the status bar / a front-camera cutout - pad that too. During ordinary
        // typing root only wraps the (bottom-anchored) keyboard, so the window never overlaps the top
        // inset region and this reports (and adds) zero padding there, exactly like the bottom inset
        // already behaves.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(0, maxOf(statusBars.top, cutout.top), 0, maxOf(bars.bottom, gestures.bottom))
            insets
        }
        // D-136: without this, the system's own gesture-area controls (the pill / back indicator) render
        // with whatever appearance the window would otherwise inherit, independent of what background
        // colour AdaptKey itself paints beneath them - poor contrast was reported on at least one device.
        // AdaptKey's keyboard background is always light (R.color.keyboard_background, no dark-theme
        // variant exists), so the controls must always render dark-on-light to stay visible against it.
        window?.window?.let { win ->
            WindowInsetsControllerCompat(win, root).isAppearanceLightNavigationBars = true
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
                providers = stores.mapValues { (_, store) -> DictionarySuggestionProvider(store, config.maxSuggestions * 2, MIN_AUTOCORRECT_CANDIDATE_FREQUENCY) }
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
            // D-59: the combined ?123 key can be disabled, in which case it disappears entirely.
            view.symbolKeyEnabled = s.symbolKeyEnabled
            // D-92: the calculator page's currency/decimal-separator keys follow the device's system
            // locale (not the active keyboard alphabet - German and Greek never disagree on either point).
            view.systemLocale = resources.configuration.locales.get(0)
            // D-55: extra spacing below the number row and above the space row.
            view.extraSpaceBelowNumberRowDp = s.extraSpaceBelowNumberRowDp
            view.extraSpaceAboveSpaceRowDp = s.extraSpaceAboveSpaceRowDp
        }
        // §56: the accepted-word flight matches the user's actual configured highlight colour (C-04/S-05),
        // not just its default.
        suggestionBar?.flyColor = config.highlightColor
        // D-126: toggling the mini-LLM switch takes effect immediately, not only on the next field focus.
        reconcileTier3Provider()
    }
    
    /**
     * D-88 / §56: feedback when a correction or suggestion is accepted - previously "too dry", silent and
     * visually unremarkable. [word] flies up out of the suggestion bar and fades, always, regardless of the
     * D-05 key-sound setting - §56 decoupled this from sound entirely, per feedback that a purely
     * sound-gated flash was too easy to miss and read as tied to the wrong toggle. The distinct "plop"
     * sample plays in addition, only when key-press sound is actually on.
     */
    private fun notifySuggestionAccepted(word: String) {
        suggestionBar?.flyAccepted(word)
        if (settings.keySoundEnabled) {
            keyboardView?.playSuggestionAcceptedSound()
        }
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
        clearComposing()
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
        // A field that primarily wants digits (phone number, plain numeric entry, date/time) opens
        // straight to the calculator page instead of the letters surface.
        setSurface(initialSurfaceFor(info), targetSymbolPage = 1)
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
        // D-139: a defensive circuit breaker, not a confirmed fix - no repro exists yet for the reported
        // "text jitters, characters get scrambled" glitch, but this function's own reactive mutations
        // (reclaimWordAtCaret() below / finishComposingText() further down) could in principle re-trigger
        // this very callback, and this codebase has hit a genuinely self-triggering onUpdateSelection
        // cascade before (a different specific bug, §32's D-87, but the same class of risk). If this ever
        // fires abnormally often in a short window, stop reacting rather than let a possible cascade
        // continue to escalate - see CallbackBurstGuard for the threshold reasoning.
        if (selectionUpdateBurstGuard.isBurst(SystemClock.uptimeMillis())) {
            return
        }
        if (composing.isEmpty()) {
            // §58: the caret may have just landed on an existing committed word - a tap, or our own
            // backspace removing a trailing delimiter - with no new character typed at all. D-62's reclaim
            // was only ever wired to run on the *next* keystroke; do it right now instead, so mid-word live
            // correction also works the instant the caret touches a word.
            if (newSelStart == newSelEnd) {
                reclaimWordAtCaret()
            }
            return
        }
        // Our own edits leave the caret collapsed at the end of the composing region - or, D-62, at the
        // logical mid-word edit point we deliberately placed it at via setSelection() when a reclaimed
        // "after" fragment still follows; anything else is a user-initiated caret move or selection.
        val ownCursor = if (composingAnchor >= 0) composingAnchor + composingCursor else candidatesEnd
        val ownEdit = newSelStart == newSelEnd && candidatesEnd >= 0 && newSelStart == ownCursor
        if (!ownEdit) {
            currentInputConnection?.finishComposingText()
            clearComposing()
            pendingMergeChar = null
            resetWordEndShift()
            clearUndo()
            previousWord = null
            clearSuggestions()
        }
    }
    
    /**
     * §58: reclaims the word the caret currently touches (D-62), purely from a caret move rather than a
     * keystroke - mirrors the reclaim-then-render sequence [handleKey]'s `CHAR` branch runs when a new token
     * starts mid-word, minus inserting a character (nothing was typed). A no-op when the caret touches no
     * word ([WordExtent.reclaim], via [reclaimSurroundingWord], finds nothing on either side) - batched for
     * the same D-87 reason: [reclaimSurroundingWord]'s own `deleteSurroundingText()` must not reach the app
     * as a standalone edit, or its callback can arrive after `composing` has already advanced and wipe what
     * this very call just built.
     */
    private fun reclaimWordAtCaret() {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            captureTokenContext(ic)
            resetWordEndShift()
            // D-123: skip the reset exactly once when this call is only the echo of a suggestion-bar tap's
            // own commit, not a genuine subsequent caret move - otherwise D-29's space-eating flag never
            // survives to see the punctuation it is meant to react to.
            if (suppressNextReclaimSpaceReset) {
                suppressNextReclaimSpaceReset = false
            } else {
                pendingSuggestionSpace = false
            }
            reclaimSurroundingWord(ic, tap = null)
            if (composing.isEmpty()) {
                return
            }
            updateComposing(ic)
        } finally {
            ic.endBatchEdit()
        }
        refreshSuggestions()
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Pick up any changes made in the settings screen since the keyboard was last shown.
        settings = SettingsStore.load(this)
        applySettings()
        // Reflect the active alphabet (G-01) on the freshly (re)created keyboard view.
        keyboardView?.greek = activeLanguage == Language.GREEK
        keyboardView?.qwerty = activeLanguage == Language.ENGLISH
        // D-03: label the space bar with the current input language.
        updateSpaceLabel()
        // Pick up an offset model seeded by the calibration screen (K-01). Safe on a fresh field (not a
        // restart): the live model was persisted on the previous onFinishInput, so storage is current.
        if (!restarting) {
            reloadOffsetModel()
        }
        // D-126: applySettings() above already calls reconcileTier3Provider() (it must also re-check on an
        // ordinary settings-screen change while the service stays resident, not only per field focus).
        reconcileOnboarding()
        currentInputConnection?.let { armShiftForNextWord(it) }
        // D-36: offer a direct-paste chip when the field opens and the clipboard holds text.
        showClipboardChipIfAvailable()
        // §48: never carry an open settings row over into a fresh keyboard presentation.
        settingsRow?.closeImmediately()
        // D-135: never carry a previous field's autofill suggestions over into a fresh one - a new
        // onCreateInlineSuggestionsRequest()/onInlineSuggestionsResponse() round-trip supplies fresh ones
        // (or none) for whatever field is now focused.
        resetInlineSuggestions()
    }
    
    /**
     * D-135: builds the request declaring how AdaptKey wants Autofill inline suggestions presented -
     * called by the platform (API 30+ only; never invoked at all on older devices) whenever the focused
     * field is autofill-relevant. One shared [InlinePresentationSpec] sized to the ordinary suggestion
     * bar's own height, with the standard [UiVersions] v1 style so the active autofill service renders a
     * suggestion compatible with what most services already expect.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        val barHeight = (SUGGESTION_BAR_HEIGHT_DP * resources.displayMetrics.density).toInt()
        val style = UiVersions.newStylesBuilder()
            .addStyle(InlineSuggestionUi.newStyleBuilder().build())
            .build()
        val spec = InlinePresentationSpec.Builder(
            Size(0, 0),
            Size(resources.displayMetrics.widthPixels, barHeight)
        ).setStyle(style).build()
        return InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(INLINE_SUGGESTION_MAX_COUNT)
            .build()
    }
    
    /**
     * D-135: receives the autofill service's actual suggestions (a saved username/email or password,
     * depending on the focused field) - each one is an opaque view the platform itself renders
     * ([InlineSuggestion.inflate]); AdaptKey never sees the underlying credential text, only places the
     * inflated views once ready. Shown in [inlineSuggestionsBar] instead of the ordinary suggestion bar for
     * as long as at least one is present; reverts to the ordinary bar once cleared (a fresh, empty response,
     * or the field changing via [resetInlineSuggestions]).
     *
     * @return true only when at least one suggestion is being shown - false (nothing to display) when the
     *         response is empty, per the API contract
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        val bar = inlineSuggestionsBar ?: return false
        bar.clearSuggestions()
        val suggestions = response.inlineSuggestions
        if (suggestions.isEmpty()) {
            bar.visibility = View.GONE
            suggestionBar?.visibility = View.VISIBLE
            return false
        }
        val barHeight = (SUGGESTION_BAR_HEIGHT_DP * resources.displayMetrics.density).toInt()
        val size = Size(INLINE_SUGGESTION_WIDTH_DP.dpToPx(), barHeight)
        for (suggestion in suggestions.take(INLINE_SUGGESTION_MAX_COUNT)) {
            suggestion.inflate(this, size, mainExecutor) { view ->
                if (view == null) {
                    return@inflate
                }
                bar.addSuggestion(view)
                bar.visibility = View.VISIBLE
                suggestionBar?.visibility = View.GONE
            }
        }
        return true
    }
    
    /** D-135: converts a dp value to pixels, for the inline-suggestion inflate size. */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    /** D-135: clears any inline suggestions and restores the ordinary suggestion bar. */
    private fun resetInlineSuggestions() {
        inlineSuggestionsBar?.clearSuggestions()
        inlineSuggestionsBar?.visibility = View.GONE
        suggestionBar?.visibility = View.VISIBLE
    }
    
    /**
     * D-36: shows a direct-paste chip in the suggestion bar when a fresh field opens and the clipboard
     * holds text; a tap pastes it. Sensitive content (e.g. a password) is masked in the preview. Typing
     * anything replaces the chip with the normal suggestions. §40: nothing is offered once the clip is
     * older than [ClipboardPreview.MAX_AGE_MS] - a long-forgotten clipboard entry should not keep
     * resurfacing every time a field opens.
     */
    private fun showClipboardChipIfAvailable() {
        if (composing.isNotEmpty()) {
            return
        }
        val clip = clipboardManager?.takeIf { it.hasPrimaryClip() }?.primaryClip ?: return
        if (clip.itemCount == 0) {
            return
        }
        if (!ClipboardPreview.isFresh(clip.description.timestamp, System.currentTimeMillis())) {
            return
        }
        val text = resolveClipboardText(clip, clip.getItemAt(0)) ?: return
        val label = ClipboardPreview.label(text, isSensitiveClip(clip)) ?: return
        val chip = SuggestionController.DisplayItem("📋 $label", SuggestionController.Kind.CLIPBOARD, "")
        suggestionBar?.setItems(listOf(chip))
        suggestionBar?.visibility = View.VISIBLE
    }
    
    /**
     * §60 / D-124: the clipboard text to preview - or null when nothing should be offered at all. D-124:
     * the clip must genuinely declare a text-family MIME type ([isTextMimeType]) *regardless* of whether
     * this particular [item] carries a [android.content.ClipData.Item.getUri] or plain
     * [android.content.ClipData.Item.getText] - checked first, before branching on which one this item
     * has, since some file-sharing/copy paths populate [ClipData.Item.getText] directly (a filename, a
     * content-URI string, or worse) rather than using the URI field the original §60 fix only ever
     * gated - the reported "an APK file is still offered" bug survived §60's own fix because it only
     * checked the MIME type on the URI branch, leaving the plain-text branch completely ungated. An
     * ordinary text copy (selecting text, `ClipData.newPlainText(...)`) always declares `text/plain` or
     * similar, so this costs the common case nothing.
     *
     * A URI item (real *file* content, e.g. copied in a Files app) is read directly, capped at
     * [CLIPBOARD_FILE_PREVIEW_CHARS] - only the chip's own short, already-truncated preview
     * ([ClipboardPreview.label]) needs the content; the actual paste (§38's native paste action) resolves
     * the file itself through the target app, not through this read.
     *
     * @param clip the current primary clip
     * @param item its first (and only ever considered) item
     * @return the text to preview/offer, or null to show no chip at all
     */
    private fun resolveClipboardText(clip: ClipData, item: ClipData.Item): CharSequence? {
        if (!isTextMimeType(clip)) {
            return null
        }
        val uri = item.uri ?: return item.coerceToText(this)
        return runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(CLIPBOARD_FILE_PREVIEW_CHARS)
                val read = reader.read(buffer)
                if (read <= 0) null else String(buffer, 0, read)
            }
        }.getOrNull()
    }
    
    /**
     * D-124: whether [clip] genuinely declares a text-family MIME type - checked against its own concrete
     * declared type(s) directly, *not* via [ClipDescription.hasMimeType], whose documented wildcard
     * matching treats [ClipDescription.MIMETYPE_UNKNOWN] (a generic "unknown type" declaration some
     * file-sharing apps use, e.g. for a copied APK) as matching any requested pattern, including a
     * `text` request - exactly the reported bug (an APK file was offered via Quick Paste). That generic
     * wildcard declaration is therefore explicitly excluded here, not treated as a text match.
     */
    private fun isTextMimeType(clip: ClipData): Boolean {
        val description = clip.description
        return (0 until description.mimeTypeCount).any { i ->
            val type = description.getMimeType(i)
            type != ClipDescription.MIMETYPE_UNKNOWN && type.startsWith("text/")
        }
    }
    
    /**
     * §38 (was D-36): whether [clip]'s content is sensitive (e.g. a password) - flagged by the app that
     * *copied* it, via the OS-level [ClipDescription.EXTRA_IS_SENSITIVE] (Android 13+ only; there is no
     * signal at all on older platforms, or from a copying app that does not set it - neither the clipboard
     * content itself nor the current paste target's field type is consulted). The same flag drives both the
     * D-36 chip's bullet-masked preview ([ClipboardPreview]) and, from §38, whether a paste auto-clears the
     * clipboard afterward.
     */
    private fun isSensitiveClip(clip: ClipData): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            clip.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
    }
    
    /**
     * §38: schedules a clipboard clear [CLIPBOARD_CLEAR_DELAY_MS] after a sensitive paste - long enough for
     * the target app's asynchronous [InputConnection.performContextMenuAction] paste handling to have
     * actually read the clipboard first (see the `CLIPBOARD` branch of `handleSuggestionTap()` for why that
     * call, not `commitText()`, is used), short enough to keep the exposure window for the sensitive content
     * small. Skips clearing if the clipboard no longer holds [expectedText] by the time it fires - something
     * else was copied in the meantime, and that must not be silently wiped.
     */
    private fun scheduleClipboardClear(expectedText: String) {
        handler.postDelayed({
            val current = clipboardManager
                ?.takeIf { it.hasPrimaryClip() }
                ?.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
            if (current == expectedText) {
                clearClipboard()
            }
        }, CLIPBOARD_CLEAR_DELAY_MS)
    }
    
    /**
     * D-36: clears the clipboard, so a sensitive paste (a password, §38) does not linger.
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
        offsetModelPattern = OffsetStore.loadDetectedPattern(this)
        keyboardView?.offsetModel = model
    }
    
    /**
     * D-74: persists what [offsetModel] learned (T-03) - but only when it is still current. This
     * long-lived service instance can sit resident across a calibration-screen visit (K-01), which
     * **replaces** the whole persisted model from a separate Activity; if the persisted pattern no longer
     * matches [offsetModelPattern], that replacement happened after this model was loaded, so saving it now
     * would silently clobber the fresh calibration with stale data (the symptom reported: after switching
     * pattern, the D-24 touch-model visualisation still showed the previous, skewed pattern). In that case
     * the fresh model is adopted instead (mirroring what a new field's [reloadOffsetModel] would do anyway)
     * rather than saved over.
     */
    private fun persistOffsetModel() {
        val model = offsetModel ?: return
        if (OffsetStore.loadDetectedPattern(this) != offsetModelPattern) {
            reloadOffsetModel()
            return
        }
        OffsetStore.save(this, model)
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        composing.setLength(0)
        clearSuggestions()
        // D-68: the typing pattern (T-04) is no longer re-derived here - it is now an explicit user choice
        // (see CalibrationActivity), not something inferred from live typing.
        persistOffsetModel()
    }
    
    override fun onDestroy() {
        handler.removeCallbacks(resortRunnable)
        SettingsStore.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        persistOffsetModel()
        tier3Executor.shutdownNow()
        onnxProvider?.close()
        onnxProvider = null
        super.onDestroy()
    }
    
    private fun handleKey(key: Key, x: Float, y: Float, ambiguity: AmbiguityResult) {
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
                // A key is only alphabetic text when it actually lives on the letters surface - a Greek
                // letter or other case-mapped glyph reused as a plain symbol on the calculator/catch-all
                // pages (e.g. π, ƒ) is not a word-forming letter there, and must not be auto-capitalised
                // or treated as continuing a composing word just because Char.isLetter() happens to be
                // true for it.
                val isWordLetter = raw.isLetter() && surface == InputSurface.LETTERS
                // D-40: a digit typed between letters (composing non-empty) is almost certainly an unwanted
                // key, so it is kept in the token and corrected like any other typo ("W8rt" -> "Wort");
                // a leading or standalone digit keeps its normal delimiter behaviour.
                val extendsToken = isWordLetter || (raw.isDigit() && composing.isNotEmpty())
                if (extendsToken) {
                    // D-87: a reclaim's own deleteSurroundingText() must not be allowed to reach the app as
                    // a standalone edit - batched together with the setComposingText/setSelection that
                    // follow, the app coalesces all three into one update and reports only the final,
                    // consistent selection. Unbatched, its own onUpdateSelection callback can arrive after
                    // composing/composingAnchor have already advanced, so onUpdateSelection's ownEdit check
                    // (comparing against the now-current, not the delete-time, expected cursor) mismatches
                    // and wipes the token it is itself in the middle of building.
                    ic.beginBatchEdit()
                    try {
                        if (composing.isEmpty()) {
                            captureTokenContext(ic)
                            resetWordEndShift()
                            // D-29: a new word after the accepted suggestion keeps its leading space (correct);
                            // the eat-the-space rule was only for an immediately following punctuation.
                            pendingSuggestionSpace = false
                            // D-62: the caret may sit inside (or against) an already-committed word - reclaim
                            // it so autocorrect/suggestions see the whole word, not just what gets typed from
                            // here.
                            reclaimSurroundingWord(ic, TapPoint(x, y))
                        }
                        val ch = if (isWordLetter && isUpperArmed()) raw.uppercaseChar() else raw
                        // T-05 / D-39 / D-62: keeps composingFlags/composingTaps in lockstep and lands the new
                        // character at the logical edit point (the end, unless a reclaim left a tail after it).
                        insertComposingChar(ch, ambiguity.kind, TapPoint(x, y))
                        consumeShift()
                        updateComposing(ic)
                    } finally {
                        ic.endBatchEdit()
                    }
                    refreshSuggestions()
                } else {
                    // Punctuation and a leading digit are delimiters; they finalise the current token.
                    finalizeAndCommit(ic, raw.toString())
                }
            }
            
            KeyCode.SPACE -> {
                // T-05: a space tapped in the upper band carries the letter inferred for a possible merge (A-06).
                // D-119: finalizeAndCommit() itself splits at the caret when it is mid-word.
                val inferred = ambiguity.inferredChar.takeIf { ambiguity.kind == TapAmbiguity.LETTER_AMBIGUOUS }
                finalizeAndCommit(ic, " ", inferred)
            }
            
            KeyCode.ENTER -> handleEnter(ic)
            
            KeyCode.DELETE -> handleBackspace(ic)
            
            KeyCode.SHIFT -> handleShift()
            
            // L-03 / §49: a tap toggles the ?123 layer (D-18's emoji-panel dual purpose retired - the
            // emoji button now lives in the §48 settings row instead).
            KeyCode.SYMBOL -> setSurface(PanelNavigation.onCombinedKeyTap(surface))
            
            // L-03: the "ABC" key on the numeric/symbol layer returns to letters.
            KeyCode.LETTERS -> setSurface(InputSurface.LETTERS)
            
            // §53 (D-103/D-104): a multi-character key (e.g. the calculator page's sin/deg) commits its
            // own label verbatim, exactly like any other symbol - finalising whatever token was in
            // progress first, never extending it.
            KeyCode.TEXT -> finalizeAndCommit(ic, key.label)
        }
    }
    
    /**
     * D-61: handles the Enter key. In a real multi-line field it inserts a newline as before. In a
     * single-line field it commits the pending word and then submits, honouring the editor's requested IME
     * action (Go / Search / Send / Done) via [InputConnection.performEditorAction]; when the field declares no
     * action (e.g. a browser address bar) a real Enter key event is sent, which such fields treat as submit.
     * This replaces the old unconditional committing of a literal `\n`, which many search / URL fields
     * ignored, so Enter appeared to do nothing.
     */
    private fun handleEnter(ic: InputConnection) {
        val editorInfo = currentInputEditorInfo
        val imeOptions = editorInfo?.imeOptions ?: 0
        val multiLine = ((editorInfo?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (multiLine) {
            finalizeAndCommit(ic, "\n")
            return
        }
        // Single-line field: commit the pending word without a newline, then submit.
        finalizeAndCommit(ic, "")
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }
    
    /**
     * Handles a long-press (L-05 / L-06 secondary symbols: finalises the current token, so a held key
     * mid-word commits the word first, then commits the symbol exactly like typing a delimiter; L-03:
     * holding the combined emoji / ?123 key switches straight to the numeric/symbol layer; or §31: holding
     * the calculator's minus key flips the sign of the number before the caret instead of committing text).
     */
    private fun handleLongPress(key: Key) {
        when (key.code) {
            KeyCode.CHAR -> {
                if (key.char == SymbolLayout.MINUS_SIGN) {
                    val ic = currentInputConnection ?: return
                    clearUndo()
                    flipSignBeforeCaret(ic)
                    return
                }
                val symbol = key.hint ?: return
                val ic = currentInputConnection ?: return
                clearUndo()
                commitLongPressSymbol(ic, symbol, key.code)
            }
            
            KeyCode.SYMBOL -> setSurface(PanelNavigation.onSwitchToSymbols())
            
            else -> Unit
        }
    }
    
    /**
     * §31: flips the sign of the number immediately before the caret - long-pressing the calculator's
     * minus key. Does nothing if there is no number directly before the caret. See [SignFlip] for the
     * pure logic.
     */
    private fun flipSignBeforeCaret(ic: InputConnection) {
        val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0)?.toString() ?: return
        val result = SignFlip.resultFor(before) ?: return
        ic.deleteSurroundingText(result.deleteLength, 0)
        ic.commitText(result.insertText, 1)
    }
    
    /**
     * Handles a D-01 popup selection: commits the chosen [alternative] exactly like a single long-press
     * secondary (D-02 full-stop punctuation is a delimiter; a letter alternative would extend the word).
     * [key] is the popup's originating key, needed to recognise a §53 [KeyCode.TEXT] alternative (e.g.
     * `cos`/`tan`/`log` on the calculator's `sin` key) as a symbol commit, never word-extending text.
     */
    private fun handleLongPressAlternative(key: Key, alternative: String) {
        val ic = currentInputConnection ?: return
        clearUndo()
        commitLongPressSymbol(ic, alternative, key.code)
    }
    
    /**
     * Commits a long-press secondary [symbol]: a letter secondary that is genuine language text (a Greek
     * accented vowel, G-01, while actually in Greek mode) extends the composing word exactly like typing
     * that letter, while any other symbol - an AltGr glyph, a D-02 punctuation mark, a Greek letter
     * borrowed as a math symbol on the Latin keyboard (§35, π/α/β/γ/δ/λ/ω on the `p` key's popup), or a §53
     * [KeyCode.TEXT] alternative (`cos`/`tan`/`log`, `rad`) - finalises the current token and commits like
     * a delimiter, verbatim and immune to auto-capitalisation. [sourceCode] is the originating key's code;
     * a [KeyCode.TEXT] key's alternatives are always symbols, regardless of script, since `cos`/`tan`/`log`
     * are ordinary Latin letters [AlternativeScript] would otherwise treat as genuine word text. See
     * [AlternativeScript] for why case is significant for the Greek case (Π ≠ π) but not for an ordinary word.
     */
    private fun commitLongPressSymbol(ic: InputConnection, symbol: String, sourceCode: KeyCode) {
        if (sourceCode != KeyCode.TEXT && AlternativeScript.extendsWord(symbol, activeLanguage == Language.GREEK)) {
            appendLongPressLetter(ic, symbol)
        } else {
            finalizeAndCommit(ic, symbol)
        }
    }
    
    /**
     * Appends a letter secondary (a Greek accented vowel, G-01) into the composing token, mirroring
     * the normal character path: it starts a token if none is open, honours a pending Shift for the
     * upper-case accented form, and refreshes the composing text and suggestions.
     *
     * D-62: also reclaims a mid-word caret like the ordinary character path, but leaves the reclaimed
     * (and these appended) characters untracked in composingTaps - there is no raw tap coordinate for a
     * long-press secondary, matching this path's pre-existing gap (see [insertComposingChar]).
     */
    private fun appendLongPressLetter(ic: InputConnection, letters: String) {
        // D-87: see the CHAR handler in handleKey for why the reclaim's deleteSurroundingText() must be
        // batched together with the setComposingText/setSelection that follow.
        ic.beginBatchEdit()
        try {
            if (composing.isEmpty()) {
                captureTokenContext(ic)
                resetWordEndShift()
                reclaimSurroundingWord(ic, tap = null)
            }
            val upper = isUpperArmed()
            for (ch in letters) {
                composing.insert(composingCursor, if (upper) ch.uppercaseChar() else ch)
                composingFlags.add(composingCursor, TapAmbiguity.NONE)
                composingCursor++
            }
            consumeShift()
            updateComposing(ic)
        } finally {
            ic.endBatchEdit()
        }
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
     *
     * D-58: the keyboard-view switch (letters <-> symbols) is animated with a perceptible slide; [forward]
     * defaults to "entering the numeric/symbol layer slides in from the right, leaving it slides in from
     * the left" and [targetSymbolPage] defaults to the current page, so ordinary callers need not think
     * about it. The D-19 horizontal-swipe cycle passes both explicitly, mirroring the actual swipe.
     *
     * @param next the surface to show
     * @param forward the D-58 slide direction
     * @param targetSymbolPage the numeric/symbol page to show; only meaningful when [next] is
     *        [InputSurface.SYMBOLS]
     */
    private fun setSurface(next: InputSurface, forward: Boolean = next == InputSurface.SYMBOLS, targetSymbolPage: Int = symbolPage) {
        surface = next
        val page = if (next == InputSurface.SYMBOLS) targetSymbolPage else 1
        if (next != InputSurface.EMOJI) {
            keyboardView?.switchPage(next, page, forward)
        }
        keyboardView?.visibility = if (next == InputSurface.EMOJI) View.GONE else View.VISIBLE
        emojiPanel?.visibility = if (next == InputSurface.EMOJI) View.VISIBLE else View.GONE
        if (next == InputSurface.EMOJI) {
            emojiPanel?.setRecentEmojis(recentEmojis)
        }
        symbolPage = page
    }
    
    /**
     * D-106 stage 1: steps the input language through the [LanguageCycle] (German, English, Greek) via
     * G-01. Any in-progress token is finalised first in the current language (so a word being typed is
     * committed with its own rules before the switch), then the keyboard shows the new alphabet and a
     * short toast confirms the change.
     *
     * @param forward true for a right swipe (advances through the cycle), false for a left swipe (goes back)
     */
    private fun toggleLanguage(ic: InputConnection, forward: Boolean) {
        finalizeAndCommit(ic, "")
        // D-130: acknowledge the switch on the space bar - captures the outgoing label before it changes.
        keyboardView?.beginLanguageChangeFade()
        activeLanguage = if (forward) LanguageCycle.next(activeLanguage) else LanguageCycle.previous(activeLanguage)
        // Persist the switch so the chosen alphabet survives a service restart.
        ActiveLanguageStore.save(this, activeLanguage)
        keyboardView?.greek = activeLanguage == Language.GREEK
        keyboardView?.qwerty = activeLanguage == Language.ENGLISH
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
     * switch toast. D-106 stage 1: German, English and Greek are all user-selectable alphabets via the
     * [LanguageCycle].
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
     * Handles a swipe gesture (§4 G-01 … G-03, the L-03 upward swipe to the symbol layer, and §48's
     * upward-swipe settings row) reported by the keyboard view. Resolves it to an action via
     * [KeyGesture] and executes it.
     *
     * @param key the key the swipe started on (T-01 contact point)
     * @param direction the recognised swipe direction
     * @return true when the swipe carried an action and was consumed (suppressing the tap), false
     *         when it should fall back to a normal tap
     */
    private fun handleSwipe(key: Key, direction: SwipeDirection): Boolean {
        val ic = currentInputConnection ?: return false
        return when (KeyGesture.resolve(key.code, direction, surface)) {
            // G-02: delete the whole previous word.
            GestureAction.DELETE_WORD -> {
                clearUndo()
                deleteWord(ic)
                true
            }
            
            // G-03: dismiss the keyboard - §48: unless the settings row is open, in which case this
            // downward swipe closes the row first; only a second one (row already closed) reaches here
            // again to actually dismiss. KeyGesture.resolve() is a pure function with no row-open state of
            // its own, so this re-routing happens here rather than as a distinct GestureAction.
            GestureAction.DISMISS_KEYBOARD -> {
                if (settingsRow?.isOpen == true) {
                    closeSettingsRow()
                } else {
                    requestHideSelf(0)
                }
                true
            }
            
            // G-01 / D-106 stage 1: switch the input language / alphabet, stepping the LanguageCycle in
            // the swipe's own direction (left = previous, right = next).
            GestureAction.LANGUAGE_PREV -> {
                toggleLanguage(ic, forward = false)
                true
            }
            
            GestureAction.LANGUAGE_NEXT -> {
                toggleLanguage(ic, forward = true)
                true
            }
            
            // L-03: upward swipe on the combined key switches to the numeric/symbol layer.
            GestureAction.OPEN_SYMBOL_LAYER -> {
                setSurface(PanelNavigation.onSwitchToSymbols())
                true
            }
            
            // §48: upward swipe anywhere else reveals the settings row.
            GestureAction.OPEN_SETTINGS_ROW -> {
                openSettingsRow()
                true
            }
            
            // D-19: a full-field horizontal swipe cycles the surface/page (letters ↔ symbols ↔ numbers).
            // D-94: the page-index step (which page to land on) is action-based - NEXT/PREV, per D-91's
            // mapping - but the slide *animation* must follow the finger's actual physical direction
            // (D-76), which D-91 decoupled from the action: a right swipe always slides "forward"
            // (RIGHT swipes NEXT), regardless of whether that swipe means NEXT or PREV.
            GestureAction.SWITCH_SURFACE_NEXT -> {
                applySwipePage(PanelNavigation.swipePage(surface, symbolPage, forward = true), forward = direction == SwipeDirection.RIGHT)
                true
            }
            
            GestureAction.SWITCH_SURFACE_PREV -> {
                applySwipePage(PanelNavigation.swipePage(surface, symbolPage, forward = false), forward = direction == SwipeDirection.RIGHT)
                true
            }
            
            GestureAction.NONE -> false
        }
    }
    
    /**
     * Applies a D-19 surface/page switch from the horizontal-swipe cycle: shows the target surface and,
     * for the numeric/symbol layer, its specific page. D-58 / D-94: [forward] carries the actual physical
     * swipe direction through to the slide animation (true = swiped right), so the page visibly moves the
     * way the finger did - independent of whether that swipe meant NEXT or PREV (D-91).
     *
     * @param page the surface/page to switch to
     * @param forward whether the triggering swipe was a right swipe (true) or a left swipe (false)
     */
    private fun applySwipePage(page: PanelNavigation.Page, forward: Boolean) {
        setSurface(page.surface, forward, page.symbolPage)
    }
    
    /**
     * §48: opens the settings row (an upward swipe anywhere on the keyboard, except the combined key).
     */
    private fun openSettingsRow() {
        settingsRow?.open()
    }
    
    /**
     * §48: closes the settings row (a downward swipe while it is open, or either of its own buttons
     * being tapped). [onClosed] runs once the close animation has finished collapsing the reserved space.
     */
    private fun closeSettingsRow(onClosed: () -> Unit = {}) {
        settingsRow?.close(onClosed) ?: onClosed()
    }
    
    /**
     * §48: the settings row's emoji button - closes the row and opens the emoji panel, exactly like the
     * combined key used to before §49 retired its dual purpose.
     */
    private fun openEmojiPanelFromSettingsRow() {
        closeSettingsRow { setSurface(InputSurface.EMOJI) }
    }
    
    /**
     * §48: the settings row's gear button - closes the row and launches [SettingsActivity], the same
     * `launchFromKeyboard` mechanism already used for the onboarding calibration/model-import screens.
     */
    private fun openSettingsAppFromSettingsRow() {
        closeSettingsRow { launchFromKeyboard(SettingsActivity::class.java) }
    }
    
    /**
     * §69: the settings row's clear-clipboard button - closes the row and immediately wipes the clipboard,
     * reusing the same [clearClipboard] the D-36/D-38 quick-paste flow already calls after a paste (P+
     * `clearPrimaryClip()` / the pre-P `newPlainText("", "")` fallback), just triggered directly by the
     * user instead of automatically after a paste.
     */
    private fun clearClipboardFromSettingsRow() {
        closeSettingsRow { clearClipboard() }
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
            clearComposing()
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
        // §41: a real, non-collapsed text selection takes priority over the ordinary single-character
        // delete below - Backspace must remove the selection itself, matching every other editor, not the
        // character before the cursor (which the selection may not even be adjacent to).
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            if (composing.isNotEmpty()) {
                ic.finishComposingText()
                clearComposing()
                clearSuggestions()
            }
            pendingMergeChar = null
            ic.commitText("", 1)
            return
        }
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
     * Removes the character immediately before the composing token's logical edit point (D-62: this may
     * be mid-word, not the end, while a reclaimed "after" fragment still follows), keeping the ambiguity
     * flags (A-05) in step and refreshing the composing text / suggestions. Re-arms Shift when the removed
     * character was uppercase (G-05 addendum).
     *
     * When the edit point is already at the very start of composing - nothing left to remove on this side,
     * though a reclaimed "after" fragment may still be sitting there - the real character just before the
     * composing region is deleted instead, via [deleteOneBefore].
     *
     * @param duringRepeat D-138: true when called from [handleBackspaceRepeat] - passed through to
     *        [refreshSuggestions] to skip its more expensive per-keystroke lookups for this tick (see there)
     */
    private fun deleteComposingChar(ic: InputConnection, duringRepeat: Boolean = false) {
        if (composingCursor == 0) {
            // D-62: batched so the app never observes the selection mid-shift - deleteOneBefore()'s
            // deleteSurroundingText() moves the still-active composing region (holding the untouched
            // "after" fragment) left by one character before composingAnchor is adjusted to match.
            ic.beginBatchEdit()
            try {
                if (deleteOneBefore(ic) && composingAnchor >= 0) {
                    composingAnchor--
                }
            } finally {
                ic.endBatchEdit()
            }
            return
        }
        val index = composingCursor - 1
        val deleted = composing[index]
        composingFlags.removeAt(index)
        if (index < composingTaps.size) {
            composingTaps.removeAt(index)
        }
        composing.deleteCharAt(index)
        composingCursor--
        if (composing.isEmpty()) {
            ic.setComposingText("", 1)
            ic.finishComposingText()
            clearSuggestions()
            composingAnchor = -1
        } else {
            updateComposing(ic)
            refreshSuggestions(duringRepeat)
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
            deleteComposingChar(ic, duringRepeat = true)
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
     * @return D-122: the net number of characters this call actually inserted into the document (the
     *         committed word plus [delimiter], minus any characters it deleted immediately before the
     *         former composing token) - used by [splitComposingAtCaretAndCommit] to compute the "after"
     *         half's new anchor arithmetically instead of re-reading the editor's state after several
     *         prior edits in the same batch, see its own KDoc for why that mattered
     */
    private fun finalizeAndCommit(ic: InputConnection, delimiter: String, spaceInferred: Char? = null): Int {
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
            // D-137: a standalone digit or punctuation mark - composing was already empty - is exactly how
            // typing a time ("14:30") actually reaches this function: per the KeyCode.CHAR handler's own
            // "a leading digit is a delimiter" rule, digits (and punctuation like `:`) never sit in
            // `composing` when typed as a fresh token, so the ordinary word-commit path further below -
            // where showNextWordPredictions() normally runs - is never reached by a time at all. This is
            // why the original wiring inside showNextWordPredictions() alone never fired for the reported
            // case; checked here too, where digit/punctuation commits actually land.
            if (!showTimeSuggestion(ic)) {
                clearSuggestions()
            }
            // A standalone letter-ambiguous space is a spurious space the next token may merge back onto.
            pendingMergeChar = spaceInferred
            armShiftForNextWord(ic)
            return delimiter.length
        }
        // D-119 / D-120: a caret sitting mid-word (not at the composing token's true end - a common state
        // since §58's reclaim-on-caret-move) means this delimiter must split the token at the caret,
        // rather than finalising the whole token with the delimiter appended at its end - previously true
        // regardless of caret position, for every delimiter (SPACE, typed punctuation, a long-press
        // symbol via commitLongPressSymbol()), since they all funnel through here.
        if (composingCursor != composing.length) {
            pendingMergeChar = mergeChar // restore for the recursive "before" finalisation below
            return splitComposingAtCaretAndCommit(ic, delimiter, spaceInferred)
        }
        pendingSuggestionSpace = false
        
        // G-05: a word-end Shift made this token's casing explicit; commit it exactly as composed,
        // bypassing autocorrect, capitalisation (§6) and token repair — the user has hand-finished it.
        if (composingCaseLocked) {
            return commitVerbatim(ic, delimiter)
        }
        val typed = composing.toString()
        // A-03: pick the dictionary/capitalisation for the recent context (German default, English or
        // Greek when explicitly active, English also auto-detected while German is active); suppress = a
        // confidently-foreign but unsupported language. D-106 stage 2: also suppressed when the token is
        // already a known word in some other consulted language (mandatory English + every G-01-cycle
        // language) - an embedded loanword like "Word" must never be silently corrected away.
        val dictChoice = selectActiveDictionary("$tokenContextBefore $typed")
        val suppressAutocorrect = dictChoice.suppressAutocorrect || knownInOtherLanguage(typed)
        
        // A-06: merge the token onto a preceding spurious letter-ambiguous space, when linguistically valid.
        if (mergeChar != null) {
            val merged = tokenRepair.tryMerge(previousWord, mergeChar, typed)
            if (merged != null) {
                val committedLength = applyMerge(ic, merged, delimiter)
                armShiftForNextWord(ic)
                return committedLength
            }
        }
        
        // A-05: split the token at a space-ambiguous tap or a fully missed space, when valid. D-48: a token
        // that is a real word once its German diacritics are restored (umlauts / ß are first-class
        // characters) is never split - "konnen" is "können", not "ko nen". D-67 generalises this: a split
        // is also vetoed when a high-confidence single-word autocorrect exists (a low edit cost, e.g. a
        // single adjacent-key slip) - "kleiben" is "kleinen" (b/n are adjacent keys), not "klei" + "en".
        // Both restorations are left to the normal autocorrect path below; here they only veto the split.
        val diacriticWord = if (suppressAutocorrect) null else provider.diacriticRestoration(typed, previousWord)
        val highConfidenceWord = if (suppressAutocorrect) null else provider.highConfidenceCorrection(typed, previousWord)
        val split = if (diacriticWord != null || highConfidenceWord != null) {
            null
        } else {
            tokenRepair.trySplit(typed, spaceAmbiguousIndices(), previousWord)
        }
        if (split != null) {
            val committedLength = applySplit(ic, split, delimiter, typed)
            armShiftForNextWord(ic)
            return committedLength
        }
        
        // A-03: an unsupported foreign context leaves the token as typed; otherwise the selected language's
        // autocorrect applies (A-01 enforced in provider). D-39: when the ordinary edit-distance autocorrect
        // finds nothing, fall back to raw-coordinate correction - walk the token's actual raw taps (T-02) and
        // see whether the geometrically next-most-plausible key at any one position (T-03) produces a known
        // word. This recovers slips the static keyboard-adjacency map cannot see, since that map never looks
        // at where the tap actually landed.
        val corrected = if (suppressAutocorrect) {
            typed
        } else {
            provider.autocorrectFor(typed, previousWord) ?: rawCoordinateCorrection(typed) ?: typed
        }
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
        clearComposing()
        ic.commitText(delimiter, 1)
        learnWord(finalWord)
        reinforceFromTier3(finalWord, tier3Result, contextWord, tier1KnewCorrected)
        
        if (finalWord != typed) {
            undoTyped = typed
            undoCommitted = finalWord
            undoDelimiter = delimiter
            // D-88: the word actually changed - this is an accepted correction, not a plain commit.
            notifySuggestionAccepted(finalWord)
        } else {
            clearUndo()
        }
        // D-43: predict the next word instead of leaving the bar blank.
        showNextWordPredictions()
        armShiftForNextWord(ic)
        trackSustainedEnglishUsage(ic, dictChoice.language)
        return finalWord.length + delimiter.length
    }
    
    /**
     * D-130: promotes A-03's per-token English routing (used while German/Greek stays active) to a real
     * active-language switch after [SUSTAINED_ENGLISH_WORD_THRESHOLD] consecutive commits routed to
     * English - the existing per-token routing, and D-106 stage 2's cross-language autocorrect protection,
     * already work well for a single embedded loanword; a sustained run of English words is a different,
     * stronger signal that the user has genuinely switched languages, not just borrowed one word.
     *
     * @param ic the current input connection
     * @param tokenLanguage the language [finalizeAndCommit] actually routed the just-committed token to
     */
    private fun trackSustainedEnglishUsage(ic: InputConnection, tokenLanguage: Language) {
        if (activeLanguage == Language.ENGLISH || tokenLanguage != Language.ENGLISH) {
            consecutiveEnglishWords = 0
            return
        }
        consecutiveEnglishWords++
        if (consecutiveEnglishWords < SUSTAINED_ENGLISH_WORD_THRESHOLD) {
            return
        }
        consecutiveEnglishWords = 0
        // D-130: acknowledge the switch on the space bar, the same way the manual G-01 swipe now does.
        keyboardView?.beginLanguageChangeFade()
        activeLanguage = Language.ENGLISH
        ActiveLanguageStore.save(this, activeLanguage)
        keyboardView?.greek = false
        keyboardView?.qwerty = true
        updateSpaceLabel()
        clearSuggestions()
        Toast.makeText(this, languageLabel(activeLanguage), Toast.LENGTH_SHORT).show()
        armShiftForNextWord(ic)
    }
    
    /**
     * D-39: raw-coordinate fallback correction, tried only once the ordinary edit-distance autocorrect has
     * found nothing. Generates respellings of [typed] from where its composing taps actually landed (T-02),
     * scored against the personal offset model (T-03), and returns the first one that is a known,
     * non-blacklisted word - or null when none qualifies.
     *
     * @param typed the composing token as typed
     * @return a raw-coordinate-derived correction, or null when there is none
     */
    private fun rawCoordinateCorrection(typed: String): String? {
        val model = offsetModel ?: return null
        val geometry = keyboardView?.charKeyGeometry() ?: return null
        val candidate = RawCoordinateCorrection.respellings(typed, composingTaps, geometry, model)
            .firstOrNull { provider.isKnownWord(it) } ?: return null
        // A-01: never override a word that is already valid (autocorrectFor enforces this too, but it
        // returns null for a known word just like it does for "no correction found" - this fallback must
        // not then reinterpret that as "try harder") - except (§44) when the typed word is itself
        // dramatically rarer than [candidate], matching autocorrectFor's own A-01 override so the two paths
        // agree rather than one silently re-protecting what the other already decided to correct.
        if (provider.isKnownWord(typed) && !provider.shouldOverrideKnownWord(typed, candidate)) {
            return null
        }
        return candidate
    }
    
    /**
     * Commits the composing token exactly as composed, followed by [delimiter] (G-05): no autocorrect,
     * no §6 capitalisation and no token repair. Used when the user fixed the token's casing explicitly
     * via a word-end Shift, which ranks as explicit input and must be preserved in both directions.
     *
     * @return D-122: the number of characters committed (`word.length + delimiter.length`) - see
     *         [finalizeAndCommit]'s own return contract
     */
    private fun commitVerbatim(ic: InputConnection, delimiter: String): Int {
        val word = composing.toString()
        ic.setComposingText(word, 1)
        ic.finishComposingText()
        clearComposing()
        resetWordEndShift()
        ic.commitText(delimiter, 1)
        learnWord(word)
        clearUndo()
        // D-43: predict the next word instead of leaving the bar blank.
        showNextWordPredictions()
        armShiftForNextWord(ic)
        return word.length + delimiter.length
    }
    
    /**
     * Applies an A-06 merge: drops the composing token, removes the spurious preceding space and
     * commits the reconstructed word (cased per §6) followed by [delimiter].
     *
     * @return D-122: the net number of characters committed, accounting for the deleted preceding space
     *         (`cased.length + delimiter.length - 1`) - see [finalizeAndCommit]'s own return contract
     */
    private fun applyMerge(ic: InputConnection, merged: String, delimiter: String): Int {
        ic.setComposingText("", 1)
        ic.finishComposingText()
        clearComposing()
        ic.deleteSurroundingText(1, 0)
        val cased = capitalisation.capitalise(merged, contextFor(merged))
        ic.commitText(cased + delimiter, 1)
        learnWord(cased)
        clearUndo()
        // D-43: predict the next word instead of leaving the bar blank.
        showNextWordPredictions()
        return cased.length + delimiter.length - 1
    }
    
    /**
     * Applies an A-05 split: drops the composing token and commits the two words (each cased per §6)
     * separated by a space, followed by [delimiter]. A-07: the split is armed for undo, so a single
     * backspace immediately after rejoins the two words back into the originally typed token.
     *
     * @param typed the original token as typed, restored if the following key is a backspace undo
     * @return D-122: the number of characters committed (`committed.length + delimiter.length`) - see
     *         [finalizeAndCommit]'s own return contract
     */
    private fun applySplit(ic: InputConnection, split: SplitResult, delimiter: String, typed: String): Int {
        ic.setComposingText("", 1)
        ic.finishComposingText()
        clearComposing()
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
        // D-43: predict the next word (following the right-hand split part) instead of a blank bar.
        showNextWordPredictions()
        return committed.length + delimiter.length
    }
    
    /**
     * D-122: commits a tapped mid-word connector-split suggestion (a [SuggestionController.Kind.NORMAL]
     * bar word containing a space - see [midWordConnectorSplitSuggestion], the only source of such a word)
     * via the same [applySplit] the automatic A-05 commit-time path uses, so per-half capitalisation, the
     * A-07 undo arming and D-13 learning all match exactly. [word] is already display-capitalised
     * (`"$left $right"`); [applySplit] recomputes the same capitalisation itself from the lower-cased
     * [SplitResult] it expects, which is deterministic against the still-current context and therefore
     * harmless, not merely redundant.
     *
     * @param ic the current input connection
     * @param word the pre-capitalised `"$left $right"` suggestion text
     */
    private fun applyMidWordSplitSuggestion(ic: InputConnection, word: String) {
        val (left, right) = word.split(' ', limit = 2)
        applySplit(ic, SplitResult(left.lowercase(), right.lowercase()), delimiter = " ", typed = composing.toString())
        // D-88: tapping a bar suggestion is always an accepted suggestion, regardless of whether it happens
        // to match what was typed.
        notifySuggestionAccepted(word)
        // D-29: arm the trailing space applySplit's own delimiter added, so immediate punctuation removes it.
        pendingSuggestionSpace = true
        // D-123: guard applySplit's own commitText() echo against clearing the flag just armed (mirrors the
        // matching guard in the ordinary NORMAL branch).
        suppressNextReclaimSpaceReset = true
        armShiftForNextWord(ic)
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
        // D-43: predict the next word (following the word the user insisted on) instead of a blank bar.
        showNextWordPredictions()
        armShiftForNextWord(ic)
    }
    
    private fun updateComposing(ic: InputConnection) {
        val text = composing.toString()
        // D-62: setComposingText always leaves the real cursor at the end of the composed text; while
        // editing mid-word (a reclaimed "after" fragment still follows composingCursor) a follow-up
        // setSelection() must pull it back to the logical edit point. Both calls are batched so the app
        // only ever observes the final, consistent selection - never the transient end-of-text one
        // setComposingText alone would produce, which onUpdateSelection's ownEdit check (D-62) would not
        // recognise as ours and would wipe the token it is itself in the middle of updating.
        val needsCursorFixup = composingAnchor >= 0 && composingCursor != composing.length
        if (needsCursorFixup) {
            ic.beginBatchEdit()
        }
        try {
            // S-05 / D-25: colour the recognised word's TEXT (not its background) while composing (C-04).
            // §47: a token about to A-05-split if finalised now gets the same colour over each resulting
            // half instead, with the dropped character / boundary left uncoloured between them - checked
            // first, since trySplit() never matches an already-known word (mutually exclusive by
            // construction with the single-word case below).
            val split = splitPreview(ic, text)
            if (split != null) {
                val (leftRange, rightRange) = split.spanRanges(text)
                val span = SpannableString(text)
                span.setSpan(ForegroundColorSpan(config.highlightColor), leftRange.first, leftRange.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(ForegroundColorSpan(config.highlightColor), rightRange.first, rightRange.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ic.setComposingText(span, 1)
            } else if (shouldHighlightComposing(ic, text)) {
                val span = SpannableString(text)
                span.setSpan(ForegroundColorSpan(config.highlightColor), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ic.setComposingText(span, 1)
            } else {
                ic.setComposingText(text, 1)
            }
            if (needsCursorFixup) {
                val absolute = composingAnchor + composingCursor
                ic.setSelection(absolute, absolute)
            }
        } finally {
            if (needsCursorFixup) {
                ic.endBatchEdit()
            }
        }
    }
    
    /**
     * Resets the in-progress composing token to empty, including the D-62 mid-word edit-point state kept
     * in step with it. Every commit / cancel / caret-move path that discards the current token calls this
     * instead of repeating the field-by-field reset.
     */
    private fun clearComposing() {
        composing.setLength(0)
        composingFlags.clear()
        composingTaps.clear()
        composingCursor = 0
        composingAnchor = -1
    }
    
    /**
     * Inserts [ch] into the composing token at the current logical edit point (D-62: this may be mid-word,
     * not the end, while a reclaimed "after" fragment still follows), keeping [ambiguity] (A-05) and the
     * raw [tap] (T-02 / D-39) in lockstep, then advances the edit point past the inserted character.
     */
    private fun insertComposingChar(ch: Char, ambiguity: TapAmbiguity, tap: TapPoint) {
        composing.insert(composingCursor, ch)
        composingFlags.add(composingCursor, ambiguity)
        // composingTaps can trail composing/composingFlags in length (appendLongPressLetter does not track
        // taps at all, D-39's pre-existing scope) - clamp so a stale index never throws; RawCoordinateCorrection
        // already treats any length mismatch as "no candidates" rather than risk a wrong substitution.
        composingTaps.add(composingCursor.coerceAtMost(composingTaps.size), tap)
        composingCursor++
    }
    
    /**
     * D-62: when a new composing token starts with the caret sitting inside (or directly against) an
     * already-committed word, reclaims that word's letters on both sides of the caret into the composing
     * token - via [WordExtent] - so the autocorrect / suggestion pipeline sees the whole word being edited,
     * not just whatever gets typed from here on. A no-op when the caret touches no word.
     *
     * The reclaimed text is deleted from the real editable and re-added to [composing] (with a neutral
     * A-05 flag, since it was not actually just tapped); [tap] seeds the raw taps for the reclaimed
     * characters when available (the ordinary character path always has one), or is left untracked when
     * not (the long-press-letter path, matching its pre-existing composingTaps gap).
     *
     * When letters follow the caret too, the real cursor must be pulled back mid-composing on every
     * subsequent redraw (see [updateComposing]), which needs the absolute document offset of the reclaim -
     * read once via [InputConnection.getExtractedText]. When that is unavailable (a rare editor quirk) the
     * reclaim is skipped outright rather than risk leaving the cursor in the wrong place.
     *
     * @param ic the current input connection
     * @param tap the raw tap to record for the reclaimed characters, or null to leave them untracked
     */
    private fun reclaimSurroundingWord(ic: InputConnection, tap: TapPoint?) {
        val after = ic.getTextAfterCursor(MAX_CONTEXT_LOOKBACK, 0) ?: ""
        val reclaim = WordExtent.reclaim(tokenContextBefore, after)
        if (reclaim.before.isEmpty() && reclaim.after.isEmpty()) {
            return
        }
        var anchor = -1
        if (reclaim.after.isNotEmpty()) {
            // D-87: see ComposingAnchor for why startOffset must be added, not just selectionStart alone.
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return
            anchor = ComposingAnchor.resolve(extracted.startOffset, extracted.selectionStart, reclaim.before.length)
        }
        ic.deleteSurroundingText(reclaim.before.length, reclaim.after.length)
        // D-84: the reclaimed "before" fragment moves into composing now, so it is no longer part of the
        // context *preceding* the token - trim it from tokenContextBefore too, or it would appear twice
        // (once here at its tail, once inside composing) in every "$tokenContextBefore $typed"-style string
        // built later (refreshSuggestions(), finalizeAndCommit()). That duplication was silently confusing
        // the A-03 language classifier into misreading the context as foreign and suppressing suggestions
        // and autocorrect entirely for the rest of the token - the reported "no suggestions at all" bug.
        tokenContextBefore = tokenContextBefore.dropLast(reclaim.before.length)
        composing.append(reclaim.before)
        composingFlags.addAll(List(reclaim.before.length) { TapAmbiguity.NONE })
        if (tap != null) {
            composingTaps.addAll(List(reclaim.before.length) { tap })
        }
        composingCursor = composing.length
        composing.append(reclaim.after)
        composingFlags.addAll(List(reclaim.after.length) { TapAmbiguity.NONE })
        if (tap != null) {
            composingTaps.addAll(List(reclaim.after.length) { tap })
        }
        composingAnchor = anchor
    }
    
    /**
     * D-119 / D-120: called by [finalizeAndCommit] when a delimiter (SPACE, typed punctuation, or a
     * long-press symbol) arrives while the caret sits mid-word (not at the composing token's true end) -
     * a common state since §58's reclaim-on-caret-move. Previously every delimiter unconditionally
     * finalised the *whole* composing token with itself appended at the end, regardless of where the
     * caret actually was - wrongly turning "insert this character here" into "I have finished typing this
     * word" (D-119: SPACE), and always landing the delimiter after the whole word instead of at the caret
     * (D-120: e.g. a quote typed before an existing word ended up after it).
     *
     * Splits the composing token in two at the caret: the part before it is finalised exactly like any
     * ordinary word (autocorrect, capitalisation, committed with [delimiter] via a recursive
     * [finalizeAndCommit] call - so the delimiter lands exactly where the caret was, between the two
     * halves); the part after it becomes a fresh composing token, re-seeded at the caret's own new
     * position (via the same [composingAnchor] mid-word-edit-point mechanism [reclaimSurroundingWord]
     * already uses) so editing continues right where the caret was - never jumped to the token's end.
     *
     * D-122 fix: the "after" half's new anchor is computed **arithmetically** - the "before" half's own
     * anchor (captured before it is finalised) plus however many characters [finalizeAndCommit] reports
     * actually landed in the document - rather than by re-reading [InputConnection.getExtractedText] right
     * after that recursive call. The previous version did exactly that re-read, still inside the same batch
     * edit and after several prior [InputConnection] mutations (the recursive finalise's own
     * `setComposingText`/`finishComposingText`/`commitText` calls) - a markedly riskier pattern than every
     * other same-batch state read in this class, which all read *before* mutating rather than after several
     * mutations. Confirmed bug (not a guess): typing "Testcwort", deleting the `c` mid-word and pressing
     * SPACE left the caret *before* the just-inserted space instead of after it, in a case where the
     * "before" half ("Test") does not even change length under autocorrect - ruling out an autocorrect-
     * length-mismatch explanation and pointing squarely at the anchor computation itself. The arithmetic
     * form needs no such read at all.
     *
     * @param ic the current input connection
     * @param delimiter the character(s) to commit between the split halves
     * @param spaceInferred forwarded to the recursive [finalizeAndCommit] call for the "before" half (A-06)
     * @return the number of characters [finalizeAndCommit] committed for the "before" half, so an outer,
     *         top-level caller of `finalizeAndCommit` that happens to land here still gets a meaningful
     *         value under the same return contract (see there)
     */
    private fun splitComposingAtCaretAndCommit(ic: InputConnection, delimiter: String, spaceInferred: Char?): Int {
        val splitAt = composingCursor
        val beforeAnchor = composingAnchor
        val beforeText = composing.substring(0, splitAt)
        val beforeFlags = ArrayList(composingFlags.subList(0, splitAt))
        val beforeTaps = ArrayList(composingTaps.subList(0, splitAt.coerceAtMost(composingTaps.size)))
        val afterText = composing.substring(splitAt)
        val afterFlags = ArrayList(composingFlags.subList(splitAt, composingFlags.size))
        val afterTaps = if (splitAt < composingTaps.size) {
            ArrayList(composingTaps.subList(splitAt, composingTaps.size))
        } else {
            ArrayList()
        }
        var committedLength: Int
        ic.beginBatchEdit()
        try {
            // Shrink the real composing region to just the "before" half first - otherwise finalising it
            // below would replace the *whole* existing composing span (before+after) and silently discard
            // the "after" half, which only exists in memory here, never yet committed anywhere.
            ic.setComposingText(beforeText, 1)
            composing.setLength(0)
            composing.append(beforeText)
            composingFlags.clear()
            composingFlags.addAll(beforeFlags)
            composingTaps.clear()
            composingTaps.addAll(beforeTaps)
            composingCursor = beforeText.length
            composingAnchor = -1
            committedLength = finalizeAndCommit(ic, delimiter, spaceInferred)
            
            // Re-seed the "after" half as a fresh composing token, right where the caret was - mirroring
            // how a brand-new word normally starts (D-62). The new anchor is simply where "before" itself
            // started plus however much it (and the delimiter) actually added to the document - see the
            // D-122 note above for why this is no longer a getExtractedText() read.
            captureTokenContext(ic)
            resetWordEndShift()
            val anchor = if (beforeAnchor >= 0) beforeAnchor + committedLength else -1
            composing.append(afterText)
            composingFlags.addAll(afterFlags)
            composingTaps.addAll(afterTaps)
            composingCursor = 0
            composingAnchor = anchor
            updateComposing(ic)
        } finally {
            ic.endBatchEdit()
        }
        refreshSuggestions()
        return committedLength
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
        return config.highlightEnabled && provider.isKnownWord(text) && !isEditingMidWord(ic)
    }
    
    /**
     * §47: whether the composing token would currently A-05-split if finalised right now, for the live
     * split-colour preview - gated the same way as [shouldHighlightComposing] (the highlight setting must
     * be on, and D-26's mid-word rule applies equally here: a reclaimed fragment being edited mid-word must
     * not be coloured). [TokenRepair.trySplit] itself already never matches an already-known word, so this
     * and the single-word highlight are naturally mutually exclusive.
     *
     * @param ic the current input connection
     * @param text the composing token
     * @return the split preview, or null when none applies right now
     */
    private fun splitPreview(ic: InputConnection, text: String): SplitResult? {
        if (!config.highlightEnabled || isEditingMidWord(ic)) {
            return null
        }
        return tokenRepair.trySplit(text, spaceAmbiguousIndices(), previousWord)
    }
    
    /**
     * D-26: whether the cursor sits inside an existing word rather than at its end - a letter immediately
     * following the cursor means the current edit is correcting mid-word, so the composing fragment must
     * not be coloured (used by both [shouldHighlightComposing] and [splitPreview]).
     */
    private fun isEditingMidWord(ic: InputConnection): Boolean {
        val after = ic.getTextAfterCursor(1, 0)
        return !after.isNullOrEmpty() && after[0].isLetter()
    }
    
    /**
     * @param duringRepeat D-138: true while called from an active [handleBackspaceRepeat] tick - skips the
     *        D-106-stage-2 / D-111-D-112 lookups below, which are never actually read mid-repeat (the bar
     *        is changing every 45-330ms, see [BackspaceRepeat]) but add several extra per-keystroke
     *        dictionary round-trips that measurably compete with the repeat's own fastest tick interval -
     *        a plausible, traced (not guessed) explanation for backspace-hold feeling jerky after they were
     *        added. Every other call site keeps the full live preview (the default `false`).
     */
    private fun refreshSuggestions(duringRepeat: Boolean = false) {
        val input = composing.toString()
        if (input.isEmpty()) {
            clearSuggestions()
            return
        }
        // A-03: pick the dictionary for the recent context; an unsupported foreign context shows nothing.
        if (selectActiveDictionary("$tokenContextBefore $input").suppressAutocorrect) {
            clearSuggestions()
            return
        }
        val candidates = provider.suggestionsFor(input, previousWord)
        // D-122 / D-131: both kept out of `candidates` itself so neither ever enters the tier-1/tier-3
        // merge's own score normalisation (SuggestionMerger normalises every tier-1 score against the
        // list's own maximum, so one inflated synthetic entry would compress every real candidate's
        // contribution toward zero) - added only to what is actually displayed, at every
        // controller.update() call site below (see extraSuggestions()).
        val splitSuggestion = if (duringRepeat) null else midWordConnectorSplitSuggestion(input)
        // D-131: D-39's raw-coordinate fallback becomes a live, incremental signal instead of only ever
        // resolving at the final delimiter - reuses rawCoordinateCorrection() exactly as finalizeAndCommit()
        // already does at commit time (same A-01/§44 checks inside it), just consulted here too. Gated on
        // emptiness and !duringRepeat like D-116/D-117: it needs composingTaps/keyboard-geometry lookups,
        // and D-138 already flagged stacking extra per-keystroke lookups as a real, previously-felt cost.
        val rawCoordinateSuggestion = if (duringRepeat || candidates.isNotEmpty()) {
            null
        } else {
            rawCoordinateCorrection(input)?.let { word -> Suggestion(word, MAX_PRIORITY_SUGGESTION_SCORE) }
        }
        val extras = listOfNotNull(splitSuggestion, rawCoordinateSuggestion)
        val pending = if (duringRepeat) {
            provider.autocorrectFor(input, previousWord)
        } else {
            // D-106 stage 2: never pend a silent replacement for a word already known in another consulted
            // language (mandatory English + every G-01-cycle language) - the active language's own
            // completions are still shown as usual, only the impending-autocorrect chip is suppressed.
            val correctionCandidate = if (knownInOtherLanguage(input)) null else provider.autocorrectFor(input, previousWord)
            // D-111 / D-112: run the eventual committed form through the same §6 capitalisation
            // finalizeAndCommit() will apply, so a pending *case-only* change (D-111 - e.g. an ordinary noun
            // about to be auto-capitalised) is visible as the existing S-06 pending chip before it is ever
            // silently applied, and a spelling correction's own case already follows the sentence/field
            // context (D-112 - "Fur" at a sentence start previews as "Für", not "für"). Deliberately mirrors
            // only the autocorrectFor/diacritic-fold path (cost-0 within it, so already covered) and not the
            // rarer raw-coordinate-correction fallback, which needs the real composing taps/geometry and
            // isn't worth computing on every keystroke just for this preview.
            val capitalizedPreview = capitalisation.capitalise(correctionCandidate ?: input, contextFor(input))
            capitalizedPreview.takeIf { it != input }
        }
        val previous = previousWord
        val sentence = "$tokenContextBefore$input"
        // §9 / C-06: consult tier 3 when the tier-1 confidence is below the threshold (never with the
        // no-op backend, where the outcome is the tier-1 list unchanged and no capitalisation proposal).
        // A-02: the mini-LLM sees the whole running context, not a punctuation-truncated fragment.
        val seq = ++tier3RequestSeq
        if (!tier3Async) {
            // No-op (or absent) backend: the orchestrator is instant, run it inline.
            applyTier3Outcome(
                input,
                pending,
                tier3.predict(input, previous, sentence, candidates, settings.llmActivationThreshold, config.maxSuggestions),
                extras
            )
            return
        }
        // A real backend runs the LLM: show the tier-1 suggestions immediately, then refine off-thread so
        // the IME never blocks. A stale result (the token changed meanwhile) is discarded via the sequence.
        controller.update(input, candidates + extras, pending)
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
                    applyTier3Outcome(input, pending, outcome, extras)
                }
            }
        }
    }
    
    /**
     * Applies a tier-3 orchestration outcome to the suggestion bar: stores the §6 capitalisation proposal
     * and the raw result (for the adaptive-learning feedback) and refreshes the bar.
     *
     * @param extras D-122's mid-word connector-split candidate and/or D-131's live raw-coordinate
     *        suggestion, appended after the tier-1/tier-3 merge rather than fed into it - see
     *        [refreshSuggestions]
     */
    private fun applyTier3Outcome(input: String, pending: String?, outcome: Tier3Outcome, extras: List<Suggestion> = emptyList()) {
        lastTier3Result = outcome.tier3
        lastCapProposal = outcome.capitalisation
        controller.update(input, outcome.suggestions + extras, pending)
        showSuggestions()
        scheduleResort()
    }
    
    /**
     * D-122: while the user is actively re-editing an existing word mid-word ([isEditingMidWord] - a much
     * stronger "please fix this word" signal than ordinary forward typing), offers a would-be split at an
     * unresolved [TokenRepair.OVER_SPACE_LETTERS] connector even without [TokenRepair.trySplit]'s usual
     * bigram-co-occurrence requirement (relaxing that gate for *ordinary* typing would reopen the exact
     * "any two known fragments get cut apart" false-positive problem §45 fixed - restricting it to this
     * deliberate re-edit moment is what makes it safe). Suggestion-only, exactly like D-116's compound
     * recognition - never silently applied, only ever offered, with a deliberately maximal score so it
     * sorts first ("noticeably higher priority", as requested). Already capitalised as it would actually
     * commit (mirroring [applySplit]'s own two calls), matched by [onSuggestionClicked]'s own tap handling
     * for such a candidate.
     *
     * @param input the composing token
     * @return the split suggestion, pre-capitalised as `"$left $right"`, or null when none applies
     */
    private fun midWordConnectorSplitSuggestion(input: String): Suggestion? {
        if (currentInputConnection?.let { isEditingMidWord(it) } != true) {
            return null
        }
        val split = tokenRepair.splitAtUnresolvedConnector(input, previousWord) ?: return null
        val left = capitalisation.capitalise(split.left, contextFor(split.left))
        val right = capitalisation.capitalise(split.right, followingPartContext())
        return Suggestion("$left $right", MAX_PRIORITY_SUGGESTION_SCORE)
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
    
    /**
     * D-43: fills the (otherwise empty) suggestion bar with next-word predictions after a word has been
     * committed - the most likely words to follow [previousWord] by bigram probability. Falls back to an
     * empty bar when there is no context word or no prediction (e.g. at the very start of an entry). The
     * tier-3 mini-LLM refines the bar the moment the user starts typing the next token; this baseline just
     * makes the pause between words useful.
     */
    private fun showNextWordPredictions() {
        handler.removeCallbacks(resortRunnable)
        lastTier3Result = Tier3Result.EMPTY
        lastCapProposal = null
        val previous = previousWord
        val predictions = (if (previous == null) emptyList() else provider.nextWordSuggestions(previous)) +
            listOfNotNull(timeSuggestion())
        if (predictions.isEmpty()) {
            clearSuggestions()
            return
        }
        controller.clear()
        controller.update("", predictions, null)
        showSuggestions()
    }
    
    /**
     * D-137: a typed time (`14:30`) is essentially always followed by "Uhr" in German, regardless of what
     * (if anything) the bigram table happens to know about that exact, effectively unique digit token -
     * offered with a deliberately maximal score, same reasoning as D-122's split suggestion, so it always
     * sorts first among whatever [showNextWordPredictions] otherwise finds.
     *
     * @return the "Uhr" suggestion, or null when the text just committed does not end in a time
     */
    private fun timeSuggestion(): Suggestion? {
        val before = currentInputConnection?.getTextBeforeCursor(TIME_LOOKBACK, 0) ?: return null
        return if (TimePattern.endsWithTime(before)) Suggestion(UHR, MAX_PRIORITY_SUGGESTION_SCORE) else null
    }
    
    /**
     * D-137: the empty-composing branch's own equivalent of [timeSuggestion] - called right after a
     * standalone digit/punctuation commit (see [finalizeAndCommit]'s own call site for why that path, not
     * [showNextWordPredictions], is what actually needs this for a typed time).
     *
     * @param ic the current input connection
     * @return true when a time was found and the suggestion bar now shows the "Uhr" prediction
     */
    private fun showTimeSuggestion(ic: InputConnection): Boolean {
        val before = ic.getTextBeforeCursor(TIME_LOOKBACK, 0) ?: return false
        if (!TimePattern.endsWithTime(before)) {
            return false
        }
        handler.removeCallbacks(resortRunnable)
        lastTier3Result = Tier3Result.EMPTY
        lastCapProposal = null
        controller.clear()
        controller.update("", listOf(Suggestion(UHR, MAX_PRIORITY_SUGGESTION_SCORE)), null)
        showSuggestions()
        return true
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
                clearComposing()
                controller.declineAutocorrect()
                // S-06: repeated verbatim confirmation is a learning signal (cf. B-03).
                learnWord(typed)
                clearSuggestions()
            }
            
            // Complete the token with the chosen word (cased per §6) and start a new one.
            SuggestionController.Kind.NORMAL -> {
                // D-122: a suggestion word containing a space can only be the mid-word connector-split
                // candidate (see midWordConnectorSplitSuggestion) - no other suggestion source in this
                // codebase ever produces a multi-word candidate - so it needs applySplit()'s own per-half
                // capitalisation and undo/learn wiring, not the single-word path below.
                if (item.word.contains(' ')) {
                    applyMidWordSplitSuggestion(ic, item.word)
                    return
                }
                val word = capitalisation.capitalise(item.word, contextFor(composing.toString()))
                ic.commitText(word + " ", 1)
                clearComposing()
                learnWord(word)
                // D-88: tapping a bar suggestion is always an accepted suggestion, regardless of whether it
                // happens to match what was typed.
                notifySuggestionAccepted(word)
                // D-43: after accepting a bar word, predict the next one so the flow continues.
                showNextWordPredictions()
                // D-29: arm the trailing space added here so an immediately following punctuation removes it.
                pendingSuggestionSpace = true
                // D-123: this commitText() above will generate its own onUpdateSelection callback shortly,
                // which - composing already empty by then - calls reclaimWordAtCaret(); guard it against
                // clearing the flag just armed, since that callback is only the echo of this very commit,
                // not a genuine subsequent tap elsewhere.
                suppressNextReclaimSpaceReset = true
                armShiftForNextWord(ic)
            }
            
            // §38 (reverted from D-36 / D-60's commitText()): fires the editor's own native paste action
            // instead of committing the clipboard text directly, so an app whose paste handling does
            // something beyond plain text insertion (e.g. a notes app splitting pasted lines into separate
            // list entries the way it would for the user's own long-press "Paste") sees that too - a plain
            // commitText() call looks like nothing more than fast typing to such an app. D-36 / D-60's two
            // original objections to this: (1) performContextMenuAction(paste) is not honoured by every
            // field, silently doing nothing there - still true, and not fixed here; accepted as a known risk
            // now that the Notes-app benefit outweighs it. (2) it races an immediate clearClipboard() - the
            // field often read an already-emptied clipboard - fixed by delaying the clear instead
            // (scheduleClipboardClear(), §38), long enough for the target's async paste handling to have
            // actually read the clipboard first. Non-sensitive content is no longer cleared at all (only a
            // password, or anything else the copying app flagged sensitive, ever gets cleared - see
            // isSensitiveClip()).
            SuggestionController.Kind.CLIPBOARD -> {
                val clip = clipboardManager?.takeIf { it.hasPrimaryClip() }?.primaryClip?.takeIf { it.itemCount > 0 }
                val text = clip?.getItemAt(0)?.coerceToText(this)
                if (!text.isNullOrEmpty()) {
                    ic.performContextMenuAction(android.R.id.paste)
                    if (isSensitiveClip(clip)) {
                        scheduleClipboardClear(text.toString())
                    }
                }
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
     * A-03 / D-106 stage 1: chooses the dictionary for the recent [context]. Greek mode and English mode
     * (both explicit G-01 language choices now, English promoted from an auto-detected-only fallback)
     * force their own lexicon unconditionally; otherwise the detector decides while German is active — a
     * confidently English context uses the English lexicon, a confidently other-foreign context (e.g.
     * French, which has no bundled lexicon) keeps the German store but holds back autocorrect so the text
     * is left as typed, and everything else defaults to German. Conservative by construction (see
     * [LanguageClassifier.isForeign]).
     */
    private fun resolveDict(context: String): DictChoice {
        if (activeLanguage == Language.GREEK) {
            return DictChoice(Language.GREEK, suppressAutocorrect = false)
        }
        if (activeLanguage == Language.ENGLISH) {
            return DictChoice(Language.ENGLISH, suppressAutocorrect = false)
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
     * the recent [context] (A-03) and returns the full [DictChoice] - the caller reads
     * [DictChoice.suppressAutocorrect] for the autocorrect gate and, since D-130, [DictChoice.language]
     * to track sustained per-token English routing towards a real active-language switch.
     *
     * @param context the recent text (context before the token plus the token itself)
     * @return the resolved choice for this token
     */
    private fun selectActiveDictionary(context: String): DictChoice {
        val choice = resolveDict(context)
        provider = providers.getValue(choice.language)
        capitalisation = engines.getValue(choice.language)
        dictionaryStore = stores.getValue(choice.language)
        return choice
    }
    
    /**
     * D-106 stage 2: whether [token] is a known word in some bundled dictionary other than the one
     * currently active for it - English is always consulted, and so is every other language reachable
     * via the G-01 [LanguageCycle] (currently every bundled language is simultaneously reachable there).
     * A deliberately plain "known anywhere" check, not a cross-language suggestion ranking: an embedded
     * loanword like "Word" is protected from autocorrect exactly like a locally-known word (A-01),
     * instead of being silently corrected to the nearest similarly-spelled active-language word ("wird").
     *
     * @param token the composing token (any case)
     * @return true when any non-active language's dictionary already knows this exact word
     */
    private fun knownInOtherLanguage(token: String): Boolean {
        return providers.any { (language, otherProvider) -> language != activeLanguage && otherProvider.isKnownWord(token) }
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
        val now = SystemClock.uptimeMillis()
        // D-15 / D-121: a press while Caps Lock is on always releases it - checked first, before the G-05
        // word-end gesture below, so a Caps-Lock-off press mid-word (e.g. right after typing "MCU" with
        // Caps Lock still on, before any delimiter) is never swallowed by G-05 instead, leaving Caps Lock
        // stuck on.
        if (view.capsLock) {
            view.capsLock = false
            view.shifted = false
            lastShiftTime = now
            return
        }
        // G-05 / D-121: Shift at the end of a fully typed word toggles the word's first-letter case - the
        // caret must genuinely sit at the composing token's own end (composingCursor == composing.length),
        // not merely "composing is non-empty": a mid-word Backspace (e.g. removing a leading character to
        // re-edit it) leaves composing non-empty with the caret elsewhere, and a Shift press there is an
        // ordinary case-toggle-for-the-next-letter, not this word-end gesture.
        if (composing.isNotEmpty() && composingCursor == composing.length) {
            handleWordEndShift(view)
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
            KeyCode.SPACE, KeyCode.ENTER, KeyCode.TEXT -> WordEndShift.NextKey.DELIMITER
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
    
    /**
     * A field whose declared type primarily wants digits (a phone number, a plain numeric field, or a
     * date/time field) opens straight to the calculator page instead of the letters surface.
     *
     * @param info the newly focused field's editor info, or null
     * @return the surface [onStartInput] should open the keyboard on
     */
    private fun initialSurfaceFor(info: EditorInfo?): InputSurface {
        val type = info?.inputType ?: return InputSurface.LETTERS
        return when (type and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME -> InputSurface.SYMBOLS
            else -> InputSurface.LETTERS
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
        
        // §38: how long a sensitive clipboard paste is left in place before it is auto-cleared - long
        // enough for the target app's async performContextMenuAction(paste) handling to have actually read
        // it (typically single-digit milliseconds for a plain text field), short enough to keep the window
        // a malicious clipboard-reading app could grab it in small.
        private const val CLIPBOARD_CLEAR_DELAY_MS = 300L
        
        // §60: how much of a clipboard *file*'s content to read for the chip's own already-truncated
        // preview - generous relative to ClipboardPreview.MAX_LENGTH (24), but still a small, bounded read
        // regardless of the file's real size.
        private const val CLIPBOARD_FILE_PREVIEW_CHARS = 512
        
        // D-37: how many times a new word must be committed (without being reverted) before it is promoted
        // to the learned dictionary, so a one-off typo is not eagerly learned.
        private const val LEARN_THRESHOLD = 2
        
        // D-29: sentence / clause punctuation that absorbs an accepted suggestion's trailing space.
        private const val SPACE_EATING_PUNCTUATION = ".,!?;:)"
        
        // D-114: an autocorrect candidate below this absolute frequency is never trustworthy enough to
        // silently apply, however good its edit cost otherwise looks - reported case: "vorhin" (missing
        // from the dictionary entirely) autocorrected to "Virgin" (an English-proper-noun artefact of the
        // German Wikipedia corpus, frequency 62), when no candidate here should have won at all. Calibrated
        // against the bundled dict_de.tsv: every legitimate correction target this session already relies
        // on sits far above this floor (komplett 881, Sankt 968, Standard 1534, kleinen 3748, Wort 4084,
        // können 23227, werden 93866), while known bad low-confidence candidates sit far below it (Virgin
        // 62, Vorhinein 11) - a candidate this rare is dropped from autocorrect consideration outright
        // (though it can still surface in the broader suggestion-bar prefix/fuzzy list, unaffected).
        private const val MIN_AUTOCORRECT_CANDIDATE_FREQUENCY = 300L
        
        // D-130: consecutive commits routed to English (while German/Greek stays active) before
        // trackSustainedEnglishUsage() promotes it to a real active-language switch.
        private const val SUSTAINED_ENGLISH_WORD_THRESHOLD = 5
        
        // D-135: at most this many Autofill inline suggestions are requested/shown at once; each is
        // inflated at this width (a reasonable single-suggestion chip width - the platform itself decides
        // the actual rendered content within it).
        private const val INLINE_SUGGESTION_MAX_COUNT = 3
        private const val INLINE_SUGGESTION_WIDTH_DP = 160
        
        // D-122 / D-137: comfortably above any real dictionary frequency (the largest bundled entries sit
        // around 1e6, see MIN_AUTOCORRECT_CANDIDATE_FREQUENCY's own comment) so a synthesised, always-right
        // suggestion (the mid-word connector-split candidate; the "Uhr" time suggestion) always sorts
        // first, without using an extreme value (Double.MAX_VALUE) that could risk odd behaviour in any
        // future score arithmetic.
        private const val MAX_PRIORITY_SUGGESTION_SCORE = 1_000_000_000.0
        
        // D-137: how far back to look for a trailing typed time - long enough for "14:30" plus a
        // reasonable amount of trailing punctuation/whitespace, short enough to stay a cheap read.
        private const val TIME_LOOKBACK = 16
        
        private const val UHR = "Uhr"
    }
}
