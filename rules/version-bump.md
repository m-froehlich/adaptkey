# Version Bumps
- One bump per completed feature (versionCode + versionName), as its own concluding commit -
  never per commit, never for docs-only commits.
- Section commits within a feature carry no bump.
- Bump only when the feature is done (build + tests green).
- Bump BEFORE the build!
- **Version scheme (user decision, 2026-11-06): future versions count from "1.3.3" onward - the
  minor was raised (1.2 -> 1.3) and the patch starts already at 3. So: 1.3.3 (versionCode 603),
  then 1.3.4, 1.3.5, ... until the user says otherwise. versionCode keeps counting up by 1.**
- Releasing for F-Droid needs no extra version bump (the tag carries the version). But the release
  APK itself MUST come from the RELEASE RECIPE (AdaptKey-Progress.md > Release Channels): fresh
  clone at the tag, --no-build-cache --no-configuration-cache, keystore copied in and removed
  afterwards. A day-to-day/test build of the main checkout is NEVER uploadable as a release asset -
  cached builds are not byte-identical and fail F-Droid's reproducible check (happened for real with
  the v1.2.65 and v1.2.93 releases; the latter is issue #14).
