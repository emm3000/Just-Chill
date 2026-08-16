package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase(private val authRepository: AuthRepository) {

    operator fun invoke(): Flow<SessionStatus> = authRepository.sessionStatus

    suspend fun awaitInitialization() = authRepository.awaitSessionInitialization()
}
