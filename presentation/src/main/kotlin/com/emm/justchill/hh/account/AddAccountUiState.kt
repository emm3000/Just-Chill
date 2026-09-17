package com.emm.justchill.hh.account

import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

data class AddAccountUiState(
    val name: String = String.Empty,
    val selectedType: AccountType = AccountType.Bank,
    val isEnabled: Boolean = false,
) : UiState
