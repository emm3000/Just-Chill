package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiIntent

sealed interface SignUpIntent : UiIntent {

    data class OnEmailChange(val value: String) : SignUpIntent

    data class OnPasswordChange(val value: String) : SignUpIntent

    data class OnConfirmPasswordChange(val value: String) : SignUpIntent

    data class OnCheckedChange(val value: Boolean) : SignUpIntent

    data object SignUp : SignUpIntent
}
