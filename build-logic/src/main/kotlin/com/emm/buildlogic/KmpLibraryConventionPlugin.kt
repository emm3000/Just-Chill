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

        val hasIosTargets = findProperty("justchill.kmp.ios") != "false"

        if (hasIosTargets) {
            if (QualityGateConventionPlugin.isMacOsHost) {
                // The only mechanical proof that the exported core stays free of `java.*` and
                // `android.*` — nothing else in the gate compiles it for a non-JVM, non-Android
                // target. See docs/adr/003.
                tasks.named(QualityGateConventionPlugin.GATE_TASK) {
                    dependsOn(tasks.matching { it.name.startsWith("compileKotlinIos") })
                }
            } else {
                logger.lifecycle(
                    "qualityGate($path): iOS compile skipped — Kotlin/Native needs a macOS host. " +
                        "detekt still covers iosMain.",
                )
            }
        }

        extensions.configure<KotlinMultiplatformExtension> {
            if (hasIosTargets) {
                iosArm64()
                iosSimulatorArm64()
            }

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
