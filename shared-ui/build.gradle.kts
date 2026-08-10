plugins {
    id("justchill.kmp.library")
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

    android {
        namespace = "com.emm.justchill.shared"
        minSdk = 28
        // Opt-in to Android resource processing for this KMP library target. Without it, the
        // com.android.kotlin.multiplatform.library plugin (AGP 9) generates the Compose Multiplatform
        // Res accessors but never runs CopyResourcesToAndroidAssetsTask, so composeResources
        // (drawables, fonts) are NOT merged into the consuming :androidApp APK assets. The app then
        // crashes at runtime with MissingResourceException on the first painterResource(Res.drawable.*)
        // (the Google sign-in button). iOS packaging is unaffected. See JetBrains CMP-9547 and the
        // KMP "Setup and configuration for multiplatform resources" docs (androidLibrary section).
        androidResources { enable = true }
    }

    // Android-only since slice S2 (docs/swiftui/PLAN.md): the iOS app consumes :presentation
    // through the JustChillKit framework, so this module dropped its iOS targets — see
    // gradle.properties (justchill.kmp.ios=false) and docs/adr/005.

    sourceSets {
        commonMain.dependencies {
            // The compose-free presentation layer (MVI core, ViewModels, DI, formatters) — extracted
            // in slice S1 of docs/swiftui/PLAN.md. api: :androidApp reaches appModules/AppGraph and
            // the iosMain KoinIos wiring reaches SupabaseConfig through this module.
            api(project(":presentation"))
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

composeCompiler {
    // :presentation's state classes are external to this compose compilation unit and carry no
    // stability annotations (the module is compose-free by design). This file declares them — and
    // the :domain values they embed — stable, replacing what @Stable/@Immutable did before S1.
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    // Enables @Preview RENDERING in the IDE (not just compilation). Required per the CMP
    // preview docs; AGP 9.0 + the android.kotlin.multiplatform.library plugin uses the
    // androidRuntimeClasspath configuration (debugImplementation is the AGP 8.x form).
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
