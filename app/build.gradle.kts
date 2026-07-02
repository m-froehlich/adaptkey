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
        versionCode = 1
        versionName = "0.1.0"
        
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
        unitTests.all {
            it.useJUnitPlatform()
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
    testRuntimeOnly(libs.junit.platform.launcher)
}
