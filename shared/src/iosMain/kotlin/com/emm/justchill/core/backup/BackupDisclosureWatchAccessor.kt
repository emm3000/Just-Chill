package com.emm.justchill.core.backup

import kotlinx.coroutines.MainScope
import org.koin.mp.KoinPlatform

fun resolveBackupDisclosureWatch(): BackupDisclosureWatch =
    BackupDisclosureWatch(KoinPlatform.getKoin().get<BackupDisclosureSignal>().isPending, MainScope())
