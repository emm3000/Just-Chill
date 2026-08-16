@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class TransactionInsert(
    val id: TransactionId = TransactionId(Uuid.random().toString()),
    val type: TransactionType,
    val amount: Money,
    val description: String,
    val categoryId: CategoryId?,
    val occurredAt: LocalDateTime,
    val accountId: AccountId,
)
