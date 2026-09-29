package com.emm.justchill.core

import com.emm.justchill.core.backup.startSnapshotBackup
import com.emm.justchill.core.di.kitModules
import com.emm.justchill.wiring.featureWirings
import org.koin.core.Koin
import org.koin.core.module.Module

fun appModules(platformModule: Module): List<Module> = kitModules + featureWirings + platformModule

fun bootstrapAppGraph(koin: Koin) {
    startSnapshotBackup(koin)
}
