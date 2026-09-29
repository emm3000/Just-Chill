import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation

plugins {
    id("justchill.kmp.feature")
}

kotlin {
    android {
        compilations.withType<KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.material.icons.extended)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.ui.test.junit4)
            implementation(libs.androidx.ui.test.manifest)
            implementation(libs.robolectric)
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}
