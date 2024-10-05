package com.emm.justchill.hh.shared

import com.emm.justchill.TransactionQueries
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.account.domain.AccountRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SharedSqlDataSource(
    dispatchersProvider: DispatchersProvider,
    private val transactionQueries: TransactionQueries,
    private val accountRepository: AccountRepository,
) : DispatchersProvider by dispatchersProvider {

    suspend fun hasAnyDataInLocalDataBase(): Boolean = withContext(ioDispatcher) {
        try {
            val transactionCount = transactionQueries.count().executeAsOneOrNull() ?: 0L
            val firstOrNull: Int = accountRepository.retrieve().firstOrNull()?.count() ?: 0
            transactionCount > 0 || firstOrNull > 0
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            e.printStackTrace()
            false
        }
    }
}