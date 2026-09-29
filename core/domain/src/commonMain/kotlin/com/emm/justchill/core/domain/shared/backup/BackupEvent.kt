package com.emm.justchill.core.domain.shared.backup

import com.emm.justchill.core.domain.shared.error.DomainException

sealed interface BackupEvent {

    data object Succeeded : BackupEvent

    data class Failed(val cause: DomainException) : BackupEvent
}
