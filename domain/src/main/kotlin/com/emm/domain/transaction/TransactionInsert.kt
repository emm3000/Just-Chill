package com.emm.domain.transaction

import com.emm.domain.account.Account
import com.emm.domain.shared.SyncState

data class TransactionInsert(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val categoryId: String?,
    val date: Long,
    val account: Account,
    val isDeleted: Boolean,
    val syncState: SyncState,
    val updatedAt: Long,
    val createdAt: Long,
)