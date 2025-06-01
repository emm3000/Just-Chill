package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val sessionStatus: Flow<SessionStatus>

    suspend fun login(email: Email, password: Password)

    suspend fun register(email: Email, password: Password)

    suspend fun logout()
}