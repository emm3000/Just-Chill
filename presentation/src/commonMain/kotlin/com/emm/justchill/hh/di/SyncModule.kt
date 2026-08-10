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
import com.emm.domain.sync.SyncLogger
import com.emm.domain.sync.SyncMutex
import com.emm.domain.sync.SyncRepository
import com.emm.justchill.core.sync.DefaultSyncCursorStore
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncOrchestrator
import com.emm.justchill.core.sync.resumeEvents
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val accountSyncQualifier = named("accountSync")
private val categorySyncQualifier = named("categorySync")
private val transactionSyncQualifier = named("transactionSync")
private val recurringSyncQualifier = named("recurringSync")

// Single application-lifetime coroutine scope qualifier. ONE scope drives BOTH the claim-on-sign-in
// observer (bootstrapAppGraph) and the SyncOrchestrator's internal loops. Public so AppGraph resolves it.
val appScopeQualifier = named("appScope")

// Single commonMain sync wiring (replaces :androidApp/hh/di/SyncModule.kt + KoinIos.kt's iosSyncModule
// AND the SyncMutex previously bound in iosProfileSupportModule). resumeEvents() is expect/actual in
// commonMain (Android ProcessLifecycleOwner / iOS UIApplicationDidBecomeActive) — no platform branching
// here. The connectivity-regained trigger is absent on both platforms (shared cross-platform debt).
val syncModule = module {
    // Per-table sync units — qualified so DefaultSyncRepository can distinguish the four TableSync
    // slots even though they share the interface type. Each takes (EmmDatabaseData, SupabaseClient,
    // SyncLogger); the logger is the platform single that makes their silent row skips visible.
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

    // Single: ONE lock per process. Serializes sync cycles against each other AND against account
    // deletion (in-flight push must not resurrect rows after delete_account).
    single { SyncMutex() }

    singleOf(::SyncDataUseCase)

    // Application-lifetime scope for SyncOrchestrator long-lived jobs and the claim observer.
    // The handler is a backstop, not the primary defence: SyncOrchestrator and the claim observer
    // both catch their own failures. Without it, anything they miss reaches the default handler,
    // which on Android is a crash — for a feature the app is fully usable without. The logger is
    // resolved once, up front, so the handler never has to touch Koin while unwinding a failure.
    single<CoroutineScope>(appScopeQualifier) {
        val logger = get<SyncLogger>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            logger.warn("uncaught in appScope", throwable)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }

    factoryOf(::ObservePendingSyncCountUseCase)

    // Single: owns long-lived coroutine jobs launched in externalScope. Does NOT self-start — start()
    // is called by bootstrapAppGraph after the graph is built (same lifecycle on both platforms).
    single {
        SyncOrchestrator(
            syncData = get(),
            observeSession = get(),
            observePendingCount = get(),
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
