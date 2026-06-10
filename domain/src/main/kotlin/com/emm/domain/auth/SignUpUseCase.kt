package com.emm.domain.auth

class SignUpUseCase(
    private val authRepository: AuthRepository,
    private val claimLocalDataUseCase: ClaimLocalDataUseCase,
) {

    suspend operator fun invoke(email: String, password: String): AuthUser? {
        validateCredentials(email, password)
        val user = authRepository.signUp(email, password)
        if (user != null) {
            claimLocalDataUseCase(user.userId)
        }
        return user
    }
}
