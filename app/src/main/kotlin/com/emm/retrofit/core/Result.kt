package com.emm.retrofit.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface Result<out T> {

    data object Loading : Result<Nothing>

    data class Success<T>(val data: T) : Result<T>

    data class Failure(val exception: Throwable) : Result<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Failure(it)) }

inline fun <T, X> Flow<Result<T>>.mapResult(
    crossinline mapping: (T) -> X
): Flow<Result<X>> = map { result ->
    when (result) {
        is Result.Failure -> Result.Failure(result.exception)
        Result.Loading -> Result.Loading
        is Result.Success -> Result.Success(mapping(result.data))
    }
}