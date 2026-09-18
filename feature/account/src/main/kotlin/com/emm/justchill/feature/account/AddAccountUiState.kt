package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.ui.mvi.UiState

data class AddAccountUiState(
    val name: String = "",
    val selectedType: AccountType = AccountType.Bank,
    val isEnabled: Boolean = false,
) : UiState
