package com.emm.domain.account

import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository

/**
 * Tombstones an account, with a live-row guard enforced in the domain layer.
 *
 * SQL ON DELETE RESTRICT only fires on hard DELETE, not on soft-delete UPDATEs.
 * This use case replicates that guard using live-row counts (deletedAt IS NULL):
 *   - If any live transaction references this account, block with ValidationError.
 *   - If any live recurring_movement references this account, block with ValidationError.
 *   - Otherwise, tombstone the account.
 *
 * Using countLiveByAccount (filters deletedAt IS NULL) means tombstoned transactions
 * no longer prevent deletion — only active rows do (DECISION 4).
 */
class DeleteAccountUseCase(
    private val repository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringMovementRepository: RecurringMovementRepository,
) {

    suspend operator fun invoke(accountId: AccountId) {
        val liveTransactions = transactionRepository.countLiveByAccount(accountId)
        if (liveTransactions > 0) {
            throw DomainException.ValidationError(
                "Cannot delete an account with transactions. Delete or move them first.",
            )
        }
        val liveRecurring = recurringMovementRepository.countLiveByAccount(accountId)
        if (liveRecurring > 0) {
            throw DomainException.ValidationError(
                "Cannot delete an account with active recurring movements. Remove them first.",
            )
        }
        repository.delete(accountId)
    }
}
