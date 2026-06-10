package com.emm.domain.sync

/**
 * Port interface for persisting the per-user pull cursor.
 *
 * The cursor is an ISO-8601 UTC string representing the max `server_updated_at` seen
 * on the last successful pull (ADR 002). Keyed by userId so cursor survives sign-out
 * and correctly resumes for the same account on re-sign-in.
 *
 * Implemented in :app over [AppPreferences]. :data injects it via constructor so the
 * module boundary (:data → :domain only, never :data → :app) is preserved.
 */
interface SyncCursorStore {
    fun lastPulledAt(userId: String): String?
    fun setLastPulledAt(userId: String, cursor: String)

    /**
     * Removes all per-user sync metadata persisted for [userId] (pull cursor and last-synced-at
     * timestamp). Called when the user deletes their account so stale cursor data does not
     * interfere if the same device re-registers with a new account.
     */
    fun clear(userId: String)
}
