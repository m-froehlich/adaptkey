# AdaptKey — Implementation Progress

Living status of the AdaptKey IME implementation, kept next to
[`android-keyboard-spec.md`](android-keyboard-spec.md). Requirement IDs
(`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`) refer to that spec. Update this file
whenever a component lands so it does not have to be restated in every prompt.

## Project / Stack

- Directory: `D:\workspace-ai\adapt-key` (workspace `D:\workspace-ai`).
- Kotlin, `InputMethodService`, package `de.froehlichmedia.adaptkey`.
- minSdk 26, compile/target 35; AGP 8.7.3 / Kotlin 2.0.21; Gradle wrapper 8.11.1;
  `androidx.preference:preference-ktx` 1.2.1.
- Self-drawn `AdaptKeyboardView` (NOT the deprecated `android.inputmethodservice.KeyboardView`)
  because the spec needs raw `ACTION_DOWN` coordinates and free key proportions.

## Build (machine specifics)

- The only system JDK is Corretto 25, which the older Gradle dislikes → builds MUST
  use the Android Studio bundled JBR (JDK 21) as `JAVA_HOME`.
- Android SDK at `D:\Android\Sdk` (in `local.properties`, gitignored).
- Command (Git Bash, from the project dir):
  ```
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME=/d/Android/Sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest
  ```
- A post-write hook normalises blank lines / CRLF and may reformat files after edits
  (do not fight it).

## Current State

- HEAD: real multilingual dictionaries (this session, see Done) on top of the Greek-input / A-03 / L-03 work.
- Unit tests: **286 green** (`:app:testDebugUnitTest`); `:app:assembleDebug` green.
- Architecture rule in force: pure, Android-free logic (recognition / thresholds /
  policy) lives in its own fully unit-tested classes; the Android layers
  (Activity / View / Service / SQLite DAO / SettingsStore IO) stay thin and are
  earmarked for instrumented tests.

## Done

### Layout (L-)
- L-01 base QWERTZ; L-02/L-04 configurable key proportions (`keyboard/KeyProportions`,
  backspace surcharge taken from third-row neighbours, narrower space / wider comma·period);
  L-06 persistent number row (toggle).
- L-05/L-06 long-press secondary symbols ACTIVE: primary char emitted on `ACTION_UP`
  so a long-press past the system timeout can emit the secondary; `KeyboardLayout.longPressSymbol`.

### Touch & learning (T-)
- T-01 `ACTION_DOWN` is the canonical tap point.
- T-03 personal offset model (`touch/OffsetModel`): per-key 2D Welford Gaussian of
  `touch − centre` deviations, warm-up → geometry fallback, offset capped to key size;
  `cappedMeanOffset`; persisted via `touch/OffsetStore` (SharedPreferences JSON).
  Also tracks a per-key mean contact area (`MotionEvent.getSize()`, only when > 0).
- T-04 typing-pattern detection (info-only, no functional effect): pure
  `touch/TypingPattern` enum + `touch/TypingPatternClassifier` (over normalised
  `KeySample`s: lateral bias / left-right asymmetry → index finger; low lateral +
  large contact area + vertically centred → thumb; needs area evidence, else UNKNOWN;
  < 40 taps → UNKNOWN) + `touch/TypingPatternAnalysis` (OffsetModel + geometry → samples).
  Service re-derives and persists the detected pattern after each model save; shown in
  Settings with a manual override (override stored, no consumer yet since info-only).
- T-05 space/letter ambiguity bands (`touch/AmbiguityBands`): lower edge band of
  c/v/b/n/m → SPACE_AMBIGUOUS; upper edge band of space → LETTER_AMBIGUOUS with nearest
  letter inferred; band depth scales with C-01 and is refined by the offset model.

### Suggestions (S-)
- S-01..S-06 suggestion bar: `suggestion/SuggestionController` (pure policy: sort+cap,
  drop verbatim-of-input, position stabilisation, re-sort on pause, pinned verbatim chip),
  `suggestion/SuggestionConfig` (C-02/C-03/C-04, validated), `SuggestionProvider` interface,
  `suggestion/SuggestionBarView` (scrollable chips via `onCreateCandidatesView`).

