package com.emm.domain.shared.backup

import kotlin.time.Instant

interface BackupVerifier {

    suspend fun verifyLatest(): BackupVerification
}

sealed interface BackupVerification {

    data object NoSnapshots : BackupVerification

    data class Verified(
        val fileName: String,
        val takenAt: Instant,
        val schemaVersion: Int,
        val rowCounts: BackupRowCounts,
        val isNewestPair: Boolean,
    ) : BackupVerification

    data class NothingVerified(val pairsInspected: Int) : BackupVerification
}

data class BackupRowCounts(val accounts: Int, val categories: Int, val transactions: Int, val recurringMovements: Int)
