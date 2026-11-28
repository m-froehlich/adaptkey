import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// D-223: release-signing credentials live in a gitignored keystore.properties (next to the equally
// gitignored local.properties) rather than inline here, so the actual keystore path/passwords never reach
// version control. Absent entirely on a checkout that hasn't set one up - only :app:assembleRelease needs it.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

android {
    namespace = "de.froehlichmedia.adaptkey"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "de.froehlichmedia.adaptkey"
        minSdk = 26
        targetSdk = 35
        // Versioning: only the third digit is bumped per APK (0.8.3 -> 0.8.4 -> ... -> 0.8.10 -> 0.8.11).
        // versionCode just keeps counting up by 1 per release regardless of the versionName - Android
        // requires it to strictly increase for updates to install, and it doesn't need to encode the
        // version number in any particular way.
        // D-283: explicit user request bumped the minor version 0.8.154 -> 0.9.0 here (not a further third-
        // digit step) to mark the D-280/D-281/D-282 multi-language rollout as its own milestone; no code
        // change accompanies this bump.
        // D-315: explicit user request bumped 0.9.26 -> 1.0.0 - a major-version milestone marker, not a
        // further third-digit step, requested right after D-314 (AZERTY) closed out the multi-language
        // rollout's last open layout gap; no code change accompanies this bump either.
        // D-404: explicit user request bumped 1.0.112 -> 1.1.0 - a minor-version milestone marker, not a
        // further third-digit step, requested right after the adjective paradigm project (Tier 1, all three
        // parts of speech) and its non-LLM lemma-linking follow-up closed out D-404 end to end.
        // D-450-followup: explicit user request bumped 1.1.78 -> 1.2.0 - a minor-version milestone marker,
        // not a further third-digit step, requested on the same commit that adds the first Cyrillic-script
        // language pack (Serbian) and the non-Latin-layout generalisation it required.
        // D-508: the gesture/long-press calibration for slow users - the space long-press arm cancels on
        // motion alone (D-380's smear protection deliberately dropped for the space bar), an armed-but-
        // never-moved slow tap commits its space, the swipe travel becomes user-scalable (space/field
        // percent sliders), the D-07 backspace repeat initial delay becomes a setting, and three
        // input-tempo profiles (apply-once over the delay/swipe sliders) ship in the calibration screen
        // and the settings.
        // D-492 paste shape: the editor's composing-span echo is tracked (lastEchoComposingSpanPresent),
        // a span killed at a matching caret is re-marked in place after synchronous caret+content
        // verification (ComposingSpanReMark), and the cosmetic composingPreviewRunnable rewrite is gated
        // on the same helper - the captured paste-into-Chrome duplication ("BesuchBesuch") is closed.
        // D-490: the D-348 double-tap-Backspace revert becomes the fresh-install default (on) - a pure
        // default flip at the four default sites; an explicitly stored Off keeps working, no migration.
        // D-500/C-28: the auto-capitalisation switch (default on) - the off state is a coarse gate on
        // AdaptKey's proactive caps: no live Shift-arming anywhere, and the ordinary commit skips the
        // linguistic force-capitalisation rules (noun-only, LLM lift, acronym restore).
        // D-515: the apostrophe-key switch for the non-English layouts (default off - the English-style
        // QWERTY arrangement always shows the key; QWERTZ/AZERTY keep their canonical middle row unless
        // the user opts in).
        // D-544: the dedicated apostrophe key's character becomes an ordinary word character - never a
        // commit character (it composes into the token, a leading one too), considered by autocorrect
        // like the normal letters, and learnWord/learnWordStrong accept it, so "ist's"/"sieht's" are
        // learned whole; the shared WordExtent.isWordChar definition keeps the composing/reclaim/
        // learning consumers in lockstep.
        // D-545: the German "jetzt" frequency recalibrated for the conversational register (1499 -> 15000,
        // de pack version 51 -> 52) so the bar's own prefixes always surface it first, plus the settings-
        // rebuild providers now carry the layout's own KeyboardProximity again.
        // D-546: the S-11 dual-casing chips take their candidate's own ranked slot while typing
        // (AmbiguousCasingPlacement, in place instead of back-append), and the ranked-list exclusion is
        // removed - a learned casing override ("Jetzt" over "jetzt") can no longer cut the word out of
        // its own suggestion bar.
        // D-547: a freshly learned entry for an already-bundled word is born with the bundled reading(s)
        // alongside its category hint (e.g. "Jetzt" over bundled OTHER: NOUN,OTHER) - the casing override
        // stays, but the entry alone no longer nails the word to noun-only capitalisation, and the
        // Learned Words editor shows the honest union.
        // D-548: pinned ambiguous continuations expand into their casing pair at their own leading slot
        // (superseding v1.2.85's single-casing exemption) - the pinned capital reading no longer sits in
        // front unreplaced while the pair lands later and duplicates it.
        // D-554: when C-09 hides the number row, the §48 swipe-up on the letters surface reveals it
        // together with the extra row; every extra-row close clears the transient reveal with it.
        // D-555: the dedicated bottom-row emoji key right of the combined `!?#` key - setting
        // d555_bottom_row_emoji_key default ON, funded from the L-02 widening (variant A surplus
        // rule), hidden while C-27 puts the emoji mode in the combined key's own slot.
        versionCode = 630
        versionName = "1.3.30"
        // The ONNX Runtime native libs (tier-3 mini-LLM) ship per ABI; keep only the ones real phones
        // use (arm64 + 32-bit arm), dropping the emulator-only x86/x86_64 libs (~43 MB). Device testing
        // of tier 3 therefore needs an arm device, not an x86_64 emulator.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    
    signingConfigs {
        // D-223: only defined when keystore.properties actually exists, so a checkout without a release
        // keystore set up yet still configures cleanly - :app:assembleRelease itself would fail without
        // one, but nothing else (including this whole build script evaluating) depends on it.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                // storeFile is relative to the project root (where keystore.properties itself lives), not
                // this app module directory - a bare file(...) here would resolve against the wrong base.
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    
    // D-481: F-Droid's reproducible-build check needs an APK without Google's encrypted "Dependency metadata"
    // signing block (its scanner rejects it) and without the git revision AGP embeds in META-INF (it differs
    // between a local build and F-Droid's own clone).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    
    buildTypes {
        release {
            // D-482: R8 (shrink + optimise, no renaming - see proguard-rules.pro) enabled at F-Droid's request; was
            // deliberately off since D-223.
            isMinifyEnabled = true
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    
    // D-223: the release APK is meant to actually be installed day-to-day (not just a debug build renamed) -
    // "AdaptKey.apk" instead of AGP's own default "app-release.apk" naming, which still leaked the module's
    // internal Gradle name ("app") into a file the user sees directly. Debug keeps its default
    // "app-debug.apk" name unchanged - it is still occasionally useful and was never the point of confusion.
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "AdaptKey.apk"
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    testOptions {
        unitTests {
            // Robolectric needs the merged Android resources/assets available to JVM unit tests.
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.onnxruntime.android)
    // D-135: androidx.autofill provides the standard UiVersions/InlineSuggestionUi style-bundle helpers for
    // the platform Inline Suggestions API (API 30+); the library itself is a plain compat helper and does
    // not raise minSdk.
    implementation(libs.androidx.autofill)
    
    testImplementation(libs.junit.jupiter)
    // Robolectric runs Android framework code on the JVM (no emulator); its tests are JUnit4, run on the
    // platform via the vintage engine alongside the Jupiter tests.
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}
