package com.emm.justchill.hh.domain.auth

@JvmInline
value class Email(val value: String) {

    init {
        require(isValidEmail(value)) { "Ingresa un email valido" }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return emailRegex.matches(email)
    }

}