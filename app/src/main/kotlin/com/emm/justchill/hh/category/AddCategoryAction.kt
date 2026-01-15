package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType

sealed interface AddCategoryAction {

    class OnNameChange(val value: String) : AddCategoryAction

    data class OnCategoryTypeChange(val value: CategoryType) : AddCategoryAction

    data class OnColorChange(val value: CategoryColor) : AddCategoryAction

    data class OnIconChange(val value: IconCatalog) : AddCategoryAction

    data object OnSave : AddCategoryAction
}