package com.emm.domain.auth

/**
 * Creates a new account. Claiming anonymous-local rows is NOT done here — it is handled by
 * [ClaimLocalDataOnAuthenticationUseCase], which reacts to the session becoming Authenticated
 * (whether the session is established now or after email confirmation + a later sign-in).
 */
class SignUpUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): AuthUser? {
        validateSignUpCredentials(email, password)
        return authRepository.signUp(email, password)
    }
}
