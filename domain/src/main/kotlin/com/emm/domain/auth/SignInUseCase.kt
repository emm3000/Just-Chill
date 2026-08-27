package com.emm.domain.auth

class SignInUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): AuthUser {
        validateSignInCredentials(email, password)
        return authRepository.signIn(email, password)
    }
}
