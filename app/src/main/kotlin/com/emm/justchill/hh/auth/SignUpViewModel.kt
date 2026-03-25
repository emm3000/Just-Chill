@file:Suppress("OPT_IN_USAGE")

package com.emm.justchill.hh.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.auth.CreateUserUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class SignUpViewModel(private val userCreator: CreateUserUseCase) : ViewModel() {

    var state by mutableStateOf(SignUpUiState())
        private set

    init {
        combine(
            flow = snapshotFlow { state.email },
            flow2 = snapshotFlow { state.password },
            flow3 = snapshotFlow { state.confirmPassword },
            flow4 = snapshotFlow { state.isChecked },
            transform = ::validateSignUpFields,
        )
            .debounce(300L)
            .launchIn(viewModelScope)
    }

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.OnEmailChange -> state = state.copy(email = action.value)
            is SignUpAction.OnPasswordChange -> state = state.copy(password = action.value)
            is SignUpAction.OnConfirmPasswordChange -> state = state.copy(confirmPassword = action.value)
            is SignUpAction.OnCheckedChange -> state = state.copy(isChecked = action.value)
            SignUpAction.SignUp -> executeSignUp()
        }
    }

    private fun executeSignUp() = viewModelScope.launch {
        try {
            state = state.copy(isLoading = true)
            userCreator(
                email = Email(state.email),
                password = Password(state.password),
            )
            state = state.copy(success = true)
        } catch (e: Exception) {
            state = state.copy(error = e.stackTraceToString(), isLoading = false)
        }
    }

    private fun validateSignUpFields(email: String, password: String, confirmPassword: String, isChecked: Boolean) {
        val isValidPassword = password.length >= 6
        val isValidConfirmPassword = password == confirmPassword
        val isValidFields: Boolean = isValidPassword && isValidConfirmPassword && isChecked
        val passwordError = if (isValidPassword.not()) "Ingrese al menos 6 caracteres" else null
        state = state.copy(
            isValidFields = isValidFields,
            emailError = null,
            passwordError = passwordError,
        )
    }
}
