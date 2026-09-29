plugins {
    id("justchill.kmp.library")
    alias(libs.plugins.skie)
}

val exportedModules: List<ProjectDependency> = listOf(
    projects.core.domain,
    projects.core.presentation,
    projects.core.database,
    projects.core.backup,
    projects.feature.account,
    projects.feature.auth,
    projects.feature.category,
    projects.feature.loan,
    projects.feature.profile,
    projects.feature.report,
    projects.feature.transaction,
)

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "JustChillKit"
            isStatic = true
            exportedModules.forEach(::export)
        }
    }

    sourceSets {
        commonMain.dependencies {
            exportedModules.forEach(::api)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.process)
        }
        androidHostTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.sqlite.driver)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.multiplatform.settings.no.arg)
        }
    }
}

skie {
    analytics {
        disableUpload.set(true)
    }
}

tasks.named("qualityGate") {
    dependsOn("linkDebugFrameworkIosSimulatorArm64")
}
