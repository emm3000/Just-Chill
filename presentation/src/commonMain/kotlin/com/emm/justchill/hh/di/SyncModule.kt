package com.emm.justchill.hh.di

import com.emm.data.sync.AccountTableSync
import com.emm.data.sync.CategoryTableSync
import com.emm.data.sync.DefaultSyncRepository
import com.emm.data.sync.RecurringMovementTableSync
import com.emm.data.sync.TableSync
import com.emm.data.sync.TransactionTableSync
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncDataUseCase
import com.emm.domain.sync.SyncRepository
import com.emm.justchill.core.appScopeQualifier
import com.emm.justchill.core.lifecycle.resumeEvents
import com.emm.justchill.core.sync.DefaultSyncCursorStore
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncOrchestrator
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val accountSyncQualifier = named("accountSync")
private val categorySyncQualifier = named("categorySync")
private val transactionSyncQualifier = named("transactionSync")
private val recurringSyncQualifier = named("recurringSync")

// resumeEvents() is expect/actual in commonMain (Android ProcessLifecycleOwner / iOS
// UIApplicationDidBecomeActive) — no platform branching here. The connectivity-regained trigger is
// absent on both platforms (shared cross-platform debt).
val syncModule = module {
    // Per-table sync units — qualified so DefaultSyncRepository can distinguish the four TableSync
    // slots even though they share the interface type. Each takes (EmmDatabaseData, SupabaseClient,
    // DiagnosticsLogger); the logger is the platform single that makes their silent row skips visible.
    factory<TableSync>(accountSyncQualifier) { AccountTableSync(get(), get(), get()) }
    factory<TableSync>(categorySyncQualifier) { CategoryTableSync(get(), get(), get()) }
    factory<TableSync>(transactionSyncQualifier) { TransactionTableSync(get(), get(), get()) }
    factory<TableSync>(recurringSyncQualifier) { RecurringMovementTableSync(get(), get(), get()) }

    // Domain port: cursor store over AppPreferences (a stateless adapter; cursor state lives in prefs).
    factoryOf(::DefaultSyncCursorStore) { bind<SyncCursorStore>() }

    // DefaultSyncRepository dependency — easy to miss; omitting it crashes at first sync resolution.
    factory { ConflictResolver() }

    // FK-safe ordering preserved: accounts -> categories -> transactions -> recurring_movements.
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

    singleOf(::SyncDataUseCase)

    // Single: owns long-lived coroutine jobs launched in externalScope. Does NOT self-start — start()
    // is called by bootstrapAppGraph after the graph is built (same lifecycle on both platforms).
    single {
        SyncOrchestrator(
            syncData = get(),
            observeSession = get(),
            syncRepository = get(),
            signOut = get(),
            prefs = get(),
            externalScope = get(appScopeQualifier),
            resumeEvents = resumeEvents(),
            logger = get(),
        )
    }

    // Bound to the SAME SyncOrchestrator single so commonMain consumers (ProfileViewModel) resolve the
    // same instance that bootstrapAppGraph started.
    single<SyncController> { get<SyncOrchestrator>() }
}
