package com.emm.domain.account

import com.emm.domain.shared.AccountId

class UpdateAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: AccountId, account: AccountUpsert) {
        repository.update(accountId, account)
    }
}
