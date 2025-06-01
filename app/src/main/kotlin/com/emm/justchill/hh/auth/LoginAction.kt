package com.emm.justchill.hh.auth

sealed interface LoginAction {

    data object Login : LoginAction

    class UpdateEmail(val value: String) : LoginAction

    class UpdatePassword(val value: String) : LoginAction
}