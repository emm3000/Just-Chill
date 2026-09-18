package com.emm.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.emm.buildlogic.internal.BuildConventions
import com.emm.buildlogic.internal.configureAndroidUnitTestDependencies
import com.emm.buildlogic.internal.configureKotlinAndroid
import com.emm.buildlogic.internal.gateOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        apply<QualityGateConventionPlugin>()

        val extension: LibraryExtension = extensions.getByType<LibraryExtension>()
        extension.namespace = BuildConventions.namespaceOf(path)
        configureKotlinAndroid(extension)
        extension.testOptions.targetSdk = BuildConventions.TARGET_SDK
        configureAndroidUnitTestDependencies()
        gateOn("testDebugUnitTest")
    }
}
