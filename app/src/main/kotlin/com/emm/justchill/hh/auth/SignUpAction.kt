package com.emm.justchill.hh.auth

sealed interface SignUpAction {

    class OnEmailChange(val value: String) : SignUpAction

    class OnPasswordChange(val value: String) : SignUpAction

    class OnConfirmPasswordChange(val value: String) : SignUpAction

    class OnCheckedChange(val value: Boolean) : SignUpAction

    object SignUp : SignUpAction
}