package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * The single definition of "is this code good enough to push".
 *
 * Before this existed there were four hand-maintained lists — the pre-push hook, the reinforced
 * gate in docs/WORKFLOW.md, and the three GitHub workflows — and none was a superset of
 * the others. The hook ran five detekt tasks and zero tests; CI ran three test suites and a
 * `detekt` task that is NO-SOURCE on three of four modules; the doc listed a set that was missing
 * `:data:detektAndroidDeviceTestSourceSet`. Every consumer now runs `./gradlew qualityGate`, so
 * the lists cannot drift apart again.
 *
 * What lands on the gate per module:
 *  - the detekt tasks in [DETEKT_GATE_TASKS] that this module actually has
 *  - the compile-only tasks in [COMPILE_GATE_TASKS] that this module actually has
 *  - the schema checks in [SCHEMA_GATE_TASKS], for the module that has a SQLDelight schema
 *  - its host test suite, contributed by [KmpLibraryConventionPlugin] or the module's build file
 *  - the iOS compile, on macOS hosts only, contributed by [KmpLibraryConventionPlugin]
 *
 * And once, on the root project only: `:build-logic:test`, the suite of the included build. See
 * the root branch in [apply].
 *
 * This plugin is repo-specific, not a general-purpose one: that root branch names the included
 * build `build-logic` literally. On a root project whose build includes no such build, Gradle's
 * `gradle.includedBuild("build-logic")` throws `UnknownDomainObjectException` while the gate task
 * is being configured. Failing there is preferable to skipping the suite silently, which is the
 * drift this class exists to stop.
 */
class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: detekt, host tests, " +
                    ":build-logic:test, and (on macOS) the iOS compile. Invoked by the pre-push " +
                    "hook and by CI."

            // Lazy and existence-safe: a module only contributes the tasks it actually has, and
            // tasks registered after this plugin still land on the gate.
            dependsOn(
                target.tasks.matching {
                    it.name in DETEKT_GATE_TASKS ||
                        it.name in COMPILE_GATE_TASKS ||
                        it.name in SCHEMA_GATE_TASKS
                },
            )

            // build-logic is an INCLUDED build, so `./gradlew qualityGate` never reaches its tasks:
            // task-name matching only walks this build's projects. Its own suite has to be named,
            // and only once — hung off the root project, which applies this plugin for no other
            // reason. Everything else on the gate is a module contributing its own tasks.
            //
            // `parent == null` rather than `== rootProject`: reaching for `rootProject` is
            // cross-project access, which is what blocks Gradle's Project Isolation — the same
            // thing the root build file stays thin for.
            if (target.parent == null) {
                dependsOn(target.gradle.includedBuild(BUILD_LOGIC_BUILD).task(":$TEST_TASK"))
            }
        }
    }

    companion object {
        const val GATE_TASK = "qualityGate"

        /** The build wired in by `pluginManagement { includeBuild(...) }` — this very build. */
        private const val BUILD_LOGIC_BUILD = "build-logic"
        private const val TEST_TASK = "test"

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
            // :androidApp: the unit tests. `detektMain` covers production classes ONLY, so without
            // this the MockK ViewModel suite in androidApp/src/test — the largest test source set in
            // the repo — was the one body of code the gate never linted. It was carrying 24 findings
            // when this joined. It fans out to the debug variants only (dev + prod): detekt registers
            // no test task for a release variant, and every variant reads the same `src/test` folder,
            // so the source set is fully covered anyway.
            "detektTest",
        )

        /**
         * Compile-only tasks for source sets nothing else on the gate builds.
         *
         * The instrumented tests in `:data/src/androidDeviceTest` need a device to RUN, so they
         * cannot join the gate as tests — but they still have to compile. `detekt` over that
         * source set is not enough: detekt downgrades unresolvable code to a "compiler errors
         * found during analysis" warning and passes anyway, so a domain signature change could
         * break the instrumented suite and the gate would stay green until someone plugged in a
         * phone. Compiling costs a second and needs no device.
         */
        val COMPILE_GATE_TASKS = setOf("compileAndroidDeviceTest")

        /**
         * Schema verification: replay every `.sqm` over the committed `.db` snapshot and assert the
         * result matches the `CREATE TABLE` statements in the `.sq` files.
         *
         * This is the only automated check that a schema change came with a migration. A `.sq`
         * edited without a matching `.sqm` compiles clean, generates working query classes, and
         * passes every test on this repo — it breaks on the first upgrade of an already-installed
         * app, which is the one place nothing here can reach. Verified by adding a column to
         * `accounts.sq` with no migration: the task fails with
         * `/tables[accounts]/columns[accounts.canaryColumn] - ADDED`.
         *
         * SQLDelight creates the task whenever `schemaOutputDirectory` is set, and wires it into
         * `check` — which is exactly the problem: neither the pre-push hook nor any workflow runs
         * `check`. It was sitting there working and unreachable.
         */
        val SCHEMA_GATE_TASKS = setOf("verifySqlDelightMigration")

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
