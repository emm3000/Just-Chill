package com.emm.justchill.hh.auth

/**
 * Platform-agnostic outcome of a Google sign-in attempt.
 *
 * The Android-only credential machinery (Credential Manager) lives in `:androidApp` and maps its
 * result into this type so [AuthViewModel] never depends on an Android credential type.
 */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String, val rawNonce: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoCredentials : GoogleSignInResult
    data class Failure(val cause: Throwable) : GoogleSignInResult
}
