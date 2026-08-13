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

    /**
     * Every LIVE template — paused ones included — as the flat model, with nothing joined on.
     *
     * The third all-row read on this interface, and it exists because neither of the other two can
     * answer "what does this device own". [allActive] filters `isActive = 1`, so exporting through it
     * would drop every paused template — data the user still owns and no backup could put back.
     * [allWithDetails] keeps them but is a joined view carrying a category name, a category colour
     * and an account name, which is a screen's shape, not a record's.
     *
     * The one consumer is the backup export. It reads accounts, categories and transactions through
     * their repository interfaces, and this is what lets it read the fourth table the same way rather
     * than reaching past the layer into SQLDelight for one table alone.
     */
    fun allLive(): Flow<List<RecurringMovement>>

    fun allWithDetails(): Flow<List<RecurringMovementDetails>>

    suspend fun confirm(insert: TransactionInsert, recurringId: RecurringMovementId, period: String)

    /** Advances the high-water mark to [period] without creating a transaction. */
    suspend fun skip(recurringId: RecurringMovementId, period: String)
}
