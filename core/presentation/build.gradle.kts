plugins {
    id("justchill.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.jetbrains.lifecycle.viewmodel)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
        androidHostTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
