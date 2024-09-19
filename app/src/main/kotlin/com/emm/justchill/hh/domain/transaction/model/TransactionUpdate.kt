package com.emm.justchill.hh.domain.transaction.model

data class TransactionUpdate(
    val type: String,
    val amount: Double,
    val description: String,
    val accountId: String,
    val date: Long,
)