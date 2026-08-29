# AdaptKey — Implementation Progress

Living status of the AdaptKey IME implementation, kept next to
[`AdaptKey-Spec.md`](AdaptKey-Spec.md) (the current, crystallised feature set) and
[`AdaptKey-History.md`](AdaptKey-History.md) (the append-only device-feedback/design-decision journal).
Requirement IDs (`L-`/`T-`/`G-`/`S-`/`A-`/`B-`/`C-`/`K-`, plus the newer `E-`/`U-`/`P-`/`W-`/`R-`/`V-`/`N-`/`X-`
domains) refer to the spec. Update this file whenever a component lands so it does not have to be restated
in every prompt.

## Project / Stack

- Directory: `D:\workspace-ai\adapt-key` (workspace `D:\workspace-ai`).
- Kotlin, `InputMethodService`, package `de.froehlichmedia.adaptkey`.
- minSdk 26, compile/target 35; AGP 8.7.3 / Kotlin 2.0.21; Gradle wrapper 8.11.1;
  `androidx.preference:preference-ktx` 1.2.1.
- Self-drawn `AdaptKeyboardView` (NOT the deprecated `android.inputmethodservice.KeyboardView`)
  because the spec needs raw `ACTION_DOWN` coordinates and free key proportions.
- D-280: only English's dictionary is bundled in `app/src/main/assets/`; German/Greek/any further language
  are installable language packs (`dictionaries/<code>/` + `language-packs/adaptkey-lang-<code>.zip` at the
  repo root, hosted, downloaded via `LanguagePacksActivity`) - see `AdaptKey-Language-Contribution-Guide.md`
  before assuming a language needs new layout code; most don't.

## Build (machine specifics)

- The only system JDK is Corretto 25, which the older Gradle dislikes → builds MUST
  use the Android Studio bundled JBR (JDK 21) as `JAVA_HOME`.
- Android SDK at `D:\Android\Sdk` (in `local.properties`, gitignored).
- Command (Git Bash, from the project dir) - **`:app:assembleRelease` since D-223** (produces the real,
  day-to-day install artifact, `app/build/outputs/apk/release/AdaptKey.apk`; `:app:testDebugUnitTest` still
  targets the debug variant regardless - the Kotlin source itself is identical between variants, there is no
  separate "release" source set to test, so this is simply the established, no-signing-required unit-test
  task and needs no change):
  ```
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME=/d/Android/Sdk ./gradlew :app:assembleRelease :app:testDebugUnitTest
  ```
- A post-write hook normalises blank lines / CRLF and may reformat files after edits
  (do not fight it).
- **D-223: `:app:assembleRelease` is now the default build target** (was `:app:assembleDebug` through D-222) -
  signed, `debuggable=false`, `isMinifyEnabled=false` (deliberately no code shrinking/obfuscation). Needs
  `keystore.properties` at the project root (gitignored, not checked in - holds
  `storeFile`/`storePassword`/`keyAlias`/`keyPassword` for the equally gitignored `release.keystore`, also at
  the project root); `:app:assembleRelease` fails without it, everything else configures fine regardless.
  **The release keystore is load-bearing once it has signed an installed build - back it up.** Losing it
  means every future version needs an uninstall + reinstall, wiping the learned dictionary/settings, since
  Android requires the same signing key to update in place.

## Release Channels

- **F-Droid (official, sole channel by explicit user decision, 2026-07-28).** IzzyOnDroid and a
  self-hosted repo were both considered and explicitly declined in favour of official F-Droid only,
  weighing discoverability against review latency and signing continuity. F-Droid will build and sign
  the app with its own key (also an explicit choice over pursuing Reproducible Builds); anyone who has
  sideloaded a self-signed `:app:assembleRelease` build will need one uninstall + reinstall to switch
  once the F-Droid release goes live - export via the Y-01 backup screen first to avoid losing learned
  words/settings.
- **Prerequisites already satisfied:** GPL-3.0-or-later with SPDX headers, public GitHub repo
  (`github.com/m-froehlich/adaptkey`), no `INTERNET` permission at all (so no tracking/ads/Play-Services
  anti-feature applies), every runtime dependency Apache-2.0/MIT, every bundled data asset under a free
  licence (see `CREDITS.md`) - including the optional tier-3 SmolLM2-360M model itself, not just its
  bundled tokenizer, confirmed Apache-2.0 on its Hugging Face model card, so no `NonFreeAssets`
  anti-feature is needed either.
