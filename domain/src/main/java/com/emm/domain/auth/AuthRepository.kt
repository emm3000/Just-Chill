package com.emm.domain.auth

interface AuthRepository {

    suspend fun login(email: Email, password: Password)

    suspend fun create(email: Email, password: Password)

    fun session(): Any?

    fun saveUserInputs(email: String, password: String)

    fun retrieveUserInputs(): Pair<String, String>
}