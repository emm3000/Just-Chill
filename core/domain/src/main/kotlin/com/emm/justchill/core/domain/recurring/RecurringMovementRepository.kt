package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow

interface RecurringMovementRepository {

    suspend fun countLiveByAccount(accountId: AccountId): Long

    suspend fun create(insert: RecurringMovementInsert)

    suspend fun update(id: RecurringMovementId, insert: RecurringMovementInsert)

    suspend fun delete(id: RecurringMovementId)

    suspend fun find(id: RecurringMovementId): RecurringMovement?

    fun allActive(): Flow<List<RecurringMovement>>

    fun allWithDetails(): Flow<List<RecurringMovementDetails>>

    suspend fun confirm(insert: TransactionInsert, recurringId: RecurringMovementId, period: String)

    suspend fun skip(recurringId: RecurringMovementId, period: String)
}
