package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiIntent

sealed interface LoginIntent : UiIntent {

    data object Submit : LoginIntent

    data class UpdateEmail(val value: String) : LoginIntent

    data class UpdatePassword(val value: String) : LoginIntent
}
