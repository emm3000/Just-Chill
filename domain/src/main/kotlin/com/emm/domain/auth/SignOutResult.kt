package com.emm.domain.auth

sealed interface SignOutResult {

    data object Revoked : SignOutResult

    data object LocalOnly : SignOutResult
}
