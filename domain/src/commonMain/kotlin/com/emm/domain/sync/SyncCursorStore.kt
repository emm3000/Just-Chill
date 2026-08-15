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
     * Removes all per-user account-deletion metadata for [userId]: the pull cursor and the
     * last-synced-at timestamp (both genuine sync metadata), **plus the persisted backup
     * watermark** (`AppPreferences.lastSuccessfulBackupAt`). The watermark is deliberately NOT
     * sync metadata — it rides this call only because `clear` is the sole account-deletion seam
     * that exists today; the implementation is `DefaultSyncCursorStore.clear` in `:presentation`.
     * Called when the user deletes their account so none of it interferes if the same device
     * re-registers with a new account.
     *
     * ADR 009 Phase 5 deletes this port together with `DefaultSyncCursorStore`. Phase 5 MUST
     * relocate the backup-watermark clear onto whatever seam replaces it — NOT simply delete the
     * call along with the class, or account deletion silently stops clearing the backup watermark.
     */
    fun clear(userId: String)
}
