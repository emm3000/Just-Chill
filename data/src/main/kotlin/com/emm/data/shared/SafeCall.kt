package com.emm.data.shared

import android.database.sqlite.SQLiteException
import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

suspend fun <T> safeDbCall(block: suspend () -> T): T = try {
    block()
} catch (e: SQLiteException) {
    throw DomainException.DatabaseError(e)
} catch (e: DomainException) {
    throw e
} catch (e: Exception) {
    throw DomainException.Unknown(e)
}

fun <T> Flow<T>.catchAsDomainException(): Flow<T> = catch { e ->
    when (e) {
        is DomainException -> throw e
        is SQLiteException -> throw DomainException.DatabaseError(e)
        else -> throw DomainException.Unknown(e)
    }
}
