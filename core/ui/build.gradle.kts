plugins {
    id("justchill.android.compose")
}

qualityGate {
    detektTasks.addAll("detektDebug", "detektDebugUnitTest")
}

dependencies {
    api(projects.core.domain)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
}
