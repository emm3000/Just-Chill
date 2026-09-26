package com.emm.justchill.core.domain.account

import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.domain.transaction.TransactionRepository

class DeleteAccountUseCase(
    private val repository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {

    suspend operator fun invoke(accountId: AccountId) {
        val liveTransactions: Long = transactionRepository.countLiveByAccount(accountId)
        if (liveTransactions > 0) {
            throw DomainException.ValidationError(
                "Cannot delete an account with transactions. Delete or move them first.",
                ValidationCode.AccountHasTransactions,
            )
        }
        repository.delete(accountId)
    }
}
