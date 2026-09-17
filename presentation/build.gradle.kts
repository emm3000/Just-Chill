plugins {
    id("justchill.android.library")
}

android {
    // The derived namespace would move this module's R class.
    namespace = "com.emm.presentation"
}

// detektMain and detektTest aggregate detektDebug, detektRelease, detektDebugUnitTest and
// detektDebugAndroidTest. This module has no per-variant and no androidTest source directory, so
// the release analysis reads the same files under an identical baseline and the androidTest one
// reads nothing. Release type checking is the gate's own compileReleaseKotlin.
qualityGate {
    detektTasks.addAll("detektDebug", "detektDebugUnitTest")
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.multiplatform.settings)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.core.viewmodel)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.sqlite.driver)
    testImplementation(libs.multiplatform.settings.test)
    testImplementation(libs.multiplatform.settings.no.arg)
}
