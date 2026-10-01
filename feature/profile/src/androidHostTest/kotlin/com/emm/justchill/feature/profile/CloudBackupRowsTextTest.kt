package com.emm.justchill.feature.profile

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudBackupRowsTextTest {

    @Test
    fun `backing up runs only signed in, idle and with the destination acknowledged`() {
        assertTrue(signedInProfile.copy(backupRow = BackupRowUi.UpToDate(0)).canBackUpNow)
        assertFalse(ProfileUiState(session = SessionUiState.SignedOut).canBackUpNow)
        assertFalse(ProfileUiState(session = SessionUiState.Initializing).canBackUpNow)
        assertFalse(signedInProfile.copy(backupRow = BackupRowUi.DisclosurePending).canBackUpNow)
        ProfileOp.entries.filter { it != ProfileOp.None }.forEach { runningOp ->
            assertFalse(signedInProfile.copy(op = runningOp).canBackUpNow, "$runningOp")
        }
    }

    @Test
    fun `verifying does not wait for the destination disclosure`() {
        assertTrue(signedInProfile.copy(backupRow = BackupRowUi.DisclosurePending).cloudBackupActionsEnabled)
    }

    @Test
    fun `the back-up row says it is running only while a backup is in flight`() {
        assertEquals("Sube una copia a la nube", signedInProfile.backUpNowMeta)
        assertFalse(signedInProfile.isBackingUp)
        assertEquals("Respaldando…", signedInProfile.copy(op = ProfileOp.BackingUp).backUpNowMeta)
        assertTrue(signedInProfile.copy(op = ProfileOp.BackingUp).isBackingUp)
        assertEquals("Sube una copia a la nube", signedInProfile.copy(op = ProfileOp.VerifyingBackup).backUpNowMeta)
        assertFalse(signedInProfile.copy(op = ProfileOp.VerifyingBackup).isBackingUp)
    }

    @Test
    fun `the verify row says it is running only while a verification is in flight`() {
        assertEquals("Revisa que el último se pueda restaurar", signedInProfile.verifyBackupMeta)
        assertFalse(signedInProfile.isVerifyingBackup)
        assertEquals(
            "Revisa que el último se pueda restaurar",
            signedInProfile.copy(op = ProfileOp.BackingUp).verifyBackupMeta,
        )
        assertFalse(signedInProfile.copy(op = ProfileOp.BackingUp).isVerifyingBackup)
        assertEquals("Verificando…", signedInProfile.copy(op = ProfileOp.VerifyingBackup).verifyBackupMeta)
        assertTrue(signedInProfile.copy(op = ProfileOp.VerifyingBackup).isVerifyingBackup)
    }
}
