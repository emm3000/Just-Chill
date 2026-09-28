plugins {
    id("justchill.android.library")
}

dependencies {
    api(projects.core.domain)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)

    testImplementation(projects.core.testing)
}
