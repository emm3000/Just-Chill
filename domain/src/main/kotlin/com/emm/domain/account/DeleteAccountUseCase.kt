package com.emm.domain.account

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository

class DeleteAccountUseCase(
    private val repository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {

    suspend operator fun invoke(accountId: AccountId) {
        val count = transactionRepository.countByAccount(accountId)
        if (count > 0) {
            throw DomainException.ValidationError(
                "Cannot delete an account with transactions. Delete or move them first.",
            )
        }
        repository.delete(accountId)
    }
}
