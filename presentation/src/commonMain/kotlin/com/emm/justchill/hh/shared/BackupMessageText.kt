package com.emm.justchill.hh.shared

import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.justchill.hh.profile.ProfileMessage

fun ProfileMessage.Backup.toBackupText(): String = when (this) {
    ProfileMessage.BackupDone -> "Listo, tu respaldo está en la nube."
    ProfileMessage.BackupFailed -> "No pude respaldar en la nube — intenta de nuevo."
    ProfileMessage.BackupNeedsAccount -> "Inicia sesión para respaldar en la nube."
    ProfileMessage.BackupNeedsDisclosure -> "Primero confirma dónde va a quedar tu respaldo."
    ProfileMessage.BackupVerifyFailed -> "No pude verificar tu respaldo — intenta de nuevo."
    is ProfileMessage.BackupVerified -> verifiedText(snapshot)
    is ProfileMessage.BackupNotVerified -> notVerifiedText(pairsInspected)
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
    if (recurringMovements == 1) "1 plantilla" else "$recurringMovements plantillas",
).joinToString(", ")
