package com.emm.justchill.feature.account

import com.emm.justchill.core.presentation.mvi.UiEffect

sealed interface AccountsEffect : UiEffect {
    data class ShowMessage(val text: String) : AccountsEffect
}
