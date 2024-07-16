package com.emm.justchill.hh.data.auth

import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.auth.Email as EmailModel
import com.emm.justchill.hh.domain.auth.Password
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo

class DefaultAuthRepository(
    private val client: SupabaseClient,
) : AuthRepository {

    override suspend fun login(email: EmailModel, password: Password) {
        client.auth.signInWith(Email) {
            this.email = email.value
            this.password = password.value
        }
    }

    override suspend fun create(email: EmailModel, password: Password) {
        client.auth.signUpWith(Email) {
            this.email = email.value
            this.password = password.value
        }
    }

    override fun session(): UserInfo? {
        return client.auth.currentSessionOrNull()?.user
    }
}