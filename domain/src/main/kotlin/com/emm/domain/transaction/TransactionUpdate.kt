package com.emm.domain.transaction

import com.emm.domain.account.Account

data class TransactionUpdate(
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val account: Account,
    val date: Long,
)