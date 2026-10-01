package com.emm.justchill.shell

import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.feature.profile.ProfileMessage
import com.emm.justchill.feature.profile.toText
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportResultTest {

    private val deleted: MutableList<String> = mutableListOf()
    private val written: MutableList<String> = mutableListOf()
    private val notified: MutableList<Pair<String, EmmSnackbarTone>> = mutableListOf()

    private fun settle(document: String?, pending: PendingExport?) {
        settleExportResult(
            document = document,
            pending = pending,
            write = { target, _ -> written.add(target) },
            delete = { target -> deleted += target },
            notify = { message, tone -> notified += message to tone },
        )
    }

    @Test
    fun `a document that arrives with no pending payload is deleted and reported as an Error`() {
        settle(document = "orphan-document", pending = null)

        assertEquals(listOf("orphan-document"), deleted)
        assertEquals(listOf(ProfileMessage.ExportFailed.toText() to EmmSnackbarTone.Error), notified)
        assertTrue(written.isEmpty())
    }

    @Test
    fun `a cancelled picker with no pending payload stays silent`() {
        settle(document = null, pending = null)

        assertTrue(deleted.isEmpty())
        assertTrue(notified.isEmpty())
    }
}
