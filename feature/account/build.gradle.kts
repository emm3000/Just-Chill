plugins {
    id("justchill.kmp.feature")
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.material.icons.extended)
        }
    }
}
