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
        versionCode = 231
        versionName = "0.8.109"
        
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
    
    buildTypes {
        release {
            isMinifyEnabled = false
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
