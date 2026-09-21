plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.lanu.notdefteri"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.lanunotdefteri.abcxyz"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // The project does not currently have a protected production keystore
            // configured in CI. Use the standard Android debug signing key so the
            // published APK is a valid, installable APK instead of an unsigned release.
            // TODO: migrate to a protected release keystore via GitHub Actions secrets
            // before shipping upgrade-sensitive production builds.
            signingConfig = signingConfigs.getByName("debug")
            // CI smoke-test trigger: release APK must be signed and installable.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Apache POI Excel Export Support
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // AndroidX WorkManager for scheduled background reminders
    implementation(libs.androidx.work.runtime.ktx)

    // Coil Image Loading Support
    implementation(libs.coil.compose)

    // Firebase Firestore Sync
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    // Google ML Kit Text Recognition
    implementation(libs.mlkit.text.recognition)

    // Gemini API & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