### Dictionary & autocorrect (A-, B-)
- Dictionary infrastructure: `dictionary/DictionaryStore` (`InMemoryDictionaryStore` pure +
  `SqliteDictionaryStore`: words / bigrams / blacklist tables, umlaut-safe lowercase key,
  survives updates A-04). `dictionary/DictionarySuggestionProvider` = tier-1 (prefix
  completion ranked by unigram freq + bigram bonus, A-01 known-words-never-autocorrected,
  A-04 blacklist filtering, single-edit autocorrect via `suggestion/EditDistance`).
- Autocorrect applied on delimiter; A-07 post-commit undo (one-keystroke armed backspace).
- Capitalisation §6 (`capitalisation/CapitalisationEngine`): explicit input wins,
  editor `CapsMode`, sentence start, proper/pure nouns, ambiguous nouns left as-is,
  B-02 post-hyphen lowercasing. B-01 hyphen as segment separator.
- A-05/A-06 retroactive split/merge (`dictionary/TokenRepair` over DictionaryStore):
  A-05 split a flagged char or insert a missed space (each half a known word, A-01 protected);
  A-06 prepend an inferred letter and merge when the candidate is known or the bigram is strong.

### Capitalisation shift behaviour (C-07, G-05)
- C-07 shift-grace (`capitalisation/ShiftGrace`): auto-arms Shift at word start for field
  mandates, guards a "surprising" mid-sentence arm against an immediate disarming Shift press
  for `shiftGraceWindowMs`; a press accepted after the window neutralises the field mandate
  for that token (linguistic noun/proper/sentence rules still apply).
- G-05 retroactive capitalisation at word end (`capitalisation/WordEndShift`): first-char
  case flip + next-key resolution table; a kept toggle / camelCase locks the token so the
  commit bypasses autocorrect + §6 + token-repair.

### Gestures (G-)
- G-01..G-05 complete (`gesture/` package). Swipe G-01/G-02/G-03 (`SwipeGesture`,
  `KeyGesture`, `WordBoundary`): G-02 delete whole word, G-03 dismiss keyboard,
  G-01 language switch recognised + consumed but a documented no-op stub (needs A-03 /
  a 2nd-language dictionary). G-04 drag-to-trash (`DragToTrash`): upward drag on a
  suggestion chip blacklists the word (USER). G-05 see above.

### Settings (C-)
- Settings UI C-01..C-09 (`settings/` package): `AdaptSettings` / `RawSettings` /
  `SettingsMapper` (pure, clamps to §10 ranges before validation), `SettingsStore`
  (reads the default SharedPreferences the androidx.preference screen writes),
  `SettingsActivity` (`PreferenceFragmentCompat` over `res/xml/settings_preferences.xml`).
  C-05 blacklist editor (`BlacklistActivity`), C-08 per-key secondary-symbol editor
  (`settings/LetterHints` pure + `LetterHintsActivity`), C-06 shown disabled (no LLM tier).
  T-04 "Tippmuster" category: read-only detected pattern + manual override.
- `AdaptKeyService` wires everything live; applies settings on input view start and via a
  `OnSharedPreferenceChangeListener`.

### Calibration / onboarding (K-)
- K-01 calibration (skippable): `CalibrationActivity` (settings package) hosts an embedded
  `AdaptKeyboardView` (number row off) whose `onKey` drives a pure `touch/CalibrationSession`
  (sentence index + typed buffer + `advance`) over `touch/CalibrationSentences` (3 ASCII-only German
  pangrams covering a–z; no autocorrect / no suggestion bar). The view's offset model is a dedicated
  `OffsetModel(warmupSamples = Long.MAX_VALUE)` so resolution stays pure-geometry for the whole
  session (each tap trains the physically-hit key). On finish the calibration model is **merged**
  (not replaced) into the persisted one via new pure `OffsetModel.merge` (parallel/Chan Welford
  combine of the per-key sufficient stats; contact-area mean = count-weighted), so a repeat from
  Settings adds to learned data; then T-04 is re-derived + persisted and a pattern-specific feedback
  dialog (§2.1) is shown. Reachable from a "Kalibrierung (K-01)" settings entry (repeatable) and a
  one-time first-launch offer dialog (`k01_calibration_offered` flag in default prefs). Service now
  reloads the offset model in `onStartInputView` (when `!restarting`) so a calibration done while the
  service was resident is adopted (storage is current there — saved on the prior `onFinishInput`).

