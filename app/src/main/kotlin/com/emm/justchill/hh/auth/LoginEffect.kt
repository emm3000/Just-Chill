package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiEffect

sealed interface LoginEffect : UiEffect {

    data object NavigateToHome : LoginEffect

    data class ShowError(val message: String) : LoginEffect
}
