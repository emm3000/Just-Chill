package com.emm.justchill.hh.account.domain

import kotlinx.coroutines.flow.Flow

class AccountFinder(private val repository: AccountRepository) {

    fun find(accountId: String): Flow<Account?> {
        return repository.findBy(accountId)
    }
}