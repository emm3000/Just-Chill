package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.util.concurrent.Callable

class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension: QualityGateExtension =
            target.extensions.create(EXTENSION, QualityGateExtension::class.java)
        val boundaries: TaskProvider<CheckModuleBoundariesTask> = target.registerBoundaryCheck()
        val composeFree: TaskProvider<CheckComposeFreeViewModelsTask> = target.registerComposeCheck()
        val snapshots: TaskProvider<CheckSqlDelightSnapshotsTask> = target.registerSnapshotCheck()

        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: detektMain and detektTest, " +
                    "compileDebugAndroidTestKotlin, compileReleaseKotlin, verifySqlDelightMigration, " +
                    ":build-logic:convention:test on the root, " +
                    "checkModuleBoundaries, checkComposeFreeViewModels and checkSqlDelightSnapshots, " +
                    "the unit tests the library plugins name, plus the tests and lint :androidApp adds. " +
                    "Invoked by CI."

            dependsOn(boundaries, composeFree, snapshots)
            dependsOn(target.tasks.matching { gates(it, extension.detektTasks.get()) })
            dependsOn(Callable { extension.detektTasks.get().map(target.tasks::named) })

            // Task-name matching never reaches an included build, so this suite has to be named or
            // it silently stops running. `parent == null` rather than `rootProject`: that is the
            // cross-project access which blocks Gradle's Project Isolation.
            if (target.parent == null) {
                dependsOn(target.gradle.includedBuild(BUILD_LOGIC_BUILD).task(TEST_TASK))
            }
        }
    }

    private fun gates(task: Task, detektTasks: Set<String>): Boolean = when (task.name) {
        in DETEKT_GATE_TASKS -> detektTasks.isEmpty()
        in COMPILE_GATE_TASKS, in SCHEMA_GATE_TASKS -> true
        else -> false
    }

    private fun Project.registerBoundaryCheck(): TaskProvider<CheckModuleBoundariesTask> =
        tasks.register<CheckModuleBoundariesTask>(BOUNDARY_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Fails when this module declares a dependency ADR 015 does not allow."
            modulePath.set(this@registerBoundaryCheck.path)
            dependencyPaths.set(provider { declaredProjectDependencies() })
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

    private fun Project.registerSnapshotCheck(): TaskProvider<CheckSqlDelightSnapshotsTask> =
        tasks.register<CheckSqlDelightSnapshotsTask>(SNAPSHOT_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Fails when a .sqm of this module has no schema snapshot, or a snapshot has no migration."
            migrations.from(fileTree(SQLDELIGHT_DIRECTORY) { include(MIGRATION_SOURCES) })
            snapshots.from(fileTree(SNAPSHOT_DIRECTORY) { include(SNAPSHOT_SOURCES) })
            report.set(layout.buildDirectory.file("reports/$SNAPSHOT_TASK.txt"))
        }

    private fun Project.declaredProjectDependencies(): Set<String> = configurations
        .flatMap { it.dependencies }
        .filterIsInstance<ProjectDependency>()
        .map { it.path }
        .filterNot { it == path }
        .toSet()

    // Every source set of every module roots under src/, and AGP 9's DSL no longer exposes a source
    // set's directories, so this follows a module's sources without naming a variant.
    private fun Project.composeFreeSources(): FileCollection =
        fileTree(SOURCE_DIRECTORY) { include(VIEW_MODEL_SOURCES, UI_STATE_SOURCES) }

    companion object {
        const val GATE_TASK: String = "qualityGate"
        const val BOUNDARY_TASK: String = "checkModuleBoundaries"
        const val COMPOSE_TASK: String = "checkComposeFreeViewModels"
        const val SNAPSHOT_TASK: String = "checkSqlDelightSnapshots"

        private const val EXTENSION: String = "qualityGate"
        private const val BUILD_LOGIC_BUILD: String = "build-logic"
        private const val TEST_TASK: String = ":convention:test"
        private const val ANDROID_BASE_PLUGIN: String = "com.android.base"
        private const val SOURCE_DIRECTORY: String = "src"
        private const val VIEW_MODEL_SOURCES: String = "**/*ViewModel.kt"
        private const val UI_STATE_SOURCES: String = "**/*UiState.kt"
        private const val SQLDELIGHT_DIRECTORY: String = "src/main/sqldelight"
        private const val SNAPSHOT_DIRECTORY: String = "src/main/sqldelight/databases"
        private const val MIGRATION_SOURCES: String = "**/*.sqm"
        private const val SNAPSHOT_SOURCES: String = "*.db"

        // An allowlist: `withType<Detekt>()` would also run the per-variant tasks these two aggregate.
        val DETEKT_GATE_TASKS: Set<String> = setOf(
            "detektMain",
            "detektTest",
        )

        // detekt passes on unresolvable code, so only compiling `androidTest` catches a signature
        // change that breaks the instrumented suite. `compileReleaseKotlin` is named because
        // `assembleProdRelease` is not on the gate and no library module's release variant is
        // type-checked anywhere else; it used to ride along on `detektRelease` by accident.
        val COMPILE_GATE_TASKS: Set<String> = setOf(
            "compileDebugAndroidTestKotlin",
            "compileReleaseKotlin",
        )

        // The only check that a `.sq` change shipped its `.sqm`. SQLDelight wires it into `check`
        // alone, which the gate never runs.
        val SCHEMA_GATE_TASKS: Set<String> = setOf("verifySqlDelightMigration")
    }
}
