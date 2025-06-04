package com.emm.domain.transaction

import com.emm.domain.account.Account

data class TransactionInsert(
    val id: String? = null,
    val type: TransactionType,
    val amount: Double = 0.0,
    val description: String,
    val categoryId: String? = null,
    val date: Long,
    val account: Account,
)