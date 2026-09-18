package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.mvi.UiState

data class AddCategoryUiState(
    val name: String = "",
    val iconId: String = "food",
    val categoryType: CategoryType = CategoryType.Spend,
    val colorId: String = "blue",
    val isAllFieldValidated: Boolean = false,
) : UiState
