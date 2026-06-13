package com.emm.domain.shared.error

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotFound(entity: String) : DomainException("$entity not found")

    class ValidationError(message: String, cause: Throwable? = null) : DomainException(message, cause)

    class DatabaseError(cause: Throwable) : DomainException(cause.message ?: "Database error", cause)

    /** Authentication failed or credentials were rejected (e.g. wrong password, invalid token). */
    class Unauthorized(message: String, cause: Throwable? = null) : DomainException(message, cause)

    /** The operation could not be completed because the network was unreachable or timed out. */
    class NetworkUnavailable(cause: Throwable) : DomainException(cause.message ?: "Network unavailable", cause)

    class Unknown(cause: Throwable) : DomainException(cause.message ?: "Unknown error", cause)
}
