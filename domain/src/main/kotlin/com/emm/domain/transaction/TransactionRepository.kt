package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    suspend fun find(transactionId: TransactionId): Transaction?

    fun all(): Flow<List<Transaction>>

    fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>>

    fun fetchAllWithCategoryInRange(startInclusive: Long, endExclusive: Long): Flow<List<TransactionWithCategory>>

    suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate)

    suspend fun countByAccount(accountId: AccountId): Long

    /** Count live (non-tombstoned) transactions for the given account. */
    suspend fun countLiveByAccount(accountId: AccountId): Long

    /** Null out categoryId on all live (deletedAt IS NULL) transactions referencing this category,
     *  and set syncState = 'Pending' on those rows. */
    suspend fun nullCategoryOnLiveRows(categoryId: CategoryId)

    suspend fun delete(transactionId: TransactionId)

    fun searchWithCategory(filter: TransactionFilter): Flow<List<TransactionWithCategory>>
}
