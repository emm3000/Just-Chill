package com.emm.justchill.hh.domain

import android.content.Context
import android.content.SharedPreferences
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.withContext

class SupabaseBackupManager(
    dispatchersProvider: DispatchersProvider,
    private val transactionRepository: TransactionRepository,
    private val sharedRepository: SharedRepository,
    private val context: Context,
) : BackupManager, DispatchersProvider by dispatchersProvider {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("random", Context.MODE_PRIVATE)
    }

    private val edit: SharedPreferences.Editor get() = prefs.edit()

    override suspend fun backup(): Boolean = withContext(ioDispatcher) {
        if (sharedRepository.existData().not()) return@withContext false

        try {
            transactionRepository.backup()
            true
        } catch (e: Throwable) {
            logError("backup", e.message)
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
            logError("seed", e.message)
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        }
    }

    private fun logError(function: String, errorMessage: String?) {
        if (errorMessage == null) return
        edit
            .putString("error", "$function: $errorMessage")
            .apply()
    }
}