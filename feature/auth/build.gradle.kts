plugins {
    id("justchill.kmp.feature")
}

kotlin {
    android {
        androidResources {
            enable = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.material.icons.extended)
        }
    }
}
