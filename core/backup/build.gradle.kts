plugins {
    id("justchill.kmp.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.supabase.auth.kt)
            api(libs.supabase.postgrest.kt)
            api(libs.supabase.storage.kt)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // Declared so the digest backups are verified against is pinned here, not by supabase-kt.
            implementation(libs.okio)
        }
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            api(libs.ktor.client.darwin)
        }
        androidHostTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
