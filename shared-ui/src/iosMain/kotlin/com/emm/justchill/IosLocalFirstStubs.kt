package com.emm.justchill

import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.emm.justchill.hh.auth.GoogleSignInResult

// iOS-only placeholders for platform features that are deferred. Multi-device sync is now REAL
// (slice 6b: IosSyncCursorStore + IosSyncOrchestrator, wired in KoinIos.kt's iosSyncModule), so the
// former NoOpSyncCursorStore / NoOpSyncController are gone. What remains here is the Google Sign-In
// launcher placeholder (native GIDSignIn deferred post-v1).

/**
 * iOS [GoogleSignInLauncher] placeholder. Native Google Sign-In is deferred post-v1 (the iOS GIDSignIn
 * SDK is not wired), so on iOS the "Continuar con Google" button is hidden (AuthScreen's
 * showGoogleSignIn = false) and AuthViewModel is constructed with googleServerClientId = "", which
 * makes submitWithGoogle() short-circuit before ever reaching this launcher. It exists only to satisfy
 * AuthViewModel's constructor; signIn() is therefore unreachable and returns a Failure rather than
 * throwing, so even a hypothetical stray invocation degrades gracefully instead of crashing.
 * TODO post-v1: replace with a real GIDSignIn-backed launcher behind this same interface.
 */
internal class UnavailableGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun signIn(serverClientId: String): GoogleSignInResult =
        GoogleSignInResult.Failure(IllegalStateException("Google Sign-In is not available on iOS"))
}
