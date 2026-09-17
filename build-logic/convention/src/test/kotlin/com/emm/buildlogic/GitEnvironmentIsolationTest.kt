package com.emm.buildlogic

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitEnvironmentIsolationTest {

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `the fixture strips every inherited git variable from its child environment`() {
        val leaked: Map<String, String> = leakedEnvironment(temporaryFolder.newFolder("outsider"))
        val environment: Map<String, String> = ConventionPluginFixture(
            projectDirectory = temporaryFolder.newFolder("probe"),
            ambientEnvironment = leaked,
        ).gitEnvironment()

        leaked.keys.forEach { variable ->
            assertTrue(variable !in environment, "$variable reached the child environment")
        }
        assertTrue(environment.keys.none { it.startsWith("GIT_") }, environment.keys.toString())
        assertTrue("PATH" in environment, environment.keys.toString())
    }

    @Test
    fun `commits and tags land in the fixture repository and never in the inherited one`() {
        val outsider: File = temporaryFolder.newFolder("outsider")
        val keeper: ConventionPluginFixture = ConventionPluginFixture(outsider)
        keeper.git("init", "-q")
        keeper.git("commit", "-q", "--allow-empty", "-m", "outsider")

        val probe: ConventionPluginFixture = ConventionPluginFixture(
            projectDirectory = temporaryFolder.newFolder("probe"),
            ambientEnvironment = leakedEnvironment(outsider),
        )
        probe.git("init", "-q")
        probe.git("commit", "-q", "--allow-empty", "-m", "probe")
        probe.git("tag", "v1.2.3")

        assertEquals("outsider", keeper.git("log", "--format=%s"))
        assertEquals("", keeper.git("tag", "--list"))
        assertEquals("false", keeper.git("config", "--get", "core.bare"))
        assertEquals("probe", probe.git("log", "--format=%s"))
        assertEquals("v1.2.3", probe.git("tag", "--list"))
    }

    private fun leakedEnvironment(repository: File): Map<String, String> {
        val gitDirectory: File = File(repository, ".git")
        return mapOf(
            "GIT_DIR" to gitDirectory.absolutePath,
            "GIT_WORK_TREE" to repository.absolutePath,
            "GIT_INDEX_FILE" to File(gitDirectory, "index").absolutePath,
            "GIT_OBJECT_DIRECTORY" to File(gitDirectory, "objects").absolutePath,
            "GIT_COMMON_DIR" to gitDirectory.absolutePath,
        )
    }
}
