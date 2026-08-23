package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.justchill.hh.profile.ProfileMessage

fun ProfileMessage.Backup.toBackupText(): String = when (this) {
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
    val opening = if (snapshot.isNewestPair) "Verificado" else "Verificado un respaldo más antiguo"
    return "$opening: ${snapshot.fileName} — ${snapshot.rowCounts.toPhrase()}"
}

private fun notVerifiedText(pairsInspected: Int): String = when (pairsInspected) {
    0 -> "No encontré ningún respaldo completo para verificar."
    1 -> "Revisé el único respaldo que hay y no se puede restaurar."
    else -> "Revisé los $pairsInspected respaldos más recientes y ninguno se puede restaurar."
}

private fun BackupRowCounts.toPhrase(): String = listOf(
    if (accounts == 1) "1 cuenta" else "$accounts cuentas",
    if (categories == 1) "1 categoría" else "$categories categorías",
    if (transactions == 1) "1 movimiento" else "$transactions movimientos",
    if (recurringMovements == 1) "1 recurrente" else "$recurringMovements recurrentes",
    if (loans == 1) "1 préstamo" else "$loans préstamos",
    if (loanPayments == 1) "1 abono" else "$loanPayments abonos",
).joinToString(", ")
