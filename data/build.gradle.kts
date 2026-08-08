import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("app.cash.sqldelight") version "2.3.2"
    kotlin("plugin.serialization") version libs.versions.kotlinVersion
}

kotlin {
    android {
        namespace = "com.emm.data"
        compileSdk = 37
        minSdk = 26
        withHostTest { }
        // Instrumented tests. These were stranded by the KMP migration: the source set moved to
        // androidDeviceTest/ but was never re-enabled, so Gradle silently skipped the schema
        // migration tests — the safety net for real user data on devices since 4e6de6c.
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.coroutines.extensions)
            api(libs.supabase.auth.kt)
            api(libs.supabase.postgrest.kt)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.android.driver)
            api(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            api(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.junit)
            // JVM in-memory SQLite for DB-integration unit tests (JdbcSqliteDriver).
            implementation(libs.sqlite.driver)
        }
        // Instrumented tests run against the REAL AndroidSqliteDriver — that is the whole point for
        // the migration tests, which a JVM driver cannot exercise faithfully.
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.android.driver)
        }
    }
}

dependencies {
    add("commonMainApi", platform(libs.supabase.bom))
}

sqldelight {
    databases {
        create("EmmDatabaseData") {
            packageName.set("com.emm.data")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
