package com.emm.justchill.hh.auth

/**
 * Launches the platform Google sign-in flow and returns a [GoogleSignInResult].
 *
 * The implementation (Android uses Credential Manager) lives in `:androidApp`; [AuthViewModel]
 * depends only on this interface, never on the Android type.
 */
interface GoogleSignInLauncher {
    suspend fun signIn(serverClientId: String): GoogleSignInResult
}
