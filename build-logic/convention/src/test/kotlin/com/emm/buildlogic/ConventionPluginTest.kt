package com.emm.buildlogic

import com.emm.buildlogic.internal.BuildConventions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
        assertEquals(AGGREGATE_GATE_TASKS, report["gateTasks"])
    }

    @Test
    fun `naming detekt tasks replaces the aggregates the gate runs by default`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.library"),
            androidConfiguration = """qualityGate { detektTasks.addAll("detektDebug", "detektDebugUnitTest") }""",
            arguments = REPORT_GATE_TASKS,
        )

        assertEquals(NAMED_GATE_TASKS, report["gateTasks"])
    }

    @Test
    fun `a module that sets its own namespace keeps it over the derived one`() {
        val report: Map<String, String> = fixture.report(
            pluginIds = listOf("justchill.android.library"),
            androidConfiguration = """android { namespace = "com.emm.data" }""",
        )

        assertEquals("com.emm.data", report["namespace"])
    }

    @Test
    fun `android compose plugin turns compose on over the library configuration`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.compose"))

        assertEquals("true", report["compose"])
        assertEquals("28", report["minSdk"])
        assertEquals("17", report["jvmTarget"])
        assertEquals(COMPOSE_OPT_INS, report["optIn"])
        assertEquals("testDebugUnitTest", report["gatedTests"])
    }

    @Test
    fun `android feature plugin adds the core modules to a compose library`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.feature"))

        assertEquals("true", report["compose"])
        assertEquals(":core:domain,:core:ui", report["projectDependencies"])
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
    fun `sqldelight plugin configures EmmDatabaseData in the data package with the schema snapshots and migration verification on`() {
        val report: Map<String, String> = fixture.report(listOf("justchill.android.library", "justchill.sqldelight"))

        assertEquals("EmmDatabaseData,com.emm.data,src/main/sqldelight/databases,true", report["database"])
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
    fun `the namespace is the module path under the app prefix`() {
        assertEquals("com.emm.justchill.core.domain", BuildConventions.namespaceOf(":core:domain"))
        assertEquals("com.emm.justchill.feature.loan", BuildConventions.namespaceOf(":feature:loan"))
        assertEquals("com.emm.justchill.ui.android", BuildConventions.namespaceOf(":ui-android"))
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
        )

        const val CHECK_PLUGINS: String = "justchill.detekt,justchill.quality.gate"

        val REPORT_GATE_TASKS: List<String> = listOf("-Pjustchill.reportGateTasks=true")

        const val AGGREGATE_GATE_TASKS: String =
            "checkComposeFreeViewModels,checkModuleBoundaries,compileDebugAndroidTestKotlin," +
                "detektMain,detektTest,testDebugUnitTest"

        const val NAMED_GATE_TASKS: String =
            "checkComposeFreeViewModels,checkModuleBoundaries,compileDebugAndroidTestKotlin," +
                "detektDebug,detektDebugUnitTest,testDebugUnitTest"

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
