package com.emm.domain.account

class AccountUpdater(private val repository: AccountRepository) {

    suspend fun update(accountId: String, account: AccountUpsert) {
        repository.update(accountId, account)
    }
}