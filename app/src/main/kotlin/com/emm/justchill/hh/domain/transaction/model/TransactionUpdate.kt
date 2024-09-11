package com.emm.justchill.hh.domain.transaction.model

import com.emm.justchill.hh.domain.transaction.SyncStatus

data class TransactionUpdate(
    val type: String,
    val amount: Double,
    val description: String,
    val date: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE
)