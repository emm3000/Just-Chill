package com.emm.justchill.hh.data.transactioncategory

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.TransactionsCategories
import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.hh.domain.AndroidIdProvider
import com.emm.justchill.hh.domain.TransactionCategoryModel
import com.emm.justchill.hh.domain.toModel
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionCategoryRepository(
    dispatchersProvider: DispatchersProvider,
    private val queries: TransactionsCategoriesQueries,
    private val networkDataSource: TransactionCategoryNetworkDataSource,
    private val androidIdProvider: AndroidIdProvider,
) : TransactionCategoryRepository, DispatchersProvider by dispatchersProvider {

    override suspend fun add(transactionId: String, categoryId: String) {
        queries.add(
            transactionId = transactionId,
            categoryId = categoryId
        )
    }

    override fun retrieve(): Flow<List<TransactionsCategories>> {
        return queries.retrieve()
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override suspend fun seed() {
        val transactionCategoryModels: List<TransactionCategoryModel> = networkDataSource.retrieve()
        queries.transaction {
            populate(transactionCategoryModels)
        }
    }

    private fun populate(data: List<TransactionCategoryModel>) = with(data) {
        forEach {
            queries.add(transactionId = it.transactionId, categoryId = it.categoryId)
        }
    }

    override suspend fun backup() {
        val transactionCategory: List<TransactionCategoryModel> = queries
            .retrieve()
            .executeAsList()
            .map(TransactionsCategories::toModel)
            .map { it.copy(deviceId = androidIdProvider.androidId()) }

        networkDataSource.upsert(transactionCategory)
    }
}