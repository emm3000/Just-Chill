package com.emm.justchill.core

import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.backup.SNAPSHOT_BACKUP_ENABLED
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.authModule
import com.emm.justchill.hh.di.backupModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.dataModule
import com.emm.justchill.hh.di.loanModule
import com.emm.justchill.hh.di.profileModule
import com.emm.justchill.hh.di.recurringModule
import com.emm.justchill.hh.di.reportModule
import com.emm.justchill.hh.di.seetransactionsModule
import com.emm.justchill.hh.di.sharedModule
import com.emm.justchill.hh.di.supabaseModule
import com.emm.justchill.hh.di.transactionModule
import org.koin.core.Koin
import org.koin.core.module.Module

// startKoin {} is not called here: it needs androidContext() / androidLogger() from koin-android,
// so :androidApp calls it directly with this list plus platformModule and its own experiencesModule.
fun appModules(platformModule: Module): List<Module> = listOf(
    transactionModule,
    seetransactionsModule,
    accountModule,
    categoryModule,
    loanModule,
    recurringModule,
    reportModule,
    profileModule,
    backupModule,
    sharedModule,
    supabaseModule,
    authModule,
    dataModule,
    commonCoreModule,
    platformModule,
)

fun bootstrapAppGraph(koin: Koin) {
    // SNAPSHOT_BACKUP_ENABLED off: skipping start() leaves BackupOrchestrator bound and lazily
    // resolvable, only its export/upload/prune loops are off. See BackupKillSwitch.kt.
    if (SNAPSHOT_BACKUP_ENABLED) koin.get<BackupOrchestrator>().start()
}
