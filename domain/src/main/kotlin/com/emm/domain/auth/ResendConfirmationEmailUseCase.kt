package com.emm.domain.auth

class ResendConfirmationEmailUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String) {
        validateEmail(email)
        authRepository.resendConfirmationEmail(email)
    }
}