### Real multilingual dictionaries (A-03 fully realised: DE / EN / EL)
- Replaces the tiny `SeedData` placeholder with **real, large Wikipedia-derived lexicons** for German,
  English and Greek, and completes A-03: instead of merely *suppressing* German autocorrect on foreign
  text, the keyboard now **switches the whole lexicon per language** — German default, English
  auto-detected, Greek in the G-01 Greek mode — so each language gets its own suggestions, autocorrect
  and capitalisation.
- **Data / licence (user decision):** built from **Wikipedia** (DE/EN/EL `pages-articles` dumps),
  licence **CC-BY-SA 4.0**. The user is putting the whole app under GPL and confirmed CC-BY-SA is fine
  because the ShareAlike obligation lands only on the *data files* (which ship in the repo + APK), not
  the code; a root `CREDITS.md` records the attribution. (Chosen over CC-BY Tatoeba, which had too
  little Greek; Leipzig Corpora — ideal CC-BY — was unreachable from this environment.)
- **Builder** (throwaway dev tool, not shipped): `scratchpad/build_dict.py` streams each bz2 dump,
  decompresses incrementally and STOPS after ~500 MB decompressed (so only ~150 MB is actually
  downloaded per language, not the 0.6–25 GB full dumps), crudely strips wikitext, tokenises with
  sentence-initial tracking, and emits per language `dict_<code>.tsv` (`word\tfreq\tPOS`, top 120k) +
  `bigram_<code>.tsv` (`prev\tword\tcount`, top 80k). Assets total ~12 MB; APK ~11 MB.
- **POS from casing (no external POS lexicon):** part-of-speech is derived from *casing statistics in
  running (non-sentence-initial) text* — German: a word mostly capitalised mid-sentence → `NOUN`, mixed
  → ambiguous `{NOUN, OTHER}`; English/Greek: almost-always-capitalised → `PROPER_NOUN`, else `OTHER`.
  This drives §6 exactly right per language (German capitalises common nouns, English/Greek only proper
  nouns + sentence start) with zero extra data. Canonical surface case = most frequent non-initial form
  (so `Haus`/`Berlin` come out capitalised, `das` lower-case). Verified: DE top words + `Jahr/Stadt/
  Deutschland` as NOUN; EN `United/American/British` as PROPER_NOUN; EL function words + `Ελλάδα/Αθήνα`.
- **Kotlin — minimal-interface approach:** the `DictionaryStore` interface is UNCHANGED; instead there
  are **three single-language `SqliteDictionaryStore`s** (new optional `databaseName` ctor param →
  `adaptkey_dictionary_<code>.db`) plus a per-language provider + capitalisation engine. Pure, tested
  `dictionary/DictionaryAssetParser` (word/bigram TSV → `WordEntry`/`Bigram`, `\r`-tolerant, skips
  junk). `SqliteDictionaryStore.bulkImport(words, bigrams)` does the first-run seed in one transaction.
  Android-only `dictionary/DictionaryLoader` builds the three stores and seeds each from its asset when
  empty (German falls back to `SeedData` if its asset is missing).
- **Service routing:** `stores`/`providers`/`engines` maps keyed by `Language`; the active
  `provider`/`capitalisation`/`dictionaryStore` fields are re-pointed per token by
  `selectActiveDictionary(context)` → `resolveDict`: Greek mode → EL; else `isForeign` false → DE;
  confidently English → EN; confidently other-foreign (e.g. French, no lexicon) → DE store but
  autocorrect suppressed + no suggestions. Applied in `finalizeAndCommit` and `refreshSuggestions`
  (the old `germanAutocorrectSuppressed` gate is gone). `LANGUAGE_WINDOW = 5` trailing words.
