plugins {
    id("justchill.android.library")
    alias(libs.plugins.kotlin.serialization)
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
