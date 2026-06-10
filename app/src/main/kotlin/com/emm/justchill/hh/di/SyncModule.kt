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
import com.emm.justchill.core.sync.AppPreferencesSyncCursorStore
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

private val accountSyncQualifier = named("accountSync")
private val categorySyncQualifier = named("categorySync")
private val transactionSyncQualifier = named("transactionSync")
private val recurringSyncQualifier = named("recurringSync")

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

    // Single, not factory: SyncDataUseCase holds a Mutex that serializes concurrent sync
    // cycles. A fresh Mutex per injection (factoryOf) would defeat that serialization.
    singleOf(::SyncDataUseCase)
}
