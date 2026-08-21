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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus

/**
 * The SupabaseClient has to be real, same reasoning as DefaultAuthRepositorySignOutTest: what these
 * tests pin is deleteAccount()'s call shape against supabase-kt's own Postgrest/Auth behaviour, not
 * a mock of this repository's own dependency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthRepositoryDeleteAccountTest {

    // The Auth plugin builds its coroutine scope on Dispatchers.Main, which does not exist on a JVM
    // host test until it is set.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    // The RPC itself failing must short-circuit before signOut()/clearSession() ever run: the
    // account still exists, so the session it belongs to must not be torn down.
    @Test
    fun `deleteAccount propagates a failing RPC and leaves the session untouched`() = runTest {
        val client = clientWithSession { request ->
            if (request.url.encodedPath == RPC_PATH) {
                respond(content = "", status = HttpStatusCode.InternalServerError)
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
    }.also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

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
    }
}
