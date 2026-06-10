package com.emm.domain.auth

/**
 * Authenticates an existing account. Claiming anonymous-local rows is NOT done here — it is handled
 * by [ClaimLocalDataOnAuthenticationUseCase], which reacts to the session becoming Authenticated.
 * This keeps a claim failure from breaking the sign-in flow.
 */
class SignInUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String): AuthUser {
        validateCredentials(email, password)
        return authRepository.signIn(email, password)
    }
}
