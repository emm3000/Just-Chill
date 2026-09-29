package com.emm.justchill.core

import android.content.Context
import com.emm.justchill.BuildInfo
import com.emm.justchill.core.session.KeystoreSessionManager
import com.emm.justchill.core.shortcuts.ShortcutPublisher
import com.emm.justchill.feature.transaction.capture.GetSpendShortcutCombos
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
    fun `androidPlatformModule keeps the supabase session behind the Keystore`() {
        val koin = koinApplication {
            androidContext(mockk<Context>(relaxed = true))
            modules(androidPlatformModule)
        }.koin

        try {
            assertIs<KeystoreSessionManager>(
                koin.get<SessionManager>(),
                "androidPlatformModule no longer stores the Supabase session encrypted.",
            )
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
    fun `androidPlatformModule binds the shortcut publisher`() {
        val koin = koinApplication {
            androidContext(mockk<Context>(relaxed = true))
            modules(androidPlatformModule, module { single { mockk<GetSpendShortcutCombos>() } })
        }.koin

        try {
            koin.get<ShortcutPublisher>()
        } finally {
            koin.close()
        }
    }
}
