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
    /**
     * Account id awaiting resolution from the accounts flow.
     * Set by [loadTemplate] when the accounts list has not yet been emitted; cleared once
     * the combine collector resolves it to an actual [Account].
     */
    val pendingAccountId: String? = null,
    val categories: List<SelectableCategory> = emptyList(),
    val selectedCategory: SelectableCategory? = null,
    val isSaveEnabled: Boolean = false,
) : UiState
