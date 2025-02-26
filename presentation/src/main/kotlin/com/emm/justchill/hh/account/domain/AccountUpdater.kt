package com.emm.justchill.hh.account.domain

class AccountUpdater(private val repository: AccountRepository) {

    suspend fun update(accountId: String, account: AccountUpsert) {
        repository.update(accountId, account)
    }
}