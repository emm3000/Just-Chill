package com.emm.justchill.hh.domain.transaction.model

data class TransactionInsert(
    val id: String? = null,
    val type: String,
    val amount: Double = 0.0,
    val description: String,
    val categoryId: String? = null,
    val date: Long,
    val accountId: String,
)