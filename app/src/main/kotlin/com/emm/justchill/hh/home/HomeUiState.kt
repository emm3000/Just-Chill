package com.emm.justchill.hh.home

import androidx.compose.runtime.Stable
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.transaction.TransactionUi

@Stable
data class HomeUiState(
    val month: YearMonth = YearMonth.current(),
    val lastTransactions: List<TransactionUi> = emptyList(),
    val income: Money = Money.Zero,
    val spend: Money = Money.Zero,
    val balance: Money = Money.Zero,
) : UiState
