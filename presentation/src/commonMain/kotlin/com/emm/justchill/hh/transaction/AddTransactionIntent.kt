package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface AddTransactionIntent : UiIntent {

    data class OnAmountChange(val value: String) : AddTransactionIntent

    data class OnDescriptionChange(val value: String) : AddTransactionIntent

    data class OnTransactionTypeChange(val value: TransactionType) : AddTransactionIntent

    /**
     * A day picked in the date sheet. Carries the day, not epoch millis: the sheet works in days,
     * and the conversion to an instant belongs at the one boundary that saves.
     */
    data class OnDateSelected(val value: LocalDate) : AddTransactionIntent

    data class OnAccountSelected(val value: Account) : AddTransactionIntent

    data class OnNewValueFromOthers(val value: SelectableCategory) : AddTransactionIntent

    data class OnCategorySelected(val value: SelectableCategory) : AddTransactionIntent

    data class OnFrequentComboSelected(val value: FrequentComboUi) : AddTransactionIntent

    data object OnReset : AddTransactionIntent

    data object OnSave : AddTransactionIntent
}
