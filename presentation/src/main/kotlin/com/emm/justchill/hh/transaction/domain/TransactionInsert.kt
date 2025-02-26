package com.emm.justchill.hh.transaction.domain

import com.emm.justchill.hh.transaction.presentation.TransactionType

data class TransactionInsert(
    val id: String? = null,
    val type: TransactionType,
    val amount: Double = 0.0,
    val description: String,
    val categoryId: String? = null,
    val date: Long,
    val accountId: String,
)