package com.emm.justchill.core.error

import com.emm.domain.shared.error.DomainException

fun DomainException.toUserMessage(): String = when (this) {
    is DomainException.NotFound -> "No encontré eso"
    is DomainException.ValidationError -> message ?: "Algo no cuadra con los datos"
    is DomainException.NetworkUnavailable -> "Sin conexión"
    is DomainException.DatabaseError -> "Hubo un problema guardando tu data"
    is DomainException.Unknown -> "Algo se rompió — capaz reinicia la app?"
}
