package com.emm.data.recurring

import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.safeDbCall
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow

class DefaultRecurringMovementRepository(private val localDataSource: RecurringMovementLocalDataSource) :
    RecurringMovementRepository {

    override fun all(): Flow<List<RecurringMovement>> = localDataSource.all().catchAsDomainException()

    override fun allActive(): Flow<List<RecurringMovement>> = localDataSource.allActive().catchAsDomainException()

    override fun allWithDetails(): Flow<List<RecurringMovementDetails>> =
        localDataSource.allWithDetails().catchAsDomainException()

    override suspend fun find(id: RecurringMovementId): RecurringMovement? = safeDbCall {
        localDataSource.find(id.value)
    }

    override suspend fun create(insert: RecurringMovementInsert): Unit = safeDbCall {
        localDataSource.create(insert)
    }

    override suspend fun update(id: RecurringMovementId, insert: RecurringMovementInsert): Unit = safeDbCall {
        localDataSource.update(id.value, insert)
    }

    override suspend fun countLiveByAccount(accountId: AccountId): Long = safeDbCall {
        localDataSource.countLiveByAccount(accountId.value)
    }

    override suspend fun nullCategoryOnLiveRows(categoryId: CategoryId): Unit = safeDbCall {
        localDataSource.nullCategoryOnLiveRows(categoryId.value)
        Unit
    }

    override suspend fun delete(id: RecurringMovementId): Unit = safeDbCall {
        localDataSource.softDelete(id.value)
    }

    override suspend fun confirm(insert: TransactionInsert, recurringId: RecurringMovementId, period: String): Unit =
        safeDbCall {
            localDataSource.confirm(insert, recurringId.value, period)
        }
}
