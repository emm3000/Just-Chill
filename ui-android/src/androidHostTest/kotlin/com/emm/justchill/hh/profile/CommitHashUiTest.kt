package com.emm.justchill.hh.profile

import org.junit.Test
import kotlin.test.assertEquals

/**
 * The profile footer's commit label, asserted where the abbreviation actually happens.
 *
 * This replaces a length check that lived in `:androidApp`'s `BuildInfoTest` and carried its own
 * copy of the number 7. Two modules holding the same constant meant the real one could move —
 * `SHORT_COMMIT_HASH_LENGTH` to 8 — and both suites stayed green while the fallback rendered
 * "Commit unknow" on screen. These assertions read the label the screen renders, so that mutation
 * fails here, and so does deleting the [CommitHashUi.Unavailable] branch.
 *
 * [GENERATOR_SENTINEL] is spelled out rather than read from [UNKNOWN_COMMIT_HASH] on purpose. Fed
 * the constant it is meant to pin, this test compared the classifier against its own input and
 * passed for any value — retyping [UNKNOWN_COMMIT_HASH] as "n/a" left the whole gate green while
 * the git-less build rendered "Commit unknow" again. With the word written here, that retype turns
 * `the generator's fallback is not a hash and says so in Spanish` red.
 *
 * The other direction is NOT covered: `GenerateBuildInfoTask.UNKNOWN_COMMIT` is a third copy of
 * this word, and build-logic is not on this module's compile classpath. `GenerateBuildInfoTaskTest`
 * does not close it either — it reads that constant rather than spelling it, so it agrees with
 * whatever the generator says. Editing the word there still breaks nothing here.
 *
 * `BuildInfoTest` keeps the other half: that the value the generator writes is one of the two
 * shapes classified below.
 */
class CommitHashUiTest {

    @Test
    fun `a full sha is abbreviated to the first seven characters`() {
        val ui = commitHashUi(FULL_SHA)

        assertEquals(CommitHashUi.Available(FULL_SHA), ui)
        assertEquals("Commit 4e47828", ui.label)
    }

    @Test
    fun `the generator's fallback is not a hash and says so in Spanish`() {
        val ui = commitHashUi(GENERATOR_SENTINEL)

        assertEquals(
            CommitHashUi.Unavailable,
            ui,
            "commitHashUi() no longer classifies \"$GENERATOR_SENTINEL\" — the word " +
                "GenerateBuildInfoTask writes when git cannot answer — as an absent commit. The " +
                "footer would abbreviate it: \"Commit unknow\", with a copy button.",
        )
        assertEquals("Commit no disponible", ui.label)
    }

    @Test
    fun `a blank hash is the same absence`() {
        // ProfileScreen's own default, and what a caller that never wired the injection passes.
        assertEquals(CommitHashUi.Unavailable, commitHashUi(""))
    }

    private companion object {
        const val FULL_SHA = "4e47828d1f2a3b4c5d6e7f8091a2b3c4d5e6f708"

        /** The literal `GenerateBuildInfoTask.UNKNOWN_COMMIT` writes. See the class KDoc. */
        const val GENERATOR_SENTINEL = "unknown"
    }
}
