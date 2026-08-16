package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.hh.profile.BackupRowUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Spanish the "Último respaldo" row puts on each [BackupRowUi]. Copy is behaviour here. */
class BackupRowTextTest {

    @Test
    fun `a running cycle says so`() {
        assertEquals("Respaldando…", BackupRowUi.BackingUp.toMetaText())
    }

    @Test
    fun `a signed-out device is told what is missing, not that backup failed`() {
        assertEquals("Inicia sesión para respaldar en la nube", BackupRowUi.NeedsAccount.toMetaText())
    }

    @Test
    fun `a device that has never backed up does not claim to be up to date`() {
        assertEquals("Todavía no hay ningún respaldo", BackupRowUi.Never.toMetaText())
    }

    @Test
    fun `today and yesterday get words, everything else gets a count`() {
        assertEquals("Hoy", BackupRowUi.UpToDate(0).toMetaText())
        assertEquals("Ayer", BackupRowUi.UpToDate(1).toMetaText())
        assertEquals("Hace 2 días", BackupRowUi.UpToDate(2).toMetaText())
        assertEquals("Hace 41 días", BackupRowUi.UpToDate(41).toMetaText())
    }

    @Test
    fun `a stale row says why it is a warning and not just how old it is`() {
        assertEquals("Hace 4 días · hay cambios sin respaldar", BackupRowUi.Stale(4).toMetaText())
    }

    @Test
    fun `the two reasons the user can act on get their own sentence`() {
        assertEquals(
            "No pude respaldar — revisa tu conexión",
            BackupRowUi.Failed(BackupFailureReason.Network).toMetaText(),
        )
        assertEquals(
            "No pude respaldar — vuelve a iniciar sesión",
            BackupRowUi.Failed(BackupFailureReason.Unauthorized).toMetaText(),
        )
    }

    @Test
    fun `an unreadable local database is not blamed on the cloud`() {
        assertEquals(
            "No pude leer los datos de este teléfono",
            BackupRowUi.Failed(BackupFailureReason.LocalDatabase).toMetaText(),
        )
    }

    /**
     * The reason degrades to null for a persisted name this build cannot resolve
     * (`BackupFailureReason.fromNameOrNull`), so a genuinely failing device arrives here unlabelled.
     * It still has to be told its backups are failing.
     */
    @Test
    fun `a failure with no resolvable reason still reads as a failure`() {
        val text = BackupRowUi.Failed(null).toMetaText()

        assertEquals("No pude respaldar — intenta de nuevo", text)
        assertTrue(text.startsWith("No pude"), "A null reason must never render as a healthy row.")
    }

    @Test
    fun `every failure reason renders something, and never an empty string`() {
        // Exhaustive over the enum by construction: a member added later lands on the generic branch
        // rather than on nothing, and this fails the day one renders blank.
        for (reason in BackupFailureReason.entries) {
            val text = BackupRowUi.Failed(reason).toMetaText()
            assertTrue(text.isNotBlank(), "$reason rendered blank.")
            assertTrue(text.startsWith("No pude"), "$reason did not read as a failure: $text")
        }
    }
}
