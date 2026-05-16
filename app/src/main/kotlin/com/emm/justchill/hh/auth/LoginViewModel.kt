package com.emm.justchill.hh.auth

import androidx.lifecycle.viewModelScope
import com.emm.domain.auth.AuthenticateUserUseCase
import com.emm.domain.auth.Email
import com.emm.domain.auth.Password
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authenticateUser: AuthenticateUserUseCase,
) : MviViewModel<LoginUiState, LoginIntent, LoginEffect>() {

    override val initialState = LoginUiState()

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> updateState { copy(email = intent.value).recomputeValidity() }
            is LoginIntent.UpdatePassword -> updateState { copy(password = intent.value).recomputeValidity() }
            LoginIntent.Submit -> submit()
        }
    }

    private fun LoginUiState.recomputeValidity(): LoginUiState =
        copy(isValidFields = email.isNotBlank() && password.isNotBlank())

    private fun submit() = viewModelScope.launch {
        updateState { copy(isLoading = true) }
        try {
            authenticateUser(Email(currentState.email), Password(currentState.password))
            sendEffect(LoginEffect.NavigateToHome)
        } catch (e: DomainException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            updateState { copy(isLoading = false) }
            sendEffect(LoginEffect.ShowError(e.toUserMessage()))
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            updateState { copy(isLoading = false) }
            sendEffect(LoginEffect.ShowError(DomainException.Unknown(e).toUserMessage()))
        }
    }
}
