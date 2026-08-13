package com.emm.justchill

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Asserts the SHAPE of the generated `BuildInfo`, never its value — the value changes on every
 * commit, including the one that would run this test.
 *
 * `BuildInfo.kt` is written by `generateBuildInfo` (build-logic) from `git rev-parse HEAD`, into
 * `build/generated/buildInfo/kotlin`. Nothing else looks at it: it compiles whatever the generator
 * emitted, detekt skips everything under `build/`, and the profile footer would happily render a
 * hash with a stray newline or a truncated prefix. This is the only place the pipeline is checked
 * end to end, and it runs on the gate through `:androidApp:testDevDebugUnitTest`.
 */
class BuildInfoTest {

    @Test
    fun `commit hash is a full lowercase sha or the unknown fallback`() {
        val hash = BuildInfo.commitHash
        assertTrue(
            hash == UNKNOWN || hash.matches(Regex("[0-9a-f]{40}")),
            "BuildInfo.commitHash is neither a 40-char sha nor \"$UNKNOWN\": \"$hash\"",
        )
    }

    @Test
    fun `short commit hash is the abbreviated form of the full one`() {
        assertEquals(BuildInfo.commitHash.take(SHORT_LENGTH), BuildInfo.shortCommitHash)
    }

    @Test
    fun `the unknown fallback survives abbreviation intact`() {
        // "unknown" is exactly SHORT_LENGTH characters, which is what stops the footer from
        // reporting a build as "unknow" when git could not answer.
        assertEquals(SHORT_LENGTH, UNKNOWN.length)
    }

    private companion object {
        const val SHORT_LENGTH = 7
        const val UNKNOWN = "unknown"
    }
}
