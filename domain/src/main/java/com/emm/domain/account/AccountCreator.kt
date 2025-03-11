package com.emm.domain.account

class AccountCreator(private val repository: AccountRepository) {

    suspend fun create(accountUpsert: AccountUpsert) {
        repository.create(accountUpsert)
    }
}