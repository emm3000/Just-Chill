package com.emm.justchill.core.domain.auth

import kotlinx.coroutines.flow.Flow

class GetSessionStatusUseCase(private val authRepository: AuthRepository) {

    operator fun invoke(): Flow<SessionStatus> = authRepository.sessionStatus
}
