package com.emm.domain.account

import kotlinx.coroutines.flow.Flow

class AccountFinder(private val repository: AccountRepository) {

    fun find(accountId: String): Flow<Account?> {
        return repository.findBy(accountId)
    }
}