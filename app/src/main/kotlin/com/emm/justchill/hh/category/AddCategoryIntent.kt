package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.UiIntent

sealed interface AddCategoryIntent : UiIntent {

    data class OnNameChange(val value: String) : AddCategoryIntent

    data class OnCategoryTypeChange(val value: CategoryType) : AddCategoryIntent

    data class OnColorChange(val value: CategoryColor) : AddCategoryIntent

    data class OnIconChange(val value: IconCatalog) : AddCategoryIntent

    data object OnSave : AddCategoryIntent
}
