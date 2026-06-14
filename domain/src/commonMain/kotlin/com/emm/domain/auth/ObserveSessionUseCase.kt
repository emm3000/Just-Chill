package com.emm.domain.auth

import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase(private val authRepository: AuthRepository) {

    operator fun invoke(): Flow<SessionStatus> = authRepository.sessionStatus

    /**
     * Suspends until the auth provider has finished loading any persisted session. Call this before
     * the first authenticated request of a sync cycle so the provider can attach the JWT (see
     * [AuthRepository.awaitSessionInitialization]).
     */
    suspend fun awaitInitialization() = authRepository.awaitSessionInitialization()
}
