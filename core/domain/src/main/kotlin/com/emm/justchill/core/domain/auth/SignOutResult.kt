package com.emm.justchill.core.domain.auth

sealed interface SignOutResult {

    data object Revoked : SignOutResult

    data object LocalOnly : SignOutResult
}
