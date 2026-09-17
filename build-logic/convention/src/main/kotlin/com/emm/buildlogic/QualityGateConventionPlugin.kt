package com.emm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

class QualityGateConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension: QualityGateExtension =
            target.extensions.create(EXTENSION, QualityGateExtension::class.java)
        val boundaries: TaskProvider<CheckModuleBoundariesTask> = target.registerBoundaryCheck()
        val composeFree: TaskProvider<CheckComposeFreeViewModelsTask> = target.registerComposeCheck()

        target.tasks.register(GATE_TASK) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description =
                "Runs every check that must pass before pushing: detektMain and detektTest, " +
                    "compileDebugAndroidTestKotlin, verifySqlDelightMigration, :build-logic:convention:test on the root, " +
                    "checkModuleBoundaries and checkComposeFreeViewModels, " +
                    "the unit tests the library plugins name, plus the tests and lint :androidApp adds. " +
                    "Invoked by the pre-push hook and by CI."

            dependsOn(boundaries, composeFree)
            dependsOn(target.tasks.matching { gates(it, extension.detektTasks.get()) })

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
        else -> task.name in detektTasks
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

    private fun Project.declaredProjectDependencies(): Set<String> = configurations
        .flatMap { it.dependencies }
        .filterIsInstance<ProjectDependency>()
        .map { it.path }
        .filterNot { it == path }
        .toSet()

    private fun Project.composeFreeSources(): FileCollection = files(
        provider {
            // :androidApp is the composition point, not a feature: its dev-flavor experiences
            // playground holds Compose-typed render models the MVI contract never covered. ADR 015.
            if (pluginManager.hasPlugin(ANDROID_APPLICATION_PLUGIN)) {
                files()
            } else {
                // Every source set roots under src/, and AGP 9's DSL no longer exposes a source
                // set's directories, so this follows a module's sources without naming a variant.
                fileTree(SOURCE_DIRECTORY) { include(VIEW_MODEL_SOURCES, UI_STATE_SOURCES) }
            }
        },
    )

    companion object {
        const val GATE_TASK = "qualityGate"
        const val BOUNDARY_TASK = "checkModuleBoundaries"
        const val COMPOSE_TASK = "checkComposeFreeViewModels"

        private const val EXTENSION = "qualityGate"
        private const val BUILD_LOGIC_BUILD = "build-logic"
        private const val TEST_TASK = ":convention:test"
        private const val ANDROID_BASE_PLUGIN = "com.android.base"
        private const val ANDROID_APPLICATION_PLUGIN = "com.android.application"
        private const val SOURCE_DIRECTORY = "src"
        private const val VIEW_MODEL_SOURCES = "**/*ViewModel.kt"
        private const val UI_STATE_SOURCES = "**/*UiState.kt"

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
