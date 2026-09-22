import java.io.FileInputStream
import java.util.Properties

plugins {
    id("justchill.android.application")
    id("justchill.android.release")
    id("justchill.build.info")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.google.services)
}

val supabasePropertiesFile = rootProject.file("supabase.properties")
val supabaseProperties = Properties()
if (supabasePropertiesFile.exists()) {
    supabaseProperties.load(FileInputStream(supabasePropertiesFile))
}

android {
    namespace = "com.emm.justchill"

    defaultConfig {
        applicationId = "com.emm.justchill"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            buildConfigField("boolean", "SNAPSHOT_BACKUP_ENABLED", "true")
        }

        create("prod") {
            dimension = flavorDimension
            manifestPlaceholders["app_name"] = "Just Chill"
            manifestPlaceholders["flavor_suffix"] = ""
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseProperties.getProperty("prod.supabase.url", "")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseProperties.getProperty("prod.supabase.anonKey", "")}\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${supabaseProperties.getProperty("prod.google.webClientId", "")}\"")
            buildConfigField("boolean", "SNAPSHOT_BACKUP_ENABLED", "false")
        }
    }

    buildFeatures {
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
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

// Android lint is deliberately absent; .github/workflows/uploadApk.yml runs it on every trunk push.
// The gate's own `compileReleaseKotlin` never matches here: this module's flavors name the task
// `compileProdReleaseKotlin`, and `assembleProdRelease` is not on the gate.
tasks.named("qualityGate") {
    dependsOn("testDevDebugUnitTest", "compileProdReleaseKotlin")
}

dependencies {

    implementation(projects.core.domain)
    implementation(projects.core.backup)
    implementation(projects.core.database)
    implementation(projects.core.ui)

    implementation(projects.feature.transaction)
    implementation(projects.feature.account)
    implementation(projects.feature.category)
    implementation(projects.feature.recurring)
    implementation(projects.feature.report)
    implementation(projects.feature.loan)
    implementation(projects.feature.profile)
    implementation(projects.feature.auth)
    implementation(projects.feature.onboarding)

    implementation(libs.androidx.core.ktx)
    // res/values/themes.xml inherits Theme.Material3.DayNight.NoActionBar from it.
    implementation(libs.material)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // ProcessLifecycleOwner, which core/lifecycle/ turns into the backup orchestrator's edges.
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose)
    implementation(libs.androidx.material.icons.extended)

    // Only the dev experiences playground uses these. Quoted: AGP generates no typed accessor
    // for flavor configurations.
    "devImplementation"(libs.androidx.navigation.compose)
    "devImplementation"(libs.androidx.appcompat)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    // AppPreferences and DefaultBackupMetadataStore sit on Settings; AndroidPlatformModule
    // builds the SharedPreferencesSettings they get.
    implementation(libs.multiplatform.settings)

    // The Snapshot's end-to-end tests live here: only the app sees both :core:backup, which writes
    // the file, and :core:database, which holds the rows it restores into.
    testImplementation(projects.core.testing)
    testImplementation(libs.sqlite.driver)

    // AppGraphKoinTest's JVM stand-in for the platform module: in-memory Settings plus the
    // androidx.startup hook supabaseModule's install(Auth) needs.
    testImplementation(libs.multiplatform.settings.test)
    testImplementation(libs.multiplatform.settings.no.arg)
}
