package com.emm.justchill.feature.profile

import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface ProfileIntent : UiIntent {
    data object ExportRequested : ProfileIntent
    data class ExportFinished(val saved: Boolean) : ProfileIntent
    data class ImportJson(val json: String) : ProfileIntent
    data object SignOut : ProfileIntent
    data object DeleteAccount : ProfileIntent

    data object BackUpNow : ProfileIntent

    data object VerifyBackup : ProfileIntent

    data object AcknowledgeBackupDestination : ProfileIntent
}