- **Done so far:**
  - `fastlane/metadata/android/{en-US,de-DE,el-GR}/` scaffold added (`title.txt`,
    `short_description.txt`, `full_description.txt`, `changelogs/<versionCode>.txt`) - both F-Droid and
    other fastlane-aware repos read the store listing directly from these files in the repo, so a future
    listing/changelog update needs no separate submission, just a commit.
  - Annotated git tags `v1.0.2`/`v1.0.3`/`v1.0.4` added locally on their respective version-bump commits
    (versionCode 306/307/308) - the first tags in the repo, backfilled onto commits that already existed
    when this work started (v1.0.3/v1.0.4 landed in this same repo while the fastlane scaffold was being
    built, so the initial `v1.0.2`-only tag/changelog work had to be corrected once that was noticed - see
    `git log`/`git tag -l` for the authoritative current set, never assume this list is still complete).
    Going forward, a tag per release (`vX.Y.Z` on the version-bump commit) joins the existing release
    routine, since F-Droid's `UpdateCheckMode: Tags` / `AutoUpdateMode: Version` needs it to detect and
    build new versions automatically after the initial submission. All three tags and the two fastlane
    scaffold commits are now pushed to `origin/main`.
  - Draft `metadata/de.froehlichmedia.adaptkey.yml` written to
    `scratchpad/fdroiddata-metadata-de.froehlichmedia.adaptkey.yml` (not consumed by this repo's own
    build - it is the exact content to place in a fork of `fdroiddata`), covering all three tagged
    versions, `Categories: [Keyboard & IME]`, `AutoUpdateMode: Version` / `UpdateCheckMode: Tags`.
  - **Confirmed by an actual clean-room build**, not assumed: `keystore.properties` was moved aside and
    `:app:assembleRelease` re-run - it succeeds and produces an unsigned `AdaptKey.apk` in the normal
    `app/build/outputs/apk/release/` location, exactly the environment F-Droid's own build server has (no
    keystore, no network beyond Maven Central dependency resolution). D-223's existing
    `keystorePropertiesFile.exists()` guards already handle this correctly - no source change was needed.
    `keystore.properties` was restored immediately afterward.
  - **User forked `fdroiddata` on GitLab and opened the merge request** with the scratchpad draft's
    content at `metadata/de.froehlichmedia.adaptkey.yml`, entirely via the GitLab web UI (no local
    checkout needed for this step). This is state that lives entirely outside this repo/session - the MR
    URL and its review status are not tracked here; check GitLab directly for the current status.
  - App icon done (D-321/D-322) - the "Optional: app icon" item below is now only about screenshots.
  - **Tags backfilled for v1.0.5/v1.0.6/v1.0.7** (versionCode 309/310/311), added locally after three more
    releases landed while the icon work was in progress - same pattern as the earlier v1.0.3/v1.0.4 gap,
    now a recurring risk worth naming explicitly: any session doing F-Droid-adjacent work should re-check
    `git tag -l` against the actual `versionCode` in `app/build.gradle.kts` before trusting this list, not
    assume it's complete. The scratchpad `fdroiddata-metadata-*.yml` draft's `Builds`/`CurrentVersion`
    updated to match (now lists through 1.0.7) - **the already-open MR itself still only lists through
    1.0.4 and needs the same update pushed to it** (GitLab web UI, same file/branch as the original MR).
  - **First real maintainer feedback arrived (`licaon-kter`)**, both addressed:
    - A stray duplicate metadata file (`metadata/metadata/de.froehlichmedia.adaptkey.yml` - doubled path
      from a GitLab "New file" UI slip, plus a first attempt committed straight to the fork's `master`
      before remembering to branch) - user removed both copies via the GitLab web UI.
    - `phoneScreenshots/` requested at `fastlane/metadata/android/en-US/images/` **in this repo**, not the
      `fdroiddata` fork - screenshots live with the app's own source since F-Droid reads Fastlane metadata
      directly from `Repo:`. 5 phone screenshots added as `1.png`-`5.png` (user's own device captures,
      chronological order preserved).
    - A signed release APK matching the `v1.0.7` tag exactly was requested for
      `github.com/m-froehlich/adaptkey/releases/tag/v1.0.7`. Built via an isolated `git worktree` checked
      out at the `v1.0.7` tag (kept the main checkout, at the time already ahead, untouched) - the
      gitignored `keystore.properties`/`release.keystore` were copied in just for that build and removed
      again afterward. Verified signed with the real AdaptKey/Froehlich Media certificate via `apksigner
      verify`. By the time the user got to actually publishing it, development had moved on far enough
      (v1.0.10) that chasing the now-stale v1.0.7 release no longer made sense - superseded rather than
      published. Instead: `v1.0.10` tagged and pushed (`versionCode` 314 - the last two intermediate
      versions, 1.0.9/`versionCode` 313, were themselves just temporary diagnostic-logging commits for an
      in-progress clipboard-chip bug investigation, not release-worthy on their own - consistent with the
      earlier "only tag release-worthy versions" discussion, deliberately left untagged), and a matching
      signed APK rebuilt directly in the main checkout (already at that exact commit) and handed to the
      user - publishing the GitHub Release itself is the user's own action, not done from here. The
      scratchpad metadata-yml draft's `Builds`/`CurrentVersion` updated to add 1.0.8 and 1.0.10 (skipping
      the untagged 1.0.9), now current through `1.0.10`/314.
    - Three further checklist items in the MR's own template (`External repos are added as git submodules
      instead of srclibs`, `Enable Reproducible Builds`, `Multiple apks for native code`) are all
      genuinely **not** satisfied today - not just unchecked boxes to tick. No submodules exist (N/A,
      nothing to do); Reproducible Builds was the alternative explicitly declined at the very start of
      this work (F-Droid signs instead, see above); multi-APK-per-ABI splitting isn't configured (one
      universal APK ships both `arm64-v8a`/`armeabi-v7a`). User confirmed (per the maintainer's own MR
      template) these are "recommended", not required for inclusion - left undone for now, not silently
      checked off.
- **The MR itself lives at `gitlab.com/fdroid/fdroiddata/-/merge_requests/44142`** (the upstream project's
  own namespace, not `gitlab.com/m-froehlich/fdroiddata/...` - a merge request from a fork is numbered
  under the *target* project, which cost some real back-and-forth to track down when a plain 404 gave no
  clue why). Source branch: `m-froehlich-master-patch-41126`.
- **Done:** the MR's `metadata/de.froehlichmedia.adaptkey.yml` updated to the current scratchpad draft
  content (through `1.0.10`) via the GitLab web UI, committed on that same branch.
- **First CI pipeline run failed with "Identity verification is required in order to run CI jobs"** - a
  GitLab.com anti-abuse gate on the user's own account, unrelated to this repo's content (0 jobs even ran).
  User verified their identity via GitLab's own account settings; retrying the pipeline then actually ran
  the real checks.
- **That real run produced 3 genuine failures, both diagnosed and fixed in the scratchpad draft:**
  - `build` job: F-Droid's scanner rejects any binary archive anywhere in the checked-out source tree,
    regardless of whether the Gradle build references it - flagged
    `language-packs/adaptkey-lang-{de,el}.zip` (the real, intentional downloadable language-pack content
    this repo ships at its root, not a build input). Fixed by adding `scandelete` (removes the named files
    before scanning/building - the documented fdroidserver field for exactly this case) to every `Builds`
    entry.
  - `test`/`checkupdates` + `test`/`fdroid rewritemeta` jobs: both compare the submitted YAML against
    fdroidserver's own canonical auto-formatted form and fail if it doesn't already match exactly. Rather
    than reverse-engineer the exact spacing from a noisy CI log, installed `fdroidserver` locally (`pip
    install fdroidserver`) and ran `fdroid rewritemeta` against a minimal local copy of the file to get the
    real canonical output directly, then diffed it against the draft to find the precise deltas: no blank
    lines between `Categories`/`License`/`AuthorName`/`SourceCode`/`IssueTracker` (only before `RepoType`
    and before `Builds`), and `versionName`/`CurrentVersion` values unquoted (`1.0.2`, not `'1.0.2'` - YAML
    doesn't need the quotes there and rewritemeta strips them). Verified the fix is a stable fixed point
    (running `rewritemeta` again on its own output changes nothing) before updating the scratchpad draft.
  - User pushed that fix to the MR (commit "Excluding some language packs") - confirmed byte-identical to
    the scratchpad draft via `curl`'d raw file diff before saying so, not just assumed.
- **Second CI run: `scandelete` fixed the scanner rejection - Gradle now runs to a genuine "BUILD**
  **SUCCESSFUL" - but a new, later failure appeared: `ERROR: Could not build app de.froehlichmedia.adaptkey:**
  **Failed to find any output apks`.** Root cause: D-223's own `applicationVariants.all { outputFileName =
  "AdaptKey.apk" }` customisation (a deliberate, documented choice - the day-to-day install artifact
  shouldn't leak AGP's default "app-release-unsigned.apk" naming) means the built APK doesn't land where
  fdroidserver's own build tool expects it by convention. Fixed with the `output:` Build field (F-Droid's
  own documented mechanism for exactly this - "a glob path where the resulting unsigned release APK...
  should be", for builds using custom output locations) pointing at
  `app/build/outputs/apk/release/AdaptKey.apk` - confirmed as the real path via this project's own earlier
  clean-room build test (D-321/D-322 prep). Re-verified canonical formatting with local `rewritemeta` again
  after adding it - still a stable fixed point.
- **The `checkupdates`/`rewritemeta` jobs were still failing on the same pushed commit, and this time it**
  **was a real gap in this session's own verification, not a new content issue.** The CI diff showed every
  line of the user's version ending in `^M` (CRLF) - this repo's own post-write hook normalises edited
  files to CRLF (correct for the actual Kotlin/Java source, wrong for this GitLab-bound scratch file), and
  the earlier local `rewritemeta` stability check had used `tr -d '\r'` before comparing, which silently
  hid exactly this discrepancy instead of catching it. A later `sed -i` edit (adding the `output:` field)
  happened to flip the scratchpad file back to pure LF as a side effect, confirmed byte-exact
  (`\r` count 0) - but the CRLF version is what the user had already pasted into GitLab. Given how easily
  a Windows copy/paste can reintroduce CRLF from a file opened locally, the corrected content was handed
  over inline in chat instead of via "open this file" - lower risk of picking up stray `\r` again, though
  not a hard guarantee across every OS/browser clipboard.
- **The chat-paste route still came back CRLF** (confirmed by re-downloading the MR's raw file via `curl`
  and byte-counting `\r`, not assumed) - Windows' clipboard convention for plain text is CRLF regardless of
  the source, so pasting from a chat message doesn't actually avoid it. Concretely proven, not just
  theorised: the CI's own `build` job failed with `location=-1` from a `grep -n '^    versionCode:\ 306$'`
  that could no longer match once a stray `\r` sat before the line's real end. Fixed properly this time by
  sidestepping copy/paste entirely - handed the user a real `.yml` file (`scratchpad/
  de.froehlichmedia.adaptkey.yml`, confirmed 0 `\r` bytes) to upload via GitLab's own "Replace file" (or
  Web IDE drag-and-drop), which commits the file's actual bytes rather than clipboard text. This worked -
  next pipeline run: `build` and `rewritemeta` both passed, only `checkupdates` still failed.
- **`checkupdates`'s own remaining failure was a real, single missing field, not a repeat of anything**
  **earlier:** it clones the actual tagged source and reads the real `AndroidManifest.xml` to auto-detect
  the app's display name (`autoname 'AdaptKey'`), then fails a `git diff --exit-code` check because adding
  `AutoName: AdaptKey` to the metadata would change the file. This is exactly why the much earlier,
  seemingly-contradictory "+AutoName: AdaptKey" line noticed in an early CI diff (dismissed at the time as
  probably stale) was in fact real: `checkupdates` can see the real manifest, this session's local
  `rewritemeta` reproduction never could, since it never had an actual repo checkout to read from. Added
  `AutoName: AdaptKey` (after `IssueTracker`, before `RepoType`, matching the CI's own proposed diff
  exactly) and re-verified locally against real `rewritemeta` output again - still an exact, stable match.
  Handed over the same way (file upload, not paste) to keep the CRLF fix intact.
- **CI pipeline fully green** after the `AutoName` push - `build`, `checkupdates`, and `rewritemeta` all
  passed. The MR is now technically complete and waiting purely on human maintainer review; no further
  content work is expected unless a reviewer asks for something new.
- **GitHub Release `v1.0.10` ("AdaptKey first official release") confirmed live**, with the signed
  `AdaptKey.apk` (36.9 MB) attached - checked via the GitHub API directly (`.../releases/tags/v1.0.10`),
  not just assumed from the page rendering.
- **Known, deliberately-deferred quirk:** the 5 `phoneScreenshots` under
  `fastlane/metadata/android/en-US/images/` are actually German-language captures, not English - the user
  noticed and explicitly chose to leave this as-is for now ("das soll erstmal egal sein, damit kommt jeder
  klar"). Not a bug to silently "fix" by moving them to `de-DE/` in a future session - revisit only if the
  user raises it again.
- **Still open:**
  - Respond to further F-Droid maintainer review feedback if/when it comes (first-time inclusion review
    commonly takes weeks, sometimes months).
  - Once merged: F-Droid's own build/publish cycle still needs to run before the app actually appears in
    the client - merged is not yet live.
  - Optional, still not done: real English `en-US` screenshots, and/or `de-DE`/`el-GR` screenshot sets.
  - Verify the `Categories: [Keyboard & IME]` choice and the exact current build-metadata field set
    against F-Droid's own docs/`fdroiddata` at MR time - both were checked against the live F-Droid docs
    and `config/categories.yml` this session, but that project's conventions can move on.
  - Await F-Droid maintainer review (expect weeks, not days, for first inclusion).

## Guardrail - Read Before Touching `onUpdateSelection` / Composing State

D-139 (§99-§101 in the spec) took three real device-log tracing rounds to actually fix. Any change to
`AdaptKeyService.onUpdateSelection()`, `reclaimSurroundingWord()`, `composingAnchor`, or the batch-edit
sequencing around them must keep spec §99-§101's three stated invariants intact - see that section's
"Guiding Principle" note before touching this area, not a full re-audit every time.

## Reserve Ideas (Deferred, Not Forgotten)

Design ideas that were seriously considered, confirmed technically workable, but deliberately not implemented
because the problem they would solve turned out not to be the actual bottleneck. Kept here (not just buried in
History.md's append-only log) so they are not lost if the situation that would justify them ever recurs.

- **`android.os.CancellationSignal` for a genuine mid-query SQLite abort** (discussed during the D-212/WAL
  round, see history §136). Confirmed technically real - Android wires it to `sqlite3_progress_handler`, a
  true mid-query interrupt, not just a pre-check - and confirmed it would NOT need to break
  `DictionarySuggestionProvider`/`DictionaryStore`'s deliberately Android-free testable abstraction: since all
  SQL work already funnels through one single-threaded executor, the cancel state could live entirely local to
  `SqliteDictionaryStore` itself via a narrow, additional (non-interface) method. **Status: not needed.** The
  D-207-D-221 investigation (closed, history §145) found the real cost was synchronous computation itself
  (redundant per-candidate queries, unbounded bucket scans, unconditional per-keystroke searches) - not
  discarded/wasted background work a mid-query abort would have addressed; the existing cooperative
  `isCancelled: () -> Boolean` polling (D-211/D-216) already covers the "stop a superseded background search
  between candidates" case adequately. Revisit only if a future profiling round again points at wasted,
  actually-superseded query time (not raw computation cost) as the dominant remaining factor.

## Open TODOs / Known Limitations (Not Yet Actioned)

Confirmed real, deliberately not fixed yet - flagged here so a future session does not have to rediscover
them, and does not fix them silently without the user's own go-ahead first (this project's own rule for
non-trivial changes).

- **`seedBundledBlacklist`'s cross-language-confusables set (A-04, `due`/`sue`/`ddr`/`aks`) is German-only.**
  Found while auditing every place that does *not* route through the active-language pipeline (history §210's
  own D-287 fix) - deliberately scoped to German today (that is the only curated list that exists), not a bug
  in the D-287 sense. Revisit once a second installed language's own confusables against English are actually
  curated - the seeding mechanism itself (`installStores()`) does not need to change shape, only gain a
  per-language list to seed from, the same way `hints_<code>.tsv` (D-281) already generalised the AltGr hint
  set per language.

- **D-306's dictionary cleanup only removed *untagged* entries (missing part-of-speech) - it did not attempt**
  **a broader sweep of entries that carry a valid tag but are still dubious** (foreign proper nouns, obscure
  fragments) **the way "til" itself was before its manual fix.** A narrow probe (short, low-frequency,
  `OTHER`-tagged German entries) turned up ~490 further candidates in a single quick check - almost certainly
  an undercount of the true scope across the full ~210k-row combined dictionary. Not attempted in D-306: no
  reliable automated signal distinguishes a genuine rare/foreign word from data-extraction noise once it
  already carries *some* tag, and manual review at that scale is not tractable in one session. Revisit with
  better tooling (a proper reference-wordlist cross-check per language, as D-306 improvised for the
  untagged-only English pass) if this class of false-positive split is reported again. **Confirmed to recur**
  (2026-07-28, see history §242): `"fir"` (German dict, frequency 12, tagged `NOUN,OTHER`) blocks
  `"fir"` -> `"dir"` autocorrect the same way, since 273/12 ≈ 22.75x falls short of the 100x override ratio -
  `git show` confirmed D-306 never touched this entry (already tagged, out of that pass's scope). Not fixed,
  same reasoning as above.

- **D-314 built French's AZERTY *geometry* only - no `dictionaries/fr/` pack (dict/bigram/hints/version)**
  **exists yet, and `LanguagePackCatalog.ENTRIES` has no `Language.FRENCH` entry.** French is now fully
  typeable (the layout is real, compiled, and wired to `Language.FRENCH`) but not yet usable end to end as a
  language pack - a French speaker gets the right keys but no dictionary/autocorrect/suggestions until
  someone builds and hosts one, following the Contribution Guide's own §3/§4. Not started; flagging so a
  future session (or an actual French-speaking contributor) doesn't have to rediscover that the geometry and
  the content are two separate, independently-completed pieces of this feature.

- **D-330 fixed `deine`/`deiner`/`deinen`/`deinem`/`deines` against their `seinX` counterparts, but the**
  **bare, uninflected `dein` (no suffix, e.g. "dein Buch") shows the identical Wikipedia-corpus**
  **register-skew ratio and was only found while verifying the already-rebuilt archive, too late to fold**
  **into that round's already-agreed scope:** `dein` 139 vs `sein` 28942 (~208x), past the 100x
  `KNOWN_WORD_OVERRIDE_RATIO` bar, `d`/`s` adjacent - almost certainly autocorrects `dein` -> `sein` today by
  the same mechanism, unconfirmed on-device. Deliberately left unfixed pending explicit go-ahead, same
  convention as D-330's own initial scoping. Revisit with the same fix shape (raise `dein`'s frequency by a
  comparable ~1.5x-over-minimum margin, e.g. into the low-to-mid hundreds, plus the usual pack rebuild/version
  bump) if confirmed or raised again - and worth a fresh, deliberate full audit of every remaining
  `dein-`/`sein-`/`mein-`/`unser-`/`ihr-` pair at that point, rather than continuing to fix this one paradigm
  one report at a time.

- **D-344 (download directory control, spec §30): the app's approach to ensuring browser-downloaded**
  **language packs and LLM models land where AdaptKey can find them is not yet decided.** Three options
  (HTTP header control, SAF/file-picker API, raw repo path) are documented in the spec; practical testing
  is needed before choosing.

- **D-345 (dictionary noise scan, spec §31): "Bri" must be blacklisted or removed, and the full bundled**
  **dictionary scanned for every remaining Wikipedia-extraction-noise entry (fragments, obscure acronyms,**
  **markup tokens).** Extends D-306's scope from *untagged* entries to also cover dubious *tagged* entries.
  Each candidate should be listed for user confirmation before removal.

## Current State

- **§292 (v1.0.46): D-411 implemented - a log-scaled, recency-aware boost lets a genuinely well-used**
  **learned word compete in the live suggestion bar, without categorically favouring learned words.** User's
  own framing: not a blanket preference, but a word that crosses a real usage threshold should be able to
  rank near the front, and a word heavily used long ago should fade back rather than staying inflated
  forever. Traced the actual frequency-merge mechanism first: `SqliteDictionaryStore.entryOf()`/
  `unigramsByPrefix()` sum bundled+learned frequency raw, and that figure already feeds every
  frequency-based decision in the app (A-01, `TokenRepair`, `CorrectionConfidence`) - but
  `DictionarySuggestionProvider.score()` turned out to be the single choke point every *ranking* use passes
  through, while every correctness-affecting read bypasses it entirely (confirmed by tracing every call
  site) - so the boost could be centralised there with zero risk of touching a correctness gate. Formula:
  `REFERENCE_FREQUENCY * ln(1 + count) / ln(1 + REFERENCE_COUNT)` (log-scaled, mirroring D-353's own
  precedent for bridging two very different frequency scales), further multiplied by 1.5 when the word was
  touched within the last 14 days. Calibrated directly with the user: `REFERENCE_FREQUENCY = 5000`,
  `REFERENCE_COUNT = 50` - user's own explicit acceptance bar: "man merkt davon in der Praxis nur so
  wenig, dass halbwegs gute Werte hier schon ein sehr gutes Ergebnis liefern," i.e. exact precision was
  explicitly not the goal. New `DictionaryStore.learnedFrequencyOf()` exposes a word's learned-only
  frequency + D-388's `last_touched` timestamp separately from the merged view (implemented in both stores;
  `InMemoryDictionaryStore` gained an injectable `clock` for deterministic tests, mirroring
  `DictionarySuggestionProvider`'s new `now` parameter). New pure `LearnedFrequencyBoost` object; `score()`
  now routes frequency through a new `rankingFrequency()` that swaps the raw learned share for the boosted
  one, bundled contribution untouched. 7 new `LearnedFrequencyBoostTest` cases plus 2
  `DictionarySuggestionProviderTest` cases proving the effect end-to-end (a heavily/recently used personal
  word now outranks a moderately common bundled word; a rarely-used, long-untouched one still does not).
  1058 unit tests total (was 1049). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 349
  -> 350, `versionName` `"1.0.45"` -> `"1.0.46"`. Spec's S-01 revised. Not yet device-confirmed - the effect
  is subtle by design. See history §292.

- **§291 (v1.0.45): D-388 implemented - Learned Words/Blacklist/Credentials editors are sortable, with a**
  **new `last_touched` column; the user's own proposed pending/learned table merge was investigated and**
  **declined as too risky.** Ist-Zustand check first (per the user's own request): Learned Words was sorted
  frequency-first (not alphabetical at all), Blacklist was alphabetical but byte-order (not locale-aware -
  umlauts sorted by raw UTF-8 value, not their natural alphabetic position), neither had a sort picker, and
  "most recently used" was technically impossible - no timestamp column existed anywhere. The user's own
  assumption that pending words already lived with a timestamp inside the learned table surfaced a bigger
  fact first: `PendingLearnStore` is a wholly separate SharedPreferences counter. Their proposed fix (merge
  the two tables, filter by frequency at read time) was traced against the code and declined - `TABLE_LEARNED`
  presence is the ground-truth "is this word known" signal at 6+ call sites across the whole suggestion
  engine (including the just-shipped D-403 `learnedCasingOf` protection); replicating the threshold filter
  at every one of those sites risked reopening D-37's exact original bug. User agreed once this was laid out
  concretely - `PendingLearnStore` stays untouched. Shipped instead: a guarded, additive `last_touched`
  column on `TABLE_LEARNED` only (stamped by every write; existing rows seeded with 1-second-apart,
  alphabetically-ordered timestamps per the user's own suggestion, so a first recency view of legacy data
  isn't arbitrary); a short two-option sort picker on Learned Words only ("Recent"/"A-Z", short labels per
  explicit request, defaulting to alphabetical); `java.text.Collator`-based locale-aware alphabetical
  sorting on all three screens (the actual fix for the byte-order gap); frequency dropped from the Learned
  Words display entirely; and `DictionaryStore.learn()` gained a `seedFrequency` parameter so a freshly
  promoted word starts at however many times it was actually seen pending (2 or 4, per W-02), not always a
  flat 1. A separate, mid-discussion clarification - "frequency and recency, weighted, for chip ranking" -
  turned out to mean the *live* suggestion bar while typing, not these review screens; explicitly deferred to
  its own future design round, comparable in scope to D-353. 2 new `InMemoryDictionaryStoreTest` cases for
  `seedFrequency`; no tests for the three Activities or the SQLite migration itself (Android/`SQLiteOpenHelper`
  glue, instrumented-test territory). 1049 unit tests total (was 1047). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 348 -> 349, `versionName` `"1.0.44"` -> `"1.0.45"`. Spec's
  W-01/W-02 updated. Not yet device-confirmed. See history §291.

- **§289 (v1.0.44): D-403/D-359 finished - a revert now gets exactly one unimpeded retry, but learns**
  **nothing itself; D-358 fixed - double-tap-Backspace revert broken right after punctuation.** Three-round
  design discussion, each round simplifying the last (full write-up: history §289): started from a
  pending-blacklist-style persistent multi-day marker + immediate full learn on retry, simplified to an
  in-memory one-shot flag, then corrected once more by the user - `performAutocorrectUndo()` already calls
  `learnWord(typed)` on an ordinary revert, so an immediate full learn on the confirmed retry would silently
  double-count (+2), reaching the ordinary 2-strike threshold only by coincidence, not by design. Fixed:
  reverting now learns nothing at all (not even n-gram context - deliberately, "verhindert im Zweifelsfall,
  dass Mist gelernt wird"); a new one-shot `revertSuppressedWord` field instead lets the next matching commit
  reach the entirely ordinary `learnWord()` path in `finalizeAndCommit()` uncorrected, for one ordinary +1 -
  exactly like any other first-time word, no shortcut. `revertConfirmed` folds into the existing
  `suppressAutocorrect` variable (same architecture D-234's own toggle already reuses) plus two explicit
  guards (`diacriticWord`, the A-06 merge) that deliberately sit outside it, mirroring D-234's own precedent
  exactly. The `wasSplit` revert branch (`learnWordStrong()`) is explicitly left untouched - user's own call,
  a different, already-settled decision. D-358: root-caused directly in the code - the double-tap-undo single
  tap's "keep the window armed" check tested `isWhitespace()` on the character before the caret, but a word
  committed directly by punctuation (not a space) sets `undoDelimiter` to that punctuation mark, so the very
  first tap wrongly saw "real content" and discarded the window before a second tap could ever fire. Fixed by
  checking against the same `armedTail` construction the non-double-tap branch already uses. No new tests
  (Android IME glue, untested per convention); 1047 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 347 -> 348, `versionName` `"1.0.43"` ->
  `"1.0.44"`. **Device-confirmed.** See history §289/§290.

- **§288 (v1.0.43): D-410 implemented - a `LanguageRules` seam separates German-specific grammar from core**
  **logic; a runtime plugin-loading design was discussed and explicitly rejected.** User asked how to cleanly
  package the app's German-only behaviours - physically into the language pack (like the dictionary), or
  activated-per-language like the keyboard layout. A research pass found this is more than a data-placement
  question: several genuinely German grammar/orthography algorithms (`TokenRepair`'s inseparable-prefix and
  feminine-`-in` split vetoes, D-116/D-115/D-125/D-252's compound/verb/adjective morphology checks) ran
  **unconditionally regardless of active language** - no code branch gated them at all - and S-08's "Uhr"
  time suggestion had no language gate whatsoever. The user's own follow-up question - could the concrete
  rules implementation live inside the downloadable language-pack ZIP and be loaded at runtime
  (`DexClassLoader`) - was discussed and rejected: it would reverse this project's own deliberate
  no-`INTERNET`-permission design (language packs are inert data a human places, never executed code),
  directly conflict with the in-progress F-Droid submission's reproducible-build premise (MR #44142), and
  turn the worst case for a bad language-pack file from "parser fails" into "arbitrary code execution inside
  an IME." Chosen instead: `LanguageRules` as a **compile-time** plugin registry
  (`Map<Language, LanguageRules>`, new `language/LanguageRules.kt` + `language/GermanRules.kt`), generalising
  `LayoutRegistry`'s existing per-language pattern - German resolves to the real `GermanRules`, every other
  language to `NoOpLanguageRules`. Rewired `TokenRepair`, `DictionarySuggestionProvider`,
  `seedBundledBlacklist`, the Uhr-suggestion (**the one user-visible fix**: no longer offered while a
  non-German language is active), and `PunctuationSpaceGlue`'s decimal-comma-glue exception - all via a
  `languageRules`/`includeComma` parameter defaulting to the historical German behaviour, so every existing
  caller/test is unchanged except the few production call sites in `AdaptKeyService` that now resolve per the
  actually active language. Deliberately left ungated, with reasoning (see history §288): `Umlaut` folding
  (near-zero real leak risk, deeply embedded in the most sensitive class), `Abbreviations`/`SentenceBoundary`
  (sits inside the Auto-Caps live-arming mechanism this project has repeatedly flagged as fragile - D-405/
  D-406/D-407), `KeyboardLayout.DEFAULT_LETTER_HINTS` (a harmless last-resort fallback, not a correctness
  bug). 21 new tests in `LanguageRulesTest` plus demonstration tests in `TokenRepairTest`/
  `DictionarySuggestionProviderTest`/`PunctuationSpaceGlueTest` proving the actual fix (the same fixture data
  is protected under `GermanRules` but not under `NoOpLanguageRules`). 1047 unit tests total (was 1020).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 346 -> 347, `versionName` `"1.0.42"` ->
  `"1.0.43"`. Not yet device-confirmed - the one observable behaviour is "Uhr" no longer appearing while
  English/French/Greek is active. See history §288.

- **§286 (v1.0.42): D-409 - D-352's word-splitting slider reordered, "Off" leftmost**, matching C-22's own
  slider convention. Display-order-only change (`arrays.xml`); persisted values and `AutoSplitMode.fromKey()`
  resolution unaffected. **Device-confirmed.** See history §286/§287.

- **§285 (v1.0.41): D-408 - slider descender clipping fixed; D-352 word-splitting also converted to a**
  **slider.** The C-22 slider's value label (`labeled_seekbar_value`) had `layout_height="match_parent"`,
  which squeezed it to the `SeekBar`'s own shorter height and clipped descenders (`g`/`j` in "Vorsichtig"/
  "Aggressiv") - invisible in the original AndroidX layout this was copied from, since no digit has a
  descender. Fixed via `layout_height="wrap_content"` + `gravity="center_vertical"` moved to the row.
  Also widened the summary-to-slider gap (`layout_marginTop`) to match this screen's other, plain
  `SeekBarPreference` sliders. `d352_auto_split_mode` swapped from `ListPreference` to
  `LabeledSeekBarPreference` too (pure widget change - `AutoSplitMode`/`SettingsMapper`/`SettingsStore`
  untouched, already a genuine 3-member enum with nothing to merge). New shared `setupLabeledSlider()`
  helper in `SettingsActivity` wires both sliders' labels/values now. No test/spec changes (UI glue).
  1020 unit tests unchanged, green. `versionCode` 344 -> 345, `versionName` `"1.0.40"` -> `"1.0.41"`.
  **Device-confirmed.** See history §285/§287.

- **§282 (v1.0.39): D-406 implemented - Auto-Caps is now consistently re-derived at every position reached,**
  **closing two live-arming gaps D-405 predicted.** Real repro: period -> auto-space arms Shift -> tap back
  into the previous word to fix a typo -> first Backspace silently swallowed -> second Backspace deletes but
  Shift stays wrongly armed -> next char wrongly uppercase -> tapping back to the real sentence start then
  shows Shift wrongly off. Confirmed model (several discussion rounds): a position is a property, re-derived
  fresh on every arrival (typed, tapped, dragged, arrow-keyed) via the existing `armShiftForNextWord`/
  `sentenceStartBefore` mechanism - **except** a position reached by Backspace, where the deleted character's
  own case is the sole signal (deleted uppercase -> on, deleted lowercase -> off), unless the deleted
  character was punctuation/whitespace/a digit, in which case ordinary context derivation applies exactly as
  for every other trigger. Three gaps closed: (1) `onUpdateSelection`'s "external caret move while composing
  was still active" branch never called `armShiftForNextWord()` at all (D-313 only fixed the other,
  composing-was-empty path via `reclaimWordAtCaret()`); (2) `handleBackspace()`'s D-262 pending-auto-space
  guard trusted a flag that could go stale within `reclaimWordAtCaret()`'s own 100ms debounce window,
  silently swallowing the keystroke; (3) `applyShiftAfterDelete()` had no branch for a deleted lowercase
  letter at all (fell through, left unchanged) and the separate pre-existing D-45 fix only ever handled the
  "re-arm on" direction from one call site. Fixed by unifying `applyShiftAfterDelete()` into two cases
  (deleted letter = hard exception; anything else = `armShiftForNextWord()`, same as any other position
  arrival), which subsumes and replaces D-45's own standalone check. No new tests (Android IME glue code,
  untested per convention); 1018 unit tests unchanged, all green. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 342 -> 343, `versionName` `"1.0.38"` -> `"1.0.39"`. Spec's
  G-05 addendum rewritten, new §37 added, §35 gained a forward-pointer. **Device-confirmed.** See history
  §282/§283.

- **§284 (v1.0.40): D-407 implemented - D-234's boolean "Autocorrect" toggle and D-353's three-level**
  **"Autocorrect confidence" list merged into one C-22 slider (Off / Cautious / Medium / Aggressive).** New
  `LabeledSeekBarPreference` (a discrete, string-valued slider, persists like `ListPreference` does; not a
  `SeekBarPreference` subclass, which owns its seek listener/state privately) shows the selected level's
  name beside the thumb, not a raw number - its own layout widens the value label to 88dp (fits
  "Vorsichtig"/"Aggressive") and lets the `SeekBar` (`layout_weight="1"`) shrink automatically to whatever
  width remains. `AutocorrectAggressiveness` itself is unchanged (still exactly 3 real levels) - "Off" is
  not a 4th level, it's `AdaptSettings.autocorrectEnabled` (D-234) now derived from the same one stored
  value via a new `AutocorrectAggressiveness.OFF_KEY` constant; `fromKey("off")` already fell back to
  `DEFAULT` (Medium) as an unrecognised key, which is exactly the desired behaviour - suggestions/chips keep
  ranking sensibly while autocorrect is off, matching the pre-merge behaviour where the boolean never
  affected the suggestion-bar confidence gate at all. No migration of old stored values (explicit request).
  3 new/changed tests. 1020 unit tests total (was 1018). `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `versionCode` 343 -> 344, `versionName` `"1.0.39"` -> `"1.0.40"`. Spec §20/§36 updated.
  **Device-confirmed** (together with D-408/D-409, §285/§286). See history §284/§287.

