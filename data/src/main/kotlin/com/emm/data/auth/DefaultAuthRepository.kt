package com.emm.data.auth

import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.auth.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DefaultAuthRepository() : AuthRepository {

    override val sessionStatus: Flow<SessionStatus>
        get() = flowOf(SessionStatus.Initializing)

    override suspend fun login(email: Email, password: Password) {

    }

    override suspend fun register(email: Email, password: Password) {

    }

    override suspend fun logout() {
    }
}