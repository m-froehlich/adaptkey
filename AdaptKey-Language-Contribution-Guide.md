# AdaptKey - Adding a New Language

This guide is for a community member (or an AI helping one) who wants AdaptKey to type well in a language
it does not yet support well. It is the concrete "how" behind `AdaptKey-Spec.md`'s multi-language
architecture (D-280) - read this file end to end before touching any code; it names exactly which files
need to change and why, in the order that actually matters.

If you only remember one thing: **run the two checklists below** (§1 for "my language is already in the
`Language` enum", §2 for "it isn't yet") before writing a single line of Kotlin.

---

## 0. The three things a language needs, and why they are separate

AdaptKey ships **only English** inside the APK. Every other language - German and Greek included - is a
**dictionary you install after the fact** (a small browser download + a system file picker, exactly like
the optional mini-LLM model; the app never gets internet permission, so this is the only way to add data
without breaking that guarantee). A **keyboard row geometry** (which physical key sits where - QWERTY,
QWERTZ, AZERTY, or a genuinely different alphabet), by contrast, is compiled Kotlin code - it cannot be
downloaded, so a *new* geometry always needs a real code change and a real app release. The **AltGr/long-press
symbol set** (D-281 - which secondary character each key offers on a long press, e.g. `ä` on `a`) sits
between the two: it is data, shipped as part of the language pack (or bundled for English), so a language
using an *existing* geometry needs no new layout code at all, only its own hint file.

This split matters for how much work adding your language actually is:

- If your language already fits one of the app's existing row geometries - **QWERTY today covers most
  Latin-script languages that do not have their own strong convention** (Spanish, Italian, Dutch, Portuguese,
  ... - see `LayoutRegistry`) - **it is already fully typeable**, and needs no new layout code at all.
  Building and hosting the dictionary (§3, including your own hint file) plus one catalog entry (§4) is the
  *entire* job.
- Some Latin-script languages have their own strong, expected physical layout convention distinct from
  QWERTY/QWERTZ - **French's AZERTY is the obvious example**, and depending on the target audience a language
  like Turkish may warrant one too. Do not assume QWERTY is good enough for these without checking what
  users of that language actually expect; building a new *geometry* (still Latin letters, different physical
  arrangement) is real work but the same *kind* of work as English/German's existing QWERTY/QWERTZ split -
  see §5.
- If your language uses a **genuinely different alphabet** (Cyrillic, Arabic, Han, ...), you additionally
  need a new compiled layout, mirroring `GreekLayout.kt` - see §5. This is real work and needs an app
  release; skip straight to §3 if this does not apply to you.

---

## 1. Checklist: your language is already in the `Language` enum

Check `app/src/main/kotlin/de/froehlichmedia/adaptkey/language/Language.kt` first. As of D-280 it already
lists `GERMAN`, `ENGLISH`, `GREEK`, `FRENCH`, `SPANISH`, `ITALIAN`, `DUTCH`, `PORTUGUESE` (plus `UNKNOWN`).
Being in the enum already does **not** by itself mean no layout work is needed - see §0's geometry question.
If yours is already there:

1. Decide whether it needs a *new* row geometry (§0/§5) - French's own AZERTY geometry is built as of D-314
   (`AzertyLayout.kt`, wired up in `LayoutRegistry`), but do not generalise from that: French only got there
   because someone actually built it, not because an enum entry implies a matching layout exists - a language
   newly added to the enum still needs its own §0/§5 decision, exactly as before.
2. If an existing geometry (QWERTY today, for most) is good enough, you need **no Kotlin code change at
   all** for the layout itself. Skip to §3.
3. Check `app/src/main/assets/language_profiles.tsv` - as of D-280 it already has character-trigram profile
   data for every one of the eight languages above (see §6). You likely need no change there either.
4. Build the dictionary and your own hint file (§3), host it, and add one line to `LanguagePackCatalog`
   (§4). Done.

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

## 3. Building the dictionary (and your own AltGr hint set)

Up to four plain text files, UTF-8, one language - D-310: every file here uses a fixed, un-suffixed name
(`dict.tsv`, not `dict_<code>.tsv`), since each archive/folder is already scoped to exactly one language by
its own location, not by its filenames:

- **`dict.tsv`** (unigrams, required) - one word per line: `word<TAB>frequency<TAB>pos,tags,here`
  - `frequency` is a plain integer (relative rank; real corpus counts work best - see the existing
    `dictionaries/de/dict.tsv`/`dictionaries/el/dict.tsv` for real examples).
  - The POS column is optional (a line with just `word<TAB>frequency` is valid) and, when present, a
    comma-separated subset of `de.froehlichmedia.adaptkey.dictionary.PartOfSpeech`: `NOUN`, `VERB`,
    `ADJECTIVE`, `PREPOSITION`, `PROPER_NOUN`, `OTHER`. An unrecognised tag is silently dropped, not an
    error - see `DictionaryAssetParser.parseWords`, the authoritative parser.
  - POS tags matter for §6 of `AdaptKey-Spec.md`'s capitalisation rules (a language that capitalises nouns
    the way German does benefits from accurate `NOUN` tags; a language that doesn't can tag everything
    `OTHER`) - **and, since D-306, also for A-05's own "not both nouns" split-safety gate**: an entry with
    no POS tag at all can never be recognised as a noun, so it can never be protected by that gate either,
    regardless of what it actually is. Leaving the column blank is technically valid (silently parsed as "no
    tags") but **discouraged** - it is exactly what let a mistagged low-frequency entry ("til", a rare
    given-name fragment with no tag at all) defeat the split gate on a real device (see
    `AdaptKey-History.md` §D-306). Prefer `OTHER` over leaving the column empty for anything that is not
    itself a noun.
