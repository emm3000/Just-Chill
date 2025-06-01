package com.emm.justchill.hh.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.auth.UserAuthenticator
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class LoginViewModel(private val userAuthenticator: UserAuthenticator) : ViewModel() {

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

    fun login() = viewModelScope.launch {
        tryLogin()
    }

    private suspend fun tryLogin() = try {
        state = state.copy(isLoading = true)
        val email = Email(state.email)
        val password = Password(state.password)
        userAuthenticator.authenticate(email, password)
        state = state.copy(successLogin = true)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        errorState(e)
    }

    private fun errorState(e: Throwable) {
        state = state.copy(isLoading = false, errorMsg = e.message)
    }
}