plugins {
    id("justchill.android.feature")
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    implementation(libs.androidx.material.icons.extended)
}