- **First-run ANR avoided:** importing ~0.5M rows into SQLite on the IMS main thread would ANR, so
  `onCreate` installs instant empty `InMemoryDictionaryStore`s and `loadDictionariesAsync()` loads the
  real SQLite stores on a background thread, swapping them in on the main thread via `installStores`.
  Until the (first-run-only) import finishes there are simply no suggestions — graceful.
- 286 unit tests (was 279; +7 `DictionaryAssetParserTest`). The Greek-input package's earlier
  "Greek committed raw / no Greek dictionary" note is now SUPERSEDED — Greek has a real lexicon and its
  own suggestions/autocorrect. `:app:assembleDebug` green; all six dict assets packaged.

### Emoji / symbol panel (L-03)
- The bottom-row combined key (`KeyCode.SYMBOL`, label 😊, corner hint "123") now does two things:
  tap opens the emoji panel; long-press **or** an upward swipe switches to the numeric/symbol layer.
  Downward swipe still dismisses the keyboard from anywhere (G-03) — no collision, since it's the
  opposite `SwipeDirection` in the same `KeyGesture.resolve` dispatch.
- Pure layer/panel state: `keyboard/InputSurface` (LETTERS/SYMBOLS/EMOJI) + `keyboard/PanelNavigation`
  (tap-transition table + long-press/swipe-up → SYMBOLS, both unit-tested).
- Pure `keyboard/SymbolLayout`: 2-page `?123` keymap mirroring the letter layout's row shape/weights
  (`KeyProportions` reused unchanged) — digits row + 10 symbols + page-toggle (`KeyCode.SYMBOL_PAGE`,
  `SymbolLayout.togglePage`) + backspace + `KeyCode.LETTERS` ("ABC", returns to letters) + comma/space/
  period/enter. `AdaptKeyboardView` gained `surface`/`symbolPage` vars that pick `KeyboardLayout.rows`
  vs. `SymbolLayout.rows` in `rebuildRows()`.
- `AdaptKeyboardView.OnLongPressListener` signature changed from `(symbol: String)` to `(key: Key)` so
  the service can dispatch by `key.code` (CHAR → commit `key.hint` as before; SYMBOL → switch layer).
  New `KeyboardLayout.hasLongPressAction(key)` (CHAR-with-hint, or SYMBOL) replaces the old
  hint-presence check for scheduling; `longPressSymbol` is untouched (still CHAR-only, still used by
  the service for the actual text to commit).
- Emoji dataset: **asset-based** (user chose the larger option over a hardcoded short list) —
  `app/src/main/assets/emoji_dataset.tsv`, 507 curated well-established single-codepoint emoji across
  7 categories (`emoji/EmojiCategory`), one `<CATEGORY>\t<emoji>` line each. Pure
  `emoji/EmojiDatasetParser` (Android-free, unit-tested: unknown categories / blank / malformed lines
  are skipped, never crash) + `emoji/EmojiDataset` (`Map<EmojiCategory, List<String>>`) are parsed from
  raw text handed in by the Android-only `emoji/EmojiDatasetLoader` (`context.assets.open`, falls back
  to `EmojiDataset.EMPTY` on any failure). Validated at authoring time with a PowerShell pass
  (`StringInfo.GetTextElementEnumerator` per entry = exactly one grapheme cluster; no cross-category
  dupes) since there is no way to visually render emoji in this environment.
- MRU recents: pure `emoji/RecentEmojis.recordUse(current, emoji, maxSize=30)` (move-to-front, dedup,
  cap; unit-tested), persisted Android-side by `emoji/RecentEmojiStore` (JSON array in
  SharedPreferences, mirrors the existing `OffsetStore` pattern).
