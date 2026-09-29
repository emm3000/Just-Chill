package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.presentation.mvi.UiState

data class AddCategoryUiState(
    val name: String = "",
    val iconId: String = "food",
    val categoryType: CategoryType = CategoryType.Spend,
    val colorId: String = "blue",
    val isAllFieldValidated: Boolean = false,
) : UiState {

    val saveLabel: String get() = if (isPreviewPlaceholder) "Escribe un nombre" else "Crear «$previewName»"

    val previewName: String get() = name.trim().ifBlank { "Tu categoría" }

    val isPreviewPlaceholder: Boolean get() = name.isBlank()
}
