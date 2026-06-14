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

    suspend fun signOut()

    /**
     * Exchanges a Google ID token for a session.
     * [rawNonce] is the un-hashed nonce whose SHA-256 digest was embedded in the token request;
     * the backend verifies the digest matches the token's nonce claim.
     */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthUser

    /**
     * Calls the remote `delete_account` RPC (removes the auth user and all remote rows), then
     * clears the on-device session without a server round-trip — the auth user no longer exists
     * server-side after the RPC, so a server-side sign-out call would fail.
     */
    suspend fun deleteAccount()

    /**
     * Re-sends the sign-up confirmation link to [email].
     * Useful when the user did not receive the original confirmation email.
     */
    suspend fun resendConfirmationEmail(email: String)
}
