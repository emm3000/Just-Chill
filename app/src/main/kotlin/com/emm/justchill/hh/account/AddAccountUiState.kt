package com.emm.justchill.hh.account

import androidx.compose.runtime.Immutable
import com.emm.domain.account.AccountType
import com.emm.domain.account.Currency
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

@Immutable
data class AddAccountUiState(
    val name: String = String.Empty,
    val selectedType: AccountType = AccountType.Bank,
    val selectedCurrency: Currency = Currency.PEN,
    val isEnabled: Boolean = false,
) : UiState
