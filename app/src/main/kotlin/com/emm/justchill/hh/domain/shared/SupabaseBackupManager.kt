package com.emm.justchill.hh.domain.shared

import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.withContext

class SupabaseBackupManager(
    dispatchersProvider: DispatchersProvider,
    private val transactionRepository: TransactionRepository,
    private val sharedRepository: SharedRepository,
) : BackupManager, DispatchersProvider by dispatchersProvider {

    override suspend fun backup(): Boolean = withContext(ioDispatcher) {
        if (sharedRepository.existData().not()) return@withContext false

        try {
            transactionRepository.backup()
            true
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    override suspend fun seed(): Boolean = withContext(ioDispatcher) {
        if (sharedRepository.existData()) {
            return@withContext false
        }

        try {
            transactionRepository.seed()
            true
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }
}