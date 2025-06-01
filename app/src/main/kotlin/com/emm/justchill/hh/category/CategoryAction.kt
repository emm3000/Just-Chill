package com.emm.justchill.hh.category

sealed interface CategoryAction {

    class OnNameChange(val value: String) : CategoryAction

    data object OnSave : CategoryAction
}