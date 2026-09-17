package com.emm.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GateCheckTest {

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val fixture: ConventionPluginFixture
        get() = ConventionPluginFixture(temporaryFolder.root)

    @Test
    fun `every edge ADR 015 allows passes the boundary check`() {
        val modules: Map<String, String> = mapOf(
            ":androidApp" to module(":feature:loan", ":core:ui", ":core:domain"),
            ":feature:loan" to module(":core:domain", ":core:ui"),
            ":core:ui" to module(":core:domain"),
            ":core:domain" to module(),
            ":ui-android" to module(":presentation"),
            ":presentation" to module(),
        )

        val result: BuildResult = fixture.check(task = BOUNDARY_TASK, modules = modules)

        modules.keys.forEach { path -> assertSucceeded(result, "$path:$BOUNDARY_TASK") }
    }

    @Test
    fun `a feature that depends on another feature fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$BOUNDARY_TASK",
            modules = mapOf(
                ":feature:loan" to module(":feature:report"),
                ":feature:report" to module(),
            ),
        )

        assertTrue(output.contains(":feature:loan depends on :feature:report"), output)
    }

    @Test
    fun `a forbidden edge declared in a test configuration fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$BOUNDARY_TASK",
            modules = mapOf(
                ":feature:loan" to module(testDependencies = arrayOf(":feature:report")),
                ":feature:report" to module(),
            ),
        )

        assertTrue(output.contains(":feature:loan depends on :feature:report"), output)
    }

    @Test
    fun `a core module that depends on anything but core domain fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:backup:$BOUNDARY_TASK",
            modules = mapOf(
                ":core:backup" to module(":core:ui"),
                ":core:ui" to module(),
            ),
        )

        assertTrue(output.contains(":core:backup depends on :core:ui"), output)
    }

    @Test
    fun `a module other than the app that depends on a feature fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":ui-android:$BOUNDARY_TASK",
            modules = mapOf(
                ":ui-android" to module(":feature:loan"),
                ":feature:loan" to module(),
            ),
        )

        assertTrue(output.contains(":ui-android depends on :feature:loan"), output)
    }

    @Test
    fun `core domain that applies an android plugin fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:domain:$BOUNDARY_TASK",
            modules = mapOf(":core:domain" to """plugins { id("justchill.android.library") }"""),
        )

        assertTrue(output.contains(":core:domain applies an Android plugin"), output)
    }

    @Test
    fun `a ViewModel or UiState free of compose passes the compose check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$COMPOSE_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanViewModel.kt" to "package sample\n\nclass LoanViewModel\n",
                "feature/loan/src/main/kotlin/LoanScreen.kt" to COMPOSE_SOURCE,
            ),
        )

        assertSucceeded(result, ":feature:loan:$COMPOSE_TASK")
    }

    @Test
    fun `a UiState that imports compose fails the compose check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$COMPOSE_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf("feature/loan/src/main/kotlin/LoanUiState.kt" to COMPOSE_SOURCE),
        )

        assertTrue(output.contains("LoanUiState.kt"), output)
    }

    @Test
    fun `a UiState outside the main source set fails the compose check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$COMPOSE_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf("feature/loan/src/test/kotlin/LoanUiState.kt" to COMPOSE_SOURCE),
        )

        assertTrue(output.contains("src/test/kotlin/LoanUiState.kt"), output)
    }

    private fun assertSucceeded(result: BuildResult, taskPath: String) {
        assertEquals(TaskOutcome.SUCCESS, result.task(taskPath)?.outcome, taskPath)
    }

    private fun module(
        vararg dependencies: String,
        testDependencies: Array<String> = emptyArray(),
    ): String = buildString {
        appendLine("""plugins { id("justchill.jvm.library") }""")
        if (dependencies.isNotEmpty() || testDependencies.isNotEmpty()) {
            appendLine("dependencies {")
            dependencies.forEach { appendLine("""    implementation(project("$it"))""") }
            testDependencies.forEach { appendLine("""    testImplementation(project("$it"))""") }
            appendLine("}")
        }
    }

    private companion object {
        const val BOUNDARY_TASK: String = QualityGateConventionPlugin.BOUNDARY_TASK
        const val COMPOSE_TASK: String = QualityGateConventionPlugin.COMPOSE_TASK

        const val COMPOSE_SOURCE: String = "package sample\n\nimport androidx.compose.runtime.Immutable\n"
    }
}
