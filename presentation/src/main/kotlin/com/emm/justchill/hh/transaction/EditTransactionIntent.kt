package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface EditTransactionIntent : UiIntent {

    data class OnAmountChange(val value: String) : EditTransactionIntent

    data class OnDescriptionChange(val value: String) : EditTransactionIntent

    data class OnTransactionTypeChange(val value: TransactionType) : EditTransactionIntent

    data class OnDateSelected(val value: LocalDate) : EditTransactionIntent

    data class OnAccountSelected(val value: Account) : EditTransactionIntent

    data class OnCategorySelected(val value: SelectableCategory) : EditTransactionIntent

    data object OnSave : EditTransactionIntent

    data object OnDelete : EditTransactionIntent

    /** Opens [sheet] over the screen. */
    data class OnSheetRequested(val sheet: TransactionSheet) : EditTransactionIntent

    /** The user dismissed whichever sheet was open. */
    data object OnSheetDismissed : EditTransactionIntent
}
