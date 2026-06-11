package com.emm.justchill.hh.auth

import android.util.Log
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignUpResult
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.delay

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val resendConfirmationEmail: ResendConfirmationEmailUseCase,
    private val googleServerClientId: String,
    private val googleSignInLauncher: GoogleSignInLauncher,
) : MviViewModel<AuthUiState, AuthIntent, AuthEffect>() {

    override val initialState: AuthUiState = AuthUiState.Form()

    // Pure dispatch — each handler owns its own state guard.
    override fun onIntent(intent: AuthIntent) = when (intent) {
        is AuthIntent.EmailChanged -> updateForm { copy(email = intent.value) }
        is AuthIntent.PasswordChanged -> updateForm { copy(password = intent.value) }
        AuthIntent.ToggleMode -> updateForm { copy(mode = mode.toggled()) }
        AuthIntent.BackToSignIn -> updateCheckEmail { AuthUiState.Form(mode = AuthMode.SignIn) }
        AuthIntent.Submit -> submit()
        AuthIntent.GoogleSignInClicked -> submitWithGoogle()
        AuthIntent.ResendEmail -> resendEmail()
        AuthIntent.OpenEmailApp -> openEmailApp()
        AuthIntent.Back -> back()
    }

    private fun submit() = launchSubmitting(via = Submitting.Email) { form ->
        val email = form.email.trim()
        when (form.mode) {
            AuthMode.SignIn -> {
                signIn(email, form.password)
                sendEffect(AuthEffect.NavigateBack)
            }

            AuthMode.SignUp -> when (signUp(email, form.password)) {
                is SignUpResult.SignedIn -> sendEffect(AuthEffect.NavigateBack)

                SignUpResult.ConfirmationPending ->
                    updateState { AuthUiState.CheckEmail(email = email) }
            }
        }
    }

    private fun submitWithGoogle() = launchSubmitting(via = Submitting.Google) {
        if (googleServerClientId.isBlank()) {
            Log.w(TAG, "GOOGLE_WEB_CLIENT_ID not configured")
            sendEffect(AuthEffect.Notify(AuthMessage.GoogleSignInFailed))
            return@launchSubmitting
        }
        when (val result = googleSignInLauncher.signIn(googleServerClientId)) {
            is GoogleCredentialClient.Result.Success -> {
                signInWithGoogle(result.idToken, result.rawNonce)
                sendEffect(AuthEffect.NavigateBack)
            }

            // User closed the sheet — silent per design.
            GoogleCredentialClient.Result.Cancelled -> Unit

            GoogleCredentialClient.Result.NoCredentials ->
                sendEffect(AuthEffect.Notify(AuthMessage.GoogleAccountUnavailable))

            is GoogleCredentialClient.Result.Failure -> {
                Log.w(TAG, "Google credential flow failed", result.cause)
                sendEffect(AuthEffect.Notify(AuthMessage.GoogleSignInFailed))
            }
        }
    }

    private fun resendEmail() {
        val check = currentState as? AuthUiState.CheckEmail ?: return
        if (check.isResending || !check.canResend) return
        updateCheckEmail { copy(isResending = true) }
        launchSafe(onError = { e -> AuthEffect.ShowError(e) }) {
            try {
                resendConfirmationEmail(check.email)
            } finally {
                // isResending only covers the network call; the cooldown below has its own flag.
                updateCheckEmail { copy(isResending = false) }
            }
            sendEffect(AuthEffect.Notify(AuthMessage.ConfirmationLinkResent))
            coolDownResend()
        }
    }

    private suspend fun coolDownResend() {
        updateCheckEmail { copy(canResend = false) }
        delay(RESEND_COOLDOWN_MS)
        updateCheckEmail { copy(canResend = true) }
    }

    private fun openEmailApp() {
        if (currentState is AuthUiState.CheckEmail) sendEffect(AuthEffect.OpenEmailApp)
    }

    private fun back() = when (currentState) {
        is AuthUiState.CheckEmail -> updateState { AuthUiState.Form(mode = AuthMode.SignIn) }
        is AuthUiState.Form -> sendEffect(AuthEffect.NavigateBack)
    }

    /**
     * Shared lifecycle for every submit path (email or Google): guard against re-entry,
     * raise [AuthUiState.Form.submitting] to [via], run [block] with a snapshot of the
     * form, and always lower the flag again.
     *
     * The try/finally is what guarantees the reset on EVERY exit — success, domain error
     * (rethrown to [launchSafe]'s handler), and coroutine cancellation. Without it each
     * path would need its own reset, and a missed one leaves the screen disabled forever.
     */
    private fun launchSubmitting(via: Submitting, block: suspend (AuthUiState.Form) -> Unit) {
        val form = currentState as? AuthUiState.Form ?: return
        if (form.submitting != Submitting.None) return
        updateForm { copy(submitting = via) }
        launchSafe(onError = { e -> AuthEffect.ShowError(e) }) {
            try {
                block(form)
            } finally {
                updateForm { copy(submitting = Submitting.None) }
            }
        }
    }

    // Routes Form mutations; no-ops when the state is CheckEmail.
    private inline fun updateForm(crossinline reducer: AuthUiState.Form.() -> AuthUiState) =
        updateState { if (this is AuthUiState.Form) reducer() else this }

    // Routes CheckEmail mutations; no-ops when the state is Form.
    private inline fun updateCheckEmail(crossinline reducer: AuthUiState.CheckEmail.() -> AuthUiState) =
        updateState { if (this is AuthUiState.CheckEmail) reducer() else this }

    private companion object {
        const val TAG = "AuthViewModel"
        const val RESEND_COOLDOWN_MS = 30_000L
    }
}

private fun AuthMode.toggled(): AuthMode = if (this == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
