package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface CategoriesIntent : UiIntent {
    data class OnEditClick(val category: Category) : CategoriesIntent
    data class OnEditNameChange(val value: String) : CategoriesIntent
    data object OnEditConfirm : CategoriesIntent
    data object OnEditDismiss : CategoriesIntent

    data class OnDeleteClick(val category: Category) : CategoriesIntent
    data object OnDeleteConfirm : CategoriesIntent
    data object OnDeleteDismiss : CategoriesIntent
}
