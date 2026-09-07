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
  - **D-356 - RESOLVED (§456, v1.2.16).** A typed umlaut should not be carelessly reverted by autocorrect -
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

- **D-452 - REOPENED, still not root-caused - timing diagnostics added (§451, v1.2.11).** Originally closed
  WON'T FIX as not reproducible; the user later sent a real device log describing it as "das immer
  wiederkehrende Performance-Problem" (the recurring performance problem), captured incidentally while typing
  in Gemini. That specific log showed no smoking gun - `showSuggestions()` fired redundantly 2-4x in a row for
  an identical, already-empty result around a field restart, but that call does no dictionary work and cannot
  plausibly cost anything perceptible; the multi-second gaps between logged events show no activity at all
  during them, consistent with either a genuine user pause or a real, still-unlogged stall - not
  distinguishable from the log alone. Per this project's own diagnosis convention, nothing was guessed at or
  changed based on that log alone. See §451: real wall-clock timing (`tookMs=`) was added to
  `refreshSuggestions()`'s own dictionary-lookup call and to `showSuggestions()`'s total duration - the next
  captured log, ideally taken at the exact moment something feels slow, should show directly whether either
  is the real cost or whether the stall (if real) lies somewhere else entirely.

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

## Current State

- **§456 (v1.2.16): D-356 - a literally-typed umlaut now breaks an autocorrect tie in its own favour,**
  **finally closed with a real, concrete repro after being open since §277.** Typing `"gedrücjz"` (a genuine
  `ü`, intending `"gedrückt"`) was silently autocorrected to `"gedruckt"` instead - both real German words,
  both correctly offered as chips, but the wrong one silently applied.

  **Root cause, confirmed directly in code, not guessed**: `DictionarySuggestionProvider.correctionCost()`
  folds *both* the typed token and every candidate (umlaut/ß -> ASCII) before computing edit distance - the
  mechanism D-12/D-28 need so a diacritic-free typing (`"grun"`) still finds `"grün"` at zero cost. Folding
  both sides means `"gedrückt"` (keeps the real `ü`) and `"gedruckt"` (discards it) become edit-cost-
  *identical* to the folded token - the fact that a real `ü`, not a plain `u`, was actually typed is folded
  away before it can ever count. Tied on cost, the ranking fell through to raw frequency alone, and the more
  common but unrelated word ("printed" vs. "pressed") won.

  **Design discussed directly before implementing** (per this project's own convention): agreed a tie-break,
  not a change to the primary cost model, is the right scope - see spec §45 for the full write-up including
  the deeper "keyboard-reachability cost baked into the primary model" alternative the user raised and both
  of us agreed to defer (recorded as a Reserve Idea above, not implemented - real but currently-hypothetical
  benefit, real risk of touching `CorrectionConfidence`'s own calibrated regression corpus for it). One real,
  useful side-confirmation surfaced while designing it: `Umlaut.foldToHostKey()`/`foldVariants()` (D-204)
  already fold `ß` to a bare `s` for exactly the same "how do you reach it on the keyboard" reasoning this
  fix needed - the user's own direct check confirmed the mechanism already existed, and confirmed a
  whole-string (not per-character) comparison handles `ß`'s two-character fold (`"ss"`) cleanly with no
  special case, precisely because it never tries to align characters 1:1 itself.

  **Mechanism.** New `DictionarySuggestionProvider.literalDistance()` - the same weighted-distance shape
  `correctionCost()` already uses (adjacent-key/other substitution costs), just unfolded on both sides, and
  unbounded (no `maxCost` band - `EditDistance.weightedDistance()` already defaults to the exact, unbounded
  distance when omitted, needed here since a literal umlaut mismatch can cost more than the folded search's
  own tight ceiling). `CandidateCost` gained a `literalCost` field, computed once per already cost-filtered
  candidate (not inside the comparator, which would otherwise re-run the DP on every pairwise comparison);
  `bestCorrection()`'s own `minWithOrNull` now sorts by `compareBy({ cost }, { literalCost }, { -score })` -
  the new key sits strictly between the existing two, so it only ever reorders candidates already tied on
  the primary (folded) cost, structurally unable to change which candidate wins when costs genuinely differ.

  New `DictionarySuggestionProviderTest` case: the exact reported pairing, with `"gedruckt"` deliberately
  given a *much higher* frequency than `"gedrückt"` (5,000 vs. 20) to confirm the tie-break genuinely beats
  frequency, not merely happens to agree with it once tried.

  1587 unit tests (1586 -> 1587, +1 new). `:app:assembleRelease`/`:app:testDebugUnitTest` green - every
  existing `CorrectionConfidence`/`AutocorrectAggressiveness` regression case (Ohren/Ihren, ddr/der, due/die,
  komplett, ...) still passes unchanged, confirming the tie-break never touched their own outcomes.
  `versionCode` 511 -> 512, `versionName` `"1.2.15"` -> `"1.2.16"`.

- **§455 (v1.2.15): D-403/D-359-followup - a confirmed revert-retry (A-07) was not actually protected**
  **against §6 capitalisation, only against dictionary substitution.** Found while the user was chasing a
  different bug, with a real device log to root-cause it - not guessed: typing `"abt"` auto-capitalised to
  `"Abt"` (a real `NOUN,PROPER_NOUN` dictionary entry - §6 rule 3 fires unconditionally for a pure noun,
  confirmed directly against `dictionaries/de/dict.tsv`), reverted via the existing A-07 mechanism - the very
  next retry of `"abt"` was silently re-capitalised to `"Abt"` again, every single time. The log's own
  `suppressAutocorrect=true`/`autocorrected=null` lines proved this was never the dictionary-correction path
  re-firing at all (that side was correctly and consistently bypassed, on every attempt, for an unrelated
  reason - D-106 stage 2's cross-language protection) - `capitalisation.capitalise()` itself, called
  unconditionally at `finalizeAndCommit()`'s own line regardless of `revertConfirmed`, was simply never part
  of the "every correction mechanism is bypassed for this one retry" promise D-403/D-359 originally made.

  Discussed directly before fixing (per this project's own convention): is this the deliberate, documented
  §6 behaviour (D-405 explicitly scoped rule 1's own symmetric "explicit input wins" protection to the
  sentence-start mechanism alone, leaving pure/proper-noun capitalisation deliberately unconditional), or
  should the revert-protection's own promise now extend to cover it too? Explicit user answer: yes, treat it
  exactly like an autocorrect.

  **Fix.** Every substitution mechanism already forces `corrected == typed` whenever `revertConfirmed` is
  true (each is individually gated on `suppressAutocorrect`/`revertConfirmed`), so `finalWord` now commits
  `typed` verbatim in that case, skipping `capitalisation.capitalise()` entirely - one line change
  ([AdaptKeyService.kt:4272](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:4272)),
  mirroring how a case-locked word already bypasses "autocorrect, capitalisation (§6) and single-word
  correction entirely" (G-05) for the identical "the user has hand-finished this" reason. No new test - this
  is `AdaptKeyService`'s own Android-glue commit path, untested by this project's own established convention;
  `revertConfirmed`/`suppressAutocorrect`'s own derivation (pure logic elsewhere) was already covered.

  1586 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  510 -> 511, `versionName` `"1.2.14"` -> `"1.2.15"`.

- **§454 (v1.2.14): D-391 - cross-word fusion across a spurious space, generalising A-06 beyond its own**
  **narrow, tap-evidence-gated scope.** Real motivating example, discussed and designed with the user before
  implementing (per this project's own convention for non-trivial correction mechanisms): typing
  `"Ar eitstag"` - neither `"Ar"` (a real, if obscure, German word) nor `"eitstag"` (nonsense) makes sense
  alone, but inserting `"b"` between them spells the everyday compound `"Arbeitstag"`.

  **Design decisions made explicitly with the user, in order:** (1) a genuine single-word fusion (both
  original words replaced by one), not merely repairing the right-hand word the way A-06 already does; (2)
  no suggestion-chip mode at all - by the time enough evidence exists (the second word already committed), a
  chip is already too late, so the mechanism is either silent or nothing; (3) the connector-letter set is
  derived from the *active layout* (reusing D-397's `RowGeometry`), not a fixed character list - "es macht
  viel mehr Sinn, das aus dem Layout auszulesen"; (4) reach is deliberately just one word back, never a
  deliberate re-edit trigger - "damit wächst auch die Gefahr für falsche Treffer... halten wir es etwas
  konservativer"; (5) its own dedicated setting (C-25), placed directly beneath C-21, rather than folding
  into C-21 or C-22 - the user's own first instinct was reusing C-21, then agreed a separate axis was
  cleaner once the risk-profile difference (rewriting already-committed text) was named directly; (6)
  learning reversal is symmetric and derived from the same validity check that gates the fusion itself - a
  fragment that independently resolves as a real word (like `"Ar"`) is never touched, only a genuinely
  nonsensical one that had accumulated real learning progress is un-taught; (7) full A-07 undo, exactly like
  a split.

  **Mechanism.** `RowGeometry` (D-397) - already the shared row/column model - now also backs `TokenRepair`'s
  own `spaceRowLetters` (replacing the old, QWERTZ-hardcoded `OVER_SPACE_LETTERS` companion constant, which
  was wrong for AZERTY/Greek/Cyrillic layouts and even missed `y`/`z` for QWERTZ/QWERTY themselves - folded
  into this same change per the user's own explicit call, rather than left for a separate pass that would
  touch `TokenRepair` twice). New `TokenRepair.tryFuseAcrossSpace(previousWord, currentToken)` tries every
  connector letter, resolves each fused candidate through the same diacritic-aware lookup A-05's own halves
  use, and returns the highest-scoring real match as a new `FusionCandidate(fused, confidence)`.

  New `MergeConfidence` (mirrors `CorrectionConfidence`'s own noun/non-noun reference-frequency split,
  D-227's finding reused for the identical reason) scores the fused candidate's own frequency - deliberately
  higher reference points than `CorrectionConfidence`'s own (300/8,000 vs. 25/2,000), since there is no
  edit-cost signal here and a wrong fusion rewrites already-finished text. New `AutoMergeAggressiveness`
  (Cautious/Medium/Aggressive, thresholds 0.90/0.75/0.55) mirrors `AutocorrectAggressiveness`'s shape but is
  its own enum/setting (C-25, `d391_auto_merge_aggressiveness`) - **defaults to off** (`SettingsMapper.
  toAutoMergeEnabled`'s fail-closed direction, the deliberate opposite of C-22's fail-open default), shown as
  a `LabeledSeekBarPreference` directly beneath C-21 in the Capitalisation settings category.

  `AdaptKeyService.finalizeAndCommit()` tries the fusion right after A-06's own block, gated on
  `settings.autoMergeEnabled` and the same `suppressAutocorrect` every other silent-correction mechanism here
  already respects. New `applyFusion()` (mirrors `applySplit()`) is the one function in this class that
  reaches *backward* past the composing token's own anchor into already-committed text - the expected
  `"$previousWord "` span is verified against the real document first (mirrors `performAutocorrectUndo()`'s
  own "verify against ground truth before touching anything" discipline), and the fusion is silently
  abandoned (falls through to A-05's own split) if the document does not actually match. Capitalisation uses
  a new `fusionContext()` - deliberately not `contextFor()`, which reads the *current* token's own live
  `tokenSentenceStart` etc., not `previousWord`'s (already-committed, potentially several actions earlier)
  real context; Rule 1 (explicit input) and the ordinary noun/proper-noun rules still apply, `sentenceStart`
  is conservatively assumed false (a genuine sentence start immediately followed by another already-
  committed word that then gets fused is a rare edge case, and no forced capital is the safe direction).

  Un-teaching the previous word reuses A-11's existing `recentLearnRecords` reach-back buffer directly - no
  new bookkeeping - matched by word text, reversed via the same `unlearnWord()` A-07/A-11 already share,
  skipped entirely when `dictionaryStore.isKnownWord(previousWord)` is true. Full A-07 undo: three new fields
  (`undoWasFusion`, `undoFusionUnlearntWord`, plus explicit `= false`/`null` resets at every other undo-arming
  call site - a new flag needs the same defensive reset `undoWasCompound`/`undoWasSplit` already get, or a
  stale fusion flag could wrongly survive into an unrelated later correction's own undo). `performAutocorrectUndo()`
  gained its own `wasFusion` branch: restores the original two-word text, re-teaches the un-learned fragment
  (if any) via `learnWordStrong()` - the same authoritative treatment D-13 already gives a rejected split's
  rejoined word - and splits the restored text back into `previousWord`/`previousPreviousWord` itself (the
  generic `previousWord = typed` the plain/split branches use would wrongly treat the whole two-word restored
  text as one word).

  New tests: `TokenRepairTest` gained six `tryFuseAcrossSpace` cases (the reported example, already-known-
  current-token gate, no-match, blacklisted-candidate, highest-confidence-wins-among-several, and layout-
  dependence via a QWERTY instance); `MergeConfidenceTest` (5 cases) and `AutoMergeAggressivenessTest` (4
  cases) mirror their `CorrectionConfidence`/`AutocorrectAggressiveness` counterparts; `SettingsMapperTest`
  gained three cases for the new fail-closed enable/aggressiveness/fallback resolution. No test exists for
  `AdaptKeyService`'s own `applyFusion()`/`performAutocorrectUndo()` wiring itself - Android `InputConnection`
  glue stays untested by this project's own established convention.

  1586 unit tests (1568 -> 1586, +18 new). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  509 -> 510, `versionName` `"1.2.13"` -> `"1.2.14"`.

- **§453 (v1.2.13): D-397 - a generic, layout-derived vertical touch-drift cap, replacing "wait for the**
  **next reported pair" with one rule covering every row boundary.** T-03's existing tighter caps (D-133
  bottom-row-into-space-bar, D-231/D-233 Enter/Backspace) were each a one-off, hand-picked pair added after a
  specific device report - every other row boundary (the reported case: `q`'s own downward drift registering
  as `a`) sat at the model's full isotropic 0.5 cap with no protection at all. Discussed directly (design
  options A/B/C) before implementing, per this project's own convention for non-trivial touch-model changes:
  the user picked the geometry-derived option (C) once it became clear the row/column position data it needs
  already exists in the repo for a related purpose.

  **Mechanism.** New `RowGeometry` (`keyboard/RowGeometry.kt`, pure/Android-free, JVM-tested) is the single
  shared row/column position model - one letter-row list per `LayoutKind` plus the shared digit row -
  extracted from `KeyboardProximity`'s own per-layout `ROWS` lists (D-442), which duplicated this exact
  geometry purely for typo-adjacency scoring (D-28/D-38) until now. All eight `KeyboardProximity*` objects
  now build their `ROWS` from `RowGeometry.rowsFor(...)` instead of a hand-duplicated literal string - zero
  behaviour change there, confirmed by the existing `KeyboardProximity*Test` suites passing unchanged (they
  only exercise the public `adjacent()`/`neighboursOf()` API).

  `AdaptKeyboardView.downwardOffsetFactorFor()`/`upwardOffsetFactorFor()` now fall through to a new shared
  `genericVerticalOffsetFactorFor()` once their own existing special cases don't match: a `KeyCode.CHAR` key
  on `InputSurface.LETTERS` (URL/email mode included - only their bottom control row differs) gets the new
  `GENERIC_VERTICAL_OFFSET_FACTOR = 0.3` in a direction whenever `RowGeometry.hasRowAbove()`/`hasRowBelow()`
  says the active layout genuinely has another row there - looser than the three existing 0.25 special cases
  (which are checked first and still win), tighter than the model's own general 0.5. Scoped to
  `InputSurface.LETTERS` deliberately: the symbol/calculator surfaces reuse some of the same characters
  (digits especially) in an unrelated grid `RowGeometry` knows nothing about.

  **The persistent number row is treated exactly like any other row, per explicit user instruction** - no
  exception carved out for it: shown, it participates in the row list like the three letter rows do (the top
  letter row gets the same generic upward cap toward it, the digit row itself gets the same generic downward
  cap toward the top letter row); hidden, the top letter row simply has no row above it, same as before.

  **Deliberately not modelled**: the third-letter-row/space-bar boundary - it already has its own tighter,
  device-confirmed D-133 override, checked first, so `RowGeometry.hasRowBelow()` correctly stays out of scope
  for that specific boundary rather than silently loosening an already-tuned value (see that function's own
  KDoc). 0.3 is the user's own confirmed value for the generic cap - a considered starting point, not yet
  device-tuned beyond that sign-off, same status as every other threshold constant in this file.

  New `RowGeometryTest` (8 cases): row-index lookup with/without the digit row, the named `q`/`a` repro
  directly, the digit-row-is-not-special assertion, the bottom-row boundary staying out of this model's own
  scope, and reachability across a second/third layout (AZERTY, Greek, Serbian). No test exists (or is
  expected) for `AdaptKeyboardView`'s own private dispatch functions themselves - Android View glue stays
  untested by this project's own established convention; `RowGeometry` is the pure, testable half.

  1568 unit tests (1560 -> 1568, +8 new). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  508 -> 509, `versionName` `"1.2.12"` -> `"1.2.13"`.

- **§452 (v1.2.12): D-457-followup - the full lowercase spelling of a learned acronym (typing the whole word,**
  **not just its prefix) still resolved to a mangled casing; plus D-455/D-457/D-458's own temporary**
  **diagnostic logging removed now that all three are device-confirmed fixed.**

  **The deeper D-457 gap**: §450's own fix only covered `capitalise()` being called with `word` already
  spelled as the acronym itself (e.g. the ordinary ranked suggestion, already correctly cased by
  `unigramsByPrefix()`'s own casing-merge). A real device log then showed typing the *entire* word `"llm"`
  lower-case (not just "ll") still produced `"Llm"`: `isPureNoun` correctly decided "uppercase" (the learned
  entry carries a `NOUN` tag), but every branch of the hierarchy can only ever touch `word`'s own first
  character (`uppercaseFirst`/`lowercaseFirst`) - none of them could ever reconstruct the real multi-capital
  `"LLM"` from a fully lower-case `"llm"`, tagged or not. Root cause is structurally different from §450's -
  not a missing veto, but `capitalise()` never having a way to learn the *real* canonical spelling of the
  word it was asked to case in the first place.

  **Fix**: a new check right after the `CapsMode.CHARACTERS` special case - `store.entryOf(word)` resolves
  case-insensitively and returns the entry's own real casing regardless of the query's own case, so if `word`
  is not already acronym-shaped but its canonical dictionary/learned entry *is* (`Acronym.isAcronym`), that
  canonical form is returned directly, skipping the whole per-first-character hierarchy for it entirely.
  Deliberately narrow: skipped whenever `context.explicitFirstUpper` is true, so rule 1 (explicit user input
  always wins) keeps its existing absolute priority unchanged - this is a later-priority fallback for the
  reported all-lowercase-typing case specifically, not a new standing exception to rule 1. Three new
  `CapitalisationEngineTest` cases: the confirmed-broken full-lowercase-typing case, explicit input still
  winning over it, and an ordinary lowercase word with no acronym-shaped entry staying unaffected (regression
  safety). Device-confirmed by the user ("Ja, ich bestätige, dass 'LLM' jetzt als Chip kommt").

  **Diagnostic cleanup**: D-455/D-457/D-458 are now all device-confirmed fixed, so their own temporary
  `"AdaptKeySuggest"` logging (`scheduleReclaimAndChipRefresh`'s suppression-state line,
  `reclaimWordAtCaret`'s three early-return/success lines, and every early-return branch inside
  `refreshSuggestions()`) is removed - only D-452's own two `tookMs=` timing lines (`refreshSuggestions`'s
  `provider.suggestionsFor()` call and `showSuggestions`'s total duration) remain, since that item is still
  open and still needs them for its next capture.

  1560 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  507 -> 508, `versionName` `"1.2.11"` -> `"1.2.12"`.

- **§451 (v1.2.11): D-458 root-caused and fixed - a spurious double space in the A-03 context string**
  **wrongly suppressed every suggestion from the second word onward - plus D-452's own timing diagnostics.**

  **D-458**: the user's device log, captured typing `"Test llm"`, showed it directly:
  `refreshSuggestions: cleared - suppressAutocorrect from selectActiveDictionary("Test  l")` - a literal
  double space between the previous word and the new token. `tokenContextBefore` is real document text
  (`captureTokenContext()` sets it straight from `getTextBeforeCursor()`), already ending in whatever real
  whitespace precedes the caret (`"Test "` right after committing "Test" + its trailing space) - both
  `finalizeAndCommit()` and `refreshSuggestions()` then concatenated a *further* literal space before the
  typed token (`"$tokenContextBefore $typed"`/`"$tokenContextBefore $input"`), producing `"Test  l"` from the
  second word onward. Never on a field's first word (`tokenContextBefore == ""` there, so only ever a
  harmless single leading space resulted) - exactly why this silently escaped notice until a real log caught
  it. The extra space confused `LanguageClassifier`'s own n-gram matching enough to misread the context as
  foreign, setting `suppressAutocorrect = true` and clearing the bar for every keystroke of the second word
  onward. Fixed at both call sites (`"$tokenContextBefore$typed"`/`"$tokenContextBefore$input"`, no
  space) - matching the already-correct convention `refreshSuggestions()`'s own tier-3 prompt-sentence string
  used a few hundred lines below all along. No new unit test - both call sites are inside
  `AdaptKeyService`/`InputConnection`-glue with no existing test harness, this project's own accepted,
  established gap for this class of code; verification is the device repro itself, per D-357's own identical
  precedent just before this round.

  **D-452**: real wall-clock timing (mirroring D-217/D-220's own established `SystemClock.uptimeMillis()`
  before/after pattern) added to `refreshSuggestions()`'s own `provider.suggestionsFor()` call (the one D-153/
  D-207/D-211 already name as the per-keystroke cost driver) and to `showSuggestions()`'s total duration -
  both logged under the same `"AdaptKeySuggest"` tag D-458's own diagnostics already use. The device log the
  user sent for this item showed no smoking gun (no dictionary work fired repeatedly, only cheap, already-
  empty `showSuggestions()` re-renders around a field restart) - still open, needs a log that actually
  captures the perceived-slow moment with this new timing data to make further progress.

  1557 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  506 -> 507, `versionName` `"1.2.10"` -> `"1.2.11"`.

- **§450 (v1.2.10): D-455 (Gemini Shift re-derivation) + D-457 (mangled learned-acronym casing), plus D-458's**
  **own temporary diagnostic logging for a new, not-yet-root-caused report.**

  **D-455**: `AdaptKeyService` gained `rearmShiftForCaretMove(ic, preserveShiftAfterOpener)` - the exact
  Shift/Caps re-derivation core `reclaimWordAtCaret()` already had inline (`captureTokenContext`/
  `resetWordEndShift`/the `shiftArmedByDelete`/`tokenShiftLiveArmed`/`preserveShiftAfterOpener` decision),
  extracted verbatim rather than rewritten so the ordinary (non-suppressed) path stays byte-identical. A new
  `rearmShiftForCaretMoveWhenReclaimSuppressed()` calls it standalone - own flag consumption, own
  `composing.isNotEmpty()` guard, own `ic.beginBatchEdit()`, mirroring `reclaimWordAtCaret()`'s own structure
  exactly rather than the IPC calls migrating outside a batch edit. `scheduleReclaimAndChipRefresh()`'s
  `reclaimOnCaretMoveSuppressed` branch now schedules a new debounced `rearmShiftForCaretMoveRunnable` instead
  of doing nothing, mirroring `reclaimEnabledRunnable`'s own existing "runs unconditionally regardless of
  suppression" precedent (D-414-followup) - cancelled in `clearComposing()` for the identical stale-callback
  reason `reclaimWordAtCaretRunnable` already is. See spec's new G-05 addendum.

  **D-457**: root-caused by tracing three separate layers with real (throwaway, later replaced by permanent
  assertions) diagnostic tests rather than guessing from the original hypothesis, which turned out wrong -
  `DictionarySuggestionProvider.suggestionsFor()` and `SuggestionController.displayed()` both already handle a
  differently-cased learned candidate correctly (confirmed directly, not assumed); the real bug was in
  `CapitalisationEngine.capitalise()`'s own "no signal, lowercase it" branch, which only ever touches a word's
  first character (`casing.lowercaseFirst()`) - correct for an ordinary word, but silently mangling a
  deliberately all-caps, not-noun-tagged learned acronym into a nonsensical hybrid (`"LLM"` -> `"lLM"`) instead
  of leaving it alone. Fixed with a new branch reusing the already-established `Acronym.isAcronym()` signal
  (D-403/D-404-followup) ahead of the plain lowercase fallback: `word` is returned completely unchanged
  whenever it is a genuine acronym, since the "upper" branch right above it already produces the correct,
  no-op result for one (`uppercaseFirst("LLM") == "LLM"`), so only the lowercase branch ever needed the guard.
  Two new `CapitalisationEngineTest` cases (the confirmed-broken untagged case, plus the already-working
  NOUN-tagged case as a non-regression check) plus one each in `DictionarySuggestionProviderTest`/
  `SuggestionControllerTest` (converted from the exploratory diagnostics into permanent assertions once they
  had done their job) - see spec's new §6 addendum.

  **D-458 (new, not part of this fix - see its own Open TODos entry)**: while implementing the two rounds
  above, the user reported a real, more severe, not-yet-root-caused regression ("ab dem zweiten Wort keine
  Chips mehr") and asked for diagnostic logging rather than a guessed fix - a new `"AdaptKeySuggest"` `diag()`
  tag now covers `refreshSuggestions()`'s own early-return branches and final candidate count,
  `showSuggestions()`'s item-count pipeline, and the D-455 functions' own early returns. Temporary, to be
  removed once a real device log pins down the actual cause.

  1557 unit tests green (was 1553). `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  505 -> 506, `versionName` `"1.2.9"` -> `"1.2.10"`.

- **§449 (v1.2.9): D-357 reopened and fixed for real, device-confirmed - mid-word edit + double-tap Shift**
  **capitalising the wrong letter.** Closed once (2026-09-01) as not reproducible; the user captured a real
  Google Keep device log reproducing it and asked for a fresh look, per this project's own "re-derive from
  real logs, don't
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
  code; verification is the device repro itself. **Device-confirmed** against the exact repro parcours
  derived from the user's own log ("hat funktioniert").

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

## Older Rounds (§1-§431, v0.7.6 through v1.1.70) - Pruned From This File

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
