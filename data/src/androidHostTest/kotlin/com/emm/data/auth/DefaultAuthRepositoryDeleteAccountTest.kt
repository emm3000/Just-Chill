package com.emm.data.auth

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus

/**
 * The SupabaseClient has to be real, same reasoning as DefaultAuthRepositorySignOutTest: what these
 * tests pin is deleteAccount()'s call shape against supabase-kt's own Postgrest/Auth behaviour, not
 * a mock of this repository's own dependency.
 */
class DefaultAuthRepositoryDeleteAccountTest {

    // This is the case the whole ticket exists for: a network failure on the revoke POST used to
    // escape before clearSession() ran, leaving the device Authenticated for an account the RPC had
    // already deleted.
    @Test
    fun `deleteAccount clears the session when the RPC succeeds but the revoke POST fails`() = runTest {
        val client = clientWithSession { request ->
            if (request.url.encodedPath == LOGOUT_PATH) throw IOException("network is unreachable") else respondOk()
        }
        val repository = DefaultAuthRepository(client)

        repository.deleteAccount()

        assertTrue(client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated)
        assertNull(client.auth.currentSessionOrNull())
    }

    @Test
    fun `deleteAccount clears the session when the RPC and the revoke POST both succeed`() = runTest {
        val client = clientWithSession { respondOk() }
        val repository = DefaultAuthRepository(client)

        repository.deleteAccount()

        assertTrue(client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated)
        assertNull(client.auth.currentSessionOrNull())
    }

    /**
     * The mirror image of DefaultAuthRepositorySignOutTest's cancellation case, and it has to be:
     * signOut() must leave the session alone because the user may still own it, while here the RPC
     * already destroyed the account, so the clear cannot be skipped for any reason — cancellation
     * included.
     */
    @Test
    fun `deleteAccount clears the session even when a cancellation interrupts the revoke POST`() =
        runTest(timeout = HANG_BOUND) {
            val revokeStarted = CompletableDeferred<Unit>()
            val client = clientWithHangingRevoke { revokeStarted.complete(Unit) }
            val repository = DefaultAuthRepository(client)

            val deferred = async { repository.deleteAccount() }
            revokeStarted.await()
            deferred.cancel()

            assertFailsWith<CancellationException> {
                deferred.await()
            }
            assertTrue(
                client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated,
                "The account is already gone server-side, so a cancelled revoke must still leave " +
                    "the device signed out — not Authenticated against an account that no longer exists.",
            )
            assertNull(client.auth.currentSessionOrNull())
        }

    // The RPC itself failing must short-circuit before signOut()/clearSession() ever run: the
    // account still exists, so the session it belongs to must not be torn down.
    @Test
    fun `deleteAccount propagates a failing RPC and leaves the session untouched`() = runTest {
        val client = clientWithSession { request ->
            if (request.url.encodedPath == RPC_PATH) {
                respond(content = RPC_FAILURE_BODY, status = HttpStatusCode.InternalServerError)
            } else {
                respondOk()
            }
        }
        val repository = DefaultAuthRepository(client)

        assertFailsWith<DomainException.RemoteRejected> {
            repository.deleteAccount()
        }
        assertTrue(client.auth.sessionStatus.value is SupabaseSessionStatus.Authenticated)
    }

    private suspend fun clientWithSession(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { request -> handler(request) }
        install(Auth) { minimalConfig() }
        install(Postgrest)
    }.settled().also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    private suspend fun clientWithHangingRevoke(onRevokeStarted: () -> Unit): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        requestTimeout = DISABLED_REQUEST_TIMEOUT
        httpEngine = MockEngine { request ->
            if (request.url.encodedPath != LOGOUT_PATH) return@MockEngine respondOk()
            onRevokeStarted()
            awaitCancellation()
        }
        install(Auth) { minimalConfig() }
        install(Postgrest)
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

    private companion object {

        const val RPC_PATH = "/rest/v1/rpc/delete_account"
        const val LOGOUT_PATH = "/auth/v1/logout"
        const val RPC_FAILURE_BODY = """{"code":"XX000","message":"delete_account failed"}"""

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
