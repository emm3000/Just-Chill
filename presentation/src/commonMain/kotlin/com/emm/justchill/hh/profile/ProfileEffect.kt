package com.emm.justchill.hh.profile

import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.mvi.UiEffect

sealed interface ProfileEffect : UiEffect {
    data class ShowError(val error: DomainException) : ProfileEffect
    data class Notify(val message: ProfileMessage) : ProfileEffect
    data class ExportReady(val json: String) : ProfileEffect
}

sealed interface ProfileMessage {
    data object SessionClosed : ProfileMessage
    data object SessionClosedLocallyOnly : ProfileMessage
    data object AccountDeleted : ProfileMessage
    data object ExportDone : ProfileMessage
    data object ExportFailed : ProfileMessage
    data class ImportDone(val transactions: Int, val recurring: Int) : ProfileMessage
    data object ImportFailed : ProfileMessage
    data object BackupDone : ProfileMessage
    data object BackupFailed : ProfileMessage
    data object BackupNeedsAccount : ProfileMessage
    data object OperationInProgress : ProfileMessage
}
