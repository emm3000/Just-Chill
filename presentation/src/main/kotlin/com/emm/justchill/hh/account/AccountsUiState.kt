package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
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
    /** Whoever still owes something, settled people excluded — the screen names them and counts them. */
    val loansPeople: List<String> = emptyList(),
) : UiState
