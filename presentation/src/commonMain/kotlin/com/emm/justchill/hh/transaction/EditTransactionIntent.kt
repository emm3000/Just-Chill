package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface EditTransactionIntent : UiIntent {

    data class OnAmountChange(val value: String) : EditTransactionIntent

    data class OnDescriptionChange(val value: String) : EditTransactionIntent

    data class OnTransactionTypeChange(val value: TransactionType) : EditTransactionIntent

    /**
     * A day picked in the date sheet. Carries the day, not epoch millis: the sheet works in days,
     * and the conversion to an instant belongs at the one boundary that saves.
     */
    data class OnDateSelected(val value: LocalDate) : EditTransactionIntent

    data class OnAccountSelected(val value: Account) : EditTransactionIntent

    data class OnCategorySelected(val value: SelectableCategory) : EditTransactionIntent

    data object OnSave : EditTransactionIntent

    data object OnDelete : EditTransactionIntent
}
