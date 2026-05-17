package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.currentTimeInMillis
import java.util.UUID

data class TransactionInsert(
    val id: TransactionId = TransactionId(UUID.randomUUID().toString()),
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val categoryId: CategoryId?,
    val date: Long,
    val accountId: AccountId,
    val updatedAt: Long = currentTimeInMillis(),
    val createdAt: Long = currentTimeInMillis(),
)
