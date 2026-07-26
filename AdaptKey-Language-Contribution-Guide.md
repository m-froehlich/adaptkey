# AdaptKey - Adding a New Language

This guide is for a community member (or an AI helping one) who wants AdaptKey to type well in a language
it does not yet support well. It is the concrete "how" behind `AdaptKey-Spec.md`'s multi-language
architecture (D-280) - read this file end to end before touching any code; it names exactly which files
need to change and why, in the order that actually matters.

If you only remember one thing: **run the two checklists below** (§1 for "my language is already in the
`Language` enum", §2 for "it isn't yet") before writing a single line of Kotlin.

---

## 0. The two things a language needs, and why they are separate

AdaptKey ships **only English** inside the APK. Every other language - German and Greek included - is a
**dictionary you install after the fact** (a small browser download + a system file picker, exactly like
the optional mini-LLM model; the app never gets internet permission, so this is the only way to add data
without breaking that guarantee). A **keyboard layout**, by contrast, is compiled Kotlin code - it cannot be
downloaded, so it always needs a real code change and a real app release.

This split matters for how much work adding your language actually is:

- If your language is written in the **Latin alphabet** (French, Spanish, Italian, Dutch, Portuguese, ... -
  anything using the ordinary 26 letters, with or without accents), **it is already fully typeable today** -
  `de.froehlichmedia.adaptkey.keyboard.LayoutRegistry` falls back to the ordinary QWERTY `KeyboardLayout` for
  any language it does not special-case, so it needs no new layout code at all. Building and hosting the
  dictionary (§3) plus one catalog entry (§4) is the *entire* job.
- If your language uses a **different alphabet** (Cyrillic, Arabic, Han, ...), you additionally need a new
  compiled layout, mirroring `GreekLayout.kt` - see §5. This is real work and needs an app release; skip
  straight to §3 if this does not apply to you.

---

## 1. Checklist: your language is already in the `Language` enum

Check `app/src/main/kotlin/de/froehlichmedia/adaptkey/language/Language.kt` first. As of D-280 it already
lists `GERMAN`, `ENGLISH`, `GREEK`, `FRENCH`, `SPANISH`, `ITALIAN`, `DUTCH`, `PORTUGUESE` (plus `UNKNOWN`).
If yours is already there:

1. Confirm it is Latin-script (§0) - if so, you need **no Kotlin code change at all**. Skip to §3.
2. Check `app/src/main/assets/language_profiles.tsv` - as of D-280 it already has character-trigram profile
   data for every one of the eight languages above (see §6). You likely need no change there either.
3. Build the dictionary (§3), host it, and add one line to `LanguagePackCatalog` (§4). Done.

## 2. Checklist: your language is not in the enum yet

1. Add it to the `Language` enum (`language/Language.kt`): a two-letter `code` (ISO 639-1, matching
   `language_profiles.tsv`'s own convention) and an `endonym` (the language's own name for itself, e.g.
   `"Polski"`). The compiler will now force you to handle it everywhere `Language` is matched exhaustively -
   follow the errors; each one is a real, deliberate checklist entry (see `AdaptKeyService.languageLabel`'s
   own KDoc for why this is deliberate, not an oversight to route around with a wildcard `else`).
2. Latin-script or not - see §0 and either skip to §3 or continue to §5 first.
3. Optionally add profile data (§6) - not required to type, but improves automatic language detection
   (A-03) while a different alphabet is active.

---

## 3. Building the dictionary

Two plain, tab-separated text files, UTF-8, one language:

- **`dict_<code>.tsv`** (unigrams) - one word per line: `word<TAB>frequency<TAB>pos,tags,here`
  - `frequency` is a plain integer (relative rank; real corpus counts work best - see the existing
    `dictionaries/de/dict_de.tsv`/`dictionaries/el/dict_el.tsv` for real examples).
  - The POS column is optional (a line with just `word<TAB>frequency` is valid) and, when present, a
    comma-separated subset of `de.froehlichmedia.adaptkey.dictionary.PartOfSpeech`: `NOUN`, `VERB`,
    `ADJECTIVE`, `PREPOSITION`, `PROPER_NOUN`, `OTHER`. An unrecognised tag is silently dropped, not an
    error - see `DictionaryAssetParser.parseWords`, the authoritative parser.
  - POS tags only actually matter for §6 of `AdaptKey-Spec.md`'s capitalisation rules (a language that
    capitalises nouns the way German does benefits from accurate `NOUN` tags; a language that doesn't can
    tag everything `OTHER` and skip this entirely).
