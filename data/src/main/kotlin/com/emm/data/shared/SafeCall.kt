package com.emm.data.shared

import android.database.sqlite.SQLiteException
import com.emm.domain.shared.error.DomainException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
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

suspend fun <T> safeApiCall(block: suspend () -> T): T = try {
    block()
} catch (e: UnauthorizedRestException) {
    throw DomainException.Unauthorized()
} catch (e: RestException) {
    if (e.statusCode == 401) throw DomainException.Unauthorized()
    else throw DomainException.NetworkUnavailable(e)
} catch (e: HttpRequestException) {
    throw DomainException.NetworkUnavailable(e)
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
