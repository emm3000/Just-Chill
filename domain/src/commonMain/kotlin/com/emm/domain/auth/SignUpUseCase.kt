package com.emm.domain.auth

class SignUpUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): SignUpResult {
        validateSignUpCredentials(email, password)
        val user = authRepository.signUp(email, password)
        return if (user != null) SignUpResult.SignedIn(user) else SignUpResult.ConfirmationPending
    }
}
