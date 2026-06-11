package com.emm.justchill.hh.profile

import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.mvi.UiEffect

sealed interface ProfileEffect : UiEffect {
    data class ShowError(val error: DomainException) : ProfileEffect
    data class Notify(val message: ProfileMessage) : ProfileEffect
}

sealed interface ProfileMessage {
    data object SessionClosed : ProfileMessage
    data object AccountDeleted : ProfileMessage
    data object ExportDone : ProfileMessage
    data object ExportFailed : ProfileMessage
    data class ImportDone(val transactions: Int) : ProfileMessage
    data object ImportFailed : ProfileMessage
}
