package com.emm.justchill.hh.account

import androidx.compose.runtime.Stable
import com.emm.domain.account.Account
import com.emm.justchill.core.mvi.UiState

@Stable
data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
) : UiState
