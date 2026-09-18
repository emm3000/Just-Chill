package com.emm.justchill.hh.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddCategoryEffect : UiEffect {

    data class CategorySaved(val created: Category) : AddCategoryEffect

    data class ShowError(val message: String) : AddCategoryEffect
}
