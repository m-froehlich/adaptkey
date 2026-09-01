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
  - **D-370 - OPEN, real design work needed, not a quick patch.** Double-quote handling: an auto-space after
    a closing double-quote should be removable; more ambitiously, every space directly before a closing
    quote should be removable, but only when a genuine preceding opening quote is nearby - the user
    explicitly flags the trap to avoid (wrongly deleting the space *before* the quoted word instead of
    *after* it). Open question posed directly: how far back to search for the matching opening quote?
  - **D-371 - RESOLVED (§351, v1.0.103).** A word ending in a digit is now only ever silently autocorrected
    at C-22's own Aggressive level - no dedicated setting, reuses the existing autocorrect-aggressiveness
    slider (see Current State for the mechanism).
  - **D-372 - OPEN.** A diagonally-adjacent key (e.g. `g`/`b`) should also count as a keyboard neighbour for
    correction purposes.
  - **D-373 - OPEN.** Appending a hyphen after a *capitalised* word should re-arm auto-capitalisation for the
    next segment (mirrors B-02's own default for the general case).
  - **D-374 - RESOLVED by D-416 (§333).** The trailing auto-space is never physically written until a real
    next character resolves it, so there is structurally nothing left to strand or fail to clean up when a
    field is left (Google Keep or otherwise) - eliminated, not patched.
  - **D-375 - RESOLVED (2026-08-31, no dedicated fix identified).** User confirmed on real-device testing
    that `"sollendafur"` no longer gets silently auto-unfolded/split the way originally reported - no code
    change in this session targeted it specifically; likely a side effect of the intervening dictionary
    cleanup rounds (§301 and later) or a later A-05/umlaut-interaction refinement, not root-caused further
    since there is nothing left to fix.
  - **D-376 - OPEN.** After `"km"` (and, separately, after a `/`), a `"km/h"` completion chip should be
    offered.
  - **D-377 - OPEN, explicitly named as expensive/last-resort.** Recover a badly garbled mid-word mistake
    where a Backspace was missed and a neighbouring key hit instead (e.g. `"welxmche"`) - the user's own
    worked reasoning is in history §276; proposes at least a chip, possibly a silent autocorrect, only when
    nothing else in the pipeline resolves the token at all.
  - **D-378 - OPEN.** While capitalisation is active (auto-armed or explicit), a quote character must not
    reset it.
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
  - **D-384 - OPEN.** Typing a minus preceded by a space should also get its own trailing A-12-style
    auto-space, but only when a space already precedes the minus.
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
  - **D-390 - OPEN.** Sentence-start auto-capitalisation must tolerate multi-part abbreviations (`"p. a."`/
    `"i. d. R."`) - a wrongly-applied capital must be retroactively corrected back once the abbreviation
    completes. Explicitly asked to be designed as a *general* rule, not a special case for one example.
  - **D-391 - OPEN.** A-05's retroactive split extended to the reverse direction (mirrors the same auto/
    chip-only setting D-352 got): if the current or preceding word makes no sense alone, but inserting a
    bottom-row connector letter (`y x c v b n m`) between them produces a sensible combined word, recognise
    it - a generalisation of A-06 merge beyond its current scope.
  - **D-392 - OPEN.** Auto-capitalisation should re-engage after Caps Lock is turned off.
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
     was deleted once superseded.**
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
     **implemented (§324) - D-404 is fully closed except for tier 2, still open.**

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

