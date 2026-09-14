plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Firebase foundation (Phase 4A): the Google Services plugin processes the
// REAL google-services.json placed in app/. Until that file is supplied, the
// plugin stays off so the build remains identical to the mock-only app — no
// fabricated configuration is used.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Release signing credentials come from Gradle properties or environment
// variables (set by CI from GitHub secrets) — never hardcoded in source.
// If none are provided, the release signing config is simply absent and
// assembleRelease/assembleDebug behave exactly as before (unsigned release,
// normally signed debug).
val releaseStoreFile = providers.gradleProperty("gpConnectStoreFile")
    .orElse(providers.environmentVariable("GP_CONNECT_STORE_FILE"))
    .orNull
val releaseStorePassword = providers.gradleProperty("gpConnectStorePassword")
    .orElse(providers.environmentVariable("GP_CONNECT_STORE_PASSWORD"))
    .orNull
val releaseKeyAlias = providers.gradleProperty("gpConnectKeyAlias")
    .orElse(providers.environmentVariable("GP_CONNECT_KEY_ALIAS"))
    .orNull
val releaseKeyPassword = providers.gradleProperty("gpConnectKeyPassword")
    .orElse(providers.environmentVariable("GP_CONNECT_KEY_PASSWORD"))
    .orNull

android {
    namespace = "com.gumlapolytechnic.gpconnect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gumlapolytechnic.gpconnect"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (releaseStoreFile != null && releaseStorePassword != null &&
            releaseKeyAlias != null && releaseKeyPassword != null
        ) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Firebase foundation (Phase 4A) — BoM-aligned. The libraries compile and
    // package fine without google-services.json; nothing calls them until the
    // Firebase repositories arrive in Phase 4B/4C (mock data remains active).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    // Firebase Cloud Messaging client (push notifications). BoM-aligned like
    // the other Firebase modules; delivery is passive — no token storage or
    // Cloud Functions yet.
    implementation(libs.firebase.messaging)

    debugImplementation(libs.androidx.ui.tooling)
}
