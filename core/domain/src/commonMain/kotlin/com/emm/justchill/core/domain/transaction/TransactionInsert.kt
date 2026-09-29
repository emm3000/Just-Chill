@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
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
