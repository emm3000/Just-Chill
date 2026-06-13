package com.emm.domain.category

import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.TransactionRepository

/**
 * Tombstones a category and enforces referential integrity in the domain layer.
 *
 * SQL ON DELETE SET NULL only fires on hard DELETE rows, not on soft-delete UPDATEs.
 * This use case replicates that integrity rule:
 *   1. Null categoryId on all live (non-tombstoned) transactions referencing this category.
 *   2. Null categoryId on all live recurring_movements referencing this category.
 *   3. Tombstone (soft-delete) the category itself.
 *
 * All nulled rows are also marked syncState = 'Pending' so the de-link propagates
 * to other devices on next sync (DECISION 4).
 */
class DeleteCategoryUseCase(
    private val repository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringMovementRepository: RecurringMovementRepository,
) {

    suspend operator fun invoke(categoryId: CategoryId) {
        transactionRepository.nullCategoryOnLiveRows(categoryId)
        recurringMovementRepository.nullCategoryOnLiveRows(categoryId)
        repository.delete(categoryId)
    }
}
