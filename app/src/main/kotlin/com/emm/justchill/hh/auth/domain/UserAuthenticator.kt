package com.emm.justchill.hh.auth.domain

class UserAuthenticator(
    private val repository: AuthRepository
) {

    suspend fun authenticate(email: Email, password: Password) {
        repository.saveUserInputs(email.value, password.value)
        repository.login(email, password)
    }
}