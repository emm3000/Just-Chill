package com.emm.justchill.hh.auth

/**
 * Launches the platform Google sign-in flow and returns a [GoogleSignInResult].
 *
 * The implementation is platform-specific (Android uses Credential Manager) and lives
 * outside commonMain; [AuthViewModel] depends only on this interface.
 */
interface GoogleSignInLauncher {
    suspend fun signIn(serverClientId: String): GoogleSignInResult
}
