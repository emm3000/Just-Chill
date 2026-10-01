package com.emm.justchill.feature.profile

import org.junit.Test
import kotlin.test.assertEquals

class PrivacyRowTextTest {

    @Test
    fun `a signed-out session keeps everything on the phone without an account`() {
        assertEquals("100 % local, sin cuenta", ProfileUiState(session = SessionUiState.SignedOut).privacyMeta)
    }

    @Test
    fun `cloud backup without a session still reads local without an account`() {
        assertEquals(
            "100 % local, sin cuenta",
            ProfileUiState(session = SessionUiState.SignedOut, isCloudBackupAvailable = true).privacyMeta,
        )
    }

    @Test
    fun `a signed-in session without cloud backup stays local`() {
        assertEquals("100 % local", signedInProfile.privacyMeta)
    }

    @Test
    fun `a signed-in session with cloud backup says the account holds the copy`() {
        assertEquals(
            "En tu celular, con respaldo en tu cuenta",
            signedInProfile.copy(isCloudBackupAvailable = true).privacyMeta,
        )
    }
}
