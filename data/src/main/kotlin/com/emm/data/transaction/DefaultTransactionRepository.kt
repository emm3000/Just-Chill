package com.emm.data.transaction

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTransactionRepository(
    private val localDataSource: TransactionLocalDataSource,
) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) = safeDbCall {
        localDataSource.create(transactionInsert)
    }

    override fun all(): Flow<List<Transaction>> {
        return localDataSource.all().catchAsDomainException()
    }

    override fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>> {
        return localDataSource.completeTransactions()
            .map { it.toDomain() }
            .catchAsDomainException()
    }

    override fun fetchAllWithCategoryInRange(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<TransactionWithCategory>> {
        return localDataSource.completeTransactionsByDateRange(startInclusive, endExclusive)
            .map { it.toDomain() }
            .catchAsDomainException()
    }

    override suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate): Unit = safeDbCall {
        localDataSource.update(transactionId.value, transactionUpdate)
        Unit
    }

    override suspend fun delete(transactionId: TransactionId): Unit = safeDbCall {
        localDataSource.delete(transactionId.value)
        Unit
    }

    override fun find(transactionId: TransactionId): Transaction? {
        return localDataSource.find(transactionId.value)
    }
}
