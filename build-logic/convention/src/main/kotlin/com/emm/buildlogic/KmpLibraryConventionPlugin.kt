package com.emm.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.emm.buildlogic.internal.BuildConventions
import com.emm.buildlogic.internal.gateOn
import com.emm.buildlogic.internal.libs
import com.emm.buildlogic.internal.library
import com.emm.buildlogic.internal.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-multiplatform"))
        pluginManager.apply(libs.pluginId("android-kotlin-multiplatform-library"))
        apply<DetektConventionPlugin>()
        apply<QualityGateConventionPlugin>()

        val multiplatform: KotlinMultiplatformExtension = extensions.getByType<KotlinMultiplatformExtension>()
        multiplatform.jvmToolchain(BuildConventions.JVM_TOOLCHAIN)
        multiplatform.compilerOptions.optIn.addAll(BuildConventions.COROUTINES_OPT_INS)
        configureAndroidTarget(multiplatform)
        multiplatform.iosArm64()
        multiplatform.iosSimulatorArm64()

        dependencies {
            add("commonTestImplementation", kotlin("test"))
            add("androidHostTestImplementation", libs.library("junit"))
            add("androidHostTestImplementation", libs.library("kotlinx-coroutines-test"))
            add("androidHostTestImplementation", libs.library("mockk"))
        }

        QualityGateConventionPlugin.KMP_GATE_TASKS.forEach(::gateOn)
    }

    private fun Project.configureAndroidTarget(multiplatform: KotlinMultiplatformExtension) {
        val android: KotlinMultiplatformAndroidLibraryTarget =
            (multiplatform as ExtensionAware).extensions.getByType<KotlinMultiplatformAndroidLibraryTarget>()
        android.namespace = BuildConventions.namespaceOf(path)
        android.compileSdk = BuildConventions.COMPILE_SDK
        android.minSdk = BuildConventions.MIN_SDK
        android.compilerOptions.jvmTarget.set(JvmTarget.fromTarget(BuildConventions.JVM_TARGET))
        android.withHostTest {}
    }
}
