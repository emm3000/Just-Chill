plugins {
    id("justchill.android.library")
    alias(libs.plugins.kotlin.serialization)
}

// detektMain and detektTest aggregate detektDebug, detektRelease and detektDebugUnitTest. This
// module has no per-variant source directory and no androidTest, so the release analysis reads the
// same files under an identical baseline. Release type checking is the gate's own compileReleaseKotlin.
qualityGate {
    detektTasks.addAll("detektDebug", "detektDebugUnitTest")
}

dependencies {
    implementation(projects.core.domain)
    api(libs.supabase.auth.kt)
    api(libs.supabase.postgrest.kt)
    api(libs.supabase.storage.kt)
    api(libs.ktor.client.okhttp)
    api(platform(libs.supabase.bom))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    // Declared so the digest backups are verified against is pinned here, not by supabase-kt.
    implementation(libs.okio)

    testImplementation(libs.ktor.client.mock)
}
