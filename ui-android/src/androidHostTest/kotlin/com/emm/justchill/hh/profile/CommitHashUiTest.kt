package com.emm.justchill.hh.profile

import org.junit.Test
import kotlin.test.assertEquals

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
        assertEquals(CommitHashUi.Unavailable, commitHashUi(""))
    }

    private companion object {
        const val FULL_SHA = "4e47828d1f2a3b4c5d6e7f8091a2b3c4d5e6f708"

        /**
         * Spelled out, never read from [UNKNOWN_COMMIT_HASH]: fed the constant it pins, this test
         * would compare the classifier against its own input and pass for any value.
         */
        const val GENERATOR_SENTINEL = "unknown"
    }
}
