package com.emm.justchill.core

import android.content.Context
import com.emm.justchill.BuildInfo
import com.emm.justchill.core.session.KeystoreSessionManager
import io.github.jan.supabase.auth.SessionManager
import io.mockk.mockk
import org.junit.Test
import org.koin.android.ext.koin.androidContext
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

    @Test
    fun `androidPlatformModule keeps the supabase session behind the Keystore`() {
        // AppGraphKoinTest resolves a test double for SessionManager, so this binding is unguarded
        // everywhere else: swapping it back reads as a green build and a plaintext refresh token.
        val koin = koinApplication {
            androidContext(mockk<Context>(relaxed = true))
            modules(androidPlatformModule)
        }.koin

        try {
            assertIs<KeystoreSessionManager>(
                koin.get<SessionManager>(),
                "androidPlatformModule no longer stores the Supabase session encrypted.",
            )
        } finally {
            koin.close()
        }
    }
}
