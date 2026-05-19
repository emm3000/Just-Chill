package com.emm.domain.shared.error

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotFound(entity: String) : DomainException("$entity not found")

    class ValidationError(message: String) : DomainException(message)

    class NetworkUnavailable(cause: Throwable? = null) : DomainException("Network unavailable", cause)

    class DatabaseError(cause: Throwable) : DomainException(cause.message ?: "Database error", cause)

    class Unknown(cause: Throwable) : DomainException(cause.message ?: "Unknown error", cause)
}
