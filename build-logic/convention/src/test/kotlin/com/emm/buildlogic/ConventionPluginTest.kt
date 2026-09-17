package com.emm.buildlogic

import com.emm.buildlogic.internal.BuildConventions
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

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
        val report: Map<String, String> = fixture.report(listOf("justchill.android.library"))

        assertEquals("testDebugUnitTest", report["gatedTests"])
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

        const val CHECK_PLUGINS: String = "justchill.detekt,justchill.quality.gate"

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
