package com.emm.justchill.feature.profile

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupDisclosureRowTest {

    @Test
    fun `the destination disclosure shows only while it is pending`() {
        assertTrue(signedInProfile.copy(backupRow = BackupRowUi.DisclosurePending).showsBackupDestinationDisclosure)
        assertFalse(signedInProfile.copy(backupRow = BackupRowUi.BackingUp).showsBackupDestinationDisclosure)
        assertFalse(signedInProfile.copy(backupRow = BackupRowUi.UpToDate(0)).showsBackupDestinationDisclosure)
        assertFalse(ProfileUiState(backupRow = BackupRowUi.NeedsAccount).showsBackupDestinationDisclosure)
    }
}
