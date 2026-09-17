import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

fun marker(plugin: Provider<PluginDependency>): String = plugin.get().run {
    "$pluginId:$pluginId.gradle.plugin:${version.requiredVersion}"
}

dependencies {
    implementation(marker(libs.plugins.android.application))
    implementation(marker(libs.plugins.detekt))
    implementation(marker(libs.plugins.kotlin.compose))
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:${libs.versions.kotlinVersion.get()}")

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.withType<Test>().configureEach {
    systemProperty("justchill.rootDir", rootDir.parentFile.absolutePath)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "justchill.android.application"
            implementationClass = "com.emm.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "justchill.android.library"
            implementationClass = "com.emm.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "justchill.android.compose"
            implementationClass = "com.emm.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "justchill.android.feature"
            implementationClass = "com.emm.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "justchill.jvm.library"
            implementationClass = "com.emm.buildlogic.JvmLibraryConventionPlugin"
        }
        register("detekt") {
            id = "justchill.detekt"
            implementationClass = "com.emm.buildlogic.DetektConventionPlugin"
        }
        register("qualityGate") {
            id = "justchill.quality.gate"
            implementationClass = "com.emm.buildlogic.QualityGateConventionPlugin"
        }
        register("buildInfo") {
            id = "justchill.build.info"
            implementationClass = "com.emm.buildlogic.BuildInfoConventionPlugin"
        }
    }
}
