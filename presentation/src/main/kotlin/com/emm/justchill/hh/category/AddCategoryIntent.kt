package com.emm.justchill.hh.category

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface AddCategoryIntent : UiIntent {

    data class OnNameChange(val value: String) : AddCategoryIntent

    data class OnCategoryTypeChange(val value: CategoryType) : AddCategoryIntent

    data class OnColorChange(val value: String) : AddCategoryIntent

    data class OnIconChange(val value: String) : AddCategoryIntent

    data object OnSave : AddCategoryIntent
}
