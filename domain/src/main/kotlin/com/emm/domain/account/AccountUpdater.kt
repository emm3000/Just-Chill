package com.emm.domain.account

class UpdateAccountUseCase(private val repository: AccountUpdateRepository) {

    suspend operator fun invoke(accountId: String, account: AccountUpsert) {
        repository.update(accountId, account)
    }
}
