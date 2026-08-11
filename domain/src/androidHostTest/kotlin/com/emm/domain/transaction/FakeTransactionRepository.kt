package com.emm.domain.transaction

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTransactionRepository : TransactionRepository {

    var lastCreated: TransactionInsert? = null
    var lastUpdatedId: TransactionId? = null
    var lastUpdate: TransactionUpdate? = null
    var lastDeleted: TransactionId? = null
    var transactionToReturn: Transaction? = null
    var allWithCategory: List<TransactionWithCategory> = emptyList()
    var rangeWithCategory: List<TransactionWithCategory> = emptyList()
    var searchWithCategoryToReturn: List<TransactionWithCategory> = emptyList()
    var countByAccountToReturn: Long = 0L
    var countLiveByAccountToReturn: Long = 0L
    var totalsToReturn: TransactionTotals = TransactionTotals.Empty
    var categoryUsageCountsToReturn: Map<CategoryId, Int> = emptyMap()

    override suspend fun create(transactionInsert: TransactionInsert) {
        lastCreated = transactionInsert
    }

    override suspend fun find(transactionId: TransactionId): Transaction? = transactionToReturn

    override fun all(): Flow<List<Transaction>> = flowOf(emptyList())

    override fun fetchAllWithCategory(): Flow<List<TransactionWithCategory>> = flowOf(allWithCategory)

    override fun fetchAllWithCategoryInRange(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<TransactionWithCategory>> = flowOf(rangeWithCategory)

    override fun observeTotals(): Flow<TransactionTotals> = flowOf(totalsToReturn)

    override fun observeCategoryUsageCounts(): Flow<Map<CategoryId, Int>> = flowOf(categoryUsageCountsToReturn)

    override suspend fun update(transactionId: TransactionId, transactionUpdate: TransactionUpdate) {
        lastUpdatedId = transactionId
        lastUpdate = transactionUpdate
    }

    override suspend fun countByAccount(accountId: AccountId): Long = countByAccountToReturn

    override suspend fun countLiveByAccount(accountId: AccountId): Long = countLiveByAccountToReturn

    override suspend fun delete(transactionId: TransactionId) {
        lastDeleted = transactionId
    }

    override fun searchWithCategory(filter: TransactionFilter): Flow<List<TransactionWithCategory>> =
        flowOf(searchWithCategoryToReturn)
}
