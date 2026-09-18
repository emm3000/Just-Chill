package com.emm.justchill.core.backup

import com.emm.justchill.core.domain.auth.ObserveSessionUseCase
import com.emm.justchill.core.domain.auth.SessionStatus
import com.emm.justchill.core.domain.shared.backup.BackupController
import com.emm.justchill.core.domain.shared.backup.BackupHealth
import com.emm.justchill.core.domain.shared.backup.SNAPSHOT_BACKUP_ENABLED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

class BackupDisclosureSignal(observeSession: ObserveSessionUseCase, backupController: BackupController) {

    val isPending: Flow<Boolean> = if (SNAPSHOT_BACKUP_ENABLED) {
        combine(observeSession(), backupController.health, ::disclosureIsPending).distinctUntilChanged()
    } else {
        flowOf(false)
    }
}

internal fun disclosureIsPending(session: SessionStatus, health: BackupHealth): Boolean =
    session is SessionStatus.Authenticated && !health.canUploadToDestination
