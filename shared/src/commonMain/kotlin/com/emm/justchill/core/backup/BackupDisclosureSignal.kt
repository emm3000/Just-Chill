package com.emm.justchill.core.backup

import com.emm.justchill.core.domain.auth.GetSessionStatusUseCase
import com.emm.justchill.core.domain.auth.SessionStatus
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.core.domain.shared.backup.BackupController
import com.emm.justchill.core.domain.shared.backup.BackupHealth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

class BackupDisclosureSignal(
    getSessionStatus: GetSessionStatusUseCase,
    backupController: BackupController,
    backupAvailability: BackupAvailability,
) {

    val isPending: Flow<Boolean> = if (backupAvailability.isAvailable) {
        combine(getSessionStatus(), backupController.health, ::disclosureIsPending).distinctUntilChanged()
    } else {
        flowOf(false)
    }
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
fun disclosureIsPending(session: SessionStatus, health: BackupHealth): Boolean =
    session is SessionStatus.Authenticated && !health.canUploadToDestination
