package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.TransactionId
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    suspend fun find(transactionId: TransactionId): Transaction?

    fun all(): Flow<List<Transaction>>

    fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>>

    fun fetchAllWithCategoryInRange(startInclusive: Long, endExclusive: Long): Flow<List<TransactionWithCategory>>

    /**
     * Whole-ledger balance and movement count, aggregated by the database.
     *
     * Callers that need only these two numbers must use this instead of [fetchAllWithCategory]:
     * the latter re-reads and re-folds every transaction ever recorded on each emission.
     */
    fun observeTotals(): Flow<TransactionTotals>

    suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate)

    suspend fun countByAccount(accountId: AccountId): Long

    /** Count live (non-tombstoned) transactions for the given account. */
    suspend fun countLiveByAccount(accountId: AccountId): Long

    suspend fun delete(transactionId: TransactionId)

    fun searchWithCategory(filter: TransactionFilter): Flow<List<TransactionWithCategory>>
}
