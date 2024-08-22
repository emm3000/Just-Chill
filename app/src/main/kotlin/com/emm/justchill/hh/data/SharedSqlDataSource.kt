package com.emm.justchill.hh.data

import com.emm.justchill.TransactionQueries
import com.emm.justchill.core.DispatchersProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.withContext

class SharedSqlDataSource(
    dispatchersProvider: DispatchersProvider,
    private val transactionQueries: TransactionQueries,
) : DispatchersProvider by dispatchersProvider {

    suspend fun hasAnyDataInLocalDataBase(): Boolean = withContext(ioDispatcher) {
        try {
            val transactionCount = transactionQueries.count().executeAsOneOrNull() ?: 0L
            transactionCount > 0
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            e.printStackTrace()
            false
        }
    }
}