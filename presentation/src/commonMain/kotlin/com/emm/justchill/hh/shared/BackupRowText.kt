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

    is BackupRowUi.Failed -> reason.toFailureText()

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
 * Why the last cycle failed, in words — with a fallback for `null`.
 *
 * The `null` branch is not defensive padding: `BackupFailureReason.fromNameOrNull` answers null for a
 * reason name a device upgraded past, so a real failing device can arrive here with no label. It has
 * to say "it failed" anyway, because the warning was already decided by the streak, not by this.
 *
 * Only the reasons that change what the user would do get their own sentence. The rest collapse:
 * telling somebody their snapshot failed to serialize, or that its read-back digest did not match,
 * names a defect they cannot do anything about and that belongs in the log.
 */
private fun BackupFailureReason?.toFailureText(): String = when (this) {
    BackupFailureReason.Network -> "No pude respaldar — revisa tu conexión"

    BackupFailureReason.Unauthorized -> "No pude respaldar — vuelve a iniciar sesión"

    // Its own sentence because it is the one reason that is NOT about the cloud: `safeDbCall` raises
    // it from the export's read and from the staleness read, both on this phone's own database, and
    // "intenta de nuevo" would send the user to check a connection that is fine.
    BackupFailureReason.LocalDatabase -> "No pude leer los datos de este teléfono"

    BackupFailureReason.Serialization,
    BackupFailureReason.Unverified,
    BackupFailureReason.Unknown,
    null,
    -> "No pude respaldar — intenta de nuevo"
}
