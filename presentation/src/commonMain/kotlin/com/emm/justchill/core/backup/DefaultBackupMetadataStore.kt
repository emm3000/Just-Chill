package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.justchill.core.preferences.AppPreferences

/**
 * Single commonMain [BackupMetadataStore] backed by [AppPreferences].
 *
 * Mirrors `DefaultSyncCursorStore`'s shape (`core/sync/`) but deliberately lives outside that
 * package: this seam is not sync metadata, and unlike `DefaultSyncCursorStore` it is NOT deleted by
 * ADR 009 Phase 5 — the backup watermark and its clear are the pipeline this whole plan is building
 * toward, not the engine Phase 5 removes.
 */
class DefaultBackupMetadataStore(private val prefs: AppPreferences) : BackupMetadataStore {
    override fun lastSuccessfulBackupAt(userId: String): Long? = prefs.lastSuccessfulBackupAt(userId)

    override fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long) =
        prefs.setLastSuccessfulBackupAt(userId, epochMillis)

    override fun failureState(userId: String): BackupFailureState = prefs.backupFailure(userId)

    /**
     * Read-then-write, and safe here because there is exactly one writer: `BackupOrchestrator`'s
     * single request consumer runs one cycle at a time, so no second cycle can read the same count
     * and write the same increment. `AppPreferences` offers no atomic increment to use instead —
     * `Settings` has none — and inventing a lock for a counter with one writer would be ceremony.
     */
    override fun recordFailure(userId: String, reason: BackupFailureReason): BackupFailureState {
        val next = BackupFailureState(
            consecutiveFailures = prefs.backupFailure(userId).consecutiveFailures + 1,
            lastReason = reason,
        )
        prefs.setBackupFailure(userId, next)
        return next
    }

    override fun clearFailures(userId: String) = prefs.setBackupFailure(userId, BackupFailureState.None)

    override fun clear(userId: String) = prefs.clearBackupMetadata(userId)
}
