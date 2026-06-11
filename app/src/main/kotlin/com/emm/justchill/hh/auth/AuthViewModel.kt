package com.emm.justchill.hh.auth

import android.util.Log
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignUpResult
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.core.mvi.MviViewModel

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val resendConfirmationEmail: ResendConfirmationEmailUseCase,
    private val googleServerClientId: String,
    private val googleSignInLauncher: GoogleSignInLauncher,
) : MviViewModel<AuthUiState, AuthIntent, AuthEffect>() {

    override val initialState: AuthUiState = AuthUiState.Form()

    // Routes all Form mutations through this helper — no-ops when state is CheckEmail.
    private inline fun updateForm(crossinline reducer: AuthUiState.Form.() -> AuthUiState) =
        updateState { if (this is AuthUiState.Form) reducer() else this }

    // Routes all CheckEmail mutations through this helper — no-ops when state is Form.
    private inline fun updateCheckEmail(crossinline reducer: AuthUiState.CheckEmail.() -> AuthUiState) =
        updateState { if (this is AuthUiState.CheckEmail) reducer() else this }

    override fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> updateForm { copy(email = intent.value) }

            is AuthIntent.PasswordChanged -> updateForm { copy(password = intent.value) }

            AuthIntent.ToggleMode -> updateForm {
                copy(mode = if (mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn)
            }

            AuthIntent.Submit -> {
                if (currentState !is AuthUiState.Form) return
                submit()
            }

            AuthIntent.Back -> handleBack()

            AuthIntent.OpenEmailApp -> {
                if (currentState !is AuthUiState.CheckEmail) return
                sendEffect(AuthEffect.OpenEmailApp)
            }

            AuthIntent.ResendEmail -> {
                if (currentState !is AuthUiState.CheckEmail) return
                resendEmail()
            }

            AuthIntent.BackToSignIn -> updateState { AuthUiState.Form(mode = AuthMode.SignIn) }

            AuthIntent.GoogleSignInClicked -> {
                if (currentState !is AuthUiState.Form) return
                signInWithGoogleFlow()
            }
        }
    }

    private fun handleBack() {
        when (currentState) {
            is AuthUiState.CheckEmail -> updateState { AuthUiState.Form(mode = AuthMode.SignIn) }
            is AuthUiState.Form -> sendEffect(AuthEffect.NavigateBack)
        }
    }

    private fun resendEmail() {
        val checkEmailState = currentState as? AuthUiState.CheckEmail ?: return
        if (checkEmailState.isResending) return
        updateCheckEmail { copy(isResending = true) }
        launchSafe(onError = { e -> AuthEffect.ShowError(e) }) {
            try {
                resendConfirmationEmail(checkEmailState.email)
                sendEffect(AuthEffect.Notify(AuthMessage.ConfirmationLinkResent))
            } finally {
                updateCheckEmail { copy(isResending = false) }
            }
        }
    }

    private fun signInWithGoogleFlow() {
        val formState = currentState as? AuthUiState.Form ?: return
        if (formState.isSubmitting) return
        if (googleServerClientId.isBlank()) {
            Log.w(TAG, "GOOGLE_WEB_CLIENT_ID not configured")
            sendEffect(AuthEffect.Notify(AuthMessage.GoogleSignInFailed))
            return
        }
        updateForm { copy(isSubmitting = true) }
        launchSafe(onError = { e -> AuthEffect.ShowError(e) }) {
            try {
                when (val result = googleSignInLauncher.signIn(googleServerClientId)) {
                    is GoogleCredentialClient.Result.Success -> {
                        signInWithGoogle(result.idToken, result.rawNonce)
                        sendEffect(AuthEffect.NavigateBack)
                    }
                    GoogleCredentialClient.Result.Cancelled -> Unit // silent per design
                    GoogleCredentialClient.Result.NoCredentials ->
                        sendEffect(AuthEffect.Notify(AuthMessage.GoogleAccountUnavailable))
                    is GoogleCredentialClient.Result.Failure -> {
                        Log.w(TAG, "Google credential flow failed", result.cause)
                        sendEffect(AuthEffect.Notify(AuthMessage.GoogleSignInFailed))
                    }
                }
            } finally {
                updateForm { copy(isSubmitting = false) }
            }
        }
    }

    private fun submit() {
        val formState = currentState as? AuthUiState.Form ?: return
        if (formState.isSubmitting) return

        val email = formState.email.trim()
        val password = formState.password

        updateForm { copy(isSubmitting = true) }
        launchSafe(onError = { e -> AuthEffect.ShowError(e) }) {
            try {
                when (formState.mode) {
                    AuthMode.SignIn -> {
                        signIn(email, password)
                        sendEffect(AuthEffect.NavigateBack)
                    }

                    AuthMode.SignUp -> {
                        when (signUp(email, password)) {
                            is SignUpResult.SignedIn -> sendEffect(AuthEffect.NavigateBack)
                            SignUpResult.ConfirmationPending ->
                                updateState { AuthUiState.CheckEmail(email = email) }
                        }
                    }
                }
            } finally {
                updateForm { copy(isSubmitting = false) }
            }
        }
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
