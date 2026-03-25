package com.emm.domain.auth

class AuthenticateUserUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(email: Email, password: Password) {
        repository.login(email, password)
    }
}
