package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * The single definition of "is this code good enough to push".
 *
 * Before this existed there were four hand-maintained lists — the pre-push hook, the reinforced
 * gate in docs/kmp/ORCHESTRATION.md, and the three GitHub workflows — and none was a superset of
 * the others. The hook ran five detekt tasks and zero tests; CI ran three test suites and a
 * `detekt` task that is NO-SOURCE on three of four modules; the doc listed a set that was missing
 * `:data:detektAndroidDeviceTestSourceSet`. Every consumer now runs `./gradlew qualityGate`, so
 * the lists cannot drift apart again.
 *
 * What lands on the gate per module:
 *  - the detekt tasks in [DETEKT_GATE_TASKS] that this module actually has
 *  - its host test suite, contributed by [KmpLibraryConventionPlugin] or the module's build file
 *  - the iOS compile, on macOS hosts only, contributed by [KmpLibraryConventionPlugin]
 */
class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: detekt, host tests, and " +
                    "(on macOS) the iOS compile. Invoked by the pre-push hook and by CI."

            // Lazy and existence-safe: a module only contributes the tasks it actually has, and
            // tasks registered after this plugin still land on the gate.
            dependsOn(target.tasks.matching { it.name in DETEKT_GATE_TASKS })
        }
    }

    companion object {
        const val GATE_TASK = "qualityGate"

        /**
         * The detekt tasks that constitute real coverage.
         *
         * Named explicitly rather than taken from `tasks.withType<Detekt>()`. KMP registers a detekt
         * task per source set in the whole hierarchy — appleMain, nativeMain, iosArm64Main,
         * iosSimulatorArm64Main — which analyse the same files as iosMain and would each demand
         * their own baseline file. Likewise `detektCommonMainSourceSet` and
         * `detektAndroidMainSourceSet` re-analyse what `detektMainAndroid` already covers, and it
         * covers them WITH type resolution, so it is strictly the better task. Blanket wiring is
         * more tasks, not more coverage.
         */
        val DETEKT_GATE_TASKS = setOf(
            // KMP modules: commonMain + androidMain, with type resolution.
            "detektMainAndroid",
            // KMP modules: iosMain. No type resolution — Kotlin/Native has none in detekt 2.0 —
            // so this is style and structure only. Runs on any host; detekt is JVM analysis.
            "detektIosMainSourceSet",
            // Test source sets, where present.
            "detektCommonTestSourceSet",
            "detektAndroidHostTestSourceSet",
            "detektAndroidDeviceTestSourceSet",
            // :androidApp: fans out across all four build variants.
            "detektMain",
        )

        /**
         * Kotlin/Native only produces iOS binaries on an Apple host, so the iOS compile joins the
         * gate on macOS only — and never silently. A difference nobody is told about is exactly how
         * the four lists drifted apart in the first place.
         */
        val isMacOsHost: Boolean
            get() = System.getProperty("os.name").orEmpty().startsWith("Mac")
    }
}

/** Adds tasks of this project, by name, to its quality gate. */
internal fun Project.contributeToQualityGate(vararg taskNames: String) {
    tasks.named(QualityGateConventionPlugin.GATE_TASK) {
        dependsOn(taskNames.toList())
    }
}
