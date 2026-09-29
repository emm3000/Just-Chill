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
    fun `every edge ADR 015 and ADR 024 allow passes the boundary check`() {
        val modules: Map<String, String> = mapOf(
            ":androidApp" to module(":shared", ":feature:loan", ":core:ui", ":core:presentation", ":core:domain"),
            ":shared" to module(":feature:loan", ":core:backup", ":core:presentation", ":core:domain"),
            ":core:backup" to module(":core:domain"),
            ":feature:loan" to module(":core:domain", ":core:presentation", ":core:ui"),
            ":core:ui" to module(":core:domain", ":core:presentation"),
            ":core:presentation" to module(":core:domain"),
            ":core:domain" to module(),
            ":core:testing" to module(":core:domain"),
        )

        val result: BuildResult = fixture.check(task = BOUNDARY_TASK, modules = modules)

        modules.keys.forEach { path -> assertSucceeded(result, "$path:$BOUNDARY_TASK") }
    }

    @Test
    fun `core presentation that depends on core ui fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:presentation:$BOUNDARY_TASK",
            modules = mapOf(
                ":core:presentation" to module(":core:ui"),
                ":core:ui" to module(),
            ),
        )

        assertTrue(output.contains(":core:presentation depends on :core:ui"), output)
    }

    @Test
    fun `core ui that depends on anything but core domain and core presentation fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:ui:$BOUNDARY_TASK",
            modules = mapOf(
                ":core:ui" to module(":core:testing"),
                ":core:testing" to module(),
            ),
        )

        assertTrue(output.contains(":core:ui depends on :core:testing"), output)
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
    fun `a feature that takes the fixtures module as a test dependency passes the boundary check`() {
        fixture.check(
            task = ":feature:loan:$BOUNDARY_TASK",
            modules = mapOf(
                ":feature:loan" to module(testDependencies = arrayOf(":core:testing")),
                ":core:testing" to module(),
            ),
        )
    }

    @Test
    fun `a feature that ships the fixtures module in production fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$BOUNDARY_TASK",
            modules = mapOf(
                ":feature:loan" to module(":core:testing"),
                ":core:testing" to module(),
            ),
        )

        assertTrue(output.contains(":feature:loan depends on :core:testing"), output)
    }

    @Test
    fun `a kmp feature that takes the fixtures module on its test source sets passes the boundary check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$BOUNDARY_TASK",
            modules = mapOf(
                ":feature:loan" to kmpModule(KMP_TEST_SOURCE_SETS.associateWith { ":core:testing" }),
                ":core:testing" to module(),
            ),
        )

        assertSucceeded(result, ":feature:loan:$BOUNDARY_TASK")
    }

    @Test
    fun `a kmp feature that ships the fixtures module from common main fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$BOUNDARY_TASK",
            modules = mapOf(
                ":feature:loan" to kmpModule(mapOf("commonMain" to ":core:testing")),
                ":core:testing" to module(),
            ),
        )

        assertTrue(output.contains(":feature:loan depends on :core:testing"), output)
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
    fun `a module outside the three families fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":legacy:$BOUNDARY_TASK",
            modules = mapOf(":legacy" to module(":core:domain"), ":core:domain" to module()),
        )

        assertTrue(output.contains(":legacy is neither :androidApp, :shared nor a :core: or :feature: module"), output)
    }

    @Test
    fun `the shared umbrella that depends on the app fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":shared:$BOUNDARY_TASK",
            modules = mapOf(":shared" to module(":androidApp"), ":androidApp" to module()),
        )

        assertTrue(output.contains(":shared depends on :androidApp"), output)
    }

    @Test
    fun `the shared umbrella that depends on core ui fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":shared:$BOUNDARY_TASK",
            modules = mapOf(":shared" to module(":core:ui"), ":core:ui" to module()),
        )

        assertTrue(output.contains(":shared depends on :core:ui"), output)
    }

    @Test
    fun `the shared umbrella that ships the fixtures module in production fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":shared:$BOUNDARY_TASK",
            modules = mapOf(":shared" to module(":core:testing"), ":core:testing" to module()),
        )

        assertTrue(output.contains(":shared depends on :core:testing"), output)
    }

    @Test
    fun `core domain that carries an android main or ios main source fails the boundary check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:domain:$BOUNDARY_TASK",
            modules = mapOf(":core:domain" to kmpModule(emptyMap())),
            sources = mapOf(
                "core/domain/src/androidMain/kotlin/Platform.kt" to PLATFORM_SOURCE,
                "core/domain/src/iosMain/kotlin/Platform.kt" to PLATFORM_SOURCE,
            ),
        )

        assertTrue(output.contains(":core:domain carries production sources in androidMain"), output)
        assertTrue(output.contains(":core:domain carries production sources in iosMain"), output)
    }

    @Test
    fun `kotlin's swiftpm lockfile configuration listing every kmp module passes the boundary check`() {
        val result: BuildResult = fixture.check(
            task = ":core:domain:$BOUNDARY_TASK",
            modules = mapOf(
                ":core:domain" to kmpModule(emptyMap()),
                ":core:testing" to kmpModule(mapOf("commonMain" to ":core:domain")),
            ),
        )

        assertSucceeded(result, ":core:domain:$BOUNDARY_TASK")
    }

    @Test
    fun `core domain with common main and host test sources passes the boundary check`() {
        val result: BuildResult = fixture.check(
            task = ":core:domain:$BOUNDARY_TASK",
            modules = mapOf(":core:domain" to kmpModule(emptyMap())),
            sources = mapOf(
                "core/domain/src/commonMain/kotlin/Platform.kt" to PLATFORM_SOURCE,
                "core/domain/src/androidHostTest/kotlin/PlatformTest.kt" to PLATFORM_SOURCE,
            ),
        )

        assertSucceeded(result, ":core:domain:$BOUNDARY_TASK")
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

    @Test
    fun `a common main ViewModel of a kmp feature that imports compose fails the ios compile`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:compileKotlinIosSimulatorArm64",
            modules = KMP_FEATURE_MODULES,
            sources = mapOf(COMMON_VIEW_MODEL_PATH to COMPOSE_VIEW_MODEL_SOURCE),
        )

        assertTrue(output.contains("LoanViewModel.kt"), output)
        assertTrue(output.contains("Unresolved reference 'compose'"), output)
    }

    @Test
    fun `a common main ViewModel of a kmp feature that imports compose fails the compose check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$COMPOSE_TASK",
            modules = KMP_FEATURE_MODULES,
            sources = mapOf(COMMON_VIEW_MODEL_PATH to COMPOSE_VIEW_MODEL_SOURCE),
        )

        assertTrue(output.contains("src/commonMain/kotlin/LoanViewModel.kt"), output)
    }

    @Test
    fun `a migration whose snapshot was never written fails the snapshot check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module(snapshotFloor = 1)),
            sources = mapOf(
                "$MIGRATION_DIRECTORY/0.sqm" to MIGRATION_SOURCE,
                "$MIGRATION_DIRECTORY/1.sqm" to MIGRATION_SOURCE,
                "$SNAPSHOT_DIRECTORY/1.db" to SNAPSHOT_SOURCE,
            ),
        )

        assertTrue(output.contains("1.sqm has no snapshot 2.db"), output)
    }

    @Test
    fun `a migration that ships its snapshot passes the snapshot check`() {
        val result: BuildResult = fixture.check(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module(snapshotFloor = 1)),
            sources = mapOf(
                "$MIGRATION_DIRECTORY/0.sqm" to MIGRATION_SOURCE,
                "$SNAPSHOT_DIRECTORY/1.db" to SNAPSHOT_SOURCE,
            ),
        )

        assertSucceeded(result, ":core:database:$SNAPSHOT_TASK")
    }

    @Test
    fun `the current pinned shape passes the snapshot check`() {
        val migrations: Map<String, String> = (0..5).associate { version ->
            "$MIGRATION_DIRECTORY/$version.sqm" to MIGRATION_SOURCE
        }
        val snapshots: Map<String, String> = (3..6).associate { version ->
            "$SNAPSHOT_DIRECTORY/$version.db" to SNAPSHOT_SOURCE
        }

        val result: BuildResult = fixture.check(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module(snapshotFloor = 3)),
            sources = migrations + snapshots,
        )

        assertSucceeded(result, ":core:database:$SNAPSHOT_TASK")
    }

    @Test
    fun `a snapshot with no migration behind it fails the snapshot check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module(snapshotFloor = 1)),
            sources = mapOf(
                "$MIGRATION_DIRECTORY/0.sqm" to MIGRATION_SOURCE,
                "$SNAPSHOT_DIRECTORY/1.db" to SNAPSHOT_SOURCE,
                "$SNAPSHOT_DIRECTORY/3.db" to SNAPSHOT_SOURCE,
            ),
        )

        assertTrue(output.contains("3.db has no migration 2.sqm"), output)
    }

    @Test
    fun `a committed snapshot deleted above the floor fails the snapshot check`() {
        val migrations: Map<String, String> = (0..5).associate { version ->
            "$MIGRATION_DIRECTORY/$version.sqm" to MIGRATION_SOURCE
        }
        val surviving: Map<String, String> = (4..6).associate { version ->
            "$SNAPSHOT_DIRECTORY/$version.db" to SNAPSHOT_SOURCE
        }

        val output: String = fixture.checkAndFail(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module(snapshotFloor = 3)),
            sources = migrations + surviving,
        )

        assertTrue(output.contains("2.sqm has no snapshot 3.db"), output)
    }

    @Test
    fun `a deleted floor snapshot whose migration was retired fails the snapshot check`() {
        val migrations: Map<String, String> = (3..5).associate { version ->
            "$MIGRATION_DIRECTORY/$version.sqm" to MIGRATION_SOURCE
        }
        val surviving: Map<String, String> = (4..6).associate { version ->
            "$SNAPSHOT_DIRECTORY/$version.db" to SNAPSHOT_SOURCE
        }

        val output: String = fixture.checkAndFail(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module(snapshotFloor = 3)),
            sources = migrations + surviving,
        )

        assertTrue(output.contains("the pinned floor 3.db is missing"), output)
    }

    @Test
    fun `a module with migrations and no pinned floor fails the snapshot check`() {
        val output: String = fixture.checkAndFail(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module()),
            sources = mapOf(
                "$MIGRATION_DIRECTORY/0.sqm" to MIGRATION_SOURCE,
                "$SNAPSHOT_DIRECTORY/1.db" to SNAPSHOT_SOURCE,
            ),
        )

        assertTrue(output.contains("no pinned snapshot floor"), output)
    }

    @Test
    fun `a kmp module whose migration shipped no snapshot fails the snapshot check`() {
        val migrations: Map<String, String> = (0..5).associate { version ->
            "$MIGRATION_DIRECTORY/$version.sqm" to MIGRATION_SOURCE
        }
        val surviving: Map<String, String> = (3..5).associate { version ->
            "$SNAPSHOT_DIRECTORY/$version.db" to SNAPSHOT_SOURCE
        }

        val output: String = fixture.checkAndFail(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to kmpModule(emptyMap(), snapshotFloor = 3)),
            sources = migrations + surviving,
        )

        assertTrue(output.contains("5.sqm has no snapshot 6.db"), output)
    }

    @Test
    fun `a module without sqldelight passes the snapshot check`() {
        val result: BuildResult = fixture.check(
            task = ":core:database:$SNAPSHOT_TASK",
            modules = mapOf(":core:database" to module()),
        )

        assertSucceeded(result, ":core:database:$SNAPSHOT_TASK")
    }

    @Test
    fun `a key that passes a value class id fails the lazy key check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanRowUi.kt" to TYPED_ID_MODEL,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to TYPED_ID_KEY,
            ),
        )

        assertTrue(output.contains("PersonLoansScreen.kt:3"), output)
    }

    @Test
    fun `a key that passes the underlying primitive passes the lazy key check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanRowUi.kt" to TYPED_ID_MODEL,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to UNDERLYING_VALUE_KEY,
            ),
        )

        assertSucceeded(result, ":feature:loan:$LAZY_KEY_TASK")
    }

    @Test
    fun `a key that passes an id this module declares as a String passes the lazy key check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanRowUi.kt" to PRIMITIVE_ID_MODEL,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to TYPED_ID_KEY,
            ),
        )

        assertSucceeded(result, ":feature:loan:$LAZY_KEY_TASK")
    }

    @Test
    fun `a callable reference to a value class id fails the lazy key check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanRowUi.kt" to TYPED_ID_MODEL,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to TYPED_ID_REFERENCE_KEY,
            ),
        )

        assertTrue(output.contains("LoanRowUi.loanId is LoanId"), output)
    }

    @Test
    fun `a key whose property another module declares passes the lazy key check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to FOREIGN_ID_KEY,
            ),
        )

        assertSucceeded(result, ":feature:loan:$LAZY_KEY_TASK")
    }

    @Test
    fun `a value class id shadowed by a same-named String elsewhere fails the lazy key check`() {
        val output: String = fixture.checkAndFail(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanRowUi.kt" to TYPED_ID_MODEL,
                "feature/loan/src/main/kotlin/LoanDetailRoute.kt" to PRIMITIVE_ID_ROUTE,
                "feature/loan/src/main/kotlin/PersonLoansUiState.kt" to ROW_COLLECTION_STATE,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to STATE_COLLECTION_KEY,
            ),
        )

        assertTrue(output.contains("LoanRowUi.loanId is LoanId"), output)
    }

    @Test
    fun `an owner-scoped String id passes the lazy key check while a same-named value class exists`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/main/kotlin/LoanRowUi.kt" to PRIMITIVE_ID_MODEL,
                "feature/loan/src/main/kotlin/LoanCache.kt" to TYPED_ID_CACHE,
                "feature/loan/src/main/kotlin/PersonLoansUiState.kt" to ROW_COLLECTION_STATE,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to STATE_COLLECTION_KEY,
            ),
        )

        assertSucceeded(result, ":feature:loan:$LAZY_KEY_TASK")
    }

    @Test
    fun `a value class declared by a test fixture alone passes the lazy key check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = mapOf(
                "feature/loan/src/test/kotlin/LoanFixtures.kt" to TYPED_ID_FIXTURE,
                "feature/loan/src/main/kotlin/PersonLoansScreen.kt" to FIXTURE_NAMED_KEY,
            ),
        )

        assertSucceeded(result, ":feature:loan:$LAZY_KEY_TASK")
    }

    @Test
    fun `a value class declared by a kmp test source set alone passes the lazy key check`() {
        val result: BuildResult = fixture.check(
            task = ":feature:loan:$LAZY_KEY_TASK",
            modules = mapOf(":feature:loan" to module()),
            sources = KMP_TEST_SOURCE_SETS.associate { sourceSet ->
                "feature/loan/src/$sourceSet/kotlin/LoanFixtures.kt" to TYPED_ID_FIXTURE
            } + mapOf("feature/loan/src/commonMain/kotlin/PersonLoansScreen.kt" to FIXTURE_NAMED_KEY),
        )

        assertSucceeded(result, ":feature:loan:$LAZY_KEY_TASK")
    }

    private fun assertSucceeded(result: BuildResult, taskPath: String) {
        assertEquals(TaskOutcome.SUCCESS, result.task(taskPath)?.outcome, taskPath)
    }

    private fun module(
        vararg dependencies: String,
        testDependencies: Array<String> = emptyArray(),
        snapshotFloor: Int? = null,
    ): String = buildString {
        appendLine("""plugins { id("justchill.android.library") }""")
        if (snapshotFloor != null) {
            appendLine("sqlDelightSnapshots { floor.set($snapshotFloor) }")
        }
        if (dependencies.isNotEmpty() || testDependencies.isNotEmpty()) {
            appendLine("dependencies {")
            dependencies.forEach { appendLine("""    implementation(project("$it"))""") }
            testDependencies.forEach { appendLine("""    testImplementation(project("$it"))""") }
            appendLine("}")
        }
    }

    private fun kmpModule(dependencies: Map<String, String>, snapshotFloor: Int? = null): String = buildString {
        appendLine("""plugins { id("justchill.kmp.library") }""")
        if (snapshotFloor != null) {
            appendLine("sqlDelightSnapshots { floor.set($snapshotFloor) }")
        }
        appendLine("kotlin {")
        appendLine("    android { withDeviceTest {} }")
        dependencies.forEach { (sourceSet, path) ->
            appendLine("""    sourceSets.maybeCreate("$sourceSet").dependencies { implementation(project("$path")) }""")
        }
        appendLine("}")
    }

    private companion object {
        val KMP_TEST_SOURCE_SETS: List<String> =
            listOf("commonTest", "androidHostTest", "androidDeviceTest", "iosTest", "iosArm64Test", "iosSimulatorArm64Test")

        val KMP_FEATURE_MODULES: Map<String, String> = mapOf(
            ":feature:loan" to """plugins { id("justchill.kmp.feature") }""",
            ":core:domain" to """plugins { id("justchill.kmp.library") }""",
            ":core:presentation" to """plugins { id("justchill.kmp.library") }""",
            ":core:ui" to "",
            ":core:testing" to "",
        )

        const val COMMON_VIEW_MODEL_PATH: String = "feature/loan/src/commonMain/kotlin/LoanViewModel.kt"
        const val COMPOSE_VIEW_MODEL_SOURCE: String =
            "package sample\n\nimport androidx.compose.runtime.Immutable\n\n@Immutable\nclass LoanViewModel\n"

        const val BOUNDARY_TASK: String = QualityGateConventionPlugin.BOUNDARY_TASK
        const val COMPOSE_TASK: String = QualityGateConventionPlugin.COMPOSE_TASK
        const val SNAPSHOT_TASK: String = QualityGateConventionPlugin.SNAPSHOT_TASK

        const val LAZY_KEY_TASK: String = QualityGateConventionPlugin.LAZY_KEY_TASK

        const val TYPED_ID_MODEL: String =
            "package sample\n\ndata class LoanRowUi(\n    val loanId: LoanId,\n)\n"
        const val TYPED_ID_KEY: String =
            "package sample\n\nfun rows() = items(loans, key = { it.loanId }) { }\n"
        const val UNDERLYING_VALUE_KEY: String =
            "package sample\n\nfun rows() = items(loans, key = { it.loanId.value }) { }\n"
        const val PRIMITIVE_ID_MODEL: String =
            "package sample\n\ndata class LoanRowUi(\n    val loanId: String,\n)\n"
        const val TYPED_ID_REFERENCE_KEY: String =
            "package sample\n\nfun rows() = items(loans, key = LoanRowUi::loanId) { }\n"
        const val PRIMITIVE_ID_ROUTE: String =
            "package sample\n\ndata class LoanDetailRoute(val loanId: String)\n"
        const val ROW_COLLECTION_STATE: String =
            "package sample\n\ndata class PersonLoansUiState(\n    val loans: List<LoanRowUi>,\n)\n"
        const val STATE_COLLECTION_KEY: String =
            "package sample\n\nfun rows() = items(state.loans, key = { it.loanId }) { }\n"

        const val TYPED_ID_CACHE: String =
            "package sample\n\ndata class LoanCache(\n    val loanId: LoanId,\n)\n"
        const val TYPED_ID_FIXTURE: String =
            "package sample\n\ndata class LoanFixture(\n    val fixtureId: LoanId,\n)\n"
        const val FIXTURE_NAMED_KEY: String =
            "package sample\n\nfun rows() = items(loans, key = { it.fixtureId }) { }\n"

        const val FOREIGN_ID_KEY: String =
            "package sample\n\nfun rows() = items(loans, key = { it.pendingId }) { }\n"

        const val COMPOSE_SOURCE: String = "package sample\n\nimport androidx.compose.runtime.Immutable\n"
        const val PLATFORM_SOURCE: String = "package sample\n\nclass Platform\n"
        const val MIGRATION_DIRECTORY: String = "core/database/src/commonMain/sqldelight/com/sample"
        const val SNAPSHOT_DIRECTORY: String = "core/database/src/commonMain/sqldelight/databases"
        const val MIGRATION_SOURCE: String = "ALTER TABLE sample ADD COLUMN note TEXT;\n"
        const val SNAPSHOT_SOURCE: String = "SQLite format 3\n"
    }
}
