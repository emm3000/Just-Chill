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

    /**
     * User asked for a snapshot backup now (ADR 009 2c-iv). Unlike [ExportRequested], which hands
     * the platform layer a JSON file to write, this asks the always-running backup orchestrator for
     * a cycle and waits for it to report back — the ViewModel neither exports nor uploads anything.
     */
    data object BackUpNow : ProfileIntent
}
