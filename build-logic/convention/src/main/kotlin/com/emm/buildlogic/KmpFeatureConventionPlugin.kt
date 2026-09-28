package com.emm.buildlogic

import com.emm.buildlogic.internal.libs
import com.emm.buildlogic.internal.library
import com.emm.buildlogic.internal.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

class KmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        apply<KmpLibraryConventionPlugin>()
        pluginManager.apply(libs.pluginId("kotlin-serialization"))
        pluginManager.apply(libs.pluginId("kotlin-compose"))

        extensions.configure<ComposeCompilerGradlePluginExtension> {
            targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
        }

        dependencies {
            add("commonMainImplementation", project(":core:domain"))
            add("commonMainImplementation", project(":core:presentation"))
            add("commonMainImplementation", platform(libs.library("koin-bom")))
            add("commonMainImplementation", libs.library("koin-core"))
            add("commonMainImplementation", libs.library("koin-core-viewmodel"))
            add("androidMainImplementation", project(":core:ui"))
            add("androidMainImplementation", libs.library("koin-compose-viewmodel"))
            add("androidMainImplementation", libs.library("androidx-navigation3-runtime"))
            add("androidMainImplementation", libs.library("kotlinx-serialization-json"))
            add("androidHostTestImplementation", project(":core:testing"))
        }
    }
}
