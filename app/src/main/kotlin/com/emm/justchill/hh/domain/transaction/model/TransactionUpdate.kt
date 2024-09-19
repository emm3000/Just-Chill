package com.emm.justchill.hh.domain.transaction.model

import com.emm.justchill.hh.presentation.transaction.TransactionType

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val accountId: String,
    val date: Long,
)