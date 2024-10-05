package com.emm.justchill.hh.transaction.domain

import com.emm.justchill.hh.transaction.presentation.TransactionType

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val accountId: String,
    val date: Long,
)