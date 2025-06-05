package com.emm.domain.home

import com.emm.domain.transaction.Transaction

data class HomeData(
    val lastTransactions: List<Transaction>,
    val income: Double,
    val spend: Double,
    val balance: Double,
)
