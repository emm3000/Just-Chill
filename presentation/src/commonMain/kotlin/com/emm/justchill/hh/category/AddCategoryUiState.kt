package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

data class AddCategoryUiState(
    val name: String = String.Empty,
    val iconId: String = "food",
    val categoryType: CategoryType = CategoryType.Spend,
    val colorId: String = "blue",
    val isAllFieldValidated: Boolean = false,
) : UiState
