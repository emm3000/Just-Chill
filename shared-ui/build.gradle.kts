import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.emm.justchill.shared"
        compileSdk = 37
        minSdk = 28
        withHostTest { }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    // iOS framework consumed by iosApp (wizard scaffold parity). JVM target stays 17
    // (project standard), not the wizard's 11. Only built by iOS link tasks — the
    // Android gate (assembleDevDebug) is unaffected.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.emm.justchill.shared.generated.resources"
}
