package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val boundaries: TaskProvider<CheckModuleBoundariesTask> = target.registerBoundaryCheck()
        val composeFree: TaskProvider<CheckComposeFreeViewModelsTask> = target.registerComposeCheck()
        val snapshots: TaskProvider<CheckSqlDelightSnapshotsTask> = target.registerSnapshotCheck()
        val lazyKeys: TaskProvider<CheckLazyListKeysTask> = target.registerLazyKeyCheck()

        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: " +
                    "compileDebugAndroidTestKotlin, compileReleaseKotlin, verifySqlDelightMigration, " +
                    ":build-logic:convention:test on the root, " +
                    "checkModuleBoundaries, checkComposeFreeViewModels, checkSqlDelightSnapshots " +
                    "and checkLazyListKeys, " +
                    "the unit tests the library plugins name, plus the tests and the prodRelease " +
                    "compile :androidApp adds. " +
                    "Invoked by CI."

            dependsOn(boundaries, composeFree, snapshots, lazyKeys)
            dependsOn(target.tasks.matching(::gates))

            // Task-name matching never reaches an included build, so this suite has to be named or
            // it silently stops running. `parent == null` rather than `rootProject`: that is the
            // cross-project access which blocks Gradle's Project Isolation.
            if (target.parent == null) {
                dependsOn(target.gradle.includedBuild(BUILD_LOGIC_BUILD).task(TEST_TASK))
            }
        }
    }

    private fun gates(task: Task): Boolean =
        task.name in COMPILE_GATE_TASKS || task.name in SCHEMA_GATE_TASKS

    private fun Project.registerBoundaryCheck(): TaskProvider<CheckModuleBoundariesTask> =
        tasks.register<CheckModuleBoundariesTask>(BOUNDARY_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Fails when this module declares a dependency ADR 015 does not allow."
            modulePath.set(this@registerBoundaryCheck.path)
            dependencyPaths.set(provider { declaredProjectDependencies(tests = false) })
            testDependencyPaths.set(provider { declaredProjectDependencies(tests = true) })
            android.set(provider { pluginManager.hasPlugin(ANDROID_BASE_PLUGIN) })
            report.set(layout.buildDirectory.file("reports/$BOUNDARY_TASK.txt"))
        }

    private fun Project.registerComposeCheck(): TaskProvider<CheckComposeFreeViewModelsTask> =
        tasks.register<CheckComposeFreeViewModelsTask>(COMPOSE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Fails when a ViewModel or UiState source of this module imports Compose."
            sources.from(composeFreeSources())
            report.set(layout.buildDirectory.file("reports/$COMPOSE_TASK.txt"))
        }

    private fun Project.registerSnapshotCheck(): TaskProvider<CheckSqlDelightSnapshotsTask> {
        val extension: SqlDelightSnapshotsExtension =
            extensions.create<SqlDelightSnapshotsExtension>(SNAPSHOT_EXTENSION)
        return tasks.register<CheckSqlDelightSnapshotsTask>(SNAPSHOT_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Fails when a .sqm of this module has no schema snapshot, or a snapshot has no migration."
            migrations.from(fileTree(SQLDELIGHT_DIRECTORY) { include(MIGRATION_SOURCES) })
            snapshots.from(fileTree(SNAPSHOT_DIRECTORY) { include(SNAPSHOT_SOURCES) })
            floor.set(extension.floor)
            report.set(layout.buildDirectory.file("reports/$SNAPSHOT_TASK.txt"))
        }
    }

    private fun Project.registerLazyKeyCheck(): TaskProvider<CheckLazyListKeysTask> =
        tasks.register<CheckLazyListKeysTask>(LAZY_KEY_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Fails when a LazyList key of this module passes an id whose type this module " +
                    "declares as a value class instead of its underlying primitive. It sees only " +
                    "this module's production sources, so a key whose row model another module " +
                    "declares is skipped, `Type::property` included. So is a key built from " +
                    "anything but `it.<property>` or `Type::property`, and one whose owner is " +
                    "unresolvable while the property name is declared more than once here. The " +
                    "report file counts the key sites found, evaluated and rejected."
            sources.from(lazyKeySources())
            report.set(layout.buildDirectory.file("reports/$LAZY_KEY_TASK.txt"))
        }

    // Split so the task can allow :core:testing from a test configuration alone: a fixture module
    // reached from src/test never reaches a user, while every other edge binds in both.
    private fun Project.declaredProjectDependencies(tests: Boolean): Set<String> = configurations
        .filter { it.isTestConfiguration() == tests }
        .flatMap { it.dependencies }
        .filterIsInstance<ProjectDependency>()
        .map { it.path }
        .filterNot { it == path }
        .toSet()

    // Every source set of every module roots under src/, and AGP 9's DSL no longer exposes a source
    // set's directories, so this follows a module's sources without naming a variant.
    private fun Project.composeFreeSources(): FileCollection =
        fileTree(SOURCE_DIRECTORY) { include(VIEW_MODEL_SOURCES, UI_STATE_SOURCES) }

    // A test fixture declaring its own value-class id would be read as a production declaration
    // and could redden a safe key through the name-only fallback, so the test source sets stay out.
    private fun Project.lazyKeySources(): FileCollection =
        fileTree(SOURCE_DIRECTORY) {
            include(KOTLIN_SOURCES)
            exclude(TEST_SOURCE_SETS)
        }

    companion object {
        const val GATE_TASK: String = "qualityGate"
        const val BOUNDARY_TASK: String = "checkModuleBoundaries"
        const val COMPOSE_TASK: String = "checkComposeFreeViewModels"
        const val SNAPSHOT_TASK: String = "checkSqlDelightSnapshots"
        const val LAZY_KEY_TASK: String = "checkLazyListKeys"
        const val SNAPSHOT_EXTENSION: String = "sqlDelightSnapshots"

        private const val BUILD_LOGIC_BUILD: String = "build-logic"
        private const val TEST_TASK: String = ":convention:test"
        private const val ANDROID_BASE_PLUGIN: String = "com.android.base"
        private const val TEST_CONFIGURATION_PREFIX: String = "test"
        private const val ANDROID_TEST_CONFIGURATION_PREFIX: String = "androidTest"

        private fun Configuration.isTestConfiguration(): Boolean =
            name.startsWith(TEST_CONFIGURATION_PREFIX) || name.startsWith(ANDROID_TEST_CONFIGURATION_PREFIX)
        private const val SOURCE_DIRECTORY: String = "src"
        private const val VIEW_MODEL_SOURCES: String = "**/*ViewModel.kt"
        private const val UI_STATE_SOURCES: String = "**/*UiState.kt"
        private const val KOTLIN_SOURCES: String = "**/*.kt"
        private val TEST_SOURCE_SETS: List<String> = listOf("test*/**", "androidTest*/**")
        private const val SQLDELIGHT_DIRECTORY: String = "src/main/sqldelight"
        private const val SNAPSHOT_DIRECTORY: String = "src/main/sqldelight/databases"
        private const val MIGRATION_SOURCES: String = "**/*.sqm"
        private const val SNAPSHOT_SOURCES: String = "*.db"

        // Compiling `androidTest` is the only thing that catches a signature change breaking the
        // instrumented suite. `compileReleaseKotlin` is named because `assembleProdRelease` is not
        // on the gate and no library module's release variant is type-checked anywhere else.
        val COMPILE_GATE_TASKS: Set<String> = setOf(
            "compileDebugAndroidTestKotlin",
            "compileReleaseKotlin",
        )

        // The only check that a `.sq` change shipped its `.sqm`. SQLDelight wires it into `check`
        // alone, which the gate never runs.
        val SCHEMA_GATE_TASKS: Set<String> = setOf("verifySqlDelightMigration")
    }
}
