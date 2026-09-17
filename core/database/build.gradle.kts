plugins {
    id("justchill.android.library")
    id("justchill.sqldelight")
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

// detektMain and detektTest aggregate detektDebug, detektRelease, detektDebugUnitTest and
// detektDebugAndroidTest. This module has no per-variant source directory, so the release analysis
// reads the same files under an identical baseline; androidTest stays, it carries the migration
// suite. Release type checking is the gate's own compileReleaseKotlin.
qualityGate {
    detektTasks.addAll("detektDebug", "detektDebugUnitTest", "detektDebugAndroidTest")
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.coroutines.extensions)
    api(libs.supabase.auth.kt)
    api(libs.supabase.postgrest.kt)
    api(libs.supabase.storage.kt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    // Declared so the digest backups are verified against is pinned here, not by supabase-kt.
    implementation(libs.okio)
    api(libs.android.driver)
    api(libs.ktor.client.okhttp)
    api(platform(libs.supabase.bom))

    testImplementation(libs.sqlite.driver)
    testImplementation(libs.ktor.client.mock)

    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.android.driver)
}
