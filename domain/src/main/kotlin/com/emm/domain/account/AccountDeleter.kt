package com.emm.domain.account

class AccountDeleter(private val repository: AccountRepository) {

    suspend fun delete(accountId: String) {
        repository.delete(accountId)
    }
}