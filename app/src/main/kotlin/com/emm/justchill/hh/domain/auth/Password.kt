package com.emm.justchill.hh.domain.auth

@JvmInline
value class Password(val value: String) {

    init {
        require(value.length >= 8) { "La contraseña debe tener al menos 8 caracteres" }
    }
}