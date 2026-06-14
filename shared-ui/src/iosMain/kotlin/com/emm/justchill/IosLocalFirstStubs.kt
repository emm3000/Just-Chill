package com.emm.justchill

import com.emm.domain.sync.SyncCursorStore
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncStatus
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.emm.justchill.hh.auth.GoogleSignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Phase 6a: iOS gains REAL email/password auth + claim-on-sign-in (Supabase-backed, wired in
// KoinIos.kt). What remains stubbed here is the multi-device SYNC engine — that is phase 6b.
// AppPreferencesSyncCursorStore (SharedPreferences) and the SyncOrchestrator (ProcessLifecycleOwner)
// are Android-only, so the iOS SyncCursorStore + SyncController stay no-op for now. Auth never
// hard-requires a real SyncController: ProfileViewModel only OBSERVES SyncController.status (the
// no-op emits a steady idle SyncStatus) and DeleteUserAccountUseCase touches SyncCursorStore.clear
// (a no-op here), so sign-in / sign-up / sign-out / claim all work end-to-end against these stubs.
// TODO phase 6b: replace with the real Supabase sync engine + an iOS cursor store.

internal class NoOpSyncCursorStore : SyncCursorStore {
    override fun lastPulledAt(userId: String): String? = null
    override fun setLastPulledAt(userId: String, cursor: String) = Unit
    override fun clear(userId: String) = Unit
}

internal class NoOpSyncController : SyncController {
    // Never syncing, never synced — the iOS surface has no remote sync backend until 6b.
    override val status: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus())
    override fun requestSync(manual: Boolean) = Unit
}

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
