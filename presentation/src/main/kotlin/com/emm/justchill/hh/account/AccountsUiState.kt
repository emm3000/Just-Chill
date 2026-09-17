package com.emm.justchill.hh.account

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class AccountsUiState(
    val month: YearMonth,
    val accounts: List<AccountMonthUi> = emptyList(),
    val monthSpent: String = formatNeutral(fromCentsToSolesWith(Money.Zero)),
    val monthIncome: String = formatNeutral(fromCentsToSolesWith(Money.Zero)),
    val pendingEdit: Account? = null,
    val editName: String = "",
    val pendingDelete: Account? = null,
    // Loans are a parallel ledger (ADR 010): this total never folds into any account's monthly net.
    val loansTotalOwed: String = formatNeutral(fromCentsToSolesWith(Money.Zero)),
    // The total's own sign — loansPeople can be non-empty at zero.
    val loansTotalOwedIsPositive: Boolean = false,
    // Settled people are excluded from this list.
    val loansPeople: List<String> = emptyList(),
) : UiState
