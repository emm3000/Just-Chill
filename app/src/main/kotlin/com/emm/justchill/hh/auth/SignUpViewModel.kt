@file:Suppress("OPT_IN_USAGE")

package com.emm.justchill.hh.auth

import androidx.lifecycle.viewModelScope
import com.emm.domain.auth.CreateUserUseCase
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val userCreator: CreateUserUseCase,
) : MviViewModel<SignUpUiState, SignUpIntent, SignUpEffect>() {

    override val initialState = SignUpUiState()

    private val validationTrigger = MutableStateFlow(SignUpUiState())

    init {
        validationTrigger
            .debounce(300L)
            .onEach { s ->
                val isValidPassword = s.password.length >= 6
                val isValidConfirmPassword = s.password == s.confirmPassword
                val isValidFields = isValidPassword && isValidConfirmPassword && s.isChecked
                updateState {
                    copy(
                        isValidFields = isValidFields,
                        emailError = null,
                        passwordError = if (isValidPassword) null else "Ingrese al menos 6 caracteres",
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SignUpIntent) {
        when (intent) {
            is SignUpIntent.OnEmailChange -> updateState { copy(email = intent.value) }.also { triggerValidation() }
            is SignUpIntent.OnPasswordChange -> updateState { copy(password = intent.value) }.also { triggerValidation() }
            is SignUpIntent.OnConfirmPasswordChange -> updateState { copy(confirmPassword = intent.value) }.also { triggerValidation() }
            is SignUpIntent.OnCheckedChange -> updateState { copy(isChecked = intent.value) }.also { triggerValidation() }
            SignUpIntent.SignUp -> executeSignUp()
        }
    }

    private fun triggerValidation() {
        validationTrigger.value = currentState
    }

    private fun executeSignUp() = viewModelScope.launch {
        updateState { copy(isLoading = true) }
        try {
            userCreator(
                email = Email(currentState.email),
                password = Password(currentState.password),
            )
            sendEffect(SignUpEffect.NavigateBack)
        } catch (e: DomainException) {
            updateState { copy(isLoading = false) }
            sendEffect(SignUpEffect.ShowError(e.toUserMessage()))
        } catch (e: Exception) {
            updateState { copy(isLoading = false) }
            sendEffect(SignUpEffect.ShowError(DomainException.Unknown(e).toUserMessage()))
        }
    }
}