- **§281 (v1.0.38): D-353/D-354 implemented - a unified `CorrectionConfidence` score replaces autocorrect's**
  **ad hoc gates.** New pure `CorrectionConfidence` object (`dictionary` package): for an unknown typed token,
  `costFactor × frequencyFactor` (frequency reference 25 for a non-noun candidate, 2,000 for a noun-tagged
  one - a two-tier reference, not a flat noun penalty, so a common noun like "Jahren" isn't punished like a
  rare proper-noun artefact like "Virgin"); for a known typed token (A-01 override), a log-scaled frequency-
  ratio score reaching 1.0 at 500x, replacing D-244's flat 100x cutoff. D-354: a correction that changes the
  typed token's own recognisable leading German prefix (`aberkennen` -> `anerkennen`) has its score capped at
  0.55 - below every level's auto-apply threshold, above every level's chip-offer threshold, so it's never
  silently applied but always still offered as a suggestion. New `AutocorrectAggressiveness` enum (C-22,
  `d353_autocorrect_aggressiveness`), 3 levels each with an auto-apply/chip-offer threshold pair: CAUTIOUS
  (0.90/0.40), MEDIUM (0.75/0.30, default, reproduces pre-D-353 behaviour exactly), AGGRESSIVE (0.70/0.20).
  Every level's auto threshold sits strictly above the confirmed-bad "Ohren"/"Ihren" regression's own score
  (~0.68) - a design decision explicitly requested mid-discussion: a more aggressive setting may only ever
  admit more of the untested grey zone, never reopen an already-confirmed false positive; a standing test
  guards this against AGGRESSIVE (the most permissive level) directly. `DictionarySuggestionProvider`'s old
  `minAutocorrectFrequency` constructor parameter (opt-in, defaulted to no floor) is gone - the confidence
  gate is now always active, at the configured aggressiveness. `fuzzyNeighbours()` (D-12 suggestion path,
  previously unfiltered) now also requires the chip-offer threshold. 26 new/changed tests, 1018 unit tests
  total (was 992). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 341 -> 342,
  `versionName` `"1.0.37"` -> `"1.0.38"`. Spec A-01/§20/new §36 revised. Not yet device-confirmed. See
  history §281.

