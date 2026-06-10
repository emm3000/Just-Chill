package com.emm.data.auth

import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
import com.emm.domain.auth.ClaimLocalDataRepository
import kotlinx.coroutines.Dispatchers
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
        withContext(Dispatchers.IO) {
            db.transaction {
                db.accountsQueries.claimAll(userId)
                db.categoriesQueries.claimAll(userId)
                db.transactionsQueries.claimAll(userId)
                db.recurring_movementsQueries.claimAll(userId)
            }
        }
    }
}
