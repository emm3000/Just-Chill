package com.emm.justchill.hh.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.auth.AuthenticateUserUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.error.toUserMessage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class LoginViewModel(private val userAuthenticator: AuthenticateUserUseCase) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    init {
        combine(
            snapshotFlow { state.email },
            snapshotFlow { state.password },
        ) { email, password ->
            val isValidFields = email.isNotBlank() && password.isNotBlank()
            state = state.copy(isValidFields = isValidFields)
        }
            .launchIn(viewModelScope)
    }

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.Login -> login()
            is LoginAction.UpdateEmail -> state = state.copy(email = action.value)
            is LoginAction.UpdatePassword -> state = state.copy(password = action.value)
        }
    }

    private fun login() = viewModelScope.launch {
        tryLogin()
    }

    private suspend fun tryLogin() = try {
        state = state.copy(isLoading = true)
        val email = Email(state.email)
        val password = Password(state.password)
        userAuthenticator(email, password)
        state = state.copy(successLogin = true)
    } catch (e: DomainException) {
        FirebaseCrashlytics.getInstance().recordException(e)
        state = state.copy(isLoading = false, errorMsg = e.toUserMessage())
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(e)
        state = state.copy(isLoading = false, errorMsg = DomainException.Unknown(e).toUserMessage())
    }
}
