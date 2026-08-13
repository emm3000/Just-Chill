package com.emm.buildlogic

import com.emm.buildlogic.GenerateBuildInfoTask.Companion.UNKNOWN_COMMIT
import com.emm.buildlogic.GenerateBuildInfoTask.Companion.normalizeCommitHash
import org.junit.Test
import kotlin.test.assertEquals

/**
 * The first tests build-logic has ever had. Almost all of them exist for one function; the
 * remaining one pins the spelling of [UNKNOWN_COMMIT], whose twin lives in another Gradle build.
 *
 * [normalizeCommitHash] is the only thing standing between `git rev-parse HEAD` and a Kotlin string
 * literal written into a generated file. Every other suite in the repo runs against code that is
 * already compiled, so the rejection branch here was unreachable from all of them: a value carrying
 * a quote or a newline would have produced a `BuildInfo.kt` that does not parse, in a file no human
 * opens, and nothing would have said which change caused it.
 *
 * These run on `./gradlew qualityGate` — build-logic is an included build, so the root project
 * depends on `:build-logic:test` explicitly (see [QualityGateConventionPlugin]).
 */
class GenerateBuildInfoTaskTest {

    @Test
    fun `the sentinel is the exact word the app side spells out`() {
        // build-logic is not on the app's compile classpath, so UNKNOWN_COMMIT and :ui-android's
        // UNKNOWN_COMMIT_HASH cannot be one constant. This spells the word out rather than reading
        // it, which is what makes retyping the constant fail here instead of at runtime, in a
        // git-less build rendering a footer nobody is watching. CommitHashUiTest does the same on
        // the consuming side, via its GENERATOR_SENTINEL.
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
        // Documented in GenerateBuildInfoTask: widening the regex to admit this suffix is the one
        // change that would make the generated file unsafe again.
        assertEquals(UNKNOWN_COMMIT, normalizeCommitHash("$FULL_SHA-dirty"))
    }

    @Test
    fun `a short sha is not a sha`() {
        assertEquals(UNKNOWN_COMMIT, normalizeCommitHash(FULL_SHA.take(7)))
    }

    @Test
    fun `characters that would break the generated string literal are rejected`() {
        // Each of these, interpolated into `val commitHash: String = "$full"`, either closes the
        // literal early, starts an escape sequence, or opens a template expression.
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
