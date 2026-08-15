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
import io.ktor.http.content.TextContent
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
 * It also pins the server-contract facts the rest of the pipeline is built on and that no other test
 * could see: the bucket id, `upsert = false`, the bare `application/json` the `.json` extension
 * derives, and the list request's limit, offset and sort order. All of them compile and pass whatever
 * value they hold — typing `"backup"` into `BACKUP_BUCKET_ID` breaks nothing anywhere else — so they
 * are asserted off the wire, on the requests a real upload and a real listing produced.
 *
 * **What a host test cannot reach, and it is worth being explicit**: whether the server honours the
 * sort order, whether it clamps the limit, and whether the `/` delimiter really makes `pinned` a
 * null-id entry. Those are server behaviours; what is pinned here is the request this client sends
 * and the decoding it does with the answer. The response shapes below are written from storage-kt's
 * `FileObject`, so a BOM bump that changed that type is a compile failure rather than a silent one —
 * but a BOM bump that changed the *route's* semantics would still pass.
 *
 * Auth is built with `minimalConfig()` for the reason `DefaultAuthRepositorySignOutTest` documents —
 * no Android lifecycle callbacks, in-memory session store, nothing to load from disk.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseBackupObjectStoreTest {

    /**
     * What the scripted transport answers the next list request with, in order.
     *
     * A listing is the one operation here whose response shape carries meaning — a folder entry is a
     * `FileObject` with a null id, and that null is what the store filters on — so these tests script
     * the body rather than the blanket upload response every other request gets.
     */
    private val listPages: ArrayDeque<String> = ArrayDeque()

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

        // Hard constraint 4: this is one of the twelve things that can go wrong in the pipeline and
        // it has to be tellable from the other eleven by reading the failure.
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

    @Test
    fun `a listing asks the V1 list route for the page it was told to, sorted by name ascending`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        listPages += page(fileEntry("backup-v3-2026-08-14T03-04-05Z.json", id = "object-1"))
        val store = SupabaseBackupObjectStore(clientWithSession(recordingInto = requests))

        store.list("$USER_ID/", limit = PAGE, offset = OFFSET)

        val request = requests.single()
        assertEquals("/storage/v1/object/list/$BUCKET", request.url.encodedPath)
        // Every one of these is a field storage-kt sends ONLY because the filter block set it: an
        // unset limit is whatever the Storage API defaults to that day, and the pager's offset
        // arithmetic is built on knowing the page size it asked for.
        val body = (request.body as TextContent).text
        assertEquals(
            """{"prefix":"$USER_ID/","limit":$PAGE,"offset":$OFFSET,"sortBy":{"column":"name","order":"asc"}}""",
            body,
        )
    }

    @Test
    fun `folder entries never reach the caller, and the names that do are relative to the prefix`() = runTest {
        // Supabase derives folders from the `/` delimiter and returns them as a FileObject whose
        // every field but the name is null — `pinned` is the one ADR 009 mandates. A folder cannot be
        // deleted as an object and the prune must never scan under it.
        listPages += page(
            fileEntry("pinned", id = null),
            fileEntry("backup-v3-2026-08-14T03-04-05Z.json", id = "object-1"),
            // Relative is what the V1 route answers with; stripping is the guard for the day it is
            // not, since the caller turns every name back into a key with `prefix + name`.
            fileEntry("$USER_ID/backup-v3-2026-08-13T03-04-05Z.json", id = "object-2"),
        )
        val store = SupabaseBackupObjectStore(clientWithSession())

        val listing = store.list("$USER_ID/", limit = PAGE, offset = 0)

        assertEquals(
            listOf("backup-v3-2026-08-14T03-04-05Z.json", "backup-v3-2026-08-13T03-04-05Z.json"),
            listing.names,
        )
    }

    @Test
    fun `a page full only because of a folder entry still reports the server's own count`() = runTest {
        // The regression this type exists for. Filtering happens here, so `names.size` comes back
        // under the limit for a page that was in fact full — and a caller that paged on THAT number
        // would read a truncated listing as a complete one. A cut can fall between a payload and its
        // sidecar, and an orphan payload is deleted on sight: one verified snapshot gone, silently.
        listPages += page(
            fileEntry("pinned", id = null),
            fileEntry("backup-v3-2026-08-14T03-04-05Z.json", id = "object-1"),
            fileEntry("backup-v3-2026-08-13T03-04-05Z.json", id = "object-2"),
        )
        val store = SupabaseBackupObjectStore(clientWithSession())

        val listing = store.list("$USER_ID/", limit = FULL_PAGE_OF_THREE, offset = 0)

        assertEquals(FULL_PAGE_OF_THREE, listing.serverReturned)
        assertEquals(FULL_PAGE_OF_THREE - 1, listing.names.size)
    }

    @Test
    fun `a short page reports fewer than the limit, which is what proves the listing ended`() = runTest {
        listPages += page(fileEntry("backup-v3-2026-08-14T03-04-05Z.json", id = "object-1"))
        val store = SupabaseBackupObjectStore(clientWithSession())

        val listing = store.list("$USER_ID/", limit = FULL_PAGE_OF_THREE, offset = 0)

        assertEquals(1, listing.serverReturned)
    }

    private suspend fun clientWithSession(recordingInto: MutableList<HttpRequestData>? = null): SupabaseClient =
        settled(client(recordingInto)).also { it.auth.importSession(session(), autoRefresh = false) }

    private suspend fun clientWithoutSession(): SupabaseClient = settled(client())

    /**
     * Waits, on a REAL dispatcher, for the Auth plugin to finish initializing. Both helpers above go
     * through it, and it is not ceremony in either.
     *
     * `Auth`'s own `init` moves `sessionStatus` out of `Initializing` from coroutines it launches on
     * its own scope — which these tests pin to [Dispatchers.Main], a standalone
     * [UnconfinedTestDispatcher] whose scheduler `runTest` never drives. Nothing here can advance it,
     * so the settle has to happen on a dispatcher backed by real threads.
     *
     * It buys a different thing on each side, and both are load-bearing:
     *
     * - **without a session**, an unsettled client is still `Initializing`, so
     *   [SupabaseBackupObjectStore.ownedPrefix] reports its ten-second ceiling instead of the missing
     *   session — a green test asserting the wrong failure.
     * - **with a session**, `importSession` into a plugin that has not finished initializing may race
     *   `init`'s own status write, and the write can land last: `awaitInitialization()` then returns
     *   with `currentUserOrNull()` null and the imported session gone. That is a red this suite hit
     *   once — `Unauthorized` from the prefix test, on a cold `--rerun-tasks` gate under heavy
     *   machine load — and never reproduced since, across 15 more cold gate runs, 15 runs of this
     *   class under 9 busy-loop CPU hogs, and a scheduler probe, all green. The gate itself is
     *   sequential, so the load source is real JVM thread scheduling, not Gradle task concurrency.
     *   Settling first demonstrably removes an ordering ambiguity between `Auth.init` and
     *   `importSession`; the diagnosis beyond that is NOT confirmed — two sub-mechanisms both fit the
     *   one failure seen, and the settle closes both. See "A test that pins a real `SupabaseClient`"
     *   in `docs/sync/ADR009_PLAN.md` for the two candidates and the observable that would tell them
     *   apart. Keep the settle regardless: it is cheap, and dropping it as ceremony would reopen a
     *   failure mode that has already fired once.
     */
    private suspend fun settled(client: SupabaseClient): SupabaseClient = client.also {
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
            val body = if (request.url.encodedPath.startsWith(LIST_PATH)) listPages.removeFirst() else UPLOAD_RESPONSE
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
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

    /**
     * One entry of a list response, in the shape storage-kt's `FileObject` decodes.
     *
     * None of its six fields carries a default, so all six have to be present — and a **null [id] is
     * what makes an entry a folder**, which is the fact the store's filter rests on and the reason
     * these bodies are written out rather than built from a library type.
     */
    private fun fileEntry(name: String, id: String?): String {
        val idJson = id?.let { "\"$it\"" } ?: "null"
        return """{"name":"$name","id":$idJson,"updated_at":null,""" +
            """"created_at":null,"last_accessed_at":null,"metadata":null}"""
    }

    private fun page(vararg entries: String): String =
        entries.joinToString(separator = ",", prefix = "[", postfix = "]")

    private companion object {

        const val USER_ID = "5f1a2b3c-0000-4000-8000-000000000001"
        const val SUPABASE_URL = "https://project.supabase.co"
        const val BUCKET = "backups"
        const val UPSERT_HEADER = "x-upsert"
        const val LIST_PATH = "/storage/v1/object/list/"

        /** An arbitrary page size and offset: what matters is that they arrive on the wire unchanged. */
        const val PAGE = 250
        const val OFFSET = 500

        /** Small enough to write out, so "the page came back full" is three entries rather than a thousand. */
        const val FULL_PAGE_OF_THREE = 3

        /** The shape storage-kt decodes an upload into. Its contents are nobody's business here. */
        const val UPLOAD_RESPONSE = """{"Id":"object-id","Key":"backups/object"}"""
    }
}
