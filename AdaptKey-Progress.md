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
  - **D-380 - OPEN.** A long-press smear too small to trigger a swipe/page-change should still open the alt
    popup - the `o` key is named as the one that frequently fails to.
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

- **§391 (v1.1.30): D-361 - fast Backspace typing let neighbour keys (Enter reported specifically) react**
  **instead of Backspace.** Design discussed first per this project's own convention (touches key
  hit-testing/`resolveKey`): two ideas were weighed - (A) temporarily grow Backspace's own touch zone while
  typing fast, (B) retroactively reinterpret a wrong neighbour-tap as Backspace after the fact. Flagged to the
  user that B has gotten meaningfully riskier since D-393 (same round): a wrongly-landed Enter tap can now
  genuinely submit a real action (search, send, login) in more fields, which is not undoable after the fact.
  User's own call: implement A only, shelve B (not declined outright - revisit only if A alone proves
  insufficient).

  **A's mechanism**, entirely in `AdaptKeyboardView` (the class already owning `resolveKey()`/D-231/D-233's
  own static Backspace/Enter/`m` drift caps): a new `lastBackspaceActivationAtMs` timestamp, stamped on every
  resolved Backspace `ACTION_DOWN` and on every accelerating-hold repeat tick (`scheduleBackspaceRepeat()`'s
  own runnable) so a still-active hold keeps the window freshly armed too. `resolveKey()` now checks, right
  after the existing Space special-case and before the offset-model resolution (so it is not diluted by a
  neighbour's own learned drift): while `backspaceStickyEnabled` and within `backspaceStickyDelayMs` of that
  timestamp, a new `isWithinBackspaceStickyZone()` resolves a tap to Backspace instead of whichever key it
  actually landed in, for the near portion (35%, `BACKSPACE_STICKY_ZONE_FRACTION`) of any key genuinely
  geometrically adjacent to Backspace's own rendered rect - adjacency and direction (above/below/left/right)
  are derived from the live `keyRects` geometry each time (an edge lining up within `gapPx` plus slack, with
  real overlap required on the perpendicular axis too), never a hardcoded per-language key list, so it holds
  unchanged for whichever key actually sits there on the active layout/surface.

  **Setting**: user found the originally-proposed idea of a brand-new duration slider unnecessary once shown
  the existing "Doppel-Tipp-Shift-Verzögerung" (`doubleTapDelayMs`, G-05, default 400 ms/200-800 ms range) -
  "das kann sich exakt an den generellen Doppel-Tap-Delay hängen." Implemented as a single new boolean,
  `backspaceStickyEnabled` (D-361, default **on**), added the same way as the D-348 double-tap-Backspace-undo
  toggle right next to it in the settings layer (`AdaptSettings`/`SettingsMapper`/`SettingsStore`, new
  `SwitchPreferenceCompat` in the Layout category right after the enlarged-backspace-width slider, localised
  de/en/el) - `backspaceStickyDelayMs` itself is not a stored setting at all, `applySettings()` feeds it
  `s.doubleTapDelayMs` directly every time settings are applied, so it always tracks that slider's current
  value live, with no separate value to fall out of sync.

  1 new test (`SettingsMapperTest`: the new flag passes through unchanged, defaulting to on) - the geometry
  itself (`isWithinBackspaceStickyZone`/`resolveKey`) is `AdaptKeyboardView` Android view glue, untested per
  this project's own convention, the same boundary `isWithinSpaceHitZone`/the offset-factor functions already
  sit on. 1243 -> 1244 unit tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec:
  L-04 gained the D-361 addendum. `versionCode` 446 -> 447, `versionName` `"1.1.29"` -> `"1.1.30"`. **Not yet
  device-confirmed** - needs real fast-Backspace-typing use (ideally including the originally-reported
  Backspace-into-Enter case) to confirm the sticky zone actually helps without getting in the way of a
  genuine, deliberate tap on the neighbour key.

- **§390 (v1.1.29): D-393 - Enter did nothing in the Google Play Store's own search bar instead of**
  **submitting the search.** Reported with a real device log (`AdaptKeyJitter`/`AdaptKeyHaptics`), root-caused
  directly rather than guessed: `com.android.vending`'s search field reports `inputType=0x228001`, which
  includes `TYPE_TEXT_FLAG_MULTI_LINE` (0x20000) despite being a genuine single-line, submit-on-enter search
  box. `handleEnter()` checked `multiLine` first and returned immediately with `finalizeAndCommit(ic, "\n")`
  whenever it was set, never reaching the action-handling code below at all - the log's own
  `finalizeAndCommit: typed="Whatsapp" -> finalWord="Whatsapp" delimiter="\n"` line is the literal proof: a
  raw newline was committed straight into the search bar, with no `performEditorAction`/submit anywhere
  afterward.

  Fixed by re-ordering the check: a field that declares a genuine IME action (Go/Search/Send/Done) and has not
  opted out via `IME_FLAG_NO_ENTER_ACTION` now always submits on Enter, regardless of whether `MULTI_LINE` is
  also set - `hasRealAction` is now computed first, and the `multiLine` newline branch only fires when
  `!hasRealAction`. The existing `IME_FLAG_NO_ENTER_ACTION` check (unchanged) is exactly the mechanism a
  genuinely multi-line field with its own action button elsewhere (e.g. a chat app's compose box) already
  uses to keep Enter as a plain newline despite declaring an action - confirmed this reordering does not
  affect that case, and does not affect D-383/Google Keep's own note-body field either (multi-line, no real
  action either way).

  Design discussed first per this project's own convention (a genuine behaviour change to Enter handling,
  affecting every app, not just Play Store) - user confirmed the re-ordering before implementation. One
  assumption remains unverified from the log alone (`imeOptions`/the actual declared action is not currently
  logged) - inferred from the field being an unambiguous search box, not proven bit-for-bit; flagged to the
  user as the one open question. Spec gained a new G-07 describing the resulting Submit-vs-Newline resolution
  order. No new tests (`AdaptKeyService.kt`'s `InputConnection`/`EditorInfo` glue, untested per this project's
  own convention). 1243 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `versionCode` 445 -> 446, `versionName` `"1.1.28"` -> `"1.1.29"`. **2026-09-03: device-confirmed.**

