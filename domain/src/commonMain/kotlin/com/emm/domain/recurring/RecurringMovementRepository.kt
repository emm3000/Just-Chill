package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface RecurringMovementRepository {

    /** Count live (non-tombstoned) recurring movements for the given account. */
    suspend fun countLiveByAccount(accountId: AccountId): Long

    suspend fun create(insert: RecurringMovementInsert)

    suspend fun update(id: RecurringMovementId, insert: RecurringMovementInsert)

    suspend fun delete(id: RecurringMovementId)

    suspend fun find(id: RecurringMovementId): RecurringMovement?

    fun allActive(): Flow<List<RecurringMovement>>

    fun allWithDetails(): Flow<List<RecurringMovementDetails>>

    suspend fun confirm(insert: TransactionInsert, recurringId: RecurringMovementId, period: String)

    /** Advances the high-water mark to [period] without creating a transaction. */
    suspend fun skip(recurringId: RecurringMovementId, period: String)
}
