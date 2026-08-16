package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.justchill.core.preferences.AppPreferences

class DefaultBackupMetadataStore(private val prefs: AppPreferences) : BackupMetadataStore {
    override fun lastSuccessfulBackupAt(userId: String): Long? = prefs.lastSuccessfulBackupAt(userId)

    override fun setLastSuccessfulBackupAt(userId: String, epochMillis: Long) =
        prefs.setLastSuccessfulBackupAt(userId, epochMillis)

    override fun failureState(userId: String): BackupFailureState = prefs.backupFailure(userId)

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
