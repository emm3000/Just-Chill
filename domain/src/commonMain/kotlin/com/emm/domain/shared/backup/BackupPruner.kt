package com.emm.domain.shared.backup

interface BackupPruner {

    suspend fun prune(): BackupPruneReport
}

data class BackupPruneReport(val kept: Int, val deleted: Int, val failedDeletes: List<String>)
