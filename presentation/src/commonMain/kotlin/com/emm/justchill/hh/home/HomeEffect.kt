package com.emm.justchill.hh.home

import com.emm.justchill.core.mvi.UiEffect

sealed interface HomeEffect : UiEffect {
    data object CloseConfirmSheet : HomeEffect
    data class ShowError(val message: String) : HomeEffect
}
