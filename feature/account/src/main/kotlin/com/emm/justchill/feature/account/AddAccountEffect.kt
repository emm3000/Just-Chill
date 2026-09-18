package com.emm.justchill.feature.account

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddAccountEffect : UiEffect {

    data object AccountSaved : AddAccountEffect

    data class ShowError(val message: String) : AddAccountEffect
}
