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
}
