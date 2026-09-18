package com.emm.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.emm.buildlogic.internal.BuildConventions
import com.emm.buildlogic.internal.configureAndroidCompose
import com.emm.buildlogic.internal.configureAndroidUnitTestDependencies
import com.emm.buildlogic.internal.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        apply<QualityGateConventionPlugin>()

        val extension: ApplicationExtension = extensions.getByType<ApplicationExtension>()
        configureKotlinAndroid(extension)
        extension.defaultConfig.targetSdk = BuildConventions.TARGET_SDK
        configureAndroidCompose(extension)
        configureAndroidUnitTestDependencies()
    }
}
