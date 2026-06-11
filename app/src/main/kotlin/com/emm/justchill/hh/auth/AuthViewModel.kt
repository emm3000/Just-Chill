package com.emm.justchill.hh.auth

import android.util.Log
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val googleServerClientId: String,
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

            AuthIntent.GoogleSignInClicked -> launchGoogleSignIn()

            is AuthIntent.GoogleTokenReceived -> exchangeGoogleToken(intent.idToken, intent.rawNonce)

            AuthIntent.GoogleSignInCancelled -> updateState { copy(isLoading = false) }

            AuthIntent.GoogleSignInUnavailable -> {
                updateState { copy(isLoading = false) }
                // Credential-retrieval failure: happens before any domain call, so it never becomes
                // a DomainException — it is a VM-level UX message like the "Te mandamos un correo"
                // string, NOT a bypass of toUserMessage().
                sendEffect(AuthEffect.ShowError("No encontramos una cuenta de Google en este teléfono."))
            }

            is AuthIntent.GoogleSignInErrored -> {
                updateState { copy(isLoading = false) }
                Log.w(TAG, "Google credential flow failed", intent.cause)
                // Credential-retrieval failure: same rationale as GoogleSignInUnavailable above.
                sendEffect(AuthEffect.ShowError("No se pudo iniciar sesión con Google."))
            }
        }
    }

    private fun launchGoogleSignIn() {
        if (currentState.isLoading) return
        if (googleServerClientId.isBlank()) {
            Log.w(TAG, "GOOGLE_WEB_CLIENT_ID not configured")
            sendEffect(AuthEffect.ShowError("No se pudo iniciar sesión con Google."))
            return
        }
        updateState { copy(isLoading = true) }
        sendEffect(AuthEffect.LaunchGoogleSignIn(googleServerClientId))
    }

    private fun exchangeGoogleToken(idToken: String, rawNonce: String) {
        // isLoading is already true (set by GoogleSignInClicked); do NOT guard on it here.
        launchSafe(onError = { e -> AuthEffect.ShowError(e.toUserMessage()) }) {
            try {
                signInWithGoogle(idToken, rawNonce)
                sendEffect(AuthEffect.NavigateBack)
            } finally {
                updateState { copy(isLoading = false) }
            }
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

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
