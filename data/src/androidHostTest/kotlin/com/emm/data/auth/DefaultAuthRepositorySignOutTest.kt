package com.emm.data.auth

import com.emm.domain.auth.SignOutResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus

/**
 * What [DefaultAuthRepository.signOut] does to the LOCAL session depending on whether the
 * server-side revoke can be reached, against a REAL [SupabaseClient] over a [MockEngine]. Mocking
 * [Auth] instead would only assert what this repository calls; the finding this pins is about what
 * supabase-kt itself does — see [DefaultAuthRepository.signOut]'s KDoc — so the provider has to be
 * real.
 *
 * Three distinct transport shapes are covered on top of the happy path, because each exercises a
 * different branch of [DefaultAuthRepository.signOut]:
 * - **fails instantly** — an unreachable host, the ordinary offline case. Swallowed by the method's
 *   `catch (e: Exception)`.
 * - **hangs, and the request times out** — a connection that is accepted but never answered.
 *   supabase-kt's own request timeout fires as an ordinary `HttpRequestTimeoutException`, an
 *   `IOException`, not a [kotlinx.coroutines.CancellationException] — so this ALSO lands in
 *   `catch (e: Exception)`, not the cancellation branch. This is NOT a duplicate of the case above:
 *   `KtorSupabaseHttpClient.request` catches three things in order — `HttpRequestTimeoutException`
 *   rethrown RAW, [CancellationException] rethrown RAW, and every remaining `Exception` wrapped into
 *   `HttpRequestException`. Only the third arm produces the wrapped type, so narrowing the
 *   repository's `catch (e: Exception)` to `catch (e: HttpRequestException)` would keep the
 *   unreachable-host test green and turn this one red. That mutant is the reason it exists. The test
 *   overrides the timeout to 500ms so it costs half a second instead of supabase-kt's 10s default;
 *   that override is what is pinned, not the default.
 * - **hangs, and the coroutine itself is cancelled** — the one shape the two tests above cannot
 *   produce. This is what exercises `catch (e: CancellationException) { throw e }`: without it, a
 *   cancellation would be indistinguishable from "the revoke failed" and get swallowed into a
 *   [SignOutResult.LocalOnly] instead of propagating.
 *
 * The request timeout is therefore a per-test choice, never a shared default: see
 * [SHORT_REQUEST_TIMEOUT] and [NON_COMPETING_REQUEST_TIMEOUT].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthRepositorySignOutTest {

    // supabase-kt's Auth plugin builds its coroutine scope on Dispatchers.Main, which does not
    // exist on a JVM host test until it is set.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    @Test
    fun `signOut clears the local session and reports LocalOnly when the logout request hangs`() = runTest {
        val client = hangingClientWithSession(requestTimeout = SHORT_REQUEST_TIMEOUT)
        val repository = DefaultAuthRepository(client)

        val result = repository.signOut()

        assertEquals(SignOutResult.LocalOnly, result)
        assertTrue(
            client.auth.sessionStatus.value is SupabaseSessionStatus.NotAuthenticated,
            "A logout post that never answers must not keep the local clear from running.",
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

    /**
     * Pins the `catch (e: CancellationException) { throw e }` branch in
     * [DefaultAuthRepository.signOut] — the thing that keeps a coroutine cancellation (app process
     * teardown, a caller's own timeout, a screen leaving composition) from being mistaken for "the
     * server-side revoke failed" and swallowed into a normal [SignOutResult.LocalOnly] that also
     * clears the session.
     *
     * Neither of the two tests above can exercise that branch: the instantly-failing transport
     * throws [IOException] (not cancellation), and the hanging transport's own request timeout
     * produces [io.ktor.client.plugins.HttpRequestTimeoutException] — an ordinary [IOException]
     * subtype, not [CancellationException] either (see [hangingClientWithSession]'s KDoc). This test
     * cancels the coroutine itself while it is suspended inside the mocked request, using a
     * `CompletableDeferred` to synchronize on that exact suspension point instead of racing a real
     * delay.
     *
     * It also builds its transport with [NON_COMPETING_REQUEST_TIMEOUT], not the 500ms the
     * hanging-transport test above uses. Sharing that 500ms made this test a wall-clock race against
     * real [Dispatchers.IO] threads: ktor arms its timeout killer inside the `Send` phase, BEFORE
     * the engine handler runs, so the half second had to cover engine dispatch, the mocked handler
     * starting, [CompletableDeferred.complete], this coroutine resuming from `await()` and
     * `cancel()` executing. Whenever that overran, the timeout won, the swallow path ran instead of
     * the cancellation path, and the test failed for a reason that has nothing to do with the code
     * under test. With a timeout that does not compete, cancellation is the only thing that can end
     * the request, and the outcome no longer depends on how loaded the machine is.
     *
     * The discriminating assertion below is deliberately NOT "does `deferred.await()` throw" —
     * verified empirically (by running this exact mutation) that it is not: cancelling a coroutine's
     * own `Job` forces its completion to report as cancelled to `.await()` regardless of what the
     * coroutine body does internally afterward, so `.await()` throws [CancellationException] under
     * BOTH the correct code and the mutant below. What differs is a side effect: the correct code's
     * rethrow exits the `try` before `client.auth.clearSession()` is ever reached, so the session is
     * left untouched; the mutant swallows the exception and keeps going, and `clearSession()`'s
     * in-memory implementation (from `minimalConfig()`) does not itself check for cancellation, so it
     * runs to completion and clears the session anyway. That is the assertion this test lives or
     * dies on.
     *
     * Mutation-verified: deleting the two lines `catch (e: CancellationException) { throw e }` from
     * [DefaultAuthRepository.signOut] makes this test fail on the session-status assertion — the
     * session ends up `NotAuthenticated` instead of staying `Authenticated`.
     */
    @Test
    fun `signOut propagates CancellationException instead of swallowing it and clearing the session`() = runTest {
        val requestStarted = CompletableDeferred<Unit>()
        val client = hangingClientWithSession(
            requestTimeout = NON_COMPETING_REQUEST_TIMEOUT,
            onRequestStarted = { requestStarted.complete(Unit) },
        )
        val repository = DefaultAuthRepository(client)

        val deferred = async { repository.signOut() }
        requestStarted.await()
        deferred.cancel()

        assertFailsWith<CancellationException> {
            deferred.await()
        }
        assertTrue(
            client.auth.sessionStatus.value is SupabaseSessionStatus.Authenticated,
            "A propagated cancellation must short-circuit before clearSession() runs — the session " +
                "must be left untouched, not cleared as an unintended side effect of the swallow.",
        )
    }

    /**
     * A client holding a valid session whose transport always fails. [IOException] is what an
     * unreachable host produces.
     *
     * supabase-kt's `KtorSupabaseHttpClient.request` wraps that into `HttpRequestException` — but
     * NOT unconditionally: `HttpRequestTimeoutException` and [CancellationException] are rethrown
     * RAW by that same method, unwrapped, so only the remainder (this [IOException] included) becomes
     * `HttpRequestException`. `HttpRequestException` extends `IOException` and is NOT a
     * `RestException`, so `AuthImpl.signOut`'s `catch (e: RestException)` does not catch it — it
     * escapes that method uncaught and is what [DefaultAuthRepository.signOut]'s own
     * `catch (e: Exception)` swallows directly on the way to [SignOutResult.LocalOnly]. That swallow
     * sits INSIDE the `authCall` block, so [DefaultAuthRepository]'s `toAuthDomainException()` mapper
     * — and `DomainException.NetworkUnavailable` — are never reached on this path; they only run for
     * a throwable that escapes all the way out to `authCall`'s own catch.
     *
     * `minimalConfig()` is the library's own testing preset: in-memory session and code-verifier
     * stores, no auto-refresh, no auto-load, and — the one that matters on a JVM host test — no
     * Android lifecycle callbacks. Without that last flag `setupPlatform` reaches
     * `ProcessLifecycleOwner`, which needs a main Looper this test does not have.
     *
     * The session is then imported explicitly with `autoRefresh = false`, so the `logout` post
     * under exercise is the ONLY request any of these tests can produce.
     */
    private suspend fun offlineClientWithSession(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { throw IOException("network is unreachable") }
        install(Auth) { minimalConfig() }
    }.also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    /**
     * The same client, with a transport that accepts the request and never answers it.
     *
     * [requestTimeout] is what ends the request, absent a cancellation from outside — and it has no
     * default here on purpose, because the two callers want opposite things from it. supabase-kt
     * 3.7.0 installs ktor's `HttpTimeout` itself on every client it builds, custom engine included,
     * reading it from `SupabaseClientBuilder.requestTimeout` (default 10 seconds). Setting it here
     * only moves when the expiry lands; it does not add the mechanism. `HttpTimeout` surfaces that
     * expiry as `HttpRequestTimeoutException`, an ordinary `IOException`, never a
     * [CancellationException] — which is what makes the hanging-transport test unable to exercise
     * the cancellation branch pinned separately below.
     *
     * [onRequestStarted] fires the instant the mocked transport receives the request, i.e. right
     * before it suspends forever — the cancellation test uses it to synchronize on that exact
     * suspension point instead of racing a real delay.
     */
    private suspend fun hangingClientWithSession(
        requestTimeout: Duration,
        onRequestStarted: () -> Unit = {},
    ): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        this.requestTimeout = requestTimeout
        httpEngine = MockEngine {
            onRequestStarted()
            awaitCancellation()
        }
        install(Auth) { minimalConfig() }
    }.also { client ->
        client.auth.importSession(session(), autoRefresh = false)
    }

    /** The same client, with a transport that answers the `logout` post with an ordinary 200. */
    private suspend fun respondingClientWithSession(): SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://project.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { respondOk() }
        install(Auth) { minimalConfig() }
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

        /**
         * For the hanging-transport test, where the timeout expiry IS the mechanism under test:
         * stands in for supabase-kt's 10s default so that test costs half a second instead of ten.
         */
        val SHORT_REQUEST_TIMEOUT = 500.milliseconds

        /**
         * For the cancellation test, where the timeout must NOT be what ends the request —
         * cancellation is. That test never waits this long on the happy path, so the value is
         * bounded from both sides rather than simply made large:
         *
         * - **Above the race.** ktor arms its timeout killer in the `Send` phase, before the engine
         *   handler runs, so the window it must clear covers engine dispatch, the mocked handler,
         *   the `CompletableDeferred` handoff and `cancel()` executing on real threads.
         *   [SHORT_REQUEST_TIMEOUT] was demonstrably losable there; this is twenty times that, and
         *   it also happens to be the timeout supabase-kt 3.7.0 defaults to, so the test is not
         *   asking for anything exotic.
         * - **Below `runTest`'s own 60s timeout.** If a future change ever stops cancellation from
         *   ending the request, this expires first and the test fails on the session assertion in
         *   ten seconds. Push it past sixty and the same regression degrades into an
         *   `UncompletedCoroutinesError` that says nothing about sign-out.
         */
        val NON_COMPETING_REQUEST_TIMEOUT = 10.seconds
    }
}
