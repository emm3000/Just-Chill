package com.emm.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.emm.buildlogic.internal.BuildConventions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("justchill.detekt")
        pluginManager.apply("justchill.quality.gate")

        contributeToQualityGate("testAndroidHostTest")

        extensions.configure<KotlinMultiplatformExtension> {
            val android = (this as ExtensionAware).extensions
                .getByName("android") as KotlinMultiplatformAndroidLibraryTarget

            android.compileSdk = BuildConventions.COMPILE_SDK
            android.withHostTest { }
            android.compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
}
