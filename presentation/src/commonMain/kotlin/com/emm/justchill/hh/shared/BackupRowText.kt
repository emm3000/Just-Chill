package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.hh.profile.BackupRowUi
import com.emm.justchill.hh.profile.LastSnapshot

fun BackupRowUi.toMetaText(): String = when (this) {
    BackupRowUi.BackingUp -> "Respaldando…"

    BackupRowUi.NeedsAccount -> "Inicia sesión para respaldar en la nube"

    BackupRowUi.Never -> "Todavía no hay ningún respaldo"

    BackupRowUi.Unreadable -> "No pude leer el estado del respaldo"

    is BackupRowUi.Failed -> {
        val action: String? = reason.toFailureAction()
        when (val snapshot = lastSnapshot) {
            LastSnapshot.None -> "Sin respaldo · ${action ?: "intenta de nuevo"}"

            LastSnapshot.AgeUnknown -> "No pude respaldar · ${action ?: "intenta de nuevo"}"

            is LastSnapshot.DaysAgo ->
                "${backupAgeLabel(snapshot.days).titlecaseFirstChar()} · ${action ?: "no pude actualizar"}"
        }
    }

    is BackupRowUi.Stale ->
        "${backupAgeLabel(daysSinceLastBackup).titlecaseFirstChar()} · hay cambios sin respaldar"

    is BackupRowUi.UpToDate -> backupAgeLabel(daysSinceLastBackup).titlecaseFirstChar()
}

private fun backupAgeLabel(days: Int): String = when (days) {
    0 -> "hoy"
    1 -> "ayer"
    else -> "hace $days días"
}

private fun BackupFailureReason?.toFailureAction(): String? = when (this) {
    BackupFailureReason.Network -> "revisa tu conexión"

    BackupFailureReason.Unauthorized -> "vuelve a iniciar sesión"

    BackupFailureReason.LocalDatabase -> "no pude leer tus datos"

    BackupFailureReason.Serialization,
    BackupFailureReason.Unverified,
    BackupFailureReason.Unknown,
    null,
    -> null
}
