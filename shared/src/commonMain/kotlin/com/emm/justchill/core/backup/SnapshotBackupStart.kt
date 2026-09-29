package com.emm.justchill.core.backup

import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import org.koin.core.Koin
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
fun startSnapshotBackup(koin: Koin) {
    if (koin.get<BackupAvailability>().isAvailable) koin.get<BackupOrchestrator>().start()
}
