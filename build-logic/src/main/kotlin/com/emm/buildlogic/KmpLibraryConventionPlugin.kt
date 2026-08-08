package com.emm.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.emm.buildlogic.internal.BuildConventions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * The "what am I" half of the three KMP library modules: :domain, :data and :shared-ui.
 *
 * Targets, compile SDK, JVM target and the host-test source set live here once instead of being
 * copy-pasted into three build files. Each module keeps the half that genuinely differs — its
 * dependencies, its `namespace`, its `minSdk`, and any extra source set such as :data's
 * androidDeviceTest.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("justchill.detekt")

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()

            // `android`, not the deprecated `androidLibrary` alias (AGP 9 marks that one
            // @Deprecated with ReplaceWith("android")). From a Plugin<Project> class there is no
            // generated DSL accessor for it, so it is reached as what it actually is: an extension
            // registered on the Kotlin multiplatform extension.
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
