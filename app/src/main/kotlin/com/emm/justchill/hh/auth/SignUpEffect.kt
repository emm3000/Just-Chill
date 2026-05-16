package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiEffect

sealed interface SignUpEffect : UiEffect {

    data object NavigateBack : SignUpEffect

    data class ShowError(val message: String) : SignUpEffect
}
