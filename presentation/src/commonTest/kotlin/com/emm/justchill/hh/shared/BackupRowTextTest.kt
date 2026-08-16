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
    fun `a failure over a snapshot leads with the age, so a good backup is never hidden`() {
        // The manual-tap-on-bad-wifi case: data is safe, an update did not happen.
        assertEquals("Hoy · no pude actualizar", BackupRowUi.Failed(BackupFailureReason.Network, 0).toMetaText())
        assertEquals("Ayer · no pude actualizar", BackupRowUi.Failed(BackupFailureReason.Unknown, 1).toMetaText())
        assertEquals(
            "Hace 4 días · no pude actualizar",
            BackupRowUi.Failed(BackupFailureReason.Unauthorized, 4).toMetaText(),
        )
    }

    @Test
    fun `a failure with no snapshot at all spends the line on the reason instead`() {
        assertEquals(
            "Sin respaldo · revisa tu conexión",
            BackupRowUi.Failed(BackupFailureReason.Network, null).toMetaText(),
        )
        assertEquals(
            "Sin respaldo · vuelve a iniciar sesión",
            BackupRowUi.Failed(BackupFailureReason.Unauthorized, null).toMetaText(),
        )
    }

    @Test
    fun `an unreadable local database is not blamed on the cloud`() {
        assertEquals(
            "Sin respaldo · no pude leer los datos de este teléfono",
            BackupRowUi.Failed(BackupFailureReason.LocalDatabase, null).toMetaText(),
        )
    }

    @Test
    fun `a row that could not be read claims neither health nor failure`() {
        val text = BackupRowUi.Unreadable.toMetaText()

        assertEquals("No pude leer el estado del respaldo", text)
        // The distinction the variant exists for: it must not read as "there is no backup".
        assertTrue("Sin respaldo" !in text)
    }

    /**
     * The reason degrades to null for a persisted name this build cannot resolve
     * (`BackupFailureReason.fromNameOrNull`), so a genuinely failing device arrives here unlabelled.
     * It still has to be told its backups are failing.
     */
    @Test
    fun `a failure with no resolvable reason still reads as a failure`() {
        assertEquals("Sin respaldo · intenta de nuevo", BackupRowUi.Failed(null, null).toMetaText())
        assertEquals("Hoy · no pude actualizar", BackupRowUi.Failed(null, 0).toMetaText())
    }

    @Test
    fun `every failure reason renders something, and never an empty string`() {
        // Exhaustive over the enum by construction: a member added later lands on the generic branch
        // rather than on nothing, and this fails the day one renders blank.
        for (reason in BackupFailureReason.entries) {
            val withoutSnapshot = BackupRowUi.Failed(reason, null).toMetaText()
            val withSnapshot = BackupRowUi.Failed(reason, 3).toMetaText()

            assertTrue(withoutSnapshot.startsWith("Sin respaldo · "), "$reason: $withoutSnapshot")
            assertTrue(withoutSnapshot.removePrefix("Sin respaldo · ").isNotBlank(), "$reason rendered no cause.")
            assertEquals("Hace 3 días · no pude actualizar", withSnapshot, "$reason with a snapshot")
        }
    }
}
