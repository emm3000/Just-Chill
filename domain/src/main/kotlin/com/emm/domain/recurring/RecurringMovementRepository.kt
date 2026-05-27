package com.emm.domain.recurring

import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface RecurringMovementRepository {

    suspend fun create(insert: RecurringMovementInsert)

    suspend fun update(id: RecurringMovementId, insert: RecurringMovementInsert)

    suspend fun delete(id: RecurringMovementId)

    suspend fun find(id: RecurringMovementId): RecurringMovement?

    fun all(): Flow<List<RecurringMovement>>

    fun allActive(): Flow<List<RecurringMovement>>

    suspend fun confirm(insert: TransactionInsert, recurringId: String, period: String)
}
