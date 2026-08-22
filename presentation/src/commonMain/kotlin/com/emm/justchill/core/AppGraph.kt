package com.emm.justchill.core

import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.backup.SNAPSHOT_BACKUP_ENABLED
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.authModule
import com.emm.justchill.hh.di.backupModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.dataModule
import com.emm.justchill.hh.di.homeModule
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

// The single shared Koin module list. The platform module supplies the platform-specific singles (DB
// driver, Settings, SupabaseConfig, app version, Google sign-in launcher, dispatchers); everything
// else — feature modules + supabase/auth/data wiring + commonCore — is identical on both platforms
// and lives here. startKoin {} itself is NOT shared: Android needs androidContext() /
// androidLogger() from koin-android, absent in commonMain. Android also appends its flavor-only
// experiencesModule to the returned list.
fun appModules(platformModule: Module): List<Module> = listOf(
    transactionModule,
    seetransactionsModule,
    accountModule,
    categoryModule,
    homeModule,
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

// Shared post-startKoin sequence (mirrors the old EmmApp.onCreate / initKoin tails). Both platforms
// call this immediately after startKoin returns.
fun bootstrapAppGraph(koin: Koin) {
    // Kill switch, off today: skipping start() registers no trigger and launches no request
    // consumer — and that consumer is the only caller of the private cycle — so nothing can export,
    // upload or prune. The single stays bound and lazily resolvable; only its loops are off. See
    // BackupKillSwitch.kt.
    if (SNAPSHOT_BACKUP_ENABLED) koin.get<BackupOrchestrator>().start()
}
