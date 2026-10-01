package com.emm.justchill.feature.auth

import com.emm.justchill.core.domain.auth.ResendConfirmationEmailUseCase
import com.emm.justchill.core.domain.auth.SignInUseCase
import com.emm.justchill.core.domain.auth.SignInWithGoogleUseCase
import com.emm.justchill.core.domain.auth.SignUpResult
import com.emm.justchill.core.domain.auth.SignUpUseCase
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.presentation.error.toUserMessage
import com.emm.justchill.core.presentation.mvi.MviViewModel
import kotlinx.coroutines.delay

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val resendConfirmationEmail: ResendConfirmationEmailUseCase,
    private val googleServerClientId: String,
    private val googleSignInLauncher: GoogleSignInLauncher,
) : MviViewModel<AuthUiState, AuthIntent, AuthEffect>(AuthUiState.Form()) {

    override fun onIntent(intent: AuthIntent) = when (intent) {
        is AuthIntent.EmailChanged -> updateForm { copy(email = intent.value, emailError = null) }
        is AuthIntent.PasswordChanged -> updateForm { copy(password = intent.value, passwordError = null) }
        AuthIntent.ToggleMode -> updateForm { copy(mode = mode.toggled(), emailError = null, passwordError = null) }
        AuthIntent.BackToSignIn -> updateCheckEmail { AuthUiState.Form(mode = AuthMode.SignIn) }
        AuthIntent.Submit -> submit()
        AuthIntent.GoogleSignInClicked -> submitWithGoogle()
        AuthIntent.ResendEmail -> resendEmail()
        AuthIntent.OpenEmailApp -> openEmailApp()
        AuthIntent.Back -> back()
    }

    private fun submit() = launchSubmitting(
        start = { copy(submitting = Submitting.Email, emailError = null, passwordError = null) },
    ) { form ->
        val email: String = form.email.trim()
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

    private fun submitWithGoogle() = launchSubmitting(start = { copy(submitting = Submitting.Google) }) {
        if (googleServerClientId.isBlank()) {
            sendEffect(AuthEffect.Notify(AuthMessage.GoogleSignInFailed))
            return@launchSubmitting
        }
        when (val result = googleSignInLauncher.signIn(googleServerClientId)) {
            is GoogleSignInResult.Success -> {
                signInWithGoogle(result.idToken, result.rawNonce)
                sendEffect(AuthEffect.NavigateBack)
            }

            GoogleSignInResult.Cancelled -> Unit

            GoogleSignInResult.NoCredentials ->
                sendEffect(AuthEffect.Notify(AuthMessage.GoogleAccountUnavailable))

            is GoogleSignInResult.Failure ->
                sendEffect(AuthEffect.Notify(AuthMessage.GoogleSignInFailed))
        }
    }

    private fun resendEmail() {
        val check: AuthUiState.CheckEmail = currentState as? AuthUiState.CheckEmail ?: return
        if (check.isResending || !check.canResend) return
        updateCheckEmail { copy(isResending = true) }
        launchSafe(onError = { e -> AuthEffect.ShowError(e) }) {
            try {
                resendConfirmationEmail(check.email)
            } finally {
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

    private fun launchSubmitting(
        start: AuthUiState.Form.() -> AuthUiState.Form,
        block: suspend (AuthUiState.Form) -> Unit,
    ) {
        val form: AuthUiState.Form = currentState as? AuthUiState.Form ?: return
        if (form.submitting != Submitting.None) return
        updateForm { start() }
        launchSafe(onError = ::refuse) {
            try {
                block(form)
            } finally {
                updateForm { copy(submitting = Submitting.None) }
            }
        }
    }

    private fun refuse(error: DomainException): AuthEffect? {
        val code: ValidationCode = (error as? DomainException.ValidationError)?.code ?: ValidationCode.Unspecified
        val message: String = error.toUserMessage()
        return when (code) {
            in EMAIL_REFUSALS -> {
                updateForm { copy(emailError = message) }
                null
            }

            in PASSWORD_REFUSALS -> {
                updateForm { copy(passwordError = message) }
                null
            }

            else -> AuthEffect.ShowError(error)
        }
    }

    private inline fun updateForm(crossinline reducer: AuthUiState.Form.() -> AuthUiState) =
        updateState { if (this is AuthUiState.Form) reducer() else this }

    private inline fun updateCheckEmail(crossinline reducer: AuthUiState.CheckEmail.() -> AuthUiState) =
        updateState { if (this is AuthUiState.CheckEmail) reducer() else this }

    private companion object {
        const val RESEND_COOLDOWN_MS = 30_000L
        val EMAIL_REFUSALS: Set<ValidationCode> = setOf(
            ValidationCode.EmailInvalid,
            ValidationCode.EmailAlreadyRegistered,
        )
        val PASSWORD_REFUSALS: Set<ValidationCode> = setOf(
            ValidationCode.PasswordRequired,
            ValidationCode.PasswordTooShort,
            ValidationCode.PasswordTooWeak,
        )
    }
}

private fun AuthMode.toggled(): AuthMode = if (this == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
