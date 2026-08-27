plugins {
    alias(libs.plugins.android.library)
    // No separate Kotlin Android plugin: AGP 9's built-in Kotlin support refuses
    // org.jetbrains.kotlin.android outright ("no longer required since AGP 9.0" —
    // https://kotl.in/gradle/agp-built-in-kotlin). See androidApp/build.gradle.kts, which is
    // already on this model.
    // The KMP modules used to get detekt/qualityGate via justchill.kmp.library; this module
    // applies them directly now.
    id("justchill.detekt")
    id("justchill.quality.gate")
}

// The compose-free presentation layer: MVI core, every feature's ViewModel/UiState/Intent/Effect,
// the Koin DI modules and the shared formatters/strings. :ui-android (Compose) sits on top of it.
// NO compose dependency may ever appear here — that is the whole point of the module.
android {
    namespace = "com.emm.presentation"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
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
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    // api: the ViewModels' public signatures expose domain types (use cases, entities).
    api(projects.domain)
    // The DI modules bind :data implementations (Default* repositories, SQLDelight wiring)
    // to :domain interfaces — same layering as ui-android had since slice H. ViewModel
    // purity (VMs take :domain interfaces only) stays a convention, reviewed not enforced.
    implementation(projects.data)
    // ViewModel + viewModelScope. androidx directly now — :presentation no longer needs the
    // JetBrains multiplatform port (jetbrains-lifecycle-viewmodel), which compiled for iOS too.
    implementation(libs.androidx.lifecycle.viewmodel)
    // Supabase's KotlinXSerializer config in supabaseModule needs kotlinx-serialization-json
    // directly (declared rather than relied on transitively via :data's api(supabase)).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    // KMP key-value preferences behind AppPreferences (core/preferences).
    implementation(libs.multiplatform.settings)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    // viewModelOf DSL for the DI modules — the compose-free base of koin-compose-viewmodel.
    implementation(libs.koin.core.viewmodel)
    // ProcessLifecycleOwner for the resumeEvents() actual (core/lifecycle).
    implementation(libs.androidx.lifecycle.process)

    // kotlin("test") alone resolves the platform-agnostic artifact, whose `Test` annotation is an
    // unimplemented `expect`. Classic KGP silently substituted the JUnit-backed variant for an
    // Android/JVM module; AGP's built-in Kotlin support (this module) does not, so it is named
    // explicitly — the former commonTest suites import `kotlin.test.Test` directly.
    testImplementation(kotlin("test-junit"))
    // Home of AppGraphKoinTest: the whole Koin graph resolved off-device against a fake platform
    // module — the only net that catches a missing binding before a user does. Everything here is
    // JVM-only.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.driver)
    testImplementation(libs.multiplatform.settings.test)
    testImplementation(libs.multiplatform.settings.no.arg)
    testImplementation(libs.mockk)
}
