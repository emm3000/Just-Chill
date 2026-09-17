package com.emm.justchill.core.domain.account

import com.emm.justchill.core.domain.recurring.RecurringMovementRepository
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.domain.transaction.TransactionRepository

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
                ValidationCode.AccountHasTransactions,
            )
        }
        val liveRecurring = recurringMovementRepository.countLiveByAccount(accountId)
        if (liveRecurring > 0) {
            throw DomainException.ValidationError(
                "Cannot delete an account with active recurring movements. Remove them first.",
                ValidationCode.AccountHasRecurringMovements,
            )
        }
        repository.delete(accountId)
    }
}
