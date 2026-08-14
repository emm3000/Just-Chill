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
 * The request timeout is therefore a per-test choice, never a shared default, and the two choices
 * are opposites: see [SHORT_REQUEST_TIMEOUT] and [DISABLED_REQUEST_TIMEOUT].
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
     * It builds its transport with [DISABLED_REQUEST_TIMEOUT] — no request timeout whatsoever, so
     * cancellation is the only thing in existence that can end the request. Sharing the 500ms the
     * hanging test uses, or any other finite value, makes the timer a competitor of the mechanism
     * under test: whenever it wins, the swallow path runs instead of the cancellation path and the
     * test fails on an assertion that has nothing to do with the code under test. Two finite values
     * were tried and both lost; [DISABLED_REQUEST_TIMEOUT] records what they were and why the answer
     * is not a third one. The test is instead bounded at [HANG_BOUND] — which is still a wall clock,
     * and is deliberately the only one left: read its KDoc for what that does and does not buy.
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
    fun `signOut propagates CancellationException instead of swallowing it and clearing the session`() =
        runTest(timeout = HANG_BOUND) {
            val requestStarted = CompletableDeferred<Unit>()
            val client = hangingClientWithSession(
                requestTimeout = DISABLED_REQUEST_TIMEOUT,
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
                "A propagated cancellation must short-circuit before clearSession() runs — the " +
                    "session must be left untouched, not cleared as an unintended side effect of " +
                    "the swallow.",
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
         *
         * This one is a real, small, finite timeout and must stay that way. It is not covered by
         * [DISABLED_REQUEST_TIMEOUT]'s reasoning below — there the expiry is the bug; here it is the
         * subject.
         */
        val SHORT_REQUEST_TIMEOUT = 500.milliseconds

        /**
         * For the cancellation test: **no request timeout at all**, not a large one.
         *
         * `Duration.INFINITE.inWholeMilliseconds` is `Long.MAX_VALUE`, supabase-kt passes it straight
         * through as ktor's `requestTimeoutMillis`
         * (`KtorSupabaseHttpClient.applyDefaultConfiguration`), and that value is exactly
         * `HttpTimeoutConfig.INFINITE_TIMEOUT_MS`. ktor's `applyRequestTimeout` opens with
         * `if (requestTimeout == null || requestTimeout == INFINITE_TIMEOUT_MS) return`, so the
         * timeout-killer coroutine is **never launched**. There is no timer to lose to. That is the
         * difference between removing this race and resizing it, and resizing it is what failed
         * twice already.
         *
         * ### Why a number was tried, and why every number is wrong
         *
         * The killer's `delay` runs on real time, not `runTest`'s virtual clock — verified, not
         * assumed: at one hour this test passed 12/12 under load, where 10 seconds did not. So the
         * question for any finite value is only *how loaded a machine has to be to beat it*, and that
         * question has no safe answer on CI.
         *
         * The window the value must clear is bigger than it looks: ktor arms the killer in the `Send`
         * phase, BEFORE the engine handler runs, so it has to cover engine dispatch, the mocked
         * handler starting, `CompletableDeferred.complete`, the test coroutine resuming from
         * `await()` and `cancel()` executing — all on real threads.
         *
         *  - **500ms** (shared with the hanging test) lost routinely. Phase 0 moved it to 5 minutes.
         *  - **5 minutes** was unreachable, but exceeded `runTest`'s 60s default, so a genuine
         *    regression degraded from a clean assertion failure into `UncompletedCoroutinesError`.
         *  - **10 seconds** was the attempt to get both. It lost: measured at **1 failure in 12 runs**
         *    under 20 competing CPU-bound processes on a 10-core machine — i.e. exactly when the gate
         *    runs it. An intermittent red is worse than a poor failure message, because the first
         *    thing anyone does with one is re-run it and the second is stop believing it.
         *
         * **Do not put a number back here.** The fast-failure half of that trade is bought
         * separately by [HANG_BOUND] on the test itself, which is where a bound on how long a test
         * may run belongs — it does not have to be smuggled in as a network timeout that competes
         * with the very mechanism under test. It is not bought for free, and [HANG_BOUND] says what
         * it costs.
         */
        val DISABLED_REQUEST_TIMEOUT = Duration.INFINITE

        /**
         * Bounds the cancellation test itself, replacing `runTest`'s 60s default.
         *
         * With no request timeout, a regression that stops cancellation from ending the request would
         * hang instead of failing, so a bound has to exist somewhere. This is that bound, and it is a
         * ceiling on a test that normally finishes in milliseconds.
         *
         * ### What it is, measured — not what it would be convenient for it to be
         *
         * **It is a real clock, and it covers the whole test body.** Verified against
         * kotlinx-coroutines-test 1.11.0, the version this build resolves: `TestScope.runTest` runs
         * its body inside `withTimeout(timeout)` (`commonMain/TestBuilders.kt:338`), and on the JVM
         * the surrounding `createTestResult` is a plain `runBlocking`
         * (`jvmMain/TestBuildersJvm.kt:9-13`). `runBlocking`'s context carries the default `Delay`,
         * not the `TestScope`'s virtual `testScheduler`, so the deadline elapses in wall-clock time.
         * The body is started `UNDISPATCHED` but parks on a `yield()` before any of its code runs
         * (`TestBuilders.kt:312-315`) precisely so the timeout is armed first — which means the
         * window opens at `createSupabaseClient`, not at the request.
         *
         * **So it did not remove the race the 10s request timeout lost; it widened the interval.**
         * ktor arms a request timeout inside the `Send` phase
         * (`HttpTimeout.kt:152` `on(Send)`, `:166` `applyRequestTimeout`), so the 10 seconds that
         * measured 1 failure in 12 runs under load covered a strict SUBINTERVAL of what these 10
         * seconds cover. Every load condition that beat the old timer beats this bound too, and
         * marginally sooner.
         *
         * **What it does buy, and it is the whole of it: the failure is unambiguous.** When the old
         * timer won, `HttpRequestTimeoutException` ended the request, the repository swallowed it,
         * the session was cleared and the test failed on the session-status assertion — reporting a
         * cancellation defect that had not happened. When this bound wins, the test fails with
         * `UncompletedCoroutinesError` naming the leaked coroutine, which cannot be mistaken for a
         * verdict about `signOut`. The race moved off the mechanism under test; it is not gone.
         *
         * Removing it outright would need the whole test to run on virtual time, and it cannot: a
         * real `SupabaseClient` over a real ktor `MockEngine` is this suite's premise (see the class
         * KDoc), and both dispatch on real threads.
         */
        val HANG_BOUND = 10.seconds
    }
}
