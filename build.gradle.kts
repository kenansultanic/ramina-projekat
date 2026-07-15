// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Added so the :app module can apply KSP. KSP runs Room's annotation processor at build time.
    // We do NOT add the Kotlin Android plugin: AGP 9 compiles Kotlin on its own ("built-in Kotlin").
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false
}
