package com.emm.justchill

import org.junit.Test
import kotlin.test.assertTrue

/**
 * Asserts the SHAPE of the generated `BuildInfo`, never its value — the value changes on every
 * commit, including the one that would run this test.
 *
 * `BuildInfo.kt` is written by `generate<Variant>BuildInfo` (build-logic) from `git rev-parse HEAD`.
 * Nothing else looks at it: the build compiles whatever the generator emitted, detekt skips
 * everything under `build/`, and the profile footer would happily render a hash with a stray
 * newline. This is the only place the pipeline is checked end to end, and it runs on the gate
 * through `:androidApp:testDevDebugUnitTest`.
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
    fun `the hash is long enough for the footer to abbreviate`() {
        // ProfileScreen renders commitHash.take(7). `take` truncates silently, so a value shorter
        // than that produces a short label rather than a failure — this is the assertion that
        // stands behind it. It holds for a 40-char sha and for the "unknown" fallback, which is
        // exactly seven characters for this reason.
        assertTrue(
            BuildInfo.commitHash.length >= SHORT_LENGTH,
            "The footer abbreviates to $SHORT_LENGTH chars, but BuildInfo.commitHash is " +
                "${BuildInfo.commitHash.length} long: \"${BuildInfo.commitHash}\"",
        )
    }

    private companion object {
        const val SHORT_LENGTH = 7
        const val UNKNOWN = "unknown"
    }
}
