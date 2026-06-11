package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiEffect

sealed interface AuthEffect : UiEffect {
    data object NavigateBack : AuthEffect
    data class ShowError(val message: String) : AuthEffect
    data class ShowMessage(val message: String) : AuthEffect
    data class LaunchGoogleSignIn(val serverClientId: String) : AuthEffect
}
