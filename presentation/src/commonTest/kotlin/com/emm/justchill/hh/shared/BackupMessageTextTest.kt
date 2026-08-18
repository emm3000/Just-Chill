package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.justchill.hh.profile.ProfileMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupMessageTextTest {

    @Test
    fun `a verified snapshot is named, and every count it reports is inflected`() {
        val counts = BackupRowCounts(accounts = 1, categories = 23, transactions = 412, recurringMovements = 3)

        val shown = verifiedText(counts)

        assertEquals(
            "Verificado: backup-v3-2026-08-16T14-22-08Z.json — 1 cuenta, 23 categorías, 412 movimientos, 3 recurrentes",
            shown,
        )
    }

    @Test
    fun `every count inflects on its own, not on whichever one happens to be singular`() {
        val counts = BackupRowCounts(accounts = 2, categories = 1, transactions = 1, recurringMovements = 0)

        val shown = verifiedText(counts)

        assertEquals(
            "Verificado: backup-v3-2026-08-16T14-22-08Z.json — 2 cuentas, 1 categoría, 1 movimiento, 0 recurrentes",
            shown,
        )
    }

    @Test
    fun `a snapshot the walk back landed on is not reported as the newest one`() {
        val shown = verifiedText(NO_ROWS, isNewestPair = false)

        assertEquals(
            "Verificado un respaldo más antiguo: backup-v3-2026-08-16T14-22-08Z.json — " +
                "0 cuentas, 0 categorías, 0 movimientos, 0 recurrentes",
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
        assertEquals(
            "El respaldo falló por algo inesperado — no es algo que hayas hecho mal.",
            failedText(BackupFailureReason.Unknown),
        )
    }

    @Test
    fun `each failure reason says what happened in its own words`() {
        assertEquals(
            "No pude armar el archivo del respaldo — es una falla de la app, no tuya.",
            failedText(BackupFailureReason.Serialization),
        )
        assertEquals(
            "No llegué a la nube — revisa tu conexión e intenta de nuevo.",
            failedText(BackupFailureReason.Network),
        )
        assertEquals(
            "El servidor rechazó tu respaldo — no depende de ti, lo reintento más tarde.",
            failedText(BackupFailureReason.RemoteRejected),
        )
        assertEquals(
            "Tu sesión ya no vale para respaldar — vuelve a iniciar sesión.",
            failedText(BackupFailureReason.Unauthorized),
        )
        assertEquals(
            "Hay otra operación en curso — espera, el respaldo se reintenta solo.",
            failedText(BackupFailureReason.Busy),
        )
        assertEquals(
            "El respaldo no coincidió al verificarlo y lo descarté — lo reintento solo.",
            failedText(BackupFailureReason.Unverified),
        )
        assertEquals(
            "No pude leer tus datos de este teléfono para armar el respaldo.",
            failedText(BackupFailureReason.LocalDatabase),
        )
    }

    @Test
    fun `no two failure reasons share a sentence`() {
        val byReason: Map<BackupFailureReason, String> = BackupFailureReason.entries.associateWith(::failedText)

        assertEquals(
            byReason.size,
            byReason.values.toSet().size,
            "two reasons render the same sentence, so the user cannot tell them apart: $byReason",
        )
    }

    @Test
    fun `a failure never shows the user the reason's internal name`() {
        BackupFailureReason.entries.forEach { reason ->
            val shown: String = failedText(reason)
            assertTrue(shown.isNotBlank(), "$reason renders nothing")
            assertFalse(shown.contains(reason.name), "$reason leaks its enum name to the user: $shown")
        }
    }
}

private fun failedText(reason: BackupFailureReason): String = ProfileMessage.BackupFailed(reason).toText()

private val NO_ROWS = BackupRowCounts(accounts = 0, categories = 0, transactions = 0, recurringMovements = 0)

private fun verifiedText(counts: BackupRowCounts, isNewestPair: Boolean = true): String = ProfileMessage.BackupVerified(
    BackupVerification.Verified(
        fileName = "backup-v3-2026-08-16T14-22-08Z.json",
        rowCounts = counts,
        isNewestPair = isNewestPair,
    ),
).toText()
