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
    // Brings AGP's application-components API onto build-logic's own compile classpath —
    // BuildInfoConventionPlugin.kt imports ApplicationAndroidComponentsExtension from it.
    implementation(marker(libs.plugins.android.application))
    implementation(marker(libs.plugins.detekt))
    // Pins the shared kotlin-gradle-plugin jar (the one behind the unversioned kotlin-jvm alias
    // :domain applies) to the project's actual Kotlin version. Without this, kotlin-dsl's own
    // (older) embedded Kotlin wins the classpath race silently, and :domain compiles against a
    // stdlib a full minor version behind the rest of the project. See the kotlin-jvm catalog entry.
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:${libs.versions.kotlinVersion.get()}")

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
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
