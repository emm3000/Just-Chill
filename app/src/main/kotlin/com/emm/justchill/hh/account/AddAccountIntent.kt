package com.emm.justchill.hh.account

import com.emm.justchill.core.mvi.UiIntent

sealed interface AddAccountIntent : UiIntent {

    data class OnNameChange(val value: String) : AddAccountIntent

    data object OnSave : AddAccountIntent
}
