import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    // Generates KSerializer for @Serializable NavKey routes (IosRoutes.kt). Without it, Kotlin/Native
    // has no serializers and rememberNavBackStack cannot persist the back stack. Mirrors :androidApp.
    kotlin("plugin.serialization") version libs.versions.kotlinVersion
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    androidLibrary {
        namespace = "com.emm.justchill.shared"
        compileSdk = 37
        minSdk = 28
        withHostTest { }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    // iOS framework consumed by iosApp (wizard scaffold parity). JVM target stays 17
    // (project standard), not the wizard's 11. Only built by iOS link tasks — the
    // Android gate (assembleDevDebug) is unaffected.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            // Multiplatform BackHandler (androidx.compose.ui.backhandler) — not pulled in
            // transitively by compose.ui; needed by AuthScreen's CheckEmail back handling.
            implementation("org.jetbrains.compose.ui:ui-backhandler:${libs.versions.composeMultiplatform.get()}")
            implementation(libs.compose.components.resources)
            // CMP 1.10+ multiplatform @Preview lives in org.jetbrains.compose.ui:ui-tooling-preview
            // (provides androidx.compose.ui.tooling.preview.Preview); the old
            // compose.components.uiToolingPreview shipped the now-deprecated jetbrains namespace.
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:${libs.versions.composeMultiplatform.get()}")
        }
        // iOS-only wiring (Phase 5a). commonMain stays :domain-only per the established
        // split rule; depending on :data is allowed HERE because the iOS Koin module
        // binds the :data repository/datasource impls (provideSqlDriver/provideDb live in
        // :data iosMain). :data is transitively exposed to the Shared framework, and it
        // also brings the SQLDelight native driver onto the iOS classpath.
        iosMain.dependencies {
            implementation(projects.data)
            // iOS nav host (5b): JetBrains Compose Multiplatform navigation3 port. Android keeps
            // its stable androidx.navigation3:* (Option A) — this KMP port is iosMain-only so it
            // never touches the Android nav host (Hh.kt) or commonMain (which stays :domain-only).
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.emm.justchill.shared.generated.resources"
}

dependencies {
    // Enables @Preview RENDERING in the IDE (not just compilation). Required per the CMP
    // preview docs; AGP 9.0 + the android.kotlin.multiplatform.library plugin uses the
    // androidRuntimeClasspath configuration (debugImplementation is the AGP 8.x form).
    androidRuntimeClasspath("org.jetbrains.compose.ui:ui-tooling:${libs.versions.composeMultiplatform.get()}")
}
