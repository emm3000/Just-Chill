package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface AddTransactionIntent : UiIntent {

    data class OnAmountChange(val value: String) : AddTransactionIntent

    data class OnDescriptionChange(val value: String) : AddTransactionIntent

    data class OnTransactionTypeChange(val value: TransactionType) : AddTransactionIntent

    data class OnDateSelected(val value: LocalDate) : AddTransactionIntent

    data class OnAccountSelected(val value: Account) : AddTransactionIntent

    data class OnNewValueFromOthers(val value: SelectableCategory) : AddTransactionIntent

    data class OnCategorySelected(val value: SelectableCategory) : AddTransactionIntent

    data class OnFrequentComboSelected(val value: FrequentComboUi) : AddTransactionIntent

    data class OnPreselectCombo(val accountId: String?, val categoryId: String?, val type: TransactionType?) :
        AddTransactionIntent

    data object OnReset : AddTransactionIntent

    data object OnSave : AddTransactionIntent
}
