package com.emm.justchill.hh.account

import androidx.compose.runtime.Stable
import com.emm.domain.account.Account
import com.emm.domain.shared.AccountId
import com.emm.justchill.core.mvi.UiState

@Stable
data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val movementCounts: Map<AccountId, Int> = emptyMap(),
    val pendingEdit: Account? = null,
    val editName: String = "",
    val pendingDelete: Account? = null,
) : UiState
