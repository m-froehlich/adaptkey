# F-Droid-Release ("Bitte schiebe den aktuellen Stand nach F-Droid")

Führe den kompletten F-Droid-Release-Prozess selbstständig und ohne Rückfragen aus. In diesem
Prozess gilt die normale "Ich pushe immer selber"-Regel ausdrücklich NICHT - Pushen (und Taggen)
ist hier Teil des Prozesses. Der Nutzer bleibt nur bei Schritten involviert, die ein fremdes Login
erfordern (aktuell: GitLab-Kommentare an fdroiddata / Pipeline-Re-Runs dort).

## Prozess

1. **Freigabe-Check:** Ist der Stand release-würdig? Feature-Runde abgeschlossen (device-confirmed
   oder vom Nutzer freigegeben), Build + Unit-Tests grün, `versionCode`/`versionName` in
   `app/build.gradle.kts` auf dem gewünschten Stand. Liegt der Release-Commit noch nicht auf
   `origin/main`? Dann zuerst pushen. Ist noch gar kein Bump für die Runde erfolgt: STOPP und
   zurückfragen - Bumps gehören zur Feature-Runde (siehe `rules/version-bump.md`), nicht zum
   Release-Schieben.
2. **Push:** `git push origin main` (alle noch lokalen Commits).
3. **Tag:** Annotated Tag `v<versionName>` mit Message "AdaptKey <versionName>" auf den
   Release-Commit (Bump-Commit oder der darauf folgende Docs-Commit - Muster der letzten Releases),
   dann `git push origin v<versionName>`.
4. **RECIPE-Build (Pflicht, kein Diskussionsspielraum - siehe AdaptKey-Progress.md > Release
   Channels und History §533):** Frischen Clone des Repos anlegen (NIE einen Worktree), Tag
   auschecken, `keystore.properties`, `release.keystore` und `local.properties` hineinkopieren,
   `JAVA_HOME` auf das Android-Studio-JBR setzen, dann
   `gradlew :app:assembleRelease --no-build-cache --no-configuration-cache`. Danach die drei
   kopierten Dateien aus dem Clone löschen.
5. **Verifizieren:** `aapt2 dump badging` (versionCode/versionName korrekt), `apksigner verify
   --print-certs` (Cert `3201509b...ee3666e`), Zip-Einträge zählen und auf vcs-/dependency-Blöcke
   prüfen (keine erlaubt, D-481), sha256 notieren. APK sichern unter
   `build/release-v<versionName>/AdaptKey.apk`.
6. **GitHub-Release:** Neu: `gh release create v<versionName> build/release-v<versionName>/AdaptKey.apk
   --title "AdaptKey <versionName>" --verify-tag` (Notes: Kurzsummary der Runde). Existiert das
   Release schon (Reparaturfall wie Issue #14): `gh release upload v<versionName>
   build/release-v<versionName>/AdaptKey.apk --clobber`. Der Asset-Name muss EXAKT `AdaptKey.apk`
   sein.
7. **Verifizieren:** Asset über die Release-URL neu herunterladen und sha256 mit dem lokalen Build
   abgleichen - erst dann gilt "nach F-Droid geschoben".
8. **Bericht an den Nutzer:** Release-URL, sha256, Cert-Status; Hinweis, dass F-Droid den Tag nun
   automatisch zieht (`AutoUpdateMode: Version`, `UpdateCheckMode: Tags`) und in einem Nachtzyklus
   baut + gegen das Asset vergleicht; Monitor-Log-URL für den Build:
   `https://monitor.f-droid.org/builds/log/de.froehlichmedia.adaptkey/<versionCode>`.

## Wann GitLab nötig ist

Normalerweise NIE - nach dem Merge von MR !44142 (2026-09-26) erzeugt der F-Droid-Bot
(`checkupdates`) den Build-Eintrag für neue Tags selbst als MR und maintainer mergen ihn. GitLab ist
nur dann ein Thema, wenn (a) die Bot-MR-Pipeline fehlschlägt (dann: Asset prüfen/ersetzen und den
Nutzer bitten, im MR zu kommentieren), oder (b) linsui ein Issue aufmacht. Kommunikation auf
GitHub-Seite (Issues am eigenen Repo) übernimmt der Assistent selbst mit `gh`.
