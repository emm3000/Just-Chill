package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.justchill.core.mvi.UiState
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
) : UiState
