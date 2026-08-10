package com.emm.justchill.hh.category

import com.emm.domain.category.Category
import com.emm.justchill.core.mvi.UiIntent

sealed interface CategoriesIntent : UiIntent {
    data class OnEditClick(val category: Category) : CategoriesIntent
    data class OnEditNameChange(val value: String) : CategoriesIntent
    data object OnEditConfirm : CategoriesIntent
    data object OnEditDismiss : CategoriesIntent

    data class OnDeleteClick(val category: Category) : CategoriesIntent
    data object OnDeleteConfirm : CategoriesIntent
    data object OnDeleteDismiss : CategoriesIntent
}
