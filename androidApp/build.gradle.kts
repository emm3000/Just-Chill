import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // The KMP modules get these via justchill.kmp.library; this module applies them directly.
    id("justchill.detekt")
    id("justchill.quality.gate")
    // Generates BuildInfo.kt (the commit HEAD points at), once per variant, into a directory AGP
    // owns and adds to that variant's Kotlin sources. There is no `main` source set involved.
    id("justchill.build.info")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.crashlytics)
    alias(libs.plugins.kotlin.compose)
}

// Signing credentials are optional AT CONFIGURE TIME, and that is the whole point. They are absent
// on a fresh clone, on forks, and on Dependabot PRs — GitHub does not expose repo secrets to
// Dependabot-triggered runs, so the setup action produced an empty file and every lookup below
// returned null. Reading them unconditionally aborted configuration of :androidApp, and Gradle
// configures this module for ANY task in the build: the iOS compile job, which signs nothing and
// does not even build Android, died on this line.
//
// Signing proves who published, not that the code works, so no validation job needs it. The
// assertion that a release must be signed lives in uploadRelease.yml, which checks the secrets
// before it builds — the one place where a missing key has to be fatal.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
val hasReleaseSigning = keystoreProperties.getProperty("keyAlias") != null

val supabasePropertiesFile = rootProject.file("supabase.properties")
val supabaseProperties = Properties()
if (supabasePropertiesFile.exists()) {
    supabaseProperties.load(FileInputStream(supabasePropertiesFile))
}

fun gitCommitCount(): Int = runCatching {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

// --match is not optional: the repo carries non-release tags (pre-kmp, post-s5, pre-redesign)
// and a bare `describe` returns whichever one is nearest, so builds shipped versionName "pre-kmp".
fun gitLatestTag(): String = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
    }.standardOutput.asText.get().trim().removePrefix("v")
}.getOrDefault("0.0.0-dev")

android {
    namespace = "com.emm.justchill"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emm.justchill"
        minSdk = 28
        targetSdk = 36
        versionCode = gitCommitCount()
        versionName = gitLatestTag()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Created only when the credentials are actually present. Without them the prod release
        // builds UNSIGNED rather than failing to configure, which is what lets a validation job
        // exercise R8, resource shrinking and manifest merging without holding the release key.
        if (hasReleaseSigning) {
            create("config") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    val flavorDimension = "tier"
    val appName = "EMM"
    flavorDimensions += listOf(flavorDimension)

    productFlavors {

        create("dev") {
            dimension = flavorDimension
            manifestPlaceholders["app_name"] = appName
            manifestPlaceholders["flavor_suffix"] = "-DEV"
            applicationIdSuffix = ".dev"
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseProperties.getProperty("dev.supabase.url", "")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseProperties.getProperty("dev.supabase.anonKey", "")}\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${supabaseProperties.getProperty("dev.google.webClientId", "")}\"")
        }

        create("prod") {
            dimension = flavorDimension
            manifestPlaceholders["app_name"] = "Just Chill"
            manifestPlaceholders["flavor_suffix"] = ""
            // Indexing signingConfigs for a config that was never created throws, so this follows
            // the same condition. An unsigned prod artifact never reaches anyone: uploadRelease.yml
            // refuses to build without the credentials, and Play rejects unsigned uploads anyway.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs["config"]
            }
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseProperties.getProperty("prod.supabase.url", "")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseProperties.getProperty("prod.supabase.anonKey", "")}\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${supabaseProperties.getProperty("prod.google.webClientId", "")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_reports")
}

// This module's contribution to `./gradlew qualityGate` (detekt is wired in by the plugin itself).
// Only the dev-debug unit tests and the dev lint variant: the prod variants run the same code
// through a signing config the gate has no reason to need.
tasks.named("qualityGate") {
    dependsOn("testDevDebugUnitTest", "lintDevDebug")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {

    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.uiAndroid)

    implementation(libs.androidx.core.ktx)
    // Material Components is NOT dead code: res/values/themes.xml inherits from
    // Theme.Material3.DayNight.NoActionBar, so removing it breaks the manifest theme.
    implementation(libs.material)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // @Preview rendering in the IDE. The androidTest* deps that used to sit here were removed:
    // androidApp/src/androidTest/ does not exist, so nothing ever consumed them.
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.material.icons.extended)

    // Nav2 + AppCompat are used ONLY by the `dev` experiences playground
    // (src/dev/.../experiences/), never by the product. Scoped to the flavor so a prod build
    // does not carry them. coil-compose used to be declared here and had zero usages — removed.
    // Quoted form on purpose: AGP does not generate a typed `devImplementation` DSL accessor for
    // flavor configurations, so `devImplementation(...)` fails to compile the build script.
    "devImplementation"(libs.androidx.navigation.compose)
    "devImplementation"(libs.androidx.appcompat)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.activity.ktx)

    testImplementation(libs.mockk)

    // nav3 (runtime + UI) is inherited transitively from :ui-android commonMain, which now hosts the
    // unified AppNavHost. :androidApp no longer references androidx.navigation3 types directly.

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // multiplatform-settings: CoreModule builds the SharedPreferencesSettings that backs the
    // ui-android AppPreferences. ui-android consumes it as `implementation`, so it is not exposed
    // transitively — this module references Settings/SharedPreferencesSettings directly.
    implementation(libs.multiplatform.settings)
}