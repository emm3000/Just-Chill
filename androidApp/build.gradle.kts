import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // The KMP modules get these via justchill.kmp.library; this module applies them directly.
    id("justchill.detekt")
    id("justchill.quality.gate")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.crashlytics)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

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

fun gitLatestTag(): String = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
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
        create("config") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
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
            signingConfig = signingConfigs["config"]
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

    lint {
        // Pin known false positives so the rule keeps catching real bugs.
        // To refresh: ./gradlew :androidApp:updateLintBaselineProdRelease
        baseline = file("lint-baseline.xml")
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
    implementation(projects.sharedUi)

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

    // nav3 (runtime + UI) is inherited transitively from :shared-ui commonMain, which now hosts the
    // unified AppNavHost. :androidApp no longer references androidx.navigation3 types directly.

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // multiplatform-settings: CoreModule builds the SharedPreferencesSettings that backs the
    // shared-ui AppPreferences. shared-ui consumes it as `implementation`, so it is not exposed
    // transitively — this module references Settings/SharedPreferencesSettings directly.
    implementation(libs.multiplatform.settings)
}