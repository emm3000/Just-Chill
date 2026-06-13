package com.emm.data.shared

import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Suppress("TooGenericExceptionCaught")
suspend fun <T> safeDbCall(block: suspend () -> T): T = try {
    block()
} catch (e: DomainException) {
    throw e
} catch (e: Exception) {
    if (e.isSqliteException()) throw DomainException.DatabaseError(e)
    throw DomainException.Unknown(e)
}

fun <T> Flow<T>.catchAsDomainException(): Flow<T> = catch { e ->
    when {
        e is DomainException -> throw e
        e.isSqliteException() -> throw DomainException.DatabaseError(e)
        else -> throw DomainException.Unknown(e)
    }
}
