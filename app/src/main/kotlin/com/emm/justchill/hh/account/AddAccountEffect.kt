package com.emm.justchill.hh.account

import com.emm.justchill.core.mvi.UiEffect

sealed interface AddAccountEffect : UiEffect {

    data object AccountSaved : AddAccountEffect

    data class ShowError(val message: String) : AddAccountEffect
}