- **`bigram_<code>.tsv`** (optional, but strongly recommended - it drives S-07 next-word prediction and
  A-05's split-scoring signal): `previousWord<TAB>word<TAB>count`. See `DictionaryAssetParser.parseBigrams`.

Put your working files under a new `dictionaries/<code>/` folder at the repo root (mirroring
`dictionaries/de/`, `dictionaries/el/`) so they stay in version control even though they never enter the
APK.

## 4. Packaging and hosting the language pack

`de.froehlichmedia.adaptkey.dictionary.LanguagePackInstaller` expects a plain zip archive with your two
files at its root - not inside a folder - named exactly `dict_<code>.tsv` and (optionally) `bigram_<code>.tsv`.
Build one like the existing `language-packs/adaptkey-lang-de.zip`/`adaptkey-lang-el.zip` (a one-line
`zipfile.ZipFile(...).write(...)` in Python, or any ordinary zip tool - just make sure there is no directory
prefix inside the archive).

Host the resulting `.zip` somewhere stable and public - a GitHub Release asset on this repository is the
recommended place (versioned, immutable once published); a raw file URL on the repo's default branch also
works and is what German/Greek currently use, but will change if the file is ever moved. Either way, add one
entry to `de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog.ENTRIES`:

```kotlin
Entry(Language.YOUR_LANGUAGE, "https://.../adaptkey-lang-xx.zip")
```

That one line is what makes your language appear in `LanguagePacksActivity` (Settings → Languages) and in
the onboarding language-selection step. Nothing else references this list.

## 5. Building a keyboard layout (non-Latin scripts only)

Mirror `app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/GreekLayout.kt`: a plain Kotlin `object`
exposing a `rows(proportions, showNumberRow, urlMode, emailMode, locale): List<List<Key>>` function that
builds your alphabet's three letter rows (number row, third-row shift/backspace, and the bottom row are all
shared with the Latin layout via `KeyboardLayout.urlBottomRow`/`emailBottomRow` - only the letters differ).
Then:

1. Add a new `LayoutKind` entry in `keyboard/LayoutRegistry.kt` and map your `Language` to it in `KINDS`.
2. `AdaptKeyboardView`'s row-selection (`InputSurface.LETTERS -> if (greek) GreekLayout.rows(...) else
   KeyboardLayout.rows(...)`) is, as of D-280, still a plain boolean pair (`greek`/`qwerty`) rather than a
   generic `LayoutKind` switch - because only one non-Latin script existed to generalise against. **If you
   are the second one, generalise this properly** (a `when (LayoutRegistry.kindFor(activeLanguage))` in the
   view, replacing both booleans) rather than adding a third ad-hoc flag - that is the point at which the
   abstraction actually earns its cost.
3. `AdaptKeyService.applyActiveLanguageToView()` is the single place that currently derives `greek`/`qwerty`
   from `activeLanguage` - update it in lockstep with step 2.

## 6. Language-profile data for automatic detection (optional)

`app/src/main/assets/language_profiles.tsv` feeds the on-device character-trigram classifier (A-03) that
lets AdaptKey recognise a switch to English (or back) without the user swiping G-01 every time. It already
has data for `de`, `en`, `el`, `es`, `fr`, `it`, `nl`, `pt` - if your language is one of these, you likely
need no change here at all.

If it is not: the classifier degrades gracefully without a profile (per-token routing simply treats your
language's text as "foreign, unclassified" - `resolveDict()` in `AdaptKeyService` keeps the active language's
dictionary and just holds back autocorrect for that token, never anything worse). Building a new profile
requires reconstructing the trigram-frequency builder that produced the existing rows - **that Python script
is not currently checked into this repository** (a known gap, not a design choice); `language/CharNgrams.kt`
documents the exact normalisation the runtime classifier uses and MUST match byte-for-byte, so treat this as
a from-scratch, careful exercise if you take it on, not a quick copy-paste.

## 7. Testing, versioning, and sending it in

- Any new pure logic (not Android-specific) gets JUnit 5 tests, following this project's existing style -
  see `LanguagePackInstallerTest`/`LanguagePackCatalogTest`/`LanguageCycleTest` for the shape.
- `dictionaries/<code>/` and `language-packs/adaptkey-lang-<code>.zip` are real, committed repo content, not
  build output - include them in your pull request.
- Bump `app/build.gradle.kts`'s `versionName`/`versionCode` (see the comment right above them) and add a new
  `D-`-numbered entry to `AdaptKey-Spec.md`/`AdaptKey-Progress.md`/`AdaptKey-History.md`, the same way every
  other change in this project's history is recorded - `AdaptKey-Progress.md`'s "Current State" section has
  many examples of the expected shape and level of detail.
- Run `:app:assembleRelease :app:testDebugUnitTest` before opening the pull request; both must be green.
