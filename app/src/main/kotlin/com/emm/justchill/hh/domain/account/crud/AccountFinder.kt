package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import kotlinx.coroutines.flow.Flow

class AccountFinder(private val repository: AccountRepository) {

    fun find(accountId: String): Flow<Account?> {
        return repository.findBy(accountId)
    }
}