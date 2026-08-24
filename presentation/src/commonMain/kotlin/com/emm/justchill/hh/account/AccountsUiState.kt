package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val movementCounts: Map<AccountId, Int> = emptyMap(),
    val pendingEdit: Account? = null,
    val editName: String = "",
    val pendingDelete: Account? = null,
    // Loans are a parallel ledger (ADR 010): this total never folds into any account balance.
    val loansTotalOwed: String = formatNeutral(fromCentsToSolesWith(Money.Zero)),
    // Home's hero showed this figure before E06-04 deleted the screen (ADR 010); Cuentas is the
    // only surface left that shows it, sourced from TransactionRepository.observeTotals().
    val totalBalance: String = Money.Zero.balanceFormatted(),
) : UiState

/**
 * Reproduces the one sign convention Home's `AmountHero` gave the all-time balance: no `+` for a
 * positive or zero figure — unlike [formatIncome][com.emm.justchill.hh.shared.formatIncome] — so it
 * still reads identically to what Home showed and to the unsigned `loansTotalOwed` beside it, and a
 * negative figure keeps its sign via the same [formatExpense] every negative amount already uses.
 */
fun Money.balanceFormatted(): String =
    if (cents < 0L) formatExpense(fromCentsToSolesWith(this)) else formatNeutral(fromCentsToSolesWith(this))
