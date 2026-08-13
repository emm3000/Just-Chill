import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

// A convention plugin can only apply a plugin whose implementation is on this build's compile
// classpath. Gradle publishes one "marker" artifact per plugin id, shaped
// `<id>:<id>.gradle.plugin:<version>` — this maps a catalog plugin alias onto that coordinate so
// the version still comes from gradle/libs.versions.toml and is never repeated here.
fun marker(plugin: Provider<PluginDependency>): String = plugin.get().run {
    "$pluginId:$pluginId.gradle.plugin:${version.requiredVersion}"
}

dependencies {
    implementation(marker(libs.plugins.kotlin.multiplatform))
    implementation(marker(libs.plugins.android.kotlin.multiplatform.library))
    // BuildInfoConventionPlugin configures ApplicationAndroidComponentsExtension, which the
    // application plugin defines. The type used to arrive transitively through the KMP-library
    // marker above — same AGP artifact, undeclared — so this build compiled against something it
    // never asked for. Declared here, the compile classpath states what the code actually uses.
    implementation(marker(libs.plugins.android.application))
    implementation(marker(libs.plugins.detekt))
}

// Convention plugins are Plugin<Project> classes rather than precompiled .gradle.kts scripts: real
// Kotlin the IDE can navigate and refactor, with a declared id -> class mapping instead of an
// implicit filename convention.
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
