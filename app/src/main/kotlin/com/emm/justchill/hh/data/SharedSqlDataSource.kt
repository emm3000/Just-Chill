package com.emm.justchill.hh.data

import com.emm.justchill.CategoriesQueries
import com.emm.justchill.TransactionQueries
import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.core.DispatchersProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.withContext

class SharedSqlDataSource(
    dispatchersProvider: DispatchersProvider,
    private val categoryQueries: CategoriesQueries,
    private val transactionQueries: TransactionQueries,
    private val queries: TransactionsCategoriesQueries,
) : DispatchersProvider by dispatchersProvider {

    suspend fun hasData(): Boolean = withContext(ioDispatcher) {
        try {
            val categoryCount = categoryQueries.count().executeAsOneOrNull() ?: 0L
            val transactionCount = transactionQueries.count().executeAsOneOrNull() ?: 0L
            val transactionCategoryCount = queries.count().executeAsOneOrNull() ?: 0L
            categoryCount > 0 || transactionCount > 0 || transactionCategoryCount > 0
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            e.printStackTrace()
            false
        }
    }

}