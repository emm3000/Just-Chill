package com.emm.justchill.hh.domain.transaction.model

import com.emm.justchill.hh.presentation.transaction.TransactionType

data class TransactionInsert(
    val id: String? = null,
    val type: TransactionType,
    val amount: Double = 0.0,
    val description: String,
    val categoryId: String? = null,
    val date: Long,
    val accountId: String,
)