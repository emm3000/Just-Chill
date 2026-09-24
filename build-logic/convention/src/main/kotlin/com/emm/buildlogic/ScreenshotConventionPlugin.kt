package com.emm.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.emm.buildlogic.internal.library
import com.emm.buildlogic.internal.libs
import com.emm.buildlogic.internal.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

class ScreenshotConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val android: CommonExtension = extensions.findByType<CommonExtension>()
            ?: error("justchill.screenshot needs an Android plugin applied before it in $path")
        android.experimentalProperties[SOURCE_SET_PROPERTY] = true
        pluginManager.apply(libs.pluginId(PLUGIN))
        TOOLING_LIBRARIES.forEach { alias -> dependencies.add(CONFIGURATION, libs.library(alias)) }
    }

    companion object {
        const val VALIDATE_TASK: String = "validateDebugScreenshotTest"

        private const val SOURCE_SET_PROPERTY: String = "android.experimental.enableScreenshotTest"
        private const val PLUGIN: String = "android-compose-screenshot"
        private const val CONFIGURATION: String = "screenshotTestImplementation"
        private val TOOLING_LIBRARIES: List<String> = listOf("screenshot-validation-api", "androidx-ui-tooling")
    }
}
