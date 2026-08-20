package com.emm.justchill.core

import com.emm.justchill.BuildInfo
import org.junit.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AndroidPlatformModuleTest {

    @Test
    fun `androidPlatformModule binds the commit hash the nav host resolves at launch`() {
        // Koin's `single { }` bindings resolve lazily, so asking only for CommitHash never touches
        // the Context-dependent ones (SQLDelight driver, SharedPreferencesSettings, sign-in launcher).
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

    @Test
    fun `androidPlatformModule binds the dispatchers provider no shared module carries`() {
        // `DispatchersProvider` lives outside `appModules()`, so `AppGraphKoinTest` cannot see it:
        // its only binding is here, and its only consumer is the dev-flavor experiences source.
        val koin = koinApplication { modules(androidPlatformModule) }.koin

        try {
            assertIs<DefaultDispatcher>(
                koin.get<DispatchersProvider>(),
                "androidPlatformModule no longer binds DispatchersProvider; the dev flavor cannot start.",
            )
        } finally {
            koin.close()
        }
    }
}
