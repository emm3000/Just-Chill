package com.emm.data.backup

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BackupManifestDtoTest {

    @Test
    fun `a manifest written before loans existed still decodes, with both new counts at zero`() {
        val manifest = assertNotNull(decodeBackupManifestOrNull(PRE_LOANS_MANIFEST.encodeToByteArray()))

        assertEquals(0, manifest.rowCounts.loans)
        assertEquals(0, manifest.rowCounts.loanPayments)
        assertEquals(1, manifest.rowCounts.accounts)
        assertEquals(4, manifest.rowCounts.recurringMovements)
    }

    private companion object {
        const val PRE_LOANS_MANIFEST = """
            {
              "manifestVersion": 1,
              "fileName": "backup-v3-2026-08-14T03-00-00Z.json",
              "payloadSha256": "abc123",
              "payloadSchemaVersion": 3,
              "rowCounts": { "accounts": 1, "categories": 2, "transactions": 3, "recurringMovements": 4 }
            }
        """
    }
}
