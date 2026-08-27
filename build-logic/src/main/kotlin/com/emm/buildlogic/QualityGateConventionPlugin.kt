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
         * An allowlist, not `tasks.withType<Detekt>()`, which sweeps in the per-source-set task KMP
         * registers across the whole hierarchy. Every entry here covers a source set no other does —
         * `detektMainAndroid` covers commonMain and androidMain WITH type resolution.
         *
         * `detektMainAndroid`/`detektCommonTestSourceSet`/`detektAndroidHostTestSourceSet` are still
         * named for `:domain`, the one module left on `justchill.kmp.library` (E11-06 removes them).
         * A plain `com.android.library` needs none of these three: `detektMain` already aggregates its
         * `detektDebug`/`detektRelease` (type-resolved, `src/main`), and `detektTest` already
         * aggregates `detektDebugUnitTest` AND `detektDebugAndroidTest` (both type-resolved, `src/test`
         * and `src/androidTest`) — confirmed with `:data:detektMain --dry-run` /
         * `:data:detektTest --dry-run` after E11-05's conversion. `detektAndroidDeviceTestSourceSet`
         * named only `:data`'s instrumented source set and has no replacement to add: it is dropped.
         */
        val DETEKT_GATE_TASKS = setOf(
            "detektMainAndroid",
            "detektCommonTestSourceSet",
            "detektAndroidHostTestSourceSet",
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

internal fun Project.contributeToQualityGate(vararg taskNames: String) {
    tasks.named(QualityGateConventionPlugin.GATE_TASK) {
        dependsOn(taskNames.toList())
    }
}
