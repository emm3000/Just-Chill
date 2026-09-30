package com.emm.justchill.feature.profile

import com.emm.justchill.core.domain.shared.backup.BackupFailureReason
import com.emm.justchill.core.domain.shared.backup.BackupRowCounts
import com.emm.justchill.core.domain.shared.backup.BackupVerification

fun ProfileMessage.toText(): String = when (this) {
    ProfileMessage.SessionClosed -> "Sesión cerrada. Tus datos siguen en este teléfono."

    ProfileMessage.SessionClosedLocallyOnly ->
        "Sesión cerrada acá; no llegué al servidor, así que tu acceso remoto sigue activo hasta " +
            "que expire. Cierra sesión con internet para cortarlo. Tus datos siguen en este teléfono."

    ProfileMessage.AccountDeleted -> "Cuenta eliminada. Tus datos siguen en este teléfono."

    ProfileMessage.ExportDone -> "Listo, tu data está guardada."

    ProfileMessage.ExportFailed -> "No pude exportar — capaz no hay espacio en tu celu?"

    ProfileMessage.CsvExportFailed -> "No pude exportar tus movimientos. Inténtalo de nuevo."

    is ProfileMessage.ImportDone -> importDoneText(transactions, recurring, loans, loanPayments)

    ProfileMessage.ImportFailed -> "No pude importar el archivo — capaz está dañado."

    ProfileMessage.OperationInProgress -> "Espera a que termine la operación en curso."

    is ProfileMessage.Backup -> backupText()
}

private fun ProfileMessage.Backup.backupText(): String = when (this) {
    ProfileMessage.BackupDone -> "Listo, tu respaldo está en la nube."
    ProfileMessage.BackupNeedsAccount -> "Inicia sesión para respaldar en la nube."
    ProfileMessage.BackupNeedsDisclosure -> "Primero confirma dónde va a quedar tu respaldo."
    ProfileMessage.BackupVerifyFailed -> "No pude verificar tu respaldo — intenta de nuevo."
    is ProfileMessage.BackupFailed -> failedText(reason)
    is ProfileMessage.BackupVerified -> verifiedText(snapshot)
    is ProfileMessage.BackupNotVerified -> notVerifiedText(pairsInspected)
}

private fun failedText(reason: BackupFailureReason): String = when (reason) {
    BackupFailureReason.Network -> "No llegué a la nube — revisa tu conexión e intenta de nuevo."
    BackupFailureReason.Unauthorized -> "Tu sesión ya no vale para respaldar — vuelve a iniciar sesión."
    BackupFailureReason.Busy -> "Hay otra operación en curso — espera, el respaldo se reintenta solo."
    BackupFailureReason.Serialization -> "No pude armar el archivo del respaldo — es una falla de la app, no tuya."
    BackupFailureReason.RemoteRejected -> "El servidor rechazó tu respaldo — no depende de ti, lo reintento más tarde."
    BackupFailureReason.Unverified -> "El respaldo no coincidió al verificarlo y lo descarté — lo reintento solo."
    BackupFailureReason.LocalDatabase -> "No pude leer tus datos de este teléfono para armar el respaldo."
    BackupFailureReason.Unknown -> "El respaldo falló por algo inesperado — no es algo que hayas hecho mal."
}

private fun verifiedText(snapshot: BackupVerification.Verified): String {
    val opening: String = if (snapshot.isNewestPair) "Verificado" else "Verificado un respaldo más antiguo"
    return "$opening: ${snapshot.fileName} — ${snapshot.rowCounts.toPhrase()}"
}

private fun notVerifiedText(pairsInspected: Int): String = when (pairsInspected) {
    0 -> "No encontré ningún respaldo completo para verificar."
    1 -> "Revisé el único respaldo que hay y no se puede restaurar."
    else -> "Revisé los $pairsInspected respaldos más recientes y ninguno se puede restaurar."
}

private fun BackupRowCounts.toPhrase(): String = listOf(
    countClause(accounts, "cuenta", "cuentas"),
    countClause(categories, "categoría", "categorías"),
    countClause(transactions, "movimiento", "movimientos"),
    countClause(recurringMovements, "recurrente", "recurrentes"),
    countClause(loans, "préstamo", "préstamos"),
    countClause(loanPayments, "abono", "abonos"),
).joinToString(", ")
