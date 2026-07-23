// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Froehlich Media

package de.froehlichmedia.adaptkey

import android.Manifest
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.ContactsContract
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
import android.util.Log
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
import java.util.concurrent.atomic.AtomicInteger
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationContext
import de.froehlichmedia.adaptkey.capitalisation.CapitalisationEngine
import de.froehlichmedia.adaptkey.capitalisation.CapsMode
import de.froehlichmedia.adaptkey.capitalisation.SentenceBoundary
import de.froehlichmedia.adaptkey.capitalisation.ShiftGrace
import de.froehlichmedia.adaptkey.capitalisation.WordEndShift
import de.froehlichmedia.adaptkey.credential.CredentialEntry
import de.froehlichmedia.adaptkey.credential.CredentialRanking
import de.froehlichmedia.adaptkey.credential.CredentialStore
import de.froehlichmedia.adaptkey.credential.LoginFieldDetector
import de.froehlichmedia.adaptkey.diagnostics.DiagnosticLog
import de.froehlichmedia.adaptkey.credential.LoginFieldKind
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
import de.froehlichmedia.adaptkey.keyboard.ExtraRowView
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
    private var extraRow: ExtraRowView? = null
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
    // D-246: S-07 trigram support - the word committed two positions before previousWord, shifted in
    // lockstep with it (see learnWord()/learnWordStrong()) and reset alongside it everywhere previousWord
    // itself resets.
    private var previousPreviousWord: String? = null
    
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
    
    // D-143: whether the currently-focused field is a recognised URL-entry field (TYPE_TEXT_VARIATION_URI)
    // - re-determined fresh per field in onStartInput, mirroring surface/loginFieldKind. Drives the letters
    // surface's URL-mode bottom row (KeyboardLayout.urlBottomRow) and short-circuits autocorrect/
    // capitalisation/suggestions exactly like a recognised login field (see finalizeAndCommit/
    // refreshSuggestions) - a domain/path is not natural-language prose either.
    private var urlMode = false
    
    // D-158: whether the currently-focused field is a recognised email-address field - unlike urlMode,
    // this is *derived* from loginFieldKind (already reliably detected via InputType for D-142's own
    // purposes, see LoginFieldDetector's KDoc) rather than re-parsing EditorInfo a second time; re-derived
    // fresh alongside loginFieldKind in onStartInput. Drives only the letters surface's email-mode bottom
    // row (KeyboardLayout.emailBottomRow) and the G-01 space-swipe suppression - autocorrect/suggestions
    // suppression for an email field is already fully handled by the existing D-142 credential pipeline
    // (loginFieldKind != LoginFieldKind.NONE), so this flag has no role there at all.
    private var emailMode = false
    private lateinit var emojiDataset: EmojiDataset
    private var recentEmojis: List<String> = emptyList()
    
    // D-36: system clipboard, for the direct-paste chip.
    private val clipboardManager by lazy { getSystemService(ClipboardManager::class.java) }
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        settings = SettingsStore.load(this)
        applySettings()
    }
    
    /**
     * D-239: lets this long-lived, resident service notice a touch-calibration change made from a
     * completely different screen (`CalibrationActivity`'s style switch/reset) immediately, rather than
     * only at this service's own fixed reload points (`onCreate`, a non-restarting `onStartInputView`) -
     * see [reloadOffsetModel]'s own updated KDoc for why those alone were not reliably enough.
     */
    private val offsetModelPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        reloadOffsetModel()
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
    // of the composing region's start - resolved by reclaimSurroundingWord() whenever composing text
    // exists (D-159), not only during a genuine mid-word reclaim: used both to place the real cursor back
    // at composingCursor after every setComposingText() call (which otherwise always leaves it at the end)
    // and, more fundamentally, by onUpdateSelection's ownEdit check to recognise our own edits without
    // relying on the target editor's own reported composing span. -1 only while composing is empty, or if
    // an ExtractedText read genuinely failed.
    private var composingCursor = 0
    private var composingAnchor = -1
    
    // D-139: see the KDoc on its use in onUpdateSelection().
    private val selectionUpdateBurstGuard = CallbackBurstGuard()
    
    // D-149: whether the most recent onUpdateSelection - the IME's own authoritative source for selection
    // state - reported a real, non-collapsed selection. See handleBackspace() for why this is trusted over
    // a fresh InputConnection.getSelectedText(0) call.
    private var selectionCollapsed = true
    
    // G-05: a word-end Shift is pending — the first character has been provisionally toggled and the
    // next key decides the outcome (camelCase vs. keep). composingCaseLocked marks a token whose casing
    // the user fixed explicitly, so it is committed verbatim (bypassing autocorrect and §6).
    private var wordEndShiftPending = false
    private var composingCaseLocked = false
    
    private var capsMode = CapsMode.NONE
    private var tokenSentenceStart = false
    private var tokenAfterHyphen = false
    
    // D-142: the currently focused field's login-relevance, classified from EditorInfo in onStartInput.
    // Drives both the suggestion pipeline (the credential list instead of the dictionary for USERNAME/
    // EMAIL, nothing at all for PASSWORD) and what gets learned (see captureCredentialIfLoginField).
    private var loginFieldKind = LoginFieldKind.NONE
    
    // D-142: whether this field's value has already been captured once - prevents double-reinforcing the
    // same observation should both handleEnter() and onFinishInput() fire for the same field.
    private var credentialCaptured = false
    
    // D-142: what LoginFieldDetector.weakSignalKind() found for the current field (only ever computed when
    // loginFieldKind is still NONE) - drives the extra-row auto-open + button flash nudge in
    // onStartInputView(), never suggestion behaviour by itself; also what toggleCredentialModeFromExtraRow()
    // activates into when the user actually taps the nudged button.
    private var weakSignalKind = LoginFieldKind.NONE
    
    // D-142: whether the current loginFieldKind was switched on via the extra-row button rather than
    // reliably detected from InputType - only a manually-activated mode can be toggled back off again by
    // the same button (a reliably-detected EMAIL/PASSWORD field is never user-toggleable).
    private var credentialModeManuallyActivated = false
    
    // D-174: a running in-memory copy of every fragment [commitVerbatimFieldFragment] has committed into
    // the current login-relevant field (reset per field in onStartInput) - [captureCredentialIfLoginField]'s
    // fallback when InputConnection.getTextBeforeCursor() itself is already unusable (see that function's
    // own KDoc for why this is needed at all, confirmed from a real device log: some editors - K9 Mail's
    // recipient field, converting the typed address into a chip - tear the connection down for that field
    // so fast that even onFinishInput()'s own read of it comes back null).
    private val credentialSnapshot = StringBuilder()
    
    // D-191: contact-derived email addresses for the currently focused EMAIL field, loaded once per field
    // focus (see loadContactEmailsAsync/onStartInput) rather than per keystroke, mirroring D-160's own
    // "not on the hot path" reasoning for an external, unpredictably-sized read. null until the async load
    // for the current field completes (or was never started); never persisted - see the class's own KDoc
    // reference to D-191 in AdaptSettings.
    private var contactEmailCache: List<String>? = null
    private val contactsExecutor = Executors.newSingleThreadExecutor()
    
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
    // D-140: exactly what learnWord() did for the committed word(s) - one entry for a plain correction,
    // two (left, right) for a split - so performAutocorrectUndo() can precisely un-learn it, rather than
    // leaving the rejected commit's dictionary/bigram reinforcement in place forever.
    private var undoLearnRecords: List<LearnRecord> = emptyList()
    // D-140: the single touch-model sample a D-39 raw-coordinate correction actually changed, so it alone
    // (never the whole token - an ordinary dictionary/diacritic/split correction is never a touch-
    // resolution error) can be un-trained on undo. Null whenever the correction was not raw-coordinate-based.
    private var undoRawCorrection: RawCorrectionUndo? = null
    
    // D-248: the last few LearnRecords learnWord()/learnWordStrong() produced (every outcome except
    // LearnOutcome.SKIPPED, which changed nothing) - unlike the A-07 undo state above, this deliberately
    // survives any number of intervening keystrokes, not just the one directly after the commit. Checked by
    // maybeUnlearnOnBackspaceReturn() whenever a plain backspace lands with the caret back at the end of one
    // of these words - most commonly after backspacing back through one or more stray Enters that
    // prematurely committed a half-typed word, but not scoped to that shape specifically. Newest last,
    // capped at RECENT_LEARN_HISTORY_SIZE.
    private val recentLearnRecords: MutableList<LearnRecord> = ArrayList()
    
    private val handler = Handler(Looper.getMainLooper())
    private val resortRunnable = Runnable {
        controller.resort()
        showSuggestions()
    }
    
    // D-211: the actual search now runs on this dedicated background thread (mirroring the existing
    // tier3Executor precedent below), so it never blocks the main thread (or the key-press flash render)
    // the way D-160's original Handler.postDelayed-on-the-main-thread debounce still did once its delay
    // elapsed.
    //
    // A genuinely forced thread kill was considered and rejected: Thread.stop() has been deprecated since
    // Java 1.2 for good reason (it can tear a shared data structure - a SQLite cursor mid-iteration, a
    // mutable map - apart at an arbitrary point) and is not reliably available on current JDKs at all.
    // Cooperative cancellation is used instead: expensiveSuggestionSeq is bumped on every fresh (non-
    // deferred) keystroke, and the background search polls it - once at the very start (a call that is
    // already stale before it even begins never touches the database at all) and again once per candidate
    // inside DictionarySuggestionProvider's own costlier searches (see their KDoc), so a call superseded
    // partway through stops there rather than finishing pointless work. The result is checked for
    // staleness a third time, on the main thread, right before it would ever be applied - so a callback
    // that still manages to "funk rein" late (the background thread finished just as, or just after,
    // another keystroke landed) is recognised as stale and discarded rather than overwriting a newer state.
    //
    // D-215: D-211/D-213 also removed the delay *before* dispatching to this executor entirely, reasoning
    // that a background thread never blocks the main one, so nothing was left for a fixed wait to protect
    // against - true, but incomplete: a real device log of genuinely *slow*, deliberate typing (so no
    // stale-request backlog could exist to explain it) still showed the same per-character slowdown,
    // proving the dominant cost was never wasted/discarded work in the first place, but the *necessary*
    // computation itself scaling with token length (O(token length x per-position store round-trips) -
    // see TokenRepair's own D-214 KDoc). The user's own diagnosis: while genuinely typing fast, the eyes
    // are on the keyboard, not the suggestion bar - a fuzzy chip, a raw-coordinate correction chip (D-39/
    // T-02/T-03), or a highlight colour that flashes in and out between keystrokes cannot be perceived or
    // acted on in time, so computing any of it at all - even off-thread, even for a token that is *not*
    // stale - is spent for nothing during a fast burst. The delay is restored below (dispatchExpensiveSuggestionSearch()),
    // now purely to decide *whether it is worth computing this at all*, not to protect the main thread -
    // that job stays with the background executor and the staleness checks above.
    private val expensiveSuggestionExecutor = Executors.newSingleThreadExecutor()
    private val expensiveSuggestionSeq = AtomicInteger(0)
    private val expensiveSuggestionRunnable = Runnable {
        dispatchExpensiveSuggestionSearch(composing.toString(), previousWord)
    }
    
    /**
     * §125 / D-194: the cached result of the S-05/§47 split-preview/highlight colouring decision for
     * exactly one composing token's text, alongside [composingPreviewFor] which records which text it was
     * computed for. See [composingPreviewRunnable].
     *
     * @property split the live A-05-split colour preview, or null when none applies
     * @property highlighted whether the whole token should render in the recognised-word colour
     */
    private data class ComposingPreview(val split: SplitResult?, val highlighted: Boolean)
    
    // D-213: §125/D-194 originally judged this "cheap enough" to leave on the main thread, debounced only
    // (not backgrounded like D-211's own suggestion search) - wrong, confirmed only after D-211/D-212
    // (background thread + WAL for the *suggestion* search) still showed no improvement on a fresh device
    // log. Traced from there: trySplit() tries every split point of the composing token (candidateAt()'s
    // own KDoc: up to ~8 store round-trips each - resolveWord() x2, frequencyOf() x3, partsOfSpeech() x2,
    // bigramFrequency()), so a ~10-character token can cost 50+ SQLite reads in one call - the exact same
    // "cost scales with token length, never short-circuits for an unresolved token" shape D-208 already
    // fixed for fuzzy matching, just in a different function this project's own prior reasoning had not
    // actually measured. Runs on this dedicated executor now, mirroring expensiveSuggestionExecutor's own
    // shape and sharing its staleness signal (expensiveSuggestionSeq) - a fresh keystroke invalidates both
    // kinds of deferred work identically, so one counter is enough. Kept as a *separate* executor (not
    // reusing expensiveSuggestionExecutor itself) so the two searches can run concurrently with each other
    // under WAL (D-212) instead of queueing behind one another on a shared thread.
    private val composingPreviewExecutor = Executors.newSingleThreadExecutor()
    
    // D-213: composingPreviewRunnable's own InputConnection-touching work (isEditingMidWord, updateComposing)
    // stays on the main thread - Android's InputConnection contract expects the IME's own main thread, not
    // an arbitrary background one - only the dictionary computation (trySplit()/isKnownWord()) is dispatched
    // to composingPreviewExecutor. D-215: dispatching itself still waits for EXPENSIVE_SUGGESTION_DELAY_MS
    // of real stability (scheduleComposingPreviewRefresh) - D-213 had removed that delay on the reasoning
    // that the background thread never blocks the main one, which is true but incomplete: a device log of
    // genuinely slow, deliberate typing (so no stale-request backlog could explain it) still showed the
    // same per-character slowdown, and while typing fast the eyes are on the keyboard anyway - a highlight
    // colour flashing in and out between keystrokes cannot be seen in time, so it is not worth computing at
    // all until there is real evidence of a pause, not just worth computing off-thread. See ComposingPreview
    // / scheduleComposingPreviewRefresh / updateComposing().
    private var composingPreviewToken: String? = null
    private var composingPreviewFor: String? = null
    private var composingPreview = ComposingPreview(null, false)
    private val composingPreviewRunnable = Runnable {
        val expected = composingPreviewToken
        composingPreviewToken = null
        val ic = currentInputConnection
        if (expected == null || ic == null || expected != composing.toString()) {
            return@Runnable
        }
        // D-213: only the cheap, InputConnection-dependent reads happen here, on the main thread - the
        // actual dictionary work (trySplit()/isKnownWord(), see splitPreview()/shouldHighlightComposing()'s
        // own former bodies) is dispatched below. composingTaps/composingFlags are mutable fields the main
        // thread keeps editing on every subsequent keystroke, so spaceAmbiguousIndices() must be snapshotted
        // into a plain, no-longer-shared Set here too - reading it from the background thread later would
        // be a genuine data race, not just a staleness risk.
        val editingMidWord = isEditingMidWord(ic)
        val ambiguous = spaceAmbiguousIndices()
        val previous = previousWord
        val highlightEnabled = config.highlightEnabled
        // D-238: also compute the split when autocorrect is disabled, even with the S-05 highlight off -
        // this same cached value now also backs the position-1 split-suggestion chip that stands in for
        // the silently-suppressed A-05 auto-apply (see refreshSuggestions()), so it must not depend on an
        // otherwise-unrelated display preference.
        val needsSplit = (highlightEnabled || !settings.autocorrectEnabled) && !editingMidWord
        val autocorrectEnabled = settings.autocorrectEnabled
        val seq = expensiveSuggestionSeq.get()
        composingPreviewExecutor.execute {
            if (seq != expensiveSuggestionSeq.get()) {
                return@execute
            }
            val split = if (needsSplit) {
                tokenRepair.trySplit(expected, ambiguous, previous) { seq != expensiveSuggestionSeq.get() }
            } else {
                null
            }
            val highlighted = highlightEnabled && !editingMidWord && provider.isKnownWord(expected)
            handler.post {
                val freshIc = currentInputConnection
                if (seq == expensiveSuggestionSeq.get() && freshIc != null && composing.toString() == expected) {
                    composingPreviewFor = expected
                    composingPreview = ComposingPreview(split, highlighted)
                    updateComposing(freshIc)
                    // D-238: the split-suggestion chip is read directly from composingPreview inside
                    // refreshSuggestions() - without this, it would only appear once the *next* keystroke
                    // happened to call refreshSuggestions() again, not as soon as this debounced result
                    // actually lands.
                    if (!autocorrectEnabled) {
                        refreshSuggestions()
                    }
                }
            }
        }
    }
    
    // D-161/D-250: reported sporadically, no repro yet - the keyboard occasionally appears to render
    // underneath the gesture navigation bar until the first touch. Candidate mechanism (from reading the
    // code, not yet confirmed from a device log): onApplyWindowInsets is not guaranteed to be delivered
    // before the input view's first visible frame, so applyWindowInsetsPadding() may not have run yet at
    // that point. This recheck, scheduled from onStartInputView, re-reads the real current insets directly
    // (no dependency on a further callback) and self-heals a stale padding if one is found - logging only
    // when it actually corrects something, so a real occurrence is finally caught rather than guessed at.
    // D-250: a single one-shot check (§104) did not catch the race reliably enough - retuned to
    // WINDOW_INSETS_RECHECK_MAX_ATTEMPTS repeats, WINDOW_INSETS_RECHECK_DELAY_MS apart, since the check
    // itself is already cheap and idempotent (a no-op once the padding is already correct), so the repeated
    // polling costs nothing worth worrying about. windowInsetsRecheckAttempt is reset to 0 alongside every
    // (re)scheduling in onStartInputView, so a fast field/app switch always gets its own full run of retries.
    private var windowInsetsRecheckAttempt = 0
    
    private val windowInsetsRecheckRunnable: Runnable = Runnable {
        val root = inputRoot
        if (root == null || !root.isAttachedToWindow) {
            return@Runnable
        }
        val insets = ViewCompat.getRootWindowInsets(root) ?: return@Runnable
        val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
        val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val expectedTop = maxOf(statusBars.top, cutout.top)
        val expectedBottom = maxOf(bars.bottom, gestures.bottom)
        if (root.paddingTop != expectedTop || root.paddingBottom != expectedBottom) {
            diag(
                "AdaptKey",
                "windowInsetsRecheck: stale padding corrected - top ${root.paddingTop}->$expectedTop, " +
                    "bottom ${root.paddingBottom}->$expectedBottom",
                warn = true
            )
            applyWindowInsetsPadding(root, insets)
        }
        windowInsetsRecheckAttempt++
        if (windowInsetsRecheckAttempt < WINDOW_INSETS_RECHECK_MAX_ATTEMPTS) {
            handler.postDelayed(windowInsetsRecheckRunnable, WINDOW_INSETS_RECHECK_DELAY_MS)
        }
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
        OffsetStore.prefs(this).registerOnSharedPreferenceChangeListener(offsetModelPrefsListener)
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
        seedBundledBlacklist(german)
    }
    
    /**
     * D-176: seeds the small, reviewed set of app-bundled German blacklist entries - see
     * [BlacklistCategory.BUNDLED]'s own KDoc and [knownInOtherLanguage]'s for the full reasoning behind
     * each word. Called from every [installStores] - both the initial synchronous in-memory stores and the
     * real SQLite stores once [loadDictionariesAsync] finishes - so a fresh install, an already-populated
     * existing install, and even the transient in-memory placeholder all end up with these entries, with no
     * `DATABASE_VERSION` bump (and the destructive full reimport that would entail) needed. `blacklist()`
     * is a plain upsert (`INSERT OR REPLACE`), so calling this on every install is cheap and idempotent -
     * never wipes or duplicates anything.
     */
    private fun seedBundledBlacklist(german: DictionaryStore) {
        for (word in BUNDLED_GERMAN_BLACKLIST) {
            german.blacklist(word, BlacklistCategory.BUNDLED)
        }
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
        view.onKeyListener = AdaptKeyboardView.OnKeyListener { key, x, y, ambiguity, weight -> handleKey(key, x, y, ambiguity, weight) }
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
        // D-247: the D-246 "Gelernt: X" chip's own two-zone drag (Kind.LEARNED only, see SuggestionBarView).
        bar.onForgetLearned = SuggestionBarView.OnForgetLearnedListener { word -> onForgetLearnedWord(word) }
        bar.onForbidLearned = SuggestionBarView.OnForbidLearnedListener { word -> onForbidLearnedWord(word) }
        // D-144: a downward swipe on the bar itself dismisses/closes too, not only on the keyboard body.
        bar.onSwipeDown = SuggestionBarView.OnSwipeDownListener { dismissKeyboardOrCloseExtraRow() }
        bar.visibility = View.VISIBLE
        suggestionBar = bar
        
        // D-135: the inline-suggestions row occupies the same slot as the ordinary suggestion bar, shown
        // instead of it whenever a real autofill suggestion is available (see onInlineSuggestionsResponse).
        val inlineBar = InlineSuggestionsBarView(this)
        inlineBar.visibility = View.GONE
        inlineSuggestionsBar = inlineBar
        
        // §48 / §51: the swipe-up extra row - sits above the suggestion bar (the topmost row while
        // open), reserved at zero height and hidden until an upward swipe opens it.
        val row = ExtraRowView(this)
        row.onEmojiClick = ExtraRowView.OnEmojiClickListener { openEmojiPanelFromExtraRow() }
        row.onSettingsClick = ExtraRowView.OnSettingsClickListener { openSettingsAppFromExtraRow() }
        row.onClearClipboardClick = ExtraRowView.OnClearClipboardClickListener { clearClipboardFromExtraRow() }
        row.onCredentialModeClick = ExtraRowView.OnCredentialModeClickListener { toggleCredentialModeFromExtraRow() }
        row.onTouchZoneToggleClick = ExtraRowView.OnTouchZoneToggleClickListener { toggleTouchZoneVisualizationFromExtraRow() }
        row.onUrlModeToggleClick = ExtraRowView.OnUrlModeToggleClickListener { toggleUrlModeFromExtraRow() }
        // D-144: a downward swipe on the row itself closes it too, not only on the keyboard body below.
        row.onSwipeDown = ExtraRowView.OnSwipeDownListener { dismissKeyboardOrCloseExtraRow() }
        extraRow = row
        
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
            applyWindowInsetsPadding(v, insets)
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
     * §42 / D-136: pads the whole input view up by the bottom system-bar + gesture inset (and, while
     * onboarding stretches [inputRoot] to the full screen height, the top status-bar/cutout inset too) -
     * see the KDoc above the [ViewCompat.setOnApplyWindowInsetsListener] call in [onCreateInputView] for
     * the full reasoning. Extracted so [windowInsetsRecheckRunnable] (D-161) can re-apply the exact same
     * computation from a fresh, synchronously-read [WindowInsetsCompat], not only from the listener's own
     * callback.
     */
    private fun applyWindowInsetsPadding(view: View, insets: WindowInsetsCompat) {
        val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
        val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        view.setPadding(0, maxOf(statusBars.top, cutout.top), 0, maxOf(bars.bottom, gestures.bottom))
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
        // D-139/D-110: toggling diagnostic recording takes effect immediately; turning it off also clears
        // whatever was recorded so far (DiagnosticLog.enabled's own setter), not just stops adding to it.
        DiagnosticLog.enabled = s.diagnosticLogEnabled
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
        // D-177: a real dictionary word must be permanently blacklisted to ever be suppressed - there is
        // nothing to "forget", it will always be in the bundled asset regardless. A purely self-taught
        // word (never in the bundled asset, only ever reached D-37's own learning threshold through the
        // user's own typing) is instead forgotten outright and only provisionally marked - see
        // learnWord()'s own isPendingBlacklistRecurrence() check for what happens if it gets learned
        // again before the mark expires.
        if (dictionaryStore.isBundledWord(word)) {
            dictionaryStore.blacklist(word, BlacklistCategory.USER)
        } else {
            forgetSelfTaughtWord(word)
        }
        refreshSuggestions()
    }
    
    /**
     * D-247: the self-taught half of [onBlacklistWord] (D-177), pulled out so the D-246 "Gelernt: X"
     * confirmation chip's own shallow ("Vergessen") drag zone can reuse it directly - a just-promoted word
     * is never bundled by construction ([learnWord]'s own [DictionaryStore.isBundledWord] check would have
     * skipped learning it entirely otherwise), so [onBlacklistWord]'s own origin branching would always
     * degenerate to this exact call anyway; calling it directly avoids re-deriving that already-known fact.
     */
    private fun forgetSelfTaughtWord(word: String) {
        dictionaryStore.forget(word)
        dictionaryStore.markPendingBlacklist(word, System.currentTimeMillis())
    }
    
    /**
     * D-247: the D-246 "Gelernt: X" chip's shallow drag zone ("Vergessen") - unlearns a just-promoted word
     * exactly like [onBlacklistWord] already does for a self-taught word, without needing to re-check its
     * origin (see [forgetSelfTaughtWord]'s own KDoc).
     *
     * @param word the just-promoted word to unlearn
     */
    private fun onForgetLearnedWord(word: String) {
        if (word.isBlank() || !this::dictionaryStore.isInitialized) {
            return
        }
        forgetSelfTaughtWord(word)
        showNextWordPredictions()
    }
    
    /**
     * D-247: the D-246 "Gelernt: X" chip's deep drag zone ("Verbieten") - blacklists a just-promoted word
     * immediately and permanently, bypassing [onBlacklistWord]'s own origin check entirely (deliberately
     * stronger than [onForgetLearnedWord]'s provisional mark, for when the user is certain the word should
     * never be reconsidered even if it recurs).
     *
     * @param word the just-promoted word to blacklist
     */
    private fun onForbidLearnedWord(word: String) {
        if (word.isBlank() || !this::dictionaryStore.isInitialized) {
            return
        }
        dictionaryStore.blacklist(word, BlacklistCategory.USER)
        showNextWordPredictions()
    }
    
    /**
     * D-139/D-110: routes one diagnostic line to both `adb logcat` (for anyone who has it set up) and the
     * in-app [DiagnosticLog] ring buffer (a rolling, in-memory, 1-minute log viewable/shareable from
     * Settings - needs no PC/USB tether at all, unlike logcat). [DiagnosticLog.record] is a cheap no-op
     * unless the user has actually turned recording on (off by default).
     *
     * D-150: neither destination is written to at all while the focused field is a password - many
     * `diag()` call sites (`finalizeAndCommit`, `updateComposing`, ...) log the literal typed/committed
     * text, and [LoginFieldKind.PASSWORD] is reliably detected from `InputType` (§82), so this is a single
     * choke point every current and future call site is automatically covered by, rather than trusting each
     * one to remember an individual check. Unconditional - independent of [DiagnosticLog.enabled] and of
     * whether `adb logcat` happens to be attached, since a password must never reach *either* log.
     *
     * @param tag the logcat tag (also prefixed onto the in-app entry, so both views group the same way)
     * @param message the diagnostic text
     * @param warn true to log at `Log.w` instead of `Log.d` (used for the one entry that is close to a
     *        smoking gun for the D-139 cascade if it ever actually fires)
     */
    private fun diag(tag: String, message: String, warn: Boolean = false) {
        if (loginFieldKind == LoginFieldKind.PASSWORD) {
            return
        }
        if (warn) Log.w(tag, message) else Log.d(tag, message)
        DiagnosticLog.record("[$tag] $message")
    }
    
    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // D-110 (temporary diagnostic): a fixed root cause was found and fixed in ShiftGrace for fields
        // that declare no caps flag at all, but it is not certain that is the *whole* story for the
        // originally-reported eBay Kleinanzeigen field - this logs exactly the EditorInfo fields
        // capsModeFor()/D-142's field-kind detection actually consult, so a `adb logcat -s AdaptKey:D` (or
        // the in-app diagnostic log, Settings -> Diagnostics) while focusing that field can finally confirm
        // or rule out a deeper cause. Remove once D-110 is closed.
        diag(
            "AdaptKey",
            "onStartInput: package=${info?.packageName} fieldName=${info?.fieldName} " +
                "inputType=0x${Integer.toHexString(info?.inputType ?: 0)} hintText=${info?.hintText}"
        )
        clearComposing()
        resetWordEndShift()
        pendingMergeChar = null
        pendingSuggestionSpace = false
        previousWord = null
        previousPreviousWord = null
        tokenContextBefore = ""
        // D-152: a fresh field's own initial selection is delivered via EditorInfo, not a guaranteed
        // onUpdateSelection callback beforehand - selectionCollapsed must not be left stale from whatever
        // the *previous* field's last reported selection happened to be. Left stale as false (e.g. the
        // previous field ended on a genuine drag-selection), handleBackspace()'s D-149 guard would wrongly
        // still trust getSelectedText() for this field's very first Backspace - most visibly hit right after
        // the field's first word gets D-62-reclaimed on focus, deleting that whole word instead of one
        // character. A fresh field is assumed collapsed until its own onUpdateSelection says otherwise,
        // matching every other real editor's default caret state.
        selectionCollapsed = true
        capsMode = capsModeFor(info)
        clearUndo()
        keyboardView?.shifted = false
        // D-15: a new field starts without Caps Lock.
        keyboardView?.capsLock = false
        // D-143 / D-158: recognised fresh per field, mirroring surface below - pushed to the view before
        // setSurface() so its own rebuildRows() (triggered by the surface/symbolPage property setters
        // inside switchPage()) already reads the correct value.
        urlMode = isUrlField(info)
        keyboardView?.urlMode = urlMode
        emailMode = isEmailField(info)
        keyboardView?.emailMode = emailMode
        // A field that primarily wants digits (phone number, plain numeric entry, date/time) opens
        // straight to the calculator page instead of the letters surface.
        setSurface(initialSurfaceFor(info), targetSymbolPage = 1)
        // D-142: classify the field once per focus - drives both the suggestion pipeline and what gets
        // learned for the rest of this field's session (see refreshSuggestions/captureCredentialIfLoginField).
        // EMAIL/PASSWORD are reliably signalled by InputType; USERNAME has no such signal at all (verified
        // against the real SDK, see LoginFieldDetector's own KDoc), and not even EMAIL is always set on a
        // field that is visibly labelled as one (confirmed on device) - both are only ever reached via the
        // extra row's manual toggle, optionally nudged by the weak weakSignalKind() below.
        loginFieldKind = LoginFieldDetector.classify((info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION)
        credentialCaptured = false
        credentialModeManuallyActivated = false
        credentialSnapshot.setLength(0)
        extraRow?.credentialModeActive = loginFieldKind != LoginFieldKind.NONE
        weakSignalKind = if (loginFieldKind == LoginFieldKind.NONE) {
            LoginFieldDetector.weakSignalKind(info?.hintText, info?.fieldName)
        } else {
            LoginFieldKind.NONE
        }
        // D-191: a fresh field's contact cache is always stale (different field, possibly different kind);
        // re-loaded from scratch only for a genuine EMAIL field, never per keystroke - see
        // loadContactEmailsAsync's own KDoc.
        contactEmailCache = null
        if (loginFieldKind == LoginFieldKind.EMAIL) {
            loadContactEmailsAsync()
        }
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
        // D-149: recorded unconditionally, before the burst-guard/ownEdit branches below, so it always
        // reflects the most recently confirmed reality regardless of how this particular call is handled.
        selectionCollapsed = newSelStart == newSelEnd
        // D-139 (temporary diagnostic): every call, with enough state to reconstruct what happened -
        // `adb logcat -s AdaptKeyJitter:D` while typing, to finally catch the reported "text jitters,
        // characters get scrambled" glitch in the act. Remove once D-139 is closed for good.
        diag(
            "AdaptKeyJitter",
            "onUpdateSelection t=${SystemClock.uptimeMillis()} old=[$oldSelStart,$oldSelEnd] " +
                "new=[$newSelStart,$newSelEnd] candidates=[$candidatesStart,$candidatesEnd] " +
                "composing=\"$composing\" anchor=$composingAnchor cursor=$composingCursor"
        )
        // D-139: a defensive circuit breaker, not a confirmed fix - no repro exists yet for the reported
        // "text jitters, characters get scrambled" glitch, but this function's own reactive mutations
        // (reclaimWordAtCaret() below / finishComposingText() further down) could in principle re-trigger
        // this very callback, and this codebase has hit a genuinely self-triggering onUpdateSelection
        // cascade before (a different specific bug, §32's D-87, but the same class of risk). If this ever
        // fires abnormally often in a short window, stop reacting rather than let a possible cascade
        // continue to escalate - see CallbackBurstGuard for the threshold reasoning.
        if (selectionUpdateBurstGuard.isBurst(SystemClock.uptimeMillis())) {
            // D-139 (temporary diagnostic): if this ever actually fires, it is close to a smoking gun for
            // the suspected self-triggering cascade - flagged at warning level so it stands out in logcat.
            diag("AdaptKeyJitter", "onUpdateSelection: CallbackBurstGuard TRIPPED - suppressing reactive branches", warn = true)
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
        // §100 (D-139): composingAnchor is resolved locally by reclaimSurroundingWord() whenever composing
        // text exists, not only for a genuine mid-word reclaim - candidatesEnd (the target editor's own,
        // remotely-reported composing span) is deliberately not consulted here at all, since it is not
        // guaranteed to arrive in sync with the selection-update stream. When composingAnchor is unknown
        // (-1, e.g. an ExtractedText read failed), there is nothing reliable to compare against - stay
        // conservative and do not react at all, since wrongly wiping a live composing token is the
        // destructive branch.
        if (composingAnchor < 0) {
            return
        }
        val expected = composingAnchor + composingCursor
        if (SelectionTruth.isAtExpectedCaret(expected, newSelStart, newSelEnd)) {
            return
        }
        // §101 (D-139): a mismatching callback alone proves nothing - callbacks lag behind the IME's own
        // local state during fast typing (a commit's echoes can arrive after the next token has already
        // started composing) and some editors report a batch edit's transient intermediate states as their
        // own updates (observed live: the reclaim's deleteSurroundingText() step echoed separately by the
        // Gemini search field). Treating any such stale echo of our own edit as an external caret move
        // wiped the live token and re-triggered reclaimWordAtCaret()'s identical-race rebuild, forming the
        // reported multi-second jitter loop (traced end-to-end from four real device logs, see spec
        // §99-§101). Synchronous InputConnection reads, unlike callbacks, are answered only after every
        // previously-sent mutation has been applied - so read the editor's true current selection now and
        // let reality decide: truth at the expected caret means this callback was a stale echo of our own
        // edit (ignore it, newer state is already correct); truth elsewhere means a genuinely external
        // change that must reset the composing state exactly as before. The extra IPC read only ever
        // happens for mismatching callbacks - the in-sync common case above stays read-free.
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted == null) {
            // No ground truth available (rare editor quirk) - same conservative do-nothing as the unknown-
            // anchor case above, for the same reason.
            diag("AdaptKeyJitter", "onUpdateSelection: mismatch (expected=$expected) but ground truth unavailable - ignored conservatively")
            return
        }
        val actualStart = ComposingAnchor.resolve(extracted.startOffset, extracted.selectionStart, 0)
        val actualEnd = ComposingAnchor.resolve(extracted.startOffset, extracted.selectionEnd, 0)
        if (SelectionTruth.isAtExpectedCaret(expected, actualStart, actualEnd)) {
            diag("AdaptKeyJitter", "onUpdateSelection: STALE ECHO of own edit (expected=$expected, reported=[$newSelStart,$newSelEnd]) - ignored")
            return
        }
        // D-139 (temporary diagnostic): this branch wipes the in-progress token - exactly what a
        // reported scramble/jitter would look like if it fires when it should not.
        diag("AdaptKeyJitter", "onUpdateSelection: EXTERNAL (expected=$expected, actual=[$actualStart,$actualEnd]) - finishing+clearing composing=\"$composing\"")
        ic.finishComposingText()
        clearComposing()
        pendingMergeChar = null
        resetWordEndShift()
        clearUndo()
        previousWord = null
        previousPreviousWord = null
        clearSuggestions()
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
        // D-161/D-250: see windowInsetsRecheckRunnable's own comment - cancel any still-pending check from a
        // previous, faster field/app switch before scheduling this one, so only the most recent survives, and
        // reset the attempt counter so this field/app switch gets its own full run of retries.
        handler.removeCallbacks(windowInsetsRecheckRunnable)
        windowInsetsRecheckAttempt = 0
        handler.postDelayed(windowInsetsRecheckRunnable, WINDOW_INSETS_RECHECK_DELAY_MS)
        // Pick up any changes made in the settings screen since the keyboard was last shown.
        settings = SettingsStore.load(this)
        applySettings()
        // Reflect the active alphabet (G-01) on the freshly (re)created keyboard view.
        keyboardView?.greek = activeLanguage == Language.GREEK
        keyboardView?.qwerty = activeLanguage == Language.ENGLISH
        // D-143: same defensive re-push, in case the view was (re)created after onStartInput already
        // computed this field's urlMode.
        keyboardView?.urlMode = urlMode
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
        // D-142: a recognised login field shows its own credential suggestions immediately, even before
        // anything is typed (the user's usual identifiers, most-used first) - takes priority over the
        // generic D-36 paste chip below, which would otherwise compete for the same bar slot.
        if (loginFieldKind == LoginFieldKind.USERNAME || loginFieldKind == LoginFieldKind.EMAIL) {
            showCredentialSuggestions()
        } else {
            // D-36: offer a direct-paste chip when the field opens and the clipboard holds text.
            showClipboardChipIfAvailable()
        }
        // §48: never carry an open extra row over into a fresh keyboard presentation.
        extraRow?.closeImmediately()
        // D-142 / D-189: reflect credential-mode state on the toggle button, and reveal the row for BOTH a
        // reliable EMAIL/PASSWORD classification and a weak (unreliable) email/username signal - the
        // reliable case used to only update the button's background silently, leaving the user with no way
        // to actually see that credential mode had switched on; the weak-signal case already opened the row
        // to nudge a manual confirmation. loginFieldKind and weakSignalKind are mutually exclusive
        // (weakSignalKind is only ever computed while loginFieldKind is still NONE), so this never
        // double-opens or double-flashes. The flash itself only runs once open() actually finishes - see
        // that function's own KDoc for why an immediate flash would run while the row is still sliding in.
        extraRow?.credentialModeActive = loginFieldKind != LoginFieldKind.NONE
        if (loginFieldKind != LoginFieldKind.NONE || weakSignalKind != LoginFieldKind.NONE) {
            extraRow?.open { extraRow?.flashCredentialModeButton() }
        }
        // D-185: the toggle button is reachable only in a real URL-variation field - tracks the field's
        // own type ([isUrlField]), not the live [urlMode] toggle, so it stays visible after being switched
        // off. Unlike the reliable-login-classification case above, a URL field always reveals the row (not
        // only on a weak/uncertain signal) since the button would otherwise be undiscoverable.
        val urlVariationField = isUrlField(info)
        extraRow?.urlModeButtonVisible = urlVariationField
        extraRow?.urlModeActive = urlMode
        if (urlVariationField) {
            extraRow?.open()
        }
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
     * D-142: replaces the ordinary suggestion pipeline entirely while a recognised login field is
     * focused. A password field ([LoginFieldKind.PASSWORD]) shows nothing at all - offering suggestions
     * while typing a password would be a pointless information leak, and this feature never learns from
     * one anyway (see [captureCredentialIfLoginField]). A username/email field shows the saved credential
     * list instead of the ordinary dictionary - a normal word is never something the user would want to
     * type here. Once the value typed so far in an email field contains `@`, the list switches to the
     * user's own most-used domains completing the address instead of matching whole saved addresses,
     * since the local part is already fixed and only the domain is still open.
     *
     * Reads the field's own text via [InputConnection.getTextBeforeCursor] rather than [composing]:
     * [commitVerbatimFieldFragment] commits every `.`/`@`/`-`/`_` as its own delimiter, so `composing` alone
     * only ever holds the current fragment (e.g. just `"e"` of `"peter@e"`) - the field's real text
     * already includes both the earlier committed fragments and the live composing span, in one read.
     *
     * Built and pushed directly to [suggestionBar], bypassing [controller] entirely (mirrors how the D-36
     * clipboard chip is already handled) - S-03's position stabilisation exists to smooth prose-typing
     * suggestion flicker, which a short, freshly-ranked credential list has no need for.
     */
    private fun showCredentialSuggestions() {
        handler.removeCallbacks(resortRunnable)
        controller.clear()
        lastTier3Result = Tier3Result.EMPTY
        lastCapProposal = null
        if (loginFieldKind == LoginFieldKind.PASSWORD) {
            suggestionBar?.setItems(emptyList())
            suggestionBar?.visibility = View.VISIBLE
            return
        }
        val input = currentInputConnection?.getTextBeforeCursor(CREDENTIAL_LOOKBACK, 0)?.toString()
            ?.takeLastWhile { !it.isWhitespace() }
            .orEmpty()
        val entries = mergedCredentialEntries()
        val atIndex = input.indexOf('@')
        val values = if (loginFieldKind == LoginFieldKind.EMAIL && atIndex >= 0) {
            val localPart = input.substring(0, atIndex)
            val domainPrefix = input.substring(atIndex + 1)
            CredentialRanking.emailDomainsFor(entries, domainPrefix, config.maxSuggestions).map { "$localPart@$it" }
        } else {
            CredentialRanking.suggestionsFor(entries, input, config.maxSuggestions)
        }
        val items = values
            .filterNot { it.equals(input, ignoreCase = true) }
            .map { SuggestionController.DisplayItem(it, SuggestionController.Kind.CREDENTIAL, it) }
        suggestionBar?.setItems(items)
        suggestionBar?.visibility = View.VISIBLE
    }
    
    /**
     * D-191: [CredentialStore]'s own entries plus any already-loaded [contactEmailCache] addresses, wrapped
     * as ordinary (never-persisted) [CredentialEntry] values with `frequency = 0` so they flow through
     * [CredentialRanking]'s existing ranking/domain-completion logic unchanged - a real learned entry always
     * outranks a contact-derived one with the same value, and the two are deduplicated by value so an
     * address that is both a saved credential and a contact is never offered twice.
     */
    private fun mergedCredentialEntries(): List<CredentialEntry> {
        val stored = CredentialStore.all(this)
        val contacts = contactEmailCache
        if (contacts.isNullOrEmpty()) {
            return stored
        }
        val storedValues = stored.mapTo(HashSet()) { it.value.lowercase() }
        val extra = contacts
            .filterNot { it.lowercase() in storedValues }
            .map { CredentialEntry(it, LoginFieldKind.EMAIL, frequency = 0L) }
        return stored + extra
    }
    
    /**
     * D-191: kicks off a background `ContactsContract` query for every address-book email (capped at
     * [CONTACT_EMAIL_LIMIT]), applied to [contactEmailCache] on the main thread once it completes - run
     * once per EMAIL field focus (see `onStartInput`), never per keystroke, mirroring D-160's own
     * "not on the hot path" reasoning for an external, unpredictably-sized read (a large address book read
     * synchronously could jank the very moment a field gains focus). A no-op unless both the user's own
     * [AdaptSettings.contactsSuggestionsEnabled] preference and the actual live `READ_CONTACTS` grant are
     * present - the live check is deliberate defence in depth against the permission having been revoked
     * (via system settings) since the preference was last turned on. Nothing is ever written back to
     * contacts, and the read addresses are never persisted anywhere in this app - see
     * [AdaptSettings.contactsSuggestionsEnabled]'s own KDoc for why.
     */
    private fun loadContactEmailsAsync() {
        if (!settings.contactsSuggestionsEnabled ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        contactsExecutor.execute {
            val addresses = runCatching {
                val projection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
                contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, null, null, null)?.use { cursor ->
                    val addressIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    val result = LinkedHashSet<String>()
                    while (cursor.moveToNext() && result.size < CONTACT_EMAIL_LIMIT) {
                        cursor.getString(addressIndex)?.takeIf { it.contains('@') }?.let { result.add(it) }
                    }
                    result.toList()
                }.orEmpty()
            }.getOrElse { emptyList() }
            handler.post { contactEmailCache = addresses }
        }
    }
    
    /**
     * D-142: saves the value just entered into a recognised username/email field, immediately (no D-37
     * count-up threshold - deliberate credential input is precise by construction). Called from both
     * [handleEnter] (the common case - the field's own "Weiter"/"Login" submit) and [onFinishInput] (the
     * field losing focus some other way, e.g. tapping an on-screen button instead) - [credentialCaptured]
     * makes the second call a no-op when the first already ran for this field. Best-effort: if the target
     * app clears the field's content before either of these fires, there is nothing left to read.
     *
     * @param ic the current input connection
     */
    private fun captureCredentialIfLoginField(ic: InputConnection) {
        // D-174: confirmed from a real device log (K9 Mail's recipient field) - getTextBeforeCursor()
        // itself can already return null by the time this runs. K9 converts the typed address into a
        // recipient "chip" the instant focus leaves the field, tearing the InputConnection down faster
        // than onFinishInput()'s own read of it - the log showed a burst of onUpdateSelection callbacks
        // with "ground truth unavailable" immediately before this exact null. loginFieldKind classification
        // itself was never the problem (confirmed EMAIL in the same log). Falls back to credentialSnapshot
        // (an in-memory record of what was actually committed, unaffected by the connection dying) below.
        val kind = loginFieldKind
        // D-190/D-224: PASSWORD is excluded unconditionally above, regardless of this setting - the toggle
        // only ever gates whether a recognised USERNAME/EMAIL value gets saved.
        if (kind == LoginFieldKind.NONE || kind == LoginFieldKind.PASSWORD || credentialCaptured || !settings.saveCredentials) {
            diag(
                "AdaptKey",
                "captureCredentialIfLoginField: skipped - kind=$kind credentialCaptured=$credentialCaptured " +
                    "saveCredentials=${settings.saveCredentials}"
            )
            return
        }
        credentialCaptured = true
        ic.finishComposingText()
        val before = ic.getTextBeforeCursor(CREDENTIAL_LOOKBACK, 0)?.toString()
        // A login field's own content is normally the whole field (no internal spaces) - only the
        // trailing run of non-whitespace characters right before the cursor is the credential value.
        // D-174: when the read above already succeeded, it already reflects the real document - including
        // whatever composing held, since finishComposingText() just finalised it there - so composing is
        // NOT appended too, or the still-pending tail would be duplicated. Only the snapshot fallback
        // (which by construction never includes that same not-yet-committed tail) needs it appended.
        val value = if (before != null) {
            before.takeLastWhile { !it.isWhitespace() }
        } else {
            diag("AdaptKey", "captureCredentialIfLoginField: getTextBeforeCursor null, using snapshot - kind=$kind", warn = true)
            (credentialSnapshot.toString() + composing).takeLastWhile { !it.isWhitespace() }
        }
        if (value.length < MIN_CREDENTIAL_LENGTH) {
            diag("AdaptKey", "captureCredentialIfLoginField: value too short (len=${value.length}) - kind=$kind")
            return
        }
        // A field classified as USERNAME may still be where the user typed their email as the identifier
        // (common on real login forms) - reclassify from the value itself so it is stored (and later
        // offered for @-domain completion) correctly.
        val actualKind = if (value.contains('@')) LoginFieldKind.EMAIL else kind
        diag("AdaptKey", "captureCredentialIfLoginField: learning value=\"$value\" kind=$actualKind")
        CredentialStore.learn(this, value, actualKind)
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
     * calibration screen merged or reset (T-03 / K-01) is adopted even when this service instance was
     * already resident.
     *
     * D-239: called from two independent triggers now, not just the `onStartInputView` one - a reported
     * bug traced to the `!restarting` guard there being skipped exactly when Android reports the same field
     * session as resuming (the common case right after returning from Settings), which left a fresh
     * calibration-screen reset invisible to the live keyboard indefinitely. [offsetModelPrefsListener] fires
     * on the actual underlying storage write itself, independent of that view-lifecycle nuance, and is the
     * mechanism this bug actually needed - the `onStartInputView` call site is kept as a second, harmless
     * belt-and-braces reload, not removed.
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
        // D-142: catches leaving a login field WITHOUT pressing Enter (e.g. tapping an on-screen "Login"
        // button) - must run before the connection below is torn down, and before composing is cleared.
        currentInputConnection?.let { captureCredentialIfLoginField(it) }
        composing.setLength(0)
        clearSuggestions()
        // D-68: the typing pattern (T-04) is no longer re-derived here - it is now an explicit user choice
        // (see CalibrationActivity), not something inferred from live typing.
        persistOffsetModel()
    }
    
    override fun onDestroy() {
        handler.removeCallbacks(resortRunnable)
        SettingsStore.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        OffsetStore.prefs(this).unregisterOnSharedPreferenceChangeListener(offsetModelPrefsListener)
        persistOffsetModel()
        tier3Executor.shutdownNow()
        expensiveSuggestionExecutor.shutdownNow()
        composingPreviewExecutor.shutdownNow()
        contactsExecutor.shutdownNow()
        onnxProvider?.close()
        onnxProvider = null
        super.onDestroy()
    }
    
    private fun handleKey(key: Key, x: Float, y: Float, ambiguity: AmbiguityResult, weight: Double) {
        val ic = currentInputConnection ?: return
        // D-217 (temporary diagnostic): the actual per-key processing cost the user asked to see alongside
        // AdaptKeyJitter's own per-call timestamps - everything below runs synchronously on the main thread
        // between the tap being resolved and this function returning, so `finally` (covering every branch's
        // return, including the A-07 undo one right below) is the one choke point that always logs it,
        // mirroring `diag()`'s own single-choke-point reasoning. Remove once D-217 is closed for good.
        val handledAt = SystemClock.uptimeMillis()
        try {
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
                                reclaimSurroundingWord(ic, TapPoint(x, y, weight))
                            }
                            val ch = if (isWordLetter && isUpperArmed()) raw.uppercaseChar() else raw
                            // T-05 / D-39 / D-62: keeps composingFlags/composingTaps in lockstep and lands the new
                            // character at the logical edit point (the end, unless a reclaim left a tail after it).
                            insertComposingChar(ch, ambiguity.kind, TapPoint(x, y, weight))
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
                // emoji button now lives in the §48 extra row instead).
                KeyCode.SYMBOL -> setSurface(PanelNavigation.onCombinedKeyTap(surface))
                
                // L-03: the "ABC" key on the numeric/symbol layer returns to letters.
                KeyCode.LETTERS -> setSurface(InputSurface.LETTERS)
                
                // §53 (D-103/D-104): a multi-character key (e.g. the calculator page's sin/deg) commits its
                // own label verbatim, exactly like any other symbol - finalising whatever token was in
                // progress first, never extending it.
                KeyCode.TEXT -> finalizeAndCommit(ic, key.label)
            }
        } finally {
            diag(
                "AdaptKeyHaptics",
                "handleKey: key=${key.code}${key.char?.let { " '$it'" } ?: ""} t=$handledAt processed in " +
                    "${SystemClock.uptimeMillis() - handledAt}ms"
            )
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
        // D-142: a login field's value is typically finished right here (Enter/"Weiter"/"Login"), before
        // the app's own submit action can navigate away or clear the field.
        captureCredentialIfLoginField(ic)
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
     *
     * D-225: `_` is the one non-letter symbol that also extends the composing token - a technical
     * identifier ("MEINE_VARIABLE") is a single token, not two words joined by a delimiter, unlike every
     * other punctuation mark this app treats as a word boundary. [finalizeAndCommit] separately recognises
     * any token containing `_` and skips autocorrect/§6 capitalisation/token-repair/learning for it entirely.
     */
    private fun commitLongPressSymbol(ic: InputConnection, symbol: String, sourceCode: KeyCode) {
        val extendsWord = symbol == "_" ||
            (sourceCode != KeyCode.TEXT && AlternativeScript.extendsWord(symbol, activeLanguage == Language.GREEK))
        if (extendsWord) {
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
     * upward-swipe extra row) reported by the keyboard view. Resolves it to an action via
     * [KeyGesture] and executes it.
     *
     * @param key the key the swipe started on (T-01 contact point)
     * @param direction the recognised swipe direction
     * @return true when the swipe carried an action and was consumed (suppressing the tap), false
     *         when it should fall back to a normal tap
     */
    private fun handleSwipe(key: Key, direction: SwipeDirection): Boolean {
        val ic = currentInputConnection ?: return false
        return when (KeyGesture.resolve(key.code, direction, surface, urlMode, emailMode)) {
            // G-02: delete the whole previous word.
            GestureAction.DELETE_WORD -> {
                clearUndo()
                deleteWord(ic)
                true
            }
            
            // G-03: dismiss the keyboard - §48: unless the extra row is open, in which case this
            // downward swipe closes the row first; only a second one (row already closed) reaches here
            // again to actually dismiss. KeyGesture.resolve() is a pure function with no row-open state of
            // its own, so this re-routing happens here rather than as a distinct GestureAction.
            GestureAction.DISMISS_KEYBOARD -> {
                dismissKeyboardOrCloseExtraRow()
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
            
            // §48: upward swipe anywhere else reveals the extra row.
            GestureAction.OPEN_SETTINGS_ROW -> {
                openExtraRow()
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
     * §48: opens the extra row (an upward swipe anywhere on the keyboard, except the combined key).
     */
    private fun openExtraRow() {
        extraRow?.open()
    }
    
    /**
     * §48: closes the extra row - a downward swipe while it is open (see
     * [dismissKeyboardOrCloseExtraRow]), or the fresh-field reset in `onStartInputView`. [onClosed] runs
     * once the close animation has finished collapsing the reserved space. D-187: every individual row
     * button used to call this on tap too (§48's original design) - dropped by direct request, reported as
     * generally wrong and confirmed at least for the toggle buttons (touch-zone/credential-mode/URL-mode),
     * where it also fought the toggle's own purpose (e.g. hiding the touch-zone overlay behind the row it
     * was meant to reveal). Left in place only for the swipe-down gesture and the field-reset path; a
     * specific action button (emoji/settings/clear-clipboard) auto-closing again is a possible future
     * exception, not reinstated speculatively here.
     */
    private fun closeExtraRow(onClosed: () -> Unit = {}) {
        extraRow?.close(onClosed) ?: onClosed()
    }
    
    /**
     * G-03 / §48 / D-144: a downward swipe closes the extra row first when it is open; only a second
     * one (row already closed) actually dismisses the keyboard. Shared by every downward-swipe source -
     * the keyboard body ([AdaptKeyboardView.OnSwipeListener] via [handleSwipe]), the suggestion bar
     * ([de.froehlichmedia.adaptkey.suggestion.SuggestionBarView.OnSwipeDownListener]) and the extra row
     * itself ([ExtraRowView.OnSwipeDownListener]) - previously only the keyboard body reacted at all.
     */
    private fun dismissKeyboardOrCloseExtraRow() {
        if (extraRow?.isOpen == true) {
            closeExtraRow()
        } else {
            requestHideSelf(0)
        }
    }
    
    /**
     * §48: the extra row's emoji button - opens the emoji panel, exactly like the combined key used to
     * before §49 retired its dual purpose. D-187: no longer closes the row - see [closeExtraRow]'s own
     * KDoc for why button taps stopped auto-closing it.
     */
    private fun openEmojiPanelFromExtraRow() {
        setSurface(InputSurface.EMOJI)
    }
    
    /**
     * §48: the extra row's gear button - launches [SettingsActivity], the same `launchFromKeyboard`
     * mechanism already used for the onboarding calibration/model-import screens. D-187: no longer closes
     * the row - see [closeExtraRow]'s own KDoc.
     */
    private fun openSettingsAppFromExtraRow() {
        launchFromKeyboard(SettingsActivity::class.java)
    }
    
    /**
     * §69: the extra row's clear-clipboard button - immediately wipes the clipboard, reusing the same
     * [clearClipboard] the D-36/D-38 quick-paste flow already calls after a paste (P+ `clearPrimaryClip()`
     * / the pre-P `newPlainText("", "")` fallback), just triggered directly by the user instead of
     * automatically after a paste. D-187: no longer closes the row - see [closeExtraRow]'s own KDoc.
     */
    private fun clearClipboardFromExtraRow() {
        clearClipboard()
    }
    
    /**
     * D-156: the extra row's touch-zone-visualisation toggle - flips [AdaptKeyboardView.showTouchModel]
     * live, unlike its only prior use (D-24) which was confined to a separate, non-live preview keyboard in
     * [de.froehlichmedia.adaptkey.settings.TouchModelActivity]. Session-only, like
     * [credentialModeManuallyActivated] - resets on the next keyboard presentation. D-187: no longer closes
     * the row - see [closeExtraRow]'s own KDoc.
     */
    private fun toggleTouchZoneVisualizationFromExtraRow() {
        val visible = !(keyboardView?.showTouchModel ?: false)
        keyboardView?.showTouchModel = visible
        extraRow?.touchZoneVisible = visible
    }
    
    /**
     * D-142: the extra row's credential-mode button - manually switches the currently focused field
     * into credential mode (the saved-username/email suggestion list, immediate learning on submit) when
     * it could not be auto-detected (see [LoginFieldDetector] - `InputType` has no signal for a plain
     * username field at all, and real apps do not always set it even for an email field). Activates as
     * [weakSignalKind] when that nudge is what prompted the tap (so domain completion still works for a
     * weak-signal *email* field, not just a generic username fallback), or plain [LoginFieldKind.USERNAME]
     * when tapped with no signal at all. A no-op for a field already reliably classified as EMAIL/PASSWORD -
     * those are not user-toggleable. Tapping again while manually active switches back off. D-187: no
     * longer closes the row - see [closeExtraRow]'s own KDoc.
     */
    private fun toggleCredentialModeFromExtraRow() {
        if (credentialModeManuallyActivated) {
            loginFieldKind = LoginFieldKind.NONE
            credentialModeManuallyActivated = false
        } else if (loginFieldKind == LoginFieldKind.NONE) {
            loginFieldKind = weakSignalKind.takeIf { it != LoginFieldKind.NONE } ?: LoginFieldKind.USERNAME
            credentialModeManuallyActivated = true
        }
        extraRow?.credentialModeActive = loginFieldKind != LoginFieldKind.NONE
        refreshSuggestions()
    }
    
    /**
     * D-185: the extra row's URL-keyboard toggle - flips [urlMode] itself (the same flag
     * [isUrlField] seeds it with on field entry), so every consumer that already reads it
     * (the view's own D-143 bottom row, [finalizeAndCommit]'s verbatim-commit branch, [refreshSuggestions]'
     * empty-bar branch) picks up the change with no separate state to keep in sync. Only reachable while
     * [ExtraRowView.urlModeButtonVisible] is showing the button, i.e. only in a real URL-variation
     * field - [urlMode] reverts to the field's own default (on) the next time any field is focused. D-187:
     * no longer closes the row - see [closeExtraRow]'s own KDoc.
     */
    private fun toggleUrlModeFromExtraRow() {
        urlMode = !urlMode
        keyboardView?.urlMode = urlMode
        extraRow?.urlModeActive = urlMode
        refreshSuggestions()
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
        previousPreviousWord = null
    }
    
    private fun handleBackspace(ic: InputConnection) {
        // §41: a real, non-collapsed text selection takes priority over the ordinary single-character
        // delete below - Backspace must remove the selection itself, matching every other editor, not the
        // character before the cursor (which the selection may not even be adjacent to).
        //
        // D-149: gated on selectionCollapsed (the IME's own onUpdateSelection-confirmed state) rather than
        // trusting a bare InputConnection.getSelectedText(0) call alone - traced from a precise repro (a
        // D-62 mid-word reclaim, e.g. tapping into "diecwird" right after the "c", then Backspace): with
        // composingCursor mid-word and the caret genuinely collapsed (confirmed by the immediately preceding
        // onUpdateSelection), getSelectedText(0) still returned the whole reclaimed word non-empty on the
        // device tested (Google Keep) - taking this branch then wiped the entire reclaimed token via
        // commitText("", 1) instead of removing the single intended character, which is exactly the
        // "text gets shaken/rearranged" symptom reported as part of D-139. A genuine drag-selection is
        // unaffected - it always reports its non-collapsed bounds via onUpdateSelection first, which is
        // exactly how an IME learns about a selection in the first place.
        val selected = if (!selectionCollapsed) ic.getSelectedText(0) else null
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
            updateComposing(ic, duringRepeat)
            refreshSuggestions(duringRepeat)
        }
        applyShiftAfterDelete(deleted)
    }
    
    /**
     * Deletes the single character before the cursor. When there is nothing to delete in the current
     * editable (the cursor is at the very start of the entry), a real DEL key event is sent instead so
     * the editor can join with the previous line/entry if it supports it (D-10).
     *
     * D-248: when [composing] is empty, this is a plain backspace into already-committed text (as opposed
     * to this function's other call site inside [deleteComposingChar], reached only while [composing] is
     * still non-empty, i.e. actively re-editing an already-open token, not "returning" to a finished word) -
     * checked via [maybeUnlearnOnBackspaceReturn].
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
        val wasComposingEmpty = composing.isEmpty()
        val deleted = before[0]
        ic.deleteSurroundingText(1, 0)
        applyShiftAfterDelete(deleted)
        if (wasComposingEmpty) {
            maybeUnlearnOnBackspaceReturn(ic)
        }
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
        
        // D-142 / D-143: a login field's value, or a URL being entered, must never be touched by
        // autocorrect/capitalisation/token-repair, and (for a login field) must never be learned
        // fragment-by-fragment into the ordinary dictionary - each `.`/`@`/`-`/`_` is its own delimiter
        // under the ordinary composing-token model, so without this short-circuit an email address or a
        // domain name would be autocorrected/capitalised piecemeal as it is typed. Every fragment commits
        // exactly as typed instead; a login field's whole value is captured once, separately, from the
        // field's own committed text (see captureCredentialIfLoginField), never here - a URL is simply
        // never learned into the dictionary at all, matching how it is never suggested from either
        // (refreshSuggestions).
        if (loginFieldKind != LoginFieldKind.NONE || urlMode) {
            if (composing.isEmpty()) {
                ic.commitText(delimiter, 1)
                clearUndo()
                return delimiter.length
            }
            return commitVerbatimFieldFragment(ic, delimiter)
        }
        
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
        // D-225: a token containing `_` is a technical identifier, not prose - no autocorrect, §6
        // capitalisation, token repair or dictionary learning of any kind must ever apply to it (unlike
        // commitVerbatim() above, which still learns the word - this is deliberately stronger). Checked
        // here, before dictChoice/diacriticWord/bestCorrection are even computed, so none of that search
        // runs for a token that can never resolve to a real dictionary word anyway.
        if ('_' in typed) {
            return commitVerbatim(ic, delimiter, learn = false)
        }
        // A-03: pick the dictionary/capitalisation for the recent context (German default, English or
        // Greek when explicitly active, English also auto-detected while German is active); suppress = a
        // confidently-foreign but unsupported language. D-106 stage 2: also suppressed when the token is
        // already a known word in some other consulted language (mandatory English + every G-01-cycle
        // language) - an embedded loanword like "Word" must never be silently corrected away.
        val dictChoice = selectActiveDictionary("$tokenContextBefore $typed")
        // D-172 (temporary diagnostic): the previous round's diag only showed the OR'd suppressAutocorrect,
        // which conflates two entirely different sources (A-03's foreign-language classification vs D-106
        // stage 2's cross-language protection) and never showed tokenContextBefore itself - needed now that
        // dictChoice.suppressAutocorrect was confirmed true for a context that should be a single word
        // (isForeign()'s own minWords=2 gate re-verified by hand against the current source and should not
        // have fired) - logging the raw context string is the only way to actually see why.
        val knownElsewhere = knownInOtherLanguage(typed)
        diag(
            "AdaptKeyJitter",
            "finalizeAndCommit: tokenContextBefore=\"$tokenContextBefore\" dictChoice.suppressAutocorrect=" +
                "${dictChoice.suppressAutocorrect} knownInOtherLanguage=$knownElsewhere"
        )
        // D-234: the user-facing autocorrect toggle folds into the exact same suppression flag D-106
        // stage 2 already uses - bestCorrection/rawCoordinateCorrection/trySplit (below) all already gate on
        // this one variable, so disabling autocorrect entirely reuses that existing architecture instead of
        // threading a new check through each of them separately.
        val suppressAutocorrect = dictChoice.suppressAutocorrect || knownElsewhere || !settings.autocorrectEnabled
        
        // A-06: merge the token onto a preceding spurious letter-ambiguous space, when linguistically valid.
        // D-234: also gated - a merge silently substitutes different text for what was typed, exactly the
        // behaviour the toggle promises to suppress.
        if (mergeChar != null && settings.autocorrectEnabled) {
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
        // D-154/D-155: diacriticWord is now also the actual correction below (not only a split veto), and
        // is deliberately evaluated against the German dictionary specifically and independent of
        // suppressAutocorrect/knownInOtherLanguage - "uber" (a bare ASCII token) reads as foreign to the
        // A-03 classifier without its umlaut, and "fur" happens to be a real English word, but neither is a
        // stronger signal than the umlauted form being an unambiguous, high-frequency German dictionary
        // entry - this project's own founding "umlauts are ordinary characters" principle says the restored
        // form should win. Confirmed via the bundled dict_en.tsv/dict_de.tsv: "fur" is a real (frequency
        // 366) English word, "uber" is in neither dictionary at all, while "für"/"über" are common German
        // words - Umlaut.fold() already round-trips both back to the bare ASCII form. Left off outside
        // German mode (G-01) - a user who explicitly switched to English/Greek does not want German umlaut
        // restoration forced onto their own explicit choice.
        // D-220 (temporary diagnostic): D-217's own handleKey timing log pointed at SPACE/punctuation as the
        // one keystroke class still slow after D-214/D-218 - this is the commit-time search chain behind it
        // (diacriticRestoration/bestCorrectionFor/trySplit/rawCoordinateCorrection below), never touched by
        // any of that work since a commit's own correction must stay synchronous. Timed per stage rather
        // than guessed, matching this project's own root-cause-before-fix convention - `adb logcat -s
        // AdaptKeyHaptics:D` shows the breakdown. Remove once D-220 is closed.
        val diacriticStartedAt = SystemClock.uptimeMillis()
        // D-234: diacriticWord is deliberately independent of suppressAutocorrect (see the comment on its
        // own use below) - the user-facing toggle is the one exception that must still gate it, since it is
        // itself a silent substitution the toggle promises to suppress.
        val diacriticWord = if (activeLanguage == Language.GERMAN && settings.autocorrectEnabled) {
            providers.getValue(Language.GERMAN).diacriticRestoration(typed, previousWord)
        } else {
            null
        }
        val diacriticMs = SystemClock.uptimeMillis() - diacriticStartedAt
        // D-207: a single search now answers both "what's the best correction" and "is it high-confidence
        // enough to veto a split" - previously two independent bestCorrection() searches for the exact
        // same token (highConfidenceCorrection() then autocorrectFor()), each running its own
        // store.correctionCandidates() scan, on every single commit. Gated on diacriticWord too (not just
        // suppressAutocorrect, as the old highConfidenceWord alone was) - split is vetoed by diacriticWord
        // regardless, so computing this at all in that case was already pure waste.
        val bestCorrectionStartedAt = SystemClock.uptimeMillis()
        val bestCorrection = if (diacriticWord != null || suppressAutocorrect) null else provider.bestCorrectionFor(typed, previousWord)
        val bestCorrectionMs = SystemClock.uptimeMillis() - bestCorrectionStartedAt
        val splitStartedAt = SystemClock.uptimeMillis()
        // D-226: suppressAutocorrect/knownElsewhere must veto a split exactly like it already vetoes
        // bestCorrection()/rawCoordinateCorrection() below - a token known in another consulted language
        // (D-106 stage 2, e.g. "commits", a real English word split into "com"+"its" against the German
        // dictionary) was never actually protected here, since this condition only ever checked
        // diacriticWord/bestCorrection's own confidence, not the same suppression flag every other
        // correction path in this function already respects.
        val split = if (diacriticWord != null || bestCorrection?.highConfidence == true || suppressAutocorrect) {
            null
        } else {
            tokenRepair.trySplit(typed, spaceAmbiguousIndices(), previousWord)
        }
        val splitMs = SystemClock.uptimeMillis() - splitStartedAt
        if (split != null) {
            // D-220: the split path returns before the dict=/suppressAutocorrect=/... diag further below is
            // ever reached, so it gets its own timing line here instead.
            diag(
                "AdaptKeyHaptics",
                "finalizeAndCommit: timing diacriticMs=$diacriticMs bestCorrectionMs=$bestCorrectionMs " +
                    "splitMs=$splitMs (split found)"
            )
            val committedLength = applySplit(ic, split, delimiter, typed)
            armShiftForNextWord(ic)
            return committedLength
        }
        
        // A-03: an unsupported foreign context leaves the token as typed; otherwise the selected language's
        // autocorrect applies (A-01 enforced in provider). D-39: when the ordinary edit-distance autocorrect
        // finds nothing, fall back to raw-coordinate correction - walk the token's actual raw taps (T-02) and
        // see whether the geometrically next-most-plausible key at any one position (T-03) produces a known
        // word. This recovers slips the static keyboard-adjacency map cannot see, since that map never looks
        // at where the tap actually landed. D-154/D-155: diacriticWord (computed above, independent of
        // suppressAutocorrect) wins outright when present - skipping autocorrectFor/rawCoordinateCorrection
        // entirely in that case, not just as a tie-breaker, since an unambiguous umlaut restoration is a
        // strictly stronger signal than either.
        val autocorrected = bestCorrection?.word
        val rawCorrectedStartedAt = SystemClock.uptimeMillis()
        val rawCorrected = if (diacriticWord == null && !suppressAutocorrect && autocorrected == null) rawCoordinateCorrection(typed) else null
        val rawCorrectedMs = SystemClock.uptimeMillis() - rawCorrectedStartedAt
        val corrected = diacriticWord ?: autocorrected ?: rawCorrected ?: typed
        // D-172 (temporary diagnostic): reported that an unknown-but-clearly-close-to-a-common-word token
        // (e.g. "aks" -> "als") sometimes never autocorrects at all, despite every gate traced by hand
        // (cost, frequency, blacklist, foreign-language classification) looking like it should fire. The
        // one variable this log did not previously show is which dictionary actually ran the check -
        // logged here so the next repro settles it instead of guessing further.
        diag(
            "AdaptKeyJitter",
            "finalizeAndCommit: dict=${dictChoice.language} suppressAutocorrect=$suppressAutocorrect " +
                "diacriticWord=$diacriticWord autocorrected=$autocorrected rawCorrected=$rawCorrected"
        )
        // D-220: see the split path's own timing line above for context - this is the same breakdown for
        // the non-split path, the one D-217's own handleKey timing log actually pointed at.
        diag(
            "AdaptKeyHaptics",
            "finalizeAndCommit: timing diacriticMs=$diacriticMs bestCorrectionMs=$bestCorrectionMs " +
                "splitMs=$splitMs rawCorrectedMs=$rawCorrectedMs"
        )
        // D-140: when the D-39 raw-coordinate fallback (not an ordinary dictionary/diacritic correction)
        // produced this correction, capture the single tap it actually changed - before clearComposing()
        // below wipes composingTaps - so a rejected correction can precisely un-train just that one
        // touch-model sample later (see performAutocorrectUndo). An ordinary autocorrect is never a
        // touch-resolution error, so it never gets a raw-correction undo at all.
        val rawCorrectionUndo = if (rawCorrected != null) rawCorrectionUndoFor(typed, rawCorrected) else null
        // §6 rule 6: a high-certainty tier-3 nominal proposal may lift an otherwise-lowercased word to
        // upper-case (never with the no-op backend, where lastCapProposal is null).
        val llmForcesUpper = HighCertaintyCapitalisation.forcesUpper(lastCapProposal, corrected)
        // §9 adaptive learning: capture the tier-3 result and pre-commit knowledge before learnWord mutates it.
        val tier3Result = lastTier3Result
        val contextWord = previousWord
        val tier1KnewCorrected = provider.isKnownWord(corrected)
        val finalWord = capitalisation.capitalise(corrected, contextFor(typed), llmForcesUpper)
        // D-139 (temporary diagnostic): the actual permanent write to the field - typed vs. what got
        // committed, so a scramble that only shows up in the final text (not just while composing) is
        // still caught here.
        diag("AdaptKeyJitter", "finalizeAndCommit: typed=\"$typed\" -> finalWord=\"$finalWord\" delimiter=\"$delimiter\"")
        
        ic.setComposingText(finalWord, 1)
        ic.finishComposingText()
        clearComposing()
        ic.commitText(delimiter, 1)
        val learnRecord = learnWord(finalWord)
        reinforceFromTier3(finalWord, tier3Result, contextWord, tier1KnewCorrected)
        
        if (finalWord != typed) {
            undoTyped = typed
            undoCommitted = finalWord
            undoDelimiter = delimiter
            undoWasSplit = false
            undoLearnRecords = listOf(learnRecord)
            undoRawCorrection = rawCorrectionUndo
            // D-88: the word actually changed - this is an accepted correction, not a plain commit.
            notifySuggestionAccepted(finalWord)
        } else {
            clearUndo()
        }
        // D-43: predict the next word instead of leaving the bar blank. D-247: a genuinely fresh promotion
        // this commit takes priority over the ordinary predictions (shown pinned ahead of them, not instead).
        showNextWordPredictions(learnRecord.word.takeIf { learnRecord.outcome == LearnOutcome.PROMOTED })
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
    
    /** D-140 / D-159: the single touch-model sample a D-39 raw-coordinate correction actually changed,
     * captured so [OffsetModel.unrecord] can be called with exactly the arguments (including the D-159
     * weight) the original [OffsetModel.record] call used. */
    private data class RawCorrectionUndo(
        val id: String,
        val centerX: Float,
        val centerY: Float,
        val x: Float,
        val y: Float,
        val weight: Double
    )
    
    /**
     * D-140: finds the single character position where [rawCorrected] (a D-39 [rawCoordinateCorrection]
     * result) actually differs from [typed], and looks up the raw tap and key geometry recorded for it -
     * so a later rejected-correction undo can reverse exactly that one touch-model sample, not the whole
     * token. Must be called before [clearComposing] runs (it reads [composingTaps], which that clears).
     * Returns null when no single differing position can be identified (defensive; [rawCoordinateCorrection]
     * only ever produces a single-position substitution, so this should always succeed when it fires).
     *
     * @param typed the token as typed, before the raw-coordinate correction
     * @param rawCorrected the raw-coordinate-corrected word
     * @return the touch sample to reverse on undo, or null
     */
    private fun rawCorrectionUndoFor(typed: String, rawCorrected: String): RawCorrectionUndo? {
        if (typed.length != rawCorrected.length) {
            return null
        }
        val index = typed.indices.firstOrNull { typed[it] != rawCorrected[it] } ?: return null
        val tap = composingTaps.getOrNull(index) ?: return null
        val id = "c:${typed[index].lowercaseChar()}"
        val geometry = keyboardView?.charKeyGeometry()?.firstOrNull { it.id == id } ?: return null
        return RawCorrectionUndo(id, geometry.centerX, geometry.centerY, tap.x, tap.y, tap.weight)
    }
    
    /**
     * Commits the composing token exactly as composed, followed by [delimiter] (G-05): no autocorrect,
     * no §6 capitalisation and no token repair. Used when the user fixed the token's casing explicitly
     * via a word-end Shift, which ranks as explicit input and must be preserved in both directions.
     *
     * @param learn D-225: false for a technical token containing `_` - such a token must never be learned
     *        into the personal dictionary either, unlike an ordinary hand-finished (G-05) word, which still
     *        is. Defaults to true so every pre-existing call site is unaffected.
     * @return D-122: the number of characters committed (`word.length + delimiter.length`) - see
     *         [finalizeAndCommit]'s own return contract
     */
    private fun commitVerbatim(ic: InputConnection, delimiter: String, learn: Boolean = true): Int {
        val word = composing.toString()
        ic.setComposingText(word, 1)
        ic.finishComposingText()
        clearComposing()
        resetWordEndShift()
        ic.commitText(delimiter, 1)
        if (learn) {
            learnWord(word)
        }
        clearUndo()
        // D-43: predict the next word instead of leaving the bar blank.
        showNextWordPredictions()
        armShiftForNextWord(ic)
        return word.length + delimiter.length
    }
    
    /**
     * D-142 / D-143: commits the composing fragment exactly as typed, followed by [delimiter], with no
     * autocorrect, §6 capitalisation, token-repair or dictionary learning at all - the [finalizeAndCommit]
     * short-circuit for every login-relevant field (username/email/password) and, since D-143, every
     * URL-mode field too. Neither a login field's value nor a URL is prose: both must never be silently
     * "corrected", and since the ordinary composing-token model treats every `.`/`@`/`-`/`_`/`/` as its own
     * delimiter, letting the normal pipeline touch even one fragment would risk corrupting the value across
     * several separately-finalised pieces. A login field's whole value is learned once, separately, from
     * the field's own committed text (see [captureCredentialIfLoginField]) - never per-fragment here, and
     * never into the ordinary dictionary; a URL is simply never learned into the dictionary at all.
     *
     * @return D-122: the number of characters committed (`word.length + delimiter.length`) - matches
     *         [finalizeAndCommit]'s own return contract
     */
    private fun commitVerbatimFieldFragment(ic: InputConnection, delimiter: String): Int {
        // D-170: finalizeAndCommit()'s login/urlMode branch returns before its own D-119/D-120 mid-word
        // split check ever runs, so this path never respected composingCursor at all - a delimiter typed
        // while the caret sits mid-fragment (e.g. repositioned via a tap to insert "@" between an
        // already-typed "foo" and "bar") always committed after the *whole* fragment instead of exactly
        // where the caret was. Mirrors splitComposingAtCaretAndCommit's own before/after split and
        // arithmetic anchor, without the recursive finalizeAndCommit()/autocorrect step this verbatim path
        // never uses anyway - a login/email/URL fragment is never autocorrected or capitalised.
        val splitAt = composingCursor
        val beforeAnchor = composingAnchor
        val beforeText = composing.substring(0, splitAt)
        val afterText = composing.substring(splitAt)
        val afterFlags = ArrayList(composingFlags.subList(splitAt, composingFlags.size))
        val afterTaps = if (splitAt < composingTaps.size) {
            ArrayList(composingTaps.subList(splitAt, composingTaps.size))
        } else {
            ArrayList()
        }
        ic.beginBatchEdit()
        try {
            ic.setComposingText(beforeText, 1)
            ic.finishComposingText()
            clearComposing()
            resetWordEndShift()
            ic.commitText(delimiter, 1)
            // D-174 / D-190: mirror what just landed in the real document into the in-memory fallback
            // snapshot - see captureCredentialIfLoginField's own KDoc. Only login-relevant fields ever get
            // read back out of it, so a pure urlMode fragment is skipped to avoid pointlessly accumulating
            // it; likewise skipped outright when the user has disabled credential recording entirely - it
            // would never be read, only held in memory for nothing.
            if (loginFieldKind != LoginFieldKind.NONE && settings.saveCredentials) {
                credentialSnapshot.append(beforeText).append(delimiter)
            }
            if (afterText.isNotEmpty()) {
                captureTokenContext(ic)
                composing.append(afterText)
                composingFlags.addAll(afterFlags)
                composingTaps.addAll(afterTaps)
                composingCursor = 0
                composingAnchor = if (beforeAnchor >= 0) beforeAnchor + beforeText.length + delimiter.length else -1
                updateComposing(ic)
            }
        } finally {
            ic.endBatchEdit()
        }
        clearUndo()
        return beforeText.length + delimiter.length
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
        val learnRecord = learnWord(cased)
        clearUndo()
        // D-43: predict the next word instead of leaving the bar blank. D-247: pin the "Gelernt: X"
        // confirmation ahead of it when this merge is what just crossed the promotion threshold.
        showNextWordPredictions(learnRecord.word.takeIf { learnRecord.outcome == LearnOutcome.PROMOTED })
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
        val left = capitalisation.capitalise(split.left, contextFor(split.left))
        val right = capitalisation.capitalise(split.right, followingPartContext())
        val committed = left + " " + right
        // D-245: setComposingText("")/finishComposingText() and commitText() batched into one editor
        // update - unbatched (as before), some editors report the intermediate "composing just wiped"
        // state as its own onUpdateSelection callback, arriving before the real, final commit's own
        // callback. reclaimWordAtCaret() runs on *both*, but the D-123 suppressNextReclaimSpaceReset guard
        // is single-shot - the first (spurious, intermediate) callback consumed it, leaving the second
        // (real) callback to hit the unguarded else branch and silently clear pendingSuggestionSpace before
        // the next keystroke (e.g. a period right after the D-122 split chip) ever saw it armed. Mirrors the
        // D-87 batching precedent (spec §1) - the app coalesces every batched edit into one reported update.
        ic.beginBatchEdit()
        try {
            ic.setComposingText("", 1)
            ic.finishComposingText()
            clearComposing()
            ic.commitText(committed + delimiter, 1)
        } finally {
            ic.endBatchEdit()
        }
        val leftRecord = learnWord(left)
        val rightRecord = learnWord(right)
        // A-07: arm the undo so the next backspace reverts the split (see performAutocorrectUndo).
        undoTyped = typed
        undoCommitted = committed
        undoDelimiter = delimiter
        // D-13: mark this as a split, so undoing it trains the rejoined word.
        undoWasSplit = true
        // D-140: precisely un-learn both halves on undo, see performAutocorrectUndo. A split never
        // involves the D-39 raw-coordinate path, so there is no touch-model sample to un-train here.
        undoLearnRecords = listOf(leftRecord, rightRecord)
        undoRawCorrection = null
        // D-43: predict the next word (following the right-hand split part) instead of a blank bar. D-247:
        // pin the "Gelernt: X" confirmation ahead of it when either half just crossed the promotion
        // threshold - the right half takes priority (it is the word actually adjacent to what comes next);
        // both promoting in the very same commit is a rare enough edge case not to need its own UI.
        val justPromoted = rightRecord.word.takeIf { rightRecord.outcome == LearnOutcome.PROMOTED }
            ?: leftRecord.word.takeIf { leftRecord.outcome == LearnOutcome.PROMOTED }
        showNextWordPredictions(justPromoted)
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
     * D-201: [delimiter] is the caller's own D-144/D-183 "don't double an already-present space" result -
     * this path used to hardcode a trailing space unconditionally, exactly the class of gap §117/D-183 had
     * already flagged as present but unreproduced here ("applyMidWordSplitSuggestion()/applySplit() ... have
     * the same class of gap but were not touched"). Applying `"der Kinderarzt"` mid-word into
     * `"dervKinderarzt "` (a real space already follows) was silently doubling it.
     *
     * @param ic the current input connection
     * @param word the pre-capitalised `"$left $right"` suggestion text
     * @param delimiter the delimiter to commit after [word]: `" "` normally, or `""` when the document
     *        already has whitespace right after the (possibly mid-word) composing token
     */
    private fun applyMidWordSplitSuggestion(ic: InputConnection, word: String, delimiter: String) {
        val (left, right) = word.split(' ', limit = 2)
        applySplit(ic, SplitResult(left.lowercase(), right.lowercase()), delimiter = delimiter, typed = composing.toString())
        // D-88: tapping a bar suggestion is always an accepted suggestion, regardless of whether it happens
        // to match what was typed.
        notifySuggestionAccepted(word)
        // D-29 / D-201: arm the trailing space applySplit's own delimiter added, so immediate punctuation
        // removes it - only when a space was actually added; a pre-existing one mid-text is real document
        // content, not ours to eat (mirrors the ordinary NORMAL branch's identical guard).
        pendingSuggestionSpace = delimiter.isNotEmpty()
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
        val learnRecords = undoLearnRecords
        val rawCorrection = undoRawCorrection
        ic.deleteSurroundingText(undoCommitted.length + undoDelimiter.length, 0)
        ic.commitText(typed + undoDelimiter, 1)
        previousWord = typed
        clearUndo()
        // D-140: un-learn exactly what the rejected commit persisted (whether it reinforced an
        // already-known word or newly promoted/counted an unknown one) before re-learning what the user
        // insisted on - otherwise a wrong correction/pairing keeps being reinforced every time it is
        // typed and rejected, even though the user never actually accepted it.
        learnRecords.forEach { unlearnWord(it) }
        // D-140: a rejected D-39 raw-coordinate correction means the touch model resolved this one tap to
        // the wrong key - reverse just that single sample. An ordinary dictionary/diacritic/split
        // correction never sets this (see rawCorrectionUndoFor's call site), so this only ever fires for
        // the raw-coordinate path, never for a purely linguistic correction the user happened to reject.
        if (rawCorrection != null) {
            offsetModel?.unrecord(
                rawCorrection.id,
                rawCorrection.centerX,
                rawCorrection.centerY,
                rawCorrection.x,
                rawCorrection.y,
                rawCorrection.weight
            )
        }
        if (wasSplit) {
            // D-13: undoing a wrong A-05 split trains the rejoined word at once, so it is never split again.
            learnWordStrong(typed)
        } else {
            learnWord(typed)
        }
        previousWord = typed
        // D-246: learnWord()/learnWordStrong() above just shifted previousPreviousWord from the transient
        // (self-referential) state this function set at line 2925, not the real word that preceded the
        // now-undone commit - restore it from the first LearnRecord's own previousWord, captured back when
        // the original (now-rejected) commit actually happened, before undo touched anything.
        previousPreviousWord = learnRecords.firstOrNull()?.previousWord
        // D-43: predict the next word (following the word the user insisted on) instead of a blank bar.
        showNextWordPredictions()
        armShiftForNextWord(ic)
    }
    
    /**
     * @param duringRepeat D-153: true while called from an active [handleBackspaceRepeat] tick - skips the
     *        colour-preview rendering below entirely (never even reads [composingPreview]), since it is
     *        never actually read mid-repeat (the composing text is changing every 45-330ms, see
     *        [BackspaceRepeat]) - the same D-138 diagnosis (uncached per-keystroke dictionary lookups
     *        competing with the repeat's fastest tick interval), but for a call site D-138 itself never
     *        covered (it only gated [refreshSuggestions]'s two additions). Every other call site keeps the
     *        full live preview (the default `false`) - "live" now meaning the cached, debounced result from
     *        [scheduleComposingPreviewRefresh] (§125 / D-194), not a fresh computation every call; see there.
     */
    private fun updateComposing(ic: InputConnection, duringRepeat: Boolean = false) {
        val text = composing.toString()
        // D-139 (temporary diagnostic): the exact string pushed to the field as composing text, on every
        // call - a scrambled/reordered result should show up directly in this sequence of log lines.
        diag("AdaptKeyJitter", "updateComposing: text=\"$text\" anchor=$composingAnchor cursor=$composingCursor")
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
            // §125 / D-194 / D-213: neither lookup runs here anymore - both read composingPreview, computed
            // off-thread by composingPreviewRunnable (see its own field-level KDoc). D-242: the split
            // preview still requires composingPreviewFor to exactly match text (a stale, different-length
            // split's span ranges are not safe to reapply); the plain highlight instead tolerates a
            // same-token-but-stale preview - see previewForSameToken below.
            if (!duringRepeat) {
                scheduleComposingPreviewRefresh(text)
            }
            val split = if (!duringRepeat && composingPreviewFor == text) composingPreview.split else null
            // D-242: the plain whole-word highlight (unlike the split preview just above, which needs an
            // exact text match - applying stale span ranges to a since-changed-length token risks a real
            // out-of-bounds/misplaced-span bug, not just a stale-looking display) now tolerates a
            // *stale-but-still-the-same-token* preview: composingPreviewFor is a prefix of text (the user
            // kept extending the token since the last completed background pass) or vice versa (a character
            // was deleted since). Reported bug: typing slowly enough that each keystroke's gap exceeds the
            // ~200ms debounce made the highlight flash green-black-green on every letter, since the old
            // strict equality reset to plain the instant any keystroke landed, even mid-word. Confirmed
            // accepted trade-off: a token can now stay shown as highlighted for up to one debounce cycle
            // after it has actually stopped being a real word (e.g. "Test" then "x" appended) - user-approved
            // as clearly preferable to the flicker.
            val previewForSameToken = composingPreviewFor?.let { text.startsWith(it) || it.startsWith(text) } == true
            val highlighted = !duringRepeat && previewForSameToken && composingPreview.highlighted
            if (split != null) {
                val (leftRange, rightRange) = split.spanRanges(text)
                val span = SpannableString(text)
                span.setSpan(ForegroundColorSpan(config.highlightColor), leftRange.first, leftRange.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(ForegroundColorSpan(config.highlightColor), rightRange.first, rightRange.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ic.setComposingText(span, 1)
            } else if (highlighted) {
                val span = SpannableString(text)
                span.setSpan(ForegroundColorSpan(config.highlightColor), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ic.setComposingText(span, 1)
            } else {
                // D-229: a bare String here (as opposed to every other branch above, which always sends a
                // Spanned) is what makes the target editor apply its own default composing-text underline
                // decoration - suppressed once the IME supplies any explicit character styling. Since D-194
                // moved the S-05 highlight decision onto a ~200ms debounce, this branch is now what every
                // keystroke sends *first* (the styled branches above only apply once the debounce catches
                // up), so the editor's own underline now visibly flashes in and out on every keystroke that
                // does not yet have a settled highlight decision - never a deliberate cue this app intended,
                // just an accidental side effect of no longer sending a Spanned unconditionally. Wrapping in
                // an unstyled SpannableString (identical rendering otherwise) keeps every keystroke already
                // "styled" from the editor's point of view, so it never adds its own underline in the first
                // place - regardless of whether a highlight is pending or never comes at all.
                ic.setComposingText(SpannableString(text), 1)
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
     * §125 / D-194 / D-213 / D-215: (re-)schedules [composingPreviewRunnable] to compute the real S-05/§47
     * colour preview for [text], unless it is already scheduled for that exact text or already cached for
     * it ([composingPreviewFor]). Called only from [updateComposing]'s non-repeat path, so the previous
     * pending callback (for whatever the token was before this keystroke) is implicitly superseded.
     * D-215: waits [EXPENSIVE_SUGGESTION_DELAY_MS] before dispatching, restored after D-213 removed it -
     * the runnable's own expensive part already runs off the main thread on [composingPreviewExecutor], so
     * the delay is no longer about protecting the UI thread, only about not bothering to compute a colour
     * nobody can see flash past mid-burst (see [expensiveSuggestionExecutor]'s own field KDoc for the full
     * reasoning, which applies identically here).
     *
     * @param text the current composing token
     */
    private fun scheduleComposingPreviewRefresh(text: String) {
        if (composingPreviewFor == text || composingPreviewToken == text) {
            return
        }
        handler.removeCallbacks(composingPreviewRunnable)
        composingPreviewToken = text
        handler.postDelayed(composingPreviewRunnable, EXPENSIVE_SUGGESTION_DELAY_MS)
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
        // §125 / D-194 / D-215: not required for correctness (composingPreviewFor/expensiveSuggestionSeq are
        // always checked against the current composing text/generation before use) but avoids leaving a
        // pointless callback scheduled for a token that no longer exists.
        handler.removeCallbacks(composingPreviewRunnable)
        handler.removeCallbacks(expensiveSuggestionRunnable)
        composingPreviewToken = null
        composingPreviewFor = null
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
     * D-159: the absolute document offset of the new composing region's start is now resolved here
     * unconditionally, via [InputConnection.getExtractedText] - not only when letters follow the caret too
     * - since [onUpdateSelection]'s ownEdit check depends on it for every composing token, not only a
     * genuine mid-word reclaim (see [composingAnchor]'s own KDoc). When extraction is unavailable (a rare
     * editor quirk) the anchor is simply left unresolved (-1) rather than aborting the reclaim outright;
     * [onUpdateSelection] already treats an unresolved anchor conservatively (never wipes), so this degrades
     * gracefully instead of silently orphaning the surrounding text in the document.
     *
     * @param ic the current input connection
     * @param tap the raw tap to record for the reclaimed characters, or null to leave them untracked
     */
    private fun reclaimSurroundingWord(ic: InputConnection, tap: TapPoint?) {
        val after = ic.getTextAfterCursor(MAX_CONTEXT_LOOKBACK, 0) ?: ""
        val reclaim = WordExtent.reclaim(tokenContextBefore, after)
        // D-87 / D-159: see ComposingAnchor for why startOffset must be added, not just selectionStart
        // alone. Read before any mutation below, matching this class's established read-before-mutating
        // convention (see splitComposingAtCaretAndCommit's own D-122 note).
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        val anchor = if (extracted != null) {
            ComposingAnchor.resolve(extracted.startOffset, extracted.selectionStart, reclaim.before.length)
        } else {
            -1
        }
        if (reclaim.before.isEmpty() && reclaim.after.isEmpty()) {
            composingAnchor = anchor
            return
        }
        // D-139 (temporary diagnostic): a reclaim mutates the real editable, then rebuilds composing from
        // these two fragments - exactly the mechanism §73/§76 suspect for the reported jitter/scramble, so
        // every actual reclaim (not the common no-op above) is logged.
        diag("AdaptKeyJitter", "reclaimSurroundingWord: before=\"${reclaim.before}\" after=\"${reclaim.after}\"")
        // D-182: mark the already-existing text as the composing region in place, rather than deleting and
        // re-inserting it - the delete step briefly left the reclaimed span empty before updateComposing()'s
        // follow-up setComposingText() restored it, which some editors (observed live: the Gemini search
        // field) rendered as a visible flash, and in one captured case even echoed that transient
        // collapsed-at-anchor state back as a *third*, differently-stale onUpdateSelection call whose own
        // ground-truth read then agreed with it - fooling §101's verification into misreading the
        // correctly-rebuilt reclaim as a genuine external change and tearing it down. setComposingRegion()
        // never removes a character, so there is no transient empty state left for any of this to observe
        // in the first place. Falls back to the old delete/reinsert only when the anchor could not be
        // resolved at all (anchor < 0, e.g. a failed ExtractedText read) - the same rare, already-
        // conservative case this method already treats specially just above.
        if (anchor >= 0) {
            ic.setComposingRegion(anchor, anchor + reclaim.before.length + reclaim.after.length)
        } else {
            ic.deleteSurroundingText(reclaim.before.length, reclaim.after.length)
        }
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
     * D-26: whether the cursor sits inside an existing word rather than at its end - a letter immediately
     * following the cursor means the current edit is correcting mid-word, so the composing fragment must
     * not be coloured (C-04/S-05). D-213: the highlight/split-colour decision itself (formerly
     * `shouldHighlightComposing()`/`splitPreview()`) now lives inline in [composingPreviewRunnable], since
     * both need this InputConnection-dependent check done on the main thread before the rest of the
     * decision can move to [composingPreviewExecutor].
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
     *        added. §125 / D-194: also now skips the `pending` autocorrect-preview lookup, which had been
     *        left unconditional by oversight (the same expensive search this whole gate exists to avoid).
     *        Every other call site keeps the full live preview (the default `false`).
     * @param includeExpensiveFallbacks D-160/D-208/D-211: true only when re-entering after the background
     *        [expensiveSuggestionExecutor] search below has finished - runs D-12 fuzzy matching and, once
     *        that also finds nothing, the expensive empty-candidates fallbacks (D-116 compound split,
     *        D-117 wide fuzzy, D-131 raw-coordinate) the per-keystroke hot path skips entirely. Traced from
     *        a real device log (spec §102): for a long unknown compound the empty-candidates gate is true
     *        on every keystroke, so exactly the worst-case token ran the whole chain per keystroke,
     *        saturating the main thread badly enough to starve callback delivery for seconds; D-208/D-211
     *        later found the same was true of fuzzy matching itself, and moved the search off the main
     *        thread entirely rather than only delaying when it ran there (see
     *        [expensiveSuggestionExecutor]'s own field KDoc).
     * @param precomputedExpensiveCandidates D-211: the background search's own already-computed result,
     *        supplied only by its own re-entrant call below - used in place of calling
     *        `provider.suggestionsFor()` a second time. Every other caller leaves this null.
     */
    private fun refreshSuggestions(
        duringRepeat: Boolean = false,
        includeExpensiveFallbacks: Boolean = false,
        precomputedExpensiveCandidates: List<Suggestion>? = null,
        precomputedPendingCandidate: String? = null
    ) {
        // D-211: any fresh (non-deferred) refresh reflects a state change that supersedes a background
        // expensive-fallback search already in flight - bumping the sequence here (before every early
        // return below) is what that search polls to recognise itself as stale, both before it starts and
        // partway through (see expensiveSuggestionSeq's own field KDoc).
        if (!includeExpensiveFallbacks) {
            expensiveSuggestionSeq.incrementAndGet()
        }
        // D-143: a URL is not natural-language prose - no dictionary word or autocorrect candidate is ever
        // useful while entering one, so the bar simply stays empty.
        if (urlMode) {
            clearSuggestions()
            return
        }
        // D-142: a recognised login field replaces the entire ordinary pipeline below - offering a normal
        // dictionary word while entering a username/email is never useful, and a password field shows no
        // suggestions at all (see showCredentialSuggestions).
        if (loginFieldKind != LoginFieldKind.NONE) {
            showCredentialSuggestions()
            return
        }
        val input = composing.toString()
        if (input.isEmpty()) {
            clearSuggestions()
            return
        }
        // D-225: a technical identifier containing `_` is never prose - no dictionary/fuzzy/compound-split
        // suggestion and no pending-correction chip is ever useful for it, mirroring urlMode's own bypass
        // above. Checked before the A-03 dictionary lookup so no store query runs for it at all.
        if ('_' in input) {
            clearSuggestions()
            return
        }
        // A-03: pick the dictionary for the recent context; an unsupported foreign context shows nothing.
        if (selectActiveDictionary("$tokenContextBefore $input").suppressAutocorrect) {
            clearSuggestions()
            return
        }
        // D-153: provider.suggestionsFor() runs several uncached dictionary lookups per call (a prefix
        // scan, fuzzy-neighbour edit-distance search, and conditionally a compound-split attempt - see its
        // own KDoc, which already names D-138's exact lesson) and grew heavier still after D-138 shipped
        // (D-116's compound split, D-147's Umlaut.unfoldCandidates) without ever being added to this
        // function's own duringRepeat gate - a plausible, traced explanation for backspace-hold feeling
        // jerky again. The bar it populates is changing every 45-330ms mid-repeat regardless (unreadable),
        // exactly the same reasoning already applied to every other addition below.
        // D-211: precomputedExpensiveCandidates, when given, is the background search's own already-final
        // result (see the dispatch below) - used as-is instead of calling suggestionsFor() a second time.
        val candidates = when {
            duringRepeat -> emptyList()
            precomputedExpensiveCandidates != null -> precomputedExpensiveCandidates
            else -> provider.suggestionsFor(input, previousWord, includeExpensiveFallbacks)
        }
        // D-160/D-208/D-211/D-215: schedule the deferred pass (fuzzy neighbours plus, once those also find
        // nothing, the expensive last-resort fallbacks) whenever the hot path ran without them - no longer
        // gated on candidates.isEmpty(): D-208 moved fuzzy matching itself into this tier, so a prefix
        // completion finding something on the hot path must not skip fuzzy's own chance to contribute too
        // (D-12: "mut" must still surface "mit" alongside "mut"'s own prefix completions). D-215: waits for
        // EXPENSIVE_SUGGESTION_DELAY_MS of real stability before dispatching anything at all - not to
        // protect the main thread (expensiveSuggestionExecutor already does that, see its own field KDoc),
        // but because none of this is perceivable mid-burst, so it is not worth computing yet either.
        if (!duringRepeat && !includeExpensiveFallbacks) {
            handler.removeCallbacks(expensiveSuggestionRunnable)
            handler.postDelayed(expensiveSuggestionRunnable, EXPENSIVE_SUGGESTION_DELAY_MS)
        }
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
        // D-160: part of the same empty-candidates escalation, so it now also waits for the deferred pass.
        val rawCoordinateSuggestion = if (duringRepeat || !includeExpensiveFallbacks || candidates.isNotEmpty()) {
            null
        } else {
            rawCoordinateCorrection(input)?.let { word -> Suggestion(word, MAX_PRIORITY_SUGGESTION_SCORE) }
        }
        // D-238: with autocorrect disabled, A-05/A-06 no longer auto-apply (see finalizeAndCommit()'s
        // suppressAutocorrect/mergeChar gates) - "everything must go through the suggestions" (the user's
        // own words) means the split/merge candidate that *would* have silently applied is instead offered
        // as a position-1 chip, exactly like D-122's own mid-word connector-split chip. The split reads the
        // already-computed, debounced composingPreviewRunnable result (D-125/D-213) rather than running
        // trySplit() a second time - it is the exact same computation finalizeAndCommit() would have used,
        // since suppressAutocorrect is unconditionally true in this state (no diacriticWord/bestCorrection
        // veto ever applies here either). Tapping it is handled for free by the existing D-122
        // `item.word.contains(' ')` branch in onSuggestionClicked() - no new click-handling needed.
        val autocorrectSplitChip = if (!duringRepeat && !settings.autocorrectEnabled && composingPreviewFor == input) {
            composingPreview.split?.let { split ->
                val left = capitalisation.capitalise(split.left, contextFor(split.left))
                val right = capitalisation.capitalise(split.right, followingPartContext())
                Suggestion("$left $right", MAX_PRIORITY_SUGGESTION_SCORE)
            }
        } else {
            null
        }
        // D-238: the A-06 counterpart - cheap enough (no diacritic/bestCorrection dependency) to compute
        // directly here rather than via the debounced pipeline. Tapping it is handled by a dedicated check
        // in onSuggestionClicked() (a merge candidate is a single word, unlike the split's own two-word
        // text, so it cannot be told apart from an ordinary dictionary completion by shape alone).
        val autocorrectMergeChip = if (!duringRepeat && !settings.autocorrectEnabled) {
            pendingMergeChar?.let { mc ->
                tokenRepair.tryMerge(previousWord, mc, input)?.let { merged ->
                    Suggestion(capitalisation.capitalise(merged, contextFor(merged)), MAX_PRIORITY_SUGGESTION_SCORE)
                }
            }
        } else {
            null
        }
        val extras = listOfNotNull(splitSuggestion, rawCoordinateSuggestion, autocorrectSplitChip, autocorrectMergeChip)
        // §125 / D-194: duringRepeat used to still call provider.autocorrectFor() here unconditionally -
        // the exact same expensive bestCorrection() search (a store query plus a banded edit-distance scan
        // per candidate) this function's own KDoc already gates everything else behind, simply missed when
        // the duringRepeat branch was introduced (D-138/D-153). Its result (the impending-autocorrect
        // preview chip) is exactly as unreadable mid-repeat as every other lookup already skipped here.
        //
        // D-218: the same expensive diacriticRestoration()/autocorrectFor() search used to also run here
        // unconditionally on every ordinary keystroke (not gated on includeExpensiveFallbacks at all) - it
        // was never moved off this synchronous path by D-207-D-217, which all focused on `candidates` and
        // the composing-preview split highlight; AdaptKeyHaptics's own new handleKey() timing log (D-217) is
        // what actually pointed at it as the still-unaddressed dominant per-keystroke cost. The hot/immediate
        // path now shows only the cheap capitalisation-only preview (e.g. a sentence-start capital, D-111/
        // D-112) computed directly from `input`; the expensive whole-word replacement only appears once
        // dispatchExpensiveSuggestionSearch() has actually searched for one on the background executor and
        // supplied it as [precomputedPendingCandidate] - exactly as imperceptible mid-burst as every other
        // expensive search this investigation already deferred.
        val pending = if (duringRepeat) {
            null
        } else {
            // D-111 / D-112: run the eventual committed form through the same §6 capitalisation
            // finalizeAndCommit() will apply, so a pending *case-only* change (D-111 - e.g. an ordinary noun
            // about to be auto-capitalised) is visible as the existing S-06 pending chip before it is ever
            // silently applied, and a spelling correction's own case already follows the sentence/field
            // context (D-112 - "Fur" at a sentence start previews as "Für", not "für"). Deliberately mirrors
            // only the autocorrectFor/diacritic-fold path (cost-0 within it, so already covered) and not the
            // rarer raw-coordinate-correction fallback, which needs the real composing taps/geometry and
            // isn't worth computing on every keystroke just for this preview.
            val capitalizedPreview = capitalisation.capitalise(precomputedPendingCandidate ?: input, contextFor(input))
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
     * D-211/D-215: the actual deferred-pass search, dispatched only once [expensiveSuggestionRunnable]
     * fires (i.e. [input] has been stable for [EXPENSIVE_SUGGESTION_DELAY_MS]) - runs entirely on
     * [expensiveSuggestionExecutor], never the main thread. A superseded call bails before touching the
     * database, or partway through its own candidate scan (see [DictionarySuggestionProvider]'s own KDoc
     * on `isCancelled`); the result is applied via one more [refreshSuggestions] call, re-entering on the
     * main thread through `handler.post`, only if [expensiveSuggestionSeq] and the composing text are both
     * still exactly what they were when this was dispatched.
     *
     * D-218: also runs [pendingCorrectionCandidate] here now, alongside `suggestionsFor` - the same
     * background executor, the same staleness guard, and the same single `refreshSuggestions` call applies
     * both results together, rather than a second independent dispatch that would recompute `activeLanguage`
     * a second time for no reason. `language` is read once here, on the main thread, before the executor
     * lambda even starts, exactly like [input]/[previous] themselves - see [knownInOtherLanguage]'s own KDoc
     * for why a background read of the live field would not be safe.
     *
     * @param input the composing token this search is for
     * @param previous the preceding word for n-gram context, or null at a fresh start
     */
    private fun dispatchExpensiveSuggestionSearch(input: String, previous: String?) {
        val seq = expensiveSuggestionSeq.get()
        val language = activeLanguage
        expensiveSuggestionExecutor.execute {
            if (seq != expensiveSuggestionSeq.get()) {
                return@execute
            }
            val expanded = provider.suggestionsFor(input, previous, includeExpensiveFallbacks = true) {
                seq != expensiveSuggestionSeq.get()
            }
            val pendingCandidate = pendingCorrectionCandidate(input, previous, language)
            handler.post {
                if (seq == expensiveSuggestionSeq.get() && composing.toString() == input) {
                    refreshSuggestions(
                        includeExpensiveFallbacks = true,
                        precomputedExpensiveCandidates = expanded,
                        precomputedPendingCandidate = pendingCandidate
                    )
                }
            }
        }
    }
    
    /**
     * D-218: the raw (not yet capitalised) impending-autocorrect/diacritic-restoration replacement for
     * [input] - the whole-word search half of [refreshSuggestions]'s own `pending` preview, split out so
     * [dispatchExpensiveSuggestionSearch] can run it on the background executor exactly like it already does
     * for [SuggestionProvider.suggestionsFor]. Capitalisation is deliberately not applied here - it reads
     * [capsMode]/[tokenSentenceStart]/[tokenAfterHyphen]/[fieldMandateOverridden] (see [contextFor]), all
     * mutable main-thread fields, so it stays a main-thread step applied once the result is back, exactly
     * like [composingPreviewRunnable]'s own split result.
     *
     * @param input the composing token to search a replacement for
     * @param previousWord the preceding word for n-gram tie-breaking, or null at a fresh start
     * @param language the language to treat as active for this search (snapshotted by the caller - see
     *        [knownInOtherLanguage]'s own KDoc)
     * @return the replacement word in its own canonical case, or null when none applies
     */
    private fun pendingCorrectionCandidate(input: String, previousWord: String?, language: Language): String? {
        // D-204: mirrors finalizeAndCommit()'s own diacriticWord-first precedence - diacriticRestoration has
        // no minAutocorrectFrequency floor at all (D-114), while autocorrectFor()/bestCorrection() always
        // applies one, so a rare-but-exact diacritic match (e.g. "Grüße", frequency 18) must be consulted
        // separately rather than assumed to already be covered by the cost-0 case below.
        val diacriticCandidate = if (language == Language.GERMAN) {
            providers.getValue(Language.GERMAN).diacriticRestoration(input, previousWord)
        } else {
            null
        }
        // D-106 stage 2: never pend a silent replacement for a word already known in another consulted
        // language (mandatory English + every G-01-cycle language) - the active language's own completions
        // are still shown as usual, only the impending-autocorrect chip is suppressed.
        return diacriticCandidate ?: if (knownInOtherLanguage(input, language)) null else provider.autocorrectFor(input, previousWord)
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
        // D-196: SuggestionController itself stays free of any capitalisation/Android dependency (its own
        // S-02/S-03 identity and dedup logic already relies on comparing raw canonical dictionary words, not
        // display text - capitalising earlier would break that), so every Kind.NORMAL entry's *display* text
        // is derived here, right before rendering, via the exact same capitalisation.capitalise(word,
        // contextFor(...)) call onSuggestionClicked() already uses to decide what actually gets committed on
        // a tap - the same formula, the same live `composing` state, so what is shown and what would commit
        // can no longer diverge. The already-capitalised S-06 pending-replacement item (also Kind.NORMAL,
        // D-111/D-112) is safely re-capitalised too: capitalise() is a pure function of (word, context), so
        // reapplying it to its own prior output yields the identical string back.
        val context = contextFor(composing.toString())
        val items = controller.displayed().map { item ->
            if (item.kind == SuggestionController.Kind.NORMAL) {
                item.copy(text = capitalisation.capitalise(item.word, context))
            } else {
                item
            }
        }
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
    /**
     * @param justPromoted D-247: when a word was just promoted to the learned dictionary this exact commit
     *        (see [LearnOutcome.PROMOTED]), the D-246 "Gelernt: X" confirmation chip for it - pinned ahead
     *        of the ordinary predictions below, never subject to their own ranking. Null (the default) for
     *        every ordinary call site, which keeps this function's prior behaviour unchanged.
     */
    private fun showNextWordPredictions(justPromoted: String? = null) {
        handler.removeCallbacks(resortRunnable)
        lastTier3Result = Tier3Result.EMPTY
        lastCapProposal = null
        val previous = previousWord
        val predictions = (if (previous == null) emptyList() else provider.nextWordSuggestions(previous, previousPreviousWord)) +
            listOfNotNull(timeSuggestion())
        controller.clear()
        controller.update("", predictions, null)
        if (justPromoted != null) {
            // D-247: Kind.LEARNED needs its own DisplayItem, not just a high-scoring Suggestion, so it is
            // built directly and prepended - the same "built outside SuggestionController" shape D-36/D-142's
            // own CLIPBOARD/CREDENTIAL chips already use - regardless of whether any ordinary prediction
            // exists to follow it (unlike the empty-predictions early-out below, this must never be silently
            // dropped just because there happens to be nothing else to predict).
            val chip = SuggestionController.DisplayItem(
                text = getString(R.string.suggestion_learned_label, justPromoted),
                kind = SuggestionController.Kind.LEARNED,
                word = justPromoted
            )
            suggestionBar?.setItems(listOf(chip) + controller.displayed())
            suggestionBar?.visibility = View.VISIBLE
            return
        }
        if (predictions.isEmpty()) {
            clearSuggestions()
            return
        }
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
    
    
    /**
     * D-140: what [learnWord] actually did for one word, so a later A-07 undo can reverse exactly that
     * and nothing else.
     */
    private enum class LearnOutcome {
        /** Not a letters-only token, too short (D-247), or blank - [learnWord] did nothing. */
        SKIPPED,
        
        /** [DictionaryStore.learn] was called to reinforce an *already-known* word (already learned or
         * bundled-and-known regardless). Distinct from [PROMOTED] - see D-247's own confirmation chip,
         * which must fire only on a genuinely new promotion, never on every reinforcement of a word
         * already typed a hundred times before. Undoes via [DictionaryStore.unlearn], same as [PROMOTED]. */
        LEARNED,
        
        /** [DictionaryStore.learn] was called because a new word was just promoted past
         * [LEARN_THRESHOLD]/[COMPOUND_LEARN_THRESHOLD] this exact commit (D-37) - the one moment D-247's
         * "Gelernt: X" confirmation chip fires for. Undoes identically to [LEARNED] via
         * [DictionaryStore.unlearn]. */
        PROMOTED,
        
        /** The word was new and not yet promoted - only [PendingLearnStore] was incremented. */
        PENDING
    }
    
    /** One word [learnWord] processed, with the context it used, for a precise [unlearnWord]. */
    private data class LearnRecord(
        val word: String,
        val previousWord: String?,
        val previousPreviousWord: String?,
        val outcome: LearnOutcome
    )
    
    /**
     * D-202: the D-37 promotion threshold to use for [word] - re-evaluated fresh on every call, never
     * cached or persisted anywhere, so a later change to either signal below reclassifies an
     * already-pending word retroactively with no data migration needed.
     *
     * Two signals, cheapest first: an embedded capital past the first character
     * ([hasEmbeddedCapital]) - a near-certain "this was meant to be two words" sign, a pure string scan with
     * no dictionary lookup at all - and, when that alone is not conclusive,
     * [SuggestionProvider.looksLikeUnsplitCompound] (D-116's own noun + Fugenelement + resolvable-rest
     * compound recognition, confirmed sufficient on its own). This is a *learning-throttle* decision only -
     * unrelated to D-167's own, separate and still-undecided question of whether such a token should also
     * auto-split more aggressively while typing. A false positive here just delays an ordinary word's
     * promotion by a couple more repetitions, not a real loss - traded deliberately for fewer incorrectly
     * glued-together compounds ending up in the learned-words list.
     *
     * @param word the word being considered for learning (any case)
     * @return [COMPOUND_LEARN_THRESHOLD] or the ordinary [LEARN_THRESHOLD]
     */
    private fun learnThresholdFor(word: String): Int {
        return if (hasEmbeddedCapital(word) || provider.looksLikeUnsplitCompound(word)) {
            COMPOUND_LEARN_THRESHOLD
        } else {
            LEARN_THRESHOLD
        }
    }
    
    /** D-202: whether [word] carries a capital letter anywhere past its first character. */
    private fun hasEmbeddedCapital(word: String): Boolean {
        return word.drop(1).any { it.isUpperCase() }
    }
    
    private fun learnWord(word: String?): LearnRecord {
        // Adaptive learning: only learn pure-letter tokens of at least MIN_LEARN_LENGTH (D-247) - updates
        // the n-gram context (tier 1). A single letter is never a real word; the most common source is a
        // fragment left over from an unintended Enter mid-word, not anything meant to be learned.
        if (word.isNullOrEmpty() || word.length < MIN_LEARN_LENGTH || !word.all { it.isLetter() }) {
            return LearnRecord(word ?: "", previousWord, previousPreviousWord, LearnOutcome.SKIPPED)
        }
        // D-37: an already-*learned* word is reinforced immediately; a genuinely new word is only counted
        // up and promoted once it has been committed learnThresholdFor(word) times (D-202: higher for a
        // suspected unsplit compound), so a one-off typo is not eagerly learned as a real word.
        val context = previousWord
        val contextContext = previousPreviousWord
        val outcome = if (dictionaryStore.isBundledWord(word)) {
            // D-186: a word already in the *bundled* dictionary (any casing - isBundledWord's own lookup
            // key is lowercased) must never be written to the learned overlay at all. D-177's original
            // design reinforced it there too ("frequency personalisation"), but every commit of an
            // ordinary word ("die", "du", "immer", ...) hitting this branch flooded the Learned Words
            // editor with plain vocabulary, defeating its purpose as a review list of what was actually
            // *taught*. There is nothing to learn about a word the dictionary already ships with.
            LearnOutcome.SKIPPED
        } else if (provider.isKnownWord(word)) {
            // Not bundled, but already known - i.e. a genuinely previously-learned word - reinforce it.
            dictionaryStore.learn(word, context, contextContext)
            LearnOutcome.LEARNED
        } else if (isPendingBlacklistRecurrence(word)) {
            // D-177: this exact word was provisionally forgotten (G-04 drag-to-trash, or the learned-words
            // editor) and is now trying to get learned again within the pending window - a genuinely
            // recurring mistake, not the one-off typo/test word the provisional mark was betting on.
            // Escalate straight to a real, permanent blacklist entry instead of ever learning it again.
            dictionaryStore.forget(word)
            dictionaryStore.blacklist(word, BlacklistCategory.USER)
            dictionaryStore.clearPendingBlacklist(word)
            LearnOutcome.SKIPPED
        } else if (PendingLearnStore.increment(this, word) >= learnThresholdFor(word)) {
            dictionaryStore.learn(word, context, contextContext)
            PendingLearnStore.clear(this, word)
            LearnOutcome.PROMOTED
        } else {
            LearnOutcome.PENDING
        }
        // D-246: shift the two-word trigram context in lockstep, oldest value first.
        previousPreviousWord = previousWord
        previousWord = word
        val record = LearnRecord(word, context, contextContext, outcome)
        rememberForBackspaceUnlearn(record)
        return record
    }
    
    /**
     * D-177: whether [word] currently carries an unexpired pending-blacklist mark
     * ([DictionaryStore.markPendingBlacklist]), checked before every promotion attempt in [learnWord] so a
     * recurring mistake escalates to a real blacklist entry instead of quietly being learned again. Also
     * clears an expired mark as a side effect (it no longer matters either way), so a stale entry does not
     * linger in the store forever once its window has passed.
     *
     * @param word the word about to be learned
     * @return true when the mark exists and is still within [AdaptSettings.pendingBlacklistExpiryDays]
     */
    private fun isPendingBlacklistRecurrence(word: String): Boolean {
        val markedAt = dictionaryStore.pendingBlacklistedSince(word) ?: return false
        val expiryMillis = settings.pendingBlacklistExpiryDays * DAY_MILLIS
        if (System.currentTimeMillis() - markedAt > expiryMillis) {
            dictionaryStore.clearPendingBlacklist(word)
            return false
        }
        return true
    }
    
    /**
     * D-140: reverses exactly what [learnWord] did for [record] - the un-learn half of A-07's undo.
     * Whatever training an autocorrect/split persisted must be un-learned when the user rejects it via
     * backspace, or the wrong word/pairing keeps getting reinforced every time it is typed and rejected.
     *
     * @param record the outcome to reverse, as returned by the original [learnWord] call
     */
    private fun unlearnWord(record: LearnRecord) {
        when (record.outcome) {
            LearnOutcome.SKIPPED -> {}
            LearnOutcome.LEARNED, LearnOutcome.PROMOTED ->
                dictionaryStore.unlearn(record.word, record.previousWord, record.previousPreviousWord)
            LearnOutcome.PENDING -> PendingLearnStore.decrement(this, record.word)
        }
    }
    
    /**
     * D-248: remembers [record] for [maybeUnlearnOnBackspaceReturn], unless it is a [LearnOutcome.SKIPPED]
     * no-op that changed nothing. Called from both [learnWord] and [learnWordStrong] - the single choke
     * point behind every call site of either (finalizeAndCommit's ordinary word-commit path, an A-05 split's
     * two halves, A-07's own re-learn of a restored word, D-13's authoritative promotion, ...), so none of
     * them need their own bookkeeping. Oldest entry evicted once [RECENT_LEARN_HISTORY_SIZE] is exceeded.
     */
    private fun rememberForBackspaceUnlearn(record: LearnRecord) {
        if (record.outcome == LearnOutcome.SKIPPED) {
            return
        }
        recentLearnRecords.add(record)
        while (recentLearnRecords.size > RECENT_LEARN_HISTORY_SIZE) {
            recentLearnRecords.removeAt(0)
        }
    }
    
    /**
     * D-248: whenever a plain backspace lands with composing empty (checked by the caller, [deleteOneBefore])
     * - i.e. not a correction-undo, which A-07 short-circuits entirely before this is ever reached (see
     * [handleKey]) - checks whether the caret now sits right at the end of one of [recentLearnRecords].
     * [WordExtent.reclaim] finds the exact word-character run now touching the caret, purely from the
     * already-committed text; a plain, case-sensitive match against a still-remembered record's own word is
     * unlearned immediately (count--, exactly mirroring A-07's own [unlearnWord]) and dropped so it cannot
     * fire twice. This is deliberately independent of whatever happened in between - most commonly reached
     * by backspacing back through one or more stray Enters that prematurely committed a half-typed word, but
     * not scoped to that shape specifically: the same "backspaced back into a recently-learned word" signal
     * is just as valid regardless of the intervening keystrokes. Fires at most once per backspace, since a
     * match is only possible for exactly one keystroke - one backspace earlier the trailing run still carries
     * an extra character, one backspace later it is already missing the word's own last letter. A matched
     * [LearnOutcome.PROMOTED] word's "Gelernt: X" chip (W-03), if still showing, is dropped along with it via
     * the ordinary [showNextWordPredictions] refresh.
     */
    private fun maybeUnlearnOnBackspaceReturn(ic: InputConnection) {
        if (recentLearnRecords.isEmpty()) {
            return
        }
        val before = ic.getTextBeforeCursor(MAX_CONTEXT_LOOKBACK, 0) ?: return
        val wordAtCaret = WordExtent.reclaim(before, "").before
        if (wordAtCaret.isEmpty()) {
            return
        }
        val record = recentLearnRecords.lastOrNull { it.word == wordAtCaret } ?: return
        unlearnWord(record)
        recentLearnRecords.remove(record)
        showNextWordPredictions()
    }
    
    /**
     * Learns [word] authoritatively (D-13): a deliberate user correction (undoing a wrong split) promotes
     * the word to the dictionary immediately, bypassing the D-37 count-up threshold.
     *
     * @param word the word to promote
     */
    private fun learnWordStrong(word: String?) {
        if (word.isNullOrEmpty() || word.length < MIN_LEARN_LENGTH || !word.all { it.isLetter() } ||
            dictionaryStore.isBundledWord(word)
        ) {
            return
        }
        val context = previousWord
        val contextContext = previousPreviousWord
        dictionaryStore.learn(word, context, contextContext)
        PendingLearnStore.clear(this, word)
        previousPreviousWord = previousWord
        previousWord = word
        rememberForBackspaceUnlearn(LearnRecord(word, context, contextContext, LearnOutcome.LEARNED))
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
                // D-144 / D-183: applying a suggestion mid-text (the composing token sits before real,
                // already-typed text) must not add a *second* space when one is already there right after
                // it - checked against the document's current state, before either branch below replaces
                // the composing span, since the real text immediately following it is untouched by that
                // replace. D-183: a D-62 mid-word reclaim leaves the real cursor *inside* the composing
                // token (composingCursor < composing.length), not at its end - the character right after
                // the cursor is then still one of the token's own remaining characters (about to be
                // replaced by commitText() below), not the real document text that follows the whole
                // token. Skip past those remaining characters first, so the check lands on the same real
                // position regardless of where inside the token the reclaim happened to be tapped. D-201:
                // hoisted above the D-122 branch split below - both branches need the identical check, and
                // it depends only on composing/composingCursor/the document, never on item.word.
                val remainingComposingChars = composing.length - composingCursor
                val alreadySpaced = ic.getTextAfterCursor(remainingComposingChars + 1, 0)
                    ?.getOrNull(remainingComposingChars)?.isWhitespace() == true
                val trailingSpace = if (alreadySpaced) "" else " "
                // D-122: a suggestion word containing a space can only be the mid-word connector-split
                // candidate (see midWordConnectorSplitSuggestion) - no other suggestion source in this
                // codebase ever produces a multi-word candidate - so it needs applySplit()'s own per-half
                // capitalisation and undo/learn wiring, not the single-word path below.
                if (item.word.contains(' ')) {
                    applyMidWordSplitSuggestion(ic, item.word, trailingSpace)
                    return
                }
                // D-238: the position-1 A-06 merge-suggestion chip (autocorrect disabled, see
                // refreshSuggestions()) is a single word, unlike the split chip above, so it cannot be told
                // apart from an ordinary dictionary completion by shape - re-derive the merge fresh from
                // pendingMergeChar/previousWord (cheap, deterministic) and only take this branch when it
                // actually reproduces the exact tapped text, mirroring how the split branch above also
                // trusts the tapped text rather than any separately cached state.
                val mergeChar = pendingMergeChar
                if (mergeChar != null) {
                    val merged = tokenRepair.tryMerge(previousWord, mergeChar, composing.toString())
                    if (merged != null && capitalisation.capitalise(merged, contextFor(merged)) == item.word) {
                        pendingMergeChar = null
                        applyMerge(ic, merged, trailingSpace)
                        notifySuggestionAccepted(item.word)
                        armShiftForNextWord(ic)
                        return
                    }
                }
                val word = capitalisation.capitalise(item.word, contextFor(composing.toString()))
                ic.commitText(word + trailingSpace, 1)
                clearComposing()
                learnWord(word)
                // D-88: tapping a bar suggestion is always an accepted suggestion, regardless of whether it
                // happens to match what was typed.
                notifySuggestionAccepted(word)
                // D-43: after accepting a bar word, predict the next one so the flow continues.
                showNextWordPredictions()
                // D-29: arm the trailing space added here so an immediately following punctuation removes
                // it - only when a space was actually added above; a pre-existing one mid-text is real
                // document content, not ours to eat.
                pendingSuggestionSpace = trailingSpace.isNotEmpty()
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
            
            // D-142: commits the tapped credential value exactly as stored - never §6-capitalised, and
            // reinforced in the credential store, never the ordinary dictionary (learnWord). item.word is
            // always the *whole* intended value (e.g. a full "local@domain" completion), but the field may
            // already hold several already-committed fragments of it (commitVerbatimFieldFragment commits
            // each `.`/`@`/`-`/`_` as its own delimiter) plus whatever is still composing - all of that is
            // deleted first, so the tap replaces the value typed so far instead of appending after it.
            SuggestionController.Kind.CREDENTIAL -> {
                ic.finishComposingText()
                clearComposing()
                val existing = ic.getTextBeforeCursor(CREDENTIAL_LOOKBACK, 0)?.toString()
                    ?.takeLastWhile { !it.isWhitespace() }.orEmpty()
                if (existing.isNotEmpty()) {
                    ic.deleteSurroundingText(existing.length, 0)
                }
                ic.commitText(item.word, 1)
                val actualKind = if (item.word.contains('@')) LoginFieldKind.EMAIL else loginFieldKind
                CredentialStore.learn(this, item.word, actualKind)
                credentialCaptured = true
                clearSuggestions()
            }
            
            // D-247: purely informational + a drag target (see SuggestionBarView's own two-zone handling) -
            // a plain tap commits nothing; it just dismisses the confirmation back to ordinary predictions,
            // matching "if you do nothing, it stays learned" (doing nothing includes tapping it away).
            SuggestionController.Kind.LEARNED -> {
                showNextWordPredictions()
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
     * D-157/D-164/D-176: a token blacklisted (A-04) in the *currently active* language's own store waives
     * this shield - originally a short hardcoded exception list ([CROSS_LANGUAGE_CONFUSABLES], now
     * removed), redesigned per direct instruction into a real, data-driven `BlacklistCategory.BUNDLED`
     * entry in the active dictionary itself: "due"/"sue" are blacklisted in German so they lose to
     * "die"/"sie" while German is active, without touching their own standing as real, fully protected
     * English words the moment English actually becomes the active language for a token - `dictionaryStore`
     * at this point in the call always already reflects [DictChoice.language] (see
     * [selectActiveDictionary]), so the check is inherently scoped to whichever language is active for
     * *this* token, not hardcoded to German - any bundled language could carry its own such entries the
     * same way. Listing a word here does not touch its cross-language *suggestions* or its own-language
     * A-01 protection - only this specific "never autocorrect it away" shield. "ddr" is a different,
     * simpler case entirely - a real German dictionary word that is blacklisted for the ordinary A-01 gate
     * in [DictionarySuggestionProvider.isKnownWord] directly, not through this cross-language path at all;
     * mentioned here only because it is seeded alongside "due"/"sue" in the same place
     * ([installStores]'s `seedBundledBlacklist`).
     *
     * D-218: [activeLang] is an explicit parameter (defaulting to the live [activeLanguage] field for every
     * existing call site) rather than always reading [activeLanguage] directly, so [pendingCorrectionCandidate]
     * can pass in a value already snapshotted on the main thread before dispatching to the background
     * executor - never racing a later keystroke's own reassignment of that field.
     *
     * @param token the composing token (any case)
     * @param activeLang the language to treat as "active" for this check
     * @return true when any non-active language's dictionary already knows this exact word
     */
    private fun knownInOtherLanguage(token: String, activeLang: Language = activeLanguage): Boolean {
        if (dictionaryStore.isBlacklisted(token)) {
            return false
        }
        return providers.any { (language, otherProvider) -> language != activeLang && otherProvider.isKnownWord(token) }
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
        // D-163: an email address is conventionally lower-case throughout - auto-capitalising the first
        // word (field entry) or the word after every `.`/`-`/`_` segment (every commit) would fight the
        // user constantly. This runs after every commit as well as on field entry, so the one guard here
        // covers the whole field's lifetime, not only its very first word. A manually engaged Caps Lock
        // (D-15) is the user's own explicit choice and is untouched - only the automatic arm is suppressed.
        if (loginFieldKind == LoginFieldKind.EMAIL) {
            keyboardView?.shifted = false
            shiftGuardedArm = false
            shiftArmTime = SystemClock.uptimeMillis()
            fieldMandateOverridden = false
            return
        }
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
        undoLearnRecords = emptyList()
        undoRawCorrection = null
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
    
    /**
     * D-143: whether [info] is a recognised URL-entry field (`TYPE_TEXT_VARIATION_URI`) - drives the
     * letters surface's URL-mode bottom row instead of a separate surface, since a URL still needs the
     * full alphabet for its domain/path (see [urlMode]).
     *
     * Compares against the real, unmasked `InputType.TYPE_TEXT_VARIATION_URI` constant, not a hand-rolled
     * literal - see [de.froehlichmedia.adaptkey.credential.LoginFieldDetector]'s own KDoc for the exact
     * class-bit-masking mistake this avoids.
     *
     * @param info the newly focused field's editor info, or null
     * @return true when the field's own declared type is a URI-variation text field
     */
    private fun isUrlField(info: EditorInfo?): Boolean {
        val type = info?.inputType ?: return false
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
            return false
        }
        return (type and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_URI
    }
    
    /**
     * D-158: whether the currently-focused field is a recognised email-address field, mirroring
     * [isUrlField]'s own direct-`InputType` check exactly. The same reliable variation bits
     * `LoginFieldDetector.classify()` already uses for D-142's own EMAIL detection (verified against the
     * real SDK there) - checked directly here too, rather than deriving from `loginFieldKind`, only
     * because that field is not computed until after `setSurface()` runs below and this must be pushed to
     * the keyboard view before it, exactly like [urlMode].
     *
     * @return true when the field's own declared type is an email-address-variation text field
     */
    private fun isEmailField(info: EditorInfo?): Boolean {
        val type = info?.inputType ?: return false
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
            return false
        }
        val variation = type and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
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
        
        // D-160/D-215: how long the composing token must stay unchanged before the deferred, off-main-
        // thread suggestion/preview searches (D-12 fuzzy matching, D-116/D-117/D-131 fallbacks, the S-05/
        // §47 highlight/split preview) are even attempted - not to protect the main thread (D-211/D-213
        // already moved the actual work off it), but because none of it is perceivable while genuinely
        // typing fast (the user's own point, D-215): a chip or highlight colour that flashes in and out
        // between keystrokes cannot be seen or acted on in time, so it is not worth computing at all until
        // there is real evidence of a pause. Longer than the gap between keystrokes of fluent typing, short
        // enough that the bar/preview still fills at any natural pause. A starting value, easy to retune.
        private const val EXPENSIVE_SUGGESTION_DELAY_MS = 200L
        
        // D-161: how long after the keyboard is shown, and D-250: how far apart each repeat check runs -
        // long enough that a normally-delivered onApplyWindowInsets callback has certainly already run
        // before the first check, short enough that a real correction (the rare case) is barely noticeable.
        // Retuned 1000->500ms per direct request - the user's own first keystroke usually lands before a
        // full second elapses, which would make the correction moot by the time it ran.
        private const val WINDOW_INSETS_RECHECK_DELAY_MS = 500L
        
        // D-250: a single one-shot check (§104) did not catch the race reliably enough - retuned to this
        // many repeat checks, WINDOW_INSETS_RECHECK_DELAY_MS apart, on the assumption that the repeated
        // polling costs nothing worth worrying about since each check is already cheap and idempotent.
        private const val WINDOW_INSETS_RECHECK_MAX_ATTEMPTS = 5
        
        // §60: how much of a clipboard *file*'s content to read for the chip's own already-truncated
        // preview - generous relative to ClipboardPreview.MAX_LENGTH (24), but still a small, bounded read
        // regardless of the file's real size.
        private const val CLIPBOARD_FILE_PREVIEW_CHARS = 512
        
        // D-37: how many times a new word must be committed (without being reverted) before it is promoted
        // to the learned dictionary, so a one-off typo is not eagerly learned.
        private const val LEARN_THRESHOLD = 2
        
        // D-202: the same D-37 promotion threshold, but for a word that looksLikeUnsplitCompound() -
        // higher on purpose, so a repeatedly mistyped, incorrectly-glued-together compound (e.g.
        // "dervKinderarzt") is not learned as if it were a genuine single word before the user notices and
        // starts fixing it. A false positive here only delays an ordinary word's promotion by a couple more
        // repetitions - accepted as harmless (see learnThresholdFor's own KDoc).
        private const val COMPOUND_LEARN_THRESHOLD = 4
        
        // D-247: a single letter is never a real word - the most common source is a fragment left behind
        // by an unintended Enter mid-word (autocomplete-triggered send, accidental keypress), not anything
        // meant to be learned. Applies to learnWord()/learnWordStrong() alike.
        private const val MIN_LEARN_LENGTH = 2
        
        // D-248: how many of the most recent learnWord()/learnWordStrong() outcomes maybeUnlearnOnBackspaceReturn()
        // can still reach - a small, cheap bound, not tuned against any particular repro; five comfortably
        // covers "typed a fragment, hit Enter by accident a few times while reaching for Backspace instead".
        private const val RECENT_LEARN_HISTORY_SIZE = 5
        
        // D-177: converts AdaptSettings.pendingBlacklistExpiryDays (whole days) to milliseconds.
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        
        // D-29: sentence / clause punctuation that absorbs an accepted suggestion's trailing space.
        private const val SPACE_EATING_PUNCTUATION = ".,!?;:)"
        
        // D-176/D-181: seeded once per installStores() call into the German store - see
        // knownInOtherLanguage()'s own KDoc and installStores()'s seedBundledBlacklist() for the full
        // reasoning. "aks" (D-172): a genuine bundled English dictionary entry ("AKS", a Wikipedia-derived
        // acronym, freq 18, PROPER_NOUN) was tripping knownInOtherLanguage()'s cross-language shield and
        // blocking "Aks" -> "als" - the identical failure mode as "due"/"sue", fixed the identical way.
        //
        // D-206: pre-1996-spelling-reform relics of otherwise ordinary, high-frequency common words - a
        // curated subset of dict_de.tsv's own ß-containing entries, hand-picked (not a blanket rule) by
        // checking each candidate against the real corpus frequencies: kept only where the modern ss-form
        // is the dominant, living spelling in the very same corpus (e.g. "daß" 868 vs. "dass" 61892) -
        // never a genuinely modern long-vowel ß word that merely has a rarer Swiss-spelling ss-counterpart
        // present too (e.g. "große"/"grosse", "außerdem"/"ausserdem" - those stay untouched, ß is correct
        // and current there). Deliberately excludes proper nouns/surnames/place names sharing the same
        // ß-vs-ss shape (e.g. "Keßler", "Reuß", "Elsaß") - a person's or place's own spelling is not an
        // error to silently correct - and excludes two outright coincidental collisions between different
        // words that the naive ß->ss substitution alone cannot tell apart ("Maße" != "Masse", "Buße" !=
        // "Busse"). Blacklisting (not purging from the dictionary) keeps each word typeable/known - so
        // quoting genuinely old text still works - while it can never surface as its own suggestion again;
        // the existing ß->"ss" fold (Umlaut.fold, unrelated to D-204's own newer host-key fold) already
        // makes each of these a cost-0 match for its modern form, so autocorrect can still silently fix a
        // live typing of one of these to the modern spelling via the existing §44 known-word override.
        private val BUNDLED_GERMAN_BLACKLIST = setOf(
            "due", "sue", "ddr", "aks",
            "daß", "muß", "mußt", "mußte", "müßte", "wußte", "läßt", "laß", "laßt",
            "einfluß", "anschluß", "schluß", "fluß", "prozeß", "kongreß", "rußland",
            "bewußt", "bewußtsein", "bewußtseins", "unbewußten",
            "haß", "gewiß", "kuß", "bißchen", "häßlich"
        )
        
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
        
        // D-142: how far back to read for a login field's own value - generous enough for a realistic
        // email address or username, short enough to stay a cheap read.
        private const val CREDENTIAL_LOOKBACK = 128
        
        // D-142: a single stray character is not a plausible credential value worth saving.
        private const val MIN_CREDENTIAL_LENGTH = 2
        
        // D-191: a generous bound on how many address-book email rows a single background query reads,
        // so even a very large contacts list stays a bounded, one-off cost rather than unbounded per field.
        private const val CONTACT_EMAIL_LIMIT = 2000
    }
}
