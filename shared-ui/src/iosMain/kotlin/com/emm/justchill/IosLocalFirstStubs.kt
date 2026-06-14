package com.emm.justchill

import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ClaimLocalDataRepository
import com.emm.domain.auth.SessionStatus
import com.emm.domain.sync.SyncCursorStore
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

// Phase 5b: the iOS build is LOCAL-FIRST ONLY. Auth + multi-device sync (Supabase, Google
// Sign-In, the SyncOrchestrator) are Android-coupled and deferred to phase 6. ProfileViewModel
// still needs an AuthRepository / ClaimLocalDataRepository / SyncCursorStore / SyncController in
// its constructor closure, so we bind these no-op iOS implementations: ProfileScreen renders and
// the local-first sections (Categorías, Cuentas, Recurrentes, Privacidad, Acerca) work; the
// auth/sync rows simply do nothing. None of these throw — a stubbed action must not crash.
// TODO phase 6: replace with real Supabase-backed iOS implementations.

internal class NoOpAuthRepository : AuthRepository {
    // Always unauthenticated on iOS for 5b — the local-first state mirrors a signed-out user.
    override val sessionStatus: Flow<SessionStatus> = flowOf(SessionStatus.NotAuthenticated)

    override suspend fun signIn(email: String, password: String): AuthUser =
        error("Auth is not available on iOS yet (phase 6)")

    override suspend fun signUp(email: String, password: String): AuthUser? = null

    override suspend fun signOut() = Unit

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthUser =
        error("Auth is not available on iOS yet (phase 6)")

    override suspend fun deleteAccount() = Unit

    override suspend fun resendConfirmationEmail(email: String) = Unit
}

internal class NoOpClaimLocalDataRepository : ClaimLocalDataRepository {
    override suspend fun claimAll(userId: String) = Unit
    override suspend fun unclaimAll(userId: String) = Unit
    override fun observeUnclaimedCount(): Flow<Long> = flowOf(0L)
}

internal class NoOpSyncCursorStore : SyncCursorStore {
    override fun lastPulledAt(userId: String): String? = null
    override fun setLastPulledAt(userId: String, cursor: String) = Unit
    override fun clear(userId: String) = Unit
}

internal class NoOpSyncController : SyncController {
    // Never syncing, never synced — the local-first iOS surface has no remote backend in 5b.
    override val status: StateFlow<SyncStatus> = MutableStateFlow(SyncStatus())
    override fun requestSync(manual: Boolean) = Unit
}
