package com.emm.domain.auth

/**
 * Stamps every anonymous-local row (userId IS NULL) with the given [userId] and marks it
 * Pending for sync, across the four tables (transactions, categories, accounts, recurring movements).
 *
 * Idempotent: rows that already carry a userId are left untouched — the WHERE clause is the guarantee.
 * Implementation must execute all updates inside a single atomic transaction.
 */
interface ClaimLocalDataRepository {

    suspend fun claimAll(userId: String)
}
