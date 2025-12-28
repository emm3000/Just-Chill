package com.emm.justchill.hh.home

import com.emm.domain.transaction.Transaction

data class HomeUiState(
    val lastTransactions: List<Transaction> = emptyList(),
    val income: Double = 0.0,
    val spend: Double = 0.0,
    val balance: Double = 0.0,
)