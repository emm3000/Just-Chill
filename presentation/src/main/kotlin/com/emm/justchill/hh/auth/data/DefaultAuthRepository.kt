package com.emm.justchill.hh.auth.data

import android.content.SharedPreferences
import com.emm.justchill.hh.auth.domain.AuthRepository
import com.emm.justchill.hh.auth.domain.Email as EmailModel
import com.emm.justchill.hh.auth.domain.Password
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo

class DefaultAuthRepository(
    private val client: SupabaseClient,
    private val sharedPreferences: SharedPreferences,
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

    override fun saveUserInputs(email: String, password: String) {
        sharedPreferences.edit()
            .putString(EMAIL_KEY, email)
            .putString(PASSWORD_KEY, password)
            .apply()
    }

    override fun retrieveUserInputs(): Pair<String, String> {
        val email = sharedPreferences.getString(EMAIL_KEY, "").orEmpty()
        val password = sharedPreferences.getString(PASSWORD_KEY, "").orEmpty()
        return Pair(email, password)
    }

    companion object {

        private const val EMAIL_KEY = "EMAIL_KEY"
        private const val PASSWORD_KEY = "PASSWORD_KEY"
    }
}