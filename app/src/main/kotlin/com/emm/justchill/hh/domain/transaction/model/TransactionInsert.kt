package com.emm.justchill.hh.domain.transaction.model

import com.emm.justchill.hh.domain.transaction.SyncStatus

data class TransactionInsert(
    val id: String? = null,
    val type: String,
    val amount: Double = 0.0,
    val description: String,
    val date: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
)