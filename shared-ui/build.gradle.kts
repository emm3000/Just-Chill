import java.io.FileInputStream
import java.util.Properties
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
        // Opt-in to Android resource processing for this KMP library target. Without it, the
        // com.android.kotlin.multiplatform.library plugin (AGP 9) generates the Compose Multiplatform
        // Res accessors but never runs CopyResourcesToAndroidAssetsTask, so composeResources
        // (drawables, fonts) are NOT merged into the consuming :androidApp APK assets. The app then
        // crashes at runtime with MissingResourceException on the first painterResource(Res.drawable.*)
        // (the Google sign-in button). iOS packaging is unaffected. See JetBrains CMP-9547 and the
        // KMP "Setup and configuration for multiplatform resources" docs (androidLibrary section).
        androidResources { enable = true }
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
        // Android-only wiring. Supplies ProcessLifecycleOwner for the resumeEvents() actual
        // (ResumeEvents.android.kt) — the Android foreground-resume signal that feeds the sync
        // orchestrator. iOS uses NSNotificationCenter instead and needs no extra dependency.
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
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

// ---------------------------------------------------------------------------
// iOS Supabase config generation (mirrors :androidApp's buildConfigField path).
//
// Android reads root supabase.properties via buildConfigField with ""-fallbacks; iOS has no
// BuildConfig, so we generate a Kotlin constants object (IosSupabaseConfig) from the SAME root
// file into a build/ generated source dir wired into iosMain. Secrets are never hardcoded and the
// generated file lives under build/ (gitignored). When supabase.properties is absent (CI), the
// fields fall back to empty / the localhost placeholder, exactly like Android — the app stays
// usable in anonymous/offline mode. The simulator build uses the ios.* keys, falling back to dev.*.
// ---------------------------------------------------------------------------
val generatedIosConfigDir: Provider<Directory> =
    layout.buildDirectory.dir("generated/iosSupabaseConfig/kotlin")

val generateIosSupabaseConfig by tasks.registering {
    // Capture only configuration-cache-safe values (a Provider<Directory> and a File). The action
    // below must NOT reference any script-level function/property, or the configuration cache rejects
    // it ("cannot serialize Gradle script object references") — that is why the escaping helper is a
    // local fun inside doLast rather than a top-level extension.
    val outputDir = generatedIosConfigDir
    val propsFile = rootProject.file("supabase.properties")
    // Declare inputs/outputs so the task is incremental and re-runs when the properties change.
    inputs.file(propsFile).optional(true)
    outputs.dir(outputDir)
    doLast {
        // Escapes a raw value into a safe Kotlin double-quoted string literal (local to keep the
        // action self-contained / configuration-cache compatible).
        fun String.toKotlinStringLiteral(): String =
            "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\${'$'}") + "\""

        val props = Properties()
        if (propsFile.exists()) {
            FileInputStream(propsFile).use { props.load(it) }
        }
        // ios.* override first, then dev.* (parity with :androidApp's dev flavor). The iOS simulator
        // shares the host network, so localhost == host; dev.supabase.url uses 10.0.2.2 (the
        // Android-emulator host alias) which the simulator cannot resolve. The ios.* override lets
        // both clients target the SAME local stack via their own alias. SUPABASE_URL keeps the empty
        // -> localhost placeholder fallback Android applies in provideSupabaseClient(); the anon key
        // falls back to dev.* (the shared local demo JWT) and stays empty only when both are absent.
        val url = props.getProperty("ios.supabase.url", "")
            .ifBlank { props.getProperty("dev.supabase.url", "") }
            .ifBlank { "http://localhost:54321" }
        val anonKey = props.getProperty("ios.supabase.anonKey", "")
            .ifBlank { props.getProperty("dev.supabase.anonKey", "") }
        val pkgDir = outputDir.get().asFile.resolve("com/emm/justchill")
        pkgDir.mkdirs()
        // String literals are escaped so quotes/backslashes in a key never break compilation.
        pkgDir.resolve("IosSupabaseConfig.kt").writeText(
            """
            |package com.emm.justchill
            |
            |// GENERATED by the generateIosSupabaseConfig Gradle task from root supabase.properties.
            |// DO NOT EDIT and DO NOT COMMIT — this file lives under build/ and is regenerated on build.
            |// iOS equivalent of Android's BuildConfig.SUPABASE_URL / SUPABASE_ANON_KEY.
            |internal object IosSupabaseConfig {
            |    const val SUPABASE_URL: String = ${url.toKotlinStringLiteral()}
            |    const val SUPABASE_ANON_KEY: String = ${anonKey.toKotlinStringLiteral()}
            |}
            |
            """.trimMargin(),
        )
    }
}

// Register the generated dir on the iOS source sets and make the iOS compile depend on the task.
kotlin {
    sourceSets {
        iosMain {
            kotlin.srcDir(generatedIosConfigDir)
        }
    }
}

// Every Kotlin/Native (iOS) compile task must see the generated file before compiling.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn(generateIosSupabaseConfig)
}

dependencies {
    // Enables @Preview RENDERING in the IDE (not just compilation). Required per the CMP
    // preview docs; AGP 9.0 + the android.kotlin.multiplatform.library plugin uses the
    // androidRuntimeClasspath configuration (debugImplementation is the AGP 8.x form).
    androidRuntimeClasspath("org.jetbrains.compose.ui:ui-tooling:${libs.versions.composeMultiplatform.get()}")
}