- `emoji/EmojiPanelView` (Android-only, thin): `LinearLayout` with a `HorizontalScrollView` tab bar
  (back-to-letters ⌨, recent 🕐, then one tab per category using `EmojiCategory.icon`) over a
  `ScrollView`-wrapped `GridLayout` (7 columns) of emoji cells; selecting an emoji fires
  `OnEmojiSelectedListener`, the tab bar's first button fires `OnBackListener`.
- Service wiring: `onCreateInputView` now returns a `FrameLayout` holding both the `AdaptKeyboardView`
  and the `EmojiPanelView` (panel starts `GONE`); `setSurface(next)` toggles visibility of both, pushes
  `surface`/`symbolPage` into the keyboard view, and resets `symbolPage` to 1 whenever leaving SYMBOLS.
  `commitEmoji(emoji)`: per spec, first `finalizeAndCommit(ic, "")` (finalises any in-progress composing
  token exactly like a delimiter, with an empty delimiter), **then** `ic.commitText(emoji, 1)`, then
  records the MRU use and pushes the updated list back into the panel. `onStartInput` resets
  `surface` to LETTERS for every new field. `CalibrationActivity`'s key switch got a no-op arm for the
  three new `KeyCode`s (irrelevant to calibration).
- 243 unit tests total (was 217; +26: `PanelNavigationTest`, `SymbolLayoutTest`,
  `EmojiDatasetParserTest`, `RecentEmojisTest`, plus additions to `KeyboardLayoutTest`/`KeyGestureTest`).
  `:app:assembleDebug` green, asset confirmed packaged into `app-debug.apk`.

### Language detection (A-03)
- **NOT the spec's literal fastText/ONNX.** After discussing it with the user we deliberately went with
  a **pure-Kotlin character-trigram classifier** instead of a `.ftz` binary + ONNX Runtime. Reasons:
  (1) runs fully offline just like fastText would (privacy identical — the app never needs internet),
  (2) no native dependency / no APK bloat, (3) the whole thing is JVM-unit-testable in the project's
  established style, and (4) the profiles are **transparent, inspectable data derived from open,
  non-Facebook corpora** (the user explicitly distrusts Facebook, even OSS). Same interface, so a real
  model could replace it later. **Honesty caveat recorded in code:** the *plumbing* is unit-tested; the
  *model accuracy* is validated by an evaluation suite over a held-out corpus (a real eval, honestly
  labelled — NOT "unit tests prove the model").
- Languages: **DE, EN, EL (Greek)** + Latin neighbours **FR, ES, IT, NL, PT** (the user writes DE+EN,
  occasional Greek, wife writes Greek often → Greek matters). Greek is decided by **script** (disjoint
  Unicode block) via `language/ScriptDetector.greekFraction` — trivial and bulletproof, no n-gram needed.
- Corpus/profiles: built from the **UDHR** (public domain) via a scratchpad Python builder
  (`scratchpad/build_profiles.py`) that downloads the eight UDHR texts from the `eric-muller/udhr` GitHub
  mirror, does a deterministic 80/20 train/eval split per language, and emits Cavnar-Trenkle profiles
  (top-200 bi+trigrams, rank order) to `app/src/main/assets/language_profiles.tsv` (`<code>\t<ngram>`,
  1600 lines) plus a held-out eval corpus to `app/src/test/resources/language_eval.tsv` (116 sentences).
  The builder is a throwaway dev tool, not shipped; the asset + eval corpus are committed.
- Pure package `language/` (all unit-tested): `Language` enum; `CharNgrams` (normalize + bi/trigram
  counts + ranked profile — **normalization is byte-for-byte identical to the Python builder**, the one
  correctness-critical parity point); `CharNgramProfile` (ngram→rank map); `LanguageProfileParser`
  (`\t`-split, trailing-space-in-ngram significant, `\r`-tolerant, unknown codes skipped); `ScriptDetector`;
  `LanguageClassifier` (Greek script fast-path → out-of-place distance over profiles → argmin; `classify`,
  `classifyRecent(wordWindow)`, and the guard `isForeign`). Android-only `LanguageProfileLoader` (asset →
  parser → classifier; empty-profiles fallback = every result UNKNOWN = guard is a safe no-op).