- **§280 (v1.0.37): D-403/D-352 implemented together.** D-403: `DictionarySuggestionProvider.
  shouldOverrideKnownWord()` now returns `false` outright whenever `store.learnedCasingOf(word) != null` -
  a learned word (fully self-taught, or a deliberately different-cased W-04 override) can no longer be
  silently corrected away by the 100x-ratio check (D-244), which was only ever calibrated against genuine
  bundled-corpus rarity - a freshly-promoted word's own frequency equals exactly its reinforcement count
  (starts at 1), so it was previously defenceless against almost any ordinary cost-1-adjacent word. D-352:
  new `AutoSplitMode` enum (AUTOMATIC/CHIP_ONLY/OFF, mirrors `LlmActivationThreshold`) wired through the full
  settings pipeline as C-21 (`d352_auto_split_mode`, Correction & Suggestions category, right after the D-234
  autocorrect toggle); gates A-05's two `trySplit()` call sites in `finalizeAndCommit()` (ordinary + the
  G-05/D-263 case-locked path) to AUTOMATIC only, and reuses the existing D-238 split-suggestion-chip
  mechanism (`composingPreviewRunnable`'s `needsSplit`/`refreshSuggestions()`'s `autocorrectSplitChip`) so
  CHIP_ONLY offers the same chip independent of the global autocorrect toggle, and OFF suppresses both the
  chip and the live split-colour preview entirely. Deliberately scoped to A-05 only - A-06 merge and D-122's
  mid-word connector-split suggestion are untouched. 8 new tests (3 `DictionarySuggestionProviderTest` for
  D-403, 2 `SettingsMapperTest` + 3 `AutoSplitModeTest` for D-352). 992 unit tests total (was 984).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 340 -> 341, `versionName` `"1.0.36"` ->
  `"1.0.37"`. Spec A-01/A-05/§20 revised. Not yet device-confirmed. See history §280.

- **§278 (v1.0.36): D-405 implemented - sentence-start capitalisation is now a purely live typing aid,**
  **never re-applied as a commit-time override.** `CapitalisationEngine.capitalise()` dropped its standalone
  `context.sentenceStart -> true` branch; `explicitFirstUpper` alone now decides both directions symmetrically
  (an explicit lower-case choice, e.g. a deliberate Shift-disarm of the pre-armed capital, is no longer
  silently overwritten at commit). A substantial inline comment documents the finding and points any future
  regression at the live-arming path (`armShiftForNextWord`/`ShiftGrace`, D-45/D-313/D-335) rather than at
  reinstating the removed branch - the user's own explicit request, so a partial reintroduction stays cheap
  if the accepted trade-off (a live-arming gap now surfaces directly instead of being masked) ever proves
  wrong. `CapitalisationContext.sentenceStart` is kept on the data class, deliberately unused inside
  `capitalise()` itself, for the same reason. Spec §6/§35/S-06/W-04 updated to match. 1 existing test
  corrected in place (not removed), 984 unit tests (unchanged). `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `versionCode` 339 -> 340, `versionName` `"1.0.35"` -> `"1.0.36"`. **Device-confirmed (2026-08-28)** -
  the user's first re-test used `"test"`, coincidentally also a pure-`NOUN`-tagged word (§6 rule 3, unrelated
  to D-405) that looked unchanged for a different reason; re-tested with a non-noun word and confirmed
  working. See history §278/§279.

- **§277 (still v1.0.35, no code change): design discussion on D-352-D-356/D-403 - a real, unified**
  **confidence metric agreed as the direction (two thresholds: auto-apply / chip-offer, tied to a new**
  **three-level autocorrect-aggressiveness setting), plus several concrete findings - nothing implemented**
  **yet.** Root-caused directly against the code: there is no unified confidence score today, only
  independent boolean gates (`MIN_AUTOCORRECT_CANDIDATE_FREQUENCY`/`ADJACENT_SUB_COST`/
  `KNOWN_WORD_OVERRIDE_RATIO`) - explains why autocorrect fires too eagerly (D-353). D-354's prefix idea
  refined from "known prefix + known stem = fully protected" to a softer confidence-reducing signal
  ("a prefix-changing edit where a plausible German prefix was recognisable beforehand" - feeds the metric,
  not a separate gate); `"aberkennen"` deferred into D-402's own dictionary-cleanup word list rather than
  fixed alone. D-403's `shouldOverrideKnownWord`-vs-learned-words fix agreed (a freshly-learned word's
  frequency literally equals its reinforcement count, D-403's own root cause, confirmed in `learn()`); its
  "dual-casing chip" idea withdrawn as a bad fit for its own `"Weg"`/`"weg"` example and redirected to a
  corrected D-368 (case-neutral entries tagged with a POS *combination*, accepted/suggested per typed
  casing - D-368's original capture wrongly framed this as cross-language, fixed in place). D-352's
  three-way setting (Auto/Chip-only/Off) confirmed as specified, implementation-ready; its "non-word split
  half" failure mode is now root-caused against real dictionary data (`"en"`=1207!, `"ell"`=16, `"lich"`=16,
  `"ische"`=15 - all pass A-05's "resolves in the dictionary" gate despite being fragments, not words) -
  folded into D-402's own list so these specific entries are not missed in the cleanup pass. **D-355's own
  supplied log turned out to demonstrate a different, previously unnumbered bug - captured fresh as D-405**
  (sentence/line-start auto-capitalisation must only ever be a *live* typing aid, never re-applied as a
  commit-time override once a token has actually been typed/explicitly disarmed via Shift) - D-355 itself
  stays open and separate, per the user's own explicit instruction not to conflate the two; D-356 also still
  open, awaiting an example. A real trade-off was flagged for D-405, not yet confirmed with the user: removing
  the commit-time `sentenceStart` override in `CapitalisationEngine` means any future gap in live Shift-arming
  coverage (a class of bug this project has hit several times before - D-45/D-313/D-335) would surface
  directly instead of being silently masked. Full write-up: history §277. No code changed, no version bump.
  984 unit tests (unchanged). **Awaiting the user's go before any implementation.**

- **§276 (still v1.0.35, no code change): large backlog batch from real-world vacation usage - D-352 through**
  **D-404, captured only, nothing designed or implemented.** User returned from a trip with an extensive,
  unfiltered list of bugs/ideas found while actually using the app; explicitly asked for a pure backlog
  capture this round ("nimm diese zunächst nur ins Backlog auf") - no tracing, no root-causing, no
  implementation. Roughly 50 items across: autocorrect being too eager / too silent / reverting-then-
  re-firing (D-353/D-354/D-355/D-356/D-359/D-403's own acronym-learning cluster); A-05 split quality and a
  settings split for auto-vs-chip-only (D-352, explicitly to be re-checked **before** any dictionary cleanup
  so the effect isn't masked - D-403 is the matching cleanup ask); several Shift/Backspace/A-07/A-12 edge
  cases (D-357/D-358/D-360/D-361/D-378/D-392); suggestion-bar/ranking issues (D-364/D-365/D-366/D-367/D-369/
  D-370/D-376/D-380/D-382/D-383); a fully-specified new feature concept (D-401, a long-press-space cursor/
  selection mode); a large German-dictionary noise/missing-word list (D-402); and settings/UX asks (touch-zone
  bleed D-397, gesture-bar distance D-395, language-switch slider D-398, numpad mirror D-394, vibration
  levels D-396, layout-follows-system-locale D-400, and more). Full itemised list with the user's own
  reasoning preserved per item: history §276. No code changed, no version bump. 984 unit tests (unchanged).
  **Not yet triaged into an implementation order** - the user will pick items from this batch next.

- **New backlog items (2026-08-01) — not yet designed or implemented, captured for discussion:**
  D-342 (German dict NOUN_OR_VERB rework), D-343 (Caps Lock vibration direct Vibrator + subtle),
  D-344 (download directory control), D-345 (dictionary noise scan / "Bri" blacklist). See spec §28-§31.
  (D-343 may already be covered by §264's Caps Lock haptics work - not re-checked against this note,
  flagged so a future pass verifies rather than assumes either way.)

- **§273 (v1.0.34): D-350 - reactively reclaiming every intermediate drag position stalled the Gemini**
  **field's own cursor-handle tracking; the D-62 reclaim-on-caret-move is now debounced (100ms).** After
  §272's corruption fix, a new device log showed the nub freezing mid-drag - traced to
  `reclaimWordAtCaret()` firing synchronously on every intermediate drag position and calling
  `ic.setComposingRegion()` whenever it landed near a word (never when it landed somewhere with nothing to
  reclaim, e.g. right after a period - matching the user's own "stable at the end, unstable elsewhere"
  observation exactly). Fixed by debouncing via the same `handler.postDelayed`/`removeCallbacks` shape
  already used for `composingPreviewRunnable`/`expensiveSuggestionRunnable`, at the user's own explicit
  100ms (shorter than the usual 200ms, since a drag moves faster than typing) - only the position still
  current once the caret has been still that long is ever reclaimed. `reclaimWordAtCaret()` gained a
  `composing.isNotEmpty()` guard (state may have changed during the delay); `clearComposing()` cancels the
  pending callback so it can never fire into a later field session. No new tests. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 337 → 338, `versionName` `"1.0.33"` → `"1.0.34"`.
  **Device-confirmed as a real improvement** (the nub no longer freezes for long stretches while dragging)
  **but not sufficient alone** - see §274, the very next round. See history §273, spec §33.

- **§274 (v1.0.35): D-351 - the debounce alone wasn't enough; any `setComposingRegion()` call stops**
  **Gemini's drag dead regardless of timing, so the reactive D-62 reclaim is now suppressed entirely for**
  **that one field, by package name.** The user's own precise report: the moment the debounced reclaim
  actually fires (the caret briefly settles), the handle disappears and the drag ends right there - a
  longer delay only postpones this, since the IME has no signal at all for "has the finger actually
  lifted". Two options discussed: (A, chosen) suppress the reactive caret-move reclaim only for
  `com.google.android.googlequicksearchbox` (no other app has shown this in this project's own extensive
  testing history, and no structural `EditorInfo` signal exists to detect it generally); (B, declined) drop
  the reactive reclaim mechanism app-wide. New `reclaimOnCaretMoveSuppressed` flag, set fresh per field in
  `onStartInput()`, mirrors the existing `urlMode`/`noSuggestionsField` pattern; typing-triggered reclaim
  (mid-word live correction once a character is actually typed) is unaffected everywhere, including
  Gemini. **Explicitly recorded as a special case the user is monitoring in real-world use, not a settled,
  final answer** - see spec §33. No new tests. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 338 → 339, `versionName` `"1.0.34"` → `"1.0.35"`. **Device-confirmed (§275): "Perfekt! Jetzt
  verhält sich das in Gemini korrekt."** Closes the whole D-347/D-350/D-351 nub saga. See history §274/§275.

- **§272 (v1.0.33): D-347 fix v2 - `before`/`after` still non-atomic even read back-to-back; derived from**
  **the single `getExtractedText()` call instead.** §271's fix (narrowing the gap between two separate
  `getTextBeforeCursor()`/`getTextAfterCursor()` calls) was disproven by a second real device log showing
  the identical corruption (`before="Tes" after="st"` → `"Tesst"`) - two calls, however close in source,
  are still two independent Binder round-trips a fast drag can move through. Fixed structurally:
  `reclaimSurroundingWord()` no longer calls `getTextBeforeCursor`/`getTextAfterCursor` at all - both
  fragments are now sliced from the single `getExtractedText()` call already made for the anchor (its own
  `.text` at its own `.selectionStart`/`.selectionEnd`), so all three values provably share one atomic
  snapshot. No new tests. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 336 → 337,
  `versionName` `"1.0.32"` → `"1.0.33"`. **Device-confirmed: the corruption is gone, the nub can be
  dragged.** A separate, distinct symptom - the nub freezing mid-drag - surfaced in the same round; see
  §273. The original flicker/disappear symptom D-347 was first reported for is still separately
  unconfirmed. See history §272, spec §33.

- **§271 (v1.0.32, superseded by §272): D-347 fix v1 - narrowed but did not close the before/after race.**
  See history §271, spec §33.

- **§270 (v1.0.31): D-348 fix v4 - correct design per user spec.** First tap is now an ordinary
  single-character delete (the delimiter/space), NOT a no-op — matching the user's explicit spec:
  single Backspace deletes the delimiter, double-tap reverts. Undo window stays armed while deleting
  whitespace (delimiter or extra spaces), clears once non-whitespace is reached. Second tap (within
  window) finishes composing first (prevents duplication caused by an active composing span making
  commitText re-insert the committed word), then calls performAutocorrectUndo with allowConsumedDelimiter
  (delimiter was deleted by the first tap). Removed the unused flashKey method. Root cause of all three
  prior bugs: (1) v1.0.27 first tap was no-op for any non-armed-tail position; (2) v1.0.28/29
  allowConsumedDelimiter path duplicated because an active composing span (reclaimed between taps) made
  commitText re-insert the committed word; (3) v1.0.30 first tap was pure no-op (never deleted).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 334 → 335, `versionName`
  `"1.0.30"` → `"1.0.31"`. **Not yet device-confirmed** - see history §270.

- **§269 (v1.0.30): D-348 fix v3 - double-tap duplicated the reverted word.** Root cause: the
  `allowConsumedDelimiter` mechanism (added in v1.0.28 to tolerate the first tap deleting the
  delimiter) was fundamentally wrong — the first tap's deletion of the delimiter broke the
  ground-truth check, and the `allowConsumedDelimiter` fallback path then deleted/committed with
  wrong offsets, causing the typed word to be inserted without the committed word being fully
  removed. Fix: removed `allowConsumedDelimiter` entirely; reverted `performAutocorrectUndo` to its
  original form. The first tap at the armed tail is now a pure no-op+flash that NEVER touches the
  delimiter, so the second tap's `performAutocorrectUndo` ground-truth check always matches. Trailing
  whitespace *beyond* the armed tail is still consumed ordinarily by the first tap; anywhere else,
  `clearUndo()` + ordinary delete. No new tests (Android-glue path).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 333 → 334, `versionName`
  `"1.0.29"` → `"1.0.30"`. **Not yet device-confirmed** - see history §269.

- **§268 (v1.0.29): D-348 fix v2 - double-tap revert stopped working after the v1.0.28 fix.** Root
  cause: the `atCommitted` check used `getTextBeforeCursor(undoCommitted.length)`, but when the
  undoDelimiter (space) was still present, the caret sat after `"Vom "` and the last 3 chars were
  `"om "` ≠ `"Vom"` — so `atCommitted` was false, the code cleared the undo window and deleted the
  space, and the second tap never saw a double-tap. Fix: `atArmedTail` now checks both the full tail
  (`undoCommitted + undoDelimiter`) and the bare `undoCommitted` (delimiter already consumed), so the
  no-op/flash path correctly recognises both positions. No new tests (Android-glue path).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 332 → 333, `versionName`
  `"1.0.28"` → `"1.0.29"`. **Not yet device-confirmed** - see history §268.

- **§267 (v1.0.28): D-348 fix - single Backspace was ignored several times after an autocorrect commit**
  when the double-tap-undo option was on. Root cause: the first-tap no-op path flashed the key for
  *any* Backspace while `undoTyped` was armed and the caret was not at the armed tail, instead of only
  when the caret actually sat at `undoCommitted`. Once the user moved the caret elsewhere, every
  subsequent Backspace was silently swallowed. Fix: no-op+flash now fires only when the caret is
  directly adjacent to `undoCommitted`; anywhere else, `clearUndo()` + ordinary `handleBackspace` runs.
  Also taught `performAutocorrectUndo` an `allowConsumedDelimiter` parameter so the double-tap revert
  still works after the first tap consumed the whitespace delimiter. No new tests (Android-glue path).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 331 → 332, `versionName`
  `"1.0.27"` → `"1.0.28"`. **Not yet device-confirmed** - see history §267.

- **Device-confirmed (2026-08-01): D-340 (backspace slide-off), D-341 (`à`/`À` popup).**

- **§266 (v1.0.27): D-340/D-341/D-346/D-348 - four backlog items shipped together.** (1) D-340:**
  **backspace hold continues after slide-off - `cancelBackspaceRepeat` gated on `!backspaceRepeated`,**
  **so only a swipe before the first tick cancels; once the hold is active it runs until finger lift.**
  **(2) D-341: `à`/`À` appended to the `a` key's popup alternatives (after `ä`/`æ`/`å`); uppercase via**
  **the existing case machinery, no per-entry data needed. 2 layout tests updated. (3) D-346: "…" loading**
  **indicator in the suggestion bar when the deferred fuzzy search is pending and the bar would otherwise**
  **be empty - new `expensiveSuggestionPending` flag + `Kind.LOADING` chip, grey italic, non-tappable.**
  **(4) D-348: optional double-tap Backspace for A-07 revert (setting C-20, default off, reuses**
  **`doubleTapDelayMs`); first tap at armed tail = no-op + key flash, second tap = revert; trailing**
  **whitespace still consumed ordinarily. 1 new `SettingsMapperTest`. `:app:assembleRelease`/**
  **`:app:testDebugUnitTest` green. `versionCode` 330 → 331, `versionName` `"1.0.26"` → `"1.0.27"`.**
  **Not yet device-confirmed** - see history §266.

- **§265 (v1.0.26): D-339 - repeated double-tap word toggle did not work (composing emptied after first**
  **toggle).** Root cause: `toggleWordStartImmediate` called `finalizeAndCommit(ic, "")`, which committed
  the word and emptied `composing` — the second double-tap found `composing` empty and did nothing. Fix:
  replaced `finalizeAndCommit` with `updateComposing(ic)` — the flipped text is immediately visible as
  composing, and the token stays composing so a further double-tap toggles it again (upper → lower →
  upper …). `composingCaseLocked = true` still ensures verbatim commit on the next delimiter/letter (no
  autocorrect, no camelCase). The `onUpdateSelection` ownEdit check recognises the `setComposingText` as
  the IME's own edit and does not reset `composingCaseLocked`. No new tests (Android-glue path).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 329 → 330, `versionName` `"1.0.25"`
  → `"1.0.26"`. **Not yet device-confirmed** — needs a repeated double-tap on the same word (upper → lower
  → upper) mid-word and at word end. See history §265.
- **§264 (v1.0.25): Shift-Handling Redesign — non-competing intents.** Three Shift intents that formerly**
  **competed in a single `handleShift()` method are now cleanly separated by input modality.** (1) **Caps Lock
  via long-press** (G-06): Shift gained a long-press action (`KeyboardLayout.hasLongPressAction` now includes
  `KeyCode.SHIFT`; `handleLongPress` sets `capsLock = true`). Replaces the former double-tap-for-Caps-Lock
  (D-15/D-312). D-337's border highlight still fires on the same `capsLock` flag, now engaged via long-press.
  (2) **Word-start toggle via double-tap** (G-05): two Shift presses within the configurable
  `doubleTapDelayMs` toggle the current word's first-character case and immediately commit verbatim — no
  provisional state, no camelCase continuation. Works regardless of caret position. The entire provisional-
  state machine (`wordEndShiftPending`, `handleWordEndShift`, `resolvePendingWordEndShift`, `nextKeyClass`,
  `WordEndShift.NextKey`/`Resolution`/`resolveNextKey`) was removed. (3) **Single tap = next-letter case**
  (ordinary toggle, C-07 grace guard unchanged). Two new settings: **double-tap Shift delay** (200-800 ms,
  default 400, Layout category below long-press delay) and **Caps Lock vibration** (default on, Feedback
  category below D-06, independent of per-key haptics, uses direct `Vibrator` path). `DOUBLE_TAP_SHIFT_MS`
  hardcoded constant removed.   985 → 983 unit tests (4 `WordEndShift` resolution tests dropped, `flipFirst` tests retained, 2 new
  `SettingsMapper` tests for `doubleTapDelayMs` clamping + `capsLockHapticsEnabled`). `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's G-05 and G-06 rewritten.
  `versionCode` 328 → 329, `versionName` `"1.0.24"` → `"1.0.25"`. **Not yet device-confirmed** — (a) long-press
  Shift engages Caps Lock + vibration; (b) double-tap Shift toggles word-start case + immediate commit;
  (c) single-tap Shift arms next-letter case normally; (d) Caps Lock border highlight still shows. See history
  §264.
