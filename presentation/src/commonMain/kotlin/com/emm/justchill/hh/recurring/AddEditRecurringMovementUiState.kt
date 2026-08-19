package com.emm.justchill.hh.recurring

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.transaction.SelectableCategory

data class AddEditRecurringMovementUiState(
    val isEdit: Boolean = false,
    val name: String = "",
    val type: TransactionType = TransactionType.Spend,
    val amountDigits: String = "",
    val isVariableAmount: Boolean = false,
    val dayOfMonth: Int = 1,
    val isActive: Boolean = true,
    val description: String = "",
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val pendingAccountId: String? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val selectedCategory: SelectableCategory? = null,
    /** Without this, an edit-mode load-ordering race resolves the category to null and silently wipes it on save. */
    val pendingCategoryId: String? = null,
    val isSaveEnabled: Boolean = false,
) : UiState
