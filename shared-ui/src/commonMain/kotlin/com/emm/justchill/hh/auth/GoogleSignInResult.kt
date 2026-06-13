package com.emm.justchill.hh.auth

/**
 * Platform-agnostic outcome of a Google sign-in attempt.
 *
 * The Android-only credential machinery (Credential Manager) lives in :app and
 * maps its result into this common type so [AuthViewModel] can stay in commonMain.
 */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String, val rawNonce: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoCredentials : GoogleSignInResult
    data class Failure(val cause: Throwable) : GoogleSignInResult
}
