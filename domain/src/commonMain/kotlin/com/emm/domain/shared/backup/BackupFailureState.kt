package com.emm.domain.shared.backup

data class BackupFailureState(val consecutiveFailures: Int, val lastReason: BackupFailureReason?) {
    companion object {
        val None = BackupFailureState(consecutiveFailures = 0, lastReason = null)
    }
}
