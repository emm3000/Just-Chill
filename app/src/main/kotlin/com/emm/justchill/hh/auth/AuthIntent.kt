package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiIntent

sealed interface AuthIntent : UiIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data object ToggleMode : AuthIntent
    data object Submit : AuthIntent
    data object Back : AuthIntent
    data class GoogleSignInResult(val result: GoogleCredentialClient.Result) : AuthIntent
}
