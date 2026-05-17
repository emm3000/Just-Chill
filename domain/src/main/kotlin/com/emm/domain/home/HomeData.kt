package com.emm.domain.home

import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionWithCategory

data class HomeData(
    val lastTransactions: List<TransactionWithCategory>,
    val income: Money,
    val spend: Money,
    val balance: Money,
)
