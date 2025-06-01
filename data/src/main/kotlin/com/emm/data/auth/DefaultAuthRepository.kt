package com.emm.data.auth

import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override val sessionStatus: Flow<com.emm.domain.auth.SessionStatus>
        get() = client.auth.sessionStatus
            .map { sessionStatus ->
                when (sessionStatus) {
                    is SessionStatus.Authenticated -> com.emm.domain.auth.SessionStatus.Authenticated
                    SessionStatus.Initializing -> com.emm.domain.auth.SessionStatus.Initializing
                    is SessionStatus.NotAuthenticated -> com.emm.domain.auth.SessionStatus.NotAuthenticated
                    else -> com.emm.domain.auth.SessionStatus.NotAuthenticated
                }
            }

    override suspend fun login(email: Email, password: Password) = withContext(Dispatchers.IO) {
        client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email.value
            this.password = password.value
        }
    }

    override suspend fun register(email: Email, password: Password) = withContext(Dispatchers.IO) {
        client.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email.value
            this.password = password.value
        }
        Unit
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        client.auth.signOut(SignOutScope.GLOBAL)
    }
}