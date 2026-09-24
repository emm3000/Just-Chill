package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface AddTransactionIntent : UiIntent {

    data class OnAmountChange(val value: String) : AddTransactionIntent

    data class OnDescriptionChange(val value: String) : AddTransactionIntent

    data class OnTransactionTypeChange(val value: TransactionType) : AddTransactionIntent

    data class OnDateSelected(val value: LocalDate) : AddTransactionIntent

    data class OnAccountSelected(val value: Account) : AddTransactionIntent

    data class OnNewValueFromOthers(val value: SelectableCategory) : AddTransactionIntent

    data class OnCategorySelected(val value: SelectableCategory) : AddTransactionIntent

    data class OnPreselectCombo(val accountId: String?, val categoryId: String?, val type: TransactionType?) :
        AddTransactionIntent

    data object OnSave : AddTransactionIntent

    data class OnSheetRequested(val sheet: TransactionSheet) : AddTransactionIntent

    data object OnSheetDismissed : AddTransactionIntent
}
