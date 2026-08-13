package com.emm.justchill.core

import com.emm.justchill.BuildInfo
import org.junit.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals

/**
 * Guards the [CommitHash] binding in `androidPlatformModule`.
 *
 * `AppGraphKoinTest` (`:presentation`) builds `appModules(testPlatformModule)` and cannot see this
 * module at all — `androidPlatformModule` lives here, and `:presentation` does not depend on
 * `:androidApp`. It therefore proves nothing about production wiring, which is why the
 * `commitHash` assertion it used to hold was a fixture checking itself.
 *
 * `CommitHash` is resolved by `AppNavHost` in `:ui-android`, on every launch of every build, before
 * the first screen renders. Deleting the binding compiles clean, passes lint and passes every other
 * suite; it crashes the app at startup. This test is the net.
 *
 * It is a net for that one binding, not for the module. `DispatchersProvider` is equally invisible
 * to `AppGraphKoinTest` and equally unasserted here; nothing below covers it.
 *
 * ### Why only this binding is resolved
 *
 * This module also binds the SQLDelight driver, `SharedPreferencesSettings`, the sign-in launcher
 * and `CurrentActivityHolder` — all of which need a real `Context`. Koin builds `single { }`
 * definitions lazily, so resolving [CommitHash] alone invokes that one lambda and leaves the
 * Context-dependent ones untouched; a `koinApplication { }` never starts the global registry
 * either. Resolving is what makes this test see what `AppNavHost` sees: it asks Koin the same
 * question, by type, rather than reading this module's own declared mappings back to itself.
 */
class AndroidPlatformModuleTest {

    @Test
    fun `androidPlatformModule binds the commit hash the nav host resolves at launch`() {
        val koin = koinApplication { modules(androidPlatformModule) }.koin

        try {
            assertEquals(
                CommitHash(BuildInfo.commitHash),
                koin.get<CommitHash>(),
                "androidPlatformModule no longer produces the generated commit hash as a CommitHash.",
            )
        } finally {
            koin.close()
        }
    }
}
