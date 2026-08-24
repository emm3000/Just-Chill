package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.balanceFormatted
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
    // balanceFormatted() collapses zero and positive to the same unsigned string, so the
    // screen-lead figure carries the raw value too: AccountsScreen.kt keys its tone off this
    // Money's sign, which the formatted string alone cannot tell apart.
    val totalBalanceMoney: Money = Money.Zero,
) : UiState
