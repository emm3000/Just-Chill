plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin)
    kotlin("plugin.serialization") version libs.versions.kotlinVersion
    id("app.cash.sqldelight") version "2.0.2"
}

android {
    namespace = "com.emm.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
}

dependencies {

    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.android.driver)
    api(libs.coroutines.extensions)

    implementation(libs.kotlinx.serialization.json)

}

sqldelight {
    databases {
        create("EmmDatabaseData") {
            packageName.set("com.emm.data")
        }
    }
}