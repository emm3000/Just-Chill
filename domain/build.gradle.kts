plugins {
    id("justchill.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "com.emm.domain"
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mockk)
        }
    }
}
