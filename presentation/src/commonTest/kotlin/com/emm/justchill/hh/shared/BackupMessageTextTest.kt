package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.justchill.hh.profile.ProfileMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BackupMessageTextTest {

    @Test
    fun `a verified snapshot is named, and every count it reports is inflected`() {
        val counts = BackupRowCounts(accounts = 1, categories = 23, transactions = 412, recurringMovements = 3)

        val shown = verifiedText(counts)

        assertEquals(
            "Verificado: backup-v3-2026-08-16T14-22-08Z.json — 1 cuenta, 23 categorías, 412 movimientos, 3 plantillas",
            shown,
        )
    }

    @Test
    fun `every count inflects on its own, not on whichever one happens to be singular`() {
        val counts = BackupRowCounts(accounts = 2, categories = 1, transactions = 1, recurringMovements = 0)

        val shown = verifiedText(counts)

        assertEquals(
            "Verificado: backup-v3-2026-08-16T14-22-08Z.json — 2 cuentas, 1 categoría, 1 movimiento, 0 plantillas",
            shown,
        )
    }

    @Test
    fun `a snapshot the walk back landed on is not reported as the newest one`() {
        val shown = verifiedText(NO_ROWS, isNewestPair = false)

        assertEquals(
            "Verificado un respaldo más antiguo: backup-v3-2026-08-16T14-22-08Z.json — " +
                "0 cuentas, 0 categorías, 0 movimientos, 0 plantillas",
            shown,
        )
    }

    @Test
    fun `nothing to inspect does not read as backups that failed to verify`() {
        assertEquals(
            "No encontré ningún respaldo completo para verificar.",
            ProfileMessage.BackupNotVerified(pairsInspected = 0).toText(),
        )
    }

    @Test
    fun `one inspected pair does not read as a plural sweep`() {
        assertEquals(
            "Revisé el único respaldo que hay y no se puede restaurar.",
            ProfileMessage.BackupNotVerified(pairsInspected = 1).toText(),
        )
    }

    @Test
    fun `several inspected pairs say how many were read`() {
        assertEquals(
            "Revisé los 5 respaldos más recientes y ninguno se puede restaurar.",
            ProfileMessage.BackupNotVerified(pairsInspected = 5).toText(),
        )
    }

    @Test
    fun `a verification that could not run reads as its own failure, not as a failed backup`() {
        assertEquals("No pude verificar tu respaldo — intenta de nuevo.", ProfileMessage.BackupVerifyFailed.toText())
        assertEquals("No pude respaldar en la nube — intenta de nuevo.", ProfileMessage.BackupFailed.toText())
    }
}

private val NO_ROWS = BackupRowCounts(accounts = 0, categories = 0, transactions = 0, recurringMovements = 0)

private fun verifiedText(counts: BackupRowCounts, isNewestPair: Boolean = true): String = ProfileMessage.BackupVerified(
    BackupVerification.Verified(
        fileName = "backup-v3-2026-08-16T14-22-08Z.json",
        takenAt = Instant.parse("2026-08-16T14:22:08Z"),
        schemaVersion = 3,
        rowCounts = counts,
        isNewestPair = isNewestPair,
    ),
).toText()
