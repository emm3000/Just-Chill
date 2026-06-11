package com.emm.justchill.hh.auth

import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
) : MviViewModel<AuthUiState, AuthIntent, AuthEffect>() {

    override val initialState = AuthUiState()

    override fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> updateState { copy(email = intent.value) }

            is AuthIntent.PasswordChanged -> updateState { copy(password = intent.value) }

            AuthIntent.ToggleMode -> updateState {
                copy(mode = if (mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn)
            }

            AuthIntent.Submit -> submit()

            AuthIntent.Back -> sendEffect(AuthEffect.NavigateBack)

            is AuthIntent.GoogleSignInResult -> handleGoogleResult(intent.result)
        }
    }

    private fun handleGoogleResult(result: GoogleCredentialClient.Result) {
        when (result) {
            is GoogleCredentialClient.Result.Success -> {
                if (currentState.isLoading) return
                updateState { copy(isLoading = true) }
                launchSafe(onError = { e -> AuthEffect.ShowError(e.toUserMessage()) }) {
                    try {
                        signInWithGoogle(result.idToken, result.rawNonce)
                        sendEffect(AuthEffect.NavigateBack)
                    } finally {
                        updateState { copy(isLoading = false) }
                    }
                }
            }
            GoogleCredentialClient.Result.Cancelled -> Unit
            GoogleCredentialClient.Result.NoCredentials -> sendEffect(
                AuthEffect.ShowError("No encontramos una cuenta de Google en este teléfono."),
            )
            is GoogleCredentialClient.Result.Failure -> sendEffect(
                AuthEffect.ShowError("No se pudo iniciar sesión con Google."),
            )
        }
    }

    private fun submit() {
        if (currentState.isLoading) return

        val email = currentState.email.trim()
        val password = currentState.password

        updateState { copy(isLoading = true) }
        launchSafe(
            // isLoading is reset in the finally block below, covering both success and error paths.
            onError = { e -> AuthEffect.ShowError(e.toUserMessage()) },
        ) {
            try {
                when (currentState.mode) {
                    AuthMode.SignIn -> {
                        signIn(email, password)
                        sendEffect(AuthEffect.NavigateBack)
                    }

                    AuthMode.SignUp -> {
                        val user = signUp(email, password)
                        if (user != null) {
                            sendEffect(AuthEffect.NavigateBack)
                        } else {
                            // null means email confirmation is pending (auto-confirm disabled)
                            sendEffect(
                                AuthEffect.ShowMessage(
                                    "Te mandamos un correo — confírmalo y vuelve a iniciar sesión.",
                                ),
                            )
                        }
                    }
                }
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }
}
