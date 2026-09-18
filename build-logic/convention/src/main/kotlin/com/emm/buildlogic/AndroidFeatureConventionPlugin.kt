package com.emm.buildlogic

import com.emm.buildlogic.internal.libs
import com.emm.buildlogic.internal.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        apply<AndroidComposeConventionPlugin>()
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:ui"))
            add("implementation", platform(libs.library("koin-bom")))
            add("implementation", libs.library("koin-core"))
            add("implementation", libs.library("koin-compose-viewmodel"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("androidx-navigation3-runtime"))
            add("implementation", libs.library("kotlinx-serialization-json"))
            add("testImplementation", project(":core:testing"))
        }
    }
}
