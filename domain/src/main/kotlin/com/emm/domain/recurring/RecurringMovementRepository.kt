package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface RecurringMovementRepository {

    /** Count live (non-tombstoned) recurring movements for the given account. */
    suspend fun countLiveByAccount(accountId: AccountId): Long

    /** Null out categoryId on all live (deletedAt IS NULL) recurring movements referencing this category,
     *  and set syncState = 'Pending' on those rows. */
    suspend fun nullCategoryOnLiveRows(categoryId: CategoryId)

    suspend fun create(insert: RecurringMovementInsert)

    suspend fun update(id: RecurringMovementId, insert: RecurringMovementInsert)

    suspend fun delete(id: RecurringMovementId)

    suspend fun find(id: RecurringMovementId): RecurringMovement?

    fun all(): Flow<List<RecurringMovement>>

    fun allActive(): Flow<List<RecurringMovement>>

    fun allWithDetails(): Flow<List<RecurringMovementDetails>>

    suspend fun confirm(insert: TransactionInsert, recurringId: RecurringMovementId, period: String)
}
