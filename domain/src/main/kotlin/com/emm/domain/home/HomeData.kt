package com.emm.domain.home

import com.emm.domain.transaction.TransactionWithCategory

data class HomeData(
    val lastTransactions: List<TransactionWithCategory>,
    val income: Double,
    val spend: Double,
    val balance: Double,
)
