package com.emm.justchill.core.domain.shared.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BackupController {

    val isBackingUp: StateFlow<Boolean>

    val events: Flow<BackupEvent>

    val health: StateFlow<BackupHealth>

    fun requestBackup(manual: Boolean = false)

    fun acknowledgeDestination(requestCycle: Boolean = true)
}
