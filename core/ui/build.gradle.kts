plugins {
    id("justchill.android.compose")
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

qualityGate {
    detektTasks.addAll("detektDebug", "detektDebugUnitTest")
}

dependencies {
    api(projects.core.domain)
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.androidx.material.icons.extended)
}
