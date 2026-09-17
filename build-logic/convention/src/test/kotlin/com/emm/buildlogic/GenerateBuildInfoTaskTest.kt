package com.emm.buildlogic

import com.emm.buildlogic.GenerateBuildInfoTask.Companion.UNKNOWN_COMMIT
import com.emm.buildlogic.GenerateBuildInfoTask.Companion.normalizeCommitHash
import org.junit.Test
import kotlin.test.assertEquals

class GenerateBuildInfoTaskTest {

    @Test
    fun `the sentinel is the exact word the app side spells out`() {
        assertEquals("unknown", UNKNOWN_COMMIT)
    }

    @Test
    fun `a full lowercase sha passes through unchanged`() {
        assertEquals(FULL_SHA, normalizeCommitHash(FULL_SHA))
    }

    @Test
    fun `an uppercase sha is lowercased rather than rejected`() {
        assertEquals(FULL_SHA, normalizeCommitHash(FULL_SHA.uppercase()))
    }

    @Test
    fun `the trailing newline git always prints is stripped`() {
        assertEquals(FULL_SHA, normalizeCommitHash("$FULL_SHA\n"))
    }

    @Test
    fun `surrounding whitespace is stripped`() {
        assertEquals(FULL_SHA, normalizeCommitHash("  $FULL_SHA\t\r\n"))
    }

    @Test
    fun `an empty value is the sentinel`() {
        assertEquals(UNKNOWN_COMMIT, normalizeCommitHash(""))
        assertEquals(UNKNOWN_COMMIT, normalizeCommitHash("   "))
    }

    @Test
    fun `a describe --dirty suffix is rejected, not truncated`() {
        assertEquals(UNKNOWN_COMMIT, normalizeCommitHash("$FULL_SHA-dirty"))
    }

    @Test
    fun `a short sha is not a sha`() {
        assertEquals(UNKNOWN_COMMIT, normalizeCommitHash(FULL_SHA.take(7)))
    }

    @Test
    fun `characters that would break the generated string literal are rejected`() {
        val hostile = listOf(
            """${FULL_SHA.take(38)}"; val pwned: String = """",
            """${FULL_SHA.take(38)}\n""",
            "${FULL_SHA.take(30)}\$hostile",
            "${FULL_SHA.take(30)}\${System.exit(0)}",
            "$FULL_SHA\nval extra: String = \"\"",
        )

        for (raw in hostile) {
            assertEquals(
                UNKNOWN_COMMIT,
                normalizeCommitHash(raw),
                "normalizeCommitHash accepted <$raw>, which does not survive being written into a " +
                    "Kotlin string literal.",
            )
        }
    }

    private companion object {
        const val FULL_SHA = "4e47828d1f2a3b4c5d6e7f8091a2b3c4d5e6f708"
    }
}
