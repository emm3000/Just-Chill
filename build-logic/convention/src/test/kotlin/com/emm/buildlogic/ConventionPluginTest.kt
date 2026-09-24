package com.emm.buildlogic

import com.emm.buildlogic.internal.BuildConventions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConventionPluginTest {

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val fixture: ConventionPluginFixture
        get() = ConventionPluginFixture(temporaryFolder.root)

    @Test
    fun `android library plugin applies the shared android configuration`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.library"))

        assertEquals("37", report["compileSdk"])
        assertEquals("28", report["minSdk"])
        assertEquals("36", report["testTargetSdk"])
        assertEquals("17", report["sourceCompatibility"])
        assertEquals("17", report["jvmTarget"])
        assertEquals("com.emm.justchill.probe", report["namespace"])
        assertEquals(COROUTINES_OPT_INS, report["optIn"])
        assertEquals(ANDROID_TEST_DEPENDENCIES, report["testDependencies"])
        assertEquals(CHECK_PLUGINS, report["plugins"])
    }

    @Test
    fun `android library plugin puts the module unit tests in the quality gate`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.library"),
            arguments = REPORT_GATE_TASKS,
        )

        assertEquals("testDebugUnitTest", report["gatedTests"])
        assertEquals(GATE_TASKS, report["gateTasks"])
    }

    @Test
    fun `a flavored application names its own release compile because compileReleaseKotlin never matches`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.application"),
            androidConfiguration = FLAVORED_APPLICATION_CONFIGURATION,
            arguments = REPORT_GATE_TASKS,
        )

        assertEquals(FLAVORED_GATE_TASKS, report["gateTasks"])
    }

    @Test
    fun `a module that sets its own namespace keeps it over the derived one`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.library"),
            androidConfiguration = """android { namespace = "com.emm.justchill.core.database" }""",
        )

        assertEquals("com.emm.justchill.core.database", report["namespace"])
    }

    @Test
    fun `android compose plugin turns compose on over the library configuration`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.compose"))

        assertEquals("true", report["compose"])
        assertEquals("28", report["minSdk"])
        assertEquals("36", report["testTargetSdk"])
        assertEquals("17", report["jvmTarget"])
        assertEquals(COMPOSE_OPT_INS, report["optIn"])
        assertEquals("testDebugUnitTest", report["gatedTests"])
    }

    @Test
    fun `android feature plugin adds the core modules to a compose library`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.feature"))

        assertEquals("true", report["compose"])
        assertEquals(":core:domain,:core:testing,:core:ui", report["projectDependencies"])
        assertTrue(report.getValue("testDependencies").split(',').contains("testing"), report.getValue("testDependencies"))
        assertTrue(
            report.getValue("implementationDependencies").split(',').containsAll(FEATURE_DEPENDENCIES),
            report.getValue("implementationDependencies"),
        )
        assertEquals("testDebugUnitTest", report["gatedTests"])
    }

    @Test
    fun `android application plugin applies the shared android and compose configuration`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.application"),
            androidConfiguration = APPLICATION_CONFIGURATION,
        )

        assertEquals("37", report["compileSdk"])
        assertEquals("28", report["minSdk"])
        assertEquals("36", report["targetSdk"])
        assertEquals("17", report["sourceCompatibility"])
        assertEquals("17", report["jvmTarget"])
        assertEquals("true", report["compose"])
        assertEquals(COMPOSE_OPT_INS, report["optIn"])
        assertEquals(ANDROID_TEST_DEPENDENCIES, report["testDependencies"])
        assertEquals(CHECK_PLUGINS, report["plugins"])
    }

    @Test
    fun `sqldelight plugin configures JustChillDatabase in the core database package with the schema snapshots and migration verification on`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.library", "justchill.sqldelight"))

        assertEquals("JustChillDatabase,com.emm.justchill.core.database,src/main/sqldelight/databases,true", report["database"])
    }

    @Test
    fun `release plugin minifies and shrinks the release unsigned and without mapping upload by default`() {
        val report: Map<String, String> = fixture.report(RELEASE_PLUGINS, APPLICATION_CONFIGURATION)

        assertEquals("true", report["releaseMinify"])
        assertEquals("true", report["releaseShrink"])
        val proguardFiles: List<String> = report.getValue("releaseProguard").split(',')
        assertTrue(proguardFiles.first().startsWith("proguard-android-optimize.txt"), proguardFiles.toString())
        assertEquals("proguard-rules.pro", proguardFiles.last())
        assertEquals("debug", report["signingConfigs"])
        assertEquals("null", report["releaseSigning"])
        assertEquals("false", report["mappingUpload"])
    }

    @Test
    fun `release plugin creates the release signing config from keystore properties without signing the build type`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = RELEASE_PLUGINS,
            androidConfiguration = APPLICATION_CONFIGURATION,
            files = mapOf("keystore.properties" to KEYSTORE_PROPERTIES),
        )

        assertEquals("debug,release", report["signingConfigs"])
        assertEquals("upload", report["releaseKeyAlias"])
        assertEquals("keys/upload.jks", report["releaseStoreFile"])
        assertEquals("null", report["releaseSigning"])
    }

    @Test
    fun `release plugin turns the mapping upload on through the gradle property`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = RELEASE_PLUGINS,
            androidConfiguration = APPLICATION_CONFIGURATION,
            arguments = listOf("-Pjustchill.crashlyticsMappingUpload=true"),
        )

        assertEquals("true", report["mappingUpload"])
    }

    @Test
    fun `version code counts commits and version name is the newest release tag ignoring other tags`() {
        val probe: ConventionPluginFixture = fixture
        probe.git("init", "-q")
        probe.git("commit", "-q", "--allow-empty", "-m", "first")
        probe.git("commit", "-q", "--allow-empty", "-m", "second")
        probe.git("tag", "v1.2.3")
        probe.git("commit", "-q", "--allow-empty", "-m", "third")
        probe.git("tag", "pre-kmp")

        val report: Map<String, String> = probe.report(RELEASE_PLUGINS, APPLICATION_CONFIGURATION)

        assertEquals("3", report["versionCode"])
        assertEquals("1.2.3", report["versionName"])
    }

    @Test
    fun `the versions come from the project repository under a leaked git environment`() {
        val outsider: File = temporaryFolder.newFolder("outsider")
        val keeper: ConventionPluginFixture = ConventionPluginFixture(outsider)
        keeper.git("init", "-q")
        keeper.git("commit", "-q", "--allow-empty", "-m", "outsider")

        val probe: ConventionPluginFixture = ConventionPluginFixture(
            projectDirectory = temporaryFolder.newFolder("probe"),
            ambientEnvironment = leakedGitEnvironment(outsider),
        )
        probe.git("init", "-q")
        probe.git("commit", "-q", "--allow-empty", "-m", "first")
        probe.git("commit", "-q", "--allow-empty", "-m", "second")
        probe.git("tag", "v1.2.3")
        probe.git("commit", "-q", "--allow-empty", "-m", "third")
        probe.git("tag", "pre-kmp")

        val report: Map<String, String> = probe.report(RELEASE_PLUGINS, APPLICATION_CONFIGURATION)

        assertEquals("3", report["versionCode"])
        assertEquals("1.2.3", report["versionName"])
        assertEquals("outsider", keeper.git("log", "--format=%s"))
    }

    @Test
    fun `outside a git repository the versions fall back to the defaults`() {
        val report: Map<String, String> = fixture.report(RELEASE_PLUGINS, APPLICATION_CONFIGURATION)

        assertEquals("1", report["versionCode"])
        assertEquals("0.0.0-dev", report["versionName"])
    }

    @Test
    fun `jvm library plugin targets java 17 and puts its tests in the quality gate`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.jvm.library"))

        assertEquals("17", report["javaToolchain"])
        assertEquals("17", report["jvmTarget"])
        assertEquals(COROUTINES_OPT_INS, report["optIn"])
        assertEquals("kotlin-test,kotlinx-coroutines-test,mockk", report["testDependencies"])
        assertEquals(CHECK_PLUGINS, report["plugins"])
        assertEquals("test", report["gatedTests"])
    }

    @Test
    fun `detekt lints every kotlin source under src with one config, no baseline and no type resolution`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.library"),
            files = PROBE_SOURCES,
        )

        assertEquals(LINTED_SOURCES, report["detektSources"])
        assertEquals("config/detekt/detekt.yml", report["detektConfig"])
        assertEquals("true", report["detektBuildUponDefaultConfig"])
        assertEquals("false", report["detektAllRules"])
        assertEquals("null", report["detektBaseline"])
        assertEquals("", report["detektClasspath"])
        assertEquals("build/reports/detekt/detekt.sarif", report["detektReports"])
        assertEquals(DETEKT_RULES, report["detektRules"])
        assertEquals("true", report["detektAutoCorrect"])
    }

    @Test
    fun `detekt adds no per-variant or per-source-set task to a flavored application or a jvm library`() {
        val application: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.application"),
            androidConfiguration = FLAVORED_APPLICATION_CONFIGURATION,
        )
        val library: Map<String, String> = ConventionPluginFixture(temporaryFolder.newFolder("jvm"))
            .report(listOf("justchill.jvm.library"))

        assertEquals("detekt", application["detektTasks"])
        assertEquals("detekt", library["detektTasks"])
    }

    @Test
    fun `detekt reports formatting instead of correcting it when CI is set`() {
        val report: Map<String, String> = ConventionPluginFixture(
            projectDirectory = temporaryFolder.root,
            ambientEnvironment = mapOf("CI" to "true"),
        ).report(listOf("justchill.jvm.library"))

        assertEquals("false", report["detektAutoCorrect"])
    }

    @Test
    fun `screenshot plugin enables the screenshot source set and gates its debug validation`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.library", "justchill.screenshot"),
            arguments = REPORT_GATE_TASKS,
            files = SCREENSHOT_PROPERTIES,
        )

        assertEquals("true", report["screenshotSourceSet"])
        assertEquals("screenshot-validation-api,ui-tooling", report["screenshotTestDependencies"])
        assertEquals(SCREENSHOT_GATE_TASKS, report["gateTasks"])
    }

    @Test
    fun `the namespace is the module path under the app prefix`() {
        assertEquals("com.emm.justchill.core.domain", BuildConventions.namespaceOf(":core:domain"))
        assertEquals("com.emm.justchill.feature.loan", BuildConventions.namespaceOf(":feature:loan"))
        assertEquals("com.emm.justchill.core.ui", BuildConventions.namespaceOf(":core:ui"))
    }

    private companion object {
        const val COROUTINES_OPT_INS: String = "kotlinx.coroutines.ExperimentalCoroutinesApi,kotlinx.coroutines.FlowPreview"

        const val COMPOSE_OPT_INS: String =
            "androidx.compose.material3.ExperimentalMaterial3Api,androidx.compose.ui.ExperimentalComposeUiApi," +
                COROUTINES_OPT_INS

        const val ANDROID_TEST_DEPENDENCIES: String = "junit,kotlin-test-junit,kotlinx-coroutines-test,mockk"

        val FEATURE_DEPENDENCIES: List<String> = listOf(
            "koin-bom",
            "koin-core",
            "koin-compose-viewmodel",
            "lifecycle-viewmodel",
            "lifecycle-viewmodel-compose",
            "navigation3-runtime",
            "kotlinx-serialization-json",
        )

        const val CHECK_PLUGINS: String = "justchill.detekt,justchill.quality.gate"

        const val DETEKT_RULES: String = "dev.detekt:detekt-rules-ktlint-wrapper,io.nlopez.compose.rules:detekt"

        val PROBE_SOURCES: Map<String, String> = mapOf(
            "probe/src/main/kotlin/Probe.kt" to "class Probe",
            "probe/src/test/kotlin/ProbeTest.kt" to "class ProbeTest",
            "probe/src/androidTest/kotlin/ProbeMigrationTest.kt" to "class ProbeMigrationTest",
            "probe/src/main/sqldelight/Probe.sq" to "SELECT 1;",
        )

        const val LINTED_SOURCES: String =
            "src/androidTest/kotlin/ProbeMigrationTest.kt,src/main/kotlin/Probe.kt,src/test/kotlin/ProbeTest.kt"

        val REPORT_GATE_TASKS: List<String> = listOf("-Pjustchill.reportGateTasks=true")

        val FLAVORED_APPLICATION_CONFIGURATION: String = """
            android {
                namespace = "com.emm.justchill.probe"

                defaultConfig {
                    applicationId = "com.emm.justchill.probe"
                }

                flavorDimensions += "environment"

                productFlavors {
                    create("dev") { dimension = "environment" }
                    create("prod") { dimension = "environment" }
                }
            }

            tasks.named("qualityGate") {
                dependsOn("compileProdReleaseKotlin")
            }
        """.trimIndent()

        const val FLAVORED_GATE_TASKS: String =
            "checkComposeFreeViewModels,checkLazyListKeys,checkModuleBoundaries," +
                "checkSqlDelightSnapshots," +
                "compileProdReleaseKotlin,detekt"

        const val GATE_TASKS: String =
            "checkComposeFreeViewModels,checkLazyListKeys,checkModuleBoundaries," +
                "checkSqlDelightSnapshots," +
                "compileDebugAndroidTestKotlin,compileReleaseKotlin,detekt,testDebugUnitTest"

        const val SCREENSHOT_GATE_TASKS: String =
            "checkComposeFreeViewModels,checkLazyListKeys,checkModuleBoundaries," +
                "checkSqlDelightSnapshots," +
                "compileDebugAndroidTestKotlin,compileReleaseKotlin,detekt,testDebugUnitTest," +
                "validateDebugScreenshotTest"

        val SCREENSHOT_PROPERTIES: Map<String, String> =
            mapOf("gradle.properties" to "android.experimental.enableScreenshotTest=true")

        val RELEASE_PLUGINS: List<String> = listOf("justchill.android.application", "justchill.android.release")

        val KEYSTORE_PROPERTIES: String = """
            keyAlias=upload
            keyPassword=key-secret
            storeFile=keys/upload.jks
            storePassword=store-secret
        """.trimIndent()

        val APPLICATION_CONFIGURATION: String = """
            android {
                namespace = "com.emm.justchill.probe"

                defaultConfig {
                    applicationId = "com.emm.justchill.probe"
                }
            }
        """.trimIndent()
    }
}
