package com.emm.justchill.core

import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.backup.SNAPSHOT_BACKUP_ENABLED
import com.emm.justchill.core.commonCoreModule
import com.emm.justchill.hh.di.authModule
import com.emm.justchill.hh.di.backupModule
import com.emm.justchill.hh.di.dataModule
import com.emm.justchill.hh.di.profileModule
import com.emm.justchill.hh.di.seetransactionsModule
import com.emm.justchill.hh.di.sharedModule
import com.emm.justchill.hh.di.supabaseModule
import com.emm.justchill.hh.di.transactionModule
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

// EmmApp calls startKoin with this list plus its own experiencesModule. The hh.di modules and
// commonCoreModule still live in :presentation; each *Wiring module takes over as ADR 015's
// waves 7 and 8 extract its feature.
fun appModules(platformModule: Module): List<Module> = listOf(
    transactionModule,
    seetransactionsModule,
    profileModule,
    backupModule,
    sharedModule,
    supabaseModule,
    authModule,
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
    // SNAPSHOT_BACKUP_ENABLED off: skipping start() leaves BackupOrchestrator bound and lazily
    // resolvable, only its export/upload/prune loops are off. See BackupKillSwitch.kt.
    if (SNAPSHOT_BACKUP_ENABLED) koin.get<BackupOrchestrator>().start()
}
