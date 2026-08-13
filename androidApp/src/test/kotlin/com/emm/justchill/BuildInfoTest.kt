package com.emm.justchill

import com.emm.justchill.hh.profile.UNKNOWN_COMMIT_HASH
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
 *
 * It asserts the SHAPE only. What the footer then MAKES of that shape — the abbreviation and the
 * "git could not answer" state — belongs to `commitHashUi()` in `:ui-android` and is asserted by
 * `CommitHashUiTest`, next to the constant that drives it.
 *
 * The fallback it compares against is `:ui-android`'s [UNKNOWN_COMMIT_HASH], the same declaration
 * `commitHashUi()` classifies, reachable because `:androidApp` depends on `:ui-android`. It used to
 * be a private literal here, a third copy of the word. Note what this pairing does and does not do:
 * it asserts that whatever the generator wrote is a shape the footer can handle, and on a machine
 * where git answers that branch is never the one taken. It is `CommitHashUiTest` that pins the
 * sentinel's spelling.
 */
class BuildInfoTest {

    @Test
    fun `commit hash is a full lowercase sha or the unknown fallback`() {
        val hash = BuildInfo.commitHash
        assertTrue(
            hash == UNKNOWN_COMMIT_HASH || hash.matches(Regex("[0-9a-f]{40}")),
            "BuildInfo.commitHash is neither a 40-char sha nor \"$UNKNOWN_COMMIT_HASH\": \"$hash\"",
        )
    }
}
