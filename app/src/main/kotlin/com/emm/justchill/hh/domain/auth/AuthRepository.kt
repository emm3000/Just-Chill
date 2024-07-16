package com.emm.justchill.hh.domain.auth

import io.github.jan.supabase.gotrue.user.UserInfo

interface AuthRepository {

    suspend fun login(email: Email, password: Password)

    suspend fun create(email: Email, password: Password)

    fun session(): UserInfo?
}