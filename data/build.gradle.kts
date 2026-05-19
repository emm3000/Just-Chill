plugins {
    alias(libs.plugins.android.library)
    id("app.cash.sqldelight") version "2.3.2"
    kotlin("plugin.serialization") version libs.versions.kotlinVersion
}

android {
    namespace = "com.emm.data"
    compileSdk = 36

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

    kotlin {
        jvmToolchain(17)
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

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
    testImplementation(libs.sqlite.driver)
}

sqldelight {
    databases {
        create("EmmDatabaseData") {
            packageName.set("com.emm.data")
        }
    }
}