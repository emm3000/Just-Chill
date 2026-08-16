package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason

data class BackupHealth(
    val lastSuccessfulBackupAt: Long?,
    val consecutiveFailures: Int,
    val lastFailureReason: BackupFailureReason?,
    val canUploadToDestination: Boolean,
) {
    companion object {
        val None = BackupHealth(
            lastSuccessfulBackupAt = null,
            consecutiveFailures = 0,
            lastFailureReason = null,
            canUploadToDestination = false,
        )
    }
}
