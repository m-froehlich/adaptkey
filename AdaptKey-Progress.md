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

- **`seedBundledBlacklist`'s cross-language-confusables set (A-04, `due`/`sue`/`ddr`/`aks`) is German-only.**
  Found while auditing every place that does *not* route through the active-language pipeline (history §210's
  own D-287 fix) - deliberately scoped to German today (that is the only curated list that exists), not a bug
  in the D-287 sense. Revisit once a second installed language's own confusables against English are actually
  curated - the seeding mechanism itself (`installStores()`) does not need to change shape, only gain a
  per-language list to seed from, the same way `hints_<code>.tsv` (D-281) already generalised the AltGr hint
  set per language.

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

- **D-314 built French's AZERTY *geometry* only - no `dictionaries/fr/` pack (dict/bigram/hints/version)**
  **exists yet, and `LanguagePackCatalog.ENTRIES` has no `Language.FRENCH` entry.** French is now fully
  typeable (the layout is real, compiled, and wired to `Language.FRENCH`) but not yet usable end to end as a
  language pack - a French speaker gets the right keys but no dictionary/autocorrect/suggestions until
  someone builds and hosts one, following the Contribution Guide's own §3/§4. Not started; flagging so a
  future session (or an actual French-speaking contributor) doesn't have to rediscover that the geometry and
  the content are two separate, independently-completed pieces of this feature.

- **D-280/D-281 follow-up: `SPANISH`/`ITALIAN`/`DUTCH`/`PORTUGUESE` are already in the `Language` enum and**
  **already fully typeable via QWERTY, but none has a dictionary or its own hint set built/hosted yet** - a
  genuine, ready-to-pick-up community contribution opportunity, same shape as the French gap above but without
  even a geometry question to resolve first (see `AdaptKey-Language-Contribution-Guide.md`). Separately, the
  Python script that originally built `language_profiles.tsv` (A-03's trigram classifier data) is not in this
  repository - reconstructing it is only needed if a future language falls outside the eight already covered
  there.

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
  - **D-357 - RESOLVED (2026-09-01, no code change - user confirmation).** User confirmed no longer
    reproducible. No dedicated fix identified as the cause; not root-caused further since there is nothing
    left to fix.
  - **D-358 - RESOLVED (§289 v1.0.44).** Double-tap-Backspace revert was broken right after punctuation -
    fixed alongside D-359.
  - **D-359 - RESOLVED (§289 v1.0.44).** A word reverted via double-tap Backspace was immediately
    re-autocorrected on the very next Space - a revert now gets exactly one unimpeded retry (learns nothing
    itself, distinct from D-403's own broader "silently-corrected word must eventually become learnable"
    ask, see below).
  - **D-360 - OPEN.** A commit + autocorrect right before an Enter/newline must still be revertible by a
    plain Backspace afterward.
  - **D-361 - OPEN.** Fast Backspace typing should not let neighbour keys (including Enter) react within the
    double-tap window - ideally Backspace's own touch zone temporarily grows while typing fast. Further,
    more aggressive idea floated by the user: a fast neighbour-tap near Backspace could itself be
    retroactively reinterpreted as Backspace. Possibly its own setting either way.
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
  - **D-369 - OPEN.** Accepting a suggestion chip must not insert a space when punctuation already
    immediately follows.
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
  - **D-380 - OPEN.** A long-press smear too small to trigger a swipe/page-change should still open the alt
    popup - the `o` key is named as the one that frequently fails to.
  - **D-381 - RESOLVED, device-confirmed (2026-08-31, no code change).** The fresh look this bullet asked
    for happened: user confirmed on-device that D-404 §323/§324's Learned Words editor (category
    multi-select, LLM-determined with a tier-3 model installed) fully satisfies the original ask.
  - **D-382 - RESOLVED (§330, v1.0.82).** `KeyboardLayout.numberKey('2')` (shared by both QWERTZ and QWERTY,
    L-01) now offers an apostrophe and subscript `₂` as a third and fourth long-press alternative, alongside
    the existing shifted-symbol/superscript pair. Every other digit unaffected.
  - **D-383 - OPEN.** In Google Keep's list mode, placing the caret before a word and pressing Enter (to push
    the rest of the line into a new list item) deletes that word.
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
  - **D-387 - OPEN.** Extend the umlaut/diacritic unfold mechanism (D-144/D-204) to other languages - know
    each language's own base letters and their diacritic variants.
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
  - **D-393 - OPEN.** In the Google Play Store's own search bar, Enter does not act as Submit.
  - **D-394 - RESOLVED, digit-mirror only (§330, v1.0.82).** The calculator page's digit block now reads
    `1 2 3` / `4 5 6` / `7 8 9` / `0` top to bottom (was calculator-style `7 8 9` / `4 5 6` / `1 2 3` / `0`) -
    the operator column and every other key untouched. The T9-letter-long-press half of this ask was
    explicitly declined by the user - dropped from scope entirely, not deferred.
  - **D-395 - OPEN.** The distance kept from the system gesture-navigation bar should be configurable - the
    user reports the gesture has become far more sensitive on their current Android version, causing
    accidental app-hide/app-switch near the space bar's lower edge.
  - **D-396 - OPEN.** Classify per-key vibration into three levels: mode-switch actions (Level 1), autocorrect/
    chip-acceptance (Level 2), ordinary key feedback (Level 3).
  - **D-397 - OPEN.** Touch zones should generally bleed less into neighbouring rows, not only the bottom
    letter row's already-capped case - named example: `q` currently reaches far enough down to frequently
    produce an unwanted `q` instead of the intended `a` below it.
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
  2. **A lighter cross-reference/lemma-link approach** - keep both inflected forms as separate dictionary
     rows, but link them so ranking/A-01's override-protection logic can tell "same word family" apart from
     "coincidentally similar, unrelated word" (today nothing distinguishes those two cases at all). More
     tractable than tier 1, but still a real data-curation task (a lemma mapping across the dictionary) plus
     real code work (`shouldOverrideKnownWord` and friends) - its own future design round, not attempted here.
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
     **D-404 is fully closed except for tier 2, still open.**

  **D-412 (see Current State) has since laid the schema groundwork tier 1 would need** - a bundled-only
  `lemma` link column on `TABLE_WORDS` - and a genuinely new, in-progress project is using it: tagging every
  German verb currently mis-tagged plain `OTHER` (not just the `NOUN`/`VERB` collision cases D-368 already
  finished), including already-present inflected forms, recording each one's base-form link via the new
  column as a byproduct. Only the ≥2000-frequency band is done so far (§306, 78 words); five bands remain
  (500-1999/200-499/50-199/10-49/<10, ~10,700 candidates total), plus the mechanical weak-verb-inflection
  derivation pass, a strong-verb principal-parts reference table, and actually populating `lemma` on the
  results (tagging and linking were kept as separate steps). This is real progress toward tier 1, not tier 1
  itself - full generative morphology (replacing stored inflected rows with runtime generation) is still its
  own, not-yet-started, uncertain-feasibility project.

- **D-351-followup - OPEN, reopened 2026-09-01.** The same field/editor incompatibility D-351 found and
  worked around for Gemini's search field (`reclaimOnCaretMoveSuppressed`, scoped by package name in
  `AdaptKeyService.onStartInput()` - see [AdaptKeyService.kt:298](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:298))
  is now also reported for Total Commander's "Datei umbenennen" (rename) date field. Not yet actioned:
  - Need Total Commander's actual package name (`com.ghisler.android.TotalCommander` is the likely value but
    unconfirmed - verify against a real device/logcat before wiring it in, per this project's own
    "verify, don't guess" rule).
  - The existing mechanism suppresses reactive caret-move reclaim for the **whole package**, not a specific
    field - worth confirming with the user whether Total Commander's other text fields (e.g. path bar, other
    dialogs) tolerate reclaim fine before blanket-suppressing the whole app the way Gemini's single-purpose
    search field made an easy call for. If they don't all tolerate it equally, the fix is the same shape but
    the tolerance question needs answering first.
  - Root cause not yet confirmed to be identical to D-351's (composing-region change breaks the field's own
    cursor-handle/selection UI) - worth a quick device-log check rather than assuming without verification,
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

- **D-416 - RESOLVED (§333, see Current State).** A-12's eager auto-space-after-punctuation replaced with the
  deferred model discussed and planned in
  [`AdaptKey-Plan-D416-Deferred-Space.md`](AdaptKey-Plan-D416-Deferred-Space.md) - see that file and the
  Current State entry for the full account. Correction to the original framing above: D-373 turned out **not**
  to be related (a hyphen never got its own A-12 auto-space to begin with, so there was nothing for it to
  collapse into - it remains its own, independent, still-open item, see below). D-384 genuinely is eased by
  the new model, but was **not** bundled into this round - still its own separate, not-yet-implemented item.
  D-363 (colon/semicolon vs. emoticon) also stays a deliberately separate, not-yet-decided follow-up. No
  settings toggle was built, per the user's own explicit call (would not scale across every touch point) -
  the rollback path instead is a dedicated, kept-current
  [rollback-notes document](AdaptKey-Rollback-D416-Deferred-Space.md) plus keeping the migration on one
  clean, isolated commit.

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

## Current State

