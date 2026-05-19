package com.emm.data.backup

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val transactionId: String,
    val type: String,
    val amountCents: Long,
    val description: String,
    val date: Long,
    val accountId: String,
    val categoryId: String?,
)

fun Transaction.toDto() = TransactionDto(
    transactionId = transactionId.value,
    type = type.name,
    amountCents = amount.cents,
    description = description,
    date = date,
    accountId = accountId.value,
    categoryId = categoryId?.value,
)

fun TransactionDto.toEntity() = Transaction(
    transactionId = TransactionId(transactionId),
    type = TransactionType.valueOf(type),
    amount = Money(amountCents),
    description = description,
    date = date,
    accountId = AccountId(accountId),
    categoryId = categoryId?.let { CategoryId(it) },
)
