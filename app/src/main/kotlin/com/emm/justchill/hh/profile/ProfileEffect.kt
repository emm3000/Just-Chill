package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiEffect

sealed interface ProfileEffect : UiEffect {
    data class ShowMessage(val text: String) : ProfileEffect
}
