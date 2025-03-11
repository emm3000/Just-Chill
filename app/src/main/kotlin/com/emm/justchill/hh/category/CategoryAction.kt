package com.emm.justchill.hh.category

import com.emm.domain.transaction.TransactionType

sealed interface CategoryAction {

    class OnNameChange(val value: String) : CategoryAction

    class OnDescriptionChange(val value: String) : CategoryAction

    class OnTransactionTypeChange(val value: TransactionType) : CategoryAction

    data object OnSave : CategoryAction
}