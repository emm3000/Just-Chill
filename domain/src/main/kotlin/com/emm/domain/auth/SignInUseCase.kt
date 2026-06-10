package com.emm.domain.auth

class SignInUseCase(
    private val authRepository: AuthRepository,
    private val claimLocalDataUseCase: ClaimLocalDataUseCase,
) {

    suspend operator fun invoke(email: String, password: String): AuthUser {
        validateCredentials(email, password)
        val user = authRepository.signIn(email, password)
        claimLocalDataUseCase(user.userId)
        return user
    }
}
