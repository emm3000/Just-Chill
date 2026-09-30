package com.emm.justchill.feature.profile

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountRowTextTest {

    @Test
    fun `the sign-in row shows only with the backup on and no session`() {
        assertTrue(ProfileUiState(isCloudBackupAvailable = true, session = SessionUiState.SignedOut).showsSignInRow)
        assertTrue(ProfileUiState(isCloudBackupAvailable = true, session = SessionUiState.Initializing).showsSignInRow)
        assertFalse(signedInProfile.copy(isCloudBackupAvailable = true).showsSignInRow)
        assertFalse(ProfileUiState(isCloudBackupAvailable = false, session = SessionUiState.SignedOut).showsSignInRow)
    }

    @Test
    fun `only a signed-in session opens the account section`() {
        assertTrue(signedInProfile.isSignedIn)
        assertFalse(ProfileUiState(session = SessionUiState.SignedOut).isSignedIn)
        assertFalse(ProfileUiState(session = SessionUiState.Initializing).isSignedIn)
    }

    @Test
    fun `the account row names the email and falls back without one`() {
        assertEquals("qa@example.com", signedInProfile.accountLabel)
        assertEquals("Tu cuenta", ProfileUiState(session = SessionUiState.SignedIn(email = null)).accountLabel)
    }

    @Test
    fun `the sign-out row promises the data stays until it runs`() {
        assertEquals("Tus datos siguen en este teléfono", ProfileUiState().signOutMeta)
        assertEquals("Cerrando sesión…", ProfileUiState(op = ProfileOp.SigningOut).signOutMeta)
    }

    @Test
    fun `the delete-account row names the cloud data until it runs`() {
        assertEquals("Borra tu cuenta y tus datos en la nube", ProfileUiState().deleteAccountMeta)
        assertEquals("Eliminando…", ProfileUiState(op = ProfileOp.DeletingAccount).deleteAccountMeta)
    }
}
