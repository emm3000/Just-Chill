package com.emm.justchill.hh.account.domain

class AccountDeleter(private val repository: AccountRepository) {

    suspend fun delete(accountId: String) {
        repository.deleteBy(accountId)
    }
}