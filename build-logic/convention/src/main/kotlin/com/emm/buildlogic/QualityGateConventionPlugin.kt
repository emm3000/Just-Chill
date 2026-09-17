package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: detektMain and detektTest, " +
                    "compileDebugAndroidTestKotlin, verifySqlDelightMigration, :build-logic:convention:test on the root, " +
                    "the unit tests the library plugins name, plus the tests and lint :androidApp adds. " +
                    "Invoked by the pre-push hook and by CI."

            dependsOn(
                target.tasks.matching {
                    it.name in DETEKT_GATE_TASKS ||
                        it.name in COMPILE_GATE_TASKS ||
                        it.name in SCHEMA_GATE_TASKS
                },
            )

            // Task-name matching never reaches an included build, so this suite has to be named or
            // it silently stops running. `parent == null` rather than `rootProject`: that is the
            // cross-project access which blocks Gradle's Project Isolation.
            if (target.parent == null) {
                dependsOn(target.gradle.includedBuild(BUILD_LOGIC_BUILD).task(TEST_TASK))
            }
        }
    }

    companion object {
        const val GATE_TASK = "qualityGate"

        private const val BUILD_LOGIC_BUILD = "build-logic"
        private const val TEST_TASK = ":convention:test"

        // An allowlist: `withType<Detekt>()` would also run the per-variant tasks these two aggregate.
        val DETEKT_GATE_TASKS = setOf(
            "detektMain",
            "detektTest",
        )

        // detekt passes on unresolvable code, so only compiling `androidTest` catches a signature
        // change that breaks the instrumented suite.
        val COMPILE_GATE_TASKS = setOf("compileDebugAndroidTestKotlin")

        // The only check that a `.sq` change shipped its `.sqm`. SQLDelight wires it into `check`
        // alone, which the gate never runs.
        val SCHEMA_GATE_TASKS = setOf("verifySqlDelightMigration")
    }
}
