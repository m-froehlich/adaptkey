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

- **A real fastText/ONNX model behind the existing `LanguageClassifier` interface**, as an alternative to the
  current on-device character-trigram classifier (A-03), if language-detection accuracy ever becomes the
  bottleneck. Purely optional - no known accuracy problem has actually motivated this yet.

## Open TODOs / Known Limitations (Not Yet Actioned)

Confirmed real, deliberately not fixed yet - flagged here so a future session does not have to rediscover
them, and does not fix them silently without the user's own go-ahead first (this project's own rule for
non-trivial changes).

- **Three long-standing gaps named only in narrative history entries, never previously tracked here -**
  **ALL THREE NOW RESOLVED (same session, 2026-09-06).** Recorded here specifically so this section (whose
  whole purpose is "a future session does not have to rediscover them") does not itself become the thing
  that causes rediscovery:
  1. **Turkish's dotted/dotless İ/I capitalisation** ("a real, still-open design question for
     `CapitalisationEngine`", named at the end of D-449) - resolved by D-449-followup's `CasingRules` seam
     + `TurkishCasingRules`. See §438 (v1.1.77).
  2. **`language_profiles.tsv` (A-03 trigram detection) never built for any of the 17 D-450-round**
     **languages** ("an accepted, named gap" repeated in every one of their own §422-§437 entries) -
     resolved: `dictionaries/build_language_profiles.py` (a real, committed builder this time, not a
     throwaway script) built profiles for all 17 from their own real `dict.tsv` corpora. See §439 (v1.1.78).
  3. **Serbian's missing Cyrillic keyboard layout** ("the one explicitly deferred exception" from the
     18-language D-450 round) - resolved: `keyboard/SerbianLayout.kt`, researched against the real Microsoft
     `KBDYCC` standard, plus the first real Serbian language pack. See §440 (v1.2.0).

- **Cyrillic-vs-Cyrillic auto-detection - RESOLVED (§446, v1.2.6).** Originally added §441 (v1.2.1) as a
  deliberate, named gap; prerequisite (1) tracked as partially satisfied through §442-§445 (v1.2.2-v1.2.5).
  Closed for real once the user explicitly asked for it with Serbian's own profile built specifically to
  unblock it (the one remaining missing piece). See §446's own "Current State" entry for the full
  implementation; recorded here only so this section's own history of the gap ends with its resolution,
  not with a since-superseded "still not started."

- **`seedBundledBlacklist`'s cross-language-confusables set (A-04, `due`/`sue`/`ddr`/`aks`) - CLOSED BY**
  **DESIGN (2026-09-04, no code change - user's own explicit call).** Found while auditing every place that
  does *not* route through the active-language pipeline (history §210's own D-287 fix) - deliberately scoped
  to German today (that is the only curated list that exists), not a bug in the D-287 sense. Previously
  framed as "revisit once a second language's own confusables are curated," implying this same German set
  would eventually generalise across languages - the user corrected that framing directly: it must stay
  German-only permanently, and any other language's confusables are that language's own separate curation
  effort, not an extension of this one. Re-verified directly in code before closing, not assumed:
  `seedBundledBlacklist()` is only ever called with `newStores[Language.GERMAN]`
  ([AdaptKeyService.kt:946](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:946)), reading
  `LanguageRulesRegistry.rulesFor(Language.GERMAN).bundledConfusablesBlacklist()`; every other language
  resolves to `NoOpLanguageRules`, whose `bundledConfusablesBlacklist()` returns `emptySet()`
  ([LanguageRules.kt:114](app/src/main/kotlin/de/froehlichmedia/adaptkey/language/LanguageRules.kt:114)) - no
  cross-language leak exists structurally, since the call site is hardcoded to German and carries no
  `Language` parameter at all. The Language Contribution Guide's own §5 item 7 (confusables/keyboard-adjacency
  risk scan) already describes the right shape for a future language's own independent scan - not touched by
  this closure, since it was never about extending the German list either. Nothing left to track here.

- **D-402/D-306-followup/D-345/D-330-followup/D-367 - ALL COMPLETE (§301 v1.0.54 + §302 v1.0.55 + §303**
  **v1.0.56 + §304 v1.0.57); the entire originally-agreed combined bundle is now closed.** Originally agreed
  to bundle D-402/D-306-followup/D-345/D-330-followup/D-367/D-368 into one combined cleanup round since they
  all touch the same `dict_de.tsv` rebuild/version-bump/pack-republish cycle - D-368 (homograph tagging) was
  finished separately across its own eight rounds (see its own bullet below). §301 handled the noise-removal
  side ("Mülltrennung", fully automatic per explicit user request): `Mur`/`BDI`/`Dee` and the four genuine
  corpus-tokeniser split-artefacts removed outright, plus 344 more via a systematic probe (see §301/history).
  §302 handled the rest of D-402's missing-word list plus D-367's `natürlich`-family frequency fix (see
  §302/history). §303 closed three more items: `"Stk."` recognition (a **code** fix -
  `Abbreviations.kt`'s `GERMAN` set, not `dict.tsv` - plus the bare word `"Stk"` was separately missing from
  the dictionary too, added alongside); confirmed `Robotische`/`Scheiße`/`Traditionell`/`Beugungen`'s bad
  splits are now structurally impossible as a side effect of §301's noise removal (no action needed); and
  `"Wegerecht"`->`"we"`+`"gerecht"` fixed directly by adding `"Wegerecht"` itself rather than chasing the
  original report's stale "`we` only exists via the English dictionary" explanation (no English pack ships
  in this project at all - `"We"` 203 sits directly in the German dict with unclear-but-not-confirmed-noise
  status, left alone). §304 finished D-330-followup itself: computed every real keyboard-adjacent
  single-substitution collision across all six possessive determiners' full declension paradigms (36 forms)
  using the app's own real `KeyboardProximity.kt` grid and `CorrectionConfidence`'s live formula - found only
  one genuine risk (bare `dein` vs `sein`, the originally-flagged case), fixed it, confirmed nothing else in
  the family needs touching. D-306-followup and D-345's own probes are superseded by §301's broader one - not
  provably exhaustive for every noise pattern, but the best done so far; treat any future noise report the
  same way rather than assuming it's exhaustive.
  - **D-368 (case-neutral homograph tagging) - COMPLETE, eight rounds done** (§294-§300, v1.0.47-1.0.53):
    **210** words retagged `NOUN,VERB` total - the three originally-confirmed cases (`stelle`/`sage`/`weg`),
    26 further weak-verb-1st-person-singular-vs-noun candidates (singular and plural), the nominalised
    infinitive `lachen`/`Lachen` (added on the user's own redirection from a dead-end `lache`), and - rounds
    3-8 - **180 more found via a real, systematic scan of the whole dictionary** rather than a recalled list
    (see below). The systematic scan itself is now finished end to end (see below) - nothing further remains
    open for this specific homograph pattern.
    `dank` gained a `NOUN` tag alongside its existing `OTHER` (the reverse direction - the noun reading was
    untagged, not the verb one). Pack rebuilt/republished each round, confirmed zero code change needed.

    **Method, worth recording precisely since it changes how any future continuation should work**: every
    `NOUN`-tagged entry checked against a hypothesised weak-verb infinitive (`+"n"`/`+"en"`) - 87,985 entries
    checked, 10,013 mechanical hits, but the overwhelming majority were false positives explained by a noun's
    own regular dative-plural form (formally identical in spelling to a weak-verb infinitive - not
    mechanically distinguishable from spelling alone; a later attempt to build an automated "own-plural"
    filter for this was tried and **failed** - `Krieg`/`kriegen` is a confirmed-real verb collision, but
    `Krieg`'s own regular plural `Kriege` also exists, so the filter would have wrongly discarded it too - the
    two facts are independent and cannot be told apart from spelling alone without a real morphological
    analyser this project does not have). Reviewed in frequency bands, each against `CorrectionConfidence`'s
    own live `NOUN_REFERENCE_FREQUENCY` (2000.0) as the starting anchor - not the removed, historical
    `MIN_AUTOCORRECT_CANDIDATE_FREQUENCY`/300 this session had cited from memory early on and confirmed no
    longer exists in the code: ≥2000 (204 candidates, 30 confirmed, ~15% hit rate), 500-1999 (717 candidates,
    16 confirmed, ~2.2%), 300-499 (524 candidates, 10 confirmed, ~1.9%), 200-299 (501 candidates, 11
    confirmed, ~2.2%), 50-199 (2,803 candidates, 39 confirmed, ~1.4%), 20-49 (2,743 candidates, 38 confirmed,
    ~1.4%), 10-19 (2,017 candidates, 23 confirmed, ~1.1%), 5-9 (504 candidates, 13 confirmed, ~2.6%) - and
    nothing at all below frequency 5. The hit rate fell sharply once below the ≥2000 tier but held **flat**
    (roughly 1.1-2.6%) across every band all the way down to the bottom, confirming - now conclusively, not
    just hypothesised - that the own-plural artefact is a grammar property, not a frequency one. **The
    mechanical scan is complete**: every band down to where it stops finding candidates at all has been
    reviewed. The candidate lists themselves are not persisted anywhere in the repo (regenerated ad hoc each
    round, discarded after use); if the dictionary content changes materially in the future, re-running the
    same scan from scratch would be the way to check for new candidates, not resuming from a saved list.

    **New, explicitly deferred follow-up from the user, unrelated to the above**: the nominalised-infinitive
    pattern (`lachen`/`Lachen`, and by the same logic `essen`/`Essen`, `leben`/`Leben`, and others) was
    deliberately left as a single example - "später können wir dann die anderen Beugungsformen davon und von
    anderen Wörtern hinzufügen." Not started.

    Still not an exhaustive sweep of the whole ~210k-row combined dictionary for every possible homograph
    *type* (only the weak-verb-infinitive-vs-noun pattern has been systematically scanned at all) - that
    remains its own, much larger future project (same "needs better tooling" shape as D-306-followup).

  - **The German dictionary carried zero `VERB` tags anywhere, across all ~120,000 rows, before D-368**
    **started this session** - a genuine, standalone structural data-quality finding from the original
    Wikipedia-corpus extraction, not merely a footnote to the homograph work above. All 210 `NOUN,VERB`
    entries that exist today are ones added across D-368's eight rounds. This does **not** mean ordinary verbs
    are missing as words - `gehen`/`kommen`/`haben`/`können`/`machen`/`sprechen` (checked directly) are all
    present with real frequencies, just tagged the catch-all `OTHER` instead of `VERB` specifically, since
    they have no noun collision to resolve. D-368 only ever tags `VERB` where a homograph exists to
    disambiguate - giving every genuine German verb its own `VERB` tag regardless of collision is a separate,
    larger, not-yet-started question, and would only matter once some future feature actually reads `VERB`
    for a purpose beyond this one. Worth keeping in mind for *any* future feature that might want to rely on
    a `VERB` tag meaning something for German - today, outside of these 210 words, it simply never does.

  D-402's own original convention (list every candidate for explicit user confirmation before touching the
  file) was the default until §301, where the user explicitly asked for the noise-removal pass specifically
  to run fully automatically instead, with a summary only at the end - not a standing change to the
  convention, just this one round's explicit instruction. Default back to per-candidate confirmation for any
  future round unless told otherwise again.

- **D-306's dictionary cleanup only removed *untagged* entries (missing part-of-speech) - RESOLVED for**
  **German by §301's broader sweep (v1.0.54).** D-306 itself did not attempt a broader sweep of entries that
  carry a valid tag but are still dubious (foreign proper nouns, obscure fragments) the way "til" itself was
  before its manual fix - a narrow probe at the time (short, low-frequency, `OTHER`-tagged German entries)
  turned up ~490 further candidates, never individually reviewed. §301 regenerated that same style of probe
  (1,061 candidates, same short/low-frequency/pure-`OTHER` shape) and reviewed every one individually,
  removing 348 confirmed noise entries including `"fir"` (12, `NOUN,OTHER`) itself, which was confirmed back
  in 2026-07-28 (history §242) to block `"fir"` -> `"dir"` autocorrect the same way `"til"` once did. Not
  claimed to be a provably exhaustive sweep of the whole ~210k-row combined dictionary for every possible
  noise pattern - only English's original D-306 pass and German's §301 probe have been done; if this class of
  false-positive split is reported again (for German or any other bundled language), treat it the same way
  rather than assuming §301 caught everything.

