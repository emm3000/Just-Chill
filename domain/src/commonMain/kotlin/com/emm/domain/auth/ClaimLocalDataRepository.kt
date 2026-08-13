package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Stamps every anonymous-local row (userId IS NULL) with the given [userId] and marks it
 * Pending for sync, across the four tables (transactions, categories, accounts, recurring movements).
 *
 * Idempotent: rows that already carry a userId are left untouched — the WHERE clause is the guarantee.
 * Implementation must execute all updates inside a single atomic transaction.
 *
 * This whole claim machinery is scheduled for deletion in Phase 5 of `docs/sync/ADR009_PLAN.md`:
 * once the row-replication engine is gone, a local `userId` is ownership metadata for a backup
 * upload, not a precondition for a push that no longer exists.
 */
interface ClaimLocalDataRepository {

    suspend fun claimAll(userId: String)

    /**
     * Reverses [claimAll]: resets userId to NULL and syncState to 'Pending' for all rows that
     * belong to [userId], across the four tables. Executed inside a single atomic transaction.
     *
     * Called as part of account deletion so that local data survives as anonymous-local rows
     * (the app stays fully usable without an account, mirroring the pre-sign-in state).
     */
    suspend fun unclaimAll(userId: String)

    /**
     * Emits the total number of anonymous-local rows (userId IS NULL) across the four tables.
     * Drives reactive claiming: while a user is authenticated, any value > 0 means there are
     * freshly-created local rows that must be claimed for the current user so they can sync.
     * (Scheduled for deletion in Phase 5 of `docs/sync/ADR009_PLAN.md` along with the rest of
     * this interface — see the class KDoc above.)
     */
    fun observeUnclaimedCount(): Flow<Long>
}
