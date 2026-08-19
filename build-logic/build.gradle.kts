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
    implementation(marker(libs.plugins.kotlin.multiplatform))
    implementation(marker(libs.plugins.android.kotlin.multiplatform.library))
    // Not redundant, and not compiler-enforced: ApplicationAndroidComponentsExtension resolves
    // through the KMP-library marker above too, the same AGP artifact undeclared.
    implementation(marker(libs.plugins.android.application))
    implementation(marker(libs.plugins.detekt))

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "justchill.kmp.library"
            implementationClass = "com.emm.buildlogic.KmpLibraryConventionPlugin"
        }
        register("detekt") {
            id = "justchill.detekt"
            implementationClass = "com.emm.buildlogic.DetektConventionPlugin"
        }
        register("qualityGate") {
            id = "justchill.quality.gate"
            implementationClass = "com.emm.buildlogic.QualityGateConventionPlugin"
        }
        register("iosSupabaseConfig") {
            id = "justchill.ios.supabase.config"
            implementationClass = "com.emm.buildlogic.IosSupabaseConfigConventionPlugin"
        }
        register("buildInfo") {
            id = "justchill.build.info"
            implementationClass = "com.emm.buildlogic.BuildInfoConventionPlugin"
        }
    }
}
