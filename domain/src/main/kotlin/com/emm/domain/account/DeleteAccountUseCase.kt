package com.emm.domain.account

import com.emm.domain.shared.AccountId

class DeleteAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: AccountId) {
        repository.delete(accountId)
    }
}