- **§262/§263 (v1.0.24): D-336 extended long-press popups on a/e/n + D-337 Caps-Lock border highlight.** Two**
  **features shipped together.** (1) D-336: the German QWERTZ layout (and AZERTY, which reuses the same
  letter-key builder) gained additional long-press popup alternatives on three letters: `æ`/`å` on `a`
  (after `ä`), `é`/`è`/`ê`/`ë` on `e` (after `€`), `ñ` on `n` (after `+`). Upper-case forms apply
  automatically via the existing case machinery (`popupDisplayTextFor`/`appendLongPressLetter`). The former
  top-row-only `topRowKey` was generalised into a row-agnostic `letterKey` (same (char, hint) → alternatives
  decision + weight param) so letters in any row gain popups; `topRowKey` stays as a weight-defaulted
  delegate. AZERTY's middle/third rows switched from bare `charKey` to `letterKey`. 9 new tests (976 → 985).
  (2) D-337: while Caps Lock is engaged, the Shift key draws a bold 3dp stroked border in accent blue
  (`#1565C0`) — Option A (user-chosen), visually distinct from the light-blue momentary press flash
  (`#A6C8FF`). Overlay stroke only, no size/position change. No new tests (Canvas drawing path). Spec's §24
  (D-336) and §25 (D-337) added. ``:app:assembleRelease``/`:app:testDebugUnitTest` green. ``versionCode``
  326 → 328, ``versionName`` ``"1.0.22"`` → ``"1.0.24"``. **Not yet device-confirmed** — (a) long-press a/e/n
  shows the new popups, upper-case forms work under Shift/Caps Lock; (b) Caps Lock shows the border,
  distinct from a momentary Shift press. See history §262/§263.
- **§261 (v1.0.22): D-335 - Shift not re-armed after deleting a capital (regression after D-313).** Typing an**
  **uppercase letter then Backspace-deleting it should leave Shift armed (G-05 addendum), but the next**
  **keystroke arrived lowercase.** Root-caused from a real device log: ``applyShiftAfterDelete('A')`` correctly
  sets ``keyboardView.shifted = true``, but the immediately following ``onUpdateSelection`` (composing now
  empty) calls ``reclaimWordAtCaret()``, where D-313's ``armShiftForNextWord(ic)`` re-derives Shift from
  ``sentenceStartBefore(ic)`` - and the caret where the capital just was is not a sentence start, so
  ``ShiftGrace.autoArmAtWordStart(capsMode, false)`` returns false, un-arming the Shift that
  ``applyShiftAfterDelete`` had just armed. D-313's addition was correct for its own case (genuine
  tap-into-word caret move) but ``onUpdateSelection`` cannot distinguish that from a backspace that just
  emptied composing - both arrive as a collapsed caret with ``composing`` empty. Fix: one-shot
  ``shiftArmedByDelete`` flag set by ``applyShiftAfterDelete`` when the deleted character was uppercase,
  consumed by ``reclaimWordAtCaret`` to skip its own ``armShiftForNextWord`` derivation for that one reclaim
  only; a subsequent genuine caret move re-derives Shift normally (D-313 untouched). Safety-net clear at top of
  ``handleKey`` for the case no reclaim fires. No new tests (Android-glue path). 976 unit tests (unchanged).
  ``:app:assembleRelease``/`:app:testDebugUnitTest` green. Spec's §23 added. ``versionCode`` 325 → 326,
  ``versionName`` ``"1.0.21"`` → ``"1.0.22"``. **Not yet device-confirmed** - needs: type uppercase letter,
  Backspace it away, confirm next letter is still uppercase. See history §261.
- **§260 (v1.0.21): D-334 - language pack update wiped learned words + stale "update available" hint never**
  **cleared.** Two problems reported after the D-330 ``deine``/``seine`` dictionary fix shipped, both traced
  from real evidence. (1) The language-packs screen showed "update available" for German (catalog 5 vs
  installed 3); importing the downloaded archive reported "already current" but the hint reappeared on every
  rebuild - a dead loop. Root cause: the hosted ``language-packs/adaptkey-lang-de.zip`` was never re-pushed
  after D-329/D-330 (live download confirmed it still reads ``version.txt = 3`` while repo source and catalog
  are both 5), AND no path in ``LanguagePacksActivity.importPack`` ever raised ``installedVersion`` to match
  the catalog on a skipped (stale) import, so the hint could never clear. (2) The attempted update silently
  reset the user's entire German learned-word overlay. Root cause: ``importPack`` called
  ``deleteDatabase(DictionaryLoader.databaseName(language))`` before recording the install - on every real
  pack update this deleted the entire SQLite DB including ``TABLE_LEARNED``/``TABLE_LEARNED_BIGRAMS``/
  ``TABLE_LEARNED_TRIGRAMS``/``TABLE_BLACKLIST``/``TABLE_PENDING_BLACKLIST``; the bundled-language path
  (D-178's ``resetBundledWords`` + reseed, learned overlay untouched) was never extended to installed
  languages. Fix: (1) removed ``deleteDatabase``; added ``installed_pack_version`` meta row to
  ``SqliteDictionaryStore`` (mirrors ``bundled_version``, no DB-version bump); ``DictionaryLoader.loadStores``
  now reseeds an installed language the same way it reseeds a bundled one - ``resetBundledWords()`` + ``seed()``
  (seeded tables only) when ``installedVersion`` moves past the recorded ``installed_pack_version``, learned
  overlay untouched. (2) Added ``suppressedCatalogVersion``/``suppressCatalogVersion`` to
  ``InstalledLanguagesStore``; on a skipped (stale) import the catalog version is recorded as suppressed;
  ``buildRow`` shows "update available" only when ``installedVersion < entry.version`` AND
  ``suppressedCatalogVersion < entry.version`` - clears after a stale check, re-arms when a future app release
  raises the catalog. ``installedVersion`` stays the real archive version (never faked), so a later genuine
  same-version update is still correctly accepted. (3) ``language-packs/adaptkey-lang-de.zip`` rebuilt from
  current ``dictionaries/de/`` (now ``version.txt = 5``, D-330/D-329 fixes inside). 8 new tests (968 → 976):
  5 ``InstalledLanguagesStoreTest``, 3 ``SqliteDictionaryStoreRoboTest``. Spec's §22 added.
  ``:app:assembleRelease``/``:app:testDebugUnitTest`` green. ``versionCode`` 324 → 325, ``versionName``
  ``"1.0.20"`` → ``"1.0.21"``. **Not yet device-confirmed** - needs a real German pack re-import to confirm
  (a) learned words survive, (b) the "update available" hint clears after a stale check. **The rebuilt
  ``.zip`` still needs to be pushed to ``origin/main``** before the hosted raw-GitHub URL serves version 5 -
  that push is the user's own action. See history §260.

## Older Rounds (§1-§259, v0.7.6 through v1.0.20) - Pruned From This File

This file only tracks the current status plus the recent working set - it is not a lossy summary of the
rounds removed below. Every pruned round's full detail (root cause, rejected alternatives, real device-log
evidence, exact reasoning) is permanently preserved, unabridged, in
[`AdaptKey-History.md`](AdaptKey-History.md) under the matching `§N` heading - search there directly (e.g.
for `## §142` or a `D-244` reference). History.md is explicitly append-only and never rewritten, so nothing
there has drifted from what actually shipped; nothing was condensed or paraphrased in the pruning that
produced this note.

