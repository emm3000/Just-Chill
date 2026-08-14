package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Where a snapshot actually lands, against a REAL [SupabaseClient] with a scripted transport.
 *
 * `DefaultBackupUploaderTest` runs behind [BackupObjectStore], so the `<uid>/` prefix it asserts on
 * is one its own stub invented. This file is what says the production prefix is the signed-in user's
 * id — the value every RLS policy on the `backups` bucket compares against
 * (`supabase/migrations/20260814200043_backup_storage_bucket.sql`) — and that a missing session is a
 * named refusal rather than a guessed prefix or a silent skip.
 *
 * Only [SupabaseBackupObjectStore.ownedKey] is exercised, and no request is ever sent: the other
 * three operations are a bucket away and belong to a live stack, which ADR 009 Phase 4 owns. Auth is
 * built with `minimalConfig()` for the reason `DefaultAuthRepositorySignOutTest` documents — no
 * Android lifecycle callbacks, in-memory session store, nothing to load from disk.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseBackupObjectStoreTest {

    // supabase-kt's Auth plugin builds its scope on Dispatchers.Main, absent on a JVM host test.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the key is the signed-in user's id, then the file name`() = runTest {
        val store = SupabaseBackupObjectStore(clientWithSession())

        // One segment before the name and nothing else: storage.foldername(name)[1] is what the
        // select, insert and delete policies all compare to auth.uid(), so an extra folder or a
        // missing one is a server-side refusal rather than a misfiled backup.
        assertEquals("$USER_ID/backup-v3.json", store.ownedKey("backup-v3.json"))
    }

    @Test
    fun `no session is a named refusal, not a guessed prefix`() = runTest {
        val store = SupabaseBackupObjectStore(clientWithoutSession())

        val failure = assertFailsWith<DomainException.Unauthorized> {
            store.ownedKey("backup-v3.json")
        }

        // Hard constraint 4: this is one of the six things that can go wrong in the pipeline and it
        // has to be tellable from the other five by reading the failure.
        assertEquals(
            "Snapshot backup failed: nobody is signed in, so there is no prefix to store it under.",
            failure.message,
        )
    }

    private suspend fun clientWithSession(): SupabaseClient = client().also {
        it.auth.importSession(session(), autoRefresh = false)
    }

    private fun clientWithoutSession(): SupabaseClient = client()

    /**
     * The transport answers 200 to anything, which is enough because nothing here sends a request:
     * `minimalConfig()` turns off auto-refresh and auto-load, and the session below is imported with
     * `autoRefresh = false`, so the engine exists only to satisfy the builder.
     */
    private fun client(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { respondOk() }
        install(Auth) { minimalConfig() }
    }

    private fun session(): UserSession = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600L,
        tokenType = "bearer",
        user = UserInfo(id = USER_ID, aud = "authenticated", email = "user@example.com"),
    )

    private companion object {

        const val USER_ID = "5f1a2b3c-0000-4000-8000-000000000001"
    }
}
