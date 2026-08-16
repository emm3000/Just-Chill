package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.hh.profile.BackupRowUi

// Shared Spanish copy for the "Último respaldo" row in Perfil, in the same shape and the same place
// as ProfileMessageText.kt: the literals are UI copy, the decision that produced them is not. Both
// UIs read this — ui-android/.../hh/profile/ProfileScreen.kt today, SwiftUI when it gets the row —
// so a second table would be a second Spanish translation of one state machine.
//
// Deliberately holds no colour and no icon. Those are per-platform tokens (docs/DESIGN_SYSTEM.md on
// Android), and this module may not know what a Color is.

/** The row's status line. One string per [BackupRowUi] variant, no `null` branch anywhere. */
fun BackupRowUi.toMetaText(): String = when (this) {
    BackupRowUi.BackingUp -> "Respaldando…"

    BackupRowUi.NeedsAccount -> "Inicia sesión para respaldar en la nube"

    BackupRowUi.Never -> "Todavía no hay ningún respaldo"

    BackupRowUi.Unreadable -> "No pude leer el estado del respaldo"

    // Both facts, and the age decides which one leads. With a snapshot to point at, the failure is
    // an update that did not happen and the data is safe — so the age leads and the reason is
    // dropped, because the row has one line and "why" matters least when nothing is at risk. With
    // no snapshot at all there is nothing else to say, so the reason gets the space instead.
    is BackupRowUi.Failed -> if (lastBackupDaysAgo == null) {
        "Sin respaldo · ${reason.toFailureCause()}"
    } else {
        "${backupAgeLabel(lastBackupDaysAgo).titlecaseFirstChar()} · no pude actualizar"
    }

    // The age alone would read as a neutral fact, so the second half says what makes it a warning:
    // there is data on this phone that no snapshot holds. That IS the stale rule's other half.
    is BackupRowUi.Stale ->
        "${backupAgeLabel(daysSinceLastBackup).titlecaseFirstChar()} · hay cambios sin respaldar"

    is BackupRowUi.UpToDate -> backupAgeLabel(daysSinceLastBackup).titlecaseFirstChar()
}

/**
 * "hoy" / "ayer" / "hace 5 días" — lowercase, so a caller can put it mid-sentence or titlecase it.
 *
 * Takes a day count rather than an instant on purpose: resolving "which calendar day was that" needs
 * an injected `Clock` **and** `TimeZone` (`docs/DATE_AUDIT.md` rule 7), and neither belongs to a
 * formatter. `GetBackupStalenessUseCase` does that resolution once, with both injected, and this
 * turns its answer into Spanish. Same shape as `relativeDayLabel` in DayLabels.kt, which takes
 * `today` for the same reason.
 */
private fun backupAgeLabel(days: Int): String = when (days) {
    0 -> "hoy"
    1 -> "ayer"
    else -> "hace $days días"
}

/**
 * The half-sentence after "Sin respaldo · " — why the cycle that would have made one failed.
 *
 * The `null` branch is not defensive padding: `BackupFailureReason.fromNameOrNull` answers null for a
 * reason name a device upgraded past, so a real failing device can arrive here with no label. It
 * still has to say something, because the warning was already decided by the streak, not by this.
 *
 * Only the reasons that change what the user would do get their own words. The rest collapse:
 * telling somebody their snapshot failed to serialize, or that its read-back digest did not match,
 * names a defect they cannot do anything about and that belongs in the log.
 *
 * Read only where there is no snapshot to point at. With one, the row spends its single line on the
 * age instead — see [toMetaText].
 */
private fun BackupFailureReason?.toFailureCause(): String = when (this) {
    BackupFailureReason.Network -> "revisa tu conexión"

    BackupFailureReason.Unauthorized -> "vuelve a iniciar sesión"

    // Its own words because it is the one reason that is NOT about the cloud: `safeDbCall` raises it
    // from the export's read of this phone's own database, and "intenta de nuevo" would send the
    // user to check a connection that is fine.
    BackupFailureReason.LocalDatabase -> "no pude leer los datos de este teléfono"

    BackupFailureReason.Serialization,
    BackupFailureReason.Unverified,
    BackupFailureReason.Unknown,
    null,
    -> "intenta de nuevo"
}
