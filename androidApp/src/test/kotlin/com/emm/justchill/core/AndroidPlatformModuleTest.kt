package com.emm.justchill.core

import android.content.Context
import com.emm.justchill.BuildInfo
import com.emm.justchill.core.session.KeystoreSessionManager
import com.emm.justchill.core.shortcuts.ShortcutPublisher
import com.emm.justchill.hh.transaction.GetShortcutCombos
import io.github.jan.supabase.auth.SessionManager
import io.mockk.mockk
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

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
        // its only binding is here, and both consumers — EmmApp's launch sweep and the dev-flavor
        // experiences source — are Android-only.
        val koin = koinApplication { modules(androidPlatformModule) }.koin

        try {
            assertIs<DefaultDispatcher>(
                koin.get<DispatchersProvider>(),
                "androidPlatformModule no longer binds DispatchersProvider; the launch sweep cannot run.",
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
            // One instance, not two of the same class: KeystoreSessionManager serialises the launch
            // sweep against the client's own first load on a monitor it owns, and a second
            // construction behind the port would give the two paths a monitor each.
            assertSame(
                koin.get<KeystoreSessionManager>(),
                koin.get<SessionManager>(),
                "The launch sweep and the Supabase client no longer share one session manager.",
            )
        } finally {
            koin.close()
        }
    }

    @Test
    fun `androidPlatformModule binds the shortcut publisher over a real Context`() {
        // GetShortcutCombos lives in :presentation's appModules(), unreachable from this module
        // alone — a mock stands in so this test proves the binding, not the whole app graph
        // AppGraphKoinTest already owns (and cannot reach this Context-dependent single, either).
        val koin = koinApplication {
            androidContext(mockk<Context>(relaxed = true))
            modules(androidPlatformModule, module { single { mockk<GetShortcutCombos>() } })
        }.koin

        try {
            assertIs<ShortcutPublisher>(
                koin.get<ShortcutPublisher>(),
                "androidPlatformModule no longer publishes launcher shortcuts.",
            )
        } finally {
            koin.close()
        }
    }
}
