import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    // Generates KSerializer for the @Serializable NavKey routes (HhRoutes.kt). Without it,
    // Kotlin/Native has no serializers and rememberNavBackStack cannot persist the back stack.
    alias(libs.plugins.kotlin.serialization)
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
            // :data is now a full KMP library (android + ios targets), so commonMain depends on it
            // directly. This REVERSES the earlier ":domain-only in commonMain" rule (slice H): the Koin
            // wiring that binds :data impls (supabaseModule/syncModule/authModule/dataModule) now lives
            // in ONE commonMain copy each, instead of being duplicated in :androidApp and KoinIos.kt.
            // The only remaining per-platform DI is the platform module (DB driver, Settings,
            // SupabaseConfig, app version, Google launcher, dispatchers). ViewModel purity (VMs take
            // :domain interfaces, never SQLDelight/Default* types) is now a CONVENTION, no longer
            // enforced by the module boundary. :data api-exposes the Supabase client SDK, inherited here.
            implementation(projects.data)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            // Supabase's KotlinXSerializer config in supabaseModule needs kotlinx-serialization-json
            // directly (declared rather than relied on transitively via :data's api(supabase)).
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // The unified nav host (hh/shared/AppNavHost.kt) lives in commonMain, so the WHOLE nav3
            // stack is shared here. Runtime: androidx.navigation3:navigation3-runtime is Google's
            // MULTIPLATFORM artifact (NavKey/NavBackStack, iosSimulatorArm64/iosArm64 from rc01). UI:
            // the JetBrains Compose Multiplatform port (org.jetbrains.androidx.navigation3:navigation3-ui
            // + lifecycle-viewmodel-navigation3) — both publish android + ios variants and sit on the
            // Google runtime. Android now inherits the JetBrains nav3-UI transitively (Hh.kt's Google
            // nav3-ui was dropped from :androidApp); the spike proved Android compiles + runs on it.
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            // KMP key-value preferences behind AppPreferences (core/preferences). Settings interface
            // in commonMain; SharedPreferencesSettings (androidMain) / NSUserDefaultsSettings (iosMain).
            implementation(libs.multiplatform.settings)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
        }
        // Android-only wiring. Supplies ProcessLifecycleOwner for the resumeEvents() actual
        // (ResumeEvents.android.kt) — the Android foreground-resume signal that feeds the sync
        // orchestrator. iOS uses NSNotificationCenter instead and needs no extra dependency.
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
            // activity-compose: the androidMain PlatformHostActions actual builds the SAF launchers
            // (rememberLauncherForActivityResult + ActivityResultContracts) for backup export/import.
            implementation(libs.androidx.activity.compose)
        }
        // iOS no longer needs an explicit :data dependency — it is inherited from commonMain (the
        // layering reversal above, slice H). The SQLDelight native driver still reaches the Shared
        // framework transitively via :data, and nav3 (runtime + JetBrains CMP UI port) is likewise
        // inherited from commonMain. The iOS-only generated-config source dir is registered in the
        // iosMain { } sourceSets block further below.
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // JVM host tests (no device). Home of the whole-graph Koin resolution test: a missing or
        // wrong binding is invisible to both the Kotlin compiler and assembleDevDebug, so the graph
        // is built off-device against a fake platform module instead. Everything here is JVM-only on
        // purpose and must never leak into commonMain (the iOS compile gate would break).
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            // Dispatchers.setMain — the ViewModels under test create a viewModelScope on construction.
            implementation(libs.kotlinx.coroutines.test)
            // In-memory JDBC SQLite: the fake platform module creates the REAL SQLDelight schema, so
            // the graph binds a real EmmDatabaseData instead of a mock (same driver :data's tests use).
            implementation(libs.sqlite.driver)
            // MapSettings — in-memory Settings backing AppPreferences without SharedPreferences.
            implementation(libs.multiplatform.settings.test)
            // SettingsInitializer + a mock Context: supabase-kt's Auth plugin builds its default
            // SettingsSessionManager through the no-arg Settings(), which on Android gets its
            // Context from androidx.startup — absent in a host test. See AppGraphKoinTest.setUp.
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.mockk)
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

// Register the generated dir on the iOS source set. The TaskProvider (not the bare Directory
// provider) is passed on purpose: srcDir then carries the task dependency through the source set's
// declared outputs, so EVERY consumer of iosMain — Kotlin/Native compiles, detekt, detekt baseline,
// IDE sync — depends on the generator implicitly. Registering the plain directory instead makes
// Gradle 9 fail the build with "uses this output of task ':shared-ui:generateIosSupabaseConfig'
// without declaring an explicit or implicit dependency" for any consumer that lacks its own
// dependsOn (detekt hit exactly this once detektIosMainSourceSet joined the KMP gate).
kotlin {
    sourceSets {
        iosMain {
            kotlin.srcDir(generateIosSupabaseConfig)
        }
    }
}

dependencies {
    // Enables @Preview RENDERING in the IDE (not just compilation). Required per the CMP
    // preview docs; AGP 9.0 + the android.kotlin.multiplatform.library plugin uses the
    // androidRuntimeClasspath configuration (debugImplementation is the AGP 8.x form).
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
