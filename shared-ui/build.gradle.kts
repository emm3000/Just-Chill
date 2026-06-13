import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

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
            implementation(libs.kotlinx.datetime)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            // Multiplatform BackHandler (androidx.compose.ui.backhandler) — not pulled in
            // transitively by compose.ui; needed by AuthScreen's CheckEmail back handling.
            implementation("org.jetbrains.compose.ui:ui-backhandler:${libs.versions.composeMultiplatform.get()}")
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.emm.justchill.shared.generated.resources"
}
