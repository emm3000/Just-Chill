package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val sessionStatus: Flow<SessionStatus>

    suspend fun signIn(email: String, password: String): AuthUser

    /**
     * Creates a new account and returns the [AuthUser] if a session was established immediately,
     * or null when the backend requires email confirmation and no session is available yet.
     */
    suspend fun signUp(email: String, password: String): AuthUser?

    suspend fun signOut()

    /**
     * Exchanges a Google ID token (obtained natively via Credential Manager) for a Supabase session.
     * [rawNonce] is the un-hashed nonce whose SHA-256 was embedded in the token request.
     */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String?): AuthUser

    /**
     * Calls the remote `delete_account` RPC (removes the auth user and all remote rows), then
     * clears the on-device session without a server round-trip — the auth user no longer exists
     * server-side after the RPC, so a server-side sign-out call would fail.
     */
    suspend fun deleteAccount()
}
