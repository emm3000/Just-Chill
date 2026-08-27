package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: detekt, host tests, and " +
                    ":build-logic:test. Invoked by the pre-push hook and by CI."

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
                dependsOn(target.gradle.includedBuild(BUILD_LOGIC_BUILD).task(":$TEST_TASK"))
            }
        }
    }

    companion object {
        const val GATE_TASK = "qualityGate"

        private const val BUILD_LOGIC_BUILD = "build-logic"
        private const val TEST_TASK = "test"

        /**
         * An allowlist, not `tasks.withType<Detekt>()`, which would sweep in per-variant tasks
         * already aggregated below. Every entry here covers a source set no other does.
         *
         * `detektMain` and `detektTest` are all a module needs, on either shape now in the graph:
         * for the three `com.android.library` modules, `detektMain` aggregates `detektDebug`/
         * `detektRelease` (type-resolved, `src/main`) and `detektTest` aggregates
         * `detektDebugUnitTest` AND `detektDebugAndroidTest` (both type-resolved, `src/test` and
         * `src/androidTest`) — confirmed with `:data:detektMain --dry-run` / `:data:detektTest
         * --dry-run` after E11-05's conversion. For `:domain`'s plain `org.jetbrains.kotlin.jvm`
         * (E11-06), the same two names resolve directly to `src/main` and `src/test` with no
         * variant aggregation needed — confirmed the same way. No module is left on
         * `justchill.kmp.library`; ADR 011's KMP-era names (`detektMainAndroid`,
         * `detektCommonTestSourceSet`, `detektAndroidHostTestSourceSet`,
         * `detektAndroidDeviceTestSourceSet`) have no replacement to add and are dropped for good.
         */
        val DETEKT_GATE_TASKS = setOf(
            "detektMain",
            "detektTest",
        )

        /**
         * detekt over `androidTest` is not a substitute for compiling it: detekt downgrades
         * unresolvable code to a warning and passes, so a domain signature change would break the
         * instrumented suite and leave the gate green. `compileDebugAndroidTestKotlin` is `:data`'s
         * instrumented compile task since E11-05 (was `compileAndroidDeviceTest` under
         * `justchill.kmp.library`).
         */
        val COMPILE_GATE_TASKS = setOf("compileDebugAndroidTestKotlin")

        /**
         * The only automated check that a schema change came with a migration: a `.sq` edited
         * without a matching `.sqm` compiles and passes every test here, then breaks on the first
         * upgrade of an installed app. SQLDelight wires it only into `check`, which nothing here runs.
         */
        val SCHEMA_GATE_TASKS = setOf("verifySqlDelightMigration")
    }
}
