package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.hh.profile.BackupRowUi
import com.emm.justchill.hh.profile.LastSnapshot

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

    // Both facts, always. The head says what this device has; the tail says what to do about the
    // failure. An earlier version rendered "no pude actualizar" for every reason once a snapshot
    // existed, which silenced the one reason whose action is not obvious: a dead refresh token needs
    // the user to sign in again, and nothing on screen ever said so while the row aged in amber.
    is BackupRowUi.Failed -> {
        val action: String? = reason.toFailureAction()
        when (val snapshot = lastSnapshot) {
            LastSnapshot.None -> "Sin respaldo · ${action ?: "intenta de nuevo"}"

            // Never "Sin respaldo": there IS one, this device just cannot date it right now.
            LastSnapshot.AgeUnknown -> "No pude respaldar · ${action ?: "intenta de nuevo"}"

            is LastSnapshot.DaysAgo ->
                "${backupAgeLabel(snapshot.days).titlecaseFirstChar()} · ${action ?: "no pude actualizar"}"
        }
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
 * What the user should DO about this failure — or null when there is nothing specific to say.
 *
 * Null rather than a generic string on purpose: the generic tail differs by what the device has, and
 * only [toMetaText] knows that. "no pude actualizar" is right over a snapshot that exists and wrong
 * where there is none to update.
 *
 * The `null` **reason** is not defensive padding: `BackupFailureReason.fromNameOrNull` answers null
 * for a reason name a device upgraded past, so a real failing device can arrive here with no label.
 * It falls through to the generic tail, because the warning was decided by the streak, not by this.
 *
 * Only the reasons that change what the user would do get their own words, and they get them
 * **wherever they appear** — the age never buys silence. A dead refresh token is the case that
 * proves it: every cycle from that moment fails, the snapshot ages, and "no pude actualizar" would
 * never once name the one action that fixes it. The rest collapse: telling somebody their snapshot
 * failed to serialize, or that its read-back digest did not match, names a defect they cannot do
 * anything about and that belongs in the log.
 */
private fun BackupFailureReason?.toFailureAction(): String? = when (this) {
    BackupFailureReason.Network -> "revisa tu conexión"

    BackupFailureReason.Unauthorized -> "vuelve a iniciar sesión"

    // Its own words because it is the one reason that is NOT about the cloud: `safeDbCall` raises it
    // from the export's read of this phone's own database, and a retry prompt would send the user to
    // check a connection that is fine.
    BackupFailureReason.LocalDatabase -> "no pude leer tus datos"

    BackupFailureReason.Serialization,
    BackupFailureReason.Unverified,
    BackupFailureReason.Unknown,
    null,
    -> null
}
