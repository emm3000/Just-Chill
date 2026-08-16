package com.emm.domain.auth

class SignOutUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(): SignOutResult = authRepository.signOut()
}
