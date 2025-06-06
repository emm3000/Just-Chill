package com.emm.domain.transaction

import com.emm.domain.account.Account

data class TransactionInsert(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val categoryId: String?,
    val date: Long,
    val account: Account,
    val deleted: Boolean,
    val isSynced: Boolean,
    val updatedAt: Long,
)