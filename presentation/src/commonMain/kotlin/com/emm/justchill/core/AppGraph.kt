package com.emm.justchill.core

import com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase
import com.emm.justchill.core.sync.SYNC_TEMPORARILY_DISABLED
import com.emm.justchill.core.sync.SyncOrchestrator
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.appScopeQualifier
import com.emm.justchill.hh.di.authModule
import com.emm.justchill.hh.di.backupModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.dataModule
import com.emm.justchill.hh.di.homeModule
import com.emm.justchill.hh.di.profileModule
import com.emm.justchill.hh.di.recurringModule
import com.emm.justchill.hh.di.reportModule
import com.emm.justchill.hh.di.seetransactionsModule
import com.emm.justchill.hh.di.sharedModule
import com.emm.justchill.hh.di.supabaseModule
import com.emm.justchill.hh.di.syncModule
import com.emm.justchill.hh.di.transactionModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.module.Module

// The single shared Koin module list. The platform module supplies the platform-specific singles (DB
// driver, Settings, SupabaseConfig, app version, Google sign-in launcher, dispatchers); everything
// else — feature modules + supabase/sync/auth/data wiring + commonCore — is identical on both
// platforms and lives here. startKoin {} itself is NOT shared: Android needs androidContext() /
// androidLogger() from koin-android, absent in commonMain. Android also appends its flavor-only
// experiencesModule to the returned list.
fun appModules(platformModule: Module): List<Module> = listOf(
    transactionModule,
    seetransactionsModule,
    accountModule,
    categoryModule,
    homeModule,
    recurringModule,
    reportModule,
    profileModule,
    backupModule,
    sharedModule,
    supabaseModule,
    syncModule,
    authModule,
    dataModule,
    commonCoreModule,
    platformModule,
)

// Shared post-startKoin sequence (mirrors the old EmmApp.onCreate / initKoin tails). Resolves the
// single application-lifetime appScope, launches the claim-on-sign-in observer on it, and starts the
// SyncOrchestrator's trigger loops. Both platforms call this immediately after startKoin returns. The
// SyncOrchestrator single is lazy, so resolving it here also forces construction; the same instance
// is later resolved by ProfileViewModel (via SyncController).
fun bootstrapAppGraph(koin: Koin) {
    val appScope = koin.get<CoroutineScope>(appScopeQualifier)
    val claimOnAuthentication = koin.get<ClaimLocalDataOnAuthenticationUseCase>()
    // Deliberately NOT gated by SYNC_TEMPORARILY_DISABLED: claiming stamps the signed-in userId onto
    // local rows — ownership, not transport — and never touches the network. Leaving it on keeps row
    // ownership correct while sync is off, so flipping the switch back needs no catch-up pass. The
    // rows it marks Pending are inert as long as nothing pushes.
    appScope.launch { claimOnAuthentication() }
    // Kill switch. Skipping start() launches no trigger (resume / sign-in / debounced writes) and no
    // request consumer — and that consumer is the only caller of the private runSync(), so no cycle
    // can reach the network. The single stays bound and lazily resolvable; only its loops are off.
    if (!SYNC_TEMPORARILY_DISABLED) koin.get<SyncOrchestrator>().start()
}
