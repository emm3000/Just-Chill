package com.emm.justchill.feature.auth

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthUiTextTest {

    @Test
    fun `the sign-in form asks to sign in and offers the sign-up`() {
        val signIn: AuthUiState.Form = AuthUiState.Form(mode = AuthMode.SignIn)

        assertEquals("Inicia sesión", signIn.heading)
        assertEquals("Iniciar sesión", signIn.submitLabel)
        assertEquals("¿No tienes cuenta? Créala", signIn.toggleLabel)
    }

    @Test
    fun `the sign-up form asks to create the account and offers the sign-in`() {
        val signUp: AuthUiState.Form = AuthUiState.Form(mode = AuthMode.SignUp)

        assertEquals("Crea tu cuenta", signUp.heading)
        assertEquals("Crear cuenta", signUp.submitLabel)
        assertEquals("¿Ya tienes cuenta? Inicia sesión", signUp.toggleLabel)
    }

    @Test
    fun `an email submit names the call in flight`() {
        assertEquals(
            "Entrando…",
            AuthUiState.Form(mode = AuthMode.SignIn, submitting = Submitting.Email).submitLabel,
        )
        assertEquals(
            "Creando…",
            AuthUiState.Form(mode = AuthMode.SignUp, submitting = Submitting.Email).submitLabel,
        )
    }

    @Test
    fun `the form takes a submit only while nothing is submitting`() {
        val idle: AuthUiState.Form = AuthUiState.Form(submitting = Submitting.None)
        val byEmail: AuthUiState.Form = AuthUiState.Form(submitting = Submitting.Email)
        val byGoogle: AuthUiState.Form = AuthUiState.Form(submitting = Submitting.Google)

        assertTrue(idle.isIdle)
        assertFalse(byEmail.isIdle)
        assertFalse(byGoogle.isIdle)
        assertFalse(idle.isSubmittingEmail)
        assertTrue(byEmail.isSubmittingEmail)
        assertFalse(byGoogle.isSubmittingEmail)
    }

    @Test
    fun `the resend link rests during the call and the cooldown`() {
        val ready: AuthUiState.CheckEmail = AuthUiState.CheckEmail(email = "qa@example.com")
        val resending: AuthUiState.CheckEmail = ready.copy(isResending = true)
        val coolingDown: AuthUiState.CheckEmail = ready.copy(canResend = false)

        assertTrue(ready.isResendEnabled)
        assertFalse(resending.isResendEnabled)
        assertFalse(coolingDown.isResendEnabled)
        assertEquals("Reenviar enlace", ready.resendLabel)
        assertEquals("Reenviando…", resending.resendLabel)
        assertEquals("Reenviar enlace", coolingDown.resendLabel)
    }

    @Test
    fun `each auth message reads as its notice`() {
        assertEquals("No encontramos una cuenta de Google en este teléfono.", AuthMessage.GoogleAccountUnavailable.text)
        assertEquals("No se pudo iniciar sesión con Google.", AuthMessage.GoogleSignInFailed.text)
        assertEquals("Listo, te reenviamos el enlace.", AuthMessage.ConfirmationLinkResent.text)
    }
}
