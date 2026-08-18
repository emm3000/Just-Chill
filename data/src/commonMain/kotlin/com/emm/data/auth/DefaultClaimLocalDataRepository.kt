package com.emm.data.auth

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.safeDbCall
import com.emm.domain.auth.ClaimLocalDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Takes EmmDatabaseData directly rather than per-entity LocalDataSources: the four tables are
 * claimed in one atomic transaction, which no per-entity data source can span.
 */
class DefaultClaimLocalDataRepository(private val db: EmmDatabaseData) : ClaimLocalDataRepository {

    override suspend fun claimAll(userId: String): Unit = safeDbCall {
        withContext(ioDispatcher) {
            db.transaction {
                db.accountsQueries.claimAll(userId)
                db.categoriesQueries.claimAll(userId)
                db.transactionsQueries.claimAll(userId)
                db.recurring_movementsQueries.claimAll(userId)
            }
        }
    }

    override suspend fun unclaimAll(userId: String): Unit = safeDbCall {
        withContext(ioDispatcher) {
            db.transaction {
                db.transactionsQueries.unclaimAll(userId)
                db.recurring_movementsQueries.unclaimAll(userId)
                db.categoriesQueries.unclaimAll(userId)
                db.accountsQueries.unclaimAll(userId)
            }
        }
    }

    override fun observeUnclaimedCount(): Flow<Long> = combine(
        db.accountsQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
        db.categoriesQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
        db.transactionsQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
        db.recurring_movementsQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
    ) { acc, cat, txn, rec -> acc + cat + txn + rec }.catchAsDomainException()
}
