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

- **Keyboard-reachability-weighted typing cost, baked into the *primary* correction-cost computation itself**
  (the user's own idea, discussed while designing D-356's tie-break fix). The thought: a character only
  reachable via long-press/AltGr on the active layout is real, independent evidence of deliberate intent -
  more so than a plain primary-key tap - so it could carry a real cost surcharge (the user's own suggestion:
  "vielleicht äquivalent zu vier einfachen Buchstaben") inside [CorrectionConfidence]'s own cost model, not
  merely as a tie-break. Confirmed technically real in principle, but explicitly **not** pursued: it would
  need a wholly new, currently-nonexistent per-layout/per-language "character reachability" cost table
  (derived from `letterHints`/each layout's own long-press alternatives), and - more importantly - it would
  touch the same primary cost computation `CorrectionConfidence`/`AutocorrectAggressiveness` already
  calibrated against a real regression corpus (D-353's own worked numbers: Ohren/Ihren, ddr/der, due/die,
  komplett, ...), risking reopening one of those for a benefit that, so far, is entirely hypothetical - no
  confirmed case exists where D-356's own tie-break (below) is actually insufficient. **Status: not needed
  yet.** D-356's tie-break-only fix (never able to out-rank a candidate with a genuinely lower folded cost,
  by construction) fully closed the one confirmed real case. Revisit only if a future concrete report shows
  a diacritic-preserving candidate losing *outright* (not merely tied) against a diacritic-discarding rival
  with a genuinely different folded cost - a case the tie-break structurally cannot reach.

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

    **The nominalised-infinitive follow-up - RESOLVED, mostly before anyone got round to it (§478).** The
    user's own deferred ask ("später können wir dann die anderen Beugungsformen davon und von anderen
    Wörtern hinzufügen") named `lachen`/`Lachen` as the single worked example, with `essen`/`Essen`,
    `leben`/`Leben` and others left open. Checked directly rather than assumed: `Lachen`, `Essen`, `Leben`
    and `Denken` all already carry `NOUN,VERB` today. §478 then closed the general case the ask was really
    about - every remaining pure-noun row colliding with an attested verb form was found and retagged, and
    the candidate scan now runs out at 0. Nothing specific to nominalised infinitives is left.

    Still not an exhaustive sweep of the whole ~210k-row combined dictionary for every possible homograph
    *type* (only the weak-verb-infinitive-vs-noun pattern has been systematically scanned at all) - that
    remains its own, much larger future project (same "needs better tooling" shape as D-306-followup).

  - **German `VERB` tagging - REWRITTEN 2026-09-08 (§478/D-461); the previous text here was materially**
    **wrong and had been since §322.** It claimed the dictionary "carried zero `VERB` tags anywhere" and
    that `gehen`/`kommen`/`haben`/`können`/`machen`/`sprechen` were tagged `OTHER`. That described the state
    *before* the §306-§315 verb-retagging sweep and §322's Wortfamilien project, and was never revisited
    afterwards; a reader acting on it would have drawn the wrong conclusion about the whole dictionary.
    Measured directly (2026-09-08, after §478): **10,712 VERB-bearing rows, 1,372 of them `NOUN,VERB`**, and
    every one of those six verbs is `VERB`-tagged. Never quote this paragraph's numbers from memory either -
    re-derive them with `awk -F'\t' '$3 ~ /(^|,)VERB(,|$)/' dictionaries/de/dict.tsv | wc -l`.

    What *is* still true, and is the useful part to carry forward: **no code path anywhere reads `VERB` to
    make a decision.** Every consumer asks only `contains(NOUN)` / `contains(PROPER_NOUN)` / "are all tags
    noun-ish" - `CapitalisationEngine` (§6), A-05's own not-both-nouns split gate, and
    `CorrectionConfidence`'s `isNounLike`. So `NOUN,VERB` and `NOUN,OTHER` are behaviourally identical
    today; D-368's retags were never "tagging verbs", they were flipping `isPureNoun` to false. A future
    feature that genuinely wants to read `VERB` should first check coverage rather than assume it: of
    14,373 single-word infinitives in `wiktionary_verben.tsv` only 2,250 have their infinitive in
    `dict.tsv` at all, and 114,162 conjugated forms are absent entirely - see the separate B1/B2 bullet
    below for the measured cost of closing that.

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

