import java.io.FileInputStream
import java.util.Properties

plugins {
    id("justchill.android.application")
    id("justchill.android.release")
    id("justchill.build.info")
    alias(libs.plugins.kotlin.serialization)
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
        }

        create("prod") {
            dimension = flavorDimension
            manifestPlaceholders["app_name"] = "Just Chill"
            manifestPlaceholders["flavor_suffix"] = ""
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "SUPABASE_URL", "\"${supabaseProperties.getProperty("prod.supabase.url", "")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseProperties.getProperty("prod.supabase.anonKey", "")}\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${supabaseProperties.getProperty("prod.google.webClientId", "")}\"")
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
}

// Dev variants only: the prod flavor adds a signing config the gate has no reason to need.
tasks.named("qualityGate") {
    dependsOn("testDevDebugUnitTest", "lintDevDebug")
}

dependencies {

    implementation(projects.domain)
    implementation(projects.data)
    implementation(projects.uiAndroid)

    implementation(libs.androidx.core.ktx)
    // res/values/themes.xml inherits Theme.Material3.DayNight.NoActionBar from it.
    implementation(libs.material)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)
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

    // CoreModule builds SharedPreferencesSettings, which :presentation does not expose.
    implementation(libs.multiplatform.settings)
}
