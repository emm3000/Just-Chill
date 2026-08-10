package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

data class AddCategoryUiState(
    val name: String = String.Empty,
    /** Catalog id of the selected icon ("food" is the catalog's first entry). */
    val iconId: String = "food",
    val categoryType: CategoryType = CategoryType.Spend,
    /** Palette id of the selected color ("blue" is the palette's first entry). */
    val colorId: String = "blue",
    val isAllFieldValidated: Boolean = false,
) : UiState
