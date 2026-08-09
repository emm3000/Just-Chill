package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncMutex
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

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
 * Resolving the session is bounded by [SESSION_RESOLVE_TIMEOUT]. The whole flow holds the shared
 * [SyncMutex], so an unbounded wait there would not just hang this deletion — it would block every
 * sync cycle for the rest of the process lifetime. A session that never settles surfaces as
 * [DomainException.NetworkUnavailable]: retryable, and the lock is released on the way out.
 *
 * NOTE: [DeleteAccountUseCase] in `com.emm.domain.account` handles FINANCIAL account deletion
 * (a bank/wallet account entity). This use case is for the AUTH user account.
 */
class DeleteUserAccountUseCase(
    private val authRepository: AuthRepository,
    private val claimLocalDataRepository: ClaimLocalDataRepository,
    private val syncCursorStore: SyncCursorStore,
    private val syncMutex: SyncMutex,
) {
    // The whole flow runs under the shared SyncMutex: an in-flight push finishing AFTER the
    // delete_account RPC would re-upsert rows the server just wiped (stateless JWT + no FK to
    // auth.users + RLS uid claim still matching). See SyncMutex for the full rationale.
    suspend operator fun invoke() = syncMutex.withLock {
        val userId = resolveAuthenticatedUserId()

        // Step 1: remote delete + local sign-out. On failure, local data is untouched.
        authRepository.deleteAccount()

        // Step 2: revert owned local rows to anonymous-local (userId = NULL, syncState = Pending).
        claimLocalDataRepository.unclaimAll(userId)

        // Step 3: clear stale pull-cursor and last-synced-at timestamp for this user.
        syncCursorStore.clear(userId)
    }

    /**
     * Waits for the session to leave [SessionStatus.Initializing] and returns the authenticated
     * userId, bounding that wait by [SESSION_RESOLVE_TIMEOUT].
     *
     * Only the resolution is wrapped — the deletion steps must never be torn down mid-flight, and
     * an aborted timeout leaves nothing behind because nothing has run yet.
     *
     * The catch is deliberately narrowed to [TimeoutCancellationException]: it is itself a
     * [kotlinx.coroutines.CancellationException], so catching anything broader would swallow a
     * genuine cancellation of the caller (the user leaving the screen) and report it as a failed
     * deletion. Mirrors `DefaultSyncRepository.currentUserId()`, which bounds the same wait for the
     * same reason.
     */
    private suspend fun resolveAuthenticatedUserId(): String {
        val status = try {
            withTimeout(SESSION_RESOLVE_TIMEOUT) {
                authRepository.sessionStatus.first { it !is SessionStatus.Initializing }
            }
        } catch (e: TimeoutCancellationException) {
            throw DomainException.NetworkUnavailable(e)
        }
        return (status as? SessionStatus.Authenticated)?.user?.userId
            ?: throw DomainException.Unauthorized("No authenticated session")
    }

    private companion object {
        /**
         * Upper bound on session resolution. Matches the sync path's own bound: long enough that a
         * slow cold-start session load still resolves, short enough that a stuck one frees the
         * shared [SyncMutex] instead of holding it for the process lifetime.
         */
        val SESSION_RESOLVE_TIMEOUT = 10.seconds
    }
}
