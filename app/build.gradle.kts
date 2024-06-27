import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "1.9.24"
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.crashlytics)
    id("app.cash.sqldelight") version "2.0.2"
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "com.emm.justchill"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.emm.justchill"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

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
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    val flavorDimension = "tier"
    val appName = "JC"
    flavorDimensions += listOf(flavorDimension)

    productFlavors {

        create("dev") {
            dimension = flavorDimension
            manifestPlaceholders["app_name"] = appName
            manifestPlaceholders["flavor_suffix"] = "-DEV"
            applicationIdSuffix = ".dev"
        }

        create("qa") {
            dimension = flavorDimension
            applicationIdSuffix = ".qa"
            manifestPlaceholders["app_name"] = appName
            manifestPlaceholders["flavor_suffix"] = "-QA"
            signingConfig = signingConfigs["config"]
        }

        create("prod") {
            dimension = flavorDimension
            manifestPlaceholders["app_name"] = "Just Chill"
            manifestPlaceholders["flavor_suffix"] = ""
            signingConfig = signingConfigs["config"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.retrofit)
    implementation(libs.logging.interceptor)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit2.kotlinx.serialization.converter)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    implementation(libs.android.driver)
    implementation(libs.coroutines.extensions)
}

sqldelight {
    databases {
        create("EmmDatabase") {
            packageName.set("com.emm.justchill")
        }
    }
}