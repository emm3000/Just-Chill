package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.hh.profile.BackupRowSeverity
import com.emm.justchill.hh.profile.BackupRowUi
import com.emm.justchill.hh.profile.LastSnapshot
import com.emm.justchill.hh.profile.severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals("Hoy · no pude actualizar", failed(BackupFailureReason.Unknown, days = 0).toMetaText())
        assertEquals("Ayer · no pude actualizar", failed(BackupFailureReason.Serialization, days = 1).toMetaText())
        assertEquals("Hace 4 días · no pude actualizar", failed(BackupFailureReason.Unverified, days = 4).toMetaText())
    }

    @Test
    fun `a reason with a real action says it however old the snapshot is`() {
        assertEquals(
            "Hace 30 días · vuelve a iniciar sesión",
            failed(BackupFailureReason.Unauthorized, days = 30, isStale = true).toMetaText(),
        )
        assertEquals(
            "Hoy · vuelve a iniciar sesión",
            failed(BackupFailureReason.Unauthorized, days = 0).toMetaText(),
        )
        assertEquals("Hace 2 días · revisa tu conexión", failed(BackupFailureReason.Network, days = 2).toMetaText())
    }

    @Test
    fun `a failure with no snapshot at all spends the line on the reason instead`() {
        assertEquals(
            "Sin respaldo · revisa tu conexión",
            BackupRowUi.Failed(BackupFailureReason.Network, LastSnapshot.None).toMetaText(),
        )
        assertEquals(
            "Sin respaldo · vuelve a iniciar sesión",
            BackupRowUi.Failed(BackupFailureReason.Unauthorized, LastSnapshot.None).toMetaText(),
        )
    }

    @Test
    fun `a local database read failure gets the generic retry, not a false promise`() {
        // The no-snapshot case (LocalDatabase renders the generic fallback) is pinned by
        // `group membership is pinned per reason...` below; this keeps only what that test doesn't cover.
        assertEquals(
            "Ayer · no pude actualizar",
            failed(BackupFailureReason.LocalDatabase, days = 1).toMetaText(),
        )
    }

    @Test
    fun `a failure over an undated snapshot does not claim there is none`() {
        val text = BackupRowUi.Failed(BackupFailureReason.Unauthorized, LastSnapshot.AgeUnknown).toMetaText()

        assertEquals("No pude respaldar · vuelve a iniciar sesión", text)
        assertTrue("Sin respaldo" !in text)
    }

    @Test
    fun `a row that could not be read claims neither health nor failure`() {
        val text = BackupRowUi.Unreadable.toMetaText()

        assertEquals("No pude leer el estado del respaldo", text)
        assertTrue("Sin respaldo" !in text)
    }

    @Test
    fun `a failure with no resolvable reason still reads as a failure`() {
        assertEquals("Sin respaldo · intenta de nuevo", BackupRowUi.Failed(null, LastSnapshot.None).toMetaText())
        assertEquals("Hoy · no pude actualizar", failed(null, days = 0).toMetaText())
        assertEquals(
            "No pude respaldar · intenta de nuevo",
            BackupRowUi.Failed(null, LastSnapshot.AgeUnknown).toMetaText(),
        )
    }

    /**
     * `toFailureAction` is `private` to `BackupRowText.kt`, so group membership is observed through
     * `toMetaText()` instead: a reason with no remedy renders the exact same generic fallback as
     * `null` does, and a reason with a remedy never does. [EXPECTED_REMEDY_GROUP] names every
     * [BackupFailureReason] explicitly and the first assertion below requires it to cover
     * [BackupFailureReason.entries] exactly, so a reason added to the enum without a matching entry
     * here fails on that coverage check instead of silently inheriting the no-remedy group.
     */
    @Test
    fun `group membership is pinned per reason, and a new reason cannot join a group silently`() {
        assertEquals(
            BackupFailureReason.entries.toSet(),
            EXPECTED_REMEDY_GROUP.keys,
            "EXPECTED_REMEDY_GROUP must name every BackupFailureReason exactly — missing or stale " +
                "entries let group membership drift unnoticed.",
        )

        for ((reason, group) in EXPECTED_REMEDY_GROUP) {
            val text = BackupRowUi.Failed(reason, LastSnapshot.None).toMetaText()
            val rendersTheGenericFallback = text == "Sin respaldo · intenta de nuevo"

            assertEquals(group == RemedyGroup.NoRemedy, rendersTheGenericFallback, "$reason: $text")
        }
    }

    @Test
    fun `every failure reason renders something, and never an empty string`() {
        for (reason in BackupFailureReason.entries) {
            val withoutSnapshot = BackupRowUi.Failed(reason, LastSnapshot.None).toMetaText()
            val withSnapshot = failed(reason, days = 3).toMetaText()

            assertTrue(withoutSnapshot.startsWith("Sin respaldo · "), "$reason: $withoutSnapshot")
            assertTrue(withoutSnapshot.removePrefix("Sin respaldo · ").isNotBlank(), "$reason rendered no cause.")
            assertTrue(withSnapshot.startsWith("Hace 3 días · "), "$reason: $withSnapshot")
            assertTrue(withSnapshot.removePrefix("Hace 3 días · ").isNotBlank(), "$reason rendered no tail.")
        }
    }

    @Test
    fun `a healthy or merely pending row is drawn normally`() {
        assertEquals(BackupRowSeverity.Normal, BackupRowUi.NeedsAccount.severity())
        assertEquals(BackupRowSeverity.Normal, BackupRowUi.BackingUp.severity())
        assertEquals(BackupRowSeverity.Normal, BackupRowUi.UpToDate(0).severity())
        assertEquals(BackupRowSeverity.Normal, BackupRowUi.Never.severity())
    }

    @Test
    fun `a stale but not failing device stays amber, because it will self-heal`() {
        assertEquals(BackupRowSeverity.Warning, BackupRowUi.Stale(9).severity())
    }

    @Test
    fun `nothing backed up at all is the loud case`() {
        assertEquals(
            BackupRowSeverity.Danger,
            BackupRowUi.Failed(BackupFailureReason.Network, LastSnapshot.None).severity(),
        )
    }

    @Test
    fun `a failure over a stale snapshot escalates, a failure over a fresh one does not`() {
        assertEquals(
            BackupRowSeverity.Danger,
            failed(BackupFailureReason.Unauthorized, days = 30, isStale = true).severity(),
        )
        assertEquals(
            BackupRowSeverity.Warning,
            failed(BackupFailureReason.Unauthorized, days = 30, isStale = false).severity(),
            "A month-old snapshot of a ledger nobody has touched is complete — the age alone is not the rule.",
        )
        assertEquals(BackupRowSeverity.Warning, failed(BackupFailureReason.Network, days = 0).severity())
    }

    @Test
    fun `not knowing the age is not evidence the snapshot is old`() {
        assertEquals(BackupRowSeverity.Warning, BackupRowUi.Unreadable.severity())
        assertEquals(
            BackupRowSeverity.Warning,
            BackupRowUi.Failed(BackupFailureReason.Network, LastSnapshot.AgeUnknown).severity(),
        )
    }

    @Test
    fun `a pending disclosure asks for the tap rather than reporting a state`() {
        val text = BackupRowUi.DisclosurePending.toMetaText()

        assertEquals("Falta tu confirmación para respaldar en esta cuenta", text)
        assertTrue("Sin respaldo" !in text)
    }

    /**
     * The sentence ADR 009 Decision 5 requires on screen: this device's WHOLE ledger, including rows
     * written under a previous account, goes into THIS account's backup, and signing out does not
     * erase it. A copy edit that drops any one fact stops satisfying the decision, so every fact is
     * asserted rather than the string.
     */
    @Test
    fun `the disclosure names the whole ledger and the previous account`() {
        assertTrue("TODO" in BACKUP_DESTINATION_DISCLOSURE, BACKUP_DESTINATION_DISCLOSURE)
        assertTrue("a esta cuenta" in BACKUP_DESTINATION_DISCLOSURE, BACKUP_DESTINATION_DISCLOSURE)
        assertTrue("otra cuenta" in BACKUP_DESTINATION_DISCLOSURE, BACKUP_DESTINATION_DISCLOSURE)
        assertTrue("cerrar sesión no borra" in BACKUP_DESTINATION_DISCLOSURE, BACKUP_DESTINATION_DISCLOSURE)
        assertTrue(BACKUP_DESTINATION_DISCLOSURE_ACTION.isNotBlank())
    }

    @Test
    fun `a pending disclosure is amber, because the row itself carries the fix`() {
        assertEquals(BackupRowSeverity.Warning, BackupRowUi.DisclosurePending.severity())
    }

    private fun failed(reason: BackupFailureReason?, days: Int, isStale: Boolean = false) =
        BackupRowUi.Failed(reason, LastSnapshot.DaysAgo(days = days, isStale = isStale))

    private enum class RemedyGroup { HasRemedy, NoRemedy }

    private companion object {
        val EXPECTED_REMEDY_GROUP: Map<BackupFailureReason, RemedyGroup> = mapOf(
            BackupFailureReason.Network to RemedyGroup.HasRemedy,
            BackupFailureReason.Unauthorized to RemedyGroup.HasRemedy,
            BackupFailureReason.Serialization to RemedyGroup.NoRemedy,
            BackupFailureReason.RemoteRejected to RemedyGroup.NoRemedy,
            BackupFailureReason.Busy to RemedyGroup.NoRemedy,
            BackupFailureReason.Unverified to RemedyGroup.NoRemedy,
            BackupFailureReason.LocalDatabase to RemedyGroup.NoRemedy,
            BackupFailureReason.Unknown to RemedyGroup.NoRemedy,
        )
    }
}
