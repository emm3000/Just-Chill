package com.emm.domain.auth

sealed interface SessionStatus {
    data object Initializing : SessionStatus
    data class Authenticated(val user: AuthUser) : SessionStatus
    data object NotAuthenticated : SessionStatus
}
