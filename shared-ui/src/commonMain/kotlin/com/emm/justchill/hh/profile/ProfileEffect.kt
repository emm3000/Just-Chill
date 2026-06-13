package com.emm.justchill.hh.profile

import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.mvi.UiEffect

sealed interface ProfileEffect : UiEffect {
    data class ShowError(val error: DomainException) : ProfileEffect
    data class Notify(val message: ProfileMessage) : ProfileEffect

    /**
     * The backup JSON is ready. The platform layer (nav host) owns the SAF file IO:
     * it picks a destination and writes [json] to it, then surfaces success/failure.
     * Keeping the IO out of the ViewModel is what lets this code live in commonMain.
     */
    data class ExportReady(val json: String) : ProfileEffect
}

sealed interface ProfileMessage {
    data object SessionClosed : ProfileMessage
    data object AccountDeleted : ProfileMessage
    data object ExportDone : ProfileMessage
    data object ExportFailed : ProfileMessage
    data class ImportDone(val transactions: Int) : ProfileMessage
    data object ImportFailed : ProfileMessage
}
