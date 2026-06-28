package com.emm.justchill.hh.di

import com.emm.data.sync.AccountTableSync
import com.emm.data.sync.CategoryTableSync
import com.emm.data.sync.DefaultSyncRepository
import com.emm.data.sync.RecurringMovementTableSync
import com.emm.data.sync.TableSync
import com.emm.data.sync.TransactionTableSync
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.ObservePendingSyncCountUseCase
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncDataUseCase
import com.emm.domain.sync.SyncMutex
import com.emm.domain.sync.SyncRepository
import com.emm.justchill.core.sync.AppPreferencesSyncCursorStore
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncOrchestrator
import com.emm.justchill.core.sync.resumeEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

private val accountSyncQualifier = named("accountSync")
private val categorySyncQualifier = named("categorySync")
private val transactionSyncQualifier = named("transactionSync")
private val recurringSyncQualifier = named("recurringSync")
val appScopeQualifier = named("appScope")

val syncModule = module {
    // Per-table sync units — registered with qualifiers so DefaultSyncRepository can
    // distinguish the four TableSync slots even though they share the same interface type.
    factory<TableSync>(accountSyncQualifier) { AccountTableSync(get(), get()) }
    factory<TableSync>(categorySyncQualifier) { CategoryTableSync(get(), get()) }
    factory<TableSync>(transactionSyncQualifier) { TransactionTableSync(get(), get()) }
    factory<TableSync>(recurringSyncQualifier) { RecurringMovementTableSync(get(), get()) }

    // Domain port: cursor store implemented over AppPreferences
    factoryOf(::AppPreferencesSyncCursorStore) bind SyncCursorStore::class

    factory { ConflictResolver() }

    // FK-safe ordering preserved: accounts → categories → transactions → recurring_movements.
    factory<SyncRepository> {
        DefaultSyncRepository(
            observeSession = get(),
            cursorStore = get(),
            conflictResolver = get(),
            accountSync = get(accountSyncQualifier),
            categorySync = get(categorySyncQualifier),
            transactionSync = get(transactionSyncQualifier),
            recurringSync = get(recurringSyncQualifier),
        )
    }

    // Single: ONE lock per process. Serializes sync cycles against each other AND against
    // account deletion (in-flight push must not resurrect rows after delete_account).
    single { SyncMutex() }

    singleOf(::SyncDataUseCase)

    // Application-lifetime scope for SyncOrchestrator long-lived jobs.
    single<CoroutineScope>(appScopeQualifier) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    factoryOf(::ObservePendingSyncCountUseCase)

    // Single: owns long-lived coroutine jobs launched in externalScope.
    // Bound to SyncController so commonMain consumers (ProfileViewModel) resolve the same instance.
    single {
        SyncOrchestrator(
            syncData = get(),
            observeSession = get(),
            observePendingCount = get(),
            signOut = get(),
            prefs = get(),
            externalScope = get(appScopeQualifier),
            resumeEvents = resumeEvents(),
        )
    } bind SyncController::class
}
