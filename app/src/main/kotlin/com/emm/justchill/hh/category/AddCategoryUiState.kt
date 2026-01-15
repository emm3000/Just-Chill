package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.justchill.hh.shared.Empty

data class AddCategoryUiState(
    val name: String = String.Empty,
    val icon: IconCatalog = AppIconCatalog.catalog.first(),
    val categoryType: CategoryType = CategoryType.Income,
    val color: CategoryColor = allColors.first(),
    val isAllFieldValidated: Boolean = false,
)