plugins {
    id("justchill.kmp.library")
}

// The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect,
// the Koin DI modules and the shared formatters/strings. :shared-ui (Compose) sits on top of it;
// the iOS SwiftUI app will consume it through the exported framework (slice S2 of
// docs/swiftui/PLAN.md). NO compose dependency may ever appear here — that is the whole point
// of the module.
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    android {
        namespace = "com.emm.presentation"
        minSdk = 28
    }

    sourceSets {
        commonMain.dependencies {
            // api: the ViewModels' public signatures expose domain types (use cases, entities).
            api(project(":domain"))
            // The DI modules bind :data implementations (Default* repositories, SQLDelight wiring)
            // to :domain interfaces — same layering as shared-ui had since slice H. ViewModel
            // purity (VMs take :domain interfaces only) stays a convention, reviewed not enforced.
            implementation(projects.data)
            // Multiplatform ViewModel + viewModelScope WITHOUT the compose runtime (shared-ui uses
            // the -compose variant of the same artifact).
            implementation(libs.jetbrains.lifecycle.viewmodel)
            // Supabase's KotlinXSerializer config in supabaseModule needs kotlinx-serialization-json
            // directly (declared rather than relied on transitively via :data's api(supabase)).
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // KMP key-value preferences behind AppPreferences (core/preferences).
            implementation(libs.multiplatform.settings)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            // viewModelOf DSL for the DI modules — the compose-free base of koin-compose-viewmodel.
            implementation(libs.koin.core.viewmodel)
        }
        // ProcessLifecycleOwner for the resumeEvents() Android actual (core/sync).
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // JVM host tests. Home of AppGraphKoinTest: the whole Koin graph resolved off-device
        // against a fake platform module — the only net that catches a missing binding before a
        // user does. Everything here is JVM-only and must never leak into commonMain.
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.sqlite.driver)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.mockk)
        }
    }
}
