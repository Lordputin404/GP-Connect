plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // On the classpath for the app module; applied there only when the real
    // google-services.json is present (Phase 4A runs without it).
    alias(libs.plugins.google.services) apply false
}
