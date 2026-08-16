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

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseBackupObjectStoreTest {

    private val listPages: ArrayDeque<String> = ArrayDeque()

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

        assertEquals("$USER_ID/", store.ownedPrefix())
    }

    @Test
    fun `no session is a named refusal, not a guessed prefix`() = runTest {
        val store = SupabaseBackupObjectStore(clientWithoutSession())

        val failure = assertFailsWith<DomainException.Unauthorized> {
            store.ownedPrefix()
        }

        assertEquals(
            "Snapshot backup failed: nobody is signed in, so there is no prefix to store it under.",
            failure.message,
        )
    }

    @Test
    fun `a session that never finishes loading fails instead of waiting forever`() = runTest {
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
        assertEquals("/storage/v1/object/$BUCKET/$USER_ID/backup-v3.json", request.url.encodedPath)
        assertEquals("application/json", request.body.contentType.toString())
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
        val body = (request.body as TextContent).text
        assertEquals(
            """{"prefix":"$USER_ID/","limit":$PAGE,"offset":$OFFSET,"sortBy":{"column":"name","order":"asc"}}""",
            body,
        )
    }

    @Test
    fun `folder entries never reach the caller, and the names that do are relative to the prefix`() = runTest {
        listPages += page(
            fileEntry("pinned", id = null),
            fileEntry("backup-v3-2026-08-14T03-04-05Z.json", id = "object-1"),
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

    private suspend fun settled(client: SupabaseClient): SupabaseClient = client.also {
        withContext(Dispatchers.Default) { it.auth.awaitInitialization() }
    }

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

        const val PAGE = 250
        const val OFFSET = 500

        const val FULL_PAGE_OF_THREE = 3

        const val UPLOAD_RESPONSE = """{"Id":"object-id","Key":"backups/object"}"""
    }
}
