package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.hh.profile.BackupRowUi
import com.emm.justchill.hh.profile.LastSnapshot

const val BACKUP_DESTINATION_DISCLOSURE: String =
    "Tu respaldo va a llevar TODO lo que hay en este teléfono a esta cuenta, incluso lo que " +
        "registraste con otra cuenta: cerrar sesión no borra nada de acá."

const val BACKUP_DESTINATION_DISCLOSURE_ACTION: String = "Entendido, respaldar"

const val BACKUP_LOCAL_ONLY_WARNING: String =
    "Nada de esto sale de tu teléfono. Si lo pierdes o cambias de celular sin exportar, tu " +
        "data se va con él."

fun BackupRowUi.toMetaText(): String = when (this) {
    BackupRowUi.BackingUp -> "Respaldando…"

    BackupRowUi.NeedsAccount -> "Inicia sesión para respaldar en la nube"

    BackupRowUi.DisclosurePending -> "Falta tu confirmación para respaldar en esta cuenta"

    BackupRowUi.Never -> "Todavía no hay ningún respaldo"

    BackupRowUi.Unreadable -> "No pude leer el estado del respaldo"

    is BackupRowUi.Failed -> failedMetaText(reason, lastSnapshot)

    is BackupRowUi.Stale ->
        "${backupAgeLabel(daysSinceLastBackup).titlecaseFirstChar()} · hay cambios sin respaldar"

    is BackupRowUi.UpToDate -> backupAgeLabel(daysSinceLastBackup).titlecaseFirstChar()
}

private fun failedMetaText(reason: BackupFailureReason?, lastSnapshot: LastSnapshot): String {
    val action: String? = reason.toFailureAction()
    return when (lastSnapshot) {
        LastSnapshot.None -> "Sin respaldo · ${action ?: "intenta de nuevo"}"

        LastSnapshot.AgeUnknown -> "No pude respaldar · ${action ?: "intenta de nuevo"}"

        is LastSnapshot.DaysAgo ->
            "${backupAgeLabel(lastSnapshot.days).titlecaseFirstChar()} · ${action ?: "no pude actualizar"}"
    }
}

private fun backupAgeLabel(days: Int): String = when (days) {
    0 -> "hoy"
    1 -> "ayer"
    else -> "hace $days días"
}

private fun BackupFailureReason?.toFailureAction(): String? = when (this) {
    BackupFailureReason.Network -> "revisa tu conexión"

    BackupFailureReason.Unauthorized -> "vuelve a iniciar sesión"

    BackupFailureReason.Busy,
    BackupFailureReason.Serialization,
    BackupFailureReason.Unverified,
    BackupFailureReason.RemoteRejected,
    BackupFailureReason.Unknown,
    BackupFailureReason.LocalDatabase,
    null,
    -> null
}
