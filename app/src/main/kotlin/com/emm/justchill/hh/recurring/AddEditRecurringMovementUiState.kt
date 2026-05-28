package com.emm.justchill.hh.recurring

import androidx.compose.runtime.Stable
import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.transaction.SelectableCategory

@Stable
data class AddEditRecurringMovementUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val type: TransactionType = TransactionType.Spend,
    /** Raw cents string — empty string means "not set yet" */
    val amountDigits: String = "",
    val isVariableAmount: Boolean = false,
    val dayOfMonth: Int = 1,
    val isActive: Boolean = true,
    val description: String = "",
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val selectedCategory: SelectableCategory? = null,
    val isLoading: Boolean = false,
    val isSaveEnabled: Boolean = false,
    /** True after loading an existing template — prevents double-loading */
    val isLoaded: Boolean = false,
) : UiState