- **D-441 - RESOLVED (§413 + §415, v1.1.54): D-314's AZERTY geometry now has a real `dictionaries/fr/`**
  **pack behind it - the full one-shot pipeline from the Language Contribution Guide's own §8, run end to**
  **end on explicit user request to see how well it works, fully autonomously.** §413's first pass (12,000
  words from a small OpenSubtitles-derived list) was explicitly rejected by the user as too thin and not
  a real run of the guide's own pipeline - §415 redid it properly against a real French Wikipedia dump
  (80,000 articles, 136.8M real tokens, capped for this machine's real memory headroom - see §415 for the
  two real bugs, a memory leak and a performance bug, found and fixed along the way) merged with real POS
  data from kaikki.org's French Wiktionary extract (402,395 entries). See §413/§415 in Current State for
  the complete build method, real numbers, and every deliberate scope cut. Final: `dict.tsv` (208,204
  words, real corpus frequencies + real POS, not heuristic-guessed), `bigram.tsv` (984,792 rows, the same
  real dump corpus), `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` (French's own, not a German reuse),
  `FrenchRules` (`LanguageRulesRegistry`), one `LanguagePackCatalog` entry. **A genuinely new, structural
  finding surfaced along the way, not specific to French**: §6 rules 3/4 (automatic noun capitalisation)
  are not gated by `Language` anywhere in `CapitalisationEngine` - see the D-441 addendum to spec §6.
  D-442 (§414) made `KeyboardProximity` layout-aware, unblocking a real AZERTY confusables scan (994
  candidates found) - left un-curated (`bundledConfusablesBlacklist()` stays empty for French) since
  several are genuine French abbreviations this round's own non-native judgement could not confidently
  separate from noise; see `FrenchRules`'s own KDoc. **Not yet done, flagged honestly per the guide's own
  step 11**: no native French speaker has sanity-checked the output yet - real corpus scale now, but still
  a "pretty good" pipeline pack, not native-reviewed quality, and should not be treated as ready to
  publish without that review. **D-444 (§417, v1.1.56): the POS-tagging source above (kaikki.org's French**
  **Wiktionary extract, 402,395 entries) turned out to be the wrong, thinner one** - the *English*
  Wiktionary's own coverage of French, not French Wiktionnaire's own native edition (714.6MB vs. 56.5MB, a
  12x gap) - found when directly asked "why is the French one so small?". Rebuilt from the correct source
  with real Wortfamilien lemma-linking added (previously entirely absent) - see §417/`AdaptKey-History.md`'s
  own D-444 entry for the full account, including two more real bugs found and fixed along the way. `dict.tsv`
  208,204 -> 373,700 rows. Still not native-speaker reviewed - this fix is a real quality improvement, not a
  substitute for step 11.

- **D-443 - RESOLVED (§416, v1.1.55): first Spanish `dictionaries/es/` pack, built fully autonomously and**
  **directly to the real-corpus/real-lexicon method D-441-followup ended up on - no intermediate/heuristic**
  **pass this time, per explicit user instruction.** Spanish needed neither a new keyboard geometry (already
  QWERTY) nor a new trigram profile (already in `language_profiles.tsv` since D-280), so this round is
  dictionary/hints/diacritics/abbreviations/rules content only, same shape as D-441's own second half. See
  §416 in Current State for the complete build method, real numbers, and every deliberate scope cut. Final:
  `dict.tsv` (233,636 words, real Wikipedia-dump frequencies + real kaikki.org Wiktionary POS),
  `bigram.tsv` (732,856 rows, same real dump corpus), `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv`
  (Spanish's own, not reused from German/French), `SpanishRules` (`LanguageRulesRegistry`), one
  `LanguagePackCatalog` entry. D-441's own structural finding (§6 rules 3/4 not `Language`-gated) was
  actively re-checked against Spanish's own real name/common-noun collisions (`sol`/`paz`/`victoria`/`luz`
  and more, per explicit user instruction to search for this pattern specifically rather than only trust the
  mechanical rule) and confirmed working correctly. `dictionaries/confusables_scan.py` ran directly (Spanish
  already had `KeyboardProximityQwerty`, no D-442-style prerequisite needed) - 625 candidates found, reviewed
  by hand, left un-curated (`bundledConfusablesBlacklist()` stays empty) for the same "cannot confidently
  separate a genuine short Spanish word/abbreviation from corpus noise without native fluency" reason
  French's own AZERTY scan gave; see `SpanishRules`'s own KDoc. **Not yet done, flagged honestly per the
  guide's own step 11**: no native Spanish speaker has sanity-checked the output yet - real corpus scale and
  a real lexicon from the very first pass this time (unlike French's own two-round history), but still a
  "pretty good" pipeline pack, not native-reviewed quality, and should not be treated as ready to publish
  without that review. **D-444 (§417, v1.1.56): the POS-tagging source above (kaikki.org's Spanish**
  **Wiktionary extract, 809,603 entries) turned out to be the wrong one too** - `kaikki.org/dictionary/
  Spanish/` (91MB) is the English Wiktionary's own coverage of Spanish, not `kaikki.org/dictionary/
  downloads/es/es-extract.jsonl.gz` (99.3MB), Spanish Wiktionary's own native edition - a smaller gap than
  French's own 12x one, but the same wrong file regardless, found the same session French's was. Rebuilt
  from the correct source with real Wortfamilien lemma-linking added (previously entirely absent, contrary
  to this bullet's own original "likely already covered incidentally" guess above - checked directly this
  time, it was not) - see §417/`AdaptKey-History.md`'s own D-444 entry for the full account, including two
  more real bugs found and fixed along the way (one shared with French's own round, one French-specific).
  `dict.tsv` 233,636 -> 419,571 rows. Still not native-speaker reviewed - this fix is a real quality
  improvement, not a substitute for step 11.

- **`KeyboardProximity` hardcoded to QWERTZ - RESOLVED by D-442 (§414, v1.1.53).** Found while building**
  **French's D-441 language pack; fixed properly and generally the same session, on explicit user**
  **instruction, not merely patched for AZERTY alone.** Now a real interface
  (`KeyboardProximityQwertz`/`Qwerty`/`Azerty`/`Greek`, one object per `LayoutKind`) with a
  `KeyboardProximityRegistry` resolving the right one per active layout, threaded through
  `DictionarySuggestionProvider` the same way `LanguageRules`/`DiacriticFolding` already are. Every
  neighbour-typo correction signal (D-28/D-38/S-09) now genuinely scores against the layout actually being
  typed on. See §414 for the full mechanism.

- **D-442's own English confusables-scan side-effects, deliberately not fixed in that round (two separate**
  **open items, split out so neither gets lost inside a mechanism-focused round's own write-up):**
  1. **A real frequency-correction backlog for `en/dict.tsv`.** The QWERTY confusables scan (§414) found
     dozens of genuine, real, but rare English words (`fir`/`otter`/`nave`/`hut`/`cab`/`bye`, and more -
     `dictionaries/confusables_scan.py app/src/main/assets/en/dict.tsv qwerty 30` reproduces the full list)
     sitting at real silent-autocorrect risk against a much more frequent QWERTY-adjacent neighbour
     (`for`/`other`/`have`/`but`/`can`). The correct fix, per German's own D-330 `dein`/`sein` precedent, is
     raising each risky word's own frequency past the risk threshold, not blacklisting - a real, careful,
     one-word-at-a-time round of its own, not started.
  2. **Possible genuine noise already living in `en/dict.tsv`, never audited English's own equivalent of**
     **German's §301 sweep.** The same scan surfaced a few implausibly-high-frequency entries that do not
     look like real English words at all (`ases`/95431, `mored`/17953, `bys`/76263) - found incidentally,
     not confirmed noise via a real review, and English has never had a dedicated noise-removal pass the
     way German (§301) and Greek (§371/D-425) did. Worth its own dedicated round if picked up.

- **D-280/D-281 follow-up - RESOLVED, all four languages now have real, hosted packs.** `SPANISH` moved out
  via D-443 (§416); `PORTUGUESE` via D-445 (§418); `ITALIAN` via D-446 (§419); `DUTCH` via D-447 (§420) - see
  those rounds for the real packs now built and hosted, all eight `Language` enum entries other than
  `UNKNOWN` now covered. Separately, the Python script that originally built `language_profiles.tsv` (A-03's
  trigram classifier data) is not in this repository - reconstructing it is only needed if a future language
  falls outside the eight already covered there.

- **Tier-3 mini-LLM and first-run dictionary import: code-complete, real-device validation still outstanding.**
  Everything code-side (orchestration, the C-06 setting, §6 rule-6 hook, adaptive learning, tokenizer +
  parity, model-import UX, ONNX session/provider wiring) is built and unit-tested; what remains is validating
  and tuning the actual inference runtime on a real ARM device (latency/battery), adding instrumented tests
  for it, and verifying/tuning first-run dictionary import time on a real device. Model already sits at
  `D:\workspace-ai\models\SmolLM2-360M-Instruct\`, imported via Settings -> Info/Großschreibung -> "Mini-LLM-
  Modell". Possible optimisation once latency is measured: ABI splits/app bundle so each device only pulls its
  own native lib (~18 MB arm64).

- **D-330-followup - RESOLVED by §304 (v1.0.57).** D-330 itself fixed `deine`/`deiner`/`deinen`/`deinem`/
  `deines` against their `seinX` counterparts; the bare, uninflected `dein` was found showing the identical
  register-skew ratio too late to fold into that round, and was left open pending a full audit. §304 did that
  audit properly: not just `dein`/`sein`, but every declined form of all six German possessive determiners
  (36 forms total) checked against every other via the app's own real keyboard-adjacency grid and its current
  (D-353, log-scaled - the flat "100x `KNOWN_WORD_OVERRIDE_RATIO` bar" this bullet used to describe is stale)
  confidence formula. Result: `dein` (139 -> 550) was indeed the only real risk in the current data; every
  other pairing, including the already-fixed suffixed `dein`-forms, sits safely below every
  `AutocorrectAggressiveness` threshold. See §304/history for the full method and numbers.

- **The 2026-07-xx vacation-usage batch (§276, history) - split into individually-addressable items below**
  **(2026-08-31), per explicit request: "das sind ja sehr viele Punkte, die nicht als ein Paragraph geführt**
  **werden können."** §276 itself (see "Current State" below) stays exactly as written - the historical
  record of *when and how* the batch was captured - this split only concerns the *actionable backlog* index.
  Full original wording/reasoning for every item lives in `AdaptKey-History.md` §276; each bullet below is a
  condensed, individually-tracked pointer, not a replacement transcription. D-368/D-402/D-367/D-345/D-330-
  followup already had their own bullets before this split (see above) and are not repeated here.

  - **D-352 - RESOLVED (§280 v1.0.37, §285 v1.0.41).** Auto-split needed its own setting (auto/chip-only/
    off) since A-05 "funktioniert zu oft falsch und zu oft nicht" - shipped as `AutoSplitMode` (C-21).
  - **D-353 - RESOLVED (§281 v1.0.38, §284 v1.0.40, part of §280 v1.0.37).** Autocorrect fired without
    genuine unambiguity - replaced with `CorrectionConfidence`'s unified, graduated score plus the
    three-level `AutocorrectAggressiveness` setting.
  - **D-354 - RESOLVED (§281 v1.0.38).** Meaning-changing silent autocorrections (`"aberkennen"` ->
    `"anerkennen"`) folded into the same confidence metric as a prefix-changing-edit signal, not a separate
    boolean gate.
  - **D-355 - RESOLVED (2026-09-01, no code change - user confirmation).** User confirmed this is long since
    fixed ("längst erledigt"). No dedicated fix identified as the cause (most likely folded into D-405's own
    rule-2 rework, which removed commit-time re-derivation from sentence position entirely) - not
    root-caused further since there is nothing left to fix.
  - **D-356 - OPEN, awaiting a concrete example.** A typed umlaut should not be carelessly reverted by
    autocorrect. Per §277: no concrete repro has been supplied yet to design against.
  - **D-357 - REOPENED then RESOLVED for real (§449, v1.2.9).** The 2026-09-01 "no longer reproducible"
    closure did not hold - the user captured a real device log reproducing it in Google Keep and asked for it
    to be re-investigated. See §449 in Current State for the real root cause (traced from that log, not
    guessed) and the fix.
  - **D-358 - RESOLVED (§289 v1.0.44).** Double-tap-Backspace revert was broken right after punctuation -
    fixed alongside D-359.
  - **D-359 - RESOLVED (§289 v1.0.44).** A word reverted via double-tap Backspace was immediately
    re-autocorrected on the very next Space - a revert now gets exactly one unimpeded retry (learns nothing
    itself, distinct from D-403's own broader "silently-corrected word must eventually become learnable"
    ask, see below).
  - **D-360 - WON'T FIX (2026-09-04, no code change - discussed and declined).** Real repro confirmed first,
    not assumed: needs D-348's own double-tap-Backspace-undo setting enabled - type "Das ist ein Teyt",
    autocorrects to "Text" on Enter, a single Backspace afterward correctly does nothing at all (D-348's own
    "first Backspace at the armed tail is a no-op" design, working exactly as documented) rather than the
    single-Backspace revert D-360 originally asked for right at a line-break boundary specifically. Discussed
    directly: the user's own reassessment is that no exception is actually warranted here - D-348's double-tap
    requirement should simply apply uniformly, line-break boundary or not, with no special-cased carve-out.
    The user's own account: this concern likely predates D-416 (the eager-to-deferred auto-space redesign,
    see above) and has been implicitly resolved as a side effect of that broader simplification anyway.
    Declined rather than implemented.
  - **D-361 - RESOLVED and device-confirmed (§391-§399, v1.1.30-v1.1.38); the retroactive-reinterpretation**
    **half deliberately shelved, not implemented.** Fast Backspace typing was letting neighbour keys (including
    Enter) react instead of Backspace. Design discussed first (touches key hit-testing): two ideas were on the
    table - (A) temporarily growing Backspace's own touch zone while typing fast, (B) retroactively
    reinterpreting a wrong neighbour-tap as Backspace after the fact. User's own call: pursue A only for now -
    B is riskier than when originally floated, since D-393 (same round) made a wrongly-landed Enter tap
    genuinely submit real actions (search, send, login) in more fields, which cannot be undone after the fact.
    A itself took six real-device-log-driven rounds to actually nail down (§391 shipped it; §392 was an
    unrelated settings-screen detour; §393-§396 built out the settings-icon side quest; §397 fixed the
    vertical/Enter direction (a missing D-55 extra-spacing allowance); §398 added temporary diagnostics once
    re-reading the geometry alone stopped finding the cause; §399 found and fixed the real remaining gap - a
    raw tap landing in the inter-row gap itself matched no key's rect at all and fell through to the personal
    offset model instead of the sticky check) - 2026-09-04: device-confirmed working end to end.
    Revisit B only if A alone turns out insufficient. See spec's L-04 addendum and Current State for A's
    mechanism.
  - **D-362 - RESOLVED (§338, v1.0.90).** The loading-indicator chip (D-346) is now bold, 20sp (vs. the
    ordinary 16sp), a dedicated amber (`#F57C00`, its own colour, not reused from another chip's meaning),
    and ticks through `.`/`..`/`...` every 400 ms instead of sitting static.
  - **D-363 - WON'T FIX (2026-08-31, no code change - discussed and declined).** Colon/semicolon arming the
    same A-12 punctuation-auto-space `.`/`!`/`?`/`,` already do, with a time-of-day exception (a digit
    immediately before the colon, e.g. `"14:30"`, must suppress it). Discussed directly: unconditionally
    arming `:`/`;` collides with text emoticons (`:)`, `;)`, `:-)`, `:D`, ...) far more often than the
    time-of-day case the user originally named - the auto-space would land as `": )"`/`"; )"`. A generalised
    "only arm when a letter immediately precedes the mark" gate was worked out (subsumes the digit exception
    and protects the common "emoticon after a space or at message start" case, though not one glued directly
    onto a word with no space, e.g. `"Danke;)"`) but the user decided the remaining collision risk/complexity
    is not worth it for this feature - declined rather than implemented.
  - **D-364 - RESOLVED (§339, v1.0.91).** Root cause confirmed by reading the actual code (first hypothesis,
    tier-1 vs. tier-3 casing, was ruled out - the user has no tier-3 model installed): `SuggestionController.
    displayed()`'s de-dup compared the pre-capitalised pending-replacement word ("Text") against `stableOrder`'s
    raw-canonical words ("text") case-sensitively, so the same underlying correction slipped through as two
    chips. Fixed with a case-insensitive comparison at that one point.
  - **D-365 - RESOLVED (§340, v1.0.92).** Answered with real numbers (bundled `dictionaries/de/bigram.tsv`:
    25-113,526, median 48, p90 ~193, p95 ~340) and fixed the same way D-411 fixed the identical problem for
    learned words: new `LearnedBigramBoost` (log-scaled, no recency - deferred, no timestamp column exists
    yet), applied to the learned share only via a new `rankingBigramFrequency()`, never to the raw
    `bigramFrequency()` A-06's own merge gate still reads directly.
  - **D-366 - RESOLVED (§340, v1.0.92).** Premise only partially held up: the plain bigram signal already
    reached prefix-completion ranking via `score()`, contrary to the original bullet - what was actually
    missing was the *trigram* signal, wired only into S-07's own blank-slate prediction. `score()` now also
    consults the personal trigram table when `previousPreviousWord` is known, via the same Stupid Backoff
    blend `nextWordSuggestions()` already used. Scoped to `DictionarySuggestionProvider` only, per explicit
    instruction - `TokenRepair`'s own separate A-05 split-scoring function left untouched.
  - **D-369 - RESOLVED (§374, v1.1.13).** Accepting a suggestion chip no longer inserts a space when
    sentence/clause punctuation or a hyphen already sits directly after the composing token.
  - **D-370 - RESOLVED for the actually-wanted core (§359, v1.0.111); the "more ambitious" half explicitly**
    **dropped by the user (2026-09-01), not implemented.** Only the basic ask ("an auto-space after a closing
    double-quote should be removable") was ever real to the user - the wrongly-placed-space bug itself, now
    fixed by making the deferred A-12 space glue past a closing quote instead of materialising before it (no
    open/close tracking needed, see spec A-12). The wider ask (eating an explicit space directly before *any*
    closing quote, requiring real open/close-quote disambiguation via a lookback/parity search) was raised only
    as an opportunistic "while we're at it" and confirmed by the user as "eher theoretischer Natur" once the
    real design cost was laid out - dropped entirely, not deferred.
  - **D-371 - RESOLVED (§351, v1.0.103).** A word ending in a digit is now only ever silently autocorrected
    at C-22's own Aggressive level - no dedicated setting, reuses the existing autocorrect-aggressiveness
    slider (see Current State for the mechanism).
  - **D-372 - RESOLVED (2026-09-01, no code change - already implemented).** Checked directly against
    [KeyboardProximity.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/suggestion/KeyboardProximity.kt)
    rather than assumed: `adjacent()` has computed full 8-directional (row/col both within 1) adjacency,
    diagonals included, since this class was introduced - its own class KDoc already states this ("adjacent
    when their keys touch horizontally, vertically or diagonally") and an existing test already covers a
    genuine diagonal pair (`a`/`w`). The named example itself (`g`/`b`) was also directly confirmed adjacent
    via a real Gradle test run, not just re-derived by hand - `KeyboardProximityTest` gained a permanent
    regression pair (`g`/`b`, `h`/`b`) to keep it that way. No version bump - no behaviour changed.
  - **D-373 - RESOLVED (§356, v1.0.108).** The §355 diagnostic log immediately found the real gap: the
    live-arm from §354 *was* correctly taking effect, but the debounced D-62 reclaim's own re-derivation
    clobbered it moments later, then a third `captureTokenContext()` call (right at the actual keystroke)
    silently re-armed it again - functionally correct by the time a letter committed, but visibly flickering
    the keyboard's own Shift indicator in between, reading as broken. Fixed with a one-shot guard flag,
    mirroring the existing `shiftArmedByDelete` pattern right next to it. See Current State for the mechanism.
  - **D-374 - RESOLVED by D-416 (§333).** The trailing auto-space is never physically written until a real
    next character resolves it, so there is structurally nothing left to strand or fail to clean up when a
    field is left (Google Keep or otherwise) - eliminated, not patched.
  - **D-375 - RESOLVED (2026-08-31, no dedicated fix identified).** User confirmed on real-device testing
    that `"sollendafur"` no longer gets silently auto-unfolded/split the way originally reported - no code
    change in this session targeted it specifically; likely a side effect of the intervening dictionary
    cleanup rounds (§301 and later) or a later A-05/umlaut-interaction refinement, not root-caused further
    since there is nothing left to fix.
  - **D-376 - RESOLVED (§366, v1.1.5).** New S-12 - a `"km/h"` completion chip offered both right after
    `"km"` and right after `"km/"`. See Current State for the mechanism.
  - **D-377 - RESOLVED (§367, v1.1.6).** New A-13 - evidence-gated missed-Backspace recovery, chip-only per
    the user's own explicit call (a generic dictionary-widen alternative was discussed and rejected on
    false-positive-risk grounds first). See Current State for the mechanism.
  - **D-378 - RESOLVED (§357, v1.0.109).** Same root-cause shape as D-373-followup (v2): the debounced D-62
    reclaim's own `armShiftForNextWord` call, ~100ms after the opener commits, was never taught about D-378 at
    all and clobbered it. Fixed with the same one-shot-flag pattern (`shiftPreservedAfterOpener`). See Current
    State for the mechanism.
  - **D-379 - RESOLVED (§330, v1.0.82).** `"bzgl."` added to `Abbreviations.GERMAN` alongside the
    already-present `abzgl.`/`zzgl.` family.
  - **D-380 - RESOLVED and device-confirmed (§403, v1.1.42).** A long-press smear leaving the key's own
    bounds (e.g. into the inter-key/inter-row gap) unconditionally cancelled the pending popup even when the
    smear was nowhere near swipe-sized - the user's own precise repro: within the key it works reliably,
    off the key it silently does nothing. Fixed in `AdaptKeyboardView`'s own ACTION_MOVE handling - see §403.
  - **D-381 - RESOLVED, device-confirmed (2026-08-31, no code change).** The fresh look this bullet asked
    for happened: user confirmed on-device that D-404 §323/§324's Learned Words editor (category
    multi-select, LLM-determined with a tier-3 model installed) fully satisfies the original ask.
  - **D-382 - RESOLVED (§330, v1.0.82).** `KeyboardLayout.numberKey('2')` (shared by both QWERTZ and QWERTY,
    L-01) now offers an apostrophe and subscript `₂` as a third and fourth long-press alternative, alongside
    the existing shifted-symbol/superscript pair. Every other digit unaffected.
  - **D-383 - RESOLVED (§389, v1.1.28).** In Google Keep's list mode, placing the caret before a word and
    pressing Enter deleted that word - root-caused from a real device log to `splitComposingAtCaretAndCommit()`'s
    delete-then-reinsert sequence losing the still-provisional re-inserted half when Keep tears down its own
    `InputConnection` in reaction to the delimiter. See spec §1's guiding principle (new point 4) and Current
    State for the mechanism.
  - **D-384 - WON'T FIX (2026-09-01, no code change - discussed and declined).** Typing a minus preceded by a
    space should also get its own trailing A-12-style auto-space, but only when a space already precedes the
    minus. Analysed in detail: technically a small, low-risk-looking addition (one more pattern in
    `pendingSentenceMark()`, D-370's own helper - B-01/B-03's compound-hyphen chain is already structurally
    unreachable for a space-preceded dash, since `composing` is only ever non-empty for a genuine mid-word
    hyphen). But the analysis itself surfaced a real interaction with D-370's own closing-quote glue (a `"`
    typed right after a space-dash is far more often *opening* a new quoted aside than closing one - the
    opposite bias from the `.`/`!`/`?`/`,` case D-370 was built for - so the two features would need to be
    explicitly kept apart, not simply share the same code path). Weighed directly against the benefit - saving
    exactly one already-deliberately-typed keystroke (the second space in "word - word") - and declined: A-12/
    composing-state is this project's own most fragile area (spec §1's guiding principle; D-373/D-378/D-421 all
    needed multiple real-device-log rounds to fully nail down), not worth the added surface area for a
    convenience this small. User's own call, not implemented.
  - **D-385 - RESOLVED (2026-08-31, no code change - a deliberate decision, not an implementation).** "German
    should go back to being a bundled language, not an installable pack" - discussed for/against directly
    with the user; decided **against** bundling (would permanently privilege the maintainer's own language for
    every future non-German install, and the current architecture has no "installed but deactivated" state
    at all - not even English can be turned off, so bundling German today would not even satisfy the user's
    own "must be at least deactivatable" requirement without first building that capability separately).
    Instead: a locale-aware first-run prompt (detect system locale `de`, prominently offer the German pack
    immediately) was agreed as the better-targeted fix for the *actual* pain point - not yet implemented,
    worth its own future backlog item if picked up. D-385's own "nothing may be lost" constraint is moot
    given this outcome (no migration ever happens).
  - **D-386 - SUPERSEDED BY D-413 (2026-08-31, no version bump - a decision, not new code).** The
    `(1)`-suffixed-duplicate folder resolution (§327 v1.0.79) is gone again - see D-413 below: the
    `ACTION_OPEN_DOCUMENT_TREE` folder grant it depended on turned out to be refused outright for the
    Downloads folder itself on a real device. The post-import 60-second-old cleanup half of D-386 survives
    unchanged (now against a directly-picked file) - see spec §30.
  - **D-387 - RESOLVED (§401 architecture + §402 data mechanism, v1.1.40 + v1.1.41).** While discussing this
    item, found and fixed a genuine bug it was resting on first (D-435, §401): the umlaut/diacritic mechanism
    (D-144/D-204) was called unconditionally regardless of active language, not merely "missing" for other
    languages. §402 then built the actual per-language content mechanism D-387 asked for: an optional
    `diacritics.tsv` per language pack (base letter -> known variants, D-436), loaded exactly like `hints.tsv`/
    `abbreviations.tsv`, resolved into a new `DataDiacriticFolding` generalising `Umlaut` beyond German's own
    1:1 vowel map (including a base letter with several real variants, e.g. French's `e` -> `é è ê ë`) - see
    §402 for the full mechanism and the two follow-up design points (direction-only unfold; the L-05 host-key
    concept as the universal single-substitution rule) it closes out. No language beyond German has real
    `diacritics.tsv` content yet - that is now exactly the same, expected, individually-picked-up-later state
    every other optional per-language file (`hints.tsv`/`abbreviations.tsv`) is already in for every language
    but German/English, not a remaining gap specific to D-387 any more.
  - **D-388 - RESOLVED (§291 v1.0.45).** Learned Words/Blacklist editors needed sortable views - shipped as
    the `last_touched` column + Recent/A-Z sort picker + locale-aware `Collator` sorting.
  - **D-389 - RESOLVED (§344 v1.0.96 + §345 v1.0.97 + §347 v1.0.99 + §348 v1.0.100 + §349 v1.0.101).**
    Learned words now expire after a configurable period of disuse - see spec W-05/C-24. C-24's *displayed*
    labels are 1 month / 4 months / 1 year / **Never** (default, §345's own opt-in-not-opt-out fix; §347's
    own follow-up switched from abstract früh/mittel/spät labels to these concrete durations), but the
    *stored* value stays the abstract `early`/`medium`/`late`/`never` level (§349's own correction, reverting
    part of §347 - the stored value and its current duration meaning are deliberately decoupled, so a future
    retuning of what a level means takes effect immediately for anyone with it already saved, no migration
    needed). A once-a-day sweep across every installed language's own learned-word store, un-learning
    (`DictionaryStore.forget`) whatever has gone untouched (`last_touched`, D-388) past the configured window
    - except that (§348) a D-404 word family only ever expires as a whole, once every one of its own members
    has individually gone stale; a single frequently-used member holds the rest of the family alive
    indefinitely.
  - **D-419 - RESOLVED (§346 v1.0.98 + §349 v1.0.101).** Every plain `ListPreference` in the settings screen
    (C-06, C-24) shows its own currently selected entry directly in the main list, as a fully **bold**
    "Currently: X" line (§349's own follow-up) - previously only C-04 (D-302) and the
    `LabeledSeekBarPreference` sliders (C-21/C-22) showed their current value at all. See spec §20's own
    D-419 note.
  - **D-390 - WON'T FIX (2026-09-01, no code change - discussed and dropped).** Sentence-start
    auto-capitalisation tolerating multi-part abbreviations (`"p. a."`/`"i. d. R."`) with a retroactive
    correction would have needed a deliberate, narrow exception to D-405's own "never a commit-time
    correction" principle, plus editing already-committed (not composing) text - real risk, discussed
    directly. User's own call: with D-405/D-416 already in place, typing straight through in lower-case is
    simply not auto-corrected back any more either, so "p. a." has no effective problem left to fix - only
    slightly more effort to type deliberately, judged acceptable. Dropped rather than designed further.
  - **D-391 - OPEN.** A-05's retroactive split extended to the reverse direction (mirrors the same auto/
    chip-only setting D-352 got): if the current or preceding word makes no sense alone, but inserting a
    bottom-row connector letter (`y x c v b n m`) between them produces a sensible combined word, recognise
    it - a generalisation of A-06 merge beyond its current scope.
  - **D-392 - RESOLVED (§353, v1.0.105).** Releasing Caps Lock now re-derives Shift fresh from the real
    caret position (D-313/D-406's own [armShiftForNextWord] mechanism) instead of unconditionally clearing
    it. See Current State for the mechanism.
  - **D-393 - RESOLVED (§390, v1.1.29).** In the Google Play Store's own search bar, Enter did not act as
    Submit - root-caused from a real device log to `handleEnter()`'s `MULTI_LINE` check firing before any
    action handling, since Play Store sets `MULTI_LINE` on its search field despite it being single-line. See
    spec's new G-07 and Current State for the mechanism.
  - **D-394 - RESOLVED, digit-mirror only (§330, v1.0.82).** The calculator page's digit block now reads
    `1 2 3` / `4 5 6` / `7 8 9` / `0` top to bottom (was calculator-style `7 8 9` / `4 5 6` / `1 2 3` / `0`) -
    the operator column and every other key untouched. The T9-letter-long-press half of this ask was
    explicitly declined by the user - dropped from scope entirely, not deferred.
  - **D-395 - WON'T FIX (2026-09-03, no code change - user's own root-cause correction).** Originally reported
    as the system gesture-navigation bar having become more sensitive near the space bar's lower edge. User
    since traced it to a personal, temporary cause instead: typing with the middle finger during an injury,
    which does not reach the gesture zone as cleanly as the index finger - confirmed gone entirely once back to
    typing normally. Not an app issue at all; nothing to build.
  - **D-396 - RESOLVED and device-confirmed (§405-§409, v1.1.44-v1.1.48).** Reread during implementation:
    "three levels" turned out to mean the OS's own Haptic-feedback intensity slider, not app-invented
    stronger/weaker signals - see §405-§409/spec §42 for the full mechanism (strength via touch-classified
    `vibrate()` calls at every API level, quantity via a new `HapticTier` gate keyed to that same slider) and
    its four device-feedback tuning rounds (`KEY_PRESS` moved to a real `EFFECT_TICK` click/detent effect;
    the L-05 popup-open cue removed as redundant, popup-accept given the one it was genuinely missing;
    `CORRECTION`'s amplitude lowered once it started firing for that same frequent accept-a-selection case).
    User's own closing word: "So ist der Effekt perfekt! Wir können den Punkt abschließen."
  - **D-397 - OPEN.** Touch zones should generally bleed less into neighbouring rows, not only the bottom
    letter row's already-capped case - named example: `q` currently reaches far enough down to frequently
    produce an unwanted `q` instead of the intended `a` below it.
  - **D-440 - RESOLVED and device-confirmed (2026-09-04, §411 display fix v1.1.50 + §412 commit fix**
    **v1.1.51).** The §410 diagnostic log the user captured cleared `SqliteDictionaryStore.canonicalWordFor()`
    outright (it already resolved "grüße"/"fröhlich" correctly) - both real bugs were one step later, at the
    two independent points that re-derive a chip's casing from live context right before use: `showSuggestions
    ()` (display, §411) and `onSuggestionClicked()`'s `Kind.NORMAL` tap handler (commit, §412). Both relied on
    `context.explicitFirstUpper` reflecting what was actually typed - meaningless with `composing` empty (a
    next-word prediction's defining state), so a genuinely ambiguous noun fell through to its unconditional
    lower-case default both times, silently discarding the casing `canonicalWordFor()` had already resolved
    correctly. Both fixed identically: skip the re-derivation while `composing` is empty. Confirmed on the
    Pixel 9a: "Fröhlich" now suggests and inserts correctly capitalised; "Grüße" too, and - confirmed by the
    user's own correct read of the mechanism - never creates its own `TABLE_LEARNED` entry, since its bundled
    entry is already canonically capitalised, so there is nothing to learn (D-327's own established design,
    not a gap). "dank" itself needed no fix -
    genuinely ambiguous with no more-specific casing to prefer; extending the S-11-style dual-casing chip to
    next-word predictions is a separate, still-open design question.
  - **D-398 - RESOLVED (§330, v1.0.82).** The automatic language-switch threshold (D-130, formerly a
    hardcoded 5) is now C-23, a 0-8 slider under the Dictionary category's language section, default 5;
    0 disables the automatic switch entirely (manual G-01 swipe unaffected).
  - **D-399 - RESOLVED (§330, v1.0.82).** C-03's maximum-suggestions range widened from a 6 floor to 3.
  - **D-400 - RESOLVED (§331, v1.0.83).** Discussed and implemented: the keyboard layout is now pinned to
    the device's system language, independent of whichever language is active for dictionary/suggestion
    purposes - neither D-130's automatic switch nor an ordinary manual G-01 swipe between two Latin-script
    languages changes it any more ("niemand will plötzlich von QWERTZ auf QWERTY wechseln"). Two exceptions,
    both the user's own explicit refinements during discussion: switching into a non-Latin-script language
    (Greek) always uses its own layout (otherwise physically untypeable); and when the system language
    itself has no sensible Latin layout to offer (system language is Greek, or unrecognised) and the user
    explicitly switches to a genuine Latin language, the layout follows *that* language's own convention
    directly rather than a blind QWERTY default - "hier wird ohnehin klar umgeschaltet, dann kann man auch
    direkt das passende Layout nehmen." See spec's G-01 addendum and `LayoutRegistry.kindFor()`'s own KDoc
    for the full resolution order.
  - **D-401 - OPEN, a fully-specified new feature concept (captured verbatim, the shape is already precise -**
    **see history §276 for the complete four-stage description).** A cursor/text-selection mode reached via a
    long-press on the space bar: long-press arms it (vibration, keys fade to 30%, crosshair appears);
    swiping in Stage 1 moves the cursor; holding still 800ms promotes to Stage 2 (second vibration, colour
    change), where swiping extends a text selection instead, and a tap ends the mode; lifting the finger keeps
    the mode armed for ~1000ms (re-touching within that window re-origins the crosshair at the new point;
    letting it expire ends the mode and, if a selection is active, opens the platform's own selection context
    menu).
  - **D-403 - RESOLVED (§280 v1.0.37, §289 v1.0.44, §325 v1.0.78).** Uppercase acronyms were apparently never
    learnable and poorly supported generally - four distinct sub-reports, all now closed: (1) the
    `learnedCasingOf` ratio exemption (§280) protects an *already-learned* word from ever being overridden
    again; (2) "a silently-corrected word's revert must count as a learning signal, and the next commit must
    never re-fire the same wrong correction" (§289's D-359 work: one unimpeded retry); (3) the concrete
    `"kWp"`-never-learnable repro - an acronym silently corrected away *before* it could ever accumulate a
    W-02 pending count at all - is exactly what §325's `Acronym.isAcronym()` veto closes (reproduced and
    confirmed fixed against the real dictionary with `"etf"`/`"ETF"` before shipping); (4) "typing `"etf"`
    lower-case should surface the learned `"ETF"` pinned at the front, same as typing it upper-case" -
    already true via D-264's own casing-merge (`unigramsByPrefix`/`entryOf` prefer the learned entry's own
    casing, confirmed via the existing `"MSCI"`-vs-`"Msci"` regression test), reconfirmed directly with the
    user this round.

- **D-344 (download directory control, spec §30) - RESOLVED, mechanism changed by D-413 (see below).** The
  SAF folder-grant approach from §327 (v1.0.79) was reverted; the app is back to a plain `ACTION_OPEN_
  DOCUMENT` single-file picker, the same family of fix D-344 originally chose between (HTTP header control
  and raw-repo-path remain declined for the reasons given in spec §30) - see spec §30 for the current
  mechanism and D-413 for why the folder-grant attempt did not survive real-device testing.

- **D-345 (dictionary noise scan, spec §31) - RESOLVED by §301 (v1.0.54); the "Bri" loose end struck**
  **(2026-08-31, user's own call, no further action).** The broader scan for Wikipedia-extraction-noise
  entries (fragments, obscure acronyms, markup tokens) that §345 called for is exactly what §301's probe did
  (see the D-402/D-306-followup/D-345 bullet above for the full method and results). `"Bri"` itself, the
  entry that originally prompted D-345, was checked directly against the current `dict.tsv` and was **not
  found** (case-insensitive) - either already removed in some earlier untracked change, or the original
  report used different casing/context that doesn't match a literal `"Bri"` row. Nothing left to track here.

- **D-404 (inflected forms flooding the Learned Words list, and more broadly the bundled dictionaries too -**
  **e.g. "Kugel"/"Kugeln" both present, risking the plural's frequency silently outranking and replacing**
  **the singular) - evaluated and deliberately split into three tiers of very different size, not one task:**
  1. **Full generative morphology** - reduce the dictionary to only irregular forms, generate regular
     plurals/inflections algorithmically at runtime. A real NLP-engineering project of uncertain feasibility
     (the user's own stated doubt) - German noun pluralisation has several major classes plus umlaut
     mutation and gender-dependent variation, far messier than `RegularVerbInflection`/`AdjectiveInflection`'s
     own already-narrow "regular case only" scope (D-115/D-125/D-252, which only ever *protect*, never
     generate, and explicitly exclude strong/ablaut forms). Deliberately **not** folded into the D-402
     cleanup round above - needs its own dedicated design discussion later, comparable in weight to
     D-353/D-410. **That design discussion happened (2026-08-30): a full phased plan for generating -**
     **not merely linking - every verb's and noun's complete paradigm now lives in its own file,**
     [`AdaptKey-Plan-Wortfamilien.md`](AdaptKey-Plan-Wortfamilien.md) - deliberately deferred, not started,
     at the user's own explicit request to prioritise other work first. Confirmed there: no gender field
     exists anywhere in the schema today (`WordEntry`/`PartOfSpeech` checked directly), a real blocker for
     noun declension specifically; methodology is rule-based generation + curated exception tables +
     sampling, not the individual-review-of-every-candidate approach the two prerequisite sweeps below used
     (confirmed infeasible at this project's estimated 300,000+ candidate-form scale). Read that file before
     picking this up, don't re-derive the plan from scratch. **RESOLVED — see §322 in Current State: the
     "Wortfamilien" project generated and added the missing paradigms end to end; `AdaptKey-Plan-Wortfamilien.md`
     was deleted once superseded.** **Adjectives followed the same way — see §360: full declension x degree
     for every already-bundled adjective lemma, `AdaptKey-Plan-Adjektive.md` deleted once superseded. Tier 1
     is now complete for nouns, verbs, and adjectives alike.**
  2. **A lighter cross-reference/lemma-link approach - RESOLVED (§388, v1.1.27).** `shouldOverrideKnownWord`
     now vetoes the A-01 ratio override outright when the typed word and the candidate share a D-412 `lemma`
     family, before any ratio is computed - see spec A-01's own D-404 Tier 2 addendum and §388 below for the
     full mechanism and reasoning.
  3. **The minimum bar, scoped to the Learned Words list specifically**: link inflected forms of an
     already-learned word at learn time so they collapse in the editor's own display (D-388's sortable
     view), instead of flooding it with near-duplicate entries for what is conceptually one word. Pure code,
     touches no dictionary data at all - would naturally extend the `LanguageRules` mechanism (D-410) the
     same way `RegularVerbInflection`/`AdjectiveInflection` already live there, reused for "is this an
     inflected form of an already-learned word" rather than only "protect from autocorrect". Independent of
     the D-402 cleanup round - no sequencing dependency either direction, can be picked up separately
     whenever convenient.

     **Explicit constraint for whenever tier 3 is implemented, from the user directly**: it must **never**
     reset or wipe the existing Learned Words list - real entries have already accumulated there that the
     user does not want to lose. Needs a genuine migration path that consolidates already-accumulated
     entries under their shared base form (mirroring D-388's own `last_touched` column migration as the
     precedent for "add new structure to an existing table without discarding what's already there"), not a
     fresh start. **RESOLVED — see §323/§324 in Current State and spec §39: the migration honours this**
     **constraint exactly (additive `ALTER TABLE`, no wipe), and the with-LLM extension (whole-family**
     **learning on every learn event, a unified "LLM installed is a state" reprocessing pass) is also now**
     **implemented (§324).** **D-404-followup (§361, v1.1.0): the with-LLM path turned out to already cover**
     **adjectives (the prompt/parser/applier were POS-agnostic from the start, `ADJECTIVE` already one of the**
     **prompt's own category options) - checked directly in the code, not assumed, before reporting back. Only**
     **the *non-LLM* path (`LearnedLemmaLinking`, this tier) was actually missing adjective endings, now added.**
     **D-404 is now fully closed, all three tiers (§388 closed tier 2, the last one open).**

  **D-412 - RESOLVED, fully closed (§305 schema, §306-§315 verb-tagging sweep, §320-§322 lemma**
  **population/Wortfamilien, plus D-422/D-424 extending the same parity to English/Greek).** The bundled-only
  `lemma` link column on `TABLE_WORDS` (§305) was populated in full for German: the nine-round verb-`OTHER`->
  `VERB` retagging sweep (§306-§315) individually reviewed all 10,925 candidates across every frequency band;
  the noun-inflection-linking project (§320-§321) then individually reviewed and linked ~20,024 further
  mechanical candidates. The originally-planned "mechanical weak-verb-inflection derivation pass" and
  "strong-verb principal-parts reference table" were superseded entirely by §322's Wiktionary-backed
  Wortfamilien project, which generated full noun/verb paradigms directly and lemma-links every new row as a
  byproduct (adjectives followed via §360). **D-422 (§368, v1.1.7) and D-424 (§370, v1.1.9) then extended the
  identical Wiktionary-parity/lemma-linking treatment to English (`app/src/main/assets/en/dict.tsv`) and Greek
  (`dictionaries/el/dict.tsv`) respectively** - `lemma` is populated for all three bundled/installable
  languages today, not German alone. This is the schema/data groundwork D-404 Tier 1 consumed (see above,
  RESOLVED) and that §388 (below) is the first real consumer of on the ranking/override side (D-404 Tier 2).

- **D-351-followup - CLOSED AGAIN (2026-09-03, no code change - not reproducible).** Had been reopened
  2026-09-01 on a report that Total Commander's "Datei umbenennen" (rename) date field showed the same
  field/editor incompatibility D-351 originally found and worked around for Gemini's search field
  (`reclaimOnCaretMoveSuppressed`, scoped by package name in `AdaptKeyService.onStartInput()` - see
  [AdaptKeyService.kt:298](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:298)). User has
  since retested and confirmed Total Commander does not actually show the problem after all - closed without
  ever wiring in a package-name-scoped suppression for it. `reclaimOnCaretMoveSuppressed` stays scoped to
  Gemini only, as before D-351-followup. Revisit only if a genuine new repro turns up.
    since this is exactly the class of bug spec §1's guiding principle warns must be re-derived from real
    logs, not guessed.

- **D-414 - RESOLVED (§337, v1.0.89) for the Reclaim half; the Cycle half explicitly shelved, not merely**
  **deferred.** A manual Reclaim button ("🧲") now lives in the extra row (§14, R-01), left side - motivated
  directly by D-351/D-351-followup (Gemini, Total Commander): tapping it fires D-62's reclaim immediately,
  unconditionally ignoring `reclaimOnCaretMoveSuppressed`, enabled only while a reclaim is genuinely possible
  right now. The originally-floated Cycle half (tapping again on an already-reclaimed word to cycle through
  suggestion candidates) was discussed and dropped after the user traced through it themselves and found real
  problems (a cycled candidate would need its own frozen list snapshot, plus open Backspace-during-cycle and
  casing questions) with no clean resolution yet: "dieses Cycle-Feature war mehr eine fixe Idee, die bei
  sauberem Durchdenken mehr Probleme aufmacht als sie löst... zurückstellen bzw. vergessen bis ich vielleicht
  mit einem sauberen Konzept nochmal auf dich zukomme." Not tracked as its own open backlog item any more -
  revisit only if the user brings a genuinely new concept for it.

- **D-415 - WON'T FIX (device-confirmed, 2026-09-01).** A "give up focus as if it had never been set"
  button in the extra row, motivated by Google Keep: once a list-item field has been tapped into, focus
  never leaves it, so the keyboard keeps popping back up and the old caret position is hard to relocate
  after dismissing/reopening. §335 (v1.0.87) wired both candidate levers to temporary test buttons
  ("🔙"/"🏁") and the user tried them directly against Keep on a real device: **`KEYCODE_BACK` does nothing
  at all; `performEditorAction(IME_ACTION_DONE)` only hides the keyboard - focus itself is retained either
  way.** Confirms the original technical concern outright: `InputMethodService` genuinely has no way to make
  a host app release focus it does not want to give up - a real Android platform limitation, not a gap in
  this app's own implementation. §336 (v1.0.88) removed both temporary test buttons again, as promised when
  they were added. Nothing left to build here unless some future, fundamentally different mechanism
  (e.g. an accessibility service, well outside an IME's own scope and permission model) is ever considered
  worth the added complexity - not currently planned.

- **D-416 - RESOLVED (§333, see History.md for the full account).** A-12's eager auto-space-after-punctuation
  replaced with the deferred model (space is set only once the next letter is actually typed, not eagerly the
  instant the punctuation mark commits) - planned in `AdaptKey-Plan-D416-Deferred-Space.md`, deleted once
  superseded (2026-09-04, explicit user call: the deferred model "hat sich extrem bewährt und wird bleiben" -
  no return path is kept any more; its own `AdaptKey-Rollback-D416-Deferred-Space.md` companion deleted
  alongside it for the same reason). Correction to the original framing above: D-373 turned out **not** to be
  related (a hyphen never got its own A-12 auto-space to begin with, so there was nothing for it to collapse
  into - it remains its own, independent, still-open item, see below). D-384 genuinely is eased by the new
  model, but was **not** bundled into this round - still its own separate, not-yet-implemented item. D-363
  (colon/semicolon vs. emoticon) also stays a deliberately separate, not-yet-decided follow-up. No settings
  toggle was ever built, per the user's own explicit call at the time (would not scale across every touch
  point).

- **D-418 - OPEN, low priority, not started (2026-09-01).** A genuine in-text visual cue for D-416's deferred
  space/caps state (as opposed to the space-key dot that shipped) - user's own explicit call: "nicht so
  furchtbar wichtig", worth revisiting later, not now. Two realistic approaches identified, neither a small
  add-on - see the conversation for the full discussion:
  1. Hold the sentence-ending mark itself in composing state (not hard-committed) until the pending decision
     resolves, so it can carry an IME-supplied visual span (underline/background), the same mechanism S-05's
     word highlight already uses. Touches the composing-state/`onUpdateSelection` area spec §1's guiding
     principle flags as historically fragile (three real device-log rounds for D-139) - would need its own
     careful design-and-device-test round, not a quick addition.
  2. A floating overlay drawn at the cursor's real screen position via `CursorAnchorInfo`/
     `requestCursorUpdates` - avoids touching document/composing state at all, but depends on the target
     app actually reporting cursor coordinates (inconsistent across apps, not guaranteed - ironically
     including Google Keep/Total Commander, the two apps that motivated D-351-followup/D-414/D-415) and
     would be this app's first use of a floating IME overlay at all.
  Revisit only when/if the user explicitly wants to pursue one of these as its own dedicated round.

- **D-451 - RESOLVED (§448, v1.2.8).** The L-05 AltGr/long-press popup opening now fires its own haptic
  confirmation again, reversing D-396-followup (v3)'s earlier removal - see §448 in Current State for the
  mechanism.

- **D-452 - WON'T FIX (2026-09-06, no code change - not reproducible).** User's own account: no longer
  reproducible on their device right now. Closed without ever identifying the "Wahrscheinlich" concern's
  original cause - per this project's own diagnosis convention, nothing was guessed at or changed based on
  the description alone, and none ever arrived to investigate further. Revisit only if it recurs with a real
  device log.

- **D-453 - RESOLVED (§448, v1.2.8).** Double-consonant "unfold" for autocorrect/chip suggestion
  (`"bite"` → `"bitte"`, `"tipen"` → `"tippen"`), implemented exactly as agreed - an extension of S-09's
  existing neighbour-prefix escalation (D-328), not a new fold/unfold table - see §448 in Current State for
  the mechanism.

- **D-454 - RESOLVED (§447, v1.2.7).** Restructured the Language Packs settings screen
  (`LanguagePacksActivity`/`activity_language_packs`, D-280) per the user's own six-point list - see §447 in
  Current State for the full implementation.

- **D-455 - OPEN, root-caused, not yet fixed (2026-09-06).** In a `reclaimOnCaretMoveSuppressed` field
  (Gemini, D-351), moving the caret from one existing word into another leaves Caps/Shift stuck at whatever
  state was active before the move - the fresh-derivation-per-position mechanism (D-313/D-406's
  `armShiftForNextWord`) never runs there. Confirmed in code, not guessed: `scheduleReclaimAndChipRefresh()`
  ([AdaptKeyService.kt:1724](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:1724)) only
  schedules `reclaimWordAtCaretRunnable` - the sole call site that ever invokes `armShiftForNextWord` for this
  "tap into an existing word" case (inside `reclaimWordAtCaret()`) - when `!reclaimOnCaretMoveSuppressed`; the
  chip-visibility refresh right below it was already deliberately split out to run unconditionally
  (D-414-followup, "the chip's own refresh is scheduled unconditionally regardless"), but the Shift
  re-derivation was not given the same treatment. The user wants exactly that: Shift/Caps re-derivation on a
  caret move must happen independent of whether the actual composing-region reclaim is suppressed. Fix shape
  (not yet implemented): extract just the position-only "is this now a fresh Shift-relevant position, and if
  so what should Shift be" check out of `reclaimWordAtCaret()`'s body into its own function, called from
  `scheduleReclaimAndChipRefresh()` unconditionally, mirroring `reclaimEnabledRunnable`'s own precedent
  exactly - without touching the composing-region reclaim itself, which must stay suppressed in Gemini.

- **D-456 - OPEN, likely already substantially covered by an existing mechanism - needs a real repro before**
  **further action (2026-09-06).** User's ask: with D-348 (double-tap-Backspace-undo) enabled, when a word
  committed *without* any autocorrect firing (so it was only ever plain-learned/counted-up, never protected by
  the A-07 undo window), backspacing back into it and changing it should un-learn the original word - "sonst
  wird ein Tippfehler dauerhaft mitgelernt, nur weil kein Autocorrect dagegen lief." An existing, closely
  related mechanism already does most of this: D-248/D-140
  (`rememberForBackspaceUnlearn`/`recentLearnRecords`/`maybeUnlearnOnBackspaceReturn`,
  [AdaptKeyService.kt:6109-6148](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:6109))
  already un-learns immediately once a plain Backspace (composing empty) lands with the caret back at the end
  of one of the last `RECENT_LEARN_HISTORY_SIZE` (5) learned words - and D-348's own armed-tail no-op branch
  only ever engages while `undoTyped != null` (i.e. only right after a genuine autocorrect), so for a plain,
  uncorrected commit this D-248 path should already be reached directly, seemingly regardless of whether
  D-348 is enabled. Two concrete open questions before treating this as closed, matching the user's own
  worry ("manche Dinge sind längst vergessen"): (1) does the punctuation-committed case (the user's own named
  example - deleting the trailing punctuation mark first) actually land the caret back at the word's end in
  time for the very same backspace's own check, or does it need a second Backspace as the user expected; (2)
  is `RECENT_LEARN_HISTORY_SIZE = 5` ever exhausted in a realistic "typed a few more words, then went back"
  scenario, silently letting the target record fall out of `recentLearnRecords` before the user gets back to
  it. Needs an actual device repro (or a targeted unit test walking both scenarios against the real
  `AdaptKeyService`/`SqliteDictionaryStore` pair) before deciding whether anything needs to change at all.

- **D-457 - OPEN, root-cause hypothesis only, not confirmed (2026-09-06).** A learned all-caps acronym (the
  user's example: `"LLM"`, already learned/promoted) is not offered at all while typing its lowercase form
  (`"ll"`/`"llm"`) - no chip appears until the very first letter is typed uppercase. Confirmed NOT a data/SQL
  problem: `SqliteDictionaryStore.unigramsByPrefix()`
  ([SqliteDictionaryStore.kt:947](app/src/main/kotlin/de/froehlichmedia/adaptkey/dictionary/SqliteDictionaryStore.kt:947))
  queries `TABLE_LEARNED` by `wkey LIKE prefix.lowercase() + '%'`, which is already fully case-insensitive and
  would return the learned `"LLM"` entry for either prefix - the entry reaches the candidate pool. Leading
  hypothesis, not yet traced end-to-end: the same class of bug D-440/D-440-followup fixed for the
  composing-*empty* next-word-prediction case (`CapitalisationEngine.capitalise()`'s rule 1, "explicit user
  input is never changed", reading a meaningless `context.explicitFirstUpper` and silently discarding an
  already-correct learned casing) may still apply, unfixed, to the composing-*not*-empty case D-440's own
  write-up explicitly left alone ("that half of the design is correct and unchanged... needs fresh §6
  derivation from live context") - except here `explicitFirstUpper` is not meaningless, it is genuinely
  `false` (the user really did type a lowercase `l`), so rule 1 may be faithfully doing exactly what it is
  documented to do (never override explicit user input) and simply was never taught that an *exact, case-
  insensitive prefix match against an already-learned all-caps entry* is a different situation from an
  ordinary ambiguous-noun default. Needs real tracing through `showSuggestions()`'s candidate-to-chip pipeline
  (not `unigramsByPrefix` itself) before implementing anything - not yet done.

## Current State

- **§449 (v1.2.9): D-357 reopened and fixed for real - mid-word edit + double-tap Shift capitalising the**
  **wrong letter.** Closed once (2026-09-01) as not reproducible; the user captured a real Google Keep device
  log reproducing it and asked for a fresh look, per this project's own "re-derive from real logs, don't
  guess" convention for exactly this class of bug (spec §1's guiding principle).
  
  **Root cause, traced line-by-line against the log**: reclaim "bar" (caret between b/a) → Backspace deletes
  'b' (composing "ar", `composingCursor` now 0) → a second Backspace has nothing left inside composing to
  remove, so `deleteComposingChar()`'s own `composingCursor == 0` branch
  ([AdaptKeyService.kt:3576](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:3576)) reaches
  past composing's own start and deletes the character immediately before it via `deleteOneBefore` - the log
  showed `composingAnchor` shift from 100 to 99 while composing's own text stayed exactly "ar". That branch
  only ever shifted `composingAnchor` to match - correct when the deleted character was an ordinary
  word-boundary delimiter, but the user's own real note had no space there at all: deleting it exposed a
  *letter* of a directly-adjacent word now glued to "ar" with nothing between them (the document reads one
  continuous word), which the old code never absorbed into composing. Retyping 'b' afterward only ever grew
  the still-too-short composing token back to "bar" - not the now-actually-longer real word - so
  `flipFirstInComposing` (G-05's double-tap-Shift toggle) correctly flipped composing's own first character,
  which was by then no longer the true word's first letter. The user's own hunch that this connects to Gemini
  (`reclaimOnCaretMoveSuppressed`) doesn't hold for the mechanism itself - this exact code path fires
  regardless of that flag, and the captured log is from Google Keep, not Gemini - but Gemini's suppression
  plausibly makes the *preconditions* (several plain Backspaces reaching past a short reclaimed token's own
  start) more common in practice, since a suppressed field leans more on explicit Backspace-driven reclaims
  than the ordinary reactive one.
  
  **Fix**: the `composingCursor == 0` branch now checks, *after* `deleteOneBefore` runs, whether the newly
  exposed character (not the deleted one - checked before, this was tried first and found to be the wrong
  character to inspect, see the code's own KDoc) is a letter, using exactly
  [`WordExtent.reclaim`](app/src/main/kotlin/de/froehlichmedia/adaptkey/gesture/WordExtent.kt)'s own
  boundary. A letter means composing must absorb the newly-adjacent word - handled by tearing composing down
  (`finishComposingText` + `clearComposing`) and calling `reclaimWordAtCaret()`, the same mechanism a fresh
  tap into a word already uses, rather than hand-rolling a character-by-character merge of composing's own
  per-character bookkeeping (`composingFlags`/`composingTaps`). `reclaimWordAtCaret()` already correctly
  defers to `applyShiftAfterDelete`'s own `shiftArmedByDelete` result from the deletion just above (D-335),
  confirmed by reading that existing interaction rather than assumed, so Shift state is unaffected by going
  through it here. A non-letter newly exposed (the ordinary case) keeps the original, simpler
  `composingAnchor`-only behaviour unchanged. No new unit test - this is `InputConnection`-glue logic with no
  existing `AdaptKeyService` test harness, this project's own accepted, established gap for this class of
  code; verification is the device repro itself.

- **§448 (v1.2.8): D-451 (AltGr popup haptic, reinstated) + D-453 (double-consonant unfold), one small round.**
  
  **D-451**: `HapticTier` gained a fourth value, `POPUP_OPEN(1)` - plays the identical click effect
  `KEY_PRESS` does (`fireHaptic()`'s own `when (tier)` branch now matches both together), so it is the
  literal "same system-standard click with fallback" the user asked for, but with `minSystemLevel = 1`
  instead of `KEY_PRESS`'s own 3 - it still fires even with the OS's "Haptic feedback" slider at its lowest
  setting. A new `AdaptKeyboardView.playPopupOpenHaptic()` (gated on `hapticsEnabled`, mirroring
  `playKeyFeedback()`) is called from `scheduleLongPress()`'s own `when` block for the one branch that was
  previously silent - a key whose long-press opens a real L-05 alternatives popup (not Shift's Caps Lock,
  not the popup-less listener-callback case). Reverses D-396-followup (v3)'s "beim Aufpoppen braucht es
  keins" call on the user's own later reconsideration - see spec §42's new D-451 note.
  
  **D-453**: `DictionarySuggestionProvider` gained a second D-328-shaped escalation,
  `doubledConsonantPrefixVariants()` - the insertion counterpart of `neighbourPrefixVariants()`'s own
  substitution, run directly after it in `obviousCandidates()` under the identical
  `candidates.isEmpty()`-gated shape. Tries, for every position holding one of `DOUBLING_ELIGIBLE_CONSONANTS`
  (`b d f g k l m n p r s t` - the German-orthography doubling set) not already doubled there, the variant
  with that consonant doubled, fed through the same `diacriticFolding.unfoldCandidates` +
  `unigramsByPrefix` loop as every other prefix variant. `MIN_DOUBLING_PREFIX_LENGTH = 3` (lower than D-328's
  own 5) since the shortest real example ("bite", 4 characters) is well below it, and - unlike a neighbour
  substitution, which multiplies by every adjacent key - this variant set is already naturally bounded by the
  token's own length, so a lower minimum does not risk the same combinatorial blow-up; confirmed the reason a
  shorter typed prefix of the same word (e.g. "bit" for "bitte") never needed this escalation in the first
  place: it is still a genuine literal prefix match on its own; only once the user types *past* the missed
  double does the mismatch - and the need for this escalation - actually arise. Not language-gated, same as
  D-328 itself - a pure keyboard/spelling heuristic, self-limiting by whatever the active language's own real
  dictionary actually contains. Four new `DictionarySuggestionProviderTest` cases (two positive - the user's
  own two examples - plus the length-gate's boundary and just-below-it) - see spec S-09's own D-453 addendum.
  
  Both closed the same round; `AdaptKeyboardView`/`DictionarySuggestionProvider` are unrelated files, no
  shared mechanism between the two beyond both reusing an existing pattern (`HapticTier`'s own tier-per-event
  gating; D-328's own escalation shape) rather than inventing a new one.

- **§447 (v1.2.7): D-454 - Language Packs settings screen restructured**, per the user's own six-point list
  (see the now-closed backlog entry above for the full original ask). `d280_intro` no longer mentions English
  at all - it now gets its own row (`LanguagePacksActivity.buildBuiltInRow()`), identical in shape to every
  other language's (flag glyph + bold endonym heading) but with one permanently-disabled "Built-in" button in
  place of the usual status text + install/remove/download buttons. `rebuild()` now builds one combined,
  sorted list (English plus every `LanguagePackCatalog.ENTRIES` row) instead of iterating the catalog in its
  raw declared order: available-for-typing-right-now (English, or an installed pack) sorts before everything
  else, each half then alphabetical by `Language.endonym` via a locale-independent `Collator.getInstance()`
  (deliberately not a per-language one - this list mixes many languages/scripts at once, so no single
  language's own collation rules are more "correct" here than any other's). `removePack()` now goes through a
  new `confirmRemove()` first - a plain `AlertDialog`, mirroring `BlacklistActivity.confirmRemove()`'s own
  existing pattern exactly rather than inventing a new one.
  
  **The flag glyphs are a new `LANGUAGE_FLAGS: Map<Language, String>`** (companion object, `LanguagePacksActivity`
  itself - UI-only data, deliberately not added to the pure/testable `Language` enum). One glyph per language,
  chosen editorially per the design discussed with the user (usually the language's own eponymous country -
  e.g. `🇩🇪` German, `🇫🇷` French, `🇪🇸` Spanish, `🇵🇹` Portuguese - see spec's own D-454 note for why a single
  "correct" choice does not exist for every language), with two deliberate exceptions worth naming: `🇬🇧🇺🇸`
  for English (the user's own explicit call - both flags shown together, not one), and `🇹🇿` for Swahili
  (Tanzania, not Kenya - the only one of the two where Swahili is the *sole* official national language,
  rather than sharing that role with English, the more defensible single-country pick between real
  alternatives). Every `Language` enum value this app currently ships a layout or pack for has an entry;
  `flagFor()` falls back to an empty string for anything missing one (`UNKNOWN` only, today).
  
  Android-view glue (`LanguagePacksActivity`) - covered by the existing build (`:app:assembleRelease
  :app:testDebugUnitTest`, both green, no new unit tests needed or possible here) rather than a device check,
  per this project's own established convention for this class of screen.

- **§446 (v1.2.6): D-450-followup - Cyrillic-vs-Cyrillic auto-detection, closing the Open TODO §441 first**
  **flagged.** Explicit user request, made once Russian/Ukrainian's own §442/§443 dictionaries had already
  built real `language_profiles.tsv` data - the one remaining piece, Serbian's own profile (deliberately
  skipped when its pack shipped, §440), was built specifically to unblock this
  (`dictionaries/build_language_profiles.py sr`, appended to the bundled asset - now 6,000 lines / 30
  languages total).

  **The actual mechanism needed almost no new logic** - `LanguageClassifier` was already fully generic
  (picks the smallest out-of-place n-gram distance among whatever profiles it is given; nothing in it
  assumes Latin script). `LayoutRegistry` gained a real `CYRILLIC_LANGUAGES` set (the `Script.CYRILLIC`
  subset of `NON_LATIN_LANGUAGES`, replacing the inline `.filter{}` the old gap-documenting canary test
  used). `LanguageProfileLoader` gained `loadProfiles()` (exposing the parsed map, not just a ready-made
  classifier) so `AdaptKeyService.onCreate()` could build a SECOND `LanguageClassifier` instance -
  `cyrillicClassifier` - scoped to exactly `CYRILLIC_LANGUAGES`'s own profiles, alongside the existing
  all-languages `languageClassifier`.

  `resolveDict()` gained a new branch, checked before the old "trust the active non-Latin language
  unconditionally" fallback: when `activeLanguage` is itself a Cyrillic language, `cyrillicClassifier`
  classifies the recent context; a confident (`>= CYRILLIC_SIBLING_MARGIN`, reusing `isForeign()`'s own
  0.15 default rather than inventing a new number), currently-INSTALLED, different sibling routes that
  token to its own dictionary (`suppressAutocorrect = false`, mirroring the existing English-routing case -
  the sibling's own dictionary is perfectly usable, nothing about the text is actually foreign to it). The
  installed-check matters: unlike English (always bundled), Russian/Ukrainian/Serbian are all optional
  downloads - routing to an uninstalled sibling would crash `providers.getValue(...)` in
  `selectActiveDictionary()`, so the new branch checks `providers.containsKey(...)` first.

  **Real active-language promotion, not just per-token routing** - the user's own wording ("ein paar
  Worte tippen... muss das Wörterbuch switchen") asked for the same sustained-usage-promotes-to-a-real-
  switch behaviour D-130 already gives English, not just a silent per-token dictionary override.
  `trackSustainedEnglishUsage`/`consecutiveEnglishWords` generalised to
  `trackSustainedLanguageUsage`/`consecutiveForeignWords`+`consecutiveForeignLanguage` (the language being
  accumulated now needs tracking too, since it is not always the same target the way it was always English
  before) - confirmed this is a strict superset of the old behaviour for the pre-existing English case, not
  a change to it: `tokenLanguage` there was always either `activeLanguage` itself or `Language.ENGLISH`,
  never a third value.

  **One real, substantive difference from the English case, not just a rename**: the promotion now calls
  `applyActiveLanguageToView()` (D-130's own English-only version deliberately did not, since English needs
  no layout of its own - "the layout... is already correct and simply stays exactly as it was"). A
  Russian-to-Ukrainian promotion genuinely needs the physical keys to change (`JcukenLayout`'s own
  `ukrainian` flag), so this call is no longer a no-op for every possible promotion target. Verified safe
  for the English case too: `LayoutRegistry.kindFor` already pins the layout to the system language
  whenever `activeLanguage` is not itself non-Latin, so calling it during an English promotion re-derives
  the identical layout the old code left untouched, just computed instead of assumed.

  New tests: `LayoutRegistryTest` gained `CYRILLIC_LANGUAGES` coverage (replacing the old gap-documenting
  canary, which is retired now that the gap it named is closed); `LanguageClassifierTest` gained a direct
  demonstration that the already-generic classifier discriminates three real Cyrillic siblings from each
  other (not just from Latin text) using real short Russian/Ukrainian/Serbian sample sentences. `resolveDict()`
  itself stays untested directly, same as every other Android-Service-internal branch in this file.
  `versionCode` 501 -> 502, `versionName` "1.2.5" -> "1.2.6". Build + full test suite green.

  Also done same session: `CREDITS.md`'s language-dictionary and language-detection-profile sections,
  stale since long before this round (still described only the original German/English/Greek bundled trio
  and a UDHR-only profile source), rewritten to name all 30 downloadable languages plus bundled English and
  to correctly split the two real profile sources (UDHR for the original 8, real Wikipedia `dict.tsv` data
  for every D-450-round addition since).

- **§445 (v1.2.5): D-450-followup - first Uzbek language pack, last of the four §441 keyboard-layout-only**
  **languages, closing the round.** No new keyboard code needed - Uzbek is Latin-QWERTY-compatible.
  `uzwiki-latest-pages-articles.xml.bz2` (309,775,381 bytes, live-verified) -> 359,331 pages (matching
  `uz.wikipedia.org`'s own live `siteinfo` count, 359,635), 49,202,207 tokens, 1,555,474 distinct words.

  **Real structural finding, checked directly before writing the extractor**: Uzbek's "oʻ"/"gʻ" modifier-
  letter apostrophe is FIVE different Unicode characters in practice (U+02BB 231,133 occurrences, plain
  ASCII U+0027 211,260, U+02BC 30,841, curly quotes 5,653/3,199 - counted in a 50MB dump sample), tangling
  together two genuinely different orthographic phenomena: U+02BB marks "oʻ"/"gʻ" itself, U+02BC marks an
  unrelated loanword glottal stop (sanʼat/"art"). `normalize_apostrophes()` (shared between both extraction
  scripts) resolves every variant contextually - after o/g becomes U+02BB, elsewhere becomes U+02BC -
  without this, the same word would fragment across up to five spellings.

  **No native Wiktionary edition** (confirmed 404) - used the English-coverage fallback (1,860,421 bytes),
  the **thinnest Wiktionary source of any pack this project has built** (4,465 total entries). One positive
  finding unlike Turkish/Azerbaijani: a clean `postp` tag (16 words, no ambiguity) mapped straight to
  `PREPOSITION`. `EXCLUDE_FORM_TAGS` deliberately omits `"error-unrecognized-form"` here (confirmed marking
  genuine words like "uydek"/"like a house", not noise - same finding sh's own script already documents).

  **Calibration check confirmed a genuine, surprising linguistic fact rather than a bug**: verb ratio 26.0x
  (n=1,101, a real sample) - Uzbek's own "-moq" infinitive citation form is rarely used in real prose
  compared to conjugated/converb forms ("boʻlmoq"/46 vastly outranked by "boʻlgan"/168,686), the same shape
  this project's own Greek pack already documents for its own citation convention. Confirmed via direct
  pair inspection across dozens of verbs, not assumed.

  **Net result**: `dict.tsv` 102,548 -> 215,306 rows (+112,758). Bare-noun safety check: 0. `bigram.tsv`:
  410,666 rows (>=10 cutoff) from 1,648,280 raw. Quality gate PASS.

  **Real architectural finding**: no `diacritics.tsv` shipped - `DiacriticTable.parse()` requires single-
  character variants, and "oʻ"/"gʻ" is a two-character digraph, so such an entry would silently become a
  non-functional empty mapping. `hints.tsv` DOES work correctly for the long-press typing side (verified
  against `AlternativeScript.extendsWord()` directly - both characters count as letters, so `o=oʻ`/`g=gʻ`
  correctly append the full sequence) plus the 10 language-neutral assignments every Latin-script pack
  shares. `abbreviations.tsv`: 11 hand-drafted entries. `UzbekRules`: `decimalCommaGluesDigits`=true,
  `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py`'s plain `"qwerty"`
  layout found 1,693 candidates, left uncurated. `language_profiles.tsv` gained a real 200-ngram profile.
  Capitalisation: does not capitalise common nouns.
  **Honesty gate (step 11) NOT satisfied**: not reviewed by an Uzbek speaker, thinnest Wiktionary source of
  any pack built so far, and the `diacritics.tsv` gap is a real, documented limitation - expect this pack to
  need the most follow-up curation of the whole round. Not device-confirmed.
  `versionCode` 500 -> 501, `versionName` "1.2.4" -> "1.2.5". **This closes the four-language Russian/
  Ukrainian/Azerbaijani/Uzbek D-450-followup round** (§442-§445).

- **§444 (v1.2.4): D-450-followup - first Azerbaijani language pack, third of the four §441 keyboard-layout-**
  **only languages to get a real dictionary.** Full Guide §8 pipeline. `azwiki-latest-pages-articles.xml.bz2`
  (323,312,325 bytes, live-verified, a small dump) -> 216,948 pages (matching `az.wikipedia.org`'s own live
  `siteinfo` count, 217,036), 55,499,902 tokens, 1,597,408 distinct words. RAM was not a constraint at this
  scale, so the extractor reused Serbian's own original settings (5 workers, 4M/8M triggers).

  **Real casing fix applied proactively**: Azerbaijani shares Turkish's own dotted/dotless İ/I Unicode
  SpecialCasing rule (confirmed against Unicode's own SpecialCasing.txt) - `azerbaijani_lower()` (Turkish's
  own mapping, reused unchanged) applied in both extraction scripts, and `CasingRulesRegistry` gained a
  one-line `Language.AZERBAIJANI to TurkishCasingRules` entry at the app's own runtime - no new logic needed.

  **No native Wiktionary edition** (confirmed 404) - used the English-coverage fallback (15,724,873 bytes),
  which despite being the "wrong"/thinner file by size turned out structurally rich: real possessive-suffix
  noun paradigms at a similar scale to Turkish's own native edition, no combining stress marks at all. **Like
  Turkish (D-449), Azerbaijani is postpositional, not prepositional** - only 3 words tagged `prep`/
  `prep_phrase` in the whole file, no curated exception list built for the same no-native-fluency reason.

  Calibration ratios checked directly even though none crossed the guide's own outlier threshold: noun
  0.0893 (n=26,669), verb 0.4712 (n=6,685), adjective 0.0972 (n=56, smallest sample calibrated so far). Real
  pairs pulled directly showed genuine Azerbaijani morphology (possessive/predicative suffix chains, real
  intensive-reduplication forms like "yaşıl"->"yamyaşıl"/"very green") - not a bug, confirmed rather than
  assumed.

  **Net result**: `dict.tsv` 117,643 -> 651,702 rows (+534,059 from Wortfamilien completion). Bare-noun
  safety check: 0. `bigram.tsv`: 509,198 rows (>=10 cutoff) from 1,970,071 raw. Quality gate PASS. Unlike the
  two Cyrillic packs, this one DOES ship `hints.tsv`/`diacritics.tsv` - but with a real twist: every
  diacritic-pair letter (ç/ə/ğ/ı/ö/ş/ü) already has its own dedicated primary key on `AzerbaijaniLayout`, so
  `diacritics.tsv` (6 pairs shared with Turkish, "e"/"ə" the Azerbaijani-specific addition) is purely about
  typed-without-diacritic autocorrect recovery, not AltGr access. `hints.tsv` reuses the 10 language-neutral
  assignments every Latin-script pack shares plus a first-draft, explicitly-flagged UX set (manat sign,
  guillemets) for the remaining keys. `abbreviations.tsv`: 13 hand-drafted entries. `AzerbaijaniRules`:
  `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty -
  `confusables_scan.py` gained `"azerbaijani"` (4,409 candidates, left uncurated). `language_profiles.tsv`
  gained a real 200-ngram Azerbaijani profile. Capitalisation: does not capitalise common nouns.
  **Honesty gate (step 11) NOT satisfied**: not reviewed by an Azerbaijani speaker, `hints.tsv`'s own
  currency/punctuation choices are a first draft not a verified fact, not device-confirmed.
  `versionCode` 499 -> 500, `versionName` "1.2.3" -> "1.2.4". Next and last: Uzbek.

- **§443 (v1.2.3): D-450-followup - first Ukrainian language pack, second of the four §441 keyboard-layout-**
  **only languages to get a real dictionary.** Full Guide §8 pipeline. `ukwiki-latest-pages-articles.xml.bz2`
  (2,689,611,157 bytes, live-verified) -> 1,433,142 pages (matching `uk.wikipedia.org`'s own live `siteinfo`
  count, 1,433,512), 389,701,751 tokens, 2,397,542 distinct words. Extractor settings reasoned from the real
  size ratio to Russian's own dump (2.69GB vs. 5.99GB): 4 workers, 3M/6M hapax-pruning triggers. Ukrainian's
  apostrophe (e.g. "п'ять"/"five") is a genuine letter-boundary marker, not punctuation - tokeniser allows it
  inside a run of letters like "-" for compounds.

  **No native Wiktionary edition** (`kaikki.org/dictionary/downloads/uk/` 404s, confirmed against
  `rawdata.html`) - used the English-Wiktionary-coverage fallback (27,908,333 bytes), genuinely thinner than
  Russian's own native source (11,250 nouns/5,348 verbs/5,126 adjectives vs. Russian's 175,567/187,834/
  52,561) - flagged explicitly in the catalog entry as needing more future curation than Russian's round.
  Learned proactively from Russian's own real bug: this fallback carries the identical combining-stress-mark
  shape, so the already-fixed `strip_stress()` (strip only U+0301/U+0300 directly, never NFD-normalise) was
  applied from the first pass - calibration ratios came back sane immediately (noun 0.2708, verb 0.5833,
  adjective 0.4667), no second debugging round needed this time.

  **Net result**: `dict.tsv` 471,516 -> 606,723 rows (+135,207 from Wortfamilien completion, all ratios sane).
  Bare-noun safety check: 0. `bigram.tsv`: 2,700,448 rows (>=10 cutoff) from 6,370,404 raw. Quality gate PASS.
  No `hints.tsv`/`diacritics.tsv` (standalone Cyrillic code points). `abbreviations.tsv`: 22 hand-drafted
  entries. `UkrainianRules`: `decimalCommaGluesDigits`=true (DSTU), `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` gained `"ukrainian_jcuken"` (found 3,832
  candidates, left uncurated, usual no-native-fluency reason). `language_profiles.tsv` gained a real
  200-ngram Ukrainian profile. Capitalisation: does not capitalise common nouns.
  **Honesty gate (step 11) NOT satisfied**: not reviewed by a Ukrainian speaker, not device-confirmed, and
  the thin Wiktionary source makes this one more likely than Russian's to need follow-up curation.
  `versionCode` 498 -> 499, `versionName` "1.2.2" -> "1.2.3". Next: Azerbaijani, then Uzbek.

- **§442 (v1.2.2): D-450-followup - first Russian language pack, first of the four §441 keyboard-layout-only**
  **languages to get a real dictionary.** Full Language Contribution Guide §8 pipeline. By far the largest
  corpus this project has processed: the entire `ruwiki-latest-pages-articles.xml.bz2`
  (5,986,059,762 bytes compressed, ~5x the previous largest, Serbian's own) - live-verified before
  downloading. Free RAM checked first (~6.6GB of 16GB) and the extractor tuned down from Serbian's own
  settings rather than reused unchanged: 3 workers (not 5), more aggressive hapax-pruning triggers (2M/4M,
  not 4M/8M). Real result: 2,116,244 pages (matching `ru.wikipedia.org`'s own live `siteinfo` article count,
  2,116,865, almost exactly), 756,562,343 tokens, 2,410,579 distinct words, 9,687,936 raw bigram rows (>=3).
  RAM stayed stable throughout, never climbing unboundedly.

  **Wiktionary: Russian has a genuinely native edition** (`kaikki.org/dictionary/downloads/ru/ru-extract.jsonl.gz`,
  290,531,383 bytes - directly verified bigger than the wrong English-coverage file's 88,879,732 bytes) - the
  richest native Wiktionary source this project has processed (175,567 nouns, 187,834 verbs, 52,561
  adjectives, 136 prepositions, 21,282 proper nouns).

  **A real, serious bug found and fixed via the Guide's own mandatory calibration-ratio sanity check - not
  dismissed as "the pipeline ran cleanly, so it's probably fine."** The first pass's adjective ratio came
  back 21.0x (n=449), a stark outlier next to noun/verb's own ~0.4x/~0.97x. Pulling the real matched pairs
  directly showed the lemma side was corrupted: extremely common adjectives ("новый"/"new", "другой"/"other",
  "последний"/"last") had lost their trailing й, becoming the wrong but still valid-LOOKING words "новыи"/
  "другои"/"последнии" - the plain-Cyrillic-letters validation regex never caught it, which is exactly why
  this needed the ratio check to surface at all. Root cause: `strip_stress()` (added to remove the source's
  own combining stress marks, e.g. "дома́") NFD-normalised the whole string and dropped every Unicode
  category-Mn character, on an assumption ("no Russian letter decomposes under NFD") that was never actually
  verified and turned out false - й (U+0439) canonically decomposes to и (U+0438) + COMBINING BREVE (U+0306,
  also Mn), ё (U+0451) to е (U+0435) + COMBINING DIAERESIS (U+0308, also Mn), confirmed directly with
  Python's own `unicodedata.normalize`. Fixed by not normalising at all - the source text is already NFC, so
  `strip_stress()` now strips only the two specific stress-mark codepoints (U+0301/U+0300) directly, leaving
  й/ё untouched. Re-extracted from scratch; ratios came back sane (noun 0.3684, verb 0.6875, adjective
  0.6316) and the pure-`ADJECTIVE` tag count alone jumped 601 -> 23,172 in the initial kaikki merge, showing
  how much real data the bug had silently been losing.

  Also verified before writing the extractor, not assumed: Russian's periphrastic imperfective future is
  documented as one literal placeholder row ("бу́ду/бу́дешь… де́лать", tagged just "future") - excluded via the
  established "reject any form containing whitespace" rule (Turkish/Dutch precedent); no dotted/dotless-I
  casing quirk; genuinely prepositional (no Turkish-style prep-tag gap).

  **Net result**: `dict.tsv` 789,336 initial rows -> 1,581,888 after Wortfamilien completion (+792,552:
  154,897 lemmas tagged, 339,224 forms linked, 792,552 generated; ratios noun 0.3684/verb 0.6875/adjective
  0.6316, all sane). Proper nouns: 14,543 tagged, 4,944 unmatched, 1,795 collision-skipped. Bare-noun safety
  check: 0. `bigram.tsv`: 4,349,059 rows (>=10 cutoff, largest of any pack so far) from 9,687,936 raw.
  Quality gate: 0 duplicates/non-positive/orphaned-lemma/bare-NOUN - PASS. No `hints.tsv`/`diacritics.tsv`
  (Cyrillic letters are standalone code points, same as Serbian/Greek); `abbreviations.tsv` hand-drafted
  (26 entries). `RussianRules`: `decimalCommaGluesDigits`=true (GOST 8.417), `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` gained `"russian_jcuken"`/`"ukrainian_jcuken"`
  row layouts (Ukrainian's own added now too, since it's next) and found 1,166 candidate pairs, left
  uncurated for the usual no-native-fluency reason. `language_profiles.tsv` gained a real 200-ngram Russian
  profile. Capitalisation: does not capitalise common nouns, like every other non-German language.
  **Honesty gate (step 11) NOT satisfied**: not reviewed by a Russian speaker, not device-confirmed.
  `versionCode` 497 -> 498, `versionName` "1.2.1" -> "1.2.2". Next: Ukrainian, then Azerbaijani, then Uzbek
  (same D-450-followup round).

- **§441 (v1.2.1): D-450-followup - keyboard layouts (only - no dictionaries yet) for Russian, Ukrainian,**
  **Azerbaijani, and Uzbek, in preparation for building all four as real language packs in a future session.**
  Added `Language.RUSSIAN`/`UKRAINIAN`/`AZERBAIJANI`/`UZBEK` to the enum. Uzbek needs no new layout code at
  all - its own real standard is Latin-QWERTY-compatible (confirmed via research: the two special letters
  `oʻ`/`gʻ` use a modifier-letter apostrophe, handled the same way any other Latin diacritic's `hints.tsv`
  entry would be - dictionary-round work, not layout work).
  
  **Russian + Ukrainian share ONE new `JcukenLayout.kt`, mirroring `KeyboardLayout`'s own QWERTY/QWERTZ**
  **variant-flag pattern - a real architectural question the user raised mid-session, verified letter-by-**
  **letter before answering it.** Both researched against the real Microsoft standards (`KBDRU`/`KBDUR`,
  `kbdlayout.info`/`learn.microsoft.com`) - confirmed the two are the SAME real historical ЙЦУКЕН keyboard
  with only three substituted letters plus one added key, not two unrelated standards: top row differs only
  in its last letter (`ъ` vs. `ї`), middle row differs in position 2 (`ы` vs. `і`) and its last letter (`э`
  vs. `є`), and Ukrainian's own bottom row adds a leading `ґ` where Russian's own leading slot is punctuation
  (a real desktop ISO-key artifact, not a letter either way). `JcukenLayout.rows(..., ukrainian: Boolean)`
  captures this with one shared implementation and two row-string constants per row, exactly like
  `KeyboardLayout.rows(qwerty: Boolean)` does for Latin. Russian's own `ё` (a real, distinct letter with no
  slot in this app's mobile row shape, same practical constraint every desktop-only extra key already has
  here) is offered as a long-press secondary on `е` instead, mirroring `GreekLayout`'s own accent-on-vowel
  convention - Ukrainian does not get this secondary at all, since `ё` is not part of its alphabet.
  
  **The user also asked whether the already-built Serbian layout could be refolded into this same
  Russian-based "variant" pattern - verified directly and answered no, with the real evidence, not just
  reasserting the original design call:** Serbian's own JUSCII-descended layout (see its own D-450-followup
  entry above) is phonetically mapped onto the LATIN QWERTZ physical grid (its own `з`/`у`/`и`/`о`/`п` sit
  exactly where Latin `z`/`u`/`i`/`o`/`p` do) - a completely different historical lineage from Russian's own
  ЙЦУКЕН, which has its own independent typewriter-derived arrangement unrelated to Latin key positions at
  all. The two share no meaningful structural overlap (`у` appears in both, but at column 7 in Serbian vs.
  column 3 in Russian) - `SerbianLayout` correctly stays its own independent object.
  
  **A real gap in the D-450-followup Cyrillic generalisation, found and fixed while wiring these two in, not**
  **before shipping**: `LayoutRegistry.NON_LATIN_LANGUAGES`'s own canary test (added for Serbian, see above)
  checked for a shared `LayoutKind`, not a shared *script* - Russian and Ukrainian each correctly get their
  own distinct `LayoutKind` (real, different physical layouts), so that test would have kept silently passing
  even though they - and Serbian - are all genuinely Cyrillic and share the exact same "AdaptKeyService.
  resolveDict() cannot yet tell these apart" concern the canary existed to catch. Fixed properly, not
  patched: added a real `Script` enum (`LATIN`/`GREEK`/`CYRILLIC`) and a `LayoutRegistry.scriptFor(Language)`
  function, and rewrote the canary to check for a shared `Script` instead. It now correctly documents the
  real, deliberate state: Serbian/Russian/Ukrainian **do** share a script, `resolveDict()` still trusts
  whichever is active unconditionally (unchanged, no code needed there), and this is a conscious decision,
  not a silent gap - because no per-language Cyrillic `language_profiles.tsv` trigram data exists yet for
  any of the three to build a real same-script classifier from (dictionary-pipeline work, added to Open
  TODOs below).
  
  **Azerbaijani needed its own full `AzerbaijaniLayout.kt` - the real standard turned out to be a much**
  **bigger restructuring than an earlier casual description suggested, caught by pulling the real Microsoft**
  **table instead of trusting a summary.** An earlier web-search summary (from the session's own prerequisites
  check) described it as "QÜERTY - Ü replaces W", implying a single-letter QWERTZ-style swap; the real
  `KBDAZST` standard (`learn.microsoft.com/en-us/globalization/keyboards/kbdazst.html`) shows `q`/`w` do not
  even sit at their ordinary QWERTY positions at all (`q` is on the bottom row, `w` is dropped from the
  primary rows entirely) - a genuine cross-row restructuring, the same "AZERTY" category (D-314) as French's
  own layout, not a same-row variant flag. All 32 letters of the Azerbaijani Latin alphabet (including the
  dotted/dotless İ/I pair - `TurkishCasingRules`'s own Unicode `SpecialCasing.txt` `tr`/`az` rule applies
  unchanged here, needing only a future registry entry) sit directly on the three primary rows with no AltGr
  dependency, matching the real standard. Still genuinely Latin script, so it keeps the L-05/C-08
  `letterHints` AltGr overlay and reuses `KeyboardLayout.topRowKey`/`letterKey` so `p`/`o`/`a`/`e`/`n` keep
  their existing math-symbol popups (D-99) wherever they land - confirmed this lookup is keyed by character,
  not row/column position, so it works unchanged for a fully rearranged layout.
  
  New `KeyboardProximityRussianCyrillic`/`UkrainianCyrillic`/`Azerbaijani` (each matching its own layout's
  rows exactly - three separate grids, not shared, since the real letter positions genuinely differ even
  where Russian/Ukrainian share most of their skeleton). New tests: `JcukenLayoutTest` (12 cases covering
  both variants + the real 33/33-letter completeness check), `AzerbaijaniLayoutTest` (9 cases), three new
  `KeyboardProximity*Test` files, `LayoutRegistryTest` additions (`scriptFor` coverage, the rewritten canary,
  Uzbek's own QWERTY-default case). 1,526 unit tests green (was 1,487).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green.
  
  **Explicitly out of scope this round, by the user's own direction**: no `LanguageRules`/dictionary/catalog
  work for any of the four - layouts only, so the actual language packs (full Wikipedia-dump extraction,
  Wiktionary-based Wortfamilien completion, calibration/quality-gate/confusables-scan checks, catalog
  entries) are a deliberately separate, future session's work. New Open TODO added below for the real
  Cyrillic-vs-Cyrillic classifier gap this round's own `resolveDict()` KDoc update documents.

- **§440 (v1.2.0): D-450-followup - first Serbian language pack, closing the one deferred exception from**
  **the 18-language D-450 round: a real Cyrillic keyboard layout.** Added `LayoutKind.SERBIAN_CYRILLIC` +
  `keyboard/SerbianLayout.kt`, researched (not guessed) against the real, established standard - Microsoft's
  own `KBDYCC` "Serbian (Cyrillic)" layout, the JUSCII-descended QWERTZ-based physical layout every Serbian
  Windows/Linux install ships (verified directly against `kbdlayout.info/KBDYCC` and
  `learn.microsoft.com/.../kbdycc.html`). The 30-letter Vuk Karadžić alphabet maps unevenly onto the QWERTZ
  grid - 13 letters top row, 11 middle, 6 bottom - kept faithfully asymmetric rather than rebalanced for a
  tidier mobile look, since matching genuine muscle memory is the whole point; the historical `Ѕ` (Macedonian
  dze) filler at the desktop layout's own ISO-only key is correctly dropped (not part of the real Serbian
  alphabet at all). Unlike every Latin-script language, no `letterHints`/`hints.tsv`/`diacritics.tsv` -
  Serbian Cyrillic's five letters not shared with Russian/Bulgarian Cyrillic (Ђ/Љ/Њ/Ћ/Џ) are standalone code
  points, not diacritic composites, the same reasoning [GreekLayout.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/keyboard/GreekLayout.kt)
  already established for the only other non-Latin-script pack. New `KeyboardProximitySerbianCyrillic`
  (matches the layout's own rows exactly) and a `"serbian_cyrillic"` row layout added to
  `confusables_scan.py`.
  
  **A real architectural generalisation, not just a new layout**: `LayoutRegistry.NON_LATIN_LANGUAGES` was
  Greek-only by construction (`filterValues { it == GREEK }`) - generalised to `filterValues { it !in
  LATIN_KINDS }` so any future non-Latin script gets the same treatment automatically.
  `AdaptKeyService.resolveDict()`'s own Greek-only unconditional-trust branch (`if (activeLanguage ==
  Language.GREEK) ...`) was generalised the same way, for a real correctness reason found while wiring
  Serbian in, not merely for symmetry: `LanguageClassifier.isForeign()`'s n-gram profiles are exclusively
  Latin-script and its own guard specifically measures GERMAN's margin (a known, already-documented
  simplification) - running Cyrillic text through it while Serbian is active would almost certainly misfire,
  silently suppressing Serbian's own autocorrect. **Deliberately NOT built**: real Cyrillic-vs-Cyrillic
  switching logic - there is only one Cyrillic language today, nothing to distinguish from, and building
  speculative same-script classification with no second real profile to verify it against would be pure
  guesswork. Instead, a new `LayoutRegistryTest` canary test asserts no two languages share a non-Latin
  `LayoutKind` yet - it is designed to **fail** the moment a second Cyrillic language joins Serbian, forcing
  that future round back to this exact spot rather than letting the gap survive silently. (Session note: the
  user asked mid-session whether restructuring the check to "look at other installed same-layout languages"
  would avoid a future code change - the answer, recorded here for continuity, is no: with zero Cyrillic
  profiles existing today, any such branch would be dead code with identical behaviour; the canary test
  achieves the actual goal - a loud, automatic reminder - without speculative branching.) Also: `Language
  .SERBIAN`'s own `endonym` corrected `"Srpski"` -> `"Српски"` (Cyrillic, matching this pack's own script
  commitment, the same convention Greek's `"Ελληνικά"` already follows).
  
  **Two real bugs found and fixed in the shared `dictionaries/sh/extract_wiktionary.py` extractor** (the
  Croatian/Bosnian/Serbian shared "sh" Wiktionary source, per kaikki.org's own single-edition treatment of
  all three) - both found via direct data inspection and the Guide's own mandatory calibration-ratio check,
  neither caught during the original hr/bs round since both use the unaffected Latin path:
  1. `usable_forms()`'s Cyrillic branch originally kept a form only when explicitly `"Cyrillic"`-tagged - but
     a Cyrillic-headword entry (e.g. "жена") documents its OWN paradigm in Cyrillic by default, tagging the
     rare Latin alternate `"romanization"` instead (confirmed: "жена" has 20 real forms, 0 tagged
     `"Cyrillic"`) - the exact mirror of a Latin-headword entry's own tagging. Nearly every Serbian word got
     zero usable forms as a direct, mechanical consequence. Fixed by making the exclusion tag
     direction-dependent; no transliteration needed since the source already provides genuine native-script
     forms either way.
  2. After fixing (1), the calibration check still measured an implausible verb ratio, traced to
     "клечати"[freq 1] pairing with "био"/"буде"/"били"/"буду" at up to 54,830x: a periphrastic/compound-tense
     raw form (e.g. "будем клечао") carries one `links` pair PER WORD, and `resolve_form_string()` naively
     read only `links[0]` - always the auxiliary verb's own form, never the actual content verb. Same
     symptom-class as French's D-444-followup/Dutch's D-447 (different root cause each time). Fixed Dutch's
     way: skip the whole form when its raw string is multi-word, checked BEFORE any `links` substitution.
     Directly spot-checked (not assumed) that the already-shipped Croatian pack shows no auxiliary-linked
     absurd ratios in practice, so no republish was needed there - the fix is shared going forward regardless.
  
  Script confirmed the same way D-450's own Croatian entry already recorded: a direct 500-page sample of
  this pack's own Wikipedia dump measured 839,282 Cyrillic letters vs. 151,666 Latin (~5.5:1) - Cyrillic
  matches both the dominant real corpus content and Serbia's own constitutional primary script.
  `srwiki-latest-pages-articles.xml.bz2` (1.16GB compressed, re-downloaded this round - the previous
  session's own copy had been cleaned up) was processed via the same multiprocessing/hapax-pruning
  extractor - 708,165 real pages, 152,283,830 real tokens, the largest single-language token count of any
  D-450-family round so far (ahead of Turkish's 136.69M).
  
  **Net result**: `dict.tsv` 315,689 rows (217,476 initial Wikipedia-frequency + kaikki-POS merge + 98,213
  from Wortfamilien completion; calibration ratios noun=0.3333 (n=26,769), verb=3.3333 (n=1,982 - elevated
  but explicable: productive "noun-from-verb" deverbal nouns like "питање"/"question" from "питати"/"to ask"
  genuinely out-frequency their own parent infinitive, consistent with the Balkan-Sprachbund
  infinitive-avoidance feature Serbian shares with Bulgarian/Macedonian/Romanian), adjective=0.6512
  (n=19,975) - all sane, verified pair-by-pair). POS tagging: 198,290 unrecognised-by-kaikki kept (`OTHER`),
  1,711,924 dropped, 0 common-English-word contamination (structurally impossible, Cyrillic vs. Latin
  script). Wiktionary matching: 18,293 lemmas tagged, 3,047 unmatched; 49,220 existing forms linked, 98,213
  generated. Proper nouns: 1,018 tagged, 92 unmatched, 62 skipped as collisions. Mandatory bare-noun safety
  check: 0. `bigram.tsv`: 1,144,589 rows (>=10 cutoff) from 3,595,744 at the raw >=3 floor. Quality gate
  clean (0 duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows).
  
  `abbreviations.tsv`: a 12-entry list transliterated directly from Croatian's own already-vetted one (same
  South Slavic convention, same shared Wiktionary source language family), not drafted from scratch.
  `SerbianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true (directly verified - Serbia uses
  a decimal comma), `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty -
  `confusables_scan.py sr/dict.tsv serbian_cyrillic 30` found 2,621 candidate pairs, left deliberately
  uncurated per the same reasoning every non-German round documents. Capitalisation (Guide step 8): does NOT
  capitalise common nouns like German - `merge_dict.py`'s own `resolve_tags()` already guarantees this
  structurally (a bare `NOUN` gains `OTHER` too), the same default every non-German language built via this
  template shares. New tests: `SerbianLayoutTest`, `KeyboardProximitySerbianCyrillicTest`,
  `LanguageRulesTest`'s `Serbian resolves to SerbianRules` block, `LayoutRegistryTest` additions incl. the
  canary test above. 1,487 unit tests green (was 1,462). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
  
  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed by anyone
  who actually speaks Serbian. Real, full-dump corpus scale (152.28M real tokens, the largest of any
  language this project has built) and a real, freshly-fixed Wortfamilien pipeline - but the same "pretty
  good, not done" ceiling as every other pipeline-built language. Not device-confirmed either. **This closes**
  **the Cyrillic-keyboard-layout gap named at the end of the 18-language D-450 round.**

- **§439 (v1.1.78): D-450-followup - `language_profiles.tsv` (A-03 trigram language detection) built for**
  **all seventeen D-450-round languages, closing a gap named as an "accepted, named gap" in every one of**
  **their own §422-§437 entries.** The original builder (a throwaway scratchpad script that worked from a
  small UDHR sentence corpus, 80/20 train/eval split) is confirmed gone from this repo, per the existing
  Progress.md note - reconstructed as a real, **committed** script this time (`dictionaries/
  build_language_profiles.py`, not scratchpad, since a future language will need it again), working from a
  much bigger real source than the original ever had: each language's own already-extracted, already-shipped
  `dict.tsv` word-frequency table (a whole Wikipedia dump's worth of real word frequencies). Byte-for-byte
  parity with `language.CharNgrams.normalize()`/`rankedProfile()` was the one correctness-critical point
  (same as A-03's original Python/Kotlin parity requirement) - lowercase, non-letter runs collapse to a
  single space, trim, wrap with one leading/trailing space, top-200 bi+trigrams by weighted count, ties
  broken by the n-gram ascending; each `dict.tsv` row's own frequency is the weight (equivalent to treating
  every dictionary word as that many independent, space-wrapped occurrences - reconstructs the true n-gram
  frequency distribution for every n-gram that does not straddle two different words, which is the dominant
  majority of any top-200 profile). Added: Swedish, Norwegian Bokmål, Danish, Finnish, Czech, Slovak,
  Hungarian, Romanian, Croatian, Bosnian, Estonian, Latvian, Lithuanian, Malay, Indonesian, Swahili, Tagalog
  (`sv`/`nb`/`da`/`fi`/`cs`/`sk`/`hu`/`ro`/`hr`/`bs`/`et`/`lv`/`lt`/`ms`/`id`/`sw`/`tl`) - 17 x 200 = 3,400 new
  lines appended after the existing 8 languages' 1,600 (never touched), `language_profiles.tsv` now 5,000
  lines / 25 languages total.

  **Real verification against the A-03 classifier, not just a build-and-ship**: extended
  `app/src/test/resources/language_eval.tsv` (116 -> 478 lines) with independent held-out real text for all
  17 - UDHR translations (`eric-muller/udhr`, same source the original 8 used) for the 16 that have one
  (confirmed directly via the repo's own file tree; Norwegian's is `nob`, Romanian's newest is `ron_2006`,
  Bosnian's is `bos_latn` not the Cyrillic `bos_cyrl` variant), and - **Malay has no UDHR translation at
  all**, a real, confirmed gap in that source, not an oversight - five real Malay Wikipedia article extracts
  (`ms.wikipedia.org`, topics: Malaysia, Bahasa Melayu, Kuala Lumpur, Bola sepak, Matematik) instead, clearly
  documented as a different-source exception in `LanguageDetectionEvaluationTest`'s own KDoc. This is
  actually a **cleaner** split than the original 8 languages' own same-domain 80/20 UDHR split: profiles are
  now built from one real corpus (Wikipedia word frequencies) and evaluated against a genuinely independent
  one (UDHR / Wikipedia article prose), not different slices of the same small source.

  **A real, fully-explained accuracy finding, not a silently-lowered bar**: with 25 languages the held-out
  suite's accuracy came in at 0.864, below the original 8-language 0.90 floor. Diagnosed directly (a
  standalone Python re-implementation of the exact same normalize/rank/distance algorithm, cross-checked
  byte-for-byte against the real Gradle failure's own reported accuracy number before being trusted) rather
  than just loosening the assertion: **every single misclassification in the whole 478-sentence corpus lands
  on a specific, named, explicable confusable language, never a random unrelated one.** Four groups, all
  genuinely near-identical or closely-related national-standard language pairs: Bosnian/Croatian, Czech/
  Slovak, Indonesian/Malay, Lithuanian/Latvian (one-directional - Latvian was never mistaken for Lithuanian),
  plus the Scandinavian trio Swedish/Danish/Norwegian-Bokmål and a single stray Romanian/Portuguese instance
  (two Romance languages). One further, less obvious empirical finding, not a known linguistic-relatedness
  fact: Swahili is measurably confusable with Tagalog/Indonesian/Malay specifically, most likely because all
  four share a simple open-CV-syllable shape despite belonging to unrelated language families (Bantu vs.
  Austronesian) - confirmed by inspecting every Swahili miss individually, all landing in that exact set,
  never elsewhere. `LanguageDetectionEvaluationTest` now encodes this as a real, lasting regression guard
  rather than just a lower number: a new `CONFUSABLE_GROUPS` table plus a new test
  (`misclassifications never escape a known closely-related-language group`) asserts every miss across all
  25 languages lands inside its own group - a future stray miss to an unrelated language fails this test
  even though the (honestly, evidence-based) lowered blanket floor (0.90 -> 0.85, real headroom above the
  measured 0.864) might still pass. 1,462 unit tests green (was 1,461).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green.

- **§438 (v1.1.77): D-449-followup - Turkish's dotted/dotless İ/I capitalisation, closing the "still-open**
  **design question" named at the end of D-449 (§423/v1.1.62).** `CapitalisationEngine` gained a new,
  deliberately separate seam - `capitalisation/CasingRules` (interface + `DefaultCasingRules` + a
  `CasingRulesRegistry` keyed by `Language`, mirroring `LanguageRules`/`DiacriticFolding`'s identical D-410/
  D-435 "delegate to the active language, default to doing nothing special" shape) - rather than a tenth hook
  on `LanguageRules` itself: every existing `LanguageRules` hook encodes German compounding/inflection
  grammar with no bearing on single-character case mapping, and `CapitalisationEngine` is the only reader, so
  the two seams are kept apart on purpose. `CapitalisationEngine` now takes an optional
  `casing: CasingRules = DefaultCasingRules` constructor param (default preserves every existing caller's/
  test's behaviour byte-for-byte); its three `word.uppercase()`/`replaceFirstChar { it.uppercaseChar() }`/
  `replaceFirstChar { it.lowercaseChar() }` call sites now go through `casing.uppercaseAll`/`uppercaseFirst`/
  `lowercaseFirst`. `AdaptKeyService.installStores()` resolves `CasingRulesRegistry.rulesFor(language)` per
  language the same way it already resolves `LanguageRulesRegistry`/`KeyboardProximityRegistry`.
  
  `TurkishCasingRules` (`capitalisation/`, not `language/` - a character-casing convention, not a
  compounding/inflection grammar rule) implements the real Turkic dotted/dotless pair, verified directly
  against Unicode's own `SpecialCasing.txt` `tr`/`az` section rather than guessed: of its four Turkish-
  specific case-mapping lines, only two actually differ from Kotlin's locale-invariant default - `'ı'`
  already uppercases to `'I'` and `'İ'` already lowercases to `'i'` under the ordinary Unicode simple-case
  tables, so the implementation is a genuinely minimal two-character override: uppercasing plain `'i'` yields
  `'İ'` (not the ordinary `'I'`), and lowercasing plain `'I'` yields `'ı'` (not the ordinary `'i'`). Every
  other character defers to `Char.uppercaseChar()`/`lowercaseChar()` unchanged.
  
  New tests: `CasingRulesTest` (7 cases: `DefaultCasingRules` matches historical behaviour incl. NOT dotting
  Turkish `i`, registry resolution for Turkish vs. every other language incl. `UNKNOWN`) and
  `TurkishCasingRulesTest` (7 cases: `istanbul`->`İstanbul`, `Işık`->`ışık`, `ışık`->`IŞIK`, `İstanbul`->
  `istanbul`, whole-word `izmir`->`İZMİR`, non-i letters unaffected, empty-string no-op). 1461 unit tests
  green (was 1454). `:app:assembleDebug`/`:app:testDebugUnitTest` green.
  
  **Scope note, asked and answered the same session**: the user asked whether Azerbaijani/Uzbek (both
  deferred as Turkish-related, not yet in the `Language` enum) have further prerequisites now that this
  casing seam exists. Researched, not guessed: **Azerbaijani** shares the identical dotted/dotless İ/I pair
  with Turkish (Unicode's own `SpecialCasing.txt` groups `tr`/`az` under the same rule) - `TurkishCasingRules`
  would apply unchanged once `Language.AZERBAIJANI` exists (just add a registry entry, no new logic), but its
  real-world standard keyboard is **not** plain QWERTY - the "QÜERTY" layout (Ü replaces W, W not directly
  reachable) - so a dedicated layout is a real prerequisite, the same shape as Serbian's Cyrillic layout gap
  below, not a data-only round; decimal separator is comma, like every language implemented so far; kaikki.org
  has no native `az.wiktionary.org` edition, only the (thin) English-Wiktionary-derived extraction - coverage
  depth unverified, would need direct measurement before committing to a round. **Uzbek** does **not** have
  the dotted/dotless distinction at all in its modern Latin alphabet (confirmed - only Turkish, Azerbaijani,
  Crimean Tatar, Gagauz, Kazakh and Tatar use it) - `TurkishCasingRules` would not apply to it; its standard
  layout is QWERTY-compatible (the two special letters `oʻ`/`gʻ` use a modifier-letter apostrophe, typically
  typed via AltGr or a plain apostrophe substitute - no new layout class needed, unlike Azerbaijani); kaikki.org's
  own Uzbek dictionary is tiny (~4,174 words in the English-Wiktionary-derived extraction) - a real, verified
  coverage concern, not yet investigated further. Neither language was added to the `Language` enum or built
  this round - this was a prerequisites check only, not a go-ahead to build.

- **§437 (v1.1.76): D-450 - first Swahili and Tagalog language packs, closing the entire 18-language**
  **autonomous round begun with Swedish.** Added `Language.SWAHILI` (`"sw"`, `"Kiswahili"`) and
  `Language.TAGALOG` (`"tl"`, `"Tagalog"`) to the enum. Neither has a native Wiktionary edition - both built
  from the English Wiktionary's own fallback coverage (7.0MB/14.7MB).

  **Swahili**: real Bantu noun-class system documented correctly (e.g. "tao" -> "matao", class-vi plural).
  Verb canonical forms carry a bound-root leading hyphen ("-soma") - already excluded via the `"canonical"`
  tag, `entry["word"]` itself always clean. A genuine English-prose noise pattern ("positive subject concord
  + -lisoma") caught for free by whitespace rejection. Like Malay, Swahili's own main countries (Kenya,
  Tanzania) are former British territories and do NOT use a comma decimal separator - verified directly.

  A real, honestly-investigated calibration finding: the adjective ratio (68.08x, n=10) is genuinely elevated
  but reflects real grammar, not a bug - "ote" ("all", bare citation form) is dominated by its own real
  noun-class-agreement variants (yote/wote/zote/kote/vyote/nyote/mote/lote, all correctly-spelled, agreeing
  with Swahili's 15+ noun classes), which are individually far more common than the abstract bare stem since
  Swahili grammar requires the agreement prefix in real usage. Verified every pair before accepting it as
  sane rather than flagging it as a bug.

  `swwiki` (70MB compressed, smallest corpus this round): 125,849 pages, 16,086,920 tokens. `dict.tsv`
  27,677 -> 52,979 rows (+25,302 generated; ratios noun=0.8805 (n=1,080), verb=4.1482 (n=1,184, elevated but
  plausible given Swahili's rich conjugation), adjective=68.0769 (n=10, verified sane above)). Only 7,059
  lemmas + 406 proper nouns of 27,677 base entries (~27.0%) carry a real link - a comparatively rich ratio.
  `bigram.tsv` 118,034 rows. `confusables_scan.py`: 1,813 pairs.

  **Tagalog**: entries carry an alternative Baybayin-script spelling (historical pre-colonial script),
  excluded explicitly (same pattern as Malay's Jawi). The `"canonical"` headword carries a stress accent
  ("basín") - already excluded, and the real completive/progressive/contemplative aspect forms are
  confirmed clean (no accent marks, no `links`-recovery needed unlike Serbo-Croatian/Lithuanian). Like Malay
  and Swahili, the Philippines follows the American period-decimal convention, NOT a comma - verified
  directly.

  `tlwiki` (88MB compressed): 49,336 pages, 15,950,161 tokens. `dict.tsv` 36,957 -> 53,745 rows (+16,788
  generated; ratios noun=0.8719 (n=46), verb=1.2778 (n=1,739), adjective=0.2173 (n=59), all sane). Only
  15,498 lemmas + 3,027 proper nouns of 36,957 base entries (~41.9%) carry a real link - the RICHEST ratio of
  any fallback-sourced language in this entire round.

  Both: mandatory bare-noun safety check 0; quality gate clean. `hints.tsv`/`diacritics.tsv`: neither
  language has any diacritic letters (plain 26-letter Latin alphabets), so both `diacritics.tsv` files are
  deliberately empty; `t=Sh` (Swahili, generic East African shilling) / `t=₱` (Tagalog, Philippine peso).
  `abbreviations.tsv`: 8-entry hand-curated lists each.

  `SwahiliRules`/`TagalogRules` (`LanguageRulesRegistry`): both `decimalCommaGluesDigits`=**false** (see
  findings above), `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty -
  `confusables_scan.py` found 1,813 (Swahili) / 1,001 (Tagalog) candidate pairs, left deliberately
  uncurated. New tests: `LanguageRulesTest` gained `Swahili resolves to SwahiliRules` and `Tagalog resolves
  to TagalogRules` cases plus their own mirroring test blocks (both asserting `false` for the decimal-comma
  test).

  **Honesty gate (step 11) - deliberately NOT claimed satisfied for either**: neither pack reviewed by a
  native speaker; both thinner-than-native-edition Wortfamilien/POS coverage; Swahili's own much larger
  Bantu noun-class/verb-affixation grammar and Tagalog's own much larger focus/trigger verb-affixation
  system are not implemented as dedicated grammar hooks this round. Neither device-confirmed.

  **This closes the entire 18-language autonomous round begun with Swedish (D-450).** 17 packs built and
  shipped this round (Swedish, Norwegian Bokmål, Danish, Finnish, Czech, Slovak, Hungarian, Romanian,
  Croatian, Bosnian, Estonian, Latvian, Lithuanian, Malay, Indonesian, Swahili, Tagalog) - Serbian remains
  the one explicitly deferred exception, pending its own dedicated Cyrillic-keyboard-layout discussion.

- **§436 (v1.1.75): D-450 (continued) - first Indonesian language pack, eighteenth of the 18-language round -**
  **a real ~800x calibration bug found (reduplicated words modeled backwards as their own separate lemma**
  **entries) and fixed, then applied proactively to Malay too.** Added `Language.INDONESIAN` (`"id"`,
  `"Bahasa Indonesia"`) to the enum. Native edition confirmed (2.87MB - genuinely SMALLER than the wrong
  file's 9.77MB, confirming the Guide's own warning that the size relationship runs either direction; the
  native URL's own structure, not size, confirms correctness).

  Indonesian's own verb morphology is documented far more richly than Malay's own native edition (same
  language family) - real voice/aspect prefixes (ku-/kau-/di-/ter- passive constructions), the bare
  transitive root, and the imperative -lah suffix are all real forms[] entries of the same lemma (confirmed
  against "melali"/"merundung"). Same reduplication-plural pattern as Malay for nouns.

  **A real calibration bug, caught by the mandatory ratio sanity check**: the first adjective pass found an
  impossible 802.5x ratio (n=18). Investigated directly: this source documents a reduplicated-plural/
  intensive word (e.g. "berat-berat") as its OWN separate dictionary entry, whose own forms[] then lists the
  bare singular/root ("berat") as if it were backwards a "form of" the reduplicated entry - confirmed against
  the raw entry. The correct direction (base -> reduplicated) is separately and correctly present via the
  base word's own entry; both directions coexist redundantly in this source. Fixed by skipping any self-
  reduplicated (`X-X`) entry as a lemma - re-run confirmed the adjective ratio dropped to n=0 (the entire
  contaminated sample was this artifact), noun/verb ratios stayed sane throughout. The same fix was applied
  proactively to Malay's own extractor too, since both share the identical kaikki source convention.

  The entire `idwiki-latest-pages-articles.xml.bz2` (1.25GB compressed, the largest single Wikipedia dump
  processed this round) was processed via the same multiprocessing/hapax-pruning extractor - 790,313 real
  pages, 189,427,790 real tokens, 2,123,590 distinct words, 4,454,833 raw (>=3) bigram rows.

  **Net result (post-fix)**: `dict.tsv` 209,225 rows (188,870 initial + 20,355 from Wortfamilien completion -
  108 noun + 20,247 verb-delta + 0 adjective-delta generated forms, Indonesian's own real verb richness
  showing directly in the verb-delta size; calibration ratios noun=0.0134 (n=105), verb=1.2628 (n=5,256),
  adjective=0.2000 (n=0, post-fix, honestly left at 0 rather than forced)). POS tagging: 176,057 words kept
  unrecognised-by-kaikki, 1,918,630 dropped, 16,090 removed as common-English-word contamination. Wiktionary
  matching: 12,916 lemmas tagged, 6,247 unmatched; 5,434 existing forms linked, 20,355 generated. Proper-noun
  handling: 2 tagged, 0 unmatched, 0 skipped - confirming this native edition's own thin proper-noun coverage
  (only 3 raw "name" entries in the whole source, similar to Czech's own D-450 finding). Mandatory bare-noun
  safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,548,045 rows (>=10 cutoff) from 4,454,833 raw. Quality
  gate: 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows
  - PASS.

  Being native-sourced, Indonesian carries no fallback-language documentation requirement, but for context:
  only 12,916 lemmas + 2 proper nouns of 188,870 base entries (~6.8%) carry a real POS/lemma link, reflecting
  this native edition's own real proper-noun-coverage gap and the post-fix absence of adjective calibration
  signal, not fallback-source thinness.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Indonesian's own: NO diacritic letters at all (the
  plain 26-letter Latin alphabet, same as Malay) - `diacritics.tsv` deliberately empty, every `hints.tsv`
  slot generic typography with `t=Rp` (rupiah). `abbreviations.tsv`: a hand-curated 8-entry list.

  `IndonesianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true (Indonesia's own real
  convention follows its Dutch colonial history, genuinely different from Malay's own British-derived period
  convention - verified directly rather than assumed identical to its close linguistic relative),
  `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 3,033
  candidate pairs, left deliberately uncurated.

  New tests: `LanguageRulesTest` gained an `Indonesian resolves to IndonesianRules` case plus its own
  mirroring test block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually
  speaks Indonesian. Real, full-dump corpus scale (189.43M real tokens) and a genuinely native-sourced
  edition with real, richer verb morphology than Malay - but still "pretty good", not native-reviewed
  quality, and with a confirmed thin proper-noun coverage gap specific to this source. Not device-confirmed
  either.

  **Seventeen of the originally-requested 18 languages are now built** (Swedish, Norwegian Bokmål, Danish,
  Finnish, Czech, Slovak, Hungarian, Romanian, Croatian, Bosnian, Estonian, Latvian, Lithuanian, Malay,
  Indonesian, plus Serbian explicitly deferred pending its own Cyrillic-keyboard discussion) - Swahili and
  Tagalog remain as the final two languages of this round.

- **§435 (v1.1.75, committed together with §436): D-450 (continued) - first Malay language pack,**
  **seventeenth of the 18-language round -**
  **one of only three native-edition languages this round, with a real discovery that Malay does NOT use a**
  **comma decimal separator (British/American convention instead).** Added `Language.MALAY` (`"ms"`,
  `"Bahasa Melayu"`) to the enum. Native edition confirmed (5.83MB, correctly bigger than the wrong file's
  4.11MB).

  Real findings, confirmed by direct inspection: (1) Malay entries carry an alternative Jawi-script
  (Arabic-based) spelling, excluded (Malay's own everyday script is Rumi/Latin). (2) Malay's own plural is
  formed by REDUPLICATION ("bank" -> "bank-bank"), not a suffix - the standard hyphen-joining `VALID_FORM_RE`
  already handles this. (3) Malay is genuinely isolating/agglutinative-by-affixation with near-zero verb
  conjugation data in this source (real meN-/di-/ber- morphology is documented as separate lemmas) - honestly
  small Wortfamilien completion for verbs/adjectives, a real characteristic not a bug. (4) no `"prep"` tag
  exists at all - 0 `PREPOSITION` rows, the same honest pattern as Turkish's own D-449 entry for a different
  reason. (5) **Malay (Malaysia) does NOT use a comma decimal separator** - it follows the British/American
  period convention, the first implemented language where this differs from the project's own default,
  verified directly rather than assumed. (6) a real calibration bug shared with Indonesian's own source
  convention (found while building Indonesian, next entry below, and applied proactively here too): this
  source documents a reduplicated word as its OWN separate dictionary entry whose own forms[] lists the bare
  singular as if it were backwards a "form of" the reduplicated entry - fixed by skipping any self-
  reduplicated (`X-X`) entry as a lemma.

  The entire `mswiki-latest-pages-articles.xml.bz2` (412MB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 442,526 real pages, 69,018,270 real tokens, 1,012,416 distinct
  words, 2,148,408 raw (>=3) bigram rows.

  **Net result (post-fix)**: `dict.tsv` 82,409 rows (79,621 initial + 2,788 from Wortfamilien completion -
  honestly small per finding 3 above; calibration ratios noun=0.0181 (n=773), verb=0.1106 (n=3, too small a
  sample to be meaningful alone), adjective=0.0182 (n=1, same caveat)). POS tagging: 73,971 words kept
  unrecognised-by-kaikki, 917,445 dropped, 15,350 removed as common-English-word contamination. Wiktionary
  matching: 5,713 lemmas tagged, 332 unmatched; 780 existing forms linked, 2,788 generated. Proper-noun
  handling: 233 tagged, 1 unmatched, 11 skipped as collisions. Mandatory bare-noun safety check: 0 bare-NOUN
  rows. `bigram.tsv`: 618,179 rows (>=10 cutoff) from 2,148,408 raw. Quality gate: 0 case-insensitive
  duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.

  Being native-sourced, Malay carries no fallback-language documentation requirement, but for context: only
  5,713 lemmas + 233 proper nouns of 79,621 base entries (~7.5%) carry a real POS/lemma link - reflecting
  this native edition's own real, honestly-limited Wortfamilien scope, not fallback-source thinness.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Malay's own: NO diacritic letters at all (the plain
  26-letter Latin alphabet, confirmed directly) - `diacritics.tsv` is deliberately empty, every `hints.tsv`
  slot carries generic typography with `t=RM` (ringgit). `abbreviations.tsv`: a hand-curated 8-entry list.

  `MalayRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=**false** (see finding 5 above),
  `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 3,194
  candidate pairs, left deliberately uncurated.

  New tests: `LanguageRulesTest` gained a `Malay resolves to MalayRules` case plus its own mirroring test
  block (including the decimal-comma test asserting `false`, not `true`).

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Malay. Real, full-dump corpus scale (69.02M real tokens) and a genuinely native-sourced edition - but a
  real, honestly documented Wortfamilien scope limit for verbs/adjectives specifically. Not device-confirmed
  either. Indonesian continues next, the same language family with a richer native edition.

- **§434 (v1.1.73): D-450 (continued) - Estonian, Latvian, and Lithuanian language packs, closing the**
  **Baltic trio (fourteenth/fifteenth/sixteenth languages of the 18-language round) - a real calibration**
  **bug found and fixed in Latvian (16.16x verb ratio from bare pronoun-subject table headers).** Added
  `Language.ESTONIAN`/`LATVIAN`/`LITHUANIAN` (`"et"`/`"Eesti"`, `"lv"`/`"Latviešu"`, `"lt"`/`"Lietuvių"`) to
  the enum. None has a native Wiktionary edition - all three built from the English Wiktionary's own
  coverage (5.2MB/16.5MB/8.7MB respectively).

  **Estonian**: checked directly (not assumed) that unlike Finnish, Estonian's noun paradigm has no
  possessive-suffix forms (uses separate possessive pronouns instead) - no paradigm-size cap needed. Its own
  `postp` tag is clean, mapped to `PREPOSITION` like Finnish's. `etwiki` (307MB compressed): 261,738 pages,
  55,645,450 tokens. `dict.tsv` 301,216 rows (165,557 initial + 135,659 generated; ratios noun=0.1519
  (n=18,376), verb=1.6062 (n=5,120), adjective=0.1361 (n=3,607), all sane). Only 5,362 lemmas + 1,394 proper
  nouns of 165,557 base entries (~4.1%) carry a real POS/lemma link - one of the lowest ratios this round.
  `bigram.tsv` 488,694 rows. `confusables_scan.py`: 4,841 pairs.

  **Latvian - a real calibration bug, caught by the mandatory ratio sanity check**: the first verb pass found
  an impossible 16.16x ratio. Investigated directly: this source's own verb table includes the row-header
  PRONOUN SUBJECT as its own genuinely tagged forms[] entry on every verb (e.g. `viņš`/"he", freq 74,031,
  linked as a spurious "form" of dozens of unrelated rare verbs - a real Latvian verb inflection can never
  literally equal a personal pronoun). Fixed with a literal-value exclusion (es/tu/viņš/viņa/mēs/jūs/viņi/
  viņas); re-run confirmed 0 remaining contamination, ratio corrected to a real 0.7647x. `lvwiki` (206MB
  compressed): 145,425 pages, 30,794,845 tokens. `dict.tsv` 150,796 rows (88,979 initial + 61,817 generated;
  ratios noun=0.6818 (n=11,587), verb=0.7647 (n=3,213, post-fix), adjective=0.9514 (n=4,968)). Only 7,534
  lemmas + 1,156 proper nouns of 88,979 base entries (~8.5%) carry a real link. `bigram.tsv` 309,403 rows.
  `confusables_scan.py`: 2,712 pairs.

  **Lithuanian**: two findings already known from other sources this round, independently reconfirmed rather
  than assumed to transfer - (1) the same pitch-accent notation as the shared Serbo-Croatian source
  (`links`-based recovery applies unmodified), (2) the same `"error-unrecognized-form"` tag marking real,
  valid words here too (deliberately not excluded). `ltwiki` (243MB compressed): 224,117 pages, 37,246,517
  tokens. `dict.tsv` 180,032 rows (113,936 initial + 66,096 generated; ratios noun=0.3764 (n=13,261),
  verb=0.5671 (n=2,140), adjective=0.2945 (n=5,908), all sane). Only 5,274 lemmas + 1,614 proper nouns of
  113,936 base entries (~6.0%) carry a real link. `bigram.tsv` 335,409 rows. `confusables_scan.py`: 2,322
  pairs.

  All three: mandatory bare-noun safety check 0; quality gate clean (0 duplicates, 0 non-positive
  frequencies, 0 orphaned lemma links, 0 bare-NOUN rows). `hints.tsv`/`diacritics.tsv` reflect each
  language's own real diacritic set (Estonian `a=ä,o=õ/ö,u=ü`; Latvian 11 single-variant diacritic letters;
  Lithuanian 7 letters with `e`/`u` each hosting two variants) with `t=€` for all three (Baltic states all
  use the Euro) and a Baltic-region `g=„`/`h="` low-quote convention (Latvian's own `g` is taken by `ģ`, so
  uses `f=„`/`h="` instead). Each got its own `*Rules` object (`decimalCommaGluesDigits`=true,
  `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty) and `LanguageRulesTest` mirroring blocks.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied for any of the three**: none reviewed by a
  native speaker; all thinner-than-native-edition Wortfamilien/POS coverage, honestly quantified above. Not
  device-confirmed. **This closes the Estonian/Latvian/Lithuanian Baltic trio.** Indonesian, Malay, Swahili,
  and Tagalog remain in this 18-language round.

- **§433 (v1.1.72): D-450 (continued) - first Bosnian language pack, tenth of the 18-language round,**
  **closing the Croatian/Bosnian pair - reuses the identical shared Serbo-Croatian Wiktionary extraction.**
  Added `Language.BOSNIAN` (`"bs"`, `"Bosanski"`) to the enum. Bosnian uses the identical Latin-script
  `wiktionary_*.tsv` files as Croatian (copied, not regenerated) - see §432's own entry for the shared
  structural context (pitch-accent recovery, the `"error-unrecognized-form"` exception, sparse verb tables,
  and the deferred Serbian decision).

  The entire `bswiki-latest-pages-articles.xml.bz2` (178MB compressed, smallest corpus of the trio) was
  processed via the same extractor - 98,698 real pages, 31,462,803 real tokens, 996,635 distinct words,
  1,205,156 raw (>=3) bigram rows.

  **Calibration spot-check**: verb ratio (3.1847, n=1,058) sat at the edge of the sanity-check warning zone
  - investigated directly rather than assumed sane: the highest-ratio pairs are all genuine words where a
  common deverbative noun/grammaticalised participle vastly outstrips its own rare source verb (e.g.
  "zahvaljivati" freq 2 -> "zahvaljujući"/"thanks to" freq 1,646) - real language, not a bug, consistent with
  this shared source's own broad "verb family" concept.

  **Net result**: `dict.tsv` 185,506 rows (88,637 initial + 96,869 from Wortfamilien completion; calibration
  ratios noun=0.4609 (n=13,069), verb=3.1847 (n=1,058, verified sane), adjective=0.8000 (n=10,334)). POS
  tagging: 70,913 words kept unrecognised-by-kaikki, 900,556 dropped, 7,442 removed as common-English-word
  contamination. Wiktionary matching: 15,953 lemmas tagged, 5,836 unmatched; 25,048 existing forms linked,
  96,869 generated. Proper-noun handling: 1,931 tagged, 261 unmatched, 169 skipped as collisions. Mandatory
  bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 279,223 rows (>=10 cutoff) from 1,205,156 raw.
  Quality gate: 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0
  bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect**: only 15,953 lemmas + 1,931 proper nouns
  (~18.0% of the 88,637 pre-Wortfamilien base entries, richer than Croatian's own despite sharing the
  identical Wiktionary source - a consequence of Bosnian's own smaller, more core-vocabulary-weighted
  Wikipedia corpus) carry a real kaikki-derived POS tag and `lemma`/form link. Same two mechanisms weakened
  as Croatian's own entry documents, plus the same sparse-verb-table source limitation.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Bosnian's own: identical diacritics to Croatian
  (`c=č/ć, d=đ, s=š, z=ž`), but `t=KM` (Bosnia's own convertible-mark abbreviation, genuinely different from
  Croatia's `t=€` since Bosnia has not adopted the Euro). Same 12-entry abbreviation list as Croatian.

  `BosnianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 3,741 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Bosnian resolves to BosnianRules` case plus its own mirroring
  test block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually
  speaks Bosnian. Real, full-dump corpus scale (31.46M real tokens) but thinner Wortfamilien/POS coverage
  than a native-edition language, plus the same sparse-verb-table limitation. Not device-confirmed either.
  **This closes the Croatian/Bosnian pair** - Serbian remains explicitly deferred. Estonian, Latvian,
  Lithuanian, Indonesian, Malay, Swahili, and Tagalog remain.

- **§432 (v1.1.71): D-450 (continued) - first Croatian language pack, ninth of the 18-language round,**
  **starting the Croatian/Bosnian/Serbian trio - shared Wiktionary source, pitch-accent recovery, and a**
  **real structural fork on Serbian's own script that led to deferring it this round.** Added
  `Language.CROATIAN` (`"hr"`, `"Hrvatski"`) to the enum.

  **Structural pre-round finding**: kaikki.org treats Croatian/Bosnian/Serbian as ONE shared "Serbo-Croatian"
  Wiktionary edition (`lang_code == "sh"`, confirmed against all 70,075 entries) - no native or per-country
  fallback exists. Per the user's own decision, this ONE shared source
  (`kaikki.org/dictionary/Serbo-Croatian/kaikki.org-dictionary-SerboCroatian.jsonl`, 276.5MB uncompressed,
  marked DEPRECATED by kaikki.org - downloaded and archived before removal) feeds all three packs, each
  still built from its own Wikipedia corpus.

  **Two real data-shape findings in the shared source**: (1) inflection-table forms use traditional
  pitch-accent dictionary notation (e.g. "kȕća" for "kuća") outside the ordinary alphabet - would have been
  silently rejected and lost entirely, except each such form carries a `links` field whose second element is
  the real plain spelling; `usable_forms()` now prefers that recovered spelling. (2) the `"error-unrecognized-
  form"` tag (noise everywhere else) here marks entries that ARE real words the parser just couldn't
  recognise the template for - deliberately not excluded for this shared source. A real, honestly documented
  richness limitation: this source's own verb tables are unusually sparse (only infinitive + 2 participles +
  1 deverbative noun per verb, no personal conjugation at all).

  The entire `hrwiki-latest-pages-articles.xml.bz2` (348MB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 221,828 real pages, 65,675,004 real tokens, 1,375,544 distinct
  words, 2,310,181 raw (>=3) bigram rows.

  **Net result**: `dict.tsv` 248,453 rows (149,011 initial + 99,442 from Wortfamilien completion;
  calibration ratios noun=0.3732 (n=18,737), verb=1.6286 (n=1,569, a small sample given the sparse source,
  still sane), adjective=0.6667 (n=15,919)). POS tagging: 129,278 words kept unrecognised-by-kaikki, 1,218,746
  dropped, 7,787 removed as common-English-word contamination. Wiktionary matching: 17,856 lemmas tagged,
  3,933 unmatched; 36,707 existing forms linked, 99,442 generated. Proper-noun handling: 2,013 tagged, 179
  unmatched, 169 skipped as collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`:
  636,290 rows (>=10 cutoff) from 2,310,181 raw. Quality gate: 0 case-insensitive duplicates, 0 non-positive
  frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect**: only 17,856 lemmas + 2,013 proper nouns
  (~12.0% of the 149,011 pre-Wortfamilien base entries) carry a real kaikki-derived POS tag and `lemma`/form
  link. Same two mechanisms weakened: (1) A-05's split-safety gate. (2) D-404 Tier 2's family-match ratio
  override. A separate gap, more acute here given the source's own sparse verb tables: Croatian's own rich
  verb-aspect system is only thinly represented in the family-match data even for tagged verb lemmas.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Croatian's own: `c=č/ć, d=đ, s=š, z=ž`. `t=€` (Croatia
  adopted the Euro in 2023). `g=„`/`h="` (Croatian's own low-quote convention). `abbreviations.tsv`: a
  hand-curated 12-entry list.

  `CroatianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 4,171 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Croatian resolves to CroatianRules` case plus its own mirroring
  test block.

  **A genuine structural fork, surfaced rather than guessed at**: Serbian - the third of this originally-
  planned trio - was found to need a real, from-scratch Cyrillic keyboard layout (this app's only prior
  non-Latin script, Greek, required a genuine ~140-line feature addition, not a data-pipeline change) -
  confirmed directly that Serbian's own real-world usage is overwhelmingly Cyrillic (839,282 vs. 151,666
  Latin characters sampled, ~5.5:1). Presented via `AskUserQuestion` with four options; **the user chose to
  skip Serbian this round entirely**, deferring it to its own dedicated discussion. The already-downloaded
  Serbian Wikipedia dump's own frequency work was not done, but the shared Wiktionary source's own
  Cyrillic-tagged extraction remains archived locally for later.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Croatian. Real, full-dump corpus scale (65.68M real tokens) but thinner Wortfamilien/POS coverage than a
  native-edition language, plus a source-wide sparse-verb-table limitation. Not device-confirmed either.
  Bosnian continues next, reusing the identical shared Wiktionary extraction with its own Wikipedia corpus.

- **§431 (v1.1.70): D-450 (continued) - first Romanian language pack, eighth of the 18-language round,**
  **closing the cs/sk/hu/ro group - the richest fallback-source coverage ratio of any language this round.**
  Added `Language.ROMANIAN` (`"ro"`, `"Română"`) to the enum. No native edition exists - built from the
  English Wiktionary's own coverage instead (28.5MB).

  A real script-standard check, per direct inspection rather than assumption: modern comma-below Ș/Ț
  (U+0218/U+0219, U+021A/U+021B) confirmed as this source's own standard (25,970 real words use it; only one
  stray legacy cedilla entry, negligible).

  The entire `rowiki-latest-pages-articles.xml.bz2` (776MB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 547,539 real pages, 145,503,966 real tokens, 1,804,278 distinct
  words, 3,461,652 raw (>=3) bigram rows. Multi-word-form shape verified directly: Romanian's own infinitive
  ("a abate") and subjunctive/negative-imperative moods ("să abat"/"nu abate") use leading marker words, the
  same marker-first pattern as every Nordic language and Turkish - reject-whitespace-outright rule applies
  unmodified.

  **Net result**: `dict.tsv` 437,905 rows (210,494 initial + 227,411 from Wortfamilien completion;
  calibration ratios noun=0.4667 (n=33,172), verb=1.0000 (n=10,803), adjective=0.7143 (n=12,641), all sane).
  POS tagging: 150,909 words kept unrecognised-by-kaikki (tagged `OTHER` only), 1,579,761 dropped, 14,023
  removed as common-English-word contamination. Wiktionary matching: 46,888 lemmas tagged, 18,295 unmatched;
  59,407 existing forms linked, 227,411 generated. Proper-noun handling: 14,200 tagged (richest of any
  fallback-sourced language this round), 3,221 unmatched, 1,736 skipped as collisions. Mandatory bare-noun
  safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,128,439 rows (>=10 cutoff) from 3,461,652 raw. Quality
  gate: 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows
  - PASS.

  **What exactly is thinner, and its concrete app-level effect**: only 46,888 lemmas + 14,200 proper nouns
  (~29.1% of the 210,494 pre-Wortfamilien base entries - the RICHEST ratio of any fallback-sourced language
  this entire round, ahead even of Finnish's own unusually rich source) carry a real kaikki-derived POS tag
  and `lemma`/form link; the remaining 150,909 rows are real words by corpus frequency alone. Same two
  mechanisms weakened, though less acutely than most other fallback languages this round given the strong
  ratio: (1) A-05's split-safety gate. (2) D-404 Tier 2's family-match ratio override.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Romanian's own: only 4 base letters carry a real
  diacritic, `a` hosting two variants (`a=ă/â, i=î, s=ș, t=ț`, modern comma-below standard). 22 free letters
  left room for generic typography; `g=„`/`h="` (same low-quote convention as Czech/Slovak/Hungarian).
  `abbreviations.tsv`: a hand-curated 14-entry list.

  `RomanianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,834 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Romanian resolves to RomanianRules` case plus its own mirroring
  test block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Romanian. Real, full-dump corpus scale (145.50M real tokens) and the richest fallback-source coverage ratio
  of any language this round - but still thinner than any native-edition language's own full coverage. Not
  device-confirmed either. **This closes the cs/sk/hu/ro group** - Croatian, Bosnian, Serbian, Estonian,
  Latvian, Lithuanian, Indonesian, Malay, Swahili, and Tagalog remain.

- **§430 (v1.1.69): D-450 (continued) - first Hungarian language pack, seventh of the 18-language round -**
  **the same possessive-suffix paradigm cap already decided for Finnish applied consistently (no re-pause**
  **needed, real scale confirmed sane), plus a real tag-vocabulary collision found and fixed before it could**
  **silently drop the entire informal-address verb paradigm.** Added `Language.HUNGARIAN` (`"hu"`, `"Magyar"`)
  to the enum. No native edition exists - built from the English Wiktionary's own coverage instead (53.4MB).

  **Finding 1**: Hungarian, like Finnish, is agglutinative and its own noun table also carries genuine
  possessive-suffix forms on top of its ~18-case paradigm (confirmed: "fa"/"tree" has 12 real possessive-
  suffix forms like "fam"/"fád"/"fája" on top of ~36 case x number forms). Per the user's own Finnish
  decision, the same cap was applied proactively (`EXCLUDE_FORM_TAGS` excludes `"possessive"`/
  `"possessed-single"`/`"possessed-many"`) rather than re-asking - and since Hungarian's own possessive
  paradigm here was NOT crossed with the full case system (~12 forms, not ~130), the resulting `dict.tsv`
  stayed at a normal scale (985,299 rows, 31MB, comparable to Polish's own), confirmed real before moving on
  rather than assumed safe - no second `AskUserQuestion` needed.

  **Finding 2**: Hungarian verb conjugation uses `"formal"`/`"informal"` tags for a REAL grammatical
  distinction (T-V polite/informal address, e.g. formal "van" vs informal "vagy" for "you are") - not a
  slang-register marker the way every other language's own `"informal"` tag has meant. Blindly reusing the
  shared `EXCLUDE_QUALIFIERS` (which excludes `"informal"` elsewhere) would have silently dropped the entire
  informal-address conjugation paradigm for every Hungarian verb - caught before it happened; Hungarian's own
  copy omits `"informal"`.

  The entire `huwiki-latest-pages-articles.xml.bz2` (1.25GB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 573,285 real pages, 193,365,986 real tokens, 2,918,360 distinct
  words, 4,765,421 raw (>=3) bigram rows. Hungarian's own `postp` tag is clean and unambiguous, like
  Finnish's - mapped directly to `PREPOSITION`, 112 tagged. Multi-word check: a genuine English-prose noise
  pattern ("intransitive verb", "definite forms are not used") caught for free by the whitespace-rejection
  rule.

  **Net result**: `dict.tsv` 985,299 rows (362,799 initial + 622,500 from capped Wortfamilien completion;
  calibration ratios noun=0.0612 (n=56,615), verb=0.0290 (n=258, a thin sample, honestly noted),
  adjective=0.0263 (n=7,348) - all low but sane, consistent with rich case-suffixed forms being naturally
  rarer than their bare lemma, the same pattern Turkish's own entry documented). POS tagging: 335,543 words
  kept unrecognised-by-kaikki (tagged `OTHER` only), 2,540,708 dropped, 14,853 removed as common-English-word
  contamination. Wiktionary matching: 26,115 lemmas tagged, 4,551 unmatched; 96,547 existing forms linked,
  622,500 generated. Proper-noun handling: 2,435 tagged, 20 unmatched, 221 skipped as collisions. Mandatory
  bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,590,443 rows (>=10 cutoff) from 4,765,421 raw.
  Quality gate: 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0
  bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect**: only 26,115 lemmas + 2,435 proper nouns
  (~7.4% of the 362,799 pre-Wortfamilien base entries) carry a real kaikki-derived POS tag and `lemma`/form
  link. Same two mechanisms weakened: (1) A-05's split-safety gate cannot veto a wrong compound split built
  from any untagged word. (2) D-404 Tier 2's family-match ratio override cannot fire for a correct-but-rarer
  untagged word. A third limitation shared with Finnish: even tagged lemmas' possessive-suffixed forms (e.g.
  "fám" - "my tree") are not family-matched, since those forms were deliberately not generated.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Hungarian's own: only 5 base letters carry a real
  diacritic, but `o`/`u` each host THREE real distinct variants (`a=á, e=é, i=í, o=ó/ö/ő, u=ú/ü/ű` - `ő`/`ű`
  genuinely unique to Hungarian). 21 free letters left room for a real currency assignment (`t=Ft`, forint)
  plus `g=„`/`h="` (same low-quote convention as Czech/Slovak). `abbreviations.tsv`: a hand-curated 16-entry
  list.

  `HungarianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found only 484 candidate pairs, the lowest
  count of any language scan so far, left deliberately uncurated.

  New tests: `LanguageRulesTest` gained a `Hungarian resolves to HungarianRules` case plus its own mirroring
  test block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Hungarian. Real, full-dump corpus scale (193.37M real tokens) but thinner Wortfamilien/POS coverage than a
  native-edition language, plus the same possessive-suffix scope limit Finnish's own entry documents. Not
  device-confirmed either. Romanian continues next, the last of the cs/sk/hu/ro group.

- **§429 (v1.1.68): D-450 (continued) - first Slovak language pack, sixth of the 18-language round - a**
  **genuinely new positional/untagged noun-table data shape, found and resolved by design.** Added
  `Language.SLOVAK` (`"sk"`, `"Slovenčina"`) to the enum. No native Slovak edition exists - built from the
  English Wiktionary's own coverage instead (5.3MB, the SMALLEST fallback source checked so far this
  project).

  **Structural finding**: Slovak noun entries encode their declension table as a flat, positional stream of
  `forms[]` entries with NO `tags` key at all - English case/number LABEL WORDS ("singular", "nominative",
  ...) appear as their own untagged entries interleaved among the real word forms, and the raw form COUNT
  following a label cannot be trusted to reliably encode which number a form belongs to (confirmed: one real
  noun's "accusative" label was followed by zero forms, "instrumental" by only one instead of two). Since
  this project's dict.tsv format only ever needs the SET of real forms per lemma (case/number tags are
  discarded downstream anyway), `usable_forms()` was rewritten to not require a `tags` key, instead excluding
  a small, closed English label vocabulary - verified no real Slovak word collides with any of them. Verbs
  and adjectives checked separately and do NOT share this shape (fully tagged, ordinary format). A second,
  smaller finding: a real abbreviation ("aug") needed an explicit `"abbreviation"` qualifier exclusion not
  already covered by the shared set.

  The entire `skwiki-latest-pages-articles.xml.bz2` (355MB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 261,162 real pages, 57,454,537 real tokens, 1,486,293 distinct
  words, 2,050,500 raw (>=3) bigram rows.

  **Net result**: `dict.tsv` 184,403 rows (144,615 initial + 39,788 from Wortfamilien completion; calibration
  ratios noun=0.3406 (n=11,838), verb=0.8061 (n=2,117), adjective=0.4727 (n=5,136), all sane despite the
  small source). POS tagging: 135,406 words kept unrecognised-by-kaikki (tagged `OTHER` only), 1,327,896
  dropped, 13,782 removed as common-English-word contamination. Wiktionary matching: 6,314 lemmas tagged,
  490 unmatched; 19,230 existing forms linked, 39,788 generated. Proper-noun handling: 3,044 tagged, 471
  unmatched, 249 skipped as collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`:
  527,103 rows (>=10 cutoff) from 2,050,500 raw. Quality gate: 0 case-insensitive duplicates, 0 non-positive
  frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect**: only 6,314 lemmas + 3,044 proper nouns
  (~6.5% of the 144,615 pre-Wortfamilien base entries - the LOWEST ratio of any fallback-sourced language
  this round, directly reflecting the smallest source size) carry a real kaikki-derived POS tag and
  `lemma`/form link. Same two mechanisms weakened, more acutely than any prior language: (1) A-05's
  split-safety gate cannot veto a wrong compound split built from any untagged word. (2) D-404 Tier 2's
  family-match ratio override cannot fire for a correct-but-rarer untagged word. Slovak should need the most
  manual curation follow-up of any language in this round so far.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Slovak's own: 14 base letters carry a real diacritic
  (`a=á/ä, c=č, d=ď, e=é, i=í, l=ĺ/ľ, n=ň, o=ó/ô, r=ŕ, s=š, t=ť, u=ú, y=ý, z=ž`), second-most of any language
  built so far (after Czech's 13). `g=„`/`h="` (same low-quote convention as Czech). `abbreviations.tsv`: a
  hand-curated 17-entry list.

  `SlovakRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,426 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Slovak resolves to SlovakRules` case plus its own mirroring test
  block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Slovak. Real, full-dump corpus scale (57.45M real tokens) but the thinnest fallback-source coverage of any
  language this round, plus a genuinely novel positional-data-shape finding resolved by design. Not
  device-confirmed either. Hungarian and Romanian continue next.

- **§428 (v1.1.67): D-450 (continued) - first Czech language pack, fifth of the 18-language round, first of**
  **the cs/sk/hu/ro group.** Added `Language.CZECH` (`"cs"`, `"Čeština"`) to the enum. Czech DOES have a
  native Wiktionary edition (Wikislovník) - one of only 3 of the 18 languages this round with real native
  coverage (with Indonesian/Malay still to come) - `cs-extract.jsonl.gz` (38.4MB, correctly bigger than the
  wrong file's 19.7MB).

  A real, honestly documented source characteristic (not a bug): this native edition's own proper-noun
  coverage is unusually thin - only 7 raw "name" entries in the whole file (3 survived to real tagged rows).
  Czech Wikislovník evidently does not catalogue proper nouns the way the English Wiktionary's own broader
  fallback coverage does for other languages here.

  The entire `cswiki-latest-pages-articles.xml.bz2` (1.30GB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 597,792 real pages, 229,425,091 real tokens, 3,247,211 distinct
  words, 5,450,935 raw (>=3) bigram rows. Multi-word-form shape verified directly: no space-containing forms
  found in a real verb's own conjugation table - Czech's own periphrastic past tense is apparently not
  documented as a single multi-word forms[] entry in this edition.

  **Net result**: `dict.tsv` 521,940 rows (353,591 initial + 168,349 from Wortfamilien completion;
  calibration ratios noun=0.3019 (n=46,061), verb=0.5556 (n=13,467), adjective=0.4478 (n=27,947), all sane).
  POS tagging: 321,624 words kept unrecognised-by-kaikki (tagged `OTHER` only), 2,879,028 dropped, 14,592
  removed as common-English-word contamination. Wiktionary matching: 32,140 lemmas tagged, 4,667 unmatched;
  89,122 existing forms linked, 168,349 generated. Proper-noun handling: 3 tagged, 0 unmatched, 0 skipped.
  Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,974,855 rows (>=10 cutoff) from
  5,450,935 raw. Quality gate: 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma
  links, 0 bare-NOUN rows - PASS.

  Being native-sourced, Czech does not carry the fallback-language "what's thinner" documentation
  requirement - 32,140 lemmas + 3 proper nouns of 353,591 base entries (~9.1%) carry a real POS/lemma link,
  broadly comparable to the Nordic fallback languages' own ratios despite being native-sourced, since Czech
  Wikislovník's own coverage breadth (not richness per documented word) is the limiting factor here.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Czech's own: 13 base letters carry a real diacritic
  (`a=á, c=č, d=ď, e=é/ě, i=í, n=ň, o=ó, r=ř, s=š, t=ť, u=ú/ů, y=ý, z=ž`) - the most of any language this
  project has built so far, filling all 26 letter slots with no room for a currency symbol. `g=„`/`h="`
  (Czech's own low-quote convention). `abbreviations.tsv`: a hand-curated 23-entry list.

  `CzechRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,897 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Czech resolves to CzechRules` case plus its own mirroring test
  block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Czech. Real, full-dump corpus scale (229.43M real tokens) and a genuinely native-sourced edition - but
  still "pretty good", not native-reviewed, with a confirmed, honestly documented proper-noun coverage gap
  specific to this source. Not device-confirmed either. Slovak, Hungarian, and Romanian continue next.

- **§427 (v1.1.66): D-450 (continued) - first Finnish language pack, fourth and final of the Nordic batch -**
  **a genuine paradigm-size structural finding (13.46M raw generated forms, 570MB), surfaced via**
  **`AskUserQuestion` and resolved by capping to core case forms per the user's own decision.** Added
  `Language.FINNISH` (`"fi"`, `"Suomi"`) to the enum. No native Finnish edition exists - built from the
  English Wiktionary's own coverage instead (268.7MB, by far the largest fallback source checked so far,
  and unusually rich: 125,244/125,814 noun lemmas have real forms).

  The entire `fiwiki-latest-pages-articles.xml.bz2` (983MB compressed, the largest dump of this round) was
  processed via the same multiprocessing/hapax-pruning extractor - 624,121 real pages, 129,787,537 real
  tokens, 2,794,769 distinct words, 3,518,226 raw (>=3) bigram rows. Finnish's own `postp` tag is clean and
  unambiguous (unlike Turkish's own structural gap) - mapped directly to `PREPOSITION`, 256 tagged.

  **Structural finding - a real paradigm-size decision point, not guessed at**: an unfiltered first pass
  generated 13.46 million noun/adjective family forms (~158 real forms per noun lemma - the full case x
  number paradigm PLUS six possessive-suffix combinations, confirmed directly against real raw JSON for
  "talo"/"house": 28 case+number forms plus ~130 possessive-suffix forms like "taloni"/"talollani") - a
  dict.tsv that would have been 570MB raw, 20-40x bigger than any other pack in this project (5-32MB range).
  Surfaced to the user via `AskUserQuestion` as a genuine structural fork; the user chose to cap generation
  to the core case x number paradigm only. Possessive-suffix forms are unambiguously identified by the
  `"possessive"`/`"singular-possessive"`/`"plural-possessive"` tags - confirmed these never co-occur with
  ordinary verb personal-conjugation forms, so the exclusion cannot strip real verb data, only the
  genuinely-possessive-suffixed forms (including a handful of possessive-suffixed non-finite verb forms, the
  same bloat class, dropped for the same reason). Re-run after the cap: 2.83M noun form rows (down from
  18.46M).

  **Net result (after the cap)**: `dict.tsv` 2,537,125 rows (380,516 initial + 2,156,609 from capped
  Wortfamilien completion; calibration ratios noun=0.3846 (n=93,590), verb=0.4438 (n=19,875),
  adjective=0.2105 (n=23,512), all sane). POS tagging: 285,052 words kept unrecognised-by-kaikki (tagged
  `OTHER` only), 2,400,125 dropped, 14,128 removed as common-English-word contamination. Wiktionary
  matching: 89,509 lemmas tagged, 51,437 unmatched; 169,408 existing forms linked, 2,156,609 generated.
  Proper-noun handling: 7,260 tagged, 196 unmatched, 1,316 skipped as collisions. Mandatory bare-noun safety
  check: 0 bare-NOUN rows. `bigram.tsv`: 1,049,602 rows (>=10 cutoff) from 3,518,226 raw. Quality gate: 0
  case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.
  Even after the cap, this pack (94MB raw dict.tsv, 18MB zipped) remains noticeably larger than any other
  language pack - a genuine, honestly documented consequence of Finnish's own real morphological richness,
  not an extraction defect.

  **What exactly is thinner, and its concrete app-level effect**: only 89,509 lemmas + 7,260 proper nouns
  (~25.5% of the 380,516 pre-Wortfamilien base entries - the richest tagged-lemma ratio of any fallback-
  sourced language this round) carry a real kaikki-derived POS tag and `lemma`/form link; the remaining
  285,052 rows are real words by corpus frequency alone. Same two mechanisms weakened: (1) A-05's
  split-safety gate cannot veto a wrong compound split built from any untagged word - practically relevant
  since Finnish is a genuinely compound-forming language. (2) D-404 Tier 2's family-match ratio override
  cannot fire for a correct-but-rarer untagged word. A third, separate limitation from the deliberate
  paradigm cap: even for tagged lemmas, the family-match override will not recognise a real possessive-
  suffixed form (e.g. "taloni") as belonging to its lemma's family, since those forms were deliberately not
  generated - a real, bounded scope limit, not a bug.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Finnish's own: `a=ä, o=ö` (matching Swedish), `s=€`
  (Finland is the only Eurozone country of this Nordic batch, unlike Sweden/Norway/Denmark's own `t=kr`),
  `g=«`/`r=»` (matching Swedish/Norwegian). `abbreviations.tsv`: a hand-curated 15-entry list.

  `FinnishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 2,466 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Finnish resolves to FinnishRules` case plus its own mirroring
  test block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually
  speaks Finnish. Real, full-dump corpus scale (129.79M real tokens) and an unusually rich fallback source
  for its class - but still thinner POS/lemma coverage than a native-edition language, plus a deliberate,
  user-approved paradigm-size cap. Not device-confirmed either. **This closes the four-language Nordic batch
  of the larger 18-language round** - Czech, Slovak, Hungarian, Romanian, Croatian, Bosnian, Serbian,
  Estonian, Latvian, Lithuanian, Indonesian, Malay, Swahili, and Tagalog remain.

- **§426 (v1.1.65): D-450 (continued) - first Danish language pack, third of the 18-language round.**
  Added `Language.DANISH` (`"da"`, `"Dansk"`) to the enum. No native Danish Wiktionary edition exists -
  built from the English Wiktionary's own coverage instead (11.0MB).

  The entire `dawiki-latest-pages-articles.xml.bz2` (455MB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 315,686 real pages, 80,803,420 real tokens, 1,846,772 distinct
  words, 2,355,076 raw (>=3) bigram rows. Multi-word-form shape verified directly: the same marker-first
  "mere X"/"mest X" pattern as Swedish/Norwegian, same reject-whitespace-outright rule applies unmodified.
  `EXCLUDE_FORM_TAGS` gained a new `"error-unknown-tag"` entry, found in Danish's own adjective data.

  **Net result**: `dict.tsv` 208,383 rows (138,330 initial + 70,053 from Wortfamilien completion; calibration
  ratios noun=0.2604 (n=13,575), verb=0.9031 (n=7,282), adjective=0.9791 (n=2,761), all sane). POS tagging:
  118,631 words kept unrecognised-by-kaikki (tagged `OTHER` only), 1,694,172 dropped, 14,270 removed as
  common-English-word contamination. Wiktionary matching: 18,715 lemmas tagged, 1,080 unmatched; 24,402
  existing forms linked, 70,053 generated. Prepositions: 60 tagged, 1 unmatched. Proper-noun handling: 1,553
  tagged, 15 unmatched, 103 skipped as collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows.
  `bigram.tsv`: 702,019 rows (>=10 cutoff) from 2,355,076 raw. Quality gate: 0 case-insensitive duplicates,
  0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect**: only 18,715 lemmas + 1,553 proper nouns
  (~14.7% of the 138,330 pre-Wortfamilien base entries - the richest ratio of the three Nordic languages so
  far) carry a real kaikki-derived POS tag and `lemma`/form link; the remaining 118,631 rows are real words
  by corpus frequency alone. Same two mechanisms weakened: (1) A-05's split-safety gate cannot veto a wrong
  compound split built from any untagged word. (2) D-404 Tier 2's family-match ratio override cannot fire
  for a correct-but-rarer untagged word.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Danish's own: `a=å, o=ø` (same pattern as Swedish/
  Norwegian), `t=kr` (same krone abbreviation), `g=„`/`r="` (Danish's own German-style low-quote convention,
  unlike Swedish/Norwegian's guillemets - verified directly). `abbreviations.tsv`: a hand-curated 20-entry
  list.

  `DanishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 2,544 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Danish resolves to DanishRules` case plus its own mirroring test
  block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Danish. Real, full-dump corpus scale (80.80M real tokens) but thinner Wortfamilien/POS coverage than every
  native-edition language. Not device-confirmed either. Finnish continues next, the last of the Nordic batch.

- **§425 (v1.1.64): D-450 (continued) - first Norwegian Bokmål language pack, second of the 18-language**
  **round - Bokmål only, per explicit user decision (Nynorsk not built).** Added `Language.NORWEGIAN`
  (`"nb"`, `"Norsk bokmål"`) to the enum. No native Norwegian Bokmål Wiktionary edition exists on
  kaikki.org - built from the English Wiktionary's own coverage instead (9.68MB, the smallest fallback
  source of this round so far).

  The entire `nowiki-latest-pages-articles.xml.bz2` (831MB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 689,377 real pages, 150,939,253 real tokens, 2,902,707 distinct
  words, 3,546,745 raw (>=3) bigram rows. Multi-word-form shape verified directly (not assumed to carry over
  from Swedish): the same marker-first "mer X"/"mest X" pattern, the same reject-whitespace-outright rule
  applies unmodified.

  **Net result**: `dict.tsv` 250,821 rows (220,865 initial + 29,956 from Wortfamilien completion; calibration
  ratios noun=0.3571 (n=16,394), verb=1.0000 (n=7,283), adjective=0.7846 (n=3,271), all sane). POS tagging:
  201,380 words kept unrecognised-by-kaikki (tagged `OTHER` only), 2,666,944 dropped, 14,898 removed as
  common-English-word contamination. Wiktionary matching: 18,794 lemmas tagged, 775 unmatched; 27,253
  existing forms linked, 29,956 generated. Prepositions: 78 tagged, 1 unmatched. Proper-noun handling: 1,138
  tagged, 17 unmatched, 41 skipped as collisions. Mandatory bare-noun safety check: 0 bare-NOUN rows.
  `bigram.tsv`: 1,169,388 rows (>=10 cutoff) from 3,546,745 raw. Quality gate: 0 case-insensitive duplicates,
  0 non-positive frequencies, 0 orphaned lemma links, 0 bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect** (same mandatory documentation as Swedish's
  own §424 entry, Norwegian's own real numbers): only 18,794 lemmas + 1,138 proper nouns (~9.0% of the
  220,865 pre-Wortfamilien base entries) carry a real kaikki-derived POS tag and `lemma`/form link; the
  remaining 201,380 rows are real words by corpus frequency alone, with no part-of-speech data. Same two
  mechanisms weakened: (1) A-05's split-safety gate cannot veto a wrong compound split built from any
  untagged word (no `NOUN` tag to check). (2) D-404 Tier 2's family-match ratio override cannot fire for a
  correct-but-rarer untagged word, so a more frequent, merely-related inflected sibling could wrongly out-
  rank it.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Norwegian Bokmål's own: `a=å, o=ø` (same pattern as
  Swedish, `diacritics.tsv` additionally keeps `a -> å,æ`), `t=kr` (krone currency, shared with Swedish/
  Danish), `g=«`/`r=»` (same guillemet convention as Swedish). `abbreviations.tsv`: a hand-curated 20-entry
  list.

  `NorwegianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,896 candidate pairs, left deliberately
  uncurated.

  New tests: `LanguageRulesTest` gained a `Norwegian resolves to NorwegianRules` case plus its own mirroring
  test block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Norwegian. Real, full-dump corpus scale (150.94M real tokens) but thinner Wortfamilien/POS coverage than
  every native-edition language. Not device-confirmed either. Danish and Finnish continue next in this round.

- **§424 (v1.1.63): D-450 - first Swedish language pack, first of a large 18-language autonomous round -**
  **no native Swedish Wiktionary edition exists on kaikki.org, so this pack was built from the thinner**
  **English-Wiktionary-of-Swedish fallback coverage, honestly documented with concrete numbers and**
  **mechanism-level app impact, per explicit user instruction.** Added `Language.SWEDISH` (`"sv"`,
  `"Svenska"`) to the enum, the first of 18 new languages added in this same batch (Norwegian Bokmål,
  Danish, Finnish, Czech, Slovak, Hungarian, Romanian, Croatian, Bosnian, Serbian, Estonian, Latvian,
  Lithuanian, Indonesian, Malay, Swahili, Tagalog to follow in the same round).

  Confirmed directly against `kaikki.org/dictionary/rawdata.html` before starting: only Czech, Indonesian,
  and Malay of these 18 have a native Wiktionary edition; the other 15 (including Swedish) use the English
  Wiktionary's own coverage instead - the same accepted fallback already used where needed for Dutch/Polish's
  predecessors, now explicitly named as the standing approach in the Guide's own step 3.

  The entire `svwiki-latest-pages-articles.xml.bz2` was processed via the same multiprocessing/hapax-pruning
  extractor built for every prior round - 2,627,827 real pages, 268,255,177 real tokens, 3,950,407 distinct
  words, 5,501,462 raw (>=3) bigram rows.

  **A real multi-word-form shape check, per the Guide's own D-447 hardening**: Swedish's own analytic
  comparative/superlative is marker-FIRST ("mer X"/"mest X"), the same shape as Dutch's periphrastic verbs
  and Turkish's "daha X"/"en X" - so the same "reject any whitespace-containing form outright" rule applies,
  unlike Polish's opposite marker-LAST shape. `EXCLUDE_FORM_TAGS` gained a new `"error-unrecognized-form"`
  entry, a genuine new wiktextract diagnostic tag found in this data.

  **Net result**: `dict.tsv` 521,823 rows (374,316 initial Wikipedia-frequency + kaikki-POS merge, +147,507
  from Wortfamilien completion - 111,218 noun + 20,546 verb-delta + 15,743 adjective-delta generated forms;
  calibration ratios noun=0.2857 (n=22,225), verb=0.6667 (n=15,249), adjective=0.9000 (n=8,223), all sane).
  POS tagging: 328,566 words kept unrecognised-by-kaikki (tagged `OTHER` only), 3,561,716 dropped below the
  count->=20 floor, 14,375 removed as common-English-word contamination. Wiktionary matching: 44,084 lemmas
  tagged with real grammatical info, 3,446 unmatched; 46,461 existing forms linked, 147,507 generated.
  Proper-noun handling: 2,287 tagged, 22 unmatched, 190 skipped as collisions. Mandatory bare-noun safety
  check: 0 bare-NOUN rows. `bigram.tsv`: 1,689,546 rows (>=10 cutoff) from the 5,501,462-row raw floor.
  Quality gate: 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0
  bare-NOUN rows - PASS.

  **What exactly is thinner, and its concrete app-level effect (per explicit user instruction, not a generic
  disclaimer)**: only 44,084 lemmas + 2,287 proper nouns (~8.4% of the 374,316 pre-Wortfamilien base entries)
  carry a real kaikki-derived POS tag and `lemma`/form link. The remaining 328,566 rows are real Swedish
  words by corpus frequency, but this thin fallback source never documented their part of speech. Two
  concrete mechanisms are weakened, not just "less complete": (1) **A-05's split-safety gate** only vetoes a
  wrong compound split when it sees a genuine `NOUN` tag - a real Swedish noun stuck in the untagged/`OTHER`
  bucket has no such protection, so a legitimate compound built from it could be wrongly offered as two
  separate words. (2) **D-404 Tier 2's family-match ratio override** only fires between forms connected by a
  real `lemma` link - a correct-but-rarer word among the 328,566 untagged rows cannot benefit from it, so a
  more frequent, merely-related inflected sibling could wrongly out-rank it in a suggestion. Expect more
  manual curation need for Swedish (and the other 14 fallback-sourced languages this round) than for the
  native-edition packs.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Swedish's own: `a=ä, o=ö` (the two umlauted vowels),
  `t=kr` (krona currency abbreviation), `g=«`/`r=»` (Swedish's guillemet quoting convention).
  `abbreviations.tsv`: a hand-curated 22-entry list.

  `SwedishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,733 candidate pairs, left deliberately
  uncurated (no native Swedish fluency available).

  New tests: `LanguageRulesTest` gained a `Swedish resolves to SwedishRules` case plus its own mirroring test
  block.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: not reviewed by anyone who actually speaks
  Swedish. Real, full-dump corpus scale (268.26M real tokens) but thinner Wortfamilien/POS coverage than
  every native-edition language (see above, concrete numbers and mechanism-level impact). Not
  device-confirmed either.

- **§423 (v1.1.62): D-449 - first Turkish language pack, second and final of the pl/tr autonomous round -**
  **a postpositional-language finding (0 PREPOSITION rows, honestly documented) and a real ~3105x**
  **calibration bug caught and fixed, the third real example of that same symptom-class.** Added
  `Language.TURKISH` (`"tr"`, `"Türkçe"`) to the enum. Turkish already used ordinary QWERTY (the dominant
  real-world "Turkish Q" convention, not the historical "Turkish F" arrangement).

  Turkish's own native Wiktionary edition (43.3MB, correctly bigger than the wrong file's 34.1MB) documents
  noun/adjective inflection richly - 55,687/91,839 noun lemmas, 11,009/12,081
  adjective lemmas with real forms (the full 6-case system). Verbs show the same "most lemmas carry their
  own full conjugation table directly" shape Polish's own round found (6,621/77,658 raw "verb" entries are
  `senses[].form_of` references, 8.5%).

  **Finding 1 - Turkish is postpositional, not prepositional, and this Wiktionary edition has no dedicated
  closed-class tag for it**: 0 entries tagged `prep`/`prep_phrase` anywhere in the file. Real Turkish
  postpositions (`için`/`gibi`/`kadar`/...) are tagged inconsistently across `particle`/`adv`/`conj`/`noun`/
  `adj` depending on the word's own other senses - several well-known ones already carry real, competing
  senses. A curated exception list was considered and deliberately not built (too risky without native
  fluency, same reasoning as Portuguese's own "-ão" decision). `dict.tsv` has 0 `PREPOSITION` rows for
  Turkish - a genuine structural finding, now named explicitly in the Guide's own preposition step.

  **Finding 2 - a real calibration bug, the third real example of the same symptom-class French's (D-444-
  followup) and Dutch's (D-447) own bugs share**: the first adjective calibration pass found a ~3105x ratio,
  traced to one mistagged entry (`"obez"`, "obese") whose own `forms[]` documented the bare analytic
  comparative/superlative marker (`"daha"`/`"en"`, ordinarily written attached to the adjective - "daha X"/
  "en X") without the adjective attached - a source annotation inconsistency (2 of 21,994 comparative/
  superlative forms in the whole file), not a systemic multi-word-splitting decision this time. Fixed with a
  narrow, evidence-based exclusion; re-run confirmed 0 remaining contamination, adjective ratio corrected to
  a real 0.0303 (n=195). The Guide's own calibration-sanity-check section now names this as a third example:
  the lesson generalises beyond `last_token()` - always pull the real raw JSON for an outlier pair before
  accepting or dismissing it.

  **Finding 3 - a real casing bug in this project's OWN extraction code, unrelated to the app's runtime
  capitalisation behaviour**: ordinary Python `str.lower()` gets Turkish's dotted/dotless İ/I pair wrong
  (`"IŞIK".lower()` wrongly produces `"işik"` instead of the real word `"ışık"` - a silent, still-plausible-
  looking corruption). A `turkish_lower()` helper (fix İ/I first, defer to ordinary Unicode rules otherwise)
  is used throughout both extraction scripts, verified directly against the real corpus output (`"ışık"`
  correctly at frequency 15,401; the wrong spelling appears only twice, consistent with organic noise). This
  is pure data-correctness work, separate from and not a substitute for the still-open capitalisation-
  behaviour question below. The Guide's own calibration-sanity section now also names this as a related but
  separate class of check worth doing for any language with its own special casing rules.

  The entire `trwiki-latest-pages-articles.xml.bz2` (1.05GB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 698,557 real pages, 136,688,360 real tokens.

  **Net result**: `dict.tsv` 706,502 rows (257,407 initial + 449,095 from full Wortfamilien completion;
  calibration ratios noun=0.0667 (n=33,751), verb=0.2797 (n=8,090), adjective=0.0303 (n=195, post-fix)).
  POS tagging: 182,351 words kept unrecognised-by-kaikki, 1,909,033 dropped, 13,477 removed as
  common-English-word contamination. Proper-noun handling: 21,575 tagged, 3,956 skipped as collisions.
  Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 1,122,647 rows (>=10 cutoff) from
  3,536,280 at the raw >=3 floor. Quality gate clean.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Turkish's own: the real, closed 29-letter alphabet
  (excludes q/w/x) has six diacritic letters, each with exactly one variant - `c=ç, g=ğ, i=ı, o=ö, s=ş, u=ü`.
  German's 10 neutral assignments fit without conflict this time. Remaining: `t=₺` (lira symbol), `l=«`/`r="`
  (quotes), `a/e/j/k/w/y/z` filled with generic typography. `abbreviations.tsv`: a hand-curated ~24-entry
  list.

  `TurkishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 4,461 candidate pairs (the largest of
  any language scan so far), left deliberately uncurated. New tests: `LanguageRulesTest` gained a `Turkish
  resolves to TurkishRules` case plus its own mirroring block. `language_profiles.tsv` NOT built for Turkish
  either, same accepted, named gap as Polish's.

  **Deliberately NOT attempted this round, per explicit user instruction**: Turkish's own dotted/dotless İ/I
  *capitalisation* behaviour (as opposed to the extraction-time casing fix above, which is a different,
  already-closed matter). Confirmed first that this project's own `CapitalisationEngine`/related code uses
  Kotlin's locale-invariant `.lowercase()`/`.uppercase()` everywhere (no locale-*default*-dependent legacy
  calls found anywhere in the codebase) - so no other language's capitalisation is put at risk by Turkish's
  presence, but genuinely correct Turkish capitalisation ("istanbul" -> "İstanbul") is simply not implemented
  yet. A real, still-open design question for `CapitalisationEngine` itself - explicitly deferred for its own
  dedicated discussion, not decided or guessed at here.

  `versionCode` 478 -> 479, `versionName` `"1.1.61"` -> `"1.1.62"`. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed by anyone
  who actually speaks Turkish. Real, full-dump corpus scale (136.69M real tokens) and a real lexicon with
  full noun/verb/adjective Wortfamilien coverage - but still "pretty good" in the guide's own sense, and,
  unlike every other implemented language, genuinely incomplete for prepositions specifically (a confirmed
  structural finding, not an oversight). Not device-confirmed either. **This closes the two-language (pl/tr)
  autonomous round** - the Language Contribution Guide gained two further hardening passes this round
  (postpositional-language exception; calibration-bug generalisation + locale-casing check), the same
  "close the loop immediately" discipline every prior round has followed.

- **§422 (v1.1.61): D-448 - first Polish language pack, first of a second two-language (pl/tr) autonomous**
  **round, requested directly after checking that both languages had every prerequisite the pipeline needs.**
  Added `Language.POLISH` (`"pl"`, `"Polski"`) to the enum - was not present before this round; QWERTY
  ("Polish programmers" layout, diacritics via AltGr) needed no new layout code, confirmed via the standard
  §0/§5 check first.

  **Richest inflectional data of any language built so far**: Polish's own native Wiktionary edition
  (`kaikki.org/dictionary/downloads/pl/pl-extract.jsonl.gz`, 130.7MB, correctly bigger than the wrong
  English-Wiktionary-coverage file's 65.1MB) documents the full 7-case declension for nouns (nominative/
  genitive/dative/accusative/instrumental/locative/vocative, singular+plural - 50,380/67,607 real lemmas)
  and the same 7 cases x 4 genders for adjectives plus comparative/superlative (19,060/22,126). Verbs show a
  genuinely different shape from every language built before this round: only 6.5% of raw "verb" entries are
  `senses[].form_of` references (vs. 70-95% for French/Spanish/Italian/Dutch/Portuguese) - most Polish verb
  lemmas carry their own complete conjugation table directly.

  **A real multi-word-form shape check, per the Guide's own D-447 hardening**: Polish's own multi-word forms
  are a slash-separated real-word/grammatical-shorthand pair (e.g. `"jestem / -(e)m"`), the OPPOSITE shape
  from Dutch's own periphrastic problem - splitting on "/" FIRST (before any whitespace check, the reverse
  order from Dutch's own fix) correctly recovers the real word while the shorthand half is separately
  rejected by `VALID_FORM_RE`. A small, separate real quirk was also found and fixed: `"nie"` ("no") turns up
  as a literal "this form does not exist" placeholder in a handful of conjugation-table cells (9 rows,
  confirmed against one real raw entry before excluding it).

  The entire `plwiki-latest-pages-articles.xml.bz2` (2.73GB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor D-445 built - 1,706,354 real pages, 383,934,279 real tokens.

  **Net result**: `dict.tsv` 884,720 rows (491,933 initial + 392,787 from full Wortfamilien completion -
  147,229 noun, 153,306 verb, 92,252 adjective generated forms; calibration ratios noun=0.3684 (n=64,346
  pairs), verb=0.9474 (n=26,141), adjective=0.6667 (n=47,068) - all three sane, checked against the Guide's
  own new mandatory calibration-ratio sanity check before trusting them). POS tagging: 425,849 words kept
  unrecognised-by-kaikki (corpus count >=20), 3,487,618 dropped below that floor, 14,510 removed as
  common-English-word contamination. Proper-noun handling: 6,203 tagged, 756 skipped as real-word collisions.
  Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 2,855,627 rows (>=10 cutoff) from
  7,167,156 rows at the raw >=3 extraction floor. Quality gate clean.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Polish's own: `hints.tsv` mostly keeps German's 10
  language-neutral assignments (`n` reassigned to `ń` - Polish's own diacritic set is systematic enough to
  deserve its natural base letter, unlike Spanish's single `ñ`; `+` moved to `j`) and gives the remaining
  letters real Polish content - `a=ą, c=ć, e=ę, l=ł, n=ń, o=ó, s=ś, z=ż` (`diacritics.tsv` keeps the fuller
  set, `z`->`ź,ż`), `g=„`/`r="` (Polish low-quotes), `t=zł` (the złoty currency symbol, a genuine 2-character
  glyph), `k=—` (Polish's own em-dash dialogue convention), `w=§`, `i/u/y` filled with generically useful
  remaining typography. `abbreviations.tsv`: a hand-curated ~25-entry list.

  `PolishRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 1,198 candidate pairs, left deliberately
  uncurated. New tests: `LanguageRulesTest` gained a `Polish resolves to PolishRules` case plus its own
  mirroring test block. `language_profiles.tsv` (A-03 trigram data) NOT built for Polish this round - the
  builder script is a documented, pre-existing gap not in this repository; the classifier degrades gracefully
  without it (per the Guide's own §6), an accepted state, not an oversight.

  `versionCode` 477 -> 478, `versionName` `"1.1.60"` -> `"1.1.61"`. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed by anyone
  who actually speaks Polish. Real, full-dump corpus scale (383.93M real tokens) and a complete, real lexicon
  with full noun/verb/adjective Wortfamilien parity (the richest of any language built so far) - but still
  "pretty good" in the guide's own sense, not native-reviewed quality. Not device-confirmed either. Turkish
  continues in the same autonomous session - see its own forthcoming §423 entry.

- **§421 (v1.1.60): D-445-followup - conservative, rule-based Portuguese noun/adjective PLURAL generator,**
  **plus a preposition-tagging audit across every non-German pack and a new mandatory Guide step for it.**
  Two separate, explicit user requests handled in one round.

  **Preposition audit (no fix needed, verified clean)**: checked whether French/Spanish/Italian/Dutch/
  Portuguese all correctly tag `PREPOSITION` - they do (97/71/93/134/45 rows respectively), spot-checked
  against each language's own most common prepositions (`de`/`à`/`en`/`avec`/`pour`/`sur`/`sans`/`chez` for
  French; `de`/`a`/`en`/`con`/`sin`/`sobre`/`para`/`por` for Spanish, and more) - all present with sensible
  frequencies. The mechanism was already correct in every `merge_dict.py`'s own `POS_MAP`; what was missing
  was a documented, named Guide step making this an explicit, checked requirement rather than an incidental
  side-effect of copying an existing script - added to the Guide's own step 3.

  **Portuguese plural generator (`dictionaries/pt/generate_plurals.py`, new)**: built on explicit instruction
  to close part of D-445's own noun/adjective Wortfamilien gap, but scoped deliberately narrowly to only
  mechanical, low-risk suffix rules, per direct instruction not to manufacture data the pipeline cannot be
  confident about:
  - Applied: vowel-ending -> +s; -m -> -ns; -r -> +es; -z -> +es (skipped when preceded by "ui"/"ai" - the
    juiz/raiz-style hiatus-accent risk); -al/-el/-ol/-ul -> -ais/-éis/-óis/-uis (skipped when the word already
    carries an earlier accent, a proparoxytone signal like `cônsul`).
  - Deliberately NOT attempted, explicitly documented as a known scope limit rather than guessed: -s-ending
    words (`lápis`/`vírus`-style invariable vs. `mês`/`país`-style oxytone -es, not reliably distinguishable
    from spelling alone); -il-ending words (the identical stress ambiguity); -ão-ending words (three genuinely
    competing patterns - -ões/-ães/-ãos - with real dialectal variation even in reference grammars for some
    members, no confident curated list was possible); adjective gender-pair (-o/-a) generation (no reliable
    per-word signal for which adjectives even take this alternation in the available data).
  - Calibration ratio (0.4474) measured empirically from 17,223 real already-matched pairs (the predicted
    plural already existing as its own real `dict.tsv` entry), the same technique `merge_wiktionary.py`
    already uses for Wortfamilien ratios, not guessed.
  - Result: `dict.tsv` 535,869 -> 558,148 rows (+22,279 generated plurals; 17,225 further existing words
    linked to their singular via `lemma`; 6,736 eligible words correctly matched no safe rule and were left
    untouched, exactly as intended). Spot-checked a dozen predictions by hand before trusting the run
    (`hotel`->`hotéis`, `farol`->`faróis`, `azul`->`azuis`, `animal`->`animais`, `jardim`->`jardins`,
    `homem`->`homens`, `mulher`->`mulheres`, `motor`->`motores`, `cartaz`->`cartazes`, `rapaz`->`rapazes` - all
    correct; `cônsul`/`juiz`/`raiz`/`país`/`lápis`/`fóssil`/`barril`/`pão`/`irmão` all correctly skipped).
    Quality gate re-verified clean: 0 duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0
    bare-NOUN rows. `dictionaries/pt/version.txt` 1 -> 2, pack rebuilt, `LanguagePackCatalog` version 1 -> 2.

  No new tests (data-only). `versionCode` 476 -> 477, `versionName` `"1.1.59"` -> `"1.1.60"`.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Portuguese's own remaining gaps (the four skipped
  classes above, plus verb-only Wortfamilien from D-445 itself) are unchanged by this round and still worth
  a future, more careful pass - not claimed complete, only conservatively improved.

- **§420 (v1.1.59): D-447 - first Dutch language pack, third and final of the three-language (pt/it/nl)**
  **overnight one-shot round - and a real, serious bug caught before shipping, the identical class of**
  **"impossible calibration ratio" French's own D-444-followup already taught this project to watch for.**
  Dutch already used ordinary QWERTY and already had a real character-trigram profile
  (`language_profiles.tsv`, "nl").

  Like Italian, Dutch's own native Wiktionary edition (`kaikki.org/dictionary/downloads/nl/
  nl-extract.jsonl.gz`, 127.8MB - here the native file IS the much bigger one, ~4.4x the wrong file's
  29.2MB, matching French's/Spanish's own original "native always bigger" pattern rather than Portuguese's/
  Italian's own reversed one, confirming both patterns are real and language-dependent, not a single rule)
  documents noun/adjective inflection very richly (130,196/149,010 real noun lemmas, 15,706/19,440 adjective
  lemmas have real forms, including a genuine Dutch-specific "diminutive" noun form kept as a real generated
  form). Full three-category Wortfamilien completion applies.

  **The real bug**: the first pass computed a verb calibration ratio of ~173x (n=35,225 pairs) - physically
  impossible. Root cause, confirmed by direct inspection of real entries (not assumed): Dutch's own
  conjugation tables are full periphrastic-tense tables (e.g. `"ingebakerd zullen hebben"` - future perfect
  infinitive - or, for separable verbs, a two-word single-clause form like `"baker in"`, verb stem first,
  separable particle last) - the OPPOSITE shape from French's own `"il/elle/on mange"` pattern (pronoun-
  prefix(es) first, real verb last) the shared `last_token()` recovery helper was built for. Applied here, it
  silently extracted the wrong half every time - wrongly linking 18,025 rows across the whole file to a bare
  `"in"`/`"hebben"`/`"worden"`/`"zijn"`, each an extremely common, semantically unrelated standalone Dutch
  word (`"in"` alone: 491,058) whose own astronomical frequency poisoned the ratio. Fixed at the root
  (`dictionaries/nl/extract_wiktionary.py`): no `last_token()` recovery for Dutch at all - any raw form
  containing whitespace is rejected outright. Dutch's own genuinely useful single-word forms (subordinate-
  clause forms, both participles) were already present as their own single-word entries, so nothing real was
  lost - re-run after the fix: verb ratio corrected to a real 0.6667 (n=14,648 pairs), contaminated rows
  dropped from 18,025 to 5.

  The entire `nlwiki-latest-pages-articles.xml.bz2` (2.04GB compressed) was processed via the same
  multiprocessing/hapax-pruning extractor - 2,225,912 real pages, 391,313,011 real tokens.

  **Net result** (after the fix): `dict.tsv` 715,368 rows (463,009 initial + 252,359 from full Wortfamilien
  completion - 174,646 noun, 27,951 verb, 49,762 adjective generated forms; calibration ratios noun=0.5000
  (n=24,477), verb=0.6667 (n=14,648, post-fix), adjective=1.0000 (n=7,845)). POS tagging: 331,809 words kept
  unrecognised-by-kaikki (corpus count >=20), 2,058,610 dropped below that floor, 12,614 removed as
  common-English-word contamination. Proper-noun handling: 6,108 tagged, 917 skipped as real-word collisions.
  Mandatory bare-noun safety check: 0 bare-NOUN rows. `bigram.tsv`: 2,361,683 rows (>=10 cutoff) from
  6,046,978 rows at the raw >=3 extraction floor. Quality gate: 0 duplicates, 0 non-positive frequencies, 0
  orphaned lemma links, 0 bare-NOUN rows - PASS.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Dutch's own: `hints.tsv` keeps German's 10 language-
  neutral assignments and gives the remaining 16 letters Dutch content - e=ë, i=ï, o=ö, u=ü (trema/diaeresis
  marks - `diacritics.tsv` keeps the fuller set: e→ë,é,è; i→ï; o→ö; u→ü), g=„/r=" (Dutch low-quote
  convention), s=€, t=— (em dash), c=§, a=† (a genuinely Dutch obituary/genealogy convention - "Jan Jansen
  †1990"), j/k/l/w/y/z filled with generically useful remaining typography (…, &, %, ~, •, ±).
  `abbreviations.tsv`: a hand-curated ~27-entry Dutch sentence-boundary list (dhr./mevr./dr./prof./bv./
  enz./...).

  `DutchRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`="uur" - unlike
  French/Spanish/Portuguese/Italian, Dutch DOES have a real single-word S-08-style convention after a typed
  time ("om 14.30 uur"), naively filled rather than left null. `bundledConfusablesBlacklist`=empty -
  `confusables_scan.py` found 1,364 candidate pairs, left deliberately uncurated for the same reasoning every
  non-German round documents.

  New tests: `DutchRules`'s registry/test wiring was already committed alongside Portuguese's own round
  (§418). `versionCode` 475 -> 476, `versionName` `"1.1.58"` -> `"1.1.59"`. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed by anyone
  who actually speaks Dutch. Real, full-dump corpus scale (391.31M real tokens) and a complete, real lexicon
  with full noun/verb/adjective Wortfamilien parity (once the periphrastic-form bug above was found and
  fixed) - but still "pretty good" in the guide's own sense, not native-reviewed quality. Not
  device-confirmed either. **This closes the three-language (pt/it/nl) overnight one-shot round** - see
  §418/§419/§420 together for the complete picture, and the Language Contribution Guide itself is worth a
  future hardening pass to fold in this round's own two new lessons (the reversed native-vs-English-coverage
  file-size pattern; the `last_token()` heuristic's own French-specific shape assumption) the same way D-444
  hardened it after the wrong-Wiktionary-source mistake.

- **§419 (v1.1.58): D-446 - first Italian language pack, second of the three-language (pt/it/nl) one-shot**
  **overnight round (see §418/D-445 directly below for the shared context).** Italian already used ordinary
  QWERTY and already had a real character-trigram profile (`language_profiles.tsv`, "it").

  **Unlike Portuguese, Italian's own native Wiktionary edition documents noun/adjective inflection richly -**
  **checked directly before assuming Portuguese's own scope limitation would repeat here, not assumed.**
  `kaikki.org/dictionary/downloads/it/it-extract.jsonl.gz` (40.0MB - again smaller than the wrong English-
  Wiktionary-coverage file, 74.2MB, the same reversed-from-French/Spanish size pattern Portuguese's own round
  found, still the correct mandatory choice regardless of size per the guide's own unconditional rule):
  22,738 of 37,208 real noun lemmas have real plural/gender forms, 12,072 of 14,972 adjective lemmas have
  real plural/gender/superlative forms - both far richer than Portuguese's own 137/52,477 and 70/18,413.
  Verbs show the identical individually-paged-conjugated-form shape every native edition this project has
  processed shows (462,027 raw "verb" entries, 454,157 `senses[].form_of` references - ~7,870 real lemmas).
  Full three-category Wortfamilien completion therefore applies here, matching French's/Spanish's own parity
  rather than Portuguese's own verb-only scope.

  The entire `itwiki-latest-pages-articles.xml.bz2` (4.24GB compressed, the largest dump this project has
  processed) was processed via the same multiprocessing/hapax-pruning extractor D-445 built (§418) -
  1,984,914 real pages, 785,078,705 real tokens, the largest raw-token count of any language pack this
  project has built so far, completed well within the same overnight run (no memory-safety stop needed).

  **Net result**: `dict.tsv` 613,988 rows (455,757 from the initial Wikipedia-frequency + kaikki-POS merge,
  +158,231 from full Wortfamilien completion - 8,862 noun, 143,232 verb, 6,137 adjective generated forms;
  calibration ratios noun=0.4340 (n=16,685 pairs), verb=0.5000 (n=36,090), adjective=0.5000 (n=20,583) - all
  three well-populated, unlike Portuguese's own sparse noun/adjective pair counts). POS tagging: 403,958
  words kept unrecognised-by-kaikki (corpus count >=20), 3,522,925 dropped below that floor, 15,053 removed
  as common-English-word contamination. Proper-noun handling: 3,842 tagged, 595 skipped as real-word
  collisions (the D-444 broad `all_pos` collision check). Mandatory bare-noun safety check: 0 bare-NOUN rows.
  `bigram.tsv`: 4,289,075 rows (>=10 cutoff) from 9,161,636 rows at the raw >=3 extraction floor. Quality
  gate (`dictionaries/quality_gate.py`): 0 case-insensitive duplicates, 0 non-positive frequencies, 0
  orphaned lemma links, 0 bare-NOUN rows - PASS.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Italian's own: `hints.tsv` keeps German's 10 language-
  neutral assignments and gives the remaining 16 letters Italian content - a=à, e=è, i=ì, o=ò, u=ù (`diacritics.
  tsv` keeps the fuller set: a→à; e→è,é; i→ì; o→ò,ó; u→ù), g=«/r=» (quotes), s=€ (currency), t=º/y=ª
  (ordinal indicators - a genuine Italian convention too, "3ª edizione"), c=§ (section sign), j/k/l/w/z
  filled with generically useful remaining typography (—, …, &, ₤, •). `abbreviations.tsv`: a hand-curated
  ~27-entry Italian sentence-boundary list (sig./dott./prof./avv./ecc./...).

  `ItalianRules` (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 2,026 candidate pairs, left deliberately
  uncurated for the same "cannot confidently separate a genuine short word from real corpus noise without
  native fluency" reasoning every non-German round documents.

  New tests: `ItalianRules`'s registry/test wiring was already committed alongside Portuguese's own round
  (§418) since it is language-neutral, zero dictionary-file dependency - only this round's real dictionary
  numbers were pending. `versionCode` 474 -> 475, `versionName` `"1.1.57"` -> `"1.1.58"`.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed by anyone
  who actually speaks Italian. Real, full-dump corpus scale (785.08M real tokens, the largest of any
  language pack this project has built) and a complete, real lexicon with full noun/verb/adjective
  Wortfamilien parity - but still "pretty good" in the guide's own sense, not native-reviewed quality. Not
  device-confirmed either. Dutch continues in the same autonomous overnight session.

- **§418 (v1.1.57): D-445 - first Portuguese language pack, first of a three-language (pt/it/nl) one-shot**
  **autonomous round run overnight per explicit user instruction, full-dump-only this time (see below).**
  Portuguese (`Language.PORTUGUESE`) already used ordinary QWERTY and already had a real character-trigram
  profile (`language_profiles.tsv`, "pt", since D-280) - both prerequisites §414/§415-style prep already
  satisfied.

  **Mid-round correction from the user, applied before this round's real extraction began**: the first
  attempt used a single capped Wikipedia dump split (105,695 pages - the French/Spanish D-441/D-443
  precedent), explicitly rejected as "obviously too small" - the user required the COMPLETE Wikipedia dump
  processed for all three languages this round, not a page-capped first split. `dictionaries/pt/
  extract_wiki_dump.py` (and its `it`/`nl` siblings, prepared the same session) gained two real additions
  over the French/Spanish reference script to make full-dump-scale processing tractable overnight:
  multiprocessing (this machine's 6 physical/12 logical cores, a worker pool doing the CPU-heavy per-page
  wikitext-cleaning/tokenising step while the main process alone streams the iterparse XML) and periodic
  hapax pruning (dropping count==1 entries once a Counter exceeds a size threshold, standard streaming-
  frequency practice - a genuinely common word keeps accumulating from later occurrences regardless of an
  early one being pruned, and `merge_dict.py`'s own >=20 floor discards anything this rare anyway). Result:
  the ENTIRE `ptwiki-latest-pages-articles.xml.bz2` (2.72GB compressed) processed in roughly 1.5 hours -
  1,181,337 real (ns=0, non-redirect) pages, 488,957,696 real tokens - an order of magnitude more real corpus
  data than any capped-dump language this project has built before.

  **A second, independent, real structural finding, found during the mandatory pre-flight size check
  (Guide's own hard gate) and confirmed by direct inspection of real entries, not assumed**: Portuguese's own
  native Wiktionary edition (`kaikki.org/dictionary/downloads/pt/pt-extract.jsonl.gz`, 35.4MB compressed) -
  still the correct, mandatory source per the guide's own unconditional rule - is actually SMALLER than the
  wrong English-Wiktionary-coverage file (54.2MB), the reverse of French's/Spanish's own "native always
  bigger" pattern. Used per the guide's rule regardless. More consequentially: this native edition documents
  Portuguese VERB conjugation richly (79,453 raw "verb" entries, 72,710 `senses[].form_of` references to
  another lemma - the identical individually-paged-conjugated-form shape every other native edition already
  showed - leaving ~6,700 real verb lemmas with full tables) but essentially does NOT document regular noun/
  adjective inflection as `forms[]` data at all: only 137 of 52,477 noun lemmas and 70 of 18,413 adjective
  lemmas have any real form beyond a non-word hyphenation marker (`"grego"` -> `"gre.go"`, tagged
  `"canonical"`, now excluded in every language's own `extract_wiktionary.py` via a new shared
  `EXCLUDE_FORM_TAGS` check). Raised with the user before proceeding (a real design fork, not a routine
  judgement call) - agreed outcome: Portuguese Wortfamilien completion is real and complete for VERBS only
  this round (calibration pair counts confirm the gap directly: noun ratio n=29 pairs, adjective ratio n=16
  pairs - both far too sparse to mean anything - versus verb ratio n=28,416 pairs); nouns/adjectives keep
  whatever the base dict.tsv/POS-tagging pass already produced, with only 129 generated noun forms and 36
  generated adjective forms (incidental alternate-spelling/superlative entries, not systematic paradigm
  completion) - a rule-based Portuguese noun-plural/adjective-agreement generator (the German-precedent
  alternative) was discussed and deliberately deferred as its own, separate future round, not built this
  time. This is a genuine, confirmed source limitation - not a bug in the extraction/merge scripts -
  documented honestly here rather than silently passed off as parity with French/Spanish.

  **Net result**: `dict.tsv` 535,869 rows (325,579 from the initial Wikipedia-frequency + kaikki-POS merge,
  +210,290 from Wortfamilien completion, 210,125 of those generated verb forms alone). POS tagging: a word
  found in kaikki gets its real part of speech; one not found is kept, tagged OTHER, once its own real corpus
  count clears 20 occurrences (274,565 kept this way, 2,684,749 below-floor rows dropped), unless it is also
  a common word (frequency >= 100) in this project's own bundled `en/dict.tsv` (14,968 rows removed this way
  - the same targeted noise signal every prior pipeline round uses). `bigram.tsv`: 2,814,934 rows (>=10 real
  occurrences, matching French's/Spanish's own final cutoff) from 6,561,082 rows at the raw >=3 extraction-
  time floor. Quality gate (Guide §8, now backed by a new shared `dictionaries/quality_gate.py` script rather
  than ad hoc checks each round): 0 case-insensitive duplicates, 0 non-positive frequencies, 0 orphaned lemma
  links, 0 bare-NOUN rows - PASS.

  `hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` are Portuguese's own: `hints.tsv` keeps German's 10
  language-neutral assignments and gives the remaining 16 letters Portuguese content - a=ã, c=ç, e=é, i=í,
  o=õ, u=ú (accents/cedilla), g=«/r=» (quotes), s=€, t=º/y=ª (ordinal indicators), j/k/l/w/z filled with
  generically useful remaining typography (—, …, &, §, •). `diacritics.tsv`: a→ã,á,à,â; c→ç; e→é,ê; i→í;
  o→ó,ô,õ; u→ú. `abbreviations.tsv`: a hand-curated ~30-entry list. `PortugueseRules`
  (`LanguageRulesRegistry`): `decimalCommaGluesDigits`=true, `timeSuggestionWord`=null,
  `bundledConfusablesBlacklist`=empty - `confusables_scan.py` found 901 candidate pairs, left deliberately
  uncurated (a direct spot-check during this round confirmed why: "sei", 176, a genuine common word - "eu
  sei" - surfaced as a candidate risking autocorrect toward "seu", 46,532 - exactly the kind of call non-
  native judgement cannot safely make).

  New tests: `LanguageRulesTest` gained a `Portuguese resolves to PortugueseRules` case plus its own
  `PortugueseRules`-mirroring test block; the same round also added `ItalianRules`/`DutchRules` (both
  language-neutral, zero dictionary dependency, so written and tested alongside Portuguese's even though
  their own dictionaries are still in progress at commit time) and their own registry/test entries.
  `LanguagePackCatalogTest` needed no change - already fully generic over `ENTRIES`. `versionCode` 473 -> 474,
  `versionName` `"1.1.56"` -> `"1.1.57"`.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**: this pack has not been reviewed by anyone
  who actually speaks Portuguese. Real corpus scale (the largest raw-token count of any language pack this
  project has built) and a real lexicon, but still "pretty good" in the guide's own sense - and, unlike
  French/Spanish, genuinely incomplete for noun/adjective word-family data specifically (a confirmed source
  limitation, not merely "not yet reviewed"). Not device-confirmed either.

- **§417 (v1.1.56): D-444 - French/Spanish rebuilt from the correct native Wiktionary source with real**
  **Wortfamilien completion; the same wrong-source mistake and a genuine bare-noun capitalisation bug fixed**
  **in English/Greek too; the Language Contribution Guide hardened against both.** Direct follow-up question
  right after §416: "did French/Spanish also get Wortfamilien completion?" - checked directly, neither had a
  `lemma` column at all. Investigating the RAM sizing for the fix surfaced the real root cause: both
  language's own kaikki source (`kaikki.org/dictionary/<Language>/`) was the *English* Wiktionary's own
  coverage of that language, not its genuinely native edition
  (`kaikki.org/dictionary/downloads/<code>/<code>-extract.jsonl.gz` - French: 714.6MB vs. the wrong file's
  56.5MB, a 12x gap; Spanish: 99.3MB vs. 91MB, smaller but still wrong) - the same source German/Greek always
  used, never applied to French/Spanish until now. Full method, every real bug found and fixed along the way
  (a form-of-entry miscount inflating French's raw verb count 30x; a French "il/elle/on mange" combined-
  pronoun-slot notation poisoning frequency calibration to an impossible ~163x; a proper-noun collision check
  that missed "les" - a common pronoun in both languages - having a rare Wiktionary surname entry too), and
  the full Guide hardening are in `AdaptKey-History.md` §417 - not repeated here.

  **A second, independent, more serious bug found along the way, affecting the already-shipped English and**
  **Greek dictionaries too, not just French/Spanish:** neither language's own D-422/D-424 Wortfamilien merge
  had ever paired a resulting bare `{NOUN}` tag with `OTHER` the way French/Spanish's own `merge_dict.py`
  already did from day one - 36,580 rows in English's bundled `dict.tsv` and 46,608 in Greek's carried a bare
  `NOUN` tag, including ordinary function words (`and`, `it`, `he`, `or`, `were`, `this`, `not`, `its`) with
  one genuine but vanishingly rare technical/archaic noun sense. `CapitalisationEngine`'s rule 3 reads only
  the tag set, never frequency - every one of these would have been force-capitalised on ordinary typing, a
  real production bug found only because this round's own investigation asked "does the same gap exist
  elsewhere?" rather than staying scoped to French/Spanish. Fixed mechanically for both
  (`dictionaries/en/fix_bare_noun.py`, copied to `dictionaries/el/`) - confirmed via diff that nothing else
  changed. `dictionaries/el/version.txt` 5 -> 6, pack rebuilt; English is bundled, ships with the next
  ordinary app release.

  **Net results** (full numbers in each language's own `LanguagePackCatalog.kt` `Entry` comment): French
  `dict.tsv` 208,204 -> 373,700 rows (+165,496, first `lemma` linking), Spanish 233,636 -> 419,571
  (+185,935, first `lemma` linking), English/Greek unchanged row counts, only retagged. Every output
  re-verified: 0 duplicates, 0 non-positive frequencies, 0 orphaned lemma links, 0 bare-`NOUN` rows, known
  name/common-word collisions (`pierre`/`jean`, `sol`/`paz`/`victoria`/...) still correct.
  `dictionaries/confusables_scan.py` re-run against both larger dictionaries (French 994 -> 1,050
  candidates, Spanish 625 -> 650) - same shape, left uncurated for the same native-review reason.
  `dictionaries/fr/version.txt`/`dictionaries/es/version.txt` both 1 -> 2, packs rebuilt. The now-dead
  `extract_kaikki.py`/`kaikki_pos.tsv` for both languages (wrong-source, fully superseded) deleted; both
  `merge_dict.py` scripts updated to read the new `wiktionary_allpos.tsv` instead, so the whole pipeline now
  runs from one native Wiktionary source throughout. `AdaptKey-Spec.md`'s A-01/D-404-Tier-2 addendum updated
  to list French/Spanish alongside German/English/Greek as lemma-populated.

  No new tests (data-only; `LanguageRulesTest`/`LanguagePackCatalogTest` needed no change, already fully
  generic). 1340 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 472 -> 473, `versionName` `"1.1.55"` -> `"1.1.56"`.

  **The Language Contribution Guide hardened against both classes of mistake, per explicit user instruction**
  **("das soll direkt beim ersten Mal sitzen") before any future language is attempted:** a new "no
  shortcuts, no partial runs" statement naming both mistakes by name with real numbers; a **mandatory
  pre-flight check** (curl -I both candidate URLs, compare sizes, before downloading anything for real) with
  `kaikki.org/dictionary/rawdata.html` named as the canonical native-edition list; step 0 pointed at the now-
  real `extract_wiki_dump.py` reference scripts (previously stated none existed - stale); step 3 rewritten
  with the correct-vs-wrong URL pattern and real measured size gaps; step 4 rewritten to require one shared
  native-extract parse feeding both POS tagging and Wortfamilien completion, to prefer Greek's own generic
  per-form extraction for any language with non-trivial verb morphology, and to make the bare-noun safety
  check **mandatory and structural**, with the real English/Greek numbers as the cautionary tale. Every new
  script this round is a real, checked-in reference implementation for the next language, not a description.

- **§416 (v1.1.55): D-443 - first Spanish language pack, built fully autonomously, directly to the**
  **real-corpus/real-lexicon method D-441-followup (French, §415) ended up on - no intermediate/heuristic**
  **pass, per explicit user instruction not to re-walk the superseded French D-441 first attempt.** Spanish
  (`Language.SPANISH`) already used ordinary QWERTY (`LayoutRegistry.kindFor` -> `LayoutKind.LATIN_QWERTY`,
  no new layout code) and already had a real character-trigram profile (`language_profiles.tsv`, "es",
  since D-280) - both prerequisites §413/§414 had to build for French were already satisfied here.

  **Machine-memory check, done live rather than reusing French's own number (explicit user instruction):**
  `Get-CimInstance Win32_OperatingSystem` showed ~6.3GB free (vs. French's own ~4.5GB) just before starting.
  Deliberately not scaled up proportionally: `dictionaries/es/extract_wiki_dump.py`'s `PAGE_CAP` was set to
  100,000 (a modest, conservative increase over French's own confirmed-safe 80,000-page/~3.3GB-resident data
  point), with the same `root.clear()` fix, regex-based (not per-character) template stripping, `python -u`
  unbuffered invocation, and 5,000-page disk checkpointing already built in from the start - none of §415's
  own three real problems (an iterparse memory leak, a too-slow template stripper, stdout-buffering-under-
  redirection read as a false "stuck" process) were rediscovered this round. In the event, the cap was never
  reached: `eswiki-latest-pages-articles1.xml-p1p159400.bz2` (the official first split, 317.7MB compressed)
  contains only 47,669 real (ns=0, non-redirect) content pages - the *entire* first split was processed,
  91.97M real tokens, peaking at ~3GB resident with 6.4GB system-free still remaining, confirmed by direct
  `Get-Process`/`Get-CimInstance` checks during the run rather than assumed safe. One genuine process-
  management slip this round, caught and fixed before it could waste memory: the first launch attempt
  chained `cd es-dir && nohup ... &` as one backgrounded shell job, which silently left the *outer* shell's
  own cwd unchanged (the `cd` only applied inside the backgrounded subshell) - the immediate `cat
  extract_log.txt` right after therefore failed ("No such file"), but the background job itself had already
  started correctly. Not realising this at first, a second, correctly-invoked attempt was launched before the
  mistake was understood, briefly running two full extraction processes over the same dump in parallel -
  caught via `ps aux` and the older one killed immediately, before any real memory pressure built up.

  **POS tagging (kaikki.org's Spanish Wiktionary extract, `dictionaries/es/extract_kaikki.py`, directly
  modelled on `dictionaries/fr/extract_kaikki.py`):** 809,603 entries, 769,370 distinct Spanish word strings -
  larger than French's own 402,395/385,932, Spanish Wiktionary being the bigger extract. Identical POS mapping
  (`noun`/`verb`/`adj`/`prep`+`prep_phrase`/`name` -> `NOUN`/`VERB`/`ADJECTIVE`/`PREPOSITION`/`PROPER_NOUN`,
  everything else `OTHER`).

  **The merge (`dictionaries/es/merge_dict.py`, directly modelled on `dictionaries/fr/merge_dict.py`, same
  thresholds: `UNRECOGNISED_MIN_COUNT`=20, common-English-word noise filter at `en/dict.tsv` frequency >=100,
  rank-1 rescaled to ~1,000,000):** `dict.tsv` 233,636 rows (30,516 kept unrecognised-by-kaikki at count>=20,
  691,290 dropped below that floor, 12,049 removed as common-English-word contamination). `bigram.tsv`:
  732,856 rows at the same final >=10-occurrence cutoff French's own published pack uses (2,489,688 distinct
  pairs existed at the raw >=3 extraction-time floor before this cutoff).

  **The capitalisation-safety mechanism (§6 rule 3 not being `Language`-gated, D-441's own structural finding)
  re-applied and actively re-verified against Spanish's own real collision pattern, per explicit user
  instruction to search for it rather than only trust the mechanical rule:** zero bare-`NOUN` rows confirmed
  directly in the final `dict.tsv` (every Spanish common noun lands `NOUN,OTHER`/`NOUN,ADJECTIVE`/`NOUN,VERB`
  instead). The `PROPER_NOUN`-drop-on-collision rule (`resolve_tags()`) was checked against exactly the kind
  of case the user named - a real Spanish first name that is *also* an ordinary common noun - and confirmed
  working correctly by direct inspection, not assumed: `sol`/`paz`/`victoria`/`luz`/`estrella`/`blanca`/
  `clara`/`esperanza`/`flor`/`dolores`/`pilar`/`mercedes`/`rosario`/`milagros`/`amparo`/`remedios`/`consuelo`
  all resolved to their common-noun tags, never `PROPER_NOUN` - a forced capital on the far more frequent
  common-noun reading (`el sol`, `la paz`, `una flor`) would otherwise have fired every time.

  **`hints.tsv`/`diacritics.tsv`/`abbreviations.tsv` - Spanish's own, not reused from German or French:**
  `hints.tsv` keeps German's 10 language-neutral math/typography assignments (`b`=*, `d`=°, `f`=ƒ, `h`=#,
  `m`=-, `n`=+, `p`=π, `q`=@, `v`=/, `x`=×) and gives the remaining 16 letters real Spanish content: `a`=á,
  `e`=é, `i`=í, `o`=ó, `u`=ú (the five accented vowels); `w`=ü (u already hosts ú, so the diaeresis - needed
  for `güe`/`güi` words like "pingüino" - sits on the loanword-only letter w instead); `l`=ñ, deliberately
  *not* `n` (explicit user instruction: `n` already carries the neutral "+", and a real Spanish hardware
  keyboard places Ñ physically next to L anyway, the mnemonic this placement borrows); `c`=¿, `j`=¡ (the two
  Spanish-iconic inverted punctuation marks); `g`=«, `r`=» (Spain's own standard quotation-mark convention);
  `s`=€; `t`=º, `y`=ª (the masculine/feminine ordinal indicators, a genuinely Spanish-specific typographic
  need, the same role German's own ° already fills for degree); `k`/`z` filled with the remaining generically
  useful glyphs (—, …). `diacritics.tsv`: `a`→á, `e`→é, `i`→í, `o`→ó, `u`→ú,ü, and `n`→ñ - included on the
  explicit understanding, checked directly against D-436's own design note before writing it, that ñ needs no
  dedicated hardcoded special case the way German's ß does (there is no genuine alternate-ASCII-spelling
  convention for ñ the way ss substitutes for ß - the base-letter-to-real-variant shape is otherwise identical
  to every other entry, so the ordinary data-driven `DataDiacriticFolding` already handles it correctly).
  `abbreviations.tsv`: a hand-curated 26-entry Spanish sentence-boundary list (sr./sra./dr./ud./pág./núm./
  etc./...).

  **`SpanishRules` (`LanguageRulesRegistry`):** `decimalCommaGluesDigits`=true (the RAE/Spain convention this
  pack follows - several Latin American locales use a point instead, not representable separately since this
  app has no per-region Spanish variant, a real, named scope limit rather than a silently-ignored one),
  `timeSuggestionWord`=null, `bundledConfusablesBlacklist`=empty. `dictionaries/confusables_scan.py
  dictionaries/es/dict.tsv qwerty 30` ran directly (no `KeyboardProximity` prerequisite to unblock first,
  unlike French's own D-442 AZERTY work) and found 625 candidate pairs, reviewed by hand per explicit
  instruction to use real language judgement rather than reflexively leaving it empty: mostly short (2-3
  letter) tokens risking autocorrect into a common neighbouring function word. Several are confirmed-real
  Spanish words/contractions (`fe`=faith, `ve`="goes"/"sees", `re`=musical note/colloquial intensifier,
  `pa`=colloquial "para", `eh`=interjection) sitting right alongside genuinely ambiguous short fragments
  (`we`, `ce`, `dd`, `sn`, `rn`, and more) this round's own non-native judgement could not confidently rule
  out as pure noise rather than an abbreviation-with-its-period-stripped artefact (the tokeniser drops all
  punctuation, so e.g. "ej." becomes bare "ej") or a foreign name fragment - left empty for a native-speaker-
  guided pass rather than guessed at, matching the explicit "im Zweifel lieber leer lassen" instruction for
  this round.

  **Packaging:** `language-packs/adaptkey-lang-es.zip` (6.1MB, six files at the archive root, no directory
  prefix), one new `LanguagePackCatalog.Entry(Language.SPANISH, ..., version = 1)` with a detailed inline
  comment covering this round's own method/numbers/honesty caveats, mirroring German/Greek/French's own
  precedent. Working intermediates not committed (`wiki_dump_freq.tsv`/`wiki_dump_bigram.tsv`/
  `noise_review_candidates.tsv`/the extraction log) - only the final `dict.tsv`/`bigram.tsv`/`hints.tsv`/
  `diacritics.tsv`/`abbreviations.tsv`/`version.txt`, the pipeline scripts themselves, and `kaikki_pos.tsv`
  (14MB, the direct kaikki-derived reference file `merge_dict.py` actually reads) were kept, mirroring
  French's own `kaikki_pos.tsv`/German's/Greek's own Wiktionary-derived reference-file precedent.

  New tests: `LanguageRulesTest` gained a `Spanish resolves to SpanishRules` case plus its own
  `SpanishRules`-mirroring test block (decimal comma, no time-suggestion word, empty blacklist, all six
  German-specific hooks no-op), matching `FrenchRules`'s existing shape exactly. `LanguagePackCatalogTest`
  needed no change - already fully generic over `ENTRIES`. 1335 -> 1340 unit tests, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. No `AdaptKey-Spec.md` change - this round confirmed
  D-441's own existing structural finding (§6 rule 3 not `Language`-gated) rather than discovering a new one,
  so no fresh addendum was needed there; `LanguagePackCatalog`'s own class KDoc updated to drop `SPANISH` from
  the "not yet built" list. `versionCode` 471 -> 472, `versionName` `"1.1.54"` -> `"1.1.55"`.

  **Honesty gate (step 11) - deliberately NOT claimed satisfied**, same standard as French: this pack has not
  been reviewed by anyone who actually speaks Spanish. Real corpus scale and a real lexicon from the very
  first pass this time (no superseded heuristic round preceding it, unlike French's own D-441/D-441-followup
  two-round history) - genuinely a stronger starting point than French's own first pass was - but still
  "pretty good" in the guide's own sense, not native-reviewed quality. Not device-confirmed either (no real
  device available in this environment). Where this pack currently stands relative to the other three: ahead
  of French on data-collection method maturity (built to the real-corpus method from the start, one round
  instead of two), but behind German/English/Greek's own months of real native-speaker device feedback, and
  behind French in one narrow respect - `dictionaries/es/dict.tsv` has not yet had a Wortfamilien-style
  paradigm-completion pass the way German's/Greek's/French's own kaikki-native-inflection-forms did (Spanish
  Wiktionary, like French's, already lists most inflected forms as their own individually-tagged kaikki
  entries, so this is likely already largely covered incidentally - not separately verified this round).

- **§415 (v1.1.54): D-441-followup - French dictionary rebuilt from a real Wikipedia dump + kaikki.org**
  **Wiktionary POS data, after direct user feedback that §413's first pass (12,000 words, heuristic POS**
  **tagging) was too thin and not a genuine run of the guide's own pipeline.** Full method, real numbers,
  and the two real bugs found/fixed along the way (a memory leak, a performance bug - both confirmed the
  hard way by running the real thing, not reasoned about abstractly) are in `AdaptKey-History.md` §415.
  Short version: a new `extract_wiki_dump.py` streams an actual French Wikipedia dump part (80,000 of
  306,134 articles, capped for this machine's real ~4.5GB free RAM, checkpointed every 5,000 pages so a
  safety stop never loses data) for real word/bigram frequencies; a new `extract_kaikki.py` pulls real POS
  tags from kaikki.org's French Wiktionary extract (402,395 entries) instead of suffix-guessing; a new
  `merge_dict.py` combines both, with a real (if bounded) noise-removal pass - any word not in kaikki that
  is also a common word in this project's own bundled `en/dict.tsv` is dropped (11,086 rows), confirmed by
  hand-sampling to catch genuine English-quote contamination while leaving rare-but-real French vocabulary
  alone. `dict.tsv` 12,000 -> 208,204 rows, `bigram.tsv` 2,293 -> 984,792 rows - both real corpus data now,
  not a small proxy source. The same capitalisation-safety mechanism (§413) re-verified against the much
  larger output (zero bare-`NOUN` rows); a new related case found and fixed - `PROPER_NOUN` is now dropped
  whenever kaikki also saw the same string used as an ordinary word (`pierre`/`jean` would otherwise have
  been wrongly force-capitalised every time). D-442's own `KeyboardProximity` fix unblocked a real AZERTY
  confusables scan (994 candidates) - deliberately left un-curated, the same "several are genuine French
  abbreviations this round can't confidently separate from noise" reasoning as D-442's own English scan.

  No test changes (data + doc-only). 1335 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 470 -> 471, `versionName` `"1.1.53"`
  -> `"1.1.54"`. Not device-confirmed, and - restated deliberately, per the guide's own mandatory step 11 -
  still not reviewed by a native French speaker: real corpus scale now, but still "pretty good", not
  native-reviewed quality.

- **§414 (v1.1.53): D-442 - `KeyboardProximity` turned into a real per-layout interface**
  **(QWERTZ/QWERTY/AZERTY/Greek), plus English's own real confusables blacklist - explicit user request,**
  **run as a parallel task while D-441's French dictionary rebuild (§415) processed in the background.**
  `KeyboardProximity` is now an interface with one object per row geometry
  (`KeyboardProximityQwertz`/`Qwerty`/`Azerty`/`Greek`) plus a `KeyboardProximityRegistry` keyed by
  `LayoutKind`, mirroring `LanguageRules`/`DiacriticFolding`'s existing "interface + object + registry"
  shape - the user's own concrete design, implemented as specified. Threaded through
  `DictionarySuggestionProvider` (the one real call site: D-28/D-38/S-09's typo-neighbour signals) via a
  new `keyboardProximity` constructor parameter, resolved per-language in `AdaptKeyService.installStores()`
  exactly like `languageRules`/`diacriticFolding` already are. A French/AZERTY user's typo correction now
  genuinely scores against AZERTY's real adjacency instead of silently falling back to QWERTZ's.

  Also built the confusables-scan tooling this unblocks (`dictionaries/confusables_scan.py`, a generic
  scanner implementing the D-304/D-330-followup method against `CorrectionConfidence.forKnownWordOverride`'s
  own real formula) and ran it against English's own bundled `en/dict.tsv` with the new
  `KeyboardProximityQwerty` grid - 1,384 candidate pairs found, the 37 at maximum score hand-reviewed. Five
  short tokens confirmed not to be real English words (`ij`/`iz`/`iy`/`ae`/`ne`) became `EnglishRules`'s own
  `bundledConfusablesBlacklist()` (new class, mirrors `FrenchRules`); `AdaptKeyService.seedBundledBlacklist()`
  itself was hardcoded to German only and is now generalised to loop over every language. Two further,
  larger findings from the same scan - a real frequency-correction backlog, and possible pre-existing noise
  in `en/dict.tsv` itself - are deliberately **not** tackled this round; see their own Open TODO entries.

  1309 → 1335 unit tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  469 -> 470, `versionName` `"1.1.52"` -> `"1.1.53"`. Spec's A-04 gained a one-sentence D-442 note. Not
  device-confirmed - pure dictionary/ranking logic, no Android glue touched.

- **§413 (v1.1.52): D-441 - French language pack built end to end, fully autonomously, on explicit user**
  **request to try the Language Contribution Guide's own §8 one-shot pipeline unattended.** D-314 already
  built AZERTY's physical geometry; this round is the dictionary/hints/diacritics/abbreviations/rules
  content. Full method, real numbers, and every deliberate scope cut are in `AdaptKey-History.md` §413 -
  short version: `dict.tsv` (12,000 words, real `hermitdave/FrequencyWords` OpenSubtitles-corpus
  frequencies rescaled to German's own magnitude, rule-based POS tagging with curated exception lists, no
  per-word LLM pass), `bigram.tsv` (2,293 rows from a real but modest live-fetched French-Wikipedia
  corpus - hit Wikipedia's own anonymous-API rate limit partway through, handled with retry/backoff rather
  than abandoned), `hints.tsv`/`diacritics.tsv` (French's own AltGr/diacritic set, not a German reuse -
  every real French accent reachable via a distinct key), `abbreviations.tsv`, `FrenchRules`
  (`LanguageRulesRegistry`, the three naively-fillable hooks only), one `LanguagePackCatalog` entry.
  **A genuinely new, structural finding, not specific to French**: `CapitalisationEngine`'s §6 rule 3
  ("pure noun -> auto-capitalise") is not gated by `Language` anywhere in the code - see the new D-441
  addendum to spec §6. French common nouns are tagged `NOUN,OTHER` (never a bare `NOUN`) specifically to
  avoid triggering it, verified directly (zero bare-`NOUN` rows in the final file). **Also found, filed as
  its own separate open TODO**: `KeyboardProximity` is hardcoded to QWERTZ with no AZERTY awareness, so the
  guide's own step-7 confusables scan could not actually be run against French's real layout -
  `bundledConfusablesBlacklist()` stays empty for French, same as English/Greek.

  1304 → 1309 unit tests (4 new `LanguageRulesTest` cases for `FrenchRules`, one existing assertion
  corrected - `Language.FRENCH` no longer resolves to `NoOpLanguageRules`), all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec §6 gained the D-441 capitalisation-mechanism
  addendum. `versionCode` 468 -> 469, `versionName` `"1.1.51"` -> `"1.1.52"`. **Not device-confirmed, and -**
  **per the guide's own mandatory step 11 - deliberately NOT claimed to have passed a native-French-speaker**
  **review yet.** This is a "pretty good", pipeline-built pack, not native-reviewed quality; do not publish
  it as more than that until a real French speaker has sanity-sampled the output.

- **§412 (v1.1.51): D-440-followup - device feedback on §411: the chip now correctly shows "Fröhlich"/**
  **"Grüße", but tapping it still inserted "fröhlich"/"grüße".** A second, independent call site with the
  identical failure shape: `onSuggestionClicked()`'s `Kind.NORMAL` tap handler unconditionally re-derives
  `capitalisation.capitalise(item.word, contextFor(composing.toString()))` before committing - correct and
  necessary for an ordinary typing-time candidate, but for a next-word-prediction chip tapped with `composing`
  empty it is the exact same meaningless-`explicitFirstUpper` fallthrough §411 already fixed once, just in the
  commit path instead of the display path. Fixed identically: only re-derive while `composing.isNotEmpty()`,
  otherwise trust `item.word` (already resolved by `canonicalWordFor()`). "dank" after "Vielen" staying
  lower-case is confirmed **expected**, not a remaining bug - see §411's own explanation (genuinely ambiguous
  dictionary entry, no dual-casing-chip coverage for next-word predictions yet, a separate open question).

  No new tests - same `AdaptKeyService` untested-orchestrator-glue boundary as §411. 1304 unit tests
  unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's S-07/D-440 addendum
  extended. `versionCode` 467 -> 468, `versionName` `"1.1.50"` -> `"1.1.51"`. **Device-confirmed (2026-09-04,**
  **Pixel 9a) - both "Fröhlich" and "Grüße" now correctly suggest and insert capitalised. D-440 closed.**

- **§411 (v1.1.50): D-440 - real root cause found from the §410 diagnostic log the user captured and**
  **shared, in a completely different place than the log's own original suspects.** The log proved
  `SqliteDictionaryStore.canonicalWordFor()` already innocent: `wkey="fröhlich" learned="Fröhlich" ... ->
  "Fröhlich"` and `wkey="grüße" ... bundled="Grüße" ... -> "Grüße"` - both resolved correctly, right there in
  the `nextWordSuggestions()` call. The actual bug sits one step later, in `AdaptKeyService.showSuggestions()`
  's blanket `if (item.kind == Kind.NORMAL) capitalisation.capitalise(item.word, context)` re-derivation
  (D-196's own design: ordinary typing candidates deliberately arrive uncapitalised, re-cased fresh from live
  context right before rendering). `capitalise()`'s rule 1 ("explicit user input is never changed") depends on
  `context.explicitFirstUpper` reflecting what was actually typed - meaningless with `composing` empty (a
  next-word prediction's defining condition), so a genuinely ambiguous noun (`isAmbiguousNoun -> false`) fell
  through to its unconditional lower-case default, silently overwriting the already-correct casing
  `canonicalWordFor()` had just supplied moments earlier. Fixed with one added condition:
  `item.kind == Kind.NORMAL && composing.isNotEmpty()` - the exact state that distinguishes "still typing,
  needs fresh §6 derivation" (unchanged) from "next-word prediction, already fully resolved, must not be
  touched again". "dank" needed no fix and stays as designed - its only bundled entry is genuinely ambiguous
  with no more-specific casing to prefer; whether next-word predictions should also get the S-11-style
  dual-casing-chip treatment for that case is a separate, still-open design question, not part of D-440.
  Removed §410's own temporary `[AdaptKeyNextWordCasing]` diagnostic logging now that it has served its
  purpose.

  No new tests - the fix is one condition in `AdaptKeyService`'s own established untested orchestrator glue
  (`showSuggestions()`); `CapitalisationEngine.capitalise()` itself (already unit-tested) is unchanged. 1304
  unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's S-07 gained
  a D-440 addendum. `versionCode` 466 -> 467, `versionName` `"1.1.49"` -> `"1.1.50"`. **Device-confirmed for**
  **the *display* half - the chip itself now correctly shows "Grüße"/"Fröhlich"; tapping it still committed
  the wrong casing, a second bug - see §412.**

- **§410 (v1.1.49, temporary diagnostic): D-440 - user-reported follow-up on §404/D-438+D-439: "vielen**
  **Dank"/"viele Grüße"/"Marvin Fröhlich" next-word-prediction chips stay lowercase ("dank"/"grüße"/**
  **"fröhlich") no matter how often retyped.** Root-caused as far as static code reading allows before this
  round, then hit a real contradiction that needed device data instead of more guessing (this project's own
  established pattern for exactly this class of bug):
  - **"dank" - fully explained, no fix needed here.** `dictionaries/de/dict.tsv` confirmed directly: `dank`
    is a single row, `NOUN,PREPOSITION,OTHER`, genuinely stored lowercase - the same intentional §6-rule-5
    ambiguity as "Weg"/"weg" (`CapitalisationEngine.isAmbiguousCasing`). The existing dual-casing-chip
    mechanism for this (`ambiguousCasingChips()`, D-404-followup) only ever resolves it while typing - it
    returns `emptyList()` outright for an empty `input` ([AdaptKeyService.kt:5522](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:5522)), which a next-word
    prediction always is (nothing typed yet) - so `showNextWordPredictions()` never reaches it. A genuine,
    confirmed gap - but a design question (dual chips before typing even starts, vs. a single best-guess
    reading?) belongs to its own round once D-440's more surprising half is understood.
  - **"grüße"/"fröhlich" - contradicted a straightforward code reading, real bug still open.**
    `dictionaries/de/dict.tsv` confirmed `Grüße` (capital G) is already the single, correctly-cased bundled
    row (`NOUN,VERB`) - unlike "dank", there is no missing-casing dictionary gap here at all. `canonicalWordFor
    (wkey) = learnedEntryOf(wkey)?.word ?: bundledEntryOf(wkey)?.word ?: wkey` should therefore already
    resolve "grüße" to "Grüße" via the bundled fallback alone. Then a real contradiction: the user confirmed
    "Fröhlich" already sits in `TABLE_LEARNED` correctly capitalised (from D-439's own fix) - `canonicalWordFor`
    should hit that on its very first lookup - yet the next-word chip still shows lowercase "fröhlich" after
    repeated retyping, and "Grüße" stays lowercase and never gets learned at all despite four more real,
    confirmed-fresh-pack retypes of "Viele Grüße". Both directly contradict this function's own code as read
    from source. Added temporary diagnostic logging (`[AdaptKeyNextWordCasing]`, gated by the existing X-01
    toggle) to `SqliteDictionaryStore.canonicalWordFor()` (what `learnedEntryOf`/`bundledEntryOf` actually
    found, if anything) and `nextWords()` (the raw wkeys the bigram queries actually returned, before any
    casing resolution) - removed once D-440 is closed. Needs the user to enable Diagnostic Log, reproduce
    (type "Marvin " / "viele " and check the chip), and share the `[AdaptKeyNextWordCasing]` lines back.

  No new tests - `SqliteDictionaryStore` is this project's own established instrumented-test/device-only
  boundary. 1304 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 465 -> 466, `versionName` `"1.1.48"` -> `"1.1.49"`. No spec change - diagnostic-only, no
  user-facing behaviour changed.

- **§409 (v1.1.48): D-396-followup (v5) - device feedback on §408: the popup-open cue's removal confirmed**
  **correct, but accepting an alt-char (`HapticTier.CORRECTION`, still `DEFAULT_AMPLITUDE`) felt considerably**
  **too pronounced/heavy.** User self-diagnosed correctly ("das ist der Chip-Annahme-Effekt") - `CORRECTION`
  was tuned for the rarer suggestion-chip case originally and had never actually been felt for a much more
  frequent everyday action until §408 also routed alt-char acceptance through it. Added a new, explicitly
  lowered `CORRECTION_AMPLITUDE` (a considered starting point, not yet device-tuned) instead of
  `DEFAULT_AMPLITUDE` - deliberately kept a bit firmer than `KEY_PRESS_AMPLITUDE`, since accepting a selection
  is still rarer/more deliberate than routine typing. `MODE_SWITCH` untouched, still `DEFAULT_AMPLITUDE`.

  No new tests - same Android `Vibrator` glue boundary as §405-§408. 1304 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's §42 gained a further addendum. `versionCode`
  464 -> 465, `versionName` `"1.1.47"` -> `"1.1.48"`. **Not yet device-confirmed** - needs a real feel-check
  on the Pixel 9a that accepting an alt-char (and a suggestion chip) now feels appropriately toned down;
  likely needs at least one further tuning round given how subjective this keeps turning out to be.

- **§408 (v1.1.47): D-396-followup (v4) - §407's `EFFECT_TICK` confirmed exactly right on the Pixel 9a**
  **(genuinely the real effect, not the pre-API-29 fallback); user then asked to fix the L-05 long-press-**
  **popup flow's own haptics while still in the same round.** Three distinct moments around a popup, and only
  one needed a vibration: tap-down (already `KEY_PRESS`, correctly kept - delaying it would make every
  ordinary tap feel unacknowledged), popup opening (previously fired the platform's own generic
  `performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)` unconditionally - redundant per the user, the
  tap-down cue already confirms the press, removed for the popup case specifically), and accepting an
  alternative (`commitPopupSelection()` - previously silent, the one genuinely missing cue, now fires
  `HapticTier.CORRECTION`, the same tier a suggestion chip already uses for "a deliberate selection was just
  accepted"). The plain system long-press haptic is kept for the one remaining branch with no popup at all (a
  key whose only long-press action is the listener callback); G-06's own Caps-Lock haptic was already routed
  through its own dedicated call, never through the removed generic one, so it is unaffected.

  No new tests - same Android View-glue touch/haptics boundary as §405-§407. 1304 unit tests unchanged, all
  green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's §42 gained a further addendum.
  `versionCode` 463 -> 464, `versionName` `"1.1.46"` -> `"1.1.47"`. **Device-confirmed (2026-09-04, Pixel 9a)**
  **for the popup-open silence - correct; the accept-a-selection cue turned out too strong, see §409.**

- **§407 (v1.1.46): D-396-followup (v2) - user feedback on §406: `HapticTier.KEY_PRESS` should read as a**
  **short click/detent ("Rasten oder Klicken"), not a brief buzz, and quieter still.** A manually-timed
  `createOneShot()` pulse is just an on/off motor tap and cannot really render a "click" feel on its own -
  switched `KEY_PRESS` to `VibrationEffect.EFFECT_TICK` (API 29+), a real, hardware-tuned predefined effect
  built for exactly this lighter click/detent sensation. `createPredefined()`'s own documented behaviour
  already falls back to a generic pattern on a device with no dedicated implementation, so no capability
  check was added. Confirmed against source before relying on it: `VibrationScaler` maps the system's
  intensity setting directly to a predefined effect's own hardware LIGHT/MEDIUM/STRONG strength (the
  prebaked-effect equivalent of the plain-amplitude scaling §406 already confirmed), so `HapticTier`'s own
  quantity gate keeps working unchanged. Below API 29 (no predefined effects), `createOneShot()` remains the
  only option - its own duration/amplitude both further shortened/lowered (new `KEY_PRESS_FALLBACK_DURATION_MS`
  = 15L, `KEY_PRESS_AMPLITUDE` 50 -> 30), both considered starting points, not device-tuned - flagged against
  D-06/D-34's own older, opposite-direction finding that a *very* short pulse could read as imperceptible on
  that device, so this specific fallback path deserves its own real check if ever exercised on genuinely old
  (API 26-28) hardware; the user's own Pixel 9a (API 34+) never reaches it. `MODE_SWITCH`/`CORRECTION`
  unaffected, still the ordinary `HAPTIC_DURATION_MS`/`DEFAULT_AMPLITUDE` one-shot.

  No new tests - same Android `Vibrator` glue boundary as §405/§406. 1304 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's §42 gained a further addendum. `versionCode`
  462 -> 463, `versionName` `"1.1.45"` -> `"1.1.46"`. **Device-confirmed (2026-09-04, Pixel 9a) - exactly**
  **right ("das gefällt mir richtig gut"), and genuinely the real `EFFECT_TICK`, not the fallback path.**

- **§406 (v1.1.45): D-396-followup - device-confirmed on the Pixel 9a: the quantity gate (§405) works**
  **exactly as designed, but `HapticTier.KEY_PRESS` felt too strong at the system's own high level.** User
  first proposed either tuning the raw amplitude down for that specific level, or - preferred - making the
  effect not scale with the system level at all; asked directly for an assessment rather than a blind
  implementation. Researched against AOSP's `VibrationScaler` source before answering: the OS's own scaling
  is a modest 0.6x-1.4x delta around whatever amplitude the app itself requests, applied to every usage
  category including "unknown" - there is no legitimate way to exempt a vibration from it, and deliberately
  going unclassified to try would not achieve that anyway (still scaled, just against a different, less
  predictable category) while reopening the D-06/D-34/D-66/D-75 OEM-mute risk this project already fixed.
  Recommended, and user agreed: keep the classification, request a deliberately low, fixed amplitude
  (`KEY_PRESS_AMPLITUDE`, a considered starting point, not individually device-tuned) for `KEY_PRESS` only
  instead of `VibrationEffect.DEFAULT_AMPLITUDE` - keeps the whole 0.6x-1.4x range comfortably quiet without
  fighting the OS's own (working-as-designed) scaling. `MODE_SWITCH`/`CORRECTION` unaffected, still
  `DEFAULT_AMPLITUDE`, matching what the user reported as already fine.

  No new tests - same Android `Vibrator` glue boundary as §405. 1304 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's §42 gained a device-confirmation addendum.
  `versionCode` 461 -> 462, `versionName` `"1.1.44"` -> `"1.1.45"`. **Device-confirmed (2026-09-04, Pixel**
  **9a) as closer, but still not quite right - "buzz" vs. the wanted "click/detent" feel, and still not
  quiet enough** - see §407 for the resulting follow-up.

- **§405 (v1.1.44): D-396 - per-key/Caps-Lock vibration now respects the OS's own three-level "Haptic**
  **feedback" intensity slider, both in strength and in quantity.** Design discussed and agreed with the user
  first (this project's own rule for non-trivial design decisions), including a real API-research round
  before committing to an approach - the obvious read-back API (`Vibrator.getDefaultVibrationIntensity`) is
  `@hide`/`@TestApi`, not public SDK, confirmed directly against AOSP source rather than assumed; the
  underlying `Settings.System` key (`"haptic_feedback_intensity"`) is a real, ordinarily-readable provider
  value though, confirmed live in `VibratorService.updateVibrationIntensityLocked()`. Two independent
  mechanisms, corresponding to the two things "level" can mean: (1) **strength** - every `vibrate()` call is
  now touch-classified (already `VibrationAttributes.USAGE_TOUCH` on API 33+; newly `AudioAttributes.
  USAGE_ASSISTANCE_SONIFICATION` below that, via the only classified overload available there), so the
  system's own amplitude scaling for this slider reaches every API level down to minSdk 26, not just 33+ as
  before; (2) **quantity** - a new `HapticTier` enum (`MODE_SWITCH`/`CORRECTION`/`KEY_PRESS`, thresholds
  1/2/3) gates which events still vibrate at a given system level, read via the new
  `AdaptKeyboardView.refreshSystemHapticLevel()` (cheap, re-read on every `applySettings()` reload, not a
  per-keystroke cost) with a fallback to 3 (today's always-on behaviour) whenever the read fails. The
  `CORRECTION` tier is genuinely new wiring: `AdaptKeyService.notifySuggestionAccepted()` (D-88/§56's
  existing single choke point for both a silent autocorrection and an explicit chip tap) now also calls the
  new `AdaptKeyboardView.fireCorrectionHaptic()` - autocorrect/chip acceptance never vibrated at all before
  this round. C-13 and G-06's own independent toggle are both unchanged and still gate everything first, per
  the user's own explicit call to keep the app-local switch - D-396 only adds a further, system-level filter
  on top.

  No new tests - this is Android `Vibrator`/`Settings.System` glue, this project's own established untested
  boundary (mirrors D-361/D-380's identical touch/haptics work); the one new call site inside
  `AdaptKeyService` (`notifySuggestionAccepted`) is this project's own established untested orchestrator glue
  too. 1304 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec
  gained new §42. `versionCode` 460 -> 461, `versionName` `"1.1.43"` -> `"1.1.44"`. **Device-confirmed for the
  quantity gate (2026-09-04, Pixel 9a) - the strength half needed a follow-up fix, see §406.**

- **§404 (v1.1.43): D-438/D-439 - two real, root-caused bugs reported together: "vielen Dank"/"viele Grüße"**
  **never got next-word suggestions despite frequent typing, and "Fröhlich" (a surname) after "Marvin" only**
  **ever got learned lower-case as the ordinary adjective "fröhlich".**

  **D-438 (S-07 next-word candidate selection):** confirmed directly against the real bundled
  `dictionaries/de/bigram.tsv`, not guessed - "vielen"/"viele" both have two dozen-plus real, high-frequency
  bundled continuations (`vielen anderen` 401, `vielen fällen` 269, ... `viele der` 606, `viele andere` 410,
  ...), none of them "dank"/"grüße" (a genuine Wikipedia-corpus register gap - an encyclopedia does not
  contain personal letter-closing phrases). `DictionaryStore.nextWords()` merged bundled+learned bigram
  counts and cut to `limit` by *raw* count *before* the caller's own D-365 rescaling ever ran - so a personal
  phrase's own modest raw count could never survive that cut when the previous word already had many
  corpus-scale bundled continuations, regardless of how many times it was actually typed; D-365's rescaling
  fix could only ever help a candidate that reached it, and this one never did. This is exactly why "Marvin"
  -> "Fröhlich" *did* work already (a first name has few or no bundled continuations to compete against) while
  "vielen"/"viele" did not (rich bundled competition). Fixed in both `SqliteDictionaryStore.nextWords()` and
  `InMemoryDictionaryStore.nextWords()`: every learned continuation for a previous word is now always
  included in the candidate pool, never subject to the bundled table's own raw-count cut - mirrors D-209's
  own "the word's own bucket is uncapped" precedent for prefix-candidate selection. `nextWordsTrigram()` was
  never affected (no bundled trigram table exists at all - purely personal, D-246).

  **D-439 (W-02/W-04 promotion, D-271's own exception):** confirmed by reading `differsOnlyInFirstChar()`'s
  own call sites, not guessed - its KDoc always said the exception exists for "a word that only looks
  differently-cased because it happened to start a sentence", but neither `learnWord()` nor `learnWordStrong()`
  actually checked `tokenSentenceStart` before applying it. "Fröhlich" typed mid-sentence directly after
  "Marvin" differs from the bundled adjective "fröhlich" only in its first character, so the exception fired
  unconditionally and routed it through D-327's `learnContext()`-only path (bigram context learned, unigram
  never reinforced) forever, regardless of how many times it was typed - it could never accumulate its own,
  separately-cased W-02 promotion. Fixed by requiring `tokenSentenceStart` alongside the existing
  `differsOnlyInFirstChar()` check in both functions - a genuine sentence-start recasing (`"das"` -> `"Das"`)
  is still correctly treated as nothing to learn, exactly as before; a mid-sentence homograph now falls
  through to the ordinary not-yet-known-word path instead, accumulating via the same W-02 threshold
  (`PendingLearnStore`) as any other new word - no accidental-pollution risk beyond what already existed for
  every other word, addressing the user's own worry directly. The existing `categoryHint` computation already
  correctly derives `PartOfSpeech.NOUN` for this exact shape (capitalised, not at a sentence start) with no
  change needed - once promoted, "Fröhlich" is tagged as a noun automatically, alongside its own separate,
  correctly-capitalised learned entry that then wins over the bundled lower-case adjective in ranking/
  suggestions (W-04's own existing override mechanism, unchanged).

  New tests: `InMemoryDictionaryStoreTest`/`SqliteDictionaryStoreRoboTest` (a learned continuation survives
  many higher-frequency bundled competitors; the bundled-only remainder still fills leftover slots),
  `DictionarySuggestionProviderTest` (the same shape end-to-end through `nextWordSuggestions()`). D-439's own
  fix sits entirely in `AdaptKeyService`'s untested orchestrator glue (`learnWord`/`learnWordStrong`), this
  project's own established untested boundary - no new tests there. 1300 -> 1304 unit tests, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's S-07 (D-438) and W-04 (D-439) both gained
  addenda. `versionCode` 459 -> 460, `versionName` `"1.1.42"` -> `"1.1.43"`. **Not yet device-confirmed** -
  needs real repeated typing of "vielen Dank"/"viele Grüße" (D-438) and "Marvin Fröhlich" enough times to
  cross the promotion threshold again (D-439, a fresh promotion - the existing wrongly-lower-cased "fröhlich"
  bigram-only context does not need manual cleanup, it simply stops being the only thing that ever
  accumulates).

- **§403 (v1.1.42): D-380/D-437 - a long-press smear that left the pressed key's own bounds unconditionally**
  **cancelled the pending alt-popup, even when nowhere near swipe-sized - the user's own precise repro (`o`
  key): smearing while staying inside the key reliably opens the popup (D-108, already correct), smearing
  off the key does nothing at all.** Root-caused directly in `AdaptKeyboardView.onTouchEvent()`'s ACTION_MOVE
  handler, not guessed: `movedOutsideKey()` alone gated the `cancelPendingLongPress()` call - the instant the
  raw touch point left the key's own small rect, the long-press timer was cancelled, regardless of how far it
  had actually travelled. Meanwhile `resolveSwipe()` (evaluated only at release) requires a real gesture's own
  much larger distance (a three-key-width field-swipe threshold, or the smaller but still-key-sized
  space-bar-language-swipe threshold) - leaving a dead zone where a smear was far enough to leave the key but
  nowhere near a genuine swipe: long-press cancelled, no swipe recognised either, nothing left to show for it.

  Fixed by requiring the *same* distance a release would need to actually register as a swipe before an
  in-progress ACTION_MOVE may cancel the pending long-press - extracted `resolveSwipe()`'s own threshold
  logic (previously computed only at release) into a new shared `swipeDirectionIfFarEnough(key, dx, dy):
  SwipeDirection?`, called both by `resolveSwipe()` itself (behaviourally unchanged) and by the new
  ACTION_MOVE gate: `movedOutsideKey(...) && swipeDirectionIfFarEnough(...) != null` now must both hold before
  the long-press is cancelled. A smear that leaves the key but never reaches real swipe distance therefore no
  longer cancels anything - the popup opens on schedule, anchored correctly to the originating key regardless
  of where the finger has since drifted (`openPopup()` derives position purely from the key's own rect, never
  from live pointer coordinates - confirmed by reading it, not assumed). D-108's own existing "smear fully
  inside the key never cancels" behaviour is untouched - this only widens the *outside-the-key* case from
  "any movement at all" to "movement close to swipe-sized".

  No new tests - `AdaptKeyboardView`'s touch resolution is Android View glue, this project's own established
  untested boundary (mirrors D-361's identical touch-zone work). 1300 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's L-05 gained a D-380/D-437 addendum.
  `versionCode` 458 -> 459, `versionName` `"1.1.41"` -> `"1.1.42"`. **Device-confirmed (2026-09-04):** long-press
  the `o` key (and any letter with an AltGr alternative), smear down off the key without swiping - the popup
  still opens.

- **§402 (v1.1.41): D-436 - the actual per-language diacritics data mechanism D-387 asked for, built on top**
  **of §401's architecture fix.** New optional sixth language-pack file `diacritics.tsv` (base letter ->
  known variants, e.g. `e<TAB>é,è,ê,ë` for French, `g<TAB>ğ` for Turkish), parsed by a new pure `DiacriticTable`
  and resolved through the identical bundled-asset-or-installed-pack pattern every other optional file
  already uses (new `LanguageDiacriticsLoader`, mirroring `LanguageAbbreviationsLoader`/
  `LanguageLetterHintsLoader` exactly). A new `DataDiacriticFolding` implements `DiacriticFolding` generically
  from that table - `Umlaut` itself stays untouched, still the compiled special case for German alone
  (`SettingsStore.loadDiacriticFolding()`'s own precedence: a hardcoded implementation, currently only
  `Umlaut`, always wins over a data table; a language with neither gets `NoOpDiacriticFolding`).

  Closes out the two follow-up design points from §401's own discussion, both confirmed exactly as the user
  argued: (1) `unfoldCandidates`/`fold` only ever need the base-letter -> variants direction, generalising
  cleanly to a base letter with *several* real variants (French's `e`) via the same per-position combinatorial
  branching `Umlaut`'s own private `unfold` already used for German's 1:1 case - no per-language algorithm
  differences needed, confirmed by `DataDiacriticFoldingTest`'s own French-shaped table; (2) the L-05 AltGr
  "host key" is always singular by construction, so `foldVariants` is single-valued for every
  `DataDiacriticFolding` instance - German's `ß` dual ASCII convention (D-204) stays a genuine, `Umlaut`-only
  special case, not a shape the generic mechanism needs to support.

  New tests: `DiacriticTableTest` (parser tolerance/validation), `DataDiacriticFoldingTest` (fold/unfold/
  foldVariants/variantsOf against synthetic Turkish- and French-shaped tables, including the multi-variant and
  capped-combinatorics cases), plus `LanguagePackInstallerTest` coverage for the new archive entry
  (write/clear). 1273 -> 1300 unit tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `AdaptKey-Language-Contribution-Guide.md` §3 gained `diacritics.tsv` as the sixth optional file, and §8's own
  step 5 now covers both `hints.tsv` and `diacritics.tsv` as one research pass (the same base-letter ->
  variants list answers both: `diacritics.tsv` wants the complete set, `hints.tsv` only its single most-used
  representative). `versionCode` 457 -> 458, `versionName` `"1.1.40"` -> `"1.1.41"`.

  No language beyond German has real `diacritics.tsv` content yet - that is now simply the same, expected,
  pick-it-up-later state every other optional per-language file is already in for every non-German/English
  language, not a gap specific to this mechanism.

- **§401 (v1.1.40): D-435 - the umlaut/diacritic unfold mechanism (`Umlaut`, D-144/D-204) was called**
  **unconditionally from `DictionarySuggestionProvider`/`TokenRepair`, regardless of the active language - a
  genuine bug (the user's own framing, discussed while working through D-387), not merely a feature missing
  for other languages.** Found by design-reviewing D-387 ("extend the umlaut mechanism to other languages"):
  `Umlaut.fold`/`unfoldCandidates`/`foldVariants` were called directly as a global object at ~10 call sites
  across both classes, never routed through the per-language `languageRules: LanguageRules` seam (D-410)
  both classes already receive - so a French/Turkish/Polish active store got exactly German's umlaut handling
  bolted on with no way to ever get its own, or none at all.

  Fixed with a new `DiacriticFolding` interface (`fold`/`unfoldCandidates`/`foldVariants`/`variantsOf`,
  mirroring `LanguageRules`' own D-410 shape) and `NoOpDiacriticFolding` as the default - explicitly **not**
  `Umlaut`, per the user's own explicit call: "es bringt Türkisch überhaupt nichts, wenn der Default auf
  deutsche Umlaute geht." `Umlaut` now implements the interface directly (unchanged behaviour, including its
  D-204 ß dual-convention special case, left entirely alone - not worth the regression risk on a historically
  fragile mechanism for a round that isn't even about German). New `DiacriticFoldingRegistry`
  (`language` package, mirrors `LanguageRulesRegistry` exactly) maps only `GERMAN -> Umlaut`; every other
  language resolves to `NoOpDiacriticFolding`, wired into all four real per-language construction sites in
  `AdaptKeyService.kt` (`installStores()`, the aggressiveness-change provider rebuild, the English-bootstrap
  `tokenRepair`, and `selectActiveDictionary()`'s own per-token repoint - the same four spots
  `LanguageRulesRegistry.rulesFor()` is already threaded through). `candidateFirstChars()`'s own hardcoded
  `'a'/'o'/'u' -> 'ä'/'ö'/'ü'` `when` block is now `diacriticFolding.variantsOf(first)` - the same bug, a
  different call site than the `Umlaut.*` grep would have found on its own.

  Since the default flipped from "always German" to "no-op", every existing German-context test/caller that
  relied on the old unconditional behaviour needed `diacriticFolding = Umlaut` added explicitly:
  `DictionarySuggestionProviderTest`'s and `TokenRepairTest`'s own primary `provider`/`repair` instances,
  `SqliteDictionaryStoreRoboTest`'s three (one of which, `missingUmlautCorrectsToTheUmlautWordNotAShapeAlike`,
  turned out to have been passing only by *accident* - via a same-edit-distance-1 frequency tie-break, not the
  intended fold-distance-0 exact match its own comment describes - restoring the real mechanism here rather
  than leaving a silently-fragile pass in place), and `BundledDictionaryDataTest`'s real-dictionary split test.
  New tests: `UmlautTest` (`variantsOf`, interface conformance), `NoOpDiacriticFoldingTest`,
  `DiacriticFoldingRegistryTest`, plus a `D-435` regression test in each of
  `DictionarySuggestionProviderTest`/`TokenRepairTest` proving a `NoOpDiacriticFolding`-configured instance
  does *not* restore German umlauts (the exact shape of bug this closes). 1262 -> 1273 unit tests, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's §1 guiding principle gained a D-435 addendum.
  `versionCode` 456 -> 457, `versionName` `"1.1.39"` -> `"1.1.40"`.

  **Deliberately stopped here, not merged with the actual D-387 language extension**: this round is the
  architecture fix only - no language beyond German has a real `DiacriticFolding` implementation yet, so
  behaviour for every other language is unchanged (still no diacritic handling, now honestly so instead of
  accidentally-German). The user's own explicit plan: fix the bug first, then design the actual per-language
  data mechanism next. Two design points already raised in that follow-up discussion, worth recording before
  they're lost: (1) the *direction* that matters for `unfoldCandidates` is always base-letter -> known-variants
  (never the reverse), so a language needing several diacritic variants per base letter (French `e` -> `é è ê
  ë`) generalises the existing per-position combinatorial branching cleanly - no per-language algorithm
  differences needed, only a richer `Map<Char, List<Char>>` than German's own 1:1 `VOWEL_UNFOLD`; (2) the L-05
  AltGr "host key" concept - exactly one physical key hosts a given diacritic - generalises as the universal
  substitution rule for essentially every target language's diacritics (dropping the diacritic mark already
  equals the host-key letter for e.g. French/Spanish/Turkish/Polish), so D-204's dual ASCII-convention
  complexity (ß's separate formal-"ss"-vs-host-key-"s" split) is a genuine German (and structurally, Nordic
  æ/ø/å) special case, not a shape every language's `DiacriticFolding` needs to support - `foldVariants`
  likely stays single-valued for every new implementation. Neither point is implemented yet; see whatever
  later round actually builds the `diacritics.tsv` mechanism for the outcome.

- **§400 (v1.1.39): D-434 - `Abbreviations.isNonTerminalPeriod`/`isAbbreviation` always checked the token**
  **against the German list regardless of the active language - a genuine cross-language leak, found while
  auditing the language-pack pipeline for the Contribution Guide work below, not from a device report.** Every
  non-German field's §6 sentence-boundary detection silently ran against German abbreviations (harmless
  overlap for a shared Latin abbreviation like `etc.`, actively wrong for a German-specific one like
  `bzgl.`/`zzgl.` inside non-German text). Fixed by making the abbreviation set a parameter throughout
  (`Abbreviations`/`SentenceBoundary`, both default to `GERMAN` so every existing caller/test keeps its old
  behaviour unchanged) and adding a full per-language data-file mechanism mirroring D-281's `hints.tsv`
  exactly: a new optional `abbreviations.tsv` in a language pack (`LanguagePackInstaller`/`LanguagePackStorage`,
  D-310 fixed-filename convention), a bundled-asset-or-installed-pack loader
  (`LanguageAbbreviationsLoader`, mirrors `LanguageLetterHintsLoader`), threaded through `AdaptSettings`/
  `SettingsMapper`/`SettingsStore` exactly like `letterHints` already is, including the same "reload on a
  plain G-01 language swipe, not just on a full settings reload" fix `applyActiveLanguageToView()` already had
  to do for `letterHints` (`toggleLanguage()` never calls `applySettings()`). A language with no
  `abbreviations.tsv` of its own falls back to the compiled-in `Abbreviations.GERMAN` list - the same
  "functional but not tailored" fallback role `KeyboardLayout.DEFAULT_LETTER_HINTS` already plays for L-05, not
  a regression from today's German-only behaviour. No bundled/installed `abbreviations.tsv` files were added
  for English/German themselves this round - the fallback already reproduces current behaviour for both, and
  a real per-language content decision was deliberately left out of scope here (see the Contribution Guide's
  own new §8 below for how a future language's own list should actually be drafted). New/updated tests:
  `AbbreviationsTest`/`SentenceBoundaryTest` (the parameterised behaviour, default-preserves-old-behaviour, and
  `parse()`), `LanguagePackInstallerTest` (the new optional archive entry, write + clear),
  `SettingsMapperTest` (empty-set-falls-back-to-GERMAN, a custom set is preserved). 1249 -> 1262 unit tests,
  all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's §6 "No Sentence Start After Known
  Abbreviations and Enumerators" gained a D-434 addendum documenting the new per-language mechanism and
  fallback. `versionCode` 455 -> 456, `versionName` `"1.1.38"` -> `"1.1.39"`.

  **Same round, doc-only:** `AdaptKey-Language-Contribution-Guide.md` gained a new §8, "A 'pretty good'
  one-shot pipeline (LLM-assisted, not a substitute for a native speaker)" - written after a design discussion
  on whether more bundled languages are needed before wider (F-Droid) release (assessed: no, DE/EN/EL is an
  honest starting point, F-Droid itself does not gate on language coverage; Spanish/French would be the
  highest-value additions if the project ever wants to actively pursue reach). The user's own follow-up ask:
  fold the LLM-assisted content-generation steps discussed there directly into the Guide as a repeatable
  pipeline, explicitly scoped to "pretty good", never claimed equal to German's own native-speaker-reviewed
  quality bar. Twelve steps, in dependency order: base corpus extraction (flagged as the one genuine
  prerequisite with no script yet in this repo), frequency-scale calibration against the app's own tuned
  constants, LLM-based noise removal (batched, replacing §301's manual per-band review), LLM-based POS tagging
  including homograph disambiguation (direct "what part of speech" prompting instead of D-368's own
  error-prone `+n`/`+en` spelling heuristic - arguably a real improvement on method, not just a faster
  imitation), Wortfamilien/lemma completion via Wiktionary (pointing at the real, checked-in
  `dictionaries/de/extract_wiktionary_*.py`/`dictionaries/el/extract_wiktionary.py` scripts as reference
  implementations now that the design-plan docs that originally described the method have been deleted as
  superseded), the AltGr hint set (§3, this round's own D-434 file as a worked example of the mechanism), the
  new abbreviations.tsv itself, the confusables/keyboard-adjacency scan (D-304/D-330-followup's method
  generalised), an explicit capitalisation-rule-applicability decision (§6 rules 3/4 are a German convention,
  not universal), `LanguageRules` naive-fill scoping (the user's own late addition to this list mid-session -
  three of its nine hooks are plain factual questions an LLM can answer directly and safely
  (`decimalCommaGluesDigits`/`timeSuggestionWord`/`bundledConfusablesBlacklist`, the last reusing the
  confusables-scan step's own output), the other six encode real per-language grammar algorithms
  (A-05's split/compound vetoes, regular-inflection recognition) that stay genuine manual grammar-engineering
  work - most contributed languages will legitimately keep `NoOpLanguageRules` for these six, a documented
  accepted state, not a defect; also noted that unlike every data file in §3, `LanguageRules` still needs an
  actual Kotlin class and PR even for the three naively-fillable answers, no data-file mechanism exists for it
  today), trigram language-profile data (closing the existing "known gap, not a design choice" from §6 in the
  same pass), and a mandatory closing gate - a real speaker's random-sample spot check across frequency bands
  before a pipeline-generated pack may be called "pretty good" and published, since a clean pipeline run is not
  the same thing as a correct one and an LLM pipeline has no ground truth of its own to check itself against.
  One correction folded in from the user's own pushback on an earlier, too-pessimistic draft of the AltGr
  point specifically: a Latin-script language whose special characters are diacritic variants of an existing
  letter (Turkish `ğ ş ı`, Polish `ł ż ń ć ś`, ...) maps onto that base letter's own popup unambiguously and
  can be LLM-drafted reliably - only the symbol/punctuation choices on top of that remain a real judgement call
  needing human/native confirmation, not the whole file. No code change, no version bump for this half (this
  project's own "pure doc update" convention) - the version bump above is entirely for the D-434 fix.

- **§399 (v1.1.38): D-361-followup (v6) - the real cause of "oben wirkt gar nicht", found from the §398**
  **diagnostic log the very first time it was captured.** User's own repro nailed it precisely: rapid
  Backspace tapping while the finger drifts upward types `LLLLLLKKKKK` instead of deleting, the moment the
  drift reaches L/K "und sei es nur die untere Kante" (even just the bottom edge). The log confirmed this
  exactly, and revealed the actual mechanism: several sticky-check log lines showed `underKey=null` - the raw
  tap landed in the gap *between* rows, matching no key's rect at all - immediately followed by `rawTap: key=
  c:l`/`key=c:k`, i.e. `resolveKey()`'s personal offset model (T-03) resolving that same gap tap to whichever
  *learned* key-zone was nearest, entirely bypassing the sticky check. Root cause: `isWithinBackspaceStickyZone()`
  required the raw tap to land literally inside the neighbour's own rect (`neighbor.contains(x, y)`) before
  considering it at all - a tap in the inter-row gap itself (extremely common during fast, sloppy repeated
  tapping, per the log) matched nothing, fell through past the sticky check entirely, and got claimed by the
  offset model instead. This explains why the log also showed some genuine `hit=true` cases for `l`/`k` (a tap
  that happened to land inside the neighbour's own rect) interspersed with the failures (a tap landing in the
  gap) - not a directional bug at all, a coverage gap.

  Rewritten to build an explicit sticky-zone rect per direction - the neighbour's own near portion *plus* the
  entire gap through to Backspace's own edge - and test that directly, so a gap tap is unambiguously claimed
  before the offset model ever runs. The §398 diagnostic logging (its purpose served) removed again in the
  same commit.

  No new tests (same `AdaptKeyboardView` touch-resolution boundary). 1249 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change. `versionCode` 454 -> 455,
  `versionName` `"1.1.37"` -> `"1.1.38"`. **2026-09-04: device-confirmed** - the exact repro (fast Backspace,
  finger drifting onto L/K) no longer types letters. D-361 itself (§391-§399, the sticky-Backspace-zone
  feature as a whole) is now closed end to end.

- **§398 (v1.1.37): D-361-followup - "oben" still does not stick after v5, temporary diagnostic logging**
  **added instead of a third blind patch.** Reported right after v5 (which fixed "unten"/Enter): left/right
  now work well, but upward still has no effect at all. Re-derived the geometry by hand rather than guessing
  again: `layoutKeys()` places every row's own last key flush against the identical right edge
  (`left + usableWidth - gapPx`, a row-independent constant, verified algebraically from the actual loop) -
  row 2 (`"asdfghjkl"`, 9 equal-weight keys, no L-04 backspace-driven squeeze applied to it) and row 3
  (Shift + `thirdRowLetters` + Backspace) should therefore both end at the exact same x, meaning `l` (row 2's
  last key) sits directly above Backspace exactly the way Enter sits directly below it - the v5 tolerance fix
  (which is direction-agnostic, applied identically to all four branches) should cover this boundary at least
  as easily as the Enter one, since there is no extra D-55 gap between row 2 and row 3 at all. No discrepancy
  found by re-reading the code alone this time - this project's own established pattern for exactly this
  situation (spec §1's guiding principle; D-386's own precedent) applies: temporary diagnostic logging
  (`AdaptKeyTouch` tag, `logTouch` - reachable via `adb logcat -s AdaptKeyTouch:D` or Settings -> Diagnostics)
  now records, on every sticky-check while the window is active, the computed hit/miss, the tap coordinates,
  Backspace's own rect, and the actual key/rect the raw tap physically landed in - enough to compare the two
  rects by hand once a real "oben" repro is captured, rather than coding blind against a theory that keeps not
  matching what the device reports.

  No new tests (diagnostic logging only, no behaviour change). 1249 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change. `versionCode` 453 -> 454,
  `versionName` `"1.1.36"` -> `"1.1.37"`.

- **§397 (v1.1.36): D-361-followup (v5) - the sticky Backspace zone never actually bled toward Enter, the**
  **one neighbour it exists for.** First real device confirmation of D-361 itself: "klebt mir noch nicht weit
  genug... nach oben und unten strahlt sie gar nicht." Root-caused, not guessed - `layoutKeys()` adds the D-55
  `extraSpaceAboveSpaceRowDp` gap (7dp default, up to 25dp) *on top of* the ordinary `gapPx` row spacing
  specifically above the space/Enter row, and Enter sits directly across exactly that widened gap from
  Backspace. `isWithinBackspaceStickyZone()`'s own adjacency tolerance (`gapPx * 1.5f`, ~7.5dp-equivalent)
  never covered this - it only ever matched the plain inter-row/inter-key gap, so the vertical branches
  (toward Enter, and symmetrically upward) silently never fired, while the horizontal one (toward `m`, no
  D-55 gap in the way) worked as designed - exactly the asymmetry reported. Fixed by adding both D-55 values
  (`extraSpaceAboveSpaceRowDp` + `extraSpaceBelowNumberRowDp`, read live from the view's own already-applied
  settings, defensively both rather than assuming which boundary a given neighbour sits across) to the
  tolerance. Separately, `BACKSPACE_STICKY_ZONE_FRACTION` widened 0.35 -> 0.45, the same report's own "nicht
  weit genug" headline complaint about the (already-working) horizontal reach.

  No new tests (same `AdaptKeyboardView` touch-resolution/Android-view-glue boundary the whole D-361 mechanism
  already sits on). 1249 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. No spec change - the spec's own D-361/L-04 text never claimed a specific tolerance value, so nothing
  there was actually wrong, only the implementation. `versionCode` 452 -> 453, `versionName` `"1.1.35"` ->
  `"1.1.36"`. **Not yet device-confirmed** - needs the same real typing test repeated: does Backspace now
  reliably absorb a mistimed re-tap that lands on Enter, not just on `m`.

- **§396 (v1.1.35): D-361-followup (v4) - the language-related flag icons now resolve from the device's own**
  **system locale, instead of a hardcoded 🇩🇪.** User's own reasoning, weighing three options directly (EU
  flag / a genuinely generic non-national flag / system-locale-derived) before picking the third explicitly:
  "Ich bin für (3.) wenn das möglich ist." Also required, in the same message: the automatic-language-switch
  icon's own "Ausgangsflagge" (starting flag, §395's `d398_sustained_language_switch_threshold` icon) must
  track the identical value whenever the language-packs flag changes, not drift out of sync.

  New `SystemFlag` object (pure, JVM-testable - takes a `Locale`, not a `Context`): a country code's two
  letters each map algorithmically onto Unicode's Regional Indicator Symbol block (`A -> U+1F1E6`, ...,
  `Z -> U+1F1FF`; concatenating the pair for e.g. `DE` yields 🇩🇪 directly) - no bundled lookup table for the
  ~250 possible codes, generalises automatically to any locale's country, including ones this app has no
  language pack for at all. Falls back to the former hardcoded 🇩🇪 when the locale carries no usable two-letter
  country (a bare language-only locale, or a malformed/non-letter code - both checked explicitly, not just the
  length). `SettingsFragment.onCreatePreferences()` now resolves `SystemFlag.glyph(resources.configuration.
  locales.get(0))` once and feeds the *same* value into both the `d280_language_packs` icon and as the leading
  flag of `d398_sustained_language_switch_threshold`'s two-flag icon (the trailing flag, English/🇬🇧, stays
  fixed - G-01's own "always available" language) - satisfies the sync requirement structurally: there is only
  ever one place either icon's starting flag is computed.

  Reads `resources.configuration.locales.get(0)`, not `Locale.getDefault()` - the same source D-92's own
  `AdaptKeyService.applySettings()` `systemLocale` field already reads, deliberately not the JVM default
  (which a per-app language override, Android 13+, can decouple from the device's actual system locale).

  5 new tests (`SystemFlagTest`): real two-letter codes resolve correctly (`DE`/`GB`/`US`), lower-case input
  resolves identically to upper-case, a language-only locale (no country) falls back, an empty locale falls
  back, and three distinct malformed-country shapes (too short, too long/UN M49, right length but not letters)
  all fall back rather than resolving nonsense. 1244 -> 1249 unit tests, all green. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. No spec change - settings-screen cosmetics. `versionCode` 451 -> 452,
  `versionName` `"1.1.34"` -> `"1.1.35"`. **Not yet device-confirmed** - needs a real look, on a device whose
  system language is not German, that the flag genuinely follows it (and that both language-related icons stay
  in sync with each other).

- **§395 (v1.1.34): D-361-followup (v3) - category header icons, plus three glyphs swapped for genuine**
  **colour emoji per direct user request.** Two independent extensions on top of §393/§394, confirmed
  "Schon besser" first:
  1. **Every `PreferenceCategory` now has its own icon too** - "Kategorien sollen bitte auch Icons haben."
     `PreferenceCategory` is a `Preference` subclass with no `android:key` in `settings_preferences.xml`
     originally (nothing to find it by); each of the nine gained one (`cat_info_group`,
     `cat_calibration_group`, ..., purely a lookup handle, never persisted - categories hold no value of
     their own). Confirmed directly in the real `androidx.preference` AAR before relying on it:
     `preference_category_material.xml` already `<include>`s the identical `image_frame.xml` icon column
     every ordinary preference uses, so the existing `GlyphIconDrawable` mechanism applies unchanged, no new
     layout work needed. Assignments: Info ℹ, Kalibrierung ◎, Wörterbuch 📚, Korrektur & Vorschläge ✓,
     Tasten-Verhalten ⌨, Layout ▦, Tasten-Rückmeldung ♪, Sicherung ☁, Diagnose ☰.
  2. **Three icons swapped for real colour emoji**, deliberately breaking the otherwise-monochrome set - the
     user's own explicit, named requests, not a reinterpretation: Language packs' 文 ("sieht aus wie ein
     Strichmännchen") -> 🇩🇪 (a single flag, arbitrarily chosen among the app's supported languages - easy to
     swap for a different one on request); Learned Words' ✎/✒ -> 🎓 ("dieser übliche Akademiker-Hut");
     and a **new** icon for `d398_sustained_language_switch_threshold` (the automatic-language-switch
     threshold, C-23) - 🇩🇪🇬🇧, two flag sequences drawn as one string ("könnte eine Ligatur aus zwei Flaggen
     sein oder so" - Unicode has no true single-glyph two-flag ligature, so this is literally two adjacent
     flags, not one fused glyph). This last one is a genuine scope extension beyond the original "only
     sub-screen entries" agreement from §392/§393 - flagged here rather than silently expanded, since the
     user directed it by naming the specific setting rather than restating the scope rule.

  **`GlyphIconDrawable` gained a real correctness fix while implementing the two-flag icon**, not just a
  cosmetic one: `Canvas.drawText` does not clip to the drawable's own bounds, so a glyph string wide enough
  to exceed the intended box (the two-flag case, but potentially any single unusually-wide character too)
  would have drawn past its own square into whatever sits next to the icon column. Fixed with a shrink-to-fit
  step - if the measured text width exceeds 95% of the drawable's own size, `paint.textSize` is scaled down
  by the overflow ratio and bounds re-measured - applied uniformly, not only to the multi-flag case.

  No new tests (same `GlyphIconDrawable`/Android-rendering-glue boundary as §393/§394). 1244 unit tests
  unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change. `versionCode`
  450 -> 451, `versionName` `"1.1.33"` -> `"1.1.34"`. **Not yet device-confirmed** - cannot be visually
  verified from here; needs a real look that the category icons actually render (the AAR inspection confirms
  the layout supports it, but binding behavour was never seen on a real device) and that the emoji glyphs
  read as intended rather than a "tofu" box or an unexpected fallback rendering.

- **§394 (v1.1.33): D-361-followup (v2) - §393's glyph icons read as thin, faint line-art on a real device,**
  **not as proper icons.** Direct device feedback right after §393 shipped: "die sind aber klein und
  minimalistisch... weniger nackte Linien und mehr Icons." Root cause of the "line-art" look: the original
  eight characters were mostly drawn from the Mathematical Operators/Supplemental Arrows/Miscellaneous
  Technical blocks (⌖/⊘/⟲), which are thin-stroke notation glyphs by design, not solid pictographs. Fixed two
  independent ways at once:
  - `GlyphIconDrawable`'s own size widened 24dp -> 28dp and its `textSize` fraction 0.75 -> 0.9 (still well
    under the icon-frame's real 48dp cap, confirmed again against the actual AndroidX layout).
  - Four of the eight characters replaced with bolder, more solidly-shaped ones from the Miscellaneous
    Symbols/Dingbats blocks: Calibration ⌖ -> ◎ (bullseye), Blacklist ⊘ -> ✗ (ballot X), Learned Words ✎ -> ✒
    (black nib), Backup ⟲ -> ☁ (cloud). Language packs (文), Credentials (⚿), Diagnostics log (☰) and Feature
    overview (ℹ) were already reasonably solid/bold shapes and kept unchanged.

  No new tests (same `GlyphIconDrawable`/Android-rendering-glue boundary as §393). 1244 unit tests unchanged,
  all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change. `versionCode` 449 -> 450,
  `versionName` `"1.1.32"` -> `"1.1.33"`. **Not yet device-confirmed** - still cannot be visually verified from
  here; needs the same real-device look §393 itself never got before this round already replaced it.

- **§393 (v1.1.32): D-361-followup - monochrome glyph icons for the settings screen's own sub-screen entry**
  **points.** Direct follow-up to §392's icon discussion: user corrected the assumed cost ("Icon-Glyph, nicht
  Icon-Image" - a single Unicode character needs no bundled asset, no licence to source/credit, unlike a real
  icon set would) and confirmed scope (only preferences that open their own dedicated screen; a group/category
  icon was floated but not actually requested this round - the confirmed candidate list was entry points only)
  and style (plain monochrome symbol characters, not colourful emoji, matching ordinary Android Settings icon
  conventions rather than this app's own extra-row emoji-button precedent).

  Also directly addressed, separately, before starting: **the double-tap-delay rename from §392 does not drop
  any functionality** - the user suspected a real, distinct "Doppel-Shift-Verzögerung" mechanism might have
  been silently merged away. Re-verified line by line: `settings.doubleTapDelayMs` was already the single
  value G-05 ([AdaptKeyService.kt:6683](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:6683)),
  D-348 ([AdaptKeyService.kt:2631](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:2631)) and
  now D-361 all read directly - no second field exists anywhere in `AdaptSettings`/`SettingsMapper`/
  `SettingsStore` (grepped explicitly to be sure). D-348's own pre-existing KDoc already said "Reuses the G-05
  double-tap delay setting" *before* this session touched anything - the sharing was an established, deliberate
  design decision from earlier work, not something introduced or discovered incorrectly this round. Only the
  UI label and a few stale KDoc comments (written back when the value was G-05-only) called it Shift-specific;
  those comments corrected too (`AdaptSettings.DEFAULT_DOUBLE_TAP_DELAY_MS`, `SettingsMapper.MIN/
  MAX_DOUBLE_TAP_DELAY_MS`, `SettingsStore.DEF_DOUBLE_TAP_DELAY`) - comment-only, no version bump for that
  piece (committed separately, `0dc4f97`).

  **Mechanism**: new `GlyphIconDrawable` (`Drawable` subclass) draws one Unicode character centred in a 24dp
  square, tinted to `?android:attr/textColorSecondary` to match the muted look of an ordinary Android Settings
  icon - confirmed via the real `androidx.preference` AAR's own `image_frame.xml` (`PreferenceImageView`,
  `maxWidth`/`maxHeight="48dp"`, `wrap_content`) that a 24dp intrinsic size renders at its own natural size, no
  forced up/downscaling. `SettingsFragment.onCreatePreferences()` assigns one to each of the eight sub-screen
  entry points via a straightforward `key -> glyph` map, right after `setPreferencesFromResource()`: Calibration
  ⌖, Language packs 文, Blacklist ⊘, Learned Words ✎, Credentials ⚿, Backup ⟲, Diagnostics log ☰, Feature
  overview ℹ. `onboarding_replay` deliberately excluded - confirmed via `SettingsActivity.kt` it only shows a
  `Toast` (`setOnPreferenceClickListener`), never opens its own screen, so it does not qualify under the user's
  own "own submenu" scoping.

  No new tests - `GlyphIconDrawable` is pure Android rendering glue (`Canvas`/`Paint`), the same untested
  boundary `isWithinSpaceHitZone`/the offset-factor functions already sit on; `SettingsFragment`'s own wiring is
  equally untested Android view glue, per this project's own convention for the whole settings screen. 1244
  unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change - pure
  settings-screen cosmetics, same precedent §380/§381 already established for this class of change. `versionCode`
  448 -> 449, `versionName` `"1.1.31"` -> `"1.1.32"`. **Not yet device-confirmed** - glyph rendering support
  varies by device/OEM font substitution in a way that cannot be verified from here; needs a real look that
  every chosen character actually renders as intended (not a "tofu" missing-glyph box) and that the icon column
  looks right alongside the rest of the row.

- **§392 (v1.1.31): settings-screen follow-up from testing §391 - a stale-APK false alarm led to a real,**
  **useful settings-screen review round.** User reported seeing no Sticky-Backspace effect, no new setting, and
  the double-tap-delay slider itself missing entirely after installing what turned out to still be v1.1.29, not
  the freshly built v1.1.30 - a install/deployment mix-up, not a code bug (confirmed: `settings_preferences.xml`
  was already syntactically valid and correctly ordered before this round; a fresh rebuild's `AdaptKey.apk`
  matched the latest commit exactly). That prompted a real settings-screen review, five distinct, unrelated
  points:

  1. **A genuine layout bug, found and root-caused, not just re-confirmed:** the first two rows under
     "Korrektur & Vorschläge" (`d353_autocorrect_aggressiveness`/`d352_auto_split_mode`, both
     `LabeledSeekBarPreference`) sit less indented than every ordinary sibling preference. Root cause:
     `preference_labeled_seekbar.xml` (`LabeledSeekBarPreference`'s own layout, D-407/D-408) still carried the
     *original*, never-actually-fixed deviation from the real AndroidX default (`marginStart=16dp`/`marginEnd=
     8dp` instead of the real `15dip`/`6dip`, `textAppearanceMedium` with no explicit colour instead of
     `textAppearanceLarge` + `textColorPrimary`, a plain `ImageView` icon frame instead of the real
     `FrameLayout` structure) - the *exact* deviations §382 (v1.1.21) found and fixed, but only ever in
     `preference_tier3_model.xml`, a file that had merely been hand-copied *from* this one; this file itself,
     the actual original template, was never corrected. Fixed the same way §383/§384's own final, real-device-
     confirmed approach settled on (not by re-tweaking individual attributes again): `<include layout=
     "@layout/preference_material" />` (not `@layout/preference` - this app's theme maps `preferenceStyle` to
     `Preference.Material`, confirmed the hard way in that earlier round) for the icon/title/summary column
     verbatim, with only the SeekBar+value row appended below it, using the same `listPreferredItemPaddingStart`/
     `End` + 56dp leading `Space` pattern `preference_tier3_model.xml` already established. Both
     `LabeledSeekBarPreference` rows in this screen (and any future one) inherit the fix automatically - no
     Kotlin change needed, `onBindViewHolder` only ever looked up its own `R.id.labeled_seekbar`/
     `R.id.labeled_seekbar_value` ids, confirmed before touching the layout.
  2. **The double-tap-delay setting's own name was genuinely misleading, not merely a display issue**: labelled
     "Doppeltipp-**Shift**-Verzögerung" even though it was already shared by G-05 (Shift), D-348 (double-tap
     Backspace undo) and now D-361 (sticky Backspace) - user's own diagnosis, reported directly (not something
     this session had flagged). Renamed to the generic "Doppeltipp-Verzögerung"/"Double-tap delay" (all three
     languages), summary rewritten to name all three consumers instead of only Shift. Spec's G-05 section and
     L-04's D-361 addendum both corrected to match (no longer call it Shift-specific or claim it lives in
     "Layout").
  3. **Two truncated/over-long titles shortened**, per direct device screenshots: `d361_backspace_sticky_title`
     ("Rücktaste beim schnellen Tippen klebend" → "Backspace klebend" / "Sticky Backspace while typing fast" →
     "Sticky Backspace"); `d348_double_tap_backspace_undo_title` (German only, the one actually reported
     truncated: "Doppel-Tipp Backspace zum Zurücksetzen" → "Doppel Backspace zum Zurücksetzen").
  4. **A new settings category, split out of the renamed "Layout" (was "Layout & Tasten"/"Layout & Keys"),**
     per the user's own explicit request: **"Tasten-Verhalten"/"Key Behaviour"**, sitting directly above
     "Layout", now holding `d32_longpress_delay_ms`, the renamed `double_tap_delay_ms`, and `d361_backspace_
     sticky` (all three moved out of "Layout", which keeps everything else - number row, symbol key, the two
     D-55 spacing sliders, the C-01 weight/backspace/shift sliders). `SettingsStore.EXPORT_SETTINGS_KEY_ORDER`
     reordered to match, per that list's own documented "mirrors the settings screen's own order" contract.
  5. **The left indentation the fix in point 1 relies on is the AndroidX-reserved icon-frame column** (56dp,
     confirmed in the §385 investigation) - currently unused by every preference in this app (no `android:icon`
     set anywhere). User asked directly whether adding glyph icons there would be worth doing. Answered as a
     discussion, not implemented: a real polish idea, but a genuinely new investment (no icon asset pipeline
     exists in this app yet; F-Droid's own licensing discipline, per the release-channel work, means any
     sourced icon set needs its licence checked and credited, same as `CREDITS.md` already does for every other
     bundled asset) with a real scoping question first - individual numeric-tuning sliders mostly have no
     natural pictographic meaning, so icons would likely only make sense on the handful of sub-screen-opening
     entry points (language packs, blacklist, learned words, credentials, backup, diagnostics), not on every
     row. Left for the user's own call whether to pursue as its own future round - not tracked as a firm open
     TODO yet, since no decision was actually made either way.

  No new tests (pure settings-screen resource/layout/naming changes, Android view glue - the same untested
  boundary every other settings-screen-only round in this project already sits on). 1244 unit tests unchanged,
  all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 447 -> 448, `versionName`
  `"1.1.30"` -> `"1.1.31"`. **Not yet device-confirmed** - this is the actual fresh build the user needs to
  install to re-test §391/D-361 in the first place; also needs a real look at whether the
  `LabeledSeekBarPreference` rows now align with their siblings.

## Older Rounds (§1-§391, v0.7.6 through v1.1.30) - Pruned From This File

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
those 366 rounds eventually produced. A round below §367 that is still unconfirmed on-device and not
otherwise tracked in those sections should be treated as abandoned/superseded, not as silently-still-pending
work - if genuinely in doubt about one specific item, check its own `§N` entry in History.md rather than
assuming either way.

Retention policy for this section going forward: keep the ~20-25 most recent rounds (enough for a fresh
session's own continuity) plus anything not yet device-confirmed; prune older, already-confirmed rounds the
same way, without summarising them - they stay permanently retrievable in History.md. **2026-09-04 fifth
pruning pass (§375-§380 removed, cutoff moved from §375 to §381):** History.md already covers this range
(backfilled through §388, confirmed in the previous pass), so nothing needed backfilling first. Had grown to
30 kept rounds after this session's own new §401-§404; none of §375-§380 carried an inline "not yet
device-confirmed" caveat, and their own device confirmation is separately attested in git history
(`a89753d Doku: §376-§385 geräte-bestätigt`, `4324134 Doku: D-428 gerätebestätigt` for §375) - a
straightforward prune, no exemption question to weigh this time. **2026-09-04 fourth pruning pass (§367-§374
removed, cutoff moved from §367 to §375):** History.md was already backfilled through
§388 (checked directly, not assumed), so nothing needed backfilling first this time. §367-§374 had grown to
34 kept rounds, past the ~20-25 target even before this pass; several of the removed rounds (§367/§368/§370/
§371/§372, all dictionary-content/Wortfamilien-parity work) still carried an inline "not yet device-confirmed"
caveat, but their own `D-`numbers (D-377, D-422, D-424) are independently tracked as RESOLVED in "Open TODOs"
above - the same "the tag is not a standing exemption this far back" precedent the two passes below already
established, applied here to a *content-quality* caveat (verify Greek/English suggestions render correctly)
rather than a UI-behaviour one, but the same reasoning: an item already closed out elsewhere does not need its
own superseded round kept alive here just because its inline text was never revisited. **2026-09-03 third
pruning pass (§329-§366 removed, cutoff moved from §329 to §367):** History.md had fallen behind by 38
rounds (only current through §350, while this file had already accumulated up to §388) since the previous
pass never got a follow-up - §351-§388 backfilled into History.md first (verbatim, reformatted from this
file's own bullet-list shape into History.md's heading+paragraph style, no content summarised or dropped),
*then* this pass removed §329-§366 from this file, exactly the same "not a standing exemption" precedent the
2026-09-01 pass already established below applies again here (several of the removed rounds were still
tagged "not yet device-confirmed"). **2026-09-01 second pruning pass (§260-§328 removed, cutoff moved from
§260 to §329):** in practice, "not yet device-confirmed"
does not carve out an exemption this far back - the still-remaining §260/§261 entries removed in this pass
were themselves still tagged that way (from months-old rounds nobody went back to re-tag once superseded),
and the very first pruning pass already cut off §259 and older despite the same tag being just as common
there. Treat the tag as load-bearing only within the current ~20-25-round working set, not as a standing
retention guarantee for anything older - honouring it literally would have blocked pruning almost entirely,
since the project has no convention of retroactively clearing it once a round is effectively superseded.

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