- **`bigram.tsv`** (optional, but strongly recommended - it drives S-07 next-word prediction and
  A-05's split-scoring signal): `previousWord<TAB>word<TAB>count`. See `DictionaryAssetParser.parseBigrams`.
- **`hints.tsv`** (optional, D-281 - the AltGr/long-press secondary symbol on each letter key, L-05):
  a single line in `key=symbol;key=symbol;...` form, e.g. `a=ä;e=€;s=ß` - exactly
  `de.froehlichmedia.adaptkey.settings.LetterHints.encode()`'s own format (that same class's `parse()` reads
  it back; see `dictionaries/de/hints.tsv` for the real German set, or `app/src/main/assets/en/hints.tsv`
  for English's). **Do not just reuse another language's file** - German's own set (`ä`/`ö`/`ü`/`ß` on
  `a`/`o`/`u`/`s`, plus `@`/`€`/`#`/`-`/`+`/`°`/`×`/`÷`/`/`/`*`/`ƒ`/`π` on `q`/`e`/`h`/`m`/`n`/`d`/`x`/`c`/`v`/
  `b`/`f`/`p`) is specifically tuned for German and is very unlikely to be what your language's users expect;
  design your own from what would actually help someone typing your language day to day. Without this file,
  your language falls back to `KeyboardLayout.DEFAULT_LETTER_HINTS` (German's own set) - functional, but not
  tailored to your language, so it is worth providing even though it is the one truly optional file here.
  Note what this file covers and what it does not: one *single* secondary symbol per key (L-05). A key
  offering *several* alternatives in one popup (e.g. `p`'s Greek-letter popup, a digit's shifted-symbol-plus-
  superscript pair) is still built in compiled Kotlin (`KeyboardLayout.kt`/`GreekLayout.kt`), not yet part of
  this data format - not something you need to worry about for an ordinary new language, only relevant if you
  are adding this kind of multi-alternative popup yourself. Whichever key ends up with one, you do **not**
  need to hand-tune its popup's left/right direction: D-282 made that automatic, based on the real popup
  width and the key's actual screen position, not on which key it happens to be - it works the same whether
  the alternatives came from this file or from hand-written Kotlin.
- **`version.txt`** (optional but strongly recommended, D-308): its first line is a single plain integer,
  e.g. `1` - this pack's own version, bumped by *you* every time you publish a revised
  `dict.tsv`/`bigram.tsv`/`hints.tsv` under the same hosted URL. `LanguagePacksActivity`'s Download/Import
  buttons stay available at any time (even for an already-installed language) so a user can always manually
  re-check; re-importing only actually applies the freshly downloaded archive when *its own* version is
  strictly newer than what is already installed - if it is exactly the same, the user is told it is already
  current; if it is *older* (D-386-followup: a stale or wrong file resolved from the download folder), the
  user is told that distinctly instead, so an accidental downgrade is never silently swallowed as "nothing to
  do". Start at `1` for your language's first release and increment by 1 every time you publish a real
  content change. **A missing version file is not neutral - it actively blocks future updates from ever
  being picked up by re-import**: an archive with no version file is always treated as version `1` (same as a
  fresh install with nothing recorded yet), so once a language is installed, every later re-import of a
  version-less archive compares as "already current", no matter how different the actual content is - the
  only way such an update then reaches an existing install is the user fully removing and reinstalling the
  language. If you intend to ever revise your pack after its first release, include this file from the start.

  D-386-followup: a second line, your language's own code (e.g. `de`), is strongly recommended too -
  `LanguagePackInstaller.parse()` cross-checks it against the language being imported and rejects a genuine
  mismatch outright (with a clear "wrong language" message, not a silent wrong import). This matters because
  `LanguagePacksActivity`'s own import flow (D-386) resolves the archive to install purely by matching an
  expected *file name* in the user's downloads folder, not by the user visually confirming the file the way a
  manual picker once implied - this line is what still catches a wrong/mismatched file at the content level.
  Omitting it is tolerated (never rejected on that basis alone), but every pack built for a specific language
  should include it:
  ```
  1
  de
  ```

Put your working files under a new `dictionaries/<code>/` folder at the repo root (mirroring
`dictionaries/de/`, `dictionaries/el/` - both already using these exact fixed names) so they stay in version
control even though they never enter the APK; the same `<code>` folder is what makes fixed filenames
unambiguous both here and inside your `.zip` below.

## 4. Packaging and hosting the language pack

`de.froehlichmedia.adaptkey.dictionary.LanguagePackInstaller` expects a plain zip archive with your files at
its root - not inside a folder - named exactly `dict.tsv`, and optionally `bigram.tsv`/`hints.tsv`/
`version.txt`. Build one like the existing `language-packs/adaptkey-lang-de.zip`/`adaptkey-lang-el.zip` (a
one-line `zipfile.ZipFile(...).write(...)` per file in Python, or any ordinary zip tool - just make sure
there is no directory prefix inside the archive itself; `LanguagePackInstaller.write()` is what creates the
`<code>/` folder on the receiving device, not something your own archive needs to contain).

Host the resulting `.zip` somewhere stable and public - a GitHub Release asset on this repository is the
recommended place (versioned, immutable once published); a raw file URL on the repo's default branch also
works and is what German/Greek currently use, but will change if the file is ever moved. Either way, add one
entry to `de.froehlichmedia.adaptkey.dictionary.LanguagePackCatalog.ENTRIES`:

```kotlin
Entry(Language.YOUR_LANGUAGE, "https://.../adaptkey-lang-xx.zip", version = 1)
```

That `version` here (D-308) is a *separate*, compiled-in copy of the same number you put in your own
`version.txt` - it only drives the lightweight "update available" hint `LanguagePacksActivity` shows
immediately on screen, without anyone downloading anything; the archive's own `version.txt` is what
actually gates whether a re-import applies. The two are expected to drift apart between your own releases
and AdaptKey's own app releases - that is fine, the hint is advisory, the archive's own file is authoritative
- but when you *do* bump your pack's version, bump this catalog line too so the next AdaptKey release
correctly hints at your update, and open a pull request for that one-line change (you cannot publish it
yourself the way you can your own hosted `.zip`).

That one `Entry(...)` line is what makes your language appear in `LanguagePacksActivity` (Settings →
Languages) and in the onboarding language-selection step. Nothing else references this list.

## 5. Building a new row geometry (a new Latin arrangement, or a non-Latin script)

Needed when an existing geometry genuinely is not good enough - a language with its own strong, expected
physical layout convention (French's AZERTY - built in D-314, see below; possibly a dedicated Turkish
arrangement), or a script that is not Latin at all (Greek today; Cyrillic/Arabic/... tomorrow). Do not build
this speculatively - if QWERTY is genuinely fine for your language's users, §0-§4 is the entire job.

Mirror `app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/GreekLayout.kt` (a genuinely different
alphabet) or `AzertyLayout.kt` (a Latin arrangement, D-314 - the concrete reference if your language needs a
new *Latin* geometry, since a within-row letter swap like German's QWERTZ isn't the only shape this can
take: AZERTY moves `q`/`w`/`a`/`z`/`m` across rows entirely, not just two letters within one): a plain Kotlin
`object` exposing a `rows(proportions, showNumberRow, urlMode, emailMode, locale): List<List<Key>>` function
that builds your alphabet's three letter rows (number row, third-row shift/backspace, and the bottom row are
all shared with the Latin layout via `KeyboardLayout.urlBottomRow`/`emailBottomRow` - only the letters
differ). A new Latin geometry can still take `letterHints` as a parameter exactly like `KeyboardLayout`
itself does, since the AltGr overlay (§3) still applies on top of it the same way - `AzertyLayout.rows()`
also reuses `KeyboardLayout.topRowKey()` directly so `p`/`o` keep their existing math-symbol/average-symbol
popups (D-99/§29) wherever those two letters land in your row; a non-Latin script (Greek) typically has its
own accent system instead and does not need the `letterHints` parameter at all. Then:

1. Add a new `LayoutKind` entry in `keyboard/LayoutRegistry.kt` and map your `Language` to it in `KINDS`.
2. `AdaptKeyboardView`'s row-selection (`InputSurface.LETTERS -> when (layoutKind) { ... }`) is, since D-314,
   a genuine `LayoutKind` switch rather than the old `greek`/`qwerty` boolean pair - add your own `when`
   branch calling your new layout object's `rows(...)`.
3. `AdaptKeyService.applyActiveLanguageToView()` (`keyboardView?.layoutKind = LayoutRegistry.kindFor(...)`)
   needs no change for an ordinary new entry - it already derives `layoutKind` generically from
   `activeLanguage` via `LayoutRegistry`.

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
