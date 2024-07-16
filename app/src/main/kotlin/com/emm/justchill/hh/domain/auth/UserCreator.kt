package com.emm.justchill.hh.domain.auth

class UserCreator(private val repository: AuthRepository) {

    suspend fun create(email: Email, password: Password) {
        repository.create(email, password)
    }
}
