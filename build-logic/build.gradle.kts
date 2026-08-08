import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

// A convention plugin can only `id("...")` a plugin whose implementation is on this build's
// compile classpath. Gradle publishes one "marker" artifact per plugin id, shaped
// `<id>:<id>.gradle.plugin:<version>` — this maps a catalog plugin alias onto that coordinate so
// the version still comes from gradle/libs.versions.toml and is never repeated here.
fun marker(plugin: Provider<PluginDependency>): String = plugin.get().run {
    "$pluginId:$pluginId.gradle.plugin:${version.requiredVersion}"
}

dependencies {
    implementation(marker(libs.plugins.kotlin.multiplatform))
    implementation(marker(libs.plugins.android.kotlin.multiplatform.library))
}