- **§368 (v1.1.7): D-422 - English "Wortfamilien" parity project - real POS tags, Wiktionary-sourced**
  **inflection forms, lemma-linking, for `app/src/main/assets/en/dict.tsv`.** User's own framing: the German
  dictionary should not set a quality bar the English one falls short of, and completeness genuinely helps
  autocorrect quality even though English capitalisation itself barely benefits (§6 mostly doesn't apply to
  English common nouns). Mirrors the German Wortfamilien project (§322) end to end - same source
  (wiktextract/kaikki.org, MIT tool / CC BY-SA content), same scope discipline (only complete *existing*
  `dict.tsv` lemmas, never grow the vocabulary itself - 223,481 of 259,374 extracted Wiktionary words had no
  existing `dict.tsv` lemma and were correctly skipped), same collision rule (never overwrite an existing
  row), same append-only merge (file stays frequency-sorted, new rows added at the tail, never resorted).

  **Real difference from §322, confirmed empirically before generating anything, not assumed:** `en/dict.tsv`
  had *zero* real POS tags going in - only `OTHER`/`PROPER_NOUN` from the original casing-heuristic seed
  (same crude starting point German itself had before §322, just never since upgraded for English). No
  separate "link what's already there" phase (German's own §320/§321, done before §322's own generation
  pass) was needed - starting from Wiktionary data from the very first step let tagging and generation happen
  in one integrated pass instead of two.

  **Source difference from German, discovered live, not assumed:** kaikki.org has no small
  English-as-target-language extract the way it does for German (`de-extract.jsonl.gz`) - since the *source*
  Wiktionary edition already is English, "English-target" entries are just the bulk of its own full,
  cross-language dump (`raw-wiktextract-data.jsonl.gz`, 2.6GB compressed / 22.9GB uncompressed, ~9x
  German's own `de-extract.jsonl.gz`). Filtered to `lang_code=="en"` in one single pass (not four) extracting
  nouns/verbs/adjectives/prepositions together, since re-streaming a file this size four times would have
  cost real, avoidable time.

  New `dictionaries/en/extract_wiktionary.py` (nouns: plural; verbs: 3rd-person-singular/present-participle/
  simple-past/past-participle; adjectives: comparative/superlative; prepositions: closed-class, tagging
  only, no generation) + `merge_wiktionary.py` (tags existing lemmas, links/generates missing forms with a
  frequency calibrated from already-linked pairs' own median form/lemma ratio - noun 0.407 n=9782, verb
  0.484 n=14202, adjective 0.066 n=271, all in the same order of magnitude as German's own §322 ratios
  (nouns 0.355, verbs 0.417), a reassuring cross-language sanity check, not merely a coincidence).

  **Two real bugs caught by spot-checking before finalising, not shipped-then-found** (mirrors §322's own
  six write-verify-revert verb rounds - this project's now-established discipline of not trusting a Wiktionary
  merge without checking real output rows first):
  1. **Case-insensitive false match.** The archaic dialectal verb `"gan"` (attested past tense `"went"`)
     case-insensitively matched an unrelated, already-present `dict.tsv` entry `"Gan"` (freq 47, `OTHER`) -
     `"went"` would have been wrongly lemma-linked to `"Gan"` instead of `"go"`. `dict.tsv`'s own casing
     convention ("the most frequent surface form") means a row whose casing differs from Wiktionary's own is
     real evidence of a *different* word, not a casing quirk of the same one - fixed by requiring an exact
     case match before accepting any lemma or form match at all (a missed completion is a far safer failure
     than cross-contaminating two unrelated words); a matching second guard prevents generating a *new* row
     that would collide case-insensitively with an existing differently-cased one, preserving `dict.tsv`'s
     own case-insensitive-uniqueness invariant (verified after the fix: 0 case-insensitive duplicates in the
     merged file).
  2. **Whole-entry archaic sense.** `"child"` has a genuine but archaic (Shakespeare-era, 1596-1608) Wiktionary
     verb sense, "(archaic, ambitransitive) To give birth" - not caught by the form-level qualifier filter
     (`EXCLUDE_QUALIFIERS`, itself needed for real cases like `"gooder"`/`"goodest"` instead of `"better"`/
     `"best"` for `"good"`, and `"yode"`/`"goed"` instead of `"went"` for `"go"`) since Wiktionary marks
     whole-sense archaism via each sense's own gloss text, not the `forms[]`-level tags that filter already
     checks. Fixed with a second, entry-level filter: skip an entry only when *every* sense's gloss starts
     with an archaic/obsolete/dialectal/nonstandard/rare/proscribed qualifier in parentheses (a word with
     both an archaic and a live modern sense keeps its modern one).

  **Net result:** `dict.tsv` 90,026 -> 116,413 rows (+26,387: 16,798 noun forms, 8,040 verb forms, 1,549
  adjective forms), 35,487 lemmas tagged with a real POS from Wiktionary, 29,315 already-present forms newly
  linked to their lemma, 156 prepositions tagged. `OTHER` (the old "unknown" fallback) dropped from 55,878 to
  16,013 words - frequency-weighted, **85.2% of the original 90,026 words' combined real-world occurrence
  count now carries a genuine POS tag** (was 0%; exceeds German's own §322 frequency-weighted coverage,
  consistent with English's structurally simpler morphology needing less generation to reach full coverage).
  `bigram.tsv`/`hints.tsv` untouched, confirmed by diff. The Wiktionary extract scripts and their own
  intermediate TSVs (`wiktionary_nouns.tsv`/`_verbs.tsv`/`_adjectives.tsv`/`_prepositions.tsv`) committed to
  `dictionaries/en/`, mirroring `dictionaries/de/`'s own precedent - the raw 2.6GB source dump itself is not
  (same as German's own `de-extract.jsonl.gz`, never checked in).

  No new tests - data-only, `en/dict.tsv` is a bundled asset read through the same already-generic,
  already-tested `DictionaryAssetParser`/`bulkImport` pipeline every language already uses (the `lemma`
  column, D-412, was already fully generic across languages with zero English-specific code needed). 1220
  unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  423 -> 424, `versionName` `"1.1.6"` -> `"1.1.7"`. **Not yet device-confirmed** - worth a real-device check
  that English suggestions/autocorrect now behave noticeably better for common inflected forms and that
  nothing regressed for already-well-known words.

- **§367 (v1.1.6): D-377 - new A-13, evidence-gated missed-Backspace recovery, chip-only.** Recovers a badly
  garbled token where a Backspace was missed and a neighbouring key was hit instead (e.g. `"welxmche"` for
  `"welche"` - the user's own worked example, an accidental `x` for `c` then a Backspace attempt that landed
  on `m`). A first proposal (widen A-09's own candidate length window so `wideFuzzyNeighbours` could even
  reach a target 2 characters shorter) was discussed and explicitly rejected by the user: that would have
  loosened acceptance for *every* long unresolved token generically, with dictionary-membership-after-
  arbitrary-deletion as the only signal - real risk of coincidental false corrections with no actual
  connection to what was typed. Root-caused before either proposal, not guessed: `correctionCandidatesInternal`
  fetches candidates within `token.length ± 1` - shared by both the ordinary (cost-2) and wide (cost-4)
  searches, but only wide enough for the ordinary one, so `"welche"` (6 chars) never even reaches the edit-
  distance comparison from `"welxmche"` (8 chars) regardless of cost budget. Verified empirically via a
  throwaway probe test (removed again after use) before either proposal, not by hand-arithmetic alone: the
  pure edit cost is exactly 4 (two deletions x `INDEL_COST` 2), sitting right at `WIDE_CORRECTION_COST`'s own
  ceiling.

  The user's own preferred design instead - concretely recognise what happened at the *input* level and
  reverse exactly that action, mirroring T-02/T-03's own `RawCoordinateCorrection` precedent (consult the
  real tap, not just the dictionary) rather than A-09's dictionary-coincidence shape. New pure
  `suggestion/MissedBackspaceRecovery`: for each composing-token tap position (from index 1 onward, mirroring
  what a real Backspace press could have reached), checks whether that tap's own recorded raw coordinate
  landed within one key's own width/height of Backspace's actual on-screen position (the user's own
  calibration, "ein Abstand von einer Taste reicht aus" - implemented as Backspace's rect expanded by exactly
  one more of its own half-width/half-height in each direction, resolution-independent by construction). Only
  a genuine near-Backspace tap becomes a removal candidate; only that character and its immediate predecessor
  are ever removed together - never an arbitrary deletion search over unrelated positions. New
  `AdaptKeyboardView.deleteKeyGeometry()` (mirrors `charKeyGeometry()`, filtered to `KeyCode.DELETE` instead
  of `KeyCode.CHAR`) supplies Backspace's own geometry; `composingTaps` already recorded the raw taps needed,
  no new input-tracking plumbing required. `AdaptKeyService.missedBackspaceCorrection()` mirrors
  `rawCoordinateCorrection()`'s own shape and is gated identically to `rawCoordinateSuggestion` in
  `refreshSuggestions()` - deferred pass only, only once `candidates.isEmpty()` (the user's own explicit "das
  Wort komplett unbekannt ist" condition, satisfied by the same "nothing else found anything" signal
  `rawCoordinateSuggestion` already relies on for its identical framing). **Chip-only per the user's own
  explicit call** ("Das wäre sonst zu riskant") - fed only into `refreshSuggestions()`'s own `extras` list,
  never wired into `finalizeAndCommit()`'s silent-correction chain the way `rawCoordinateCorrection()` itself
  is.

  8 new `MissedBackspaceRecoveryTest` cases (the worked example itself, the one-key-away boundary exactly and
  one unit past it, no near-Backspace tap, the first-character exemption, multiple independent candidates in
  one token, a tap-count mismatch, a too-short token). 1212 -> 1220 unit tests. Spec: new A-13.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 422 -> 423, `versionName` `"1.1.5"` ->
  `"1.1.6"`. **Not yet device-confirmed** - needs a real device check: deliberately mistype a word so a wrong
  key lands where Backspace should have, on a badly garbled multi-typo token, and verify the corrected word
  appears as a chip (never silently applied).

- **§366 (v1.1.5): D-376 - new S-12, a "km/h" speed-unit completion chip.** Two independent trigger points,
  both mirroring S-08's own "Uhr" time-suggestion reasoning exactly: **Trigger 1** - the composing token is
  exactly `"km"` - offers the full `"km/h"` as a completion, injected into `refreshSuggestions()`'s own
  `extras` list (`MAX_PRIORITY_SUGGESTION_SCORE`, same shape as `rawCoordinateSuggestion`/
  `autocorrectSplitChip` right beside it) so it competes directly with the plain `"km"` candidate; **Trigger
  2** - `"km/"` has just been committed (the user typed the `/` themselves) - offers the glued remainder
  `"h"`, no leading space (the `/` is already there). New pure `suggestion/SpeedUnitCompletion`
  (`completionForComposing`/`suffixAfterSlash`) mirrors `TimePattern`'s own "just-typed shape" reasoning -
  `"km/h"` can never itself be a tokenisable dictionary word (it contains `/`), so no amount of bigram/
  frequency data could ever surface it on its own. Deliberately **not** gated through `LanguageRules` unlike
  S-08's own German-only "Uhr" - `"km/h"` is the identical SI notation in German and English alike, not a
  genuinely language-specific word (a deliberate design choice, not an oversight - see S-12's own KDoc).

  Wired at both of the same two structural points S-08 already needed, for the identical reason:
  `speedUnitSuffixSuggestion()` alongside `timeSuggestion()` in `showNextWordPredictions()` (the common case
  - `/` typed immediately after `"km"` with nothing in between, so composing was still `"km"` and the commit
  went through the ordinary word-commit path), and `showSpeedUnitSuggestion(ic)` alongside
  `showTimeSuggestion(ic)` in `finalizeAndCommit()`'s composing-already-empty branch (the rarer case - `"km"`
  was finalised by a Space first, `/` typed as its own fresh standalone token afterwards).

  One real bug caught before shipping, not after: a first-draft `\bkm/$` regex for Trigger 2 used a plain
  `\b` word-boundary before `"km"`, which would have silently rejected the single most realistic case,
  `"50km/"` - `\b` requires a transition between a `\w` and a non-`\w` character, and a digit is itself `\w`,
  so there is no boundary at all between `"5"` and `"k"`. Fixed with a negative lookbehind for a preceding
  Unicode *letter* specifically (`(?<![\p{L}])km/$`) - nothing immediately before `"km"` (string start), a
  digit, whitespace or punctuation are all fine, only a preceding letter disqualifies it (correctly rejects
  `"akm/"`, where `"km"` is the tail of a longer word, not its own token). Caught by writing the test for the
  realistic `"50km/"` case before trusting the regex, not by a device report.

  13 new `SpeedUnitCompletionTest` cases (both triggers, case-insensitivity, the digit-vs-letter boundary
  distinction, empty input). 1199 -> 1212 unit tests. Spec: new S-12. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 421 -> 422, `versionName` `"1.1.4"` -> `"1.1.5"`. **Not yet
  device-confirmed** - needs a real device check for both triggers, ideally including the `"50km/"` digit-
  prefixed shape specifically, since that is exactly the case the pre-ship regex fix targeted.

- **§365 (v1.1.4): D-404-followup (v3) - Learned Words editor's language/sort labels removed.** Reported
  directly right after §363's row-merge shipped: "Sprache:"/"Sortierung:" left too little width for the two
  spinners' own selected-value text, which was getting cut off. Removed both `TextView` labels from
  `activity_learned_words.xml` entirely (a spinner's own content already makes its purpose obvious without
  one, per the user's own call - "Die versteht man auch ohne") rather than shrinking them further; the two
  spinners now split the row's full width evenly. The now-unused `learned_words_language`/`learned_words_sort`
  string resources removed from all three locales (en/de/el) - nothing else referenced them.

  No new tests - pure Android layout/resource change, the same untested layer this project's convention
  already leaves untested throughout. 1199 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 420 -> 421, `versionName` `"1.1.3"` ->
  `"1.1.4"`. **Not yet device-confirmed** - needs a real device/screen check that both spinners' selected
  values now display in full.

- **§364 (v1.1.3): D-36-followup regression fix - the clipboard peek button's own chips flashed and**
  **immediately vanished again.** Reported directly right after §363 shipped ("kurz angezeigt, verschwinden
  aber sofort wieder"). Root-caused, not guessed: `openClipboardPeek()`'s own `finalizeAndCommit(ic, "")` call
  generates an asynchronous `onUpdateSelection` echo - composing already empty by the time it lands, exactly
  [`suppressNextReclaimSpaceReset`]'s own D-123 precedent - which reaches `scheduleReclaimAndChipRefresh()`
  and, `RECLAIM_DEBOUNCE_MS` (100ms) later, silently overwrites the bar via `reclaimWordAtCaret()`'s/
  `reclaimEnabledRunnable`'s own `refreshSuggestions()`/`showSuggestions()` calls - neither of which has any
  idea the bar was just showing something special, the same class of race D-421-followup already found and
  fixed for D-36's field-open chip and D-142's credential list, just triggered reactively here instead of
  from a direct call site.

  Fixed with a short, self-expiring blackout window rather than a single-consume flag: unlike an ordinary
  suggestion tap, whether this particular `commitText("")` actually produces an echo at all depends on the
  target editor (composing may already have been empty, with nothing to commit), so a flag only ever cleared
  by the echo it is waiting for could stay wrongly armed and swallow the *next genuine* caret move
  indefinitely if that echo never arrives. New `reclaimChipRefreshSuppressedUntil` (a timestamp,
  `SystemClock.uptimeMillis()`-based) is armed by `openClipboardPeek()` for `RECLAIM_DEBOUNCE_MS +
  CLIPBOARD_PEEK_ECHO_GUARD_MARGIN_MS` (100 + 250 = 350ms) right before its own `finalizeAndCommit()` call;
  `scheduleReclaimAndChipRefresh()` now no-ops entirely while still inside that window, covering both
  runnables it schedules in one place rather than patching each of their own eventual render calls
  separately.

  No new tests - the fix lives entirely in `AdaptKeyService.kt`'s own Android/`InputConnection` timing glue,
  the same untested class this project's convention already leaves untested throughout. 1199 unit tests
  unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 419 -> 420,
  `versionName` `"1.1.2"` -> `"1.1.3"`. **Not yet device-confirmed** - needs a real device check: tap the
  clipboard peek button and verify the chips now stay up instead of vanishing within ~350ms.

- **§363 (v1.1.2): a "Kleinigkeiten" punch-list batch - ten small, mostly independent items, built and**
  **committed together at the user's own request ("bitte nicht jedes Mal ein Version Bump und Build").**

  1. **`Morgen` tagging fix.** Retagged `NOUN,OTHER` (frequency 561) in `dictionaries/de/dict.tsv` so the
     lower-case adverb "morgen" ("tomorrow") is no longer force-capitalised by §6 rule 3 the way the pure noun
     "Morgen" ("morning") correctly still is.

  2. **`fröhlich` capitalisation report - withdrawn, no code change.** Investigated on the user's report that
     typing "Fröhlich" (their own surname) got silently lower-cased; the user re-checked and could no longer
     reproduce it ("Meine Diagnose... kann ich nicht mehr bestätigen"). No fix applied - nothing was found to
     be actually wrong once re-checked live.

  3. **D-404-followup: dual-casing chips for a genuinely ambiguous noun (new S-11).** A word like "Weg"/"weg"
     (`NOUN,OTHER`) previously only ever showed one casing in the suggestion bar, since capitalisation is
     derived from live context, never from a candidate's own cased form. New `SuggestionController.Kind.
     AMBIGUOUS_CASE` chip, built by `ambiguousCasingChips()`: both casings offered while the composing token is
     only a prefix of the word, only the *other* casing once it exactly matches (S-02, unconditional, no
     exception for autocorrect being off). `excludeAmbiguousCasingWords()` drops the same candidates from the
     ordinary ranked list so they are never shown twice.

  4. **`OK` autocorrect-to-`Öl` fix.** Reported directly ("Ok" kept correcting to "Öl"). Raised `OK`'s
     frequency in `dict.tsv` from 46 to 800 - `CorrectionConfidence`'s own ratio-based override formula was
     recomputed by hand and should not have fired at any of the three built-in aggressiveness levels at the
     old frequency either, so the raise is independently justified regardless of root cause. The actual
     trigger turned out to be the user's own *personal* blacklist entry for "ok" (invisible to this session,
     routing the word through the far more permissive `forUnknownToken` path instead of the protected
     `forKnownWordOverride` one) - removed by the user directly in the app; the frequency raise stays as a
     confirmed-valid fix on its own merits.

  5. **D-404-followup: raw-coordinate correction now vetoes a split too (A-05).** Reported directly
     ("Trobaner" splitting at the "b" instead of correcting to "Trojaner"). `finalizeAndCommit()`'s T-02/T-03
     raw-coordinate-correction search now runs *before* the split decision (previously computed only
     afterwards, as an unprotected last resort) and a non-null result vetoes the split exactly like
     `bestCorrection()`'s own high-confidence flag already did - a split may only win once every safer
     mechanism has already had its own chance and found nothing.

  6. **D-404-followup / D-410: German compound-forming-particle veto against a false split (A-05).** Reported
     directly ("Schonfenster" splitting into "schon"/"Fenster"). New `LanguageRules.blocksAsCompoundPrefix`
     interface method (German-bound per D-410, like every other grammar veto in this section - explicit user
     request, "muss fest ans deutsche Sprachpaket gebunden sein"), implemented in `GermanRules` against a
     curated set of common adverbs/particles (`schon`, `wohl`, `hoch`, `tief`, `voll`, `halb`, `fern`, `nah`,
     `kaum`, `fast`) that only vetoes a split when the right half is itself a known noun - each particle is
     also an ordinary standalone word, so "schon gut" still splits normally. Wired into `TokenRepair.
     candidateAt()` right after the existing "both nouns" veto. 3 new `TokenRepairTest` cases (the veto firing,
     the veto *not* firing against a non-noun right half, and `NoOpLanguageRules` staying unaffected -
     confirms the D-410 language-binding).

  7. **D-404-followup: A-12's deferred space now materialises correctly after a protected abbreviation.**
     Reported directly ("bzgl." showed the pending-space dot but the space never actually landed once the next
     word started, even though the auto-capital was correctly suppressed). `shouldMaterializeSpace()`
     previously treated Shift being off at a sentence-terminator position as *always* meaning a deliberate
     user override (correct at a genuine sentence end) - wrong for a known abbreviation/enumerator, where §6's
     own "No Sentence Start After Known Abbreviations and Enumerators" rule means Shift is correctly never
     armed there in the first place, nothing to override. Fixed by re-deriving `sentenceStartBefore(ic)` fresh
     at the space decision (the same check `armShiftForNextWord` itself already uses) to tell "never armed"
     apart from "deliberately overridden" - only the latter still suppresses the space.

  8. **D-36-followup: clipboard peek button mid-text (new V-04).** V-01's own direct-paste chip only ever
     appeared once, right when a field opened - gone for good the moment anything was typed, with no way back
     short of leaving and re-entering the field. New button shares V-03's clear-button square (mutually
     exclusive visibility) whenever the clipboard holds a clip fresher than V-01's own 5-minute window and the
     chips are not already showing, independent of composing state. A tap finalises any in-progress word via
     `finalizeAndCommit(ic, "")` (the same call `toggleLanguage()` already relies on to close out a mid-word
     token before switching context) and shows the V-01/V-02 chips exclusively; the caret's next move reverts
     the bar to normal on its own via the existing `onUpdateSelection` -> `reclaimEnabledRunnable` ->
     `showSuggestions()` path, no dedicated "close peek" code needed. The freshness flag
     (`clipboardPeekAvailable`) is cached and only recomputed at the handful of points it could plausibly
     change (field open, a `ClipboardManager.OnPrimaryClipChangedListener` firing, the caret settling) rather
     than on every keystroke, so `setSuggestionBarItems()` - which runs on every keystroke - stays free of a
     live clipboard query.

  9. **D-404-followup: Learned Words editor - Save moved into the dialog's own button row, plus a
     frequency/last-used info line.** The old Save icon sat right next to the text field, next to Copy, where
     it was repeatedly mis-tapped for Forget; moved down to the dialog's own neutral button (bottom row,
     alongside Forget/Cancel) via `setNeutralButton`, enabled/disabled the same way as before (only when the
     edit is a case-only correction of the original word, D-292). Also added a frequency/last-used line
     (`DateFormat.getDateFormat`) above the category checkboxes, so a power user can judge an entry's real
     standing - and how close it is to C-24's own expiry window - without leaving the dialog.
     `activity_learned_words.xml`: the language and sort-order filter rows merged into one horizontal row
     (both fit comfortably; stacking wasted vertical space).

  10. **D-362-followup: the "Gelernt: ..." loading chip's tick animation starts on its own first frame.**
      Previously showed a static "…" placeholder until the view's `onAttachStateChangeListener` fired and
      posted the first real animation tick - a visible one-frame gap before the dot animation actually
      started. `SuggestionBarView` now seeds the label with the animation's own starting frame
      (`LOADING_MAX_DOTS` dots) directly, matching what the `Runnable` itself starts from.

  No new tests beyond item 6's three (items 1/4 are data-only; items 9/10 are Android view glue this
  project's own convention already leaves untested; items 2/3/5/7/8 are either no-op or the usual untested
  `AdaptKeyService.kt` Android/`InputConnection` glue). 1196 -> 1199 unit tests. Spec: A-05 (items 5/6), A-12
  (item 7), new S-11 (item 3), new V-04 (item 8) all updated to the current, crystallised state - items 9/10
  are implementation-only polish with no spec-level requirement change, left undocumented there per this
  project's own "occasionally skippable for a trivial change" convention. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 418 -> 419, `versionName` `"1.1.1"` -> `"1.1.2"`. **Not yet
  device-confirmed** - items 1/4 were device-reported and should be spot-checked again; items 3/5/6/7/8/9/10
  are new/changed behaviour with no device pass yet at all.

- **§362 (v1.1.1): three small, related `dict.tsv` data fixes - a real tag-order regression fix plus two**
  **cheap, closed-class taggings (prepositions, proper nouns).** User caught the regression directly from a
  throwaway mention in chat ("OTHER,ADJECTIVE") rather than a bug report: §360's adjective round appended
  `ADJECTIVE` after whatever tag a lemma already carried, landing as `OTHER,ADJECTIVE` - breaking the
  established convention every pre-existing multi-tag row already followed (`NOUN,OTHER`, `VERB,OTHER`,
  `NOUN,VERB,OTHER` - `OTHER` always last, i.e. tags in `PartOfSpeech`'s own enum declaration order). Fixed
  by re-sorting every multi-tag row's POS field into canonical enum order (3,646 rows corrected this round
  specifically for the violation, e.g. `schön` is now `ADJECTIVE,OTHER`; the general sort would silently fix
  any future ordering slip the same way).
  
  Bundled while already touching this file, both discussed and confirmed cheap first (closed word classes,
  no inflection, no generation project needed - unlike nouns/verbs/adjectives): **prepositions** - 155 total
  in Wiktionary's `pos=="prep"`, 109 already present in `dict.tsv` tagged `PREPOSITION` (46 not yet present
  deliberately left out, same "complete existing words only" scope as every other round - several of them
  looked like noise anyway, e.g. `vong`/`vmb`, an internet-slang/typo-looking entry and an unclear fragment).
  **Proper nouns** - 15,808 total in Wiktionary's `pos=="name"`, 6,052 already present, but only the 6,049
  already tagged `NOUN` were additionally tagged `PROPER_NOUN` - checked `CapitalisationEngine` directly
  before doing this: `isProper` forces capitalisation ahead of the "ambiguous, leave alone" rule, so this was
  only genuinely risk-free for words *already* `NOUN` (capitalisation outcome unchanged either way, e.g.
  `Zeit`/`Welt`/`Bild` stay capitalised exactly as before despite also having a real name-sense in
  Wiktionary). The 3 non-`NOUN` matches (`iPhone`/`iPad`/`eBay`, all `OTHER`) were deliberately excluded -
  forcing first-letter capitalisation would have produced `Iphone` from a lower-case-typed `iphone`, wrong
  for a brand name with its own internal-capitalisation convention; left untouched, same as before.
  
  Net result: 9,804 `dict.tsv` lines changed (3,646 pure reorder + 109 preposition + 6,049 proper-noun tags -
  the sums don't add to 9,804 exactly since a handful of rows needed more than one kind of change at once,
  e.g. `in` went from `OTHER,ADJECTIVE` straight to `ADJECTIVE,PREPOSITION,OTHER` in one pass). `git status`
  confirms `bigram.tsv`/`hints.tsv` untouched. `dictionaries/de/version.txt` 35 -> 36, pack rebuilt and
  verified byte-identical after unzip, `LanguagePackCatalog` version 35 -> 36. No new tests (data-only). 1196
  unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 417
  → 418, `versionName` `"1.1.0"` → `"1.1.1"`. Not yet device-confirmed.

- **§361 (v1.1.0): D-404-followup - the non-LLM Learned-Words lemma linker (`LearnedLemmaLinking`, spec**
  **§39) gained adjective declension/degree endings, closing a real gap the with-LLM path never had.**
  User asked directly whether adjectives get whole-family learning "bei Verfügbarkeit eines LLMs" the same
  way nouns/verbs already do. Checked the actual code before answering, not assumed: `Tier3FamilyPrompt`
  already lists `ADJECTIVE` as one of the categories the model is asked to choose from,
  `Tier3FamilyResponseParser`/`Tier3FamilyApplier`/`SqliteDictionaryStore.learn()`'s `categoryHint` are all
  fully POS-generic, and both trigger sites (`dispatchFamilyLearning()` on every learn event,
  `maybeReprocessFamiliesAsync()`'s backfill) gate only on `LearnOutcome`/missing-lemma, never on part of
  speech - the with-LLM path already covered adjectives from the day it was built, no code change needed
  there. The gap was actually in the **separate, non-LLM** conservative linker (spec §39's own "Non-LLM
  Path") - `NOUN_ENDINGS`/`RegularVerbInflection` only, no adjective endings at all. User confirmed this was
  the real ask and requested the non-LLM side be extended.
  
  Added `ADJECTIVE_ENDINGS` to `LearnedLemmaLinking.kt`: the plain declined Positiv's `-em` (the rest of its
  endings already overlap `NOUN_ENDINGS`), the declined Komparativ (`-er -ere -eren -erem -erer -eres`), and
  the declined Superlativ in **both** the regular (`-ste -sten -stem -ster -stes`) and dental/sibilant-
  extended (`-este -esten -estem -ester -estes`) form - both tried unconditionally since this lookup-only
  linker has no way to know which is grammatically correct for an arbitrary stem, unlike the D-404 Tier 1
  generation project (§360) which does. Wired into both `findLemma` (forward: strip an ending, check if the
  remainder is already learned) and `candidateInflections` (reverse: generate candidates from a newly-learned
  base, check each against the existing lexicon) - same unconditional-try pattern the noun/verb endings
  already used, same accepted coincidental-match trade-off. Deliberately excludes e-elision (`dunkel` ->
  `dunkler`) - a plain suffix strip cannot recover an elided stem, so an e-eliding adjective falls outside
  this conservative linker's reach, same scope boundary `RegularVerbInflection` already draws around strong/
  ablaut verbs. Spec §39's "Base-form linking" section updated to describe all three ending families. 5 new
  tests (`findLemma`: declined comparative, regular declined superlative, dental-extended declined
  superlative, plain declined positive `-em`; `candidateInflections`: adjective-style forms) - 1191 -> 1196
  unit tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  
  **Explicit user-requested milestone bump, not the usual third-digit step** (same shape as D-283's 0.9.0 and
  D-315's 1.0.0): `versionCode` 416 → 417, `versionName` `"1.0.112"` → `"1.1.0"`, marking D-404's full closure
  (all three tiers now either resolved or explicitly still-open-by-design) as its own milestone. Not yet
  device-confirmed.

- **§360 (v1.0.112): D-404 Tier 1, adjectives - full German adjective declension x degree paradigm added**
  **for every already-bundled lemma, closing the last open part of speech from the "Wortfamilien" project**
  **(§322).** Same scope discipline as nouns/verbs: complete existing lemmas only, no vocabulary growth -
  verified before starting, not assumed carried over from the noun precedent: of 27,957 Wiktionary adjective
  lemmas not yet in `dict.tsv`, only 274 (~1%) have any `bigram.tsv` occurrence at all, the same near-zero
  signal that ruled out bulk noun import, so the same call was made here (user confirmed after seeing the
  real numbers).
  
  Real Wiktionary data checked before any code was written, not assumed from how nouns/verbs worked: German
  Wiktionary's own adjective entries carry a **complete, real Flexion table** in `wiktextract`'s `forms`
  array wherever one exists at all (17,061 of 32,202 total lemmas, 3,786 of them in scope) - every case x
  number x declension-type x degree combination as its own attested entry, not just the three bare degree
  stems as first assumed from the Wiktionary page itself. This is why `extract_wiktionary_adjektive.py`
  needed no declension-generation logic of its own for the covered majority - it only had to extract and
  deduplicate what was already there (predicative multi-word entries like `"er ist schön"` filtered out as
  phrases, not tokens). A genuine, confirmed lexical irregularity - `hoch` declines as `hoher`/`hohe`/`hohes`
  (a stem change, not merely an ending, present even in the Positiv) - is exactly why full attested forms
  were taken directly rather than derived from a generic suffix rule; would have silently produced
  `hocher`/`hoches` otherwise.
  
  The small remainder (20 in-scope lemmas with only bare Positiv/Komparativ/Superlativ stems, no full table;
  0 lemmas had neither) went through a new rule module, `adjektiv_deklination.py`, cross-verified against
  real Wiktionary-attested forms for regular words before use (1,296 declined forms checked, 0 real
  mismatches - the two apparent "mismatches" found first were `dunkel`'s own optional umlauted comparative
  variant `dünklere`, itself present as a second, equally valid attested form, and `kurz`'s umlaut-class
  irregularity, correctly outside this module's deliberate scope since `kurz` is fully Wiktionary-covered
  anyway). Endings table derived programmatically from `schön`'s own real data, not typed from memory -
  confirmed byte-identical across all three degrees. Two confirmed rules: e-elision for `-el`/`-er` stems
  (`dunkel`+`er`→`dunkler`, `dunkel`+declension-`e`→`dunkle`) applies before the Komparativ `-er` marker and
  before any vowel-initial declension ending, but **not** before the Superlativ `-st`/`-est` marker itself
  (`dunkelste`, not `dunklste` - confirmed against real `dunkel`/`edel` data, an easy wrong guess since
  `-est` also starts with a vowel); the dental/sibilant Superlativ extension (`-est` instead of `-st`) after
  s/ß/z/x/d/t/sch, confirmed against `heiß`/`kurz`/`bunt`/`rund`/`laut`/`frisch` (the last of which Wiktionary
  itself lists two valid variants for, `frischeste`/`frischste` - this module deliberately only ever produces
  the `-este` form). Deliberately excludes the closed umlaut-mutation class (`alt`/`kurz`/`groß`/...) from
  the rule engine entirely, same scoping precedent as the noun/verb round's own strong-verb table - an
  umlauted form is only ever used when Wiktionary itself attests it.
  
  Frequency: new-form frequency = lemma frequency x 0.5 - a **real, adjective-specific** median (not borrowed
  from the noun/verb figures), computed from 8,837 real, already-existing `dict.tsv` form/lemma frequency
  pairs (declined adjective forms that already sat in `dict.tsv` unlinked, matched to their lemma via the
  Wiktionary form list itself as ground truth) - a substantially larger, more direct calibration sample than
  either the noun (14,976 pairs) or verb (191 pairs) round had. Collision rule unchanged: never write a form
  already present in `dict.tsv` under any POS. Existing lemma rows also gained the `ADJECTIVE` tag alongside
  whatever they already carried (3,804 rows, e.g. `schön` is now `OTHER,ADJECTIVE` not just `OTHER`) - `dict.
  tsv`'s own low-frequency tail is appended-to, never resorted (confirmed the file is frequency-descending,
  not alphabetical, before writing - an early draft of the merge script would have alphabetised the entire
  158k-row file otherwise). `viel`/`wenig` are not reachable under Wiktionary's `pos=="adj"` filter (tagged
  `adv`/`pron` there instead, confirmed directly) - deliberately excluded rather than building a special case
  for two words, a conscious scope call flagged rather than silently made.
  
  Pre-existing inflected adjective forms already sitting in `dict.tsv` before this round (e.g. `kälter`, `54`,
  `OTHER`) are **not** retroactively lemma-linked or re-tagged by this round - mirrors the noun/verb project's
  own split (§320/§321's linking pass was always separate from §322's generation pass), not attempted here
  either; left as a known, explicitly out-of-scope gap, same shape as D-404 Tier 2.
  
  Net result: `dict.tsv` 158,073 -> 189,267 rows (+31,194 adjective forms, all lemma-linked). `git status`
  confirms `bigram.tsv`/`hints.tsv` untouched. `dictionaries/de/version.txt` 34 -> 35, pack rebuilt and
  verified byte-identical after unzip (all four files, sha256), `LanguagePackCatalog` version 34 -> 35. Spec
  §38 gained the adjective-coverage addendum. New tooling committed: `extract_wiktionary_adjektive.py`,
  `adjektiv_deklination.py` (kept, reusable "Formen-Auskunft" style like `nomen.py`/`verben.py`); the one-off
  merge script was not committed (scratchpad only), matching how nouns/verbs never kept a dedicated write
  script either. `AdaptKey-Plan-Adjektive.md` deleted, superseded by this section. No new tests (data-only;
  `lemma` still has zero code readers). 1191 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 415 → 416, `versionName` `"1.0.111"` →
  `"1.0.112"`. Not yet device-confirmed.

- **§359 (v1.0.111): D-370 - a closing double-quote right after sentence punctuation no longer gets a**
  **wrongly-placed deferred space shoved in front of it.** Root-caused directly in the current code, not
  guessed: `handlePunctuationDelimiter()`'s A-12 deferred-space check (D-416) only ever looked at the single
  character immediately before the cursor - typing `"` right after `.` with a space genuinely pending (e.g.
  closing a quoted sentence, `"Ja."`) fell into neither the punctuation-run nor the digit-glue exception, so
  the space materialised *before* the quote (`"Ja. "` instead of `"Ja."`). Scoped to only "Problem 1" of the
  two originally discussed - the wider "eat an explicit space before *any* closing quote" idea was raised only
  as an opportunistic add-on and explicitly dropped by the user once its real design cost (open/close-quote
  disambiguation via a lookback/parity search) was laid out; not implemented, not tracked as its own item any
  more (see D-370's own Open TODOs entry).
  
  Fixed by extending, not replacing, D-416's existing "read fresh from the real document, no stored flags"
  design: a new `pendingSentenceMark()` resolves the actually-pending mark from up to two characters before
  the cursor, skipping over exactly one trailing closing `"` first. A `"` typed directly after a bare
  `SENTENCE_PUNCTUATION` mark with nothing composing is structurally unambiguous as closing (nobody opens a
  new quote with no space directly after `.`/`!`/`?`/`,`), so no real open/close-quote tracking was needed for
  this narrower, actually-reported case. `handlePunctuationDelimiter()` now glues a closing quote directly onto
  the pending mark (exactly like a punctuation run), and every downstream consumer that used to read only the
  one immediate character - the new-word-start space materialisation in both `handleKey`'s CHAR branch and
  `appendLongPressLetter()` (the ä/ö/ü/ß entry point), plus the space-key's own D-416 pending-space dot
  (`updatePendingSpaceIndicator()`) - now goes through the same helper, so the space still lands correctly
  after the quote and the dot correctly stays lit through it rather than going dark early. Spec's A-12 gained
  the D-370 bullet (closing-quote glue) and dot note. No new tests (Android/`InputConnection` glue). 1191 unit
  tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 414 → 415,
  `versionName` `"1.0.110"` → `"1.0.111"`. **2026-09-01: device-confirmed working** ("Top! Das hat
  funktioniert.").

- **§358 (v1.0.110): D-421-followup (v2) - regression: the credential list and clipboard paste chip stopped**
  **appearing on a fresh empty field.** Reported directly: an empty email field no longer showed the saved
  address list until the first letter was typed, and a fresh field with clipboard content no longer offered
  the paste chip at all - both worked before §352/§354's own D-421 rounds. Root-caused, not guessed: §352's
  `onStartInputView` change schedules the debounced reclaim/chip-refresh unconditionally on every fresh field;
  its own `reclaimEnabledRunnable` calls the *ordinary* `showSuggestions()` ~100ms later, which knows nothing
  about D-36's paste chip or D-142's credential list - both built directly against the suggestion bar,
  deliberately bypassing that ordinary pipeline entirely (D-142's own KDoc) - and silently overwrote whichever
  one had just been shown with an empty bar. Fixed by only scheduling the reclaim/chip-refresh when neither
  special chip actually claimed the bar: `showClipboardChipIfAvailable()` now returns whether it actually
  showed a chip, `showCredentialSuggestions()`'s own branch always counts as claiming it (even the
  password-field empty-bar case is a deliberate, settled state), and `onStartInputView` gates the scheduling
  on `!showedSpecialInitialChip`. No functional loss: a field with one of these chips showing has nothing left
  for the reclaim/chip-refresh to usefully add, and a genuinely empty field (the only case D-421's own
  initial-focus reclaim could matter for at all) never has a special chip to protect in the first place. No
  new tests (Android/`InputConnection` glue). 1191 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's S-10 gained the D-421-followup (v2) note.
  `versionCode` 413 → 414, `versionName` `"1.0.109"` → `"1.0.110"`. **2026-09-01: device-confirmed working.**

- **§357 (v1.0.109): D-378-followup (v2) - a second, real device log confirmed the exact same root-cause**
  **shape as D-373-followup (v2), just via the opener path instead of the hyphen path.** User's own log
  (`AdaptKeyShift` tag, real repro this time - the first log sent for this round turned out to be an accidental
  duplicate of the D-373 one) traced it precisely: `armShiftForNextWordUnlessOpener` correctly left Shift
  untouched right when `"` committed (`isOpener=true`, `shiftedBefore=true` → `shiftedAfter=true`) - but by the
  time the user actually typed the next letter, `shifted` had already flipped back to `false` in the
  meantime, with no logged event visibly responsible (only the debounced D-62 reclaim's own *unlogged*
  `armShiftForNextWord` call could explain the gap). Confirmed: the debounced reclaim
  (`reclaimWordAtCaret`, fires ~100ms after any commit that leaves composing empty, openers included) calls
  the raw `armShiftForNextWord` directly, with zero awareness that the most recent commit was an opener.
  Fixed with the identical pattern D-373-followup (v2) just established: new one-shot
  `shiftPreservedAfterOpener` flag, set (to the opener decision itself, not just `true` - so a later
  non-opener commit correctly clears a stale value) by `armShiftForNextWordUnlessOpener`, consumed by
  `reclaimWordAtCaret()` right next to the existing `shiftArmedByDelete`/`tokenShiftLiveArmed` guards -
  captured into a local and cleared unconditionally before the function's own early-return, so a user typing
  faster than the 100ms debounce cannot leave it stale for some later, unrelated reclaim. All temporary
  `AdaptKeyShift` diagnostic logging from §355 removed now that both root causes are confirmed and fixed - see
  that entry if this class of bug ever needs the same treatment again. No new tests (Android/`InputConnection`
  glue). 1191 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's
  D-378 "Addendum to G-05" gained the (v2) note. `versionCode` 412 → 413, `versionName` `"1.0.108"` →
  `"1.0.109"`. **2026-09-01: device-confirmed working.**

- **§356 (v1.0.108): D-373-followup (v2) - the §355 diagnostic log immediately found the real gap: a**
  **flicker, not a functional failure.** User's own real-device log (`AdaptKeyShift` tag) traced end to end:
  the hyphen commits (`armShiftForNextWordUnlessOpener` correctly leaves Shift alone/false, not a sentence
  start) - ~100ms later the debounced D-62 reactive reclaim (`reclaimWordAtCaret`, fires because composing is
  empty right after the hyphen too) calls `captureTokenContext()` a *second* time, which correctly live-arms
  Shift for the hyphen-propagation case (§354's own fix) - but the very next line in that same function,
  `armShiftForNextWord(ic)` (D-313's own reactive re-derivation, there for a completely different reason: a
  caret landing on an existing word), immediately re-derives fresh and silently overwrites it back to `false`.
  By the time the user actually pressed the next letter, `captureTokenContext()` ran a *third* time (the
  ordinary CHAR-handler call site) and correctly re-armed it again - so the committed letter was capitalised
  correctly the whole time, exactly as the user eventually noticed ("scheint effektiv zu greifen") - but the
  keyboard's own Shift key visibly flickered on/off in the ~100ms window in between, reading as "not working"
  on a first glance. Fixed with a new one-shot `tokenShiftLiveArmed` flag, set by `captureTokenContext()` and
  consumed by `reclaimWordAtCaret()` exactly like the existing `shiftArmedByDelete` guard immediately above it
  in the same function - the established pattern in this codebase for "a just-armed special case must survive
  the next line's own generic re-derivation." D-378-followup's own diagnostic logging is left in place -
  still unexplained, no log for that one yet. No new tests (Android/`InputConnection` glue). 1191 unit tests
  unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's B-02 gained the
  D-373-followup (v2) note. `versionCode` 411 → 412, `versionName` `"1.0.107"` → `"1.0.108"`.
  **2026-09-01: device-confirmed working** ("Top! Das hat funktioniert.").

- **§355 (v1.0.107): D-373-followup and D-378-followup confirmed still broken after §354's broadening -**
  **temporary diagnostic logging added instead of a third blind patch.** Two full rounds of code-level fixes
  each traced correctly on paper (confirmed again by re-reading the actual diff, not just re-deriving from
  memory) - both still failed with the *exact same* repro on-device. Rather than guess a third time, this
  project's own established pattern for exactly this class of bug (spec §1's guiding principle: Shift/
  composing-state bugs "reproduce silently and took real device logs, not code review alone, to find each
  time", D-110/D-139/D-217/D-324's own precedent) - a new `AdaptKeyShift` diagnostic tag logs the full
  decision chain: `captureTokenContext()`'s own hyphen-segment/live-arm decision (before and after),
  `armShiftForNextWordUnlessOpener()`'s own opener check and shifted/capsLock before and after, the CHAR
  handler's own `isUpperArmed()` read at the exact moment a letter's case is decided, and `handleShift()`'s
  own toggle/grace-window-suppression outcome. Reachable via `adb logcat -s AdaptKeyShift:D` or the in-app
  Settings -> Diagnostics log. Genuinely no working theory left worth coding blind against - waiting on a
  real repro's log output before touching either mechanism again. No new tests (diagnostic logging only, no
  behaviour change). 1191 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. No spec change (no behaviour changed). `versionCode` 410 → 411, `versionName` `"1.0.106"` ->
  `"1.0.107"`.

- **§354 (v1.0.106): three same-day device-reported follow-ups - §353's D-373/D-378 fixes were each too**
  **narrow, plus a genuinely different D-421 flash path.** All from real device feedback right after §353/
  §352 shipped; each re-diagnosed from the actual code path the report implied, not patched blind (this
  project's own rule after a negative device report on an already-"fixed" point). **D-373-followup:** the
  original fix only ever changed the eventual *committed* casing (`CapitalisationEngine`), never what Shift
  shows armed while the segment is still being typed - a user watching the keyboard saw nothing happen.
  `captureTokenContext()` now also live-arms Shift directly, but only for the non-sentence-start branch (no
  dictionary lookup needed there); the sentence-start branch stays commit-time-only, matching how B-02's own
  original proper-noun exception already behaved. **D-378-followup:** the original fix only guarded
  `finalizeAndCommit`'s composing-*empty* branch - every other commit branch (split, merge, verbatim, the
  ordinary word-commit path, the A-07 undo retry) still called `armShiftForNextWord` directly and still
  clobbered Shift on an opening quote/bracket. New shared `armShiftForNextWordUnlessOpener(ic, delimiter)` -
  every one of those call sites now funnels through it instead of calling `armShiftForNextWord` directly, so
  a future new commit branch cannot reopen the same gap. **D-421-followup:** the reported flash survived
  because it came from a genuinely different path than the one §352 fixed - tapping from one still-composing
  word straight onto another reaches `onUpdateSelection`'s "external caret move while composing" branch
  (D-406), which clears the abandoned word's `reclaimPending` along with its composing state and calls
  `armShiftForNextWord` immediately, rendering the bar before any reclaim was ever scheduled for the *new*
  position - `reclaimPending` alone cannot guard a reclaim that was never scheduled in the first place. That
  branch now also calls `scheduleReclaimAndChipRefresh()` (gated on a genuinely collapsed caret) before its
  own immediate re-arm, so a pending reclaim is already in place by the time the chip's own next render runs.
  No new tests (all three are Android/`InputConnection` glue). 1191 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: B-02 gained the D-373-followup note, the D-378
  "Addendum to G-05" gained its own follow-up note, S-10 gained the D-421-followup note. `versionCode`
  409 → 410, `versionName` `"1.0.105"` → `"1.0.106"`. **2026-09-01: D-421-followup device-confirmed - the**
  **reclaim flash is gone.** D-373-followup and D-378-followup are **not** - same exact repro, still broken;
  see §355 for the diagnostic round that replaces a third blind patch attempt.

- **§353 (v1.0.105): D-373 + D-378 + D-392 - three capitalisation fixes discussed and designed with the**
  **user first, D-390 dropped in the same discussion.** All four were raised together; design discussed
  before any code, per this project's own rule for non-trivial capitalisation/algorithm decisions.
  **D-373** (hyphen-chain capitalisation propagation): B-02's own `isProper`-only exception missed the common
  case of an ordinary word after a hyphen whose *predecessor* was capitalised but which itself carries no
  proper-noun tag (`"Nord"` in `"München-Nord"`). User's own explicit hybrid design: `CapitalisationContext`
  gained `previousHyphenSegment`/`previousHyphenSegmentAtSentenceStart`, computed in `captureTokenContext()`
  via a new pure `SentenceBoundary.previousHyphenSegment()` (walks back to the segment right before the
  trailing hyphen, then re-runs `isSentenceStart()` on the text before *that* segment). `CapitalisationEngine`
  gained `previousSegmentPropagates()`: when the predecessor was itself at a sentence start, only propagates
  if it is independently a known noun/proper noun in the dictionary (a bare sentence-initial capital proves
  nothing about grammar); otherwise the predecessor's capital is trusted directly. **D-378** (opening
  quote/bracket must not disturb Shift): root-caused to `finalizeAndCommit`'s composing-empty branch calling
  `armShiftForNextWord(ic)` unconditionally after *every* delimiter - re-deriving after a bare opener with
  nothing yet composing either wrongly de-arms an already-correct auto-arm (`SentenceBoundary` sees the
  opener itself as "still mid-token") or silently clobbers an explicit Shift press. Fixed by skipping that one
  call for a recognised opener (`"([{<` - new `OPENING_PUNCTUATION`; the apostrophe deliberately excluded,
  genuinely ambiguous between opening and closing a quote per the user's own confirmation) - `handlePunctuationDelimiter`'s
  own re-arm branch already only fires for `SENTENCE_PUNCTUATION`, which no opener is a member of, so no
  second call site needed the same guard. **D-392** (Caps Lock release): `handleShift()`'s Caps-Lock-release
  branch unconditionally cleared Shift with no context re-derivation, unlike every other "position reached"
  event (D-313/D-406) - now calls `armShiftForNextWord(ic)` when nothing is composing (a genuine word
  boundary), keeping the old plain disarm only for the mid-word edge case (no well-defined "next word" to
  re-derive against there). **D-390 dropped** (WON'T FIX, no code change): a general multi-part-abbreviation
  rule would have needed a deliberate, narrow exception to D-405's own "never a commit-time correction"
  principle plus editing already-committed text - real risk. User's own call: D-405/D-416 already mean typing
  "p. a." straight through in lower-case is never auto-corrected back either, so there is no effective problem
  left, just marginally more typing effort - acceptable, not worth the risk. 11 new tests (6
  `CapitalisationEngineTest` covering both propagation branches in both directions, 5
  `SentenceBoundaryTest` covering `previousHyphenSegment()` directly incl. chain/no-predecessor cases) -
  D-378/D-392 are untested Android/`InputConnection` glue per this project's own convention. 1180 → 1191 unit
  tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: B-02 gained the D-373
  addendum, G-06 gained the D-392 addendum, a new "Addendum to G-05" covers D-378. `versionCode` 408 → 409,
  `versionName` `"1.0.104"` → `"1.0.105"`. **Not yet device-confirmed.**

- **§352 (v1.0.104): D-421 - two Reclaim-chip/Google-Keep bugs, both root-caused from real device reports,**
  **not guessed.** (1) **A field's very first caret position was never reclaimed.** Reported: tapping
  directly into an already-typed word to focus a Google Keep field for the first time left it unreclaimed;
  only a *second* tap onto a different word actually reclaimed. Root cause: `onStartInput`'s own D-152
  comment already documents that a field's initial selection arrives only via `EditorInfo.initialSelStart`/
  `initialSelEnd`, never guaranteed through a subsequent `onUpdateSelection` callback - the reactive D-62
  reclaim was only ever wired to that callback, so the field's true first caret position, delivered a
  different way, was simply never seen by it. Fixed in `onStartInputView`: when `initialSelStart ==
  initialSelEnd` (a collapsed caret) and both are known, schedules the identical debounced reclaim (and the
  chip's own visibility refresh) the reactive caret-move path already uses.
  (2) **A visible flash: chip briefly shown, then hidden.** Reported on a *second* tap, immediately after
  (1)'s own leftover unreclaimed chip was already on screen. Root cause traced one level deeper than the
  report itself: `reclaimPossible()` (the chip's own visibility check) is re-evaluated live on *every*
  `showSuggestions()` call, with no awareness of whether `reclaimWordAtCaretRunnable`'s own 100 ms debounce
  is already scheduled to resolve the very same position - so the very first render after any caret move
  could see "nothing composing yet, a word touches the caret" and show the chip, moments before the
  scheduled automatic reclaim quietly resolved it. New `reclaimPending` flag (true only for the duration of
  a *non-suppressed* field's own pending reclaim - never set at all in Gemini, where nothing is ever
  automatically pending) closes this for any caret move, not merely the one the original report happened to
  hit. Both fixes share one new `scheduleReclaimAndChipRefresh()` (extracted from `onUpdateSelection`'s own
  existing scheduling code, now also called from `onStartInputView`) rather than a second, differently-timed
  reclaim path. No new tests (Android/`InputConnection` glue, per this project's own convention - the
  `reclaimPending` state machine itself has no pure/testable seam separate from the real callbacks). 1180
  unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec's S-10 gained
  the D-421 addendum. `versionCode` 407 → 408, `versionName` `"1.0.103"` → `"1.0.104"`. **Not yet
  device-confirmed.**

- **§351 (v1.0.103): D-371 - a digit-ending typed token is now only ever silently autocorrected at C-22's**
  **Aggressive level.** Design discussed and agreed with the user first (this project's own rule for
  non-trivial confidence/algorithm decisions): rather than a new one-off level check, reuses the exact same
  **cap** mechanism D-354 already established in `CorrectionConfidence` for the structurally identical
  "risky signal, but the chip offer should still stand" question (`prefixShiftsAway`) - a second, independent
  mechanism for the same kind of decision was explicitly rejected as inconsistent. `forUnknownToken` gained a
  `typedEndsInDigit` parameter and a new `DIGIT_SUFFIX_CONFIDENCE_CAP` (0.72), deliberately placed *above*
  Aggressive's own auto-apply threshold (0.70) but *below* Medium's (0.75) - unlike `PREFIX_CONFIDENCE_CAP`
  (0.55, below every level), this cap lets an otherwise high-confidence candidate still auto-apply, but only
  at the most permissive level; every chip-offer threshold sits well under 0.70, so the chip is never
  suppressed at any level. `capped()` now combines both caps via the lower of the two when both apply.
  Wired in at `DictionarySuggestionProvider.candidateConfidence()` - the one function already shared between
  the chip-offer filter and the auto-apply decision, so this needed no new call site - with a local comment
  pointing at the new constant's own KDoc. A-01's known-word-override path (`forKnownWordOverride`) is
  untouched: a digit-ending token is practically never itself a dictionary entry, so that path is not
  reachable for this case. 4 new tests (`CorrectionConfidenceTest`: clears Aggressive but not Medium/Cautious
  at maximal underlying confidence; a low-confidence digit-ending token still fails every level, proving the
  cap only ever lowers a score, never raises one - `DictionarySuggestionProviderTest`: end-to-end at the
  default Medium level vs. an Aggressive-configured provider). 1175 → 1179 unit tests, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec §36 gained the D-371 addendum (and its own
  header D-number list). `versionCode` 406 → 407, `versionName` `"1.0.102"` → `"1.0.103"`. **Not yet
  device-confirmed.**

- **§350 (v1.0.102): D-420 - an A-05 split at a sentence/line start now keeps its own capital.** Reported
  directly: "Komischerweise" typed as the first word of a line committed as "komischer weise" after A-05
  split it, instead of "Komischer Weise". Root cause: `TokenRepair.SplitResult.resolvedLeft`/`resolvedRight`
  are always lower-case by contract; all three split-applying call sites (`applySplit()`, the autocorrect
  split chip, `midWordConnectorSplitSuggestion()`) derived the left half's capitalisation context via
  `contextFor(split.resolvedLeft)`, so `explicitFirstUpper` was unconditionally false there - the real typed
  capital (sentence-start-armed or hand-typed) never reached `CapitalisationEngine.capitalise()`. Fixed by
  passing the original typed token (`typed`/`input`, already in scope) to `contextFor()` at all three sites
  instead - `contextFor()` only reads the passed string's first character, and the left half always starts
  at the same position as the original token, so this recovers the real capitalisation correctly without any
  sentence-start-specific special case (Rule 1, "explicit user input always wins", simply reaches the left
  half now). Closes a design question the spec had explicitly flagged as unresolved since A-05 shipped. No
  new tests (the fix lives entirely in `AdaptKeyService.kt`'s own untested Android/`InputConnection` glue;
  `TokenRepair` itself stays pure/tested, lower-casing both halves is its own explicit contract). 1175 unit
  tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: the A-05 section's
  previously-open design question replaced with the resolution. `versionCode` 405 → 406, `versionName`
  `"1.0.101"` → `"1.0.102"`. **Not yet device-confirmed.**

- **§349 (v1.0.101): D-389-followup (v5) + D-419-followup - decouple C-24's storage from its display; bold**
  **"Currently" line.** Two pieces of direct feedback. (1) §347 tied the *stored* preference value directly
  to the current duration meaning (enum renamed to `ONE_MONTH`/etc.) - user's own explicit correction: the
  stored value must stay the abstract `early`/`medium`/`late`/`never` level, decoupled from what duration it
  currently means, so retuning a threshold later applies immediately to everyone with that level already
  saved, no migration needed. `LearnedWordExpiryWindow` renamed back to `EARLY`/`MEDIUM`/`LATE`/`NEVER` (days
  unchanged: 30/120/365/null); the displayed label strings stay hand-maintained duration text, with a
  class-KDoc warning to keep both in sync manually when `days` changes. `arrays.xml`'s stored-value array
  reverted to match. Hit and fixed a nasty KDoc bug along the way: a literal `*/` inside a doc-comment line
  (`res/values*/strings.xml`) silently closed the comment early, cascading into unrelated syntax errors
  further down. (2) The "Currently: X" line (D-419) is now fully bold via a `SpannableString`/`StyleSpan`,
  not plain text, so it visibly stands apart from the description above it. No new tests (rename + Android
  view glue). 1175 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  Spec W-05 and the D-419 note both updated. `versionCode` 404 → 405, `versionName` `"1.0.100"` →
  `"1.0.101"`. **Not yet device-confirmed.**

- **§348 (v1.0.100): D-389-followup (v4) - a word family only expires once every member has gone stale.**
  User's own explicit framing: a single stale member of a D-404 word family must not "tear a hole" in an
  otherwise-alive family - the whole family now expires together, or a frequently-used member holds the rest
  alive indefinitely. Needed the `lemma` link on every swept entry, which `learnedWords()` alone doesn't
  reliably carry (`SqliteDictionaryStore`'s own version never selected it - a pre-existing gap vs.
  `InMemoryDictionaryStore`'s). Fixed properly: `learnedWordsWithTimestamp()` (word/frequency/lastTouched/
  category/lemma) promoted from a `SqliteDictionaryStore`-only method onto the shared `DictionaryStore`
  interface, with a new `InMemoryDictionaryStore` implementation added. `LearnedWordExpirySweep` now groups
  by `entry.lemma ?: entry.word.lowercase()` and only expires a family once every member is individually
  stale - an unlinked word is still exactly a family of one, unchanged behaviour. 4 new tests (1
  `InMemoryDictionaryStoreTest`, 3 `LearnedWordExpirySweepTest` - both directions of "one member keeps the
  other alive", plus whole-family-expires-together). 1171 → 1175 unit tests, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec W-05 gained the family-expiry addendum.
  `versionCode` 403 → 404, `versionName` `"1.0.99"` → `"1.0.100"`. **Not yet device-confirmed.**

- **§347 (v1.0.99): D-389-followup (v3) - C-24 shows concrete durations, not abstract labels.** User's own
  reasoning: a person picking this setting wants to know how long an entry actually survives, not a
  relative früh/mittel/spät ranking. `LearnedWordExpiryWindow`'s `EARLY`/`MEDIUM`/`LATE` (90/180/365 days)
  renamed to `ONE_MONTH`/`FOUR_MONTHS`/`ONE_YEAR` (30/120/365 days - real value change, not just relabelling:
  1/4/12 months instead of 3/6/12) and reordered so `NEVER` sits last, per the explicit requested order
  ("1 Monat/4 Monate/1 Jahr/Nie"). `DEFAULT` stays `NEVER` (§345, unchanged). New stored values
  `one_month`/`four_months`/`one_year`; all three languages' labels now concrete text. `LearnedWordExpirySweep`
  needed no change (reads `.days` generically). Existing tests updated in place, no new scenarios. 1171 unit
  tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec W-05/C-24 updated.
  `versionCode` 402 → 403, `versionName` `"1.0.98"` → `"1.0.99"`. **Not yet device-confirmed.**

- **§346 (v1.0.98): D-419 - every list-type setting now shows its currently selected value in the main**
  **settings list.** Explicit user request, prompted by C-06/C-24 both needing a tap into their own dialog
  to see what was set. C-04 (D-302) and the `LabeledSeekBarPreference` sliders (C-21/C-22) already did this
  correctly; the actual gap was every other plain `ListPreference`. Fixed by generalising
  `updateCalibrationSummary()`'s own "base description + live current value" shape into shared
  `SettingsFragment` helpers (`setupListPreferenceCurrentValueSummary`/
  `updateListPreferenceCurrentValueSummary`), wired for `c06_llm_threshold`/`d389_learned_word_expiry_window`
  both at screen build and via each preference's own change listener. New shared string
  `pref_current_value` ("Currently: %1$s"). No new tests (Android `Preference` view glue, already untested
  throughout). `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1171 unit tests unchanged. Spec: new
  D-419 note under §20. `versionCode` 401 → 402, `versionName` `"1.0.97"` → `"1.0.98"`. **Not yet
  device-confirmed.**

- **§345 (v1.0.97): D-389-followup - added a "Nie" (Never) option to C-24, made it the default.**
  Immediate feedback right after §344: expiring learned words should be opt-in, not opt-out by default.
  `LearnedWordExpiryWindow` gained a fourth `NEVER` value (`days: Int?` = null - a genuine "no expiry"
  state, not a sentinel large number), `DEFAULT` moved from `MEDIUM` to `NEVER`, `LearnedWordExpirySweep`
  short-circuits to an empty result when `window.days` is null. New stored value `"never"`, placed first in
  the enum/arrays (mirroring C-06's own "off state leads" ordering), `ListPreference` default changed to
  `"never"`, all three languages' labels added ("Nie"/"Never"/"Ποτέ"). 2 new tests; every §344 test still
  passes. 1169 → 1171 unit tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec
  W-05/C-24 updated. `versionCode` 400 → 401, `versionName` `"1.0.96"` → `"1.0.97"`. **Not yet
  device-confirmed.**

- **§344 (v1.0.96): D-389 implemented - learned words now expire after a configurable period of disuse.**
  Design confirmed with the user first: 3/6/12 months for früh/mittel/spät (new `LearnedWordExpiryWindow`
  enum, `EARLY`/`MEDIUM`/`LATE`, default `MEDIUM`), scoped to individual learned words only (bigrams/trigrams
  have no `last_touched` column yet), a once-a-day sweep, new C-24 setting directly beneath the Learned Words
  editor's own row. New pure `LearnedWordExpirySweep.sweep(store, now, window)` reuses only existing
  `DictionaryStore` methods (`learnedWords()`/`learnedFrequencyOf()`/`forget()` - the same permanent removal
  G-04's drag-to-trash performs) - no new interface method or SQL needed. `AdaptKeyService.
  maybeSweepExpiredLearnedWords()` runs on `loadDictionariesAsync()`'s own background thread, across every
  installed language's own store, throttled against a new internal-only stored timestamp (excluded from
  backup export). New setting wired through the full existing pipeline (`SettingsStore`/`RawSettings`/
  `SettingsMapper`/`AdaptSettings`), localised into all three languages. 12 new tests
  (`LearnedWordExpiryWindowTest`, `LearnedWordExpirySweepTest`, 2 `SettingsMapperTest` cases); every
  pre-existing test passes unchanged. 1157 → 1169 unit tests, all green. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. Spec: new W-05 (§13) and C-24 (§10 settings table). `versionCode` 399 →
  400, `versionName` `"1.0.95"` → `"1.0.96"`. **Not yet device-confirmed** - the daily throttle and
  multi-language sweep are real-device-timing behaviour no unit test can exercise end to end.

- **§343 (v1.0.95): D-414-followup (v2) - the Reclaim chip could show with nothing genuinely at the caret.**
  Root cause: `reclaimPossible()` read `getTextBeforeCursor`/`getTextAfterCursor` as two separate Binder
  round-trips - exactly the pattern `reclaimSurroundingWord()`'s own D-347 v2 fix already moved away from,
  after a real device log showed two independent calls observing two different document snapshots on a
  fast-moving caret in Gemini's own search field. Fixed by reusing that exact mechanism instead of a
  hand-rolled reimplementation: one atomic `getExtractedText()` round-trip fed straight into
  `WordExtent.reclaim()`, the same ground-truth function the real reclaim commits with. No new tests (same
  untested `InputConnection` glue). `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1157 unit tests
  unchanged. Spec S-10 gained a short addendum. `versionCode` 398 → 399, `versionName` `"1.0.94"` →
  `"1.0.95"`. **Not yet device-confirmed.**

- **§342 (v1.0.94): D-414-followup's Reclaim button migrated into the suggestion bar; G-05's double-tap now**
  **reclaims first; D-416's "quiet dot" no longer sticks after a plain caret move.** (1) The Reclaim button
  (§341 confirmed it lights up correctly in Gemini, but it still lived in the extra row) moved into the
  suggestion bar itself as a true visibility toggle, per explicit request. New
  `SuggestionController.Kind.RECLAIM`, pinned by `showSuggestions()` whenever the bar would otherwise be empty
  and a new `reclaimPossible(ic)` predicate is true (replaces `updateReclaimEnabled()`, now also folding in
  the login/URL/no-suggestions-field guard). `armShiftForNextWord()` and the suppression-independent
  `reclaimEnabledRunnable` now call `showSuggestions()` directly instead of pushing to the extra row; a tap
  dispatches to the same `reclaimWordAtCaret()` as before. The extra-row button and all its supporting code
  were removed from `ExtraRowView` entirely, not left duplicated. (2) G-05's double-tap-Shift toggle only
  ever flipped an already-*composing* word, so it silently did nothing in Gemini (reactive reclaim
  suppressed there, D-351) until the word happened to already be composing. `toggleWordStartImmediate()` now
  reclaims first (`reclaimWordAtCaret()`) when nothing is composing yet - the same safe, unconditional,
  suppression-bypassing call the chip/Backspace already use. (3) The deferred-space "quiet dot" above the
  space key was only ever re-evaluated inside `armShiftForNextWord()` (commit/field-entry) or a
  new-word-starts-composing keystroke - a plain caret move away from the punctuation mark with nothing typed
  left it lit indefinitely. Factored into `updatePendingSpaceIndicator(ic)`, now also called from
  `reclaimEnabledRunnable` on the same debounced composing-empty-caret-move cadence the Reclaim chip already
  uses. No new tests (all three live in the same untested `AdaptKeyService`/`InputConnection`/view glue).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1157 unit tests unchanged. Spec: R-01 rewritten
  (Reclaim moved to new §5 S-10), G-05 and A-12/D-416 each gained an addendum. `versionCode` 397 → 398,
  `versionName` `"1.0.93"` → `"1.0.94"`. **Not yet device-confirmed.**

- **§341 (v1.0.93): four fixes from real device feedback on D-362/D-416/D-414.** (1) The loading chip's dots
  now start full and bounce (`3→2→1→2→3…`) instead of cycling forward from one - the chip is often visible
  only briefly, so the old cycle mostly showed a single, glitch-looking dot. (2) An explicit lower-case
  override right after a sentence-ending mark (`.`/`!`/`?`, never a comma) now also suppresses the deferred
  space, with **no new tracked state** - `isUpperArmed()` read live at the exact moment already tells the
  whole story, since a terminator's own capital is armed by default and can only be off there because the
  user just explicitly disarmed it. New `shouldMaterializeSpace()`/`SENTENCE_TERMINATORS`. (3) The Reclaim
  button's enabled state was gated by the same suppression it exists to work around - it could never light up
  for the exact scenario (tap mid-word in Gemini) it was built for. Fixed with a genuinely separate, always-
  debounced push (`reclaimEnabledRunnable`), never gated by `reclaimOnCaretMoveSuppressed`, but still
  debounced to avoid re-hammering IPC calls at drag frequency. (4) A plain Backspace now also reclaims in a
  suppressed field (`deleteOneBefore()` confirmed to never call `setComposingRegion` at all - the suppression
  was only ever about the drag case) - but a *held* Backspace reclaims only once, at release
  (`OnBackspaceRepeatEndListener`), never per tick, per the user's own explicit performance concern (D-138).
  No new tests (all Android view/`InputConnection` glue). `:app:assembleRelease`/`:app:testDebugUnitTest`
  green, 1157 unit tests unchanged. Spec: A-12, S-01/D-362, and R-01/D-414 all updated. `versionCode` 396 →
  397, `versionName` `"1.0.92"` → `"1.0.93"`. **Not yet device-confirmed - four real behaviour changes in one**
  **round, worth validating individually.**

- **§340 (v1.0.92): D-365/D-366 - a self-learned bigram/trigram now competes fairly, and the trigram**
  **signal survives once typing starts.** Both discussed with the user first, per this project's own
  weighting-decision convention. D-365 answered with real numbers (bundled bigram data: 25-113,526, median
  48, p90 ~193, p95 ~340) and fixed exactly like D-411 fixed the same problem for learned words: new
  `LearnedBigramBoost` (log-scaled, `REFERENCE_COUNT` 50 for cross-app consistency, `REFERENCE_FREQUENCY`
  250 recalibrated for the bigram scale, no recency factor per explicit instruction - `TABLE_LEARNED_BIGRAMS`
  has no timestamp column yet), applied only to the learned share via new `rankingBigramFrequency()` -
  `bigramFrequency()` itself stays untouched, since `TokenRepair`'s `>= MIN_BIGRAM` merge gate (A-06) must
  keep reading the raw count. D-366's own premise only partially held up on inspection: plain bigram signal
  already reached prefix-completion ranking via `score()` - what was actually missing was the *trigram*
  signal, wired only into S-07's blank-slate `nextWordSuggestions()`. `score()` gained an optional
  `previousPreviousWord` parameter and the same Stupid Backoff blend, threaded through `suggestionsFor()`
  (default `null`, zero existing test call sites needed changes) and `AdaptKeyService`'s two real call sites.
  Deliberately scoped to `DictionarySuggestionProvider` only - `TokenRepair`'s own separate A-05 split-scoring
  left untouched, per explicit instruction. 9 new tests (`LearnedBigramBoostTest`, two
  `InMemoryDictionaryStoreTest`, two `DictionarySuggestionProviderTest` integration cases demonstrating both
  fixes end to end); every pre-existing test passes unchanged. 1148 → 1157 unit tests, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec S-07 gained both addenda. `versionCode` 395 →
  396, `versionName` `"1.0.91"` → `"1.0.92"`. **Not yet device-confirmed.**

- **§339 (v1.0.91): D-364 - root-caused and fixed: the duplicate "Text" chip was a case-sensitive dedup**
  **bug, not tier-3.** First hypothesis (tier-1 vs. tier-3 producing the same word in different casing via
  `SuggestionMerger.merge()`) traced all the way through and ruled out directly by the user - no tier-3 model
  installed. Their own detail ("es sind genau die ersten beiden Chips... beide vorne groß geschrieben")
  repointed the trace correctly: `SuggestionController.displayed()`'s `alreadyShown` de-dup compared the
  pending-replacement chip's word (pre-capitalised by `AdaptKeyService`, D-111/D-112's own deliberate "preview
  the eventual committed casing" reason) against `stableOrder`'s raw, uncapitalised canonical dictionary words
  ("text" vs. "Text") case-sensitively - confirmed against the real dictionary asset that `text` is stored
  lowercase. Fixed with a minimal, targeted change: the comparison itself is now case-insensitive
  (`word.lowercase()` on both sides), not the underlying capitalisation convention, keeping
  `SuggestionController`'s own "free of any capitalisation/Android dependency" design intact. New test
  reproduces the exact reported shape directly (no tier-3/Android dependency needed to exercise it); every
  pre-existing test (including the identical-casing dedup case) still passes unchanged. 1147 → 1148 unit
  tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec S-06 gained a short addendum.
  `versionCode` 394 → 395, `versionName` `"1.0.90"` → `"1.0.91"`. **Not yet device-confirmed.**

- **§338 (v1.0.90): D-362 - the loading-indicator chip, from static/subtle to bold/large/animated.**
  D-346's original "…" was a plain 16sp italic grey `TextView`, easy to miss next to ordinary suggestions of
  the same visual weight. Discussed with the user first (concrete proposal, before writing code); their
  reaction: "Ich hatte etwas anders im Sinn. Aber es gefällt mir sehr gut so." Now bold, 20sp, a dedicated new
  colour (`suggestion_loading_text`, `#F57C00` Material Amber 700 - deliberately its own meaning, not reused
  from the verbatim chip's blue or the search-query chip's grey), ticking `.`/`..`/`...` every 400 ms. The
  ticker is tied to the chip's own `OnAttachStateChangeListener` (`post()`/`removeCallbacks()`), starting/
  stopping automatically as `setItems()` rebuilds the bar - no external field, no explicit cleanup call, no
  leak risk. Spec §32 updated. No new tests (`SuggestionBarView` has no existing test file - Android view
  glue). `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1147 unit tests unchanged. `versionCode` 393
  → 394, `versionName` `"1.0.89"` → `"1.0.90"`. **Not yet device-confirmed.**

- **§337 (v1.0.89): D-414 - manual Reclaim button; the Cycle half explicitly shelved after design**
  **discussion, not implemented.** A new "🧲" button in the extra row (§14, R-01), left side, fixed slot 4
  (never collides with `emojiButton`'s own two possible positions). Tapping it calls `reclaimWordAtCaret()`
  directly - confirmed by reading the function that it never checks `reclaimOnCaretMoveSuppressed` itself
  (only `onUpdateSelection`'s own *scheduling* of the reactive call does), so a direct call already fires
  immediately and already bypasses the suppression unconditionally, no new logic needed for either property.
  Exactly what D-351/D-351-followup's suppressed fields (Gemini, Total Commander) need; available in every
  field, not gated to those two. Enabled state (`ExtraRowView.reclaimEnabled`, dimmed via alpha) pushed live
  from `armShiftForNextWord()` - the same central point D-416's `pendingSpaceIndicator` uses, extended with
  one more live check equivalent to `WordExtent.reclaim`'s own "found nothing" case. The originally-floated
  Cycle half (cycle through suggestion candidates on an already-reclaimed word) was discussed and dropped by
  the user's own call after tracing through real problems with it (frozen-list snapshot needed, open
  Backspace/casing questions) - shelved pending a genuinely new concept, not tracked as an open item. Spec
  R-01 updated. No new tests (Android glue). `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1147 unit
  tests unchanged. `versionCode` 392 → 393, `versionName` `"1.0.88"` → `"1.0.89"`. **Not yet device-confirmed.**

- **§336 (v1.0.88): D-415 - WON'T FIX, device-confirmed.** The §335 experiment ran on a real device against
  Google Keep: `KEYCODE_BACK` did nothing at all; `performEditorAction(IME_ACTION_DONE)` hid the keyboard but
  left focus on the field exactly as before. A clean negative result for both candidate levers - confirms an
  `InputMethodService` genuinely cannot make a host app release focus it does not choose to give up, a real
  platform limitation, not a missing trick to keep chasing. Both temporary test buttons removed exactly as
  promised (`OnTestBackClickListener`/`OnTestDoneClickListener`, their fields/`addView` calls, wiring, and
  `testGiveUpFocusViaBack()`/`testGiveUpFocusViaDone()` all deleted). No new tests (Android glue).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1147 unit tests unchanged. `versionCode` 391 → 392,
  `versionName` `"1.0.87"` → `"1.0.88"`. **Device-confirmed** (the test itself was the confirmation).

- **§335 (v1.0.87): D-415 - two temporary diagnostic buttons to test both "give up focus" levers on a**
  **real device.** No IME has a public API to clear a host view's focus - the two indirect levers
  (`KEYCODE_BACK`, `performEditorAction(IME_ACTION_DONE)`) only work if the target app's own code reacts to
  them, entirely unverified until actually tried. Added "🔙"/"🏁" to the extra row's right side (two more
  slots left of the touch-zone/settings pair), wired to `sendDownUpKeyEvents(KeyEvent.KEYCODE_BACK)` and
  `performEditorAction(IME_ACTION_DONE)` respectively - both already-used mechanisms elsewhere
  (`handleEnter`), just newly exposed as a manual probe. Every new piece explicitly marked as temporary
  diagnostic scaffolding in its own KDoc, meant to be removed entirely once the experiment concludes,
  whichever way it goes. No new tests (Android glue). `:app:assembleRelease`/`:app:testDebugUnitTest` green,
  1147 unit tests unchanged. `versionCode` 390 → 391, `versionName` `"1.0.86"` → `"1.0.87"`. **Not yet
  device-tested - this round's entire purpose is the device test itself, next.**

- **§334 (v1.0.86): D-416-followup - two real bugs found on first read-through, before any device test.**
  (1) The space-key dot stayed lit for the whole following word, not just its first letter -
  `pendingSpaceIndicator` is only recomputed inside `armShiftForNextWord()`, which does not run again
  mid-word, so nothing told the view the pending state had already resolved. Fixed with an explicit
  `keyboardView?.pendingSpaceIndicator = false` right at the materialisation point in `handleKey`'s `CHAR`
  branch. (2) `appendLongPressLetter()` (the entry point for `ä`/`ö`/`ü`/`ß` and other long-press
  alternatives starting a brand-new word) never materialised the deferred space at all - a real gap missed
  in §333, since D-351's own KDoc already names this as the *other* typing-triggered entry point alongside
  the `CHAR` branch. Fixed with the identical live-derived check. Verified, not assumed: the digit-glue/
  punctuation-run cases inside `handlePunctuationDelimiter` do **not** share this staleness problem -
  `finalizeAndCommit()`'s own internal `armShiftForNextWord` call already covers them. No new tests (Android
  glue). `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1147 unit tests unchanged. `versionCode` 389
  → 390, `versionName` `"1.0.85"` → `"1.0.86"`. **Not yet device-confirmed.**

- **§333 (v1.0.85): D-416 - A-12's auto-space after sentence punctuation, from eager to deferred - the**
  **biggest single behaviour change this project has shipped in one round.** Fully designed and agreed with
  the user across several rounds before any code was written - see
  [`AdaptKey-Plan-D416-Deferred-Space.md`](AdaptKey-Plan-D416-Deferred-Space.md) (kept, not deleted) for the
  full design pass and [`AdaptKey-Rollback-D416-Deferred-Space.md`](AdaptKey-Rollback-D416-Deferred-Space.md)
  for the precise rollback account, both explicit user-requested deliverables of this round, not incidental.
  - **What changed:** `.`/`!`/`?`/`,` no longer insert a real space the instant they commit. They arm a live,
    re-derived pending state instead (unifying with §6 rule 2's own `armShiftForNextWord` pre-arm philosophy),
    materialised only on the next real keystroke - a letter/digit gets a real space first; another mark from
    the same set glues onto the previous one (a run); a digit continuing a decimal number (D-320) glues onto
    the mark instead. Nothing is ever tracked in a stored flag - "is a space pending right here" is re-derived
    live from the real document every time, the same discipline A-07's undo already uses.
  - **Two real findings from actually reading the code during the design pass, not assumed:**
    `SentenceBoundary.isSentenceStart` requires physical trailing whitespace to recognise a sentence start at
    all - fixed with a new, still-pure `SentenceBoundary.withPendingTerminatorSpace()` used by every
    `armShiftForNextWord` call site via `sentenceStartBefore(ic)`. And the plan's own predicted
    "two-step Backspace" for a sentence-ending correction turned out to only hold with
    `doubleTapBackspaceUndo` on - with it off, the very first Backspace already reverts immediately today for
    an ordinary correction, so removing the old D-273 priority carve-out outright (not replacing it) makes a
    sentence-ending correction behave consistently with every other one for the first time, a genuine
    harmonising improvement discovered mid-implementation.
  - **Removed outright, not simplified:** `pendingPunctuationSpace`/`pendingPunctuationSpacePos`, the whole
    `consumeStrandedPunctuationSpace()` function (D-279), `handleBackspace()`'s D-262/D-406 phantom-space
    guard, `handleEnter()`'s D-270 remove-before-newline block, and the D-273 undo-priority carve-out - five
    pieces of machinery that only ever existed to manage a physical space this design eliminates entirely.
  - **New, additive:** a quiet dot above the space key's own label (`AdaptKeyboardView.pendingSpaceIndicator`)
    while the deferred state is armed - the user's own explicit ask once the idea was explained, mirroring
    S-05's "quiet confirmation" philosophy for a non-word pending state.
  - **Explicitly out of scope, corrected from the original backlog framing:** D-373 turned out unrelated to
    A-12 entirely (a hyphen never got its own auto-space) - stays open, independently. D-384 (space-preceded
    minus) is genuinely eased by the new model but not implemented here. D-363 (colon/semicolon vs. emoticons)
    stays a deliberately separate, not-yet-decided follow-up even though its blocking objection no longer
    applies.
  - **No settings toggle**, per the user's own explicit call ("das skaliert gar nicht gut") - the rollback
    doc plus a clean, isolated commit is the agreed safety net instead.
  - **The one permanent, user-visible behaviour change:** a Backspace right after `.`/`!`/`?`/`,` with
    nothing typed since now deletes the mark itself directly on the first press, not a phantom space first -
    flagged to the user during design and accepted as a deliberate trade.
  - `PunctuationSpaceGlue.gluesDigit()`'s contract changed from a 3-character (digit, mark, already-inserted
    space) to a 2-character (digit, mark, directly adjacent) pattern - its test rewritten in step, same 11
    cases. `SentenceBoundaryTest` gained new `withPendingTerminatorSpace` cases; its pre-existing
    `isSentenceStart` cases are all untouched, confirming that contract genuinely was not modified. 1141 ->
    1147 unit tests, all green. Spec: A-12 rewritten to describe the current deferred behaviour (not
    appended to); A-07 and two historical D-406 mentions updated for accuracy; S-05 cross-references the new
    indicator.
  - `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 388 → 389, `versionName` `"1.0.84"`
    → `"1.0.85"`. **Not yet device-confirmed** - flagged as needing early, repeated real-device validation
    given the muscle-memory stakes, per the design plan's own §5.

- **§332 (v1.0.84): D-417 - reordered the `2` key's popup so the apostrophe sits ahead of the superscript.**
  D-382 (§330, v1.0.82) shipped the order `"` / `²` / `'` / `₂`; confirmed working on-device, but the user
  wanted the apostrophe moved right after the shifted symbol. `KeyboardLayout.numberKey()`'s `'2'` branch
  (shared by QWERTZ/QWERTY, L-01 - `AzertyLayout`/`GreekLayout` have their own separate implementation and
  are unaffected) reordered to `listOf(hint, "'", superscript, "₂")`, giving `"` / `'` / `²` / `₂`. Spec's
  L-06 and the KDoc updated; `KeyboardLayoutTest`'s existing D-382 assertion updated in place (no new test).
  `:app:assembleRelease`/`:app:testDebugUnitTest` green, 1141 unit tests unchanged. `versionCode` 387 -> 388,
  `versionName` `"1.0.83"` -> `"1.0.84"`. **Not yet device-confirmed.**

- **§331 (v1.0.83): D-400 - the keyboard layout is pinned to the system language, decoupled from the**
  **active dictionary language.** Design discussed directly with the user before implementing (this
  project's own convention for non-trivial decisions) - see the conversation itself for the full back-and-
  forth; summary of what shipped:
  - **Root problem, confirmed from the actual code, not assumed:** D-130's automatic sustained-English
    promotion hard-set `keyboardView.layoutKind = LayoutKind.LATIN_QWERTY` unconditionally
    ([AdaptKeyService.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt), inside
    `trackSustainedEnglishUsage()`) - typing 5 English words while German (QWERTZ) was active silently
    flipped the physical layout to QWERTY, a real, reproducible surprise for a purely cosmetic Y/Z
    difference. The manual G-01 swipe (`toggleLanguage()`) and app/field startup (`onStartInputView()`)
    instead called `LayoutRegistry.kindFor(activeLanguage)` via the shared `applyActiveLanguageToView()`.
  - **Key finding during discussion that resolved an earlier false concern:** D-130's automatic promotion
    can *only* ever target English, and can only ever fire while the active language is already
    Latin-typeable (Greek's own layout has no Latin key positions, so the English words that would trigger
    it could never actually be typed while Greek is active) - so the automatic path never needs to touch
    the layout at all, not even conditionally. The line was simply deleted, no replacement.
  - **New shared resolution, `LayoutRegistry.kindFor(systemLocale: Locale, activeLanguage: Language):
    LayoutKind`** (pure, unit-tested, reuses the existing `Language.fromCode()` lookup rather than
    duplicating locale-matching logic): resolves to `activeLanguage`'s own layout when it is non-Latin
    (Greek - always wins, otherwise physically untypeable); otherwise to the system language's own layout
    when the system language is itself a recognised Latin one; otherwise (system language is Greek, or one
    this app has no entry for at all) falls back to `activeLanguage`'s own layout directly - the user's own
    late refinement during discussion ("hier wird ohnehin klar umgeschaltet, dann kann man auch direkt das
    passende Layout nehmen"), added after the initial "always pin to system language" framing turned out to
    have no sensible answer for a Greek-system-language device explicitly switching to German or French.
  - `applyActiveLanguageToView()` (the single place feeding `AdaptKeyboardView.layoutKind`, shared by
    `onStartInputView`/`toggleLanguage`/`installStores`'s removed-language fallback) now calls the new
    two-argument overload with `Locale.getDefault()`. New `AdaptKeyService.onConfigurationChanged()`
    override re-derives it on a live system-language change while the keyboard happens to be open (the
    user's own "nice to have, not important, this doesn't happen often" ask - cheap enough to include).
  - 5 new tests (`LayoutRegistryTest`, new file - the class had none before). 1136 → 1141 unit tests. Spec's
    G-01 gained a D-400 addendum explaining the pin + both exceptions; `LayoutRegistry.kindFor()`'s own KDoc
    carries the full resolution-order reasoning.
  - `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 386 → 387, `versionName` `"1.0.82"`
    → `"1.0.83"`. **Device-confirmed.**

- **§330 (v1.0.82): a batch of seven small, independently-decided backlog items - D-379, D-382, D-394**
  **(digit-mirror half only), D-398, D-399 implemented; D-375 and D-381 confirmed with no code change.**
  User pre-decided each item individually (no open design questions, unlike the project's usual "discuss
  first" convention for non-trivial choices), so all seven were handled in one round:
  - **D-379**: `"bzgl."` added to `Abbreviations.GERMAN` ([Abbreviations.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/capitalisation/Abbreviations.kt)), alongside the existing `abzgl.`/`zzgl.` family - a sentence start is no longer wrongly assumed right after it.
  - **D-382**: `KeyboardLayout.numberKey('2')` gained an apostrophe (`'`) and subscript (`₂`) as a third/
    fourth long-press alternative, alongside the existing shifted-symbol (`"`) and superscript (`²`) pair -
    shared code between QWERTZ and QWERTY (L-01), so both get it identically; every other digit unaffected.
  - **D-394**: the calculator page's digit block (`SymbolLayout.calculatorRows()`) mirrored vertically to
    the phone-style `1 2 3` / `4 5 6` / `7 8 9` / `0` order (was calculator-style `7 8 9` / `4 5 6` /
    `1 2 3` / `0`) - only the digit keys moved; the operator column, currency/decimal/ABC/Enter cells and
    their row positions are untouched. The original ask's T9-letter-long-press half was explicitly declined
    by the user ("streichen wir aber ersatzlos") - dropped from scope, not deferred.
  - **D-398**: the D-130 automatic-language-switch threshold (formerly a hardcoded `SUSTAINED_ENGLISH_
    WORD_THRESHOLD = 5` in `AdaptKeyService`) is now a real setting - new C-23, `sustainedLanguageSwitch
    Threshold` threaded through `RawSettings`/`SettingsStore`/`SettingsMapper`/`AdaptSettings` the same way
    every other slider setting is, clamped 0-8, default 5, exposed as a `SeekBarPreference` in the
    Dictionary category right after the language-packs link (the "language section" the user asked for).
    `trackSustainedEnglishUsage()` now reads `settings.sustainedLanguageSwitchThreshold`; a stored `0`
    disables the whole promotion (checked explicitly, not merely "compare against 0" which would have
    fired after zero consecutive words) - only the manual G-01 swipe still changes the active language.
  - **D-399**: `SettingsMapper.MIN_MAX_SUGGESTIONS` lowered from 6 to 3; the C-03 `SeekBarPreference`'s
    `app:min` updated to match. `MAX_MAX_SUGGESTIONS` (10) unchanged.
  - **D-375** (no code change): user confirmed on real-device testing that `"sollendafur"` no longer gets
    silently auto-unfolded/split the way originally reported. No dedicated fix targeted it this round or
    any prior one identifiably - likely a side effect of the intervening dictionary cleanup rounds (§301+)
    or a later A-05/umlaut-interaction refinement; not root-caused further since there is nothing left to
    fix. Marked resolved on the strength of the user's own direct repro-no-longer-reproduces report.
  - **D-381** (no code change): user confirmed on-device that D-404 §323/§324's Learned Words editor
    (category multi-select, LLM-determined with a tier-3 model installed) fully satisfies the original
    "learned word's POS tag should be user-editable" ask - the "worth a fresh look" this bullet had been
    carrying is now actually closed, not just assumed.

  6 new/updated unit tests (`AbbreviationsTest` +1 assertion in-place; `KeyboardLayoutTest` split into two,
  net +1; `SymbolLayoutTest` three assertions updated in-place, no new tests; `SettingsMapperTest` +1).
  1134 → 1136 unit tests. Spec: L-06 (D-382), L-07 (D-394), G-01/A-03 (D-398), S-01 (D-399), and the §20
  C-03/new-C-23 settings table rows all updated to the current, crystallised state.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 385 → 386, `versionName` `"1.0.81"` →
  `"1.0.82"`. **Not yet device-confirmed** (D-379/D-382/D-394/D-398/D-399's own new behaviour) - needs a real
  device check: `bzgl.` no longer triggers a false sentence start; the `2` key's popup shows all four
  alternatives; the calculator digit block reads in the new order; the new language-switch-threshold slider
  appears under Dictionary settings and actually gates the automatic switch (including at 0 = off); the
  max-suggestions slider now reaches down to 3.

- **§329 (v1.0.81): D-413 - reverted D-344/D-386's SAF folder-grant import back to a plain
  `ACTION_OPEN_DOCUMENT` single-file picker; D-363 declined as Won't Fix.** Real-device report: trying to
  import the German language pack, the system refused to grant the Downloads folder at all via
  `ACTION_OPEN_DOCUMENT_TREE` ("Dieser Ordner kann nicht verwendet werden. Zum Schutz deiner Daten einen
  anderen Ordner auswählen.") - a platform-level SAF restriction (worsened by Samsung One UI's own download
  sandboxing) on exactly the folder D-386's whole automatic-resolution mechanism was built around, not an
  app bug. Discussed directly: reverting to a manual single-file pick was the only viable path (a folder that
  cannot be granted has no file to automatically resolve within it either). Removed `download.
  DownloadFolderStore` (persisted tree grant) and `download.DuplicateDownloadMatcher`/`DownloadCandidate`
  (browser-`" (N)"`-duplicate matching, no longer needed once the user picks the exact file again) entirely,
  including `DuplicateDownloadMatcherTest`. `download.DownloadFolderResolver` renamed to `download.
  DownloadFileSupport`, keeping only what still applies to a single picked document unchanged:
  `downloadsInitialUriHint()` (the `EXTRA_INITIAL_URI` Downloads-folder hint) and `deleteIfRecentlyCreated()`/
  `isRecentlyCreated()`/`DELETE_MAX_AGE_MILLIS` (D-386's post-import cleanup, kept at the user's own explicit
  request even though the file is picked manually again). `LanguagePacksActivity`/`Tier3ModelActivity`:
  `ACTION_OPEN_DOCUMENT_TREE` + persisted-grant reuse + the upfront "why we need a folder" rationale dialog
  are all gone; "Import" now launches `ACTION_OPEN_DOCUMENT` (`CATEGORY_OPENABLE`, `type = "*/*"`,
  `FLAG_GRANT_WRITE_URI_PERMISSION` for the delete-after-import to actually work) fresh every time and
  imports the result directly. D-386-followup's content-level checks (`LanguagePackInstaller.
  compareVersions`/`LanguageMismatchException`) are untouched and now do double duty as the safety net a
  folder-scan design no longer needs but a "trust the user's own pick" design still benefits from. Four now-
  unused `d386_grant_folder_*`/`d386_file_not_found` strings removed from all three locales (en/de/el);
  `c06_model_privacy`'s user-facing text corrected in all three (no longer claims a one-time folder grant).
  Spec §30 rewritten to describe the current single-file-picker mechanism as the crystallised state, with
  D-344/D-386's folder-grant attempt kept as explained, superseded history rather than silently deleted.
  1144 → 1134 unit tests (10 `DuplicateDownloadMatcherTest` cases removed; `DownloadFolderResolverTest`
  renamed to `DownloadFileSupportTest`, same 4 cases, unchanged). `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 384 → 385, `versionName` `"1.0.80"` → `"1.0.81"`.
  **Separately, no code change:** D-363 (colon/semicolon arming A-12's punctuation-auto-space) discussed and
  declined as Won't Fix - unconditionally arming `:`/`;` collides with text emoticons (`:)`, `;)`, ...) far
  more often than the time-of-day case that originally motivated it; a "only arm when a letter immediately
  precedes the mark" gate was worked out (subsumes the digit exception, protects the common
  space-or-message-start emoticon case) but the user judged the remaining complexity/risk not worth it. See
  the "Open TODOs" list above for both items' updated status. **Device-confirmed** - a real import via the
  new single-file picker succeeded on the same device that hit the original Downloads-folder-grant refusal.

## Older Rounds (§1-§328, v0.7.6 through v1.0.80) - Pruned From This File

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
those 328 rounds eventually produced. A round below §329 that is still unconfirmed on-device and not
otherwise tracked in those sections should be treated as abandoned/superseded, not as silently-still-pending
work - if genuinely in doubt about one specific item, check its own `§N` entry in History.md rather than
assuming either way.

Retention policy for this section going forward: keep the ~20-25 most recent rounds (enough for a fresh
session's own continuity) plus anything not yet device-confirmed; prune older, already-confirmed rounds the
same way, without summarising them - they stay permanently retrievable in History.md. **2026-09-01 second
pruning pass (§260-§328 removed, cutoff moved from §260 to §329):** in practice, "not yet device-confirmed"
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
