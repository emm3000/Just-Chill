package com.emm.justchill.hh.account

import com.emm.domain.account.Account
import com.emm.justchill.core.mvi.UiState

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
) : UiState
