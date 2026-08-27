package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val sessionStatus: Flow<SessionStatus>

    suspend fun awaitSessionInitialization()

    suspend fun signIn(email: String, password: String): AuthUser

    suspend fun signUp(email: String, password: String): AuthUser?

    // The local clear runs outside the swallow covering the server revoke: returning a SignOutResult
    // means the device IS signed out, and a broken session store throws instead of returning one.
    suspend fun signOut(): SignOutResult

    suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthUser

    suspend fun deleteAccount()

    suspend fun resendConfirmationEmail(email: String)
}
