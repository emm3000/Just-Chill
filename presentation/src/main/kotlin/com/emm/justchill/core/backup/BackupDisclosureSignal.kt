package com.emm.justchill.core.backup

import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

class BackupDisclosureSignal(observeSession: ObserveSessionUseCase, backupController: BackupController) {

    // SNAPSHOT_BACKUP_ENABLED off: the orchestrator never starts, so canUploadToDestination stays
    // false for every account and an ungated signal would announce a gate that cannot run.
    val isPending: Flow<Boolean> = if (SNAPSHOT_BACKUP_ENABLED) {
        combine(observeSession(), backupController.health, ::disclosureIsPending).distinctUntilChanged()
    } else {
        flowOf(false)
    }
}

internal fun disclosureIsPending(session: SessionStatus, health: BackupHealth): Boolean =
    session is SessionStatus.Authenticated && !health.canUploadToDestination