The still-genuinely-open items from that whole span are not lost by this pruning - they already live
elsewhere: see "Open TODOs / Known Limitations" and "Reserve Ideas" above for the handful of confirmed-
still-relevant gaps, and [`AdaptKey-Spec.md`](AdaptKey-Spec.md) for the current, crystallised feature set
those 259 rounds eventually produced. A round below §260 that is still unconfirmed on-device and not
otherwise tracked in those sections should be treated as abandoned/superseded, not as silently-still-pending
work - if genuinely in doubt about one specific item, check its own `§N` entry in History.md rather than
assuming either way.

Retention policy for this section going forward: keep the ~20-25 most recent rounds (enough for a fresh
session's own continuity) plus anything not yet device-confirmed; prune older, already-confirmed rounds the
same way, without summarising them - they stay permanently retrievable in History.md.

## Done

### §15 D-37 less-eager learning (v0.7.20)
- **Count-based promotion:** new `dictionary/PendingLearnStore` (own private SharedPreferences file — no
  SQLite migration). `learnWord` now reinforces a word already in the dictionary immediately, but a
  genuinely **new** word is only counted up and promoted to the learned lexicon after `LEARN_THRESHOLD` (=2)
  commits — so a one-off typo (e.g. "asdf") is no longer learned as a real word on the first accept.
- **Un-learn on undo:** an A-07 autocorrect-undo decrements the rejected correction's pending count and
  counts up the word the user insisted on (promoted after repeated insistence). A **split**-undo (D-13) still
  promotes the rejoined word **immediately** via the new `learnWordStrong` (a deliberate correction is
  authoritative — so "Backspace" is still trained in one undo).
- Robolectric `PendingLearnStoreRoboTest` covers the increment/decrement/clear IO.
- **Only §15 item left: D-39** (raw-coordinate per-character correction) — a larger architectural piece.

### §15 D-36 direct paste + D-40 digit-in-word (v0.7.19)
- **D-36 direct paste:** when a field opens and the clipboard holds text, a 📋 chip appears in the suggestion
  bar (`SuggestionController.Kind.CLIPBOARD`); tapping it runs the **exact system paste**
  (`ic.performContextMenuAction(android.R.id.paste)`), then **clears the clipboard** (esp. passwords). Sensitive
  content is masked (`ClipDescription.EXTRA_IS_SENSITIVE`, API 33+) — shown as bullets, never revealed. Pure
  `suggestion/ClipboardPreview` (mask/truncate/collapse, unit-tested); typing replaces the chip with normal
  suggestions.
- **D-40 digit-in-word:** a digit typed between letters (composing non-empty) now stays in the token instead
  of delimiting, so existing correction fixes it — `W8rt` → `Wort` (the digit is one substitution from the
  neighbouring letter). A leading/standalone digit keeps its normal behaviour.
- **Rescoped from D-38 note:** the user pushed back — `W8rt` IS handled now (D-40), and `Stabdsrx` (multi-typo)
  is captured as **D-39** (raw-coordinate per-character correction, walking each char's retained raw tap to the
  intended neighbour key; very garbled cases may go to the tier-3 LLM). D-39 still open.

### §15 D-38 correction quality: first-char / umlaut-initial / cost-ranked (v0.7.18)
- **First-char + umlaut-initial candidates:** `DictionaryStore.correctionCandidates(token, firstChars)` (new
  overload; SQLite searches one indexed bucket per first char) + provider `candidateFirstChars` = the token's
  own initial letter plus its keyboard neighbours and, for a/o/u, the umlaut variant. So a first-key typo
  (`eerden`→`werden`) and a missing initial umlaut (`Uberblick`→`Überblick`) are now reachable.
- **Cost-ranked autocorrect:** `autocorrectFor` now ranks by lowest proximity-weighted edit cost first,
  frequency only as a tiebreak (`correctionCost` extracted from `isCloseMatch`), so `dasy`→`dass` (one
  adjacent edit) beats the far more frequent `das` (a deletion).
- Still open in §15: **D-36** direct paste (feature), **D-37** less-eager count-based learning + un-learn on
  undo. Heavily-garbled inputs (`Stabdsrx`→`Standard`) and digit-in-word (`W8rt`→`Wort`, a tokenisation
  issue) remain beyond the current budget/scope — noted.

### §15 round-4 bugs: D-30…D-35 (v0.7.17)
- **D-30 (critical bug) keyboard freeze after held backspace:** `backspaceRepeated` was only reset on a
  DELETE press, so after a hold it stayed true and the ACTION_UP tap-guard (`!backspaceRepeated`) swallowed
  **every** subsequent tap (incl. backspace). Fixed by resetting it on every ACTION_DOWN. (This was the real
  cause of the earlier "last word of the line" report — the whole keyboard was frozen.)
- **D-31 backspace-hold speed:** `BackspaceRepeat` reworked — char-wise now starts at 190 ms and accelerates
  only **moderately** (factor 0.90) to a 65 ms floor; word-wise runs at a clearly **slower** fixed 330 ms.
  The service drives the cadence: `onBackspaceRepeat(step)` now returns the next delay, computed from the
  running deletion count, so the char→word transition is a visible slow-down.
- **D-32 long-press delay + setting:** the view's `longPressDelayMs` is settable (default 80 % of the system
  timeout = ~20 % shorter). New `d32_longpress_delay_ms` slider (150–600, default 320) through the settings
  pipeline + strings ×3.
- **D-33 popup primary bottom-aligned:** the offset primary cell now sits level with the bottom of the
  secondary column (near the finger) instead of top-left.
- **D-34 vibration:** the haptic pulse was 18 ms (imperceptible) → 40 ms; VIBRATE is a normal auto-granted
  permission (no runtime step). Now actually felt.
- **D-35 swipe thresholds:** raised markedly — field gestures (dismiss-down / page swipe / word-delete)
  64→**110 dp**, space-bar language swipe 28→**44 dp**, so a faint motion no longer triggers them.

### Nice-to-haves: persist language / Greek diaeresis / language-aware blacklist (v0.7.16)
- **Persist active language (G-01):** new `language/ActiveLanguageStore` (own private prefs file); the
  service loads it in `onCreate` and saves it on each `toggleLanguage`, so the chosen alphabet
  (German/Greek) survives a service restart.
- **Greek diaeresis (ϊ / ϋ):** now that the long-press popup is multi-alternative (D-23), the ι and υ keys
  carry `[tonos, diaeresis]` (`GreekLayout.DIAERESIS`, new `letterKey` helper): tonos is the pre-selected
  primary, diaeresis the cell above. Other vowels keep the single tonos.
- **Language-aware blacklist editor (C-05):** the editor opened the legacy `adaptkey_dictionary.db` instead
  of the per-language DBs the keyboard actually uses — so edits had no effect. Now a language selector
  (DE/EN/EL) reopens the matching store via the now-public `DictionaryLoader.databaseName(language)`, so the
  blacklist is edited in the store the keyboard reads.

### §13 finish: D-13 / D-18 / D-17 / D-16 (v0.7.15)
- **D-13 user word training:** undoing a wrong A-05 split (the A-07 backspace) now **learns** the rejoined
  word (`undoWasSplit` flag → `learnWord` in `performAutocorrectUndo`), so a real word the splitter mangled
  (e.g. "Backspace" → "Back Space") is trained and never split/autocorrected again (A-01). Discoverable via
  the existing backspace-undo flow; typing + one undo teaches the word.
- **D-18 emoji panel toggle:** setting `d18_emoji_panel` (default on) through the settings pipeline + a
  Layout-category switch. When off, `PanelNavigation.onCombinedKeyTap(current, emojiEnabled=false)` makes the
  combined key a pure ?123 toggle (letters ↔ symbols), no emoji panel.
- **D-17 onboarding USP text:** expanded the welcome body (all 3 locales) with the strongest USPs — provably
  offline / no-internet-permission, per-finger adaptation + calibration, smart neighbour-key/umlaut correction
  (komplezz→komplett) + space repair, learns-as-you-go, DE/EN/EL dictionaries, optional on-device mini-AI,
  GPLv3 no-ads-no-accounts.
- **D-16 pattern-driven key enlargement:** `KeyProportions` gained `shiftBaseWeight` + `shiftExtra` (computed
  `shiftWeight`, mirroring backspace); `thirdRowLetterWeight` now takes both surcharges from the letters so
  the row width is preserved. New `c01_shift_extra` slider (settings pipeline + pref + strings ×3). After a
  calibration, `SettingsStore.applyPatternEnlargement` presets the enlargement from the detected hand:
  left-index → enlarged backspace, right-index → enlarged shift (THUMB/UNKNOWN untouched); user-adjustable.
- **Only §13 item left open:** D-07 "word-wise delete stops before the line's last word" — not reproducible
  from the pure logic; needs on-device repro.

### Round-3: I18N — English default + German/Greek locales (v0.7.13 / v0.7.14)
- **§14 I18N:** all app-chrome strings localised. **English is the default** (`res/values/strings.xml`), so
  any unknown system language falls back to English; German is `res/values-de/`, Greek `res/values-el/`.
  (v0.7.13 first shipped German-as-default with a `values-en`; v0.7.14 flipped it to English-as-default per
  user requirement.) The C-04 colour-label array is localised too (`values*/arrays.xml`). ~90 strings each
  (settings, onboarding, dialogs, calibration, D-24). No hardcoded UI literals remain (the only in-code
  string is the language-name space-bar/toast label, an intentional endonym). This completes spec §14.

### Round-3: calibration reliability (T-04/K-01) + D-24 touch-model visualisation (v0.7.12)
- **T-04/K-01 calibration now produces a result:** the live classifier is deliberately conservative
  (lateral threshold 0.18) so a real-but-modest hand bias fell through to UNKNOWN. Added
  `TypingPatternClassifier.forCalibration()` — a decisive preset (lateral threshold 0.05, min 20 taps) used
  only by `CalibrationActivity.detectPattern`; live info-only detection stays conservative. Also lengthened
  `CalibrationSentences` 3→5 (more taps → stabler bias). Result is stated plainly via the existing
  pattern-specific feedback strings.
