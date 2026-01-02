package com.emm.domain.transaction

import com.emm.domain.account.Account
import com.emm.domain.shared.currentTimeInMillis
import java.util.UUID

data class TransactionInsert(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val categoryId: String?,
    val date: Long,
    val account: Account,
    val updatedAt: Long = currentTimeInMillis(),
    val createdAt: Long = currentTimeInMillis(),
)