package com.emm.domain.shared.error

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NotFound(entity: String) : DomainException("$entity not found")

    /**
     * The input was rejected by a domain rule.
     *
     * [message] is English and diagnostic; [code] is what the UI translates. See [ValidationCode].
     */
    class ValidationError(
        message: String,
        val code: ValidationCode = ValidationCode.Unspecified,
        cause: Throwable? = null,
    ) : DomainException(message, cause)

    class DatabaseError(cause: Throwable) : DomainException(cause.message ?: "Database error", cause)

    /** Authentication failed or credentials were rejected (e.g. wrong password, invalid token). */
    class Unauthorized(message: String, cause: Throwable? = null) : DomainException(message, cause)

    /**
     * The operation could not be completed because the network was unreachable or timed out.
     *
     * [message] is English and diagnostic, exactly like [ValidationError]'s, and it defaults to the
     * cause's own text — the right answer whenever the throwable already names what failed. A caller
     * passes its own when the throwable does NOT: a pipeline whose steps all fail as the same
     * transport error needs the step in the message, because that is the only thing separating them
     * in a log. ADR 009's hard constraint 4 (no silent failure in the backup pipeline) is what asks
     * for it — uploading a snapshot, reading it back and deleting an unverified one are three
     * different outages behind one `HttpRequestException`.
     */
    class NetworkUnavailable(cause: Throwable, message: String = cause.message ?: "Network unavailable") :
        DomainException(message, cause)

    /** [message] is diagnostic and defaults to the cause's; see [NetworkUnavailable] for when to pass one. */
    class Unknown(cause: Throwable, message: String = cause.message ?: "Unknown error") :
        DomainException(message, cause)

    /**
     * Data the caller already trusts could not be turned into (or out of) the wire format — a
     * `kotlinx.serialization` encode/decode failure that is neither a database failure nor a
     * rejection of untrusted input.
     *
     * Distinct from [Unknown]: ADR 009's Phase 3 requires the backup pipeline to log a NAMED reason
     * for every failure mode it can hit (serialization, network, hash mismatch, storage error), and
     * folding this into [Unknown] would erase exactly the fact that answers "which one broke".
     * Distinct from [ValidationError]'s `BackupFileInvalid` / `BackupVersionUnsupported`: those are
     * for a FILE the reader does not trust and rejects on purpose; this is the pipeline's own
     * encoder or decoder failing on data it already trusts (e.g. a snapshot just read from the
     * local database, not yet handed to anyone).
     */
    class SerializationError(cause: Throwable, message: String = cause.message ?: "Serialization error") :
        DomainException(message, cause)
}
