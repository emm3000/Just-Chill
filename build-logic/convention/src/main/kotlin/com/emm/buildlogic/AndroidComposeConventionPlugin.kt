package com.emm.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.emm.buildlogic.internal.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        apply<AndroidLibraryConventionPlugin>()
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        configureAndroidCompose(extensions.getByType<LibraryExtension>())
    }
}
