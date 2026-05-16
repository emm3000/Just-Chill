package com.emm.domain.account

class UpdateAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(accountId: String, account: AccountUpsert) {
        repository.update(accountId, account)
    }
}
