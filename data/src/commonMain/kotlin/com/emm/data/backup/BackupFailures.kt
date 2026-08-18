package com.emm.data.backup

import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Caught broadly and translated into a DomainException below, never swallowed; CancellationException
 * is rethrown first so a cancelled operation is never reported as a failure.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> storageCall(reason: String, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    throw e.asBackupFailure(reason)
}

internal fun Throwable.asBackupFailure(reason: String): DomainException = when (this) {
    is DomainException -> this

    is UnauthorizedRestException -> DomainException.Unauthorized(reason, this)

    is SessionRequiredException -> DomainException.Unauthorized(reason, this)

    // Below the two auth branches on purpose: UnauthorizedRestException is a RestException, and a
    // session that expired must stay Unauthorized.
    is RestException -> DomainException.RemoteRejected(
        "$reason The server answered HTTP $statusCode: $error.",
        statusCode,
        this,
    )

    is HttpRequestException,
    is HttpRequestTimeoutException,
    -> DomainException.NetworkUnavailable(this, reason)

    else -> DomainException.Unknown(this, reason)
}
