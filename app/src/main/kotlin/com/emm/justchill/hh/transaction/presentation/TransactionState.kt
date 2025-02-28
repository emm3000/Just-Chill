package com.emm.justchill.hh.transaction.presentation

import androidx.compose.runtime.Immutable
import com.emm.domain.account.Account

@Immutable
data class TransactionState(
    val accounts: List<Account> = emptyList(),
    val accountSelected: Account? = null,
    val isEnabledButton: Boolean = false,
)