- **§389 (v1.1.28): D-383 - in Google Keep's list mode, pressing Enter with the caret right before a**
  **reclaimed word deleted that word.** Reported with a real device log (`AdaptKeyJitter`), root-caused from it
  directly rather than guessed: the field is multi-line (`inputType=0x2ac001`), so `handleEnter()` routes
  through `finalizeAndCommit(ic, "\n")`; since the caret sat exactly at the composing word's own start
  (`cursor=0`, not at its end), that took the mid-word split branch, `splitComposingAtCaretAndCommit()`. With
  the "before" half empty, that function's own first step (`ic.setComposingText("", 1)`) collapsed the *real*
  composing region to nothing - genuinely deleting the word from the document, not just resetting internal
  state - before re-inserting it moments later as fresh, still-provisional composing text at the new position
  after the newline. The log showed exactly this sequence, then an `onUpdateSelection` mismatch with "ground
  truth unavailable", then an immediate fresh `onStartInput` for the same package - Google Keep recreates the
  list item's own `InputConnection` the instant the committed `"\n"` splits the line, arriving before the
  re-inserted word ever reached the (now-superseded) old connection, so it was lost for good.

  Fixed narrowly, keyed off the same `beforeText.isEmpty()` condition already computed in
  `splitComposingAtCaretAndCommit()`: `ic.finishComposingText()` (commits the word for real, in place, deletes
  nothing) instead of `ic.setComposingText("", 1)` when there is no "before" half to shrink down to - the word
  becomes genuine, committed document text *before* the delimiter is ever touched, so nothing is at risk even
  if the host tears down its `InputConnection` immediately after. Because the word is now already real text at
  its final position rather than absent, the closing step also had to change: routing it back through the
  ordinary `updateComposing()` (which always calls `setComposingText`, i.e. *inserts* text at the cursor) would
  have inserted a second, duplicate copy right in front of the text already there - fixed by calling
  `ic.setComposingRegion(anchor, anchor + afterText.length)` directly in this one branch instead, marking the
  already-present range as composing without writing anything.

  Design discussed first per this project's own convention (the change touches exactly the composing-state/
  batch-edit machinery spec §1's guiding principle flags as historically fragile) - user confirmed the
  finishComposingText-first approach before it was implemented. Spec §1's guiding principle gained a fourth
  numbered invariant documenting this failure class (delete-then-reinsert of already-real content is unsafe
  whenever the host might tear down the `InputConnection` in direct reaction to the delimiter itself) for any
  future change in this area. No new tests (`AdaptKeyService.kt`'s `InputConnection` glue, untested per this
  project's own convention). 1243 unit tests unchanged, all green. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 444 -> 445, `versionName` `"1.1.27"` -> `"1.1.28"`. **2026-09-03:
  device-confirmed.**

- **§388 (v1.1.27): D-404 Tier 2 - `shouldOverrideKnownWord` now vetoes A-01's ratio override outright when**
  **the typed word and the candidate share a D-412 word family.** Design discussed first per this project's own
  convention (non-trivial algorithm decision): hard veto vs. a softer raised ratio bar, and scope (this one
  gate only vs. also touching ordinary ranking/`forUnknownToken`) - user confirmed hard veto, scope limited to
  `shouldOverrideKnownWord`. Two corrections surfaced during that discussion, both verified live rather than
  taken on trust: D-412 (see below) was already fully resolved, not "5 bands remain" as this file's own
  stale Open-TODOs text still said at the time (fixed separately first, commit `18a4503`); and `lemma`
  coverage is **not** German-only - D-422/D-424 (§368/§370) already extended the identical Wortfamilien
  parity to English and Greek, confirmed by directly counting non-empty `lemma` rows in all three dict.tsv
  files before writing anything (`en/dict.tsv` 43,799/116,388, `dictionaries/el/dict.tsv` 64,828/154,338,
  `dictionaries/de/dict.tsv` 84,429/188,252).

  New `DictionarySuggestionProvider.sameWordFamily(wordLower, candidateLower)`: resolves each side's family
  key via `store.entryOf(...)?.let { it.lemma ?: it.word }.lowercase()` (mirrors `LearnedWordExpirySweep`'s
  own D-389 grouping pattern, lower-cased explicitly since `lemma` is stored in the base entry's own
  canonical case, not a lower-case key) and compares them; `shouldOverrideKnownWord` returns `false`
  immediately on a match, before `CorrectionConfidence.forKnownWordOverride`'s ratio is even computed. Reuses
  `store.entryOf` (already merges bundled+learned lemma links, D-264/D-404) rather than a dedicated lookup,
  so a learned/LLM-derived family link (D-323/D-324) is honoured too, at no extra query cost worth
  mentioning. Deliberately narrow: `forUnknownToken` (ordinary typo correction) and suggestion-bar ranking are
  untouched - D-404 Tier 2's own framing was always specifically about A-01's override gate.

  3 new tests in `DictionarySuggestionProviderTest`: the motivating "Kugel"/"Kugeln" case at an extreme,
  would-otherwise-clear-every-threshold ratio; a shared-third-base pair ("lief"/"läuft", both linked to
  "laufen"); and a regression guard confirming an unrelated, unlinked pair (`ddr`/`der`) still overrides
  exactly as before. 1240 → 1243 unit tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. Spec: A-01 gained the D-404 Tier 2 addendum. `versionCode` 443 → 444, `versionName` `"1.1.26"` ->
  `"1.1.27"`. **2026-09-03: closed without a dedicated device test, per the user's own call** - pure
  dictionary/ranking logic with no directly observable behaviour to check for; will be revisited only if a
  negative report comes in.

- **§387 (v1.1.26): D-431-followup closed - real device log confirmed the gate fix works, found a genuinely**
  **structural (non-bug) limitation, and caught a real performance regression the diagnostic round itself had
  introduced.** Three distinct outcomes from one real `AdaptKeyA13` log, all confirmed from the log's own
  content, not inferred:

  1. **The D-431 gate fix is confirmed correct.** `hasObviousCandidate=false` for `"welxmche"` even with
     `"Welsche"` sitting in `candidates`, and `recover() candidates=[welche]` / `first known-word
     candidate=welche` - A-13 fires and finds the right word, exactly as designed.
  2. **A genuine, structural, not-a-bug limitation, confirmed from the same log**: an earlier attempt at this
     exact repro showed `composingTaps.size=0` against a 7-character reclaimed token
     (`reclaimSurroundingWord: before="welxmch"`) - traced to `reclaimSurroundingWord(ic, tap: TapPoint?)`'s
     own `if (tap != null) { composingTaps.addAll(...) }` guard: the D-421 initial-caret-position reclaim (a
     field reopening with the caret already inside existing text) passes `tap = null`, since there genuinely
     is no live tap history for text from a previous session - deliberately never fabricated. Because
     `MissedBackspaceRecovery`/`RawCoordinateCorrection` both require an exact `token.length == taps.size`
     match, a single reclaimed character anywhere in the token makes the whole thing ineligible. Confirmed not
     a regression: typing the same garbled word freshly, in one unbroken session (no field reopening), worked
     immediately - documented as a known, accepted boundary in spec A-13.
  3. **A real, self-inflicted performance regression, found in the very same log**: `handleKey: key=DELETE`
     entries during a backspace-hold on the still-long, unknown token took 370-400ms each (`welxmche`->`welxm`),
     dropping to 30ms once the token shortened - the classic profile of an expensive per-keystroke search this
     project has fought to keep off the hot path several times before (D-138/D-160/D-208/D-211/D-215). Root
     cause: §386's own diagnostic logging had extracted `provider.hasObviousCandidate(...)` into its own eager
     `val` so the gate decision could be logged, breaking the `||` chain's short-circuit evaluation that
     previously skipped this expensive call entirely whenever `duringRepeat` or `!includeExpensiveFallbacks`
     already made the result irrelevant - so it now ran on *every* keystroke, including every backspace-repeat
     tick. Fixed by restoring the original inline `if (duringRepeat || !includeExpensiveFallbacks ||
     provider.hasObviousCandidate(...))` short-circuit shape while removing the temporary logging (both changes
     land in the same edit, since the logging was the only reason the short-circuit had been broken up).

  `AdaptKeyA13`'s own temporary diagnostic logging (both in `refreshSuggestions()`'s gate and inside
  `missedBackspaceCorrection()`) fully removed now that it has served its purpose. No new tests (the fix
  restores previously-tested-and-shipped control flow rather than changing behaviour; the perf characteristic
  itself is not something this project's unit tests measure). 1240 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: A-13 gained the D-431-followup addendum
  documenting the reclaimed-token limitation. `versionCode` 442 -> 443, `versionName` `"1.1.25"` ->
  `"1.1.26"`. **2026-09-03: device-confirmed working** - both the A-13 fix itself and the performance
  regression fix (Backspace-hold stays fast throughout).

- **§386 (v1.1.25): D-431-followup - "welche" still not suggested for "welxmche" on a real device after**
  **D-431 shipped; temporary diagnostic logging added instead of a third blind patch.** The gate fix itself
  was re-verified directly (both by re-reading `hasObviousCandidate`'s own logic and by re-confirming the
  existing `LearnedBigramBoostTest`-style unit test for this exact repro still passes) - `"welsche"`/`"Welsche"`
  are real, separate dictionary rows only reachable via the D-117 wide-fuzzy fallback (cost 3, `x`/`s` being
  keyboard-adjacent), correctly excluded from `hasObviousCandidate`'s own narrower search, so the gate itself
  should not be blocking A-13 here. Rather than guess a third mechanism (composingTaps desync, a stale
  reclaim, the raw tap simply not landing close enough to Backspace on this occasion - all plausible, none
  confirmable from code alone), this project's own established pattern for exactly this class of bug (spec
  §1's guiding principle; D-355/D-373-followup's own precedent) applies: a new `AdaptKeyA13` diagnostic tag
  now logs, via the existing `diag()` helper (`Log.d` + the in-app X-01 diagnostic log, Settings ->
  Diagnostics), the gate decision itself (`hasObviousCandidate` result and the candidates already found) and,
  inside `missedBackspaceCorrection()`, the backspace geometry, `composingTaps.size` vs. the typed token's
  own length, `MissedBackspaceRecovery.recover()`'s own raw candidate list before the dictionary filter, and
  the final known-word result. Reachable via `adb logcat -s AdaptKeyA13:D` or the in-app Diagnostics log.
  Genuinely no working theory left worth coding blind against - waiting on a real repro's log output before
  touching either mechanism again. No new tests (diagnostic logging only, no behaviour change). 1240 unit
  tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change (no
  behaviour changed). `versionCode` 441 -> 442, `versionName` `"1.1.24"` -> `"1.1.25"`.

