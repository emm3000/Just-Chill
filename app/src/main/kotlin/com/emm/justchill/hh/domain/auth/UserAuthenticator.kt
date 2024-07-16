package com.emm.justchill.hh.domain.auth

class UserAuthenticator(private val repository: AuthRepository) {

    suspend fun authenticate(email: Email, password: Password) {
        repository.login(email, password)
    }
}