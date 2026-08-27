// Top-level build file. Deliberately thin: everything that used to be configured here through a
// `subprojects { }` block now lives in build-logic as a convention plugin each module applies for
// itself. Cross-project configuration is what blocks Gradle's Project Isolation, and it also made
// this file the place where "how is detekt set up?" secretly lived.
plugins {
    // The one thing the root project is for: `build-logic` is an included build, so nothing in the
    // module graph can reach its `test` task. The gate task registered here names it. See
    // QualityGateConventionPlugin.
    id("justchill.quality.gate")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.google.crashlytics) apply false
    alias(libs.plugins.detekt) apply false
}