- **§385 (v1.1.24): D-433-followup (v5) - the last remaining gap: "Mehr erfahren" still sat flush left,**
  **not indented with the title/summary column above it.** Reported directly, once §384's own title/summary
  fix was confirmed correct. Root-caused, not re-guessed: `preference_material.xml`'s own icon column
  (`<include layout="@layout/image_frame"/>`) is a `LinearLayout` with `android:minWidth="56dp"` - reserved
  at that fixed width *regardless* of whether an icon is actually shown, confirmed by reading `image_frame.xml`
  directly. The button row below the include has no icon_frame of its own, so its own
  `listPreferredItemPaddingStart` padding alone landed it under where an icon would sit, not under the
  title/summary text one column further right. Fixed with a leading `56dp` `Space` before the two-button
  `LinearLayout`, matching that exact reserved width. No new tests (pure layout fix). 1240 unit tests
  unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode` 440 -> 441,
  `versionName` `"1.1.23"` -> `"1.1.24"`. **2026-09-03: device-confirmed.**

- **§384 (v1.1.23): D-433-followup (v4) - v3's own `<include>` pointed at the wrong layout resource.**
  Screenshot confirmed the exact symptom precisely: "Mini-LLM (Tier 3)" rendered larger but not bold, and
  visibly further left than every sibling title/summary in the same list. Root-caused properly this time by
  reading the actual style chain inside the `androidx.preference:1.2.1` AAR rather than assuming
  `@layout/preference` (the plain, pre-Material default `v3` included) was what this screen actually uses:
  `values/themes.xml` sets `preferenceTheme="@style/PreferenceThemeOverlay"`, whose own `BasePreferenceThemeOverlay`
  parent maps `preferenceStyle` to `@style/Preference.Material`, whose own `android:layout` is
  `@layout/preference_material` - a genuinely different file from `@layout/preference` (confirmed by reading
  both directly): theme-derived `?android:attr/listPreferredItemPadding*` instead of a fixed margin, and
  `?android:attr/textAppearanceListItem` for the title instead of `textAppearanceLarge` - exactly the two
  attributes behind both symptoms in the screenshot. Fixed by including `@layout/preference_material`
  instead - the literal layout every sibling plain `Preference` in this screen already renders through - and
  by switching the two-button row's own horizontal padding from a hard-coded `16dp` guess to the identical
  `listPreferredItemPaddingStart`/`End` theme attributes the included layout's own title/summary column uses,
  so the buttons line up under that text by construction rather than by an approximated constant. No new
  tests (pure layout-resource fix). 1240 unit tests unchanged, all green. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. `versionCode` 439 -> 440, `versionName` `"1.1.22"` -> `"1.1.23"`. **2026-09-03:
  device-confirmed.**

- **§383 (v1.1.22): D-433-followup (v3) - §382's own fix made it worse (fully flush left, still not bold) -**
  **the real root cause was more fundamental than wrong margin/textAppearance values.** Reported directly:
  after §382 shipped, the row went from "a few pixels off" to completely unindented, and the title, while now
  a larger font, still was not bold. Root-caused properly this time, not patched again by further attribute
  tweaking: `preference_tier3_model.xml` re-declared `android:id="@+id/icon_frame"` - inside this app's own
  layout resource, that mints a **new** id under this app's own `R` class, a different numeric value than
  `androidx.preference.R.id.icon_frame`, the id `Preference.onBindViewHolder()` is actually compiled to look
  up internally to manage that view's own visibility (same name, unrelated id - resource ids are scoped per
  compiled `R` class, not aliased by name across a library boundary). The library's own icon-frame handling
  therefore silently never reached this layout's view at all in either v1 or v2; §382's own change (dropping
  the `minWidth` safety net that happened to reserve *some* space in v1) simply removed the one thing that had
  been accidentally producing an approximately-plausible result, exposing the real gap.

  Fixed properly by not hand-copying AndroidX's internal structure at all: `preference_tier3_model.xml` now
  `<include layout="@layout/preference" />`s the library's own real default layout resource verbatim -
  the exact same physical XML `androidx.preference.Preference` already renders for every sibling row in this
  screen, ids included, so title/icon/summary rendering is now byte-identical to every other preference by
  construction, not by re-derivation. Only the two-button row is this project's own content, appended below
  the include in the same wrapping `LinearLayout`. `Tier3ModelPreference.onBindViewHolder()` itself needed no
  change - it only ever looked up its own `tier3_pref_setup`/`tier3_pref_learn_more` ids, never the library's.
  No new tests (pure layout fix, Android view glue). 1240 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green (confirms `@layout/preference` resolves correctly
  against the `androidx.preference` dependency, not shadowed by any local resource of the same name).
  `versionCode` 438 -> 439, `versionName` `"1.1.21"` -> `"1.1.22"`. **2026-09-03: device-confirmed.**

- **§382 (v1.1.21): D-433-followup (v2) - three real visual bugs in §381's own hand-built preference layout,**
  **root-caused by diffing against AndroidX's real default layout, not guessed.** Reported directly: the row
  sat a few pixels left of every other setting, its own title was no longer bold, and the "Jetzt einrichten"
  button's text was nearly invisible (pale on light grey). Root cause confirmed by extracting the actual
  `preference.xml` straight out of the `androidx.preference:preference:1.2.1` AAR in the local Gradle cache
  (`~/.gradle/caches/modules-2/files-2.1/androidx.preference/...`) and diffing it line-for-line against
  `preference_tier3_model.xml` - which had been hand-copied from this project's own pre-existing
  `preference_labeled_seekbar.xml` template (D-407/D-408) rather than the true AndroidX default, and that
  template itself turns out to already carry small, previously-unnoticed deviations (masked there by the
  SeekBar's own visual weight, never masked here):

  - **The shift**: `preference_labeled_seekbar.xml`'s own title/summary `RelativeLayout` uses
    `marginStart=16dp`/`marginEnd=8dp`; the real default uses `marginStart=15dip`/`marginEnd=6dip`. Fixed to
    the exact real-default values.
  - **The missing bold**: the template's title `TextView` uses `textAppearance="?android:attr/
    textAppearanceMedium"` with no explicit colour; the real default uses `textAppearanceLarge` plus an
    explicit `textColor="?android:attr/textColorPrimary"`. Copied verbatim.
  - **The unreadable button**: the new setup button (`Kind.tier3_pref_setup`) inherited `?android:attr/
    borderlessButtonStyle`'s own default text colour with no override - resolves to a pale tone against this
    screen's light background. Fixed with an explicit `textColor="?android:attr/textColorPrimary"`, matching
    the same attribute the title itself now correctly uses (the "Mehr erfahren" button's own deliberate blue
    `link_text` override is untouched - that one was never the complaint).

  The icon frame was also switched from the template's bare `ImageView` (`android:minWidth="48dp"`) to the
  real default's exact `FrameLayout` + `ImageView` (`maxWidth`/`maxHeight` 48dp, no `minWidth`) structure -
  the one piece a plain diff alone could not fully explain (would need on-device measurement to confirm it
  was actually contributing to the shift), but replicating the identical structure removes it as a possible
  factor entirely rather than leaving an approximation in place. No new tests (pure layout-attribute fix,
  Android view glue). 1240 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `versionCode` 437 -> 438, `versionName` `"1.1.20"` -> `"1.1.21"`. **2026-09-03: device-confirmed.**

