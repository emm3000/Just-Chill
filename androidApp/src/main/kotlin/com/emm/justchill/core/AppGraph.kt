package com.emm.justchill.core

import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.commonCoreModule
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.core.di.backupModule
import com.emm.justchill.core.di.dataModule
import com.emm.justchill.core.di.sharedModule
import com.emm.justchill.core.di.supabaseModule
import com.emm.justchill.wiring.accountWiring
import com.emm.justchill.wiring.authWiring
import com.emm.justchill.wiring.categoryWiring
import com.emm.justchill.wiring.loanWiring
import com.emm.justchill.wiring.profileWiring
import com.emm.justchill.wiring.recurringWiring
import com.emm.justchill.wiring.reportWiring
import com.emm.justchill.wiring.transactionWiring
import org.koin.core.Koin
import org.koin.core.module.Module

fun appModules(platformModule: Module): List<Module> = listOf(
    backupModule,
    sharedModule,
    supabaseModule,
    dataModule,
    commonCoreModule,
    transactionWiring,
    accountWiring,
    categoryWiring,
    recurringWiring,
    reportWiring,
    loanWiring,
    profileWiring,
    authWiring,
    platformModule,
)

fun bootstrapAppGraph(koin: Koin) {
    if (koin.get<BackupAvailability>().isAvailable) koin.get<BackupOrchestrator>().start()
}
