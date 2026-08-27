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
    alias(libs.plugins.kotlin.compose)
    // Generates KSerializer for the @Serializable NavKey routes (HhRoutes.kt). Without it,
    // rememberNavBackStack cannot persist the back stack.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.emm.justchill.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// This module depends on Google's Compose artifacts directly instead of the Compose
// Multiplatform ports — see this module's CLAUDE.md for why that swap was worth making.

composeCompiler {
    // :presentation's state classes are external to this compose compilation unit and carry no
    // stability annotations (the module is compose-free by design). This file declares them — and
    // the :domain values they embed — stable, replacing what @Stable/@Immutable did before S1.
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

tasks.named("qualityGate") {
    dependsOn("testDebugUnitTest")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    // The compose-free presentation layer (MVI core, ViewModels, DI, formatters). api: :androidApp
    // reaches appModules/AppGraph through this module.
    api(projects.presentation)
    implementation(projects.domain)
    // :data is reached directly so the Koin wiring that binds its impls (supabaseModule/authModule/
    // dataModule) exists in one place. ViewModel purity (VMs take :domain interfaces, never
    // SQLDelight/Default* types) is a CONVENTION, not a module boundary. :data api-exposes the
    // Supabase client SDK, inherited here.
    implementation(projects.data)
    // Supabase's KotlinXSerializer config in supabaseModule needs kotlinx-serialization-json
    // directly (declared rather than relied on transitively via :data's api(supabase)).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // ProcessLifecycleOwner for the resumeEvents() actual — the foreground-resume signal that
    // feeds the sync orchestrator.
    implementation(libs.androidx.lifecycle.process)
    // activity-compose: the SAF launchers in PlatformHostActions
    // (rememberLauncherForActivityResult + ActivityResultContracts) and AuthScreen's BackHandler.
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    // One BOM governs every Compose artifact below; none of them carries its own version.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)

    // Enables @Preview rendering in the IDE (not just compilation).
    debugImplementation(libs.androidx.ui.tooling)

    // kotlin("test") alone resolves the platform-agnostic artifact, whose `Test` annotation is an
    // unimplemented `expect`. Classic KGP silently substituted the JUnit-backed variant for an
    // Android/JVM module; AGP's built-in Kotlin support (this module) does not, so it is named
    // explicitly — RouteSerializationTest and friends import `kotlin.test.Test` directly.
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
