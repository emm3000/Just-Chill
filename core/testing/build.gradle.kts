plugins {
    id("justchill.jvm.library")
}

dependencies {
    api(projects.core.domain)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
}
