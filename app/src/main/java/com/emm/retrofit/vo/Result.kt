package com.emm.retrofit.vo

sealed interface Result<out T> {

    data object Loading : Result<Nothing>

    data class Success<T>(val data: T) : Result<T>

    data class Failure(val exception: Exception) : Result<Nothing>
}