package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Where a snapshot actually lands, against a REAL [SupabaseClient] with a scripted transport.
 *
 * `DefaultBackupUploaderTest` runs behind [BackupObjectStore], so the `<uid>/` prefix it asserts on
 * is one its own stub invented. This file is what says the production prefix is the signed-in user's
 * id — the value every RLS policy on the `backups` bucket compares against
 * (`supabase/migrations/20260814200043_backup_storage_bucket.sql`) — and that a missing session is a
 * named refusal rather than a guessed prefix or a silent skip.
 *
 * It also pins the three server-contract facts the rest of the pipeline is built on and that no
 * other test could see: the bucket id, `upsert = false`, and the bare `application/json` the `.json`
 * extension derives. All three compile and pass whatever value they hold — typing `"backup"` into
 * `BACKUP_BUCKET_ID` breaks nothing anywhere else — so they are asserted off the wire, on the request
 * a real upload produced.
 *
 * Auth is built with `minimalConfig()` for the reason `DefaultAuthRepositorySignOutTest` documents —
 * no Android lifecycle callbacks, in-memory session store, nothing to load from disk.
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
    fun `the prefix is the signed-in user's id and one separator`() = runTest {
        val store = SupabaseBackupObjectStore(clientWithSession())

        // One segment before the name and nothing else: storage.foldername(name)[1] is what the
        // select, insert and delete policies all compare to auth.uid(), so an extra folder or a
        // missing one is a server-side refusal rather than a misfiled backup. The trailing separator
        // is part of the value so a caller only ever concatenates.
        assertEquals("$USER_ID/", store.ownedPrefix())
    }

    @Test
    fun `no session is a named refusal, not a guessed prefix`() = runTest {
        val store = SupabaseBackupObjectStore(clientWithoutSession())

        val failure = assertFailsWith<DomainException.Unauthorized> {
            store.ownedPrefix()
        }

        // Hard constraint 4: this is one of the eleven things that can go wrong in the pipeline and
        // it has to be tellable from the other ten by reading the failure.
        assertEquals(
            "Snapshot backup failed: nobody is signed in, so there is no prefix to store it under.",
            failure.message,
        )
    }

    @Test
    fun `a session that never finishes loading fails instead of waiting forever`() = runTest {
        // The defect this closes is not a slow backup. ADR 009 Phase 2c routes "Back up now" through
        // the `launchOp` concurrent-op guard, so a wait with no ceiling holds that guard with no
        // exception and no message — invisible, which is exactly what hard constraint 4 forbids.
        val store = SupabaseBackupObjectStore(clientWhoseSessionNeverLoads())

        val failure = assertFailsWith<DomainException.NetworkUnavailable> {
            store.ownedPrefix()
        }

        assertEquals(
            "Snapshot backup failed: the session did not finish loading within 10000ms, " +
                "so there is no prefix to store it under.",
            failure.message,
        )
    }

    @Test
    fun `an upload goes to the backups bucket, as a bare application slash json, and never overwrites`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val store = SupabaseBackupObjectStore(clientWithSession(recordingInto = requests))

        store.upload("$USER_ID/backup-v3.json", "{}".encodeToByteArray())

        val request = requests.single()
        // The bucket id: a typo in BACKUP_BUCKET_ID compiles and passes every other test in the repo,
        // because nothing else in the process ever names a bucket.
        assertEquals("/storage/v1/object/$BUCKET/$USER_ID/backup-v3.json", request.url.encodedPath)
        // The mime is compared VERBATIM by storage-api against the bucket's allowed_mime_types, so a
        // `; charset=utf-8` parameter is HTTP 415 and not a near-miss. Nothing sets this explicitly —
        // it is derived from the key's `.json`, which is why that extension is enforced.
        assertEquals("application/json", request.body.contentType.toString())
        // The bucket grants insert and delete and deliberately not update, so an upsert is refused by
        // policy. This is the header that says the client is not asking for one.
        assertEquals("false", request.headers[UPSERT_HEADER])
    }

    @Test
    fun `a key that does not end in json is refused here rather than by the server`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val store = SupabaseBackupObjectStore(clientWithSession(recordingInto = requests))

        val failure = assertFailsWith<DomainException.Unknown> {
            store.upload("$USER_ID/backup-v3.txt", "{}".encodeToByteArray())
        }

        assertTrue(failure.message.orEmpty().endsWith("$USER_ID/backup-v3.txt."), "unnamed key: ${failure.message}")
        // Not sent at all. A `.txt` key would come back as HTTP 415 invalid_mime_type, which reads
        // like the bucket is misconfigured rather than like this app named the object wrong.
        assertEquals(emptyList(), requests.toList())
    }

    private suspend fun clientWithSession(recordingInto: MutableList<HttpRequestData>? = null): SupabaseClient =
        client(recordingInto).also { it.auth.importSession(session(), autoRefresh = false) }

    /**
     * The session is settled on a REAL dispatcher before the store ever sees the client, and that is
     * not ceremony. supabase-kt leaves `sessionStatus` in `Initializing` until a coroutine on its own
     * scope moves it, and `runTest`'s virtual clock advances the moment the test scheduler has
     * nothing left to run — so an unsettled client would make [SupabaseBackupObjectStore.ownedPrefix]
     * report the timeout instead of the missing session, testing the wrong thing entirely.
     */
    private suspend fun clientWithoutSession(): SupabaseClient = client().also {
        withContext(Dispatchers.Default) { it.auth.awaitInitialization() }
    }

    /**
     * A client whose persisted session never arrives: `autoLoadFromStorage` is switched back on and
     * handed a [SessionManager] whose load suspends forever, so `sessionStatus` never leaves
     * `Initializing` and `awaitInitialization()` never returns. `runTest`'s virtual clock is what
     * makes the ten-second ceiling fire immediately rather than in ten real seconds.
     */
    private fun clientWhoseSessionNeverLoads(): SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { respondOk() }
        install(Auth) {
            minimalConfig()
            autoLoadFromStorage = true
            sessionManager = NeverLoadingSessionManager
        }
    }

    /**
     * The transport answers the one shape a Storage upload expects, and records what it was asked.
     *
     * `minimalConfig()` turns off auto-refresh and auto-load and the session below is imported with
     * `autoRefresh = false`, so the only request that ever reaches the engine is one a test made on
     * purpose.
     */
    private fun client(recordingInto: MutableList<HttpRequestData>? = null): SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { request ->
            recordingInto?.add(request)
            respond(UPLOAD_RESPONSE, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        install(Auth) { minimalConfig() }
        // The default resumable cache is backed by multiplatform-settings' no-arg factory, which has
        // no JVM host implementation and NPEs the first time `from()` builds a bucket. Nothing here
        // uploads resumably — the bucket's RLS forbids it (see the class KDoc) — so an in-memory
        // cache is exactly as much of it as this test needs to exist.
        install(Storage) { resumable { cache = MemoryResumableCache() } }
    }

    private fun session(): UserSession = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600L,
        tokenType = "bearer",
        user = UserInfo(id = USER_ID, aud = "authenticated", email = "user@example.com"),
    )

    private object NeverLoadingSessionManager : SessionManager {

        override suspend fun saveSession(session: UserSession) = Unit

        override suspend fun loadSession(): UserSession = awaitCancellation()

        override suspend fun deleteSession() = Unit
    }

    private companion object {

        const val USER_ID = "5f1a2b3c-0000-4000-8000-000000000001"
        const val SUPABASE_URL = "https://project.supabase.co"
        const val BUCKET = "backups"
        const val UPSERT_HEADER = "x-upsert"

        /** The shape storage-kt decodes an upload into. Its contents are nobody's business here. */
        const val UPLOAD_RESPONSE = """{"Id":"object-id","Key":"backups/object"}"""
    }
}
