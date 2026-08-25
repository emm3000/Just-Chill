package com.emm.data.shared

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlin.coroutines.cancellation.CancellationException

// CancellationException is rethrown first because it is an Exception on every target: folded into
// DomainException it would stop propagating, and a cancelled caller would run on to its next line.
@Suppress("TooGenericExceptionCaught")
suspend fun <T> safeDbCall(block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: DomainException) {
    throw e
} catch (e: Exception) {
    if (e.isSqliteException()) throw DomainException.DatabaseError(e)
    throw DomainException.Unknown(e)
}

fun <T> Flow<T>.catchAsDomainException(): Flow<T> = catch { e ->
    when {
        e is CancellationException -> throw e
        e is DomainException -> throw e
        e.isSqliteException() -> throw DomainException.DatabaseError(e)
        else -> throw DomainException.Unknown(e)
    }
}
