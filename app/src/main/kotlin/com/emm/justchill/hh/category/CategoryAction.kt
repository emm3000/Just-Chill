package com.emm.justchill.hh.category

import com.emm.domain.transaction.TransactionType

sealed interface CategoryAction {

    class OnNameChange(val value: String) : CategoryAction

    data class OnCategoryTypeChange(val value: TransactionType) : CategoryAction

    data class OnColorChange(val value: CategoryColor) : CategoryAction

    data class OnIconChange(val value: IconCatalog) : CategoryAction

    data object OnSave : CategoryAction
}