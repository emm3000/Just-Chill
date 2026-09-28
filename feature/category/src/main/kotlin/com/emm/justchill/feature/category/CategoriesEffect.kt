package com.emm.justchill.feature.category

import com.emm.justchill.core.presentation.mvi.UiEffect

sealed interface CategoriesEffect : UiEffect {
    data class ShowMessage(val text: String) : CategoriesEffect
}
