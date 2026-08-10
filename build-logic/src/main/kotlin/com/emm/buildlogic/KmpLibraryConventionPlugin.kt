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
        pluginManager.apply("justchill.quality.gate")

        // Every KMP module here has a JVM host-test suite; it belongs on the gate. The pre-push
        // hook used to run detekt and no tests at all.
        contributeToQualityGate("testAndroidHostTest")

        // :shared-ui opted out in slice S2 (docs/swiftui/PLAN.md): its Compose UI is Android-only;
        // iOS consumes :presentation through the JustChillKit framework instead. The property lives
        // in the module's own gradle.properties, so the opt-out is visible next to the build file.
        val hasIosTargets = findProperty("justchill.kmp.ios") != "false"

        if (hasIosTargets) {
            if (QualityGateConventionPlugin.isMacOsHost) {
                // The iOS compile is the only mechanical proof that commonMain stays free of java.* /
                // android.* — see docs/adr/003 (gate relocated to :presentation by docs/adr/005).
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
