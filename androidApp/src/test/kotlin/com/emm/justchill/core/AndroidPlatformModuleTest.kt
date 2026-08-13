package com.emm.justchill.core

import com.emm.justchill.hh.profile.COMMIT_HASH_QUALIFIER
import org.junit.Test
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.qualifier.named
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Guards the one binding in `androidPlatformModule` that nothing else can reach.
 *
 * `AppGraphKoinTest` (`:presentation`) builds `appModules(testPlatformModule)` and cannot see this
 * module at all — `androidPlatformModule` lives here, and `:presentation` does not depend on
 * `:androidApp`. It therefore proves nothing about production wiring, which is why the
 * `commitHash` assertion it used to hold was a fixture checking itself.
 *
 * `named(COMMIT_HASH_QUALIFIER)` is resolved by `AppNavHost` in `:ui-android`, on every launch of
 * every build, before the first screen renders. Deleting the binding compiles clean, passes lint
 * and passes every other suite; it crashes the app at startup. This test is the net.
 *
 * ### Why definitions and not resolution
 *
 * This module binds the SQLDelight driver, `SharedPreferencesSettings`, the sign-in launcher and
 * `CurrentActivityHolder` — all of which need a real `Context`. Resolving the graph here would need
 * an Android runtime this JVM test does not have. Koin's declared mappings answer the only question
 * that matters ("is it bound, under this qualifier, as a String?") without constructing anything:
 * the `single { }` lambdas are never invoked.
 */
@OptIn(KoinInternalApi::class)
class AndroidPlatformModuleTest {

    @Test
    fun `androidPlatformModule declares the commit hash the nav host resolves at launch`() {
        val definition = androidPlatformModule.mappings.values
            .map { it.beanDefinition }
            .firstOrNull { it.qualifier == named(COMMIT_HASH_QUALIFIER) }

        assertNotNull(
            definition,
            "androidPlatformModule binds nothing under named(\"$COMMIT_HASH_QUALIFIER\"). AppNavHost " +
                "resolves it before the first screen renders, so the app crashes at launch.",
        )
        assertEquals(
            String::class,
            definition.primaryType,
            "named(\"$COMMIT_HASH_QUALIFIER\") is bound, but not as a String — koinInject<String> fails.",
        )
    }
}
