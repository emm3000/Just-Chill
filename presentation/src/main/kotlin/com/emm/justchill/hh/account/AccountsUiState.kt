package com.emm.justchill.hh.account

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral
import com.emm.justchill.core.ui.mvi.UiState

data class AccountsUiState(
    val month: YearMonth,
    val accounts: List<AccountMonthUi> = emptyList(),
    val monthSpent: String = formatNeutral(Money.Zero.format()),
    val monthIncome: String = formatNeutral(Money.Zero.format()),
    val pendingEdit: Account? = null,
    val editName: String = "",
    val pendingDelete: Account? = null,
    // Loans are a parallel ledger (ADR 010): this total never folds into any account's monthly net.
    val loansTotalOwed: String = formatNeutral(Money.Zero.format()),
    // The total's own sign — loansPeople can be non-empty at zero.
    val loansTotalOwedIsPositive: Boolean = false,
    // Settled people are excluded from this list.
    val loansPeople: List<String> = emptyList(),
) : UiState
