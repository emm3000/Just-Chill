package com.emm.data.transaction

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Implements the domain interface one-to-one; the count is the contract's, not this class's.
@Suppress("TooManyFunctions")
class DefaultTransactionRepository(private val localDataSource: TransactionLocalDataSource) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) = safeDbCall {
        localDataSource.create(transactionInsert)
    }

    override fun all(): Flow<List<Transaction>> = localDataSource.all().catchAsDomainException()

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

    override suspend fun countByAccount(accountId: AccountId): Long = safeDbCall {
        localDataSource.countByAccount(accountId.value)
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
        )
            .map { it.toDomain() }
            .catchAsDomainException()
}
