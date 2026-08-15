plugins {
    id("justchill.kmp.library")
    // Generates IosSupabaseConfig into iosMain from root supabase.properties. iOS has no
    // BuildConfig, so this is the counterpart of :androidApp's buildConfigField block. Moved here
    // from :ui-android in slice S2: the config's only consumer is KoinIos.kt, which lives here now.
    id("justchill.ios.supabase.config")
    // SKIE rewrites the framework's Swift interface: sealed -> Swift enums (exhaustive
    // onEnum(of:)), Flow/StateFlow -> AsyncSequence, suspend -> async. Without it the MVI surface
    // (sealed intents/effects, StateFlow state) is unusable from Swift.
    alias(libs.plugins.skie)
}

// The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect,
// the Koin DI modules and the shared formatters/strings. :ui-android (Compose) sits on top of it;
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

    // The iOS app consumes this module as the JustChillKit framework (slice S2). :domain and :data
    // are export()ed so their public types (use cases, entities, repository interfaces) appear in
    // the framework's Swift interface instead of as opaque forward declarations. Consumed by
    // iosApp/; built by the iOS link tasks only — the Android gate is unaffected.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "JustChillKit"
            isStatic = true
            export(project(":domain"))
            export(project(":data"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api: the ViewModels' public signatures expose domain types (use cases, entities).
            api(project(":domain"))
            // The DI modules bind :data implementations (Default* repositories, SQLDelight wiring)
            // to :domain interfaces — same layering as ui-android had since slice H. ViewModel
            // purity (VMs take :domain interfaces only) stays a convention, reviewed not enforced.
            // api (not implementation) because the framework block export()s :data — export
            // requires the exported project on the api configuration.
            api(projects.data)
            // Multiplatform ViewModel + viewModelScope WITHOUT the compose runtime (ui-android uses
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
        // ProcessLifecycleOwner for the resumeEvents() Android actual (core/lifecycle).
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

skie {
    analytics {
        enabled.set(false)
    }
}
