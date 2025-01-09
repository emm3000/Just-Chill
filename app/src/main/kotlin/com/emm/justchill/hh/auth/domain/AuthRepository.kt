package com.emm.justchill.hh.auth.domain

import io.github.jan.supabase.auth.user.UserInfo

interface AuthRepository {

    suspend fun login(email: Email, password: Password)

    suspend fun create(email: Email, password: Password)

    fun session(): UserInfo?

    fun saveUserInputs(email: String, password: String)

    fun retrieveUserInputs(): Pair<String, String>
}