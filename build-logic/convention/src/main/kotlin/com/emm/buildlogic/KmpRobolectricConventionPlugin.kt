package com.emm.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.emm.buildlogic.internal.library
import com.emm.buildlogic.internal.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpRobolectricConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val multiplatform: KotlinMultiplatformExtension? = extensions.findByType<KotlinMultiplatformExtension>()
        val android: KotlinMultiplatformAndroidLibraryTarget = (multiplatform as? ExtensionAware)
            ?.extensions
            ?.findByType<KotlinMultiplatformAndroidLibraryTarget>()
            ?: error("justchill.kmp.robolectric needs justchill.kmp.feature applied before it in $path")
        android.compilations.withType<KotlinMultiplatformAndroidHostTestCompilation>().configureEach {
            isIncludeAndroidResources = true
        }
        HOST_TEST_LIBRARIES.forEach { alias -> dependencies.add(CONFIGURATION, libs.library(alias)) }
    }

    private companion object {
        const val CONFIGURATION: String = "androidHostTestImplementation"
        val HOST_TEST_LIBRARIES: List<String> =
            listOf("androidx-ui-test-junit4", "androidx-ui-test-manifest", "robolectric")
    }
}
