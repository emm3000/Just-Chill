plugins {
    alias(libs.plugins.kotlin.jvm)
    id("justchill.detekt")
    id("justchill.quality.gate")
}

kotlin {
    jvmToolchain(17)
}

// `test` is in none of QualityGateConventionPlugin's task-name sets, so without this line
// :domain's tests would silently leave the gate.
tasks.named("qualityGate") {
    dependsOn("test")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
