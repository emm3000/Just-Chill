package com.emm.justchill.hh.recurring

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent
import com.emm.justchill.hh.transaction.SelectableCategory

sealed interface AddEditRecurringMovementIntent : UiIntent {
    data class OnNameChange(val value: String) : AddEditRecurringMovementIntent
    data class OnTypeChange(val value: TransactionType) : AddEditRecurringMovementIntent
    data class OnAmountChange(val digits: String) : AddEditRecurringMovementIntent
    data class OnVariableAmountToggle(val isVariable: Boolean) : AddEditRecurringMovementIntent
    data class OnDayOfMonthChange(val day: Int) : AddEditRecurringMovementIntent
    data class OnIsActiveChange(val isActive: Boolean) : AddEditRecurringMovementIntent
    data class OnDescriptionChange(val value: String) : AddEditRecurringMovementIntent
    data class OnAccountSelected(val account: Account) : AddEditRecurringMovementIntent
    data class OnCategorySelected(val category: SelectableCategory?) : AddEditRecurringMovementIntent
    data object Save : AddEditRecurringMovementIntent
}
