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

Up to six plain text files, UTF-8, one language - D-310: every file here uses a fixed, un-suffixed name
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
- **`abbreviations.tsv`** (optional, D-434 - §6's sentence-boundary detection): one lower-cased abbreviation
  per line, period included (e.g. `usw.`, `etc.`), parsed by `Abbreviations.parse()`; blank lines and `#`
  comment lines are ignored. Without this file, your language falls back to `Abbreviations.GERMAN` -
  functional but not tailored (the exact same fallback shape `hints.tsv` has above), so it is worth
  providing your own list of the abbreviations that actually end a sentence-final period in your language,
  rather than leaving German's own list (`usw.`, `bzgl.`, `z.b.`, ...) silently applied to your text instead.
  Getting this wrong only ever costs a missed/extra auto-capitalisation after a period - never anything
  destructive - so a rough first list is a safe, low-risk contribution even before it is exhaustive.
- **`diacritics.tsv`** (optional, D-436 - the D-144/D-204/D-387 umlaut-style fold/unfold mechanism,
  generalised beyond German): one base letter per line, `baseLetter<TAB>variant1,variant2,...`, e.g.
  `e<TAB>é,è,ê,ë` for French or `g<TAB>ğ` for Turkish - parsed by `DiacriticTable.parse()`; blank lines and
  `#` comment lines are ignored. This is the file that lets a token typed without its diacritic (`"cafe"` for
  `"café"`) still reach the correctly-spelled word, both live while typing (prefix completion) and at
  autocorrect/split-repair time - exactly what German gets from `ä`/`ö`/`ü`/`ß` today, generalised via
  `de.froehlichmedia.adaptkey.suggestion.DataDiacriticFolding`. Only the base -> known-variants direction is
  ever needed, even for a base letter with *several* real variants (French's `e` above) - see
  `DataDiacriticFolding`'s own KDoc for why the algorithm never needs the reverse. Without this file, your
  language falls back to `NoOpDiacriticFolding` - no diacritic handling at all, not German's own map (which
  would help nothing for a language that does not have German's diacritics) - so this file is a genuine,
  standalone improvement, not a "tailored vs. generic default" choice the way `hints.tsv`/`abbreviations.tsv`
  are. §8 below covers where this base-letter list most naturally comes from alongside `hints.tsv`'s own.
- **`version.txt`** (optional but strongly recommended, D-308): its first line is a single plain integer,
  e.g. `1` - this pack's own version, bumped by *you* every time you publish a revised
  `dict.tsv`/`bigram.tsv`/`hints.tsv`/`abbreviations.tsv`/`diacritics.tsv` under the same hosted URL. `LanguagePacksActivity`'s Download/Import
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
`abbreviations.tsv`/`diacritics.tsv`/`version.txt`. Build one like the existing
`language-packs/adaptkey-lang-de.zip`/`adaptkey-lang-el.zip` (a
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

---

## 8. A "pretty good" one-shot pipeline (LLM-assisted, not a substitute for a native speaker)

§§3-6 above describe *what* files a language pack needs and *how* to package/host them, once you already
have their content. This section is the *how to actually generate that content fast*, using an LLM to do in
one sitting what German's own dictionary took many separate, individually-reviewed rounds to reach (see
`AdaptKey-Progress.md`'s D-368/D-402/D-412/D-412-adjacent entries for that real history). Read it before
starting a language you do not speak yourself.

**Be honest about the ceiling.** This pipeline gets a new language to "pretty good" - typable, with
reasonable autocorrect/suggestions and no glaring false-positive corrections - not to the same bar as German
today, which reflects a genuine native speaker (the app's own maintainer) reviewing real device feedback over
many months. There is no shortcut past that for true native-level quality; do not label a pipeline-only
pack as anything more than "pretty good" in its own PR description. Step 11 below is the one non-optional
gate between "the pipeline ran" and "this is fit to publish."

**No shortcuts, no partial runs.** Every numbered step below is required, in order, for every language this
pipeline builds - "I'll do the dictionary now and the word families later" is exactly how French shipped
twice with a real, silently-lower-quality dictionary each time (see the two cautionary tales immediately
below): once with step 0's frequency corpus swapped for a small, wrong-shaped proxy source instead of a real
Wikipedia dump, and once with step 3/4's own Wiktionary source silently the wrong (thinner) one. Neither was a
deliberate scope cut - both were an implicit "this is probably close enough" that turned out not to be, caught
only because a human asked a pointed question afterwards. Run every step, verify each against the checks it
names, and only then move to the next.

**Two real, previously-shipped mistakes, so a future run does not repeat either of them:**

- **Step 0's corpus must be an actual Wikipedia dump, not a smaller proxy** (a pre-built word-frequency list,
  a handful of live API-sampled articles, ...). French's own first pass (superseded, see `AdaptKey-History.md`
  §413/§415) used `hermitdave/FrequencyWords`' OpenSubtitles-derived list capped at 12,000 words plus
  suffix-heuristic POS tagging, reasoned as "close enough for one sitting" - it was not: 12,000 words is a
  toy vocabulary next to German's/Greek's own 150,000+, and heuristic suffix tagging reintroduced exactly the
  false-positive class (`Krieg`/`kriegen`-style spelling collisions) step 3 below exists to avoid. Redone
  properly in §415 once a real reader asked "that's only 12k words?" - do the real dump extraction the first
  time; see step 0 below for how, and read `dictionaries/fr/extract_wiki_dump.py`/`dictionaries/es/
  extract_wiki_dump.py` as real, working reference implementations (including the memory-leak/performance/
  stdout-buffering pitfalls their own comments document) before writing a new one from scratch.
- **Step 3/4's Wiktionary source must be the target language's *own*, native-language Wiktionary edition, not
  the English Wiktionary's coverage of that language.** kaikki.org publishes both, and the URL that a human
  (or an LLM) naturally lands on first - `kaikki.org/dictionary/<Language>/` (e.g. `.../French/`,
  `.../Spanish/`) - is the *wrong* one for this purpose: it is the English-language Wiktionary edition's own
  documentation of foreign words, filtered to your target language, and is systematically thinner than that
  language's own speakers' own Wiktionary (real, measured numbers: French's correct source is 714.6MB
  compressed vs. the wrong one's 56.5MB - a 12x difference; Spanish's is closer, 99.3MB vs. 91MB, but still the
  wrong file). This was used for **both** French and Spanish's own first dictionary builds without anyone
  noticing until directly asked "why is the French one so small?" - see step 3 below for the correct URL
  pattern and how to verify you have the right file *before* spending an hour parsing it.

Both mistakes share the same shape: a smaller, more-convenient-to-fetch source that still *produces output*
- nothing errors, nothing looks obviously broken, the pipeline "runs cleanly" - so the only real defence is
checking each step's own stated size/scale expectations before trusting its output, not just checking that it
completed.

**Mandatory pre-flight check, before downloading anything for real: verify both sources' real sizes first,
every time, even if you are confident you already know the right URL.** This is a hard gate, not a
suggestion - do not start step 0's or step 3's actual extraction until both checks below have been run and
their numbers written down (in your own working notes, or directly in the PR):

```bash
# Wikipedia corpus (step 0) - confirm you are looking at a real dump part, not a proxy list
curl -sI "https://dumps.wikimedia.org/<code>wiki/latest/" | ...   # list available parts first, then:
curl -sI "https://dumps.wikimedia.org/<code>wiki/latest/<code>wiki-latest-pages-articles1.xml-p1p<N>.bz2"
# Expect: low hundreds of MB to low GB for a major language's first split. A number in the single-digit MBs
# means you found the wrong file (or a near-empty language edition) - stop and re-check the listing.

# Wiktionary corpus (step 3/4) - confirm you have the NATIVE edition, not the English-Wiktionary-coverage one
curl -sI "https://kaikki.org/dictionary/downloads/<code>/<code>-extract.jsonl.gz"        # correct
curl -sI "https://kaikki.org/dictionary/<Language>/kaikki.org-dictionary-<Language>.jsonl.gz"  # wrong for this
# Compare Content-Length of both. Always use the downloads/<code>/ one when it exists (check
# https://kaikki.org/dictionary/rawdata.html first) - even when the two sizes happen to be close (Spanish's
# own case), not only when the gap is dramatic (French's own case, 12x).
```

If the `downloads/<code>/<code>-extract.jsonl.gz` URL 404s, your language genuinely has no native Wiktionary
edition yet (check the `rawdata.html` list to be sure, not just this one 404) - the English-Wiktionary-coverage
page is then the *correct*, only available choice, not a fallback to feel bad about; say so plainly in the PR
rather than silently treating it as equivalent to a language that does have its own edition.

0. **Base corpus extraction** - `dict.tsv` (word + frequency + optional POS) and `bigram.tsv`
   (`previousWord<TAB>word<TAB>count`) from a real Wikipedia XML dump of your target language, the same source
   German/Greek's own bundled dictionaries and French's/Spanish's own were built from - **an actual dump part
   from `https://dumps.wikimedia.org/<code>wiki/latest/`, streamed and parsed, not a pre-built word-frequency
   list or a small live-API sample.** `dictionaries/fr/extract_wiki_dump.py` and `dictionaries/es/
   extract_wiki_dump.py` are real, checked-in reference implementations - adapt one of them (mostly just the
   target-language letter set/tokeniser regex) rather than writing this from scratch; their own comments
   document three real pitfalls worth reading before you start, not after: an `iterparse` memory leak
   (`root.clear()`, not only `elem.clear()`, after every page), a too-slow per-character template stripper
   (use the regex-driven iterative version instead), and `python -u` for the background run (a fully-buffered
   redirected stdout can make a genuinely healthy multi-hour process look stuck for a long stretch). Check the
   machine's actual free RAM live (`Get-CimInstance Win32_OperatingSystem` on Windows) before choosing how many
   pages to cap the run at - conservative, reasoned from a previous run's own confirmed-safe resident-memory
   figure, never blindly copied from a different machine's number.

1. **Frequency-scale calibration.** Several ranking/correction formulas are tuned against the *magnitude* of
   German Wikipedia's own frequency numbers - `CorrectionConfidence.NOUN_REFERENCE_FREQUENCY` (2000.0), the
   D-411 log-scaling calibrated to "50 personal uses ≈ a moderately common dictionary word", and similar
   constants elsewhere. A corpus of very different size (most languages' Wikipedia is smaller than German's)
   produces systematically different frequency magnitudes, which can silently misfire these thresholds without
   ever throwing an error. Sanity-check your own corpus's frequency distribution (e.g. compare the frequency
   of a handful of common vs. rare words against the equivalent German ones) before trusting downstream
   ranking to behave the same way it does for German/English/Greek.

2. **LLM-based noise removal**, replacing the manual, band-by-band review German's own dictionary went
   through (`AdaptKey-Progress.md` §301): batch your `dict.tsv` entries (by frequency band, not one word at a
   time - individual per-word LLM calls do not scale to a corpus this size) and have the model flag
   tokeniser artefacts, foreign-language leakage, and Wikipedia-markup fragments for removal.

3. **LLM/Wiktionary-based part-of-speech tagging, including homograph disambiguation - from the correct
   source, verified before you trust it.** Tag every entry with `PartOfSpeech` (§3 above). Prefer a real
   Wiktionary extract over an LLM's own per-word judgement wherever one exists (more reliable, and doubles as
   step 4's own input) - but **the extract must be the target language's own, native-language Wiktionary
   edition**, not the English Wiktionary's coverage of that language:
   - **Correct**: `https://kaikki.org/dictionary/downloads/<code>/<code>-extract.jsonl.gz` (e.g. `.../fr/
     fr-extract.jsonl.gz`, `.../es/es-extract.jsonl.gz`) - that language's own Wiktionary (fr.wiktionary.org,
     es.wiktionary.org, ...) documenting itself. Check
     [`kaikki.org/dictionary/rawdata.html`](https://kaikki.org/dictionary/rawdata.html) first for the current
     list of languages with their own native edition (twenty as of this writing, including `de`/`el`/`es`/
     `fr`/`it`/`nl`/`pt` - covers every language this project has built or is likely to build next). A
     language *not* on that list has no native Wiktionary edition to extract from at all - fall back to the
     English-Wiktionary-coverage page below and say so explicitly in your PR, rather than silently treating it
     as equivalent.
   - **Wrong for this purpose, easy to land on by accident**: `https://kaikki.org/dictionary/<Language>/
     kaikki.org-dictionary-<Language>.jsonl(.gz)` (e.g. `.../French/`, `.../Spanish/`) - this is the *English*
     Wiktionary edition's own documentation of foreign words, filtered to your target language. It looks like
     exactly what you want (a per-language kaikki page, a working `.jsonl.gz` download) and produces real,
     valid-looking output either way - the only way to tell you have the thinner source is to notice the file
     is suspiciously small. **Before trusting either file, `curl -I` both URLs and compare sizes** - a large
     gap (French's own real numbers: 714.6MB native vs. 56.5MB English-coverage, a 12x difference) is the tell.
     If they are close in size (Spanish's own case: 99.3MB vs. 91MB), the native one is still correct - prefer
     it on principle, not only when the size gap makes the mistake obvious.
   - Ask the model directly "what part(s) of speech does this word have in \<language\>" only for words the
     Wiktionary extract itself does not resolve (unlike German's own retrofitted D-368 sweep, which had to lean
     on an error-prone `+n`/`+en` spelling heuristic, `Krieg`/`kriegen` vs. `Krieg`/`Kriege`,
     indistinguishable from spelling alone) - direct LLM POS questions sidestep that whole class of false
     positive for the remainder.

4. **Wortfamilien / lemma completion via Wiktionary, with a mandatory bare-noun safety check.** Generate full
   inflectional paradigms (verb conjugations, noun declensions/plurals, adjective degree/declension) and link
   every generated form back to its lemma (the `lemma` column, D-412), from the **same native-language
   Wiktionary extract step 3 already fetched** - one parse, feeding both steps, not a second download.
   `dictionaries/de/extract_wiktionary_*.py`, `dictionaries/el/extract_wiktionary.py`/`merge_wiktionary.py`,
   and `dictionaries/en/extract_wiktionary.py`/`merge_wiktionary.py` are real, checked-in reference
   implementations to adapt, not just a description - read them before writing your own; prefer the Greek
   one's *generic* per-form extraction (`word<TAB>form`, many rows per lemma, grouped back at merge time) over
   English's own fixed-named-slot approach (`s_form`/`ing_form`/`past`/`participle`) unless your language's own
   morphology is genuinely as small and regular as English's - German/Greek/French/Spanish all needed the
   generic shape. Rule-based generation plus curated exception tables, not individual-candidate review - the
   German/Greek scale (tens of thousands of generated forms) makes manual review of every one infeasible, the
   same conclusion the (now-superseded, deleted) `AdaptKey-Plan-Wortfamilien.md`/`AdaptKey-Plan-Adjektive.md`
   design docs reached.

   **Mandatory, structural check, not a judgement call: after tagging (this step or step 3), no row may carry
   a bare `{NOUN}` tag set - alone, with nothing else - in a language whose step-8 decision (below) is "does
   not capitalise common nouns."** This is not optional or "usually fine": English's and Greek's own
   already-shipped `merge_wiktionary.py` scripts had no such check at all - `add_tag()` simply adds whichever
   POS a Wiktionary entry documents, discarding `OTHER`, with no regard for how rare or archaic that sense is
   relative to the word's actual everyday use. Real, measured consequence, found only by directly grepping the
   shipped data rather than trusting the pipeline had worked: 36,580 rows in English's bundled `dict.tsv` and
   46,608 in Greek's carried a bare `NOUN` tag this way, including ordinary function words with one genuine but
   vanishingly rare technical/archaic noun sense Wiktionary happens to document (`and`/`or` as logic-gate
   nouns, `he`/`it` as rare informal nouns, `were` as a homograph of an unrelated word, and more) -
   `CapitalisationEngine`'s own rule 3 (`isPureNoun -> true`) reads *only* the tag set, never frequency or
   how central that sense actually is, so every one of these would auto-capitalise on ordinary typing. Enforce
   the fix as one unconditional, mechanical step over the *entire* output, not a per-word review: any row whose
   final tag set is exactly `{NOUN}` gets `OTHER` added (`{NOUN, OTHER}`) - safe and cheap, since it only ever
   suppresses rule 3's forced capitalisation and never removes the real `NOUN` signal A-05's split-safety gate
   and this same step's own lemma-linking still depend on. A row already carrying `PROPER_NOUN` is correctly
   unaffected either way (`isProper` forces capitalisation regardless of language - see `CapitalisationEngine`'s
   own hierarchy). For a language whose step-8 decision *is* "capitalises common nouns like German" (rare -
   German is the only one so far), this check does not apply; say which case you are in, explicitly, in your
   PR, the same way step 8 already asks you to.

5. **AltGr / long-press hint set (`hints.tsv`) and the diacritics table (`diacritics.tsv`, D-436) - one
   research pass, two files.** More automatable than an earlier draft of this guide assumed, for one large,
   common case: a Latin-script language whose own special characters are diacritic variants of an existing
   Latin letter - Turkish `ğ ş ı`, Polish `ł ż ń ć ś`, French `é è ê ë`, and similar - maps unambiguously onto
   that base letter (`ğ`/`ş` belong to `g`/`s`, `ł` to `l`, exactly the same relationship `ä`/`ö`/`ü` already
   have to `a`/`o`/`u` in German's own set). An LLM can draft this base-letter -> variants list directly and
   reliably, since which base letter a diacritic belongs to is never in question - the same list answers both
   files at once: `diacritics.tsv` wants the *complete* variant set per base letter (D-436's own
   `DataDiacriticFolding` handles several variants on one base letter natively, e.g. French's `e`), while
   `hints.tsv` wants only *one* representative symbol per key (a single AltGr popup hint, §3's own format
   limit) - pick the single most-used variant for that file, keep the full list for this one. Symbol/
   punctuation choices for the rest of `hints.tsv` (currency signs, quotation marks, math symbols) are more of
   a genuine UX judgement call about what that language's users actually reach for day to day - still worth an
   LLM first draft, but flag it explicitly for a human/native check rather than trusting it the way the
   diacritic mapping can be trusted.

6. **Abbreviation list (`abbreviations.tsv`, D-434, §3).** Have the model draft a first list of the
   sentence-final-period abbreviations actually used in the target language (the German list itself,
   `Abbreviations.GERMAN`, is a real reference example of the shape/size to aim for). Low risk even before
   it is exhaustive - see §3's own note on what getting this wrong actually costs.

7. **Confusables / keyboard-adjacency risk scan.** Needs the language's own actual keyboard geometry, so it
   only makes sense once §0/§5's layout question is settled. Mirrors the method D-304/D-330-followup used for
   German's own possessive-determiner collisions: for pairs of real, frequent words that are a single
   keyboard-adjacent-key substitution apart (via `KeyboardProximity.kt`'s real adjacency grid), check the
   live `CorrectionConfidence` formula for whether one could silently and wrongly autocorrect into the other,
   and blacklist the genuine risks. This is the mechanism `AdaptKey-Progress.md`'s open TODO ("
   `seedBundledBlacklist`'s cross-language-confusables set is German-only") is asking a future language to
   extend - see item 9 below for why doing so still needs a real Kotlin `LanguageRules` implementation today,
   not just a data file.

8. **Capitalisation-rule applicability - an explicit decision, not a silent default.** §6 rules 3/4 (automatic
   noun capitalisation) are a German-specific orthographic convention, not a universal one. Decide
   consciously whether the target language shares it: if not, still tag nouns accurately in step 3 (other
   mechanisms, e.g. A-05's split-safety gate, read the `NOUN` tag independent of capitalisation), but the
   language simply never triggers rules 3/4's automatic capitalisation in practice, precisely because "noun"
   there does not imply "always capitalised" the way it does in German. Write this decision down in your PR -
   do not let a future reader assume it was silently inherited from German's own behaviour.

9. **`LanguageRules` (§4's own D-410 seam, `language/LanguageRules.kt`) - naive-fill where genuinely possible,
   left `NoOpLanguageRules` everywhere else, and say so explicitly.** Today only German has a real
   implementation (`GermanRules`); every other language silently gets the no-op default (every check answers
   "does not apply"). Of its nine hooks, three are plain factual questions an LLM can answer directly and
   safely: `decimalCommaGluesDigits()` (does this language write decimals with a comma or a point - a simple
   locale fact), `timeSuggestionWord()` (the S-08-style word, if any, this language's speakers write after a
   clock time - `null` is a perfectly correct answer for most languages), and `bundledConfusablesBlacklist()`
   (directly reuses step 7's own scan output). The other six -
   `blocksAsSplitPrefix`/`blocksAsFeminineAgentException`/`blocksAsCompoundPrefix` (A-05's German-compounding-
   specific split vetoes) and `isPlausibleVerbInflection`/`isPlausibleAdjectiveComparative`/`splitCompound`
   (regular-inflection/compound recognition) - encode real, language-specific grammatical algorithms, not data
   lookups; an LLM can only draft a first attempt at these for a human who actually knows the language's
   grammar to rework, and most contributed languages will legitimately ship with `NoOpLanguageRules` doing
   nothing for these six until someone invests that real grammar-engineering effort. That is a documented,
   accepted degraded state (exactly what `NoOpLanguageRules`'s own KDoc already describes), not a defect to
   silently paper over with a naive implementation that gets the grammar wrong. **This hook, unlike every
   other file in §3, still requires an actual Kotlin class and a PR even for the three naively-fillable
   answers** - there is no data-file mechanism for `LanguageRules` today (the D-434 pipeline step 6 above is
   the closest recent precedent for what turning it into one would look like, if a future round takes that on).

10. **Character-trigram profile data (§6, `language_profiles.tsv`) - worth closing in the same pass.** The
    builder script for this is documented as "a known gap, not a design choice" (§6 above); since you are
    already building a full pipeline, generate this alongside everything else rather than leaving language
    detection (A-03) degraded for your language too. Match `language/CharNgrams.kt`'s normalisation
    byte-for-byte (§6's own warning) or the classifier's accuracy collapses silently.

11. **The mandatory gate: a real speaker's sanity sample, before calling any of the above "pretty good."** An
    LLM pipeline has no ground truth of its own to check itself against. Before publishing, have someone who
    actually speaks the language (does not need to be a programmer) spot-check a genuine random sample across
    several frequency bands - noise survived removal, POS tags plausible, no obviously wrong autocorrect
    pairs. A confidently-shipped, silently-wrong dictionary is worse for that language's users than shipping
    no dictionary at all (§0's own honest "typable, no smart features" fallback tells the truth about its own
    limits; a wrong dictionary does not). Do not skip this step because every earlier step ran cleanly - a
    clean pipeline run is not the same thing as a correct one.
