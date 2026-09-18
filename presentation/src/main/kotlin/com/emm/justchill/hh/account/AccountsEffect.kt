package com.emm.justchill.hh.account

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AccountsEffect : UiEffect {
    data class ShowMessage(val text: String) : AccountsEffect
}