- **D-24 touch-model visualisation:** pure `OffsetModel.spreadFor(id)` (mean offset + per-axis std dev);
  `AdaptKeyboardView.showTouchModel` overlay draws a translucent spread ellipse + a strike-point dot over
  each trained char key. New `TouchModelActivity` (+ layout, manifest, strings) hosts a keyboard with the
  overlay over the persisted model; reachable from a new settings entry (Kalibrierung category) and as the
  calibration result (the feedback dialog's neutral button → visualisation, or → D-09 export when recording).

### Round-3 popup rework: D-22 punctuation split + D-23 vertical popup (v0.7.11)
- **D-22:** the full-stop key now carries only the sentence terminators (`KeyboardLayout.PERIOD_ALTERNATIVES`
  = `. ! ?`); the comma key carries the clause punctuation (`COMMA_ALTERNATIVES` = `, ; : - _ /`). Index 0 of
  each is the key's own char (the primary). Both Latin and Greek layouts.
- **D-23 vertical popup:** replaced the horizontal `LongPressPopup` with pure `VerticalLongPressPopup`
  (y→index). The popup now draws the primary (index 0) as a cell offset at the top-left (pre-selected) and
  the secondaries stacked in a column directly above the finger, bottom-to-top (so on the period key `!` is
  directly above the finger and `?` above it; on the comma key `,` is the top-left default and `; : - _ /`
  run bottom-to-top). Selection follows the finger's vertical position (`updatePopupSelection(event.y)`); a
  single-alternative key (umlaut/AltGr) shows one preview cell above the key. Cells are clamped into the view.

### Round-3 bugs + quality: D-27 / D-05-06 / D-25-26 / D-29 / D-28 (v0.7.10)
- **D-27 space-bar top edge (bug):** `resolveKey` now short-circuits to SPACE when the raw point is inside
  the space-bar rect, so the offset model can't pull a clear space tap up to c/v. T-05 flagging unchanged.
- **D-05/D-06 sound + haptics (bug):** the toggles were silenced by the system touch-sound/vibration
  settings. Now authoritative: sound via a short `ToneGenerator` click, haptic via the `Vibrator` directly
  (added the `VIBRATE` permission — normal, non-privacy; the no-INTERNET/no-storage guarantee is unchanged).
  ToneGenerator released in `onDetachedFromWindow`.
- **D-25 text colour / D-26 mid-word bug:** the recognised-word highlight is now a `ForegroundColorSpan`
  (colours the text, not the background); default colour reverted to a readable medium green `#2E7D32`.
  `shouldHighlightComposing` skips the colour when a letter follows the cursor (mid-word edit), so the two
  correction characters are no longer coloured.
- **D-29 punctuation eats accepted-suggestion space:** accepting a suggestion arms `pendingSuggestionSpace`;
  the immediately following sentence/clause punctuation (`SPACE_EATING_PUNCTUATION` = `.,!?;:)`) deletes the
  auto-added trailing space. One-shot — cleared as soon as a letter is typed or on a new field; spaces
  before typed punctuation are never stripped in general.
- **D-28 proximity + distance-2 correction:** new pure `suggestion/KeyboardProximity` (QWERTZ adjacency map)
  + `EditDistance.weightedDistance` (weighted Levenshtein). The provider's fuzzy/autocorrect matching now
  accepts a candidate up to a total cost of 2, where a neighbouring-key substitution costs 1 and any other
  sub / indel costs 2 — so `komplezz`→`komplett` (two adjacent z→t slips) is caught, while two unrelated
  edits are rejected. Distance-1 behaviour is unchanged. **Works without the LLM** (the answer to the user's
  question: tier-3 adds context prediction on top, but typo correction must and does work offline).

### Round-2 slice 2: D-04 / D-14 / C-04 / D-21 / D-07 / A-07 (v0.7.9)
- **D-04 flash (shortened again):** `flashDurationMs` 80→45→**28 ms** (Gboard-like; §14 D-28 still flagged
  it as too slow after 45, so this round goes to 28 — device-tune further if needed).
- **D-14 in-keyboard long-press feedback:** the long-press popup now shows for **single-alternative** keys
  too (umlauts, ß, AltGr, Greek tonos) — a one-cell preview bubble that confirms the press and commits on
  release, Gboard-style. `AdaptKeyboardView.popupAlternativesFor(key)` = `key.alternatives` else the single
  `key.hint`; `openPopup(key, alternatives)`; only a key with no secondary (the ?123 key) falls back to the
  listener action. (The vertical layout + punctuation reorg is the separate §14 D-22/D-23, not this.)
- **C-04 defaults:** recognised-word highlight now defaults **on** (`SettingsStore` default + pref
  `c04_highlight_enabled` true), default colour a lighter green `#FF81C784` (`SuggestionConfig.DEFAULT_
  HIGHLIGHT_COLOR`, arrays green preset, pref default). NB §14 D-25 supersedes this to colour the *text*
  not the background — next round.
- **D-21 key cell padding:** `gapPx` 3→**5 dp** between keys.
- **D-07 faster backspace hold:** `BackspaceRepeat` tuned — INITIAL 400→260, START 200→120, MIN 45→28,
  ACCEL 0.82→0.78. (The word-mode "stops before the line's last word" report still needs device repro.)
- **A-07 split-undo:** a backspace immediately after an A-05 retroactive split now rejoins the two words
  into the originally typed token — `applySplit` takes the typed token and arms the existing
  `undoTyped/undoCommitted/undoDelimiter` state (commits `left␣right`, undo restores `typed`). Merge (A-06)
  undo is still not wired (needs to re-insert the removed space).

### Round-2 slice: K-01 inset / D-11-D-12 / D-15 / D-19-D-20 (v0.7.8)
- **K-01 calibration inset (blocker fixed):** `CalibrationActivity` now applies the bottom
  navigation/gesture inset to its root (`ViewCompat.setOnApplyWindowInsetsListener`, bottom only — the
  AppCompat ActionBar handles the top), so the embedded keyboard sits above the gesture pill and
  calibration is usable. Layout root got an id (`calibration_root`).
- **D-11/D-12 earlier + fuzzy suggestions:** `DictionarySuggestionProvider.suggestionsFor` now merges
  prefix completions (shown from the 1st letter) with **fuzzy neighbours** (from the 3rd letter): single
  edit / umlaut-folded matches via the new pure `suggestion/Umlaut.fold` (ä→a, ö→o, ü→u, ß→ss) + the
  existing bounded `correctionCandidates`. So a valid-but-wrong "mut" still offers "mit", "grun"→"grün",
  "defaukt"→"Default". `autocorrectFor` is likewise umlaut-aware now. (Note: a junk-in-dict word being
  highlighted as "known" — the "Defaukt was green" report — is a data issue tied to C-04, not fixed here;
  the constructive part, offering "Default", is.)
- **D-15 Caps Lock:** double-tap Shift (within `DOUBLE_TAP_SHIFT_MS`=300) engages a persistent
  `AdaptKeyboardView.capsLock`; a further Shift press releases it. The Shift key shows "⇪" while locked,
  letters render/commit uppercase (`isUpperArmed()` = shifted || capsLock), `consumeShift` leaves capsLock
  intact, and a new field resets it.
- **D-19/D-20 swipes:** a full-field horizontal swipe now cycles the surface/page
  (letters → symbols-1 → symbols-2, wrapping) via new `GestureAction.SWITCH_SURFACE_NEXT/PREV`,
  `KeyGesture.surfaceSwipe`, pure `PanelNavigation.swipePage`, and service `applySwipePage` — separate from
  the space-bar language swipe (G-01) and the backspace word-delete (G-02). D-20: the view now uses
  per-gesture thresholds (`resolveSwipe`): the small `spaceSwipeThresholdPx`=28dp only for the space-bar
  language swipe, the larger `fieldSwipeThresholdPx`=64dp for dismiss-down / surface-swipe / word-delete, so
  a faint down-swipe no longer hides the keyboard.

### Device-feedback batch D-01 / D-02 / D-05 / D-06 / D-09 (v0.7.7)
- **D-01/D-02 multi-alternative long-press popup:** `Key` gained `alternatives: List<String>`; a key with
  **≥2** alternatives opens a Gboard-style popup on long-press (finger slides to select, release commits),
  while ≤1 keeps the immediate-apply behaviour (umlauts, ß, Greek tonos). The full-stop key carries
  `KeyboardLayout.PERIOD_ALTERNATIVES` = `. ! ? , ; : - _ /` (D-02, full stop pre-selected = index 0),
  shared by `GreekLayout`. Pure `keyboard/LongPressPopup.selectedIndex(pointerX, popupLeft, cellWidth,
  count)` (unit-tested) does the x→cell maths; the view (`AdaptKeyboardView`) owns the popup state, draws
  it above the key (clamped into view bounds, cells shrink if the row is too wide), tracks ACTION_MOVE to
  re-highlight and commits on ACTION_UP via a new `OnLongPressPopupListener`. `hasLongPressAction` now also
  true for `alternatives.size >= 2`; `scheduleLongPress` branches popup-vs-single. Service:
  `handleLongPressAlternative` → shared `commitLongPressSymbol` (letter → append into word, else
  finalise+commit like a delimiter) — the old single-secondary `handleLongPress` CHAR path was refactored
  to reuse it.
- **D-05/D-06 optional key sound + haptics (default off):** two settings (`d05_key_sound`, `d06_key_haptics`)
  through `RawSettings`/`AdaptSettings`/`SettingsMapper`/`SettingsStore` + a new "Tasten-Rückmeldung"
  preference category (two switches). The view holds `soundEnabled`/`hapticsEnabled` (pushed in
  `applySettings`) and fires on ACTION_DOWN: `AudioManager.playSoundEffect(FX_KEYPRESS_STANDARD)` (lazy
  AudioManager) and `performHapticFeedback(KEYBOARD_TAP)`. **No VIBRATE permission** — `performHapticFeedback`
  routes through the window system, preserving the minimal-permission stance (dropped the deprecated
  `FLAG_IGNORE_GLOBAL_SETTING` to stay warning-free).
- **D-09 raw-tap recording (opt-in diagnostic):** setting `d09_record_raw_taps` (default off, in the
  Kalibrierung category). Pure `touch/RawTapRecorder` + `RawTap` (unit-tested): collects
  expected-char/resolved-key/key-centre/contact-point per tap, exposes per-axis deviation, serialises to a
  TSV table (locale-independent 2-decimal coords). New view `OnRawTapListener` fires at ACTION_DOWN with the
  raw point + resolved key centre; `CalibrationActivity` (when enabled) pairs each with the sentence's
  expected char and, on finish, offers a neutral "Tipp-Daten exportieren" dialog button that shares the TSV
  as `ACTION_SEND` text (no storage permission needed). Lets an uncalibrated finger's systematic offset be
  analysed offline.
- **Device-only verification:** popup rendering/geometry + finger-tracking, the punctuation set, the
  sound/haptic feel, and the D-09 export flow all need a pass on the Pixel 9a. Pure cores (`LongPressPopup`,
  `RawTapRecorder`, the settings mapping) are JVM-unit-tested; the view/activity glue is Android-only.

### Device-feedback batch D-03 / D-04 / D-07 / D-10 (v0.7.6)
- **D-03 space bar shows the language:** `AdaptKeyboardView.spaceLabel` (a view property, drawn for
  `KeyCode.SPACE` in `labelFor`, so it survives layout rebuilds); the service pushes the current input
  language's label via `updateSpaceLabel()` → `languageLabel(activeLanguage)` ("Deutsch" / "Ελληνικά";
  "English" mapped for completeness but never the active alphabet — English is only auto-detected for
  autocorrect). Called from `onCreateInputView`, `onStartInputView` and `toggleLanguage` (G-01), so the
  label tracks the space-swipe switch. The G-01 toast now reuses `languageLabel`.
