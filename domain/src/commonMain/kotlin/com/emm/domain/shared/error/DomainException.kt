package com.emm.domain.shared.error

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotFound(entity: String) : DomainException("$entity not found")

    class ValidationError(
        message: String,
        val code: ValidationCode = ValidationCode.Unspecified,
        cause: Throwable? = null,
    ) : DomainException(message, cause)

    class DatabaseError(cause: Throwable) : DomainException(cause.message ?: "Database error", cause)

    class Unauthorized(message: String, cause: Throwable? = null) : DomainException(message, cause)

    class Busy(message: String, cause: Throwable? = null) : DomainException(message, cause)

    class NetworkUnavailable(cause: Throwable, message: String = cause.message ?: "Network unavailable") :
        DomainException(message, cause)

    // The status is a field so a caller never has to parse the sentence to branch on it.
    class RemoteRejected(message: String, val statusCode: Int, cause: Throwable) : DomainException(message, cause)

    class Unknown(cause: Throwable, message: String = cause.message ?: "Unknown error") :
        DomainException(message, cause)

    class SerializationError(cause: Throwable?, message: String = cause?.message ?: "Serialization error") :
        DomainException(message, cause)
}
