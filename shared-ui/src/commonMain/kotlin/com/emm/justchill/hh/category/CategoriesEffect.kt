package com.emm.justchill.hh.category

import com.emm.justchill.core.mvi.UiEffect

sealed interface CategoriesEffect : UiEffect {
    data class ShowMessage(val text: String) : CategoriesEffect
}
