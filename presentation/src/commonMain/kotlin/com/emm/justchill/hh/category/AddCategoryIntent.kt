package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.UiIntent

sealed interface AddCategoryIntent : UiIntent {

    data class OnNameChange(val value: String) : AddCategoryIntent

    data class OnCategoryTypeChange(val value: CategoryType) : AddCategoryIntent

    /** [value] is the color's catalog id — the UI resolves it back to the palette entry. */
    data class OnColorChange(val value: String) : AddCategoryIntent

    /** [value] is the icon's catalog id — the UI resolves it back to the catalog entry. */
    data class OnIconChange(val value: String) : AddCategoryIntent

    data object OnSave : AddCategoryIntent
}
