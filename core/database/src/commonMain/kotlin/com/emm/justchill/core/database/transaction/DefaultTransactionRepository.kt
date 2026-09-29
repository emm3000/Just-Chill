package com.emm.justchill.core.database.transaction

import com.emm.justchill.core.database.shared.catchAsDomainException
import com.emm.justchill.core.database.shared.safeDbCall
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionFilter
import com.emm.justchill.core.domain.transaction.TransactionInsert
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionTotals
import com.emm.justchill.core.domain.transaction.TransactionUpdate
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTransactionRepository(private val localDataSource: TransactionLocalDataSource) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) = safeDbCall {
        localDataSource.create(transactionInsert)
    }

    override fun allInRange(startInclusive: String, endExclusive: String): Flow<List<Transaction>> =
        localDataSource.byDateRange(startInclusive, endExclusive).catchAsDomainException()

    override fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>> = localDataSource.completeTransactions()
        .map { it.toDomain() }
        .catchAsDomainException()

    override fun fetchAllWithCategoryInRange(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<TransactionWithCategory>> =
        localDataSource.completeTransactionsByDateRange(startInclusive, endExclusive)
            .map { it.toDomain() }
            .catchAsDomainException()

    override fun observeTotals(): Flow<TransactionTotals> = localDataSource.liveTotals()
        .map { it.toDomain() }
        .catchAsDomainException()

    override fun observeCategoryUsageCounts(): Flow<Map<CategoryId, Int>> = localDataSource.countPerCategory()
        .map { it.toDomain() }
        .catchAsDomainException()

    override suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate): Unit = safeDbCall {
        localDataSource.update(transactionId.value, transactionUpdate)
        Unit
    }

    override suspend fun countLiveByAccount(accountId: AccountId): Long = safeDbCall {
        localDataSource.countLiveByAccount(accountId.value)
    }

    override suspend fun delete(transactionId: TransactionId): Unit = safeDbCall {
        localDataSource.softDelete(transactionId.value)
        Unit
    }

    override suspend fun find(transactionId: TransactionId): Transaction? = safeDbCall {
        localDataSource.find(transactionId.value)
    }

    override fun searchWithCategory(filter: TransactionFilter): Flow<List<TransactionWithCategory>> =
        localDataSource.searchTransactions(
            query = filter.query,
            categoryIds = filter.categoryIds.map { it.value }.toSet(),
            minAmountCents = filter.minAmount?.cents,
            maxAmountCents = filter.maxAmount?.cents,
        )
            .map { it.toDomain() }
            .catchAsDomainException()
}