- **D-04 space bar (and every special key) flashes on press:** root cause was `pressedKeyPaint` using
  `key_background_special` — the *same* colour the special keys already draw at rest, so space/shift/enter
  showed no change when pressed. Added a distinct `key_background_pressed` (#A6C8FF) colour for the pressed
  paint. Plus a **post-release flash** (`flash(key)` / `flashKey` held for `flashDurationMs` = 80 ms via
  the existing handler) so even a sub-frame quick tap is visibly acknowledged; `onDraw` highlights
  `pressedKey || flashKey`; `cancelFlash()` on the next ACTION_DOWN. Flash fires on the tap-emit path only
  (not on swipe/long-press).
- **D-07 accelerating backspace-on-hold:** pure `keyboard/BackspaceRepeat` (unit-tested, 6 tests):
  `INITIAL_DELAY_MS` = 400 before the first repeat, `nextDelayMs(step)` geometric decay from
  `START_DELAY_MS` 200 → floor `MIN_DELAY_MS` 45 (factor 0.82), and `deletesWord(charsDeleted)` switches to
  word-wise once `WORD_MODE_AFTER_CHARS` = 18 chars (~3 words) have gone. The view owns the timer
  (`scheduleBackspaceRepeat` on ACTION_DOWN of `KeyCode.DELETE`, cancelled on UP/CANCEL/move-beyond-slop —
  a move is a G-02 swipe, not a hold) and fires `OnBackspaceRepeatListener.onBackspaceRepeat(step)`;
  `backspaceRepeated` suppresses the would-be single-delete tap on release so a hold never double-counts.
  The service's `handleBackspaceRepeat(step)` resets on step 0, deletes composing chars first, then
  committed text char-wise (via the shared `deleteOneBefore`) and word-wise (via `WordBoundary`) past the
  threshold; `backspaceHeldChars` tracks the count.
- **D-10 backspace at start of entry:** the single-char delete path is now `deleteOneBefore(ic)` — when
  `getTextBeforeCursor(1)` is empty (cursor at the very start of the editable) it sends a real DEL key
  event (`sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)`) so the editor can join with the previous line/entry
  (Google-Keep-style), instead of the old no-op `deleteSurroundingText(1,0)`. Used by both the normal
  backspace and the D-07 repeat. A newline within one field is still a normal in-editable delete (joins
  lines) as before; only a truly empty prefix triggers the DEL fallback.
- **Device-only verification** (this environment cannot run an emulator): the space-bar label rendering,
  the flash timing feel, the backspace acceleration curve / word-switch threshold, and the D-10 DEL
  fallback across real editors (Keep notes, chat apps) all need a pass on the Pixel 9a. `BackspaceRepeat`
  itself is fully JVM-unit-tested; the view/service glue is Android-only.

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

### §6 sentence-start rules (comma-line C-10 + abbreviations/enumerators)
- Two new §6 capitalisation rules, replacing the old `endsAtSentenceBoundary` heuristic with a pure,
  unit-tested `capitalisation/SentenceBoundary.isSentenceStart(before, suppressAfterCommaLine)`:
  - **No auto-capital after a comma-terminated line (C-10, default on):** the content line following a
    line whose last non-whitespace char is a comma is not a sentence start — covers the German e-mail
    salutation (`Hallo Max Mustermann,` ⏎⏎ `danke …` → "danke" lower-case). Blank lines between are
    skipped (they are part of the trailing-whitespace run). Bound to a boolean setting C-10.
  - **No sentence start after a known abbreviation or enumerator:** a period ending `usw.`/`Nr.`/`z.B.`
    (pure `capitalisation/Abbreviations`, a curated German set, case-insensitive) or an enumerator
    `1.`/`10.` (regex) is not a sentence terminator, so the next word keeps its case.
- **Behaviour change (documented in the spec too):** a **line start now counts as a sentence start**
  (a deliberate newline capitalises the next word, like common mobile keyboards) — this is what makes
  the comma exception meaningful. Previously only `.`/`!`/`?`+whitespace triggered a sentence start.
  Preserved: a `.` with no following whitespace is still not a boundary.
- Settings plumbing: C-10 added through `AdaptSettings` / `RawSettings` / `SettingsMapper` /
  `SettingsStore` (`c10_comma_line_not_sentence_start`, default true) + a `SwitchPreferenceCompat` in
  the Großschreibung category. Service reads `settings.commaLineNotSentenceStart` in
  `captureTokenContext` and `sentenceStartBefore`.
- Scope note: the abbreviation rule uses a **curated built-in set + enumerator regex**. Spaced forms
  written with an internal space (`z. B.`) — only the space-free `z.B.` is recognised — and
  *user-learned* abbreviations are a later extension (would need an abbreviation flag on dict entries;
  the Wikipedia dicts strip periods so they carry none today).
- 301 unit tests (was 286; +15: `AbbreviationsTest`, `SentenceBoundaryTest`, +1 `SettingsMapperTest`).
  `:app:assembleDebug` green.

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

### Tier-3 mini-LLM orchestration (C-06, §9 — architecture first, real model deferred)

- **User decision (Option A):** built the *pure, fully unit-tested tier-3 orchestration* behind a
  pluggable backend with a **no-op default**, and left the heavy native ONNX Runtime / Gemma-Nano model
  as a documented, optional, instrumented-test follow-on. Rationale (agreed): a real on-device LLM is a
  hundreds-of-MB model + native dep, only instrumented-testable, unobtainable/unverifiable in this
  environment, and clashes with the project's pure-logic style and the earlier A-03 decision to avoid
  ONNX/fastText. The app also stays 100% offline (no runtime download). With the stub backend the whole
  pipeline is **inert** — observable behaviour is byte-for-byte the tier-1-only build.
- **Pure package `prediction/` (all JVM-unit-tested):**
  - `Tier3Provider` (interface: `isAvailable` + `predict(Tier3Request): Tier3Result`) + the inert
    `NoopTier3Provider` (default backend, `isAvailable=false`). This is the seam a real ONNX backend
    slots behind later, unchanged.
  - `Tier3Request` (input, previousWord, **full `sentence`** — A-02: the LLM sees the whole running
    context, not a punctuation-truncated fragment) / `Tier3Result` (`Tier3Suggestion`s +
    optional `CapitalisationProposal`).
  - `Tier1Confidence` — pure rank-margin measure: top score ÷ Σ scores (one dominant candidate → ~1.0,
    many similar → low, empty → 0.0). This is the value the C-06 threshold gates on.
  - `LlmActivationThreshold` — the C-06 setting as an enum LOW/MEDIUM/HIGH carrying the tier-1-confidence
    threshold (0.25/0.50/0.75; default MEDIUM); higher = LLM more eager. `fromKey` is the validation/clamp
    point (unknown/blank/null → MEDIUM).
  - `Tier3Activation.shouldActivate(conf, threshold, available)` — pure gate (available && conf < thr).
  - `SuggestionMerger` — pure tier-1+tier-3 merge: tier-1 scores normalised to [0,1] vs their own max,
    tier-3 confidence added (agreement between tiers floats a word to the top), stable order, capped.
  - `HighCertaintyCapitalisation.forcesUpper(proposal, word, min=0.85)` — the §6 **rule-6 LLM exception**
    reduced to one boolean; only lifts a word to upper-case at high certainty, never lowercases.
  - `AdaptiveLearning.learningSignal(committed, tier3, tier1KnewWord, min=0.6)` — §9 feedback: a confident
    LLM word the n-gram didn't know is returned as a reinforcement signal (else null).
  - `Tier3Orchestrator(provider = NoopTier3Provider)` — composes the above: computes tier-1 confidence,
    gates on C-06, consults the backend when activated, merges, and returns a `Tier3Outcome`
    (suggestions / capitalisation proposal / activated / tier1Confidence / raw tier3). Not activated →
    returns the tier-1 list **unchanged** (same reference), so the no-op path is a true identity.
- **§6 rule-6 hook:** `CapitalisationEngine.capitalise` gained an optional `llmForcesUpper: Boolean = false`
  placed just below proper/pure-noun and above the ambiguous-noun/`else` arms — so it can capitalise an
  otherwise-lowercased ambiguous or unknown word, but the `afterHyphen` (B-02) and sentence-start arms
  short-circuit first, so it never overrides B-02. Default `false` → all existing §6 tests unchanged.
- **C-06 setting plumbing:** `llmActivationThreshold` added through `AdaptSettings` / `RawSettings`
  (`llmThresholdKey: String?`) / `SettingsMapper.toLlmActivationThreshold` (the tested clamp via
  `LlmActivationThreshold.fromKey`) / `SettingsStore` (`c06_llm_threshold`). The previously **disabled**
  C-06 preference is now a real `ListPreference` (Niedrig/Mittel/Hoch → low/medium/high,
  `useSimpleSummaryProvider`, default medium) in the Großschreibung category; the service picks it up via
  the existing `OnSharedPreferenceChangeListener`.
- **Service wiring (thin, inert under the stub):** `AdaptKeyService` holds a `Tier3Orchestrator()` and
  the last token's `lastTier3Result` / `lastCapProposal`. `refreshSuggestions` runs the orchestrator
  (`sentence = "$tokenContextBefore$input"`, A-02) and feeds `outcome.suggestions` to the controller;
  `finalizeAndCommit` passes `HighCertaintyCapitalisation.forcesUpper(...)` into `capitalise` and, after
  `learnWord`, calls `reinforceFromTier3` (the §9 feedback). All three read as identity with the no-op
  backend (empty result, null proposal, no signal). `clearSuggestions` resets the tier-3 token state.
- 345 unit tests (was 301; +44 across `Tier1ConfidenceTest`, `LlmActivationThresholdTest`,
  `Tier3ActivationTest`, `SuggestionMergerTest`, `HighCertaintyCapitalisationTest`, `AdaptiveLearningTest`,
  `NoopTier3ProviderTest`, `Tier3OrchestratorTest`, +3 `CapitalisationEngineTest`, +2 `SettingsMapperTest`).
  `:app:assembleDebug` green.

### Tier-3 real backend — model chosen + ONNX-route groundwork (pure, verified)

- **Model (user endorsed "Apache + ONNX Runtime"):** `HuggingFaceTB/SmolLM2-360M-Instruct` — Apache-2.0
  (clean GPL fit, non-Facebook, non-gated), `LlamaForCausalLM`, vocab 49152, 32 layers, GQA 15/5 heads,
  hidden 960, rope_theta 100000, `tie_word_embeddings`, max_pos 8192. Its HF repo already ships ONNX
  exports, so the Python conversion is **skippable**. Downloaded (curl; net + git-lfs work here) to
  `D:\workspace-ai\models\SmolLM2-360M-Instruct\` — OUTSIDE the app repo, NOT in git/APK:
  `onnx/model_q4f16.onnx` (273 MB, 4-bit + fp16, fp16 KV-cache) + tokenizer/config files.
- **Runtime route decided by availability:** `onnxruntime-genai` is **not on Maven Central** (only plain
  `onnxruntime-android` 1.22.0 + `onnxruntime-extensions-android` 0.13.0 are). So the buildable path is
  **plain ONNX Runtime**, meaning the app owns the tokenizer + the autoregressive decode loop.
- **Confirmed ONNX graph I/O** (`model_q4f16.onnx`): inputs `input_ids`, `attention_mask`,
  `past_key_values.{0..31}.key/value`; outputs `logits`, `present.{0..31}.key/value` (32 layers, GQA
  5 KV-heads, head-dim 64, fp16 KV-cache). No `position_ids` input.
- **Pure ONNX-backend core (`prediction/`, all JVM-unit-tested):**
  - `Tier3Prompt` — continuation prompt = the full running context (A-02), capped to a trailing window.
  - `Tier3ResponseParser` — raw continuation → ranked candidate words (completes the current token, or
    yields next-word predictions); confidence is rank-derived (a scored decode can supply real probs).
  - `Tier3Decoding` — validated per-activation limits (maxNewTokens / numCandidates), tiny by default.
  - `Tier3ModelFiles` — the private model dir needs only the user-provided `model.onnx` (the tokenizer
    is bundled in the APK, see below); presence check over `File` drives `isAvailable`.
  - `VocabJson` — minimal pure parser for a flat `vocab.json` (token→id, JSON string-escape + raw UTF-8).
  - `Tier3TokenizerParser` — assembles a `BpeTokenizer` from `vocab.json` + `merges.txt` text (resolves the
    SmolLM2 special-token ids from the vocab; skips the `#version` header; CRLF-tolerant). Pure/testable;
    the Android layer only reads the files and calls it.
- **Pure byte-level BPE tokenizer (`prediction/onnx/`, user chose hand-rolled Kotlin over a native lib):**
  - `ByteLevel` — the reversible GPT-2 byte↔char mapping.
  - `BpeTokenizer` — full SmolLM2/GPT-2 pipeline: `individual_digits` split, the GPT-2 pre-tokenisation
    regex (`UNICODE_CHARACTER_CLASS`), byte-level encoding, rank-ordered merges, special-token isolation;
    `encode`/`decode` over plain vocab + merges + specials (the Android loader supplies these from
    `tokenizer.json`).
  - **Parity is VERIFIED, not deferred:** `BpeTokenizerParityTest` checks the Kotlin tokenizer against
    golden `text→ids` vectors generated by the real HF `tokenizers` (0.23.1, works on the machine's
    Python 3.14) for the actual SmolLM2 vocabulary — German umlauts, Greek, digits, punctuation,
    newlines, leading spaces all match **byte-for-byte**, and decode round-trips. The vocab/merges/golden
    data live under `src/test/resources/tokenizer/` (test-only, ~1.2 MB, not in the APK); generator is
    `scratchpad/gen_tokenizer_golden.py` (dev-only).
- **Bundled tokenizer + model-import UX (browser + SAF, user choice):** `vocab.json` + `merges.txt`
  (~1.3 MB, Apache-2.0) are bundled in `assets/tier3/` (CREDITS updated), so the user imports **only** the
  single 273 MB `model.onnx`. Delivery needs **no permissions**: the app has no `INTERNET` (so it is
  *provably* offline — verifiable in the manifest) and no storage permission. `Tier3ModelActivity`
  (settings → Großschreibung category) delegates the download to the browser (`ACTION_VIEW` to the model's
  public HF URL) and imports the picked file via SAF (`ACTION_OPEN_DOCUMENT`), copying it off the UI thread
  through the pure, unit-tested `Tier3ModelInstaller` (atomic temp `.part` + rename → an interrupted import
  never looks complete) into the app-private `Tier3ModelStorage.modelDir` (`filesDir/tier3-model`).
  Android glue (`Tier3ModelActivity`, `Tier3ModelStorage`) is compile-verified / instrumented-test territory.
- 392 unit tests (was 345; +19 pure core; +12 tokenizer; +11 loader; +6 `Tier3ModelInstallerTest`,
  `Tier3ModelFilesTest` retargeted to the single-file layout). `:app:assembleDebug` green; tier3 assets
  confirmed in `app-debug.apk`.

### Tier-3 inference runtime — WIRED (compile-verified; device-verification pending)

- **Dependency:** `onnxruntime-android` 1.22.0. `abiFilters` restricted to `arm64-v8a` + `armeabi-v7a`
  (drops the emulator-only x86/x86_64 native libs; debug APK 87 MB → 43 MB). Tier-3 therefore needs an
  **arm device**, not an x86_64 emulator, to test.
- **`Fp16`** (pure, unit-tested): IEEE-754 half→float, for reading the fp16 logits (subnormal/inf/NaN
  covered) — extracted so it is JVM-testable without the ONNX dependency.
- **`OnnxCausalLmSession`** (Android, ONNX Runtime): greedy autoregressive decode loop against the
  confirmed I/O (`input_ids`/`attention_mask`/`past_key_values.{0..31}.key/value` → `logits`/`present.*`).
  Key trick: the fp16 KV cache is only **shuttled** (`present.*` fed straight back as `past_key_values.*`
  via the retained `OrtSession.Result`), never read/converted; only the last-position logits are read
  (fp16→float) for the argmax. Empty initial past = fp16 tensors of shape `[1,5,0,64]`.
- **`Tier3TokenizerLoader`** (Android): reads the bundled `assets/tier3/` files → the pure
  `Tier3TokenizerParser`.
- **`OnnxTier3Provider`**: prompt → tokenize → `generate` → decode → `Tier3ResponseParser`; stop tokens =
  `<|endoftext|>`/`<|im_end|>`. `createIfAvailable` returns null when no model is installed.
- **Service wiring:** `loadTier3ProviderAsync` builds the provider off-thread (heavy session init) and
  swaps in a `Tier3Orchestrator(provider)`. When a real backend is active, `refreshSuggestions` shows the
  tier-1 suggestions immediately and runs tier-3 on a **single-thread executor** (`OrtSession.run` is not
  concurrent-safe) with a volatile sequence guard (skip stale tokens; discard late results). Reconciled on
  `onStartInputView` (picks up an import / drops on removal), executor + provider closed on `onDestroy`.
- **NB device-only:** the decode loop / session / tensor lifecycle / fp16 read are **compile-checked only**
  (no emulator/ONNX runtime here). Runtime correctness — and per-token latency/battery — must be validated
  on a real arm device; iterate on device logs. Also to tune on device: whether greedy is enough, prompt
  windowing, and whether the single big-model latency is acceptable per activation.

## Remaining (per spec §11)

- **D-278 (captured, §198): cross-device export/import** of settings, blacklist, learned words, and
  credentials/e-mail store - not designed yet, see the Current State entry above for the open questions.
- **D-280/D-281 follow-ups (§200/§201):** `SPANISH`/`ITALIAN`/`DUTCH`/`PORTUGUESE` are already in the
  `Language` enum and already fully typeable via QWERTY, but none has a dictionary or its own hint set
  built/hosted yet - a genuine, ready-to-pick-up community contribution opportunity (see
  `AdaptKey-Language-Contribution-Guide.md`). `FRENCH` is a separate case: already in the enum (for A-03's
  classifier), but per the user's own D-281 pushback its AZERTY convention likely means QWERTY is not
  actually good enough - still needs a real geometry decision (§5 of the guide) before it is genuinely done,
  not only a dictionary. Separately, the Python script that built `language_profiles.tsv` (A-03's trigram
  data) is not in this repository - reconstructing it is only needed for a language outside the eight
  already covered there.
- **Tier-3 — device work only:** validate + tune the inference runtime above on a real arm device (import
  the model via Settings → Info/Großschreibung → "Mini-LLM-Modell"), add instrumented tests, and confirm
  latency/battery. Everything code-side (orchestration, C-06 setting, §6 rule-6 hook, adaptive-learning,
  tokenizer + parity, model-import UX, ONNX session/provider/wiring) is built. Model already at
  `D:\workspace-ai\models\SmolLM2-360M-Instruct\`. Possible optimisation: ABI splits / app bundle so each
  device only pulls its own native lib (~18 MB arm64), and quantise/prune further if latency is high.
- Optional: a real fastText/ONNX model behind the same `LanguageClassifier` interface, if ever wanted.
- Verify/tune the first-run dictionary import time on a real device.
- **Stale-doc cleanup (found while answering "what's still open", not re-derived from anything new):** this
  section previously also listed "persist `activeLanguage` across service restarts", "Greek diaeresis (ϊ/ϋ)
  input" and "a language-aware C-05 blacklist editor" as nice-to-haves - all three were already implemented
  long ago (`ActiveLanguageStore`; the G-01 Greek-input package, v0.7.16; `BlacklistActivity`'s language
  spinner, also v0.7.16) and confirmed still present in the current code. Removed rather than left to
  mislead a future session into re-implementing them.

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
  unit-tested. Tier-3 adds: the `AdaptKeyService` orchestrator glue (`refreshSuggestions` merge,
  `finalizeAndCommit` §6 rule-6 hook + `reinforceFromTier3`, `lastTier3Result`/`lastCapProposal`
  lifecycle) — inert with the no-op backend, so nothing observable to test until a real backend lands;
  the whole `prediction/` package is fully JVM-unit-tested.

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
