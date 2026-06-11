package com.emm.domain.auth

/**
 * Re-sends the sign-up confirmation email to the given address.
 *
 * Validates the email format before delegating to the repository so the call
 * is never made with obviously invalid input.
 */
class ResendConfirmationEmailUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String) {
        validateEmail(email)
        authRepository.resendConfirmationEmail(email)
    }
}
