package com.emm.justchill

import com.emm.justchill.hh.profile.UNKNOWN_COMMIT_HASH
import org.junit.Test
import kotlin.test.assertTrue

class BuildInfoTest {

    @Test
    fun `commit hash is a full lowercase sha or the unknown fallback`() {
        // This is the only check that exercises `generate<Variant>BuildInfo`'s output.
        val hash = BuildInfo.commitHash
        assertTrue(
            hash == UNKNOWN_COMMIT_HASH || hash.matches(Regex("[0-9a-f]{40}")),
            "BuildInfo.commitHash is neither a 40-char sha nor \"$UNKNOWN_COMMIT_HASH\": \"$hash\"",
        )
    }
}
