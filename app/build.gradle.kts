plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        versionCode = 145
        versionName = "0.8.23"
        
        // The ONNX Runtime native libs (tier-3 mini-LLM) ship per ABI; keep only the ones real phones
        // use (arm64 + 32-bit arm), dropping the emulator-only x86/x86_64 libs (~43 MB). Device testing
        // of tier 3 therefore needs an arm device, not an x86_64 emulator.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    
    testImplementation(libs.junit.jupiter)
    // Robolectric runs Android framework code on the JVM (no emulator); its tests are JUnit4, run on the
    // platform via the vintage engine alongside the Jupiter tests.
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}