- **Guard design gotcha (important):** `isForeign` does NOT use the general top-2 confidence — for
  closely-related languages (PT vs ES/IT) that gap is tiny even when German is nowhere close, so a
  confidence gate flagged only ~58% of non-German. Instead `isForeign` measures **German's own margin**:
  fire only when some language wins *and* German's out-of-place distance is ≥ `germanMargin` (0.15)
  worse than the winner's. This is the right question ("is this German?") and is conservative — UNKNOWN
  / borderline → not foreign, so German autocorrect stays on by default and is never wrongly disabled.
- Evaluation (`LanguageDetectionEvaluationTest`, honestly labelled as a **same-domain** UDHR held-out
  split, not open-domain proof): overall argmin accuracy asserted ≥ 0.90 (actual well above), Greek
  100% via the script path, German flagged foreign ≤ 1/15, and ≥ 0.85 of non-German sentences flagged.
- Service wiring: `languageClassifier` loaded in `onCreate` (defaults to the empty/no-op classifier).
  `captureTokenContext` now also stashes `tokenContextBefore`; new `germanAutocorrectSuppressed(typed)` =
  `classifier.isForeign("$tokenContextBefore $typed")`. Applied in **two** spots: `finalizeAndCommit`
  (commit `typed` verbatim instead of the German autocorrect) and `refreshSuggestions` (drop the pending
  autocorrect chip). §6 capitalisation is left as-is — it is driven by German-dictionary POS tags, so a
  foreign word (absent from the dict) is not force-capitalised anyway; noted as an accepted limitation.
- **Scope line the user agreed to:** this session is *detection + German-autocorrect guard only*. Full
  **Greek input** (a Greek layout + Greek dictionary + the real G-01 language switch) is a deliberate
  separate next package; G-01's swipe remains the documented no-op stub for now.
- 270 unit tests total (was 243; +27 across `CharNgramsTest`, `ScriptDetectorTest`,
  `LanguageProfileParserTest`, `LanguageClassifierTest`, `LanguageDetectionEvaluationTest`).
  `:app:assembleDebug` green; `language_profiles.tsv` confirmed packaged into `app-debug.apk`.

### Greek input (G-01 real language switch)
- The G-01 space-bar swipe (left **or** right — both toggle, with only two languages) now really
  switches the input alphabet between **German (Latin QWERTZ)** and **Greek**, replacing the old
  documented no-op stub. A short toast (`Ελληνικά` / `Deutsch`) confirms the switch. `AdaptKeyService`
  holds `activeLanguage` (default German, kept for the service lifetime, re-synced onto the view in
  `onStartInputView`); `toggleLanguage(ic)` finalises any in-progress token in the *current* language
  first, flips `activeLanguage`, and sets `keyboardView.greek`.
- Pure `keyboard/GreekLayout` (unit-tested): the standard modern-Greek touch rows
  (`ςερτυθιοπ` / `ασδφγηξκλ` / `ζχψωβνμ`), with the number row and the whole control/bottom row
  (shift, combined emoji-?123 key, comma/space/period/enter) mirroring `KeyboardLayout` exactly and
  reusing the same `KeyProportions`. So switching alphabets only swaps the letters; emoji, ?123 and all
  gestures work identically in Greek.
- **Accents (tonos):** stressed vowels carry their accented form as the `Key.hint` long-press secondary
  (α→ά, ε→έ, η→ή, ι→ί, ο→ό, υ→ύ, ω→ώ; `GreekLayout.ACCENTS`). Because a hint that is itself a **letter**
  must extend the word rather than delimit it, `handleLongPress` now checks `symbol.all { isLetter() }`:
  letter secondaries go through the new `appendLongPressLetter` (appends into the composing token,
  honours Shift for the upper-case accented form, `TapAmbiguity.NONE` flag), while the existing
  non-letter secondaries (@, €, !, /, …) still commit as a delimiter as before. Diaeresis forms (ϊ, ϋ)
  are deliberately omitted for now — one long-press slot per key, tonos is far more common.
