plugins {
    id("justchill.android.library")
    id("justchill.sqldelight")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.coroutines.extensions)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    api(libs.android.driver)

    testImplementation(libs.sqlite.driver)

    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.android.driver)
}
