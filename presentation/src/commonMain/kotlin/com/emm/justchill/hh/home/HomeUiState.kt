package com.emm.justchill.hh.home

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.recurring.PendingRecurringUi
import com.emm.justchill.hh.transaction.TransactionUi

/**
 * [month] carries no default. A default that reads the wall clock answers "which month is it" from
 * the machine, for whoever forgot to say — the ambient read this whole state class exists downstream
 * of. The ViewModel holds the injected clock and zone and supplies it; previews name one.
 */
data class HomeUiState(
    val month: YearMonth,
    val lastTransactions: List<TransactionUi> = emptyList(),
    val income: Money = Money.Zero,
    val spend: Money = Money.Zero,
    val balance: Money = Money.Zero,
    val hasAnyTransaction: Boolean = false,
    val pendingRecurringMovements: List<PendingRecurringUi> = emptyList(),
) : UiState {
    val isFirstLaunch: Boolean
        get() = !hasAnyTransaction
    val isMonthEmpty: Boolean
        get() = hasAnyTransaction && lastTransactions.isEmpty()
}
