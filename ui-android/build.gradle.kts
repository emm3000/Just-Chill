plugins {
    id("justchill.kmp.library")
    alias(libs.plugins.kotlin.compose)
    // Generates KSerializer for the @Serializable NavKey routes (HhRoutes.kt). Without it,
    // rememberNavBackStack cannot persist the back stack.
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
        // This module ships real Android resources (androidMain/res: the bundled Inter / IBM Plex
        // Mono families and the Google sign-in vector). The KMP android library target does not
        // process resources unless this is turned on, and the failure is a runtime
        // MissingResourceException rather than a build error.
        androidResources { enable = true }
    }

    // Android is the only target since slice S2 (docs/swiftui/PLAN.md): the iOS app consumes
    // :presentation through the JustChillKit framework. Because there is nothing to share, the UI
    // lives in androidMain and depends on Google's Compose artifacts directly instead of the
    // Compose Multiplatform ports — see this module's CLAUDE.md for why that swap was worth making.
    sourceSets {
        androidMain.dependencies {
            // The compose-free presentation layer (MVI core, ViewModels, DI, formatters) — extracted
            // in slice S1 of docs/swiftui/PLAN.md. api: :androidApp reaches appModules/AppGraph
            // through this module.
            api(project(":presentation"))
            implementation(project(":domain"))
            // :data is reached directly so the Koin wiring that binds its impls
            // (supabaseModule/syncModule/authModule/dataModule) exists in one place. ViewModel purity
            // (VMs take :domain interfaces, never SQLDelight/Default* types) is a CONVENTION, not a
            // module boundary. :data api-exposes the Supabase client SDK, inherited here.
            implementation(projects.data)
            // Supabase's KotlinXSerializer config in supabaseModule needs kotlinx-serialization-json
            // directly (declared rather than relied on transitively via :data's api(supabase)).
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // navigation3, runtime AND UI, both from Google. Slice F had put the UI on the JetBrains
            // CMP port so a single commonMain host could drive Android and iOS; ADR 005 sent iOS to
            // SwiftUI, so the port lost its reason to exist.
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            // ProcessLifecycleOwner for the resumeEvents() actual — the foreground-resume signal that
            // feeds the sync orchestrator.
            implementation(libs.androidx.lifecycle.process)
            // activity-compose: the SAF launchers in PlatformHostActions
            // (rememberLauncherForActivityResult + ActivityResultContracts) and AuthScreen's
            // BackHandler.
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // One BOM governs every Compose artifact below; none of them carries its own version.
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.runtime)
            implementation(libs.androidx.foundation)
            implementation(libs.androidx.material3)
            implementation(libs.androidx.material.icons.extended)
            implementation(libs.androidx.ui)
            implementation(libs.androidx.ui.tooling.preview)
        }
        // JVM host tests (no device).
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
        }
    }
}

composeCompiler {
    // :presentation's state classes are external to this compose compilation unit and carry no
    // stability annotations (the module is compose-free by design). This file declares them — and
    // the :domain values they embed — stable, replacing what @Stable/@Immutable did before S1.
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    // Enables @Preview RENDERING in the IDE (not just compilation). AGP 9 + the
    // android.kotlin.multiplatform.library plugin uses the androidRuntimeClasspath configuration
    // (debugImplementation is the AGP 8.x form). The BOM has to be applied here too — this
    // configuration does not inherit the one declared on androidMain.
    androidRuntimeClasspath(platform(libs.androidx.compose.bom))
    androidRuntimeClasspath(libs.androidx.ui.tooling)
}
