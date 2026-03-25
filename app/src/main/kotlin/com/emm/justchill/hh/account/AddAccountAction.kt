package com.emm.justchill.hh.account

sealed interface AddAccountAction {

    class OnNameChange(val value: String) : AddAccountAction

    data object OnSave : AddAccountAction
}
