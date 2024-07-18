package com.emm.justchill.hh.presentation.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.auth.Email
import com.emm.justchill.hh.domain.auth.Password
import com.emm.justchill.hh.domain.auth.UserAuthenticator
import com.emm.justchill.hh.domain.auth.UserCreator
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userAuthenticator: UserAuthenticator,
    private val userCreator: UserCreator,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    private val _loginState: MutableStateFlow<LoginUi> = MutableStateFlow(LoginUi())
    val loginState: StateFlow<LoginUi> get() = _loginState.asStateFlow()

    init {
        authRepository.retrieveUserInputs().apply {
            email = first
            password = second
        }
        combine(
            snapshotFlow { email },
            snapshotFlow { password },
        ) { email, password ->
            "$email - $password"
        }
            .onEach { Log.e("aea", it) }
            .launchIn(viewModelScope)
    }

    fun updateEmail(value: String) {
        email = value
    }

    fun updatePassword(value: String) {
        password = value
    }

    fun login() = viewModelScope.launch {
        _loginState.update {
            it.copy(isLoading = true)
        }
        tryLogin()
    }

    fun register() = viewModelScope.launch {
        _loginState.update {
            it.copy(isLoading = true)
        }
        tryRegister()
    }

    private suspend fun tryRegister() = try {
        val email = Email(email)
        val password = Password(password)
        userCreator.create(email, password)
        userAuthenticator.authenticate(email, password)
        _loginState.update { it.copy(successLogin = true) }
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        _loginState.update {
            it.copy(
                isLoading = false,
                errorMsg = e.message
            )
        }
    }

    private suspend fun tryLogin() = try {
        val email = Email(email)
        val password = Password(password)
        userAuthenticator.authenticate(email, password)
        _loginState.update { it.copy(successLogin = true) }
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        _loginState.update {
            it.copy(
                isLoading = false,
                errorMsg = e.message
            )
        }
    }
}

data class LoginUi(
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val successLogin: Boolean = false,
)