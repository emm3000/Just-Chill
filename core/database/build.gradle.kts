import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    id("justchill.kmp.library")
    id("justchill.sqldelight")
}

sqlDelightSnapshots {
    floor.set(3)
}

// The simulator cannot read a checkout under a TCC-protected folder such as ~/Documents.
val stagedSnapshots: File = File(
    System.getProperty("java.io.tmpdir"),
    "justchill-sqldelight-snapshots-${layout.projectDirectory.asFile.absolutePath.hashCode()}",
)

val stageSqlDelightSnapshots: TaskProvider<Sync> = tasks.register<Sync>("stageSqlDelightSnapshots") {
    from(layout.projectDirectory.dir("src/commonMain/sqldelight/databases"))
    into(stagedSnapshots)
}

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    inputs.files(stageSqlDelightSnapshots).withPathSensitivity(PathSensitivity.RELATIVE)
    environment("SIMCTL_CHILD_SQLDELIGHT_SNAPSHOTS", stagedSnapshots.absolutePath, tracked = false)
}

kotlin {
    android {
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.coroutines.extensions)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            api(libs.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        androidHostTest.dependencies {
            implementation(libs.sqlite.driver)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.junit)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.android.driver)
        }
    }
}
