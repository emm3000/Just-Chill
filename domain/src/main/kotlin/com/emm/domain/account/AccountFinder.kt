package com.emm.domain.account

class AccountFinder(private val repository: AccountRepository) {

    suspend fun find(accountId: String): Account? {
        return repository.find(accountId)
    }
}