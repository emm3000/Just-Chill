plugins {
    alias(libs.plugins.android.library)
    // No separate Kotlin Android plugin: AGP 9's built-in Kotlin support refuses
    // org.jetbrains.kotlin.android outright ("no longer required since AGP 9.0" —
    // https://kotl.in/gradle/agp-built-in-kotlin). See androidApp/build.gradle.kts, which is
    // already on this model.
    id("justchill.detekt")
    id("justchill.quality.gate")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.emm.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        // Instrumented tests, on top of testDebugUnitTest the convention plugin sets up. These
        // were stranded by the KMP migration: the source set moved to androidDeviceTest/ but was
        // never re-enabled, so Gradle silently skipped the schema migration tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.named("qualityGate") {
    dependsOn("testDebugUnitTest")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.domain)
    implementation(libs.coroutines.extensions)
    api(libs.supabase.auth.kt)
    api(libs.supabase.postgrest.kt)
    api(libs.supabase.storage.kt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    // SHA-256 for the backup integrity check. Already transitive through supabase-kt/Ktor;
    // declared so the digest the backups are verified against is ours to pin. See the
    // catalog note on the `okio` version.
    implementation(libs.okio)
    // api, not implementation: DatabaseDriver.provideSqlDriver()'s return type and
    // EmmDatabaseData's constructor both expose SqlDriver, and androidApp's Koin module
    // (single { provideSqlDriver(...) }) builds one directly, so it needs the type on its own
    // compile classpath. The KMP android target's project-dependency publishing exposed this
    // transitively even as `implementation`; a plain com.android.library does not.
    api(libs.android.driver)
    api(libs.ktor.client.okhttp)
    api(platform(libs.supabase.bom))

    // kotlin("test") alone resolves the platform-agnostic artifact, whose `Test` annotation is an
    // unimplemented `expect`. Classic KGP silently substituted the JUnit-backed variant for an
    // Android/JVM module; AGP's built-in Kotlin support (this module) does not, so it is named
    // explicitly — Sha256HexTest and friends import `kotlin.test.Test` directly.
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    // JVM in-memory SQLite for DB-integration unit tests (JdbcSqliteDriver).
    testImplementation(libs.sqlite.driver)
    // Ktor MockEngine: lets DefaultAuthRepositorySignOutTest build a REAL SupabaseClient whose
    // transport is scripted. Mocking the Auth plugin instead would only prove what this repository
    // calls, not what the provider does to the session — and the provider's behaviour is the whole
    // finding.
    testImplementation(libs.ktor.client.mock)

    // Instrumented tests run against the REAL AndroidSqliteDriver — that is the whole point for
    // the migration tests, which a JVM driver cannot exercise faithfully.
    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.android.driver)
}

sqldelight {
    databases {
        create("EmmDatabaseData") {
            packageName.set("com.emm.data")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
        }
    }
}
