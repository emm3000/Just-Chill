plugins {
    id("justchill.android.library")
    id("justchill.sqldelight")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

// detektMain and detektTest aggregate detektDebug, detektRelease, detektDebugUnitTest and
// detektDebugAndroidTest. This module has no per-variant source directory, so the release analysis
// reads the same files under an identical baseline; androidTest stays, it carries the migration
// suite. Release type checking is the gate's own compileReleaseKotlin.
qualityGate {
    detektTasks.addAll("detektDebug", "detektDebugUnitTest", "detektDebugAndroidTest")
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
