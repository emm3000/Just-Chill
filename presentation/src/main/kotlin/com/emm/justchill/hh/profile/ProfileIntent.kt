package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiIntent

sealed interface ProfileIntent : UiIntent {
    data object ExportRequested : ProfileIntent
    data object ExportSaved : ProfileIntent
    data class ImportJson(val json: String) : ProfileIntent
    data object SignOut : ProfileIntent
    data object DeleteAccount : ProfileIntent

    data object BackUpNow : ProfileIntent

    data object VerifyBackup : ProfileIntent

    data object AcknowledgeBackupDestination : ProfileIntent
}
