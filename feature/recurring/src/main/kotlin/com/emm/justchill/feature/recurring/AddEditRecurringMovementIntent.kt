package com.emm.justchill.feature.recurring

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.mvi.UiIntent

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
    data class OnSheetRequested(val sheet: RecurringSheet) : AddEditRecurringMovementIntent
    data object OnSheetDismissed : AddEditRecurringMovementIntent
    data object Save : AddEditRecurringMovementIntent
}
