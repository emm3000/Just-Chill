package com.emm.data.auth

import com.emm.domain.auth.SignOutResult
import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus

/**
 * The SupabaseClient has to be real: what these tests pin is supabase-kt's own behaviour, and a
 * mocked Auth would only assert what this repository calls.
 */
class DefaultAuthRepositorySignOutTest {

    @Test
    fun `signOut clears the local session and reports LocalOnly when the network is unreachable`() = runTest {
        val client = offlineClientWithSession()
        val repository = DefaultAuthRepository(client)

        val result = repository.signOut()

        assertEquals(SignOutResult.LocalOnly, result)
        assertTrue(
            client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated,
            "The local clear runs unconditionally — it must happen whether or not the server-side " +
                "revoke succeeded.",
        )
        assertNull(client.auth.currentSessionOrNull())
    }

    // Not a duplicate of the unreachable-host case: `KtorSupabaseHttpClient.request` rethrows
    // HttpRequestTimeoutException raw and wraps every other exception, so this is the only case
    // that turns red if the repository narrows its `catch (e: Exception)` to HttpRequestException.
    @Test
    fun `signOut clears the local session and reports LocalOnly when the logout request times out`() = runTest {
        val client = timedOutClientWithSession()
        val repository = DefaultAuthRepository(client)

        val result = repository.signOut()

        assertEquals(SignOutResult.LocalOnly, result)
        assertTrue(
            client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated,
            "A logout post that times out must not keep the local clear from running.",
        )
        assertNull(client.auth.currentSessionOrNull())
    }

    @Test
    fun `signOut reports Revoked and clears the session when the server-side revoke succeeds`() = runTest {
        val client = respondingClientWithSession()
        val repository = DefaultAuthRepository(client)

        val result = repository.signOut()

        assertEquals(SignOutResult.Revoked, result)
        assertTrue(client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated)
        assertNull(client.auth.currentSessionOrNull())
    }

    // `deferred.await()` throws CancellationException whether or not signOut rethrows it, so the
    // session-status assertion is the only discriminating one here.
    @Test
    fun `signOut propagates CancellationException instead of swallowing it and clearing the session`() =
        runTest(timeout = HANG_BOUND) {
            val requestStarted = CompletableDeferred<Unit>()
            val client = hangingClientWithSession(onRequestStarted = { requestStarted.complete(Unit) })
            val repository = DefaultAuthRepository(client)

            val deferred = async { repository.signOut() }
            requestStarted.await()
            deferred.cancel()

            assertFailsWith<CancellationException> {
                deferred.await()
            }
            assertTrue(
                client.auth.sessionStatus.value is SupabaseSessionStatus.Authenticated,
                "A propagated cancellation must short-circuit before clearSession() runs — the " +
                    "session must be left untouched, not cleared as an unintended side effect of " +
                    "the swallow.",
            )
        }

    @Test
    fun `signOut propagates a failing local clear instead of reporting LocalOnly`() = runTest {
        val client = respondingClientWithFailingLocalClear()
        val repository = DefaultAuthRepository(client)

        assertFailsWith<DomainException.Unknown> {
            repository.signOut()
        }
        assertTrue(
            client.auth.sessionStatus.value is SupabaseSessionStatus.Authenticated,
            "The failure must come from deleteSession() itself, not an unrelated throw: had " +
                "clearSession() completed despite it, the session would show NotAuthenticated here.",
        )
    }

    private suspend fun offlineClientWithSession(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { throw IOException("network is unreachable") }
        install(Auth) { minimalConfig() }
    }.settled().also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    /**
     * Raises the exception directly instead of installing a real ktor timeout and waiting for it,
     * so the outcome never races the wall clock.
     */
    private suspend fun timedOutClientWithSession(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { request -> throw HttpRequestTimeoutException(request) }
        install(Auth) { minimalConfig() }
    }.settled().also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    private suspend fun hangingClientWithSession(onRequestStarted: () -> Unit): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        requestTimeout = DISABLED_REQUEST_TIMEOUT
        httpEngine = MockEngine {
            onRequestStarted()
            awaitCancellation()
        }
        install(Auth) { minimalConfig() }
    }.settled().also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    private suspend fun respondingClientWithSession(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { respondOk() }
        install(Auth) { minimalConfig() }
    }.settled().also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    /**
     * The server-side revoke succeeds so the local clear is the only thing that can fail — the
     * override has to run after `minimalConfig()`, which installs its own in-memory manager first.
     */
    private suspend fun respondingClientWithFailingLocalClear(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { respondOk() }
        install(Auth) {
            minimalConfig()
            sessionManager = SessionManagerWithFailingDelete()
        }
    }.settled().also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    /**
     * Auth.init() flips Initializing to NotAuthenticated from its own scope on the client's default
     * dispatcher, and the check is not atomic: an importSession() that lands between that read and
     * its write is overwritten, and every test here then runs on a session that is gone.
     */
    private suspend fun SupabaseClient.settled(): SupabaseClient = also { it.auth.awaitInitialization() }

    private fun session(): UserSession = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600L,
        tokenType = "bearer",
        user = UserInfo(id = "user-1", aud = "authenticated", email = "user@example.com"),
    )

    /**
     * `saveSession`/`loadSession` delegate to a real in-memory manager so `importSession()` still
     * works; only `deleteSession()` fails, to isolate the local clear as the one broken step.
     */
    private class SessionManagerWithFailingDelete : SessionManager {

        private val delegate = MemorySessionManager()

        override suspend fun saveSession(session: UserSession) = delegate.saveSession(session)

        override suspend fun loadSession(): UserSession = delegate.loadSession()

        override suspend fun deleteSession(): Unit = error("local session deletion failed")
    }

    private companion object {

        /**
         * INFINITE is the one value for which ktor launches no timeout coroutine at all. Any finite
         * one competes with the cancellation under test and wins under load.
         */
        val DISABLED_REQUEST_TIMEOUT = Duration.INFINITE

        /**
         * A real clock covering the whole test body, so load still beats it. It buys an
         * unambiguous failure — a leaked coroutine, not a session-status assertion blaming a
         * cancellation defect that never happened — never determinism.
         */
        val HANG_BOUND = 10.seconds
    }
}
