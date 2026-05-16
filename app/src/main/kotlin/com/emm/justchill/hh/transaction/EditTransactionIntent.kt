package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent

sealed interface EditTransactionIntent : UiIntent {

    data class OnAmountChange(val value: String) : EditTransactionIntent

    data class OnDescriptionChange(val value: String) : EditTransactionIntent

    data class OnDateChange(val value: String) : EditTransactionIntent

    data class OnTransactionTypeChange(val value: TransactionType) : EditTransactionIntent

    data class OnDateChangeInMillis(val value: Long?) : EditTransactionIntent

    data class OnAccountSelected(val value: Account) : EditTransactionIntent

    data object OnSave : EditTransactionIntent

    data object OnDelete : EditTransactionIntent
}