- **§305 (v1.0.58): D-412 - a bundled-only `lemma` link column, laid down as groundwork for the**
  **in-progress German verb-tagging project (see below) and, further out, D-404 Tier 1.** `WordEntry` gained
  `lemma: String? = null`; `dict.tsv`'s word-line format gained a matching optional 4th column, parsed
  exactly like the POS column (absent/empty = no link, fully backward-compatible with every existing row in
  every language's asset). `SqliteDictionaryStore` schema: `TABLE_WORDS` alone gained the column (`lemma
  TEXT`, `CREATE TABLE` for fresh installs plus a new guarded `ensureLemmaColumn()` - same `PRAGMA
  table_info`-checked `ALTER TABLE ADD COLUMN` pattern as D-388's `ensureLastTouchedColumn()`, called
  unconditionally from `init {}`) - deliberately **not** `TABLE_LEARNED`, and deliberately not via a
  `DATABASE_VERSION`/`onUpgrade` bump, since `onUpgrade` drops every table including `TABLE_LEARNED` (the
  user's real learned words/blacklist) and this project has apparently never actually exercised that path for
  exactly that reason. Every `TABLE_WORDS` read/write path (`putWord`, `bulkImport`, `entryOfIn`,
  `queryByPrefix`) threads the column through; both of `entryOf`'s and `unigramsByPrefix`'s merge-with-learned
  branches were fixed to keep the bundled entry's own `lemma` rather than silently losing it to the learned
  entry's always-null one (mirrored in `InMemoryDictionaryStore` too). Value stored as a plain word string,
  not a `rowid`/integer FK - discussed directly with the user (who asked about using the table's implicit
  SQLite `rowid` for a cheaper lookup): `TABLE_WORDS.wkey` is already the indexed primary key so a text
  lookup is no slower, every other cross-reference in this schema already keys by lower-cased text
  (`TABLE_BIGRAMS`, `TABLE_LEARNED_TRIGRAMS`, `TABLE_BLACKLIST`), and a rowid has no stability guarantee
  across `TABLE_WORDS`'s routine `resetBundledWords()`+`bulkImport()` wipe-and-reseed cycle. 6 new tests
  (`DictionaryAssetParserTest` lemma-column parsing, `InMemoryDictionaryStoreTest` put/`entryOf`/
  `unigramsByPrefix` round-trip and merge-survival). 1064 unit tests total (was 1058), all green (via JDK 21).
  `versionCode` 361 -> 362, `versionName` `"1.0.57"` -> `"1.0.58"`. Spec gained new §38. **Zero readers of
  `lemma` exist yet** - same groundwork-only status D-368's own `VERB` tag had. Not yet device-confirmed (no
  device-observable change expected). See history (this session).

- **§306 (v1.0.59): German verb-in-`OTHER` tagging, round 1 - the ≥2000-frequency band (78 rows) - project**
  **overview and remaining scope below.** New effort: unlike D-368 (which only tagged a `NOUN`-`VERB`
  collision for capitalisation purposes), this tags *every* genuine verb form currently mis-tagged plain
  `OTHER`, including already-present inflected forms, not just infinitives - explicit groundwork for D-404
  Tier 1, using D-412's new `lemma` column to record each inflected form's base-form link as a byproduct of
  finding it (not yet populated this round - that starts once the base-form/inflected-form linking pass
  itself begins; round 1 only applied the POS tag). Candidate pool: lowercase, `OTHER`-only, ends in
  `-en`/`-eln`/`-ern`/`-n` (10,925 total, banded by frequency - `-eln`/`-ern` needed adding after the user
  flagged the plain `-en` heuristic misses them, e.g. `sammeln`/`wandern`; `sein` needed its own hand-add
  since it's the one infinitive that fits none of those suffixes). This round: the ≥2000 band (181
  candidates), reviewed individually. 73 unambiguous verbs `OTHER` -> `VERB` (infinitives:
  `werden`/`können`/`haben`/`finden`/`lassen`/`gehören`/`erhalten`/`kommen`/`bilden`/`liegen`/`stehen`/
  `müssen`/`führen`/`stellen`/`befinden`/`bestehen`/`machen`/`sollen`/`sehen`/`gelten`/`enthalten`/`spielen`/
  `vertreten`/`erreichen`/`zeigen`/`unterscheiden`/`zählen`/`entstehen`/`dienen`/`existieren`/`besitzen`/
  `geben`/`tragen`/`halten`/`nutzen`/`nehmen`/`bleiben`/`übertragen`/`gehen`; finite/participle forms already
  present as their own rows: `wurden`/`waren`/`kann`/`hatten`/`begann`/`konnten`/`entstanden`/`kamen`/
  `gewesen`/`verbunden`/`erschien`/`seien`/`sollten`/`fanden`/`aufgenommen`/`hochgeladen`/`betrieben`/
  `mussten`/`gefunden`/`gegeben`/`gewann`/`führten`/`gehörten`/`verloren`/`geworden`/`unterschieden`/
  `geboren`/`übernommen`/`gesprochen`/`geschrieben`/`würden`/`gesehen`/`angesehen`/`worden`). 5 genuine
  dual-meaning words `OTHER` -> `OTHER,VERB` (the non-verb reading has no better-fitting `PartOfSpeech` tag
  than `OTHER`, so it stays alongside `VERB` rather than being replaced by it): `sein` (verb *and* the
  extremely common possessive determiner - the one word the user flagged by name as needing individual
  treatment), `einigen` (sich einigen = to agree), `sieben` (to sieve, alongside the number "seven"),
  `gleichen` (to resemble, alongside the adjective "gleich" inflected), `bestimmten` (preterite of
  "bestimmen", alongside "bestimmt" inflected). 3 genuinely rare/archaic verb readings left untouched by
  explicit agreement (`sondern`/`freien`/`langen` - each dominated overwhelmingly by a non-verb reading in
  real usage, judged not worth the `OTHER,VERB` addition, matching this project's own precedent of not
  chasing every conceivable rare reading, e.g. the removed `"Bri"` noise entry). Präsens-Partizip-as-adjective
  forms (`folgenden`/`genannten`/...) explicitly out of scope by the user's own call - "generativ gut
  greifbar" (regenerable later under D-404 Tier 1, not worth tagging individually now). `git diff --stat`
  confirmed exactly 78 lines changed (verified via a fail-loud Python script, same pattern as D-368/D-301 -
  asserts every target word is found with exactly the expected prior tag before writing, and that the found
  count matches the target count exactly). `dictionaries/de/version.txt` 16 -> 17, pack rebuilt/verified
  (unzipped back, spot-checked `sein`/`werden`/`einigen` all carry `VERB`), `LanguagePackCatalog` version 16
  -> 17. No new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 362 ->
  363, `versionName` `"1.0.58"` -> `"1.0.59"`. Not yet device-confirmed.

  **Design discussion, resolved before this round started (no code change of its own):** the user asked
  whether, once base forms are tagged, a *prefix search* against the infinitive's stem could mechanically
  find already-present inflected forms, kept as short as possible to also catch irregular ones. Checked
  empirically against ten real strong verbs from this round's own candidate list (`gehen`/`kommen`/`nehmen`/
  `sehen`/`geben`/`sprechen`/`tragen`/`stehen`/`sein`/`werden`): the preterite typically shares only the
  infinitive's first 1-3 characters with the base form (ablaut), and the participle shares **zero** leading
  characters in 8 of 10 cases (the `ge-` prefix is added at the front, not appended - `gehen`/`gegangen`
  share no prefix at all; `gehen`/`geben`'s own `ge`-vs-`ge` overlap with their participles is coincidental,
  not structural). Confirmed no prefix length threads the needle: `g`/`ge`/`geh` as literal dictionary
  prefixes return 2496/1903/77 entries respectively, yet none of the three ever includes `ging` or
  `gegangen` - shortening trades recall for precision on both ends *simultaneously* rather than tuning
  between them. Conclusion: prefix search works only for the mechanical (Phase 2) weak-verb suffix-stripping
  path already planned (exact `stem+ending` reconstruction, no fuzziness needed there at all); strong-verb
  forms need a small hand-authored reference table of principal parts instead (closed class, ~150-200 verbs,
  pure grammar knowledge - not an external content source), matched by exact string equality against the
  candidate pool, never by prefix.

  **Remaining scope for this project** (not yet started): the remaining five frequency bands (500-1999: 455,
  200-499: 788, 50-199: 2290, 10-49: 6138, <10: 1073 candidates), the mechanical weak-verb-inflection pass
  described above (once base forms are confirmed, derive each one's own regular personal-ending forms and
  check which already exist as `OTHER` rows - no individual review needed, exact reconstruction), the
  strong-verb principal-parts table (not yet written), and populating the actual `lemma` link on every
  inflected form found via either path (this round only applied the `VERB`/`OTHER,VERB` tag, not yet the
  link - the two were kept as separate, independently-verifiable steps rather than combined into one script).

- **§307 (v1.0.60): round 2 - the 500-1999 band (227 rows), plus a tag-order fix applied retroactively to**
  **round 1.** User caught the tag order: `OTHER` must sort *last* against `VERB` (`VERB,OTHER`, not
  `OTHER,VERB`) - fixed going forward and retroactively re-applied to round 1's 5 dual words
  (`sein`/`einigen`/`sieben`/`gleichen`/`bestimmten`), via the same fail-loud script (asserts the exact prior
  tag before rewriting, so a silent double-application or a stale assumption about round 1's state would have
  aborted rather than corrupted anything). **Open question raised, not yet decided**: D-368's own 210
  pre-existing `NOUN,OTHER,VERB` entries follow the *old*, alphabetical convention (`OTHER` before `VERB`) -
  whether those should also be reordered to `NOUN,VERB,OTHER` for full consistency is a separate, larger
  retroactive change spanning several already-released versions, flagged for the user rather than assumed.
  This round's own 455-candidate band was reviewed individually exactly like round 1, with one added
  distinction worth keeping for every future band: a **bare** participle used predicatively (`geschlossen`,
  `verstanden`, `gehalten` - e.g. "die Tür ist geschlossen") is tagged plain `VERB`, but a **further-declined**
  participle-as-adjective (`entstandenen`, `geplanten`, `gewählten` - no corresponding preterite-plural
  collision) is left untouched, consistent with the already-agreed Präsens-Partizip exception
  (`folgenden`/`genannten`). Where a participle-plus-adjective-ending form *does* genuinely double as a real
  preterite-plural (`entwickelten` = "sie entwickelten" *and* "die entwickelten Länder" - the weak-verb
  `-ten` preterite ending and the dative/genitive-plural adjective `-en` ending on a `-t`-final participle
  coincide), it gets `VERB,OTHER` rather than being skipped. 209 words `OTHER` -> `VERB`; 18 genuine
  dual-meaning words `OTHER` -> `VERB,OTHER` (15 participle/preterite-plural collisions plus 3 borderline
  cases flagged for the user and confirmed: `linken`/`weiten`/`schnellen`, each real but a minority reading
  next to a dominant adjective one). `git diff --stat` confirmed exactly 232 lines changed (227 + 5 reorder).
  `dictionaries/de/version.txt` 17 -> 18, pack rebuilt/verified, `LanguagePackCatalog` version 17 -> 18. No
  new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 363 -> 364,
  `versionName` `"1.0.59"` -> `"1.0.60"`. Not yet device-confirmed. Remaining scope unchanged from §306's own
  note, minus the now-done 500-1999 band: four frequency bands left (200-499: 788, 50-199: 2290, 10-49: 6138,
  <10: 1073), the mechanical weak-verb-inflection pass, the strong-verb principal-parts table, and populating
  `lemma` on every result so far.

- **§308 (v1.0.61): round 3 - the 200-499 band (423 rows), plus the D-368 tag-order question from §307**
  **resolved: yes, reorder those too.** User confirmed retroactively fixing D-368's own 210 pre-existing
  entries as well - the 42 that actually needed it (`NOUN,OTHER,VERB` -> `NOUN,VERB,OTHER`; the other 168
  were plain `NOUN,VERB` with no `OTHER` to reorder, untouched) via the same fail-loud pattern, verifying
  the exact prior tag before rewriting. 788 candidates reviewed individually; 360 `OTHER` -> `VERB`, 63
  genuine dual-meaning words `OTHER` -> `VERB,OTHER` (the established participle/preterite-plural collision
  pattern from §307 accounts for most of them, e.g. `erhöhten`/`erklärten`/`kritisierten`; six are a
  different, independent-second-meaning shape rather than a spelling coincidence and were flagged for
  visibility rather than silently decided: `weichen` (to yield/soften, vs. adjective "weich" inflected),
  `achten` (to respect, vs. ordinal "achte" inflected), `meinen` (to mean/think, vs. possessive "mein"
  inflected), `nötigen` (to compel, vs. adjective "nötig" inflected), `dichten` (to write poetry/seal, vs.
  adjective "dicht" inflected), `tätigen` (to carry out, vs. adjective "tätig" inflected) - all six genuinely
  real, non-archaic verb readings, unlike the round-1 `sondern`/`freien`/`langen` skips). Noted but
  deliberately not touched (out of scope for this tagging pass, a separate D-301-style noise-cleanup
  question): `begin`/`align`/`sin`/`min`/`colon`/`varepsilon`/`return`/`schen` all surfaced in this band's
  own candidate pool and are clearly not German words at all (LaTeX/math-markup and programming-keyword
  leaks, the same corpus-extraction-artefact class §301 already cleaned up) - flagged for a future noise
  pass, not removed here. `git diff --stat` confirmed exactly 465 lines changed (423 + 42 reorder).
  `dictionaries/de/version.txt` 18 -> 19, pack rebuilt/verified (spot-checked `Drang` now reads
  `NOUN,VERB,OTHER`), `LanguagePackCatalog` version 18 -> 19. No new tests (data-only). 1064 unit tests
  unchanged, all green (via JDK 21). `versionCode` 364 -> 365, `versionName` `"1.0.60"` -> `"1.0.61"`. Not
  yet device-confirmed. Remaining scope: three frequency bands left (50-199: 2290, 10-49: 6138, <10: 1073),
  the mechanical weak-verb-inflection pass, the strong-verb principal-parts table, the flagged noise entries
  (separate decision), and populating `lemma` on every result so far.

- **§309 (v1.0.62): round 4 - the 50-199 band (2290 candidates, 964 rows changed) - largest single band**
  **done so far, reviewed individually exactly like the first three.** Before starting, flagged to the user
  that this band alone (2290) already exceeds bands 1-3 combined (~1424), with 6138 + 1073 still to come
  after it - the user explicitly confirmed continuing the same exhaustive per-word review for `≥50`
  frequency, to be reassessed once this band closed. Same established method throughout: bare predicative
  participles and genuine infinitives/finite forms tagged plain `VERB`; further-declined participle-as-
  adjective forms left untouched unless their spelling genuinely doubles as a weak-verb preterite-plural
  (`VERB,OTHER`); Präsens-Partizip-as-adjective and pure adjective/determiner/number/conjunction entries
  untouched. 774 words `OTHER` -> `VERB`; 190 genuine dual-meaning words `OTHER` -> `VERB,OTHER` (the
  established participle/preterite-plural collision pattern, plus a handful of independent-second-meaning
  cases in the same shape as round 3's six flagged ones, e.g. `kühlen`/`leeren`/`fertigen`/`billigen`/
  `mäßigen` - real but secondary verb readings next to a dominant adjective one - not separately re-flagged
  this round since the pattern itself is now established). `git diff --stat` confirmed exactly 964 lines
  changed, via the same fail-loud script pattern - caught and fixed two real authoring slips before writing
  anything (two words accidentally duplicated within `verb_only`, and four words - `ersetzten`/`stützten`/
  `versorgten`/`bearbeiteten` - accidentally placed in both the plain-`VERB` and dual lists; the script's own
  overlap/duplicate assertions caught both classes immediately, nothing was silently corrupted).
  `dictionaries/de/version.txt` 19 -> 20, pack rebuilt/verified, `LanguagePackCatalog` version 19 -> 20. No
  new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 365 -> 366,
  `versionName` `"1.0.61"` -> `"1.0.62"`. Not yet device-confirmed. Remaining scope: two frequency bands left
  (10-49: 6138, <10: 1073 - together over 7x this band's own size), the mechanical weak-verb-inflection pass,
  the strong-verb principal-parts table, the flagged noise entries (separate decision), and populating
  `lemma` on every result so far. **Given the scale of what's left, the next session should re-raise the
  pacing question from the top of this round before committing to the same exhaustive approach for 10-49 and
  <10.**

  **User explicitly confirmed (next message): both remaining bands, same exhaustive per-word approach,**
  **explicitly for dictionary quality ("Ich will ein wirklich hochqualitatives Wörterbuch haben")** -
  supersedes the "re-raise pacing" flag above. `10-49` (6138) is being worked in three frequency sub-bands
  for manageability (`30-49`: 1454, `20-29`: 1459, `10-19`: 3225), each its own verify/build/commit cycle,
  same fail-loud script pattern as every round so far - not a change in rigor, purely a chunking choice for
  a band this large.

- **§310 (v1.0.63): round 5a - the `30-49` sub-band of `10-49` (1454 candidates, 628 rows changed).** Same
  method as every round so far (bare predicative participle/infinitive/finite form -> `VERB`; further-declined
  participle-as-adjective untouched unless it doubles as a real preterite-plural -> `VERB,OTHER`; adjective/
  determiner/number/conjunction/present-participle-as-adjective untouched). 528 words `OTHER` -> `VERB`; 100
  genuine dual-meaning words `OTHER` -> `VERB,OTHER` (established collision pattern, e.g. `weiterentwickelten`,
  plus a couple of independent-second-meaning cases like `lichten` - to melt/lift anchor vs. adjective
  "licht" - not separately re-flagged, per the precedent set in §309). The fail-loud verification script
  caught several real authoring slips before anything was written this round - two misspelled words
  (`unzubenennen`/`heroisch`, neither a real dictionary entry), one word (`thematisieren`) already tagged by
  an earlier round and mistakenly re-listed, and four words placed in both the plain-`VERB` and dual lists
  (`bombardierten`/`weiterentwickeln`/`überliefern`/`proklamierten`) - all caught by the script's own
  assertions and fixed before the actual write, nothing silently corrupted. `git diff --stat` confirmed
  exactly 628 lines changed. `dictionaries/de/version.txt` 20 -> 21, pack rebuilt/verified,
  `LanguagePackCatalog` version 20 -> 21. No new tests (data-only). 1064 unit tests unchanged, all green (via
  JDK 21). `versionCode` 366 -> 367, `versionName` `"1.0.62"` -> `"1.0.63"`. Not yet device-confirmed.
  Remaining: `20-29` (1459) and `10-19` (3225) sub-bands of `10-49`, then the `<10` band (1073), the
  mechanical weak-verb-inflection pass, the strong-verb principal-parts table, the flagged noise entries, and
  populating `lemma` on every result so far.

- **§311 (v1.0.64): round 5b - the `20-29` sub-band of `10-49` (1459 candidates, 613 rows changed).** Same
  method as every round so far. 532 words `OTHER` -> `VERB`; 81 genuine dual-meaning words `OTHER` ->
  `VERB,OTHER`. The fail-loud script again caught real authoring slips before writing anything - most
  notably a batch of ~40 words where the dual (`VERB,OTHER`) form had, correctly, been placed in the dual
  list, but a related sibling form had been mistakenly duplicated into the plain-`VERB` list as well; fixed
  by deriving `verb_only` as "everything not already claimed by `verb_other`" rather than hand-editing each
  one, then re-verified the resulting counts against the manual review before writing. Also caught: one word
  (`bekehren`) already tagged by an earlier round and mistakenly re-listed, and one word (`dezimieren`) that
  turned out not to exist as its own dictionary row at all (only its preterite-plural sibling
  `dezimierten` does) - both dropped rather than forced. `git diff --stat` confirmed exactly 613 lines
  changed. `dictionaries/de/version.txt` 21 -> 22, pack rebuilt/verified, `LanguagePackCatalog` version 21
  -> 22. No new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 367 ->
  368, `versionName` `"1.0.63"` -> `"1.0.64"`. Not yet device-confirmed. Remaining: the `10-19` sub-band of
  `10-49` (3225 - by far the largest remaining chunk), then the `<10` band (1073), the mechanical
  weak-verb-inflection pass, the strong-verb principal-parts table, the flagged noise entries, and
  populating `lemma` on every result so far.

- **§312 (v1.0.65): round 5c - the `15-19` sub-band of `10-19` (1165 candidates, 395 rows changed) - `10-19`**
  **itself further split into `15-19`/`10-14` for manageability, same reasoning as the `10-49` split.** Same
  method as every round so far. 315 words `OTHER` -> `VERB`; 80 genuine dual-meaning words `OTHER` ->
  `VERB,OTHER`. The verb-list-authoring approach from §311 (derive `verb_only` as "not already in
  `verb_other`", plus a dedup pass) was kept from the start this round specifically because it proved more
  reliable than hand-fixing each overlap - the script ran clean on the first real attempt as a result,
  compared to several iterative fixes needed in earlier rounds. `git diff --stat` confirmed exactly 395
  lines changed. `dictionaries/de/version.txt` 22 -> 23, pack rebuilt/verified, `LanguagePackCatalog` version
  22 -> 23. No new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 368 ->
  369, `versionName` `"1.0.64"` -> `"1.0.65"`. Not yet device-confirmed. Remaining: the `10-14` sub-band of
  `10-19` (2060 candidates), then the `<10` band (1073), the mechanical weak-verb-inflection pass, the
  strong-verb principal-parts table, the flagged noise entries, and populating `lemma` on every result so far.

- **§313 (v1.0.66): round 5d - the `12-14` sub-band of `10-14` (1110 candidates, 393 rows changed) - `10-14`**
  **itself further split into `12-14`/`10-11`.** Same method as every round so far, using the §311/§312
  "derive `verb_only` as not-already-in-`verb_other`, plus dedup" authoring pattern throughout. 340 words
  `OTHER` -> `VERB`; 53 genuine dual-meaning words `OTHER` -> `VERB,OTHER`. Fail-loud script caught three
  small slips before writing: one word (`verzierten`) already tagged by an earlier round and mistakenly
  re-listed, one word (`verkleiden`) wrongly placed in the dual list when it has no adjective-collision
  reason at all, and one word (`balancieren`, the bare infinitive) that turned out not to exist as its own
  row - only its preterite-plural sibling `balancierten` does, which itself had been reasoned through as a
  dual case but never actually added to either list until the fix. `git diff --stat` confirmed exactly 393
  lines changed. `dictionaries/de/version.txt` 23 -> 24, pack rebuilt/verified, `LanguagePackCatalog` version
  23 -> 24. No new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 369 ->
  370, `versionName` `"1.0.65"` -> `"1.0.66"`. Not yet device-confirmed. Remaining: the `10-11` sub-band of
  `10-14` (949 candidates), then the `<10` band (1073), the mechanical weak-verb-inflection pass, the
  strong-verb principal-parts table, the flagged noise entries, and populating `lemma` on every result so far.

- **§314 (v1.0.67): round 5e - the `10-11` sub-band of `10-14` (949 candidates, 357 rows changed) - closes**
  **out the entire `10-49` band (rounds 1-5e, §306-§314).** Same method throughout. 315 words `OTHER` ->
  `VERB`; 42 genuine dual-meaning words `OTHER` -> `VERB,OTHER`. Fail-loud script caught two slips before
  writing: one genuine typo (`überschriften`, never a real candidate) and one bare infinitive
  (`involvieren`) that doesn't exist as its own row - only its preterite-plural sibling `involvierten`
  (already correctly in the dual list) does. `git diff --stat` confirmed exactly 357 lines changed.
  `dictionaries/de/version.txt` 24 -> 25, pack rebuilt/verified, `LanguagePackCatalog` version 24 -> 25. No
  new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 370 -> 371,
  `versionName` `"1.0.66"` -> `"1.0.67"`. Not yet device-confirmed. **Only the `<10` band (1073 candidates)**
  **remains of the frequency sweep itself.** Also still open, unrelated to the sweep: the mechanical
  weak-verb-inflection pass, the strong-verb principal-parts table, the flagged noise entries (deferred
  cleanup round), and populating `lemma` on every result tagged so far (D-412's own column has not yet been
  written to at all across this whole project - every round so far has only ever applied the `VERB`/
  `VERB,OTHER` tag, deliberately kept as its own separate, independently-verifiable step).

- **§315 (v1.0.68): round 6 - the final `<10` band (1073 candidates, 380 rows changed) - closes out the**
  **entire German verb-in-`OTHER` retagging sweep (rounds 1-6, §306-§315).** Same method throughout. 327
  words `OTHER` -> `VERB`; 53 genuine dual-meaning words `OTHER` -> `VERB,OTHER`. Fail-loud script caught two
  slips before writing: `überdecken` and `beanstanden` were each mistakenly re-listed as fresh candidates
  when only their preterite-plural siblings (`überdeckten`, `beanstandeten`) were actually the new find -
  both bare infinitives already carried `VERB` from an earlier round. `git diff --stat` confirmed exactly 380
  lines changed. `dictionaries/de/version.txt` 25 -> 26, pack rebuilt, `LanguagePackCatalog` version 25 ->
  26. No new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 371 -> 372,
  `versionName` `"1.0.67"` -> `"1.0.68"`. Not yet device-confirmed. **This closes the entire frequency sweep**
  (10,925 candidates across all bands, §306-§315, 9 rounds) - every lowercase, `OTHER`-tagged
  `-en`/`-eln`/`-ern`/`-n` word in the German dictionary has now been individually reviewed. Still open,
  unrelated to the sweep itself: the mechanical weak-verb-inflection derivation pass, the strong-verb
  principal-parts reference table, the flagged noise entries and Learned-Words inflection-gap list (deferred
  cleanup round), and populating `lemma` on every result tagged across all nine rounds (D-412's own column
  has not been written to at all yet - kept as its own separate, independently-verifiable step, deliberately
  not started without a fresh check-in per this project's own convention).

- **§316 (v1.0.69): closed the deferred Learned-Words-inflection-gap and `haptisch`-family backlog, plus**
  **the confirmed LaTeX-noise backlog - all three items queued in Open TODOs after §308.** Verified every
  word against the live dictionary first (nothing assumed). **28 missing words added**: 5 new infinitives
  (`besparen`, `fortfahren`, `duzen`, `siezen`, `nachdenken` - none of these existed at all) plus 17 missing
  finite/imperative forms of already-present verbs (`würdest`, `hättest`, `meinst`, `erzähl`, `beobachte`,
  `ignorierst`, `investiere`, `nachdenkt`, `lösche`, `packt`, `packst`, `vermisse`, `verrosten`, `verrostet`,
  `verrotten`, `verrottet`, `zeigst`, `zeig`), all tagged `VERB`, frequencies calibrated against comparable
  already-present verb forms (person/mood-matched ratios, not guessed blind - e.g. 2nd-person `-st` and bare
  imperative forms consistently land low regardless of the verb's own commonness, matching `siehst`(18)/
  `sieh`(11)/`kommst`(14)'s existing pattern). Plus 5 new `haptisch`-family entries (`haptisch`/`haptische`/
  `haptischen`/`haptischer`/`haptisches`, tagged `ADJECTIVE`, calibrated against `optisch`/`akustisch`'s own
  base-vs-declined frequency ratios - `haptischem` deliberately not added, matching the confirmed pattern
  that this whole adjective class never has a dictionary row for the `-em` dative form either).
  **Explicit participle exclusion honoured**: `fortgefahren`/`bespart` (participles of `fortfahren`/
  `besparen`) deliberately **not** added, per the user's own constraint - future D-404 Tier 1 territory, not
  a bare dictionary row. `verrostet`/`verrottet` were added despite being participle-shaped because both are
  also the genuine 3rd-person-present form (inseparable `ver-` prefix, weak verb, no `ge-` - present and
  participle are true homographs here), not a participle-only addition. **3 existing rows found mistagged**
  while investigating (out of scope for the earlier `OTHER`-only verb sweep since none end in `-en`/`-n`,
  so the mechanical candidate scan never saw them) and fixed: `hätte`(2194) `OTHER` -> `VERB`, `erzähle`(12)
  `OTHER` -> `VERB`, `löschen`(181) `NOUN,OTHER` -> `NOUN,VERB,OTHER`. **7 confirmed LaTeX/programming-noise**
  **rows removed** (`begin`/`align`/`sin`/`colon`/`varepsilon`/`return`/`schen`) - cross-checked against
  `bigram.tsv` before removing, which confirmed genuine LaTeX/math context for all seven (e.g. `begin
  pmatrix`/`begin align`/`sin alpha`/`sin varphi`) and surfaced a second, larger noise cluster left for a
  fresh Open TODO (see below) rather than folded into this round unreviewed. `min` was deliberately **kept**
  despite being on the original candidate list - genuinely ambiguous (the everyday "Minute" abbreviation vs.
  a `\min` LaTeX leak) with a real everyday use case, unlike the other seven which have no legitimate German
  reading at all; erring toward not breaking a real typing case. 15 stale `bigram.tsv` rows referencing the
  3 removed words that had bigram entries (`begin`/`align`/`sin`) also removed, same pattern as D-329's own
  bigram cleanup. `git diff --stat`: `dict.tsv` 41 lines (31 insertions, 10 deletions), `bigram.tsv` 15
  deletions. `dictionaries/de/version.txt` 26 -> 27, pack rebuilt, `LanguagePackCatalog` version 26 -> 27.
  No new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 372 -> 373,
  `versionName` `"1.0.68"` -> `"1.0.69"`. Not yet device-confirmed. One open call worth the user's own
  attention: `besparen` was added as a real (if rare/dialectal) verb based on this session's own knowledge,
  not re-confirmed against the user's actual Learned Words entry as the original TODO note asked - worth a
  quick sanity check that this was really the intended word.

- **§317 (v1.0.70): removed §316's own flagged "second LaTeX-noise cluster" and closed a fresh**
  **user-supplied word-family list.** **7 more confirmed LaTeX-noise rows removed** (`pmatrix`, `bmatrix`,
  `cdot`, `varphi`, `width`, `left`, `end`) - the two genuinely ambiguous candidates spotted at the same time
  (`alpha`, `text`) deliberately left untouched, per the user's own explicit scoping (see Open TODOs). 47
  stale `bigram.tsv` rows referencing them also removed. **19 missing words added** from a fresh user-
  supplied list, verified against the live dictionary first: 4 new infinitives/base forms that didn't exist
  at all (`kriegen` - colloquial "to get", `wischen`, `Dreirad`, `Regenrinne`, `erfreulich`, `hoffentlich` -
  6 total, tagged `VERB`/`NOUN`/`ADJECTIVE`/`OTHER` matching the dictionary's own convention for each word
  class) plus 13 missing finite forms of already-present verbs (`bewerte`/`bewertest`, `erstelle`/
  `erstellst`, `gib`, `installiere`/`installierst`, `kriegst`/`kriegt`, `vermute`/`vermutest`, `wische`/
  `wischst`), all `VERB`, frequencies calibrated against comparable existing entries the same way as §316
  (colloquial-register verbs like `kriegen` calibrated well below their formal synonyms `bekommen`(737)/
  `erhalten`(5857), matching this project's own consistent register-skew pattern). **2 more existing rows**
  **found mistagged** while investigating (same root cause as §316's three - `gibst`/`vermisst` don't end in
  `-en`/`-n` so the original tagging sweep never saw them): `gibst`(150) `OTHER` -> `VERB`, `vermisst`(45)
  `OTHER` -> `VERB`. `git diff --numstat`: `dict.tsv` 21 insertions/9 deletions, `bigram.tsv` 47 deletions.
  `dictionaries/de/version.txt` 27 -> 28, pack rebuilt, `LanguagePackCatalog` version 27 -> 28. No new tests
  (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 373 -> 374, `versionName`
  `"1.0.69"` -> `"1.0.70"`. Not yet device-confirmed.

- **§318 (v1.0.71): `text` `NOUN,VERB` -> `NOUN` - a D-368 mechanical-scan false positive, caught by the**
  **user's own review.** §316/§317's noise cross-check had flagged `text`(5076) as one of two genuinely
  ambiguous LaTeX-noise candidates (alongside `alpha`, kept as-is); the user resolved both by keeping the
  word, but pointed out the `VERB` half of its tag was simply wrong regardless of the LaTeX question - D-368's
  mechanical homograph scan checked whether stem+`"en"` forms a real verb (`text`+`en` = `texten`, which is
  indeed real), but never checked whether the bare stem itself is a valid *inflected form* of that verb. It
  is not: `texten`'s own finite forms are `texte`/`textest`/`textet`, never bare `text`. `git diff --stat`:
  1 line. `dictionaries/de/version.txt` 28 -> 29, pack rebuilt, `LanguagePackCatalog` version 28 -> 29. No
  new tests (data-only). 1064 unit tests unchanged, all green (via JDK 21). `versionCode` 374 -> 375,
  `versionName` `"1.0.70"` -> `"1.0.71"`. Not yet device-confirmed. Found in passing: `texten` itself has no
  dictionary entry at all (see Open TODOs) - not fixed here, out of what was actually asked this round.

- **§319 (v1.0.72): added `texten` ("to text/message someone") with its finite forms, per explicit user**
  **request following up on §318's find.** 4 new rows: `texten`(15)/`textet`(12)/`texte`(10)/`textest`(6),
  all `VERB`, frequencies calibrated against the one existing anchor point for this verb's register - its
  own participle `getextet`(9) - since every other comparable informal/loanword communication verb checked
  for calibration (`chatten`, `simsen`, `mailen`, `posten`, `surfen`) turned out to be entirely absent from
  this Wikipedia-extracted corpus too, consistent with it being a systematically under-represented register
  here, not a gap specific to `texten`. `getextet`(9) itself, already present, gained the `VERB` tag it was
  missing (`OTHER` -> `VERB`) - not a fresh participle addition (which the project's own participle-exclusion
  rule would forbid), just correcting an existing row's tag, same as every other "found mistagged" fix in
  this backlog cleanup. `git diff --stat`: 6 lines (5 insertions, 1 deletion). `dictionaries/de/version.txt`
  29 -> 30, pack rebuilt, `LanguagePackCatalog` version 29 -> 30. No new tests (data-only). 1064 unit tests
  unchanged, all green (via JDK 21). `versionCode` 375 -> 376, `versionName` `"1.0.71"` -> `"1.0.72"`. Not
  yet device-confirmed.

- **§328 (v1.0.80): D-386-followup - is the automatically-resolved file actually the right one?** User's own
  direct follow-up after §327 shipped: does the app report a genuine downgrade distinctly, and does it
  verify the resolved archive's own language, not just trust its file name? Both turned out to be real,
  previously-unnoticed gaps, confirmed by reading the code (not guessed) - and, for the language check, by
  an existing test unintentionally proving it (installing arbitrary content labelled as any `Language`
  without any content check at all). Two independent fixes, one shared round per the user's own explicit
  instruction (interrupted an earlier attempt at bumping/building for the first fix alone - "sonst hättest
  du mir zweimal geversionbumpt und gebuildet"):
  1. **Staleness reporting.** The old `pack.version <= installedVersion` check collapsed "exactly current"
     and "actually older" into the same "already up to date" message. New `LanguagePackInstaller.
     compareVersions()` (pure, unit-tested - 4 new cases) returns a three-way `VersionCheck`
     (`INSTALL`/`ALREADY_CURRENT`/`OLDER_THAN_INSTALLED`); `LanguagePacksActivity.importPack()` now shows a
     distinct message for a genuine downgrade attempt (new `d280_older_than_installed` string) instead of
     silently reporting "nothing to do".
  2. **Language identity.** `version.txt` gained an optional second line - the pack's own declared language
     code - cross-checked in `LanguagePackInstaller.parse()` against the language actually being imported;
     a mismatch throws the new `LanguageMismatchException` (caught distinctly in the Activity, new
     `d280_language_mismatch` string with both codes interpolated), never silently accepted or lumped into
     a generic "import failed". Tolerant of a legacy archive with no second line at all (never rejected on a
     missing declaration). User's own explicit choice between two implementation options (a version.txt
     content line vs. renaming an archive entry) - went with the content line, keeping D-310's own
     fixed-entry-name convention intact.
  Both hosted packs rebuilt to carry the new line, **per the user's own explicit instruction that this must
  cover Greek too, not only German**: `dictionaries/de/version.txt` 33 -> 34, `dictionaries/el/version.txt`
  1 -> 2, `LanguagePackCatalog` versions 33 -> 34 / 1 -> 2. `dict.tsv`/`bigram.tsv`/`hints.tsv` themselves
  untouched - rebuilt via a Python `zipfile.ZipFile` one-liner (this environment's Git Bash has no `zip`
  binary; the German zip was briefly, accidentally deleted mid-attempt and restored via `git checkout`
  before any data was lost) and verified byte-identical against the source `dictionaries/<code>/` files
  afterward, same discipline as every prior pack-rebuild round. `AdaptKey-Language-Contribution-Guide.md`
  updated so a future community-contributed language pack knows about both the accurate-staleness-reporting
  behaviour and the new, recommended `version.txt` second line. 8 new unit tests (`LanguagePackInstallerTest`:
  4 for `compareVersions`, 4 for the language-mismatch validation including case-insensitivity and legacy-
  archive tolerance). 1144 unit tests total (was 1136), all green (via JBR JDK 21, both `:app:assembleRelease`
  and `:app:testDebugUnitTest` verified). `versionCode` 383 -> 384, `versionName` `"1.0.79"` -> `"1.0.80"`
  (one bump covering both fixes together, not two separate rounds). Spec §30 extended with both checks;
  D-386 backlog bullet unchanged (already resolved by §327, this is a direct refinement of the same item, not
  a new one). Not yet device-confirmed.

- **§327 (v1.0.79): D-344/D-386 implemented together - duplicate-download-file resolution via a**
  **once-granted folder tree, not a single-file picker.** Verified feasibility against the official Android
  documentation before implementing (`WebFetch`/`WebSearch`, not guessed): `ACTION_OPEN_DOCUMENT` cannot
  expose sibling files or the parent directory at all - a hard SAF limitation - so finding a browser-renamed
  `"(1)"` duplicate genuinely requires `ACTION_OPEN_DOCUMENT_TREE` (a folder grant) instead; confirmed
  neither intent needs a manifest-declared permission (corrected a wrong assumption raised mid-discussion
  that the tree approach would need one - it does not, same "user-driven system picker, no
  `<uses-permission>`" model as the single-file picker already in use). User's own explicit design call,
  after weighing a simpler "just always delete, rely on a clean folder" alternative: go with the more robust
  tree approach anyway, since "der ganze Ansatz ist schon nicht benutzerfreundlich" and it should not be made
  harder than necessary - still deletes after import too, as originally asked.
  New `download` package: `DuplicateDownloadMatcher` (pure - matches a file's exact name or a `" (N)"`-suffixed
  duplicate, returns the newest by last-modified; unit-tested including case-sensitivity, no-extension names,
  multi-digit suffixes, and unrelated-similar-prefix rejection) + `DownloadCandidate`; `DownloadFolderStore`
  (Android glue - persists the granted tree URI via `takePersistableUriPermission`, shared by both import
  screens so the grant is asked for only once regardless of which screen needs it first); `DownloadFolderResolver`
  (Android glue - lists a tree's children via `DocumentsContract`, resolves the newest match, deletes it
  after import when no older than the new `DELETE_MAX_AGE_MILLIS` = 60,000ms per the user's own figure; the
  age comparison itself is split into a pure, unit-tested `isRecentlyCreated`). Both `LanguagePacksActivity`
  and `Tier3ModelActivity`'s own "Import" button rewired identically: reuse an existing grant, or show a
  short rationale dialog before requesting one (`ACTION_OPEN_DOCUMENT_TREE`, best-effort `EXTRA_INITIAL_URI`
  hint toward Downloads); resolve + import automatically, no file picker shown at all once a folder is
  granted; no match found clears the stale grant and re-prompts (the only sensible recovery). `importPack`/
  `importModel` no longer spawn their own background thread (the caller already runs on one). Localised the
  new/updated strings (EN/DE/EL), including correcting `c06_model_privacy`'s now-stale "only reads the one
  file you pick" claim. 14 new unit tests (`DuplicateDownloadMatcherTest` 10, `DownloadFolderResolverTest`
  4). 1136 unit tests total (was 1122), all green (via JBR JDK 21, both `:app:assembleRelease` and
  `:app:testDebugUnitTest` verified). `versionCode` 382 -> 383, `versionName` `"1.0.78"` -> `"1.0.79"`. Spec
  §30 rewritten from "decision deferred" to the implemented mechanism; D-344/D-386 backlog bullets marked
  resolved. Not yet device-confirmed - Samsung One UI's own sandboxing behaviour, the concrete complaint that
  prompted D-386, is exactly the kind of OEM-specific quirk this environment cannot verify directly.

- **§326 (still v1.0.78, no code change): two backlog decisions plus a documentation restructure.** (1)
  Discussed for/against directly with the user whether German should become a bundled language again
  (D-385, from the vacation batch) - decided against; captured as its own resolved backlog item (see "Open
  TODOs" above) rather than left ambiguous. (2) Found D-386 (the actual backlog item for "the picked
  download file isn't the right one, Samsung One UI renames it `(1)`") after an initial wrong search hit
  D-344 instead - a different, related item (download-directory *control*, not post-download duplicate-file
  *detection*); both now live side by side in the backlog. (3) At the user's own explicit request - "das
  sind ja sehr viele Punkte, die nicht als ein Paragraph geführt werden können" - split the §276 vacation-
  usage batch (D-352 through D-404, ~50 items previously reachable only as one dense paragraph plus the full
  original text buried in `AdaptKey-History.md`) into 45 individually-addressable backlog bullets (the six
  that already had their own bullets - D-330-followup/D-344/D-345/D-367/D-368/D-402/D-404 - were left as
  they were, not duplicated). Statuses resolved via `grep`-driven cross-referencing against every later round
  that touched each D-number, not guessed: 12 already fully resolved by earlier rounds (D-352/D-353/D-354/
  D-358/D-359/D-368/D-388, plus D-403 closed out this very round by §325 above), 2 resolved just now (D-385)
  or in progress (D-386), 2 flagged with an important nuance (D-355 - distinct from the D-405 bug that got
  spun off from the same original report; D-356 - still awaiting a concrete example), 1 flagged as possibly
  superseded and worth re-checking rather than closed by assumption (D-381 - a learned word's category is
  now user-editable per D-404 §323/§324, which may already satisfy this), and the remaining ~29 left open
  exactly as originally captured, condensed rather than transcribed (full original wording stays in
  `AdaptKey-History.md` §276, untouched, per the project's own append-only convention for that file). No
  code changed, no tests run, no version bump. See "Open TODOs / Known Limitations" above for the full
  split list.

- **§325 (v1.0.78): D-404-followup - acronyms ("ETF"/"AVD") are never autocorrected away and learn at the**
  **ordinary threshold.** User's own worry directly confirmed as a real, currently-reproducible bug before
  this fix - not hypothetical: probed the real bundled German dictionary (a throwaway test, deleted after
  use) and found typing `"etf"` was silently autocorrected to `"etc"` (cost-1 QWERTZ-adjacent, far more
  frequent) on every single attempt, meaning the acronym's own W-02 pending counter could never even begin
  to move through ordinary typing - it would never have been learnable at all. New shared, pure
  `suggestion.Acronym.isAcronym()` (at least two letters, every one uppercase - the deliberate, explicit
  "this is an acronym" signal a user gives by typing that way) used at two call sites, kept in lockstep
  rather than duplicated: `DictionarySuggestionProvider.bestCorrection()` (backs `autocorrectFor`/
  `bestCorrectionFor`/`highConfidenceCorrection`) now vetoes autocorrect against an acronym outright -
  checked against the original typed casing, ahead of every other branch, an absolute veto stronger than
  D-403's own `learnedCasingOf` exemption since it also protects the very first, not-yet-learned typing, not
  only an already-learned word. `AdaptKeyService.learnThresholdFor()` (W-02) now checks the acronym signal
  before either existing compound-suspicion signal, so an acronym promotes after the ordinary two
  repetitions rather than the four a suspected-compound token needs - its embedded capitals are what *make*
  it an acronym, not evidence of a missing space. Third part of the user's own three-part request turned out
  to need no code change at all: confirmed (and cited an existing test proving it) that a later lower-case
  typing already resolves to, and ranks by, the learned entry's own casing over any differently-cased
  bundled variant sharing the same key (D-264's own `unigramsByPrefix`/`entryOf` merge - the exact
  `"MSCI"`-vs-`"Msci"` case already regression-tested). Deliberately never silent auto-substitution on
  commit either way, by design (A-01's own "explicit input is never silently changed" principle) - confirmed
  directly with the user, who wanted exactly this (a prioritised chip, not a silent rewrite). Accepted
  trade-off, confirmed with the user: a stuck Caps Lock disables ordinary typo-autocorrection for as long as
  it stays engaged, matching how other mainstream keyboards already treat all-caps input. 10 new tests
  (`AcronymTest` 7, `DictionarySuggestionProviderTest` +3, including a direct reproduction of the real
  `"etf"`->`"etc"` bug pinned with synthetic data so it stays pinned independent of the live dictionary's
  own future contents). 1122 unit tests total (was 1112), all green (via JBR JDK 21, both
  `:app:assembleRelease` and `:app:testDebugUnitTest` verified). `versionCode` 381 -> 382, `versionName`
  `"1.0.77"` -> `"1.0.78"`. Spec gained new §40. Not yet device-confirmed.

- **§324 (v1.0.77): D-404 Tier 3, with-LLM path - whole-family learning + the unified reprocessing**
  **backfill.** Closes out D-404's own explicit "with LLM, always learn the whole family" requirement
  (§323's non-LLM path stays exactly as it was - this is purely additive). `Tier3Provider` gained a second
  task method, `predictFamily(Tier3FamilyRequest): Tier3FamilyResult`, deliberately separate from the
  existing next-word-continuation `predict`/`Tier3Request`/`Tier3Result` (different prompting - an explicit
  instruction plus a rigid `KEY=value` answer format the model is primed to continue mid-answer, not plain
  continuation - and a different token budget, the hard `Tier3Decoding.MAX_NEW_TOKENS` cap since this runs
  once per learn event rather than once per keystroke); default implementation returns
  `Tier3FamilyResult.EMPTY`, so `NoopTier3Provider` needed no override. New `Tier3FamilyPrompt` (builds the
  German instruction prompt, primes `"GRUNDFORM="` so the model's own continuation starts right at the
  answer) + `Tier3FamilyResponseParser` (deliberately generous - each of `GRUNDFORM=`/`WORTART=`/`FORMEN=`
  is extracted independently via its own regex rather than requiring the whole line to parse as one unit,
  since a small 360M-parameter model will not always reproduce the format exactly; only the lemma is
  load-bearing - a missing/garbled category or forms field just leaves that part null/empty rather than
  discarding an otherwise-usable lemma; forms are letters-only-filtered, de-duplicated case-insensitively,
  and capped at 16 so a degenerate/repetitive generation cannot flood the lexicon). `OnnxTier3Provider.
  predictFamily()` composes these with the same tokenizer/session already used for `predict()`.
  `Tier3FamilyApplier.apply(store, result)` (new, pure over `DictionaryStore`, unit-tested via
  `InMemoryDictionaryStore` rather than living untested inside `AdaptKeyService`) is the one shared
  operation both call sites below reduce to: learn every family form (an ordinary `learn()` call per form,
  `result.category` as the `categoryHint`), then link every non-lemma form back to the lemma via
  `DictionaryStore.setLearnedLemma` - promoted from a `SqliteDictionaryStore`-only method to the shared
  interface (implemented in `InMemoryDictionaryStore` too) specifically so this logic never needs to
  downcast the interface - unless that form already carries its own link, so a prior manual "Grundform"
  correction or an earlier application of the same family is never silently overwritten.
  Live path: `AdaptKeyService.dispatchFamilyLearning()`, called from both `learnWord()`/`learnWordStrong()`
  right after their own ordinary (non-LLM) outcome, only for a genuine write (`LEARNED`/`PROMOTED` - never
  `SKIPPED`/`PENDING`) and only when a real backend (`onnxProvider`) is present; dispatches on
  `tier3Executor` with `dictionaryStore`/`onnxProvider` captured on the calling thread first (mirrors
  `refreshSuggestions()`'s own orchestrator-capture pattern) - `predictFamily` is synchronous and heavy, so
  it must never run on the IME thread. Backfill path: "LLM installed is a state, not a history" - rather
  than special-casing "already installed at migration time" inside `ensureLearnedLemmaColumn()`'s own
  synchronous `init {}` migration (which must stay fast, ruled out heavy LLM work there entirely), each
  language store's `TABLE_META` gained a `family_reprocess_version` key
  (`familyReprocessVersion()`/`setFamilyReprocessVersion()`, mirroring `learnedCleanupVersion`'s own scheme).
  New `AdaptKeyService.maybeReprocessFamiliesAsync()` runs every time `loadTier3ProviderAsync()` actually
  builds a real backend - which happens on *every* fresh service instance that finds a model already
  installed, not only right after a fresh import, which is what makes this single mechanism cover both of
  D-404's original trigger conditions at once - and, for every `SqliteDictionaryStore` still behind the
  current version, reprocesses every learned word still missing a category or a lemma link
  (`learnedWordsWithTimestamp()`, filtered) through the same `predictFamily`+`Tier3FamilyApplier.apply`
  pair, entirely on `tier3Executor`, before bumping that store's version - a cheap no-op on every later
  startup once already run. No original sentence context is available for a backfilled word
  (`Tier3FamilyRequest.sentence` defaults to empty, treated as "lemmatise this word in isolation").
  28 new unit tests (`Tier3FamilyPromptTest` 6, `Tier3FamilyResponseParserTest` 12, `Tier3FamilyApplierTest`
  6, `InMemoryDictionaryStoreTest` +3 for `setLearnedLemma`, `NoopTier3ProviderTest` +1). 1112 unit tests
  total (was 1084), all green (via JBR JDK 21, both `:app:assembleRelease` and `:app:testDebugUnitTest`
  verified). `versionCode` 380 -> 381, `versionName` `"1.0.76"` -> `"1.0.77"`. Spec §39 extended (its own
  "Deferred" section rewritten into "With-LLM path (implemented)"); the D-404 backlog item above is now
  fully resolved except for tier 2. Not yet device-confirmed - no device/ONNX runtime in this environment,
  same already-accepted limitation the rest of tier 3 has; the prompt format's actual reliability against
  the real SmolLM2-360M-Instruct model is therefore unverified beyond the parser's own generous, defensive
  design.

- **§323 (v1.0.76): D-404 Tier 3, non-LLM path - Learned Words base-form consolidation.** Pure code, no
  dictionary data touched (see spec §39 for the full mechanics). `TABLE_LEARNED` gained its own `lemma`
  column via a new guarded `ensureLearnedLemmaColumn()` (same `PRAGMA table_info`-checked `ALTER TABLE ADD
  COLUMN` pattern as D-388/D-412's own precedents, called unconditionally from `init {}` - never a
  `DATABASE_VERSION` bump, honouring the user's explicit "never reset/wipe the Learned Words list"
  constraint). New `LearnedLemmaLinking` object: extremely conservative, lookup-only base-form linking in
  both directions (`findLemma` - a newly-learned word strips a closed set of noun endings
  `-s/-es/-e/-en/-er/-n/-nen/-ern` or, failing that, `RegularVerbInflection`'s own verb-personal endings,
  and checks whether the stripped candidate is already a learned entry; `candidateInflections` - the reverse,
  checking whether the newly-learned word is itself the base of an already-learned, not-yet-linked entry) -
  never fabricates a row, only links two words already genuinely typed and learned. Caught and fixed a
  self-match bug of my own during test-writing: `findLemma`'s noun branch originally relied on a length guard
  alone before calling `String.removeSuffix()`, which no-ops (returns the string unchanged) when the suffix
  doesn't actually match - risking a word reinforcing itself into its own lemma; fixed with an explicit
  `endsWith` check before stripping. `DictionaryStore.learn()` gained an optional `categoryHint` parameter -
  `AdaptKeyService.learnWord()`/`learnWordStrong()` pass `NOUN` when a word is typed capitalised but only
  *mid-sentence* (never at a sentence start, which capitalises regardless of true category), applied only
  while the category is still unset and re-checked on every reinforcement, never overriding an
  already-known category. `putWordInternal` now always carries `TABLE_LEARNED`'s own `lemma` forward
  explicitly on every write (`learn`/`unlearn`/`recaseLearnedWord`/`restoreLearnedWord`) - `INSERT OR
  REPLACE` semantics meant an omitted column reverts to its default, so an established link would otherwise
  be silently wiped by the very next reinforcement. Migration's own one-time pass: after the `ALTER TABLE`,
  every pre-existing row runs the same forward-only lookup against every other row already in the table
  (covers both directions across the whole existing set without a separate reverse sweep). Editor
  (`LearnedWordsActivity`, D-292/D-294's dialog): the list itself now shows only entries with no lemma link
  as their own row (a linked inflected form no longer clutters it once its base is known); an entry with no
  known category gets a trailing asterisk; the tap-to-edit dialog gained a category multi-select (one
  checkbox per `PartOfSpeech` tag, backed by new `setLearnedCategories()`) and a "Grundform" dropdown (every
  other learned word plus "unbekannt", backed by new `setLearnedLemma()`) for manual power-user correction.
  Localised the new strings (EN/DE/EL). Known, accepted limitation: a mis-linked child entry has no direct
  path back into the edit dialog today, since it no longer appears as its own row - relink from the correct
  entry's own dropdown instead; accepted given this whole screen is explicitly framed as power-user-only.
  20 new unit tests (`LearnedLemmaLinkingTest`, 12; new `InMemoryDictionaryStoreTest` cases, 7 - including
  one asserting the self-match bug stays fixed; `RegularVerbInflectionTest` unchanged, still green after
  `candidateInfinitives()` was factored out of `isPlausibleInflection()`). 1084 unit tests total (was 1064),
  all green (via JBR JDK 21, both `:app:assembleDebug` and `:app:assembleRelease` verified). `versionCode`
  379 -> 380, `versionName` `"1.0.75"` -> `"1.0.76"`. Spec gained new §39; the D-404 backlog item above marked
  resolved for tiers 1 and 3's non-LLM path. **Deferred, not implemented this round**: tier 2 (bundled-
  dictionary ranking/override cross-reference), and tier 3's own LLM-powered extension (whole-family learning
  on every learn event via a `Tier3Provider` extension not yet designed, plus the "LLM newly installed"
  reprocessing-pass trigger and the migration's own LLM-aware branch) - both explicitly scoped out at the
  user's own direction, to be picked up as a separate follow-up round. Not yet device-confirmed (UI/gesture
  behaviour is untestable in this environment - no emulator - only the pure logic underneath was unit-tested,
  per this project's own accepted, already-established limitation).

- **§322 (v1.0.75): D-404 Tier 1, the "Wortfamilien" project - complete German noun/verb paradigms**
  **generated and added, not just linked.** Extends §320/§321's lemma-linking groundwork from "link what
  already exists" to "generate and add what's missing": full noun declension (Genitiv/Dativ/Akkusativ
  Singular, Plural, Dativ Plural) and full verb conjugation (Präsens x6, Präteritum x6, Partizip II,
  Imperativ Singular+Plural), for every already-present lemma without one, each new row lemma-linked via
  D-412's column as a direct byproduct of generation. First attempt was a from-scratch rule engine:
  `genus.py` (noun gender via `eine`/`einer`=feminine, `das`=neuter-if-present-at-any-count-since-real-
  masculines never co-occur with it, `ein`/`einem`/`eines`=masculine-by-elimination otherwise - `der`
  deliberately never trusted alone, since it also marks feminine Genitiv/Dativ), `deklination.py`/
  `plural.py` (strong/weak declension + plural-class rules), `konjugation.py` (strong/weak conjugation +
  a curated per-verb override table, `praefix_overrides.py`, for the ~150 individually-reviewed cases
  where durch-/um-/über-/unter-/voll-/hinter-/wieder- genuinely can't be classified as separable-or-not
  from the prefix alone - e.g. `übertragen` stays unhandled, `unternehmen` is always inseparable,
  `wiederfinden` always separable). Each component went through several real-bug-found-and-fixed rounds
  against live corpus/reference data (documented in-line in the scripts themselves - epenthesis gaps,
  sibilant-collapse rules, Präteritopräsentia's suppletive forms, false-friend prefix matches like
  `erben`/`Beispiel` that only *look* prefixed), but noun plural-class assignment in particular kept
  surfacing new exception classes faster than rules could close them (German plural choice is often
  lexical, not derivable from spelling), so on explicit user instruction the approach pivoted: the
  German Wiktionary extract via `wiktextract`/kaikki.org (MIT-licensed tool, CC BY-SA-licensed content -
  same licence family this project's `dict.tsv`/`bigram.tsv` already come from) is now the *primary*
  source for both nouns (`extract_wiktionary_nouns.py` -> `wiktionary_nomen.tsv`, 119,779 nouns with
  genus+genitiv+plural) and verbs (`extract_wiktionary_verbs.py` -> `wiktionary_verben.tsv`, 14,412 verbs
  with full conjugation); `nomen.py`/`verben.py` check the Wiktionary table first, falling back to the
  hand-built rule engine only for words missing there (~38% of noun lemmas / ~57% of verb lemmas by word
  count found in Wiktionary, but ~64%/higher by frequency weight - common words are disproportionately
  covered). Explicitly rejected on the user's own reflection: bulk-importing the ~91,559 Wiktionary nouns
  entirely absent from `dict.tsv` - cross-checked against `bigram.tsv` and only 2 of them have any
  occurrence there at all, meaning that pool carries essentially no real frequency signal; the project
  stayed scoped to completing existing lemmas' paradigms, not growing the vocabulary itself. New-row
  frequency: lemma-frequency × a ratio calibrated from each POS's own already-linked pairs (nouns: median
  0.355 from the 14,976 §320/§321 pairs; verbs: median 0.417 from 191 pairs). Collision rule: never write
  a form already present anywhere in `dict.tsv` under any POS. The verb write surfaced real candidate-
  list contamination the earlier validation passes had missed - preterite-plural/participle/Konjunktiv-II
  forms of already-known strong verbs (`wurden`/`waren`/`worden`, `misslangen`, `gestünden`) and zu-
  infinitives of separable verbs, both classes being mistaken for their own base infinitives - found and
  fixed over six write-verify-revert-refix rounds; the eventual fix generalised a fixed-prefix-list
  exclusion check into "does this word end in a long-enough known-strong-verb inflected form, regardless
  of what precedes it", which also closed compound/double-prefix cases the fixed list structurally
  couldn't reach (`nachvollzogen` = nach+vollzogen). Final random-sample spot check (70 rows) came back
  fully error-free. 8 more strong verbs turned up missing from the original hand-curated table along the
  way and were added, each confirmed against real `dict.tsv`-attested forms first, not guessed:
  `schmelzen`/`verderben`/`gedeihen`/`weichen`/`schwellen`/`erlöschen`/`bergen`/`gleichen`/`streichen`/
  `schleichen`/`preisen`/`erwägen`/`winden`/`schinden`/`sprießen`/`reiben`/`hauen`/`stechen`/`schwören`/
  `schleifen`/`treten`/`werben`/`fallen`/`ringen`/`schreiten`/`gebären`/`schlingen`/`misslingen`/`heben`/
  `empfinden`/`schleißen`/`trügen`. Net result: `dict.tsv` 119,701 -> 158,073 rows (+33,390 noun forms,
  +4,982 verb forms), 153,091 of them now lemma-linked (was 14,976). `git status`-clean otherwise -
  `bigram.tsv`/`hints.tsv` untouched, confirmed by hash. `dictionaries/de/version.txt` 32 -> 33, pack
  rebuilt and verified by unzipping it back and byte-comparing `dict.tsv` against the source, plus a
  `version.txt` content check; `LanguagePackCatalog` version 32 -> 33. No new tests (data-only; `lemma`
  still has zero code readers - remains groundwork for whatever eventually reads it). 1064 unit tests
  unchanged, all green (via JDK 21). `versionCode` 378 -> 379, `versionName` `"1.0.74"` -> `"1.0.75"`. Not
  yet device-confirmed.

- **§321 (v1.0.74): the noun-inflection-linking project completed end to end - every remaining**
  **frequency band worked through to closure.** Continuation of §320's round 1, per explicit user
  instruction to proceed autonomously band by band without stopping for interim builds. Bands
  processed this pass: `500-1999` (408 links), `200-499` (770), `50-199` (3,113 across 6 rounds),
  `20-49` (4,597 across 8 rounds), `10-19` (5,238 across 10 rounds), `5-9` (771 across 2 rounds) - the
  last of these confirmed empty below frequency 5 by the same generator script used throughout. Total
  across the whole sweep (§320's round 1 included): ~20,024 mechanical candidates individually
  reviewed, 14,976 confirmed and linked to their base form (verified directly: `awk` count of non-empty
  `lemma` rows in `dict.tsv` matches the sum of every round's own reported link count exactly). Same
  taxonomy as §320 throughout, refined round to round as new patterns appeared: **rejected** -
  derivational demonyms (`Berliner`->`Berlin`-style place-to-person forms, per the original explicit
  exclusion), agent-noun `-er` derivations (`Politiker`/`Lehrer`-style "one who does X" from a root that
  is not itself what's being inflected), short-stem/coincidental collisions with an unrelated real word
  (`Mais`≠`Mai`, `Kohlen`≠`Kohl`, `Ulmen`≠`Ulm`), cross-category pairs (a nominalised-infinitive vs. a
  plain noun sharing a root, e.g. `Schälen`≠`Schal`), foreign/English-spelled plurals kept out for
  safety (`Divisions`, `Operas`, `Chains`), and proper-noun genitives that are themselves standalone
  surnames rather than derived forms (`Carlsen`, `Johannsen`, `Andresen`). **Accepted** beyond plain
  case/number inflection - adjective declension where both sides already carry `NOUN` (nominalised
  adjectives), genitive-of-name/surname unless the `-s` form is itself an independent surname, archaic/
  dialectal doublets of an already-established word (`Lichter`/`Licht`, `Türe`/`Tür`), and a small,
  case-by-case-verified set of person-noun plural exceptions extending §320's own list
  (`Zwangsarbeiterinnen`->`Zwangsarbeiter`, `Franziskanerinnen`->`Franziskaner`,
  `Soldatinnen`->`Soldat`) where the base itself was already an accepted person-noun, never a fresh
  place-to-demonym derivation. Chain candidates continued to resolve to the true deepest root rather
  than an intermediate mechanical hop, per the pattern §320 established, with several dozen manual
  corrections per round where the mechanical umlaut-reversal or suffix-strip landed on a spurious
  fragment or a semantically unrelated intermediate word instead of the real lemma (e.g.
  `Ulmen`->`Ulme` not `Ulm`, `Kohlen`->`Kohle` not `Kohl`, `Baches`->`Bach` skipping the false
  intermediate `Bache`=female wild boar). Write script's fail-loud asserts (target is `NOUN`-tagged,
  currently has an empty `lemma`, chosen base actually exists as its own row) never fired across any of
  the 27 rounds committed this pass. `git diff --stat`/`--numstat` confirmed each round's exact
  2x-line-count before every commit, all committed individually (27 data-only commits, no version bump
  per round per the explicit "don't stop for interim builds" instruction). `dictionaries/de/
  version.txt` 31 -> 32, pack rebuilt, `LanguagePackCatalog` version 31 -> 32. No new tests (data-only;
  `lemma` still has zero code readers - this remains pure groundwork for D-404 Tier 1). 1064 unit tests
  unchanged, all green (via JDK 21). `versionCode` 377 -> 378, `versionName` `"1.0.73"` -> `"1.0.74"`.
  Not yet device-confirmed. **This closes the entire noun-inflection-linking project** - `lemma` is now
  populated for every noun inflection the mechanical suffix/umlaut heuristic could find and a human
  could individually confirm, across the full frequency range down to 5.

- **§320 (v1.0.73): kicked off the German noun-inflection-linking project - D-412's `lemma` column**
  **populated for the first time (0 rows -> 79), round 1 of the `>=2000` frequency band.** New effort,
  requested explicitly and scoped via `AskUserQuestion` before starting (mirroring how the verb project
  began): 88,012 words carry the `NOUN` tag (~8x the verb project's own candidate pool). A mechanical
  prototype (suffix-stripping `-nen`/`-innen`/`-en`/`-er`/`-n`/`-e`/`-s` plus umlaut-reversal on the *last*
  umlaut only) found ~20,000 words with a plausible existing-row base-form candidate; a 40-word random spot
  check found ~12-15% clear false positives (short/foreign stems coincidentally colliding with an unrelated
  word - `Mais`->`Mai`, `Pointer`->`Point`, `Orbiter`->`Orbit`, `Preston`->`Presto`), confirming individual
  review is still required, not a mechanical auto-apply. **Two design decisions resolved with the user
  up front**: (1) full individual review of every candidate, same rigor as the verb project, since `lemma`
  is not yet read by any code (safe to refine later before D-404 Tier 1 actually consumes it); (2)
  derivational word-formation (demonyms like `Berliner`->`Berlin`, `Schweizer`->`Schweiz`, `Wiener`->`Wien`)
  explicitly **excluded** - only true inflection (case/number, and adjective declension when both sides
  already carry the `NOUN` tag, e.g. nominalised `deutsche`/`deutschen`->`deutsch`) counts as a link. This
  round: the `>=2000` band, 124 mechanical candidates individually reviewed, 79 confirmed and linked, 45
  rejected (demonyms, proper-noun/place-name collisions, and short-stem false matches like `Kaiser`->`Kais`,
  `Wasser`->`Wass`, `Bürger`->`Burg`). **13 links were manually corrected to the true base rather than the
  raw mechanical match** where the mechanical candidate landed on a spurious fragment or an intermediate
  form instead of the real lemma (`Namen`->`Name` not `Nam`, `Kirchen`->`Kirche` not `Kirch`, `Ländern`-
  >`Land` not the intermediate plural `Länder`, `Jahres`->`Jahr` not the intermediate plural `Jahre`, and
  similarly for `Landes`/`Stimmen`/`Regionen`/`Quellen`/`Reiches`/`Grenzen`/`Krieges`/`Schulen`/`Juden`) -
  each verified to actually exist as its own `NOUN`-tagged row before writing. `Nord`/`Süd`/`West`/`Ost`
  vs. `Norden`/`Süden`/`Westen`/`Osten` deliberately treated as independent lemmas, not an inflection pair
  (both are established independent headwords in modern usage, not one derived live from the other).
  Write script fail-loud-asserts each target word is `NOUN`-tagged, currently has an empty `lemma` field,
  and its chosen base actually exists as its own row, before writing - same pattern as every tagging round.
  `git diff --stat` confirmed exactly 79 lines changed (158 total: old+new per line). `dictionaries/de/
  version.txt` 30 -> 31, pack rebuilt, `LanguagePackCatalog` version 30 -> 31. No new tests (data-only;
  `lemma` still has zero code readers, same groundwork-only status as before). 1064 unit tests unchanged,
  all green (via JDK 21). `versionCode` 376 -> 377, `versionName` `"1.0.72"` -> `"1.0.73"`. Not yet
  device-confirmed. **Remaining: ~19,900 more mechanical candidates across the rest of the frequency range**
  (500-1999, 200-499, ... down through the same banding pattern as the verb project), to be worked through
  round by round.

- **§304 (v1.0.57): D-330-followup - the full possessive-pronoun audit; the entire combined cleanup bundle**
  **is now closed.** Read `KeyboardProximity.kt` (the app's real QWERTZ adjacency grid) and confirmed
  `forKnownWordOverride`'s `cost <= 1` gate only ever fires for a single keyboard-adjacent substitution with
  nothing else different (`ADJACENT_SUB_COST=1` vs `SUB_COST`/`INDEL_COST=2`). Pulled all 36 existing declined
  forms of `mein`/`dein`/`sein`/`ihr`/`unser`/`euer` from the live dictionary and computed every real
  single-adjacent-substitution pair among them programmatically (not estimated), scoring each with
  `CorrectionConfidence`'s actual log-scaled formula. Only one genuine risk found: bare `dein` (139) vs `sein`
  (28942), score ≈0.86, clears `MEDIUM`'s threshold - the exact case originally flagged. The suffixed
  `deine`/`deinen`/`deinem`/`deiner`/`deines` D-330 already fixed all score 0.64-0.68 today, confirmed still
  safely below every level. No other cross-pronoun pair scored above 0.12 anywhere in the full comparison -
  five of the six determiners need no change at all. Fixed `dein` 139 -> 550 (matching `deine`'s own already-
  fixed margin). `git diff --stat`: 1 line. `version.txt` 15 -> 16, pack rebuilt/verified, `LanguagePackCatalog`
  version 15 -> 16. No new tests. 1059 unit tests unchanged, all green (via JDK 21). `versionCode` 360 -> 361,
  `versionName` `"1.0.56"` -> `"1.0.57"`. Not yet device-confirmed. **This closes the entire originally-agreed
  D-402/D-306-followup/D-330-followup/D-345/D-367/D-368 combined cleanup bundle** - nothing remains open from
  it. See history §304.

  Also answered a design question first (no code change): why `"Stk."`-style abbreviation recognition lives
  in `Abbreviations.kt` rather than as an `ABBR` tag in `dict.tsv`. Kept separate deliberately - `PartOfSpeech`
  drives word-capitalisation, `Abbreviations.kt` answers a punctuation question (`SentenceBoundary.kt`'s own
  sentence-end check) that several entries (`z.b.`, `n.chr.` - internal periods) don't fit into the
  one-word-per-row dictionary model anyway, and the abbreviation list is deliberately dependency-free
  (doesn't need the dictionary loaded). The existing `OTHER` tags on bare forms already in `dict.tsv`
  (`bzw`/`ca`/`usw`/...) are correct for their own purpose; the `"Stk."` gap was a missing-entry problem in
  both places, not a tagging problem.

- **§303 (v1.0.56): D-402 "Stk." fix, and everything else in this cleanup round checked/closed out.**
  `"Stk."` recognition is a **code** fix, not a dictionary one - `capitalisation/Abbreviations.kt`'s `GERMAN`
  set (consumed only by `SentenceBoundary.kt`, to stop a trailing period being treated as a sentence end)
  was missing `"stk."` - added, plus a matching `AbbreviationsTest` case. Also added the bare word `"Stk"`
  (350, `OTHER`) to `dict.tsv` itself, missing independently of the abbreviation-period question. Checked
  every other still-open item from D-402's original report against the live dictionary: `Robotische`/
  `Scheiße`/`Traditionell`/`Beugungen`'s bad splits are now structurally impossible (their noise halves
  `ische`/`Sc`/`ell`/`en` no longer resolve, `ische`/`ell`/`en` removed in §301, `Sc` was never real) - no
  action needed. `"Wegerecht"`->`"we"`+`"gerecht"`: the original report's "`we` only exists via the English
  dictionary" explanation is stale (no English pack ships at all; `"We"` 203 sits directly in the German
  dict, status unclear) - fixed the symptom directly instead by adding `"Wegerecht"` (20, `NOUN`) as its own
  entry, leaving the ambiguous `"We"` alone. `git diff --stat`: `dict.tsv` +2 rows (119,662 -> 119,664).
  `version.txt` 14 -> 15, pack rebuilt/verified, `LanguagePackCatalog` version 14 -> 15. One new unit test;
  1059 total, all green (via JDK 21). `versionCode` 359 -> 360, `versionName` `"1.0.55"` -> `"1.0.56"`. Not
  yet device-confirmed. **This closes the whole D-402/D-306-followup/D-345/D-367 combined round** - only
  D-330-followup's own audit remains open (not part of what was asked this round). See history §303.

- **§302 (v1.0.55): D-402 missing-word additions + D-367 `natürlich`-frequency fix.** Checked every word on
  D-402's list live first - several (`tue`/48, `vorm`/30, `wessen`/20, all lowercase) already existed, likely
  from an earlier untracked change; the case-insensitive-key architecture means those lowercase rows already
  cover the capitalised `Vorm`/`Wessen` reports too. Read `CorrectionConfidence.kt` directly (not the stale
  "100x flat bar" text still in the D-330-followup bullet below - D-353 replaced that with a log-scaled
  curve, `ln(ratio)/ln(500)` against `AutocorrectAggressiveness`'s thresholds) and confirmed `vorm`/`tue`
  still had a live override risk against `Form`(10141)/`The`(7983) at MEDIUM's default 0.75 threshold - fixed
  the same way D-330 fixed `dein`/`sein`: raised the too-rare word's frequency (`vorm` 30->200, `tue`
  48->250, `wessen` 20->90 with no confirmed live collision but unrealistically low regardless). Also fixed
  `"aggressive"`(119) outranking `"aggressiv"`(80) exactly as D-402 reported (`aggressiv` 80->300). Checked
  `AdjectiveInflection.kt` before adding `"agentisch"` - confirmed it only protects comparative/superlative
  forms, never plain positive-degree declension, so added all six regular forms by hand as the report asked.
  Checked the D-354 regression test before adding `"aberkennen"` - confirmed no conflict (that test's store
  is synthetic, never reads the real dict). Remaining genuinely-missing words added with frequencies
  calibrated against comparable existing entries: `drüber`/15, `drunter`/15, `neulich`/40, `vertan`/45,
  `ah`/60, `Oh`/90, `erstaunlicherweise`/20. D-367: confirmed `natürlich`(707) really was ranked below its
  own `natürlichen`(1512)/`natürliche`(1313) - raised to 2500; `"Nature"`(148, the English-loanword/citation
  artefact) removed outright per the explicit "should not appear" instruction, `"Natura"`(131, the real
  Natura-2000 term) left alone. `git diff --stat`: 19 insertions/6 deletions exactly (14 additions + 5
  corrections + 1 removal); `dict.tsv` 119,649 -> 119,662 rows. `version.txt` 13 -> 14, pack rebuilt/verified,
  `LanguagePackCatalog` version 13 -> 14. Still open: `"Wegerecht"`->`"we gerecht"` (cross-language, needs its
  own investigation), the `"Stk."` abbreviation request, and D-330-followup's full audit - none touched this
  round. No new tests. 1058 unit tests unchanged, all green (via JDK 21). `versionCode` 358 -> 359,
  `versionName` `"1.0.54"` -> `"1.0.55"`. Not yet device-confirmed. See history §302.

- **§301 (v1.0.54): D-402/D-306-followup/D-345 dictionary garbage cleanup - 348 noise entries removed,**
  **fully automatically at the user's explicit request.** Removed the already-named noise from spec/history
  outright (`Mur`/`BDI`/`Dee` - no English pack exists to blacklist against instead; `en`/`ell`/`lich`/
  `ische`, the four confirmed corpus-tokeniser split-artefacts from §277 - `heiße`, the fifth and genuinely
  real one, explicitly left alone; `fir`, the confirmed `"fir"`->`"dir"` autocorrect-noise entry). For the
  broader sweep, regenerated D-306-followup's probe as short (<=5 chars), low-frequency (<=100), pure-`OTHER`
  entries (1,061 candidates) and reviewed each individually with real linguistic judgement - most turned out
  to be genuine German word-forms simply missing a specific POS tag and were correctly left untouched. 348
  confirmed real noise: LaTeX/math markup command names leaked from Wikipedia math rendering, programming/
  Unix keyword leaks, Latin citation-fragment leaks, and other-language function-word/transliteration
  fragments (the last category located programmatically via an alphabet-filter script rather than hand-typed,
  to avoid Unicode transcription errors). Applied via the same fail-loud verification pattern as D-368,
  checking both tag and frequency before deleting each row. `git diff --stat` confirmed exactly 348 lines
  removed. `dictionaries/de/dict.tsv` 119,997 -> 119,649 rows, `version.txt` 12 -> 13, pack rebuilt/verified,
  `LanguagePackCatalog` version 12 -> 13. No new tests (data + comment only); full suite re-run to confirm the
  row removal itself broke nothing. 1058 unit tests unchanged, all green (via JDK 21). `versionCode` 357 ->
  358, `versionName` `"1.0.53"` -> `"1.0.54"`. Not yet device-confirmed. See history §301.

- **§300 (v1.0.53): D-368 round 8 - the entire remaining pool in one sitting; the systematic scan is now**
  **complete.** User asked to finish the rest at once. Reviewed all three remaining bands (20-49: 2,743,
  10-19: 2,017, 5-9: 504 - the scan finds nothing below frequency 5) in one sitting, 5,264 candidates total.
  74 confirmed real (~1.4%, still flat - now confirmed flat across five bands running, settling that this
  noise floor is a permanent grammar property, not something that would have gotten cheaper by waiting).
  210 `NOUN,VERB`/`NOUN,OTHER,VERB` words total now, across all eight D-368 rounds - including several very
  high-frequency everyday imperatives (`Zieh`/`Hau`/`Hör`/`Lach`/`Wasch`/`Renn`/`Spring`/`Steh`/`Trink`/
  `Fahr`) and ten entries that were themselves already lowercase conjugated verb forms mistagged as nouns
  (Wikipedia-extraction artefacts, corrected at zero cost). Two hand-transcription slips (`Steig`, `Hau`)
  caught by the fail-loud verification script before anything was written - fixed by generating the retag
  mapping programmatically from a fresh grep instead of retyping it. `git diff --stat` confirmed exactly 74
  lines changed; `dictionaries/de/version.txt` 11 -> 12, pack rebuilt/verified, `LanguagePackCatalog` version
  11 -> 12. **This closes D-368's systematic scan** - every `NOUN`-tagged entry in the dictionary has now
  been checked against the weak-verb-infinitive hypothesis down to where the mechanical check stops finding
  anything. No new tests (data + comment only). 1058 unit tests unchanged, all green (via JDK 21).
  `versionCode` 356 -> 357, `versionName` `"1.0.52"` -> `"1.0.53"`. Not yet device-confirmed. See history
  §300.

- **§299 (v1.0.52): D-368 round 7 - the whole 50-199 band in one sitting (39 more homographs, 136 total).**
  User asked whether the entire remaining 8,067-candidate pool could be done at once; counted the real
  sub-band sizes first (150-199: 428, 100-149: 776, 50-99: 1,599, 20-49: 2,743, 10-19: 2,017, 5-9: 504) and
  took the user's own fallback - the 50-199 band, 2,803 candidates, reviewed in three sub-passes with one
  retag/rebuild/commit at the end. 39 confirmed real (~1.4%, still the flat noise floor from round 6, not a
  further decline) - mostly ordinary 1st-person-present (`-e`-ending nouns) or informal-imperative (stem-only
  nouns) collisions, several very high-frequency everyday imperatives (`Halt`/`Schlaf`/`Sing`/`Schreib`/
  `Fang`/`Stopp`). Four already-`NOUN,OTHER` entries upgraded to `NOUN,OTHER,VERB`. Verified via the same
  fail-loud Python script pattern; `git diff --stat` confirmed exactly 39 lines changed. `dictionaries/de/
  version.txt` 10 -> 11, pack rebuilt/verified (136 total), `LanguagePackCatalog` version 10 -> 11. Also
  confirmed the JDK-25 `JAVA_HOME` drift from round 6 is environment-only, not a project issue - JDK 21
  remains a full workaround. No new tests (data + comment only). 1058 unit tests unchanged, all green (via
  JDK 21). `versionCode` 355 -> 356, `versionName` `"1.0.51"` -> `"1.0.52"`. Not yet device-confirmed.
  Everything below frequency 50 remains explicitly open (2,743 + 2,017 + 504 = 5,264 candidates left; none
  at frequency ≤4). See history §299.

- **§298 (v1.0.51): D-368 round 6 - the 200-299 band (11 more homographs, 97 total); hit rate confirmed**
  **flat rather than falling.** Continued the manual review one more band per the user's "mach das gerne noch
  weiter": 200-299 (501 candidates), 11 confirmed real (~2.2%) - essentially the same rate as the 300-499
  band's ~1.9%, not a continued decline from the 500-1999 band's ~2%. Confirms the false-positive rate is a
  property of German grammar (the dative-plural/weak-verb-infinitive spelling collision), not of word
  frequency, so the remaining lower bands should be expected to cost about the same per find rather than get
  cheaper. 11 words retagged `NOUN,VERB` (97 total): `Rauch`/`Rate`/`Box`/`Ernte`/`Brauch`/`Heil`/`Fülle`/
  `Leid`/`Decke` (1st-person/imperative), `Mach` ("Mach" the physics unit vs "mach" = colloquial imperative of
  "machen"), and `Drang` (preterite - "der Drang" vs "ich drang" = preterite of "dringen"; was already
  `NOUN,OTHER`, now `NOUN,OTHER,VERB`). `dictionaries/de/version.txt` 9 -> 10, pack rebuilt/verified,
  `LanguagePackCatalog` version 9 -> 10. Environment note: local JDK default drifted to 25.0.1, which Gradle
  8.11.1 cannot run against yet (bare `25.0.1` error, no diagnostics) - worked around with `JAVA_HOME` pointed
  at the still-installed JDK 21 for this build; not fixed at the project level. No new tests (data + comment
  only). 1058 unit tests unchanged, all green (via JDK 21). `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `versionCode` 354 -> 355, `versionName` `"1.0.50"` -> `"1.0.51"`. Not yet device-confirmed. Everything
  below frequency 200 remains explicitly open. See history §298.

- **§297 (v1.0.50): D-368 rounds 4/5 - down through frequency 300 (26 more homographs, 86 total); a**
  **mechanical own-plural filter was tried and failed; the "zero VERB tags" question answered directly.**
  User asked whether the missing `VERB` tags meant we're done once every `NOUN`-also-`VERB` word is found -
  answered no: ordinary verbs with no noun collision (`gehen`/`kommen`/`haben`/`können`, checked directly)
  were never missing as words, just tagged `OTHER` instead of `VERB` - D-368 only tags `VERB` where a
  collision needs resolving, giving *every* verb its own tag regardless of collision is a separate, larger,
  not-yet-started question nothing currently consumes. Also tried to build a mechanical pre-filter (exclude a
  candidate when its own regular plural also exists) to cut review cost before going lower - it failed:
  `Krieg`/`kriegen` (confirmed real) would have been wrongly discarded too, since `Krieg`'s own real plural
  `Kriege` also exists - the two facts are independent, not mechanically distinguishable without a real
  morphological analyser. Continued the same manual review instead, in two more bands (500-1999: 717
  candidates, 16 confirmed; 300-499: 524 candidates, 10 confirmed) - hit rate fell from ~15% to ~2%, but the
  noise rate held steady, confirming the own-plural artefact is frequency-independent. One rebuild/commit for
  both bands together (per the user's own request to bundle several bands per build cycle). Everything below
  frequency 300 remains explicitly open, with an explicit note for a future session to weigh review cost
  against shrinking yield before committing to the remaining ~9,300 mechanical candidates. No new tests
  (data + comment only). 1058 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest`
  green. `versionCode` 353 -> 354, `versionName` `"1.0.49"` -> `"1.0.50"`. Not yet device-confirmed. See
  history §297.

- **§296 (v1.0.49): D-368 round 3 - a real systematic whole-dictionary scan (not a recalled list) found 30**
  **more homographs; the German dictionary's zero pre-existing `VERB` tags recorded as its own finding.**
  User pushed back on §294/§295: those were not samples of a larger search, they were the entire result of
  what this session generated from memory. Ran an actual scan: every `NOUN`-tagged entry checked against a
  hypothesised weak-verb infinitive - 87,985 entries checked, 10,013 mechanical hits, but ~85% were false
  positives (German's regular dative-plural ending is formally identical to the weak-verb infinitive ending,
  so `Jahr` -> `jahren` mechanically looks like a hit but is just `Jahr`'s own plural, not a verb). Also
  confirmed directly: zero `VERB` tags existed anywhere in the whole ~120k-row dictionary before this
  session - no reliable existing signal to cross-check a hypothesis against either, the same missing-tooling
  gap D-306-followup already named. Restricted review to the 204 hits at or above `CorrectionConfidence`'s
  own live `NOUN_REFERENCE_FREQUENCY` (2000, not the removed historical 300 this session had cited from
  memory) and reviewed each individually; 30 confirmed real and retagged `NOUN,VERB` (60 total across all
  three D-368 rounds now) - including a genuinely new subtype this session's earlier memory-based lists never
  surfaced: preterite-form collisions (`Band`/`band` = "bound", `Macht`/`macht` = "does/makes", etc.).
  Everything below frequency 2000 explicitly recorded as still open (see Open TODOs). Pack rebuilt/verified,
  `LanguagePackCatalog` version 7 -> 8 (one in-place correction made to the version comment before committing
  - `Macht`/`macht` is present-tense, not preterite, as first mis-written). No new tests (data + comment
  only). 1058 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 352 -> 353, `versionName` `"1.0.48"` -> `"1.0.49"`. Not yet device-confirmed. See history §296.

- **§295 (v1.0.48): D-368 finished for this round - the full candidate list applied (28 more entries), plus**
  **"lachen"/"Lachen" added on the user's own redirection.** User approved §294's candidate list as
  proposed (`lade` left out, `dank` handled separately) and additionally asked for `lache` - redirected by
  the user themselves to the *real* pair, the nominalised infinitive `lachen`/`Lachen` ("das Lachen"), framed
  explicitly as the first instance of a further, distinct pattern (infinitive-as-noun) to extend later, not
  swept now. Retagged via a small verification script (fails loudly on any missing/duplicate target rather
  than trusting a blind replace) - 16 singular + 10 plural weak-verb/noun homographs `NOUN`/`NOUN,OTHER` ->
  `NOUN,VERB`, `Lachen` `NOUN,OTHER` -> `NOUN,VERB`, `dank` `OTHER` -> `NOUN,OTHER` (the one entry going the
  other direction - noun reading was untagged, not the verb). `git diff --stat` confirmed exactly 28 lines
  changed. `dictionaries/de/version.txt` 6 -> 7, pack rebuilt and verified (unzipped back, version + a count
  of 30 `NOUN,VERB` rows checked), `LanguagePackCatalog` version 6 -> 7. No new tests (data + comment only).
  1058 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green. `versionCode`
  351 -> 352, `versionName` `"1.0.47"` -> `"1.0.48"`. Not yet device-confirmed. See history §295.

- **§294 (v1.0.47): D-368 started - "Weg"/"Stelle"/"Sage" retagged `NOUN,VERB` in the German dictionary;**
  **a verified candidate list compiled for more (not yet applied).** `dictionaries/de/dict.tsv` retagged
  (Stelle's vague `NOUN,OTHER` replaced by the specific `NOUN,VERB`; Weg/Sage `NOUN` -> `NOUN,VERB`);
  confirmed no code change needed (`CapitalisationEngine.isPureNoun`/`isAmbiguousNoun` already routes a
  `NOUN,VERB` entry to §6 rule 5, S-06 chip only). `dictionaries/de/version.txt` 5 -> 6,
  `language-packs/adaptkey-lang-de.zip` rebuilt and verified by unzipping it back, `LanguagePackCatalog`'s
  German entry `version` 5 -> 6. Confirmed mechanically that `dict.tsv` has zero case-only-differing
  duplicate keys - no further homograph pairs are free/mechanical to find, so a candidate list for more was
  instead generated from the same productive pattern (weak-verb 1st-person-singular colliding with a common
  noun, singular and plural) and verified word-by-word against the real file - handed to the user for review
  (see chat), not yet applied. One nuance flagged: the user's fuller ask (both castings actively offered as
  suggestions, not just neither forced) needs its own small mechanism beyond this data fix - the schema
  stores exactly one row per case-insensitive key, confirmed directly. No new tests (data + version/comment
  only). 1058 unit tests unchanged, all green. `:app:assembleRelease`/`:app:testDebugUnitTest` green.
  `versionCode` 350 -> 351, `versionName` `"1.0.46"` -> `"1.0.47"`. Not yet device-confirmed - needs a real
  pack re-import plus typing "stelle"/"sage"/"weg" fresh and confirming no forced capitalisation. See history
  §294.

- **§293 (still v1.0.46, no code change): backlog reconciliation - which open items belong in the D-402**
  **dictionary-cleanup round, and D-404 split into three tiers.** D-352 (the item originally named as a hard
  prerequisite for the cleanup) is shipped and device-confirmed, so that blocker is cleared. D-306-followup/
  D-345/D-330-followup/D-367/D-368(scoped) bundled into one combined cleanup round with D-402 itself - all
  touch the same dictionary file/rebuild cycle; D-368 confirmed to need zero code change (`CapitalisationEngine`'s
  `isPureNoun` already routes a `NOUN,VERB`-tagged entry to the ambiguous-word rule correctly). D-404 (inflected
  forms flooding the dictionary/Learned Words) split into three genuinely different-sized tiers - full
  generative morphology (deferred, its own future design round, uncertain feasibility), a lighter lemma-link
  approach (also deferred), and a minimum bar scoped to collapsing inflected forms in the Learned Words editor
  alone (pure code, no dictionary data, independent of the cleanup round) - with an explicit user constraint
  that the eventual Learned Words fix must migrate, never reset, the existing list. Full reconciled scope now
  recorded in this file's own "Open TODOs" section. No code changed, no version bump. 1058 unit tests
  unchanged. See history §293.

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
