package com.emm.justchill.hh.home

import androidx.compose.runtime.Stable
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.transaction.TransactionUi

@Stable
data class HomeUiState(
    val lastTransactions: List<TransactionUi> = emptyList(),
    val income: Double = 0.0,
    val spend: Double = 0.0,
    val balance: Double = 0.0,
) : UiState