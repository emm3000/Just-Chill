package com.emm.justchill.feature.profile

import com.emm.justchill.core.domain.shared.backup.BackupFailureReason
import com.emm.justchill.core.presentation.mvi.UiState

sealed interface SessionUiState {
    data object Initializing : SessionUiState
    data object SignedOut : SessionUiState
    data class SignedIn(val email: String?) : SessionUiState
}

sealed interface LastExportUi {

    data object Never : LastExportUi

    data class DaysAgo(val days: Int) : LastExportUi
}

sealed interface LastSnapshot {

    data object None : LastSnapshot

    data object AgeUnknown : LastSnapshot

    data class DaysAgo(val days: Int, val isStale: Boolean) : LastSnapshot
}

enum class BackupRowSeverity { Normal, Warning, Danger }

sealed interface BackupRowUi {

    data object BackingUp : BackupRowUi

    data object NeedsAccount : BackupRowUi

    data object DisclosurePending : BackupRowUi

    data object Never : BackupRowUi

    data object Unreadable : BackupRowUi

    data class Failed(val reason: BackupFailureReason?, val lastSnapshot: LastSnapshot) : BackupRowUi

    data class Stale(val daysSinceLastBackup: Int) : BackupRowUi

    data class UpToDate(val daysSinceLastBackup: Int) : BackupRowUi
}

fun BackupRowUi.severity(): BackupRowSeverity = when (this) {
    BackupRowUi.NeedsAccount,
    BackupRowUi.BackingUp,
    BackupRowUi.Never,
    -> BackupRowSeverity.Normal

    BackupRowUi.DisclosurePending -> BackupRowSeverity.Warning

    BackupRowUi.Unreadable -> BackupRowSeverity.Warning

    is BackupRowUi.UpToDate -> BackupRowSeverity.Normal

    is BackupRowUi.Stale -> BackupRowSeverity.Warning

    is BackupRowUi.Failed -> when (val snapshot = lastSnapshot) {
        LastSnapshot.None -> BackupRowSeverity.Danger
        LastSnapshot.AgeUnknown -> BackupRowSeverity.Warning
        is LastSnapshot.DaysAgo -> if (snapshot.isStale) BackupRowSeverity.Danger else BackupRowSeverity.Warning
    }
}

enum class ProfileOp { None, Exporting, Importing, DeletingAccount, SigningOut, BackingUp, VerifyingBackup }

enum class ProfileDialog { None, DeleteAccount, Import, Export }

data class ProfileUiState(
    val op: ProfileOp = ProfileOp.None,
    val dialog: ProfileDialog = ProfileDialog.None,
    val categoryCount: Int = 0,
    val incomeCategoryCount: Int = 0,
    val lastExport: LastExportUi = LastExportUi.Never,
    val session: SessionUiState = SessionUiState.Initializing,
    val backupRow: BackupRowUi = BackupRowUi.NeedsAccount,
    val isCloudBackupAvailable: Boolean = false,
) : UiState {

    val categoriesLabel: String
        get() = "$categoryCount en total · $incomeCategoryCount de ingreso"

    val isSignedIn: Boolean
        get() = session is SessionUiState.SignedIn

    val cloudBackupActionsEnabled: Boolean
        get() = isSignedIn && op == ProfileOp.None

    val showsSignInRow: Boolean
        get() = isCloudBackupAvailable && !isSignedIn

    val showsBackupDestinationDisclosure: Boolean
        get() = backupRow == BackupRowUi.DisclosurePending

    val privacyMeta: String
        get() = when {
            !isSignedIn -> "100 % local, sin cuenta"
            isCloudBackupAvailable -> "En tu celular, con respaldo en tu cuenta"
            else -> "100 % local"
        }

    val accountLabel: String
        get() = (session as? SessionUiState.SignedIn)?.email ?: "Tu cuenta"

    val signOutMeta: String
        get() = if (op == ProfileOp.SigningOut) "Cerrando sesión…" else "Tus datos siguen en este teléfono"

    val deleteAccountMeta: String
        get() = if (op == ProfileOp.DeletingAccount) "Eliminando…" else "Borra tu cuenta y tus datos en la nube"

    val importWarning: String
        get() = if (session is SessionUiState.SignedIn) {
            "Tus movimientos, categorías y cuentas quedan tal cual el archivo. " +
                "Lo que no esté ahí se borra, y como tienes sesión iniciada también se " +
                "borra en tus otros dispositivos. No se puede deshacer."
        } else {
            "Tus movimientos, categorías y cuentas quedan tal cual el archivo. " +
                "Lo que no esté ahí se borra. No se puede deshacer."
        }

    val lastExportLabel: String
        get() = when (val export: LastExportUi = lastExport) {
            LastExportUi.Never -> "Nunca"
            is LastExportUi.DaysAgo -> "Último: ${daysAgoLabel(export.days)}"
        }
}