- **§381 (v1.1.20): D-433-followup - reworked the Mini-LLM settings row itself, plus a real, unrelated bug**
  **found and fixed in passing.** Direct feedback right after §380 shipped, several distinct points at once:

  1. **The settings-list summary was confirmed to fit without truncating, but judged too thin** - "etwas mehr
     könnte man an dieser Stelle schon dazu erzählen." `c06_model_pref_summary` widened moderately (one
     two-clause sentence covering both the contextual-completion and ambiguous-capitalisation benefits) - a
     deliberate middle length between the original one-liner and the full paragraph that prompted §380's own
     truncation report in the first place, since there is no device here to test the exact new wrap point
     against.
  2. **The row must show, at a glance, whether the model is actually installed.** Rather than a separate bold
     status line (the D-419 `ListPreference` pattern doesn't fit here - this isn't a list of named choices),
     folded directly into the new setup button's own label instead (see point 4).
  3. **A real, unrelated bug, reported in passing:** `Tier3ModelActivity` was missing the edge-to-edge inset
     fix every other settings sub-screen already has (`BlacklistActivity`'s own K-01-derived fix, §13,
     confirmed by grep - `LanguagePacksActivity`/`CredentialsActivity`/`CalibrationActivity`/
     `DiagnosticLogActivity`/`BackupActivity`/`LearnedWordsActivity` all have it; this one alone never did).
     Its content sat under the status bar/display cutout instead of being pushed down below it. Fixed with
     the identical `ViewCompat.setOnApplyWindowInsetsListener` block every sibling screen already uses,
     against a newly-named `@id/tier3_root` (the screen's own root `ScrollView`, padding moved there from the
     inner `LinearLayout` to match the established convention exactly).
  4. **The settings row itself redesigned**: the on-screen "why" paragraph that used to live at the top of
     `Tier3ModelActivity` (`c06_model_benefit`, added only in §380) is gone entirely - "der hier angezeigte
     Text [ist] unnötig... man kann ihn sicher so kompakt halten, dass er einfach in die Settings View
     passt." The settings-list row itself now hosts two independent buttons directly beneath its (widened)
     summary: "Mehr erfahren" (opens the same detail dialog immediately, no navigation at all) and a
     status-aware button reading "Bereit" when a model is installed or "Jetzt einrichten" otherwise (tapping
     either state still opens `Tier3ModelActivity`, to manage/remove or to actually install). A plain
     `Preference` cannot express two independently-clickable regions with different actions - built as a new
     `Tier3ModelPreference` (custom `layoutResource`, `isSelectable = false` so only the two buttons react,
     mirroring `LabeledSeekBarPreference`'s own D-407 precedent for a custom-layout preference in this same
     screen) with a new `preference_tier3_model.xml` layout (mirrors `preference_labeled_seekbar.xml`'s own
     icon/title/summary structure, D-407/D-408, with a two-button row in place of the SeekBar).
     `settings_preferences.xml`'s `c06_model` entry now declares the custom class directly, dropping the old
     `<intent>` child (navigation is now the setup button's own explicit `startActivity` call, not the whole
     row's). `Tier3ModelActivity.showDetailsDialog()` promoted to a companion function taking a `Context`, so
     the preference's own button can show it without ever creating the activity. The setup button's own label
     is re-derived on every bind, refreshed via a new public `Tier3ModelPreference.refresh()` (`notifyChanged()`
     itself is protected) called from `SettingsFragment.onResume()` - identical reasoning to the pre-existing
     K-01 calibration-summary refresh right next to it: the install state only ever changes via the separate
     `Tier3ModelActivity`, so returning from it must re-bind this row to pick up a fresh
     `Tier3ModelStorage.isModelInstalled()` read.

  Localised into all three languages (de/en/el) - `c06_model_pref_summary` revised, two new button-label
  strings (`c06_model_pref_ready`/`c06_model_pref_setup_now`), `c06_model_benefit` removed (confirmed no
  remaining reference anywhere). No new tests (pure Android view/`Preference`/dialog glue, all untested per
  this project's own convention, same as every other settings-screen change). 1240 unit tests unchanged, all
  green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change - same D-89 precedent as §380.
  `versionCode` 436 -> 437, `versionName` `"1.1.19"` -> `"1.1.20"`. **2026-09-03: device-confirmed.**

- **§380 (v1.1.19): D-433 - the Mini-LLM (Tier 3) settings screen explains its actual value instead of just**
  **its install mechanics.** User's own explicit request, after a full re-read of the tier-3 orchestration
  code (`Tier3Orchestrator`/`Tier1Confidence`/`SuggestionMerger`/`HighCertaintyCapitalisation`/
  `AdaptiveLearning`/`Tier3FamilyPrompt`/`Tier3FamilyApplier`) to distil what genuinely differs with vs.
  without it: contextual completion for word combinations tier-1's own bigram/trigram statistics have never
  seen (only ever consulted when `Tier1Confidence` is low - the LLM stays dormant for confident predictions);
  resolving genuinely ambiguous capitalisation (§6 rule 6, e.g. "Weg"/"weg") from real sentence meaning, at
  ≥85% confidence only; determining a newly-learned word's whole family (lemma, part of speech, principal
  inflected forms) in one step, versus the non-LLM path's much narrower fixed-ending-list heuristic (D-404,
  `LearnedLemmaLinking`); and feeding a confirmed LLM suggestion straight back into tier 1 so the LLM is
  needed less over time. Explicitly unaffected either way: ordinary spelling correction, umlaut restoration,
  A-05/A-06 split/merge, and A-13 missed-Backspace recovery - all independent of tier 3 entirely.

  First attempt (a 2-3 sentence on-screen paragraph) was reported back as still too long once tested against
  the real UI - "das hatten wir an anderer Stelle schon einmal" (echoing §365's own spinner-label width
  lesson). Root cause of the confusion, clarified through the report: the actually space-constrained spot is
  the **settings-list row's own summary** (`c06_model_pref_summary`, shown inline in the main Settings list
  before the user even taps in) - not the dedicated `Tier3ModelActivity` screen's own intro text, which sits
  in an unconstrained `ScrollView` and was never actually at risk of truncation. Resolved by keeping the
  content at three deliberately different lengths for three different spots:

  1. **`c06_model_pref_summary`** (the settings-list row, genuinely space-constrained): a single tight
     sentence ("Verbessert unsichere Vorschläge durch Satzverständnis statt nur Statistik") replacing the
     old purely-mechanical "download and import" text.
  2. **New `c06_model_benefit`** (top of `Tier3ModelActivity`'s own screen, `ScrollView`, no length
     constraint): a short, 2-3 sentence why-use-this paragraph, ahead of the existing install-mechanics text.
  3. **New `c06_model_details_title`/`c06_model_details`** (a full deep-dive, six short paragraphs covering
     tier-1's own limits, contextual completion, capitalisation resolution, family-learning, adaptive
     learning, and what stays unaffected) - reached via a new "Mehr erfahren" link
     (`R.id.tier3_learn_more`, reusing the existing `@string/d89_learn_more` label and `link_text` colour,
     D-192's own established pattern) that opens a plain `AlertDialog` rather than navigating to another
     screen, since this is one self-contained block of text, not `FeatureCatalog`'s own repeating list.

  Localised into all three languages (de/en/el) - the German original checked directly against this
  session's own from-the-code research, not paraphrased from memory; English and Greek translated in step.
  No new tests (pure string-resource content plus Android view/dialog glue, both untested per this project's
  own convention). 1240 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. No spec change - purely informational settings-screen content, the same precedent D-89's own much
  larger Feature Overview screen already set (never mentioned in `AdaptKey-Spec.md` either). `versionCode`
  435 -> 436, `versionName` `"1.1.18"` -> `"1.1.19"`. **2026-09-03: device-confirmed.**

- **§379 (v1.1.18): D-432 - applied D-431's same `hasObviousCandidate()` gate to `rawCoordinateSuggestion`**
  **(T-02/D-39's own live preview chip), for consistency.** Flagged as a noticed-in-passing parallel while
  fixing D-431 (identical `candidates.isNotEmpty()` gate shape), confirmed with the user before touching it.
  One real difference found while investigating, worth recording: unlike A-13 (chip-only, no other call
  site), `rawCoordinateCorrection()` also has a second, independent call site - `finalizeAndCommit()`'s own
  silent-autocorrect application at commit time ([AdaptKeyService.kt:4008](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt:4008)) - which was **never** affected by this
  bug, since it gates on `autocorrected == null` (the tight, cost-≤2 `bestCorrection()` search) rather than on
  `candidates`/the wide-fuzzy fallback at all. So this fix only restores the *live, mid-word preview chip*'s
  visibility in the rare case a coincidental last-resort match also exists - the actual commit-time correction
  itself was always correct. No new tests (same untested `AdaptKeyService.kt` Android/`InputConnection` glue
  as the call site itself, and `hasObviousCandidate()` is already fully covered from D-431). 1240 unit tests
  unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: T-02 gained the D-432
  note. `versionCode` 434 -> 435, `versionName` `"1.1.17"` -> `"1.1.18"`. **2026-09-03: device-confirmed.**

- **§378 (v1.1.17): D-431 - A-13's own "everything else must have failed first" gate was wrongly satisfied**
  **by a coincidental last-resort dictionary match, silently blocking the exact case A-13 exists for.**
  Reported directly against the feature's own worked example: typing `"welxmche"` (intending `"welche"`)
  suggested `"welsche"` instead - never even reached `missedBackspaceCorrection()`. Root-caused, not guessed:
  `"welsche"` (a genuine but rare German word) sits at edit cost 3 from `"welxmche"` (delete `"m"`, substitute
  the keyboard-adjacent `x`->`s`), cheaper than the actually-intended two-deletion reconstruction `"welche"`
  (cost 4) - so A-09's own wide-fuzzy fallback (D-117, cost ≤ 4, explicitly documented as "never trusted
  enough for autocorrect") surfaced it, making `candidates.isNotEmpty()` true and blocking A-13's own gate
  ([AdaptKeyService.kt](app/src/main/kotlin/de/froehlichmedia/adaptkey/AdaptKeyService.kt)) before it ever ran.

  User's own reframing of the original D-377 condition, confirmed as the intended design: "alle anderen
  Versuche müssen vorher gescheitert sein" never meant the whole suggestion bar had to end up empty -
  something can almost always be suggested - only that nothing *obvious* (prefix/umlaut-unfold/neighbour-
  substituted-prefix completion, a close cost-≤2 fuzzy match, or a recognised compound) turned up first.
  Whether the wide-fuzzy last-resort fallback separately, coincidentally, also finds something is irrelevant -
  both are simply offered together, A-13's own chip pinned ahead.

  Implemented by extracting `DictionarySuggestionProvider.suggestionsFor()`'s own candidate search (everything
  except the D-117 wide-fuzzy fallback) into a shared private `obviousCandidates()`, reused by both
  `suggestionsFor()` itself and a new `hasObviousCandidate()` (promoted onto the `SuggestionProvider` interface
  with a `suggestionsFor(...).isNotEmpty()` default for a provider with no tiered fallback to distinguish) -
  the two can never silently drift apart on what counts as "obvious". `AdaptKeyService`'s own
  `missedBackspaceSuggestion` gate now calls `provider.hasObviousCandidate(...)` instead of reading
  `candidates.isNotEmpty()` directly.

  5 new tests (`DictionarySuggestionProviderTest`: `hasObviousCandidate` true/false on ordinary matches, false
  when only the D-117 wide-fuzzy fallback finds something while `suggestionsFor` itself still does, and the
  exact `"welxmche"`/`"welsche"`/`"welche"` repro reproduced and confirmed fixed end to end;
  `StubSuggestionProviderTest`: the default delegates to `suggestionsFor(...).isNotEmpty()`). 1235 -> 1240 unit
  tests, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: A-13 rewritten to describe the
  current gate. `versionCode` 433 -> 434, `versionName` `"1.1.16"` -> `"1.1.17"`.
  **2026-09-03: device-confirmed working** end to end (real `AdaptKeyA13` diagnostic log: `hasObviousCandidate=
  false`, `recover() candidates=[welche]`) once §386/§387 below closed out the one remaining real-device gap
  (a reclaimed token has no tap evidence, not a bug) and the diagnostic-logging round's own self-inflicted
  performance regression (§387).

- **§377 (v1.1.16): D-430 - the Learned Words editor's per-entry dialog swaps its Save/Forget button roles.**
  User's own explicit call, after confirming the rest of §363/§365's own dialog rework is now "perfekt": Save
  moves to the dialog's positive button, Forget to the neutral one - Cancel (negative) untouched. Pure button-
  role swap in `LearnedWordsActivity.showEntryDialog()`'s `AlertDialog.Builder` chain (`setPositiveButton`/
  `setNeutralButton` bodies exchanged verbatim, no behaviour change to either action itself);
  `updateSaveEnabled()`'s own `dialog.getButton(...)` call updated from `BUTTON_NEUTRAL` to `BUTTON_POSITIVE`
  to keep gating the now-repositioned Save button. No new tests (Android dialog/view glue, per this project's
  own convention - no existing test referenced either button's role). 1235 unit tests unchanged, all green.
  `:app:assembleRelease`/`:app:testDebugUnitTest` green. No spec change - implementation-only polish, same
  "occasionally skippable" precedent §363's own items 9/10 already established. `versionCode` 432 -> 433,
  `versionName` `"1.1.15"` -> `"1.1.16"`. **2026-09-03: device-confirmed.**

- **§376 (v1.1.15): D-429 - learned bigrams and trigrams now carry their own `last_touched` timestamp, and**
  **the recency boost D-411 already gives individual learned words now applies to them too.** Explicit
  follow-up request: this was flagged but deliberately left out of both D-365 (§340, v1.0.92, the bigram
  ranking rescale) and W-05 (§344-§349, learned-word expiry) - "das hatten wir beim letzten Mal ausgelassen" -
  worth closing before wider release even though the practical effect is small. Design discussed and agreed
  first (this project's own rule for a ranking/algorithm change): reuse D-388/D-411's own established
  guarded-`ALTER TABLE` migration pattern and D-411's own 14-day/×1.5 recency constants unchanged, rather than
  inventing a second mechanism or recalibrating - "used within the last 14 days" is a general
  personal-relevance signal, not specific to which n-gram order it came from.

  **Schema/write path.** `TABLE_LEARNED_BIGRAMS`/`TABLE_LEARNED_TRIGRAMS` each gained a guarded
  `last_touched INTEGER NOT NULL DEFAULT 0` column (`ensureBigramLastTouchedColumn`/
  `ensureTrigramLastTouchedColumn`, called from `init {}` alongside the three existing D-388/D-412/D-404
  migrations) - existing rows seeded with strictly increasing timestamps in key order, exactly like
  `ensureLastTouchedColumn` already does for `TABLE_LEARNED`. Every write to either table now stamps
  `System.currentTimeMillis()`: `learn()`'s and `learnContext()`'s own reinforcement writes, `unlearn()`'s
  decrement writes (a decrement still counts as a "touch", mirroring `TABLE_LEARNED`'s own identical
  behaviour), and the backup-import `restoreLearnedBigram`/`restoreLearnedTrigram` merges. The bundled
  `TABLE_BIGRAMS` table is untouched - `putBigramInternal`'s new `lastTouched` parameter is only ever passed
  by a learned-table call site, `null` (omitted) everywhere else. `InMemoryDictionaryStore` mirrors this with
  two new `learnedBigramTouch`/`learnedTrigramTouch` maps, stamped identically via its own `clock` parameter.

  **Read path.** New shared `LearnedNgram(count, lastTouched)` data class (mirrors `LearnedFrequency`'s
  identical D-411 shape) plus two new `DictionaryStore` methods, `learnedBigramWithTimestamp`/
  `trigramWithTimestamp`, implemented by both stores - `learnedBigramFrequency`/`trigramFrequency` themselves
  are untouched and still feed A-06's own merge gate directly, exactly as `LearnedBigramBoost`'s own KDoc
  already promises for the plain count.

  **Ranking.** `LearnedBigramBoost.boost()` gained `lastTouched`/`now` parameters and the identical
  ×1.5-within-14-days multiplier `LearnedFrequencyBoost` already has. `DictionarySuggestionProvider.
  rankingBigramFrequency()` and `score()`'s trigram branch now thread the timestamp through.

  **Explicit follow-up decision, also agreed with the user first**: `nextWordSuggestions()` (S-07's own
  blank-slate prediction) was the one remaining place still scoring a trigram match by its literal raw count
  directly, never through `LearnedBigramBoost` at all - a pre-existing inconsistency with `score()`'s already-
  boosted trigram branch, unrelated to recency by itself but surfaced by this same round. Unified onto the
  same boosted, now recency-aware value rather than left as a second, inconsistent scoring path.

  19 new/updated tests: `LearnedBigramBoostTest` rewritten for the new signature plus 3 new recency cases (8
  total, mirroring `LearnedFrequencyBoostTest`'s own structure); 5 new `InMemoryDictionaryStoreTest` cases
  (`learnedBigramWithTimestamp`/`trigramWithTimestamp` default-null and reported-value cases, plus one
  covering `unlearn`'s own timestamp-then-removal behaviour); 4 new `SqliteDictionaryStoreRoboTest` cases
  (the same shape, against the real SQLite-backed store); 3 new `DictionarySuggestionProviderTest` integration
  cases (a recently-touched learned bigram outranking a moderately common bundled one, the identical case
  losing that edge once long untouched, and `nextWordSuggestions` no longer using a raw trigram count
  directly); 1 existing `DictionarySuggestionProviderTest` case (`nextWordSuggestions ranks a trigram match by
  its own raw count...`) rewritten to assert the new rescaled/boosted value instead, computed via
  `LearnedBigramBoost.boost()` itself rather than a hand-typed literal, so the test tracks the production
  formula rather than duplicating it. 1220 -> 1235 unit tests, all green. `:app:assembleRelease`/
  `:app:testDebugUnitTest` green. Spec: S-07 gained the D-429 addendum (and its own opening sentence updated -
  "raw trigram count" was no longer accurate); W-05's own bigram/trigram note corrected (they now have
  `last_touched`, but the *expiry sweep* itself is still not extended to them - a still-separate, not-yet-built
  item). `versionCode` 431 -> 432, `versionName` `"1.1.14"` -> `"1.1.15"`. **2026-09-03: device-confirmed.**

- **§375 (v1.1.14): D-428 - the V-04 clipboard-peek button flashed back visible for one render right after**
  **its own V-03 clear button emptied the clipboard.** Reported directly: tap the peek button, tap the clear
  button that appears alongside its chips - clipboard clears, chips and clear button vanish as expected, but
  the peek button itself briefly reappears before disappearing again on the next tap/keystroke. Root-caused
  by reading the actual code, not guessed: `clearClipboardFromSuggestionBar()` calls `clearClipboard()`
  (synchronously empties the system clipboard) then `clearSuggestions()` (synchronously calls
  `setSuggestionBarItems(emptyList())`, which derives the peek button's visibility from the cached
  `clipboardPeekAvailable` field) - but that field is otherwise only refreshed by the *asynchronous*
  `ClipboardManager.OnPrimaryClipChangedListener` notification, which had not yet fired by the time
  `setSuggestionBarItems()` read it, so it briefly rendered the stale, still-`true` value. The next real
  `setSuggestionBarItems()` call (typing, a caret move) always saw the by-then-correct `false` value, matching
  exactly the "briefly there, then properly hidden" symptom reported.

  Fixed by calling `updateClipboardPeekAvailability()` explicitly, synchronously, between `clearClipboard()`
  and `clearSuggestions()` - `clearClipboard()`'s own Binder call has already emptied the clipboard by the
  time it returns (only the *listener notification* about the change is asynchronous), so this recomputes the
  field correctly in time rather than waiting for that notification. `updateClipboardPeekAvailability()`'s own
  KDoc call-site list updated to name this new trigger point. No new tests (the fix lives entirely in
  `AdaptKeyService.kt`'s own untested Android/`InputConnection` glue, per this project's own convention). 1220
  unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. Spec: V-04 gained the
  D-428 addendum. `versionCode` 430 -> 431, `versionName` `"1.1.13"` -> `"1.1.14"`.
  **2026-09-03: device-confirmed working.**

## Older Rounds (§1-§374, v0.7.6 through v1.1.6) - Pruned From This File

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
same way, without summarising them - they stay permanently retrievable in History.md. **2026-09-04 fourth
pruning pass (§367-§374 removed, cutoff moved from §367 to §375):** History.md was already backfilled through
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
