package com.emm.justchill.hh.profile

import com.emm.domain.shared.Money
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.justchill.core.mvi.UiState

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

    /**
     * @property reason may be null even on a real, ongoing failure streak — this case is chosen on
     *   `consecutiveFailures > 0`, never on `reason != null`.
     */
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

data class ProfileUiState(
    val op: ProfileOp = ProfileOp.None,
    val categoryCount: Int = 0,
    val incomeCategoryCount: Int = 0,
    val recurringCount: Int = 0,
    val recurringMonthlyOutflow: Money = Money.Zero,
    val lastExport: LastExportUi = LastExportUi.Never,
    val session: SessionUiState = SessionUiState.Initializing,
    val backupRow: BackupRowUi = BackupRowUi.NeedsAccount,
) : UiState
