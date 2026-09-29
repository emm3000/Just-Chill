package com.emm.justchill.feature.profile

import com.emm.justchill.core.domain.shared.backup.BackupFailureReason
import com.emm.justchill.core.domain.shared.backup.BackupVerification
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.presentation.mvi.UiEffect

sealed interface ProfileEffect : UiEffect {
    data class ShowError(val error: DomainException) : ProfileEffect
    data class Notify(val message: ProfileMessage) : ProfileEffect
    data class ExportReady(val json: String) : ProfileEffect
    data class CsvReady(val fileName: String, val content: String) : ProfileEffect
}

sealed interface ProfileMessage {

    sealed interface Backup : ProfileMessage

    data object SessionClosed : ProfileMessage {
        val text: String
            get() = "Sesión cerrada. Tus datos siguen en este teléfono."
    }

    data object SessionClosedLocallyOnly : ProfileMessage {
        val text: String
            get() = "Sesión cerrada acá; no llegué al servidor, así que tu acceso remoto sigue activo hasta " +
                "que expire. Cierra sesión con internet para cortarlo. Tus datos siguen en este teléfono."
    }

    data object AccountDeleted : ProfileMessage {
        val text: String
            get() = "Cuenta eliminada. Tus datos siguen en este teléfono."
    }

    data object ExportDone : ProfileMessage
    data object ExportFailed : ProfileMessage
    data object CsvExportFailed : ProfileMessage
    data class ImportDone(val transactions: Int, val recurring: Int, val loans: Int, val loanPayments: Int) :
        ProfileMessage {

        val summary: String
            get() {
                val clauses: List<String> = buildList {
                    add(countClause(transactions, "movimiento", "movimientos"))
                    if (recurring > 0) add(countClause(recurring, "recurrente", "recurrentes"))
                    if (loans > 0) add(countClause(loans, "préstamo", "préstamos"))
                    if (loanPayments > 0) add(countClause(loanPayments, "abono", "abonos"))
                }
                val participle: String = if (clauses.size == 1 && transactions == 1) "importado" else "importados"
                return "Listo — ${joinedClauses(clauses)} $participle."
            }
    }
    data object ImportFailed : ProfileMessage
    data object OperationInProgress : ProfileMessage

    data object BackupDone : Backup
    data class BackupFailed(val reason: BackupFailureReason) : Backup
    data object BackupNeedsAccount : Backup
    data object BackupNeedsDisclosure : Backup
    data class BackupVerified(val snapshot: BackupVerification.Verified) : Backup
    data class BackupNotVerified(val pairsInspected: Int) : Backup
    data object BackupVerifyFailed : Backup
}

private fun countClause(count: Int, singular: String, plural: String): String =
    if (count == 1) "1 $singular" else "$count $plural"

private fun joinedClauses(clauses: List<String>): String =
    if (clauses.size <= 1) clauses.joinToString() else "${clauses.dropLast(1).joinToString(", ")} y ${clauses.last()}"