- **Greek was committed raw in that package — NOW SUPERSEDED** by the real multilingual dictionaries
  section above: Greek has its own Wikipedia lexicon, so Greek mode gives Greek suggestions/autocorrect,
  and the `germanAutocorrectSuppressed` gate was replaced by per-language `selectActiveDictionary`.
- The `AdaptKeyboardView` gained a `greek` toggle that picks `GreekLayout.rows` vs `KeyboardLayout.rows`
  in `rebuildRows()` (same shape as the L-03 `surface` switch). `KeyboardLayout.hasLongPressAction` /
  `longPressSymbol` are generic over `Key`, so they work on Greek keys unchanged.
- 279 unit tests (was 270; +9 `GreekLayoutTest`). `:app:assembleDebug` green. Greek keyboard/accent/
  switch View+Service glue is Android-only → instrumented-test backlog.

## Remaining (per spec §11)

- **Mini-LLM tier-3** follow-on (C-06).
- Optional: a real fastText/ONNX model behind the same `LanguageClassifier` interface, if ever wanted.
- Nice-to-haves: persist `activeLanguage` across service restarts; Greek diaeresis (ϊ/ϋ) input; a C-05
  blacklist editor that is language-aware (currently operates on the active store); verify/tune the
  first-run dictionary import time on a real device.

## Testing gaps

- Android-only layers (Activity / View / Service / `SqliteDictionaryStore` DAO /
  `SettingsStore` IO) still lack instrumented tests. Notably untested glue:
  the T-05/A-05/A-06 service wiring (flag retention, merge/split `InputConnection` surgery),
  the swipe/drag/word-end-shift View+Service glue, the T-04 View/Service glue
  (`event.size` capture, `charKeyGeometry`, `persistTypingPattern`), and the K-01 `CalibrationActivity`
  glue (key→session driving, merge-on-finish, feedback dialog) + the service offset-model reload.
  L-03 adds: `EmojiPanelView` (tab selection, grid population, back/emoji click wiring),
  `EmojiDatasetLoader` (asset read/fallback), `RecentEmojiStore` (JSON persistence) and the service's
  container/`setSurface` visibility toggling. A-03 adds: `LanguageProfileLoader` (asset read) and the
  service's per-language `selectActiveDictionary` routing (the pure classifier itself is fully
  JVM-tested + evaluated). Real-dictionaries adds: `DictionaryLoader` (3-store build + first-run asset
  import), `SqliteDictionaryStore.bulkImport` (transaction), and the async `loadDictionariesAsync` /
  `installStores` swap (background import → main-thread install); the pure `DictionaryAssetParser` is
  unit-tested.

## Notes / gotchas

- JUnit 5 (Jupiter). For `assertThrows`, import `org.junit.jupiter.api.Assertions.assertThrows`
  and use the `(Class, executable)` form, or Kotlin picks the reified overload and fails to compile.
- Contact area comes from `MotionEvent.getSize()` (Float); Float→Double rounding means
  contact-area test tolerances are ~1e-6, not 1e-9.
- Kotlin style: same-line braces, no spaces inside parentheses, 4-space indent,
  stdlib `require()` / `use {}` (not INFOLOG Commons), KDoc on non-trivial public/protected API.
- Minimal diff; do not reformat unrelated code; no unused imports / Kotlin warnings.
- A-03 parity gotcha: `language/CharNgrams.normalize` MUST stay byte-for-byte identical to the Python
  profile builder (`scratchpad/build_profiles.py`), else runtime n-grams won't line up with the stored
  profiles and accuracy collapses. The evaluation test doubles as a cross-check of that parity.
- Python on this machine: `/c/Program Files/Python314/python` (the `python3`/`python` aliases are the
  broken Windows-Store stubs). Git-Bash path conversion mangles `/c/...` args unless you leave it on —
  don't set `MSYS_NO_PATHCONV=1`, it made Python prepend the wrong drive.