- **D-442's own English confusables-scan side-effects - BOTH RESOLVED (§457, v1.2.17).** The
  frequency-correction backlog (121 real auto-apply-risk pairs, fixed) and the `en/dict.tsv` noise audit (140
  bogus generator-artifact entries removed, 1 retagged) were both closed in one round - see §457 in Current
  State for the full method, the root-caused generator bug behind the noise, and the real numbers. The
  chip-offer-only tier (1176 pairs) was deliberately left as-is on explicit user agreement, once the real
  frequency-bump sizes needed to clear it fully turned out grossly disproportionate to these words' actual
  corpus rarity.

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
  - **D-356 - RESOLVED, device-confirmed (§456, v1.2.16).** A typed umlaut should not be carelessly reverted by autocorrect -
    finally got a concrete, real example: typing `"gedrücjz"` (a genuine `ü`) autocorrected to `"gedruckt"`
    instead of `"gedrückt"`, purely because both tied on the existing folded edit cost and the more frequent
    word then won on raw frequency alone, discarding the fact that a real `ü` (not a lazy `u`) was actually
    typed. Fixed with a literal-spelling tie-break, consulted only between candidates that already share the
    same folded cost - see §456 in Current State and the design discussion above (a deeper, cost-model-level
    "keyboard reachability" idea was also discussed and deliberately deferred - see Reserve Ideas).
  - **D-357 - REOPENED then RESOLVED for real, device-confirmed (§449, v1.2.9).** The 2026-09-01 "no longer
    reproducible" closure did not hold - the user captured a real device log reproducing it in Google Keep
    and asked for it to be re-investigated. See §449 in Current State for the real root cause (traced from
    that log, not guessed) and the fix. Confirmed working against the exact repro parcours derived from that
    log ("hat funktioniert").
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
  - **D-385 - RESOLVED (2026-08-31 decision + §458/v1.2.18 implementation).** "German should go back to being
    a bundled language, not an installable pack" - discussed for/against directly with the user; decided
    **against** bundling (would permanently privilege the maintainer's own language for every future
    non-German install, and the current architecture has no "installed but deactivated" state at all - not
    even English can be turned off, so bundling German today would not even satisfy the user's own "must be
    at least deactivatable" requirement without first building that capability separately). Instead: a
    locale-aware first-run prompt was agreed as the better-targeted fix - **the follow-up (D-385-followup,
    §458) generalised this to every installable pack, not German alone**, per the user's own explicit
    instruction when finally picking this up. D-385's own "nothing may be lost" constraint is moot given the
    no-bundling outcome (no migration ever happens).
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
  - **D-391 - RESOLVED (§454, v1.2.14).** A-06 generalised beyond its own narrow, tap-evidence-gated scope:
    at ordinary commit, every space-row connector letter of the *active layout* (D-397's `RowGeometry`, not
    a fixed character list) is tried between the previously-committed word and the current token, and the
    two genuinely fuse into one word (e.g. `"Ar eitstag"` -> `"Arbeitstag"`) rather than only the right-hand
    one being repaired. Gated by a new confidence measure and its own dedicated, off-by-default setting
    (C-25) - never C-21/C-22, a "chip" tier makes no sense here - with full A-07 undo and symmetric
    learning-reversal for whichever original fragment did not independently resolve as a real word. See §454
    in Current State and spec §44 for the full mechanism and design discussion.
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
  - **D-397 - RESOLVED (§453, v1.2.13).** A new generic 0.3 vertical-drift cap now applies to any letter or
    digit key with a genuine row above/below it on the active layout (derived from the same shared
    `RowGeometry` model `KeyboardProximity` already uses for typo adjacency), wherever no tighter,
    hand-confirmed override already applies - see §453 in Current State for the full mechanism and the
    design discussion that led to it.
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
  - **D-401 - RESOLVED, implemented (§460, v1.2.20).** The four-stage space-bar cursor/selection mode from
    history §276's own verbatim capture - see spec G-08 and §460 in Current State for the full mechanism and
    the design decisions made along the way (Gboard-inspired calibration, DPAD-based line movement,
    composing state deliberately left untouched, its own opt-in Settings toggle, default off).
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

- **D-452 - RESOLVED, device-confirmed by the user on 2026-09-08 (§477, v1.2.37) - including the long-token**
  **`"hqllervoorden"` case §477 itself had flagged as not yet verified.** Originally closed WON'T FIX as not reproducible; the
  user later sent a real device log describing it as "das immer wiederkehrende Performance-Problem" (the
  recurring performance problem), captured incidentally while typing in Gemini. That specific log showed no
  smoking gun - `showSuggestions()` fired redundantly 2-4x in a row for an identical, already-empty result
  around a field restart, but that call does no dictionary work and cannot plausibly cost anything perceptible;
  the multi-second gaps between logged events show no activity at all during them, consistent with either a
  genuine user pause or a real, still-unlogged stall - not distinguishable from the log alone. Per this
  project's own diagnosis convention, nothing was guessed at or changed based on that log alone. §451: real
  wall-clock timing (`tookMs=`) was added to `refreshSuggestions()`'s own dictionary-lookup call and to
  `showSuggestions()`'s total duration - the next captured log, ideally taken at the exact moment something
  feels slow, should show directly whether either is the real cost or whether the stall (if real) lies
  somewhere else entirely.

  **§459 (v1.2.19): that next log arrived - a real ~1.3s gap while long-pressing the full-stop key's own**
  **`!`/`.`/`?` popup, right as the deferred/expensive-fallback suggestion search (D-160/D-208/D-211) for an**
  **unknown word ("Habeck") returned.** Both of §451's own timers (`candidates=`/`tookMs=` and
  `showSuggestions()`'s own total) came back fast in this log too - confirming, this time with a concrete
  case, that the stall genuinely sits somewhere else inside `refreshSuggestions()`, between those two already-
  measured points. Traced (not guessed) to the strongest remaining suspect in that gap:
  `ambiguousCasingChips()` calls `dictionaryStore.partsOfSpeech()` once per candidate, and each such call runs
  two full, uncached SQLite queries (`SqliteDictionaryStore.entryOf()`'s own bundled+learned lookup) - up to
  24 synchronous main-thread round-trips for the 12 candidates this specific case had, right at the exact
  moment the background executor's own heaviest search (compound-split/wide-fuzzy/raw-coordinate) hands its
  result back. The same class of cost ("redundant per-candidate queries") the D-207-D-221 investigation
  already named once for a different call site, apparently never covered here since `ambiguousCasingChips()`
  itself was added later (D-404-followup). Not yet device-confirmed as the actual cause - the evidence is a
  traced code path, not a live measurement, so three new temporary timers (`ambiguousCasingMs`,
  `extrasMs`, `pendingMs`, plus `tier3InlineMs` for the `!tier3Async` inline path) were added right where the
  gap was narrowed to, mirroring §451's own diagnostic style exactly. Waiting on the user's next captured log
  from a real repro to confirm or rule this out with an actual number, per this project's own convention -
  nothing changed behaviourally, diagnostic-only.

  **§477 (v1.2.37): that confirming log arrived - a second "Habeck" repro, this time with the §459 timers**
  **actually populated - and it DISPROVED §459's own suspect.** `ambiguousCasingMs=1` (not the culprit);
  `extrasMs=1335` instead, pinpointing `refreshSuggestions()`'s own extras block (split/raw-coordinate/
  autocorrect-chip/speed-unit/missed-backspace). Root cause, found by reading the code at that exact point:
  `provider.hasObviousCandidate()` was called twice there - once for `rawCoordinateSuggestion`'s own gate,
  once more for `missedBackspaceSuggestion`'s - and each call independently re-ran the *entire* expensive
  candidate search (prefix completion + D-328 neighbour-prefix escalation ×24 variants + D-453 doubled-
  consonant escalation ×24 variants + D-12 fuzzy neighbours + D-116 compound-split) from scratch,
  synchronously, on the main thread. For a genuinely unknown word ("Habeck", a surname) every one of those
  escalation stages runs to completion since nothing short-circuits it early - and it ran twice. Worse:
  `dispatchExpensiveSuggestionSearch()` (D-211) already computes this identical value once, on the
  background executor, specifically to keep expensive work off the main thread - the extras block's own two
  calls quietly defeated that by recomputing it synchronously anyway. Fixed by threading the background
  result through as a new `precomputedHasObviousCandidate` parameter (the same shape as the three existing
  `precomputed*` parameters `refreshSuggestions()` already had for exactly this reason), removing both
  synchronous main-thread calls entirely for the deferred-pass case; the one remaining caller-supplied-null
  fallback path now also computes it at most once, not twice, sharing a single local between both gates.
  [AdaptKeyService.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt). The §459 comment
  naming `ambiguousCasingChips()`/`partsOfSpeech()` as the suspect was corrected in place rather than left
  stale, since the new log actively disproves it (`ambiguousCasingMs=1`) rather than merely superseding it.
  Diagnostic timers kept in place (cheap, `SystemClock.uptimeMillis()` only) to confirm the fix on the next
  real device log, per this project's own standing convention - build green, 1640 tests green.

- **D-453 - RESOLVED (§448, v1.2.8).** Double-consonant "unfold" for autocorrect/chip suggestion
  (`"bite"` → `"bitte"`, `"tipen"` → `"tippen"`), implemented exactly as agreed - an extension of S-09's
  existing neighbour-prefix escalation (D-328), not a new fold/unfold table - see §448 in Current State for
  the mechanism.

- **D-454 - RESOLVED (§447, v1.2.7).** Restructured the Language Packs settings screen
  (`LanguagePacksActivity`/`activity_language_packs`, D-280) per the user's own six-point list - see §447 in
  Current State for the full implementation.

- **D-455 - RESOLVED, device-confirmed (§450, v1.2.10).** In a `reclaimOnCaretMoveSuppressed` field (Gemini,
  D-351), moving the caret between existing words now correctly re-derives Shift/Caps fresh, independent of
  the composing-region reclaim itself staying suppressed there - see §450 in Current State for the mechanism.

- **D-456 - CLOSED, device-confirmed working (2026-09-08, no code change).** The user confirmed on device
  that the behaviour is already correct, so the existing D-248/D-140 mechanism
  (`rememberForBackspaceUnlearn`/`recentLearnRecords`/`maybeUnlearnOnBackspaceReturn`) does cover this
  case after all - the analysis below had already concluded it probably did, and the two open questions it
  raised (does the punctuation-committed case land the caret in time; is `RECENT_LEARN_HISTORY_SIZE = 5`
  ever exhausted in practice) are answered by that confirmation rather than by a test. Kept for the
  reasoning, which stays the right starting point if it is ever reported again.
  Original ask: with D-348 (double-tap-Backspace-undo) enabled, when a word
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

- **D-457 - RESOLVED, device-confirmed (§450 + §452 followup, v1.2.10/v1.2.12).** A learned all-caps acronym
  (`"LLM"`) with no noun/proper-noun tag of its own used to come back a mangled `"lLM"` - not literally "no
  chip", but close enough to read as one (§450). §450's own fix only covered *`word` already spelled as the
  acronym itself* (e.g. the ordinary suggestion-list candidate, already correctly cased by
  `unigramsByPrefix()`) - a real device log then showed the *full lowercase spelling* case (typing the whole
  word `"llm"`, not just its prefix) still came back `"Llm"`, since every branch of the hierarchy can only
  ever touch a word's own first character, never reconstruct a multi-capital canonical form from scratch. See
  §452 in Current State for that follow-up fix (`store.entryOf()`'s own case-insensitive canonical lookup).

- **D-458 - RESOLVED, device-confirmed (§451, v1.2.11).** "Ab dem zweiten Wort" no suggestion chips appeared at all any more -
  root-caused directly from a real device log the user captured (typing "Test llm"), not guessed: a spurious
  double space in the A-03 language-classification context string (`selectActiveDictionary("Test  l")`,
  visible verbatim in the log) confused the classifier into reading the context as foreign, suppressing every
  suggestion from the second word onward. See §451 in Current State for the exact mechanism and fix. Turned
  out unrelated to D-455/D-357 - both real suspects given the timing, neither actually involved.

- **D-459 - WON'T FIX for now, deliberately deferred (2026-09-07, no code change).** While chasing an
  unrelated, still-unreproduced bug (a Backspace positioned right after a letter mid-word deleting the
  letter *before* it too - not caught in this session's own log), the user found and reported a second,
  real effect in the same log: typing `"Wie Abt ist der da?"`, repeatedly editing the word at the "Abt"
  position down to `"at"` with the caret left *mid-word* (between the two letters, not at the true end), a
  `SPACE` tap classified `LETTER_AMBIGUOUS` (T-05 - the raw tap coordinate sat in the ambiguous zone near
  the top of the space bar, plausibly a genuine light mistouch) landed there. Root-caused precisely from the
  log, not guessed: `finalizeAndCommit: typed="a"` (not `"at"`) is D-119/D-120's own documented mid-word-
  delimiter-split mechanism firing correctly on its own terms - the space arrived while `composingCursor !=
  composing.length`, so it split `"at"` into `"a"` + space + a freshly re-seeded `"t"` composing token,
  exactly as that mechanism is specified to. Not a corruption bug - the user's own very next Backspace
  correctly undid it (deleted the space, reclaimed `"a"`+`"t"` back into one token via the ordinary
  reclaim-on-adjacent-word path), and typing continued normally to a correct final `"alt"`.
  **The real, still-open design question**: should a `SPACE` tap already flagged `LETTER_AMBIGUOUS` be
  treated more conservatively when `composingCursor` is genuinely mid-word (biased toward *not* splitting,
  since the tap itself is already flagged as plausibly not a real space) - a narrower, more targeted change
  than touching D-119/D-120's own mid-word-split behavior for a clean, unambiguous space tap. Explicit user
  call: not worth the risk right now - this touches exactly the composing-state/`onUpdateSelection` area
  spec §1's guiding principle already flags as historically fragile, and the priority right now is getting
  the base stable for an upcoming F-Droid release (real maintainer feedback has come in - see the Release
  Channels section). Deferred, not abandoned - "vielleicht nutzt es uns später noch einmal" - revisit only
  if the user raises it again, ideally with its own dedicated repro.

- **D-460 - CLOSED, device-confirmed (2026-09-08); no dedicated fix was ever written, so the mechanism**
  **below is inferred rather than proven.** The user confirmed the symptom is gone. Nothing ever targeted
  it directly, so the most plausible explanation is §477's own fix: the chips vanishing was a
  reclaim/render race against a 350 ms guard window, and §477 removed a multi-second main-thread block
  (two full `hasObviousCandidate()` searches per keystroke) that would have made exactly that race fire
  reliably. Recorded as inference, not fact - if it ever returns, treat the analysis below as still
  current and capture a real log rather than assuming this closure explained it. Original report: tapping the D-36
  clipboard-peek button while the caret touches an existing word shows the clipboard chips only briefly
  before the word gets reclaimed and the chips disappear again - reported as reproducing independently of the
  D-401 cursor-control gesture (§463), and confirmed here not to trace to anything that session touched.
  `openClipboardPeek()`'s own KDoc already documents a `reclaimChipRefreshSuppressedUntil` window
  (`RECLAIM_DEBOUNCE_MS` + `CLIPBOARD_PEEK_ECHO_GUARD_MARGIN_MS`, 350 ms total) built specifically for "the
  chips flashed and immediately vanished again" - the identical symptom now reported again, either a
  regression of that existing fix or a related race it does not fully cover. Needs a real device log to
  root-cause properly, per this project's own convention - not attempted blind.

- **D-461 - RESOLVED, device-confirmed (§478, v1.2.38; confirmed 2026-09-09).** Automatic capitalisation now fires only
  for a word with no reading beyond noun/proper noun - §6's rules 3 and 4 collapsed into one `isNounOnly`
  predicate, closing a live bug where a `PROPER_NOUN` tag silently overrode a correctly-detected ambiguity
  and re-broke D-368's own retags (`Weg`, `waren`, `Arbeit`, `Rolle`, `Recht`, `Alter` - 159 German rows).
  User's own framing, worth keeping because it is the standing principle for this whole area rather than one
  decision: automatic capitalisation is only for cases with genuinely no doubt, since a wrongly forced
  capital on a deliberately lower-cased word is what makes people switch autocorrect off entirely - one chip
  tap is the cheaper failure. See spec §46 and §478 in Current State. The one class this deliberately does
  **not** fix: a surname that is also an ordinary word (`Ehrlich`, `Jung`, `Kluge`) stays ambiguous by
  design, with W-04 learning the user's own casing from real use.

- **D-462 - RESOLVED for the conservative scope (§483, v1.2.43), not yet device-confirmed; the wider**
  **import stays open.** 5,591 forms of 1,976 attested verbs added at floor frequency 1 after a real user
  review that turned 70 hand-rejections into a mechanical rule - see §483. What remains open is the ~100,000
  forms of verbs the corpus never attested, including ordinary everyday ones the user named as legitimate
  (`entarten`, `einbürgern`, `aufhellen`, `befeuchten`, `entkernen`, ...) that are simply absent from
  `dict.tsv`. Original analysis: German's verb coverage is
  far thinner than every comparable language's, and this is a **deliberate §322 decision that the later
  language packs silently abandoned**, not an oversight. §322 explicitly rejected importing vocabulary the
  Wikipedia corpus never attested ("the project stayed scoped to completing existing lemmas' paradigms, not
  growing the vocabulary itself") because such rows carry no real frequency signal. Every pack built from
  D-441 onwards ignored that rule. The result, measured: Spanish 269,508 VERB rows (64.2% of its
  dictionary, 77.6% of them at frequency <= 3 - i.e. generated, not attested), French 51.8%, Portuguese
  43.7%, against **German 5.5%** with a mean VERB-row frequency of 166.5. 114,162 German conjugated forms
  documented in `wiktionary_verben.tsv` are absent from `dict.tsv` entirely.

  The regression cost of closing that was measured against `CorrectionConfidence.forKnownWordOverride`'s
  own formula (a new word blocks an existing correction when `freq_target / freq_new < 500^0.75 ~ 105.7`),
  and the result **inverts the obvious intuition**:
  - **B1** (complete the paradigms of the 2,250 verbs already in the dictionary, using §322's own
    lemma-derived frequency): 9,079 new forms, 354 regressions, **38 of them against a target with
    frequency >= 500** - real losses like `befundet`->`befindet`(7567), `begingt`->`beginnt`(2011),
    `lieft`->`liegt`(12420).
  - **B2** (additionally import the 12,123 verbs missing entirely, at a floor frequency of 2): 110,549 new
    forms, 3,667 regressions, **0 against a target with frequency >= 500** - every one is against a word
    below frequency 212, which nobody types.

  So the risk is driven by the *assigned frequency*, not the word count: a floor frequency keeps A-01's
  ratio override firing, a lemma-derived one blocks it. The safe shape of a broad import is therefore the
  one Spanish/French already use, and §322's own frequency rule is the part to drop - the opposite of what
  the conservative framing suggests. **Not started, and not a data-only change:** the dictionary would go
  188,244 -> ~308,000 rows, so given §477's stall history this needed a real runtime measurement (prefix
  scan, D-328/D-453 escalation cost) before shipping, not just a quality-gate pass.

  **That measurement happened (§480) and largely dissolved this objection - but only because it found
  something else first.** The prefix query was doing a full table scan on every keystroke (D-465), so
  growth would indeed have cost proportionally. With the query now index-backed, cost tracks the
  prefix's own match count rather than the dictionary's size: at 307,826 rows the same lookups measure
  roughly **three times faster than today's 188,244 rows did before the fix**. Verb forms do cluster on
  ge-/be-/ver-/ent-/sch- prefixes (2-3x more matches there, against 1.64x overall), so those specific
  lookups do get dearer - from a far lower base. D-465 is now device-confirmed (several days of smooth
  real-device use, 2026-09-10). Remaining before a decision: the import's own *benefit* is still
  unquantified (a floor-frequency form never ranks into the top 8, so the gain is that genuinely-typed verb
  forms stop being unknown tokens that trigger the whole escalation cascade - plausible, but not measured).

  **2026-09-10: scope settled with the user and the review list handed over.** The chosen shape is the
  conservative one - complete the paradigms of the 2,250 verbs the corpus already attests (19,129 missing
  forms, dictionary +10.2%), and leave the ~100,000 forms of verbs the corpus never saw alone, where §322's
  own no-frequency-signal reasoning still holds and where the junk sits (`abbaken`, `abbahren`). **Floor
  frequency 1, not 2**: the A-01 override keeps firing while `freq_target / freq_new >= 105.7`, so a lower
  floor protects *more* - 893 affected corrections instead of 1,035, and the strongest affected target drops
  from 211 to 105. The remaining 893 cases were written to a banded review list for the user (`denkst`,
  `glühte`, `rauchten`, `hackt` and the like - real words whose correction only ever fired because the word
  was missing). Awaiting that review before applying. A methodological correction worth keeping: the first
  version of that list had 1,057 entries because it measured the strongest *blocked* competitor; the right
  question is whether the strongest competitor overall still wins, since that is the correction that
  actually existed before the import.

- **S-11 dual-casing chips - RESOLVED, device-confirmed (§479, v1.2.39; confirmed 2026-09-09).** Both open halves
  closed: next-word predictions now offer both casings (expanded in place, keeping the prediction's own
  rank), and the exact-match carve-out stays as it was on the user's own explicit reasoning. A stale-chip
  lifecycle bug was found and fixed alongside it. Original entry:
  **(2026-09-08).** Explicit user instruction given mid-round and parked. Today `ambiguousCasingChips()`
  returns `emptyList()` outright for an empty `input`, so a next-word prediction never offers both casings -
  D-440 closed with exactly this as its named open design question ("dual chips before anything is even
  typed, vs. a single best-guess reading"), and the user has now answered it: always offer both. One point
  still to settle before implementing: whether "immer" also drops the existing S-02 carve-out (once the
  token exactly matches, only the *other* casing is offered today), or only extends the mechanism to the
  `composing.isEmpty()` case. This became more valuable with §478 - far more words are now genuinely
  ambiguous, so the chips are the primary way their casing gets chosen at all.

- **D-464 - RESOLVED (§479, v1.2.39).** `quality_gate.py` gained `--capitalises-nouns`; the Language
  Contribution Guide's own now-false `isProper` claim was corrected in the same pass. Original entry: Its "0 bare-NOUN rows" check encodes D-441's convention for languages that
  do *not* capitalise common nouns (English tags them `NOUN,OTHER`). German's bare `NOUN` is exactly what
  drives its own auto-capitalisation, so running the gate the Language Contribution Guide documents against
  `dictionaries/de/dict.tsv` prints `QUALITY GATE: FAIL` with 108,779 "violations", every one of them
  correct. The other three checks pass. Needs a per-language flag ("this language capitalises common
  nouns") rather than dropping the check - it is genuinely right for every other language.

- **D-467 - RESOLVED (§482, v1.2.42), not yet device-confirmed.** German's `lemma` column had two wrong
  links plus 223 cyclic and 605 chained rows; all repaired mechanically, the cycle directions decided from
  `wiktionary_verben.tsv` rather than guessed. New `dictionaries/lemma_check.py` guards the three structural
  classes going forward. Data-only, no code touched.

- **D-468 - RESOLVED (§484, v1.2.44), not yet device-confirmed.** The same structural repair as D-467
  (chain flattening + attested-direction cycle resolution), generalised to all 31 remaining dictionaries
  (30 packs + bundled English) via new `dictionaries/lemma_repair.py`. Doing so found a real limit of the
  method itself that the original scoping got wrong: unlike German, the other packs' own Wiktionary
  extraction makes cycle direction almost never mechanically decidable, so nearly all cycles fail open
  (every link among their members dropped) instead of being resolved the way German's were - see §484 below
  for the proof and the numbers. Chain flattening (the larger share of the originally measured defect
  volume) is completely unaffected by this and ran exactly as planned. 446,015 lemma-column rows changed
  across the 31 packs; every pack passes `lemma_check.py` and `quality_gate.py`.

- **D-461 cross-language effect - CHECKED, no action needed (2026-09-10).** D-461 switched off forced
  capitalisation for 1,295 rows across all languages (rows carrying `PROPER_NOUN` plus some other reading).
  The frequency-strongest were sampled per language and the outcome is right almost everywhere, because
  these are overwhelmingly ordinary lower-case words that picked up a spurious proper-noun tag from the
  corpus: `grande`/`tous`/`telle` (fr), `italiana`/`romana` (it), `italieni`/`ruși` (ro), `hrvatska` (hr,
  also the adjective), `andre` (nb, "other"). The structural exception is a language that capitalises
  nationality adjectives - essentially only English, already corrected in §479. **One residue this session
  could not settle honestly**: Greek's 15 rows (`Αγίου`, `Αγίας`, `Άγιο`, ...) need a native reader; a guess
  was deliberately not made.

- **§484 (v1.2.44): D-468 - the lemma-column structural repair generalised from German to all 31 remaining**
  **dictionaries, and the generalisation itself turned up a real, proven limit of the method.** New
  `dictionaries/lemma_repair.py` reuses `lemma_check.py`'s own resolver: chains always flatten onto their
  deepest root (asserts nothing new, exactly what §320/§321 already require); cycles get decided in two
  tiers - strong (every other cycle member is attested as one of a candidate's own paradigm forms) then weak
  (exactly one member is a paradigm headword at all, German's own actual method) - and dropped entirely
  (every link among the cycle's members removed) when neither tier decides.

  **The regression check against German passed first**: 0 changes, confirming the script reproduces §482's
  already-approved manual work before touching anything new.

  **The dry run across the other 31 packs then surfaced the real finding, not assumed going in.** Of 10,006
  cycles found (component count - one entry per A<->B pair or larger ring, not per row), only **6** resolved
  via attestation (cs 1, fi 1, nl 1, ru 3) - everywhere else, both tiers tied and the cycle was dropped.
  Traced to the root, not just observed: German's `wiktionary_verben.tsv` lists ONLY genuine infinitives -
  a conjugated form never gets its own row - so its attestation check is inherently one-sided. Every other
  pack's extraction (`extract_wiktionary.py`'s `is_form_of_entry()` filter) gives an inflected form its own
  full paradigm entry too, whenever that form happens to have its own non-"form_of" Wiktionary page -
  confirmed directly on French: `grandes` (fem. pl. of `grand`) has its own page listing `grand`/`grands`/
  `grande` as ITS forms, exactly as `grand`'s own page lists `grandes`. `merge_wiktionary.py`'s
  `process()` walks every `(headword, forms)` pair in file order and sets `form.lemma = headword` whenever
  the target's lemma slot is still empty - when both directions of a pair each have their own paradigm
  entry, both writes happen, which is definitionally how a same-file 2-cycle arises. Proven, not just
  measured: a 2-cycle A<->B can only exist in `dict.tsv` in the first place if `forms(A)` contains B AND
  `forms(B)` contains A - precisely the condition the strong-tier check needs to decide a direction - so it
  is guaranteed to tie for essentially every real case, not just usually. Frequency-ratio tie-breaking was
  considered and rejected for the same reason the `--suspects` screen is out of scope for D-468 in the first
  place: it would silently reintroduce the exact suppletive-paradigm judgement call (`is`<-`be`) that needs
  a native speaker, under a different name.

  **Applied to all 31 packs on that basis** (`export PYTHONIOENCODING=utf-8` needed first - the default
  Windows console codepage crashed the script's own sample-printing on any non-Latin-1 cycle member, e.g.
  Cyrillic or Croatian `ć`; no dict.tsv was corrupted by this, the crash happened before the write). Spot-
  checked real transformations by hand afterward, not just the gates: French `références` (which chained
  through `référence`) now correctly flattens straight to `référencer`; `notes`->`note` now flattens to
  `noter`; `française`'s link to the non-existent `françaix` (a pre-existing bad link this round happened to
  clean up, not something D-468 set out to fix) is gone; Russian `лет` (which chained through `лета`) now
  flattens straight to `лето`; Ukrainian `році`/`років`/`роках` (all inflections of `рік`, "year") lost their
  link entirely because `рік` itself sat in an undecidable cycle with `ріка` ("river") - dropped correctly
  rather than guessed. **446,015 total lemma-column rows
  changed across the 31 packs** (chain flattenings plus cycle redirects/removals combined - the same figure
  `git diff --stat`'s own insertion/deletion count independently confirms). Every one of the 31 packs passes
  `lemma_check.py` (0 self-links/cycles/chains) and `quality_gate.py`.

  **Packaging, all 31.** For the 30 downloadable packs: `dictionaries/<code>/version.txt` bumped by 1,
  archive rebuilt under `language-packs/` (Python `zipfile`, `ZIP_DEFLATED` - the per-language
  `dictionaries/<code>/build_zip.py` scripts already checked in do the same thing), unzipped and byte-
  compared against the source `dict.tsv`/`bigram.tsv`/`hints.tsv`/`version.txt` before trusting it, and
  `LanguagePackCatalog`'s own per-language `version` bumped with a comment recording that language's own
  cycle/chain counts. English's bundled `app/src/main/assets/en/dict.tsv` (2,496 rows changed, 0 cycles -
  chains only) needed neither a `version.txt` nor a catalog entry - it ships inside the APK directly
  (D-280), covered by the app's own `versionCode`/`versionName` bump. See spec §38's new D-467/D-468
  addendum for the structural invariants and the full proof of why the attestation method cannot generalise.

  Data-only; no Kotlin logic touched (only `LanguagePackCatalog`'s own version numbers/comments).
  1651 unit tests unchanged, `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 539 -> 540,
  `versionName` `"1.2.43"` -> `"1.2.44"`. Not yet device-confirmed.

- **§483 (v1.2.43): D-462 - the German verb-paradigm gap closed, and the user's review turned a**
  **hand-check into a rule.** 5,591 missing inflected forms of 1,976 attested verbs added at floor
  frequency 1 with a `VERB` tag. Data only; no Kotlin touched.

  **The review found a systematic flaw in the filter, not just 70 bad words.** The candidate set came from
  "the infinitive exists in `dict.tsv`" - and that test was matching **noun plurals spelled like
  infinitives**. `Arten`, `Osten`, `Tonnen`, `Kunden`, `Hosen`, `Socken`, `Ketten`, `Linden`, `Bullen` are
  all ordinary noun rows, and the whole fake paradigm of each was queued for import. The user rejected 70
  such verbs by hand; checking their dictionary rows showed **64 of the 70 carry no `VERB` tag at all**,
  which turned the hand list into a mechanical rule instead of a one-off patch.

  **The rule, and why it needs two arms.** Requiring a `VERB` tag alone was too strict: a real verb whose
  infinitive coincides with a noun plural has the *noun* row (`Abstürzen`, `Abfragen`, `Abgleichen`), so it
  is untagged too - and §478's Phase 1 could not tag those without switching off the noun's own
  capitalisation. What separates them is exactly what the user's own remarks kept saying ("nur mit
  Vorsilbe"): the rejects are bare stems, the real verbs are prefixed. So: **VERB tag, or a known verb
  prefix plus at least four further characters**. Validated against the review rather than asserted - of the
  70 rejections it would still admit only 7, and those are excluded by name.

  **A real bug caught by spot-checking rather than by the gates.** Separable verbs come out of
  `wiktionary_verben.tsv` as **multi-word** forms (`stürzt ab`, `stürze ab`). The first applied pass
  inserted **8,365 of them** - rows a word dictionary can never match, since the tokeniser never sees a
  space inside a token, and pure ballast for every prefix scan. Both gates passed anyway; only checking why
  `abstürzt` was missing surfaced it. Reverted and refiltered to single-word forms, which is also why the
  final count is 5,591 rather than the ~14,000 the naive pass produced. (Noted in passing: 333 multi-word
  rows already existed before this round, from an earlier noun pass - `des Nord`, `(des) Französisch`. Not
  touched here, but they are the same class of defect.)

  **Floor frequency 1, not 2 and emphatically not §322's lemma-derived value.** A-01's override keeps firing
  while `freq_target / freq_new >= ~105.7`, so the *lowest* floor protects existing corrections most: 438
  affected corrections, strongest affected target at frequency 105. §322's formula would have put that at
  12,420 (`lieft` -> `liegt`). And the 438 are overwhelmingly the good kind - `denkst` -> `senkst`,
  `glühte` -> `blühte`, `rauchten` -> `tauchten`, `wettete` -> `wertete`: real words whose "correction" only
  ever fired because the word itself was missing from the dictionary.

  **Lemma links only where they can be honest.** A new form links to its infinitive unless that infinitive
  row already carries a lemma of its own - in which case the row is really the noun plural
  (`Abenteuern` -> `Abenteuer`), and a link would either build a chain or claim the verb form belongs to the
  noun's family. The one-lemma schema cannot express "form of the verb reading of this row", so nothing is
  asserted. `lemma_check.py` and `quality_gate.py` both PASS afterwards.

  **What this round deliberately does not fix.** The prefixed verbs the user named as the legitimate
  counterparts of the rejected bare stems - `entarten`, `ausarten`, `beschatten`, `einbürgern`, `bezwecken`,
  `beurkunden`, `aussöhnen`, `beseelen`, `aufhellen`, `benetzen`, `befeuchten`, `ergrauen`, `entkernen`,
  `begatten`, `anketten`, `verdingen` - are **not in `dict.tsv` at all** (16 of the 27 checked). They exist
  in `wiktionary_verben.tsv`, so they would arrive with the wider D-462 B2 import, which stays out of scope.
  Worth its own decision later: these are ordinary everyday verbs, and the corpus simply never contained
  them.

  Dictionary 188,243 -> 193,834 rows (+3.0%). 1651 unit tests unchanged, `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `dictionaries/de/version.txt` 40 -> 41, pack rebuilt and verified
  byte-identical after unzip, `LanguagePackCatalog` version 40 -> 41. `versionCode` 538 -> 539,
  `versionName` `"1.2.42"` -> `"1.2.43"`. **Not yet device-confirmed.**

- **§482 (v1.2.42): D-467 - the `lemma` column had structural defects, and a new checker now catches**
  **them.** Started as the small half of the D-462 round (fix two German links the §480 survey had flagged)
  and grew once the same check was pointed at the structure rather than the content.

  **The two content fixes.** `waren`(31549) linked to `Ware`(263) - the dominant reading is the preterite
  of `sein`, and because the store keys case-insensitively that single row carries *both* readings at once,
  so no single lemma value can be right; the link was removed rather than re-pointed. `verlassen`(1569)
  linked to `Verlass`(8), though `verlassen` is itself a base form. Removal, not invention: a null lemma is
  explicitly valid under D-412.

  **The structural finding.** A survey of all 32 shipped dictionaries turned up three defect classes that
  are wrong *regardless of language* - self-links (0 everywhere, good), cycles (A is an inflection of B and
  B of A), and chains (the lemma itself has a lemma). German had 223 cyclic rows and 605 chained ones.
  Chains matter because `AdaptKeyService` resolves a family with exactly **one hop**
  (`(entry.lemma ?: entry.word)`), so an intermediate link hands two members of the same real family two
  different family keys and D-404 Tier 2's veto silently fails to fire. Checked before worrying: the
  one-hop resolver also means a cycle can never hang the app - it is a correctness defect, not a liveness
  one.

  **Both fixed mechanically, neither guessed.** All 27 German cycle *pairs* turned out decidable from real
  data: in every single one, exactly one side is an attested infinitive in `wiktionary_verben.tsv`, so that
  side is the base (`bann` -> `bannen`, `besuch` -> `besuchen`). 652 chains flattened onto their deepest
  root - `alte` -> `Alter` -> `alt` now points straight at `alt` - which is what §320/§321's own "resolve to
  the true deepest root rather than an intermediate mechanical hop" always required. 679 rows changed;
  word, frequency and POS columns verified byte-identical.

  **A self-inflicted detour worth recording.** The first pass also normalised each lemma value's *casing* to
  whatever casing that base row happens to carry, changing 60 further rows - `Dauer`'s base went from the
  verb `dauern` to the noun `Dauern`. Behaviourally inert (the resolver lower-cases both sides, and so does
  the quality gate's orphan check) but factually worse and never asked for, so it was reverted; the diff is
  exactly the 27 + 652 intended rows.

  **New `dictionaries/lemma_check.py`**, the companion to `quality_gate.py`: self-links, cycles and chains
  as hard PASS/FAIL, plus an opt-in `--suspects` screen (a form an order of magnitude more frequent than
  its own base is usually a mis-link). The screen is deliberately labelled as screening and never a verdict
  - a suppletive paradigm looks identical to the defect (`is` <- `be`, `είναι` <- `είμαι`), so every hit
  needs someone who reads the language. German now PASSes the hard checks; the wider cross-language picture
  is tracked as its own item (D-468).

  Data-only; no Kotlin touched, 1651 unit tests unchanged, `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `dictionaries/de/version.txt` 39 -> 40, pack rebuilt and verified byte-identical after unzip,
  `LanguagePackCatalog` version 39 -> 40. `versionCode` 537 -> 538, `versionName` `"1.2.41"` -> `"1.2.42"`.

- **§481 (v1.2.41): D-466 - "zum gluck" autocorrected to "Gluck" instead of "Glück"; root-caused to a**
  **single bogus dictionary row, fixed by removing it, the correction mechanism itself left untouched.**
  Reported case traced end-to-end before touching anything, per this project's own convention: a throwaway
  diagnostic unit test against the real corpus numbers (`Gluck` 26, `Glück` 376, both `dictionaries/de/
  dict.tsv`) confirmed `autocorrectFor("gluck")` correctly returns `null` - A-01's known-word override needs
  roughly a 500x frequency ratio (log-scaled, `CorrectionConfidence.forKnownWordOverride`), and `Gluck`/
  `Glück`'s ~14.5x sits even further below it than the already-accepted "Ohren"/"Ihren" case (70x, deliberately
  kept blocked) - and `suggestionsFor("gluck")` already returned `["Glück", "Gluck"]`, "Glück" top-ranked. So
  the mechanism was not "doing half the job" - it was correctly declining to silently guess and correctly
  offering the right correction as a chip; the capitalisation to "Gluck" simply reflected what was literally
  typed, a technically-valid but (per the user's own native-speaker judgement) unattested dictionary entry
  that was never tapped away.
  
  The deeper reason the umlaut was not silently restored: [DictionarySuggestionProvider.diacriticRestoration]
  (the dedicated, unconditional "typed without umlaut" mechanism, D-48) bails out whenever the bare ASCII
  spelling is itself already a known word, deferring to the much stricter general A-01 ratio path - `"Gluck"`
  existing in the dictionary at all was precisely what disabled the shortcut that would otherwise have fixed
  this silently and correctly.
  
  **Design discussed first, per this project's own convention**: giving a pure-diacritic-fold match its own,
  lower override bar than the general 500x one was considered (a fold-only difference is a narrower, stronger
  signal than an arbitrary rare/common pair) but explicitly declined for now - real German homograph pairs
  exist that differ *only* by umlaut and are both genuinely common with different meanings (e.g. "mochte"/
  "möchte"), so loosening the unconditional `diacriticRestoration` path risked silently corrupting a
  correctly-typed, common, different word, for a benefit that (on inspection) was really just one bad
  dictionary row. User's own explicit call: leave `CorrectionConfidence`/`diacriticRestoration` alone,
  curate the dictionary instead - "Gluck" is not attested German vocabulary (confirmed noise, not to be
  confused with "Glucke", the hen), carried no lemma links or dependent inflected forms, and was the only row
  of its own spelling - removed outright.
  
  `dictionaries/de/dict.tsv` 188,244 -> 188,243 rows; `quality_gate.py --capitalises-nouns`: PASS (0
  duplicates, 0 non-positive frequencies, 0 orphaned lemma links). `dictionaries/de/version.txt` 38 -> 39,
  pack rebuilt and verified byte-identical after unzip, `LanguagePackCatalog` version 38 -> 39. No code
  touched, 1651 unit tests unchanged, `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  536 -> 537, `versionName` `"1.2.40"` -> `"1.2.41"`. Not yet device-confirmed.

- **§480 (v1.2.40): D-465 - the suggestion pipeline's hottest query was scanning the whole lexicon on**
  **every keystroke.** Found while measuring D-462 (below), which asked what 110,000 extra rows would cost
  the hot path. The measurement kept returning the same number for every prefix - ~12 ms whether the prefix
  matched 71 rows or 6,002 - which is not what a working index looks like. `EXPLAIN QUERY PLAN` said why:
  `SCAN words`, plus a temp B-tree for the ORDER BY.

  **Root cause.** `queryByPrefix` used `WHERE wkey LIKE 'prefix%'`. SQLite's `LIKE` is case-insensitive by
  default; the table's own `PRIMARY KEY` index on `wkey` is `BINARY`. SQLite's LIKE-to-range optimisation
  only fires when those agree, so it did not, and every call fell back to a full scan of all 188,244 German
  rows. That is *per keystroke*, and S-09's escalations (D-328 neighbour-prefix, D-453 doubled-consonant)
  each issue their own lookup - up to a couple of dozen per pass.

  **Fix**: a half-open key range (`wkey >= prefix AND wkey < prefix + U+FFFF`), which the existing index
  serves. Measured on the real German dictionary with identical schema and query: 5.6x-1045x faster
  depending on prefix, and now proportional to what the prefix actually matches instead of to the table's
  size. Equivalent by construction rather than coincidence - `wkey` is always written as `word.lowercase()`
  (every insert path funnels through `putWordInternal`, checked directly), so a BINARY range over
  lower-cased keys selects exactly what the case-insensitive LIKE did; result equality was verified for
  every tested prefix before the replacement landed.

  **The upper bound is the simple form on the user's explicit call** ("halten wir es einfach"): exact for
  the whole BMP, would miss a key whose next character after the prefix sits above it (an emoji).
  Theoretical for a word dictionary; the alternative buys only that case at the cost of real surrogate
  handling. Documented in the code and in spec §47 rather than left implicit.

  **Honest limits of the measurement.** This is desktop SQLite, not Android's - absolute times do not
  transfer, the ratios and the query plan do. And while a full scan multiplied by two dozen escalation
  lookups fits D-452's long-running "recurring performance problem" description well, that remains a
  hypothesis consistent with the evidence, not a proven cause; §459's own timers are still in place to
  settle it on the next real device log.

  **Genuinely testable, unlike most of this area.** `SqliteDictionaryStoreRoboTest` already runs real SQLite
  under Robolectric, so this is not the untested Android-glue layer - 5 new cases pin the behaviour the
  replacement has to preserve (exact prefix membership with neither neighbour leaking in, case-insensitive
  queries, a high-BMP character right after the prefix per spec §1's umlaut principle, frequency ordering
  plus limit, and the empty result). They pass against either implementation by design: they guard
  correctness, and the performance property is what the code comment and §47 record.

  1651 unit tests (1646 -> 1651, +5). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  535 -> 536, `versionName` `"1.2.39"` -> `"1.2.40"`.

  **Device-confirmed** (2026-09-10): the user confirmed several days of real-device use running smoothly
  ("läuft wie geschmiert seit ein paar Tagen") - no dedicated repro needed, closed on that basis.

- **§479 (v1.2.39): D-463 (S-11 for next-word predictions, plus a stale-chip lifecycle bug) and D-464**
  **(the quality gate reports German as FAIL by design).** Two of the three items the user asked to clear;
  the third (D-462) turned into a much larger finding and is held for a decision - see its own bullet above.

  **D-463, the decision first.** The user's instruction was "immer beide Chips anbieten". Split into two
  questions and answered separately: (1) a next-word prediction now offers both casings, previously skipped
  outright (`ambiguousCasingChips()` returns early on an empty input) - the exact open question D-440 closed
  with; (2) once the typed token *exactly* matches, only the other casing is still offered, unchanged. The
  user's own reasoning for keeping (2): "Nein, das getippte brauchen wir nicht als Chip. Das hat man ja
  schon getippt. Es wird in dem fall aber nie auto-committet, weil definitiv keine Eindeutigkeit besteht.
  Deshalb wird ein Commit es nicht kaputt korrigieren." - which D-461 is exactly what guarantees.

  **In place, not appended - and this is the part worth recording.** While typing, the dual chips are
  deliberately appended at the back so a better ordinary suggestion can crowd them out (D-404-followup's own
  explicit design). Reusing that shape for predictions would have been wrong: a prediction can itself be the
  single best entry in the bar, so appending demotes it to last place while `excludeAmbiguousCasingWords()`
  simultaneously removes it from its real rank. New `expandAmbiguousCasingInPlace()` replaces each ambiguous
  prediction with its two casings at its own position instead, store-resolved casing first; the typing path
  is untouched. Split on the same `composing.isEmpty()` discriminator D-440 already uses two lines above.

  **A real lifecycle bug found while reading that code, not reported by anyone.**
  `pendingAmbiguousCasingChips` is filled only in `refreshSuggestions()` and was emptied only in
  `clearSuggestions()` - but the ordinary commit path is `clearComposing()` -> `showNextWordPredictions()`
  -> `showSuggestions()`, and the middle step only reaches `clearSuggestions()` when there is no prediction
  at all. So committing an ambiguous word with a real prediction following it left its chips standing,
  appended to the *next* word's bar, while `excludeAmbiguousCasingWords()` dropped a legitimate prediction
  for that same word from the ranked list. Conclusive from the state lifecycle, not a timing suspicion.
  Fixed by clearing them in `clearComposing()`, where the token they describe actually ends. D-461 made this
  considerably more visible - 1,372 ambiguous words now instead of 1,089.

  **D-464:** `dictionaries/quality_gate.py` gained `--capitalises-nouns`. Its bare-NOUN check encodes
  D-441's convention for languages that do *not* capitalise common nouns; for German a bare `NOUN` is
  precisely what drives auto-capitalisation, so the documented command reported `FAIL` with 108,779
  "violations", every one correct. The flag skips that one check and prints why; the other three are
  unchanged, and an unknown option now exits 2 with a usage line instead of being ignored. The Language
  Contribution Guide's step-8 passage was corrected in the same pass - it still claimed "a row already
  carrying `PROPER_NOUN` is correctly unaffected either way (`isProper` forces capitalisation regardless of
  language)", which D-461 made false; a contributor following it would have switched a proper noun's
  capitalisation off by adding `OTHER`.

  No new tests: both `ambiguousCasingChips()` and the new `expandAmbiguousCasingInPlace()` are
  `AdaptKeyService` suggestion-bar glue, this project's own established untested layer (the pure part they
  rest on, `CapitalisationEngine.isAmbiguousCasing`, gained its own cases in §478). 1646 unit tests
  unchanged, `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 534 -> 535, `versionName`
  `"1.2.38"` -> `"1.2.39"`. **D-463 device-confirmed (2026-09-09)**; D-464 is a tooling/data-only change
  with no device-observable behaviour, nothing to confirm there.

- **§478 (v1.2.38): D-461 - automatic capitalisation now requires genuine unambiguity, plus the German**
  **verb-tagging gap behind it.** Started as a question about this file's own point 7 below ("the German
  dictionary carries zero VERB tags") - which turned out to be **materially wrong**, and chasing why
  surfaced a live capitalisation bug that had been silently reverting D-368's work for months.

  **What point 7 actually got wrong.** It described the state *before* §322's Wortfamilien project, and was
  never revisited afterwards. Measured directly: 10,265 VERB-bearing rows (not 210), 1,089 `NOUN,VERB` (not
  210), and `gehen`/`kommen`/`haben`/`können`/`machen`/`sprechen` are all `VERB`-tagged, not `OTHER` as the
  text claimed. The §306-§315 sweep had done that work properly across every frequency band. Point 7 is
  rewritten below rather than patched.

  **The real gap it was hiding.** Of 14,373 single-word infinitives in `wiktionary_verben.tsv`, only 2,250
  have their infinitive in `dict.tsv` at all, and 392 of those carried no VERB tag - the §306-§315 sweep
  targeted pure-`OTHER` rows and never reached them. 349 were tagged (`schreiben`, `treffen`, `fehlen`,
  `geschehen`, `lernen`, `messen`, `sitzen`, `feiern`, `prüfen`, `singen`, ... down to `decodieren`),
  applying only where `isPureNoun` was **already false**, so no capitalisation outcome could change - the
  safety is a property of the transformation, asserted per row, not a per-word judgement. 43 were held back
  by a mechanical filter (the spelling is also an attested adjective form: `freien`, `langen`, `festen`,
  `gesunden`, ...) rather than guessed at.

  **A naive "tag every verb form" pass would have been a disaster, and the measurement is why it was not**
  **attempted.** Run against the real conjugation tables, 1,955 bare-`NOUN` rows collide with an attested
  verb form - `Gebiet`, `Werk`, `Tag`, `Wasser`, `Vater`, `Mutter`, `Grenze`, `Schule`. Tagging those would
  have silently switched off auto-capitalisation for ordinary German nouns. The noise is not a paradigm-slot
  artefact (`praes1` is as noisy as `imp_sg`, checked) but marginal denominal verbs in the Wiktionary source
  itself (`vatern`, `muttern`, `wassern`, `berlinern`). Restricting propagation to infinitives the
  dictionary *already* accepts as verbs cut the candidate set to 98 reviewable rows.

  **All 98 retagged `NOUN,VERB`, per explicit user instruction to tag every real double reading.** The
  original proposal split them into "everyday finite forms" (35) / "unclear" (10) / "bare-stem imperatives"
  (53) and recommended only the first two, since a bare-stem imperative collides with a noun by
  construction and `Vertrag`(2329)/`Stich`/`Ertrag` would lose their automatic capital for a reading used
  once a month. The user overrode that deliberately and it is worth recording verbatim: *"die automatische
  Großschreibung soll nur dann gemacht werden, wenn es wirklich keine Zweifel gibt ... Es ist viel besser,
  einmal einen Chip zu akzeptieren als dass ich wenn auch nur gelegentlich falsche
  Auto-korrekt-Großschreibungen erlebe. die führen nur dazu, dass Leute die Autokorrektur ausschalten."*
  The candidate scan now runs out at 0.

  **Then the actual bug (D-461, spec §46).** `CapitalisationEngine.capitalise()` ranked `isProper -> true`
  *above* `isPureNoun -> true`, so a `PROPER_NOUN` tag overrode a correctly-detected ambiguity outright.
  `Weg` is `NOUN,VERB,PROPER_NOUN`: D-368 gave it the VERB tag precisely so `"weg sein"` would stop being
  capitalised, a later corpus pass added `PROPER_NOUN`, and the force-capitalisation came back unnoticed.
  `waren`(31549) committed as `"wir Waren"`. 159 German rows were affected including `Arbeit`, `Rolle`,
  `Bau`, `Band`, `Park`, `Liebe`, `Recht`, `Alter`. Fixed by collapsing rules 3 and 4 into one
  `isNounOnly(pos)` predicate - capitalise exactly when the word has no reading beyond noun/proper noun.
  `isAmbiguousCasing` lost its own `!isProper` exclusion in lockstep (otherwise the freed words would have
  lost the capital *and* gained no S-11 chips). B-02 was deliberately **not** widened - its hyphen branch
  still tests `isProper && isNounOnly`, since using the general rule there would capitalise the second half
  of any plain compound (`"Haus-tür"`).

  **The literal user proposal ("Eigennamen braucht es nie") was checked and would have broken 31**
  **languages.** German is the only language whose every `PROPER_NOUN` row also carries `NOUN`; everywhere
  else bare `PROPER_NOUN` rows are the norm (English 29,458, Greek 29,963, French 24,854) and `isProper`
  is the only thing capitalising them. The generalised `isNounOnly` formulation achieves the user's stated
  rule without that cost.

  **Data corrections in both directions.** 57 genuine German proper nouns carried a spurious `OTHER` from
  corpus noise and would have lost their capital under the new rule - tag removed (`Ben`, `Nova`, `Terra`,
  `Ella`, `Papa`, `Felicitas`, `Wikimedia`, ...). Five English rows needed the opposite fix (`German`,
  `Jewish`, `Mussolini`, `Lindy`, `Frenchy`): English capitalises nationality adjectives, German does not,
  so the `ADJECTIVE` tag is dropped there - the same per-language data decision D-441 already established.
  Surnames that are genuinely also ordinary words (`Ehrlich`, `Rau`, `Kühn`, `Jung`, `Kluge`, `Wunderlich`,
  `Treuen`, `Frechen`) were deliberately **left** ambiguous; W-04 learns the user's own casing from real use.

  **Two unrelated finds, both fixed.** `ines` sat in the dictionary at frequency **261,120** (higher than
  `Jahr`, 33,028) as `ADJECTIVE,NOUN,PROPER_NOUN` with `lemma = in` - a generated "declension" of the
  preposition *in*, the exact over-generation bug §457 fixed for English, unnoticed here. Corrected to
  `Ines 60 NOUN,PROPER_NOUN`, calibrated against real siblings in the same corpus (`Monika` 45, `Ilse` 80),
  not an invented number. Its siblings were worse and are simply not words: `inem`/`inen`/`iner` (261,120
  each) and `zue`/`zuem`/`zuen`/`zuer`/`zues` (97,088 each) - all eight removed. The `ADJECTIVE` tags on
  `in`/`zu` themselves are kept: *"das ist in"* / *"die Tür ist zu"* are real adjectival uses, they simply
  do not decline.

  **`SeedData.kt` + `SeedDataTest.kt` deleted.** 34 hardcoded German words and 5 bigrams whose only caller
  was its own test; its KDoc described a replacement ("in a later session") that happened at D-280. English
  is bundled and every pack ships its own dictionary, so the "no dictionary at all" state it guarded against
  does not exist.

  1646 unit tests (1640 -> 1637 after removing `SeedDataTest`, -> 1646 with 9 new `CapitalisationEngineTest`
  cases covering the verb-homograph proper noun, the noun-only proper noun, B-02 both ways, and
  `isAmbiguousCasing`'s widened contract). `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `dictionaries/de/version.txt` 37 -> 38, pack rebuilt and verified byte-identical after unzip,
  `LanguagePackCatalog` version 37 -> 38. `versionCode` 533 -> 534, `versionName` `"1.2.37"` ->
  `"1.2.38"`. **Device-confirmed (2026-09-09)** - the user confirmed the broad capitalisation change on
  real typing, no word came back under-capitalised.

- **§477 (v1.2.37): D-452-followup - the ~1.3s "Habeck" suggestion-bar stall, root-caused and fixed.** User
  pasted a real device log for a genuinely unknown word ("Habeck", tapped for autocorrect) with §459's own
  timing diagnostics finally populated: `ambiguousCasingMs=1` (§459's own suspect, disproved) but
  `extrasMs=1335` - the real cost sits in `refreshSuggestions()`'s own extras block. Cause: `provider.
  hasObviousCandidate()` was called twice there (once per gate, `rawCoordinateSuggestion` and
  `missedBackspaceSuggestion`), each call independently re-running the entire expensive candidate search
  (prefix + D-328 neighbour-prefix + D-453 doubled-consonant + D-12 fuzzy + D-116 compound) from scratch on
  the main thread - for a token nothing matches, every escalation stage runs to completion, twice.
  `dispatchExpensiveSuggestionSearch()` (D-211) already computes this exact value once, on the background
  executor - the extras block's own two calls silently defeated that. Fix: a fourth `precomputed*` parameter
  (`precomputedHasObviousCandidate`, matching the shape of the three `refreshSuggestions()` already had)
  threads the background result through, removing both synchronous main-thread calls for the deferred-pass
  case entirely; the remaining direct-call fallback (no precomputed value supplied) now also computes it at
  most once, shared between both gates, not twice. §459's own "ambiguousCasingChips()/partsOfSpeech() is the
  strongest suspect" comment corrected in place - the new log actively disproves it, not merely supersedes
  it. See D-452 in Current State (above) for the full before/after story. Build green, 1640 tests green,
  -0/+~35 lines (mostly KDoc explaining the precomputed-value shape and why it replaced two full-cost calls).

  **Second log, same session: a "Ersetzungsproblem" (garbled final text) traced to the identical stall, not**
  **a separate bug.** User typed `"hqllervoorden"` (a garbled `"Hallervorden"`) - the deferred search
  correctly found it, but `extrasMs=3450` (worse than "Habeck"'s 1335ms, since the doubled escalation search
  scales with token length: 13 characters vs. 6). For that whole multi-second window the main thread (and
  therefore all key/touch dispatch) was blocked; Android still queued the user's frustrated `DELETE, DELETE,
  'a', DELETE` taps and delivered all four in one burst the instant the block ended - visible in the log as
  four `rawTap` lines sharing the identical `-13,0s` timestamp, landing right as the deferred result was also
  being applied (`onUpdateSelection: EXTERNAL (expected=31, actual=[21,21])`, followed by `STALE ECHO`
  entries as the tracked and real cursor positions fought to resync). The correctly-found `"Hallervorden"`
  was lost and the composing token ended up mangled to `"hlervoorden"` instead. Analysis only, no further
  code change: `rawCoordinateCorrection()`/`missedBackspaceCorrection()` (the extras block's only other
  per-keystroke work in this path) are both O(token length) with no store-scanning cost of their own, so
  §477's fix - both `hasObviousCandidate()` calls now served from the value already computed on the
  background executor - should collapse this specific freeze close to zero, not merely halve it. Not yet
  device-confirmed for a token this long; worth a fresh log on v1.2.37 with a similarly long unknown word.

- **§476 (v1.2.36): D-401 closed - the gesture's temporary diagnostics removed, no behaviour change.** User's
  own call after §475: "so lassen wir das, das Thema können wir abhaken" plus an explicit request to remove
  the jitter log and tidy up around Stage 2. -186/+51 lines.

  **Removed**: every `AdaptKeyJitter` line this feature added - the view's per-move `cursorControlMove` trace
  (and the two `cursorControlLastSentChars`/`Lines` fields that existed *only* to gate it, now that the
  listener is called on every move anyway), `applyCursorControlMove`'s entry/settle/clamp traces,
  `adjacentLineStart`'s four, `lineBoundsFor`'s two, `driveCursorControlServo`'s one, and
  `onUpdateSelection`'s "echo (session active)" line. The 29 remaining `diag()` calls in the service all
  belong to other features (D-139, D-347, autocorrect, clipboard) and were left untouched.

  **Kept, deliberately**: `requestCursorUpdates()` itself. It began life as §472's probe but is now
  load-bearing - it is what makes the caret's drawn position observable at all - so only the probe's
  *scaffolding* went (`cursorControlAnchorInfoSeen`, its 500 ms "nothing arrived" runnable, and
  `CURSOR_CONTROL_ANCHOR_PROBE_MS`). Renamed `start`/`stopCursorAnchorInfoProbe()` to
  `start`/`stopCursorAnchorInfoUpdates()` to stop the name implying a diagnostic, and rewrote both KDocs
  around what they now do (request per gesture, not permanently: an editor that honours this recomputes on
  every caret move, scroll and layout pass, which is real work for the target app and useless to this
  keyboard between gestures).

  **Stage 2 comments corrected to match §475's evidence.** Two places still described Stage 2's exclusion as
  "we only have evidence for the collapsed case, extending a selection needs its own probe" - written before
  the log answered it. It now reads as what it is: while a selection exists the reported insertion marker
  sits at its *anchor*, so the moving end is not observable and there is nothing to steer towards. A
  structural property, not a deferral.

  1640 unit tests (unchanged - pure logging and comment removal).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green, no warnings. `versionCode` 531 -> 532, `versionName`
  `"1.2.35"` -> `"1.2.36"`.

- **§475 (v1.2.35): D-401-followup - Stage 2 (selection) froze after its very first move; a real bug in the**
  **newline-based line-boundary logic, not in the new screen-space model.** Device report: "the selection
  mode on hold no longer works." The log names the failure directly - `applyCursorControlMove: line bounds
  unavailable - column unchanged`, repeated for every single move after the first, in both drag directions.

  **Root cause.** `getTextBeforeCursor()` returns the text before the *selection's start* and
  `getTextAfterCursor()` the text after its *end* - both anchored to the selection, not to the gesture's
  moving end. So in Stage 2 the selection's own text sits between the moving end and the surrounding text on
  one side, and has to be stitched back in there. The old `leftBoundary()`/`rightBoundary()` instead searched
  **only the selection** for a line break on that side and returned null when they found none - abandoning
  the move outright. A selection almost never spans a line break, so that was the normal case: the first move
  worked (still collapsed, other branch), and every one after it did nothing. Stage 1 was never affected,
  which is why this survived §471-§474 unnoticed.

  **Fix.** Both functions replaced by one `lineBoundsFor()` that reads all three pieces of text and hands
  them to a new pure `keyboard/CursorLineBounds` (13 tests, including both freeze directions as explicit
  regression cases). Getting this stitching wrong is demonstrably not hypothetical, which is why the
  reasoning was extracted somewhere it can actually be tested rather than left in the Android glue. A null
  `getSelectedText()` now degrades to treating the selection as empty rather than abandoning the move - the
  clamp then misses by the selection's length instead of freezing, a far better failure mode.

  **Bonus finding, and it closes an open question with evidence rather than assumption.** §473 deliberately
  kept Stage 2 on the newline model because it was not known whether `CursorAnchorInfo` reports anything
  usable during a selection. This log answers it: **it reports the insertion marker at the selection's
  anchor, never at the moving end.** Three independent confirmations - `reportedSel=[66,67]` with
  `markerX=474.4961` (offset 66's own x), `reportedSel=[67,61]` and `reportedSel=[67,133]` both with
  `markerX=484.4961` (offset 67's). The moving end is therefore *not observable*, so the screen-space servo
  structurally cannot drive Stage 2, and Stage 2 staying on the newline model is now a documented
  consequence rather than a deferred decision.

  1640 unit tests (1627 -> 1640, +13 `CursorLineBoundsTest`).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 530 -> 531, `versionName` `"1.2.34"`
  -> `"1.2.35"`.

- **§474 (v1.2.34): D-401-followup - vertical tuning after the first device round on the screen-space model.**
  User verdict on §473: "already quite good, not perfect - but let's leave it rather than doing 20 more
  rounds." One concrete complaint: 0.5 is **"exactly right" horizontally**, but vertically the caret still
  slips into the neighbouring line too easily. The question asked was whether that needs a threshold or just
  a lower vertical gain. **It needs both, and they belong in different places** - which is what makes this
  worth writing down rather than just retuning a number.

  **Why a gain alone is not enough.** The unwanted vertical movement is not noise, it is a systematic arc: a
  thumb pivots rather than sliding straight, and §472's log shows vertical travel running at a fairly
  consistent ~20% of horizontal travel throughout a sideways drag (dx 459 against dy -95). Since that drift
  is *proportional* to the horizontal distance, no fixed threshold absorbs it on a long swipe - only
  weighting the axis down does. Hence `SCREEN_SPACE_GAIN_VERTICAL` = 0.35 next to the horizontal 0.5, which
  stays at the value the user confirmed.

  **Why a gain alone is still not enough.** With plain rounding, the boundary to the next row sits half a
  row away from wherever the caret is, so a drag only has to produce `rowHeight / (2 x gain)` of travel to
  cross it - about 5 mm at 0.5, which is nothing. So vertical travel is now quantised into *whole rows* at
  the input (`CursorControlGesture.targetPointFor` truncates towards zero): a line change costs a full row
  of scaled travel measured from the drag's own origin, and the resulting target always lands on a row's
  *centre*, so the caret is never balanced on a boundary where a pixel of drift tips it either way. That is
  the whole deadband - no threshold constant was added. Together the two put a line change at roughly one
  and a half row heights of vertical finger travel, beyond the arc of even a full-width sideways swipe.

  **A real bug the tests caught, worth recording because the first attempt was wrong.** Truncation was
  initially put in the *servo* instead, replacing its `roundToInt`. `VisualCaretServoTest` failed
  immediately, and the failure was genuine rather than a fixture artefact: an early proposal in a loop that
  has learned nothing yet can land a row or two out, and a truncating servo then refuses to come back
  (0.8 rows of error truncates to zero), stranding the caret on the wrong row permanently. The distinction
  the fix rests on: the deadband belongs where *intent* is read (a drag's own travel), while the end that
  *corrects* must stay free to go to whichever row the target names. `VisualCaretServo` therefore keeps
  rounding, and gained a regression test for exactly that overshoot-recovery case.

  1627 unit tests (1620 -> 1627; `VisualCaretServoTest` 18 -> 19, `CursorControlGestureTest` 13 -> 19).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 529 -> 530, `versionName` `"1.2.33"`
  -> `"1.2.34"`. Both gains and the quantisation are single, independent constants - the expected next
  adjustment is one number, not another rearchitecture.

- **§473 (v1.2.33): D-401-followup - the probe came back positive, so the gesture's Stage 1 is now a**
  **screen-space model: the caret is driven towards a *point*, and a soft-wrapped line is finally a real line.**

  **The device log settled all three questions §472 asked** (Google Keep, one note = three paragraphs across
  five visible rows). (1) Callbacks arrive: `accepted=true`, then a dense stream, one per applied
  `setSelection()`. (2) Coordinates are real: never `NaN`, `visible=true` throughout. (3) **y genuinely
  changes across a soft wrap**: within the 107-character paragraph that contains no `'\n'` at all, offset 0
  was drawn at screen y 539 and offsets 55-80 at y 596 - one row lower, in the same paragraph. The visible
  line is observable after all; the conclusion recorded in §472 that it was not is now definitively closed.

  Three further facts the same log gave for free, each of which shaped the design:
  - **The font is proportional.** Per-character advances between offsets 55 and 80 ranged from 11 to 24 px.
    No fixed character width could ever have placed the caret correctly, which is why this is a *feedback
    loop* and not a calculation.
  - **Reports lag by one step.** `trackedOffset=68 reportedSel=[67,67]` recurs throughout, followed by the
    settled report. So a report must be paired with the offset it actually names, never with whatever this
    app currently believes - `VisualCaretServo.expect()` exists exactly for that.
  - **The old line-step constant was wildly out.** `DP_PER_LINE_STEP` = 200 dp = 525 px of finger travel per
    line against a 53 px row - the mechanical reason "changing lines is nearly impossible" was a fair report.

  **What was built.** New pure, fully unit-tested `keyboard/VisualCaretServo` (18 tests): it accumulates
  reported "offset N is drawn at (x, y)" facts, learns the average character advance and each row's own
  offset range from them, and proposes the next offset to move to in order to get closer to a target point.
  The caller applies it, the editor reports back, and the error feeds the next proposal - so holding the
  finger still settles the caret within a few frames instead of needing the first estimate to be right. The
  test fixture is a synthetic proportional, soft-wrapping layout, so the whole loop runs to convergence in a
  plain JVM test; the soft-wrap cases are the ones the newline-based model could not express at all.

  **The user's own two rules now fall out of the geometry instead of being enforced by thresholds.** A
  sideways drag names a point on the same row, so it cannot change lines - and the row an offset belongs to
  is *learned from real reports* (`rowBounds()`), so the clamp is exact rather than guessed. A downward drag
  names a point one row height lower, so it moves one *visible* line. `stepsFor()`'s dominant-axis gate and
  both `DP_PER_*` constants are therefore unnecessary on this path: 40 px of hand wobble against a 53 px row
  simply names the same row, with nothing to tune. Re-touching now *keeps* everything learned about the
  field's layout and only re-anchors where the finger started - the opposite of the newline-based model,
  where every re-touch re-derived a guessed column reference (§472's first finding).

  **The gain is now a real parameter.** `CursorControlGesture.SCREEN_SPACE_GAIN` = 0.5, the user's explicit
  requirement that the caret move *less* than the finger ("otherwise you may as well tap in the text
  directly"). For the first time it is a plain ratio between two distances on the same screen rather than a
  dp-per-character constant that silently depended on the target app's font - and for reference, the old
  `DP_PER_CHARACTER_STEP` worked out to roughly 0.6 against the advances the device actually reported.
  Expect to tune it; it is one constant.

  **Deliberately scoped out, with the reason.** Stage 2 (selection extension) keeps the newline-based path in
  every editor: the reported insertion marker is only unambiguous while the selection is *collapsed*, and
  this round has device evidence for the collapsed case only. Assuming otherwise is precisely the mistake
  that cost rounds §462-§470, so the existing probe logging will answer it on the next device round instead.
  The handover is explicit - when Stage 2 begins, the fallback's "origin column + total characters" reference
  is re-based onto wherever the servo actually left the caret, so the drag continues rather than jumping.
  The newline-based path also remains the full fallback for any editor that reports no coordinates at all.

  1620 unit tests (1599 -> 1620: +18 `VisualCaretServoTest`, +3 `CursorControlGestureTest`).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 528 -> 529, `versionName` `"1.2.32"`
  -> `"1.2.33"`. **Not yet device-confirmed** - the mechanism is new and the gain is a first guess.

- **§472 (v1.2.32): D-401-followup - a premise probe, not a fix: does the target editor report the caret's**
  **own drawn coordinates?** No behaviour change at all; nothing reads the probed values yet.

  **Where this came from.** §471's rearchitecture was device-tested and produced two findings. The first was
  cosmetic: horizontal positioning really is absolute *within* one touch contact (`targetColumn =
  originColumn + characters`, `originColumn` fixed), but `originColumn` is re-established on every re-touch
  inside the lift-grace window, and the log showed that happening every few seconds - which sums to
  something that *looks* incremental without being it. The second is structural and is the real blocker:
  line detection reads only real `'\n'` characters, and the tested note effectively had two of them - a
  107-character paragraph with no embedded newline (soft-wrapping across several visible lines) and an empty
  line after it. So "drag down = one visible line down" cannot work by construction, and a line change can
  only ever jump to the next real paragraph - which is exactly the reported "flip" and "changing lines is
  nearly impossible".

  That analysis then concluded the gap was unbridgeable, since `InputConnection` offers no way to ask the
  target app for its text layout, and DPAD navigation (which would have used the app's own layout) was
  already tried and rejected in an earlier round - it moved system focus to a different field entirely in
  Google Keep (see `adjacentLineStart`'s own KDoc). **That conclusion was wrong, and this entry exists to
  record the correction**: `CursorAnchorInfo`/`requestCursorUpdates` does not expose the layout, but it does
  expose where the caret is *drawn* - and a soft wrap is directly observable there as a jump in the caret's
  own y coordinate. `CursorAnchorInfo` appears twice elsewhere in this file (D-418, and §460's own DPAD
  note), both times dismissed for *drawing an overlay* on the grounds that app support is inconsistent; it
  was never considered as a layout oracle for this gesture.

  **Why a probe and not the rearchitecture.** Rounds §462-§470 were each built on a premise that only failed
  on a real device. The premise here is "Google Keep reports caret coordinates", which is cheap to test and
  expensive to assume, so this round tests only that. `startCursorAnchorInfoProbe()` requests
  `CURSOR_UPDATE_IMMEDIATE or CURSOR_UPDATE_MONITOR` when the gesture arms (logging the editor's own accept/
  decline return), `onUpdateCursorAnchorInfo()` logs every reported position next to this app's own tracked
  text offset - so the log reads as "offset N is drawn at screen point (x, y)" - and
  `stopCursorAnchorInfoProbe()` cancels the request again when the gesture ends, so nothing is requested
  outside an active gesture. A `cursorControlAnchorInfoProbeRunnable` fires after 500 ms without a callback
  and records the negative verdict explicitly, so "no support" reads as a real log line rather than silence.

  **What the log has to answer**: (1) do callbacks arrive at all, (2) are the coordinates real rather than
  `NaN`, (3) does y genuinely change across a soft wrap *inside one paragraph*. If yes, the planned direction
  is a screen-space rearchitecture - drive the caret towards a target *point* rather than a target character,
  with the visual line falling out for free and `stepsFor()`'s dominant-axis gate becoming unnecessary
  (40 px of drift against a ~60 px line height simply targets the same line, geometrically, with no threshold
  involved). A user requirement captured for that round and not to be lost: **the finger-to-caret ratio must
  stay well below 1:1, at least horizontally** - the point of the gesture is precision, and 1:1 would be no
  better than tapping in the text directly. In a screen-space model that is a plain gain factor applied to
  `(dx, dy)`, and calibratable in real screen millimetres for the first time. If the log says no, that whole
  direction is closed and the gesture's line handling gets honestly re-scoped to "jumps between real
  paragraphs" instead.

  1599 unit tests (unchanged - Android-only glue, per this project's own accepted testing gap).
  `:app:assembleDebug`/`:app:testDebugUnitTest` green. `versionCode` 527 -> 528, `versionName` `"1.2.31"` ->
  `"1.2.32"`.

- **§471 (v1.2.31): D-401-followup - complete rearchitecture, prompted by the user directly correcting this**
  **gesture's own mental model rather than reporting another symptom.** After a 7th round still showed
  flipping and a "stuck at the empty line, then stuck at the left edge - unbenutzbar" report, the user
  rejected further incremental tuning and instead restated the actual intent from scratch: the caret is
  positioned *directly and absolutely*, never by moving it through the document's own text flow. Dragging
  right must always mean "as far right as the current line allows, however far that is" - never "wander
  further right in the text, wherever that leads." A line change is a separate action, driven only by
  genuinely vertical motion.

  **This reframes the whole D-401 saga (§462-§470) as symptoms of the wrong mental model, not a string of
  unrelated bugs**: the old design tracked an *incremental* running caret offset, updated by a signed delta
  each move and clamped after the fact - every round's fix (the DPAD-focus bug, the arm-time seeding bug,
  the empty-line clamp flip-flop, the line-step threshold) patched a real symptom of that model without
  ever addressing that the model itself, not any one clamp or threshold, was the actual source of fragility.

  **Rearchitected around direct, stateless positioning**: `applyCursorControlMove()` no longer accumulates
  characters as a delta at all - `cursorControlOriginColumn` (the column at arm time or the last re-touch)
  plus the gesture's own *total* signed horizontal offset from origin is recomputed and re-clamped to
  whichever line is active on every single move, never accumulated. `CursorControlGesture.stepsFor()`
  gained a dominant-axis gate: a line change (`lines`) is now forced to zero whenever the horizontal
  distance is at least as large as the vertical one, directly preventing the exact real-device shape that
  caused an unwanted line change in §470's own log (514px sideways against only 42px of drift) - a line
  change can now only ever come from motion that is recognisably more "up/down" than "left/right" at the
  moment it fires. `moveOneLine()` (which used to compute and preserve "column" itself) is replaced by a
  simpler `adjacentLineStart()` that only finds the target line's own start; the same direct column
  computation then settles the exact position once the caret has landed there, so column preservation
  across a line change now falls out of the model for free. New `onCursorControlReTouched()` listener
  callback (a real, previously-missing gap) refreshes `cursorControlOriginColumn` on every lift/re-touch,
  not just at arm time, so resuming a drag after a re-touch doesn't snap the caret back to wherever it
  started at the very beginning of the whole gesture.

  The empty-line "stuck" behaviour is unchanged and is now, finally, framed correctly in the spec as
  intentional, not a defect: a zero-width line clamps to a single column in both directions by construction
  (start == end), so a horizontal drag genuinely cannot leave it - only a real line change can, exactly
  matching the user's own restated intent.

  1599 unit tests (was 1596: 3 new `CursorControlGestureTest` cases for the dominant-axis gate).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 526 -> 527, `versionName`
  "1.2.30" -> "1.2.31". Spec (`G-08`) rewritten (not merely amended) for Stage 1 and the calibration
  section, to describe the new model on its own terms rather than as a patch history. Diagnostic logging
  (§465) still kept in place - this is now genuinely new logic, not a re-verification of old logic, so a
  fresh device log matters more than ever for the next round.

- **§470 (v1.2.30): D-401-followup - the sixth log finally isolated the real mechanism: this was never a**
  **clamp bug at all, it was gesture-calibration sensitivity.** §469's revert compiled clean, and this log
  confirms it held: every single `leftBoundary()`/`rightBoundary()` call in the new log correctly clamps
  (`target=107 clampedPosition=108`, never crossing) - the horizontal boundary logic is, and now stays,
  entirely correct. The user's continued "flipping" was coming from a completely different code path this
  whole investigation had not yet looked at closely: `moveOneLine()`, firing on ordinary, mostly-horizontal
  drags that the user never intended as a line change at all.

  **Root cause, measured directly from the log's own numbers, not guessed**: `CursorControlGesture.stepsFor()`
  computes `lines` from vertical distance *accumulated from the gesture's own origin*, independently of how
  large the horizontal distance already is. One log entry shows a line step firing at a cumulative drag of
  `dx=-514, dy=42` - a drag that is overwhelmingly horizontal (514px) with only 42px (16dp) of vertical
  drift, yet `DP_PER_LINE_STEP`'s own 32dp threshold rounds anything past *half* of itself (16dp) up to a
  full line step. 16dp is trivially exceeded by ordinary hand wobble over any drag long enough to cover more
  than a few characters - this is why the flipping tracked with drag length/duration rather than with any
  deliberate vertical intent, and why it survived three straight rounds of clamp-logic changes untouched:
  the clamp was never the mechanism producing it.

  **Fixed by raising `DP_PER_LINE_STEP` from 32f to 200f** (`CursorControlGesture.kt`) - a real, deliberate
  vertical drag (~2 keyboard key-rows tall at the reporting device's own density, per that same log) is now
  required before a line step registers at all, while `DP_PER_CHARACTER_STEP` is untouched: only line
  movement had this failure mode, since an unwanted character move is immediately visible and
  self-correcting on screen, but an unwanted line jump silently teleports the caret somewhere else entirely
  with no visual warning. Existing `CursorControlGestureTest` cases reference the constant symbolically, not
  as a hardcoded magic number, so all pass unchanged against the new value with no test edits needed.

  1596 unit tests unchanged. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 525 -> 526,
  `versionName` "1.2.29" -> "1.2.30". Spec (`G-08`'s own "Calibration status" paragraph) updated with the
  measured numbers and reasoning. This is the first D-401-followup round in this whole saga addressing
  gesture *sensitivity* rather than clamp *logic* - if the flipping persists after this, the next log should
  specifically distinguish "still a boundary/clamp symptom" from "still a spurious line-step symptom" so the
  two failure classes are not conflated again.

- **§469 (v1.2.29): D-401-followup - both empty-line "fixes" (§467, §468) reverted outright; the premise**
  **itself was wrong, not the implementation.** User's fifth log on this exact area: "Es hat sich nichts
  geändert" (nothing changed) - §468's own corrected two-sided `isOnZeroWidthLine()` check compiled, tested
  green, and was logically sound, yet the user still perceived a "flip" on the device.

  **Re-reading the user's own original D-401 requirement, not the log, settled it**: from the very first
  round of this feature, the explicit, repeated call was that a horizontal drag must never cross a line
  boundary under any circumstances ("Ich bewege den Cursor hiermit bereits zweidimensional. Es gibt keinen
  Grund für eine Weiterbewegung im Textfluss."). §467's own diagnosis of the original "sticking" report as a
  bug was the actual mistake - a zero-width line clamping to a single point in both directions, forcing the
  user to leave it via a vertical (line) move rather than a horizontal one, is not stuck at all: it is this
  gesture's own two-independent-dimensions design working exactly as originally specified. §467/§468's own
  "escape" logic was solving a problem that, per the user's own standing requirement, was never actually a
  problem - it was reintroducing the one behaviour (crossing a line horizontally) the user had explicitly and
  consistently ruled out from the start, just via a character move instead of an accidental line move.

  **Fixed by reverting, not patching a third time**: `isOnZeroWidthLine()` deleted outright;
  `leftBoundary()`/`rightBoundary()` restored to their pre-§467 form - `return result` unconditionally, no
  unclamping exception for any boundary value, empty line or not. A blank line now clamps firmly like every
  other line boundary; leaving it requires an explicit vertical move, exactly as G-08 always specified.

  1596 unit tests unchanged. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 524 -> 525,
  `versionName` "1.2.28" -> "1.2.29". Spec (`G-08`) updated to record both reverted attempts and why, so a
  future round does not re-litigate the same "stuck" report as a bug a third time. Diagnostic logging (§465)
  still kept in place - this round's own device confirmation is the next open item.

- **§468 (v1.2.28): D-401-followup - §467's own empty-line fix was itself a real regression, caught on the**
  **very next device test.** User reported: "Jetzt ist es wieder wie vorher... man flippt einfach durch die
  Zeilen" (back to flipping through lines again) - the fourth log in this saga, and it pinpointed the exact
  mistake immediately.

  **Root cause**: §467's own `result == position` check could not tell a genuinely empty line apart from the
  caret simply sitting at the very start or end of an ordinary, *non-empty* line - both produce
  `result == position` when only ONE direction is checked (which is all `leftBoundary()`/`rightBoundary()`
  ever compute per call). The log showed this precisely: at position 109 - the real, content-bearing start of
  a genuine third line - `leftBoundary()` correctly computed `result = 109 = position`, and §467's fix
  unclamped it exactly as it would a true empty line, letting the caret cross straight back into the previous
  (actually empty) line and beyond - the reopened flip.

  **Fixed by actually distinguishing the two cases**, not by another single-direction check: new
  `isOnZeroWidthLine()` reads one character before and one character after the caret directly - true only
  when *both* are a newline (or the field's own start/end respectively). Only then does the boundary
  actually get left unclamped; every ordinary line boundary (content on at least one side) keeps clamping to
  `result` exactly as it did before §466/§467, correctly blocking the flip again. Verified against the same
  device log line by line: position 109 (real line start) is no longer treated as zero-width (character
  after the caret is real content, not a newline) and stays clamped; position 108 (the log's own confirmed
  empty line) still reads as zero-width on both sides and still escapes correctly.

  1596 unit tests unchanged (Android-glue logic only). `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 523 -> 524, `versionName` "1.2.27" -> "1.2.28". Spec (`G-08`) updated to describe the
  corrected two-sided detection and name the regression explicitly, so a future round does not repeat the
  same single-direction mistake. Diagnostic logging (§465) still kept in place - not yet independently
  re-confirmed by the user on device for this specific fix.

- **§467 (v1.2.27): D-401-followup - user confirmed §466's own two fixes both hold on device**
  **("Das hat definitiv geklappt" - chip suppression reliable, checkmark tap ends the mode), plus one more**
  **real bug found from a third device log.** User reported the caret now "sticks stubbornly" at what looked
  like the end of the text block, was hard to move away from, and occasionally "flipped" into the next line
  when finally dragged out.

  **Root cause, found directly in the log, not guessed**: the field's real content contains a genuinely
  empty line (two adjacent newlines - a blank paragraph break). At that exact offset, `leftBoundary()` and
  `rightBoundary()` both resolve to the caret's own current position (an empty line has zero width - there is
  nothing to its left *within the line* and nothing to its right either), so `clampToCurrentLine()` clamped
  every character move straight back to where the caret already was, in *both* directions at once - the log
  showed `target=109 clampedPosition=108` and `target=107 clampedPosition=108` for dozens of consecutive
  moves in a row. This is a real, previously-unconsidered edge case of the D-401-followup no-flip clamp
  (§462/§463): the clamp assumed a line always has positive width to move around within, true for every
  earlier test but not for a blank line. The caret was not "stuck" by any timer or debounce - it was
  mathematically unable to satisfy `target != position` via a character move alone. Escaping only ever
  happened by accident, when the same drag's accumulated vertical distance coincidentally crossed a line-move
  threshold too - which is also exactly the "flip" the user reported, since that vertical move could land
  anywhere, not where a further horizontal drag was actually aimed.

  **Fixed by recognising the degenerate case explicitly**: when a computed boundary equals the caret's own
  current position, `leftBoundary()`/`rightBoundary()` now return `null` (this project's own existing
  "unclamped" signal) instead of that self-referential result - the character move is then applied without
  clamping, crossing into the adjacent line by exactly the drag's own delta, matching what an ordinary text
  editor's arrow key already does from an empty line. The very next move re-reads a fresh boundary from
  wherever it landed, so this does not reopen the original flip bug for any line with real width - only a
  genuinely zero-width line is affected.

  1596 unit tests unchanged (Android-glue logic, no new pure logic - both changed functions are already
  Android-glue-only, per their own existing KDoc). `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 522 -> 523, `versionName` "1.2.26" -> "1.2.27". Spec (`G-08`) updated with this exception.
  Diagnostic logging (§465) still kept in place, one more round - not yet independently re-confirmed on
  device for this specific fix.

- **§466 (v1.2.26): D-401-followup - the real root cause(s), found from §465's own instrumented device log,**
  **not another guess.** The user reproduced the drag again in Google Keep and attached the full log. Two
  genuine, independent bugs, both confirmed directly against the log rather than inferred:

  **Bug 1 - the DPAD-driven line move could leave the field entirely, and this explains all three still-open**
  **symptoms at once, not only the caret "flip".** The log showed `onStartInput`/`onStartInputView` firing
  *mid-gesture*, alternating `hintText=Titel` and `hintText=Notiz` - Google Keep's Title and Body are separate
  `EditText` fields, and `KEYCODE_DPAD_UP` sent at the top of the Body was moving **system input focus** to the
  sibling Title field, not just an imprecise vertical jump within the same field. This is the real reason
  three previously-separate-looking symptoms never resolved despite three rounds of real fixes: (a) the caret
  "flipping" (§462/§463's own line-clamp work only ever guarded *within* one field's own text, powerless
  against a focus change to a different field entirely), (b) the clipboard chip repeatedly reappearing and (c)
  the checkmark chip never staying, both because `onStartInputView` unconditionally calls
  `showClipboardChipIfAvailable()`/`showCredentialSuggestions()` on every field focus - each spurious
  Title/Body bounce silently overwrote the gesture's own checkmark/hint bar content with ordinary field-open
  content. **Fixed at the actual source, not by patching around it**: line movement no longer sends any DPAD
  key event at all. New `moveOneLine()` (`AdaptKeyService.kt`) computes the target line purely from this
  field's own text via `getTextBeforeCursor()`/`getTextAfterCursor()` - the same text-scanning approach
  `clampToCurrentLine()` already used horizontally - finds the previous/next line's start and length by
  scanning for newlines, and preserves the horizontal column as closely as possible; by construction this can
  never move focus to a different view. This also fully removes the entire async-echo-race class §464 was
  fighting: a line move is now exactly as synchronous and self-trusting as a character move already was, so
  `cursorControlAwaitingLineSync` is deleted outright and `onUpdateSelection`'s own resync branch is now
  diagnostic-only (never mutates `cursorControlPosition`). **Belt-and-suspenders**: `onStartInputView`'s own
  credential-list/clipboard-chip calls are now also directly gated on `!cursorControlSessionActive`, so even a
  genuinely different cause of a mid-gesture field-focus event can no longer stomp the bar.

  **Bug 2 - a separate, independent seeding bug**, found by re-reading `onCursorControlArmed()` against the
  log's very first move: `cursorControlPosition` is seeded from `liveSelectionEnd`, which - exactly like
  `selectionCollapsed` already had its own documented D-152 fix for - is only ever updated by
  `onUpdateSelection`, never guaranteed to fire again once a field is simply refocused with an already-existing
  caret position (that initial position is delivered via `EditorInfo` instead). The log's very first
  `applyCursorControlMove` call showed `positionBefore=0` while the field's own `getTextBeforeCursor()` read
  back 67 real characters - the tracked position and the field's real caret were never the same value to begin
  with, so the gesture's first move warped the real caret to a wrong location before any drag-clamping logic
  even ran. Fixed the same way D-152 was: `onStartInput` now seeds `liveSelectionEnd` from
  `info.initialSelEnd` (falling back to 0 only when the field reports none) instead of leaving it stale from
  whatever the *previous* field's last callback happened to report.

  1596 unit tests unchanged (Android-glue logic, no new pure-logic units). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 521 -> 522, `versionName` `"1.2.25"` -> `"1.2.26"`. Diagnostic
  logging (§465) deliberately kept in place one more round, not yet stripped - not independently re-confirmed
  by the user on a real device yet. Spec (`G-08`) updated to describe the computed line move and the arm-time
  seeding fix.

- **§465 (v1.2.25): D-401-followup - diagnostic-only round, no fix attempted.** User reported v1.2.24 (§464's**
  **own async-echo-race fix) STILL produced "no observable change at all"** - the third consecutive report of
  that exact phrasing across three logically-reasoned, verified-compiling fixes (§462's tap/reclaim/clamp
  attempt, §463's anchor-comparison rewrite, §464's echo-race fix), this time accompanied by a real device
  log (Google Keep) showing chaotic, non-monotonic `onUpdateSelection` position jumps during a cursor-control
  drag that don't match any hypothesis formed by reading the code alone.

  Per this project's own established convention (root-cause from real device data, never guess repeatedly -
  the same discipline D-452's own stall investigation already used successfully), stopped attempting a fourth
  blind fix and instead added comprehensive diagnostic logging at every point in the pipeline a real log could
  distinguish between competing explanations: `AdaptKeyboardView.handleCursorControlTouch()`'s ACTION_MOVE
  branch now logs raw `dx`/`dy`/density, the computed `Steps`, the applied-before counters and the resulting
  delta, and the active stage (`AdaptKeyTouch`); `AdaptKeyService.applyCursorControlMove()` logs its own entry
  parameters and the position both before and after clamping; `leftBoundary()`/`rightBoundary()` log which
  branch was taken, the read text length, the found newline index (or its absence) and the computed result;
  and `onUpdateSelection()`'s own resync decision now logs explicitly which of its two outcomes fired -
  `"line-sync resync"` (the one-shot flag consumed) vs. `"echo ignored"` (an echo arriving while not awaiting
  one) - so a fresh log can show directly whether §464's own guard is actually preventing echo-driven
  stomping, or whether the chaos has some other, still-unidentified source entirely (all tagged
  `AdaptKeyJitter`, this project's own established tag for this exact historically-fragile race class).

  1596 unit tests unchanged (diagnostic Android-glue additions only, no logic changed). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 520 -> 521, `versionName` `"1.2.24"` -> `"1.2.25"`. Deliberately
  no spec-prose update this round - no user-facing behaviour changed. Next step is the user reproducing the
  issue again on this build so a fully-instrumented log can actually explain the chaotic jumps, rather than
  another guess.

- **§464 (v1.2.24): D-401-followup - the real reason §462/§463's own fixes produced no observable change at**
  **all ("es ist als hättest du gar nichts gemacht"), found only after first ruling out a deployment gap.**
  User confirmed the Settings screen's own live `PackageManager`-read version showed `1.2.23` - the fix
  genuinely had reached the device, so the problem was real code, not staleness.

  **Root cause, found by re-reading the whole D-401 caret-tracking mechanism end to end rather than the**
  **clamp function alone again**: `onUpdateSelection` resynced `cursorControlPosition` from its own
  `newSelEnd` **unconditionally** on every single callback while a session was active - originally added so
  a DPAD-driven line jump's own (otherwise uncomputable) resulting offset would be picked up. `onUpdateSelection`
  is asynchronous and its callbacks are **not** guaranteed to arrive in the order a fast drag's own rapid-fire
  `setSelection()` calls were issued in - this project's own extensively-documented history of exactly this
  class of race (spec §1's guiding principle, D-139/D-149) applied here too, just not yet connected to this
  gesture specifically. A stale echo from *before* a clamp had taken effect could land after a newer,
  already-correctly-clamped one and silently stomp `cursorControlPosition` back, so the very next delta's own
  baseline was wrong - defeating §463's own (independently correct) `leftBoundary()`/`rightBoundary()` fix
  completely, on every single character move, not merely at the boundary. This is why nothing appeared to
  change at all, not only the line-clamp.

  Fixed by only ever trusting the echo when actually needed: new one-shot `cursorControlAwaitingLineSync` flag,
  armed right before the DPAD events are sent and consumed only by the very next `onUpdateSelection` call.
  Every character-based move now keeps trusting its own synchronous, already-clamped result unconditionally,
  exactly like it always should have - `cursorControlPosition` is now written from exactly one of two places
  at any given moment: the clamp function itself (character moves), or this one gated echo (line moves), never
  both racing for the same field.

  1596 unit tests unchanged (Android-glue timing behaviour, no new pure logic). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 519 -> 520, `versionName` `"1.2.23"` -> `"1.2.24"`. Not yet
  re-confirmed by the user - the checkmark-visibility fix from §463 is a logically separate mechanism from
  this race and was not obviously implicated by it, but has also not been independently re-verified since.

- **§463 (v1.2.23): D-401-followup - the real bug behind the still-open line-clamp (§462's own fix did not**
  **work), plus the clipboard peek button suppressed inside the gesture, plus a separate pre-existing**
  **clipboard bug flagged (not fixed) for its own investigation.**

  **The line-clamp bug, found by re-reading `leftBoundary()`/`rightBoundary()` line by line rather than**
  **guessing again**: `cursorControlAnchor` is only ever updated at Stage 2's own start (`onCursorControl-
  StageChanged`) - it stays frozen at the gesture's arm-time position throughout the whole of Stage 1, while
  `cursorControlPosition` moves freely as the drag continues. §462's own `position <= anchor`/`position >=
  anchor` check (added specifically to disambiguate Stage 2's live-selection-relative
  `getTextBeforeCursor()`/`getTextAfterCursor()` reads) was silently wrong for Stage 1 the moment the drag
  passed that frozen point in either direction: it fell into the "past the anchor" branch meant for Stage 2,
  found no real selection to read via `getSelectedText()` (Stage 1 is always collapsed), and gave up clamping
  entirely - exactly the reported "still flips" symptom, confirmed as a real logic bug, not merely another
  reliability gap in the underlying `InputConnection` calls. Fixed by threading `stage` through
  `clampToCurrentLine()`/`leftBoundary()`/`rightBoundary()`: Stage 1 now always takes the direct
  `getTextBeforeCursor()`/`getTextAfterCursor()` path unconditionally (correct by construction - the
  selection is never anything but collapsed at `cursorControlPosition` itself there), and only Stage 2 still
  compares against the anchor to decide. Not yet re-confirmed by the user at time of writing.

  **The checkmark chip's own remaining disappearance, traced to a second competing UI element**: the D-36
  clipboard-peek button (`clipboardPeekButtonView`, a dedicated square next to the bar, not a suggestion
  chip) is shown by `setSuggestionBarItems()` - the same single choke point `showCursorControlHint()` itself
  calls - whenever the bar's own content isn't already clipboard chips and the clipboard holds something
  peek-worthy, entirely independently of §462's own `showSuggestions()`/`refreshSuggestions()` gates (neither
  of which this button's visibility ever went through). With clipboard content present, the button reappeared
  right alongside the gesture's own checkmark/hint on every `showCursorControlHint()` call, and tapping it
  (`openClipboardPeek()`) replaced the bar with clipboard chips - the user's own precise report ("weil die
  Chips dort immer wieder eingeblendet werden"). Fixed at that same choke point: the peek button (and, by the
  same reasoning, nothing new needed for `clearClipboardButtonView`, which only ever shows *for* clipboard
  chips already being displayed) now also stays hidden for as long as `cursorControlSessionActive` is true.

  **A separate, pre-existing clipboard bug flagged, not fixed**: tapping the peek button while the caret
  touches an existing word shows the clipboard chips only briefly before the word gets reclaimed and the
  chips disappear again - explicitly reported as reproducing **outside** this gesture too ("das passiert
  völlig unabhängig von dem neuen Tool"), and confirmed here not to trace to anything touched this session
  (`cursorControlSessionActive` is false throughout ordinary use, so none of this round's new gates apply).
  `openClipboardPeek()`'s own KDoc already documents a `reclaimChipRefreshSuppressedUntil` window
  (`RECLAIM_DEBOUNCE_MS` + `CLIPBOARD_PEEK_ECHO_GUARD_MARGIN_MS`, 350 ms total) built specifically for "the
  chips flashed and immediately vanished again" - the identical symptom now reported again, either a
  regression of that existing fix or a related race it does not fully cover. Left open rather than guessed at
  - needs its own real device log to root-cause properly, per this project's own convention, not attempted
  blind in the middle of an already-large round.

  1596 unit tests unchanged. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 518 -> 519,
  `versionName` `"1.2.22"` -> `"1.2.23"`.

- **§462 (v1.2.22): D-401-followup - four real, device-reported problems with the cursor/selection-control**
  **gesture fixed the same session it first went out, across two feedback rounds.** All from direct usage
  ("das funktioniert schon außerordentlich gut" overall) - none guessed at, each with a concrete root cause;
  two of the four (the checkmark chip and the line-clamp) only reached their real fix on a second report,
  once the first attempt turned out incomplete.

  1. **"Tippen beendet" (a tap ends the mode) did not actually work in Stage 2 - "ich kann den Modus nur**
     **durch warten beenden."** Root cause, confirmed by re-reading the code rather than assumed: the original
     tap check ("zero movement since this touch-down began") was essentially unreachable in practice -
     reaching Stage 2 at all, and extending a selection there, both already require dragging, so by the time
     a real touch lifts its own applied-step counters are almost never still zero. Fixed by dropping the
     zero-movement requirement entirely: **any** release while Stage 2 is active now ends the mode and
     collapses the selection - `AdaptKeyboardView`'s own `onCursorControlTap()` callback renamed to
     `onCursorControlSelectionReleased()` to match what it now actually means. Stage 1 releases are
     unaffected - still the ordinary re-touch grace window. Confirmed working on the user's own re-test.
  2. **No reclaim, no suggestion-chip computation while the gesture is active - explicit user request**
     **("das verwirrt und macht die Sache nicht schneller und kann auch dazu führen, dass ungewollt Wörter**
     **verstümmelt werden").** First pass only gated `scheduleReclaimAndChipRefresh()` (the D-62 reactive-
     reclaim dispatcher, already known from D-347/D-350 to mis-fire on many fast intermediate caret positions
     in quick succession - a cursor-handle drag being the exact precedent case) behind a new
     `cursorControlSessionActive` check. Confirmed working on the user's own re-test ("das hat geklappt") -
     but a second, independent problem surfaced alongside it (item 4 below), fixed with the same field.
  3. **Dragging the cursor to the start/end of a line and continuing must not flip to the previous/next**
     **line - explicit user request: "Ich bewege den Cursor hiermit bereits zweidimensional. Es gibt keinen**
     **Grund für eine Weiterbewegung im Textfluss."** A plain absolute-document-offset `setSelection()` call
     naturally crosses a real newline once the target goes far enough, which is exactly the flip the user does
     not want given vertical movement is already its own separate, deliberate DPAD step (spec G-08).
     **First attempt (reverted): a `clampToCurrentLine()` reading a bounded window via `getExtractedText()`**
     - still crossed the boundary on the user's own re-test ("das Flippen des Cursors im Textflow ist
     immernoch da"), described precisely down to the direction ("flippt der Cursor nach rechts in die Zeile
     darüber"). `getExtractedText()`'s real-world reliability across arbitrary third-party apps was the
     suspected weak point (a silent `null`/failed read there fell back to no clamping at all, indistinguishable
     from the reported symptom) - this app already has a proven, simpler mechanism
     (`getTextBeforeCursor()`/`getTextAfterCursor()`, `flipSignBeforeCaret()`'s own precedent) that was not
     used the first time specifically because of Stage 2's own selection-relative reading ambiguity (these two
     calls read relative to the *live selection's* start/end, not necessarily the gesture's own moving end,
     once Stage 2 drags that end backward past its own anchor). **Rewritten**: `leftBoundary()`/
     `rightBoundary()` now check only the one boundary actually at risk for the current `characterDelta`'s own
     sign (never both at once), using the direct, proven calls whenever the moving end is on the expected side
     (always true in Stage 1, where anchor and position coincide) and falling back to searching
     `getSelectedText()` - the span between anchor and position - for the rarer case where Stage 2 has
     already dragged past the anchor. Not yet re-confirmed by the user at time of writing.
  4. **The new "done" checkmark chip (added in response to the user's own mid-session suggestion, see below)**
     **was only ever visible right when the gesture armed, then disappeared during use - "ich kann ihn also**
     **nicht drücken, um zu beenden."** Root cause, traced (not guessed) once reported: `resortRunnable`
     (S-04's deferred re-sort) and `reclaimEnabledRunnable` (D-414-followup's own debounced chip-visibility
     refresh) both call `showSuggestions()` **directly**, bypassing `refreshSuggestions()` entirely - neither
     was covered by item 2's own gate, so a resort/reclaim-chip-refresh left pending from typing right before
     the long-press would silently overwrite the gesture's own bar content once its own delay elapsed
     mid-gesture. Fixed at the one real rendering choke point both of them (and everything inside
     `refreshSuggestions()`) actually share: `showSuggestions()` itself now also gates on
     `cursorControlSessionActive` - `showCursorControlHint()` never reaches it at all (pushes directly via
     `setSuggestionBarItems()`), so this cannot suppress the gesture's own content, only every other, now-stale
     caller. `onCursorControlArmed()` additionally now cancels `resortRunnable`/`reclaimEnabledRunnable`/
     `reclaimWordAtCaretRunnable`/`expensiveSuggestionRunnable` outright (the user's own "keine Chips berechnet
     werden" taken literally - avoids the now-pointless background computation entirely, not merely its
     display). Not yet re-confirmed by the user at time of writing.

  **Also added, on the user's own follow-up suggestion mid-session**: a green checkmark chip
  (`SuggestionController.Kind.CURSOR_CONTROL_DONE`, `AdaptKeyboardView.dismissCursorControl()`) pinned first
  in the suggestion bar's own hint slot in both stages - an explicit, discoverable way to end the gesture on
  tap, on top of (not instead of) the gesture-only fixes above. Reuses the same green already used for the
  Stage 2 crosshair (`suggestion_learned_text`). See item 4 above for why the first version of this chip did
  not actually stay usable.

  1596 unit tests unchanged (every fix this round is Android-glue behaviour - `CursorControlGesture` itself
  untouched). `:app:assembleRelease`/`:app:testDebugUnitTest` green throughout. `versionCode` 517 -> 518,
  `versionName` `"1.2.21"` -> `"1.2.22"`. Items 1/2 confirmed working; items 3/4 (the reworked line-clamp and
  the checkmark-visibility fix) not yet re-confirmed - the calibration constants from §460 also remain
  untested.

- **§461 (v1.2.21): D-401-followup - `cursorControlEnabled` flipped to default ON.** User's own explicit
  call, right after §460 shipped it default-off: genuine accidental activation is unlikely (G-01's own
  space-bar language switch is a plain swipe, not a long-press, so the two gestures never actually compete
  the way the original off-by-default reasoning worried about) and a useful feature should default to
  discoverable rather than hidden behind an opt-in the user has to go find first. Flipped in all four places
  a new boolean setting's default lives in this codebase: `AdaptSettings.cursorControlEnabled`,
  `SettingsMapper.RawSettings.cursorControlEnabled`, `SettingsStore.DEF_CURSOR_CONTROL_ENABLED`, and
  `settings_preferences.xml`'s own `android:defaultValue`. Spec G-08 updated to match. No test/behaviour
  change beyond the default itself - 1596 unit tests unchanged, `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 516 -> 517, `versionName` `"1.2.20"` -> `"1.2.21"`.

- **§460 (v1.2.20): D-401 - the space-bar cursor/selection-control gesture, implemented end to end from**
  **history §276's own verbatim four-stage capture.** New opt-in setting (`cursorControlEnabled`, C-01-style
  toggle in the Key Behaviour category, default **off** - a new, screen-consuming gesture on the app's own
  most-tapped key needs an explicit opt-in, not a risk of accidental activation during ordinary typing).

  **Design discussed first, per this project's own convention**, resolving six real open points before any
  code: (1) the drag-distance-per-character/line calibration - explicit user call to start from Gboard's own
  known feel rather than inventing one, expecting it to change once tried for real; (2) horizontal *and*
  vertical movement both in scope from the start, not horizontal-only; (3) **a currently-composing word is
  deliberately left completely untouched when the gesture arms** - the user's own explicit call, reasoning
  through it directly: committing it first (matching every other long-press action, L-05/L-06) risked an
  unwanted autocorrect firing before the user gets a chance to reject it, and the free-cursor-move-while-
  composing case is already handled identically to any other external caret change (D-313/D-406) with no new
  special case needed; (4) the two tap-semantics questions (Stage 1 tap; where a Stage 2 tap's collapse
  lands) resolved by this session's own judgement, per the user's explicit "arbeiten mit deinen Vermutungen";
  (5) the platform's own text-selection context menu at timeout - accepted as "probably automatic, verify on
  a real device" rather than adding an explicit, uncertain trigger call; (6) a dedicated Settings toggle,
  confirmed worth having.

  **Mechanism.** New pure `keyboard/CursorControlGesture` object (mirrors `BackspaceRepeat`'s own split -
  stateless stage/timing/geometry constants and a `stepsFor(dx, dy, density)` pure function; all running
  state lives on the Android side) plus 7 new JUnit5 tests. `AdaptKeyboardView` gained the entire touch state
  machine: `scheduleLongPress()` now also arms for `KeyCode.SPACE` when `cursorControlEnabled` is set
  (`KeyboardLayout.hasLongPressAction()` itself stays unaware of this feature-toggle-gated case, deliberately
  kept pure); once armed, `onTouchEvent()` routes every subsequent motion event for that touch session to a
  dedicated `handleCursorControlTouch()` instead of threading more conditionals through the already
  finely-tuned ordinary key-press handling - re-touch within the 1000ms lift-grace window is recognised
  before ordinary key resolution even runs, anywhere on the keyboard, not only the space key. 30% key-dimming
  is one `Canvas.saveLayerAlpha()` wrapped around the existing `drawKeys()` call (never touches the shared,
  reused per-key `Paint` objects themselves); the crosshair reuses the app's own established accent-blue
  (Stage 1) and "confirmed/active" green (Stage 2, `suggestion_learned_text`) colours rather than introducing
  new ones, and its lift-grace fade-back-to-centre is an ordinary `ValueAnimator`, the same mechanism the
  space-bar language-change label fade already uses.

  A new `AdaptKeyboardView.OnCursorControlListener` (armed / move / stage-changed / tap / ended) carries the
  gesture to `AdaptKeyService`, which applies it: a character delta calls `InputConnection.setSelection()`
  directly (collapsed for Stage 1, against a frozen anchor for Stage 2); a line delta is sent as synthetic
  `KEYCODE_DPAD_UP`/`DOWN` key events instead of a computed offset - this app has no reliable way to know a
  target field's real line-wrap layout across every app (the same reliability gap already named for
  `CursorAnchorInfo` elsewhere), so the target app's own text layout decides what "one line up" means, the
  same way a hardware d-pad already would. The two are deliberately never combined in the same tick (the
  absolute offset after a DPAD jump is unknown here until a fresh `onUpdateSelection` reports it back), a
  documented, imperceptible-at-real-drag-speed simplification, not an oversight. A new `liveSelectionEnd`
  field, updated unconditionally in `onUpdateSelection` exactly like the existing `selectionCollapsed` already
  is, seeds and continuously resyncs the gesture's own optimistic running caret offset - added, not
  substituted, so nothing about the existing D-139-flagged reactive machinery there changed.

  Suggestion-bar hint text uses a new `SuggestionController.Kind.CURSOR_CONTROL_HINT`, the same "built outside
  `SuggestionController`, pushed directly" shape `LOADING`/`EMOJI_SEARCH_QUERY` already use. Explicitly
  disabled while D-317 emoji search owns the keyboard (`enterEmojiSearch()`/`exitEmojiSearch()`, plus
  `applySettings()`'s own `&& !emojiSearchActive` guard against a settings reload mid-search re-enabling it) -
  the identical "nothing from an alternate input mode may reach the real document" guarantee that function's
  own class KDoc already documents for every other listener.

  New spec §4 requirement G-08. 1596 unit tests (1589 -> 1596, +7 new, all in `CursorControlGestureTest`).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 515 -> 516, `versionName` `"1.2.19"` ->
  `"1.2.20"`.

  **Not yet device-confirmed** - this is a genuinely new touch/gesture mechanism with real ergonomic
  unknowns (the calibration constants above, the context-menu-at-timeout behaviour) that only a real device
  can actually settle; the user's own next step is trying it and reporting what needs to change.

- **§459 (v1.2.19): D-452-followup - diagnostic-only, no behaviour change.** A real device log the user sent
  (see §459's own D-452 backlog entry above for the full narration) narrowed the still-open "recurring
  performance problem" to a ~1.3s gap sitting inside `refreshSuggestions()`, between the two points §451
  already instrumented - both fast in this log too, confirming the stall is genuinely elsewhere in that
  function. Traced through the code (not guessed) to the strongest remaining suspect: `ambiguousCasingChips()`
  calls `dictionaryStore.partsOfSpeech()` once per candidate, each doing two uncached SQLite queries
  (`SqliteDictionaryStore.entryOf()`), up to 24 synchronous main-thread round-trips for a 12-candidate case -
  right at the exact moment the background deferred/expensive-fallback search (D-160/D-208/D-211) hands its
  result back for re-entry. The same "redundant per-candidate queries" cost class D-207-D-221 already fixed
  once elsewhere, apparently never covered for this specific, later-added (D-404-followup) call site.

  Three new temporary timers added exactly where the gap was narrowed to, mirroring §451's own diagnostic
  style: `ambiguousCasingMs` (wraps the suspect call directly), `extrasMs` (the split/raw-coordinate/
  autocorrect-chip/speed-unit/missed-backspace block), `pendingMs` (the capitalised-preview computation), and
  `tier3InlineMs` (the `!tier3Async` inline `tier3.predict()` call, verifying its own "the orchestrator is
  instant" comment directly rather than trusting it). All four logged via the same `diag("AdaptKeySuggest",
  ...)` channel §451/§452 already use.

  **Not yet device-confirmed** - the evidence is a traced, plausible code path, not a live measurement; per
  this project's own diagnosis convention, nothing was changed behaviourally and no fix was attempted before
  a real number confirms it. Waiting on the user's next captured log from an actual repro (they will report
  back once it happens again).

  1589 unit tests unchanged (diagnostic-only, no new logic branch worth a dedicated test - same as §451's own
  first round of timers). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 514 -> 515,
  `versionName` `"1.2.18"` -> `"1.2.19"`.

## Older Rounds (§1-§458, v0.7.6 through v1.2.18) - Pruned From This File

D-462 (§483): thirty-first pruning pass - §458 removed, cutoff moved to §459, keeping the working
set at 25 rounds. Backfilled into History.md first, token-count verified.

D-467 (§482): thirtieth pruning pass - §457 removed, cutoff moved from §457 to §458, keeping the
working set at 25 rounds (§458-§482). Backfilled into History.md first, token-count verified.

D-466 (§481): twenty-ninth pruning pass - §456 removed, cutoff moved from §456 to §457, keeping the
working set at 25 rounds (§457-§481). Backfilled into History.md first and token-count verified.

D-465 (§480): twenty-eighth pruning pass - §455 removed, cutoff moved from §455 to §456, keeping the
working set at 25 rounds (§456-§480). Backfilled into History.md first and token-count verified.

D-463 (§479): twenty-seventh pruning pass - §454 removed, cutoff moved from §454 to §455, keeping the
working set at 25 rounds (§455-§479). History.md was backfilled with §454 first, verified at an exact
token-count match, using the corrected extraction the pass below documents.

D-461 (§478): twenty-sixth pruning pass - §447-§453 removed, cutoff moved from §447 to §454, bringing the
working set back to exactly 25 rounds (§454-§478) from 31. History.md was current only through §449, so
§450-§453 were backfilled first (verbatim, reformatted from this file's own bullet shape into History.md's
heading+paragraph style, exactly the method every earlier pass used), then removed here. The backfill was
verified by token-counting each round's Progress.md block against its new History.md entry - all four came
back at delta 0, so nothing was summarised or dropped. Worth recording because two earlier attempts in this
same pass silently lost content and had to be reverted: these rounds' titles are wrapped across lines as
*two adjacent bold runs*, and a naive "take the first `**...**`" title extraction swallowed real body text
into the heading. Any future backfill script must consume consecutive bold runs while they sit in the same
paragraph, and must stop at the first blank line - otherwise it eats the round's own first sub-heading
(`**D-455**: ...`) instead.

D-401-followup (§471): twenty-fifth pruning pass - §446 removed (already logged verbatim in History.md),
cutoff moved from §446 to §447, keeping the working set at 25 rounds (§447-§471).

D-401-followup (§470): twenty-fourth pruning pass - §445 removed (already logged verbatim in History.md),
cutoff moved from §445 to §446, keeping the working set at 25 rounds (§446-§470).

D-401-followup (§469): twenty-third pruning pass - §444 removed (already logged verbatim in History.md),
cutoff moved from §444 to §445, keeping the working set at 25 rounds (§445-§469).

D-401-followup (§468): twenty-second pruning pass - §443 removed (already logged verbatim in History.md),
cutoff moved from §443 to §444, keeping the working set at 25 rounds (§444-§468).

D-401-followup (§467): twenty-first pruning pass - §442 removed (already logged verbatim in History.md),
cutoff moved from §442 to §443, keeping the working set at 25 rounds (§443-§467).

D-401-followup (§466): twentieth pruning pass - §441 removed (already logged verbatim in History.md), cutoff
moved from §441 to §442, keeping the working set at 25 rounds (§442-§466).

D-401-followup (§465): nineteenth pruning pass - §440 removed (already logged verbatim in History.md), cutoff
moved from §440 to §441, keeping the working set at 25 rounds (§441-§465).

D-401-followup (§464): eighteenth pruning pass - §439 removed (already logged verbatim in History.md), cutoff
moved from §439 to §440, keeping the working set at 25 rounds (§440-§464).

D-401-followup (§463): seventeenth pruning pass - §438 removed (already logged verbatim in History.md),
cutoff moved from §438 to §439, keeping the working set at 25 rounds (§439-§463).

D-401-followup (§462): sixteenth pruning pass - §437 removed (already logged verbatim in History.md), cutoff
moved from §437 to §438, keeping the working set at 25 rounds (§438-§462).

D-401-followup (§461): fifteenth pruning pass - §436 removed (already logged verbatim in History.md), cutoff
moved from §436 to §437, keeping the working set at 25 rounds (§437-§461).

D-401 (§460): fourteenth pruning pass - §435 removed (already logged verbatim in History.md), cutoff moved
from §435 to §436, keeping the working set at 25 rounds (§436-§460).

D-452-followup (§459): thirteenth pruning pass - §434 removed (already logged verbatim in History.md), cutoff
moved from §434 to §435, keeping the working set at 25 rounds (§435-§459).

D-385-followup (§458): twelfth pruning pass - §433 removed (already logged verbatim in History.md), cutoff
moved from §433 to §434, keeping the working set at 25 rounds (§434-§458).

D-442-followup (§457): eleventh pruning pass - §432 removed (already logged verbatim in History.md), cutoff
moved from §432 to §433, keeping the working set at 25 rounds (§433-§457).

D-397 (§453): seventh pruning pass - §425-§428 removed (all four already logged verbatim in History.md, no
backfill needed), cutoff moved from §425 to §429, keeping the working set at 25 rounds (§429-§453).

D-391 (§454): eighth pruning pass - §429 removed (already logged verbatim in History.md), cutoff moved from
§429 to §430, keeping the working set at 25 rounds (§430-§454).

D-403/D-359-followup (§455): ninth pruning pass - §430 removed (already logged verbatim in History.md),
cutoff moved from §430 to §431, keeping the working set at 25 rounds (§431-§455).

D-356 (§456): tenth pruning pass - §431 removed (already logged verbatim in History.md), cutoff moved from
§431 to §432, keeping the working set at 25 rounds (§432-§456).

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
same way, without summarising them - they stay permanently retrievable in History.md. **2026-09-06 sixth
pruning pass (§392-§424 removed, cutoff moved from §392 to §425):** had grown to 58 kept rounds (§392-§449),
well past the ~20-25 target - History.md was behind by 12 rounds (current only through §437, this file had
already accumulated to §449), so §438-§449 were backfilled first (verbatim, reformatted from this file's own
bullet-list shape into History.md's heading+paragraph style exactly as the third pass below already
established the method, no content summarised or dropped), *then* this pass removed §392-§424. Several of the
removed rounds (§392-§399, §405-§409 - the D-361/D-396 haptics-and-Backspace-zone rounds) still carried an
inline "not yet device-confirmed" caveat, but both `D-`numbers are independently tracked as RESOLVED and
device-confirmed in "Open TODOs" above (`D-361 - RESOLVED and device-confirmed (§391-§399, ...)`, `D-396 -
RESOLVED and device-confirmed (§405-§409, ...)`) - the same "the tag is not a standing exemption this far
back, once the D-number is independently confirmed elsewhere" precedent every pass below already established.
**2026-09-04 fifth
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
