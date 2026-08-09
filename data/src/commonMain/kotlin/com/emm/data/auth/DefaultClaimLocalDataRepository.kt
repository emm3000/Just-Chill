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
 * Stamps all anonymous-local rows (userId IS NULL) across the four tables with [userId] and marks
 * them Pending for sync, inside a single atomic SQLDelight transaction.
 *
 * Design rationale: this repository touches all four tables in one go, so it takes [EmmDatabaseData]
 * directly rather than going through per-entity LocalDataSources. Introducing four separate data
 * sources just to call a single claimAll query each would be over-engineering; the pattern matches
 * [DefaultBackupRepository] which also operates multi-table within one transaction.
 */
class DefaultClaimLocalDataRepository(private val db: EmmDatabaseData) : ClaimLocalDataRepository {

    override suspend fun claimAll(userId: String): Unit = safeDbCall {
        // Move the blocking transaction off the caller's dispatcher (callers reach this from
        // viewModelScope / Main via SignInUseCase). Mirrors the per-entity LocalDataSources.
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

    // catchAsDomainException, like every other observe flow in this module: this one is collected
    // by the claim observer on the application scope, where a raw SQLite throw is an uncaught
    // exception rather than a failed call.
    override fun observeUnclaimedCount(): Flow<Long> = combine(
        db.accountsQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
        db.categoriesQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
        db.transactionsQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
        db.recurring_movementsQueries.countUnclaimed().asFlow().mapToOne(ioDispatcher),
    ) { acc, cat, txn, rec -> acc + cat + txn + rec }.catchAsDomainException()
}
