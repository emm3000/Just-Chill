package com.emm.justchill.core

import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.di.kitModules
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.wiring.featureWirings
import org.koin.core.Koin
import org.koin.core.module.Module

fun appModules(platformModule: Module): List<Module> = kitModules + featureWirings + platformModule

fun bootstrapAppGraph(koin: Koin) {
    if (koin.get<BackupAvailability>().isAvailable) koin.get<BackupOrchestrator>().start()
}
