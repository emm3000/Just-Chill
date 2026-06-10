package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.SyncCursorStore
import kotlinx.coroutines.flow.first

/**
 * Deletes the authenticated user's Supabase account and all remote data, then reverts local rows
 * to anonymous state so the app stays fully usable without an account.
 *
 * Order matters:
 * 1. [AuthRepository.deleteAccount] — calls the remote RPC (removes remote data + auth user) and
 *    clears the local session. Executing this BEFORE unclaim ensures the SyncOrchestrator's
 *    pending-count trigger (active only while Authenticated) never sees the new Pending rows.
 *    If this step throws, local data is untouched and no orphaned state is created.
 * 2. [ClaimLocalDataRepository.unclaimAll] — resets userId → NULL and syncState → 'Pending' for
 *    all rows belonging to [userId]. Local data survives as anonymous-local rows.
 * 3. [SyncCursorStore.clear] — removes stale pull-cursor and last-synced-at metadata for [userId]
 *    so it does not interfere if the same device registers again in the future.
 *
 * NOTE: [DeleteAccountUseCase] in `com.emm.domain.account` handles FINANCIAL account deletion
 * (a bank/wallet account entity). This use case is for the AUTH user account.
 */
class DeleteUserAccountUseCase(
    private val authRepository: AuthRepository,
    private val claimLocalDataRepository: ClaimLocalDataRepository,
    private val syncCursorStore: SyncCursorStore,
) {
    suspend operator fun invoke() {
        val status = authRepository.sessionStatus.first { it !is SessionStatus.Initializing }
        val userId = (status as? SessionStatus.Authenticated)?.user?.userId
            ?: throw DomainException.Unauthorized("No authenticated session")

        // Step 1: remote delete + local sign-out. On failure, local data is untouched.
        authRepository.deleteAccount()

        // Step 2: revert owned local rows to anonymous-local (userId = NULL, syncState = Pending).
        claimLocalDataRepository.unclaimAll(userId)

        // Step 3: clear stale pull-cursor and last-synced-at timestamp for this user.
        syncCursorStore.clear(userId)
    }
}
