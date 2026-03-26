package com.emm.data.auth

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeApiCall
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.auth.SessionStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override val sessionStatus: Flow<SessionStatus>
        get() = client.auth.sessionStatus
            .map { sessionStatus: io.github.jan.supabase.auth.status.SessionStatus ->
                when (sessionStatus) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> SessionStatus.Authenticated
                    io.github.jan.supabase.auth.status.SessionStatus.Initializing -> SessionStatus.Initializing
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> SessionStatus.NotAuthenticated
                    else -> SessionStatus.NotAuthenticated
                }
            }.flowOn(Dispatchers.IO)
            .catchAsDomainException()

    override suspend fun login(email: Email, password: Password) = safeApiCall {
        withContext(Dispatchers.IO) {
            client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email.value
                this.password = password.value
            }
        }
    }

    override suspend fun register(email: Email, password: Password) = safeApiCall {
        withContext(Dispatchers.IO) {
            client.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email.value
                this.password = password.value
            }
            Unit
        }
    }

    override suspend fun logout() = safeApiCall {
        withContext(Dispatchers.IO) {
            client.auth.signOut()
        }
    }
}