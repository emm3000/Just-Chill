// Top-level build file. Deliberately thin: everything that used to be configured here through a
// `subprojects { }` block now lives in build-logic as a convention plugin each module applies for
// itself. Cross-project configuration is what blocks Gradle's Project Isolation, and it also made
// this file the place where "how is detekt set up?" secretly lived.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.google.crashlytics) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.detekt) apply false
}
