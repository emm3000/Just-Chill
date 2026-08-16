package com.emm.justchill.core.backup

import com.emm.domain.shared.backup.BackupFailureReason

// isDestinationDisclosed false blocks every upload to this account until the user acknowledges the
// disclosure (ADR 009 Decision 5), so a UI must not read it as a mere hint.
data class BackupHealth(
    val lastSuccessfulBackupAt: Long?,
    val consecutiveFailures: Int,
    val lastFailureReason: BackupFailureReason?,
    val isDestinationDisclosed: Boolean,
) {
    companion object {
        val None = BackupHealth(
            lastSuccessfulBackupAt = null,
            consecutiveFailures = 0,
            lastFailureReason = null,
            isDestinationDisclosed = false,
        )
    }
}
