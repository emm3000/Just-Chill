package com.emm.justchill.hh.transaction.presentation

import androidx.compose.runtime.Immutable
import com.emm.justchill.hh.account.domain.Account

@Immutable
data class TransactionState(
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val isEnabledButton: Boolean = false,
)