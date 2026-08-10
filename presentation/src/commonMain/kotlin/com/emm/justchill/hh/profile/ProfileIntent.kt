package com.emm.justchill.hh.profile

import com.emm.justchill.core.mvi.UiIntent

sealed interface ProfileIntent : UiIntent {
    /**
     * User asked to export. The ViewModel generates the backup JSON and emits it via
     * [ProfileEffect.ExportReady]; the platform layer does the SAF file write.
     * Symmetric with [ImportJson], which carries the file contents the other way.
     */
    data object ExportRequested : ProfileIntent
    data class ImportJson(val json: String) : ProfileIntent
    data object SignOut : ProfileIntent
    data object SyncNow : ProfileIntent
    data object DeleteAccount : ProfileIntent
}
