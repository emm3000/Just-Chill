package com.emm.data.transaction

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.MonthlySectionStats
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<TransactionWithCategory>> =
        localDataSource.completeTransactionsByDateRange(startInclusive, endExclusive)
            .map { it.toDomain() }
            .catchAsDomainException()

    override suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate): Unit = safeDbCall {
        localDataSource.update(transactionId.value, transactionUpdate)
        Unit
    }

    override suspend fun countByAccount(accountId: AccountId): Long = safeDbCall {
        localDataSource.countByAccount(accountId.value)
    }

    override suspend fun delete(transactionId: TransactionId): Unit = safeDbCall {
        localDataSource.delete(transactionId.value)
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

    override suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): List<CategoryAmount> = safeDbCall {
        localDataSource.monthlyAmountByCategory(type, startInclusive, endExclusive)
            .map { it.toDomain() }
    }

    override suspend fun monthlyStats(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): MonthlySectionStats = safeDbCall {
        val (count, total) = localDataSource.monthlyStats(type, startInclusive, endExclusive)
        val average = if (count == 0L) Money.Zero else Money(total / count)
        MonthlySectionStats(
            movementCount = count.toInt(),
            averageAmount = average,
        )
    }

    override suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: Long, limit: Int): List<CategoryId> =
        safeDbCall {
            localDataSource.topUsedCategoryIds(type, startInclusive, limit.toLong())
                .map { CategoryId(it) }
        }
}
