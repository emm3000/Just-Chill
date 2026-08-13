package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val sessionStatus: Flow<SessionStatus>

    /**
     * Suspends until the underlying auth provider has finished loading any persisted session,
     * i.e. the session status has left its initial "Initializing" state.
     *
     * Sync requests rely on the provider attaching the authenticated JWT to outgoing calls. On
     * some platforms (Kotlin/Native), the session is loaded asynchronously after the client is
     * built, so a request fired before initialization completes resolves with no token and is
     * silently downgraded to an anonymous request (HTTP 403 under RLS). Awaiting initialization
     * before the first authenticated request guarantees the token is available. On platforms where
     * the session is already settled (e.g. JVM after sign-in) this returns immediately.
     */
    suspend fun awaitSessionInitialization()

    suspend fun signIn(email: String, password: String): AuthUser

    /**
     * Creates a new account and returns the [AuthUser] if a session was established immediately,
     * or null when the backend requires email confirmation and no session is available yet.
     */
    suspend fun signUp(email: String, password: String): AuthUser?

    /**
     * Signs the user out. The server-side revoke is best effort; the local clear is attempted
     * unconditionally, which is not the same as guaranteed — see *"Unconditionally is not
     * guaranteed"* below.
     *
     * supabase-kt 3.7.0's `AuthImpl.signOut` posts a `logout` request whenever a session exists —
     * for every `SignOutScope`, `LOCAL` included — and wraps that post in `catch (e: RestException)`.
     * A network-level failure is `HttpRequestException`, an `IOException`, so it is not a
     * `RestException`: it escapes the provider's own method before it clears the session, leaving
     * the device signed in. `SignOutScope.LOCAL` narrows which sessions the SERVER revokes; it does
     * not remove the round-trip or the exception it can throw.
     *
     * This app's backup pipeline uploads only while a session exists (ADR 009), so signing out is
     * the only mechanism for ceasing to upload to an account. A sign-out that can fail silently on a
     * flaky network would leave the next background snapshot going to the account the user believes
     * they left. So the revoke is attempted, ITS failure is swallowed, and the local session is then
     * cleared unconditionally — a failed revoke comes back as [SignOutResult.LocalOnly] rather than
     * thrown.
     *
     * **Unconditionally is not guaranteed.** The local clear runs OUTSIDE that swallow on purpose,
     * so it is the one failure this method lets propagate: if the session store itself breaks, the
     * caller gets an exception and no [SignOutResult] at all, because the user is still signed in
     * and has to hear about it. Returning a [SignOutResult] therefore means the device IS signed
     * out; the two values only say whether the server was successfully told. Nothing pins that
     * split today — the follow-up is named in `docs/sync/ADR009_PLAN.md`, Phase 0.
     *
     * The accepted cost: when the revoke could not be reached, the refresh token stays valid
     * remotely until it expires naturally. The alternative — leaving the device signed in because a
     * network round-trip failed — is worse: the user is actively trying to leave and the app would
     * trap them mid-sign-out.
     */
    suspend fun signOut(): SignOutResult

    /**
     * Exchanges a Google ID token for a session.
     * [rawNonce] is the un-hashed nonce whose SHA-256 digest was embedded in the token request;
     * the backend verifies the digest matches the token's nonce claim.
     */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthUser

    /**
     * Calls the remote `delete_account` RPC (removes the auth user and all remote rows), then
     * ATTEMPTS to clear the on-device session. Unlike [signOut], the local clear is not a guarantee
     * here.
     *
     * The implementation asks for the narrowest sign-out scope because the auth user no longer
     * exists server-side after the RPC — but that scope does not buy a local-only operation. It is
     * the same provider behaviour [signOut] documents: supabase-kt 3.7.0 posts `logout` whenever a
     * session exists, for every scope, and catches only `RestException`. A deleted user's JWT comes
     * back 401/403/404, which the provider ignores by design, so the ordinary case does clear. A
     * NETWORK failure does not: it is an `IOException`, it escapes the provider before the clear
     * runs, and it surfaces here as [com.emm.domain.shared.error.DomainException.NetworkUnavailable]
     * with the device still holding a session for a user that no longer exists.
     *
     * That is a known defect and it is deliberately NOT repaired with [signOut]'s
     * swallow-then-clear shape: whether a delete whose remote half already succeeded should report
     * a qualified success is its own contract question, deferred to Phase 5 of
     * `docs/sync/ADR009_PLAN.md`. And unlike [signOut], nothing pins any of this — it is read off
     * the provider's source, not off a failing test.
     */
    suspend fun deleteAccount()

    /**
     * Re-sends the sign-up confirmation link to [email].
     * Useful when the user did not receive the original confirmation email.
     */
    suspend fun resendConfirmationEmail(email: String)
}
