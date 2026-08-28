package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun create(transactionInsert: TransactionInsert)

    suspend fun find(transactionId: TransactionId): Transaction?

    // No plain `all()` here: `allInRange` replaced its one caller (AccountsViewModel, E14-06), and
    // an unused read method is a liability nothing else in this interface carries.
    fun allInRange(startInclusive: String, endExclusive: String): Flow<List<Transaction>>

    fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>>

    fun fetchAllWithCategoryInRange(startInclusive: String, endExclusive: String): Flow<List<TransactionWithCategory>>

    fun observeTotals(): Flow<TransactionTotals>

    fun observeCategoryUsageCounts(): Flow<Map<CategoryId, Int>>

    suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate)

    suspend fun countLiveByAccount(accountId: AccountId): Long

    suspend fun delete(transactionId: TransactionId)

    fun searchWithCategory(filter: TransactionFilter): Flow<List<TransactionWithCategory>>
}
