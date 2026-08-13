package com.emm.domain.recurring

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRecurringMovementRepository : RecurringMovementRepository {

    private val store = MutableStateFlow<MutableMap<String, RecurringMovement>>(mutableMapOf())

    var createCount = 0
        private set
    var updateCount = 0
        private set
    var deleteCount = 0
        private set
    var confirmCount = 0
        private set
    var lastConfirmInsert: TransactionInsert? = null
        private set
    var lastConfirmPeriod: String? = null
        private set
    var skipCount = 0
        private set
    var lastSkipPeriod: String? = null
        private set

    fun addTemplate(vararg templates: RecurringMovement) {
        val map = store.value.toMutableMap()
        templates.forEach { map[it.id.value] = it }
        store.value = map
    }

    override suspend fun countLiveByAccount(accountId: AccountId): Long = 0L

    override suspend fun create(insert: RecurringMovementInsert) {
        createCount++
    }

    override suspend fun update(id: RecurringMovementId, insert: RecurringMovementInsert) {
        updateCount++
        val existing = store.value[id.value] ?: return
        val updated = existing.copy(
            name = insert.name,
            type = insert.type,
            amount = insert.amount,
            description = insert.description,
            categoryId = insert.categoryId,
            accountId = insert.accountId,
            dayOfMonth = insert.dayOfMonth,
            isActive = insert.isActive,
        )
        val map = store.value.toMutableMap()
        map[id.value] = updated
        store.value = map
    }

    override suspend fun delete(id: RecurringMovementId) {
        deleteCount++
        val map = store.value.toMutableMap()
        map.remove(id.value)
        store.value = map
    }

    override suspend fun find(id: RecurringMovementId): RecurringMovement? = store.value[id.value]

    override fun allActive(): Flow<List<RecurringMovement>> = store.map { it.values.filter { rm -> rm.isActive } }

    // No tombstone in this fake — `delete` removes the entry outright — so every stored template is
    // a live one, paused ones included. That is the whole difference from `allActive` above.
    override fun allLive(): Flow<List<RecurringMovement>> = store.map { it.values.toList() }

    override fun allWithDetails(): Flow<List<RecurringMovementDetails>> = store.map { map ->
        map.values.map { rm ->
            RecurringMovementDetails(
                id = rm.id.value,
                name = rm.name,
                type = rm.type,
                amount = rm.amount,
                categoryName = null,
                categoryColor = null,
                accountName = "FakeAccount",
                dayOfMonth = rm.dayOfMonth,
                isActive = rm.isActive,
            )
        }
    }

    override suspend fun confirm(insert: TransactionInsert, recurringId: RecurringMovementId, period: String) {
        confirmCount++
        lastConfirmInsert = insert
        lastConfirmPeriod = period
        markSettled(recurringId, period)
    }

    override suspend fun skip(recurringId: RecurringMovementId, period: String) {
        skipCount++
        lastSkipPeriod = period
        markSettled(recurringId, period)
    }

    /** Moves the high-water mark, so the monotonic guard can be exercised. */
    private fun markSettled(recurringId: RecurringMovementId, period: String) {
        val map = store.value.toMutableMap()
        val existing = map[recurringId.value] ?: return
        map[recurringId.value] = existing.copy(lastConfirmedPeriod = period)
        store.value = map
    }
}
