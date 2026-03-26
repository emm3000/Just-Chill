package com.emm.justchill.core.error

import com.emm.domain.shared.error.DomainException

fun DomainException.toUserMessage(): String = when (this) {
    is DomainException.NotFound -> "No se encontró el elemento solicitado"
    is DomainException.ValidationError -> message ?: "Error de validación"
    is DomainException.NetworkUnavailable -> "Sin conexión a internet"
    is DomainException.DatabaseError -> "Error al acceder a los datos"
    is DomainException.Unauthorized -> "Sesión expirada, por favor iniciá sesión nuevamente"
    is DomainException.Unknown -> "Ocurrió un error inesperado"
}
